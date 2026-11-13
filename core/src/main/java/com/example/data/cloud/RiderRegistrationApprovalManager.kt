package com.example.data.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.RiderProfile
import com.example.data.model.RiderShiftStatus
import com.example.data.model.RiderVerificationStatus
import com.example.data.model.RiderWorkMode
import com.example.data.model.VehicleType
import com.example.data.repository.SharedOrderRepository
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class RiderApprovalWorkflowStatus {
    NEEDS_KYC,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED;

    companion object {
        fun fromString(raw: String?): RiderApprovalWorkflowStatus = when (raw?.trim()?.uppercase()) {
            "PENDING_APPROVAL", "PENDING" -> PENDING_APPROVAL
            "APPROVED", "VERIFIED" -> APPROVED
            "REJECTED" -> REJECTED
            else -> NEEDS_KYC
        }
    }
}

data class RiderRegistrationRecord(
    val riderKey: String,
    val riderId: String,
    val name: String,
    val phone: String,
    val email: String,
    val vehicleType: String = VehicleType.BIKE.name,
    val vehicleNumber: String = "",
    val aadhaarNumber: String = "",
    val panNumber: String = "",
    val drivingLicenseNumber: String = "",
    val aadhaarImageBase64: String = "",
    val panImageBase64: String = "",
    val dlImageBase64: String = "",
    val rcImageBase64: String = "",
    val selfieImageBase64: String = "",
    val approvalStatus: RiderApprovalWorkflowStatus = RiderApprovalWorkflowStatus.NEEDS_KYC,
    val rejectionReason: String = "",
    val submittedAtEpochMs: Long = 0L,
    val submittedAtLabel: String = "",
    val approvedAtEpochMs: Long = 0L,
    val reLoggedInAfterApproval: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val requiresReLoginAfterApproval: Boolean
        get() = approvalStatus == RiderApprovalWorkflowStatus.APPROVED && !reLoggedInAfterApproval

    fun toJson(): JSONObject = JSONObject().apply {
        put("riderKey", riderKey)
        put("riderId", riderId)
        put("name", name)
        put("phone", phone)
        put("email", email)
        put("vehicleType", vehicleType)
        put("vehicleNumber", vehicleNumber)
        put("aadhaarNumber", aadhaarNumber)
        put("panNumber", panNumber)
        put("drivingLicenseNumber", drivingLicenseNumber)
        put("aadhaarImageBase64", aadhaarImageBase64)
        put("panImageBase64", panImageBase64)
        put("dlImageBase64", dlImageBase64)
        put("rcImageBase64", rcImageBase64)
        put("selfieImageBase64", selfieImageBase64)
        put("approvalStatus", approvalStatus.name)
        put("rejectionReason", rejectionReason)
        put("submittedAtEpochMs", submittedAtEpochMs)
        put("submittedAtLabel", submittedAtLabel)
        put("approvedAtEpochMs", approvedAtEpochMs)
        put("reLoggedInAfterApproval", reLoggedInAfterApproval)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): RiderRegistrationRecord? {
            val key = obj.optString("riderKey", "").trim()
            if (key.isEmpty()) return null
            return RiderRegistrationRecord(
                riderKey = key,
                riderId = obj.optString("riderId", "RIDER-REG-${key.takeLast(4)}"),
                name = obj.optString("name", "Rider Partner"),
                phone = obj.optString("phone", ""),
                email = obj.optString("email", ""),
                vehicleType = obj.optString("vehicleType", VehicleType.BIKE.name),
                vehicleNumber = obj.optString("vehicleNumber", ""),
                aadhaarNumber = obj.optString("aadhaarNumber", ""),
                panNumber = obj.optString("panNumber", ""),
                drivingLicenseNumber = obj.optString("drivingLicenseNumber", ""),
                aadhaarImageBase64 = obj.optString("aadhaarImageBase64", ""),
                panImageBase64 = obj.optString("panImageBase64", ""),
                dlImageBase64 = obj.optString("dlImageBase64", ""),
                rcImageBase64 = obj.optString("rcImageBase64", ""),
                selfieImageBase64 = obj.optString("selfieImageBase64", ""),
                approvalStatus = RiderApprovalWorkflowStatus.fromString(obj.optString("approvalStatus")),
                rejectionReason = obj.optString("rejectionReason", ""),
                submittedAtEpochMs = obj.optLong("submittedAtEpochMs", 0L),
                submittedAtLabel = obj.optString("submittedAtLabel", ""),
                approvedAtEpochMs = obj.optLong("approvedAtEpochMs", 0L),
                reLoggedInAfterApproval = obj.optBoolean("reLoggedInAfterApproval", false),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Manages end-to-end first-time Rider KYC onboarding, local persistence,
 * live cross-phone Admin approval sync, and mandatory post-approval re-login before going Online.
 */
object RiderRegistrationApprovalManager {
    private const val PREFS_NAME = "quick_delivery_rider_kyc_approval_prefs"
    private const val KEY_REGISTRATIONS_JSON = "registrations_json_v1"

    private val lock = Any()
    private var loaded = false

    private val _registrationsByKey = MutableStateFlow<Map<String, RiderRegistrationRecord>>(emptyMap())
    val registrationsByKey: StateFlow<Map<String, RiderRegistrationRecord>> = _registrationsByKey.asStateFlow()

    fun buildRiderKey(phone: String?, email: String?): String {
        val cleanPhone = phone.orEmpty().filter(Char::isDigit).takeLast(10)
        if (cleanPhone.length == 10) return "phone_$cleanPhone"
        val cleanEmail = email.orEmpty().trim().lowercase()
        if (cleanEmail.isNotBlank()) return "email_$cleanEmail"
        return ""
    }

    fun ensureLoaded(context: Context) {
        synchronized(lock) {
            if (loaded) return
            loaded = true
            runCatching {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val raw = prefs.getString(KEY_REGISTRATIONS_JSON, null) ?: return@runCatching
                val arr = JSONArray(raw)
                val map = mutableMapOf<String, RiderRegistrationRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rec = RiderRegistrationRecord.fromJson(obj) ?: continue
                    map[rec.riderKey] = rec
                }
                _registrationsByKey.value = map
            }
        }
    }

    private fun persist(context: Context) {
        runCatching {
            val arr = JSONArray()
            _registrationsByKey.value.values.forEach { arr.put(it.toJson()) }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_REGISTRATIONS_JSON, arr.toString())
                .apply()
        }
    }

    fun compressBitmapToBase64(bitmap: Bitmap?, maxDimPx: Int = 260, quality: Int = 52): String {
        if (bitmap == null) return ""
        return runCatching {
            val w = bitmap.width.coerceAtLeast(1)
            val h = bitmap.height.coerceAtLeast(1)
            val scale = minOf(maxDimPx.toFloat() / w, maxDimPx.toFloat() / h, 1f)
            val scaled = if (scale < 0.99f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }.getOrDefault("")
    }

    fun decodeBase64ToBitmap(encoded: String?): Bitmap? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun getRecordForIdentity(context: Context, phone: String?, email: String?): RiderRegistrationRecord? {
        ensureLoaded(context)
        val phoneKey = buildRiderKey(phone, null)
        val emailKey = buildRiderKey(null, email)
        val map = _registrationsByKey.value
        return map[phoneKey] ?: map[emailKey] ?: map.values.firstOrNull { rec ->
            (phoneKey.isNotBlank() && rec.phone.filter(Char::isDigit).takeLast(10) == phoneKey.removePrefix("phone_")) ||
                (emailKey.isNotBlank() && rec.email.trim().lowercase() == emailKey.removePrefix("email_"))
        }
    }

    /**
     * Called when a new Rider creates an account for the first time.
     * Creates a NEEDS_KYC record if no approved/pending record already exists.
     */
    fun onRiderNewSignUp(context: Context, name: String, phone: String, email: String): RiderRegistrationRecord {
        ensureLoaded(context)
        val key = buildRiderKey(phone, email).ifBlank { "phone_${System.currentTimeMillis()}" }
        synchronized(lock) {
            val existing = _registrationsByKey.value[key]
            if (existing != null &&
                (existing.approvalStatus == RiderApprovalWorkflowStatus.PENDING_APPROVAL ||
                    existing.approvalStatus == RiderApprovalWorkflowStatus.APPROVED)
            ) {
                return existing
            }
            val now = System.currentTimeMillis()
            val suffix = key.filter(Char::isDigit).takeLast(4).ifBlank { (now % 10000).toString() }
            val record = RiderRegistrationRecord(
                riderKey = key,
                riderId = existing?.riderId ?: "RIDER-KYC-$suffix",
                name = name.trim(),
                phone = phone.filter(Char::isDigit).takeLast(10),
                email = email.trim().lowercase(),
                approvalStatus = RiderApprovalWorkflowStatus.NEEDS_KYC,
                reLoggedInAfterApproval = false,
                updatedAtEpochMs = now
            )
            _registrationsByKey.value = _registrationsByKey.value + (key to record)
            persist(context)
            return record
        }
    }

    /**
     * Saves all 5 KYC documents + vehicle details and moves Rider to PENDING_APPROVAL waiting for Admin.
     */
    fun submitRiderKycDocuments(
        context: Context,
        name: String,
        phone: String,
        email: String,
        vehicleType: VehicleType,
        vehicleNumber: String,
        aadhaarNumber: String,
        panNumber: String,
        drivingLicenseNumber: String,
        aadhaarBitmap: Bitmap?,
        panBitmap: Bitmap?,
        dlBitmap: Bitmap?,
        rcBitmap: Bitmap?,
        selfieBitmap: Bitmap?
    ): RiderRegistrationRecord {
        ensureLoaded(context)
        val key = buildRiderKey(phone, email).ifBlank { "phone_${System.currentTimeMillis()}" }
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(now))
        synchronized(lock) {
            val existing = _registrationsByKey.value[key]
            val suffix = key.filter(Char::isDigit).takeLast(4).ifBlank { (now % 10000).toString() }
            val updated = RiderRegistrationRecord(
                riderKey = key,
                riderId = existing?.riderId ?: "RIDER-KYC-$suffix",
                name = name.trim().ifBlank { existing?.name ?: "Rider Partner" },
                phone = phone.filter(Char::isDigit).takeLast(10).ifBlank { existing?.phone.orEmpty() },
                email = email.trim().lowercase().ifBlank { existing?.email.orEmpty() },
                vehicleType = vehicleType.name,
                vehicleNumber = vehicleNumber.trim().uppercase(),
                aadhaarNumber = aadhaarNumber.trim(),
                panNumber = panNumber.trim().uppercase(),
                drivingLicenseNumber = drivingLicenseNumber.trim().uppercase(),
                aadhaarImageBase64 = compressBitmapToBase64(aadhaarBitmap).ifBlank { existing?.aadhaarImageBase64.orEmpty() },
                panImageBase64 = compressBitmapToBase64(panBitmap).ifBlank { existing?.panImageBase64.orEmpty() },
                dlImageBase64 = compressBitmapToBase64(dlBitmap).ifBlank { existing?.dlImageBase64.orEmpty() },
                rcImageBase64 = compressBitmapToBase64(rcBitmap).ifBlank { existing?.rcImageBase64.orEmpty() },
                selfieImageBase64 = compressBitmapToBase64(selfieBitmap, maxDimPx = 220, quality = 55)
                    .ifBlank { existing?.selfieImageBase64.orEmpty() },
                approvalStatus = RiderApprovalWorkflowStatus.PENDING_APPROVAL,
                rejectionReason = "",
                submittedAtEpochMs = now,
                submittedAtLabel = label,
                approvedAtEpochMs = 0L,
                reLoggedInAfterApproval = false,
                updatedAtEpochMs = now
            )
            _registrationsByKey.value = _registrationsByKey.value + (key to updated)
            persist(context)
            return updated
        }
    }

    /**
     * Called from Admin Console App to approve a pending Rider.
     */
    fun approveRiderByAdmin(context: Context, riderKey: String, adminNote: String = "Approved by Admin"): RiderRegistrationRecord? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        val updated: RiderRegistrationRecord? = synchronized(lock) {
            val existing = _registrationsByKey.value[riderKey] ?: return@synchronized null
            val next = existing.copy(
                approvalStatus = RiderApprovalWorkflowStatus.APPROVED,
                rejectionReason = "",
                approvedAtEpochMs = now,
                reLoggedInAfterApproval = false,
                updatedAtEpochMs = now
            )
            _registrationsByKey.value = _registrationsByKey.value + (riderKey to next)
            persist(context)
            next
        }
        if (updated != null) {
            ensureRiderProfileInRepository(SharedOrderRepository.getInstance(context), updated)
        }
        return updated
    }

    /**
     * Called from Admin Console App to reject a pending Rider with reason.
     */
    fun rejectRiderByAdmin(context: Context, riderKey: String, reason: String): RiderRegistrationRecord? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val existing = _registrationsByKey.value[riderKey] ?: return null
            val next = existing.copy(
                approvalStatus = RiderApprovalWorkflowStatus.REJECTED,
                rejectionReason = reason.trim().ifBlank { "Please upload clearer KYC document photos." },
                reLoggedInAfterApproval = false,
                updatedAtEpochMs = now
            )
            _registrationsByKey.value = _registrationsByKey.value + (riderKey to next)
            persist(context)
            return next
        }
    }

    /**
     * Marks that the Rider signed in again AFTER Admin approval — unlocking the Online toggle & dashboard!
     */
    fun markRiderReLoggedInAfterApproval(context: Context, phone: String?, email: String?) {
        ensureLoaded(context)
        val rec = getRecordForIdentity(context, phone, email) ?: return
        if (rec.approvalStatus == RiderApprovalWorkflowStatus.APPROVED && !rec.reLoggedInAfterApproval) {
            synchronized(lock) {
                val updated = rec.copy(reLoggedInAfterApproval = true)
                _registrationsByKey.value = _registrationsByKey.value + (rec.riderKey to updated)
                persist(context)
                ensureRiderProfileInRepository(SharedOrderRepository.getInstance(context), updated)
            }
        }
    }

    fun ensureRiderProfileInRepository(repository: SharedOrderRepository, rec: RiderRegistrationRecord) {
        val currentList = repository.riders.value
        val vehicleType = runCatching { VehicleType.valueOf(rec.vehicleType) }.getOrDefault(VehicleType.BIKE)
        val existingIdx = currentList.indexOfFirst {
            it.riderId == rec.riderId || (rec.phone.isNotBlank() && it.mobile.filter(Char::isDigit).takeLast(10) == rec.phone)
        }
        val profile = RiderProfile(
            riderId = rec.riderId,
            name = rec.name,
            mobile = rec.phone,
            email = rec.email,
            aadhaarNumber = rec.aadhaarNumber,
            panNumber = rec.panNumber,
            vehicleType = vehicleType,
            vehicleNumber = rec.vehicleNumber,
            verificationStatus = if (rec.approvalStatus == RiderApprovalWorkflowStatus.APPROVED) {
                RiderVerificationStatus.VERIFIED
            } else {
                RiderVerificationStatus.PENDING
            },
            accountStatus = "ACTIVE",
            isOnline = if (existingIdx >= 0) currentList[existingIdx].isOnline else false,
            walletBalanceInr = if (existingIdx >= 0) currentList[existingIdx].walletBalanceInr else 0.0,
            commissionDebtInr = if (existingIdx >= 0) currentList[existingIdx].commissionDebtInr else 0.0,
            totalDeliveries = if (existingIdx >= 0) currentList[existingIdx].totalDeliveries else 0,
            adminApprovalNote = "Admin Approved KYC Verified",
            workMode = RiderWorkMode.FULL_TIME,
            shiftStatus = if (existingIdx >= 0 && currentList[existingIdx].isOnline) {
                RiderShiftStatus.ON_ACTIVE_SHIFT
            } else {
                RiderShiftStatus.ENDED_AT_HOME
            },
            currentLat = if (existingIdx >= 0) currentList[existingIdx].currentLat else 0.0,
            currentLng = if (existingIdx >= 0) currentList[existingIdx].currentLng else 0.0
        )
        val nextFleet = if (existingIdx >= 0) {
            currentList.toMutableList().also { it[existingIdx] = profile }
        } else {
            listOf(profile) + currentList
        }
        AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, nextFleet)
        AutomatedFairRiderAssignmentEngine.selectActiveRiderForPartnerApp(profile.riderId)
    }

    fun exportAllToJsonArray(): JSONArray {
        val arr = JSONArray()
        _registrationsByKey.value.values.forEach { arr.put(it.toJson()) }
        return arr
    }

    /**
     * Merges remote synced records from `/api/live-sync` without losing local `reLoggedInAfterApproval`
     * state or overwriting newer local changes.
     */
    fun mergeFromServerJsonArray(context: Context, array: JSONArray?) {
        if (array == null || array.length() == 0) return
        ensureLoaded(context)
        var changed = false
        synchronized(lock) {
            val currentMap = _registrationsByKey.value.toMutableMap()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val remote = RiderRegistrationRecord.fromJson(obj) ?: continue
                val local = currentMap[remote.riderKey]
                if (local == null) {
                    // Server won't set reLoggedInAfterApproval for this device until user logs in again locally
                    currentMap[remote.riderKey] = remote.copy(reLoggedInAfterApproval = false)
                    changed = true
                } else {
                    val useRemote = remote.updatedAtEpochMs > local.updatedAtEpochMs ||
                        (remote.approvalStatus == RiderApprovalWorkflowStatus.APPROVED &&
                            local.approvalStatus != RiderApprovalWorkflowStatus.APPROVED) ||
                        (remote.approvalStatus == RiderApprovalWorkflowStatus.REJECTED &&
                            local.approvalStatus == RiderApprovalWorkflowStatus.PENDING_APPROVAL)
                    if (useRemote) {
                        val preserveRelogin = local.reLoggedInAfterApproval &&
                            local.approvalStatus == RiderApprovalWorkflowStatus.APPROVED &&
                            remote.approvalStatus == RiderApprovalWorkflowStatus.APPROVED
                        currentMap[remote.riderKey] = remote.copy(
                            reLoggedInAfterApproval = preserveRelogin
                        )
                        changed = true
                    }
                }
            }
            if (changed) {
                _registrationsByKey.value = currentMap
                persist(context)
            }
        }
    }
}
