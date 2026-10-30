package com.example.data.cloud

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.core.BuildConfig
import com.example.core.R
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.security.QuickDeliverySecurityGuard
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

/**
 * Ensures Customer (:app), Rider (:rider), and Admin (:admin) modules initialize against
 * the exact same provisioned Firebase project (gen-lang-client-0738232271) and named
 * Firestore database (ai-studio-android-quickdel-424e8f15-7bd2-46ff-a987-31f206ba3fe2).
 */
object QuickDeliveryFirebaseInitializer {
    const val EXPECTED_PROJECT_ID = "gen-lang-client-0738232271"
    const val EXPECTED_DATABASE_ID = "ai-studio-android-quickdel-424e8f15-7bd2-46ff-a987-31f206ba3fe2"

    fun ensureInitialized(context: Context): FirebaseApp? {
        val appContext = context.applicationContext
        val existing = runCatching { FirebaseApp.getApps(appContext) }.getOrDefault(emptyList())
        if (existing.isNotEmpty()) {
            return FirebaseApp.getInstance()
        }
        return runCatching {
            FirebaseApp.initializeApp(appContext) ?: run {
                val apiKeyId = appContext.resources.getIdentifier("google_api_key", "string", appContext.packageName)
                val apiKey = if (apiKeyId != 0) appContext.getString(apiKeyId) else ""
                if (apiKey.isNotBlank()) {
                    val options = FirebaseOptions.Builder()
                        .setProjectId(appContext.getString(R.string.firebase_project_id))
                        .setApplicationId(appContext.getString(R.string.firebase_application_id))
                        .setGcmSenderId(appContext.getString(R.string.firebase_gcm_sender_id))
                        .setStorageBucket(appContext.getString(R.string.firebase_storage_bucket))
                        .setApiKey(apiKey)
                        .build()
                    FirebaseApp.initializeApp(appContext, options)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    fun resolveWebClientId(context: Context): String {
        val defaultId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (defaultId != 0) {
            return context.getString(defaultId)
        }
        return context.getString(R.string.firebase_web_client_id)
    }

    fun getFirestore(context: Context): FirebaseFirestore {
        ensureInitialized(context)
        return FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    }
}

@IgnoreExtraProperties
data class CloudDeliveryOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderCode: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val vehicleType: String = "BIKE",
    val paymentMethod: String = "CASH_ON_DELIVERY",
    val paymentStatus: String = "CASH_COLLECT_ON_DELIVERY",
    val status: String = "PENDING_ASSIGNMENT",
    val totalAmountPaise: Long = 10000L,
    val companyCommissionPaise: Long = 2000L,
    val riderEarningPaise: Long = 8000L,
    val assignedRiderUid: String? = null,
    val assignedRiderId: String? = null,
    val pickupProofNote: String? = null,
    val deliveryProofNote: String? = null,
    val riderLat: Double? = null,
    val riderLng: Double? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isNew: Boolean = true): Map<String, Any> {
        return mapOf<String, Any?>(
            "userId" to userId,
            "orderCode" to orderCode,
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "pickupAddress" to pickupAddress,
            "dropAddress" to dropAddress,
            "vehicleType" to vehicleType,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to paymentStatus,
            "status" to status,
            "totalAmountPaise" to totalAmountPaise,
            "companyCommissionPaise" to companyCommissionPaise,
            "riderEarningPaise" to riderEarningPaise,
            "assignedRiderUid" to assignedRiderUid,
            "assignedRiderId" to assignedRiderId,
            "pickupProofNote" to pickupProofNote,
            "deliveryProofNote" to deliveryProofNote,
            "riderLat" to riderLat,
            "riderLng" to riderLng,
            "createdAt" to if (isNew) FieldValue.serverTimestamp() else (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value as Any }
    }

    companion object {
        const val MAX_PARCEL_WEIGHT_KG: Double = 20.0

        fun fromCustomerOrder(order: CustomerOrder, userId: String): CloudDeliveryOrder {
            val weightVal = order.packageDetails.weightKg.trim().replace("[^0-9.-]".toRegex(), "").toDoubleOrNull() ?: 1.0
            require(weightVal > 0.0 && weightVal <= MAX_PARCEL_WEIGHT_KG) {
                "Maximum allowed parcel weight is 20 kg. Parcels above 20 kg cannot be booked. (Requested: ${order.packageDetails.weightKg} kg)"
            }
            val totalInr = order.totalAmount.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 100.0
            val totalPaise = (totalInr * 100.0).roundToLong().coerceIn(1000L, 10000000L)
            val commissionPaise = (totalPaise * 20L) / 100L
            val riderPaise = totalPaise - commissionPaise
            val mappedVehicle = when (order.vehicleType.name) {
                "BIKE", "SCOOTER" -> "BIKE"
                "AUTO" -> "EV"
                else -> "MINI_TRUCK"
            }
            val mappedPaymentMethod = when (order.paymentMethod) {
                CustomerPaymentMethod.CASH_ON_DELIVERY -> "CASH_ON_DELIVERY"
                CustomerPaymentMethod.GATEWAY_UPI -> "GATEWAY_UPI"
                CustomerPaymentMethod.GATEWAY_CREDIT_CARD -> "CREDIT_CARD"
                CustomerPaymentMethod.GATEWAY_DEBIT_CARD -> "DEBIT_CARD"
                CustomerPaymentMethod.GATEWAY_NET_BANKING -> "NET_BANKING"
                CustomerPaymentMethod.DIRECT_RIDER_UPI -> "DIRECT_RIDER_UPI"
            }
            // Client never self-writes VERIFIED_PAID_TO_COMPANY; online payments start as PENDING_GATEWAY_VERIFICATION.
            val mappedPaymentStatus = when (order.paymentMethod) {
                CustomerPaymentMethod.CASH_ON_DELIVERY -> "CASH_COLLECT_ON_DELIVERY"
                CustomerPaymentMethod.DIRECT_RIDER_UPI -> "DIRECT_RIDER_UPI_PENDING"
                else -> "PENDING_GATEWAY_VERIFICATION"
            }
            val mappedStatus = when (order.orderStatus) {
                OrderStatus.SEARCHING_RIDER -> "PENDING_ASSIGNMENT"
                OrderStatus.RIDER_ASSIGNED -> "ASSIGNED"
                OrderStatus.ARRIVED_AT_PICKUP -> "ARRIVED_AT_PICKUP"
                OrderStatus.PICKED_UP -> "PICKED_UP"
                OrderStatus.IN_TRANSIT -> "IN_TRANSIT"
                OrderStatus.DELIVERED -> "DELIVERED"
                OrderStatus.CANCELLED -> "CANCELLED"
            }
            val safeDocId = QuickDeliverySecurityGuard.sanitizeIdentifier(order.orderId, 64).ifBlank {
                "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            }
            val safeOrderCode = QuickDeliverySecurityGuard.sanitizeIdentifier(order.orderId, 40).ifBlank { "QD-1001" }
            val safeCustomerName = QuickDeliverySecurityGuard.sanitizeText(order.customerName, 80).ifBlank { "Customer" }
            val safeCustomerPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(order.customerPhone).padStart(10, '9')
            val safePickup = QuickDeliverySecurityGuard.sanitizeText(order.pickupAddress.formattedAddress(), 300).ifBlank { "Pickup Address" }
            val safeDrop = QuickDeliverySecurityGuard.sanitizeText(order.dropAddress.formattedAddress(), 300).ifBlank { "Drop Address" }
            return CloudDeliveryOrder(
                id = safeDocId,
                userId = userId,
                orderCode = safeOrderCode,
                customerName = safeCustomerName,
                customerPhone = safeCustomerPhone,
                pickupAddress = safePickup,
                dropAddress = safeDrop,
                vehicleType = mappedVehicle,
                paymentMethod = mappedPaymentMethod,
                paymentStatus = mappedPaymentStatus,
                status = mappedStatus,
                totalAmountPaise = totalPaise,
                companyCommissionPaise = commissionPaise,
                riderEarningPaise = riderPaise
            )
        }
    }
}

@IgnoreExtraProperties
data class CloudRiderWallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val riderId: String = "RIDER-101",
    val riderName: String = "Vikram Singh",
    val balancePaise: Long = 148000L,
    val commissionDebtPaise: Long = 9000L,
    val maxCommissionDebtPaise: Long = 15000L,
    val workMode: String = "FULL_TIME",
    val shiftStatus: String = "ONLINE_ACTIVE",
    val batteryPercent: Long = 85L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isNew: Boolean = true): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "riderId" to riderId,
            "riderName" to riderName,
            "balancePaise" to balancePaise,
            "commissionDebtPaise" to commissionDebtPaise,
            "maxCommissionDebtPaise" to 15000L,
            "workMode" to workMode,
            "shiftStatus" to shiftStatus,
            "batteryPercent" to batteryPercent,
            "createdAt" to if (isNew) FieldValue.serverTimestamp() else (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class CloudCompanyLedgerDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val entryType: String = "CUSTOMER_GATEWAY_COLLECTION",
    val referenceId: String = "",
    val amountPaise: Long = 0L,
    val isCredit: Boolean = true,
    val settlementStatus: String = "PENDING_IN_GATEWAY",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "entryType" to entryType,
            "referenceId" to referenceId,
            "amountPaise" to amountPaise,
            "isCredit" to isCredit,
            "settlementStatus" to settlementStatus,
            "description" to description,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

/**
 * Firestore document model for `/delivery_orders/{orderId}/chat_messages/{messageId}`.
 * Enables real-time direct chat communication between the Customer and the Assigned Rider during delivery.
 */
@IgnoreExtraProperties
data class CloudOrderChatMessage(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val senderUid: String = "",
    val senderRole: String = "CUSTOMER",
    val senderName: String = "",
    val messageText: String = "",
    val deliveryStage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "orderId" to orderId,
            "senderUid" to senderUid,
            "senderRole" to senderRole,
            "senderName" to senderName.trim(),
            "messageText" to messageText.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!deliveryStage.isNullOrBlank()) {
            map["deliveryStage"] = deliveryStage
        }
        return map
    }
}

/**
 * Cloud Firestore Repository for cross-device Customer, Rider, and Admin synchronization.
 * Always initialized with R.string.firestore_database_id.
 */
class QuickDeliveryCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        QuickDeliveryFirebaseInitializer.getFirestore(context)
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun createOrder(order: CloudDeliveryOrder): Result<String> = runCatching {
        val uid = requireUserId()
        require(order.paymentStatus != "VERIFIED_PAID_TO_COMPANY") {
            "Client cannot mark an order paid directly; paymentStatus must be verified by a trusted backend."
        }
        require(order.totalAmountPaise == order.companyCommissionPaise + order.riderEarningPaise) {
            "Order fare split mismatch: totalAmountPaise must equal companyCommissionPaise + riderEarningPaise."
        }
        val cleanName = QuickDeliverySecurityGuard.sanitizeText(order.customerName, 80).ifBlank { "Customer" }
        val cleanPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(order.customerPhone).padStart(10, '9')
        val cleanPickup = QuickDeliverySecurityGuard.sanitizeText(order.pickupAddress, 300)
        val cleanDrop = QuickDeliverySecurityGuard.sanitizeText(order.dropAddress, 300)
        require(cleanPickup.length >= 3 && cleanDrop.length >= 3) {
            "Pickup and drop addresses must be at least 3 characters after sanitization."
        }
        QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
            actorKey = uid,
            pickupAddress = cleanPickup,
            dropAddress = cleanDrop,
            vehicleType = order.vehicleType
        ).getOrThrow()

        val orderId = QuickDeliverySecurityGuard.sanitizeIdentifier(order.id, 64).ifBlank {
            "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        }
        val safeOrderCode = QuickDeliverySecurityGuard.sanitizeIdentifier(order.orderCode, 40).ifBlank { "QD-1001" }
        val docRef = db.collection("delivery_orders").document(orderId)
        try {
            val payload = order.copy(
                id = orderId,
                userId = uid,
                orderCode = safeOrderCode,
                customerName = cleanName,
                customerPhone = cleanPhone,
                pickupAddress = cleanPickup,
                dropAddress = cleanDrop,
                pickupProofNote = order.pickupProofNote?.let { QuickDeliverySecurityGuard.sanitizeText(it, 300) },
                deliveryProofNote = order.deliveryProofNote?.let { QuickDeliverySecurityGuard.sanitizeText(it, 300) }
            ).toWriteMap(isNew = true)
            docRef.set(payload).await()
            orderId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeCustomerOrders(userId: String = requireUserId()): Flow<List<CloudDeliveryOrder>> {
        val path = "delivery_orders"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudDeliveryOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    /**
     * Real-time stream of unassigned orders (status == PENDING_ASSIGNMENT) for authorized Riders on other devices.
     */
    fun observeAvailableOrdersForRider(): Flow<List<CloudDeliveryOrder>> {
        val path = "delivery_orders"
        return db.collection(path)
            .whereEqualTo("status", "PENDING_ASSIGNMENT")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudDeliveryOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    /**
     * Real-time stream of orders assigned to a specific Rider UID across devices.
     */
    fun observeAssignedOrdersForRider(riderUid: String = requireUserId()): Flow<List<CloudDeliveryOrder>> {
        val path = "delivery_orders"
        return db.collection(path)
            .whereEqualTo("assignedRiderUid", riderUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudDeliveryOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getOrderById(orderId: String): Result<CloudDeliveryOrder?> = runCatching {
        val docRef = db.collection("delivery_orders").document(orderId)
        try {
            val snap = docRef.get().await()
            snap.toObject(
                CloudDeliveryOrder::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateOrderStatusByCustomer(
        orderId: String,
        newStatus: String,
        newPaymentStatus: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        require(newPaymentStatus != "VERIFIED_PAID_TO_COMPANY") {
            "Client callback cannot set VERIFIED_PAID_TO_COMPANY without server-side signature verification."
        }
        val docRef = db.collection("delivery_orders").document(orderId)
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newPaymentStatus != null) {
                updates["paymentStatus"] = newPaymentStatus
            }
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Atomically claims a PENDING_ASSIGNMENT order for an online rider while enforcing the ₹150 (15000 paise) debt cap.
     */
    suspend fun claimOrderByRider(
        orderId: String,
        riderId: String = "RIDER-101"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        QuickDeliverySecurityGuard.checkAndRecordEndpointCall(
            endpoint = QuickDeliverySecurityGuard.ENDPOINT_RIDER_CLAIM,
            actorKey = uid
        ).getOrThrow()
        val safeOrderId = QuickDeliverySecurityGuard.sanitizeIdentifier(orderId, 64)
        val safeRiderId = QuickDeliverySecurityGuard.sanitizeIdentifier(riderId, 64).ifBlank { "RIDER-101" }
        val walletRef = db.collection("rider_wallets").document(uid)
        val orderRef = db.collection("delivery_orders").document(safeOrderId)
        try {
            db.runTransaction { transaction ->
                val walletSnap = transaction.get(walletRef)
                val wallet = walletSnap.toObject(
                    CloudRiderWallet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Rider wallet must be initialized before claiming orders.")
                if (wallet.shiftStatus != "ONLINE_ACTIVE") {
                    throw IllegalStateException("Rider must be ONLINE_ACTIVE to claim an order.")
                }
                if (wallet.commissionDebtPaise > 15000L) {
                    throw IllegalStateException("Rider commission debt exceeds ₹150.00 cap.")
                }
                val orderSnap = transaction.get(orderRef)
                val currentStatus = orderSnap.getString("status")
                if (currentStatus != "PENDING_ASSIGNMENT") {
                    throw IllegalStateException("Order $orderId is no longer PENDING_ASSIGNMENT (current: $currentStatus).")
                }
                transaction.update(
                    orderRef,
                    mapOf(
                        "assignedRiderUid" to uid,
                        "assignedRiderId" to safeRiderId,
                        "status" to "ASSIGNED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            throw e
        }
    }

    /**
     * Atomically releases an ASSIGNED order before pickup back to PENDING_ASSIGNMENT so the next
     * eligible rider in the fair rotation can claim it.
     */
    suspend fun releaseAssignedOrderBeforePickupByRider(
        orderId: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val orderRef = db.collection("delivery_orders").document(orderId)
        try {
            db.runTransaction { transaction ->
                val orderSnap = transaction.get(orderRef)
                val currentStatus = orderSnap.getString("status")
                val currentRiderUid = orderSnap.getString("assignedRiderUid")
                if (currentStatus != "ASSIGNED" || currentRiderUid != uid) {
                    throw IllegalStateException("Only the assigned rider can release an order while in ASSIGNED state.")
                }
                transaction.update(
                    orderRef,
                    mapOf(
                        "assignedRiderUid" to FieldValue.delete(),
                        "assignedRiderId" to FieldValue.delete(),
                        "status" to "PENDING_ASSIGNMENT",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            throw e
        }
    }

    suspend fun updateRiderProofAndGps(
        orderId: String,
        newStatus: String,
        pickupProofNote: String? = null,
        deliveryProofNote: String? = null,
        riderLat: Double? = null,
        riderLng: Double? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("delivery_orders").document(orderId)
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (pickupProofNote != null) updates["pickupProofNote"] = pickupProofNote
            if (deliveryProofNote != null) updates["deliveryProofNote"] = deliveryProofNote
            if (riderLat != null) updates["riderLat"] = riderLat
            if (riderLng != null) updates["riderLng"] = riderLng
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun upsertRiderWallet(wallet: CloudRiderWallet): Result<String> = runCatching {
        val uid = requireUserId()
        require(wallet.maxCommissionDebtPaise == 15000L) {
            "Maximum negative commission balance must be ₹150 (15000 paise)."
        }
        require(wallet.commissionDebtPaise in 0L..15000L) {
            "Rider commission debt cannot exceed ₹150 (15000 paise)."
        }
        val docRef = db.collection("rider_wallets").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "balancePaise" to wallet.balancePaise,
                        "commissionDebtPaise" to wallet.commissionDebtPaise,
                        "workMode" to wallet.workMode,
                        "shiftStatus" to wallet.shiftStatus,
                        "batteryPercent" to wallet.batteryPercent,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = wallet.copy(userId = uid, maxCommissionDebtPaise = 15000L).toWriteMap(isNew = true)
                docRef.set(payload).await()
            }
            uid
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Enforces the ₹150 (15000 paise) commission debt limit atomically via a Firestore transaction
     * so concurrent requests cannot bypass the cap.
     */
    suspend fun recordCommissionDebtOrBlockIfOverLimit(
        projectedCommissionPaise: Long
    ): Result<CloudRiderWallet> = runCatching {
        require(projectedCommissionPaise >= 0L) { "Projected commission must be non-negative." }
        val uid = requireUserId()
        val docRef = db.collection("rider_wallets").document(uid)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                val current = snap.toObject(
                    CloudRiderWallet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Rider wallet not initialized")
                val newDebt = current.commissionDebtPaise + projectedCommissionPaise
                if (newDebt > 15000L) {
                    throw IllegalStateException(
                        "Order assignment blocked: Commission debt (${newDebt / 100.0} INR) exceeds ₹150.00 limit."
                    )
                }
                transaction.update(
                    docRef,
                    mapOf(
                        "commissionDebtPaise" to newDebt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                current.copy(commissionDebtPaise = newDebt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordCompanyLedgerEntry(entry: CloudCompanyLedgerDoc): Result<String> = runCatching {
        val uid = requireUserId()
        val entryId = entry.id.ifBlank { "led_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val docRef = db.collection("company_ledger").document(entryId)
        try {
            docRef.set(entry.copy(userId = uid).toWriteMap()).await()
            entryId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Sends a real-time chat message between the Customer and the Assigned Rider on `/delivery_orders/{orderId}/chat_messages/{messageId}`.
     */
    suspend fun sendOrderChatMessage(
        orderId: String,
        senderRole: String,
        senderName: String,
        messageText: String,
        deliveryStage: String? = null,
        customMessageId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        QuickDeliverySecurityGuard.checkAndRecordEndpointCall(
            endpoint = QuickDeliverySecurityGuard.ENDPOINT_ORDER_CHAT,
            actorKey = uid
        ).getOrThrow()
        val trimmedText = QuickDeliverySecurityGuard.sanitizeText(messageText, 500)
        require(trimmedText.isNotEmpty() && trimmedText.length <= 500) {
            "Chat message must be between 1 and 500 characters after sanitization."
        }
        require(senderRole == "CUSTOMER" || senderRole == "RIDER") {
            "senderRole must be CUSTOMER or RIDER."
        }
        val safeOrderId = QuickDeliverySecurityGuard.sanitizeIdentifier(orderId, 64)
        val msgId = QuickDeliverySecurityGuard.sanitizeIdentifier(customMessageId, 64).ifBlank {
            "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        }
        val cleanSenderName = QuickDeliverySecurityGuard.sanitizeText(senderName, 80)
            .ifBlank { if (senderRole == "RIDER") "Rider Partner" else "Customer" }
        val docRef = db.collection("delivery_orders")
            .document(safeOrderId)
            .collection("chat_messages")
            .document(msgId)
        try {
            val payload = CloudOrderChatMessage(
                id = msgId,
                orderId = safeOrderId,
                senderUid = uid,
                senderRole = senderRole,
                senderName = cleanSenderName,
                messageText = trimmedText,
                deliveryStage = deliveryStage?.let { QuickDeliverySecurityGuard.sanitizeIdentifier(it, 64) }
            ).toWriteMap()
            docRef.set(payload).await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Real-time stream of chat messages for a delivery order (`snapshots()`), ordered chronologically by `createdAt`.
     */
    fun observeOrderChatMessages(orderId: String): Flow<List<CloudOrderChatMessage>> {
        val path = "delivery_orders/$orderId/chat_messages"
        return db.collection("delivery_orders")
            .document(orderId)
            .collection("chat_messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudOrderChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }
}

/**
 * Client-Safe Payment Gateway Configuration Bridge.
 * IMPORTANT SECURITY INVARIANT:
 * - Private merchant secrets (RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET, SMS_API_KEY, ADMIN_SECRET)
 *   are NEVER compiled into client APKs or accessed on the device.
 * - Client-side code cannot compute trusted payment HMAC signatures or mark an order VERIFIED_PAID_TO_COMPANY.
 * - All gateway callbacks on the client remain in PENDING_GATEWAY_VERIFICATION until verified by a trusted server.
 */
object QuickDeliveryLiveGatewayBridge {
    val publicRazorpayKeyId: String
        get() = runCatching { BuildConfig.QUICK_DELIVERY_RAZORPAY_KEY_ID }.getOrDefault("")

    val isPublicKeyIdConfigured: Boolean
        get() = publicRazorpayKeyId.isNotBlank() &&
            !publicRazorpayKeyId.contains("CONFIGURE") &&
            (publicRazorpayKeyId.startsWith("rzp_test_") || publicRazorpayKeyId.startsWith("rzp_live_"))

    val backendUrl: String
        get() = runCatching { BuildConfig.QUICK_DELIVERY_BACKEND_URL }.getOrDefault("http://10.0.2.2:8080/")
}

/**
 * One-shot FusedLocationProviderClient GPS & Camera Proof helper for Customer and Rider apps.
 * Note: Performs foreground one-shot high-accuracy location retrieval when location permission is granted;
 * continuous background foreground-service tracking is not enabled.
 */
object DeliveryProofAndGpsManager {
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchLiveGpsLocation(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            if (loc != null) Pair(loc.latitude, loc.longitude) else null
        }.getOrNull()
    }

    fun formatCameraProofSummary(bitmap: Bitmap, stageLabel: String): String {
        val ts = SimpleDateFormat("dd MMM HH:mm:ss", Locale.US).format(Date())
        return "$stageLabel Photo Proof Verified (${bitmap.width}x${bitmap.height}px @ $ts)"
    }
}
