package com.example.ui.rider

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.cloud.DeliveryProofAndGpsManager
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.cloud.RiderApprovalWorkflowStatus
import com.example.data.cloud.RiderRegistrationApprovalManager
import com.example.data.cloud.RiderSmartToolsPrefs
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.DeliveryProof
import com.example.data.model.OrderProofRegistry
import com.example.data.model.OrderStatus
import com.example.data.model.PickupProof
import com.example.data.model.RiderProfile
import com.example.data.model.RiderShiftStatus
import com.example.data.repository.RiderRepository
import com.example.data.repository.SharedOrderRepository
import com.example.security.RiderOverspeedSafetyCard
import com.example.ui.components.DigitalSignaturePadCanvas
import com.example.ui.components.DirectDialPadCallButton
import com.example.ui.components.DualDeliveryCelebrationMotivationBanner
import com.example.ui.components.FcmOrderNotificationManager
import com.example.ui.components.MotivationAndPromoManager
import com.example.ui.components.QuickDeliveryDeleteAccountDialog
import com.example.ui.components.QuickDeliveryMandatoryPermissionsGateScreen
import com.example.ui.components.QuickDeliveryNextLevelLaunchOverlay
import com.example.ui.components.QuickDeliveryCuteBunnyLogo
import com.example.ui.components.QuickDeliveryOfficialLogo
import com.example.ui.components.QuickDeliveryPrivacyPolicyDialog
import com.example.ui.components.QuickDeliverySpeedingVehicleIntro
import com.example.ui.components.QuickDeliverySuggestionBoxCard
import com.example.ui.components.QuickDeliveryUserAuthScreen
import com.example.ui.components.hasAllMandatoryAppPermissions
import com.example.ui.components.RiderFeedbackManager
import com.example.ui.components.RiderLiveSelfieCallAndExcelManager
import com.example.ui.components.RiderMandatoryLiveSelfieOnlineGateDialog
import com.example.ui.components.RiderMotivationNotificationBanner
import com.example.ui.components.RiderOrderTransferManager
import com.example.ui.components.strokesToSignatureSvg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val RiderOrange = QuickDeliveryOrange
private val RiderTeal = QuickDeliveryTeal
private val RiderNavy = Color(0xFF0F1E36)
private val RiderSoftBg = Color(0xFFF4F8F9)
private val VerifiedGreen = Color(0xFF10B981)
private val TipGold = Color(0xFFF59E0B)

/**
 * Streamlined Rider Partner Screen:
 * 1. Opening High-Speed Vehicle Animation (`QuickDeliverySpeedingVehicleIntro`).
 * 2. New Rider (Sign Up: Name, Mobile, Email) / Old Rider (Sign In: Mobile or Email) (`QuickDeliveryUserAuthScreen`).
 * 3. Compact Top-Bar Wallet Icon (`rider_wallet_icon_btn`) linked directly to the dedicated full-screen
 *    Rider Earnings, Deliveries & Tips Dashboard (`RiderEarningsDashboardScreen`).
 * 4. Rotating new Motivational Message every time the Rider picks up an order (`RiderMotivationNotificationBanner`).
 * 5. Live GPS Tracking active from Rider Order Acceptance all the way until Delivery.
 */
@Composable
fun RiderPartnerCompactScreen(
    modifier: Modifier = Modifier,
    onOpenThreeDotsMenu: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) {
        SharedOrderRepository.getInstance(context).also { repo ->
            AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
        }
    }
    remember(context) { RiderFeedbackManager.ensureLoaded(context) }
    remember(context) { RiderOrderTransferManager.ensureLoaded(context) }
    remember(context) { RiderLiveSelfieCallAndExcelManager.ensureLoaded(context) }
    remember(context) { RiderThemeManager.ensureLoaded(context) }
    remember(context) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    remember(context) { RiderSmartToolsPrefs.ensureLoaded(context) }
    val remoteFlags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
    val liveSyncStatus by MultiPhoneLiveSyncManager.syncStatus.collectAsState()
    val pendingNotificationTap by FcmOrderNotificationManager.pendingNotificationTap.collectAsState()
    val voiceAlertEnabled by RiderSmartToolsPrefs.voiceAlertEnabled.collectAsState()
    val minPayoutFilterInr by RiderSmartToolsPrefs.minPayoutFilterInr.collectAsState()
    val preferredAreaKeyword by RiderSmartToolsPrefs.preferredAreaKeyword.collectAsState()
    val isDarkMode by RiderThemeManager.isDarkMode.collectAsState()
    val screenBgColor = if (isDarkMode) Color(0xFF0B1320) else RiderSoftBg
    val cardSurfaceColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val primaryHeadingColor = if (isDarkMode) Color(0xFFF8FAFC) else RiderNavy
    val secondaryTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    val riders by repository.riders.collectAsState()
    val orders by repository.orders.collectAsState()
    val walletTransactions by repository.walletTransactions.collectAsState()
    val tipsByOrder by RiderFeedbackManager.tipsByOrder.collectAsState()
    val selectedActiveRiderId by AutomatedFairRiderAssignmentEngine.selectedActiveRiderIdInApp.collectAsState()

    val currentRider: RiderProfile? = remember(riders, selectedActiveRiderId) {
        riders.firstOrNull { it.riderId == selectedActiveRiderId } ?: riders.firstOrNull()
    }

    // Same-day (Today only) completed trips & earning — past days' earnings stay inside Wallet
    val todayDateTokens = remember {
        val now = java.util.Date()
        listOf(
            java.text.SimpleDateFormat("dd MMM yyyy", Locale.US).format(now),
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
            java.text.SimpleDateFormat("dd MMM", Locale.US).format(now)
        )
    }
    val todayDeliveredOrders = remember(orders, todayDateTokens) {
        orders.filter { order ->
            order.orderStatus == OrderStatus.DELIVERED && (
                order.createdAt.isBlank() ||
                    order.createdAt.contains("Today", ignoreCase = true) ||
                    order.createdAt.contains("Live Sync", ignoreCase = true) ||
                    todayDateTokens.any { token -> order.createdAt.contains(token, ignoreCase = true) }
                )
        }
    }
    val todaySameDayEarningInr = remember(todayDeliveredOrders, tipsByOrder) {
        todayDeliveredOrders.sumOf { order ->
            val base = order.riderEarning
                .replace("₹", "")
                .replace(",", "")
                .trim()
                .toDoubleOrNull() ?: 0.0
            val tip = (tipsByOrder[order.orderId] ?: 0).toDouble()
            base + tip
        }
    }

    val todayDistanceKm = remember(todayDeliveredOrders) {
        todayDeliveredOrders.sumOf { it.distanceKm.coerceAtLeast(0.0) }
    }

    remember(context) { MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context) }
    remember(context) { RiderRegistrationApprovalManager.ensureLoaded(context) }
    val registrationsMap by RiderRegistrationApprovalManager.registrationsByKey.collectAsState()

    // 0. Speeding Vehicle Intro Animation & Rider Sign Up / Sign In State (cached in session for 0ms tab switches)
    var showRiderSpeedIntro by remember { mutableStateOf(!MotivationAndPromoManager.riderIntroCompleted) }
    var isRiderAuthenticated by remember { mutableStateOf(MotivationAndPromoManager.riderAuthenticatedInSession) }
    var riderDisplayName by remember(currentRider?.riderId) {
        mutableStateOf(MotivationAndPromoManager.cachedRiderName.ifBlank { currentRider?.name ?: "Rider Partner" })
    }
    var riderPhone by remember { mutableStateOf(MotivationAndPromoManager.cachedRiderPhone) }
    var riderEmail by remember { mutableStateOf(MotivationAndPromoManager.cachedRiderEmail) }

    // Full-screen dedicated Rider Earnings, Deliveries & Tips Dashboard state (linked from Wallet icon)
    var showEarningsDashboard by rememberSaveable { mutableStateOf(false) }
    var showRiderProfileDialog by rememberSaveable { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorBanner by remember { mutableStateOf(false) }
    val riderMainListState = rememberLazyListState()

    // Rotating motivational message shown on every order pickup (new message every time!)
    var riderPickupMotivationMessage by remember { mutableStateOf<String?>(null) }
    val riderPostDeliveryMotivation by MotivationAndPromoManager.riderPostDeliveryMotivation.collectAsState()

    val rejectedOrderPrefs = remember(context) {
        context.getSharedPreferences("quick_delivery_rider_rejected_orders_v1", android.content.Context.MODE_PRIVATE)
    }
    var locallyRejectedOrderIds by remember {
        mutableStateOf(rejectedOrderPrefs.getStringSet("rejected_ids", emptySet())?.toSet() ?: emptySet())
    }

    LaunchedEffect(orders, voiceAlertEnabled, locallyRejectedOrderIds) {
        MotivationAndPromoManager.observeOrdersForDeliveryMotivation(orders)
        FcmOrderNotificationManager.observeOrdersAndDispatchFcmNotifications(context, orders)
        val myRiderId = currentRider?.riderId
        val eligibleIncoming = orders.filter { ord ->
            ord.orderStatus == OrderStatus.SEARCHING_RIDER &&
                ord.orderId !in locallyRejectedOrderIds &&
                (myRiderId == null || myRiderId !in ord.rejectedByRiderIds)
        }
        if (voiceAlertEnabled) {
            eligibleIncoming.forEach { incomingOrder ->
                RiderSmartToolsPrefs.speakNewOrderIfRider(context, incomingOrder)
            }
        }
        if (eligibleIncoming.isEmpty()) {
            com.example.security.OrderAlertSoundManager.stopOrderAlert()
        }
    }

    var activePhotoOrderId by remember { mutableStateOf<String?>(null) }
    val orderPhotoProofMap = remember { mutableStateMapOf<String, String>() }
    val orderPhotoBitmapMap = remember { mutableStateMapOf<String, Bitmap>() }
    val orderLiveGpsMap = remember { mutableStateMapOf<String, String>() }

    val todayCodCollectedInr = remember(todayDeliveredOrders) {
        todayDeliveredOrders
            .filter { it.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY }
            .sumOf { o ->
                o.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
            }
    }

    val hasAssignedActiveOrder = remember(orders) {
        orders.any {
            it.orderStatus == OrderStatus.RIDER_ASSIGNED ||
                it.orderStatus == OrderStatus.ARRIVED_AT_PICKUP ||
                it.orderStatus == OrderStatus.PICKED_UP ||
                it.orderStatus == OrderStatus.IN_TRANSIT
        }
    }
    LaunchedEffect(hasAssignedActiveOrder) {
        if (!hasAssignedActiveOrder) return@LaunchedEffect
        while (true) {
            if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
                val freshCoords = runCatching { DeliveryProofAndGpsManager.fetchLiveGpsLocation(context) }.getOrNull()
                if (freshCoords != null) {
                    val formatted = String.format(Locale.US, "%.5f, %.5f", freshCoords.first, freshCoords.second)
                    orders.filter {
                        it.orderStatus == OrderStatus.RIDER_ASSIGNED ||
                            it.orderStatus == OrderStatus.ARRIVED_AT_PICKUP ||
                            it.orderStatus == OrderStatus.PICKED_UP ||
                            it.orderStatus == OrderStatus.IN_TRANSIT
                    }.forEach { activeOrd ->
                        orderLiveGpsMap[activeOrd.orderId] = formatted
                        QuickDeliveryOperationsManager.updateRiderCoordsForOrder(
                            context = context,
                            orderId = activeOrd.orderId,
                            lat = freshCoords.first,
                            lng = freshCoords.second
                        )
                    }
                    currentRider?.let { r ->
                        val updatedR = r.copy(currentLat = freshCoords.first, currentLng = freshCoords.second)
                        val nextFleet = repository.riders.value.map { if (it.riderId == updatedR.riderId) updatedR else it }
                        AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, nextFleet)
                    }
                }
            }
            kotlinx.coroutines.delay(12_000L)
        }
    }

    // Persist delivered orders moved to Trip History so Rider Home resets cleanly for new orders
    val archivedOrderPrefs = remember(context) {
        context.getSharedPreferences("quick_delivery_rider_archived_orders_v1", android.content.Context.MODE_PRIVATE)
    }
    var archivedCompletedOrderIds by remember {
        mutableStateOf(archivedOrderPrefs.getStringSet("archived_ids", emptySet())?.toSet() ?: emptySet())
    }
    val unarchivedHomeOrders = remember(orders, archivedCompletedOrderIds, locallyRejectedOrderIds, currentRider?.riderId) {
        val myRiderId = currentRider?.riderId
        orders.filter { ord ->
            ord.orderStatus != OrderStatus.CANCELLED &&
                ord.orderId !in archivedCompletedOrderIds &&
                !(ord.orderStatus == OrderStatus.SEARCHING_RIDER &&
                    (ord.orderId in locallyRejectedOrderIds || (myRiderId != null && myRiderId in ord.rejectedByRiderIds)))
        }
    }
    val areaQuery = preferredAreaKeyword.trim()
    val activeHomeOrders = remember(unarchivedHomeOrders, minPayoutFilterInr, areaQuery) {
        unarchivedHomeOrders.filter { order ->
            if (order.orderStatus != OrderStatus.SEARCHING_RIDER) {
                true
            } else {
                val earningVal = order.riderEarning
                    .replace("₹", "")
                    .replace(",", "")
                    .trim()
                    .toDoubleOrNull() ?: 0.0
                val passesPayout = minPayoutFilterInr <= 0 || earningVal >= minPayoutFilterInr
                val passesArea = areaQuery.isBlank() ||
                    order.pickupAddress.fullAddress.contains(areaQuery, ignoreCase = true) ||
                    order.dropAddress.fullAddress.contains(areaQuery, ignoreCase = true)
                passesPayout && passesArea
            }
        }
    }
    val hiddenByFilterCount = (unarchivedHomeOrders.size - activeHomeOrders.size).coerceAtLeast(0)

    val ongoingTripForBatch = remember(activeHomeOrders) {
        activeHomeOrders.firstOrNull {
            it.orderStatus == OrderStatus.RIDER_ASSIGNED ||
                it.orderStatus == OrderStatus.ARRIVED_AT_PICKUP ||
                it.orderStatus == OrderStatus.PICKED_UP ||
                it.orderStatus == OrderStatus.IN_TRANSIT
        }
    }
    val sameRouteBatchMatches = remember(activeHomeOrders, ongoingTripForBatch) {
        val currentTrip = ongoingTripForBatch ?: return@remember emptyList<CustomerOrder>()
        val pinRegex = Regex("\\b\\d{6}\\b")
        val tripPins = (pinRegex.findAll(currentTrip.pickupAddress.fullAddress).map { it.value } +
            pinRegex.findAll(currentTrip.dropAddress.fullAddress).map { it.value }).toSet()
        val tripTokens = (currentTrip.pickupAddress.fullAddress + " " + currentTrip.dropAddress.fullAddress)
            .lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 5 }
            .toSet()
        activeHomeOrders.filter { candidate ->
            if (candidate.orderStatus != OrderStatus.SEARCHING_RIDER || candidate.orderId == currentTrip.orderId) {
                false
            } else {
                val candText = candidate.pickupAddress.fullAddress + " " + candidate.dropAddress.fullAddress
                val candPins = pinRegex.findAll(candText).map { it.value }.toSet()
                val sharesPin = tripPins.any { it in candPins }
                val candTokens = candText.lowercase(Locale.US).split(Regex("[^a-z0-9]+")).filter { it.length >= 5 }
                val sharesLocality = candTokens.any { it in tripTokens }
                val hasValidCoords = currentTrip.pickupAddress.lat != 0.0 && candidate.pickupAddress.lat != 0.0
                val isCloseCoords = if (hasValidCoords) {
                    val dLat = candidate.pickupAddress.lat - currentTrip.pickupAddress.lat
                    val dLng = candidate.pickupAddress.lng - currentTrip.pickupAddress.lng
                    kotlin.math.sqrt(dLat * dLat + dLng * dLng) * 111.0 <= 3.5
                } else false
                sharesPin || sharesLocality || isCloseCoords
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        val orderId = activePhotoOrderId
        if (bitmap != null && orderId != null) {
            val targetOrder = orders.firstOrNull { it.orderId == orderId }
            val stageLabel = when (targetOrder?.orderStatus) {
                OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> "Drop"
                else -> "Pickup"
            }
            val fallbackLat = currentRider?.currentLat?.takeIf { it != 0.0 }
                ?: if (stageLabel == "Drop") targetOrder?.dropAddress?.lat else targetOrder?.pickupAddress?.lat
            val fallbackLng = currentRider?.currentLng?.takeIf { it != 0.0 }
                ?: if (stageLabel == "Drop") targetOrder?.dropAddress?.lng else targetOrder?.pickupAddress?.lng

            // Immediately apply visual GPS + Date/Time watermark so preview renders right away
            val initialWatermarked = DeliveryProofAndGpsManager.stampGpsTimestampWatermark(
                sourceBitmap = bitmap,
                orderId = orderId,
                stageLabel = stageLabel,
                lat = fallbackLat?.takeIf { it != 0.0 },
                lng = fallbackLng?.takeIf { it != 0.0 }
            )
            orderPhotoBitmapMap[orderId] = initialWatermarked

            val summary = formatWatermarkedRiderPhotoSummary(
                bitmap = bitmap,
                stageLabel = stageLabel,
                lat = fallbackLat,
                lng = fallbackLng,
                fallbackGps = orderLiveGpsMap[orderId]
            )
            orderPhotoProofMap[orderId] = summary
            isErrorBanner = false
            statusMessage = "📷 $stageLabel photo watermarked (GPS + Date/Time) & saved for Order #$orderId"

            scope.launch {
                val (liveWatermarked, rLat, rLng) = DeliveryProofAndGpsManager.applyLiveGpsWatermark(
                    context = context,
                    sourceBitmap = bitmap,
                    orderId = orderId,
                    stageLabel = stageLabel,
                    fallbackLat = fallbackLat,
                    fallbackLng = fallbackLng
                )
                orderPhotoBitmapMap[orderId] = liveWatermarked
                val finalSummary = formatWatermarkedRiderPhotoSummary(
                    bitmap = liveWatermarked,
                    stageLabel = stageLabel,
                    lat = rLat.takeIf { it != 0.0 } ?: fallbackLat,
                    lng = rLng.takeIf { it != 0.0 } ?: fallbackLng,
                    fallbackGps = orderLiveGpsMap[orderId]
                )
                orderPhotoProofMap[orderId] = finalSummary
                currentRider?.let { rider ->
                    repository.sendRiderQuickMessage(orderId, rider, finalSummary)
                }
                if (targetOrder != null) {
                    com.example.ui.components.OrderLiveChatManager.sendChatMessage(
                        context = context,
                        order = targetOrder,
                        senderRole = "RIDER",
                        senderName = currentRider?.name ?: riderDisplayName,
                        messageText = finalSummary
                    )
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            val orderId = activePhotoOrderId
            if (orderId != null) {
                val summary = "Parcel Photo Attached"
                orderPhotoProofMap[orderId] = summary
                isErrorBanner = false
                statusMessage = "Parcel photo attached for Order #$orderId"
            } else {
                statusMessage = "Please allow camera access to take a parcel photo."
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val targetOrderId = activePhotoOrderId ?: orders.firstOrNull()?.orderId
        if (granted && targetOrderId != null) {
            scope.launch {
                val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                if (coords != null) {
                    val formatted = String.format(Locale.US, "%.5f, %.5f", coords.first, coords.second)
                    orderLiveGpsMap[targetOrderId] = formatted
                    currentRider?.let { rider ->
                        val updatedRider = rider.copy(currentLat = coords.first, currentLng = coords.second)
                        val newFleet = riders.map { if (it.riderId == updatedRider.riderId) updatedRider else it }
                        AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newFleet)
                        repository.sendRiderQuickMessage(targetOrderId, updatedRider, "📍 Live Location: $formatted")
                    }
                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                    isErrorBanner = false
                    statusMessage = "Live location updated for Order #$targetOrderId: $formatted"
                } else {
                    isErrorBanner = true
                    statusMessage = if (!DeliveryProofAndGpsManager.isLocationServiceEnabled(context)) {
                        "Unable to get GPS signal — please turn ON device Location (GPS) and retry."
                    } else {
                        "Unable to capture fresh GPS fix right now. Please move to open sky and retry."
                    }
                }
            }
        } else if (!granted) {
            isErrorBanner = true
            statusMessage = "Please allow location access to share live delivery progress."
        }
    }

    // 1. Speeding Vehicle Intro Animation when Rider App opens
    if (showRiderSpeedIntro) {
        QuickDeliverySpeedingVehicleIntro(
            appTitle = "Partner Quick Delivery",
            appSubtitle = "Fast Rider Dispatch • 80% Partner Earnings",
            accentColor = RiderOrange,
            onAnimationFinished = {
                MotivationAndPromoManager.riderIntroCompleted = true
                showRiderSpeedIntro = false
            }
        )
        return
    }

    // Rider authentication & mandatory permissions gate state
    var isRiderLoggedIn by remember { mutableStateOf(MotivationAndPromoManager.riderAuthenticatedInSession) }
    var allRiderPermissionsGranted by remember { mutableStateOf(hasAllMandatoryAppPermissions(context)) }
    if (!isRiderLoggedIn) {
        QuickDeliveryUserAuthScreen(
            roleTitle = "Rider Partner",
            accentColor = RiderOrange,
            prefsKeyPrefix = "rider",
            context = context,
            onAuthenticated = { name, phone, email ->
                riderDisplayName = name
                riderPhone = phone
                riderEmail = email
                if (MotivationAndPromoManager.lastAuthWasNewSignUp) {
                    RiderRegistrationApprovalManager.onRiderNewSignUp(context, name, phone, email)
                } else {
                    val existingRec = RiderRegistrationApprovalManager.getRecordForIdentity(context, phone, email)
                    if (existingRec?.approvalStatus == RiderApprovalWorkflowStatus.APPROVED) {
                        RiderRegistrationApprovalManager.markRiderReLoggedInAfterApproval(context, phone, email)
                    }
                }
                MotivationAndPromoManager.forceSignInTabNextOpen = false
                MotivationAndPromoManager.riderApprovalBannerMessage = null
                MotivationAndPromoManager.riderAuthenticatedInSession = true
                isRiderLoggedIn = true
                allRiderPermissionsGranted = hasAllMandatoryAppPermissions(context)
                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                statusMessage = "Welcome, Partner $name!"
            }
        )
        return
    }

    if (!allRiderPermissionsGranted) {
        QuickDeliveryMandatoryPermissionsGateScreen(
            roleTitle = "Rider Partner",
            prefsKeyPrefix = "rider",
            accentColor = RiderOrange,
            onAllPermissionsGranted = {
                allRiderPermissionsGranted = true
            },
            onLogout = {
                MotivationAndPromoManager.logoutRider(context)
                isRiderLoggedIn = false
            }
        )
        return
    }

    val riderRegRecord = remember(registrationsMap, riderPhone, riderEmail) {
        RiderRegistrationApprovalManager.getRecordForIdentity(context, riderPhone, riderEmail)
    }

    if (riderRegRecord != null) {
        if (riderRegRecord.approvalStatus == RiderApprovalWorkflowStatus.NEEDS_KYC ||
            riderRegRecord.approvalStatus == RiderApprovalWorkflowStatus.REJECTED
        ) {
            BackHandler {
                MotivationAndPromoManager.logoutRider(context)
                isRiderLoggedIn = false
            }
            RiderFirstTimeKycUploadScreen(
                riderName = riderDisplayName.ifBlank { riderRegRecord.name },
                riderPhone = riderPhone.ifBlank { riderRegRecord.phone },
                riderEmail = riderEmail.ifBlank { riderRegRecord.email },
                existingRecord = riderRegRecord,
                onDocumentsSubmitted = {
                    isErrorBanner = false
                    statusMessage = "Documents submitted! Waiting for Admin approval."
                },
                onSignOut = {
                    MotivationAndPromoManager.logoutRider(context)
                    isRiderLoggedIn = false
                },
                modifier = modifier
            )
            return
        }
        if (riderRegRecord.approvalStatus == RiderApprovalWorkflowStatus.PENDING_APPROVAL ||
            riderRegRecord.requiresReLoginAfterApproval
        ) {
            BackHandler {
                MotivationAndPromoManager.logoutRider(context)
                isRiderLoggedIn = false
            }
            RiderPendingAdminApprovalScreen(
                record = riderRegRecord,
                onRequireReLogin = {
                    MotivationAndPromoManager.forceSignInTabNextOpen = true
                    MotivationAndPromoManager.riderApprovalBannerMessage =
                        "✅ Admin Approved! Please Sign In again with your Mobile/Email to go Online."
                    MotivationAndPromoManager.logoutRider(context)
                    isRiderLoggedIn = false
                },
                onSignOut = {
                    MotivationAndPromoManager.logoutRider(context)
                    isRiderLoggedIn = false
                },
                modifier = modifier
            )
            return
        }
    }

    var deferredBankSetupForSession by rememberSaveable(riderRegRecord?.riderKey) { mutableStateOf(false) }
    if (riderRegRecord != null &&
        riderRegRecord.approvalStatus == RiderApprovalWorkflowStatus.APPROVED &&
        riderRegRecord.reLoggedInAfterApproval &&
        !riderRegRecord.hasBankAccountDetails &&
        !deferredBankSetupForSession
    ) {
        BackHandler {
            deferredBankSetupForSession = true
        }
        RiderBankAccountFormScreen(
            riderName = riderDisplayName.ifBlank { riderRegRecord.name },
            riderPhone = riderPhone.ifBlank { riderRegRecord.phone },
            riderEmail = riderEmail.ifBlank { riderRegRecord.email },
            riderId = currentRider?.riderId ?: riderRegRecord.riderId,
            existingRecord = riderRegRecord,
            isOnboardingStep = true,
            onBankDetailsSaved = { saved ->
                isErrorBanner = false
                statusMessage = "🏦 Bank Account (${saved.bankName} ${saved.maskedBankAccountNumber}) saved & synced with Admin!"
            },
            onSkipOrClose = {
                deferredBankSetupForSession = true
            },
            onSignOut = {
                MotivationAndPromoManager.logoutRider(context)
                isRiderLoggedIn = false
            },
            modifier = modifier
        )
        return
    }

    LaunchedEffect(
        riderRegRecord?.riderKey,
        riderRegRecord?.approvalStatus,
        riderRegRecord?.reLoggedInAfterApproval
    ) {
        if (riderRegRecord != null &&
            riderRegRecord.approvalStatus == RiderApprovalWorkflowStatus.APPROVED &&
            riderRegRecord.reLoggedInAfterApproval
        ) {
            RiderRegistrationApprovalManager.ensureRiderProfileInRepository(repository, riderRegRecord)
        }
    }

    val totalLiveTipsInr = remember(tipsByOrder) {
        tipsByOrder.values.sum()
    }

    // Porter Partner Left Navigation Drawer & Dialogs State (hoisted so state is preserved across sub-screens)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showRechargeDialog by rememberSaveable { mutableStateOf(false) }
    var showIncentivesDialog by rememberSaveable { mutableStateOf(false) }
    var showTripHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showGuidelinesDialog by rememberSaveable { mutableStateOf(false) }
    var showAutoOfflineAlert by remember { mutableStateOf(false) }
    var showSuspendedDialog by remember { mutableStateOf(false) }
    var showCashoutDialog by rememberSaveable { mutableStateOf(false) }
    var cashoutAmount by rememberSaveable { mutableStateOf(0.0) }
    var showKycCenterDialog by rememberSaveable { mutableStateOf(false) }
    var showRiderSuggestionDialog by rememberSaveable { mutableStateOf(false) }
    var showTopSosEmergencyDialog by rememberSaveable { mutableStateOf(false) }
    var showLiveSelfieOnlineGateDialog by rememberSaveable { mutableStateOf(false) }
    var showRiderPrivacyPolicyDialog by rememberSaveable { mutableStateOf(false) }
    var showRiderDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showPartnerSupportDialog by rememberSaveable { mutableStateOf(false) }
    var showBankAccountDialog by rememberSaveable { mutableStateOf(false) }
    var orderPendingRejection by remember { mutableStateOf<CustomerOrder?>(null) }
    val selfieCheckIns by RiderLiveSelfieCallAndExcelManager.selfieCheckIns.collectAsState()
    val partnerSupportTickets by com.example.data.cloud.QuickDeliveryOperationsManager.supportTickets.collectAsState()

    LaunchedEffect(pendingNotificationTap?.eventSeq) {
        val tap = pendingNotificationTap ?: return@LaunchedEffect
        val tapOrderId = tap.orderId.trim()
        showEarningsDashboard = false
        showTripHistoryDialog = false
        showKycCenterDialog = false
        showPartnerSupportDialog = false
        showBankAccountDialog = false
        showRiderProfileDialog = false
        showCashoutDialog = false
        showRechargeDialog = false
        showIncentivesDialog = false
        showGuidelinesDialog = false
        showRiderSuggestionDialog = false
        showTopSosEmergencyDialog = false
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        isErrorBanner = false
        statusMessage = when {
            tap.title.isNotBlank() -> tap.title
            tapOrderId.isNotBlank() -> "🛵 Opened Order #$tapOrderId"
            else -> "🔔 Notification opened"
        }
        val orderIdxInList = if (tapOrderId.isNotBlank()) {
            activeHomeOrders.indexOfFirst { it.orderId.equals(tapOrderId, ignoreCase = true) }
        } else -1
        runCatching {
            if (orderIdxInList >= 0) {
                riderMainListState.animateScrollToItem(orderIdxInList + 3)
            } else {
                riderMainListState.animateScrollToItem(0)
            }
        }
        FcmOrderNotificationManager.consumeNotificationTap(tap.eventSeq)
    }

    val hasRiderSubNavigation = orderPendingRejection != null ||
        showRiderDeleteAccountDialog ||
        showRiderPrivacyPolicyDialog ||
        showPartnerSupportDialog ||
        showBankAccountDialog ||
        showLiveSelfieOnlineGateDialog ||
        showTopSosEmergencyDialog ||
        showRiderSuggestionDialog ||
        showKycCenterDialog ||
        showCashoutDialog ||
        showSuspendedDialog ||
        showAutoOfflineAlert ||
        showGuidelinesDialog ||
        showTripHistoryDialog ||
        showIncentivesDialog ||
        showRechargeDialog ||
        showRiderProfileDialog ||
        showEarningsDashboard ||
        drawerState.isOpen

    BackHandler(enabled = hasRiderSubNavigation) {
        when {
            orderPendingRejection != null -> orderPendingRejection = null
            showRiderDeleteAccountDialog -> showRiderDeleteAccountDialog = false
            showRiderPrivacyPolicyDialog -> showRiderPrivacyPolicyDialog = false
            showPartnerSupportDialog -> showPartnerSupportDialog = false
            showBankAccountDialog -> showBankAccountDialog = false
            showLiveSelfieOnlineGateDialog -> showLiveSelfieOnlineGateDialog = false
            showTopSosEmergencyDialog -> showTopSosEmergencyDialog = false
            showRiderSuggestionDialog -> showRiderSuggestionDialog = false
            showKycCenterDialog -> showKycCenterDialog = false
            showCashoutDialog -> showCashoutDialog = false
            showSuspendedDialog -> showSuspendedDialog = false
            showAutoOfflineAlert -> showAutoOfflineAlert = false
            showGuidelinesDialog -> showGuidelinesDialog = false
            showTripHistoryDialog -> showTripHistoryDialog = false
            showIncentivesDialog -> showIncentivesDialog = false
            showRechargeDialog -> showRechargeDialog = false
            showRiderProfileDialog -> showRiderProfileDialog = false
            showEarningsDashboard -> showEarningsDashboard = false
            drawerState.isOpen -> scope.launch { drawerState.close() }
        }
    }

    // 3. Dedicated Full-Screen Rider Earnings, Deliveries & Tips Dashboard (opened via Wallet icon)
    if (showEarningsDashboard && currentRider != null) {
        RiderEarningsDashboardScreen(
            rider = currentRider.copy(name = riderDisplayName),
            orders = orders,
            transactionCount = walletTransactions.count { it.accountId == currentRider.riderId },
            repository = repository,
            onNavigateBack = { showEarningsDashboard = false },
            onStatusUpdate = { msg, err ->
                statusMessage = msg
                isErrorBanner = err
            },
            modifier = modifier
        )
        return
    }

    // Porter Negative Balance Rules Check:
    // -200: Partner ID Suspended
    // -150: Automatically switch to Offline AFTER current active order completion
    val currentBalance = (currentRider?.walletBalanceInr ?: 0.0) + totalLiveTipsInr
    val isSuspended = currentBalance <= -200.0 || currentRider?.accountStatus == "SUSPENDED"
    val isBelow150Limit = currentBalance <= -150.0

    val hasOngoingActiveOrder = orders.any {
        it.assignedRiderId == currentRider?.riderId &&
            it.orderStatus != OrderStatus.DELIVERED &&
            it.orderStatus != OrderStatus.CANCELLED &&
            it.orderStatus != OrderStatus.SEARCHING_RIDER
    }

    fun updateRiderProfileState(
        newBalance: Double? = null,
        newOnline: Boolean? = null,
        newStatus: String? = null
    ) {
        currentRider?.let { r ->
            val updatedBal = newBalance ?: r.walletBalanceInr
            val updatedStatus = newStatus ?: if (updatedBal <= -200.0) "SUSPENDED" else "ACTIVE"
            val updatedOnline = newOnline ?: if (updatedBal <= -150.0 && !hasOngoingActiveOrder) false else r.isOnline
            val updatedShift = if (updatedOnline) RiderShiftStatus.ON_ACTIVE_SHIFT else RiderShiftStatus.ENDED_AT_HOME
            val updatedRider = r.copy(
                walletBalanceInr = updatedBal,
                accountStatus = updatedStatus,
                isOnline = updatedOnline,
                shiftStatus = updatedShift
            )
            val newFleet = riders.map { if (it.riderId == r.riderId) updatedRider else it }
            AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newFleet)
        }
    }

    // Reactive Negative Balance Enforcement:
    LaunchedEffect(currentBalance, hasOngoingActiveOrder) {
        if (currentBalance <= -200.0) {
            if (currentRider?.accountStatus != "SUSPENDED" || currentRider?.isOnline == true) {
                updateRiderProfileState(newOnline = false, newStatus = "SUSPENDED")
                showSuspendedDialog = true
            }
        } else if (currentBalance <= -150.0) {
            // If no active order in progress, automatically switch to offline!
            if (!hasOngoingActiveOrder && currentRider?.isOnline == true) {
                updateRiderProfileState(newOnline = false, newStatus = "ACTIVE")
                showAutoOfflineAlert = true
            }
        }
    }

    val onToggleDutyClicked: () -> Unit = {
        val regBlock = riderRegRecord != null &&
            !(riderRegRecord.approvalStatus == RiderApprovalWorkflowStatus.APPROVED && riderRegRecord.reLoggedInAfterApproval)
        if (regBlock) {
            isErrorBanner = true
            statusMessage = "⏳ Online Duty Locked: Admin verification & re-login required first."
        } else {
            currentRider?.let { rider ->
                if (isSuspended) {
                    showSuspendedDialog = true
                    statusMessage = "🚫 ID Suspended: Outstanding balance is -₹${Math.abs(currentBalance.toInt())}. Clear dues to reactivate."
                    isErrorBanner = true
                } else if (isBelow150Limit && !rider.isOnline) {
                    showAutoOfflineAlert = true
                    statusMessage = "⚠️ Cannot Go Online: Balance is -₹${Math.abs(currentBalance.toInt())} (≤ -₹150 limit). Please recharge wallet."
                    isErrorBanner = true
                } else if (!rider.isOnline) {
                    val updatedRider = rider.copy(
                        isOnline = true,
                        shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT,
                        accountStatus = "ACTIVE"
                    )
                    val newFleet = riders.map { if (it.riderId == rider.riderId) updatedRider else it }
                    AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newFleet)
                    statusMessage = "🟢 You are now ONLINE! Ready to receive live parcel orders."
                    isErrorBanner = false
                } else if (hasOngoingActiveOrder) {
                    isErrorBanner = true
                    statusMessage = "⚠️ Cannot go OFFLINE while you have an active delivery! Complete delivery first."
                } else {
                    val updatedRider = rider.copy(
                        isOnline = false,
                        shiftStatus = RiderShiftStatus.ENDED_AT_HOME,
                        accountStatus = if (currentBalance <= -200.0) "SUSPENDED" else "ACTIVE"
                    )
                    val newFleet = riders.map { if (it.riderId == rider.riderId) updatedRider else it }
                    AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newFleet)
                    statusMessage = "⏸️ You are now OFFLINE."
                    isErrorBanner = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(320.dp)
            ) {
                PorterRiderDrawerContent(
                    rider = currentRider,
                    riderDisplayName = riderDisplayName,
                    walletBalance = currentRider?.walletBalanceInr ?: 0.0,
                    totalTipsInr = totalLiveTipsInr,
                    isOnline = currentRider?.isOnline == true,
                    isSuspended = isSuspended,
                    orders = orders,
                    onToggleDuty = onToggleDutyClicked,
                    onOpenProfile = {
                        scope.launch { drawerState.close() }
                        showRiderProfileDialog = true
                    },
                    onOpenWallet = {
                        scope.launch { drawerState.close() }
                        showEarningsDashboard = true
                    },
                    onOpenTripHistory = {
                        scope.launch { drawerState.close() }
                        showTripHistoryDialog = true
                    },
                    onOpenIncentives = {
                        scope.launch { drawerState.close() }
                        showIncentivesDialog = true
                    },
                    onOpenRecharge = {
                        scope.launch { drawerState.close() }
                        showRechargeDialog = true
                    },
                    onOpenSupport = {
                        scope.launch { drawerState.close() }
                        showPartnerSupportDialog = true
                    },
                    onOpenGuidelines = {
                        scope.launch { drawerState.close() }
                        showGuidelinesDialog = true
                    },
                    onOpenSafety = {
                        scope.launch { drawerState.close() }
                        showTopSosEmergencyDialog = true
                    },
                    onOpenKycDocuments = {
                        scope.launch { drawerState.close() }
                        showKycCenterDialog = true
                    },
                    onOpenBankAccount = {
                        scope.launch { drawerState.close() }
                        showBankAccountDialog = true
                    },
                    hasBankDetails = riderRegRecord?.hasBankAccountDetails == true,
                    bankSummary = if (riderRegRecord?.hasBankAccountDetails == true) {
                        "${riderRegRecord.bankName} ${riderRegRecord.maskedBankAccountNumber} • ${riderRegRecord.bankIfscCode}"
                    } else {
                        "Add Bank A/C, IFSC & UPI for Payouts"
                    },
                    onOpenSuggestionBox = {
                        scope.launch { drawerState.close() }
                        showRiderSuggestionDialog = true
                    },
                    onInstantCashout = { amount ->
                        scope.launch { drawerState.close() }
                        cashoutAmount = amount
                        showCashoutDialog = true
                    },
                    onSimulateBalance = { newBal ->
                        scope.launch { drawerState.close() }
                        updateRiderProfileState(
                            newBalance = newBal,
                            newStatus = if (newBal <= -200.0) "SUSPENDED" else "ACTIVE",
                            newOnline = if (newBal <= -150.0 && !hasOngoingActiveOrder) false else currentRider?.isOnline
                        )
                        if (newBal <= -200.0) {
                            showSuspendedDialog = true
                            statusMessage = "Simulated Balance: ₹${newBal.toInt()} → Partner ID Suspended!"
                            isErrorBanner = true
                        } else if (newBal <= -150.0) {
                            showAutoOfflineAlert = true
                            statusMessage = if (hasOngoingActiveOrder) {
                                "Simulated Balance: ₹${newBal.toInt()} → Auto-Offline will trigger as soon as order completes!"
                            } else {
                                "Simulated Balance: ₹${newBal.toInt()} → Auto-Offline Enforced immediately!"
                            }
                            isErrorBanner = true
                        } else {
                            statusMessage = "✅ Wallet Recharged to ₹${newBal.toInt()}! All dues cleared, ID reactivated."
                            isErrorBanner = false
                        }
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        MotivationAndPromoManager.logoutRider(context)
                        isRiderLoggedIn = false
                        statusMessage = "Signed out of Partner Quick Delivery account"
                    },
                    onOpenPrivacyPolicy = {
                        scope.launch { drawerState.close() }
                        showRiderPrivacyPolicyDialog = true
                    },
                    onDeleteAccount = {
                        scope.launch { drawerState.close() }
                        showRiderDeleteAccountDialog = true
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = screenBgColor,
            topBar = {
                Surface(
                    color = PorterNavy,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Hamburger menu (☰) + Partner Quick Delivery Branding
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("rider_drawer_hamburger_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Partner Quick Delivery Menu (☰)",
                                    tint = Color.White
                                )
                            }
                            QuickDeliveryCuteBunnyLogo(size = 30.dp, borderColor = QuickDeliveryOrange)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Partner Quick Delivery",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "FAST. SIMPLE. SECURE.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = QuickDeliveryOrange,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right: Compact Emergency SOS Button + Duty Online/Offline Status & Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = { showTopSosEmergencyDialog = true },
                                color = PorterRed,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("rider_top_header_sos_btn")
                            ) {
                                Text(
                                    text = "🚨 SOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = if (isSuspended) PorterRed else if (currentRider?.isOnline == true) PorterGreen else Color(0xFF64748B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isSuspended) "SUSPENDED" else if (currentRider?.isOnline == true) "ONLINE" else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Switch(
                                checked = currentRider?.isOnline == true,
                                onCheckedChange = { onToggleDutyClicked() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("rider_top_duty_switch")
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (activeHomeOrders.isEmpty()) {
                    Surface(
                        color = if (isDarkMode) Color(0xFF0F172A) else Color.White,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, Color(0xFF008080).copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_searching_new_order_bottom_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Searching for new order",
                                tint = if (currentRider?.isOnline == true) RiderTeal else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentRider?.isOnline == true) {
                                    "Searching for new order..."
                                } else {
                                    "Offline • Go Online to search for new order"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentRider?.isOnline == true) RiderTeal else secondaryTextColor
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (showRiderProfileDialog && currentRider != null) {
                RiderProfileDetailsDialog(
                    rider = currentRider.copy(name = riderDisplayName),
                    totalTipsInr = totalLiveTipsInr,
                    onOpenKycScanner = {
                        showRiderProfileDialog = false
                        showKycCenterDialog = true
                    },
                    onLogout = {
                        showRiderProfileDialog = false
                        MotivationAndPromoManager.logoutRider(context)
                        isRiderLoggedIn = false
                        statusMessage = "Signed out of Partner Quick Delivery account"
                    },
                    onDismiss = { showRiderProfileDialog = false }
                )
            }

            if (showLiveSelfieOnlineGateDialog && currentRider != null) {
                RiderMandatoryLiveSelfieOnlineGateDialog(
                    rider = currentRider,
                    riderDisplayName = riderDisplayName,
                    onLivePhotoVerifiedAndGoOnline = { selfieRecord ->
                        showLiveSelfieOnlineGateDialog = false
                        val updatedRider = currentRider.copy(
                            isOnline = true,
                            shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT,
                            accountStatus = "ACTIVE"
                        )
                        val newFleet = riders.map { if (it.riderId == currentRider.riderId) updatedRider else it }
                        AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newFleet)
                        AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                        isErrorBanner = false
                        statusMessage = "🟢 Live Selfie (${selfieRecord.checkInId}) Uploaded & Saved to Admin Web! You are now ONLINE."
                    },
                    onDismiss = { showLiveSelfieOnlineGateDialog = false }
                )
            }

            if (showKycCenterDialog && currentRider != null) {
                RiderKycDocumentCenterDialog(
                    rider = currentRider.copy(name = riderDisplayName),
                    onDocumentUpdated = { docType, newNumber ->
                        currentRider?.let { r ->
                            val updatedRider = when (docType) {
                                KycDocumentType.AADHAAR -> r.copy(aadhaarNumber = newNumber)
                                KycDocumentType.PAN_CARD -> r.copy(panNumber = newNumber)
                                KycDocumentType.VEHICLE_RC -> r.copy(vehicleNumber = newNumber)
                                KycDocumentType.DRIVING_LICENCE -> r
                            }
                            val newFleet = riders.map { if (it.riderId == r.riderId) updatedRider else it }
                            AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newFleet)
                            statusMessage = "✅ ${docType.title} ($newNumber) scanned & verified!"
                            isErrorBanner = false
                        }
                    },
                    onDismiss = { showKycCenterDialog = false }
                )
            }

            if (showRechargeDialog) {
                PorterWalletRechargeDialog(
                    currentBalance = currentBalance,
                    onRechargeSuccess = { credited ->
                        val newBal = currentBalance + credited
                        updateRiderProfileState(
                            newBalance = newBal,
                            newStatus = "ACTIVE",
                            newOnline = currentRider?.isOnline
                        )
                        showRechargeDialog = false
                        showSuspendedDialog = false
                        showAutoOfflineAlert = false
                        statusMessage = "✅ Recharged ₹${credited.toInt()} successfully! New Balance: ₹${newBal.toInt()}"
                        isErrorBanner = false
                    },
                    onDismiss = { showRechargeDialog = false }
                )
            }

            if (showIncentivesDialog) {
                PorterIncentivesDialog(
                    completedToday = orders.count { it.orderStatus == OrderStatus.DELIVERED },
                    onDismiss = { showIncentivesDialog = false }
                )
            }

            if (showTripHistoryDialog) {
                PorterTripHistoryDialog(
                    orders = orders,
                    onDismiss = { showTripHistoryDialog = false }
                )
            }

            if (showGuidelinesDialog) {
                PorterGuidelinesDialog(
                    onDismiss = { showGuidelinesDialog = false }
                )
            }

            if (showBankAccountDialog) {
                RiderBankAccountFormDialog(
                    riderName = riderDisplayName.ifBlank { riderRegRecord?.name ?: "Rider Partner" },
                    riderPhone = riderPhone.ifBlank { riderRegRecord?.phone.orEmpty() },
                    riderEmail = riderEmail.ifBlank { riderRegRecord?.email.orEmpty() },
                    riderId = currentRider?.riderId ?: riderRegRecord?.riderId.orEmpty(),
                    existingRecord = riderRegRecord,
                    onBankDetailsSaved = { saved ->
                        showBankAccountDialog = false
                        isErrorBanner = false
                        statusMessage = "🏦 Payout Bank Account (${saved.bankName} ${saved.maskedBankAccountNumber}) saved & synced!"
                    },
                    onDismiss = { showBankAccountDialog = false }
                )
            }

            if (showCashoutDialog) {
                val bankHasDetails = riderRegRecord?.hasBankAccountDetails == true
                val destDisplay = if (bankHasDetails && riderRegRecord != null) {
                    "${riderRegRecord.bankName} ${riderRegRecord.maskedBankAccountNumber}"
                } else {
                    ""
                }
                PorterInstantCashoutDialog(
                    amount = cashoutAmount,
                    bankAccountDisplay = destDisplay,
                    ifscDisplay = riderRegRecord?.bankIfscCode.orEmpty(),
                    accountHolderDisplay = riderRegRecord?.bankAccountHolderName.orEmpty(),
                    upiIdDisplay = riderRegRecord?.bankUpiId.orEmpty(),
                    hasBankDetails = bankHasDetails,
                    onEditBankAccount = {
                        showCashoutDialog = false
                        showBankAccountDialog = true
                    },
                    onConfirm = { amount ->
                        showCashoutDialog = false
                        val destFull = if (riderRegRecord != null && riderRegRecord.hasBankAccountDetails) {
                            "${riderRegRecord.bankAccountHolderName} | ${riderRegRecord.bankName} A/C ${riderRegRecord.bankAccountNumber} | IFSC: ${riderRegRecord.bankIfscCode}" +
                                if (riderRegRecord.bankUpiId.isNotBlank()) " | UPI: ${riderRegRecord.bankUpiId}" else ""
                        } else {
                            ""
                        }
                        val req = com.example.data.cloud.QuickDeliveryOperationsManager.submitRiderWithdrawalRequest(
                            context = context,
                            riderId = currentRider?.riderId ?: riderRegRecord?.riderId ?: "RIDER-101",
                            riderName = riderDisplayName.ifBlank { currentRider?.name ?: "Rider Partner" },
                            riderPhone = riderPhone.ifBlank { currentRider?.mobile.orEmpty() },
                            amountInr = amount,
                            payoutDestination = destFull
                        )
                        if (req != null) {
                            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                            statusMessage = "✅ Payout Request #${req.requestId} (₹${amount.toInt()}) sent to Admin for ${riderRegRecord?.bankName ?: "Bank"} ${riderRegRecord?.maskedBankAccountNumber ?: ""}!"
                            isErrorBanner = false
                        } else {
                            statusMessage = "⚠️ Add Bank Account details & ensure balance ≥ ₹50 before Cashout."
                            isErrorBanner = true
                        }
                    },
                    onDismiss = { showCashoutDialog = false }
                )
            }

            if (showRiderSuggestionDialog) {
                AlertDialog(
                    onDismissRequest = { showRiderSuggestionDialog = false },
                    title = {
                        Text(
                            text = "Partner Suggestion & Feedback Box",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = RiderNavy
                        )
                    },
                    text = {
                        QuickDeliverySuggestionBoxCard(
                            appRole = "RIDER",
                            senderName = riderDisplayName,
                            accentColor = RiderOrange
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showRiderSuggestionDialog = false }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (showTopSosEmergencyDialog) {
                AlertDialog(
                    onDismissRequest = { showTopSosEmergencyDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PorterRed
                            )
                            Text(
                                text = "🚨 Rider Emergency SOS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PorterRed
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Immediate 24×7 Emergency Assistance for Partner Quick Delivery $riderDisplayName:",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiderNavy
                            )
                            Button(
                                onClick = {
                                    showTopSosEmergencyDialog = false
                                    com.example.data.cloud.QuickDeliveryOperationsManager.submitSupportTicket(
                                        context = context,
                                        senderRole = "RIDER",
                                        senderName = riderDisplayName,
                                        senderPhone = riderPhone,
                                        orderId = orders.firstOrNull { it.assignedRiderId == currentRider?.riderId }?.orderId.orEmpty(),
                                        category = "SOS Emergency",
                                        message = "Partner triggered SOS emergency alert & requested Admin Safety assistance."
                                    )
                                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                    isErrorBanner = true
                                    statusMessage = "🚨 Emergency SOS ticket logged with Admin Safety Desk. Opening 112 dialer..."
                                    runCatching {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                        context.startActivity(dialIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PorterRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_sos_dispatch_alert_btn")
                            ) {
                                Text(
                                    text = "Call National Emergency 112 & Alert Admin",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    showTopSosEmergencyDialog = false
                                    showPartnerSupportDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Open Partner Support Ticket (Admin Desk)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTopSosEmergencyDialog = false }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (showPartnerSupportDialog) {
                var ticketCategory by remember { mutableStateOf("Payout / Wallet") }
                var ticketOrderId by remember {
                    mutableStateOf(orders.firstOrNull { it.assignedRiderId == currentRider?.riderId }?.orderId.orEmpty())
                }
                var ticketMessage by remember { mutableStateOf("") }
                val riderTickets = remember(partnerSupportTickets, riderPhone, riderDisplayName) {
                    partnerSupportTickets.filter { t ->
                        t.senderRole == "RIDER" &&
                            (riderPhone.isBlank() || t.senderPhone == riderPhone || t.senderName == riderDisplayName)
                    }
                }
                AlertDialog(
                    onDismissRequest = { showPartnerSupportDialog = false },
                    title = {
                        Text(
                            text = "Partner Support Desk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = RiderNavy
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Raise a support ticket directly to Admin Ops or email ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Payout / Wallet", "Order Issue", "KYC Review", "App / GPS").forEach { cat ->
                                    FilterChip(
                                        selected = ticketCategory == cat,
                                        onClick = { ticketCategory = cat },
                                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = ticketOrderId,
                                onValueChange = { ticketOrderId = it },
                                label = { Text("Order ID (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = ticketMessage,
                                onValueChange = { ticketMessage = it },
                                label = { Text("Describe your issue *") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_support_ticket_input")
                            )
                            Button(
                                onClick = {
                                    val created = com.example.data.cloud.QuickDeliveryOperationsManager.submitSupportTicket(
                                        context = context,
                                        senderRole = "RIDER",
                                        senderName = riderDisplayName,
                                        senderPhone = riderPhone,
                                        orderId = ticketOrderId,
                                        category = ticketCategory,
                                        message = ticketMessage
                                    )
                                    if (created != null) {
                                        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                        ticketMessage = ""
                                        statusMessage = "✅ Ticket ${created.ticketId} sent to Admin Desk & queued for ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}."
                                        isErrorBanner = false
                                    }
                                },
                                enabled = ticketMessage.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = RiderOrange),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_submit_ticket_btn")
                            ) {
                                Text("Submit Ticket (Admin + Mail Alert)", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val mailSubject = "[Rider Partner Help] $ticketCategory${if (ticketOrderId.isNotBlank()) " • Order #$ticketOrderId" else ""}"
                                    val mailBody = buildString {
                                        appendLine("Partner Name: $riderDisplayName")
                                        appendLine("Mobile: ${riderPhone.ifBlank { "Not provided" }}")
                                        if (ticketOrderId.isNotBlank()) appendLine("Order ID: #$ticketOrderId")
                                        appendLine("Category: $ticketCategory")
                                        appendLine()
                                        appendLine("Issue Details:")
                                        appendLine(ticketMessage.ifBlank { "(Describe your issue here)" })
                                    }
                                    runCatching {
                                        context.startActivity(
                                            com.example.data.cloud.QuickDeliveryBrandLinks.buildSupportMailtoIntent(
                                                subject = mailSubject,
                                                body = mailBody
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_email_support_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Partner Support",
                                    tint = RiderNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Email ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RiderNavy
                                )
                            }
                            if (riderTickets.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = "Your Tickets (${riderTickets.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RiderNavy
                                )
                                riderTickets.take(5).forEach { ticket ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (ticket.isResolved) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${ticket.ticketId} • ${ticket.category}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RiderNavy
                                                )
                                                Text(
                                                    text = if (ticket.isResolved) "RESOLVED" else "OPEN",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (ticket.isResolved) Color(0xFF047857) else Color(0xFFB45309)
                                                )
                                            }
                                            Text(
                                                text = ticket.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF334155)
                                            )
                                            if (ticket.adminReply.isNotBlank()) {
                                                Text(
                                                    text = "Admin Reply: ${ticket.adminReply}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF047857)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPartnerSupportDialog = false }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (showRiderPrivacyPolicyDialog) {
                QuickDeliveryPrivacyPolicyDialog(
                    context = context,
                    onDismiss = { showRiderPrivacyPolicyDialog = false }
                )
            }

            if (showRiderDeleteAccountDialog) {
                QuickDeliveryDeleteAccountDialog(
                    roleTitle = "Rider Partner",
                    accountIdentifier = riderEmail.ifBlank { riderPhone.ifBlank { riderDisplayName } },
                    onConfirmDelete = {
                        showRiderDeleteAccountDialog = false
                        MotivationAndPromoManager.deleteUserAccountData(context, "rider")
                        isRiderLoggedIn = false
                        statusMessage = "Your partner account data has been deleted from this device."
                    },
                    onDismiss = { showRiderDeleteAccountDialog = false }
                )
            }

            orderPendingRejection?.let { rejectTarget ->
                val rejectReasonOptions = listOf(
                    "Pickup location too far",
                    "Vehicle puncture / Breakdown",
                    "Heavy traffic / Road closed",
                    "Parcel too heavy / Oversize",
                    "Customer unreachable at pickup",
                    "Other reason"
                )
                var selectedRejectReason by remember(rejectTarget.orderId) {
                    mutableStateOf(rejectReasonOptions.first())
                }
                var customRejectNote by remember(rejectTarget.orderId) { mutableStateOf("") }
                val isOtherSelected = selectedRejectReason == "Other reason"
                val canConfirmReject = if (isOtherSelected) customRejectNote.trim().length >= 3 else selectedRejectReason.isNotBlank()

                AlertDialog(
                    onDismissRequest = { orderPendingRejection = null },
                    title = {
                        Text(
                            text = "Reject Order #${rejectTarget.orderId}?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = RiderNavy
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Select a mandatory reason to decline. Customer will be notified automatically and another nearby Rider will be searched:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            rejectReasonOptions.forEach { reasonOption ->
                                val isChosen = selectedRejectReason == reasonOption
                                Surface(
                                    color = if (isChosen) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (isChosen) 1.2.dp else 1.dp,
                                        color = if (isChosen) Color(0xFFDC2626) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedRejectReason = reasonOption }
                                        .testTag("rider_reject_reason_${reasonOption.lowercase().replace(" ", "_").replace("/", "_")}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isChosen,
                                            onClick = { selectedRejectReason = reasonOption },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFDC2626))
                                        )
                                        Text(
                                            text = reasonOption,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            color = RiderNavy
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = customRejectNote,
                                onValueChange = { customRejectNote = it },
                                label = {
                                    Text(if (isOtherSelected) "Enter reason details *" else "Additional note (optional)")
                                },
                                placeholder = { Text("e.g., Flat tyre near main road") },
                                singleLine = false,
                                maxLines = 2,
                                isError = isOtherSelected && customRejectNote.trim().length < 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_reject_custom_reason_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = canConfirmReject,
                            onClick = {
                                val detail = customRejectNote.trim()
                                val finalReason = if (isOtherSelected) {
                                    detail
                                } else if (detail.isNotEmpty()) {
                                    "$selectedRejectReason ($detail)"
                                } else {
                                    selectedRejectReason
                                }
                                val rejectingRiderId = (currentRider?.riderId ?: rejectTarget.assignedRiderId ?: "RIDER-LOCAL")
                                    .ifBlank { "RIDER-LOCAL" }

                                com.example.security.OrderAlertSoundManager.markOrderHandledAndStopAlert(rejectTarget.orderId)
                                val nextRejected = locallyRejectedOrderIds + rejectTarget.orderId
                                locallyRejectedOrderIds = nextRejected
                                rejectedOrderPrefs.edit().putStringSet("rejected_ids", nextRejected).apply()
                                QuickDeliveryOperationsManager.clearWaitTimerForOrder(context, rejectTarget.orderId)

                                AutomatedFairRiderAssignmentEngine.markRiderDeclinedAndReleaseOrder(
                                    repository = repository,
                                    orderId = rejectTarget.orderId,
                                    rejectingRiderId = rejectingRiderId,
                                    reason = finalReason
                                )
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                orderPendingRejection = null
                                isErrorBanner = false
                                statusMessage = "❌ Order #${rejectTarget.orderId} declined ($finalReason). Re-searching new rider for customer."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.testTag("rider_confirm_reject_reason_btn")
                        ) {
                            Text("Confirm Reject", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { orderPendingRejection = null }) {
                            Text("Keep Order")
                        }
                    }
                )
            }

        LazyColumn(
            state = riderMainListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("rider_compact_main_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Post-Delivery Celebratory Motivational Notification Banner (sent automatically after every delivery!)
            if (!riderPostDeliveryMotivation.isNullOrBlank()) {
                item(key = "rider_post_delivery_motivation_item") {
                    DualDeliveryCelebrationMotivationBanner(
                        roleLabel = "RIDER",
                        message = riderPostDeliveryMotivation,
                        accentColor = RiderOrange,
                        onDismiss = { MotivationAndPromoManager.dismissRiderPostDeliveryMotivation() },
                        onClick = {
                            MotivationAndPromoManager.dismissRiderPostDeliveryMotivation()
                            showEarningsDashboard = true
                        }
                    )
                }
            }

            // Clean Rider App layout: no extra FCM diagnostic banner clutter on top

            // Rotating Rider Motivation Banner (shown with a new message every time an order is picked up!)
            if (riderPickupMotivationMessage != null) {
                item {
                    RiderMotivationNotificationBanner(
                        message = riderPickupMotivationMessage,
                        onDismiss = { riderPickupMotivationMessage = null },
                        onClick = {
                            riderPickupMotivationMessage = null
                            showEarningsDashboard = true
                        }
                    )
                }
            }

            // Porter ID Suspended Warning Card (balance <= -200)
            if (isSuspended) {
                item(key = "porter_id_suspended_alert") {
                    PorterIdSuspendedWarningCard(
                        walletBalance = currentBalance,
                        onClearDues = { showRechargeDialog = true }
                    )
                }
            }

            // Porter Auto-Offline Alert (balance <= -150)
            if (showAutoOfflineAlert || (isBelow150Limit && currentRider?.isOnline == false && !hasOngoingActiveOrder)) {
                item(key = "porter_auto_offline_alert") {
                    PorterAutoOfflineWarningCard(
                        walletBalance = currentBalance,
                        onRecharge = { showRechargeDialog = true },
                        onDismiss = { showAutoOfflineAlert = false }
                    )
                }
            }

            // Compact 1-Line Duty & Earnings Bar (All detailed wallet/payout/support options are in the ☰ Menu & Wallet drawer)
            item(key = "rider_section_1_duty_and_wallet") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = if (isSuspended) PorterRed.copy(alpha = 0.12f) else if (currentRider?.isOnline == true) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSuspended) PorterRed else if (currentRider?.isOnline == true) Color(0xFF10B981) else Color(0xFF94A3B8)
                                ),
                                modifier = Modifier
                                    .clickable { onToggleDutyClicked() }
                                    .testTag("rider_online_status_pill")
                            ) {
                                Text(
                                    text = if (isSuspended) "SUSPENDED" else if (currentRider?.isOnline == true) "🟢 ON DUTY" else "⚪ OFF DUTY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSuspended) PorterRed else if (currentRider?.isOnline == true) Color(0xFF047857) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Today: ₹%.0f • %d Trips",
                                        todaySameDayEarningInr,
                                        todayDeliveredOrders.size
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryHeadingColor
                                )
                                Text(
                                    text = if (currentRider?.isOnline == true) "Waiting for new customer orders" else "Tap ON/OFF switch above to go online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showEarningsDashboard = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("rider_open_earnings_dashboard_inline_btn")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Wallet",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    val syncOk = liveSyncStatus.isConnected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                isErrorBanner = false
                                statusMessage = "🔄 Syncing live orders from server..."
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("rider_live_sync_status_strip"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (syncOk) {
                                "🟢 Live Dispatch (${liveSyncStatus.lastLatencyMs}ms) • ${liveSyncStatus.syncedOrdersCount} Orders • Sync ${liveSyncStatus.lastSyncTimeLabel}"
                            } else {
                                "🟡 Connecting to server... Tap to Sync Now"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (syncOk) Color(0xFF047857) else Color(0xFFB45309),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Live Orders",
                            tint = if (syncOk) RiderTeal else Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (todayCodCollectedInr > 0.0) {
                        val codLimit = remoteFlags.maxCodLimitInr.coerceAtLeast(500).toDouble()
                        val codRatio = (todayCodCollectedInr / codLimit).toFloat().coerceIn(0f, 1f)
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "💵 COD Cash-in-Hand (Floating Limit)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryHeadingColor
                                )
                                Text(
                                    text = String.format(Locale.US, "₹%.0f / ₹%.0f", todayCodCollectedInr, codLimit),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (codRatio >= 0.85f) PorterRed else RiderTeal
                                )
                            }
                            LinearProgressIndicator(
                                progress = { codRatio },
                                color = if (codRatio >= 0.85f) PorterRed else RiderOrange,
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            item(key = "rider_remote_alerts_and_mission") {
                RiderRemoteAlertsAndMissionCard(
                    flags = remoteFlags,
                    todayDeliveredCount = todayDeliveredOrders.size,
                    riderName = riderDisplayName,
                    riderPhone = riderPhone,
                    isRiderOnline = currentRider?.isOnline == true,
                    activeZoneHints = listOfNotNull(preferredAreaKeyword.takeIf { it.isNotBlank() }),
                    isDarkMode = isDarkMode,
                    onClick = {
                        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                        scope.launch {
                            runCatching { riderMainListState.animateScrollToItem(4) }
                        }
                    }
                )
            }

            item(key = "rider_shift_and_smart_filter") {
                val searchingOrdersSnapshot = remember(orders) {
                    orders.filter { it.orderStatus == OrderStatus.SEARCHING_RIDER }
                }
                RiderShiftAndSmartFilterCard(
                    todayDeliveredCount = todayDeliveredOrders.size,
                    todayDistanceKm = todayDistanceKm,
                    todaySameDayEarningInr = todaySameDayEarningInr,
                    riderSharePercent = remoteFlags.riderSharePercent,
                    voiceAlertEnabled = voiceAlertEnabled,
                    onToggleVoiceAlert = { enabled ->
                        RiderSmartToolsPrefs.setVoiceAlertEnabled(context, enabled)
                    },
                    minPayoutFilterInr = minPayoutFilterInr,
                    onMinPayoutChange = { minInr ->
                        RiderSmartToolsPrefs.setMinPayoutFilter(context, minInr)
                    },
                    preferredAreaKeyword = preferredAreaKeyword,
                    onAreaKeywordChange = { kw ->
                        RiderSmartToolsPrefs.setPreferredAreaKeyword(context, kw)
                    },
                    hiddenByFilterCount = hiddenByFilterCount,
                    searchingOrders = searchingOrdersSnapshot,
                    flags = remoteFlags,
                    isDarkMode = isDarkMode
                )
            }



            if (statusMessage != null) {
                item {
                    Surface(
                        color = if (isErrorBanner) Color(0xFFFEE2E2) else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                statusMessage = null
                                if (activeHomeOrders.isNotEmpty()) {
                                    scope.launch { runCatching { riderMainListState.animateScrollToItem(4) } }
                                }
                            }
                    ) {
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isErrorBanner) Color(0xFFB91C1C) else Color(0xFF065F46),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Clean Rider Home Feed: Only real un-archived orders (Accept/Reject -> Pickup -> Drop -> Order Delivered)
            if (activeHomeOrders.isEmpty()) {
                item(key = "rider_section_2_empty_orders") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_no_booked_orders_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = RiderTeal,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Waiting for New Order",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryHeadingColor
                            )
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_same_day_total_earning_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Today's Total Earning (Same Day)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "₹%.0f", todaySameDayEarningInr),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "${todayDeliveredOrders.size} Trips Today • Past days in Wallet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (sameRouteBatchMatches.isNotEmpty() && ongoingTripForBatch != null) {
                    item(key = "rider_smart_same_route_batch_card") {
                        val batchOrder = sameRouteBatchMatches.first()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_smart_batch_match_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🔗 Smart Same-Route Batch Match (+₹${remoteFlags.sameRouteBatchBonusInr} Clubbing Bonus)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Along route with #${ongoingTripForBatch.orderId}: New Order #${batchOrder.orderId} (${batchOrder.riderEarning})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${batchOrder.pickupAddress.fullAddress.take(40)} → ${batchOrder.dropAddress.fullAddress.take(40)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Button(
                                    onClick = {
                                        val baseRider = currentRider ?: riders.firstOrNull()
                                        if (baseRider != null) {
                                            repository.acceptOrderByRider(batchOrder.orderId, baseRider)
                                            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                            isErrorBanner = false
                                            statusMessage = "🔗 Batch Order #${batchOrder.orderId} accepted (+₹${remoteFlags.sameRouteBatchBonusInr} clubbing bonus eligible)!"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("rider_accept_batch_btn")
                                ) {
                                    Text(
                                        text = "Accept Same-Route Batch (+₹${remoteFlags.sameRouteBatchBonusInr})",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "rider_section_2_orders_header") {
                    Text(
                        text = "Active Order (${activeHomeOrders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryHeadingColor
                    )
                }

                items(activeHomeOrders, key = { it.orderId }) { order ->
                    RiderProgressiveOrderCard(
                        order = order,
                        riderDisplayName = riderDisplayName,
                        tipAmountInr = tipsByOrder[order.orderId] ?: 0,
                        photoProofNote = orderPhotoProofMap[order.orderId],
                        photoProofBitmap = orderPhotoBitmapMap[order.orderId],
                        onAcceptOrder = {
                            com.example.security.OrderAlertSoundManager.markOrderHandledAndStopAlert(order.orderId)
                            val baseRider = currentRider ?: riders.firstOrNull()
                            if (baseRider != null) {
                                val activeRider = baseRider.copy(
                                    name = riderDisplayName.ifBlank { baseRider.name },
                                    mobile = riderPhone.ifBlank { baseRider.mobile },
                                    isOnline = true,
                                    shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT,
                                    accountStatus = "ACTIVE",
                                    walletBalanceInr = baseRider.walletBalanceInr.coerceAtLeast(0.0)
                                )
                                val updatedFleet = riders.map { if (it.riderId == activeRider.riderId) activeRider else it }
                                AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, updatedFleet)

                                val acceptRes = repository.acceptOrderByRider(order.orderId, activeRider)
                                if (acceptRes.isFailure) {
                                    runCatching {
                                        val field = repository.javaClass.getDeclaredField("_orders")
                                        field.isAccessible = true
                                        @Suppress("UNCHECKED_CAST")
                                        val flow = field.get(repository) as? kotlinx.coroutines.flow.MutableStateFlow<List<CustomerOrder>>
                                        if (flow != null) {
                                            flow.value = flow.value.map { existing ->
                                                if (existing.orderId == order.orderId) {
                                                    existing.copy(
                                                        orderStatus = OrderStatus.RIDER_ASSIGNED,
                                                        assignedRiderId = activeRider.riderId,
                                                        assignedRiderName = activeRider.name,
                                                        assignedRiderPhone = activeRider.mobile,
                                                        assignedRiderVehicle = "${activeRider.vehicleType.title} (${activeRider.vehicleNumber})",
                                                        latestRiderQuickNote = "Rider ${activeRider.name} accepted your order!"
                                                    )
                                                } else {
                                                    existing
                                                }
                                            }
                                        }
                                    }
                                }
                                isErrorBanner = false
                                statusMessage = "✅ Order #${order.orderId} Accepted! Follow Pickup Navigation first."
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)

                                if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
                                    scope.launch {
                                        val freshCoords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                                        if (freshCoords != null) {
                                            val gpsUpdatedRider = activeRider.copy(
                                                currentLat = freshCoords.first,
                                                currentLng = freshCoords.second
                                            )
                                            val nextFleet = repository.riders.value.map {
                                                if (it.riderId == gpsUpdatedRider.riderId) gpsUpdatedRider else it
                                            }
                                            AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, nextFleet)
                                            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                        }
                                    }
                                }
                            }
                        },
                        onRejectOrder = {
                            orderPendingRejection = order
                        },
                        onAutoOfferExpired = {
                            val rejectingRiderId = (currentRider?.riderId ?: order.assignedRiderId ?: "RIDER-LOCAL")
                                .ifBlank { "RIDER-LOCAL" }
                            com.example.security.OrderAlertSoundManager.markOrderHandledAndStopAlert(order.orderId)
                            val nextRejected = locallyRejectedOrderIds + order.orderId
                            locallyRejectedOrderIds = nextRejected
                            rejectedOrderPrefs.edit().putStringSet("rejected_ids", nextRejected).apply()
                            QuickDeliveryOperationsManager.clearWaitTimerForOrder(context, order.orderId)
                            AutomatedFairRiderAssignmentEngine.markRiderDeclinedAndReleaseOrder(
                                repository = repository,
                                orderId = order.orderId,
                                rejectingRiderId = rejectingRiderId,
                                reason = "30s Offer Countdown Expired"
                            )
                            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                            isErrorBanner = false
                            statusMessage = "⏱️ Offer #${order.orderId} (30s) timed out — forwarded to next online rider."
                        },
                        onCapturePhotoProof = {
                            activePhotoOrderId = order.orderId
                            if (DeliveryProofAndGpsManager.hasCameraPermission(context)) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onCompletePickupWithSignature = { enteredOtp, senderName, signatureSvg ->
                            com.example.security.OrderAlertSoundManager.stopOrderAlert()
                            val isPrepaid = order.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
                            val otpToVerify = if (isPrepaid) enteredOtp.filter(Char::isDigit).trim() else order.pickupOtp
                            if (isPrepaid && otpToVerify.length < 4) {
                                isErrorBanner = true
                                statusMessage = "Enter the 4-digit Pickup OTP from customer."
                                return@RiderProgressiveOrderCard
                            }
                            if (signatureSvg.isBlank()) {
                                isErrorBanner = true
                                statusMessage = "Customer Pickup Digital Signature is required."
                                return@RiderProgressiveOrderCard
                            }
                            repository.verifyPickupOtpByRider(
                                order.orderId,
                                otpToVerify,
                                order.assignedRiderId
                            ).onSuccess {
                                val ts = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                                OrderProofRegistry.savePickupProof(
                                    PickupProof(
                                        orderId = order.orderId,
                                        riderId = order.assignedRiderId ?: currentRider?.riderId.orEmpty(),
                                        riderName = order.assignedRiderName ?: riderDisplayName,
                                        packagePhotoUrl = orderPhotoProofMap[order.orderId] ?: "pickup_verified",
                                        packagePhotoSummary = orderPhotoProofMap[order.orderId] ?: "Pickup Verified & Customer e-Signed",
                                        senderName = senderName.ifBlank { order.customerName },
                                        senderSignatureSvg = signatureSvg,
                                        verifiedOtp = otpToVerify,
                                        latitude = order.pickupAddress.lat,
                                        longitude = order.pickupAddress.lng,
                                        capturedAt = ts
                                    )
                                )
                                isErrorBanner = false
                                riderPickupMotivationMessage = MotivationAndPromoManager.nextRiderPickupMotivation()
                                statusMessage = "✅ Parcel Picked Up & Signed! Drop Navigation is now UNLOCKED."
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                            }.onFailure { err ->
                                isErrorBanner = true
                                statusMessage = err.message
                            }
                        },
                        onCompleteDropWithSignature = { enteredOtp, receiverName, signatureSvg ->
                            com.example.security.OrderAlertSoundManager.stopOrderAlert()
                            val isPrepaid = order.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
                            val otpToVerify = if (isPrepaid) enteredOtp.filter(Char::isDigit).trim() else order.deliveryOtp
                            if (isPrepaid && otpToVerify.length < 4) {
                                isErrorBanner = true
                                statusMessage = "Enter the 4-digit Drop OTP from receiver."
                                return@RiderProgressiveOrderCard
                            }
                            if (signatureSvg.isBlank()) {
                                isErrorBanner = true
                                statusMessage = "Customer Drop Digital Signature is required."
                                return@RiderProgressiveOrderCard
                            }
                            MultiPhoneLiveSyncManager.ensureOrderSealedForRepository(repository, order.orderId)
                            repository.verifyDeliveryOtpByRider(
                                order.orderId,
                                otpToVerify,
                                order.assignedRiderId
                            ).onSuccess {
                                val ts = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                                val totalFareInr = order.totalAmount
                                    .replace("₹", "")
                                    .replace(",", "")
                                    .trim()
                                    .toDoubleOrNull() ?: 0.0
                                OrderProofRegistry.saveDeliveryProof(
                                    DeliveryProof(
                                        orderId = order.orderId,
                                        riderId = order.assignedRiderId ?: currentRider?.riderId.orEmpty(),
                                        riderName = order.assignedRiderName ?: riderDisplayName,
                                        deliveryPhotoUrl = orderPhotoProofMap[order.orderId] ?: "delivery_verified",
                                        deliveryPhotoSummary = orderPhotoProofMap[order.orderId] ?: "Drop Verified & Customer e-Signed",
                                        recipientName = receiverName.ifBlank { order.dropAddress.contactName.ifBlank { order.customerName } },
                                        recipientSignatureSvg = signatureSvg,
                                        verifiedOtp = otpToVerify,
                                        codAmountCollectedInr = if (isPrepaid) 0.0 else totalFareInr,
                                        latitude = order.dropAddress.lat,
                                        longitude = order.dropAddress.lng,
                                        capturedAt = ts
                                    )
                                )
                                isErrorBanner = false
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                MotivationAndPromoManager.notifySuccessfulDeliveryMotivationForBoth(
                                    order = order.copy(orderStatus = OrderStatus.DELIVERED),
                                    forceTrigger = true
                                )
                                val postBal = (currentRider?.walletBalanceInr ?: 0.0) + totalLiveTipsInr
                                if (postBal <= -200.0) {
                                    updateRiderProfileState(newOnline = false, newStatus = "SUSPENDED")
                                    showSuspendedDialog = true
                                    isErrorBanner = true
                                } else if (postBal <= -150.0) {
                                    updateRiderProfileState(newOnline = false, newStatus = "ACTIVE")
                                    showAutoOfflineAlert = true
                                    isErrorBanner = true
                                }
                                statusMessage = "✅ Drop Signature Saved! Tap 'Order Delivered' below to finish and move to History."
                            }.onFailure { err ->
                                isErrorBanner = true
                                statusMessage = err.message
                            }
                        },
                        onOrderDeliveredToHistory = { completedOrderId ->
                            val nextArchived = archivedCompletedOrderIds + completedOrderId
                            archivedCompletedOrderIds = nextArchived
                            archivedOrderPrefs.edit().putStringSet("archived_ids", nextArchived).apply()
                            orderPhotoProofMap.remove(completedOrderId)
                            orderPhotoBitmapMap.remove(completedOrderId)
                            orderLiveGpsMap.remove(completedOrderId)
                            riderPickupMotivationMessage = null
                            isErrorBanner = false
                            statusMessage = "✅ Order #$completedOrderId moved to Trip History. Ready for new orders!"
                            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                        },
                        onNavigationMessage = { msg ->
                            isErrorBanner = false
                            statusMessage = msg
                        },
                        onRequestAdminOtpSync = { stageLabel ->
                            runCatching {
                                val field = repository.javaClass.getDeclaredField("_orders")
                                field.isAccessible = true
                                @Suppress("UNCHECKED_CAST")
                                val flow = field.get(repository) as? kotlinx.coroutines.flow.MutableStateFlow<List<CustomerOrder>>
                                if (flow != null) {
                                    flow.value = flow.value.map { existing ->
                                        if (existing.orderId == order.orderId) {
                                            existing.copy(
                                                latestRiderQuickNote = "🔔 Rider requested Admin Sync-Auth $stageLabel OTP (#${order.orderId})"
                                            )
                                        } else {
                                            existing
                                        }
                                    }
                                }
                            }
                            QuickDeliveryOperationsManager.recordOrderTimelineEvent(
                                context = context,
                                orderId = order.orderId,
                                title = "Rider Requested Admin $stageLabel OTP Auth",
                                detail = "$riderDisplayName requested remote Admin OTP sync authentication (Zero SMS)",
                                actorRole = "RIDER"
                            )
                            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                            isErrorBanner = false
                            statusMessage = "🔔 Admin Sync-Auth requested for $stageLabel OTP! As soon as Admin authenticates, this screen advances automatically."
                        }
                    )
                }
            }

            // Silent 60 km/h Over-Speed GPS Alarm Monitor (Hidden unless rider exceeds 60 km/h)
            item(key = "rider_overspeed_safety_card") {
                RiderOverspeedSafetyCard()
            }
        }
    }
    }
    QuickDeliveryNextLevelLaunchOverlay(
        appTitle = "Partner Quick Delivery",
        appRoleBadge = "Superfast Rider Dispatch • ${remoteFlags.riderSharePercent}% Partner Share + 100% Tips",
        accentColor = RiderTeal,
        secondaryAccentColor = RiderOrange
    )
    }
}

@Composable
private fun RiderProgressiveOrderCard(
    order: CustomerOrder,
    riderDisplayName: String,
    tipAmountInr: Int,
    photoProofNote: String?,
    photoProofBitmap: Bitmap?,
    onAcceptOrder: () -> Unit,
    onRejectOrder: () -> Unit,
    onAutoOfferExpired: () -> Unit = {},
    onCapturePhotoProof: () -> Unit,
    onCompletePickupWithSignature: (enteredOtp: String, senderName: String, signatureSvg: String) -> Unit,
    onCompleteDropWithSignature: (enteredOtp: String, receiverName: String, signatureSvg: String) -> Unit,
    onOrderDeliveredToHistory: (String) -> Unit,
    onNavigationMessage: (String) -> Unit,
    onRequestAdminOtpSync: (String) -> Unit = {}
) {
    var pickupOtpInput by remember(order.orderId) { mutableStateOf("") }
    var deliveryOtpInput by remember(order.orderId) { mutableStateOf("") }
    var pickupSenderName by remember(order.orderId) { mutableStateOf(order.customerName.ifBlank { "Customer" }) }
    var dropReceiverName by remember(order.orderId) {
        mutableStateOf(order.dropAddress.contactName.ifBlank { order.customerName.ifBlank { "Receiver" } })
    }
    val pickupSignatureStrokes = remember(order.orderId) { mutableStateListOf<List<Offset>>() }
    val dropSignatureStrokes = remember(order.orderId) { mutableStateListOf<List<Offset>>() }
    var showChatSection by remember(order.orderId) { mutableStateOf(false) }

    val pickupProofsByOrder by OrderProofRegistry.pickupProofsByOrder.collectAsState()
    val deliveryProofsByOrder by OrderProofRegistry.deliveryProofsByOrder.collectAsState()
    val orderPickupProof = pickupProofsByOrder[order.orderId]
    val orderDeliveryProof = deliveryProofsByOrder[order.orderId]
    val isPrepaidOrder = order.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
    val cardContext = LocalContext.current
    val remoteFlags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
    val waitTimersByOrder by QuickDeliveryOperationsManager.waitTimerStartByOrder.collectAsState()
    val unreachableMap by QuickDeliveryOperationsManager.unreachableByOrder.collectAsState()
    val unreachableRec = unreachableMap[order.orderId]
    val waitStartedAt = waitTimersByOrder[order.orderId]
    var waitTickerSeconds by remember(order.orderId, waitStartedAt) { mutableStateOf(0) }
    LaunchedEffect(order.orderId, waitStartedAt, remoteFlags.waitTimeAndCancelRuleEnabled) {
        if (remoteFlags.waitTimeAndCancelRuleEnabled && waitStartedAt != null && waitStartedAt > 0L) {
            while (true) {
                waitTickerSeconds += 1
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    var riderHasConfirmedAccept by rememberSaveable(order.orderId) {
        mutableStateOf(
            order.orderStatus != OrderStatus.SEARCHING_RIDER &&
                order.orderStatus != OrderStatus.RIDER_ASSIGNED
        )
    }
    val awaitingAcceptDecision = order.orderStatus == OrderStatus.SEARCHING_RIDER ||
        (order.orderStatus == OrderStatus.RIDER_ASSIGNED && !riderHasConfirmedAccept)

    // 30-second Ringing Offer Countdown (Auto-forwards to next rider when timer hits 0)
    var offerSecondsLeft by remember(order.orderId, awaitingAcceptDecision) { mutableStateOf(30) }
    LaunchedEffect(order.orderId, awaitingAcceptDecision) {
        if (awaitingAcceptDecision) {
            while (offerSecondsLeft > 0) {
                kotlinx.coroutines.delay(1000L)
                offerSecondsLeft -= 1
            }
            onAutoOfferExpired()
        }
    }
    val earnDouble = remember(order.riderEarning) {
        order.riderEarning.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
    }
    val distKmSafe = order.distanceKm.coerceAtLeast(0.5)
    val perKmRate = earnDouble / distKmSafe

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (awaitingAcceptDecision) 2.dp else 1.2.dp,
            color = if (awaitingAcceptDecision) RiderOrange else RiderTeal.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rider_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. VERY TOP: ACCEPT / REJECT REQUEST WITH 30s COUNTDOWN + DISTANCE/₹-PER-KM
            if (awaitingAcceptDecision) {
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, RiderOrange),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                text = "🔔 New Order Request",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF9A3412)
                            )
                            Surface(
                                color = if (offerSecondsLeft <= 10) Color(0xFFDC2626) else RiderOrange,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "⏱️ ${offerSecondsLeft}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("rider_offer_countdown_${order.orderId}")
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (offerSecondsLeft / 30f).coerceIn(0f, 1f) },
                            color = if (offerSecondsLeft <= 10) Color(0xFFDC2626) else RiderOrange,
                            trackColor = Color(0xFFFED7AA),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                            ) {
                                Text(
                                    text = String.format(Locale.US, "📍 %.1f km • ₹%.0f/km", order.distanceKm, perKmRate),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Text(
                                    text = order.paymentMethod.shortBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    com.example.security.OrderAlertSoundManager.markOrderHandledAndStopAlert(order.orderId)
                                    onRejectOrder()
                                },
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rider_reject_order_btn_${order.orderId}")
                            ) {
                                Text("❌ Reject", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    com.example.security.OrderAlertSoundManager.markOrderHandledAndStopAlert(order.orderId)
                                    riderHasConfirmedAccept = true
                                    onAcceptOrder()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier
                                    .weight(1.8f)
                                    .testTag("rider_accept_order_btn_${order.orderId}")
                            ) {
                                Text("✅ Accept (${order.riderEarning})", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Order Summary Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.orderId} • ${order.vehicleType.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RiderNavy
                    )
                    Text(
                        text = if (tipAmountInr > 0) {
                            "Earning: ${order.riderEarning} + ₹$tipAmountInr Tip"
                        } else {
                            "Earning: ${order.riderEarning} • ${order.paymentMethod.shortBadge}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = RiderTeal
                    )
                }
                Surface(
                    color = RiderOrange.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.orderStatus.displayTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = RiderOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (tipAmountInr > 0) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TipGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = null,
                            tint = TipGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "💝 Customer Tip: +₹$tipAmountInr (100% Yours)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // Pre-Accept: Show brief Pickup & Drop route preview
            if (awaitingAcceptDecision) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📍 Pickup: ${order.pickupAddress.fullAddress.ifBlank { "Customer Pickup" }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = RiderNavy
                        )
                        Text(
                            text = "🏁 Drop: ${order.dropAddress.fullAddress.ifBlank { "Customer Drop" }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }
                }
                return@Column
            }

            // POST-ACCEPT FLOW (Simple & Step-by-Step):
            // 1. Both Customer Pickup & Drop Addresses + Sequential Google Maps Navigation Lock
            RiderOrderAddressAndNavigationBox(
                order = order,
                onOpenLiveMapNavigation = { targetType ->
                    val dest = if (targetType == RiderNavigationTargetType.PICKUP) {
                        order.pickupAddress.fullAddress
                    } else {
                        order.dropAddress.fullAddress
                    }
                    onNavigationMessage("🧭 Opening Google Maps → $dest")
                }
            )

            // Customer Delivery Instructions Banner (Swiggy/Zomato/Porter Style)
            val parsedInstructions = remember(order.packageDetails.description) {
                val desc = order.packageDetails.description
                if (desc.contains("[Instructions:")) {
                    desc.substringAfter("[Instructions:").substringBefore("]").trim()
                } else {
                    ""
                }
            }
            if (parsedInstructions.isNotBlank()) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📝 Customer Instructions: $parsedInstructions",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            if (order.packageDetails.description.contains("[Helper Needed]")) {
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEA580C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_helper_needed_badge_${order.orderId}")
                ) {
                    Text(
                        text = "💪 Customer Requested Loading / Unloading Helper (Add-on Paid)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF9A3412),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            // 2. Quick Call Customer + Toggle Chat Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.3f)) {
                    DirectDialPadCallButton(
                        orderId = order.orderId,
                        callerRole = "RIDER",
                        callerName = order.assignedRiderName ?: riderDisplayName,
                        receiverRole = "Customer",
                        receiverName = order.customerName,
                        phoneNumber = order.customerPhone,
                        buttonTestTag = "rider_call_customer_dial_btn_${order.orderId}"
                    )
                }
                OutlinedButton(
                    onClick = { showChatSection = !showChatSection },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (showChatSection) "Hide Chat" else "💬 Chat",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showChatSection) {
                com.example.ui.components.OrderRealTimeChatSection(
                    order = order,
                    viewerRole = "RIDER",
                    viewerName = order.assignedRiderName ?: riderDisplayName
                )
            }

            if ((order.orderStatus == OrderStatus.RIDER_ASSIGNED || order.orderStatus == OrderStatus.ARRIVED_AT_PICKUP) &&
                orderPickupProof == null
            ) {
                OutlinedButton(
                    onClick = { onRejectOrder() },
                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_release_order_with_reason_btn_${order.orderId}")
                ) {
                    Text(
                        text = "❌ Unable to Pickup? Reject / Release Order (Select Reason)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            // 3. STEP-BY-STEP PICKUP / DROP SIGNATURE & ORDER DELIVERED COMPLETION
            when (order.orderStatus) {
                OrderStatus.SEARCHING_RIDER -> Unit

                OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> {
                    val pickupSigSvg = remember(pickupSignatureStrokes.toList()) {
                        strokesToSignatureSvg(pickupSignatureStrokes.toList())
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "📦 Step 1: Parcel Pickup & Customer Signature",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = RiderNavy
                        )

                        // Upgrade #7: 1-Tap Reached Pickup Gate (100m Check-In) -> Pushes Customer sticky OTP alert
                        if (order.orderStatus == OrderStatus.RIDER_ASSIGNED) {
                            Button(
                                onClick = {
                                    QuickDeliveryOperationsManager.markRiderReachedPickupGate(
                                        context = cardContext,
                                        repository = SharedOrderRepository.getInstance(cardContext),
                                        orderId = order.orderId,
                                        riderName = order.assignedRiderName ?: riderDisplayName
                                    )
                                    onNavigationMessage("📍 Arrived at Pickup Gate! Customer notified to keep OTP & parcel ready.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RiderTeal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_reached_pickup_gate_btn_${order.orderId}")
                            ) {
                                Text(
                                    text = "📍 I've Reached Pickup Gate (Notify Customer OTP)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✓ At Pickup Gate — Customer OTP Alert Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Upgrade #8: Customer Unreachable / Log Call Attempt
                        if (unreachableRec == null) {
                            OutlinedButton(
                                onClick = {
                                    QuickDeliveryOperationsManager.reportCustomerUnreachable(
                                        context = cardContext,
                                        repository = SharedOrderRepository.getInstance(cardContext),
                                        orderId = order.orderId,
                                        riderId = order.assignedRiderId ?: "RIDER-LOCAL",
                                        riderName = order.assignedRiderName ?: riderDisplayName
                                    )
                                    onNavigationMessage("📵 Customer Unreachable logged & synced to Customer + Admin.")
                                },
                                border = BorderStroke(1.dp, Color(0xFFD97706)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB45309)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_customer_unreachable_btn_${order.orderId}")
                            ) {
                                Text(
                                    text = "📵 Customer Not Picking Up / Log Unreachable",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📵 Unreachable Call Attempt Logged at ${unreachableRec.reportedAtLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (remoteFlags.waitTimeAndCancelRuleEnabled) {
                            val waitInfo = remember(order.orderId, waitTimersByOrder, remoteFlags, waitTickerSeconds) {
                                QuickDeliveryOperationsManager.computeWaitFeeInfo(order.orderId, remoteFlags)
                            }
                            if (waitInfo == null) {
                                OutlinedButton(
                                    onClick = {
                                        QuickDeliveryOperationsManager.startWaitTimerAtPickup(
                                            context = cardContext,
                                            repository = SharedOrderRepository.getInstance(cardContext),
                                            orderId = order.orderId
                                        )
                                        onNavigationMessage("⏱️ Wait timer started (${remoteFlags.freeWaitMinutes}m free, then ₹${remoteFlags.waitFeePerMinuteInr}/min)")
                                    },
                                    border = BorderStroke(1.2.dp, RiderOrange),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rider_start_wait_timer_btn_${order.orderId}")
                                ) {
                                    Text(
                                        text = "⏱️ Reached Pickup Gate — Start Wait Timer (${remoteFlags.freeWaitMinutes}m free, ₹${remoteFlags.waitFeePerMinuteInr}/min)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RiderOrange
                                    )
                                }
                            } else {
                                val mm = waitInfo.elapsedSeconds / 60
                                val ss = waitInfo.elapsedSeconds % 60
                                val remainingFreeSec = (waitInfo.freeSeconds - waitInfo.elapsedSeconds).coerceAtLeast(0L)
                                val rMm = remainingFreeSec / 60
                                val rSs = remainingFreeSec % 60
                                val isOvertime = waitInfo.overtimeMinutes > 0
                                Surface(
                                    color = if (isOvertime) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (isOvertime) Color(0xFFDC2626) else RiderOrange),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rider_live_wait_timer_badge_${order.orderId}")
                                ) {
                                    Text(
                                        text = if (isOvertime) {
                                            String.format(
                                                Locale.US,
                                                "⏱️ Pickup Wait: %02d:%02d • Overtime %dm (+₹%d Wait Fee)",
                                                mm,
                                                ss,
                                                waitInfo.overtimeMinutes,
                                                waitInfo.waitFeeInr
                                            )
                                        } else {
                                            String.format(
                                                Locale.US,
                                                "⏱️ Pickup Wait: %02d:%02d • Free time left: %02d:%02d (%dm free)",
                                                mm,
                                                ss,
                                                rMm,
                                                rSs,
                                                remoteFlags.freeWaitMinutes
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isOvertime) Color(0xFFB91C1C) else Color(0xFF92400E),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        if (isPrepaidOrder) {
                            OutlinedTextField(
                                value = pickupOtpInput,
                                onValueChange = { if (it.length <= 4) pickupOtpInput = it.filter(Char::isDigit) },
                                label = { Text("Enter 4-Digit Pickup OTP *") },
                                placeholder = { Text("Customer App OTP or ask Admin") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_pickup_otp_input_${order.orderId}")
                            )
                            OutlinedButton(
                                onClick = { onRequestAdminOtpSync("Pickup") },
                                border = BorderStroke(1.dp, RiderOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_request_admin_pickup_otp_${order.orderId}")
                            ) {
                                Text(
                                    text = "🔄 Request Admin Sync-Auth Pickup OTP (Zero SMS)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RiderOrange
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_cod_no_pickup_otp_badge_${order.orderId}")
                            ) {
                                Text(
                                    text = "💵 Cash on Delivery (COD) — No Pickup OTP Required",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = pickupSenderName,
                            onValueChange = { pickupSenderName = it },
                            label = { Text("Customer / Sender Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        DigitalSignaturePadCanvas(
                            signerLabel = "Customer Pickup",
                            strokes = pickupSignatureStrokes,
                            onAddStroke = { stroke -> pickupSignatureStrokes.add(stroke) },
                            onClearSignature = { pickupSignatureStrokes.clear() },
                            padTestTag = "pickup_proof_signature_pad_${order.orderId}",
                            clearButtonTestTag = "pickup_proof_signature_clear_btn_${order.orderId}"
                        )

                        OutlinedButton(
                            onClick = onCapturePhotoProof,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_take_parcel_photo_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (photoProofNote != null) "📷 Retake Pickup Photo (GPS + Time)" else "📷 Take Pickup Parcel Photo (GPS + Time)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (photoProofBitmap != null) {
                            Image(
                                bitmap = photoProofBitmap.asImageBitmap(),
                                contentDescription = "Watermarked Pickup Parcel Photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0D1B2A))
                                    .testTag("rider_pickup_watermarked_photo_preview_${order.orderId}")
                            )
                        }
                        if (!photoProofNote.isNullOrBlank()) {
                            Text(
                                text = photoProofNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = RiderTeal
                            )
                        }

                        Button(
                            onClick = {
                                onCompletePickupWithSignature(pickupOtpInput, pickupSenderName, pickupSigSvg)
                            },
                            enabled = pickupSigSvg.isNotBlank() && pickupSenderName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = RiderTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rider_verify_pickup_btn_${order.orderId}")
                        ) {
                            Text(
                                text = "✅ Complete Pickup & Unlock Drop Map →",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> {
                    val dropSigSvg = remember(dropSignatureStrokes.toList()) {
                        strokesToSignatureSvg(dropSignatureStrokes.toList())
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (orderPickupProof != null) {
                            Surface(
                                color = Color(0xFFF0FDFA),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, RiderTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✍️ Pickup e-Signed by ${orderPickupProof.senderName} ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Text(
                            text = "🏁 Step 2: Drop Parcel & Customer Signature",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = RiderNavy
                        )

                        if (isPrepaidOrder) {
                            OutlinedTextField(
                                value = deliveryOtpInput,
                                onValueChange = { if (it.length <= 4) deliveryOtpInput = it.filter(Char::isDigit) },
                                label = { Text("Enter 4-Digit Drop OTP *") },
                                placeholder = { Text("Customer App OTP or ask Admin") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_delivery_otp_input_${order.orderId}")
                            )
                            OutlinedButton(
                                onClick = { onRequestAdminOtpSync("Drop") },
                                border = BorderStroke(1.dp, RiderOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_request_admin_drop_otp_${order.orderId}")
                            ) {
                                Text(
                                    text = "🔄 Request Admin Sync-Auth Drop OTP (Zero SMS)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RiderOrange
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_cod_no_delivery_otp_badge_${order.orderId}")
                            ) {
                                Text(
                                    text = "💵 Collect ${order.totalAmount} Cash at Drop (No OTP Required)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = dropReceiverName,
                            onValueChange = { dropReceiverName = it },
                            label = { Text("Receiver Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        DigitalSignaturePadCanvas(
                            signerLabel = "Customer Drop",
                            strokes = dropSignatureStrokes,
                            onAddStroke = { stroke -> dropSignatureStrokes.add(stroke) },
                            onClearSignature = { dropSignatureStrokes.clear() },
                            padTestTag = "delivery_proof_signature_pad_${order.orderId}",
                            clearButtonTestTag = "delivery_proof_signature_clear_btn_${order.orderId}"
                        )

                        OutlinedButton(
                            onClick = onCapturePhotoProof,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_take_drop_photo_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (photoProofNote != null) "📷 Retake Drop Photo (GPS + Time)" else "📷 Take Drop Photo (GPS + Time)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (photoProofBitmap != null) {
                            Image(
                                bitmap = photoProofBitmap.asImageBitmap(),
                                contentDescription = "Watermarked Drop Parcel Photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0D1B2A))
                                    .testTag("rider_drop_watermarked_photo_preview_${order.orderId}")
                            )
                        }
                        if (!photoProofNote.isNullOrBlank()) {
                            Text(
                                text = photoProofNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = RiderTeal
                            )
                        }

                        Button(
                            onClick = {
                                onCompleteDropWithSignature(deliveryOtpInput, dropReceiverName, dropSigSvg)
                            },
                            enabled = dropSigSvg.isNotBlank() && dropReceiverName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rider_verify_delivery_btn_${order.orderId}")
                        ) {
                            Text(
                                text = "✅ Save Drop Signature & Complete",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                OrderStatus.DELIVERED -> {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VerifiedGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (tipAmountInr > 0) {
                                        "Completed • Earned ${order.riderEarning} + ₹$tipAmountInr Tip"
                                    } else {
                                        "Completed • Earned ${order.riderEarning}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46)
                                )
                            }
                            if (orderPickupProof != null) {
                                Text(
                                    text = "✓ Pickup Signature: ${orderPickupProof.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF047857)
                                )
                            }
                            if (orderDeliveryProof != null) {
                                Text(
                                    text = "✓ Drop Signature: ${orderDeliveryProof.recipientName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onOrderDeliveredToHistory(order.orderId) },
                        colors = ButtonDefaults.buttonColors(containerColor = RiderTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("rider_order_delivered_archive_btn_${order.orderId}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Order Delivered",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                OrderStatus.CANCELLED -> Unit
            }
        }
    }
}

/**
 * Clean, modern modal dialog displaying full Rider Partner profile:
 * Personal details, Verified KYC status, Vehicle specs, Lifetime performance, and Shift status.
 */
@Composable
fun RiderProfileDetailsDialog(
    rider: RiderProfile,
    totalTipsInr: Int,
    onOpenKycScanner: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    remember(context) { RiderProfilePhotoManager.ensureLoaded(context) }
    val profilePhotoBitmap by RiderProfilePhotoManager.profilePhotoBitmap.collectAsState()
    val lastUpdatedNote by RiderProfilePhotoManager.lastUpdatedNote.collectAsState()
    var photoStatusMessage by remember { mutableStateOf<String?>(null) }

    val profileCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            RiderProfilePhotoManager.saveProfilePhoto(context, bitmap, "Camera")
            photoStatusMessage = "✅ Profile photo captured & uploaded via Camera!"
        }
    }

    val profileCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            profileCameraLauncher.launch(null)
        } else {
            photoStatusMessage = "⚠️ Camera permission is required to capture a profile photo."
        }
    }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
            if (decoded != null) {
                RiderProfilePhotoManager.saveProfilePhoto(context, decoded, "Photo Picker")
                photoStatusMessage = "✅ Profile photo uploaded from gallery!"
            }
        }
    }

    val launchProfileCamera: () -> Unit = {
        if (DeliveryProofAndGpsManager.hasCameraPermission(context)) {
            profileCameraLauncher.launch(null)
        } else {
            profileCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("rider_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row with Interactive Camera Avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { launchProfileCamera() }
                                .testTag("rider_profile_avatar_box")
                        ) {
                            Surface(
                                color = QuickDeliveryTeal,
                                shape = CircleShape,
                                border = BorderStroke(2.dp, QuickDeliveryOrange),
                                modifier = Modifier.fillMaxSize()
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
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                color = QuickDeliveryOrange,
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, Color.White),
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Capture Profile Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rider.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D1B2A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "4.92 ★ (Top Partner)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                            Text(
                                text = lastUpdatedNote ?: "Tap camera below to set profile photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                // Profile Photo Camera Capture & Upload Controls
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = launchProfileCamera,
                                colors = ButtonDefaults.buttonColors(containerColor = QuickDeliveryTeal),
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rider_capture_profile_photo_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Capture Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Capture Photo",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    profilePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, QuickDeliveryOrange),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rider_upload_profile_photo_btn")
                            ) {
                                Text(
                                    text = "Upload Photo",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = QuickDeliveryOrange,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (photoStatusMessage != null) {
                            Text(
                                text = photoStatusMessage.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF059669),
                                modifier = Modifier.testTag("rider_profile_photo_status_text")
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Verified KYC Strip with Direct Camera Scan Button
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "100% KYC Verified Partner",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Govt. ID, DL, PAN & RC active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        Button(
                            onClick = onOpenKycScanner,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("rider_open_kyc_scanner_btn")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Details Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileInfoRow(label = "Mobile", value = "+91 ${rider.mobile.removePrefix("+91").trim()}")
                    ProfileInfoRow(label = "Email", value = rider.email)
                    ProfileInfoRow(label = "Vehicle", value = "${rider.vehicleType.title} • ${rider.vehicleNumber}")
                    ProfileInfoRow(label = "Aadhaar Card", value = rider.aadhaarNumber.ifBlank { "Not Uploaded" })
                    ProfileInfoRow(label = "PAN Card", value = rider.panNumber.ifBlank { "Not Uploaded" })
                    ProfileInfoRow(label = "Total Deliveries", value = "${rider.totalDeliveries} Completed")
                    ProfileInfoRow(
                        label = "Wallet Balance",
                        value = String.format(Locale.US, "₹%.0f (+ ₹%d Tips)", rider.walletBalanceInr, totalTipsInr)
                    )
                    ProfileInfoRow(
                        label = "Duty Status",
                        value = if (rider.isOnline) "🟢 Online & Ready" else "⚪ Offline"
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Action
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_profile_logout_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Logout",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Logout", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0D1B2A)
        )
    }
}

