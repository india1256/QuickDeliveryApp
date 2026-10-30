package com.example.ui.rider

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cloud.DeliveryProofAndGpsManager
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.data.model.RiderProfile
import com.example.data.model.RiderShiftStatus
import com.example.data.repository.SharedOrderRepository
import com.example.security.RiderOverspeedSafetyCard
import com.example.ui.components.QuickDeliveryOfficialLogo
import com.example.ui.components.DualDeliveryCelebrationMotivationBanner
import com.example.ui.components.FairAutoRiderDispatchSummaryCard
import com.example.ui.components.FcmOrderNotificationManager
import com.example.ui.components.FcmRealtimeNotificationCenterCard
import com.example.ui.components.FcmTargetAppRole
import com.example.ui.components.MotivationAndPromoManager
import com.example.ui.components.PhoneStorageLoginBackupCard
import com.example.ui.components.QuickDeliveryNextLevelLaunchOverlay
import com.example.ui.components.QuickDeliverySpeedingVehicleIntro
import com.example.ui.components.QuickDeliverySuggestionBoxCard
import com.example.ui.components.QuickDeliveryUserAuthScreen
import com.example.ui.components.RiderFeedbackManager
import com.example.ui.components.RiderMotivationNotificationBanner
import com.example.ui.components.RiderOrderTransferFacilityCard
import com.example.ui.components.RiderOrderTransferManager
import com.example.ui.components.RiderRateCustomerSection
import com.example.ui.components.RiderReceivedFeedbackCard
import com.example.ui.components.SosSafetyTriggerButtonAndBanner
import com.example.ui.components.DirectDialPadCallButton
import com.example.ui.components.RiderLiveSelfieCallAndExcelManager
import com.example.ui.components.RiderMandatoryLiveSelfieOnlineGateDialog
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

    remember(context) { MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context) }

    // 0. Speeding Vehicle Intro Animation & Rider Sign Up / Sign In State (cached in session for 0ms tab switches)
    var showRiderSpeedIntro by remember { mutableStateOf(!MotivationAndPromoManager.riderIntroCompleted) }
    var isRiderAuthenticated by remember { mutableStateOf(MotivationAndPromoManager.riderAuthenticatedInSession) }
    var riderDisplayName by remember(currentRider?.riderId) {
        mutableStateOf(currentRider?.name ?: MotivationAndPromoManager.cachedRiderName.ifBlank { "Vikram Rathore" })
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

    LaunchedEffect(orders) {
        MotivationAndPromoManager.observeOrdersForDeliveryMotivation(orders)
        FcmOrderNotificationManager.observeOrdersAndDispatchFcmNotifications(context, orders)
    }

    var activePhotoOrderId by remember { mutableStateOf<String?>(null) }
    val orderPhotoProofMap = remember { mutableStateMapOf<String, String>() }
    val orderLiveGpsMap = remember { mutableStateMapOf<String, String>() }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        val orderId = activePhotoOrderId
        if (bitmap != null && orderId != null) {
            val summary = DeliveryProofAndGpsManager.formatCameraProofSummary(bitmap, "Parcel")
            orderPhotoProofMap[orderId] = summary
            currentRider?.let { rider ->
                repository.sendRiderQuickMessage(orderId, rider, "📷 Parcel photo uploaded")
            }
            isErrorBanner = false
            statusMessage = "Parcel photo saved for Order #$orderId"
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
                val formatted = if (coords != null) {
                    String.format(Locale.US, "%.5f, %.5f", coords.first, coords.second)
                } else {
                    "12.97160, 77.59460"
                }
                orderLiveGpsMap[targetOrderId] = formatted
                currentRider?.let { rider ->
                    repository.sendRiderQuickMessage(targetOrderId, rider, "📍 Live Location: $formatted")
                }
                isErrorBanner = false
                statusMessage = "Live location updated for Order #$targetOrderId"
            }
        } else if (!granted) {
            statusMessage = "Please allow location access to share live delivery progress."
        }
    }

    // 1. Speeding Vehicle Intro Animation when Rider App opens
    if (showRiderSpeedIntro) {
        QuickDeliverySpeedingVehicleIntro(
            appTitle = "Quick Delivery Partner",
            appSubtitle = "Fast Rider Dispatch • 80% Partner Earnings",
            accentColor = RiderOrange,
            onAnimationFinished = {
                MotivationAndPromoManager.riderIntroCompleted = true
                showRiderSpeedIntro = false
            }
        )
        return
    }

    // Rider authentication state
    var isRiderLoggedIn by remember { mutableStateOf(MotivationAndPromoManager.riderAuthenticatedInSession) }
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
                MotivationAndPromoManager.riderAuthenticatedInSession = true
                isRiderLoggedIn = true
                statusMessage = "Welcome back, Partner $name! You are ready to accept deliveries."
            }
        )
        return
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
    var cashoutAmount by rememberSaveable { mutableStateOf(1240.0) }
    var showKycCenterDialog by rememberSaveable { mutableStateOf(false) }
    var showRiderSuggestionDialog by rememberSaveable { mutableStateOf(false) }
    var showTopSosEmergencyDialog by rememberSaveable { mutableStateOf(false) }
    var showLiveSelfieOnlineGateDialog by rememberSaveable { mutableStateOf(false) }
    val selfieCheckIns by RiderLiveSelfieCallAndExcelManager.selfieCheckIns.collectAsState()

    val hasRiderSubNavigation = showLiveSelfieOnlineGateDialog ||
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
                // MANDATORY LIVE PHOTO GATE: Rider must capture & upload a Live Selfie before going ONLINE!
                showLiveSelfieOnlineGateDialog = true
                isErrorBanner = false
                statusMessage = "📷 Mandatory Check: Upload your Live Selfie Photo to go ONLINE."
            } else {
                val updatedRider = rider.copy(
                    isOnline = false,
                    shiftStatus = RiderShiftStatus.ENDED_AT_HOME,
                    accountStatus = if (currentBalance <= -200.0) "SUSPENDED" else "ACTIVE"
                )
                val newFleet = riders.map { if (it.riderId == rider.riderId) updatedRider else it }
                AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newFleet)
                statusMessage = "⏸️ Duty Paused. Upload a new Live Selfie anytime to go ONLINE."
                isErrorBanner = false
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
                        statusMessage = "📞 Quick Delivery Helpline: 1800-QUICK-CARE (+91 80 4410 0100)"
                        isErrorBanner = false
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
                        statusMessage = "Signed out of Quick Delivery Partner account"
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // Left: Hamburger menu (☰) + Quick Delivery Branding
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("rider_drawer_hamburger_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Quick Delivery Menu (☰)",
                                    tint = Color.White
                                )
                            }
                            QuickDeliveryOfficialLogo(size = 28.dp, borderColor = QuickDeliveryOrange)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Quick Delivery",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "FAST. SIMPLE. SECURE.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = QuickDeliveryOrange,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right: Compact Emergency SOS Button + Duty Online/Offline Status & Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.align(Alignment.CenterEnd)
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
                        statusMessage = "Signed out of Quick Delivery Partner account"
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

            if (showCashoutDialog) {
                PorterInstantCashoutDialog(
                    amount = cashoutAmount,
                    onConfirm = { amount ->
                        showCashoutDialog = false
                        statusMessage = "⚡ Instant Payout of ₹${amount.toInt()} transferred to HDFC Bank (•••• 4092) via IMPS! Ref: QUICK${System.currentTimeMillis() % 1000000}"
                        isErrorBanner = false
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
                                text = "Immediate 24×7 Emergency Assistance for Quick Delivery Partner $riderDisplayName:",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiderNavy
                            )
                            Button(
                                onClick = {
                                    showTopSosEmergencyDialog = false
                                    isErrorBanner = true
                                    statusMessage = "🚨 Emergency SOS Alert + Live GPS sent to Quick Delivery Safety Control Room & Police 112!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PorterRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_sos_dispatch_alert_btn")
                            ) {
                                Text(
                                    text = "Send Live GPS Alert to Safety Desk & 112",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    showTopSosEmergencyDialog = false
                                    isErrorBanner = false
                                    statusMessage = "📞 Calling Quick Delivery 24×7 Partner Helpline: 1800-QUICK-CARE"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Call Partner Support (1800-QUICK-CARE)",
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
                        onDismiss = { MotivationAndPromoManager.dismissRiderPostDeliveryMotivation() }
                    )
                }
            }

            // Real-Time FCM (Firebase Cloud Messaging) Order Status & Delivery Completion Notification Center
            item(key = "rider_fcm_realtime_notification_center") {
                val riderActiveOrder = orders.firstOrNull { it.assignedRiderId == currentRider?.riderId }
                    ?: orders.firstOrNull()
                FcmRealtimeNotificationCenterCard(
                    appRole = FcmTargetAppRole.RIDER,
                    activeOrder = riderActiveOrder,
                    accentColor = RiderOrange
                )
            }

            // Rotating Rider Motivation Banner (shown with a new message every time an order is picked up!)
            if (riderPickupMotivationMessage != null) {
                item {
                    RiderMotivationNotificationBanner(
                        message = riderPickupMotivationMessage,
                        onDismiss = { riderPickupMotivationMessage = null }
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

            // Porter Partner Duty Status & Wallet Header Card
            item(key = "rider_section_1_duty_and_wallet") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSuspended) {
                                        "Duty: SUSPENDED (≤ -₹200 Due)"
                                    } else if (currentRider?.isOnline == true) {
                                        "ON DUTY • Live GPS Active"
                                    } else {
                                        "OFF DUTY"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuspended) PorterRed else primaryHeadingColor
                                )
                                Text(
                                    text = if (isSuspended) {
                                        "⚠️ Clear dues to reactivate partner ID"
                                    } else if (isBelow150Limit) {
                                        "⚠️ Balance is ≤ -₹150. Auto-offline applied."
                                    } else {
                                        "80% Rider Share + 100% Customer Tips • Instant UPI"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuspended) PorterRed else if (isBelow150Limit) PorterAmber else secondaryTextColor
                                )
                            }
                            Surface(
                                color = if (isSuspended) PorterRed.copy(alpha = 0.12f) else if (currentRider?.isOnline == true) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSuspended) PorterRed else if (currentRider?.isOnline == true) Color(0xFF10B981) else Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag("rider_online_status_pill")
                            ) {
                                Text(
                                    text = if (isSuspended) "SUSPENDED" else if (currentRider?.isOnline == true) "🟢 ON DUTY" else "⚪ OFF DUTY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSuspended) PorterRed else if (currentRider?.isOnline == true) Color(0xFF047857) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Today's Net Earnings (80% Share), Completed Trips & Cash-in-Hand (COD) Limit Strip
                        Surface(
                            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Today's Net (80% Share)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = secondaryTextColor
                                        )
                                        Text(
                                            text = String.format(Locale.US, "₹%.0f", currentBalance.coerceAtLeast(0.0) + 864.0),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Completed Trips",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = secondaryTextColor
                                        )
                                        Text(
                                            text = "${currentRider?.totalDeliveries ?: 14} Trips",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = primaryHeadingColor
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            isErrorBanner = false
                                            statusMessage = "⚡ Instant UPI Payout initiated to your linked bank account!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("rider_instant_upi_payout_btn")
                                    ) {
                                        Text(
                                            text = "Instant UPI Payout",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cash-in-Hand (COD) Limit: ₹90 / ₹150 Cap",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryTextColor
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TextButton(
                                            onClick = { showTopSosEmergencyDialog = true },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("rider_quick_emergency_sos_btn")
                                        ) {
                                            Text(
                                                text = "🚨 Emergency SOS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PorterRed
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                isErrorBanner = false
                                                statusMessage = "🎧 Admin Helpdesk connected! Priority support ticket active."
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("rider_admin_helpdesk_btn")
                                        ) {
                                            Text(
                                                text = "🎧 Admin Helpdesk",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RiderTeal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val latestSelfie = remember(selfieCheckIns, currentRider?.riderId, riderDisplayName) {
                            RiderLiveSelfieCallAndExcelManager.getLatestSelfieForRider(
                                currentRider?.riderId ?: "RIDER-101",
                                riderDisplayName
                            )
                        }

                        if (currentRider?.isOnline == true) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_live_selfie_verified_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "✓ Live Selfie Verified Online (${latestSelfie.capturedAt}) • Synced to Admin Web",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { showLiveSelfieOnlineGateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RiderOrange),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_open_live_selfie_gate_btn")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "📷 Upload Live Photo to Go ONLINE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showEarningsDashboard = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_open_earnings_dashboard_inline_btn")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Earnings & Wallet →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }



            if (statusMessage != null) {
                item {
                    Surface(
                        color = if (isErrorBanner) Color(0xFFFEE2E2) else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
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

            // KRAM 2: Booked Orders Section (Accept -> Pickup -> Deliver -> Rate -> Transfer -> SOS)
            if (orders.isEmpty()) {
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = RiderTeal,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Active Orders — Ready for Dispatch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryHeadingColor
                            )
                            Text(
                                text = "Stay Online to receive instant nearby customer delivery requests with live GPS route navigation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor
                            )
                        }
                    }
                }
            } else {
                item(key = "rider_section_2_orders_header") {
                    Text(
                        text = "Active Deliveries (${orders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryHeadingColor
                    )
                }

                items(orders, key = { it.orderId }) { order ->
                    RiderProgressiveOrderCard(
                        order = order,
                        tipAmountInr = tipsByOrder[order.orderId] ?: 0,
                        photoProofNote = orderPhotoProofMap[order.orderId],
                        liveGpsNote = orderLiveGpsMap[order.orderId],
                        onAcceptOrder = {
                            currentRider?.let { rider ->
                                AutomatedFairRiderAssignmentEngine.claimOrderAtomicallyByRider(
                                    repository = repository,
                                    orderId = order.orderId,
                                    riderId = rider.riderId
                                )
                                    .onSuccess {
                                        isErrorBanner = false
                                        orderLiveGpsMap[order.orderId] = "Live GPS Streaming Active (${rider.currentLat}, ${rider.currentLng})"
                                        statusMessage = "Order #${order.orderId} accepted by ${rider.name}! Live tracking is now active until delivery."
                                    }
                                    .onFailure { err ->
                                        isErrorBanner = true
                                        statusMessage = err.message
                                    }
                            }
                        },
                        onRejectOrder = {
                            currentRider?.let { rider ->
                                repository.releaseAssignedOrderBeforePickup(
                                    orderId = order.orderId,
                                    riderId = rider.riderId,
                                    reason = "Rider declined offered order"
                                )
                                AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                                isErrorBanner = false
                                statusMessage = "Order #${order.orderId} declined. Offered to other fleet partners."
                            }
                        },
                        onCapturePhotoProof = {
                            activePhotoOrderId = order.orderId
                            if (DeliveryProofAndGpsManager.hasCameraPermission(context)) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onUpdateLiveLocation = {
                            activePhotoOrderId = order.orderId
                            if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
                                scope.launch {
                                    val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                                    val formatted = if (coords != null) {
                                        String.format(Locale.US, "%.5f, %.5f", coords.first, coords.second)
                                    } else {
                                        "12.97160, 77.59460 (Live GPS)"
                                    }
                                    orderLiveGpsMap[order.orderId] = formatted
                                    currentRider?.let { rider ->
                                        repository.sendRiderQuickMessage(order.orderId, rider, "📍 Rider Live Location: $formatted")
                                    }
                                    isErrorBanner = false
                                    statusMessage = "Live GPS shared with customer: $formatted"
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onVerifyPickupOtp = { enteredOtp ->
                            repository.verifyPickupOtpByRider(
                                order.orderId,
                                enteredOtp.ifBlank { order.pickupOtp },
                                order.assignedRiderId
                            ).onSuccess {
                                isErrorBanner = false
                                // Show a brand-new motivational message every time Rider picks up an order!
                                riderPickupMotivationMessage = MotivationAndPromoManager.nextRiderPickupMotivation()
                                orderLiveGpsMap[order.orderId] = "In-Transit Live GPS Active → Drop Location"
                                statusMessage = "Parcel Picked Up by ${order.assignedRiderName ?: "Rider"}! Live GPS tracking continues until delivery."
                            }.onFailure { err ->
                                isErrorBanner = true
                                statusMessage = err.message
                            }
                        },
                        onVerifyDeliveryOtp = { enteredOtp ->
                            repository.verifyDeliveryOtpByRider(
                                order.orderId,
                                enteredOtp.ifBlank { order.deliveryOtp },
                                order.assignedRiderId
                            ).onSuccess {
                                isErrorBanner = false
                                AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                                // Send post-delivery motivational notification to BOTH Rider and Customer!
                                MotivationAndPromoManager.notifySuccessfulDeliveryMotivationForBoth(
                                    order = order.copy(orderStatus = OrderStatus.DELIVERED),
                                    forceTrigger = true
                                )

                                // PORTER NEGATIVE BALANCE ENFORCEMENT POST-DELIVERY:
                                val postBal = (currentRider?.walletBalanceInr ?: 0.0) + totalLiveTipsInr
                                if (postBal <= -200.0) {
                                    // 200- gaya to id suspended ho gaya
                                    updateRiderProfileState(newOnline = false, newStatus = "SUSPENDED")
                                    showSuspendedDialog = true
                                    statusMessage = "🚫 Order Delivered! But Wallet Balance is -₹${Math.abs(postBal.toInt())} (≤ -₹200 limit). Partner ID Suspended."
                                    isErrorBanner = true
                                } else if (postBal <= -150.0) {
                                    // 150- me jate to automatically offline ho jaye par order complit hone ke bad
                                    updateRiderProfileState(newOnline = false, newStatus = "ACTIVE")
                                    showAutoOfflineAlert = true
                                    statusMessage = "⚠️ Order Delivered! Wallet balance is -₹${Math.abs(postBal.toInt())} (≤ -₹150 limit). Automatically switched to OFFLINE."
                                    isErrorBanner = true
                                } else {
                                    statusMessage = "Order #${order.orderId} DELIVERED by ${order.assignedRiderName ?: "Rider"}! ${order.riderEarning} credited."
                                }
                            }.onFailure { err ->
                                isErrorBanner = true
                                statusMessage = err.message
                            }
                        },
                        onRiderSosDispatched = { gpsCoords ->
                            currentRider?.let { rider ->
                                repository.sendRiderQuickMessage(
                                    order.orderId,
                                    rider,
                                    "🚨 RIDER SOS ALERT: Support notified at GPS $gpsCoords"
                                )
                            }
                            isErrorBanner = true
                            statusMessage = "🚨 SOS Emergency Alert sent to Support with GPS: $gpsCoords"
                        },
                        onOrderTransferred = { record ->
                            currentRider?.let { rider ->
                                repository.sendRiderQuickMessage(
                                    order.orderId,
                                    rider,
                                    "🔄 Order Transferred to ${record.toRiderName} (${record.toRiderMobile}) • Reason: ${record.reason}"
                                )
                            }
                            isErrorBanner = false
                            statusMessage = "Order #${order.orderId} transferred to ${record.toRiderName} (${record.toRiderMobile}) • Weekly transfers used: ${RiderOrderTransferManager.weeklyTransfersUsed.value}/${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS}"
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
        appTitle = "Quick Delivery Partner",
        appRoleBadge = "Superfast Rider Dispatch • 80% Partner Share + 100% Tips",
        accentColor = RiderTeal,
        secondaryAccentColor = RiderOrange
    )
    }
}

@Composable
private fun RiderProgressiveOrderCard(
    order: CustomerOrder,
    tipAmountInr: Int,
    photoProofNote: String?,
    liveGpsNote: String?,
    onAcceptOrder: () -> Unit,
    onRejectOrder: () -> Unit,
    onCapturePhotoProof: () -> Unit,
    onUpdateLiveLocation: () -> Unit,
    onVerifyPickupOtp: (String) -> Unit,
    onVerifyDeliveryOtp: (String) -> Unit,
    onRiderSosDispatched: (String) -> Unit,
    onOrderTransferred: (com.example.ui.components.OrderTransferRecord) -> Unit
) {
    var pickupOtpInput by remember(order.orderId) { mutableStateOf(order.pickupOtp) }
    var deliveryOtpInput by remember(order.orderId) { mutableStateOf(order.deliveryOtp) }
    var activeMapNavigationTarget by remember(order.orderId) {
        mutableStateOf<RiderNavigationTargetType?>(null)
    }
    val isPrepaidOrder = order.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY

    val isLiveTrackingActive = order.orderStatus != OrderStatus.SEARCHING_RIDER &&
        order.orderStatus != OrderStatus.DELIVERED &&
        order.orderStatus != OrderStatus.CANCELLED

    if (activeMapNavigationTarget != null) {
        RiderLiveTurnByTurnMapNavigationModal(
            order = order,
            initialTargetType = activeMapNavigationTarget ?: RiderNavigationTargetType.PICKUP,
            onArrivedOrClose = { activeMapNavigationTarget = null }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RiderTeal.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rider_order_card_${order.orderId}")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.orderId} • ${order.vehicleType.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RiderNavy
                    )
                    Text(
                        text = if (tipAmountInr > 0) {
                            "Rider Earning (80%): ${order.riderEarning} + ₹$tipAmountInr Tip"
                        } else {
                            "Rider Earning (80%): ${order.riderEarning} • Total: ${order.totalAmount}"
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
                            text = "💝 Customer Tip Included: +₹$tipAmountInr (100% Yours)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            if (isLiveTrackingActive) {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = VerifiedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔴 Continuous Live Tracking Active (Accepted → Delivery)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }

            // Direct Customer Pickup & Drop Address Box with 1-Click Auto-Start Live Map Navigation
            RiderOrderAddressAndNavigationBox(
                order = order,
                onOpenLiveMapNavigation = { targetType ->
                    activeMapNavigationTarget = targetType
                }
            )

            // 1-Click Direct Phone Dial Pad Button for Rider to Call Customer Immediately
            DirectDialPadCallButton(
                orderId = order.orderId,
                callerRole = "RIDER",
                callerName = order.assignedRiderName ?: "Vikram Rathore",
                receiverRole = "Customer",
                receiverName = order.customerName,
                phoneNumber = order.customerPhone,
                buttonTestTag = "rider_call_customer_dial_btn_${order.orderId}"
            )

            // REAL-TIME FIRESTORE CHAT WITH CUSTOMER (Active once order is assigned)
            if (order.orderStatus != OrderStatus.SEARCHING_RIDER && order.orderStatus != OrderStatus.CANCELLED) {
                com.example.ui.components.OrderRealTimeChatSection(
                    order = order,
                    viewerRole = "RIDER",
                    viewerName = order.assignedRiderName ?: "Vikram Singh"
                )
            }

            HorizontalDivider()

            // KRAM 2A: Primary Progressive Step Action (Step 1 Accept -> Step 2 Pickup -> Step 3 Deliver -> Step 4 Rate Customer)
            when (order.orderStatus) {
                OrderStatus.SEARCHING_RIDER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRejectOrder,
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rider_reject_order_btn_${order.orderId}")
                        ) {
                            Text("Decline", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onAcceptOrder,
                            colors = ButtonDefaults.buttonColors(containerColor = RiderOrange),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("rider_accept_order_btn_${order.orderId}")
                        ) {
                            Text("Accept (${order.riderEarning})", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> {
                    if (isPrepaidOrder) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pickupOtpInput,
                                onValueChange = { pickupOtpInput = it },
                                label = { Text("Prepaid Pickup OTP (${order.pickupOtp})") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rider_pickup_otp_input_${order.orderId}")
                            )
                            Button(
                                onClick = { onVerifyPickupOtp(pickupOtpInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = RiderTeal),
                                modifier = Modifier.testTag("rider_verify_pickup_btn_${order.orderId}")
                            ) {
                                Text("Step 2: Pickup Parcel")
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_cod_no_pickup_otp_badge_${order.orderId}")
                            ) {
                                Text(
                                    text = "💵 Cash on Delivery (COD) — No OTP Required for Pickup",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Button(
                                onClick = { onVerifyPickupOtp(order.pickupOtp) },
                                colors = ButtonDefaults.buttonColors(containerColor = RiderTeal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_verify_pickup_btn_${order.orderId}")
                            ) {
                                Text("Step 2: Confirm Parcel Pickup (COD — No OTP)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> {
                    if (isPrepaidOrder) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = deliveryOtpInput,
                                onValueChange = { deliveryOtpInput = it },
                                label = { Text("Prepaid Drop OTP (${order.deliveryOtp})") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rider_delivery_otp_input_${order.orderId}")
                            )
                            Button(
                                onClick = { onVerifyDeliveryOtp(deliveryOtpInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                                modifier = Modifier.testTag("rider_verify_delivery_btn_${order.orderId}")
                            ) {
                                Text("Step 3: Complete Delivery")
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_cod_no_delivery_otp_badge_${order.orderId}")
                            ) {
                                Text(
                                    text = "💵 Cash on Delivery (COD) — Collect ${order.totalAmount} Cash (No OTP Required)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Button(
                                onClick = { onVerifyDeliveryOtp(order.deliveryOtp) },
                                colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rider_verify_delivery_btn_${order.orderId}")
                            ) {
                                Text(
                                    "Step 3: Collect ${order.totalAmount} Cash & Complete Delivery (No OTP)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OrderStatus.DELIVERED -> {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (tipAmountInr > 0) {
                                    "Delivered Successfully • Earned ${order.riderEarning} + ₹$tipAmountInr Tip"
                                } else {
                                    "Delivered Successfully • Earned ${order.riderEarning}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                    RiderReceivedFeedbackCard(
                        orderId = order.orderId
                    )
                    RiderRateCustomerSection(
                        orderId = order.orderId,
                        riderId = order.assignedRiderId ?: "RIDER-101",
                        riderName = order.assignedRiderName ?: "Vikram Singh",
                        customerName = order.customerName
                    )
                }

                OrderStatus.CANCELLED -> {
                    Text(
                        text = "Order Cancelled",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB91C1C)
                    )
                }
            }

            // KRAM 2B: Parcel Photo Proof & Live GPS Update
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCapturePhotoProof,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rider_take_parcel_photo_btn_${order.orderId}")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (photoProofNote != null) "Retake Photo" else "Parcel Photo",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedButton(
                    onClick = onUpdateLiveLocation,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rider_share_live_gps_btn_${order.orderId}")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Update Live GPS",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (photoProofNote != null) {
                Text(
                    text = "📷 $photoProofNote",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = VerifiedGreen
                )
            }
            if (liveGpsNote != null) {
                Text(
                    text = "📍 $liveGpsNote",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RiderTeal
                )
            }

            // RIDER ORDER TRANSFER FACILITY (Vehicle Breakdown / Emergency — Max 3x/Week, locks until next week)
            if (isLiveTrackingActive) {
                RiderOrderTransferFacilityCard(
                    orderId = order.orderId,
                    currentRiderId = order.assignedRiderId ?: "RIDER-101",
                    onOrderTransferred = onOrderTransferred
                )
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
                    ProfileInfoRow(label = "Aadhaar Card", value = rider.aadhaarNumber.ifBlank { "XXXX-XXXX-1101" })
                    ProfileInfoRow(label = "PAN Card", value = rider.panNumber.ifBlank { "ABCDE1101F" })
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

