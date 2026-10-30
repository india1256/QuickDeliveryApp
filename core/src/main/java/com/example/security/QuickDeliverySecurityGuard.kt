package com.example.security

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/**
 * Authoritative Security Guard for Quick Delivery (Customer, Rider Partner, and Admin Enterprise).
 *
 * Implements:
 * 1. Sliding-window rate limiting & burst abuse prevention on order placement and API endpoints
 *    to block fake mass-order spam attacks, duplicate submission bursts, and brute-force auth attempts.
 * 2. Strict input sanitization against XSS/HTML/script injection, control characters, and NoSQL operator injection.
 * 3. Cryptographic HMAC-SHA256 role-based session token signing and verification to prevent
 *    privilege escalation or local storage/preference tampering across Customer, Rider, and Admin roles.
 */
object QuickDeliverySecurityGuard {

    // --- Rate Limit Thresholds ---
    const val MAX_ORDERS_PER_MINUTE: Int = 5
    const val MAX_ORDERS_PER_HOUR: Int = 20
    const val MAX_ACTIVE_PENDING_ORDERS_PER_USER: Int = 5
    const val DUPLICATE_ORDER_COOLDOWN_MS: Long = 8_000L

    const val ENDPOINT_ORDER_CREATE = "ORDER_CREATE"
    const val ENDPOINT_RIDER_CLAIM = "RIDER_CLAIM"
    const val ENDPOINT_ORDER_CHAT = "ORDER_CHAT"
    const val ENDPOINT_AUTH_ATTEMPT = "AUTH_ATTEMPT"
    const val ENDPOINT_ADMIN_MUTATION = "ADMIN_MUTATION"

    private const val ONE_MINUTE_MS = 60_000L
    private const val ONE_HOUR_MS = 3_600_000L

    private val lock = Any()

    // Per-actor sliding window timestamps: key = "$endpoint:$actorKey"
    private val endpointCallHistory = mutableMapOf<String, MutableList<Long>>()

    // Duplicate order fingerprint tracker: key = sha256(actor + pickup + drop + vehicle), value = lastEpochMs
    private val recentOrderFingerprints = mutableMapOf<String, Long>()

    // Security audit log for Admin visibility
    data class SecurityAuditEvent(
        val timestampEpochMs: Long,
        val category: String,
        val actorKey: String,
        val allowed: Boolean,
        val detail: String
    )

    private val securityAuditLog = ArrayDeque<SecurityAuditEvent>(128)

    private val dangerousMarkupRegex = Regex(
        pattern = "(?i)(<\\s*/?\\s*(script|iframe|object|embed|svg|link|style|meta)[^>]*>|javascript\\s*:|vbscript\\s*:|data\\s*:\\s*text/html|on(error|load|click|mouseover|focus)\\s*=)",
    )
    private val noSqlOperatorRegex = Regex(
        pattern = "(?i)(\\\$where|\\\$ne|\\\$gt|\\\$lt|\\\$gte|\\\$lte|\\\$regex|__proto__|constructor\\.prototype)"
    )
    private val controlCharsRegex = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")
    private val angleBracketRegex = Regex("[<>]")

    /**
     * Resets rate-limiting windows and duplicate fingerprint caches (used when resetting test state).
     */
    fun resetRateLimitsForTesting() {
        synchronized(lock) {
            endpointCallHistory.clear()
            recentOrderFingerprints.clear()
            securityAuditLog.clear()
        }
    }

    fun getRecentSecurityAuditEvents(limit: Int = 50): List<SecurityAuditEvent> {
        synchronized(lock) {
            return securityAuditLog.takeLast(limit).reversed()
        }
    }

    private fun recordSecurityEvent(
        category: String,
        actorKey: String,
        allowed: Boolean,
        detail: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        if (securityAuditLog.size >= 120) {
            securityAuditLog.removeFirst()
        }
        securityAuditLog.addLast(
            SecurityAuditEvent(
                timestampEpochMs = nowEpochMs,
                category = category,
                actorKey = actorKey.take(64),
                allowed = allowed,
                detail = detail.take(240)
            )
        )
    }

    // =========================================================================
    // 1. RATE LIMITING & MASS-ORDER SPAM ABUSE PREVENTION
    // =========================================================================

    /**
     * Enforces multi-tier rate limiting and spam protection on Customer Order Placement:
     * - Blocks if the customer already has >= [MAX_ACTIVE_PENDING_ORDERS_PER_USER] unassigned orders.
     * - Blocks duplicate rapid order submissions (identical pickup, drop, and vehicle within 8s).
     * - Enforces max [MAX_ORDERS_PER_MINUTE] (5/min) and [MAX_ORDERS_PER_HOUR] (20/hr) per user/actor.
     */
    fun checkAndRecordOrderPlacement(
        actorKey: String,
        pickupAddress: String,
        dropAddress: String,
        vehicleType: String,
        activePendingOrdersCount: Int = 0,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<Unit> {
        synchronized(lock) {
            val normalizedActor = sanitizeIdentifier(actorKey).ifBlank { "anon_customer" }

            // 1. Active Pending Order Queue Cap
            if (activePendingOrdersCount >= MAX_ACTIVE_PENDING_ORDERS_PER_USER) {
                val msg = "Rate limit protection: You already have $activePendingOrdersCount active unassigned orders. Please wait for rider assignment or cancel an existing order before booking more."
                recordSecurityEvent("ORDER_QUEUE_CAP", normalizedActor, false, msg, nowEpochMs)
                return Result.failure(IllegalStateException(msg))
            }

            // 2. Duplicate Order Burst Protection (blocks rapid double-taps / replay spam of identical route)
            val routeFingerprint = sha256Hex(
                "$normalizedActor|${sanitizeText(pickupAddress, 300).lowercase()}|${sanitizeText(dropAddress, 300).lowercase()}|${vehicleType.uppercase()}"
            )
            recentOrderFingerprints.entries.removeIf { nowEpochMs - it.value > ONE_MINUTE_MS }
            val lastSameRouteMs = recentOrderFingerprints[routeFingerprint]
            if (lastSameRouteMs != null && (nowEpochMs - lastSameRouteMs) in 0 until DUPLICATE_ORDER_COOLDOWN_MS) {
                val waitSec = ((DUPLICATE_ORDER_COOLDOWN_MS - (nowEpochMs - lastSameRouteMs)) / 1000L).coerceAtLeast(1L)
                val msg = "Duplicate order protection: An identical order was just placed. Please wait ${waitSec}s before placing the same order again."
                recordSecurityEvent("ORDER_DUPLICATE_BLOCKED", normalizedActor, false, msg, nowEpochMs)
                return Result.failure(IllegalStateException(msg))
            }

            // 3. Sliding-Window Per-Minute & Per-Hour Rate Limit
            val historyKey = "$ENDPOINT_ORDER_CREATE:$normalizedActor"
            val timestamps = endpointCallHistory.getOrPut(historyKey) { mutableListOf() }
            timestamps.removeAll { nowEpochMs - it > ONE_HOUR_MS }

            val lastMinuteCount = timestamps.count { nowEpochMs - it <= ONE_MINUTE_MS }
            if (lastMinuteCount >= MAX_ORDERS_PER_MINUTE) {
                val msg = "Order rate limit exceeded: Maximum $MAX_ORDERS_PER_MINUTE orders allowed per minute to prevent automated spam. Please wait a moment and try again."
                recordSecurityEvent("ORDER_RATE_LIMIT_MINUTE", normalizedActor, false, msg, nowEpochMs)
                return Result.failure(IllegalStateException(msg))
            }

            if (timestamps.size >= MAX_ORDERS_PER_HOUR) {
                val msg = "Order rate limit exceeded: Maximum $MAX_ORDERS_PER_HOUR orders allowed per hour per account."
                recordSecurityEvent("ORDER_RATE_LIMIT_HOUR", normalizedActor, false, msg, nowEpochMs)
                return Result.failure(IllegalStateException(msg))
            }

            timestamps.add(nowEpochMs)
            recentOrderFingerprints[routeFingerprint] = nowEpochMs
            recordSecurityEvent("ORDER_PLACEMENT_ALLOWED", normalizedActor, true, "Order rate-limit check passed", nowEpochMs)
            return Result.success(Unit)
        }
    }

    /**
     * Generic sliding-window rate limiter for API / repository endpoints (Rider claim, Order chat, Auth attempts, Admin mutations).
     */
    fun checkAndRecordEndpointCall(
        endpoint: String,
        actorKey: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<Unit> {
        synchronized(lock) {
            val normalizedActor = sanitizeIdentifier(actorKey).ifBlank { "default_actor" }
            val maxPerMinute = when (endpoint) {
                ENDPOINT_ORDER_CREATE -> MAX_ORDERS_PER_MINUTE
                ENDPOINT_RIDER_CLAIM -> 10
                ENDPOINT_ORDER_CHAT -> 12
                ENDPOINT_AUTH_ATTEMPT -> 6
                ENDPOINT_ADMIN_MUTATION -> 30
                else -> 15
            }
            val historyKey = "$endpoint:$normalizedActor"
            val timestamps = endpointCallHistory.getOrPut(historyKey) { mutableListOf() }
            timestamps.removeAll { nowEpochMs - it > ONE_MINUTE_MS }

            if (timestamps.size >= maxPerMinute) {
                val msg = "Rate limit exceeded for $endpoint (max $maxPerMinute requests/min). Please wait before retrying."
                recordSecurityEvent("ENDPOINT_RATE_LIMIT", "$endpoint:$normalizedActor", false, msg, nowEpochMs)
                return Result.failure(IllegalStateException(msg))
            }

            timestamps.add(nowEpochMs)
            return Result.success(Unit)
        }
    }

    // =========================================================================
    // 2. INPUT SANITIZATION & PAYLOAD VALIDATION
    // =========================================================================

    /**
     * Sanitizes user-supplied text (names, addresses, landmarks, notes, chat messages) by removing
     * HTML/script tags, JS URI schemes, NoSQL injection tokens, angle brackets, and control characters.
     */
    fun sanitizeText(input: String?, maxLength: Int = 300): String {
        if (input.isNullOrBlank()) return ""
        return input
            .replace(controlCharsRegex, "")
            .replace(dangerousMarkupRegex, "")
            .replace(noSqlOperatorRegex, "")
            .replace(angleBracketRegex, "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)
    }

    /**
     * Sanitizes IDs and codes to strictly alphanumeric, underscore, and hyphen characters (`^[a-zA-Z0-9_\-]+$`).
     */
    fun sanitizeIdentifier(input: String?, maxLength: Int = 64): String {
        if (input.isNullOrBlank()) return ""
        return input
            .trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(maxLength)
    }

    /**
     * Sanitizes and normalizes an Indian mobile phone number to 10 digits.
     */
    fun sanitizePhoneNumber(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val digitsOnly = input.filter { it.isDigit() }
        return if (digitsOnly.length >= 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }

    /**
     * Validates that a phone number is a legitimate 10-digit mobile number.
     */
    fun isValidPhoneNumber(input: String?): Boolean {
        val clean = sanitizePhoneNumber(input)
        return clean.length == 10 && clean.all { it.isDigit() }
    }

    /**
     * Sanitizes and normalizes an email address.
     */
    fun sanitizeEmail(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input
            .replace(controlCharsRegex, "")
            .replace(dangerousMarkupRegex, "")
            .replace(Regex("[^a-zA-Z0-9@._+\\-]"), "")
            .trim()
            .lowercase()
            .take(120)
    }

    fun isValidEmail(input: String?): Boolean {
        val clean = sanitizeEmail(input)
        return clean.length in 5..120 &&
            clean.contains("@") &&
            !clean.startsWith("@") &&
            !clean.endsWith("@") &&
            clean.substringAfter("@").contains(".")
    }

    /**
     * Validates and sanitizes all fields of a customer order before repository or Firestore persistence.
     */
    fun validateAndSanitizeOrderInputs(
        customerName: String,
        customerPhone: String,
        pickupAddress: String,
        dropAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ): Result<SanitizedOrderFields> {
        val cleanName = sanitizeText(customerName, 80).ifBlank { "Customer" }
        val cleanPhone = sanitizePhoneNumber(customerPhone).ifBlank { "9876543210" }
        if (cleanPhone.length != 10) {
            return Result.failure(IllegalArgumentException("Customer phone number must be a valid 10-digit mobile number."))
        }
        val cleanPickup = sanitizeText(pickupAddress, 300)
        val cleanDrop = sanitizeText(dropAddress, 300)
        if (cleanPickup.length < 3) {
            return Result.failure(IllegalArgumentException("Pickup address must be at least 3 characters after sanitization."))
        }
        if (cleanDrop.length < 3) {
            return Result.failure(IllegalArgumentException("Drop address must be at least 3 characters after sanitization."))
        }
        if (pickupLat.isNaN() || pickupLat !in -90.0..90.0 || pickupLng.isNaN() || pickupLng !in -180.0..180.0) {
            return Result.failure(IllegalArgumentException("Invalid pickup GPS coordinates."))
        }
        if (dropLat.isNaN() || dropLat !in -90.0..90.0 || dropLng.isNaN() || dropLng !in -180.0..180.0) {
            return Result.failure(IllegalArgumentException("Invalid drop GPS coordinates."))
        }
        return Result.success(
            SanitizedOrderFields(
                customerName = cleanName,
                customerPhone = cleanPhone,
                pickupAddress = cleanPickup,
                dropAddress = cleanDrop
            )
        )
    }

    data class SanitizedOrderFields(
        val customerName: String,
        val customerPhone: String,
        val pickupAddress: String,
        val dropAddress: String
    )

    // =========================================================================
    // 3. CRYPTOGRAPHIC ROLE-BASED AUTHENTICATION TOKEN HANDLING
    // =========================================================================

    private const val SESSION_TOKEN_TTL_MS: Long = 30L * 24L * 3600L * 1000L // 30 days
    private const val HMAC_ALGO = "HmacSHA256"

    private fun resolveDeviceSigningSecret(context: Context?): ByteArray {
        val pkg = context?.packageName ?: "com.aistudio.quickdelivery.ltkeqf"
        val seed = "QD_ROLE_GUARD_V1|$pkg|gen-lang-client-0738232271"
        return MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Issues a tamper-evident HMAC-SHA256 signed session token binding the user's identity
     * (`name`, `phone`, `email`) to a specific authorized `role` (`customer`, `rider`, or `admin`).
     */
    fun issueRoleSessionToken(
        context: Context?,
        role: String,
        name: String,
        phone: String,
        email: String,
        issuedAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        val normalizedRole = role.trim().lowercase()
        require(normalizedRole in setOf("customer", "rider", "admin")) {
            "Invalid session role: $role"
        }
        val payloadJson = JSONObject().apply {
            put("v", 1)
            put("role", normalizedRole)
            put("name", sanitizeText(name, 80))
            put("phone", sanitizePhoneNumber(phone))
            put("email", sanitizeEmail(email))
            put("iat", issuedAtEpochMs)
            put("exp", issuedAtEpochMs + SESSION_TOKEN_TTL_MS)
        }.toString()

        val payloadB64 = Base64.encodeToString(
            payloadJson.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE
        )
        val signatureHex = computeHmacSha256Hex(payloadB64, resolveDeviceSigningSecret(context))
        return "qd_v1.$payloadB64.$signatureHex"
    }

    /**
     * Verifies a signed role session token and confirms that:
     * 1. The HMAC-SHA256 signature matches (no local tampering of role, phone, or email).
     * 2. The token has not expired.
     * 3. The embedded role strictly matches [expectedRole] (prevents privilege escalation across roles).
     */
    fun verifyRoleSessionToken(
        context: Context?,
        token: String?,
        expectedRole: String,
        expectedName: String? = null,
        expectedPhone: String? = null,
        expectedEmail: String? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (token.isNullOrBlank() || !token.startsWith("qd_v1.")) return false
        val parts = token.split(".")
        if (parts.size != 3) return false
        val payloadB64 = parts[1]
        val providedSig = parts[2]

        val expectedSig = computeHmacSha256Hex(payloadB64, resolveDeviceSigningSecret(context))
        if (!MessageDigest.isEqual(
                providedSig.toByteArray(StandardCharsets.UTF_8),
                expectedSig.toByteArray(StandardCharsets.UTF_8)
            )
        ) {
            recordSecurityEvent("TOKEN_TAMPER_DETECTED", expectedRole, false, "HMAC signature mismatch on session token", nowEpochMs)
            return false
        }

        return runCatching {
            val decodedBytes = Base64.decode(payloadB64, Base64.NO_WRAP or Base64.URL_SAFE)
            val json = JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
            val tokenRole = json.optString("role", "").lowercase()
            val exp = json.optLong("exp", 0L)
            if (tokenRole != expectedRole.trim().lowercase()) {
                recordSecurityEvent("ROLE_ESCALATION_BLOCKED", expectedRole, false, "Token role '$tokenRole' != expected '$expectedRole'", nowEpochMs)
                return@runCatching false
            }
            if (exp <= nowEpochMs) {
                return@runCatching false
            }
            if (!expectedName.isNullOrBlank() && json.optString("name") != sanitizeText(expectedName, 80)) {
                return@runCatching false
            }
            if (!expectedPhone.isNullOrBlank() && json.optString("phone") != sanitizePhoneNumber(expectedPhone)) {
                return@runCatching false
            }
            if (!expectedEmail.isNullOrBlank() && json.optString("email") != sanitizeEmail(expectedEmail)) {
                return@runCatching false
            }
            true
        }.getOrDefault(false)
    }

    private fun computeHmacSha256Hex(data: String, secretKey: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(secretKey, HMAC_ALGO))
        val rawHmac = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return rawHmac.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
