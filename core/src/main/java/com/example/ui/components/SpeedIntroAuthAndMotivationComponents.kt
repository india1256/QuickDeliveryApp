package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    // Default to true so the Customer App and Rider App render immediately in the preview without blocking on intro/auth walls
    @Volatile
    var customerIntroCompleted: Boolean = true
    @Volatile
    var riderIntroCompleted: Boolean = true
    @Volatile
    var customerAuthenticatedInSession: Boolean = true
    @Volatile
    var riderAuthenticatedInSession: Boolean = true
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

    fun restoreSavedLoginFromPhoneStorage(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            val custGranted = prefs.getBoolean("customer_storage_permission_granted", false)
            val riderGranted = prefs.getBoolean("rider_storage_permission_granted", false)
            _customerStoragePermissionGranted.value = custGranted
            _riderStoragePermissionGranted.value = riderGranted

            // Restore from SharedPreferences or phone storage JSON backup file
            val customerBackup = readBackupJsonFromPhoneStorage(context, "customer")
            cachedCustomerName = prefs.getString("customer_name", null)
                ?: customerBackup?.optString("name")?.takeIf { it.isNotBlank() }
                ?: cachedCustomerName
            cachedCustomerPhone = prefs.getString("customer_phone", null)
                ?: customerBackup?.optString("phone")?.takeIf { it.isNotBlank() }
                ?: cachedCustomerPhone
            cachedCustomerEmail = prefs.getString("customer_email", null)
                ?: customerBackup?.optString("email")?.takeIf { it.isNotBlank() }
                ?: cachedCustomerEmail

            val riderBackup = readBackupJsonFromPhoneStorage(context, "rider")
            cachedRiderName = prefs.getString("rider_name", null)
                ?: riderBackup?.optString("name")?.takeIf { it.isNotBlank() }
                ?: cachedRiderName
            cachedRiderPhone = prefs.getString("rider_phone", null)
                ?: riderBackup?.optString("phone")?.takeIf { it.isNotBlank() }
                ?: cachedRiderPhone
            cachedRiderEmail = prefs.getString("rider_email", null)
                ?: riderBackup?.optString("email")?.takeIf { it.isNotBlank() }
                ?: cachedRiderEmail
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
            val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("${rolePrefix}_name", name)
                .putString("${rolePrefix}_phone", phone)
                .putString("${rolePrefix}_email", email)
                .putBoolean("${rolePrefix}_logged_in", true)
                .putBoolean("${rolePrefix}_storage_permission_granted", storageAllowed)
                .putLong("${rolePrefix}_saved_timestamp", System.currentTimeMillis())
                .apply()

            if (rolePrefix == "customer") {
                cachedCustomerName = name
                cachedCustomerPhone = phone
                cachedCustomerEmail = email
                _customerStoragePermissionGranted.value = storageAllowed
            } else {
                cachedRiderName = name
                cachedRiderPhone = phone
                cachedRiderEmail = email
                _riderStoragePermissionGranted.value = storageAllowed
            }

            if (storageAllowed) {
                writeBackupJsonToPhoneStorage(context, rolePrefix, name, phone, email)
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

            writeBackupJsonToPhoneStorage(context, "customer", cachedCustomerName, cachedCustomerPhone, cachedCustomerEmail)
            writeBackupJsonToPhoneStorage(context, "rider", cachedRiderName, cachedRiderPhone, cachedRiderEmail)
        }
    }

    private fun writeBackupJsonToPhoneStorage(
        context: Context,
        rolePrefix: String,
        name: String,
        phone: String,
        email: String
    ) {
        runCatching {
            val payload = JSONObject().apply {
                put("role", rolePrefix)
                put("name", name)
                put("phone", phone)
                put("email", email)
                put("savedAtEpochMs", System.currentTimeMillis())
                put("autoLoginEnabled", true)
            }.toString()

            // Save in internal app phone storage
            val internalFile = File(context.filesDir, "quick_delivery_${rolePrefix}_login_backup.json")
            internalFile.writeText(payload)

            // Also save in external app-specific phone storage directory if mounted
            context.getExternalFilesDir("login_backup")?.let { extDir ->
                if (!extDir.exists()) extDir.mkdirs()
                File(extDir, "quick_delivery_${rolePrefix}_login_backup.json").writeText(payload)
            }
        }
    }

    private fun readBackupJsonFromPhoneStorage(context: Context, rolePrefix: String): JSONObject? {
        return runCatching {
            val internalFile = File(context.filesDir, "quick_delivery_${rolePrefix}_login_backup.json")
            if (internalFile.exists()) {
                return@runCatching JSONObject(internalFile.readText())
            }
            val extFile = context.getExternalFilesDir("login_backup")?.let {
                File(it, "quick_delivery_${rolePrefix}_login_backup.json")
            }
            if (extFile != null && extFile.exists()) {
                return@runCatching JSONObject(extFile.readText())
            }
            null
        }.getOrNull()
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

    // Toggle between New User (Sign Up) and Old User (Sign In)
    var isNewUserSignUp by remember { mutableStateOf(true) }

    // New User Sign Up fields: Name, Mobile Number, Email ID
    var fullName by remember {
        mutableStateOf(prefs.getString("${prefsKeyPrefix}_name", "") ?: "")
    }
    var mobileNumber by remember {
        mutableStateOf(prefs.getString("${prefsKeyPrefix}_phone", "") ?: "")
    }
    var emailId by remember {
        mutableStateOf(prefs.getString("${prefsKeyPrefix}_email", "") ?: "")
    }

    // Old User Sign In field: Mobile Number OR Email ID
    var existingMobileOrEmail by remember {
        mutableStateOf(prefs.getString("${prefsKeyPrefix}_phone", "") ?: "")
    }
    var allowPhoneStoragePermission by remember {
        mutableStateOf(prefs.getBoolean("${prefsKeyPrefix}_storage_permission_granted", true))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = "$roleTitle Welcome",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D1B2A)
                        )
                        Text(
                            text = if (isNewUserSignUp) {
                                "New $roleTitle: Sign Up with Name, Mobile & Email"
                            } else {
                                "Existing $roleTitle: Sign In with Mobile or Email"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Mode Switcher: New User (Sign Up) vs Old User (Sign In)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = isNewUserSignUp,
                        onClick = {
                            isNewUserSignUp = true
                            errorMessage = null
                        },
                        label = { Text("New User (Sign Up)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${prefsKeyPrefix}_tab_new_signup")
                    )
                    FilterChip(
                        selected = !isNewUserSignUp,
                        onClick = {
                            isNewUserSignUp = false
                            errorMessage = null
                        },
                        label = { Text("Old User (Sign In)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${prefsKeyPrefix}_tab_old_signin")
                    )
                }

                if (isNewUserSignUp) {
                    // NEW USER SIGN UP: Name + Mobile Number + Email ID
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name (Naam)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signup_name_input")
                    )

                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { if (it.length <= 10) mobileNumber = it.filter(Char::isDigit) },
                        label = { Text("Mobile Number (10 digits)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signup_phone_input")
                    )

                    OutlinedTextField(
                        value = emailId,
                        onValueChange = { emailId = it },
                        label = { Text("Email ID (Mail ID)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signup_email_input")
                    )
                } else {
                    // OLD USER SIGN IN: Only Mobile Number OR Email ID
                    OutlinedTextField(
                        value = existingMobileOrEmail,
                        onValueChange = { existingMobileOrEmail = it },
                        label = { Text("Enter Mobile Number OR Email ID") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${prefsKeyPrefix}_signin_identifier_input")
                    )
                }

                // REMEMBER LOGIN OPTION FOR PERSISTENT SESSION
                Surface(
                    color = if (allowPhoneStoragePermission) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (allowPhoneStoragePermission) Color(0xFF10B981) else Color(0xFFF59E0B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${prefsKeyPrefix}_storage_permission_box")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remember My Login",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D1B2A)
                            )
                            Text(
                                text = if (allowPhoneStoragePermission) {
                                    "Your account will stay signed in on this device."
                                } else {
                                    "Turn on to stay signed in on this device."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                        }
                        Switch(
                            checked = allowPhoneStoragePermission,
                            onCheckedChange = { allowPhoneStoragePermission = it },
                            modifier = Modifier.testTag("${prefsKeyPrefix}_storage_permission_switch")
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C)
                    )
                }

                Button(
                    onClick = {
                        if (isNewUserSignUp) {
                            val finalName = fullName.trim().ifBlank {
                                if (prefsKeyPrefix == "rider") "Vikram Singh" else "Aarav Sharma"
                            }
                            val finalPhone = mobileNumber.trim().ifBlank { "9876543210" }
                            val finalEmail = emailId.trim().ifBlank { "${prefsKeyPrefix}@quickdelivery.in" }
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
                            val identifier = existingMobileOrEmail.trim()
                            if (identifier.isBlank()) {
                                errorMessage = "Please enter your Mobile Number or Email ID to Sign In."
                                return@Button
                            }
                            val savedName = prefs.getString("${prefsKeyPrefix}_name", null)
                                ?: if (prefsKeyPrefix == "rider") "Vikram Singh" else "Aarav Sharma"
                            val resolvedPhone = if (identifier.all { it.isDigit() } && identifier.length >= 10) {
                                identifier.take(10)
                            } else {
                                prefs.getString("${prefsKeyPrefix}_phone", "9876543210") ?: "9876543210"
                            }
                            val resolvedEmail = if (identifier.contains("@")) {
                                identifier
                            } else {
                                prefs.getString("${prefsKeyPrefix}_email", "${prefsKeyPrefix}@quickdelivery.in")
                                    ?: "${prefsKeyPrefix}@quickdelivery.in"
                            }
                            MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
                                context = context,
                                rolePrefix = prefsKeyPrefix,
                                name = savedName,
                                phone = resolvedPhone,
                                email = resolvedEmail,
                                storageAllowed = allowPhoneStoragePermission
                            )
                            onAuthenticated(savedName, resolvedPhone, resolvedEmail)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("${prefsKeyPrefix}_auth_submit_btn")
                ) {
                    Text(
                        text = if (isNewUserSignUp) {
                            "Create Account & Continue →"
                        } else {
                            "Sign In & Continue →"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
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
    onSimulateOrAdvanceStep: () -> Unit,
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

    // Calculate Live Remaining Distance (km) & Approximate ETA (mins + secs)
    val totalDistanceKm = order.distanceKm.coerceAtLeast(1.8)
    val remainingDistanceKm = if (isDelivered) {
        0.0
    } else {
        (totalDistanceKm * (1.0f - continuousLiveProgress)).coerceAtLeast(0.15)
    }
    // Assume average city speed ~26 km/h (~2.3 mins per km)
    val totalRemainingSeconds = if (isDelivered) {
        0
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
                Column {
                    Text(
                        text = "Live Order Tracking (#${order.orderId})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A)
                    )
                    Text(
                        text = "${order.vehicleType.title} • Fare: ${order.totalAmount} (${order.paymentMethod.shortBadge})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                }
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
                    Surface(
                        color = if (isDelivered) Color(0xFF10B981) else Color(0xFFFF6B35),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isDelivered) "ARRIVED" else "~${approxEtaMinutes.coerceAtLeast(1)} MIN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                // Waiting for Rider to Accept Order
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏳ Auto-Assigning Nearest Online Rider Partner...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Live GPS map tracking starts automatically and shows every moment of your Rider's movement with live approximate arrival time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                        Button(
                            onClick = onSimulateOrAdvanceStep,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_advance_live_stage_btn")
                        ) {
                            Text("Accept Order as Rider & Start Live Tracking →", fontWeight = FontWeight.Bold)
                        }
                        DirectDialPadCallButton(
                            orderId = order.orderId,
                            callerRole = "CUSTOMER",
                            callerName = order.customerName,
                            receiverRole = "Rider",
                            receiverName = order.assignedRiderName ?: "Vikram Rathore",
                            phoneNumber = order.assignedRiderPhone ?: "+91 98450 67890",
                            buttonTestTag = "customer_call_rider_dial_btn_${order.orderId}"
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

                                    // Completed route up to Rider's live position (solid glowing emerald)
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
                                        color = Color(0xFF10B981),
                                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                                    )

                                    // Pickup Marker (Green Pin)
                                    drawCircle(
                                        color = Color(0xFF10B981).copy(alpha = 0.28f),
                                        radius = 22f,
                                        center = pickupPt
                                    )
                                    drawCircle(
                                        color = Color(0xFF10B981),
                                        radius = 10f,
                                        center = pickupPt
                                    )

                                    // Drop Marker (Orange Pin)
                                    drawCircle(
                                        color = Color(0xFFFF6B35).copy(alpha = 0.28f),
                                        radius = 22f,
                                        center = dropPt
                                    )
                                    drawCircle(
                                        color = Color(0xFFFF6B35),
                                        radius = 10f,
                                        center = dropPt
                                    )

                                    // Live Moving Rider Position & Radar Pulse
                                    val riderPt = pointOnRoute(continuousLiveProgress)
                                    val pulseRadius = 18f + (microStepOffset * 180f)
                                    drawCircle(
                                        color = Color(0xFFFF6B35).copy(alpha = 0.35f),
                                        radius = pulseRadius,
                                        center = riderPt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 14f,
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
                                        text = "📍 Pickup → 🏍️ Live Rider → 🏁 Drop",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Bottom-Right Live ETA & Speed Overlay Pill on Map
                                Surface(
                                    color = Color(0xFF00A896),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (isDelivered) {
                                            "Delivered ✓"
                                        } else {
                                            "ETA ~${approxEtaMinutes}m ${approxEtaSeconds}s • ${String.format(Locale.US, "%.2f", remainingDistanceKm)} km"
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
                        val riderMobile = order.assignedRiderPhone?.takeIf { it.isNotBlank() } ?: "+91 98450 67890"

                        Text(
                            text = "Rider: ${order.assignedRiderName ?: "Vikram Singh"} ($riderMobile) • ${order.vehicleType.title}",
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

                if (!isDelivered) {
                    val isPrepaidOrder = order.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
                    Button(
                        onClick = onSimulateOrAdvanceStep,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_advance_live_stage_btn")
                    ) {
                        val nextActionLabel = when (order.orderStatus) {
                            OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP ->
                                if (isPrepaidOrder) {
                                    "Next: Rider Picks Up Parcel (Prepaid OTP ${order.pickupOtp}) →"
                                } else {
                                    "Next: Rider Picks Up Parcel (COD — No OTP Required) →"
                                }
                            else ->
                                if (isPrepaidOrder) {
                                    "Next: Complete Delivery at Drop (Prepaid OTP ${order.deliveryOtp}) →"
                                } else {
                                    "Next: Complete COD Delivery at Drop (No OTP) →"
                                }
                        }
                        Text(nextActionLabel, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // POST-DELIVERY STAR RATING & COMMENT SECTION FOR THE RIDER (Visible ONLY in Admin Web)
                    PostDeliveryRiderFeedbackSection(
                        orderId = order.orderId,
                        riderId = order.assignedRiderId ?: "RIDER-101",
                        riderName = order.assignedRiderName ?: "Vikram Singh",
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

