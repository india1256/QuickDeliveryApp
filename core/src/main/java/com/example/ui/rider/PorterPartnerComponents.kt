package com.example.ui.rider

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.QuickDeliveryOfficialLogo
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.RiderProfile
import java.util.Locale

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Quick Delivery Official Brand Theme Palette
val QuickDeliveryTeal = Color(0xFF078B8D)
val QuickDeliveryOrange = Color(0xFFFF7A1A)
const val QuickDeliveryTagline = "FAST. SIMPLE. SECURE."

val PorterNavy = Color(0xFF0F1E36)
val PorterBlue = QuickDeliveryTeal
val PorterTeal = QuickDeliveryTeal
val PorterOrange = QuickDeliveryOrange
val PorterGreen = Color(0xFF10B981)
val PorterRed = Color(0xFFEF4444)
val PorterAmber = Color(0xFFF59E0B)
val PorterCardBg = Color(0xFFF8FAFC)

/**
 * Global Theme Manager for the Rider App (Light Mode vs Night/Dark Mode for night-time visibility).
 * Persists preference across sessions in SharedPreferences.
 */
object RiderThemeManager {
    private const val PREFS_NAME = "quick_delivery_rider_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private var initialized = false

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (!initialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, false)
            initialized = true
        }
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        _isDarkMode.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }

    fun toggleTheme(context: Context) {
        setDarkMode(context, !_isDarkMode.value)
    }
}

/**
 * Manages Rider Profile Photo capture & upload persistence across the Rider App.
 */
object RiderProfilePhotoManager {
    private const val PREFS_NAME = "quick_delivery_rider_profile_photo_prefs"
    private const val KEY_PHOTO_BASE64 = "rider_profile_photo_base64"
    private const val KEY_PHOTO_NOTE = "rider_profile_photo_note"
    private var loaded = false

    private val _profilePhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val profilePhotoBitmap: StateFlow<Bitmap?> = _profilePhotoBitmap.asStateFlow()

    private val _lastUpdatedNote = MutableStateFlow<String?>(null)
    val lastUpdatedNote: StateFlow<String?> = _lastUpdatedNote.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (!loaded) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val encoded = prefs.getString(KEY_PHOTO_BASE64, null)
            val note = prefs.getString(KEY_PHOTO_NOTE, null)
            if (!encoded.isNullOrBlank()) {
                runCatching {
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()?.let { bmp ->
                    _profilePhotoBitmap.value = bmp
                }
            }
            _lastUpdatedNote.value = note
            loaded = true
        }
    }

    fun saveProfilePhoto(context: Context, bitmap: Bitmap, sourceLabel: String = "Camera") {
        _profilePhotoBitmap.value = bitmap
        val timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val note = "Uploaded via $sourceLabel ($timestamp)"
        _lastUpdatedNote.value = note
        runCatching {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            val encoded = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PHOTO_BASE64, encoded)
                .putString(KEY_PHOTO_NOTE, note)
                .apply()
        }
    }
}

/**
 * Porter Partner Left Navigation Drawer Content:
 * Opened via the top-left hamburger menu ("ten line" / "teen line" icon ☰).
 * Shows Rider Profile header, KYC badge, Duty switch, Live wallet, and all Porter Partner options.
 */
@Composable
fun PorterRiderDrawerContent(
    rider: RiderProfile?,
    riderDisplayName: String,
    walletBalance: Double,
    totalTipsInr: Int,
    isOnline: Boolean,
    isSuspended: Boolean,
    orders: List<CustomerOrder> = emptyList(),
    onToggleDuty: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenTripHistory: () -> Unit,
    onOpenIncentives: () -> Unit,
    onOpenRecharge: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenGuidelines: () -> Unit,
    onOpenSafety: () -> Unit = onOpenSupport,
    onOpenKycDocuments: () -> Unit = onOpenProfile,
    onOpenSuggestionBox: () -> Unit = {},
    onInstantCashout: (Double) -> Unit = {},
    onSimulateBalance: (Double) -> Unit,
    onLogout: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveBalance = walletBalance + totalTipsInr

    // Live daily earnings metrics from actual orders & partner milestones
    val deliveredOrders = orders.filter { it.orderStatus == OrderStatus.DELIVERED }
    val deliveredTripEarnings = deliveredOrders.sumOf {
        it.riderEarning.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 60.0
    }
    val baseTripEarnings = if (deliveredTripEarnings > 0) 340.0 + deliveredTripEarnings else 340.0
    val customerTips = if (totalTipsInr > 0) totalTipsInr.toDouble() else 40.0
    val completedTripsCount = deliveredOrders.size + 4
    val dailyIncentives = if (completedTripsCount >= 6) 150.0 else if (completedTripsCount >= 4) 80.0 else 40.0
    val platformDeduction = (baseTripEarnings * 0.15).toInt().toDouble()
    val netDailyTakeHome = baseTripEarnings + customerTips + dailyIncentives - platformDeduction

    // Live pending payout scheduled for bi-weekly disbursement
    val pendingPayoutAmount = if (effectiveBalance > 0) {
        (effectiveBalance * 1.25 + 640.0).coerceAtLeast(640.0)
    } else {
        1240.0
    }

    val context = LocalContext.current
    remember(context) {
        RiderThemeManager.ensureLoaded(context)
        RiderProfilePhotoManager.ensureLoaded(context)
    }
    val isDarkMode by RiderThemeManager.isDarkMode.collectAsState()
    val profilePhotoBitmap by RiderProfilePhotoManager.profilePhotoBitmap.collectAsState()
    val drawerBgColor = if (isDarkMode) Color(0xFF0F172A) else Color.White
    val dividerColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(drawerBgColor)
            .testTag("porter_rider_drawer_content")
    ) {
        // Drawer Header with Porter Branding & Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PorterNavy, Color(0xFF1A2B4C))
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        QuickDeliveryOfficialLogo(size = 36.dp, borderColor = QuickDeliveryOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Delivery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = QuickDeliveryTagline,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = QuickDeliveryOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Drawer",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Profile Info Row (clickable to view Profile Details)
                Surface(
                    onClick = onOpenProfile,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_profile_summary_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Surface(
                                color = PorterBlue,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                val currentPhoto = profilePhotoBitmap
                                if (currentPhoto != null) {
                                    Image(
                                        bitmap = currentPhoto.asImageBitmap(),
                                        contentDescription = "Rider Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Rider Profile",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            // Online/Offline Green/Gray Dot
                            Surface(
                                color = if (isSuspended) PorterRed else if (isOnline) PorterGreen else Color(0xFF94A3B8),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, PorterNavy),
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                            ) {}
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = riderDisplayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "ID: ${rider?.riderId ?: "RIDER-101"} • ${rider?.vehicleType?.title ?: "Bike"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = PorterAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "4.92 ★ • 100% KYC Verified",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PorterGreen
                                )
                            }
                        }
                    }
                }

                // Duty Status Readout inside Drawer Header (Single toggle is on top header)
                Surface(
                    color = if (isSuspended) PorterRed.copy(alpha = 0.25f) else if (isOnline) Color(0xFF10B981).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSuspended) PorterRed else if (isOnline) Color(0xFF10B981) else Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_duty_status_strip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isSuspended) PorterRed else if (isOnline) Color(0xFF10B981) else Color(0xFF94A3B8),
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSuspended) "DUTY: SUSPENDED" else if (isOnline) "DUTY: ONLINE" else "DUTY: OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isOnline) "ACTIVE" else "STANDBY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Scrollable List of Quick Delivery Partner Drawer Options Grouped by Section
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
        ) {
            // 1. EARNINGS & PAYMENTS SECTION
            item {
                RiderDrawerSectionHeader(title = "EARNINGS & PAYMENTS")
            }

            item(key = "drawer_earnings_wallet_section") {
                PorterDrawerEarningsAndWalletSection(
                    currentBalance = effectiveBalance,
                    pendingPayout = pendingPayoutAmount,
                    baseTripEarnings = baseTripEarnings,
                    customerTips = customerTips,
                    dailyIncentives = dailyIncentives,
                    platformDeduction = platformDeduction,
                    netTakeHome = netDailyTakeHome,
                    completedTripsCount = completedTripsCount,
                    isSuspended = isSuspended,
                    onOpenWallet = onOpenWallet,
                    onOpenRecharge = onOpenRecharge,
                    onInstantCashout = { onInstantCashout(pendingPayoutAmount) }
                )
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Earnings & Wallet",
                    subtitle = if (effectiveBalance < 0) "⚠️ Negative Due: ₹${effectiveBalance.toInt()}" else "Full Earnings, Tips & Payment Records",
                    badge = if (effectiveBalance <= -200.0) "SUSPENDED" else if (effectiveBalance <= -150.0) "DUE" else null,
                    badgeColor = if (effectiveBalance <= -200.0) PorterRed else PorterAmber,
                    onClick = onOpenWallet,
                    tag = "drawer_item_wallet"
                )
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.EmojiEvents,
                    title = "Daily Incentives & Rewards",
                    subtitle = "Complete 5 trips → ₹150 Bonus",
                    onClick = onOpenIncentives,
                    tag = "drawer_item_incentives"
                )
            }

            // 2. HISTORY SECTION
            item {
                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                RiderDrawerSectionHeader(title = "HISTORY")
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.History,
                    title = "Trip & Delivery History",
                    subtitle = "Past Deliveries & Customer Receipts",
                    onClick = onOpenTripHistory,
                    tag = "drawer_item_trips"
                )
            }

            // 3. PROFILE & KYC SECTION
            item {
                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                RiderDrawerSectionHeader(title = "PROFILE & KYC")
            }

            // Verified KYC Status Card inside Left Drawer Menu (☰)
            item(key = "rider_verified_kyc_card") {
                Card(
                    onClick = onOpenKycDocuments,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("rider_verified_kyc_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Color(0xFFD1FAE5),
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Verified KYC",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "KYC Status: Verified Partner",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "Aadhaar: ${rider?.aadhaarNumber ?: "XXXX-XXXX-1101"} • PAN: ${rider?.panNumber ?: "ABCDE1101F"} • RC: ${rider?.vehicleNumber ?: "KA-01-QD-1101"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF047857),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.Person,
                    title = "Profile & Vehicle Details",
                    subtitle = "Partner ID, Vehicle (${rider?.vehicleType?.title ?: "Bike"}) & Documents",
                    onClick = onOpenProfile,
                    tag = "drawer_item_profile"
                )
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Scan KYC Documents",
                    subtitle = "Camera Verification (Aadhaar, DL, PAN, RC)",
                    badge = "CAMERA",
                    badgeColor = PorterTeal,
                    onClick = onOpenKycDocuments,
                    tag = "drawer_item_kyc_scan"
                )
            }

            // 4. NOTIFICATIONS & SUPPORT SECTION
            item {
                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                RiderDrawerSectionHeader(title = "SAFETY, GUIDELINES & SUPPORT")
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.Security,
                    title = "Safety & Emergency SOS",
                    subtitle = "Emergency Hotline 112 & Quick Safety Escalation",
                    onClick = onOpenSafety,
                    tag = "drawer_item_safety"
                )
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.Description,
                    title = "Quick Delivery Guidelines & SOP",
                    subtitle = "Operating Checklist & Commission Rules",
                    onClick = onOpenGuidelines,
                    tag = "drawer_item_guidelines"
                )
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.Help,
                    title = "Help Desk & Partner Support",
                    subtitle = "Call 1800-QUICK-CARE / 1800-258-258 / Raise Ticket",
                    onClick = onOpenSupport,
                    tag = "drawer_item_support"
                )
            }

            item {
                PorterDrawerMenuItem(
                    icon = Icons.Default.Description,
                    title = "Suggestion & Feedback Box",
                    subtitle = "Send suggestion or report issue directly to Admin",
                    badge = "FEEDBACK",
                    badgeColor = QuickDeliveryOrange,
                    onClick = onOpenSuggestionBox,
                    tag = "drawer_item_suggestion_box"
                )
            }

            // 5. SETTINGS & LOGOUT SECTION
            item {
                HorizontalDivider(
                    color = dividerColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                RiderDrawerSectionHeader(title = "SETTINGS & LOGOUT")
            }

            // Global Rider Light / Dark Theme Toggle for Night-Time Visibility
            item(key = "drawer_rider_theme_toggle") {
                Surface(
                    onClick = { RiderThemeManager.toggleTheme(context) },
                    color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkMode) QuickDeliveryTeal.copy(alpha = 0.6f) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("rider_theme_toggle_btn")
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
                                text = if (isDarkMode) "App Theme: Night Dark Mode" else "App Theme: Day Light Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else PorterNavy,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "High-contrast night visibility",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Segmented Light / Dark Pill Selector
                        Surface(
                            color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { RiderThemeManager.setDarkMode(context, false) },
                                    color = if (!isDarkMode) QuickDeliveryTeal else Color.Transparent,
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.testTag("rider_theme_light_btn")
                                ) {
                                    Text(
                                        text = "☀️ Light",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (!isDarkMode) Color.White else Color(0xFF94A3B8),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    onClick = { RiderThemeManager.setDarkMode(context, true) },
                                    color = if (isDarkMode) QuickDeliveryOrange else Color.Transparent,
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.testTag("rider_theme_dark_btn")
                                ) {
                                    Text(
                                        text = "🌙 Dark",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkMode) Color.White else Color(0xFF475569),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = {
                        onCloseDrawer()
                        onLogout()
                    },
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("drawer_rider_logout_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Logout",
                                    tint = PorterRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PorterRed
                            )
                            Text(
                                text = "Sign out of your partner account",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderDrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = QuickDeliveryTeal,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
    )
}

@Composable
private fun PorterDrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = PorterOrange,
    onClick: () -> Unit,
    tag: String
) {
    val isDarkMode by RiderThemeManager.isDarkMode.collectAsState()
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isDarkMode) PorterBlue.copy(alpha = 0.22f) else PorterBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDarkMode) Color(0xFF2DD4BF) else PorterBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFFF8FAFC) else PorterNavy,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badge != null) {
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, badgeColor)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Banner shown when Partner ID is SUSPENDED because negative balance reached <= -200 INR.
 */
@Composable
fun PorterIdSuspendedWarningCard(
    walletBalance: Double,
    onClearDues: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, PorterRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("porter_id_suspended_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PorterRed,
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "ID Suspended",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PARTNER ID SUSPENDED (≤ -₹200 Due)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = "Current Balance: ₹${walletBalance.toInt()} • Exceeded -₹200 limit",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB91C1C)
                    )
                }
            }

            Text(
                text = "As per Quick Delivery Partner rules, when negative balance reaches -₹200, your ID is suspended. You cannot accept or receive deliveries until dues are cleared.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7F1D1D)
            )

            Button(
                onClick = onClearDues,
                colors = ButtonDefaults.buttonColors(containerColor = PorterRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("porter_suspended_clear_dues_btn")
            ) {
                Text(
                    text = "Clear Dues & Reactivate ID Now →",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Banner shown when Auto-Offline was triggered because balance is <= -150 INR after completing an order.
 */
@Composable
fun PorterAutoOfflineWarningCard(
    walletBalance: Double,
    onRecharge: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, PorterAmber),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("porter_auto_offline_warning_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Auto Offline",
                        tint = PorterAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUTO-OFFLINE ACTIVATED (≤ -₹150)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF92400E)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF78350F))
                }
            }

            Text(
                text = "Order delivery was completed, but your wallet balance is -₹${Math.abs(walletBalance.toInt())}. As per Quick Delivery rules, duty was automatically set to OFFLINE after delivery. Please recharge above -₹150 to go back Online.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF78350F)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRecharge,
                    colors = ButtonDefaults.buttonColors(containerColor = PorterAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("porter_auto_offline_recharge_btn")
                ) {
                    Text(
                        text = "Recharge Wallet to Go Online",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Porter Wallet Recharge & Dues Payment Dialog.
 * Allows rider to clear negative balance or top up wallet with ₹150, ₹200, ₹500, or Custom Amount.
 */
@Composable
fun PorterWalletRechargeDialog(
    currentBalance: Double,
    onRechargeSuccess: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAmount by remember { mutableIntStateOf(if (currentBalance < 0) Math.abs(currentBalance.toInt()).coerceAtLeast(150) else 200) }
    var customAmountText by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("porter_recharge_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PorterBlue.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = PorterBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (currentBalance < 0) "Clear Negative Balance" else "Recharge Quick Delivery Wallet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PorterNavy
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Current Balance Banner
                Surface(
                    color = if (currentBalance < 0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (currentBalance < 0) PorterRed else PorterGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Partner Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.0f", currentBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentBalance < 0) PorterRed else PorterGreen
                            )
                        }

                        if (currentBalance <= -200.0) {
                            Surface(color = PorterRed, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "ID SUSPENDED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (currentBalance <= -150.0) {
                            Surface(color = PorterAmber, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "OFFLINE DUE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Select Recharge Amount:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PorterNavy
                )

                // Quick Amount Chips
                val quickAmounts = listOf(150, 200, 300, 500)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickAmounts.forEach { amt ->
                        val isSelected = selectedAmount == amt && customAmountText.isBlank()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedAmount = amt
                                customAmountText = ""
                            },
                            label = { Text("₹$amt", fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recharge_chip_$amt")
                        )
                    }
                }

                // Custom Amount Input
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = {
                        customAmountText = it.filter { char -> char.isDigit() }
                        it.toIntOrNull()?.let { num -> selectedAmount = num }
                    },
                    label = { Text("Or Enter Custom Amount (₹)") },
                    placeholder = { Text("e.g. 500") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_recharge_input")
                )

                Button(
                    onClick = {
                        val amountToCredit = selectedAmount.toDouble().coerceAtLeast(50.0)
                        onRechargeSuccess(amountToCredit)
                        isSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PorterBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_wallet_recharge_btn")
                ) {
                    Text(
                        text = "Pay ₹$selectedAmount via UPI / Card →",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Porter Partner Daily Incentives & Rewards Dialog.
 */
@Composable
fun PorterIncentivesDialog(
    completedToday: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("porter_incentives_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = PorterAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Incentives & Rewards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PorterNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Progress Bar
                val target1 = 5
                val target2 = 8
                val progress = (completedToday.toFloat() / target2).coerceIn(0f, 1f)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Today's Deliveries: $completedToday trips",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PorterNavy
                        )
                        Text(
                            text = "Target: $target2 trips",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        color = PorterOrange,
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }

                // Milestone Cards
                IncentiveMilestoneRow(
                    targetTrips = 5,
                    bonusAmountInr = 150,
                    completed = completedToday >= 5
                )
                IncentiveMilestoneRow(
                    targetTrips = 8,
                    bonusAmountInr = 250,
                    completed = completedToday >= 8
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PorterNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It")
                }
            }
        }
    }
}

@Composable
private fun IncentiveMilestoneRow(
    targetTrips: Int,
    bonusAmountInr: Int,
    completed: Boolean
) {
    Surface(
        color = if (completed) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (completed) PorterGreen else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (completed) PorterGreen else PorterAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Complete $targetTrips Deliveries",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PorterNavy
                    )
                    Text(
                        text = if (completed) "✅ Bonus Unlocked!" else "Earn ₹$bonusAmountInr Extra Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (completed) PorterGreen else Color(0xFF64748B)
                    )
                }
            }

            Text(
                text = "+₹$bonusAmountInr",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (completed) PorterGreen else PorterOrange
            )
        }
    }
}

/**
 * Porter Partner Trip History Dialog:
 * Shows completed deliveries, trip earnings, and customer feedback.
 */
@Composable
fun PorterTripHistoryDialog(
    orders: List<CustomerOrder>,
    onDismiss: () -> Unit
) {
    val completedOrders = remember(orders) {
        orders.filter { it.orderStatus == OrderStatus.DELIVERED }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("porter_trip_history_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = PorterBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trip History (${completedOrders.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PorterNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                if (completedOrders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No completed trips in current session",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(completedOrders) { order ->
                            Surface(
                                color = PorterCardBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Order #${order.orderId}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PorterNavy
                                        )
                                        Text(
                                            text = "Earned: ${order.riderEarning}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PorterGreen
                                        )
                                    }
                                    Text(
                                        text = "${order.pickupAddress.fullAddress.take(28)}... → ${order.dropAddress.fullAddress.take(28)}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "${order.vehicleType.title} • ${order.paymentMethod.shortBadge} • Delivered ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PorterBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PorterNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Porter Partner Standard Operating Guidelines & Rules Dialog.
 */
@Composable
fun PorterGuidelinesDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("porter_guidelines_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = PorterNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Delivery Partner Guidelines",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PorterNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleBullet(
                        title = "1. Negative Balance Limit (-₹150)",
                        desc = "If wallet balance drops to -₹150, you can complete your current ongoing order. After order completion, duty will automatically switch to OFFLINE until recharged."
                    )
                    RuleBullet(
                        title = "2. Partner ID Suspension (-₹200)",
                        desc = "If negative balance reaches -₹200 or below, your Partner ID is SUSPENDED. All orders are blocked until dues are cleared."
                    )
                    RuleBullet(
                        title = "3. 80% Earnings & 100% Tips",
                        desc = "80% of parcel fare is credited to you. 100% of customer tips go directly to the rider."
                    )
                    RuleBullet(
                        title = "4. Safety & Overspeed Guard",
                        desc = "Riding above 60 km/h triggers an emergency phone alarm. Keep GPS enabled at all times."
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PorterNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Understand")
                }
            }
        }
    }
}

@Composable
private fun RuleBullet(title: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PorterNavy
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF475569)
        )
    }
}

/**
 * Dedicated 'Earnings & Wallet' Section designed for the Porter Partner Side Drawer.
 * Displays:
 * 1. Current Balance (with negative dues rule states: -150 Auto-Offline and -200 Suspension warnings)
 * 2. Pending Payouts (with linked verified Bank account & instant cashout CTA)
 * 3. Daily Earnings Breakdown (Trip fares, 100% customer tips, incentives, platform fee & net take-home)
 */
@Composable
fun PorterDrawerEarningsAndWalletSection(
    currentBalance: Double,
    pendingPayout: Double,
    baseTripEarnings: Double,
    customerTips: Double,
    dailyIncentives: Double,
    platformDeduction: Double,
    netTakeHome: Double,
    completedTripsCount: Int,
    isSuspended: Boolean,
    onOpenWallet: () -> Unit,
    onOpenRecharge: () -> Unit,
    onInstantCashout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isBreakdownExpanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("drawer_earnings_and_wallet_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = PorterBlue.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PorterBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Earnings & Wallet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PorterNavy,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Live Quick Delivery Passbook",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    onClick = onOpenWallet,
                    color = PorterBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, PorterBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("drawer_wallet_open_dashboard_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Passbook",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PorterBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PorterBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 1. Current Balance Box
            Surface(
                color = if (currentBalance <= -200.0) Color(0xFFFEF2F2)
                else if (currentBalance <= -150.0) Color(0xFFFFFBEB)
                else if (currentBalance < 0) Color(0xFFFFF7ED)
                else Color(0xFFF0FDF4),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (currentBalance <= -200.0) PorterRed
                    else if (currentBalance <= -150.0) PorterAmber
                    else if (currentBalance < 0) PorterOrange
                    else PorterGreen.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        val badgeText = if (currentBalance <= -200.0) "🚫 ID SUSPENDED"
                        else if (currentBalance <= -150.0) "⚠️ AUTO-OFFLINE"
                        else if (currentBalance < 0) "⚠️ DUES PENDING"
                        else "● Active"

                        val badgeColor = if (currentBalance <= -200.0) PorterRed
                        else if (currentBalance <= -150.0) PorterAmber
                        else if (currentBalance < 0) PorterOrange
                        else PorterGreen

                        Surface(
                            color = badgeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "₹%.2f", currentBalance),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (currentBalance <= -200.0) PorterRed
                                else if (currentBalance <= -150.0) PorterAmber
                                else if (currentBalance < 0) PorterOrange
                                else PorterNavy
                            )
                            if (currentBalance < 0) {
                                Text(
                                    text = if (currentBalance <= -200.0) "Exceeded -₹200 limit" else "Recharge to avoid auto-offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentBalance <= -200.0) PorterRed else PorterAmber
                                )
                            }
                        }

                        Button(
                            onClick = onOpenRecharge,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentBalance < 0) PorterOrange else PorterGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("drawer_balance_action_btn")
                        ) {
                            Text(
                                text = if (currentBalance < 0) "Clear Dues" else "+ Top Up",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. Pending Payouts Box
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = PorterTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PENDING PAYOUT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }

                        Surface(
                            color = PorterAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⏳ Scheduled",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "₹%.2f", pendingPayout),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PorterNavy
                            )
                            Text(
                                text = "HDFC Bank •••• 4092 • Verified ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }

                        OutlinedButton(
                            onClick = onInstantCashout,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PorterTeal),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PorterTeal),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("drawer_instant_cashout_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = PorterTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Cashout",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "🗓️ Auto-transfer scheduled tomorrow at 06:00 AM (Bi-weekly cycle)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 3. Breakdown of Daily Earnings Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBreakdownExpanded = !isBreakdownExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = PorterBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daily Earnings Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = PorterBlue.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PorterBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isBreakdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isBreakdownExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            DailyBreakdownRow(
                                label = "Trip Fares ($completedTripsCount trips)",
                                value = String.format(Locale.US, "+₹%.2f", baseTripEarnings),
                                valueColor = PorterNavy
                            )
                            DailyBreakdownRow(
                                label = "Customer Tips (100% Rider)",
                                value = String.format(Locale.US, "+₹%.2f", customerTips),
                                valueColor = PorterGreen
                            )
                            DailyBreakdownRow(
                                label = "Daily Incentives & Bonus",
                                value = String.format(Locale.US, "+₹%.2f", dailyIncentives),
                                valueColor = PorterOrange
                            )
                            DailyBreakdownRow(
                                label = "Platform Fee / Commission (15%)",
                                value = String.format(Locale.US, "-₹%.2f", platformDeduction),
                                valueColor = Color(0xFFEF4444)
                            )

                            HorizontalDivider(
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Net Daily Take-Home",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PorterNavy
                                )
                                Text(
                                    text = String.format(Locale.US, "₹%.2f", netTakeHome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = PorterGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBreakdownRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF475569)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

/**
 * Dialog for Instant Payout Cashout to Rider's linked Bank Account / UPI.
 */
@Composable
fun PorterInstantCashoutDialog(
    amount: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("porter_instant_cashout_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PorterTeal.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = PorterTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Instant Payout Cashout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PorterNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PorterGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Transfer Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = String.format(Locale.US, "₹%.2f", amount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = PorterGreen
                        )
                        Text(
                            text = "Zero convenience fee • IMPS 24x7 Fast Transfer",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Destination Account", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        Text("HDFC Bank •••• 4092", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PorterNavy)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("UPI ID", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        Text("rider@okhdfcbank", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PorterNavy)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transfer Mode", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        Text("Immediate (Within 60 sec)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PorterTeal)
                    }
                }

                Button(
                    onClick = { onConfirm(amount) },
                    colors = ButtonDefaults.buttonColors(containerColor = PorterTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_instant_cashout_btn")
                ) {
                    Text(
                        text = "Confirm & Cashout ₹${amount.toInt()} Now",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
