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
    val payoutMode: String = "INSTANT_UPI", // INSTANT_UPI or WEEKLY_AUTO
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
        put("payoutMode", payoutMode)
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
                payoutMode = obj.optString("payoutMode", "INSTANT_UPI").ifBlank { "INSTANT_UPI" },
                status = WithdrawalStatus.fromString(obj.optString("status")),
                adminNote = obj.optString("adminNote", ""),
                createdAtLabel = obj.optString("createdAtLabel", ""),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

data class RiderCodSettlementRecord(
    val settlementId: String,
    val riderId: String,
    val riderName: String,
    val riderPhone: String,
    val amountInr: Double,
    val referenceUtr: String,
    val settledAtLabel: String = "",
    val status: String = "VERIFIED", // "PENDING_ADMIN" (submitted by Rider) or "VERIFIED" (approved by Admin)
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("settlementId", settlementId)
        put("riderId", riderId)
        put("riderName", riderName)
        put("riderPhone", riderPhone)
        put("amountInr", amountInr)
        put("referenceUtr", referenceUtr)
        put("settledAtLabel", settledAtLabel)
        put("status", status)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): RiderCodSettlementRecord? {
            val id = obj.optString("settlementId", "").trim()
            if (id.isEmpty()) return null
            return RiderCodSettlementRecord(
                settlementId = id,
                riderId = obj.optString("riderId", ""),
                riderName = obj.optString("riderName", "Rider Partner"),
                riderPhone = obj.optString("riderPhone", ""),
                amountInr = obj.optDouble("amountInr", 0.0),
                referenceUtr = obj.optString("referenceUtr", "CASH_DESK"),
                settledAtLabel = obj.optString("settledAtLabel", ""),
                status = obj.optString("status", "VERIFIED").uppercase().ifBlank { "VERIFIED" },
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Unified 100% Automatic Rider Wallet & COD 20% Commission Ledger Snapshot:
 * - COD Delivered Orders: Rider keeps 80% cash, 20% company commission debits Net Ledger Balance (goes negative).
 * - Online/Prepaid Delivered Orders: Company keeps 20%, Rider's 80% automatically offsets any negative COD due FIRST,
 *   and any surplus automatically credits Rider's positive withdrawable wallet balance!
 * - ₹200 Limit (configurable): Reaching -₹200 automatically locks new orders; valid payment automatically unlocks back to ₹0 (FREE).
 */
data class RiderCodLedgerSnapshot(
    val riderId: String,
    val codDeliveredOrdersCount: Int,
    val totalCodCashCollectedInr: Double,
    val rider80ShareKeptInr: Double,
    val grossCompany20CommissionInr: Double,
    val verifiedPaidToAdminInr: Double,
    val pendingTransferClaimInr: Double,
    val pendingClaimRecord: RiderCodSettlementRecord?,
    val unpaidCommissionDueInr: Double,
    val codLimitInr: Double,
    val commissionPercent: Int,
    val prepaidDeliveredOrdersCount: Int = 0,
    val totalPrepaidFareInr: Double = 0.0,
    val prepaidRider80CreditsInr: Double = 0.0,
    val prepaidAutoOffsetAppliedInr: Double = 0.0,
    val autoSettledTransfersInr: Double = 0.0,
    val paidWithdrawalsInr: Double = 0.0,
    val netLedgerBalanceInr: Double = -unpaidCommissionDueInr,
    val withdrawableWalletBalanceInr: Double = netLedgerBalanceInr.coerceAtLeast(0.0)
) {
    val isLockedAtLimit: Boolean
        get() = unpaidCommissionDueInr >= codLimitInr && codLimitInr > 0.0

    val isFreeZeroDue: Boolean
        get() = unpaidCommissionDueInr < 0.5

    val progressFraction: Float
        get() = if (codLimitInr <= 0.0) 0f else (unpaidCommissionDueInr / codLimitInr).toFloat().coerceIn(0f, 1f)

    val remainingBeforeLockInr: Double
        get() = (codLimitInr - unpaidCommissionDueInr).coerceAtLeast(0.0)
}

enum class DisputeStatus {
    OPEN,
    REFUNDED,
    REJECTED;

    companion object {
        fun fromString(raw: String?): DisputeStatus = when (raw?.trim()?.uppercase()) {
            "REFUNDED" -> REFUNDED
            "REJECTED" -> REJECTED
            else -> OPEN
        }
    }
}

data class OrderDisputeRefundRecord(
    val disputeId: String,
    val orderId: String,
    val customerPhone: String,
    val customerName: String,
    val reason: String,
    val photoProofSummary: String = "",
    val status: DisputeStatus = DisputeStatus.OPEN,
    val refundAmountInr: Double = 0.0,
    val adminNote: String = "",
    val createdAtLabel: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("disputeId", disputeId)
        put("orderId", orderId)
        put("customerPhone", customerPhone)
        put("customerName", customerName)
        put("reason", reason)
        put("photoProofSummary", photoProofSummary)
        put("status", status.name)
        put("refundAmountInr", refundAmountInr)
        put("adminNote", adminNote)
        put("createdAtLabel", createdAtLabel)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): OrderDisputeRefundRecord? {
            val id = obj.optString("disputeId", "").trim()
            if (id.isEmpty()) return null
            return OrderDisputeRefundRecord(
                disputeId = id,
                orderId = obj.optString("orderId", ""),
                customerPhone = obj.optString("customerPhone", ""),
                customerName = obj.optString("customerName", "Customer"),
                reason = obj.optString("reason", "Delivery Issue"),
                photoProofSummary = obj.optString("photoProofSummary", ""),
                status = DisputeStatus.fromString(obj.optString("status")),
                refundAmountInr = obj.optDouble("refundAmountInr", 0.0),
                adminNote = obj.optString("adminNote", ""),
                createdAtLabel = obj.optString("createdAtLabel", ""),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

enum class DocExpirySeverity {
    VALID,
    EXPIRING_SOON,
    EXPIRED,
    NOT_SET
}

data class RiderDocExpiryRecord(
    val riderId: String,
    val riderName: String,
    val dlExpiry: String = "",
    val rcExpiry: String = "",
    val insuranceExpiry: String = "",
    val autoHoldOnExpired: Boolean = false,
    val alertSentAtLabel: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("riderId", riderId)
        put("riderName", riderName)
        put("dlExpiry", dlExpiry)
        put("rcExpiry", rcExpiry)
        put("insuranceExpiry", insuranceExpiry)
        put("autoHoldOnExpired", autoHoldOnExpired)
        put("alertSentAtLabel", alertSentAtLabel)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): RiderDocExpiryRecord? {
            val id = obj.optString("riderId", "").trim()
            if (id.isEmpty()) return null
            return RiderDocExpiryRecord(
                riderId = id,
                riderName = obj.optString("riderName", "Rider Partner"),
                dlExpiry = obj.optString("dlExpiry", ""),
                rcExpiry = obj.optString("rcExpiry", ""),
                insuranceExpiry = obj.optString("insuranceExpiry", ""),
                autoHoldOnExpired = obj.optBoolean("autoHoldOnExpired", false),
                alertSentAtLabel = obj.optString("alertSentAtLabel", ""),
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

data class OrderTimelineEvent(
    val eventId: String,
    val orderId: String,
    val title: String,
    val detail: String,
    val actorRole: String,
    val timestampLabel: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("eventId", eventId)
        put("orderId", orderId)
        put("title", title)
        put("detail", detail)
        put("actorRole", actorRole)
        put("timestampLabel", timestampLabel)
        put("createdAtEpochMs", createdAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): OrderTimelineEvent? {
            val id = obj.optString("eventId", "").trim()
            val oid = obj.optString("orderId", "").trim()
            val title = obj.optString("title", "").trim()
            if (id.isEmpty() || oid.isEmpty() || title.isEmpty()) return null
            return OrderTimelineEvent(
                eventId = id,
                orderId = oid,
                title = title,
                detail = obj.optString("detail", ""),
                actorRole = obj.optString("actorRole", "SYSTEM"),
                timestampLabel = obj.optString("timestampLabel", ""),
                createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

data class CustomerUnreachableRecord(
    val orderId: String,
    val riderId: String,
    val riderName: String,
    val reportedAtLabel: String,
    val note: String = "Customer not responding at pickup/drop gate",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("orderId", orderId)
        put("riderId", riderId)
        put("riderName", riderName)
        put("reportedAtLabel", reportedAtLabel)
        put("note", note)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): CustomerUnreachableRecord? {
            val oid = obj.optString("orderId", "").trim()
            if (oid.isEmpty()) return null
            return CustomerUnreachableRecord(
                orderId = oid,
                riderId = obj.optString("riderId", ""),
                riderName = obj.optString("riderName", "Rider Partner"),
                reportedAtLabel = obj.optString("reportedAtLabel", ""),
                note = obj.optString("note", "Customer not responding"),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

enum class B2bDukaanStatus {
    PENDING_ADMIN_APPROVAL,
    APPROVED,
    SUSPENDED;

    companion object {
        fun fromString(raw: String?): B2bDukaanStatus = when (raw?.trim()?.uppercase()) {
            "APPROVED" -> APPROVED
            "SUSPENDED" -> SUSPENDED
            else -> PENDING_ADMIN_APPROVAL
        }
    }
}

data class B2bDukaanAccountRecord(
    val accountId: String,
    val shopName: String,
    val ownerName: String,
    val ownerPhone: String,
    val gstin: String = "URD",
    val pickupAddress: String,
    val status: B2bDukaanStatus = B2bDukaanStatus.PENDING_ADMIN_APPROVAL,
    val approvedCreditLimitInr: Int = 0,
    val usedCreditInr: Double = 0.0,
    val totalBilledOrders: Int = 0,
    val gstInvoiceNumber: String = "INV-B2B-2026-1001",
    val lastSettledLabel: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val availableCreditInr: Double
        get() = if (status == B2bDukaanStatus.APPROVED) {
            (approvedCreditLimitInr.toDouble() - usedCreditInr).coerceAtLeast(0.0)
        } else {
            0.0
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("accountId", accountId)
        put("shopName", shopName)
        put("ownerName", ownerName)
        put("ownerPhone", ownerPhone)
        put("gstin", gstin)
        put("pickupAddress", pickupAddress)
        put("status", status.name)
        put("approvedCreditLimitInr", approvedCreditLimitInr)
        put("usedCreditInr", usedCreditInr)
        put("totalBilledOrders", totalBilledOrders)
        put("gstInvoiceNumber", gstInvoiceNumber)
        put("lastSettledLabel", lastSettledLabel)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): B2bDukaanAccountRecord? {
            val id = obj.optString("accountId", "").trim()
            val shop = obj.optString("shopName", "").trim()
            if (id.isEmpty() || shop.isEmpty()) return null
            return B2bDukaanAccountRecord(
                accountId = id,
                shopName = shop,
                ownerName = obj.optString("ownerName", "Owner"),
                ownerPhone = obj.optString("ownerPhone", ""),
                gstin = obj.optString("gstin", "URD").ifBlank { "URD" },
                pickupAddress = obj.optString("pickupAddress", ""),
                status = B2bDukaanStatus.fromString(obj.optString("status")),
                approvedCreditLimitInr = obj.optInt("approvedCreditLimitInr", 0).coerceAtLeast(0),
                usedCreditInr = obj.optDouble("usedCreditInr", 0.0).coerceAtLeast(0.0),
                totalBilledOrders = obj.optInt("totalBilledOrders", 0).coerceAtLeast(0),
                gstInvoiceNumber = obj.optString("gstInvoiceNumber", "INV-B2B-2026-1001"),
                lastSettledLabel = obj.optString("lastSettledLabel", ""),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

data class ServerAppUpdateManifest(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val forceUpdateRequired: Boolean = true,
    val updateNotes: String = "",
    val customerUpdateUrl: String = QuickDeliveryBrandLinks.CUSTOMER_APK_URL,
    val riderUpdateUrl: String = QuickDeliveryBrandLinks.RIDER_APK_URL,
    val adminUpdateUrl: String = QuickDeliveryBrandLinks.ADMIN_APK_URL,
    val unifiedUpdateUrl: String = QuickDeliveryBrandLinks.ALL_IN_ONE_APK_URL,
    val publishedAtIso: String = ""
) {
    fun isUpdateAvailableForApp(currentVersionName: String, currentVersionCode: Int): Boolean {
        if (latestVersionCode > 0 && latestVersionCode > currentVersionCode) return true
        val target = latestVersionName.trim().removePrefix("v").removePrefix("V")
        if (target.isBlank()) return false
        val current = currentVersionName.trim().removePrefix("v").removePrefix("V")
        return AdminRemoteFeatureFlags.compareSemver(target, current) > 0
    }

    fun resolveDownloadUrl(appRole: String): String = when (appRole.trim().uppercase()) {
        "RIDER" -> riderUpdateUrl.ifBlank { QuickDeliveryBrandLinks.RIDER_APK_URL }
        "ADMIN" -> adminUpdateUrl.ifBlank { QuickDeliveryBrandLinks.ADMIN_APK_URL }
        "CUSTOMER" -> customerUpdateUrl.ifBlank { QuickDeliveryBrandLinks.CUSTOMER_APK_URL }
        else -> unifiedUpdateUrl.ifBlank { QuickDeliveryBrandLinks.ALL_IN_ONE_APK_URL }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("latestVersionCode", latestVersionCode)
        put("latestVersionName", latestVersionName)
        put("forceUpdateRequired", forceUpdateRequired)
        put("updateNotes", updateNotes)
        put("customerUpdateUrl", customerUpdateUrl)
        put("riderUpdateUrl", riderUpdateUrl)
        put("adminUpdateUrl", adminUpdateUrl)
        put("unifiedUpdateUrl", unifiedUpdateUrl)
        put("publishedAtIso", publishedAtIso)
    }

    companion object {
        fun fromJson(obj: JSONObject): ServerAppUpdateManifest? {
            val code = obj.optInt("latestVersionCode", 0).coerceAtLeast(0)
            val name = obj.optString("latestVersionName", "").trim().take(24)
            if (code <= 0 && name.isBlank()) return null
            val urlsObj = obj.optJSONObject("urls")
            val custUrl = obj.optString(
                "customerUpdateUrl",
                urlsObj?.optString("CUSTOMER", QuickDeliveryBrandLinks.CUSTOMER_APK_URL)
                    ?: QuickDeliveryBrandLinks.CUSTOMER_APK_URL
            ).trim().ifBlank { QuickDeliveryBrandLinks.CUSTOMER_APK_URL }
            val riderUrl = obj.optString(
                "riderUpdateUrl",
                urlsObj?.optString("RIDER", QuickDeliveryBrandLinks.RIDER_APK_URL)
                    ?: QuickDeliveryBrandLinks.RIDER_APK_URL
            ).trim().ifBlank { QuickDeliveryBrandLinks.RIDER_APK_URL }
            val adminUrl = obj.optString(
                "adminUpdateUrl",
                urlsObj?.optString("ADMIN", QuickDeliveryBrandLinks.ADMIN_APK_URL)
                    ?: QuickDeliveryBrandLinks.ADMIN_APK_URL
            ).trim().ifBlank { QuickDeliveryBrandLinks.ADMIN_APK_URL }
            val unifiedUrl = obj.optString(
                "unifiedUpdateUrl",
                urlsObj?.optString("UNIFIED", QuickDeliveryBrandLinks.ALL_IN_ONE_APK_URL)
                    ?: QuickDeliveryBrandLinks.ALL_IN_ONE_APK_URL
            ).trim().ifBlank { QuickDeliveryBrandLinks.ALL_IN_ONE_APK_URL }
            return ServerAppUpdateManifest(
                latestVersionCode = code,
                latestVersionName = name,
                forceUpdateRequired = obj.optBoolean("forceUpdateRequired", true),
                updateNotes = obj.optString("updateNotes", "").trim().take(240),
                customerUpdateUrl = custUrl,
                riderUpdateUrl = riderUrl,
                adminUpdateUrl = adminUrl,
                unifiedUpdateUrl = unifiedUrl,
                publishedAtIso = obj.optString("publishedAtIso", "").trim()
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
    private const val KEY_REMOTE_FEATURE_FLAGS = "admin_remote_feature_flags_v1"
    private const val KEY_CUSTOMER_WALLET_BONUS = "customer_referral_wallet_bonus_inr"
    private const val KEY_CLAIMED_REFERRAL_CODES = "customer_claimed_referral_codes"
    private const val KEY_WAIT_TIMERS_BY_ORDER = "wait_timers_by_order_json"
    private const val KEY_RIDER_COORDS_BY_ORDER = "rider_live_coords_by_order_json"
    private const val KEY_COD_SETTLEMENTS = "cod_settlement_ledger_json"
    private const val KEY_DISPUTE_RECORDS = "order_dispute_refund_records_json"
    private const val KEY_RIDER_DOC_EXPIRIES = "rider_doc_expiry_records_json"
    private const val KEY_RIDER_PAYOUT_MODE = "rider_payout_mode_pref"
    private const val KEY_CREDITED_DISPUTE_IDS = "credited_refund_dispute_ids"
    private const val KEY_TIMELINE_EVENTS = "order_timeline_events_json"
    private const val KEY_UNREACHABLE_RECORDS = "customer_unreachable_records_json"
    private const val KEY_B2B_ACCOUNTS = "b2b_dukaan_accounts_json"
    private const val KEY_SERVER_APP_UPDATE = "server_app_update_manifest_json"

    private val lock = Any()
    private var loaded = false
    private var activeCustomerAccountKey: String = ""

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

    private val _remoteFeatureFlags = MutableStateFlow(AdminRemoteFeatureFlags())
    val remoteFeatureFlags: StateFlow<AdminRemoteFeatureFlags> = _remoteFeatureFlags.asStateFlow()

    private val _customerWalletBonusInr = MutableStateFlow(0)
    val customerWalletBonusInr: StateFlow<Int> = _customerWalletBonusInr.asStateFlow()

    private val _waitTimerStartByOrder = MutableStateFlow<Map<String, Long>>(emptyMap())
    val waitTimerStartByOrder: StateFlow<Map<String, Long>> = _waitTimerStartByOrder.asStateFlow()

    private val _riderLiveCoordsByOrder = MutableStateFlow<Map<String, Pair<Double, Double>>>(emptyMap())
    val riderLiveCoordsByOrder: StateFlow<Map<String, Pair<Double, Double>>> = _riderLiveCoordsByOrder.asStateFlow()

    private val _codSettlements = MutableStateFlow<List<RiderCodSettlementRecord>>(emptyList())
    val codSettlements: StateFlow<List<RiderCodSettlementRecord>> = _codSettlements.asStateFlow()

    private val _disputeRecords = MutableStateFlow<List<OrderDisputeRefundRecord>>(emptyList())
    val disputeRecords: StateFlow<List<OrderDisputeRefundRecord>> = _disputeRecords.asStateFlow()

    private val _riderDocExpiries = MutableStateFlow<Map<String, RiderDocExpiryRecord>>(emptyMap())
    val riderDocExpiries: StateFlow<Map<String, RiderDocExpiryRecord>> = _riderDocExpiries.asStateFlow()

    private val _riderPayoutModePreference = MutableStateFlow("INSTANT_UPI")
    val riderPayoutModePreference: StateFlow<String> = _riderPayoutModePreference.asStateFlow()

    private val _timelineEventsByOrder = MutableStateFlow<Map<String, List<OrderTimelineEvent>>>(emptyMap())
    val timelineEventsByOrder: StateFlow<Map<String, List<OrderTimelineEvent>>> = _timelineEventsByOrder.asStateFlow()

    private val _unreachableByOrder = MutableStateFlow<Map<String, CustomerUnreachableRecord>>(emptyMap())
    val unreachableByOrder: StateFlow<Map<String, CustomerUnreachableRecord>> = _unreachableByOrder.asStateFlow()

    private val _b2bAccounts = MutableStateFlow<List<B2bDukaanAccountRecord>>(emptyList())
    val b2bAccounts: StateFlow<List<B2bDukaanAccountRecord>> = _b2bAccounts.asStateFlow()

    private val _serverAppUpdate = MutableStateFlow(ServerAppUpdateManifest())
    val serverAppUpdate: StateFlow<ServerAppUpdateManifest> = _serverAppUpdate.asStateFlow()

    private val claimedReferralCodes = mutableSetOf<String>()
    private val creditedDisputeIds = mutableSetOf<String>()

    val ALLOWED_ADMIN_EMAILS = setOf(
        "ravindergupta141@gmail.com",
        QuickDeliveryBrandLinks.ADMIN_BRAND_EMAIL,
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

                prefs.getString(KEY_REMOTE_FEATURE_FLAGS, null)?.let { raw ->
                    val obj = JSONObject(raw)
                    val loadedFlags = AdminRemoteFeatureFlags.fromJson(obj)
                    _remoteFeatureFlags.value = loadedFlags
                    if (obj.optInt("flagsSchemaVersion", 1) < AdminRemoteFeatureFlags.CURRENT_FLAGS_SCHEMA_VERSION) {
                        prefs.edit().putString(KEY_REMOTE_FEATURE_FLAGS, loadedFlags.toJson().toString()).apply()
                    }
                }

                _customerWalletBonusInr.value = prefs.getInt(KEY_CUSTOMER_WALLET_BONUS, 0).coerceAtLeast(0)
                prefs.getStringSet(KEY_CLAIMED_REFERRAL_CODES, emptySet())?.let { codes ->
                    claimedReferralCodes.clear()
                    claimedReferralCodes.addAll(codes)
                }

                prefs.getString(KEY_WAIT_TIMERS_BY_ORDER, null)?.let { raw ->
                    val obj = JSONObject(raw)
                    val map = mutableMapOf<String, Long>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = obj.optLong(k, 0L)
                        if (v > 0L) map[k] = v
                    }
                    _waitTimerStartByOrder.value = map
                }

                prefs.getString(KEY_RIDER_COORDS_BY_ORDER, null)?.let { raw ->
                    val obj = JSONObject(raw)
                    val map = mutableMapOf<String, Pair<Double, Double>>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val pairObj = obj.optJSONObject(k) ?: continue
                        val lat = pairObj.optDouble("lat", 0.0)
                        val lng = pairObj.optDouble("lng", 0.0)
                        if (lat != 0.0 || lng != 0.0) map[k] = lat to lng
                    }
                    _riderLiveCoordsByOrder.value = map
                }

                prefs.getString(KEY_COD_SETTLEMENTS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    _codSettlements.value = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.let(RiderCodSettlementRecord::fromJson) }
                }

                prefs.getString(KEY_DISPUTE_RECORDS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    _disputeRecords.value = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.let(OrderDisputeRefundRecord::fromJson) }
                }

                prefs.getString(KEY_RIDER_DOC_EXPIRIES, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    val map = mutableMapOf<String, RiderDocExpiryRecord>()
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let(RiderDocExpiryRecord::fromJson)?.let { rec ->
                            map[rec.riderId] = rec
                        }
                    }
                    _riderDocExpiries.value = map
                }

                _riderPayoutModePreference.value = prefs.getString(KEY_RIDER_PAYOUT_MODE, "INSTANT_UPI") ?: "INSTANT_UPI"
                prefs.getStringSet(KEY_CREDITED_DISPUTE_IDS, emptySet())?.let { ids ->
                    creditedDisputeIds.clear()
                    creditedDisputeIds.addAll(ids)
                }

                prefs.getString(KEY_TIMELINE_EVENTS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    val grouped = mutableMapOf<String, MutableList<OrderTimelineEvent>>()
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let(OrderTimelineEvent::fromJson)?.let { evt ->
                            grouped.getOrPut(evt.orderId) { mutableListOf() }.add(evt)
                        }
                    }
                    _timelineEventsByOrder.value = grouped.mapValues { (_, list) -> list.sortedBy { it.createdAtEpochMs } }
                }

                prefs.getString(KEY_UNREACHABLE_RECORDS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    val map = mutableMapOf<String, CustomerUnreachableRecord>()
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let(CustomerUnreachableRecord::fromJson)?.let { rec ->
                            map[rec.orderId] = rec
                        }
                    }
                    _unreachableByOrder.value = map
                }

                prefs.getString(KEY_B2B_ACCOUNTS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    _b2bAccounts.value = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.let(B2bDukaanAccountRecord::fromJson) }
                }

                prefs.getString(KEY_SERVER_APP_UPDATE, null)?.let { raw ->
                    ServerAppUpdateManifest.fromJson(JSONObject(raw))?.let { manifest ->
                        _serverAppUpdate.value = manifest
                    }
                }
            }
        }
    }

    fun setActiveCustomerAccount(context: Context, phone: String, email: String) {
        ensureLoaded(context)
        val cleanPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(phone)
        val newKey = when {
            cleanPhone.length == 10 -> cleanPhone
            email.isNotBlank() -> email.trim().lowercase(Locale.US)
            else -> ""
        }
        synchronized(lock) {
            if (activeCustomerAccountKey == newKey) return
            activeCustomerAccountKey = newKey
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val scopedKey = if (newKey.isNotBlank()) "${KEY_SAVED_ADDRESSES}_$newKey" else KEY_SAVED_ADDRESSES
            val raw = prefs.getString(scopedKey, null)
            _savedAddresses.value = if (!raw.isNullOrBlank()) {
                runCatching {
                    val arr = JSONArray(raw)
                    (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(SavedCustomerAddress::fromJson) }
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
        }
    }

    private fun persist(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val waitObj = JSONObject().apply {
                _waitTimerStartByOrder.value.forEach { (k, v) -> put(k, v) }
            }
            val coordsObj = JSONObject().apply {
                _riderLiveCoordsByOrder.value.forEach { (k, pair) ->
                    put(k, JSONObject().apply {
                        put("lat", pair.first)
                        put("lng", pair.second)
                    })
                }
            }
            val addrPrefKey = if (activeCustomerAccountKey.isNotBlank()) {
                "${KEY_SAVED_ADDRESSES}_$activeCustomerAccountKey"
            } else {
                KEY_SAVED_ADDRESSES
            }
            prefs.edit()
                .putString(addrPrefKey, JSONArray().apply { _savedAddresses.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_PROMO_COUPONS, JSONArray().apply { _promoCoupons.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_WITHDRAWALS, JSONArray().apply { _withdrawalRequests.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_SUPPORT_TICKETS, JSONArray().apply { _supportTickets.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_REMOTE_FEATURE_FLAGS, _remoteFeatureFlags.value.toJson().toString())
                .putInt(KEY_CUSTOMER_WALLET_BONUS, _customerWalletBonusInr.value)
                .putStringSet(KEY_CLAIMED_REFERRAL_CODES, claimedReferralCodes.toSet())
                .putString(KEY_WAIT_TIMERS_BY_ORDER, waitObj.toString())
                .putString(KEY_RIDER_COORDS_BY_ORDER, coordsObj.toString())
                .putString(KEY_COD_SETTLEMENTS, JSONArray().apply { _codSettlements.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_DISPUTE_RECORDS, JSONArray().apply { _disputeRecords.value.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_RIDER_DOC_EXPIRIES, JSONArray().apply { _riderDocExpiries.value.values.forEach { put(it.toJson()) } }.toString())
                .putString(KEY_RIDER_PAYOUT_MODE, _riderPayoutModePreference.value)
                .putStringSet(KEY_CREDITED_DISPUTE_IDS, creditedDisputeIds.toSet())
                .putString(
                    KEY_TIMELINE_EVENTS,
                    JSONArray().apply {
                        _timelineEventsByOrder.value.values.flatten().takeLast(300).forEach { put(it.toJson()) }
                    }.toString()
                )
                .putString(
                    KEY_UNREACHABLE_RECORDS,
                    JSONArray().apply {
                        _unreachableByOrder.value.values.forEach { put(it.toJson()) }
                    }.toString()
                )
                .putString(
                    KEY_B2B_ACCOUNTS,
                    JSONArray().apply {
                        _b2bAccounts.value.forEach { put(it.toJson()) }
                    }.toString()
                )
                .putString(KEY_SERVER_APP_UPDATE, _serverAppUpdate.value.toJson().toString())
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
    fun setRiderPayoutModePreference(context: Context, mode: String) {
        ensureLoaded(context)
        val normalized = if (mode.uppercase() == "WEEKLY_AUTO") "WEEKLY_AUTO" else "INSTANT_UPI"
        synchronized(lock) {
            _riderPayoutModePreference.value = normalized
            persist(context)
        }
    }

    fun submitRiderWithdrawalRequest(
        context: Context,
        riderId: String,
        riderName: String,
        riderPhone: String,
        amountInr: Double,
        payoutDestination: String,
        payoutMode: String = _riderPayoutModePreference.value
    ): RiderWithdrawalRequest? {
        ensureLoaded(context)
        if (amountInr < 50.0 || payoutDestination.trim().length < 5) return null
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val mode = if (payoutMode.uppercase() == "WEEKLY_AUTO") "WEEKLY_AUTO" else "INSTANT_UPI"
        val req = RiderWithdrawalRequest(
            requestId = "WD-${(now % 100000).toString().padStart(5, '0')}",
            riderId = riderId,
            riderName = riderName.ifBlank { "Rider Partner" },
            riderPhone = riderPhone,
            amountInr = amountInr,
            payoutDestination = payoutDestination.trim(),
            payoutMode = mode,
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

    // ---------------- ADMIN REMOTE FEATURE SWITCHES & REFERRAL WALLET ----------------
    fun updateRemoteFeatureFlagsFromAdmin(
        context: Context,
        updated: AdminRemoteFeatureFlags,
        repository: SharedOrderRepository? = null
    ): AdminRemoteFeatureFlags {
        ensureLoaded(context)
        val stamped = updated.copy(
            flagsSchemaVersion = AdminRemoteFeatureFlags.CURRENT_FLAGS_SCHEMA_VERSION,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        synchronized(lock) {
            _remoteFeatureFlags.value = stamped
            persist(context)
        }
        applyPricingOverridesFromFlags(stamped, repository)
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return stamped
    }

    @Suppress("UNCHECKED_CAST")
    fun applyPricingOverridesFromFlags(
        flags: AdminRemoteFeatureFlags,
        repository: SharedOrderRepository?
    ) {
        runCatching {
            val currentPricing = repository?.pricingConfig?.value
                ?: com.example.data.pricing.ServerFareEngine.getPricingConfig()
            val effectiveSurge = flags.effectiveSurgeMultiplier()
            val effectiveComm = flags.platformCommissionPercent.toDouble().coerceIn(0.0, 40.0)
            val surgeLabel = when {
                flags.surgePricingEnabled -> "Admin Surge ${String.format(Locale.US, "%.2fx", effectiveSurge)}"
                flags.isCurrentHourInNightWindow() -> "Night Surcharge ${String.format(Locale.US, "%.2fx", effectiveSurge)}"
                flags.isCurrentHourInPeakWindow() -> "Peak Auto-Surge ${String.format(Locale.US, "%.2fx", effectiveSurge)}"
                else -> "Normal (1.0x)"
            }
            val nextPricing = currentPricing.copy(
                bikeBaseFare = if (flags.remoteRateCardOverrideEnabled) flags.bikeBaseFareInr else currentPricing.bikeBaseFare,
                bikeRatePerKm = if (flags.remoteRateCardOverrideEnabled) flags.bikeRatePerKmInr else currentPricing.bikeRatePerKm,
                scooterBaseFare = if (flags.remoteRateCardOverrideEnabled) flags.scooterBaseFareInr else currentPricing.scooterBaseFare,
                scooterRatePerKm = if (flags.remoteRateCardOverrideEnabled) flags.scooterRatePerKmInr else currentPricing.scooterRatePerKm,
                autoBaseFare = if (flags.remoteRateCardOverrideEnabled) flags.autoBaseFareInr else currentPricing.autoBaseFare,
                autoRatePerKm = if (flags.remoteRateCardOverrideEnabled) flags.autoRatePerKmInr else currentPricing.autoRatePerKm,
                surgeMultiplier = effectiveSurge,
                surgePresetName = surgeLabel,
                platformCommissionPercent = effectiveComm
            )
            val saved = com.example.data.pricing.ServerFareEngine.updatePricingConfig(nextPricing, "Admin Master Switch")
            if (repository != null) {
                val field = SharedOrderRepository::class.java.getDeclaredField("_pricingConfig")
                field.isAccessible = true
                val flow = field.get(repository) as MutableStateFlow<com.example.data.model.PricingConfig>
                flow.value = saved
            }
            // Also sync vehicle availability flags to AutomatedFairRiderAssignmentEngine
            com.example.data.delivery.AutomatedFairRiderAssignmentEngine.updateLivePricingAndWeightPolicyFromAdmin(
                repository = repository,
                bikeEnabled = flags.bikeEnabled,
                scooterEnabled = flags.scooterEnabled,
                autoEnabled = flags.autoEnabled,
                updatedBy = "Admin Master Sync"
            )
        }
    }

    // ---------------- ADMIN COD SETTLEMENT LEDGER ----------------
    fun markCodSettledByAdmin(
        context: Context,
        riderId: String,
        riderName: String,
        riderPhone: String,
        amountInr: Double,
        referenceUtr: String
    ): RiderCodSettlementRecord? {
        ensureLoaded(context)
        if (amountInr <= 0.0) return null
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val rec = RiderCodSettlementRecord(
            settlementId = "COD-${(now % 100000).toString().padStart(5, '0')}",
            riderId = riderId,
            riderName = riderName.ifBlank { "Rider Partner" },
            riderPhone = riderPhone,
            amountInr = amountInr,
            referenceUtr = referenceUtr.trim().ifBlank { "CASH_DESK_${now % 10000}" },
            settledAtLabel = label,
            status = "VERIFIED",
            updatedAtEpochMs = now
        )
        synchronized(lock) {
            // Also resolve any pending claims for this rider so nothing stays stuck
            val updated = _codSettlements.value.map { existing ->
                if (existing.riderId == riderId && existing.status == "PENDING_ADMIN") {
                    existing.copy(status = "SUPERSEDED", updatedAtEpochMs = now)
                } else {
                    existing
                }
            }
            _codSettlements.value = listOf(rec) + updated
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return rec
    }

    /**
     * Rider submits a 20% COD commission transfer (UPI UTR or Bank Ref) to Admin.
     * In Autopilot mode (default), any valid 6+ char UTR/payment ref IMMEDIATELY records status = "AUTO_SETTLED"
     * so the Rider's ledger resets to ₹0 (FREE) & unlocks automatically without waiting for manual Admin approval!
     * Admin still sees full audit trail and can click "Reverse & Re-Lock" if a UTR is disputed.
     */
    fun submitRiderCodTransferClaim(
        context: Context,
        riderId: String,
        riderName: String,
        riderPhone: String,
        amountInr: Double,
        referenceUtr: String,
        autoUnlockImmediately: Boolean = true
    ): RiderCodSettlementRecord? {
        ensureLoaded(context)
        if (riderId.isBlank() || amountInr <= 0.0) return null
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val cleanRef = referenceUtr.trim()
        val hasValidPaymentRef = cleanRef.count { it.isLetterOrDigit() } >= 6
        val resolvedStatus = if (autoUnlockImmediately && hasValidPaymentRef) "AUTO_SETTLED" else "PENDING_ADMIN"
        val rec = RiderCodSettlementRecord(
            settlementId = "TRF-${(now % 100000).toString().padStart(5, '0')}",
            riderId = riderId,
            riderName = riderName.ifBlank { "Rider Partner" },
            riderPhone = riderPhone,
            amountInr = amountInr,
            referenceUtr = cleanRef.ifBlank { "UPI_TRF_${now % 10000}" },
            settledAtLabel = label,
            status = resolvedStatus,
            updatedAtEpochMs = now
        )
        synchronized(lock) {
            val baseList = if (resolvedStatus == "AUTO_SETTLED") {
                _codSettlements.value.map { existing ->
                    if (existing.riderId == riderId && existing.status == "PENDING_ADMIN") {
                        existing.copy(status = "SUPERSEDED", updatedAtEpochMs = now)
                    } else {
                        existing
                    }
                }
            } else {
                _codSettlements.value
            }
            _codSettlements.value = listOf(rec) + baseList
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return rec
    }

    /**
     * Admin verifies a pending Rider COD transfer claim, crediting the verified settlement and
     * freeing the Rider to accept orders again once due reaches ₹0.
     */
    fun verifyRiderCodTransferClaimByAdmin(
        context: Context,
        settlementId: String
    ): RiderCodSettlementRecord? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        var verifiedRec: RiderCodSettlementRecord? = null
        synchronized(lock) {
            _codSettlements.value = _codSettlements.value.map { rec ->
                if (rec.settlementId == settlementId) {
                    rec.copy(status = "VERIFIED", updatedAtEpochMs = now).also { verifiedRec = it }
                } else {
                    rec
                }
            }
            persist(context)
        }
        if (verifiedRec != null) {
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        return verifiedRec
    }

    /**
     * Admin safety override: reverses an AUTO_SETTLED (or VERIFIED) COD payment if UTR is fake/bounced,
     * immediately restoring the Rider's unpaid COD due and re-locking if due >= ₹200 limit.
     */
    fun reverseCodSettlementByAdmin(
        context: Context,
        settlementId: String
    ): RiderCodSettlementRecord? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        var reversedRec: RiderCodSettlementRecord? = null
        synchronized(lock) {
            _codSettlements.value = _codSettlements.value.map { rec ->
                if (rec.settlementId == settlementId) {
                    rec.copy(status = "REVERSED", updatedAtEpochMs = now).also { reversedRec = it }
                } else {
                    rec
                }
            }
            persist(context)
        }
        if (reversedRec != null) {
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        return reversedRec
    }

    fun settledCodAmountForRider(riderId: String): Double {
        return _codSettlements.value
            .filter { it.riderId == riderId && (it.status == "VERIFIED" || it.status == "AUTO_SETTLED") }
            .sumOf { it.amountInr }
    }

    /**
     * Computes live 100% Automatic Unified Rider Wallet & Porter-style COD 20% Commission Ledger:
     * 1) Delivered COD orders: Rider collects cash, keeps 80%, owes 20% to company (debits Net Ledger negative).
     * 2) Delivered Online/Prepaid orders: Company keeps 20%, credits Rider's 80% directly to Net Ledger —
     *    AUTOMATICALLY offsetting any negative COD due FIRST (e.g. -₹199 + ₹240 = +₹41 withdrawable, ₹0 due!).
     * 3) Valid UTR transfers (AUTO_SETTLED or Admin VERIFIED) credit Net Ledger back toward ₹0 (FREE).
     * 4) Completed PAID withdrawals debit positive wallet surplus cleanly without creating false COD debt.
     */
    fun computeRiderCodLedgerSnapshot(
        riderId: String,
        riderCommissionDebtInr: Double,
        orders: List<CustomerOrder>,
        settlements: List<RiderCodSettlementRecord> = _codSettlements.value,
        flags: AdminRemoteFeatureFlags = _remoteFeatureFlags.value,
        withdrawalRequests: List<RiderWithdrawalRequest> = _withdrawalRequests.value
    ): RiderCodLedgerSnapshot {
        val commissionPct = flags.platformCommissionPercent.coerceIn(0, 40)
        val codLimit = flags.riderCodCommissionLimitInr.coerceIn(50, 5000).toDouble()
        val deliveredForRider = orders.filter { o ->
            o.orderStatus == com.example.data.model.OrderStatus.DELIVERED &&
                (riderId.isBlank() || o.assignedRiderId == riderId)
        }
        val riderCodOrders = deliveredForRider.filter { o ->
            o.paymentMethod == com.example.data.model.CustomerPaymentMethod.CASH_ON_DELIVERY
        }
        val riderPrepaidOrders = deliveredForRider.filter { o ->
            o.paymentMethod != com.example.data.model.CustomerPaymentMethod.CASH_ON_DELIVERY
        }

        var totalCodCash = 0.0
        var grossCompanyComm = 0.0
        riderCodOrders.forEach { o ->
            val fare = o.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
            val explicitComm = o.platformCommission.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
            val comm = if (explicitComm > 0.0) explicitComm else (fare * commissionPct / 100.0)
            totalCodCash += fare
            grossCompanyComm += comm
        }
        val rider80Kept = (totalCodCash - grossCompanyComm).coerceAtLeast(0.0)

        var totalPrepaidFare = 0.0
        var prepaidRider80Credits = 0.0
        riderPrepaidOrders.forEach { o ->
            val fare = o.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
            val explicitComm = o.platformCommission.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
            val comm = if (explicitComm > 0.0) explicitComm else (fare * commissionPct / 100.0)
            val riderShare = (fare - comm).coerceAtLeast(0.0)
            totalPrepaidFare += fare
            prepaidRider80Credits += riderShare
        }

        val verifiedPaid = settlements
            .filter { it.riderId == riderId && (it.status == "VERIFIED" || it.status == "AUTO_SETTLED") }
            .sumOf { it.amountInr }
        val autoSettledPaid = settlements
            .filter { it.riderId == riderId && it.status == "AUTO_SETTLED" }
            .sumOf { it.amountInr }
        val pendingClaims = settlements.filter { it.riderId == riderId && it.status == "PENDING_ADMIN" }
        val pendingTotal = pendingClaims.sumOf { it.amountInr }

        val paidWithdrawals = withdrawalRequests
            .filter { (riderId.isBlank() || it.riderId == riderId) && it.status == WithdrawalStatus.PAID }
            .sumOf { it.amountInr }

        val codDebitBase = if (riderCodOrders.isNotEmpty()) {
            grossCompanyComm
        } else {
            riderCommissionDebtInr.coerceAtLeast(0.0)
        }
        val remainingCodAfterTransfers = (codDebitBase - verifiedPaid).coerceAtLeast(0.0)
        val prepaidAutoOffset = minOf(remainingCodAfterTransfers, prepaidRider80Credits).coerceAtLeast(0.0)

        val effectivePaidWithdrawals = paidWithdrawals.coerceAtMost(prepaidRider80Credits + verifiedPaid)
        val netLedgerBalance = prepaidRider80Credits + verifiedPaid - effectivePaidWithdrawals - codDebitBase
        val effectiveDue = (-netLedgerBalance).coerceAtLeast(0.0)
        val withdrawableWallet = netLedgerBalance.coerceAtLeast(0.0)

        return RiderCodLedgerSnapshot(
            riderId = riderId,
            codDeliveredOrdersCount = riderCodOrders.size,
            totalCodCashCollectedInr = totalCodCash,
            rider80ShareKeptInr = rider80Kept,
            grossCompany20CommissionInr = grossCompanyComm,
            verifiedPaidToAdminInr = verifiedPaid,
            pendingTransferClaimInr = pendingTotal,
            pendingClaimRecord = pendingClaims.firstOrNull(),
            unpaidCommissionDueInr = effectiveDue,
            codLimitInr = codLimit,
            commissionPercent = commissionPct,
            prepaidDeliveredOrdersCount = riderPrepaidOrders.size,
            totalPrepaidFareInr = totalPrepaidFare,
            prepaidRider80CreditsInr = prepaidRider80Credits,
            prepaidAutoOffsetAppliedInr = prepaidAutoOffset,
            autoSettledTransfersInr = autoSettledPaid,
            paidWithdrawalsInr = effectivePaidWithdrawals,
            netLedgerBalanceInr = netLedgerBalance,
            withdrawableWalletBalanceInr = withdrawableWallet
        )
    }

    /**
     * Generates a formatted End-of-Day Rider Shift Summary Slip ready to share on WhatsApp.
     */
    fun buildRiderShiftSummarySlipText(
        riderName: String,
        riderId: String,
        todayTripsCount: Int,
        todayNetEarningInr: Int,
        ledger: RiderCodLedgerSnapshot
    ): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        val statusText = when {
            ledger.isLockedAtLimit -> "🔴 AUTO-LOCKED (₹${ledger.unpaidCommissionDueInr.toInt()}/₹${ledger.codLimitInr.toInt()} Due)"
            ledger.withdrawableWalletBalanceInr >= 1.0 -> "🟢 FREE • Wallet +₹${ledger.withdrawableWalletBalanceInr.toInt()}"
            ledger.isFreeZeroDue -> "🟢 FREE (₹0 Due)"
            else -> "🟡 Active (-₹${ledger.unpaidCommissionDueInr.toInt()}/₹${ledger.codLimitInr.toInt()} Due)"
        }
        return buildString {
            appendLine("🧾 *PARTNER QUICK DELIVERY — DAILY SHIFT SLIP*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🙋 Partner: *$riderName* ($riderId)")
            appendLine("📅 Time: $dateStr")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📦 Today's Completed Trips: *$todayTripsCount*")
            appendLine("💵 COD Cash Collected: *₹${ledger.totalCodCashCollectedInr.toInt()}*")
            if (ledger.prepaidRider80CreditsInr >= 1.0) {
                appendLine("📲 Online Orders (${100 - ledger.commissionPercent}% Credit): *+₹${ledger.prepaidRider80CreditsInr.toInt()}*")
            }
            if (ledger.prepaidAutoOffsetAppliedInr >= 1.0) {
                appendLine("⚡ Auto-Adjusted COD Due: *₹${ledger.prepaidAutoOffsetAppliedInr.toInt()}*")
            }
            appendLine("💰 Your ${100 - ledger.commissionPercent}% Net Earning: *₹$todayNetEarningInr*")
            appendLine("🏦 Company ${ledger.commissionPercent}% Commission Paid: ₹${ledger.verifiedPaidToAdminInr.toInt()}")
            appendLine("👛 Withdrawable Wallet: *₹${ledger.withdrawableWalletBalanceInr.toInt()}*")
            appendLine("📒 Current Ledger Due: *₹${ledger.unpaidCommissionDueInr.toInt()} / ₹${ledger.codLimitInr.toInt()}*")
            appendLine("📌 Account Status: $statusText")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            append("Quick Delivery Partner Operations")
        }
    }

    // ---------------- ADMIN DISPUTE & INSTANT WALLET REFUND DESK ----------------
    fun submitOrderDispute(
        context: Context,
        orderId: String,
        customerPhone: String,
        customerName: String,
        reason: String,
        photoProofSummary: String = ""
    ): OrderDisputeRefundRecord? {
        ensureLoaded(context)
        if (orderId.isBlank() || reason.isBlank()) return null
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val rec = OrderDisputeRefundRecord(
            disputeId = "DSP-${(now % 100000).toString().padStart(5, '0')}",
            orderId = orderId,
            customerPhone = customerPhone,
            customerName = customerName.ifBlank { "Customer" },
            reason = reason.trim(),
            photoProofSummary = photoProofSummary.trim(),
            status = DisputeStatus.OPEN,
            refundAmountInr = 0.0,
            createdAtLabel = label,
            updatedAtEpochMs = now
        )
        synchronized(lock) {
            _disputeRecords.value = listOf(rec) + _disputeRecords.value
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return rec
    }

    fun processDisputeRefundByAdmin(
        context: Context,
        disputeId: String,
        approveRefund: Boolean,
        refundAmountInr: Double,
        adminNote: String
    ): OrderDisputeRefundRecord? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        var updatedItem: OrderDisputeRefundRecord? = null
        synchronized(lock) {
            _disputeRecords.value = _disputeRecords.value.map { d ->
                if (d.disputeId == disputeId) {
                    val next = d.copy(
                        status = if (approveRefund) DisputeStatus.REFUNDED else DisputeStatus.REJECTED,
                        refundAmountInr = if (approveRefund) refundAmountInr.coerceAtLeast(10.0) else 0.0,
                        adminNote = adminNote.trim().ifBlank {
                            if (approveRefund) "Instant Wallet Refund Credited" else "Rejected after proof review"
                        },
                        updatedAtEpochMs = now
                    )
                    updatedItem = next
                    if (approveRefund && next.disputeId !in creditedDisputeIds &&
                        (activeCustomerAccountKey.isBlank() ||
                            next.customerPhone.filter(Char::isDigit).takeLast(10) == activeCustomerAccountKey)
                    ) {
                        creditedDisputeIds.add(next.disputeId)
                        _customerWalletBonusInr.value += next.refundAmountInr.toInt()
                    }
                    next
                } else d
            }
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return updatedItem
    }

    // ---------------- RIDER DOCUMENT EXPIRY TRACKER ----------------
    fun computeDocExpirySeverity(dateStr: String): DocExpirySeverity {
        val clean = dateStr.trim()
        if (clean.isEmpty()) return DocExpirySeverity.NOT_SET
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(clean)
        }.getOrNull() ?: return DocExpirySeverity.NOT_SET
        val diffDays = ((parsed.time - System.currentTimeMillis()) / 86_400_000L)
        return when {
            diffDays < 0 -> DocExpirySeverity.EXPIRED
            diffDays <= 7 -> DocExpirySeverity.EXPIRING_SOON
            else -> DocExpirySeverity.VALID
        }
    }

    fun upsertRiderDocExpiry(
        context: Context,
        riderId: String,
        riderName: String,
        dlExpiry: String,
        rcExpiry: String,
        insuranceExpiry: String,
        autoHoldOnExpired: Boolean
    ): RiderDocExpiryRecord {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        val prev = _riderDocExpiries.value[riderId]
        val rec = RiderDocExpiryRecord(
            riderId = riderId,
            riderName = riderName.ifBlank { prev?.riderName ?: "Rider Partner" },
            dlExpiry = dlExpiry.trim(),
            rcExpiry = rcExpiry.trim(),
            insuranceExpiry = insuranceExpiry.trim(),
            autoHoldOnExpired = autoHoldOnExpired,
            alertSentAtLabel = prev?.alertSentAtLabel.orEmpty(),
            updatedAtEpochMs = now
        )
        synchronized(lock) {
            _riderDocExpiries.value = _riderDocExpiries.value + (riderId to rec)
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return rec
    }

    fun sendDocExpiryAlertByAdmin(context: Context, riderId: String, riderName: String): RiderDocExpiryRecord {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val prev = _riderDocExpiries.value[riderId] ?: RiderDocExpiryRecord(riderId = riderId, riderName = riderName)
        val next = prev.copy(alertSentAtLabel = "Alerted $label", updatedAtEpochMs = now)
        synchronized(lock) {
            _riderDocExpiries.value = _riderDocExpiries.value + (riderId to next)
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return next
    }

    fun sendBroadcastFromAdmin(
        context: Context,
        message: String,
        target: String = "ALL",
        repository: SharedOrderRepository? = null
    ) {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        val next = _remoteFeatureFlags.value.copy(
            adminBroadcastMessage = message.trim(),
            adminBroadcastTarget = target.trim().uppercase().ifBlank { "ALL" },
            adminBroadcastEpochMs = if (message.isBlank()) 0L else now,
            updatedAtEpochMs = now
        )
        updateRemoteFeatureFlagsFromAdmin(context, next, repository)
    }

    fun buildPersonalReferralCode(name: String, phone: String): String {
        val cleanName = name.uppercase(Locale.US).filter(Char::isLetter).take(3).ifBlank { "QD" }
        val digits = phone.filter(Char::isDigit).takeLast(4).ifBlank { "2026" }
        return "QD$cleanName$digits"
    }

    fun claimReferralBonus(context: Context, inviteCode: String, ownCode: String): Result<Int> {
        ensureLoaded(context)
        val clean = inviteCode.trim().uppercase().filter(Char::isLetterOrDigit)
        if (clean.length < 5) {
            return Result.failure(IllegalArgumentException("Enter a valid referral code (at least 5 characters)."))
        }
        if (clean == ownCode.trim().uppercase()) {
            return Result.failure(IllegalArgumentException("You cannot apply your own referral code."))
        }
        synchronized(lock) {
            if (clean in claimedReferralCodes) {
                return Result.failure(IllegalArgumentException("Referral code $clean has already been redeemed."))
            }
            val bonus = _remoteFeatureFlags.value.referralBonusInr.coerceIn(10, 500)
            claimedReferralCodes.add(clean)
            _customerWalletBonusInr.value += bonus
            persist(context)
            return Result.success(bonus)
        }
    }

    fun consumeWalletBonus(context: Context, amountUsedInr: Int) {
        ensureLoaded(context)
        if (amountUsedInr <= 0) return
        synchronized(lock) {
            _customerWalletBonusInr.value = (_customerWalletBonusInr.value - amountUsedInr).coerceAtLeast(0)
            persist(context)
        }
    }

    // ---------------- WAIT TIMER & LIVE RIDER GPS ETA ----------------
    data class WaitTimerFeeInfo(
        val startedAtMs: Long,
        val elapsedSeconds: Long,
        val freeSeconds: Long,
        val overtimeMinutes: Int,
        val waitFeeInr: Int
    )

    fun startWaitTimerAtPickup(
        context: Context,
        repository: SharedOrderRepository,
        orderId: String
    ): Long {
        ensureLoaded(context)
        val startedAt = synchronized(lock) {
            val existing = _waitTimerStartByOrder.value[orderId]
            if (existing != null && existing > 0L) {
                existing
            } else {
                val now = System.currentTimeMillis()
                _waitTimerStartByOrder.value = _waitTimerStartByOrder.value + (orderId to now)
                persist(context)
                now
            }
        }
        getMutableOrdersFlow(repository)?.let { flow ->
            flow.value = flow.value.map { ord ->
                if (ord.orderId == orderId && ord.orderStatus == OrderStatus.RIDER_ASSIGNED) {
                    ord.copy(
                        orderStatus = OrderStatus.ARRIVED_AT_PICKUP,
                        latestRiderQuickNote = "Rider reached pickup gate — Wait timer started"
                    )
                } else ord
            }
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return startedAt
    }

    fun clearWaitTimerForOrder(context: Context, orderId: String) {
        ensureLoaded(context)
        synchronized(lock) {
            if (_waitTimerStartByOrder.value.containsKey(orderId)) {
                _waitTimerStartByOrder.value = _waitTimerStartByOrder.value - orderId
                persist(context)
            }
        }
    }

    fun computeWaitFeeInfo(
        orderId: String,
        flags: AdminRemoteFeatureFlags,
        nowMs: Long = System.currentTimeMillis()
    ): WaitTimerFeeInfo? {
        val startedAt = _waitTimerStartByOrder.value[orderId] ?: return null
        if (startedAt <= 0L) return null
        val elapsedSec = ((nowMs - startedAt) / 1000L).coerceAtLeast(0L)
        val freeSec = (flags.freeWaitMinutes.coerceIn(1, 20) * 60L)
        val extraSec = (elapsedSec - freeSec).coerceAtLeast(0L)
        val overtimeMins = if (extraSec > 0L) ((extraSec + 59L) / 60L).toInt() else 0
        val waitFee = overtimeMins * flags.waitFeePerMinuteInr.coerceIn(1, 50)
        return WaitTimerFeeInfo(
            startedAtMs = startedAt,
            elapsedSeconds = elapsedSec,
            freeSeconds = freeSec,
            overtimeMinutes = overtimeMins,
            waitFeeInr = waitFee
        )
    }

    fun updateRiderCoordsForOrder(
        context: Context,
        orderId: String,
        lat: Double,
        lng: Double
    ) {
        if (lat == 0.0 && lng == 0.0) return
        ensureLoaded(context)
        synchronized(lock) {
            val prev = _riderLiveCoordsByOrder.value[orderId]
            if (prev == null || kotlin.math.abs(prev.first - lat) > 0.00005 || kotlin.math.abs(prev.second - lng) > 0.00005) {
                _riderLiveCoordsByOrder.value = _riderLiveCoordsByOrder.value + (orderId to (lat to lng))
                persist(context)
            }
        }
    }

    fun haversineDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if ((lat1 == 0.0 && lng1 == 0.0) || (lat2 == 0.0 && lng2 == 0.0)) return -1.0
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    fun computeLiveRiderEtaKmAndMins(order: CustomerOrder): Pair<Double, Int>? {
        val riderCoords = _riderLiveCoordsByOrder.value[order.orderId] ?: return null
        val targetLat = if (order.orderStatus == OrderStatus.PICKED_UP || order.orderStatus == OrderStatus.IN_TRANSIT) {
            order.dropAddress.lat
        } else {
            order.pickupAddress.lat
        }
        val targetLng = if (order.orderStatus == OrderStatus.PICKED_UP || order.orderStatus == OrderStatus.IN_TRANSIT) {
            order.dropAddress.lng
        } else {
            order.pickupAddress.lng
        }
        val dist = haversineDistanceKm(riderCoords.first, riderCoords.second, targetLat, targetLng)
        if (dist < 0.0 || dist > 120.0) return null
        val etaMins = kotlin.math.ceil((dist / 24.0) * 60.0).toInt().coerceIn(1, 90)
        return dist to etaMins
    }

    // ---------------- SERVER LIVE-SYNC MERGE & EXPORT ----------------
    fun exportOpsPayloadJson(): JSONObject = JSONObject().apply {
        put("promoCoupons", JSONArray().apply { _promoCoupons.value.forEach { put(it.toJson()) } })
        put("withdrawalRequests", JSONArray().apply { _withdrawalRequests.value.forEach { put(it.toJson()) } })
        put("supportTickets", JSONArray().apply { _supportTickets.value.forEach { put(it.toJson()) } })
        put("codSettlements", JSONArray().apply { _codSettlements.value.forEach { put(it.toJson()) } })
        put("disputeRecords", JSONArray().apply { _disputeRecords.value.forEach { put(it.toJson()) } })
        put("riderDocExpiries", JSONArray().apply { _riderDocExpiries.value.values.forEach { put(it.toJson()) } })
        if (_remoteFeatureFlags.value.updatedAtEpochMs > 0L) {
            put("remoteFeatureFlags", _remoteFeatureFlags.value.toJson())
        }
        put("waitTimerStartByOrder", JSONObject().apply {
            _waitTimerStartByOrder.value.forEach { (k, v) -> put(k, v) }
        })
        put("riderLiveCoordsByOrder", JSONObject().apply {
            _riderLiveCoordsByOrder.value.forEach { (k, pair) ->
                put(k, JSONObject().apply {
                    put("lat", pair.first)
                    put("lng", pair.second)
                })
            }
        })
        put("orderTimelineEvents", JSONArray().apply {
            _timelineEventsByOrder.value.values.flatten().takeLast(300).forEach { put(it.toJson()) }
        })
        put("unreachableByOrder", JSONArray().apply {
            _unreachableByOrder.value.values.forEach { put(it.toJson()) }
        })
        put("b2bAccounts", JSONArray().apply {
            _b2bAccounts.value.forEach { put(it.toJson()) }
        })
    }

    fun mergeFromServerPayload(context: Context, root: JSONObject) {
        ensureLoaded(context)
        var changed = false
        var flagsUpdated: AdminRemoteFeatureFlags? = null
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

            root.optJSONArray("codSettlements")?.let { arr ->
                val merged = _codSettlements.value.associateBy { it.settlementId }.toMutableMap()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)?.let(RiderCodSettlementRecord::fromJson) ?: continue
                    val prev = merged[item.settlementId]
                    if (prev == null || item.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[item.settlementId] = item
                        changed = true
                    }
                }
                _codSettlements.value = merged.values.sortedByDescending { it.updatedAtEpochMs }
            }

            root.optJSONArray("disputeRecords")?.let { arr ->
                val merged = _disputeRecords.value.associateBy { it.disputeId }.toMutableMap()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)?.let(OrderDisputeRefundRecord::fromJson) ?: continue
                    val prev = merged[item.disputeId]
                    if (prev == null || item.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[item.disputeId] = item
                        changed = true
                    }
                    if (item.status == DisputeStatus.REFUNDED &&
                        item.refundAmountInr > 0.0 &&
                        item.disputeId !in creditedDisputeIds &&
                        (activeCustomerAccountKey.isNotBlank() &&
                            item.customerPhone.filter(Char::isDigit).takeLast(10) == activeCustomerAccountKey)
                    ) {
                        creditedDisputeIds.add(item.disputeId)
                        _customerWalletBonusInr.value += item.refundAmountInr.toInt()
                        changed = true
                    }
                }
                _disputeRecords.value = merged.values.sortedByDescending { it.updatedAtEpochMs }
            }

            root.optJSONArray("riderDocExpiries")?.let { arr ->
                val merged = _riderDocExpiries.value.toMutableMap()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)?.let(RiderDocExpiryRecord::fromJson) ?: continue
                    val prev = merged[item.riderId]
                    if (prev == null || item.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[item.riderId] = item
                        changed = true
                    }
                }
                _riderDocExpiries.value = merged
            }

            root.optJSONObject("remoteFeatureFlags")?.let { flagsObj ->
                val incoming = AdminRemoteFeatureFlags.fromJson(flagsObj)
                val prev = _remoteFeatureFlags.value
                if (incoming.updatedAtEpochMs > prev.updatedAtEpochMs) {
                    _remoteFeatureFlags.value = incoming
                    flagsUpdated = incoming
                    changed = true
                }
            }

            root.optJSONObject("serverAppUpdate")?.let { updObj ->
                ServerAppUpdateManifest.fromJson(updObj)?.let { manifest ->
                    if (manifest != _serverAppUpdate.value) {
                        _serverAppUpdate.value = manifest
                        changed = true
                    }
                }
            }

            root.optJSONObject("waitTimerStartByOrder")?.let { wtObj ->
                val merged = _waitTimerStartByOrder.value.toMutableMap()
                val keys = wtObj.keys()
                while (keys.hasNext()) {
                    val oid = keys.next()
                    val ts = wtObj.optLong(oid, 0L)
                    if (ts > 0L && (!merged.containsKey(oid) || ts < (merged[oid] ?: Long.MAX_VALUE))) {
                        merged[oid] = ts
                        changed = true
                    }
                }
                _waitTimerStartByOrder.value = merged
            }

            root.optJSONObject("riderLiveCoordsByOrder")?.let { rcObj ->
                val merged = _riderLiveCoordsByOrder.value.toMutableMap()
                val keys = rcObj.keys()
                while (keys.hasNext()) {
                    val oid = keys.next()
                    val pairObj = rcObj.optJSONObject(oid) ?: continue
                    val lat = pairObj.optDouble("lat", 0.0)
                    val lng = pairObj.optDouble("lng", 0.0)
                    if (lat != 0.0 || lng != 0.0) {
                        merged[oid] = lat to lng
                        changed = true
                    }
                }
                _riderLiveCoordsByOrder.value = merged
            }

            root.optJSONArray("orderTimelineEvents")?.let { arr ->
                val merged = _timelineEventsByOrder.value.mapValues { it.value.toMutableList() }.toMutableMap()
                for (i in 0 until arr.length()) {
                    val evt = arr.optJSONObject(i)?.let(OrderTimelineEvent::fromJson) ?: continue
                    val list = merged.getOrPut(evt.orderId) { mutableListOf() }
                    if (list.none { it.eventId == evt.eventId }) {
                        list.add(evt)
                        changed = true
                    }
                }
                _timelineEventsByOrder.value = merged.mapValues { (_, list) -> list.sortedBy { it.createdAtEpochMs } }
            }

            root.optJSONArray("unreachableByOrder")?.let { arr ->
                val merged = _unreachableByOrder.value.toMutableMap()
                for (i in 0 until arr.length()) {
                    val rec = arr.optJSONObject(i)?.let(CustomerUnreachableRecord::fromJson) ?: continue
                    val prev = merged[rec.orderId]
                    if (prev == null || rec.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[rec.orderId] = rec
                        changed = true
                    }
                }
                _unreachableByOrder.value = merged
            }

            root.optJSONArray("b2bAccounts")?.let { arr ->
                val merged = _b2bAccounts.value.associateBy { it.accountId }.toMutableMap()
                for (i in 0 until arr.length()) {
                    val acc = arr.optJSONObject(i)?.let(B2bDukaanAccountRecord::fromJson) ?: continue
                    val prev = merged[acc.accountId]
                    if (prev == null || acc.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                        merged[acc.accountId] = acc
                        changed = true
                    }
                }
                _b2bAccounts.value = merged.values.sortedByDescending { it.updatedAtEpochMs }
            }

            if (changed) persist(context)
        }
        flagsUpdated?.let { applyPricingOverridesFromFlags(it, SharedOrderRepository.getInstance(context)) }
    }

    fun updateServerAppManifest(context: Context, json: JSONObject): ServerAppUpdateManifest? {
        ensureLoaded(context)
        val manifest = ServerAppUpdateManifest.fromJson(json) ?: return null
        synchronized(lock) {
            if (manifest != _serverAppUpdate.value) {
                _serverAppUpdate.value = manifest
                persist(context)
            }
        }
        return manifest
    }

    // ---------------- B2B / DUKAAN MONTHLY POSTPAID ACCOUNTS & GST INVOICING ----------------
    fun registerOrUpdateB2bDukaanRequest(
        context: Context,
        shopName: String,
        ownerName: String,
        ownerPhone: String,
        gstin: String,
        pickupAddress: String
    ): B2bDukaanAccountRecord {
        ensureLoaded(context)
        val cleanPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(ownerPhone)
        val cleanShop = com.example.security.QuickDeliverySecurityGuard.sanitizeText(shopName, 100)
            .ifBlank { "Local Dukaan Partner" }
        val cleanOwner = com.example.security.QuickDeliverySecurityGuard.sanitizeText(ownerName, 80)
            .ifBlank { "Store Owner" }
        val cleanGstin = gstin.trim().uppercase().replace("[^A-Z0-9\\-]".toRegex(), "").ifBlank { "URD" }.take(20)
        val cleanAddr = com.example.security.QuickDeliverySecurityGuard.sanitizeText(pickupAddress, 300)
        val now = System.currentTimeMillis()
        val id = "B2B-${cleanPhone.takeLast(4).ifBlank { "0000" }}-${(now % 10000).toString().padStart(4, '0')}"
        val existing = _b2bAccounts.value.firstOrNull { it.ownerPhone == cleanPhone }
        val record = if (existing != null) {
            existing.copy(
                shopName = cleanShop,
                ownerName = cleanOwner,
                gstin = cleanGstin,
                pickupAddress = cleanAddr,
                updatedAtEpochMs = now
            )
        } else {
            B2bDukaanAccountRecord(
                accountId = id,
                shopName = cleanShop,
                ownerName = cleanOwner,
                ownerPhone = cleanPhone,
                gstin = cleanGstin,
                pickupAddress = cleanAddr,
                status = B2bDukaanStatus.PENDING_ADMIN_APPROVAL,
                approvedCreditLimitInr = 0,
                usedCreditInr = 0.0,
                totalBilledOrders = 0,
                gstInvoiceNumber = "INV-B2B-${SimpleDateFormat("yyyyMM", Locale.US).format(Date(now))}-${cleanPhone.takeLast(4)}",
                updatedAtEpochMs = now
            )
        }
        synchronized(lock) {
            val rest = _b2bAccounts.value.filterNot { it.accountId == record.accountId }
            _b2bAccounts.value = listOf(record) + rest
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return record
    }

    fun approveOrManageB2bAccountByAdmin(
        context: Context,
        accountId: String,
        newStatus: B2bDukaanStatus,
        creditLimitInr: Int,
        settleMonthlyBill: Boolean = false
    ): B2bDukaanAccountRecord? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        val settledLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(now))
        var updatedRec: B2bDukaanAccountRecord? = null
        synchronized(lock) {
            _b2bAccounts.value = _b2bAccounts.value.map { acc ->
                if (acc.accountId == accountId) {
                    val nextInv = if (settleMonthlyBill) {
                        "INV-B2B-${SimpleDateFormat("yyyyMM", Locale.US).format(Date(now))}-${(now % 10000).toString().padStart(4, '0')}"
                    } else acc.gstInvoiceNumber
                    acc.copy(
                        status = newStatus,
                        approvedCreditLimitInr = creditLimitInr.coerceIn(0, 1000000),
                        usedCreditInr = if (settleMonthlyBill) 0.0 else acc.usedCreditInr,
                        lastSettledLabel = if (settleMonthlyBill) "Paid $settledLabel" else acc.lastSettledLabel,
                        gstInvoiceNumber = nextInv,
                        updatedAtEpochMs = now
                    ).also { updatedRec = it }
                } else acc
            }
            persist(context)
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return updatedRec
    }

    fun chargeOrderToB2bPostpaid(
        context: Context,
        accountId: String,
        amountInr: Double
    ): Result<B2bDukaanAccountRecord> {
        ensureLoaded(context)
        if (amountInr <= 0.0) return Result.failure(IllegalArgumentException("Amount must be positive"))
        synchronized(lock) {
            val acc = _b2bAccounts.value.firstOrNull { it.accountId == accountId }
                ?: return Result.failure(IllegalStateException("B2B Dukaan account not found."))
            if (acc.status != B2bDukaanStatus.APPROVED) {
                return Result.failure(IllegalStateException("B2B account is awaiting Admin approval."))
            }
            if (acc.usedCreditInr + amountInr > acc.approvedCreditLimitInr.toDouble()) {
                return Result.failure(IllegalStateException("Monthly credit limit ₹${acc.approvedCreditLimitInr} exceeded."))
            }
            val updated = acc.copy(
                usedCreditInr = acc.usedCreditInr + amountInr,
                totalBilledOrders = acc.totalBilledOrders + 1,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            _b2bAccounts.value = _b2bAccounts.value.map { if (it.accountId == accountId) updated else it }
            persist(context)
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
            return Result.success(updated)
        }
    }

    fun mergeB2bAccountsFromCloud(context: Context, cloudList: List<CloudB2bDukaanAccount>) {
        if (cloudList.isEmpty()) return
        ensureLoaded(context)
        var changed = false
        synchronized(lock) {
            val merged = _b2bAccounts.value.associateBy { it.accountId }.toMutableMap()
            cloudList.forEach { cloud ->
                val mapped = B2bDukaanAccountRecord(
                    accountId = cloud.accountId,
                    shopName = cloud.shopName,
                    ownerName = cloud.ownerName,
                    ownerPhone = cloud.ownerPhone,
                    gstin = cloud.gstin,
                    pickupAddress = cloud.pickupAddress,
                    status = B2bDukaanStatus.fromString(cloud.status),
                    approvedCreditLimitInr = cloud.approvedCreditLimitInr,
                    usedCreditInr = cloud.usedCreditPaise / 100.0,
                    totalBilledOrders = cloud.totalBilledOrders,
                    gstInvoiceNumber = cloud.gstInvoiceNumber,
                    lastSettledLabel = cloud.lastSettledEpochMs?.let { ts ->
                        "Paid ${SimpleDateFormat("dd MMM", Locale.US).format(Date(ts))}"
                    } ?: "",
                    updatedAtEpochMs = cloud.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                )
                val prev = merged[mapped.accountId]
                if (prev == null || mapped.updatedAtEpochMs >= prev.updatedAtEpochMs) {
                    merged[mapped.accountId] = mapped
                    changed = true
                }
            }
            if (changed) {
                _b2bAccounts.value = merged.values.sortedByDescending { it.updatedAtEpochMs }
                persist(context)
            }
        }
    }

    fun buildB2bConsolidatedGstInvoiceText(account: B2bDukaanAccountRecord): String {
        val total = account.usedCreditInr.coerceAtLeast(0.0)
        val taxableBase = total / 1.18
        val cgst = (total - taxableBase) / 2.0
        val sgst = (total - taxableBase) / 2.0
        return buildString {
            appendLine("=== QUICK DELIVERY LOGISTICS • CONSOLIDATED B2B TAX INVOICE ===")
            appendLine("Invoice No: ${account.gstInvoiceNumber} | HSN/SAC: 996812 (Courier Services)")
            appendLine("B2B Account: ${account.shopName} (${account.accountId})")
            appendLine("GSTIN / URD: ${account.gstin} | Owner: ${account.ownerName} (${account.ownerPhone})")
            appendLine("Pickup Hub: ${account.pickupAddress}")
            appendLine("------------------------------------------------------------")
            appendLine("Billed Orders This Cycle : ${account.totalBilledOrders}")
            appendLine("Approved Credit Limit    : ₹${account.approvedCreditLimitInr}")
            appendLine(String.format(Locale.US, "Taxable Value            : ₹%.2f", taxableBase))
            appendLine(String.format(Locale.US, "CGST @ 9%%                : ₹%.2f", cgst))
            appendLine(String.format(Locale.US, "SGST @ 9%%                : ₹%.2f", sgst))
            appendLine(String.format(Locale.US, "TOTAL POSTPAID DUE       : ₹%.2f", total))
            if (account.lastSettledLabel.isNotBlank()) {
                appendLine("Last Settlement          : ${account.lastSettledLabel}")
            }
            appendLine("Billing & Support        : ${QuickDeliveryBrandLinks.PRIMARY_DOMAIN} | ${QuickDeliveryBrandLinks.SUPPORT_EMAIL}")
            appendLine("============================================================")
        }
    }

    // ---------------- ORDER TIMELINE, UNREACHABLE LOGGING & FORCE RELEASE ----------------
    fun recordOrderTimelineEvent(
        context: Context,
        orderId: String,
        title: String,
        detail: String,
        actorRole: String
    ) {
        if (orderId.isBlank() || title.isBlank()) return
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.US).format(Date(now))
        synchronized(lock) {
            val currentList = _timelineEventsByOrder.value[orderId].orEmpty()
            val recentDuplicate = currentList.any {
                it.title == title && kotlin.math.abs(now - it.createdAtEpochMs) < 4000L
            }
            if (!recentDuplicate) {
                val evt = OrderTimelineEvent(
                    eventId = "EVT-${orderId}-${now % 1000000}-${currentList.size}",
                    orderId = orderId,
                    title = title,
                    detail = detail.trim(),
                    actorRole = actorRole,
                    timestampLabel = label,
                    createdAtEpochMs = now
                )
                _timelineEventsByOrder.value = _timelineEventsByOrder.value + (orderId to (currentList + evt).sortedBy { it.createdAtEpochMs })
                persist(context)
            }
        }
    }

    fun buildCombinedTimelineForOrder(
        order: CustomerOrder,
        pickupProof: com.example.data.model.PickupProof? = null,
        deliveryProof: com.example.data.model.DeliveryProof? = null
    ): List<OrderTimelineEvent> {
        val baseEvents = mutableListOf<OrderTimelineEvent>()
        val createdBaseMs: Long = System.currentTimeMillis() - 600_000L
        baseEvents.add(
            OrderTimelineEvent(
                eventId = "auto_booked_${order.orderId}",
                orderId = order.orderId,
                title = "Order Booked (${order.paymentMethod.shortBadge})",
                detail = "${order.customerName} • ${order.vehicleType.title} • ${order.totalAmount}",
                actorRole = "CUSTOMER",
                timestampLabel = order.createdAt.ifBlank { SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(createdBaseMs)) },
                createdAtEpochMs = createdBaseMs
            )
        )
        if (order.rejectedByRiderIds.isNotEmpty()) {
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_declined_${order.orderId}_${order.rejectedByRiderIds.size}",
                    orderId = order.orderId,
                    title = "Declined by ${order.rejectedByRiderIds.size} Rider(s)",
                    detail = order.latestRiderQuickNote ?: "Re-queued automatically to nearby online riders",
                    actorRole = "RIDER",
                    timestampLabel = "Re-dispatched",
                    createdAtEpochMs = createdBaseMs + 20_000L
                )
            )
        }
        if (!order.assignedRiderName.isNullOrBlank()) {
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_assigned_${order.orderId}_${order.assignedRiderId}",
                    orderId = order.orderId,
                    title = "Rider Assigned: ${order.assignedRiderName}",
                    detail = order.assignedRiderVehicle ?: "Verified Partner",
                    actorRole = "RIDER",
                    timestampLabel = "Assigned",
                    createdAtEpochMs = createdBaseMs + 40_000L
                )
            )
        }
        _waitTimerStartByOrder.value[order.orderId]?.let { wtMs ->
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_wait_${order.orderId}",
                    orderId = order.orderId,
                    title = "Reached Pickup Gate & Wait Timer Started",
                    detail = "Pickup gate check-in timer active",
                    actorRole = "RIDER",
                    timestampLabel = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(wtMs)),
                    createdAtEpochMs = wtMs
                )
            )
        }
        _unreachableByOrder.value[order.orderId]?.let { unrec ->
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_unreach_${order.orderId}",
                    orderId = order.orderId,
                    title = "Customer Unreachable Logged",
                    detail = "${unrec.riderName}: ${unrec.note}",
                    actorRole = "RIDER",
                    timestampLabel = unrec.reportedAtLabel,
                    createdAtEpochMs = unrec.updatedAtEpochMs
                )
            )
        }
        if (pickupProof != null) {
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_pickup_${order.orderId}",
                    orderId = order.orderId,
                    title = "Parcel Picked Up (OTP + Photo Verified)",
                    detail = pickupProof.packagePhotoSummary.ifBlank { "Verified by ${pickupProof.riderName}" },
                    actorRole = "RIDER",
                    timestampLabel = pickupProof.capturedAt,
                    createdAtEpochMs = createdBaseMs + 120_000L
                )
            )
        } else if (order.orderStatus == OrderStatus.PICKED_UP || order.orderStatus == OrderStatus.IN_TRANSIT) {
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_transit_${order.orderId}",
                    orderId = order.orderId,
                    title = "In Transit to Drop Location",
                    detail = "Heading toward ${order.dropAddress.fullAddress}",
                    actorRole = "RIDER",
                    timestampLabel = "In Progress",
                    createdAtEpochMs = createdBaseMs + 130_000L
                )
            )
        }
        if (deliveryProof != null || order.orderStatus == OrderStatus.DELIVERED) {
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_delivered_${order.orderId}",
                    orderId = order.orderId,
                    title = "Delivered & Drop OTP Verified",
                    detail = deliveryProof?.deliveryPhotoSummary ?: "Signed by ${order.dropAddress.contactName.ifBlank { order.customerName }}",
                    actorRole = "RIDER",
                    timestampLabel = deliveryProof?.capturedAt ?: "Delivered",
                    createdAtEpochMs = createdBaseMs + 240_000L
                )
            )
        } else if (order.orderStatus == OrderStatus.CANCELLED) {
            baseEvents.add(
                OrderTimelineEvent(
                    eventId = "auto_cancelled_${order.orderId}",
                    orderId = order.orderId,
                    title = "Order Cancelled",
                    detail = order.latestRiderQuickNote ?: "Cancelled before delivery",
                    actorRole = "SYSTEM",
                    timestampLabel = "Cancelled",
                    createdAtEpochMs = createdBaseMs + 250_000L
                )
            )
        }

        val explicit = _timelineEventsByOrder.value[order.orderId].orEmpty()
        val combined = (baseEvents + explicit)
            .distinctBy { "${it.title}_${it.timestampLabel}" }
            .sortedBy { it.createdAtEpochMs }
        return combined
    }

    fun reportCustomerUnreachable(
        context: Context,
        repository: SharedOrderRepository,
        orderId: String,
        riderId: String,
        riderName: String
    ): CustomerUnreachableRecord {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(now))
        val rec = CustomerUnreachableRecord(
            orderId = orderId,
            riderId = riderId,
            riderName = riderName.ifBlank { "Rider Partner" },
            reportedAtLabel = label,
            note = "Rider called & waited at gate — customer unreachable ($label)",
            updatedAtEpochMs = now
        )
        synchronized(lock) {
            _unreachableByOrder.value = _unreachableByOrder.value + (orderId to rec)
            persist(context)
        }
        recordOrderTimelineEvent(
            context = context,
            orderId = orderId,
            title = "Customer Unreachable Logged",
            detail = "${rec.riderName} logged call & gate wait attempt at $label",
            actorRole = "RIDER"
        )
        getMutableOrdersFlow(repository)?.let { flow ->
            flow.value = flow.value.map { ord ->
                if (ord.orderId == orderId) {
                    ord.copy(latestRiderQuickNote = "📵 Customer Unreachable (${rec.riderName}) logged at $label")
                } else ord
            }
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        return rec
    }

    fun markRiderReachedPickupGate(
        context: Context,
        repository: SharedOrderRepository,
        orderId: String,
        riderName: String
    ): Boolean {
        val flow = getMutableOrdersFlow(repository) ?: return false
        var updated = false
        flow.value = flow.value.map { ord ->
            if (ord.orderId == orderId && ord.orderStatus == OrderStatus.RIDER_ASSIGNED) {
                updated = true
                ord.copy(
                    orderStatus = OrderStatus.ARRIVED_AT_PICKUP,
                    latestRiderQuickNote = "📍 $riderName arrived at Pickup Gate (Within 100m check-in)"
                )
            } else ord
        }
        if (updated) {
            recordOrderTimelineEvent(
                context = context,
                orderId = orderId,
                title = "Rider Reached Pickup Gate",
                detail = "$riderName checked in at pickup location",
                actorRole = "RIDER"
            )
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        return updated
    }

    fun adminForceReleaseOrderToSearching(
        context: Context,
        repository: SharedOrderRepository,
        orderId: String,
        reason: String = "Emergency Manual Release by Admin"
    ): Boolean {
        val flow = getMutableOrdersFlow(repository) ?: return false
        var released = false
        var previousRiderId: String? = null
        flow.value = flow.value.map { ord ->
            if (ord.orderId == orderId &&
                ord.orderStatus != OrderStatus.PICKED_UP &&
                ord.orderStatus != OrderStatus.IN_TRANSIT &&
                ord.orderStatus != OrderStatus.DELIVERED &&
                ord.orderStatus != OrderStatus.CANCELLED
            ) {
                released = true
                previousRiderId = ord.assignedRiderId
                val updatedRejected = if (!ord.assignedRiderId.isNullOrBlank()) {
                    (ord.rejectedByRiderIds + ord.assignedRiderId!!).distinct()
                } else {
                    ord.rejectedByRiderIds
                }
                ord.copy(
                    orderStatus = OrderStatus.SEARCHING_RIDER,
                    assignedRiderId = null,
                    assignedRiderName = null,
                    assignedRiderPhone = null,
                    assignedRiderVehicle = null,
                    rejectedByRiderIds = updatedRejected,
                    latestRiderQuickNote = "Admin Force-Released ($reason) — Searching new rider..."
                )
            } else ord
        }
        if (released) {
            clearWaitTimerForOrder(context, orderId)
            recordOrderTimelineEvent(
                context = context,
                orderId = orderId,
                title = "Admin Force-Released to Queue",
                detail = "Released from ${previousRiderId ?: "queue"} ($reason) — Re-searching nearby riders",
                actorRole = "ADMIN"
            )
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        return released
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
            recordOrderTimelineEvent(
                context = context,
                orderId = orderId,
                title = "Cancelled by $cancelledByRole",
                detail = cleanReason,
                actorRole = cancelledByRole.uppercase()
            )
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
            recordOrderTimelineEvent(
                context = context,
                orderId = orderId,
                title = "Admin Reassigned Rider",
                detail = "Assigned directly to ${targetRider.name} (${targetRider.riderId})",
                actorRole = "ADMIN"
            )
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        return found
    }
}

