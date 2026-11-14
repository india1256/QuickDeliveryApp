package com.example.data.cloud

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.RiderProfile
import com.example.data.repository.SharedOrderRepository

data class SavedCustomerAddress(
    val id: String,
    val label: String, // "Home", "Office", "Shop", or custom
    val houseFlat: String,
    val building: String,
    val areaStreet: String,
    val pinCode: String,
    val landmark: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("houseFlat", houseFlat)
        put("building", building)
        put("areaStreet", areaStreet)
        put("pinCode", pinCode)
        put("landmark", landmark)
        put("lat", lat)
        put("lng", lng)
    }

    companion object {
        fun fromJson(obj: JSONObject): SavedCustomerAddress? {
            val id = obj.optString("id", "").trim()
            val label = obj.optString("label", "").trim()
            val area = obj.optString("areaStreet", "").trim()
            if (id.isEmpty() || label.isEmpty() || area.isEmpty()) return null
            return SavedCustomerAddress(
                id = id,
                label = label,
                houseFlat = obj.optString("houseFlat", ""),
                building = obj.optString("building", ""),
                areaStreet = area,
                pinCode = obj.optString("pinCode", ""),
                landmark = obj.optString("landmark", ""),
                lat = obj.optDouble("lat", 0.0),
                lng = obj.optDouble("lng", 0.0)
            )
        }
    }
}

data class AdminPromoCoupon(
    val code: String,
    val discountPercent: Int = 0,
    val maxDiscountInr: Int = 50,
    val flatDiscountInr: Int = 0,
    val description: String = "",
    val isActive: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun computeDiscountInr(subtotalInr: Double): Double {
        if (!isActive || subtotalInr <= 0.0) return 0.0
        val pctDisc = if (discountPercent > 0) (subtotalInr * discountPercent / 100.0).coerceAtMost(maxDiscountInr.toDouble()) else 0.0
        val flatDisc = flatDiscountInr.toDouble().coerceAtMost(subtotalInr * 0.4)
        return maxOf(pctDisc, flatDisc).coerceAtLeast(0.0)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("code", code)
        put("discountPercent", discountPercent)
        put("maxDiscountInr", maxDiscountInr)
        put("flatDiscountInr", flatDiscountInr)
        put("description", description)
        put("isActive", isActive)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): AdminPromoCoupon? {
            val code = obj.optString("code", "").trim().uppercase()
            if (code.isBlank()) return null
            return AdminPromoCoupon(
                code = code,
                discountPercent = obj.optInt("discountPercent", 0),
                maxDiscountInr = obj.optInt("maxDiscountInr", 50),
                flatDiscountInr = obj.optInt("flatDiscountInr", 0),
                description = obj.optString("description", ""),
                isActive = obj.optBoolean("isActive", true),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

enum class WithdrawalStatus {
    PENDING,
    PAID,
    REJECTED;

    companion object {
        fun fromString(raw: String?): WithdrawalStatus = when (raw?.trim()?.uppercase()) {
            "PAID", "APPROVED" -> PAID
            "REJECTED" -> REJECTED
            else -> PENDING
        }
    }
}

data class RiderWithdrawalRequest(
    val requestId: String,
    val riderId: String,
    val riderName: String,
    val riderPhone: String,
    val amountInr: Double,
    val payoutDestination: String, // UPI ID or Bank Account
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val adminNote: String = "",
    val createdAtLabel: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("requestId", requestId)
        put("riderId", riderId)
        put("riderName", riderName)
        put("riderPhone", riderPhone)
        put("amountInr", amountInr)
        put("payoutDestination", payoutDestination)
        put("status", status.name)
        put("adminNote", adminNote)
        put("createdAtLabel", createdAtLabel)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): RiderWithdrawalRequest? {
            val id = obj.optString("requestId", "").trim()
            if (id.isEmpty()) return null
            return RiderWithdrawalRequest(
                requestId = id,
                riderId = obj.optString("riderId", ""),
                riderName = obj.optString("riderName", "Rider Partner"),
                riderPhone = obj.optString("riderPhone", ""),
                amountInr = obj.optDouble("amountInr", 0.0),
                payoutDestination = obj.optString("payoutDestination", ""),
                status = WithdrawalStatus.fromString(obj.optString("status")),
                adminNote = obj.optString("adminNote", ""),
                createdAtLabel = obj.optString("createdAtLabel", ""),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

data class CustomerSupportTicket(
    val ticketId: String,
    val senderRole: String,
    val senderName: String,
    val senderPhone: String,
    val orderId: String,
    val category: String,
    val message: String,
    val isResolved: Boolean = false,
    val adminReply: String = "",
    val createdAtLabel: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("ticketId", ticketId)
        put("senderRole", senderRole)
        put("senderName", senderName)
        put("senderPhone", senderPhone)
        put("orderId", orderId)
        put("category", category)
        put("message", message)
        put("isResolved", isResolved)
        put("adminReply", adminReply)
        put("createdAtLabel", createdAtLabel)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): CustomerSupportTicket? {
            val id = obj.optString("ticketId", "").trim()
            val msg = obj.optString("message", "").trim()
            if (id.isEmpty() || msg.isEmpty()) return null
            return CustomerSupportTicket(
                ticketId = id,
                senderRole = obj.optString("senderRole", "CUSTOMER"),
                senderName = obj.optString("senderName", "User"),
                senderPhone = obj.optString("senderPhone", ""),
                orderId = obj.optString("orderId", ""),
                category = obj.optString("category", "General Help"),
                message = msg,
                isResolved = obj.optBoolean("isResolved", false),
                adminReply = obj.optString("adminReply", ""),
                createdAtLabel = obj.optString("createdAtLabel", ""),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Shared Production Operations Manager across Customer, Rider Partner, and Admin Console:
 * - Customer Saved Addresses (Home / Office / Shop)
 * - Dynamic Admin-Managed Promo Coupons
 * - Rider Bank/UPI Withdrawal Requests + Admin Payout Settlement
 * - Real In-App Support & Cancellation Tickets (replaces static fake helplines)
 */
object QuickDeliveryOperationsManager {
    private const val PREFS_NAME = "quick_delivery_production_ops_prefs_v1"
    private const val KEY_SAVED_ADDRESSES = "saved_customer_addresses"
    private const val KEY_PROMO_COUPONS = "admin_promo_coupons"
    private const val KEY_WITHDRAWALS = "rider_withdrawal_requests"
    private const val KEY_SUPPORT_TICKETS = "support_tickets"
    private const val KEY_ADMIN_ALLOWLIST_EMAIL = "admin_signed_in_email"

    private val lock = Any()
    private var loaded = false

    private val _savedAddresses = MutableStateFlow<List<SavedCustomerAddress>>(emptyList())
    val savedAddresses: StateFlow<List<SavedCustomerAddress>> = _savedAddresses.asStateFlow()

    private val _promoCoupons = MutableStateFlow<List<AdminPromoCoupon>>(emptyList())
    val promoCoupons: StateFlow<List<AdminPromoCoupon>> = _promoCoupons.asStateFlow()

    private val _withdrawalRequests = MutableStateFlow<List<RiderWithdrawalRequest>>(emptyList())
    val withdrawalRequests: StateFlow<List<RiderWithdrawalRequest>> = _withdrawalRequests.asStateFlow()

    private val _supportTickets = MutableStateFlow<List<CustomerSupportTicket>>(emptyList())
    val supportTickets: StateFlow<List<CustomerSupportTicket>> = _supportTickets.asStateFlow()

    private val _signedInAdminEmail = MutableStateFlow<String?>(null)
    val signedInAdminEmail: StateFlow<String?> = _signedInAdminEmail.asStateFlow()

    val ALLOWED_ADMIN_EMAILS = setOf(
        "ravindergupta141@gmail.com",
        "admin@quickdelivery.in"
    )

    fun ensureLoaded(context: Context) {
        synchronized(lock) {
            if (loaded) return
            loaded = true
            runCatching {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _signedInAdminEmail.value = prefs.getString(KEY_ADMIN_ALLOWLIST_EMAIL, null)

                prefs.getString(KEY_SAVED_ADDRESSES, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    _savedAddresses.value = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.let(SavedCustomerAddress::fromJson) }
                }

                prefs.getString(KEY_PROMO_COUPONS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    _promoCoupons.value = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.let(AdminPromoCoupon::fromJson) }
                }

                prefs.getString(KEY_WITHDRAWALS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    _withdrawalRequests.value = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.let(RiderWithdrawalRequest::fromJson) }
                }

                prefs.getString(KEY_SUPPORT_TICKETS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    _supportTickets.value = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.let(CustomerSupportTicket::fromJson) }
                }
            }
        }
    }

    private fun persist(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SAVED_ADDRESSES, JSONArray().apply { _savedAddresses.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_PROMO_COUPONS, JSONArray().apply { _promoCoupons.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_WITHDRAWALS, JSONArray().apply { _withdrawalRequests.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_SUPPORT_TICKETS, JSONArray().apply { _supportTickets.value.forEach { put(it.toJson()) } }.toString())
                .apply()
        }
    }

    // ---------------- CUSTOMER SAVED ADDRESSES ----------------
    fun saveCustomerAddress(
        context: Context,
        label: String,
        houseFlat: String,
        building: String,
        areaStreet: String,
        pinCode: String,
        landmark: String,
        lat: Double,
        lng: Double
    ): SavedCustomerAddress {
        ensureLoaded(context)
        val cleanLabel = label.trim().ifBlank { "Saved Place" }
        val entry = SavedCustomerAddress(
            id = "addr_${cleanLabel.lowercase(Locale.US)}_${System.currentTimeMillis() % 10000}",
            label = cleanLabel,
            houseFlat = houseFlat.trim(),
            building = building.trim(),
            areaStreet = areaStreet.trim(),
            pinCode = pinCode.trim(),
            landmark = landmark.trim(),
            lat = lat,
            lng = lng
        )
        synchronized(lock) {
            val filtered = _savedAddresses.value.filterNot { it.label.equals(cleanLabel, ignoreCase = true) }
            _savedAddresses.value = listOf(entry) + filtered
            persist(context)
        }
        return entry
    }

    fun deleteSavedAddress(context: Context, id: String) {
        ensureLoaded(context)
        synchronized(lock) {
            _savedAddresses.value = _savedAddresses.value.filterNot { it.id == id }
            persist(context)
        }
    }

    // ---------------- DYNAMIC ADMIN PROMO COUPONS ----------------
    fun upsertPromoCouponFromAdmin(
        context: Context,
        code: String,
        discountPercent: Int,
        maxDiscountInr: Int,
        flatDiscountInr: Int,
        description: String,
        isActive: Boolean = true
    ): AdminPromoCoupon? {
        ensureLoaded(context)
        val cleanCode = code.trim().uppercase().filter { it.isLetterOrDigit() }
        if (cleanCode.length < 3) return null
        val coupon = AdminPromoCoupon(
            code = cleanCode,
            discountPercent = discountPercent.coerceIn(0, 50),
            maxDiscountInr = maxDiscountInr.coerceIn(10, 500),
            flatDiscountInr = flatDiscountInr.coerceIn(0, 200),
            description = description.trim().ifBlank { "Admin Instant Offer" },
            isActive = isActive,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        synchronized(lock) {
            val rest = _promoCoupons.value.filterNot { it.code == cleanCode }
            _promoCoupons.value = listOf(coupon) + rest
            persist(context)
        }
        return coupon
    }

    fun togglePromoCouponStatus(context: Context, code: String) {
        ensureLoaded(context)
        synchronized(lock) {
            _promoCoupons.value = _promoCoupons.value.map {
                if (it.code == code) it.copy(isActive = !it.isActive, updatedAtEpochMs = System.currentTimeMillis()) else it
            }
            persist(context)
        }
    }

    fun findActiveCoupon(code: String): AdminPromoCoupon? {
        val clean = code.trim().uppercase()
        return _promoCoupons.value.firstOrNull { it.code == clean && it.isActive }
    }

    // ---------------- RIDER PAYOUT WITHDRAWAL REQUESTS ----------------
    fun submitRiderWithdrawalRequest(
        context: Context,
        riderId: String,
        riderName: String,
        riderPhone: String,
        amountInr: Double,
        payoutDestination: String
    ): RiderWithdrawalRequest? {
        ensureLoaded(context)
        if (amountInr < 50.0 || payoutDestination.trim().length < 5) return null
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val req = RiderWithdrawalRequest(
            requestId = "WD-${(now % 100000).toString().padStart(5, '0')}",
            riderId = riderId,
            riderName = riderName.ifBlank { "Rider Partner" },
            riderPhone = riderPhone,
            amountInr = amountInr,
            payoutDestination = payoutDestination.trim(),
            status = WithdrawalStatus.PENDING,
            createdAtLabel = label,
            updatedAtEpochMs = now
        )
        synchronized(lock) {
            _withdrawalRequests.value = listOf(req) + _withdrawalRequests.value
            persist(context)
        }
        return req
    }

    fun updateWithdrawalStatusByAdmin(
        context: Context,
        requestId: String,
        status: WithdrawalStatus,
        note: String = ""
    ) {
        ensureLoaded(context)
        synchronized(lock) {
            _withdrawalRequests.value = _withdrawalRequests.value.map {
                if (it.requestId == requestId) {
                    it.copy(
                        status = status,
                        adminNote = note.trim().ifBlank { if (status == WithdrawalStatus.PAID) "Transferred via Bank/UPI" else "Rejected by Admin" },
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                } else it
            }
            persist(context)
        }
    }

    // ---------------- SUPPORT TICKETS ----------------
    fun submitSupportTicket(
        context: Context,
        senderRole: String,
        senderName: String,
        senderPhone: String,
        orderId: String,
        category: String,
        message: String
    ): CustomerSupportTicket? {
        ensureLoaded(context)
        if (message.trim().isEmpty()) return null
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val ticket = CustomerSupportTicket(
            ticketId = "TKT-${(now % 100000).toString().padStart(5, '0')}",
            senderRole = senderRole,
            senderName = senderName.ifBlank { "Customer" },
            senderPhone = senderPhone,
            orderId = orderId,
            category = category.ifBlank { "Order Support" },
            message = message.trim(),
            isResolved = false,
            createdAtLabel = label,
            updatedAtEpochMs = now
        )
        synchronized(lock) {
            _supportTickets.value = listOf(ticket) + _supportTickets.value
            persist(context)
        }
        return ticket
    }

    fun resolveSupportTicketByAdmin(context: Context, ticketId: String, adminReply: String) {
        ensureLoaded(context)
        synchronized(lock) {
            _supportTickets.value = _supportTickets.value.map {
                if (it.ticketId == ticketId) {
                    it.copy(
                        isResolved = true,
                        adminReply = adminReply.trim().ifBlank { "Resolved by Admin Support Team" },
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                } else it
            }
            persist(context)
        }
    }

    // ---------------- ADMIN ALLOWLIST LOGIN ----------------
    fun isEmailAllowlistedForAdmin(email: String?): Boolean {
        val clean = email.orEmpty().trim().lowercase()
        return clean in ALLOWED_ADMIN_EMAILS
    }

    fun saveSignedInAdminSession(context: Context, email: String) {
        ensureLoaded(context)
        val clean = email.trim().lowercase()
        _signedInAdminEmail.value = clean
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ADMIN_ALLOWLIST_EMAIL, clean)
                .apply()
        }
    }

    fun signOutAdminSession(context: Context) {
        ensureLoaded(context)
        _signedInAdminEmail.value = null
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ADMIN_ALLOWLIST_EMAIL)
                .apply()
        }
    }

    // ---------------- SERVER LIVE-SYNC MERGE & EXPORT ----------------
    fun exportOpsPayloadJson(): JSONObject = JSONObject().apply {
        put("promoCoupons", JSONArray().apply { _promoCoupons.value.forEach { put(it.toJson()) } })
        put("withdrawalRequests", JSONArray().apply { _withdrawalRequests.value.forEach { put(it.toJson()) } })
        put("supportTickets", JSONArray().apply { _supportTickets.value.forEach { put(it.toJson()) } })
    }

    fun mergeFromServerPayload(context: Context, root: JSONObject) {
        ensureLoaded(context)
        var changed = false
        synchronized(lock) {
            root.optJSONArray("promoCoupons")?.let { arr ->
                val merged = _promoCoupons.value.associateBy { it.code }.toMutableMap()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)?.let(AdminPromoCoupon::fromJson) ?: continue
                    val prev = merged[item.code]
                    if (prev == null || item.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[item.code] = item
                        changed = true
                    }
                }
                _promoCoupons.value = merged.values.sortedByDescending { it.updatedAtEpochMs }
            }

            root.optJSONArray("withdrawalRequests")?.let { arr ->
                val merged = _withdrawalRequests.value.associateBy { it.requestId }.toMutableMap()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)?.let(RiderWithdrawalRequest::fromJson) ?: continue
                    val prev = merged[item.requestId]
                    if (prev == null || item.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[item.requestId] = item
                        changed = true
                    }
                }
                _withdrawalRequests.value = merged.values.sortedByDescending { it.updatedAtEpochMs }
            }

            root.optJSONArray("supportTickets")?.let { arr ->
                val merged = _supportTickets.value.associateBy { it.ticketId }.toMutableMap()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)?.let(CustomerSupportTicket::fromJson) ?: continue
                    val prev = merged[item.ticketId]
                    if (prev == null || item.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[item.ticketId] = item
                        changed = true
                    }
                }
                _supportTickets.value = merged.values.sortedByDescending { it.updatedAtEpochMs }
            }

            if (changed) persist(context)
        }
    }

    // ---------------- ORDER MUTATIONS (CANCEL & REASSIGN) ----------------
    @Suppress("UNCHECKED_CAST")
    private fun getMutableOrdersFlow(repository: SharedOrderRepository): MutableStateFlow<List<CustomerOrder>>? {
        return runCatching {
            val field = SharedOrderRepository::class.java.getDeclaredField("_orders")
            field.isAccessible = true
            field.get(repository) as? MutableStateFlow<List<CustomerOrder>>
        }.getOrNull()
    }

    fun cancelOrderWithReason(
        context: Context,
        repository: SharedOrderRepository,
        orderId: String,
        reason: String,
        cancelledByRole: String
    ): Boolean {
        val flow = getMutableOrdersFlow(repository) ?: return false
        val cleanReason = reason.trim().ifBlank { "Cancelled by $cancelledByRole" }
        var found = false
        flow.value = flow.value.map { order ->
            if (order.orderId == orderId &&
                order.orderStatus != OrderStatus.DELIVERED &&
                order.orderStatus != OrderStatus.CANCELLED
            ) {
                found = true
                order.copy(
                    orderStatus = OrderStatus.CANCELLED,
                    latestRiderQuickNote = "Cancelled ($cancelledByRole): $cleanReason"
                )
            } else {
                order
            }
        }
        if (found) {
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        return found
    }

    fun reassignOrderToRider(
        context: Context,
        repository: SharedOrderRepository,
        orderId: String,
        targetRider: RiderProfile
    ): Boolean {
        val flow = getMutableOrdersFlow(repository) ?: return false
        var found = false
        flow.value = flow.value.map { order ->
            if (order.orderId == orderId &&
                order.orderStatus != OrderStatus.DELIVERED &&
                order.orderStatus != OrderStatus.CANCELLED
            ) {
                found = true
                val nextStatus = if (order.orderStatus == OrderStatus.SEARCHING_RIDER) {
                    OrderStatus.RIDER_ASSIGNED
                } else {
                    order.orderStatus
                }
                order.copy(
                    orderStatus = nextStatus,
                    assignedRiderId = targetRider.riderId,
                    assignedRiderName = targetRider.name,
                    assignedRiderPhone = targetRider.mobile,
                    assignedRiderVehicle = "${targetRider.vehicleType.title} (${targetRider.vehicleNumber})",
                    latestRiderQuickNote = "Reassigned by Admin to ${targetRider.name}"
                )
            } else {
                order
            }
        }
        if (found) {
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        return found
    }
}

