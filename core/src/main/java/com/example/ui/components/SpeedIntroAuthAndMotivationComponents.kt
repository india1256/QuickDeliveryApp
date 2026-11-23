package com.example.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import com.example.ui.i18n.LanguageSwitcherPill
import com.example.ui.i18n.rememberDynamicUiStrings
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.security.QuickDeliverySecurityGuard
import com.example.ui.auth.GoogleSignInButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Rotating Motivation & Customer Notification Manager:
 * - Ensures every time a Rider picks up an order, a brand-new motivational message is shown.
 * - Triggers a celebratory Motivational Notification for BOTH Rider and Customer after EVERY successful delivery.
 */
object MotivationAndPromoManager {
    private val riderMotivationMessages = listOf(
        "🚀 Shabash Partner! Aapki tezi aur mehnat har customer ke chehre par muskan lati hai — Safe drive karein!",
        "🌟 Zabardast Pickup! Har safal delivery aapke sapno aur kamyabi ko aur kareeb lati hai. Keep going!",
        "💪 Super Rider! Aapki waqt ki pabandi aur imandari Quick Delivery ki asli pehchan hai!",
        "🏆 Champion Effort! Ek aur parcel manzil ki taraf — dhyan se gadi chalayein, aapki suraksha sabse pehle hai!",
        "⚡ Shandar Kaam Partner! Har order ke sath aapki earning aur customer trust dono badh rahe hain!",
        "🔥 Top Performer! Aapki mehnat se aaj phir ek zaroori parcel samay par pahunchega — All the best!",
        "🌈 Proud Partner! Aap sirf parcel nahi, bharosa deliver kar rahe hain — Aise hi aage badhte rahein!",
        "🏅 Great Going! Agla milestone bas kuch hi deliveries door hai — Keep shining!"
    )

    private val riderPostDeliveryMotivationMessages = listOf(
        "🏆 शानदार डिलीवरी पूरी हुई पार्टनर! आपकी मेहनत और समय की पाबंदी ने एक और ग्राहक का दिल जीत लिया — आपकी 80% कमाई वॉलेट में तुरंत क्रेडिट हो गई है! ऐसे ही आगे बढ़ते रहें!",
        "🌟 कमाल कर दिया चैंपियन! हर सफल डिलीवरी आपके परिवार की खुशियों और आपकी तरक्की की नई सीढ़ी है। आप Quick Delivery के असली हीरो हैं!",
        "🚀 सुपरफास्ट और सुरक्षित डिलीवरी! आपकी लगन और ईमानदारी को सलाम — अगली बुकिंग के लिए तैयार रहें, सफलता आपके कदम चूम रही है!",
        "💪 गर्व है आप पर पार्टनर! आपने सिर्फ एक पार्सल नहीं बल्कि विश्वास और खुशी पहुंचाई है। आपकी मेहनत का हर पल अनमोल है!"
    )

    private val customerPostDeliveryMotivationMessages = listOf(
        "🎉 बधाई हो! आपका पार्सल सुरक्षित और समय पर डिलीवर हो गया है! आपके विश्वास और सहयोग से हमारे राइडर पार्टनर्स को आगे बढ़ने की प्रेरणा मिलती है — आपका दिन खुशियों से भरा रहे!",
        "🌟 सफल डिलीवरी! मुस्कुराइए, आपका पार्सल अपनी मंजिल पर पहुंच चुका है। सकारात्मक सोच और सही समय हर काम को आसान बना देते हैं — Quick Delivery चुनने के लिए दिल से धन्यवाद!",
        "🚀 मिशन पूरा हुआ! आपकी हर बुकिंग किसी मेहनती राइडर के सपनों को उड़ान देती है। ऐसे ही मुस्कुराते रहें और आगे बढ़ते रहें!",
        "💫 शानदार अनुभव! समय की कद्र और भरोसे का दूसरा नाम है आपकी और हमारी साझेदारी। आपका आज का दिन बेहद शुभ और सफल रहे!"
    )

    private val customerPromoNotifications = listOf(
        "🔔 Quick Notification: Aapka parcel safe hands me hai! Ghar ya office ka agla saman bhejna ho to abhi 1-click me naya order book karein.",
        "🎁 Express Alert: Dost, family ya customer ko kuch aur bhejna hai? Abhi agla order book karein aur super-fast pickup payein!",
        "⚡ Fast City Courier: Dukaan, documents ya tiffin — agla parcel bhi Quick Delivery se bhejein aur live track karein!",
        "🌟 Happy Delivery Update: Aapka order tezi se aage badh raha hai! Zaroorat padne par agla order turant book karein.",
        "🚀 Instant Dispatch Ready: Hamare rider partners aapke area me active hain — apna agla order abhi book karein!"
    )

    private var riderIndex = 0
    private var customerIndex = 0
    private var riderDeliveryMotivationIndex = 0
    private var customerDeliveryMotivationIndex = 0

    private val notifiedDeliveredOrderIds = mutableSetOf<String>()

    private val _riderPostDeliveryMotivation = MutableStateFlow<String?>(null)
    val riderPostDeliveryMotivation: StateFlow<String?> = _riderPostDeliveryMotivation.asStateFlow()

    private val _customerPostDeliveryMotivation = MutableStateFlow<String?>(null)
    val customerPostDeliveryMotivation: StateFlow<String?> = _customerPostDeliveryMotivation.asStateFlow()

    private val _lastDeliveredOrderBadge = MutableStateFlow<String?>(null)
    val lastDeliveredOrderBadge: StateFlow<String?> = _lastDeliveredOrderBadge.asStateFlow()

    // Customer renders smoothly while Rider checks persistent rider_logged_in flag on first launch
    @Volatile
    var customerIntroCompleted: Boolean = true
    @Volatile
    var riderIntroCompleted: Boolean = true
    @Volatile
    var customerAuthenticatedInSession: Boolean = false
    @Volatile
    var riderAuthenticatedInSession: Boolean = false
    @Volatile
    var lastAuthWasNewSignUp: Boolean = false
    @Volatile
    var forceSignInTabNextOpen: Boolean = false
    @Volatile
    var riderApprovalBannerMessage: String? = null
    @Volatile
    var cachedCustomerName: String = ""
    @Volatile
    var cachedCustomerPhone: String = ""
    @Volatile
    var cachedCustomerEmail: String = ""
    @Volatile
    var cachedRiderName: String = ""
    @Volatile
    var cachedRiderPhone: String = ""
    @Volatile
    var cachedRiderEmail: String = ""

    private val _customerStoragePermissionGranted = MutableStateFlow(false)
    val customerStoragePermissionGranted: StateFlow<Boolean> = _customerStoragePermissionGranted.asStateFlow()

    private val _riderStoragePermissionGranted = MutableStateFlow(false)
    val riderStoragePermissionGranted: StateFlow<Boolean> = _riderStoragePermissionGranted.asStateFlow()

    private const val AUTH_PREFS_NAME = "quick_delivery_user_auth_prefs"
    private const val KEY_REGISTERED_ACCOUNTS_JSON = "registered_accounts_json_v1"

    data class RegisteredAccountRecord(
        val role: String,
        val name: String,
        val phone: String,
        val email: String,
        val updatedAtEpochMs: Long = System.currentTimeMillis()
    )

    private fun normalizeAccountRole(rolePrefix: String): String =
        if (rolePrefix.trim().lowercase() == "rider") "rider" else "customer"

    private fun loadRegisteredAccountsMap(context: Context): MutableMap<String, RegisteredAccountRecord> {
        val result = linkedMapOf<String, RegisteredAccountRecord>()
        runCatching {
            val raw = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_REGISTERED_ACCOUNTS_JSON, null)
            if (raw.isNullOrBlank()) return@runCatching
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val role = normalizeAccountRole(obj.optString("role", "customer"))
                val phone = QuickDeliverySecurityGuard.sanitizePhoneNumber(obj.optString("phone", ""))
                val email = QuickDeliverySecurityGuard.sanitizeEmail(obj.optString("email", ""))
                if (phone.isBlank() && email.isBlank()) continue
                val key = "${role}:${if (phone.length == 10) phone else email.lowercase()}"
                result[key] = RegisteredAccountRecord(
                    role = role,
                    name = QuickDeliverySecurityGuard.sanitizeText(obj.optString("name", ""), 80),
                    phone = phone,
                    email = email,
                    updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                )
            }
        }
        return result
    }

    private fun saveRegisteredAccountsMap(context: Context, map: Map<String, RegisteredAccountRecord>) {
        runCatching {
            val arr = org.json.JSONArray()
            map.values.forEach { rec ->
                arr.put(
                    JSONObject().apply {
                        put("role", rec.role)
                        put("name", rec.name)
                        put("phone", rec.phone)
                        put("email", rec.email)
                        put("updatedAtEpochMs", rec.updatedAtEpochMs)
                    }
                )
            }
            context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_REGISTERED_ACCOUNTS_JSON, arr.toString())
                .apply()
        }
    }

    /**
     * Records a genuinely registered Customer or Rider account locally (called ONLY on New Sign Up,
     * Google Sign-In, or verified cloud account restoration).
     */
    fun registerNewAccountLocally(
        context: Context,
        rolePrefix: String,
        name: String,
        phone: String,
        email: String,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        val cleanRole = normalizeAccountRole(rolePrefix)
        val cleanPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(phone)
        val cleanEmail = QuickDeliverySecurityGuard.sanitizeEmail(email)
        val cleanName = QuickDeliverySecurityGuard.sanitizeText(name, 80).ifBlank {
            if (cleanRole == "rider") "Partner User" else "Customer"
        }
        if (cleanPhone.isBlank() && cleanEmail.isBlank()) return
        val map = loadRegisteredAccountsMap(context)
        val key = "${cleanRole}:${if (cleanPhone.length == 10) cleanPhone else cleanEmail.lowercase()}"
        val prev = map[key]
        map[key] = RegisteredAccountRecord(
            role = cleanRole,
            name = cleanName.ifBlank { prev?.name.orEmpty() },
            phone = cleanPhone.ifBlank { prev?.phone.orEmpty() },
            email = cleanEmail.ifBlank { prev?.email.orEmpty() },
            updatedAtEpochMs = maxOf(updatedAtEpochMs, prev?.updatedAtEpochMs ?: 0L)
        )
        saveRegisteredAccountsMap(context, map)
    }

    /**
     * Strictly checks whether a Mobile Number or Email has ALREADY been registered on this device
     * (via New Sign Up, synced cloud registry, Rider KYC registry, or historical real customer orders).
     * Returns null for any unregistered number/email — NEVER fabricates a fake profile!
     */
    fun findLocallyRegisteredAccount(
        context: Context,
        rolePrefix: String,
        identifier: String
    ): RegisteredAccountRecord? {
        val cleanRole = normalizeAccountRole(rolePrefix)
        val trimmed = QuickDeliverySecurityGuard.sanitizeText(identifier, 120)
        if (trimmed.isBlank()) return null
        val isEmailInput = trimmed.contains("@") && trimmed.contains(".")
        val cleanEmail = if (isEmailInput) QuickDeliverySecurityGuard.sanitizeEmail(trimmed) else ""
        val cleanPhone = if (!isEmailInput) QuickDeliverySecurityGuard.sanitizePhoneNumber(trimmed) else ""
        if (cleanEmail.isBlank() && cleanPhone.length != 10) return null

        // 1. Check strict local registered accounts table
        val localMap = loadRegisteredAccountsMap(context)
        localMap.values.firstOrNull { rec ->
            rec.role == cleanRole && (
                (cleanPhone.length == 10 && rec.phone == cleanPhone) ||
                    (cleanEmail.isNotBlank() && rec.email.equals(cleanEmail, ignoreCase = true))
                )
        }?.let { return it }

        // 2. For Rider, also check RiderRegistrationApprovalManager (synced from cloud across reinstalls)
        if (cleanRole == "rider") {
            com.example.data.cloud.RiderRegistrationApprovalManager.getRecordForIdentity(
                context = context,
                phone = cleanPhone.takeIf { it.length == 10 },
                email = cleanEmail.takeIf { it.isNotBlank() }
            )?.let { riderRec ->
                return RegisteredAccountRecord(
                    role = "rider",
                    name = riderRec.name.ifBlank { "Rider Partner" },
                    phone = QuickDeliverySecurityGuard.sanitizePhoneNumber(riderRec.phone),
                    email = QuickDeliverySecurityGuard.sanitizeEmail(riderRec.email),
                    updatedAtEpochMs = riderRec.updatedAtEpochMs
                )
            }
        } else if (cleanPhone.length == 10) {
            // 3. Legacy fallback for genuine pre-v6.6.0 customers who already placed real orders
            val repo = runCatching { com.example.data.repository.SharedOrderRepository.getInstance(context) }.getOrNull()
            val matchedOrder = repo?.orders?.value?.firstOrNull { ord ->
                QuickDeliverySecurityGuard.sanitizePhoneNumber(ord.customerPhone) == cleanPhone
            }
            if (matchedOrder != null) {
                return RegisteredAccountRecord(
                    role = "customer",
                    name = matchedOrder.customerName.ifBlank { "Customer" },
                    phone = cleanPhone,
                    email = ""
                )
            }
        }
        return null
    }

    fun getRegisteredCustomersSnapshot(context: Context): List<RegisteredAccountRecord> {
        return loadRegisteredAccountsMap(context).values
            .filter { it.role == "customer" }
            .sortedByDescending { it.updatedAtEpochMs }
    }

    fun exportRegisteredCustomersToJsonArray(context: Context): org.json.JSONArray {
        val arr = org.json.JSONArray()
        loadRegisteredAccountsMap(context).values
            .filter { it.role == "customer" }
            .forEach { rec ->
                arr.put(
                    JSONObject().apply {
                        put("role", "CUSTOMER")
                        put("name", rec.name)
                        put("phone", rec.phone)
                        put("email", rec.email)
                        put("updatedAtEpochMs", rec.updatedAtEpochMs)
                    }
                )
            }
        return arr
    }

    fun mergeRegisteredCustomersFromServer(context: Context, arr: org.json.JSONArray?) {
        if (arr == null || arr.length() == 0) return
        val map = loadRegisteredAccountsMap(context)
        var changed = false
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val phone = QuickDeliverySecurityGuard.sanitizePhoneNumber(obj.optString("phone", ""))
            val email = QuickDeliverySecurityGuard.sanitizeEmail(obj.optString("email", ""))
            if (phone.length != 10 && email.isBlank()) continue
            val name = QuickDeliverySecurityGuard.sanitizeText(obj.optString("name", ""), 80)
            val updatedAt = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
            val key = "customer:${if (phone.length == 10) phone else email.lowercase()}"
            val prev = map[key]
            if (prev == null || updatedAt >= prev.updatedAtEpochMs) {
                map[key] = RegisteredAccountRecord(
                    role = "customer",
                    name = name.ifBlank { prev?.name.orEmpty().ifBlank { "Customer" } },
                    phone = phone.ifBlank { prev?.phone.orEmpty() },
                    email = email.ifBlank { prev?.email.orEmpty() },
                    updatedAtEpochMs = updatedAt
                )
                changed = true
            }
        }
        if (changed) {
            saveRegisteredAccountsMap(context, map)
        }
    }

    fun clearRegisteredAccountsForTesting(context: Context) {
        runCatching {
            context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_REGISTERED_ACCOUNTS_JSON)
                .apply()
        }
    }

    fun restoreSavedLoginFromPhoneStorage(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            val custGranted = prefs.getBoolean("customer_storage_permission_granted", false)
            val riderGranted = prefs.getBoolean("rider_storage_permission_granted", false)
            _customerStoragePermissionGranted.value = custGranted
            _riderStoragePermissionGranted.value = riderGranted

            // Restore from SharedPreferences or phone storage JSON backup file with HMAC role token verification
            val customerBackup = readBackupJsonFromPhoneStorage(context, "customer")
            val rawCustName = prefs.getString("customer_name", null)
                ?: customerBackup?.optString("name")?.takeIf { it.isNotBlank() }
                ?: cachedCustomerName
            val rawCustPhone = prefs.getString("customer_phone", null)
                ?: customerBackup?.optString("phone")?.takeIf { it.isNotBlank() }
                ?: cachedCustomerPhone
            val rawCustEmail = prefs.getString("customer_email", null)
                ?: customerBackup?.optString("email")?.takeIf { it.isNotBlank() }
                ?: cachedCustomerEmail
            val custToken = prefs.getString("customer_session_token", null)
                ?: customerBackup?.optString("sessionToken")?.takeIf { it.isNotBlank() }
            val custLoggedInSaved = prefs.getBoolean("customer_logged_in", false)
            if (custToken == null || QuickDeliverySecurityGuard.verifyRoleSessionToken(
                    context = context,
                    token = custToken,
                    expectedRole = "customer",
                    expectedName = rawCustName,
                    expectedPhone = rawCustPhone,
                    expectedEmail = rawCustEmail
                )
            ) {
                cachedCustomerName = QuickDeliverySecurityGuard.sanitizeText(rawCustName, 80)
                cachedCustomerPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(rawCustPhone)
                cachedCustomerEmail = QuickDeliverySecurityGuard.sanitizeEmail(rawCustEmail)
                customerAuthenticatedInSession = custLoggedInSaved && (cachedCustomerPhone.isNotBlank() || cachedCustomerEmail.isNotBlank())
            } else {
                customerAuthenticatedInSession = false
            }

            val riderBackup = readBackupJsonFromPhoneStorage(context, "rider")
            val rawRiderName = prefs.getString("rider_name", null)
                ?: riderBackup?.optString("name")?.takeIf { it.isNotBlank() }
                ?: cachedRiderName
            val rawRiderPhone = prefs.getString("rider_phone", null)
                ?: riderBackup?.optString("phone")?.takeIf { it.isNotBlank() }
                ?: cachedRiderPhone
            val rawRiderEmail = prefs.getString("rider_email", null)
                ?: riderBackup?.optString("email")?.takeIf { it.isNotBlank() }
                ?: cachedRiderEmail
            val riderToken = prefs.getString("rider_session_token", null)
                ?: riderBackup?.optString("sessionToken")?.takeIf { it.isNotBlank() }
            val riderLoggedInSaved = prefs.getBoolean("rider_logged_in", false)
            if (riderToken == null || QuickDeliverySecurityGuard.verifyRoleSessionToken(
                    context = context,
                    token = riderToken,
                    expectedRole = "rider",
                    expectedName = rawRiderName,
                    expectedPhone = rawRiderPhone,
                    expectedEmail = rawRiderEmail
                )
            ) {
                cachedRiderName = QuickDeliverySecurityGuard.sanitizeText(rawRiderName, 80)
                cachedRiderPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(rawRiderPhone)
                cachedRiderEmail = QuickDeliverySecurityGuard.sanitizeEmail(rawRiderEmail)
                riderAuthenticatedInSession = riderLoggedInSaved && (cachedRiderPhone.isNotBlank() || cachedRiderEmail.isNotBlank())
            } else {
                riderAuthenticatedInSession = false
            }
        }
    }

    fun grantPhoneStorageAndSaveLogin(
        context: Context,
        rolePrefix: String,
        name: String,
        phone: String,
        email: String,
        storageAllowed: Boolean = true
    ) {
        runCatching {
            val cleanRole = if (rolePrefix.trim().lowercase() == "rider") "rider" else "customer"
            val cleanName = QuickDeliverySecurityGuard.sanitizeText(name, 80).ifBlank {
                if (cleanRole == "rider") "Partner User" else "Customer User"
            }
            val cleanPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(phone)
            val cleanEmail = QuickDeliverySecurityGuard.sanitizeEmail(email)
            val signedToken = QuickDeliverySecurityGuard.issueRoleSessionToken(
                context = context,
                role = cleanRole,
                name = cleanName,
                phone = cleanPhone,
                email = cleanEmail
            )

            val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("${cleanRole}_name", cleanName)
                .putString("${cleanRole}_phone", cleanPhone)
                .putString("${cleanRole}_email", cleanEmail)
                .putString("${cleanRole}_session_token", signedToken)
                .putBoolean("${cleanRole}_logged_in", true)
                .putBoolean("${cleanRole}_storage_permission_granted", storageAllowed)
                .putLong("${cleanRole}_saved_timestamp", System.currentTimeMillis())
                .apply()

            if (cleanRole == "customer") {
                cachedCustomerName = cleanName
                cachedCustomerPhone = cleanPhone
                cachedCustomerEmail = cleanEmail
                _customerStoragePermissionGranted.value = storageAllowed
            } else {
                cachedRiderName = cleanName
                cachedRiderPhone = cleanPhone
                cachedRiderEmail = cleanEmail
                _riderStoragePermissionGranted.value = storageAllowed
            }

            if (storageAllowed) {
                writeBackupJsonToPhoneStorage(context, cleanRole, cleanName, cleanPhone, cleanEmail, signedToken)
            }
        }
    }

    /**
     * Automatically synchronizes the logged-in user's profile and active/past order data
     * in the background using the user's logged-in account credentials.
     */
    fun syncAccountAndOrdersInBackground(
        context: Context,
        orders: List<CustomerOrder>,
        authEmail: String? = null,
        authDisplayName: String? = null
    ) {
        runCatching {
            restoreSavedLoginFromPhoneStorage(context)
            if (!authEmail.isNullOrBlank() && cachedCustomerEmail.isBlank()) {
                cachedCustomerEmail = authEmail
            }
            if (!authDisplayName.isNullOrBlank() && cachedCustomerName.isBlank()) {
                cachedCustomerName = authDisplayName
            }
            val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            val ordersSummaryArray = org.json.JSONArray()
            orders.forEach { order ->
                ordersSummaryArray.put(
                    JSONObject().apply {
                        put("orderId", order.orderId)
                        put("status", order.orderStatus.name)
                        put("customerName", order.customerName.ifBlank { cachedCustomerName })
                        put("customerPhone", order.customerPhone.ifBlank { cachedCustomerPhone })
                        put("pickupAddress", order.pickupAddress.formattedAddress())
                        put("dropAddress", order.dropAddress.formattedAddress())
                        put("totalAmount", order.totalAmount)
                        put("assignedRiderId", order.assignedRiderId ?: "")
                        put("assignedRiderName", order.assignedRiderName ?: "")
                    }
                )
            }
            prefs.edit()
                .putString("customer_name", cachedCustomerName)
                .putString("customer_phone", cachedCustomerPhone)
                .putString("customer_email", cachedCustomerEmail)
                .putString("rider_name", cachedRiderName)
                .putString("rider_phone", cachedRiderPhone)
                .putString("rider_email", cachedRiderEmail)
                .putString("synced_orders_json", ordersSummaryArray.toString())
                .putLong("last_background_sync_epoch_ms", System.currentTimeMillis())
                .apply()

            writeBackupJsonToPhoneStorage(context, "customer", cachedCustomerName, cachedCustomerPhone, cachedCustomerEmail, ordersJson = ordersSummaryArray)
            writeBackupJsonToPhoneStorage(context, "rider", cachedRiderName, cachedRiderPhone, cachedRiderEmail, ordersJson = ordersSummaryArray)
        }
    }

    private fun requiredVaultFileName(cleanRole: String): String =
        "quick_delivery_${cleanRole}_vault.json"

    private fun writeVaultToPublicPhoneStorage(
        context: Context,
        cleanRole: String,
        payload: String
    ) {
        if (!hasPhoneStoragePermission(context)) return
        val fileName = requiredVaultFileName(cleanRole)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")
                val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/QuickDelivery"
                val existingId = resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                    arrayOf(fileName, "%QuickDelivery%"),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else null
                }

                val uri = if (existingId != null) {
                    ContentUris.withAppendedId(collection, existingId)
                } else {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    }
                    resolver.insert(collection, values)
                }
                if (uri != null) {
                    resolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val qdDir = File(docsDir, "QuickDelivery")
                if (!qdDir.exists()) qdDir.mkdirs()
                File(qdDir, fileName).writeText(payload, Charsets.UTF_8)
            }
        }
    }

    private fun readVaultFromPublicPhoneStorage(context: Context, cleanRole: String): JSONObject? {
        if (!hasPhoneStoragePermission(context)) return null
        val fileName = requiredVaultFileName(cleanRole)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")
                resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                    arrayOf(fileName, "%QuickDelivery%"),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val uri = ContentUris.withAppendedId(collection, id)
                        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                            val raw = reader.readText()
                            if (raw.isNotBlank()) return@runCatching JSONObject(raw)
                        }
                    }
                }
                null
            } else {
                @Suppress("DEPRECATION")
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val file = File(File(docsDir, "QuickDelivery"), fileName)
                if (file.exists()) JSONObject(file.readText(Charsets.UTF_8)) else null
            }
        }.getOrNull()
    }

    private fun deleteVaultFromPublicPhoneStorage(context: Context, cleanRole: String) {
        val fileName = requiredVaultFileName(cleanRole)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")
                resolver.delete(
                    collection,
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                    arrayOf(fileName, "%QuickDelivery%")
                )
            } else {
                @Suppress("DEPRECATION")
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val file = File(File(docsDir, "QuickDelivery"), fileName)
                if (file.exists()) file.delete()
            }
        }
    }

    private fun writeBackupJsonToPhoneStorage(
        context: Context,
        rolePrefix: String,
        name: String,
        phone: String,
        email: String,
        sessionToken: String? = null,
        ordersJson: org.json.JSONArray? = null
    ) {
        runCatching {
            val cleanRole = if (rolePrefix.trim().lowercase() == "rider") "rider" else "customer"
            val cleanName = QuickDeliverySecurityGuard.sanitizeText(name, 80)
            val cleanPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(phone)
            val cleanEmail = QuickDeliverySecurityGuard.sanitizeEmail(email)
            val resolvedToken = sessionToken ?: QuickDeliverySecurityGuard.issueRoleSessionToken(
                context = context,
                role = cleanRole,
                name = cleanName,
                phone = cleanPhone,
                email = cleanEmail
            )
            val payload = JSONObject().apply {
                put("role", cleanRole)
                put("name", cleanName)
                put("phone", cleanPhone)
                put("email", cleanEmail)
                put("sessionToken", resolvedToken)
                if (ordersJson != null) {
                    put("ordersCount", ordersJson.length())
                    put("ordersSummary", ordersJson)
                }
                put("savedAtEpochMs", System.currentTimeMillis())
                put("autoLoginEnabled", true)
            }.toString()

            // 1. Save in internal app phone storage
            val internalFile = File(context.filesDir, "quick_delivery_${cleanRole}_login_backup.json")
            internalFile.writeText(payload)

            // 2. Save in external app-specific phone storage directory if mounted
            context.getExternalFilesDir("login_backup")?.let { extDir ->
                if (!extDir.exists()) extDir.mkdirs()
                File(extDir, "quick_delivery_${cleanRole}_login_backup.json").writeText(payload)
            }

            // 3. Save in durable Phone Storage (Documents/QuickDelivery) when storage permission is granted
            writeVaultToPublicPhoneStorage(context, cleanRole, payload)
        }
    }

    private fun readBackupJsonFromPhoneStorage(context: Context, rolePrefix: String): JSONObject? {
        return runCatching {
            val cleanRole = if (rolePrefix.trim().lowercase() == "rider") "rider" else "customer"
            val internalFile = File(context.filesDir, "quick_delivery_${cleanRole}_login_backup.json")
            val json = if (internalFile.exists()) {
                JSONObject(internalFile.readText())
            } else {
                val extFile = context.getExternalFilesDir("login_backup")?.let {
                    File(it, "quick_delivery_${cleanRole}_login_backup.json")
                }
                if (extFile != null && extFile.exists()) {
                    JSONObject(extFile.readText())
                } else {
                    readVaultFromPublicPhoneStorage(context, cleanRole)
                }
            } ?: return@runCatching null

            val token = json.optString("sessionToken", "")
            if (token.isNotBlank() && !QuickDeliverySecurityGuard.verifyRoleSessionToken(
                    context = context,
                    token = token,
                    expectedRole = cleanRole,
                    expectedName = json.optString("name"),
                    expectedPhone = json.optString("phone"),
                    expectedEmail = json.optString("email")
                )
            ) {
                return@runCatching null
            }
            json
        }.getOrNull()
    }

    fun markNewSignUpAppGuidePending(context: Context, rolePrefix: String) {
        val cleanRole = normalizeAccountRole(rolePrefix)
        runCatching {
            context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("${cleanRole}_pending_app_guide_v1", true)
                .apply()
        }
    }

    fun hasPendingNewSignUpAppGuide(context: Context, rolePrefix: String): Boolean {
        val cleanRole = normalizeAccountRole(rolePrefix)
        return runCatching {
            context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("${cleanRole}_pending_app_guide_v1", false)
        }.getOrDefault(false)
    }

    fun dismissNewSignUpAppGuide(context: Context, rolePrefix: String) {
        val cleanRole = normalizeAccountRole(rolePrefix)
        lastAuthWasNewSignUp = false
        runCatching {
            context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("${cleanRole}_pending_app_guide_v1", false)
                .apply()
        }
    }

    fun logoutCustomer(context: Context? = null) {
        customerAuthenticatedInSession = false
        context?.let { ctx ->
            runCatching {
                ctx.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("customer_logged_in", false)
                    .apply()
            }
        }
    }

    fun logoutRider(context: Context? = null) {
        riderAuthenticatedInSession = false
        context?.let { ctx ->
            runCatching {
                ctx.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("rider_logged_in", false)
                    .apply()
            }
        }
    }

    fun deleteUserAccountData(context: Context, rolePrefix: String) {
        val cleanRole = if (rolePrefix.trim().lowercase() == "rider") "rider" else "customer"
        val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        val removedPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(prefs.getString("${cleanRole}_phone", ""))
        val removedEmail = QuickDeliverySecurityGuard.sanitizeEmail(prefs.getString("${cleanRole}_email", ""))
        if (cleanRole == "rider") {
            riderAuthenticatedInSession = false
            _riderStoragePermissionGranted.value = false
            cachedRiderName = ""
            cachedRiderPhone = ""
            cachedRiderEmail = ""
        } else {
            customerAuthenticatedInSession = false
            _customerStoragePermissionGranted.value = false
            cachedCustomerName = ""
            cachedCustomerPhone = ""
            cachedCustomerEmail = ""
        }
        runCatching {
            val map = loadRegisteredAccountsMap(context)
            val filtered = map.filterValues { rec ->
                !(rec.role == cleanRole && (
                    (removedPhone.length == 10 && rec.phone == removedPhone) ||
                        (removedEmail.isNotBlank() && rec.email.equals(removedEmail, ignoreCase = true))
                    ))
            }
            if (filtered.size != map.size) {
                saveRegisteredAccountsMap(context, filtered)
            }
        }
        runCatching {
            prefs.edit()
                .remove("${cleanRole}_name")
                .remove("${cleanRole}_phone")
                .remove("${cleanRole}_email")
                .remove("${cleanRole}_logged_in")
                .remove("${cleanRole}_storage_permission_granted")
                .apply()
        }
        runCatching {
            val internalFile = File(context.filesDir, "quick_delivery_${cleanRole}_login_backup.json")
            if (internalFile.exists()) internalFile.delete()
            context.getExternalFilesDir("login_backup")?.let { extDir ->
                val extFile = File(extDir, "quick_delivery_${cleanRole}_login_backup.json")
                if (extFile.exists()) extFile.delete()
            }
            deleteVaultFromPublicPhoneStorage(context, cleanRole)
        }
        runCatching { Firebase.auth.signOut() }
    }

    fun nextRiderPickupMotivation(): String {
        val msg = riderMotivationMessages[riderIndex % riderMotivationMessages.size]
        riderIndex = (riderIndex + 1) % riderMotivationMessages.size
        return msg
    }

    fun nextCustomerOrderPromoNotification(): String {
        val msg = customerPromoNotifications[customerIndex % customerPromoNotifications.size]
        customerIndex = (customerIndex + 1) % customerPromoNotifications.size
        return msg
    }

    /**
     * Triggers celebratory post-delivery motivational notifications for BOTH Rider and Customer
     * whenever an order transitions to DELIVERED.
     */
    fun notifySuccessfulDeliveryMotivationForBoth(order: CustomerOrder, forceTrigger: Boolean = false) {
        if (!forceTrigger && notifiedDeliveredOrderIds.contains(order.orderId)) return
        notifiedDeliveredOrderIds.add(order.orderId)

        val riderMsg = riderPostDeliveryMotivationMessages[
            riderDeliveryMotivationIndex % riderPostDeliveryMotivationMessages.size
        ]
        riderDeliveryMotivationIndex = (riderDeliveryMotivationIndex + 1) % riderPostDeliveryMotivationMessages.size

        val customerMsg = customerPostDeliveryMotivationMessages[
            customerDeliveryMotivationIndex % customerPostDeliveryMotivationMessages.size
        ]
        customerDeliveryMotivationIndex = (customerDeliveryMotivationIndex + 1) % customerPostDeliveryMotivationMessages.size

        _lastDeliveredOrderBadge.value = "Order #${order.orderId} • ${order.totalAmount} (Rider Earning: ${order.riderEarning})"
        _riderPostDeliveryMotivation.value = "Order #${order.orderId} Delivered (${order.riderEarning}): $riderMsg"
        _customerPostDeliveryMotivation.value = "Order #${order.orderId} Delivered: $customerMsg"
    }

    fun observeOrdersForDeliveryMotivation(orders: List<CustomerOrder>) {
        // Seed initial already-delivered sample orders on first call only if set is empty and we want to track new transitions,
        // or if an order was just delivered, notify both Rider and Customer immediately!
        val deliveredOrders = orders.filter { it.orderStatus == OrderStatus.DELIVERED }
        if (notifiedDeliveredOrderIds.isEmpty() && deliveredOrders.isNotEmpty()) {
            // Mark historical pre-seeded delivered orders, and also show the latest delivered celebration
            deliveredOrders.forEach { notifiedDeliveredOrderIds.add(it.orderId) }
            val latest = deliveredOrders.first()
            notifySuccessfulDeliveryMotivationForBoth(latest, forceTrigger = true)
            return
        }
        deliveredOrders.forEach { delivered ->
            if (!notifiedDeliveredOrderIds.contains(delivered.orderId)) {
                notifySuccessfulDeliveryMotivationForBoth(delivered, forceTrigger = true)
            }
        }
    }

    fun dismissRiderPostDeliveryMotivation() {
        _riderPostDeliveryMotivation.value = null
    }

    fun dismissCustomerPostDeliveryMotivation() {
        _customerPostDeliveryMotivation.value = null
    }

    private val shownLaunchOverlaysInSession = mutableSetOf<String>()

    fun shouldShowLaunchOverlayOnce(appTitle: String): Boolean {
        if (customerIntroCompleted && riderIntroCompleted) {
            shownLaunchOverlaysInSession.add(appTitle)
            return false
        }
        return shownLaunchOverlaysInSession.add(appTitle)
    }
}

/**
 * Next-Level Professional Opening Animation Overlay for Customer App and Rider App:
 * Plays an uplifting, cinematic high-speed city delivery animation with parallax skyline,
 * glowing xenon headlights, neon turbo speed trails, and celebratory particles once per session
 * without ever re-triggering on back navigation, sub-screen pops, or tab switches.
 */
@Composable
fun QuickDeliveryNextLevelLaunchOverlay(
    appTitle: String,
    appRoleBadge: String,
    accentColor: Color,
    secondaryAccentColor: Color = Color(0xFFFF6B35),
    modifier: Modifier = Modifier
) {
    var isVisible by remember(appTitle) {
        mutableStateOf(MotivationAndPromoManager.shouldShowLaunchOverlayOnce(appTitle))
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(2200)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(450)),
        modifier = modifier
    ) {
        QuickDeliverySpeedingVehicleIntro(
            appTitle = appTitle,
            appSubtitle = appRoleBadge,
            accentColor = accentColor,
            secondaryAccentColor = secondaryAccentColor,
            onAnimationFinished = { isVisible = false }
        )
    }
}

/**
 * Celebratory Dual Post-Delivery Motivational Notification Banner shown in BOTH
 * Customer App and Rider App after every successful delivery!
 */
@Composable
fun DualDeliveryCelebrationMotivationBanner(
    roleLabel: String,
    message: String?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, accentColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .testTag("${roleLabel.lowercase(Locale.US)}_post_delivery_motivation_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B),
                                accentColor.copy(alpha = 0.28f)
                            )
                        )
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Delivery Success Motivation",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.22f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "🎉 DELIVERY SUCCESS MOTIVATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Motivation Notification",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 1. Next-Level Cinematic High-Speed Vehicle Launch Animation Screen for Customer App and Rider App.
 */
@Composable
fun QuickDeliverySpeedingVehicleIntro(
    appTitle: String,
    appSubtitle: String,
    accentColor: Color,
    secondaryAccentColor: Color = Color(0xFFFF6B35),
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speeding_vehicle_intro")

    val vehicleProgress by infiniteTransition.animateFloat(
        initialValue = -0.10f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vehicle_x"
    )

    val speedLineShift by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speed_lines"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    LaunchedEffect(Unit) {
        delay(2100)
        onAnimationFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050B14),
                        Color(0xFF0D1B2A),
                        Color(0xFF1B263B),
                        accentColor.copy(alpha = 0.90f)
                    )
                )
            )
            .clickable { onAnimationFinished() }
            .testTag("speeding_vehicle_intro_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Glowing Brand Crest Badge
            Surface(
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, secondaryAccentColor),
                modifier = Modifier.graphicsLayer {
                    scaleX = glowPulse
                    scaleY = glowPulse
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = secondaryAccentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "FAST. SIMPLE. SECURE.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD166)
                    )
                }
            }

            Text(
                text = appTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = appSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.90f)
            )

            // Next-Level Animated City Skyline, Neon Highway & Express Vehicle Canvas
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF091326)),
                border = BorderStroke(2.dp, accentColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val roadY = h * 0.70f

                        // 1. Twinkling Night Sky Stars & Aurora Glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.5f, h * 0.35f),
                                radius = w * 0.55f
                            ),
                            center = Offset(w * 0.5f, h * 0.35f),
                            radius = w * 0.55f
                        )

                        // 2. Parallax City Skyline Silhouette
                        val buildingWidths = listOf(0.11f, 0.09f, 0.13f, 0.10f, 0.12f, 0.09f, 0.14f, 0.11f, 0.12f)
                        val buildingHeights = listOf(0.34f, 0.46f, 0.28f, 0.50f, 0.38f, 0.44f, 0.32f, 0.48f, 0.36f)
                        var bx = -((1f - speedLineShift) * w * 0.18f)
                        buildingWidths.forEachIndexed { idx, bwRatio ->
                            val bw = w * bwRatio
                            val bh = h * buildingHeights[idx % buildingHeights.size]
                            drawRoundRect(
                                color = Color(0xFF132238),
                                topLeft = Offset(bx, roadY - bh),
                                size = Size(bw - 6f, bh),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            // Lit golden windows
                            for (wy in 1..3) {
                                val winY = roadY - bh + wy * (bh / 4.2f)
                                drawRoundRect(
                                    color = Color(0xFFFACC15).copy(alpha = 0.45f),
                                    topLeft = Offset(bx + 8f, winY),
                                    size = Size((bw - 24f).coerceAtLeast(6f), 4f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                            bx += bw
                        }

                        // 3. High-speed neon wind streaks in sky
                        for (i in 0..6) {
                            val lineY = h * (0.14f + i * 0.075f)
                            val startX = ((speedLineShift + i * 0.17f) % 1f) * w
                            drawLine(
                                color = if (i % 2 == 0) accentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.38f),
                                start = Offset(startX, lineY),
                                end = Offset((startX - 110f).coerceAtLeast(0f), lineY),
                                strokeWidth = if (i % 2 == 0) 5f else 3.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        // 4. Illuminated Express Highway Surface
                        drawRect(
                            color = Color(0xFF162032),
                            topLeft = Offset(0f, roadY),
                            size = Size(w, h - roadY)
                        )
                        drawLine(
                            color = accentColor,
                            start = Offset(0f, roadY),
                            end = Offset(w, roadY),
                            strokeWidth = 3f
                        )

                        // Moving dashed highway lane markers
                        for (dash in 0..8) {
                            val dashX = (((speedLineShift + dash * 0.13f) % 1f) * w)
                            drawLine(
                                color = Color(0xFFFACC15),
                                start = Offset(dashX, roadY + (h - roadY) * 0.52f),
                                end = Offset((dashX - 48f).coerceAtLeast(0f), roadY + (h - roadY) * 0.52f),
                                strokeWidth = 6f,
                                cap = StrokeCap.Round
                            )
                        }

                        // 5. Speeding Express Cruiser + Headlight Beam + Turbo Flame Trail
                        val carX = vehicleProgress * w
                        val carY = roadY - 52f

                        // Xenon Headlight Cone Beam ahead of vehicle
                        val beamPath = Path().apply {
                            moveTo(carX + 104f, carY + 26f)
                            lineTo(carX + 230f, carY - 4f)
                            lineTo(carX + 230f, carY + 52f)
                            close()
                        }
                        drawPath(
                            path = beamPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFEF08A).copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                startX = carX + 104f,
                                endX = carX + 230f
                            )
                        )

                        // Dual Neon Turbo Exhaust Trails behind vehicle
                        drawRoundRect(
                            color = secondaryAccentColor.copy(alpha = 0.75f),
                            topLeft = Offset(carX - 75f, carY + 12f),
                            size = Size(70f, 12f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.65f),
                            topLeft = Offset(carX - 52f, carY + 28f),
                            size = Size(48f, 10f),
                            cornerRadius = CornerRadius(5f, 5f)
                        )

                        // Main Aerodynamic Express Cargo Body
                        drawRoundRect(
                            color = secondaryAccentColor,
                            topLeft = Offset(carX, carY),
                            size = Size(92f, 42f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        // Glowing stripe on cargo box
                        drawRoundRect(
                            color = Color(0xFFFFD166),
                            topLeft = Offset(carX + 8f, carY + 18f),
                            size = Size(68f, 6f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        // Front Aerodynamic Cabin
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(carX + 70f, carY + 8f),
                            size = Size(36f, 34f),
                            cornerRadius = CornerRadius(9f, 9f)
                        )
                        // Cabin Tinted Windshield
                        drawRoundRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(carX + 82f, carY + 12f),
                            size = Size(20f, 14f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        // Glowing Alloy Wheels
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 13f,
                            center = Offset(carX + 24f, roadY)
                        )
                        drawCircle(
                            color = accentColor,
                            radius = 7f,
                            center = Offset(carX + 24f, roadY)
                        )
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 13f,
                            center = Offset(carX + 84f, roadY)
                        )
                        drawCircle(
                            color = accentColor,
                            radius = 7f,
                            center = Offset(carX + 84f, roadY)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.22f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "100% Automated Live Dispatch • Live Map & ETA Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            LinearProgressIndicator(
                color = secondaryAccentColor,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(7.dp)
            )

            OutlinedButton(
                onClick = onAnimationFinished,
                border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.85f)),
                modifier = Modifier.testTag("skip_intro_animation_btn")
            ) {
                Text("Enter Quick Delivery →", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/**
 * 2. Unified New User (Sign Up: Name, Mobile Number, Email ID) &
 *    Old User (Sign In: Mobile Number OR Email ID) Screen for both Customer & Rider Apps.
 */
@Composable
fun QuickDeliveryUserAuthScreen(
    roleTitle: String,
    accentColor: Color,
    prefsKeyPrefix: String,
    context: Context,
    onAuthenticated: (name: String, phone: String, email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = remember(context) {
        context.getSharedPreferences("quick_delivery_user_auth_prefs", Context.MODE_PRIVATE)
    }
    val uiStrings = rememberDynamicUiStrings()

    // Toggle between New User (Sign Up) and Old User (Sign In)
    val initialSignUp = remember(prefsKeyPrefix) {
        if (prefsKeyPrefix == "rider" && MotivationAndPromoManager.forceSignInTabNextOpen) false else true
    }
    var isNewUserSignUp by remember { mutableStateOf(initialSignUp) }

    val hasSavedLoggedOutProfile = remember(prefsKeyPrefix) {
        prefs.getBoolean("${prefsKeyPrefix}_logged_in", false)
    }

    // New User Sign Up fields start completely fresh unless already logged in
    var fullName by remember {
        mutableStateOf(if (hasSavedLoggedOutProfile) prefs.getString("${prefsKeyPrefix}_name", "") ?: "" else "")
    }
    var mobileNumber by remember {
        mutableStateOf(if (hasSavedLoggedOutProfile) prefs.getString("${prefsKeyPrefix}_phone", "") ?: "" else "")
    }
    var emailId by remember {
        mutableStateOf(if (hasSavedLoggedOutProfile) prefs.getString("${prefsKeyPrefix}_email", "") ?: "" else "")
    }

    // Old User Sign In field: Mobile Number OR Email ID (blank unless actively logged in)
    var existingMobileOrEmail by remember {
        mutableStateOf(if (hasSavedLoggedOutProfile) prefs.getString("${prefsKeyPrefix}_phone", "") ?: "" else "")
    }
    var allowPhoneStoragePermission by remember {
        mutableStateOf(prefs.getBoolean("${prefsKeyPrefix}_storage_permission_granted", true))
    }
    var acceptedPrivacyTerms by remember(prefsKeyPrefix, isNewUserSignUp) {
        mutableStateOf(
            if (isNewUserSignUp) false
            else prefs.getBoolean("${prefsKeyPrefix}_privacy_terms_accepted", false)
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingExistingAccount by remember { mutableStateOf(false) }
    var showSwitchToSignUpPrompt by remember { mutableStateOf(false) }
    var signupPhoneOverLimit by remember { mutableStateOf(false) }
    var signinPhoneOverLimit by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val authScope = rememberCoroutineScope()
    var emailOtpInput by remember { mutableStateOf("") }
    var emailOtpSending by remember { mutableStateOf(false) }
    var emailOtpVerifying by remember { mutableStateOf(false) }
    var emailOtpSentTo by remember { mutableStateOf<String?>(null) }
    var verifiedOtpEmail by remember { mutableStateOf<String?>(null) }
    var emailOtpStatus by remember { mutableStateOf<String?>(null) }
    var emailOtpIsError by remember { mutableStateOf(false) }

    if (showPrivacyPolicyDialog) {
        QuickDeliveryPrivacyPolicyDialog(
            context = context,
            onDismiss = { showPrivacyPolicyDialog = false },
            onAcceptAndAgree = {
                acceptedPrivacyTerms = true
                prefs.edit()
                    .putBoolean("${prefsKeyPrefix}_privacy_terms_accepted", true)
                    .putLong("${prefsKeyPrefix}_privacy_terms_accepted_at", System.currentTimeMillis())
                    .apply()
                errorMessage = null
                showPrivacyPolicyDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F9F9))
            .padding(16.dp)
            .testTag("${prefsKeyPrefix}_auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = accentColor,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (prefsKeyPrefix == "rider") {
                                        Icons.AutoMirrored.Filled.DirectionsBike
                                    } else {
                                        Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (prefsKeyPrefix == "rider") {
                                    "Partner Quick Delivery"
                                } else {
                                    "Quick Delivery • $roleTitle"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D1B2A)
                            )
                            Text(
                                text = if (isNewUserSignUp) {
                                    String.format(uiStrings.createAccountSubtitle, roleTitle)
                                } else {
                                    String.format(uiStrings.signInSubtitle, roleTitle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                    if (prefsKeyPrefix == "customer" || prefsKeyPrefix == "rider") {
                        LanguageSwitcherPill(accentColor = accentColor)
                    }
                }

                if (prefsKeyPrefix == "rider" && !MotivationAndPromoManager.riderApprovalBannerMessage.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_auth_approval_notice_banner")
                    ) {
                        Text(
                            text = MotivationAndPromoManager.riderApprovalBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Mode Switcher: New User (Sign Up) vs Existing User (Sign In)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = isNewUserSignUp,
                        onClick = {
                            isNewUserSignUp = true
                            errorMessage = null
                            showSwitchToSignUpPrompt = false
                        },
                        label = {
                            Text(
                                text = uiStrings.newUserSignUpTab,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${prefsKeyPrefix}_tab_new_signup")
                    )
                    FilterChip(
                        selected = !isNewUserSignUp,
                        onClick = {
                            isNewUserSignUp = false
                            errorMessage = null
                            showSwitchToSignUpPrompt = false
                        },
                        label = {
                            Text(
                                text = uiStrings.existingSignInTab,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${prefsKeyPrefix}_tab_old_signin")
                    )
                }

                if (isNewUserSignUp) {
                    // NEW USER SIGN UP: Full Name + Mobile Number + Email Address
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text(uiStrings.fullNameLabel) },
                        placeholder = { Text(uiStrings.fullNamePlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signup_name_input")
                    )

                    val signupPhoneError = evaluateIndianMobileError(
                        digits = mobileNumber,
                        overLimitAttempted = signupPhoneOverLimit
                    )
                    val isSignupPhoneValid = mobileNumber.length == 10 &&
                        !QuickDeliverySecurityGuard.isFakeOrDummyPhone(mobileNumber)
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { raw ->
                            val parsed = processIndianMobileInput(raw)
                            mobileNumber = parsed.sanitizedDigits
                            signupPhoneOverLimit = parsed.overLimitAttempted
                            if (errorMessage != null) errorMessage = null
                        },
                        label = {
                            Text(
                                text = uiStrings.mobileNumberLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = { Text("10-digit mobile number") },
                        leadingIcon = { IndianPhonePrefixBadge(accentColor = accentColor) },
                        trailingIcon = {
                            if (isSignupPhoneValid && !signupPhoneOverLimit) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid 10-digit mobile",
                                    tint = Color(0xFF059669)
                                )
                            }
                        },
                        isError = signupPhoneError != null,
                        supportingText = signupPhoneError?.let { errText ->
                            {
                                Text(
                                    text = errText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.testTag("${prefsKeyPrefix}_signup_phone_error")
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signup_phone_input")
                    )

                    OutlinedTextField(
                        value = emailId,
                        onValueChange = {
                            emailId = it
                            if (!it.trim().equals(verifiedOtpEmail, ignoreCase = true)) {
                                verifiedOtpEmail = null
                            }
                            if (errorMessage != null) errorMessage = null
                        },
                        label = {
                            Text(
                                text = uiStrings.emailAddressLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = { Text("name@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signup_email_input")
                    )
                } else {
                    // EXISTING USER SIGN IN: 10-Digit Mobile Number (+91) OR Email ID
                    val isSignInEmailMode = existingMobileOrEmail.contains("@") ||
                        existingMobileOrEmail.any(Char::isLetter)
                    val isSignInPhoneMode = !isSignInEmailMode
                    val signInDigits = existingMobileOrEmail.filter(Char::isDigit)
                    val showIndianPrefixBadge = isSignInPhoneMode && signInDigits.isNotEmpty()
                    val signInPhoneError = if (isSignInPhoneMode) {
                        when {
                            signinPhoneOverLimit -> "Only 10 digits allowed (cannot exceed 10 digits)"
                            signInDigits.isEmpty() -> null
                            signInDigits.length < 10 -> "Enter exact 10-digit mobile number (${signInDigits.length}/10) or Email"
                            QuickDeliverySecurityGuard.isFakeOrDummyPhone(signInDigits) -> "Enter a valid 10-digit Indian mobile number"
                            else -> null
                        }
                    } else null
                    val isSignInPhoneValid = isSignInPhoneMode &&
                        signInDigits.length == 10 &&
                        !QuickDeliverySecurityGuard.isFakeOrDummyPhone(signInDigits)

                    OutlinedTextField(
                        value = existingMobileOrEmail,
                        onValueChange = { raw ->
                            val looksEmailInput = raw.contains("@") || raw.any(Char::isLetter)
                            if (looksEmailInput) {
                                signinPhoneOverLimit = false
                                existingMobileOrEmail = raw
                            } else {
                                val parsed = processIndianMobileInput(raw)
                                existingMobileOrEmail = parsed.sanitizedDigits
                                signinPhoneOverLimit = parsed.overLimitAttempted
                            }
                            if (!existingMobileOrEmail.trim().equals(verifiedOtpEmail, ignoreCase = true)) {
                                verifiedOtpEmail = null
                            }
                            if (errorMessage != null) errorMessage = null
                            if (showSwitchToSignUpPrompt) showSwitchToSignUpPrompt = false
                        },
                        label = {
                            Text(
                                text = uiStrings.mobileOrEmailLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = uiStrings.mobileOrEmailPlaceholder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            when {
                                showIndianPrefixBadge -> IndianPhonePrefixBadge(accentColor = accentColor)
                                isSignInEmailMode -> Icon(Icons.Default.Email, contentDescription = null, tint = accentColor)
                                else -> Icon(Icons.Default.Person, contentDescription = null, tint = accentColor)
                            }
                        },
                        trailingIcon = {
                            if (isSignInPhoneValid && !signinPhoneOverLimit) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid 10-digit mobile",
                                    tint = Color(0xFF059669)
                                )
                            }
                        },
                        isError = signInPhoneError != null,
                        supportingText = signInPhoneError?.let { errText ->
                            {
                                Text(
                                    text = errText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.testTag("${prefsKeyPrefix}_signin_phone_error")
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signin_identifier_input")
                    )
                }

                val targetOtpEmail = if (isNewUserSignUp) {
                    QuickDeliverySecurityGuard.sanitizeEmail(emailId)
                } else {
                    val cleanIdent = existingMobileOrEmail.trim()
                    if (cleanIdent.contains("@") && cleanIdent.contains(".")) {
                        QuickDeliverySecurityGuard.sanitizeEmail(cleanIdent)
                    } else ""
                }
                val isEmailVerifiedForTarget = targetOtpEmail.isNotBlank() &&
                    verifiedOtpEmail.equals(targetOtpEmail, ignoreCase = true)

                if (isNewUserSignUp || targetOtpEmail.isNotBlank()) {
                    Surface(
                        color = if (isEmailVerifiedForTarget) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isEmailVerifiedForTarget) Color(0xFF10B981) else accentColor.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_email_otp_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = if (isEmailVerifiedForTarget) {
                                            "✅ Email Verified"
                                        } else {
                                            "📧 Email OTP Verification"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEmailVerifiedForTarget) Color(0xFF065F46) else Color(0xFF0D1B2A),
                                        maxLines = 2
                                    )
                                    Text(
                                        text = if (isEmailVerifiedForTarget) {
                                            "$targetOtpEmail verified"
                                        } else {
                                            "From support@quickdelivery.co.in"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isEmailVerifiedForTarget) Color(0xFF047857) else Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                                if (!isEmailVerifiedForTarget) {
                                    Button(
                                        enabled = !emailOtpSending && targetOtpEmail.contains("@") && targetOtpEmail.contains("."),
                                        onClick = {
                                            emailOtpSending = true
                                            emailOtpStatus = null
                                            emailOtpIsError = false
                                            authScope.launch {
                                                com.example.data.cloud.MultiPhoneLiveSyncManager.sendSignUpEmailOtp(
                                                    context = context,
                                                    email = targetOtpEmail,
                                                    role = prefsKeyPrefix.uppercase()
                                                ).onSuccess { msg ->
                                                    emailOtpSending = false
                                                    emailOtpSentTo = targetOtpEmail
                                                    emailOtpIsError = false
                                                    emailOtpStatus = msg
                                                }.onFailure { err ->
                                                    emailOtpSending = false
                                                    emailOtpIsError = true
                                                    emailOtpStatus = err.message ?: "Failed to send Email OTP."
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 96.dp, minHeight = 40.dp)
                                            .testTag("${prefsKeyPrefix}_send_email_otp_btn")
                                    ) {
                                        if (emailOtpSending) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (emailOtpSentTo.equals(targetOtpEmail, ignoreCase = true)) {
                                                    "Resend"
                                                } else {
                                                    "Send OTP"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isEmailVerifiedForTarget && (emailOtpSentTo.equals(targetOtpEmail, ignoreCase = true) || emailOtpInput.isNotEmpty())) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = emailOtpInput,
                                        onValueChange = { raw ->
                                            if (raw.length <= 6) emailOtpInput = raw.filter(Char::isDigit)
                                        },
                                        label = { Text("6-Digit OTP") },
                                        placeholder = { Text("Enter OTP") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("${prefsKeyPrefix}_email_otp_input")
                                    )
                                    Button(
                                        enabled = !emailOtpVerifying && emailOtpInput.length == 6,
                                        onClick = {
                                            emailOtpVerifying = true
                                            emailOtpStatus = null
                                            emailOtpIsError = false
                                            authScope.launch {
                                                com.example.data.cloud.MultiPhoneLiveSyncManager.verifySignUpEmailOtp(
                                                    context = context,
                                                    email = targetOtpEmail,
                                                    otp = emailOtpInput,
                                                    role = prefsKeyPrefix.uppercase()
                                                ).onSuccess { msg ->
                                                    emailOtpVerifying = false
                                                    verifiedOtpEmail = targetOtpEmail
                                                    emailOtpIsError = false
                                                    emailOtpStatus = msg
                                                    errorMessage = null
                                                }.onFailure { err ->
                                                    emailOtpVerifying = false
                                                    emailOtpIsError = true
                                                    emailOtpStatus = err.message ?: "Invalid OTP."
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .height(50.dp)
                                            .testTag("${prefsKeyPrefix}_verify_email_otp_btn")
                                    ) {
                                        if (emailOtpVerifying) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Verify", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }

                            if (!emailOtpStatus.isNullOrBlank()) {
                                Text(
                                    text = emailOtpStatus.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (emailOtpIsError) Color(0xFFB91C1C) else Color(0xFF065F46)
                                )
                            }
                        }
                    }
                }

                // COMPACT REMEMBER LOGIN STRIP
                Surface(
                    color = if (allowPhoneStoragePermission) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (allowPhoneStoragePermission) Color(0xFF10B981) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${prefsKeyPrefix}_storage_permission_box")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiStrings.keepMeSignedIn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D1B2A),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = allowPhoneStoragePermission,
                            onCheckedChange = { allowPhoneStoragePermission = it },
                            modifier = Modifier
                                .scale(0.78f)
                                .testTag("${prefsKeyPrefix}_storage_permission_switch")
                        )
                    }
                }

                // COMPACT PRIVACY POLICY & TERMS CONSENT STRIP (SHOWN ONLY ON NEW USER SIGN UP)
                if (isNewUserSignUp) {
                    Surface(
                        color = if (acceptedPrivacyTerms) Color(0xFFECFDF5) else Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (acceptedPrivacyTerms) Color(0xFF10B981) else Color(0xFFF97316)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !acceptedPrivacyTerms
                                acceptedPrivacyTerms = next
                                if (next) {
                                    prefs.edit()
                                        .putBoolean("${prefsKeyPrefix}_privacy_terms_accepted", true)
                                        .putLong("${prefsKeyPrefix}_privacy_terms_accepted_at", System.currentTimeMillis())
                                        .apply()
                                    errorMessage = null
                                }
                            }
                            .testTag("${prefsKeyPrefix}_privacy_terms_consent_box")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = acceptedPrivacyTerms,
                                onCheckedChange = { checked ->
                                    acceptedPrivacyTerms = checked
                                    if (checked) {
                                        prefs.edit()
                                            .putBoolean("${prefsKeyPrefix}_privacy_terms_accepted", true)
                                            .putLong("${prefsKeyPrefix}_privacy_terms_accepted_at", System.currentTimeMillis())
                                            .apply()
                                        errorMessage = null
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = Color(0xFFEA580C)
                                ),
                                modifier = Modifier
                                    .scale(0.86f)
                                    .testTag("${prefsKeyPrefix}_privacy_terms_checkbox")
                            )
                            Text(
                                text = "I agree to ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "Privacy Policy & Terms",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clickable { showPrivacyPolicyDialog = true }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                                    .testTag("${prefsKeyPrefix}_privacy_policy_read_link")
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.testTag("${prefsKeyPrefix}_auth_error_text")
                    )
                }

                if (showSwitchToSignUpPrompt && !isNewUserSignUp) {
                    OutlinedButton(
                        onClick = {
                            val trimmed = existingMobileOrEmail.trim()
                            if (trimmed.contains("@")) {
                                emailId = trimmed
                            } else {
                                val parsed = processIndianMobileInput(trimmed)
                                mobileNumber = parsed.sanitizedDigits
                            }
                            isNewUserSignUp = true
                            errorMessage = null
                            showSwitchToSignUpPrompt = false
                        },
                        border = BorderStroke(1.5.dp, accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("${prefsKeyPrefix}_switch_to_signup_btn")
                    ) {
                        Text(
                            text = uiStrings.goToSignUpBtn,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val isAuthButtonEnabled = !isCheckingExistingAccount && (if (isNewUserSignUp) acceptedPrivacyTerms else true)

                Button(
                    enabled = isAuthButtonEnabled,
                    onClick = {
                        if (isNewUserSignUp && !acceptedPrivacyTerms) {
                            errorMessage = "Please accept Privacy Policy & Terms first."
                            return@Button
                        }
                        val rateCheck = QuickDeliverySecurityGuard.checkAndRecordEndpointCall(
                            endpoint = QuickDeliverySecurityGuard.ENDPOINT_AUTH_ATTEMPT,
                            actorKey = prefsKeyPrefix
                        )
                        if (rateCheck.isFailure) {
                            errorMessage = rateCheck.exceptionOrNull()?.message
                            return@Button
                        }
                        prefs.edit()
                            .putBoolean("${prefsKeyPrefix}_privacy_terms_accepted", true)
                            .putLong("${prefsKeyPrefix}_privacy_terms_accepted_at", System.currentTimeMillis())
                            .apply()
                        if (isNewUserSignUp) {
                            val finalName = QuickDeliverySecurityGuard.sanitizeText(fullName, 80)
                            val finalPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(mobileNumber)
                            val finalEmail = QuickDeliverySecurityGuard.sanitizeEmail(emailId)
                            when {
                                finalName.length < 2 -> {
                                    errorMessage = "Please enter your valid Full Name (minimum 2 characters)."
                                    return@Button
                                }
                                finalPhone.length != 10 || QuickDeliverySecurityGuard.isFakeOrDummyPhone(finalPhone) -> {
                                    errorMessage = "Please enter a valid 10-digit Indian Mobile Number (+91)."
                                    return@Button
                                }
                                !finalEmail.contains("@") || !finalEmail.contains(".") -> {
                                    errorMessage = "Please enter a valid Email Address."
                                    return@Button
                                }
                                !verifiedOtpEmail.equals(finalEmail, ignoreCase = true) -> {
                                    errorMessage = "Please tap 'Send OTP' and verify the Email OTP sent to $finalEmail."
                                    return@Button
                                }
                            }
                            MotivationAndPromoManager.lastAuthWasNewSignUp = true
                            MotivationAndPromoManager.markNewSignUpAppGuidePending(context, prefsKeyPrefix)
                            MotivationAndPromoManager.forceSignInTabNextOpen = false
                            MotivationAndPromoManager.riderApprovalBannerMessage = null
                            MotivationAndPromoManager.registerNewAccountLocally(
                                context = context,
                                rolePrefix = prefsKeyPrefix,
                                name = finalName,
                                phone = finalPhone,
                                email = finalEmail
                            )
                            authScope.launch {
                                com.example.data.cloud.MultiPhoneLiveSyncManager.registerAccountOnServer(
                                    context = context,
                                    role = prefsKeyPrefix.uppercase(),
                                    name = finalName,
                                    phone = finalPhone,
                                    email = finalEmail
                                )
                                com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                            }
                            MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
                                context = context,
                                rolePrefix = prefsKeyPrefix,
                                name = finalName,
                                phone = finalPhone,
                                email = finalEmail,
                                storageAllowed = allowPhoneStoragePermission
                            )
                            onAuthenticated(finalName, finalPhone, finalEmail)
                        } else {
                            val identifier = QuickDeliverySecurityGuard.sanitizeText(existingMobileOrEmail, 120)
                            val digitsOnly = identifier.filter(Char::isDigit)
                            val looksLikePhone = digitsOnly.length == 10 &&
                                !identifier.contains("@") &&
                                !QuickDeliverySecurityGuard.isFakeOrDummyPhone(digitsOnly)
                            val looksLikeEmail = identifier.contains("@") && identifier.contains(".")
                            if (!looksLikePhone && !looksLikeEmail) {
                                errorMessage = "Please enter a valid 10-digit Mobile Number (+91) or Email Address."
                                showSwitchToSignUpPrompt = false
                                return@Button
                            }
                            val localRegistered = MotivationAndPromoManager.findLocallyRegisteredAccount(
                                context = context,
                                rolePrefix = prefsKeyPrefix,
                                identifier = identifier
                            )
                            if (localRegistered != null) {
                                val resolvedName = localRegistered.name.ifBlank { roleTitle }
                                val resolvedPhone = localRegistered.phone.ifBlank { if (looksLikePhone) digitsOnly else "" }
                                val resolvedEmail = localRegistered.email.ifBlank {
                                    if (looksLikeEmail) QuickDeliverySecurityGuard.sanitizeEmail(identifier) else ""
                                }
                                showSwitchToSignUpPrompt = false
                                MotivationAndPromoManager.lastAuthWasNewSignUp = false
                                MotivationAndPromoManager.dismissNewSignUpAppGuide(context, prefsKeyPrefix)
                                MotivationAndPromoManager.forceSignInTabNextOpen = false
                                MotivationAndPromoManager.riderApprovalBannerMessage = null
                                MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
                                    context = context,
                                    rolePrefix = prefsKeyPrefix,
                                    name = resolvedName,
                                    phone = resolvedPhone,
                                    email = resolvedEmail,
                                    storageAllowed = allowPhoneStoragePermission
                                )
                                onAuthenticated(resolvedName, resolvedPhone, resolvedEmail)
                            } else {
                                // Strictly verify against live cloud registry (FAIL-CLOSED: never allow unregistered login!)
                                isCheckingExistingAccount = true
                                errorMessage = null
                                showSwitchToSignUpPrompt = false
                                authScope.launch {
                                    val checkRes = com.example.data.cloud.MultiPhoneLiveSyncManager.verifyExistingAccountOnServer(
                                        context = context,
                                        role = prefsKeyPrefix.uppercase(),
                                        identifier = identifier
                                    )
                                    isCheckingExistingAccount = false
                                    checkRes.onSuccess { remoteAcc ->
                                        if (remoteAcc != null) {
                                            val resolvedName = remoteAcc.name.ifBlank { roleTitle }
                                            val resolvedPhone = remoteAcc.phone.ifBlank { if (looksLikePhone) digitsOnly else "" }
                                            val resolvedEmail = remoteAcc.email.ifBlank {
                                                if (looksLikeEmail) QuickDeliverySecurityGuard.sanitizeEmail(identifier) else ""
                                            }
                                            MotivationAndPromoManager.registerNewAccountLocally(
                                                context = context,
                                                rolePrefix = prefsKeyPrefix,
                                                name = resolvedName,
                                                phone = resolvedPhone,
                                                email = resolvedEmail
                                            )
                                            MotivationAndPromoManager.lastAuthWasNewSignUp = false
                                            MotivationAndPromoManager.dismissNewSignUpAppGuide(context, prefsKeyPrefix)
                                            MotivationAndPromoManager.forceSignInTabNextOpen = false
                                            MotivationAndPromoManager.riderApprovalBannerMessage = null
                                            MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
                                                context = context,
                                                rolePrefix = prefsKeyPrefix,
                                                name = resolvedName,
                                                phone = resolvedPhone,
                                                email = resolvedEmail,
                                                storageAllowed = allowPhoneStoragePermission
                                            )
                                            onAuthenticated(resolvedName, resolvedPhone, resolvedEmail)
                                        } else {
                                            showSwitchToSignUpPrompt = true
                                            errorMessage = uiStrings.accountNotFoundErrorMessage
                                        }
                                    }.onFailure {
                                        showSwitchToSignUpPrompt = true
                                        errorMessage = uiStrings.accountVerificationFailedErrorMessage
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        disabledContainerColor = Color(0xFFCBD5E1),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("${prefsKeyPrefix}_auth_submit_btn")
                ) {
                    if (isCheckingExistingAccount) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verifying Account...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Text(
                            text = if (isNewUserSignUp) {
                                uiStrings.createAccountContinueBtn
                            } else {
                                uiStrings.signInContinueBtn
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                GoogleSignInButton(
                    enabled = isAuthButtonEnabled,
                    onAuthSuccess = {
                        prefs.edit()
                            .putBoolean("${prefsKeyPrefix}_privacy_terms_accepted", true)
                            .putLong("${prefsKeyPrefix}_privacy_terms_accepted_at", System.currentTimeMillis())
                            .apply()
                        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
                        val googleEmail = QuickDeliverySecurityGuard.sanitizeEmail(user?.email)
                        val googleName = QuickDeliverySecurityGuard.sanitizeText(user?.displayName, 80)
                            .ifBlank {
                                googleEmail.substringBefore("@").ifBlank { roleTitle }
                            }
                        val googlePhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(mobileNumber)
                        MotivationAndPromoManager.lastAuthWasNewSignUp = isNewUserSignUp
                        if (isNewUserSignUp) {
                            MotivationAndPromoManager.markNewSignUpAppGuidePending(context, prefsKeyPrefix)
                        } else {
                            MotivationAndPromoManager.dismissNewSignUpAppGuide(context, prefsKeyPrefix)
                        }
                        MotivationAndPromoManager.forceSignInTabNextOpen = false
                        MotivationAndPromoManager.riderApprovalBannerMessage = null
                        MotivationAndPromoManager.registerNewAccountLocally(
                            context = context,
                            rolePrefix = prefsKeyPrefix,
                            name = googleName,
                            phone = googlePhone,
                            email = googleEmail
                        )
                        authScope.launch {
                            com.example.data.cloud.MultiPhoneLiveSyncManager.registerAccountOnServer(
                                context = context,
                                role = prefsKeyPrefix.uppercase(),
                                name = googleName,
                                phone = googlePhone,
                                email = googleEmail
                            )
                            com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                        }
                        MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
                            context = context,
                            rolePrefix = prefsKeyPrefix,
                            name = googleName,
                            phone = googlePhone,
                            email = googleEmail,
                            storageAllowed = allowPhoneStoragePermission
                        )
                        onAuthenticated(googleName, googlePhone, googleEmail)
                    },
                    onAuthError = { errMsg ->
                        errorMessage = errMsg
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isNewUserSignUp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyPolicyDialog = true }
                            .padding(vertical = 2.dp)
                            .testTag("${prefsKeyPrefix}_privacy_policy_link"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy Policy",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = uiStrings.viewPrivacyTermsFooter,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

fun requiredPhoneStoragePermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q ->
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

fun hasPhoneStoragePermission(context: Context): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        }
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
            val readOk = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val writeOk = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            readOk && writeOk
        }
        else -> {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

fun hasAllMandatoryAppPermissions(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val storageGranted = hasPhoneStoragePermission(context)
    return (fineGranted || coarseGranted) && cameraGranted && notificationsGranted && storageGranted
}

/**
 * Mandatory Post-Login Permission Gate for both Customer & Partner Rider Apps.
 * Ensures Location, Camera, Notifications & Phone Storage permissions are granted before proceeding to the dashboard.
 */
@Composable
fun QuickDeliveryMandatoryPermissionsGateScreen(
    roleTitle: String,
    prefsKeyPrefix: String,
    accentColor: Color,
    onAllPermissionsGranted: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiStrings = rememberDynamicUiStrings()

    fun checkLocationGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun checkCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun checkNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var locationGranted by remember { mutableStateOf(checkLocationGranted()) }
    var cameraGranted by remember { mutableStateOf(checkCameraGranted()) }
    var notificationsGranted by remember { mutableStateOf(checkNotificationsGranted()) }
    var storageGranted by remember { mutableStateOf(hasPhoneStoragePermission(context)) }
    var attemptedRequest by remember { mutableStateOf(false) }

    val refreshPermissions = {
        locationGranted = checkLocationGranted()
        cameraGranted = checkCameraGranted()
        notificationsGranted = checkNotificationsGranted()
        storageGranted = hasPhoneStoragePermission(context)
    }

    val allGranted = locationGranted && cameraGranted && notificationsGranted && storageGranted

    LaunchedEffect(allGranted) {
        if (allGranted) {
            MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context)
            onAllPermissionsGranted()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        attemptedRequest = true
        refreshPermissions()
    }

    val launchMissingPermissionsRequest = {
        val missing = buildList {
            if (!locationGranted) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (!cameraGranted) {
                add(Manifest.permission.CAMERA)
            }
            if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!storageGranted) {
                requiredPhoneStoragePermissions().forEach { add(it) }
            }
        }.toTypedArray()
        if (missing.isEmpty()) {
            onAllPermissionsGranted()
        } else {
            permissionsLauncher.launch(missing)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F9F9))
            .padding(18.dp)
            .testTag("${prefsKeyPrefix}_mandatory_permissions_gate"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Required App Permissions",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = uiStrings.permissionsRequiredTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D1B2A)
                        )
                        Text(
                            text = String.format(uiStrings.permissionsRequiredSubtitle, roleTitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                PermissionGateItemRow(
                    icon = Icons.Default.MyLocation,
                    title = uiStrings.locationPermissionTitle,
                    description = if (prefsKeyPrefix == "rider") {
                        "Needed for live order dispatch, pickup/drop navigation & live ETA."
                    } else {
                        "Needed for 1-tap current pickup address capture & live rider tracking."
                    },
                    isGranted = locationGranted,
                    accentColor = accentColor
                )

                PermissionGateItemRow(
                    icon = Icons.Default.CameraAlt,
                    title = uiStrings.cameraPermissionTitle,
                    description = if (prefsKeyPrefix == "rider") {
                        "Needed for live pickup/delivery proof photos & partner selfie verification."
                    } else {
                        "Needed to attach parcel safety photos before dispatch."
                    },
                    isGranted = cameraGranted,
                    accentColor = accentColor
                )

                PermissionGateItemRow(
                    icon = Icons.Default.NotificationsActive,
                    title = uiStrings.notificationsPermissionTitle,
                    description = "Needed for real-time order status alerts & instant Rider-Customer chat.",
                    isGranted = notificationsGranted,
                    accentColor = accentColor
                )

                PermissionGateItemRow(
                    icon = Icons.Default.Shield,
                    title = uiStrings.storagePermissionTitle,
                    description = "Needed to keep an encrypted backup of your profile, saved addresses & orders safely stored on your phone.",
                    isGranted = storageGranted,
                    accentColor = accentColor
                )

                if (attemptedRequest && !allGranted) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Some required permissions are still denied. Please tap Allow below or enable them in App Settings to continue.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = launchMissingPermissionsRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("${prefsKeyPrefix}_allow_permissions_btn")
                ) {
                    Text(
                        text = if (allGranted) "Continue →" else uiStrings.allowPermissionsBtn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (attemptedRequest && !allGranted) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val settingsIntent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(settingsIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("${prefsKeyPrefix}_open_app_settings_btn")
                    ) {
                        Text("Open App Settings", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Sign Out / Switch Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGateItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    accentColor: Color
) {
    Surface(
        color = if (isGranted) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isGranted) Color(0xFF10B981) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isGranted) Color(0xFF10B981) else accentColor.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                        contentDescription = title,
                        tint = if (isGranted) Color.White else accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Surface(
                        color = if (isGranted) Color(0xFFDCFCE7) else Color(0xFFFFEDD5),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isGranted) "Allowed" else "Required",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGranted) Color(0xFF047857) else Color(0xFFC2410C),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
fun QuickDeliveryPrivacyPolicyDialog(
    context: Context,
    onDismiss: () -> Unit,
    onAcceptAndAgree: (() -> Unit)? = null
) {
    val publicPrivacyUrl = com.example.data.cloud.QuickDeliveryBrandLinks.PRIVACY_POLICY_URL
    val publicTermsUrl = com.example.data.cloud.QuickDeliveryBrandLinks.TERMS_OF_SERVICE_URL

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Privacy Policy",
                    tint = Color(0xFF008080),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy Policy & Terms of Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1B2A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Delivery Logistics enforces strict privacy, safety, and delivery terms across Customer & Rider Partner accounts:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1B2A)
                )
                Text(
                    text = "1. Account Isolation & Identity: Your mobile number and email strictly isolate your orders, GST invoices, and saved addresses so no other user can view your bookings.\n\n" +
                        "2. Essential Permissions Only:\n" +
                        "   • Location (GPS): Used only while the app is open/on active delivery for accurate pickup/drop addresses, fare distance, and live rider tracking.\n" +
                        "   • Camera: Triggered only when you capture parcel safety photos, pickup/delivery proofs, or Rider KYC selfies.\n" +
                        "   • Phone Storage / Media: Used strictly to store encrypted offline backups of your profile, saved addresses, and order history in Phone Storage (Documents/QuickDelivery) and attach KYC/parcel photos.\n" +
                        "   • Notifications: Used for instant order updates, OTP alerts, and live Rider-Customer chat.\n\n" +
                        "3. Prohibited Parcels & Liability: Sending illegal, hazardous, flammable, stolen, or prohibited goods is strictly banned. Senders are solely liable for accurate parcel declaration.\n\n" +
                        "4. Payments, Cancellation & Rider Terms: Fair fare splits, OTP + Digital Signature Proof-of-Delivery, pickup wait-time & cancellation rules, and traffic/KYC compliance apply.\n\n" +
                        "5. Data Security: All traffic uses HTTPS/TLS encryption and HMAC-SHA256 verification. We never sell personal data to third parties.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF334155)
                )
                Text(
                    text = "Account & Data Deletion: Delete your profile anytime from Menu → Delete Account & Data, visit ${com.example.data.cloud.QuickDeliveryBrandLinks.DELETE_ACCOUNT_URL}, or email ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F766E)
                )
            }
        },
        confirmButton = {
            if (onAcceptAndAgree != null) {
                Button(
                    onClick = onAcceptAndAgree,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080))
                ) {
                    Text("I Agree & Accept", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(publicPrivacyUrl))
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080))
                ) {
                    Text("View Online Policy", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(publicPrivacyUrl))
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Online Policy")
                }
                TextButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(publicTermsUrl))
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Terms")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun QuickDeliveryDeleteAccountDialog(
    roleTitle: String,
    accountIdentifier: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Account",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete $roleTitle Account?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF991B1B)
                )
            }
        },
        text = {
            Text(
                text = "This will permanently erase your saved profile, session tokens, and local login backup for \"$accountIdentifier\" from this device and sign you out. To request server log deletion as well, visit ${com.example.data.cloud.QuickDeliveryBrandLinks.DELETE_ACCOUNT_URL} or email ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF334155)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("confirm_delete_account_btn")
            ) {
                Text("Delete Account & Data", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Keep Account")
            }
        }
    )
}

/**
 * 2B. Saved Account & Quick Login Status Card for both Customer App and Rider App.
 */
@Composable
fun PhoneStorageLoginBackupCard(
    rolePrefix: String,
    roleTitle: String,
    userName: String,
    userPhone: String,
    userEmail: String,
    accentColor: Color,
    context: Context,
    onOpenLoginScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(context) {
        MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context)
    }

    val isStorageGranted by if (rolePrefix == "rider") {
        MotivationAndPromoManager.riderStoragePermissionGranted.collectAsState()
    } else {
        MotivationAndPromoManager.customerStoragePermissionGranted.collectAsState()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isStorageGranted) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.2.dp,
            if (isStorageGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("${rolePrefix}_phone_storage_login_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Account Status",
                        tint = if (isStorageGranted) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isStorageGranted) {
                                "Signed In as $userName"
                            } else {
                                "Remember $roleTitle Login"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D1B2A)
                        )
                        Text(
                            text = if (isStorageGranted) {
                                "$roleTitle • $userPhone"
                            } else {
                                "Save your $roleTitle account on this device for quick sign-in."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenLoginScreen,
                    modifier = Modifier.testTag("${rolePrefix}_open_saved_login_btn")
                ) {
                    Text("Account", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            if (!isStorageGranted) {
                Button(
                    onClick = {
                        MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
                            context = context,
                            rolePrefix = rolePrefix,
                            name = userName,
                            phone = userPhone,
                            email = userEmail,
                            storageAllowed = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${rolePrefix}_allow_storage_permission_btn")
                ) {
                    Text(
                        text = "Remember My $roleTitle Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 3. Rider Motivation Notification Card (displayed whenever Rider picks up an order, with a new message each time).
 */
@Composable
fun RiderMotivationNotificationBanner(
    message: String?,
    onDismiss: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .testTag("rider_motivation_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF59E0B),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Rider Motivation",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Partner Motivation Boost",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF78350F)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF92400E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 4. Customer Notification-Style Promo Banner (encourages customer to book another order).
 */
@Composable
fun CustomerOrderMoreNotificationBanner(
    message: String?,
    onBookAnotherOrderClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_promo_notification_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF059669),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Customer Notification",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onBookAnotherOrderClick() }
                ) {
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                    Text(
                        text = "Tap here to book another parcel →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF065F46),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 5. Continuous Live Rider Tracking Card (starts live tracking as soon as the Rider accepts the order
 *    and continues tracking through Pickup and In-Transit until Delivered).
 */
@Composable
fun LiveRiderContinuousTrackingCard(
    order: CustomerOrder,
    photoProofSummary: String?,
    liveRiderGpsCoordinates: String?,
    onRefreshLiveGps: () -> Unit,
    onBookNewOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRiderAcceptedOrBeyond = order.orderStatus != OrderStatus.SEARCHING_RIDER &&
        order.orderStatus != OrderStatus.CANCELLED
    val isDelivered = order.orderStatus == OrderStatus.DELIVERED

    val stageProgress = when (order.orderStatus) {
        OrderStatus.SEARCHING_RIDER -> 0.1f
        OrderStatus.RIDER_ASSIGNED -> 0.35f
        OrderStatus.ARRIVED_AT_PICKUP -> 0.5f
        OrderStatus.PICKED_UP -> 0.72f
        OrderStatus.IN_TRANSIT -> 0.88f
        OrderStatus.DELIVERED -> 1.0f
        OrderStatus.CANCELLED -> 0f
    }

    val animatedStageProgress by animateFloatAsState(
        targetValue = stageProgress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "smoothDeliveryStageProgress"
    )

    // Automatically refresh GPS coordinates every second so the Rider moves EVERY MOMENT on the Customer's Live Map!
    var tickCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(order.orderStatus, isRiderAcceptedOrBeyond) {
        if (isRiderAcceptedOrBeyond && !isDelivered) {
            onRefreshLiveGps()
            while (true) {
                delay(1000)
                tickCount += 1
            }
        }
    }

    val infiniteMapMotion = rememberInfiniteTransition(label = "live_rider_every_moment_motion")
    val microStepOffset by infiniteMapMotion.animateFloat(
        initialValue = 0f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "micro_step_offset"
    )

    val continuousLiveProgress = if (isDelivered) {
        1.0f
    } else if (!isRiderAcceptedOrBeyond) {
        0.08f
    } else {
        (animatedStageProgress + microStepOffset + (tickCount % 12) * 0.006f).coerceIn(0.12f, 0.96f)
    }

    val simulatedLat = remember(order.orderStatus, tickCount, continuousLiveProgress) {
        val baseLat = order.pickupAddress.lat
        val dropLat = order.dropAddress.lat
        baseLat + (dropLat - baseLat) * continuousLiveProgress
    }
    val simulatedLng = remember(order.orderStatus, tickCount, continuousLiveProgress) {
        val baseLng = order.pickupAddress.lng
        val dropLng = order.dropAddress.lng
        baseLng + (dropLng - baseLng) * continuousLiveProgress
    }

    val waitTimersByOrder by com.example.data.cloud.QuickDeliveryOperationsManager.waitTimerStartByOrder.collectAsState()
    val riderLiveCoordsByOrder by com.example.data.cloud.QuickDeliveryOperationsManager.riderLiveCoordsByOrder.collectAsState()
    val remoteFlags by com.example.data.cloud.QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
    val liveGpsEtaPair = remember(order.orderId, order.orderStatus, riderLiveCoordsByOrder) {
        com.example.data.cloud.QuickDeliveryOperationsManager.computeLiveRiderEtaKmAndMins(order)
    }
    val waitFeeInfo = remember(order.orderId, waitTimersByOrder, remoteFlags, tickCount) {
        if (remoteFlags.waitTimeAndCancelRuleEnabled) {
            com.example.data.cloud.QuickDeliveryOperationsManager.computeWaitFeeInfo(order.orderId, remoteFlags)
        } else null
    }

    // Calculate Live Remaining Distance (km) & Approximate ETA (mins + secs)
    val totalDistanceKm = order.distanceKm.coerceAtLeast(1.8)
    val remainingDistanceKm = if (isDelivered) {
        0.0
    } else if (liveGpsEtaPair != null) {
        liveGpsEtaPair.first.coerceAtLeast(0.05)
    } else {
        (totalDistanceKm * (1.0f - continuousLiveProgress)).coerceAtLeast(0.15)
    }
    // Assume average city speed ~26 km/h (~2.3 mins per km)
    val totalRemainingSeconds = if (isDelivered) {
        0
    } else if (liveGpsEtaPair != null) {
        (liveGpsEtaPair.second * 60 - (tickCount % 45)).coerceAtLeast(30)
    } else {
        ((remainingDistanceKm * 138.0).roundToInt() - (tickCount % 45)).coerceAtLeast(35)
    }
    val approxEtaMinutes = totalRemainingSeconds / 60
    val approxEtaSeconds = totalRemainingSeconds % 60
    val arrivalClockTime = remember(approxEtaMinutes) {
        val targetMillis = System.currentTimeMillis() + (approxEtaMinutes * 60_000L)
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(targetMillis))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, Color(0xFF00A896).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Order Tracking (#${order.orderId})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${order.vehicleType.title} • Fare: ${order.totalAmount} (${order.paymentMethod.shortBadge})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedContent(
                    targetState = order.orderStatus,
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                    label = "liveOrderStatusBadgeTransition"
                ) { targetStatus ->
                    val targetDelivered = targetStatus == OrderStatus.DELIVERED
                    Surface(
                        color = if (targetDelivered) Color(0xFFECFDF5) else Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = targetStatus.displayTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetDelivered) Color(0xFF059669) else Color(0xFFEA580C),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // LIVE APPROXIMATE TIME (ETA) & DISTANCE BANNER (Always visible in Customer Tracking)
            Surface(
                color = if (isDelivered) Color(0xFFECFDF5) else Color(0xFFF0FDFA),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, if (isDelivered) Color(0xFF10B981) else Color(0xFF00A896)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_live_eta_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDelivered) {
                                "✅ Delivered Successfully on Time!"
                            } else if (liveGpsEtaPair != null) {
                                "🛵 Rider ~${liveGpsEtaPair.second} mins away • ${String.format(Locale.US, "%.2f", remainingDistanceKm)} km"
                            } else {
                                "⏱️ Approx. Delivery Time: ~${approxEtaMinutes} min ${approxEtaSeconds}s"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D1B2A)
                        )
                        Text(
                            text = if (isDelivered) {
                                "Parcel handed over at ${order.dropAddress.fullAddress}"
                            } else {
                                String.format(
                                    Locale.US,
                                    "%.2f km remaining • Estimated Arrival by %s",
                                    remainingDistanceKm,
                                    arrivalClockTime
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F766E)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (isDelivered) Color(0xFF10B981) else Color(0xFFFF6B35),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isDelivered) "ARRIVED" else "~${approxEtaMinutes.coerceAtLeast(1)} MIN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (remoteFlags.waitTimeAndCancelRuleEnabled && (order.orderStatus == OrderStatus.RIDER_ASSIGNED || order.orderStatus == OrderStatus.ARRIVED_AT_PICKUP)) {
                if (waitFeeInfo != null) {
                    val mins = waitFeeInfo.elapsedSeconds / 60
                    val secs = waitFeeInfo.elapsedSeconds % 60
                    val isOvertime = waitFeeInfo.overtimeMinutes > 0
                    Surface(
                        color = if (isOvertime) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isOvertime) Color(0xFFDC2626) else Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_live_wait_timer_card")
                    ) {
                        Text(
                            text = if (isOvertime) {
                                String.format(
                                    Locale.US,
                                    "⏱️ Rider waiting at Pickup (%02d:%02d) • Overtime Wait Fee: +₹%d",
                                    mins,
                                    secs,
                                    waitFeeInfo.waitFeeInr
                                )
                            } else {
                                String.format(
                                    Locale.US,
                                    "⏱️ Rider at Pickup Gate (%02d:%02d) • %dm free wait window",
                                    mins,
                                    secs,
                                    remoteFlags.freeWaitMinutes
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOvertime) Color(0xFFB91C1C) else Color(0xFF92400E),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFF0F9FF),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_wait_policy_info_card")
                    ) {
                        Text(
                            text = "⏱️ Pickup Wait Policy: First ${remoteFlags.freeWaitMinutes} mins FREE at pickup gate, then ₹${remoteFlags.waitFeePerMinuteInr}/min overtime.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            if (!photoProofSummary.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📷 Parcel Photo Attached: $photoProofSummary",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (!isRiderAcceptedOrBeyond) {
                val isDeclinedReSearching = order.rejectedByRiderIds.isNotEmpty() ||
                    order.latestRiderQuickNote?.contains("Declined", ignoreCase = true) == true ||
                    order.latestRiderQuickNote?.contains("Rejected", ignoreCase = true) == true
                // Waiting or Re-Searching for a New Rider Partner
                Surface(
                    color = if (isDeclinedReSearching) Color(0xFFFFF7ED) else Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isDeclinedReSearching) 1.5.dp else 1.dp,
                        color = if (isDeclinedReSearching) Color(0xFFEA580C) else Color(0xFFF59E0B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            if (isDeclinedReSearching) {
                                "customer_rider_declined_researching_banner"
                            } else {
                                "customer_waiting_for_rider_banner"
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isDeclinedReSearching) {
                                "🔄 Previous Rider Declined — Searching for New Rider..."
                            } else {
                                "⏳ Waiting for Rider Partner to Accept Order..."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9A3412)
                        )
                        LinearProgressIndicator(
                            color = if (isDeclinedReSearching) Color(0xFFEA580C) else Color(0xFF00A896),
                            trackColor = Color(0xFFFDE68A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        if (isDeclinedReSearching && !order.latestRiderQuickNote.isNullOrBlank()) {
                            Text(
                                text = "🔔 Update: ${order.latestRiderQuickNote}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                        Text(
                            text = if (isDeclinedReSearching) {
                                "We sent you an alert and automatically dispatched Order #${order.orderId} to the next nearest online Rider Partner. Your OTPs remain intact."
                            } else {
                                "Your booking (#${order.orderId}) has been sent to nearby online Rider Partners. As soon as a Rider taps Accept in the Rider Partner App, live GPS map tracking and Rider contact details will appear here automatically."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                        Text(
                            text = "Pickup OTP: ${order.pickupOtp} • Delivery OTP: ${order.deliveryOtp}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F766E)
                        )
                    }
                }
            } else {
                // RIDER HAS ACCEPTED: Show Compact Inline Assigned Rider Profile Card (Name, Photo, Mobile Number)
                AssignedRiderProfileTrustCard(
                    order = order
                )

                // REAL-TIME FIRESTORE CHAT WITH ASSIGNED RIDER
                OrderRealTimeChatSection(
                    order = order,
                    viewerRole = "CUSTOMER",
                    viewerName = order.customerName
                )

                // Continuous Every-Moment Live Map & Tracking Corridor until Delivered!
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF00A896)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_live_rider_location_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDelivered) {
                                        "✅ Destination Reached — Order Delivered!"
                                    } else {
                                        "🔴 LIVE MAP • RIDER MOVING EVERY MOMENT"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Text(
                                text = "${(continuousLiveProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Interactive Live Moving Map Canvas (Rider moves smoothly every moment from Pickup to Drop)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C)),
                            border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(195.dp)
                                .testTag("customer_live_moving_map_canvas")
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // 1. City Map Grid Blocks & Roads
                                    val gridColor = Color(0xFF1E293B)
                                    for (gx in 1..6) {
                                        val xPos = w * (gx / 7f)
                                        drawLine(
                                            color = gridColor,
                                            start = Offset(xPos, 0f),
                                            end = Offset(xPos, h),
                                            strokeWidth = if (gx % 2 == 0) 6f else 2f
                                        )
                                    }
                                    for (gy in 1..4) {
                                        val yPos = h * (gy / 5f)
                                        drawLine(
                                            color = gridColor,
                                            start = Offset(0f, yPos),
                                            end = Offset(w, yPos),
                                            strokeWidth = if (gy % 2 == 0) 6f else 2f
                                        )
                                    }

                                    // 2. Curved Route Path from Pickup (bottom-left) to Drop (top-right)
                                    val pickupPt = Offset(w * 0.14f, h * 0.78f)
                                    val dropPt = Offset(w * 0.86f, h * 0.22f)

                                    // Sample 40 points along an S-curve route
                                    fun pointOnRoute(t: Float): Offset {
                                        val clamped = t.coerceIn(0f, 1f)
                                        val baseX = pickupPt.x + (dropPt.x - pickupPt.x) * clamped
                                        val baseY = pickupPt.y + (dropPt.y - pickupPt.y) * clamped
                                        val curveWave = sin(clamped * Math.PI.toFloat() * 2f) * (h * 0.11f)
                                        return Offset(baseX, baseY - curveWave)
                                    }

                                    val fullRoutePath = Path().apply {
                                        val p0 = pointOnRoute(0f)
                                        moveTo(p0.x, p0.y)
                                        for (step in 1..40) {
                                            val pt = pointOnRoute(step / 40f)
                                            lineTo(pt.x, pt.y)
                                        }
                                    }

                                    // Remaining route (dashed teal)
                                    drawPath(
                                        path = fullRoutePath,
                                        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                                        style = Stroke(
                                            width = 7f,
                                            cap = StrokeCap.Round,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                        )
                                    )

                                    // Glow halo under completed route (Swiggy/Zomato live map polish)
                                    val completedPath = Path().apply {
                                        val p0 = pointOnRoute(0f)
                                        moveTo(p0.x, p0.y)
                                        val maxStep = (continuousLiveProgress * 40).toInt().coerceAtLeast(1)
                                        for (step in 1..maxStep) {
                                            val pt = pointOnRoute(step / 40f)
                                            lineTo(pt.x, pt.y)
                                        }
                                    }
                                    drawPath(
                                        path = completedPath,
                                        color = Color(0xFF10B981).copy(alpha = 0.28f),
                                        style = Stroke(width = 19f, cap = StrokeCap.Round)
                                    )
                                    drawPath(
                                        path = completedPath,
                                        color = Color(0xFF10B981),
                                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                                    )

                                    // Pickup Marker (Green Ring + Pin)
                                    drawCircle(
                                        color = Color(0xFF10B981).copy(alpha = 0.28f),
                                        radius = 24f,
                                        center = pickupPt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 12f,
                                        center = pickupPt
                                    )
                                    drawCircle(
                                        color = Color(0xFF10B981),
                                        radius = 8f,
                                        center = pickupPt
                                    )

                                    // Drop Marker (Orange Ring + Pin)
                                    drawCircle(
                                        color = Color(0xFFFF6B35).copy(alpha = 0.28f),
                                        radius = 24f,
                                        center = dropPt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 12f,
                                        center = dropPt
                                    )
                                    drawCircle(
                                        color = Color(0xFFFF6B35),
                                        radius = 8f,
                                        center = dropPt
                                    )

                                    // Live Moving Rider Position & Radar Pulse
                                    val riderPt = pointOnRoute(continuousLiveProgress)
                                    val pulseRadius = 20f + (microStepOffset * 200f)
                                    drawCircle(
                                        color = Color(0xFFFF6B35).copy(alpha = 0.35f),
                                        radius = pulseRadius,
                                        center = riderPt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 15f,
                                        center = riderPt
                                    )
                                    drawCircle(
                                        color = Color(0xFFFF6B35),
                                        radius = 10f,
                                        center = riderPt
                                    )
                                }

                                // Top-Left Pickup & Drop Map Legend Overlay
                                Surface(
                                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "📍 P → 🏍️ Live → 🏁 D",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Top-Right Prominent "Arriving in X mins" Countdown Badge
                                Surface(
                                    color = if (isDelivered) Color(0xFF059669) else Color(0xFFFF6B35),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .testTag("customer_arriving_countdown_badge")
                                ) {
                                    Text(
                                        text = if (isDelivered) {
                                            "DELIVERED ✓"
                                        } else {
                                            "Arriving in ~${approxEtaMinutes.coerceAtLeast(1)} mins"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }

                                // Bottom-Right Live ETA & Distance Overlay Pill on Map
                                Surface(
                                    color = Color(0xFF00A896),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (isDelivered) {
                                            "Trip Complete ✓"
                                        } else {
                                            "ETA ~${approxEtaMinutes}m ${approxEtaSeconds}s • ${String.format(Locale.US, "%.2f", remainingDistanceKm)} km left"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Visual Smooth Animated Live Rider Route Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(23.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            LinearProgressIndicator(
                                progress = { continuousLiveProgress },
                                color = Color(0xFF00A896),
                                trackColor = Color(0xFF334155),
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .testTag("customer_animated_delivery_progress_bar")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pickup", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                LiveRiderPulseBadge(isDelivered = isDelivered)
                                Text("Delivered", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }

                        val isPrepaidOrder = order.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
                        val riderMobile = order.assignedRiderPhone?.takeIf { it.isNotBlank() }
                        val riderLabel = order.assignedRiderName?.takeIf { it.isNotBlank() } ?: "Assigning Delivery Partner"
                        val riderContactSuffix = if (!riderMobile.isNullOrBlank()) " ($riderMobile)" else ""

                        Text(
                            text = "Rider: $riderLabel$riderContactSuffix • ${order.vehicleType.title}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Live GPS: ${
                                liveRiderGpsCoordinates ?: String.format(
                                    Locale.US,
                                    "%.5f, %.5f (Moving Live • ~%d min %ds left)",
                                    simulatedLat,
                                    simulatedLng,
                                    approxEtaMinutes,
                                    approxEtaSeconds
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8)
                        )
                        if (isPrepaidOrder) {
                            Text(
                                text = "Prepaid OTPs • Pickup OTP: ${order.pickupOtp}  •  Delivery OTP: ${order.deliveryOtp}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFACC15),
                                modifier = Modifier.testTag("customer_prepaid_otp_text_${order.orderId}")
                            )
                        } else {
                            Text(
                                text = "Cash on Delivery (COD) • No OTP Required for Pickup or Delivery",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.testTag("customer_cod_no_otp_text_${order.orderId}")
                            )
                        }
                    }
                }

                // Stage Milestones Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val milestones = listOf(
                        "Accepted" to (stageProgress >= 0.35f),
                        "Picked Up" to (stageProgress >= 0.72f),
                        "In Transit" to (stageProgress >= 0.85f),
                        "Delivered" to isDelivered
                    )
                    milestones.forEach { (label, done) ->
                        val iconTint by animateColorAsState(
                            targetValue = if (done) Color(0xFF059669) else Color(0xFFCBD5E1),
                            animationSpec = tween(450),
                            label = "milestoneIconTint_$label"
                        )
                        val textTint by animateColorAsState(
                            targetValue = if (done) Color(0xFF065F46) else Color(0xFF64748B),
                            animationSpec = tween(450),
                            label = "milestoneTextTint_$label"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                                color = textTint
                            )
                        }
                    }
                }

                if (isDelivered) {
                    PostDeliveryRiderFeedbackSection(
                        orderId = order.orderId,
                        riderId = order.assignedRiderId ?: "",
                        riderName = order.assignedRiderName ?: "Delivery Partner",
                        customerName = order.customerName
                    )
                }
            }

            OutlinedButton(
                onClick = onBookNewOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_book_another_order_btn")
            ) {
                Text("+ Book Another Parcel Delivery")
            }
        }
    }
}

@Composable
private fun LiveRiderPulseBadge(isDelivered: Boolean) {
    val pulsePx = if (isDelivered) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "continuous_live_rider_track")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 18f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rider_pulse"
        )
        animated
    }
    Surface(
        color = Color(0xFFFF6B35),
        shape = CircleShape,
        modifier = Modifier
            .graphicsLayer { translationX = pulsePx }
            .size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = "Live Rider",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun QuickDeliveryCuteBunnyLogo(
    size: Dp = 34.dp,
    borderColor: Color = Color(0xFFFF6B35),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bunnyResId = remember(context) {
        context.resources.getIdentifier(
            "cute_scooter_bunny_logo_1794655841710",
            "drawable",
            context.packageName
        )
    }
    if (bunnyResId != 0) {
        Surface(
            color = Color(0xFFFCF4E1),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, borderColor),
            modifier = modifier.size(size)
        ) {
            Image(
                painter = painterResource(id = bunnyResId),
                contentDescription = "Quick Delivery Scooter Bunny Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        QuickDeliveryOfficialLogo(size = size, borderColor = borderColor, modifier = modifier)
    }
}


