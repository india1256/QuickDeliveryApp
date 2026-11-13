package com.example.data.cloud

import android.content.Context
import android.os.Build
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.AddressDetails
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.DeliveryProof
import com.example.data.model.OrderProofRegistry
import com.example.data.model.OrderStatus
import com.example.data.model.PackageDetails
import com.example.data.model.PackageType
import com.example.data.model.PaymentVerificationStatus
import com.example.data.model.PickupProof
import com.example.data.model.VehicleType
import com.example.data.repository.SharedOrderRepository
import com.example.security.OrderAlertSoundManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LivePhoneSyncStatus(
    val isConnected: Boolean = false,
    val activePhoneRole: String = "UNIFIED",
    val syncedOrdersCount: Int = 0,
    val connectedPhonesCount: Int = 1,
    val connectedRolesSummary: String = "Ready for 3-Phone Live Sync",
    val lastSyncTimeLabel: String = "Connecting..."
)

/**
 * Real-Time Multi-Phone Live Sync & Phone Role Lock Manager.
 *
 * Enables 3 separate phones (Phone 1 = Customer APK, Phone 2 = Rider Partner APK, Phone 3 = Admin Console APK)
 * to synchronize orders, 4-digit Pickup/Delivery OTPs, live GPS coordinates, 80%/20% fare splits, and
 * finger-drawn Digital Signature proofs in real time (~1.5s latency) with zero manual setup.
 */
object MultiPhoneLiveSyncManager {
    private const val PREFS_NAME = "quick_delivery_multi_phone_live_sync_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_LOCKED_ROLE = "locked_phone_role"

    private val LIVE_SYNC_ENDPOINTS = listOf(
        "https://ais-dev-l6kwxrdc54lqesfozowtv2-809441526823.asia-southeast1.run.app/api/live-sync",
        "https://ais-pre-l6kwxrdc54lqesfozowtv2-809441526823.asia-southeast1.run.app/api/live-sync"
    )

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alertedOrderIds = ConcurrentHashMap.newKeySet<String>()
    private var isLoopStarted = false

    @Volatile
    private var localResetEpochMs: Long = 0L

    private val _syncStatus = MutableStateFlow(LivePhoneSyncStatus())
    val syncStatus: StateFlow<LivePhoneSyncStatus> = _syncStatus.asStateFlow()

    private val _lockedPhoneRole = MutableStateFlow("UNIFIED")
    val lockedPhoneRole: StateFlow<String> = _lockedPhoneRole.asStateFlow()

    private val isRobolectricUnitTest: Boolean by lazy {
        runCatching {
            Class.forName("org.robolectric.Robolectric")
            true
        }.getOrDefault(false)
    }

    fun getOrInitDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "PH-${UUID.randomUUID().toString().replace("-", "").take(8).uppercase()}"
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    private val DEMO_ORDER_IDS = setOf("QD-7001", "QD-6204", "QD-5910", "QD-5402", "ORD-1001", "ORD-1002")

    fun resolveLockedPhoneRole(context: Context, buildConfigRole: String): String {
        val normalizedBuildRole = buildConfigRole.trim().uppercase()
        if (normalizedBuildRole == "CUSTOMER" || normalizedBuildRole == "RIDER" || normalizedBuildRole == "ADMIN") {
            _lockedPhoneRole.value = normalizedBuildRole
            return normalizedBuildRole
        }
        val pkg = context.packageName.lowercase()
        if (pkg.endsWith(".customer") || pkg.endsWith(".cust01")) {
            _lockedPhoneRole.value = "CUSTOMER"
            return "CUSTOMER"
        }
        if (pkg.endsWith(".rider") || pkg.endsWith(".ride02")) {
            _lockedPhoneRole.value = "RIDER"
            return "RIDER"
        }
        if (pkg.endsWith(".admin") || pkg.endsWith(".admn03")) {
            _lockedPhoneRole.value = "ADMIN"
            return "ADMIN"
        }
        // In the unified preview APK, always start in UNIFIED mode so the 3-tab bottom bar is accessible
        _lockedPhoneRole.value = "UNIFIED"
        return "UNIFIED"
    }

    fun setLockedPhoneRole(context: Context, role: String) {
        val cleanRole = role.trim().uppercase()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOCKED_ROLE, cleanRole).apply()
        _lockedPhoneRole.value = cleanRole
        _syncStatus.value = _syncStatus.value.copy(activePhoneRole = cleanRole)
        triggerImmediateSync(context)
    }

    fun notifyOrdersReset(context: Context) {
        localResetEpochMs = System.currentTimeMillis()
        triggerImmediateSync(context, forceResetServer = true)
    }

    fun startBackgroundMultiPhoneSync(context: Context, activeRoleHint: String = "") {
        if (isRobolectricUnitTest) return
        val appContext = context.applicationContext
        if (activeRoleHint.isNotBlank()) {
            _syncStatus.value = _syncStatus.value.copy(activePhoneRole = activeRoleHint)
        }
        synchronized(this) {
            if (isLoopStarted) return
            isLoopStarted = true
        }
        syncScope.launch {
            while (true) {
                runCatching {
                    performSingleSyncCycle(appContext, forceResetServer = false)
                }
                delay(1800L)
            }
        }
    }

    fun triggerImmediateSync(context: Context, forceResetServer: Boolean = false) {
        if (isRobolectricUnitTest) return
        val appContext = context.applicationContext
        syncScope.launch {
            runCatching {
                performSingleSyncCycle(appContext, forceResetServer = forceResetServer)
            }
        }
    }

    fun statusRank(status: OrderStatus): Int = when (status) {
        OrderStatus.SEARCHING_RIDER -> 1
        OrderStatus.RIDER_ASSIGNED -> 2
        OrderStatus.ARRIVED_AT_PICKUP -> 3
        OrderStatus.PICKED_UP -> 4
        OrderStatus.IN_TRANSIT -> 5
        OrderStatus.DELIVERED -> 6
        OrderStatus.CANCELLED -> 7
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMutableOrdersFlow(repo: SharedOrderRepository): MutableStateFlow<List<CustomerOrder>>? {
        return runCatching {
            val field = SharedOrderRepository::class.java.getDeclaredField("_orders")
            field.isAccessible = true
            field.get(repo) as? MutableStateFlow<List<CustomerOrder>>
        }.getOrNull()
    }

    private suspend fun performSingleSyncCycle(context: Context, forceResetServer: Boolean) = withContext(Dispatchers.IO) {
        val repo = SharedOrderRepository.getInstance(context)
        AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
        RiderRegistrationApprovalManager.ensureLoaded(context)
        val localOrders = repo.orders.value.filterNot { it.orderId in DEMO_ORDER_IDS }
        val pickupProofs = OrderProofRegistry.pickupProofsByOrder.value
        val deliveryProofs = OrderProofRegistry.deliveryProofsByOrder.value
        val deviceId = getOrInitDeviceId(context)
        val activeRole = _lockedPhoneRole.value.ifBlank { "UNIFIED" }
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        val requestPayload = JSONObject().apply {
            put("deviceId", deviceId)
            put("role", activeRole)
            put("deviceModel", deviceModel)
            if (forceResetServer) {
                put("resetEpochMs", localResetEpochMs.coerceAtLeast(System.currentTimeMillis()))
            }
            val ordersArray = JSONArray()
            localOrders.forEach { order ->
                ordersArray.put(serializeOrderToJson(order, pickupProofs[order.orderId], deliveryProofs[order.orderId]))
            }
            put("orders", ordersArray)
            put("riderRegistrations", RiderRegistrationApprovalManager.exportAllToJsonArray())
        }

        var responseJson: JSONObject? = null
        for (endpoint in LIVE_SYNC_ENDPOINTS) {
            val attempt = runCatching {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 3500
                    readTimeout = 3500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestPayload.toString())
                    writer.flush()
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    JSONObject(text)
                } else {
                    null
                }
            }.getOrNull()
            if (attempt != null) {
                responseJson = attempt
                break
            }
        }

        val root = responseJson ?: return@withContext
        val serverResetEpochMs = root.optLong("resetEpochMs", 0L)
        val mutableOrdersFlow = getMutableOrdersFlow(repo)

        if (serverResetEpochMs > localResetEpochMs && localResetEpochMs > 0L) {
            localResetEpochMs = serverResetEpochMs
            mutableOrdersFlow?.value = emptyList()
        } else if (localResetEpochMs == 0L && serverResetEpochMs > 0L) {
            localResetEpochMs = serverResetEpochMs
        }

        RiderRegistrationApprovalManager.mergeFromServerJsonArray(
            context,
            root.optJSONArray("riderRegistrations")
        )

        val remoteOrdersArr = root.optJSONArray("orders") ?: JSONArray()
        if (mutableOrdersFlow != null && remoteOrdersArr.length() > 0) {
            val currentList = mutableOrdersFlow.value.filterNot { it.orderId in DEMO_ORDER_IDS }
            val currentById = currentList.associateBy { it.orderId }.toMutableMap()
            var listChanged = false

            for (i in 0 until remoteOrdersArr.length()) {
                val itemObj = remoteOrdersArr.optJSONObject(i) ?: continue
                val orderId = itemObj.optString("orderId", "").trim()
                if (orderId.isEmpty() || orderId in DEMO_ORDER_IDS) continue

                // Restore PickupProof & DeliveryProof into OrderProofRegistry across all 3 phones
                itemObj.optJSONObject("pickupProof")?.let { ppObj ->
                    val proof = parsePickupProof(ppObj, orderId)
                    if (proof != null && OrderProofRegistry.getPickupProof(orderId) == null) {
                        OrderProofRegistry.savePickupProof(proof)
                    }
                }
                itemObj.optJSONObject("deliveryProof")?.let { dpObj ->
                    val proof = parseDeliveryProof(dpObj, orderId)
                    if (proof != null && OrderProofRegistry.getDeliveryProof(orderId) == null) {
                        OrderProofRegistry.saveDeliveryProof(proof)
                    }
                }

                val remoteOrder = deserializeOrderFromJson(itemObj) ?: continue
                val existingLocal = currentById[orderId]

                if (existingLocal == null) {
                    currentById[orderId] = remoteOrder
                    listChanged = true
                    val effectiveRole = _lockedPhoneRole.value
                    if ((effectiveRole == "RIDER" || activeRole == "RIDER") &&
                        remoteOrder.orderStatus == OrderStatus.SEARCHING_RIDER &&
                        OrderAlertSoundManager.shouldAlertForOrder(orderId) &&
                        alertedOrderIds.add(orderId)
                    ) {
                        runCatching {
                            OrderAlertSoundManager.startLoudOrderAlert(context, orderId)
                        }
                    }
                } else {
                    val localRank = statusRank(existingLocal.orderStatus)
                    val remoteRank = statusRank(remoteOrder.orderStatus)
                    val riderJustAssigned = existingLocal.assignedRiderId.isNullOrBlank() && !remoteOrder.assignedRiderId.isNullOrBlank()
                    val noteChanged = existingLocal.latestRiderQuickNote != remoteOrder.latestRiderQuickNote &&
                        !remoteOrder.latestRiderQuickNote.isNullOrBlank()

                    if (remoteRank > localRank || riderJustAssigned || noteChanged) {
                        val mergedOrder = existingLocal.copy(
                            orderStatus = if (remoteRank >= localRank) remoteOrder.orderStatus else existingLocal.orderStatus,
                            assignedRiderId = remoteOrder.assignedRiderId ?: existingLocal.assignedRiderId,
                            assignedRiderName = remoteOrder.assignedRiderName ?: existingLocal.assignedRiderName,
                            assignedRiderPhone = remoteOrder.assignedRiderPhone ?: existingLocal.assignedRiderPhone,
                            assignedRiderVehicle = remoteOrder.assignedRiderVehicle ?: existingLocal.assignedRiderVehicle,
                            latestRiderQuickNote = remoteOrder.latestRiderQuickNote ?: existingLocal.latestRiderQuickNote,
                            pickupOtp = remoteOrder.pickupOtp.ifBlank { existingLocal.pickupOtp },
                            deliveryOtp = remoteOrder.deliveryOtp.ifBlank { existingLocal.deliveryOtp }
                        )
                        currentById[orderId] = mergedOrder
                        listChanged = true
                    }
                }
            }

            if (listChanged) {
                mutableOrdersFlow.value = currentById.values.toList()
            }
        }

        // Always stop order alert immediately if no SEARCHING_RIDER order remains locally
        if (repo.orders.value.none { it.orderStatus == OrderStatus.SEARCHING_RIDER }) {
            runCatching { OrderAlertSoundManager.stopOrderAlert() }
        }

        val connectedDevicesArr = root.optJSONArray("connectedDevices") ?: JSONArray()
        val rolesList = mutableListOf<String>()
        for (i in 0 until connectedDevicesArr.length()) {
            val dev = connectedDevicesArr.optJSONObject(i) ?: continue
            val r = dev.optString("role", "PHONE")
            rolesList.add(r)
        }
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _syncStatus.value = LivePhoneSyncStatus(
            isConnected = true,
            activePhoneRole = activeRole,
            syncedOrdersCount = repo.orders.value.size,
            connectedPhonesCount = connectedDevicesArr.length().coerceAtLeast(1),
            connectedRolesSummary = if (rolesList.isEmpty()) activeRole else rolesList.distinct().joinToString(" + "),
            lastSyncTimeLabel = timeStr
        )
    }

    private fun serializeOrderToJson(
        order: CustomerOrder,
        pickupProof: PickupProof?,
        deliveryProof: DeliveryProof?
    ): JSONObject {
        return JSONObject().apply {
            put("orderId", order.orderId)
            put("customerId", order.customerId)
            put("customerName", order.customerName)
            put("customerPhone", order.customerPhone)
            put("pickupFullAddress", order.pickupAddress.fullAddress)
            put("pickupLandmark", order.pickupAddress.landmark)
            put("pickupContactName", order.pickupAddress.contactName)
            put("pickupContactPhone", order.pickupAddress.contactPhone)
            put("pickupLat", order.pickupAddress.lat)
            put("pickupLng", order.pickupAddress.lng)
            put("dropFullAddress", order.dropAddress.fullAddress)
            put("dropLandmark", order.dropAddress.landmark)
            put("dropContactName", order.dropAddress.contactName)
            put("dropContactPhone", order.dropAddress.contactPhone)
            put("dropLat", order.dropAddress.lat)
            put("dropLng", order.dropAddress.lng)
            put("packageType", order.packageDetails.packageType.name)
            put("weightKg", order.packageDetails.weightKg)
            put("description", order.packageDetails.description)
            put("vehicleType", order.vehicleType.name)
            put("orderStatus", order.orderStatus.name)
            put("statusRank", statusRank(order.orderStatus))
            put("totalAmount", order.totalAmount)
            put("platformCommission", order.platformCommission)
            put("riderEarning", order.riderEarning)
            put("distanceKm", order.distanceKm)
            put("pickupOtp", order.pickupOtp)
            put("deliveryOtp", order.deliveryOtp)
            put("assignedRiderId", order.assignedRiderId ?: "")
            put("assignedRiderName", order.assignedRiderName ?: "")
            put("assignedRiderPhone", order.assignedRiderPhone ?: "")
            put("assignedRiderVehicle", order.assignedRiderVehicle ?: "")
            put("latestRiderQuickNote", order.latestRiderQuickNote ?: "")
            put("paymentMethod", order.paymentMethod.name)
            put("paymentStatus", order.paymentStatus.name)
            put("createdAt", order.createdAt)
            if (pickupProof != null) {
                put("pickupProof", JSONObject().apply {
                    put("orderId", pickupProof.orderId)
                    put("riderId", pickupProof.riderId)
                    put("riderName", pickupProof.riderName)
                    put("packagePhotoUrl", pickupProof.packagePhotoUrl)
                    put("packagePhotoSummary", pickupProof.packagePhotoSummary)
                    put("senderName", pickupProof.senderName)
                    put("senderSignatureSvg", pickupProof.senderSignatureSvg)
                    put("verifiedOtp", pickupProof.verifiedOtp)
                    put("latitude", pickupProof.latitude)
                    put("longitude", pickupProof.longitude)
                    put("capturedAt", pickupProof.capturedAt)
                })
            }
            if (deliveryProof != null) {
                put("deliveryProof", JSONObject().apply {
                    put("orderId", deliveryProof.orderId)
                    put("riderId", deliveryProof.riderId)
                    put("riderName", deliveryProof.riderName)
                    put("deliveryPhotoUrl", deliveryProof.deliveryPhotoUrl)
                    put("deliveryPhotoSummary", deliveryProof.deliveryPhotoSummary)
                    put("recipientName", deliveryProof.recipientName)
                    put("recipientSignatureSvg", deliveryProof.recipientSignatureSvg)
                    put("verifiedOtp", deliveryProof.verifiedOtp)
                    put("codAmountCollectedInr", deliveryProof.codAmountCollectedInr)
                    put("latitude", deliveryProof.latitude)
                    put("longitude", deliveryProof.longitude)
                    put("capturedAt", deliveryProof.capturedAt)
                })
            }
        }
    }

    private fun deserializeOrderFromJson(obj: JSONObject): CustomerOrder? = runCatching {
        val pickup = AddressDetails(
            fullAddress = obj.optString("pickupFullAddress", "Indiranagar"),
            landmark = obj.optString("pickupLandmark", ""),
            contactName = obj.optString("pickupContactName", "Sender"),
            contactPhone = obj.optString("pickupContactPhone", "9876543210"),
            lat = obj.optDouble("pickupLat", 12.9784),
            lng = obj.optDouble("pickupLng", 77.6408)
        )
        val drop = AddressDetails(
            fullAddress = obj.optString("dropFullAddress", "Koramangala"),
            landmark = obj.optString("dropLandmark", ""),
            contactName = obj.optString("dropContactName", "Receiver"),
            contactPhone = obj.optString("dropContactPhone", "9845012345"),
            lat = obj.optDouble("dropLat", 12.9352),
            lng = obj.optDouble("dropLng", 77.6245)
        )
        val pkgType = runCatching { PackageType.valueOf(obj.optString("packageType", "DOCUMENTS")) }
            .getOrDefault(PackageType.DOCUMENTS)
        val pkg = PackageDetails(
            packageType = pkgType,
            weightKg = obj.optString("weightKg", "1.5"),
            description = obj.optString("description", "Parcel")
        )
        val vehicle = runCatching { VehicleType.valueOf(obj.optString("vehicleType", "BIKE")) }
            .getOrDefault(VehicleType.BIKE)
        val status = runCatching { OrderStatus.valueOf(obj.optString("orderStatus", "SEARCHING_RIDER")) }
            .getOrDefault(OrderStatus.SEARCHING_RIDER)
        val payMethod = runCatching { CustomerPaymentMethod.valueOf(obj.optString("paymentMethod", "CASH_ON_DELIVERY")) }
            .getOrDefault(CustomerPaymentMethod.CASH_ON_DELIVERY)
        val payStatus = runCatching { PaymentVerificationStatus.valueOf(obj.optString("paymentStatus", "UNPAID_CASH_DUE")) }
            .getOrDefault(PaymentVerificationStatus.UNPAID_CASH_DUE)

        val assignedRiderId = obj.optString("assignedRiderId", "").ifBlank { null }
        val assignedRiderName = obj.optString("assignedRiderName", "").ifBlank { null }
        val assignedRiderPhone = obj.optString("assignedRiderPhone", "").ifBlank { null }
        val assignedRiderVehicle = obj.optString("assignedRiderVehicle", "").ifBlank { null }
        val latestNote = obj.optString("latestRiderQuickNote", "").ifBlank { null }

        CustomerOrder(
            orderId = obj.optString("orderId", "QD-1001"),
            customerId = obj.optString("customerId", "CUST-1001"),
            customerName = obj.optString("customerName", "Customer"),
            customerPhone = obj.optString("customerPhone", "9876543210"),
            pickupAddress = pickup,
            dropAddress = drop,
            packageDetails = pkg,
            vehicleType = vehicle,
            orderStatus = status,
            totalAmount = obj.optString("totalAmount", "₹100"),
            platformCommission = obj.optString("platformCommission", "₹20"),
            riderEarning = obj.optString("riderEarning", "₹80"),
            distanceKm = obj.optDouble("distanceKm", 4.5),
            pickupOtp = obj.optString("pickupOtp", "4821"),
            deliveryOtp = obj.optString("deliveryOtp", "9314"),
            assignedRiderId = assignedRiderId,
            assignedRiderName = assignedRiderName,
            assignedRiderPhone = assignedRiderPhone,
            assignedRiderVehicle = assignedRiderVehicle,
            latestRiderQuickNote = latestNote,
            paymentMethod = payMethod,
            paymentStatus = payStatus,
            createdAt = obj.optString("createdAt", "Live Sync")
        )
    }.getOrNull()

    private fun parsePickupProof(obj: JSONObject, fallbackOrderId: String): PickupProof? = runCatching {
        PickupProof(
            orderId = obj.optString("orderId", fallbackOrderId),
            riderId = obj.optString("riderId", "RIDER-101"),
            riderName = obj.optString("riderName", "Vikram Rathore"),
            packagePhotoUrl = obj.optString("packagePhotoUrl", ""),
            packagePhotoSummary = obj.optString("packagePhotoSummary", "Live Camera Package Photo Verified"),
            senderName = obj.optString("senderName", "Sender"),
            senderSignatureSvg = obj.optString("senderSignatureSvg", ""),
            verifiedOtp = obj.optString("verifiedOtp", ""),
            latitude = obj.optDouble("latitude", 12.9716),
            longitude = obj.optDouble("longitude", 77.5946),
            capturedAt = obj.optString("capturedAt", "")
        )
    }.getOrNull()

    private fun parseDeliveryProof(obj: JSONObject, fallbackOrderId: String): DeliveryProof? = runCatching {
        DeliveryProof(
            orderId = obj.optString("orderId", fallbackOrderId),
            riderId = obj.optString("riderId", "RIDER-101"),
            riderName = obj.optString("riderName", "Vikram Rathore"),
            deliveryPhotoUrl = obj.optString("deliveryPhotoUrl", ""),
            deliveryPhotoSummary = obj.optString("deliveryPhotoSummary", "Live Camera Drop Photo Verified"),
            recipientName = obj.optString("recipientName", "Receiver"),
            recipientSignatureSvg = obj.optString("recipientSignatureSvg", ""),
            verifiedOtp = obj.optString("verifiedOtp", ""),
            codAmountCollectedInr = obj.optDouble("codAmountCollectedInr", 0.0),
            latitude = obj.optDouble("latitude", 12.9352),
            longitude = obj.optDouble("longitude", 77.6245),
            capturedAt = obj.optString("capturedAt", "")
        )
    }.getOrNull()
}
