package com.example.security

import android.content.Context
import com.example.data.cloud.QuickDeliveryOperationsManager
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminCredentialsState(
    val isConfigured: Boolean = false,
    val userId: String = "",
    val recoveryEmail: String = "",
    val updatedAtEpochMs: Long = 0L
)

data class SavedAdminCredentialHashes(
    val userId: String,
    val recoveryEmail: String,
    val saltHex: String,
    val hashHex: String,
    val updatedAtEpochMs: Long
)

/**
 * Manages salted, stretched SHA-256 Admin User ID & Password authentication locally
 * and supports private server synchronization for multi-device Admin access.
 */
object AdminCredentialsManager {
    private const val PREFS_NAME = "quick_delivery_admin_credentials_v1"
    private const val KEY_USER_ID = "admin_user_id"
    private const val KEY_RECOVERY_EMAIL = "admin_recovery_email"
    private const val KEY_SALT_HEX = "admin_password_salt_hex"
    private const val KEY_HASH_HEX = "admin_password_hash_hex"
    private const val KEY_UPDATED_AT = "admin_credentials_updated_at"
    private const val HASH_ROUNDS = 10_000

    private val lock = Any()
    private var loaded = false

    private val _credentialsState = MutableStateFlow(AdminCredentialsState())
    val credentialsState: StateFlow<AdminCredentialsState> = _credentialsState.asStateFlow()

    fun ensureLoaded(context: Context) {
        synchronized(lock) {
            if (loaded) return
            loaded = true
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val userId = prefs.getString(KEY_USER_ID, "").orEmpty().trim()
            val recoveryEmail = prefs.getString(KEY_RECOVERY_EMAIL, "").orEmpty().trim().lowercase()
            val salt = prefs.getString(KEY_SALT_HEX, "").orEmpty().trim()
            val hash = prefs.getString(KEY_HASH_HEX, "").orEmpty().trim()
            val updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L)
            val configured = userId.isNotBlank() && salt.length == 32 && hash.length == 64
            _credentialsState.value = AdminCredentialsState(
                isConfigured = configured,
                userId = if (configured) userId else "",
                recoveryEmail = if (configured) recoveryEmail else "",
                updatedAtEpochMs = if (configured) updatedAt else 0L
            )
        }
    }

    fun sanitizeUserId(raw: String): String {
        return raw.trim().lowercase().replace(Regex("[^a-z0-9._-]"), "").take(32)
    }

    fun validateUserId(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.length < 4) return "User ID must be at least 4 characters."
        if (trimmed.length > 32) return "User ID cannot exceed 32 characters."
        if (!Regex("^[a-zA-Z0-9._-]+$").matches(trimmed)) {
            return "User ID can only use letters, numbers, dot (.), underscore (_), or hyphen (-)."
        }
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < 6) return "Password must be at least 6 characters."
        if (password.length > 64) return "Password cannot exceed 64 characters."
        if (password != password.trim()) return "Password cannot start or end with spaces."
        return null
    }

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun computeStretchedPasswordHash(saltHex: String, plainPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var current = "QD_ADMIN_V1|${saltHex.lowercase()}|$plainPassword".toByteArray(Charsets.UTF_8)
        repeat(HASH_ROUNDS) {
            digest.reset()
            current = digest.digest(current)
        }
        return current.joinToString("") { "%02x".format(it) }
    }

    fun saveCredentials(
        context: Context,
        rawUserId: String,
        plainPassword: String,
        recoveryEmail: String
    ): Result<SavedAdminCredentialHashes> {
        ensureLoaded(context)
        val userIdErr = validateUserId(rawUserId)
        if (userIdErr != null) return Result.failure(IllegalArgumentException(userIdErr))
        val passErr = validatePassword(plainPassword)
        if (passErr != null) return Result.failure(IllegalArgumentException(passErr))
        val cleanEmail = recoveryEmail.trim().lowercase()
        if (!QuickDeliveryOperationsManager.isEmailAllowlistedForAdmin(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Recovery email must be an authorized Admin email."))
        }

        val cleanUserId = rawUserId.trim().lowercase()
        val saltHex = generateSaltHex()
        val hashHex = computeStretchedPasswordHash(saltHex, plainPassword)
        val now = System.currentTimeMillis()
        persistHashes(context, cleanUserId, cleanEmail, saltHex, hashHex, now)
        return Result.success(
            SavedAdminCredentialHashes(
                userId = cleanUserId,
                recoveryEmail = cleanEmail,
                saltHex = saltHex,
                hashHex = hashHex,
                updatedAtEpochMs = now
            )
        )
    }

    fun cacheServerVerifiedCredentials(
        context: Context,
        userId: String,
        recoveryEmail: String,
        saltHex: String,
        hashHex: String,
        updatedAtEpochMs: Long
    ) {
        ensureLoaded(context)
        val cleanId = userId.trim().lowercase()
        val cleanEmail = recoveryEmail.trim().lowercase()
        if (cleanId.isBlank() || saltHex.length != 32 || hashHex.length != 64) return
        persistHashes(context, cleanId, cleanEmail, saltHex.lowercase(), hashHex.lowercase(), updatedAtEpochMs)
    }

    fun verifyLocalLogin(
        context: Context,
        identifier: String,
        plainPassword: String
    ): Result<String> {
        ensureLoaded(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedId = prefs.getString(KEY_USER_ID, "").orEmpty().trim().lowercase()
        val storedEmail = prefs.getString(KEY_RECOVERY_EMAIL, "").orEmpty().trim().lowercase()
        val storedSalt = prefs.getString(KEY_SALT_HEX, "").orEmpty().trim().lowercase()
        val storedHash = prefs.getString(KEY_HASH_HEX, "").orEmpty().trim().lowercase()
        if (storedId.isBlank() || storedSalt.length != 32 || storedHash.length != 64) {
            return Result.failure(IllegalStateException("Admin credentials are not set on this device yet."))
        }

        val cleanIdent = identifier.trim().lowercase()
        val identifierMatches = cleanIdent == storedId || (storedEmail.isNotBlank() && cleanIdent == storedEmail)
        val computedHash = computeStretchedPasswordHash(storedSalt, plainPassword)
        val hashMatches = MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
        return if (identifierMatches && hashMatches) {
            Result.success(storedEmail.ifBlank { QuickDeliveryOperationsManager.ALLOWED_ADMIN_EMAILS.first() })
        } else {
            Result.failure(IllegalArgumentException("Incorrect Admin User ID or Password."))
        }
    }

    fun verifyCurrentPassword(context: Context, currentPassword: String): Boolean {
        ensureLoaded(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedSalt = prefs.getString(KEY_SALT_HEX, "").orEmpty().trim().lowercase()
        val storedHash = prefs.getString(KEY_HASH_HEX, "").orEmpty().trim().lowercase()
        if (storedSalt.length != 32 || storedHash.length != 64) return false
        val computed = computeStretchedPasswordHash(storedSalt, currentPassword)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun clearForTesting(context: Context) {
        synchronized(lock) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            _credentialsState.value = AdminCredentialsState()
            loaded = true
        }
    }

    private fun persistHashes(
        context: Context,
        userId: String,
        recoveryEmail: String,
        saltHex: String,
        hashHex: String,
        updatedAtEpochMs: Long
    ) {
        synchronized(lock) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_RECOVERY_EMAIL, recoveryEmail)
                .putString(KEY_SALT_HEX, saltHex)
                .putString(KEY_HASH_HEX, hashHex)
                .putLong(KEY_UPDATED_AT, updatedAtEpochMs)
                .apply()
            _credentialsState.value = AdminCredentialsState(
                isConfigured = true,
                userId = userId,
                recoveryEmail = recoveryEmail,
                updatedAtEpochMs = updatedAtEpochMs
            )
            loaded = true
        }
    }
}
