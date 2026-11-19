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
import com.example.security.CryptoSecurityEngine
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LivePhoneSyncStatus(
    val isConnected: Boolean = false,
    val activePhoneRole: String = "UNIFIED",
    val syncedOrdersCount: Int = 0,
    val connectedPhonesCount: Int = 1,
    val connectedRolesSummary: String = "Ready for 3-Phone Live Sync",
    val lastSyncTimeLabel: String = "Connecting...",
    val activeEndpointHost: String = "",
    val lastLatencyMs: Long = 0L,
    val blockedFakeOrdersCount: Int = 0,
    val rateLimitedRequestsCount: Int = 0,
    val lastSyncError: String = ""
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
        QuickDeliveryBrandLinks.LIVE_SYNC_API_URL,
        "https://quickdeliveryapp-1wll.onrender.com/api/live-sync",
        "https://ais-dev-l6kwxrdc54lqesfozowtv2-809441526823.asia-southeast1.run.app/api/live-sync",
        "https://ais-pre-l6kwxrdc54lqesfozowtv2-809441526823.asia-southeast1.run.app/api/live-sync"
    )

    @Volatile
    private var lastWorkingEndpoint: String? = null

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val alertedOrderIds = ConcurrentHashMap.newKeySet<String>()
    private var isLoopStarted = false

    @Volatile
    private var pendingImmediateSync = false

    @Volatile
    private var pendingForceResetServer = false

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
                    val useReset = pendingForceResetServer
                    pendingForceResetServer = false
                    pendingImmediateSync = false
                    performSingleSyncCycle(appContext, forceResetServer = useReset)
                }
                if (pendingImmediateSync) {
                    delay(200L)
                } else {
                    delay(1800L)
                }
            }
        }
    }

    fun triggerImmediateSync(context: Context, forceResetServer: Boolean = false) {
        if (isRobolectricUnitTest) return
        val appContext = context.applicationContext
        pendingImmediateSync = true
        if (forceResetServer) {
            pendingForceResetServer = true
        }
        syncScope.launch {
            while (pendingImmediateSync) {
                val forceNow = pendingForceResetServer
                pendingImmediateSync = false
                pendingForceResetServer = false
                runCatching {
                    performSingleSyncCycle(appContext, forceResetServer = forceNow)
                }
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

    fun ensureValidLocalFareSeal(order: CustomerOrder): CustomerOrder {
        val parseAmt: (String) -> Double = { raw ->
            raw.substringBefore("(").replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        }
        val totalInr = parseAmt(order.totalAmount)
        val commInr = parseAmt(order.platformCommission)
        val riderInr = parseAmt(order.riderEarning)
        val alreadyValid = order.fareSeal.isNotBlank() && CryptoSecurityEngine.verifyOrderFareSeal(
            orderId = order.orderId,
            distanceKm = order.distanceKm,
            totalCustomerPrice = totalInr,
            platformCommission = commInr,
            riderEarnings = riderInr,
            providedSeal = order.fareSeal
        )
        if (alreadyValid) return order
        val freshSeal = CryptoSecurityEngine.computeOrderFareSeal(
            orderId = order.orderId,
            distanceKm = order.distanceKm,
            totalCustomerPrice = totalInr,
            platformCommission = commInr,
            riderEarnings = riderInr
        )
        return order.copy(fareSeal = freshSeal)
    }

    fun ensureOrderSealedForRepository(repo: SharedOrderRepository, orderId: String) {
        val mutableFlow = getMutableOrdersFlow(repo) ?: return
        val currentList = mutableFlow.value
        var modified = false
        val updated = currentList.map { ord ->
            if (ord.orderId == orderId) {
                val sealed = ensureValidLocalFareSeal(ord)
                if (sealed.fareSeal != ord.fareSeal) modified = true
                sealed
            } else {
                ord
            }
        }
        if (modified) {
            mutableFlow.value = updated
        }
    }

    private suspend fun performSingleSyncCycle(context: Context, forceResetServer: Boolean) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
        val repo = SharedOrderRepository.getInstance(context)
        AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
        RiderRegistrationApprovalManager.ensureLoaded(context)
        QuickDeliveryOperationsManager.ensureLoaded(context)
        com.example.ui.components.OrderLiveChatManager.ensureLoaded(context)
        val localOrders = repo.orders.value.filterNot { it.orderId in DEMO_ORDER_IDS }
        val pickupProofs = OrderProofRegistry.pickupProofsByOrder.value
        val deliveryProofs = OrderProofRegistry.deliveryProofsByOrder.value
        val deviceId = getOrInitDeviceId(context)
        val activeRole = _lockedPhoneRole.value.ifBlank { "UNIFIED" }
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        com.example.ui.components.FcmOrderNotificationManager.ensureInitialized(context)
        val fcmTokenValue = com.example.ui.components.FcmOrderNotificationManager.fcmToken.value

        // If running on Rider phone, capture latest Rider coordinates onto active assigned orders
        if (activeRole == "RIDER") {
            val activeRider = repo.riders.value.firstOrNull()
            if (activeRider != null && (activeRider.currentLat != 0.0 || activeRider.currentLng != 0.0)) {
                localOrders.filter {
                    it.orderStatus != OrderStatus.SEARCHING_RIDER &&
                        it.orderStatus != OrderStatus.DELIVERED &&
                        it.orderStatus != OrderStatus.CANCELLED
                }.forEach { activeOrder ->
                    QuickDeliveryOperationsManager.updateRiderCoordsForOrder(
                        context = context,
                        orderId = activeOrder.orderId,
                        lat = activeRider.currentLat,
                        lng = activeRider.currentLng
                    )
                }
            }
        }
        val activeRiderLat = repo.riders.value.firstOrNull()?.currentLat ?: 0.0
        val activeRiderLng = repo.riders.value.firstOrNull()?.currentLng ?: 0.0

        val requestPayload = JSONObject().apply {
            put("deviceId", deviceId)
            put("role", activeRole)
            put("deviceModel", deviceModel)
            if (fcmTokenValue.isNotBlank() && !fcmTokenValue.contains("connecting")) {
                put("fcmToken", fcmTokenValue)
            }
            if (activeRole == "RIDER" && (activeRiderLat != 0.0 || activeRiderLng != 0.0)) {
                put("deviceLat", activeRiderLat)
                put("deviceLng", activeRiderLng)
            }
            if (forceResetServer) {
                put("resetEpochMs", localResetEpochMs.coerceAtLeast(System.currentTimeMillis()))
            }
            val ordersArray = JSONArray()
            localOrders.forEach { order ->
                ordersArray.put(serializeOrderToJson(order, pickupProofs[order.orderId], deliveryProofs[order.orderId]))
            }
            put("orders", ordersArray)
            put("riderRegistrations", RiderRegistrationApprovalManager.exportAllToJsonArray())
            val opsJson = QuickDeliveryOperationsManager.exportOpsPayloadJson()
            put("promoCoupons", opsJson.optJSONArray("promoCoupons"))
            put("withdrawalRequests", opsJson.optJSONArray("withdrawalRequests"))
            put("supportTickets", opsJson.optJSONArray("supportTickets"))
            put("codSettlements", opsJson.optJSONArray("codSettlements"))
            put("disputeRecords", opsJson.optJSONArray("disputeRecords"))
            put("riderDocExpiries", opsJson.optJSONArray("riderDocExpiries"))
            opsJson.optJSONObject("remoteFeatureFlags")?.let { flagsObj ->
                put("remoteFeatureFlags", flagsObj)
            }
            opsJson.optJSONObject("waitTimerStartByOrder")?.let { wtObj ->
                put("waitTimerStartByOrder", wtObj)
            }
            opsJson.optJSONObject("riderLiveCoordsByOrder")?.let { rcObj ->
                put("riderLiveCoordsByOrder", rcObj)
            }
            opsJson.optJSONArray("orderTimelineEvents")?.let { tlArr ->
                put("orderTimelineEvents", tlArr)
            }
            opsJson.optJSONArray("unreachableByOrder")?.let { unArr ->
                put("unreachableByOrder", unArr)
            }
            opsJson.optJSONArray("b2bAccounts")?.let { b2bArr ->
                put("b2bAccounts", b2bArr)
            }
            com.example.ui.components.RiderFeedbackManager.ensureLoaded(context)
            put("orderRatings", com.example.ui.components.RiderFeedbackManager.exportRatingsAndTipsToJsonArray())
            put("chatMessages", com.example.ui.components.OrderLiveChatManager.exportAllMessagesToJsonArray())
        }

        val orderedEndpoints = buildList {
            lastWorkingEndpoint?.let { add(it) }
            LIVE_SYNC_ENDPOINTS.forEach { if (it != lastWorkingEndpoint) add(it) }
        }
        val syncTs = System.currentTimeMillis()
        val syncSig = com.example.security.QuickDeliverySecurityGuard.computeSyncIntegritySignature(
            deviceId = deviceId,
            role = activeRole,
            timestampEpochMs = syncTs
        )

        var responseJson: JSONObject? = null
        var winningHost = ""
        var winningLatencyMs = 0L
        var lastErrMsg = ""
        val isCurrentlyDisconnected = !_syncStatus.value.isConnected
        for (endpoint in orderedEndpoints) {
            val startMs = System.currentTimeMillis()
            val isPrimaryProductionEndpoint = endpoint.contains("quickdelivery.co.in", ignoreCase = true) ||
                endpoint.contains("onrender.com", ignoreCase = true)
            // Render Free Tier cold-starts can take 15-35s after idle; give primary endpoints generous timeout so orders never silently fail to sync!
            val epConnectTimeout = when {
                isPrimaryProductionEndpoint && isCurrentlyDisconnected -> 15_000
                isPrimaryProductionEndpoint -> 10_000
                else -> 2_500
            }
            val epReadTimeout = when {
                isPrimaryProductionEndpoint && isCurrentlyDisconnected -> 28_000
                isPrimaryProductionEndpoint -> 15_000
                else -> 3_000
            }
            val attempt = runCatching {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = epConnectTimeout
                    readTimeout = epReadTimeout
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("X-QD-App-Client", "QuickDelivery-Android-v6.3.5")
                    setRequestProperty("X-QD-App-Role", activeRole)
                    setRequestProperty("X-QD-Sync-Ts", syncTs.toString())
                    setRequestProperty("X-QD-Sync-Sig", syncSig)
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestPayload.toString())
                    writer.flush()
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    val parsed = JSONObject(text)
                    if (parsed.has("orders") || parsed.optBoolean("ok", false)) parsed else null
                } else {
                    lastErrMsg = "HTTP $code from ${url.host}"
                    null
                }
            }.onFailure { err ->
                lastErrMsg = err.message?.take(80) ?: "Network unreachable"
            }.getOrNull()
            if (attempt != null) {
                responseJson = attempt
                lastWorkingEndpoint = endpoint
                winningHost = runCatching { URL(endpoint).host }.getOrDefault(endpoint)
                winningLatencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                break
            } else if (lastWorkingEndpoint == endpoint) {
                lastWorkingEndpoint = null
            }
        }

        val root = responseJson ?: run {
            _syncStatus.value = _syncStatus.value.copy(
                isConnected = false,
                lastSyncError = lastErrMsg.ifBlank { "Server unreachable — check internet" }
            )
            return@withLock
        }
        val serverResetEpochMs = root.optLong("resetEpochMs", 0L)
        val mutableOrdersFlow = getMutableOrdersFlow(repo)

        if (serverResetEpochMs > localResetEpochMs && localResetEpochMs > 0L) {
            localResetEpochMs = serverResetEpochMs
            mutableOrdersFlow?.value = emptyList()
            com.example.ui.components.OrderLiveChatManager.clearAllOnReset(context)
        } else if (localResetEpochMs == 0L && serverResetEpochMs > 0L) {
            localResetEpochMs = serverResetEpochMs
        }

        RiderRegistrationApprovalManager.mergeFromServerJsonArray(
            context,
            root.optJSONArray("riderRegistrations")
        )
        QuickDeliveryOperationsManager.mergeFromServerPayload(context, root)
        com.example.ui.components.RiderFeedbackManager.mergeRatingsAndTipsFromJsonArray(
            context,
            root.optJSONArray("orderRatings")
        )
        val newlySyncedMessages = com.example.ui.components.OrderLiveChatManager.mergeFromServerJsonArray(
            context,
            root.optJSONArray("chatMessages")
        )
        newlySyncedMessages.forEach { msg ->
            val fromOppositeRole = !msg.senderRole.equals(activeRole, ignoreCase = true)
            if (fromOppositeRole && !msg.messageId.startsWith("msg_seed_")) {
                com.example.ui.components.FcmOrderNotificationManager.dispatchIncomingChatNotification(context, msg)
            }
        }

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
                val myRiderId = repo.riders.value.firstOrNull()?.riderId

                if (existingLocal == null) {
                    currentById[orderId] = remoteOrder
                    listChanged = true
                    val effectiveRole = _lockedPhoneRole.value
                    val alreadyRejectedByMe = myRiderId != null && myRiderId in remoteOrder.rejectedByRiderIds
                    if ((effectiveRole == "RIDER" || activeRole == "RIDER") &&
                        remoteOrder.orderStatus == OrderStatus.SEARCHING_RIDER &&
                        !alreadyRejectedByMe &&
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
                    val mergedRejectedIds = (existingLocal.rejectedByRiderIds + remoteOrder.rejectedByRiderIds).distinct()
                    val rejectionExpandedFromRemote = remoteOrder.rejectedByRiderIds.size > existingLocal.rejectedByRiderIds.size ||
                        mergedRejectedIds.size > existingLocal.rejectedByRiderIds.size
                    val isRiderRejectionRollback = remoteOrder.orderStatus == OrderStatus.SEARCHING_RIDER &&
                        rejectionExpandedFromRemote &&
                        (existingLocal.orderStatus == OrderStatus.SEARCHING_RIDER ||
                            existingLocal.orderStatus == OrderStatus.RIDER_ASSIGNED ||
                            existingLocal.orderStatus == OrderStatus.ARRIVED_AT_PICKUP)
                    val staleAssignForRejectedRider = existingLocal.orderStatus == OrderStatus.SEARCHING_RIDER &&
                        remoteOrder.orderStatus == OrderStatus.RIDER_ASSIGNED &&
                        !remoteOrder.assignedRiderId.isNullOrBlank() &&
                        remoteOrder.assignedRiderId in mergedRejectedIds

                    if (isRiderRejectionRollback) {
                        val mergedRollback = ensureValidLocalFareSeal(
                            existingLocal.copy(
                                orderStatus = OrderStatus.SEARCHING_RIDER,
                                assignedRiderId = null,
                                assignedRiderName = null,
                                assignedRiderPhone = null,
                                assignedRiderVehicle = null,
                                rejectedByRiderIds = mergedRejectedIds,
                                latestRiderQuickNote = remoteOrder.latestRiderQuickNote ?: existingLocal.latestRiderQuickNote,
                                pickupOtp = remoteOrder.pickupOtp.ifBlank { existingLocal.pickupOtp },
                                deliveryOtp = remoteOrder.deliveryOtp.ifBlank { existingLocal.deliveryOtp }
                            )
                        )
                        currentById[orderId] = mergedRollback
                        QuickDeliveryOperationsManager.clearWaitTimerForOrder(context, orderId)
                        listChanged = true
                    } else if (!staleAssignForRejectedRider) {
                        val riderJustAssigned = existingLocal.assignedRiderId.isNullOrBlank() && !remoteOrder.assignedRiderId.isNullOrBlank()
                        val noteChanged = existingLocal.latestRiderQuickNote != remoteOrder.latestRiderQuickNote &&
                            !remoteOrder.latestRiderQuickNote.isNullOrBlank()
                        val rejectListGrew = mergedRejectedIds.size > existingLocal.rejectedByRiderIds.size

                        if (remoteRank > localRank || riderJustAssigned || noteChanged || rejectListGrew) {
                            val mergedOrder = ensureValidLocalFareSeal(
                                existingLocal.copy(
                                    orderStatus = if (remoteRank >= localRank) remoteOrder.orderStatus else existingLocal.orderStatus,
                                    assignedRiderId = remoteOrder.assignedRiderId ?: existingLocal.assignedRiderId,
                                    assignedRiderName = remoteOrder.assignedRiderName ?: existingLocal.assignedRiderName,
                                    assignedRiderPhone = remoteOrder.assignedRiderPhone ?: existingLocal.assignedRiderPhone,
                                    assignedRiderVehicle = remoteOrder.assignedRiderVehicle ?: existingLocal.assignedRiderVehicle,
                                    rejectedByRiderIds = mergedRejectedIds,
                                    latestRiderQuickNote = remoteOrder.latestRiderQuickNote ?: existingLocal.latestRiderQuickNote,
                                    pickupOtp = remoteOrder.pickupOtp.ifBlank { existingLocal.pickupOtp },
                                    deliveryOtp = remoteOrder.deliveryOtp.ifBlank { existingLocal.deliveryOtp }
                                )
                            )
                            currentById[orderId] = mergedOrder
                            listChanged = true
                        } else {
                            val sealedLocal = ensureValidLocalFareSeal(existingLocal)
                            if (sealedLocal.fareSeal != existingLocal.fareSeal) {
                                currentById[orderId] = sealedLocal
                                listChanged = true
                            }
                        }
                    }
                }
            }

            if (listChanged) {
                val remoteIndexById = buildMap {
                    for (idx in 0 until remoteOrdersArr.length()) {
                        val oid = remoteOrdersArr.optJSONObject(idx)?.optString("orderId", "")?.trim().orEmpty()
                        if (oid.isNotEmpty()) put(oid, idx)
                    }
                }
                mutableOrdersFlow.value = currentById.values.sortedBy { ord ->
                    remoteIndexById[ord.orderId] ?: Int.MAX_VALUE
                }
                runCatching {
                    com.example.ui.components.FcmOrderNotificationManager.observeOrdersAndDispatchFcmNotifications(
                        context = context,
                        orders = mutableOrdersFlow.value
                    )
                }
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
        val secStatsObj = root.optJSONObject("securityStats")
        val blockedOrdersCount = secStatsObj?.optInt("blockedFakeOrders", 0) ?: 0
        val rateLimitedCount = secStatsObj?.optInt("rateLimitedRequests", 0) ?: 0
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _syncStatus.value = LivePhoneSyncStatus(
            isConnected = true,
            activePhoneRole = activeRole,
            syncedOrdersCount = repo.orders.value.size,
            connectedPhonesCount = connectedDevicesArr.length().coerceAtLeast(1),
            connectedRolesSummary = if (rolesList.isEmpty()) activeRole else rolesList.distinct().joinToString(" + "),
            lastSyncTimeLabel = timeStr,
            activeEndpointHost = winningHost,
            lastLatencyMs = winningLatencyMs,
            blockedFakeOrdersCount = blockedOrdersCount,
            rateLimitedRequestsCount = rateLimitedCount,
            lastSyncError = ""
        )
        }
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
            put("rejectedByRiderIds", JSONArray().apply { order.rejectedByRiderIds.forEach { put(it) } })
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
            fullAddress = obj.optString("pickupFullAddress", ""),
            landmark = obj.optString("pickupLandmark", ""),
            contactName = obj.optString("pickupContactName", "Sender"),
            contactPhone = obj.optString("pickupContactPhone", ""),
            lat = obj.optDouble("pickupLat", 0.0),
            lng = obj.optDouble("pickupLng", 0.0)
        )
        val drop = AddressDetails(
            fullAddress = obj.optString("dropFullAddress", ""),
            landmark = obj.optString("dropLandmark", ""),
            contactName = obj.optString("dropContactName", "Receiver"),
            contactPhone = obj.optString("dropContactPhone", ""),
            lat = obj.optDouble("dropLat", 0.0),
            lng = obj.optDouble("dropLng", 0.0)
        )
        val pkgType = runCatching { PackageType.valueOf(obj.optString("packageType", "DOCUMENTS")) }
            .getOrDefault(PackageType.DOCUMENTS)
        val pkg = PackageDetails(
            packageType = pkgType,
            weightKg = obj.optString("weightKg", "1.0"),
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
        val rejectedIds = mutableListOf<String>()
        obj.optJSONArray("rejectedByRiderIds")?.let { arr ->
            for (j in 0 until arr.length()) {
                val rid = arr.optString(j, "").trim()
                if (rid.isNotEmpty() && rid !in rejectedIds) rejectedIds.add(rid)
            }
        }

        ensureValidLocalFareSeal(
            CustomerOrder(
                orderId = obj.optString("orderId", "QD-1001"),
                customerId = obj.optString("customerId", "CUST-1001"),
                customerName = obj.optString("customerName", "Customer"),
                customerPhone = obj.optString("customerPhone", ""),
                pickupAddress = pickup,
                dropAddress = drop,
                packageDetails = pkg,
                vehicleType = vehicle,
                orderStatus = status,
                totalAmount = obj.optString("totalAmount", "₹0"),
                platformCommission = obj.optString("platformCommission", "₹0"),
                riderEarning = obj.optString("riderEarning", "₹0"),
                distanceKm = obj.optDouble("distanceKm", 0.0),
                pickupOtp = obj.optString("pickupOtp", ""),
                deliveryOtp = obj.optString("deliveryOtp", ""),
                assignedRiderId = assignedRiderId,
                assignedRiderName = assignedRiderName,
                assignedRiderPhone = assignedRiderPhone,
                assignedRiderVehicle = assignedRiderVehicle,
                rejectedByRiderIds = rejectedIds,
                latestRiderQuickNote = latestNote,
                paymentMethod = payMethod,
                paymentStatus = payStatus,
                createdAt = obj.optString("createdAt", "Live Sync")
            )
        )
    }.getOrNull()

    private fun parsePickupProof(obj: JSONObject, fallbackOrderId: String): PickupProof? = runCatching {
        PickupProof(
            orderId = obj.optString("orderId", fallbackOrderId),
            riderId = obj.optString("riderId", ""),
            riderName = obj.optString("riderName", "Rider Partner"),
            packagePhotoUrl = obj.optString("packagePhotoUrl", ""),
            packagePhotoSummary = obj.optString("packagePhotoSummary", "Pickup Verified"),
            senderName = obj.optString("senderName", "Sender"),
            senderSignatureSvg = obj.optString("senderSignatureSvg", ""),
            verifiedOtp = obj.optString("verifiedOtp", ""),
            latitude = obj.optDouble("latitude", 0.0),
            longitude = obj.optDouble("longitude", 0.0),
            capturedAt = obj.optString("capturedAt", "")
        )
    }.getOrNull()

    private fun parseDeliveryProof(obj: JSONObject, fallbackOrderId: String): DeliveryProof? = runCatching {
        DeliveryProof(
            orderId = obj.optString("orderId", fallbackOrderId),
            riderId = obj.optString("riderId", ""),
            riderName = obj.optString("riderName", "Rider Partner"),
            deliveryPhotoUrl = obj.optString("deliveryPhotoUrl", ""),
            deliveryPhotoSummary = obj.optString("deliveryPhotoSummary", "Delivery Verified"),
            recipientName = obj.optString("recipientName", "Receiver"),
            recipientSignatureSvg = obj.optString("recipientSignatureSvg", ""),
            verifiedOtp = obj.optString("verifiedOtp", ""),
            codAmountCollectedInr = obj.optDouble("codAmountCollectedInr", 0.0),
            latitude = obj.optDouble("latitude", 0.0),
            longitude = obj.optDouble("longitude", 0.0),
            capturedAt = obj.optString("capturedAt", "")
        )
    }.getOrNull()

    suspend fun sendSignUpEmailOtp(
        context: Context,
        email: String,
        role: String
    ): Result<String> = withContext(Dispatchers.IO) {
        postSignedAuthJson(
            context = context,
            apiPath = "/api/auth/send-email-otp",
            role = role,
            bodyJson = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("role", role.trim().uppercase())
                put("deviceId", getOrInitDeviceId(context))
            }
        )
    }

    suspend fun verifySignUpEmailOtp(
        context: Context,
        email: String,
        otp: String,
        role: String
    ): Result<String> = withContext(Dispatchers.IO) {
        postSignedAuthJson(
            context = context,
            apiPath = "/api/auth/verify-email-otp",
            role = role,
            bodyJson = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("otp", otp.filter(Char::isDigit).trim())
                put("role", role.trim().uppercase())
                put("deviceId", getOrInitDeviceId(context))
            }
        )
    }

    private fun postSignedAuthJson(
        context: Context,
        apiPath: String,
        role: String,
        bodyJson: JSONObject
    ): Result<String> {
        val deviceId = getOrInitDeviceId(context)
        val cleanRole = role.trim().uppercase().ifBlank { "CUSTOMER" }
        val syncTs = System.currentTimeMillis()
        val syncSig = com.example.security.QuickDeliverySecurityGuard.computeSyncIntegritySignature(
            deviceId = deviceId,
            role = cleanRole,
            timestampEpochMs = syncTs
        )
        val orderedEndpoints = buildList {
            lastWorkingEndpoint?.let { add(it.replace("/api/live-sync", apiPath)) }
            LIVE_SYNC_ENDPOINTS.forEach { ep ->
                val target = ep.replace("/api/live-sync", apiPath)
                if (target !in this) add(target)
            }
        }
        var lastErrorMsg = "Could not connect to Quick Delivery verification server."
        for (endpoint in orderedEndpoints) {
            val isPrimary = endpoint.contains("quickdelivery.co.in", ignoreCase = true) ||
                endpoint.contains("onrender.com", ignoreCase = true)
            val attempt = runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = if (isPrimary) 12_000 else 3_000
                    readTimeout = if (isPrimary) 20_000 else 4_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("X-QD-App-Client", "QuickDelivery-Android")
                    setRequestProperty("X-QD-App-Role", cleanRole)
                    setRequestProperty("X-QD-Sync-Ts", syncTs.toString())
                    setRequestProperty("X-QD-Sync-Sig", syncSig)
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(bodyJson.toString())
                    writer.flush()
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                val rawResp = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                conn.disconnect()
                val json = runCatching { JSONObject(rawResp) }.getOrNull()
                if (code in 200..299 && json?.optBoolean("ok", false) == true) {
                    lastWorkingEndpoint = endpoint.replace(apiPath, "/api/live-sync")
                    return Result.success(json.optString("message", "Success"))
                } else {
                    val serverErr = json?.optString("error")?.takeIf { it.isNotBlank() }
                        ?: "Server returned HTTP $code"
                    // If server responded cleanly with a 4xx/503/502 business/SMTP error, surface it immediately!
                    if (code in 400..599 && json != null) {
                        return Result.failure(IllegalStateException(serverErr))
                    }
                    lastErrorMsg = serverErr
                }
            }.onFailure { err ->
                lastErrorMsg = err.message ?: lastErrorMsg
            }
        }
        return Result.failure(IllegalStateException(lastErrorMsg))
    }
}
