package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.DisputeStatus
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.cloud.RiderApprovalWorkflowStatus
import com.example.data.cloud.RiderRegistrationApprovalManager
import com.example.data.cloud.WithdrawalStatus
import com.example.data.delivery.AdminVehicleAndWeightPolicy
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.DeliveryProof
import com.example.data.model.OrderProofRegistry
import com.example.data.model.OrderStatus
import com.example.data.model.PickupProof
import com.example.data.model.PricingConfig
import com.example.data.model.RiderProfile
import com.example.data.model.RiderShiftStatus
import com.example.data.model.RiderVerificationStatus
import com.example.data.model.VehicleType
import com.example.data.model.WalletTransaction
import com.example.data.repository.SharedOrderRepository
import com.example.security.RiderBatteryPowerMonitor
import com.example.security.RiderBatteryPowerState
import com.example.security.RiderOverspeedAlarmManager
import com.example.ui.rider.AdminKycPhotoZoomPreviewDialog
import com.example.security.RiderOverspeedState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Quick Delivery Emerald, Mint & Slate Enterprise Command Center Palette
private val SidebarDarkTeal = Color(0xFF042F2E)
private val SidebarDeepTeal = Color(0xFF022C22)
private val SidebarActiveTeal = Color(0xFF059669)
private val SidebarMintText = Color(0xFFA7F3D0)
private val DashBgLight = Color(0xFFF1F5F9)
private val DashNavy = Color(0xFF0F172A)
private val DashTeal = Color(0xFF0D9488)
private val DashEmerald = Color(0xFF10B981)
private val DashOrange = Color(0xFFEA580C)
private val DashGold = Color(0xFFD97706)
private val DashCrimson = Color(0xFFDC2626)

enum class AdminDedicatedPage(
    val pageIndex: Int,
    val title: String,
    val shortLabel: String,
    val icon: ImageVector,
    val navTestTag: String,
    val legacyTabTag: String
) {
    DASHBOARD(0, "Dashboard", "Overview", Icons.Default.Dashboard, "admin_nav_dashboard", "admin_tab_overview"),
    ORDERS(1, "Orders", "Live Orders", Icons.Default.LocalShipping, "admin_nav_orders", "admin_tab_live_orders"),
    LIVE_TRACKING(2, "Live Tracking", "Live Tracking", Icons.Default.GpsFixed, "admin_nav_live_tracking", "admin_tab_live_tracking"),
    RIDERS(3, "Riders", "Riders", Icons.AutoMirrored.Filled.DirectionsBike, "admin_nav_riders", "admin_tab_riders"),
    CUSTOMERS(4, "Customers", "Customers", Icons.Default.People, "admin_nav_customers", "admin_tab_customers"),
    KYC(5, "KYC Verification", "KYC Verification", Icons.Default.VerifiedUser, "admin_nav_kyc", "admin_tab_kyc_verify"),
    PAYMENTS_SETTLEMENTS(6, "Payments & Settlements", "Payments & Settlements", Icons.Default.Payments, "admin_nav_payments", "admin_tab_settlements"),
    COMMISSION_EARNINGS(7, "20% Commission / 80% Earnings", "20% / 80% Split", Icons.Default.PieChart, "admin_nav_commission_earnings", "admin_tab_commission_earnings"),
    COMPLAINTS_SOS(8, "Complaints & SOS", "Complaints & SOS", Icons.Default.Warning, "admin_nav_complaints_sos", "admin_tab_complaints"),
    REPORTS(9, "Reports & Excel (.xlsx)", "Excel Sheet", Icons.Default.BarChart, "admin_nav_reports", "admin_tab_excel_sheet"),
    NOTIFICATIONS(10, "Notifications (FCM)", "Notifications", Icons.Default.NotificationsActive, "admin_nav_notifications", "admin_tab_notifications"),
    SETTINGS(11, "Settings & Pricing", "Settings", Icons.Default.Settings, "admin_nav_settings", "admin_tab_settings")
}

private fun parseOrderFareDouble(amountText: String): Double {
    return amountText.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
}

private fun maskPhoneForAdmin(phone: String, hasPiiAccess: Boolean): String {
    if (hasPiiAccess) return if (phone.startsWith("+")) phone else "+91 ${phone.removePrefix("+91").trim()}"
    val digits = phone.filter { it.isDigit() }
    val last4 = digits.takeLast(4).ifBlank { "4321" }
    return "+91 XXXXX-$last4"
}

private fun maskDocForAdmin(docValue: String, hasPiiAccess: Boolean): String {
    if (hasPiiAccess) return docValue
    val clean = docValue.trim()
    if (clean.length <= 4) return "XXXX"
    return "XXXX-XXXX-${clean.takeLast(4)}"
}

/**
 * Upgraded Professional Quick Delivery Admin Web Dashboard Suite with:
 * - Collapsible Dark Teal Sidebar (12 dedicated pages)
 * - Top Global Search, Date-Range Selector, City Filter, and Admin RBAC Permission / PII Masking Gate
 * - Live KPI Cards, Revenue & Order Charts, Live Orders/Riders Map, Recent Orders, and Activity Feed
 * - Searchable, Filterable, Paginated Tables with genuine OpenXML (.xlsx) Export on every dataset
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDeliveryEnterpriseAdminSuite(
    repository: SharedOrderRepository,
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    pricingConfig: PricingConfig,
    vehiclePolicy: AdminVehicleAndWeightPolicy,
    walletTransactions: List<WalletTransaction>,
    tipsByOrder: Map<String, Int>,
    sosAlerts: List<SosSupportAlert>,
    suggestions: List<AppSuggestionEntry>,
    riderRatings: Map<String, RiderDeliveryFeedback>,
    customerRatings: Map<String, CustomerRatingByRider>,
    overspeedState: RiderOverspeedState,
    batteryState: RiderBatteryPowerState,
    weeklyTransfersUsed: Int,
    isAutoPilotEnabled: Boolean,
    onToggleAutoPilot: (Boolean) -> Unit,
    lastAutoActionLog: String,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit,
    onResetOrders: () -> Unit,
    onExitToCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AdminExecutiveThemeManager.ensureLoaded(context)
    val isDarkMode by AdminExecutiveThemeManager.isDarkMode.collectAsState()
    val palette = remember(isDarkMode) { adminExecPalette(isDarkMode) }
    val fcmNotifications by FcmOrderNotificationManager.fcmMessages.collectAsState()
    val pendingNotificationTap by FcmOrderNotificationManager.pendingNotificationTap.collectAsState()
    val liveSyncStatus by MultiPhoneLiveSyncManager.syncStatus.collectAsState()
    val selfieCheckIns by RiderLiveSelfieCallAndExcelManager.selfieCheckIns.collectAsState()
    val callLogs by RiderLiveSelfieCallAndExcelManager.callLogs.collectAsState()

    // Navigation & Sidebar State (Responsive: hidden by default on mobile < 768.dp, open on desktop >= 768.dp)
    var currentPage by rememberSaveable { mutableStateOf(AdminDedicatedPage.DASHBOARD) }
    val adminPageHistoryStack = remember { mutableStateListOf<AdminDedicatedPage>() }
    var previousPageRef by remember { mutableStateOf(currentPage) }

    LaunchedEffect(currentPage) {
        if (currentPage != previousPageRef) {
            if (adminPageHistoryStack.lastOrNull() == currentPage) {
                adminPageHistoryStack.removeAt(adminPageHistoryStack.lastIndex)
            } else {
                adminPageHistoryStack.add(previousPageRef)
            }
            previousPageRef = currentPage
        }
    }

    // Global Search, Date Range, City Filter, More Sheet, and Loading State
    var globalSearchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var showMoreModulesSheet by rememberSaveable { mutableStateOf(false) }
    var selectedDateRange by rememberSaveable { mutableStateOf("All Time") }
    var customStartDate by rememberSaveable { mutableStateOf("2026-10-01") }
    var customEndDate by rememberSaveable { mutableStateOf("2026-10-26") }
    var showDateFilterBar by rememberSaveable { mutableStateOf(false) }
    var selectedCityFilter by rememberSaveable { mutableStateOf("All Cities") }
    var isSyncLoading by remember { mutableStateOf(false) }

    LaunchedEffect(pendingNotificationTap?.eventSeq) {
        val tap = pendingNotificationTap ?: return@LaunchedEffect
        val oid = tap.orderId.trim()
        val isSecurityOrSos = tap.orderStatus.equals("SOS", ignoreCase = true) ||
            tap.title.contains("SOS", ignoreCase = true) ||
            tap.title.contains("Security", ignoreCase = true) ||
            tap.title.contains("Attack", ignoreCase = true)
        when {
            isSecurityOrSos -> {
                currentPage = AdminDedicatedPage.COMPLAINTS_SOS
            }
            oid.isNotBlank() && !oid.contains("BROADCAST", ignoreCase = true) -> {
                globalSearchQuery = oid
                showSearchBar = true
                currentPage = AdminDedicatedPage.ORDERS
            }
            else -> {
                currentPage = AdminDedicatedPage.NOTIFICATIONS
            }
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        FcmOrderNotificationManager.consumeNotificationTap(tap.eventSeq)
    }

    // Admin Permission & Sensitive Data (PII) Protection State
    var adminSessionToken by remember { mutableStateOf(repository.issueAdminPreviewSessionToken()) }
    var hasSensitivePiiPermission by remember { mutableStateOf(true) }
    var adminPermissionBanner by remember { mutableStateOf<String?>(null) }

    // Filter orders & riders by Dynamic City/Area Filter, Date Range, and Global Search
    val cityOptions = remember(orders) {
        val dynamicAreas = orders.mapNotNull { order ->
            val parts = order.pickupAddress.fullAddress.split(",")
                .map { it.trim() }
                .filter { it.length in 3..24 && !it.all(Char::isDigit) }
            parts.lastOrNull() ?: parts.firstOrNull()
        }.distinct().take(6)
        listOf("All Cities") + dynamicAreas
    }
    val dateRangeOptions = remember {
        listOf("Today", "Last 7 Days", "Last 30 Days", "Custom Range", "All Time")
    }

    val filteredOrdersByGlobal = remember(orders, selectedCityFilter, selectedDateRange, globalSearchQuery) {
        orders.filter { order ->
            val matchesCity = selectedCityFilter == "All Cities" ||
                order.pickupAddress.fullAddress.contains(selectedCityFilter, ignoreCase = true) ||
                order.dropAddress.fullAddress.contains(selectedCityFilter, ignoreCase = true)

            val q = globalSearchQuery.trim()
            val matchesGlobalSearch = q.isEmpty() ||
                order.orderId.contains(q, ignoreCase = true) ||
                order.customerName.contains(q, ignoreCase = true) ||
                order.customerPhone.contains(q, ignoreCase = true) ||
                (order.assignedRiderName?.contains(q, ignoreCase = true) == true) ||
                order.pickupAddress.fullAddress.contains(q, ignoreCase = true) ||
                order.dropAddress.fullAddress.contains(q, ignoreCase = true)

            matchesCity && matchesGlobalSearch
        }
    }

    val filteredRidersByGlobal = remember(riders, globalSearchQuery) {
        val q = globalSearchQuery.trim()
        if (q.isEmpty()) {
            riders
        } else {
            riders.filter { r ->
                r.name.contains(q, ignoreCase = true) ||
                    r.riderId.contains(q, ignoreCase = true) ||
                    r.mobile.contains(q, ignoreCase = true) ||
                    r.vehicleNumber.contains(q, ignoreCase = true) ||
                    r.vehicleType.title.contains(q, ignoreCase = true)
            }
        }
    }

    // Live Backend Financial Aggregations (never fake hardcoded stats!)
    val nonCancelledOrders = remember(filteredOrdersByGlobal) {
        filteredOrdersByGlobal.filter { it.orderStatus != OrderStatus.CANCELLED }
    }
    val totalGmvInr = remember(nonCancelledOrders) {
        nonCancelledOrders.sumOf { parseOrderFareDouble(it.totalAmount) }
    }
    val companyCommission20Inr = remember(nonCancelledOrders) {
        nonCancelledOrders.sumOf { o ->
            val explicit = parseOrderFareDouble(o.platformCommission)
            if (explicit > 0.0) explicit else parseOrderFareDouble(o.totalAmount) * 0.20
        }
    }
    val riderEarnings80Inr = remember(nonCancelledOrders) {
        nonCancelledOrders.sumOf { o ->
            val explicit = parseOrderFareDouble(o.riderEarning)
            if (explicit > 0.0) explicit else parseOrderFareDouble(o.totalAmount) * 0.80
        }
    }
    val totalTipsInr = remember(filteredOrdersByGlobal, tipsByOrder) {
        filteredOrdersByGlobal.sumOf { tipsByOrder[it.orderId] ?: 0 }
    }

    val pulseTransition = rememberInfiniteTransition(label = "adminSystemOnlinePulse")
    val onlinePulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onlinePulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        val isDesktop = maxWidth >= 768.dp
        var isSidebarExpanded by remember(isDesktop) { mutableStateOf(isDesktop) }

        val hasAdminBackHistory = (!isDesktop && isSidebarExpanded) ||
            showMoreModulesSheet ||
            showSearchBar ||
            showDateFilterBar ||
            adminPageHistoryStack.isNotEmpty() ||
            currentPage != AdminDedicatedPage.DASHBOARD

        BackHandler(enabled = hasAdminBackHistory) {
            when {
                showMoreModulesSheet -> showMoreModulesSheet = false
                !isDesktop && isSidebarExpanded -> isSidebarExpanded = false
                showSearchBar -> showSearchBar = false
                showDateFilterBar -> showDateFilterBar = false
                adminPageHistoryStack.isNotEmpty() -> {
                    val prev = adminPageHistoryStack.removeAt(adminPageHistoryStack.lastIndex)
                    previousPageRef = prev
                    currentPage = prev
                }
                currentPage != AdminDedicatedPage.DASHBOARD -> {
                    previousPageRef = AdminDedicatedPage.DASHBOARD
                    currentPage = AdminDedicatedPage.DASHBOARD
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
            ) {
                // Emergency SOS Live Banner at top whenever an unresolved Rider SOS exists
                AdminSosSupportLiveBanner(
                    onAlertClick = { alert ->
                        if (alert.orderId.isNotBlank()) {
                            globalSearchQuery = alert.orderId
                        }
                        currentPage = AdminDedicatedPage.COMPLAINTS_SOS
                    }
                )

                // 1. SLEEK EXECUTIVE TOP APP BAR
                Surface(
                    color = palette.topBarBg,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_compact_logo_header")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { isSidebarExpanded = !isSidebarExpanded },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("admin_sidebar_toggle_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Navigation Drawer",
                                        tint = Color.White
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { currentPage = AdminDedicatedPage.DASHBOARD }
                                ) {
                                    QuickDeliveryCuteBunnyLogo(
                                        size = 32.dp,
                                        borderColor = Color(0xFF34D399)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (currentPage == AdminDedicatedPage.DASHBOARD) "Executive Overview" else currentPage.shortLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                color = if (liveSyncStatus.isConnected) DashEmerald.copy(alpha = 0.2f) else DashGold.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier.clickable {
                                                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                (if (liveSyncStatus.isConnected) Color(0xFF34D399) else Color(0xFFFBBF24))
                                                                    .copy(alpha = onlinePulseAlpha)
                                                            )
                                                    )
                                                    Text(
                                                        text = if (liveSyncStatus.isConnected) "Live" else "Syncing",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (liveSyncStatus.isConnected) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "$selectedCityFilter • $selectedDateRange",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Clean Action Icon Cluster
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { showSearchBar = !showSearchBar },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Orders, Riders & Customers",
                                        tint = if (showSearchBar || globalSearchQuery.isNotEmpty()) Color(0xFF34D399) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showDateFilterBar = !showDateFilterBar },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("admin_date_city_filter_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Filter by Date & City",
                                        tint = if (showDateFilterBar) Color(0xFF34D399) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { AdminExecutiveThemeManager.toggleDarkMode(context) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("admin_theme_mode_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Dark or Light Mode",
                                        tint = if (isDarkMode) Color(0xFFFCD34D) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (!hasSensitivePiiPermission) {
                                            adminSessionToken = repository.issueAdminPreviewSessionToken()
                                            hasSensitivePiiPermission = true
                                            adminPermissionBanner = "Admin Session Verified: Full PII & Document Unmasking Enabled"
                                        } else {
                                            hasSensitivePiiPermission = false
                                            adminPermissionBanner = "Sensitive PII Masked (Phones, Aadhaar, PAN, Bank Protected)"
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("admin_permission_pii_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (hasSensitivePiiPermission) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = "RBAC Sensitive Data Masking",
                                        tint = if (hasSensitivePiiPermission) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSpawnNewLiveOrder() },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("admin_header_autopilot_badge")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync Live Data",
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Expandable Global Search Bar
                        AnimatedVisibility(visible = showSearchBar || globalSearchQuery.isNotEmpty()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.09f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 8.dp)
                                    .height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Global Search",
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = globalSearchQuery,
                                        onValueChange = { globalSearchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (globalSearchQuery.isEmpty()) {
                                                    Text(
                                                        text = "Search Order ID, Rider, Customer, Hub...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        maxLines = 1
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_global_search_input")
                                    )
                                    if (globalSearchQuery.isNotEmpty() || showSearchBar) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Search",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    globalSearchQuery = ""
                                                    showSearchBar = false
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Expandable Date-Range & City Filter Bar
                        AnimatedVisibility(visible = showDateFilterBar) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Date Range:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                    dateRangeOptions.forEach { rangeLabel ->
                                        val selected = selectedDateRange == rangeLabel
                                        FilterChip(
                                            selected = selected,
                                            onClick = { selectedDateRange = rangeLabel },
                                            label = { Text(rangeLabel, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SidebarActiveTeal,
                                                selectedLabelColor = Color.White,
                                                labelColor = Color.White.copy(alpha = 0.85f)
                                            ),
                                            modifier = Modifier.testTag("admin_date_range_chip_${rangeLabel.replace(" ", "_")}")
                                        )
                                    }
                                }

                                if (selectedDateRange == "Custom Range") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customStartDate,
                                            onValueChange = { customStartDate = it },
                                            label = { Text("Start Date (YYYY-MM-DD)", color = SidebarMintText) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = customEndDate,
                                            onValueChange = { customEndDate = it },
                                            label = { Text("End Date (YYYY-MM-DD)", color = SidebarMintText) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "City / Hub:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                    cityOptions.forEach { city ->
                                        val selected = selectedCityFilter == city
                                        FilterChip(
                                            selected = selected,
                                            onClick = { selectedCityFilter = city },
                                            label = { Text(city, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SidebarActiveTeal,
                                                selectedLabelColor = Color.White,
                                                labelColor = Color.White.copy(alpha = 0.85f)
                                            ),
                                            modifier = Modifier.testTag("admin_city_filter_chip_${city.replace(" ", "_")}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Optional Admin RBAC Permission Notice
                adminPermissionBanner?.let { msg ->
                    Surface(
                        color = if (hasSensitivePiiPermission) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (hasSensitivePiiPermission) DashEmerald else DashCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasSensitivePiiPermission) Color(0xFF065F46) else Color(0xFF991B1B),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { adminPermissionBanner = null }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // 2. MAIN BODY: Desktop Sidebar (>= 768.dp) + Full-Width Active Workspace
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // On Desktop (>= 768.dp), keep the left sidebar cleanly visible inside the main Row
                    if (isDesktop) {
                        AnimatedVisibility(visible = isSidebarExpanded) {
                            AdminEmeraldNavigationMenuContent(
                                currentPage = currentPage,
                                onSelectPage = { page -> currentPage = page },
                                onCloseMobileDrawer = null,
                                filteredOrdersCount = filteredOrdersByGlobal.size,
                                activeTrackingCount = filteredOrdersByGlobal.count {
                                    it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
                                },
                                filteredRidersCount = filteredRidersByGlobal.size,
                                verifiedKycBadge = "${riders.count { it.verificationStatus == RiderVerificationStatus.VERIFIED }}/${riders.size}",
                                unresolvedSosCount = sosAlerts.count { !it.isResolved },
                                fcmCount = fcmNotifications.size,
                                isSyncLoading = isSyncLoading,
                                onSyncBackend = {
                                    scope.launch {
                                        isSyncLoading = true
                                        AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                                        RiderBatteryPowerMonitor.refreshFromSystem(context)
                                        delay(350L)
                                        isSyncLoading = false
                                    }
                                },
                                modifier = Modifier
                                    .width(230.dp)
                                    .fillMaxHeight()
                                    .testTag("admin_dark_teal_sidebar")
                            )
                        }
                    }

                    // Active Workspace Container (100% full width on mobile!)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        val activeDateLabel = if (selectedDateRange == "Custom Range") {
                            "$customStartDate..$customEndDate"
                        } else {
                            selectedDateRange
                        }

                        when (currentPage) {
                            AdminDedicatedPage.DASHBOARD -> EnterpriseDashboardOverviewPage(
                                orders = filteredOrdersByGlobal,
                                riders = filteredRidersByGlobal,
                                totalGmvInr = totalGmvInr,
                                companyCommission20Inr = companyCommission20Inr,
                                riderEarnings80Inr = riderEarnings80Inr,
                                totalTipsInr = totalTipsInr,
                                tipsByOrder = tipsByOrder,
                                sosAlerts = sosAlerts,
                                fcmNotifications = fcmNotifications,
                                selfieCheckIns = selfieCheckIns,
                                callLogs = callLogs,
                                walletTransactions = walletTransactions,
                                overspeedState = overspeedState,
                                batteryState = batteryState,
                                isAutoPilotEnabled = isAutoPilotEnabled,
                                onToggleAutoPilot = onToggleAutoPilot,
                                lastAutoActionLog = lastAutoActionLog,
                                onExecuteAutoStepNow = onExecuteAutoStepNow,
                                onSpawnNewLiveOrder = onSpawnNewLiveOrder,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading,
                                isDesktop = isDesktop,
                                palette = palette,
                                repository = repository,
                                onNavigateToPage = { currentPage = it }
                            )

                            AdminDedicatedPage.ORDERS -> EnterpriseOrdersPage(
                                orders = filteredOrdersByGlobal,
                                riders = riders,
                                tipsByOrder = tipsByOrder,
                                repository = repository,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading,
                                palette = palette,
                                onExecuteAutoStepNow = onExecuteAutoStepNow,
                                onSpawnNewLiveOrder = onSpawnNewLiveOrder
                            )

                            AdminDedicatedPage.LIVE_TRACKING -> EnterpriseLiveTrackingPage(
                                orders = filteredOrdersByGlobal,
                                riders = filteredRidersByGlobal,
                                overspeedState = overspeedState,
                                batteryState = batteryState,
                                isAutoPilotEnabled = isAutoPilotEnabled,
                                onToggleAutoPilot = onToggleAutoPilot,
                                lastAutoActionLog = lastAutoActionLog,
                                onExecuteAutoStepNow = onExecuteAutoStepNow,
                                onSpawnNewLiveOrder = onSpawnNewLiveOrder
                            )

                            AdminDedicatedPage.RIDERS -> EnterpriseRidersPage(
                                orders = filteredOrdersByGlobal,
                                riders = filteredRidersByGlobal,
                                tipsByOrder = tipsByOrder,
                                selfieCheckIns = selfieCheckIns,
                                overspeedState = overspeedState,
                                batteryState = batteryState,
                                weeklyTransfersUsed = weeklyTransfersUsed,
                                repository = repository,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading,
                                palette = palette
                            )

                            AdminDedicatedPage.CUSTOMERS -> EnterpriseCustomersPage(
                                orders = filteredOrdersByGlobal,
                                tipsByOrder = tipsByOrder,
                                riderRatings = riderRatings,
                                suggestions = suggestions,
                                totalGmvInr = totalGmvInr,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading
                            )

                            AdminDedicatedPage.KYC -> EnterpriseKycPage(
                                riders = filteredRidersByGlobal,
                                selfieCheckIns = selfieCheckIns,
                                repository = repository,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading
                            )

                            AdminDedicatedPage.PAYMENTS_SETTLEMENTS -> EnterprisePaymentsSettlementsPage(
                                orders = filteredOrdersByGlobal,
                                riders = filteredRidersByGlobal,
                                walletTransactions = walletTransactions,
                                totalGmvInr = totalGmvInr,
                                companyCommission20Inr = companyCommission20Inr,
                                riderEarnings80Inr = riderEarnings80Inr,
                                totalTipsInr = totalTipsInr,
                                repository = repository,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading
                            )

                            AdminDedicatedPage.COMMISSION_EARNINGS -> EnterpriseCommissionAndRiderEarningsPage(
                                orders = filteredOrdersByGlobal,
                                riders = filteredRidersByGlobal,
                                tipsByOrder = tipsByOrder,
                                totalGmvInr = totalGmvInr,
                                companyCommission20Inr = companyCommission20Inr,
                                riderEarnings80Inr = riderEarnings80Inr,
                                totalTipsInr = totalTipsInr,
                                repository = repository,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading
                            )

                            AdminDedicatedPage.COMPLAINTS_SOS -> EnterpriseComplaintsAndSosPage(
                                orders = filteredOrdersByGlobal,
                                sosAlerts = sosAlerts,
                                suggestions = suggestions,
                                riderRatings = riderRatings,
                                customerRatings = customerRatings,
                                overspeedState = overspeedState,
                                batteryState = batteryState
                            )

                            AdminDedicatedPage.REPORTS -> EnterpriseReportsAndWorkbookPage(
                                orders = filteredOrdersByGlobal,
                                riders = filteredRidersByGlobal,
                                walletTransactions = walletTransactions,
                                tipsByOrder = tipsByOrder,
                                totalGmvInr = totalGmvInr,
                                companyCommission20Inr = companyCommission20Inr,
                                riderEarnings80Inr = riderEarnings80Inr,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter
                            )

                            AdminDedicatedPage.NOTIFICATIONS -> EnterpriseNotificationsPage(
                                orders = filteredOrdersByGlobal,
                                fcmNotifications = fcmNotifications,
                                dateRangeLabel = activeDateLabel,
                                cityFilterLabel = selectedCityFilter,
                                isLoading = isSyncLoading,
                                onOrderSelected = { orderId ->
                                    globalSearchQuery = orderId
                                    showSearchBar = true
                                    currentPage = AdminDedicatedPage.ORDERS
                                }
                            )

                            AdminDedicatedPage.SETTINGS -> EnterpriseSettingsPage(
                                repository = repository,
                                pricingConfig = pricingConfig,
                                vehiclePolicy = vehiclePolicy,
                                hasSensitivePiiPermission = hasSensitivePiiPermission,
                                onToggleSensitivePii = { hasSensitivePiiPermission = it },
                                adminSessionToken = adminSessionToken,
                                onRefreshAdminToken = {
                                    adminSessionToken = repository.issueAdminPreviewSessionToken()
                                },
                                onResetOrders = onResetOrders,
                                onExitToCustomer = onExitToCustomer
                            )
                        }
                    }
                }

                // 3. MATERIAL 3 5-TAB BOTTOM NAVIGATION BAR (Mobile < 768.dp)
                if (!isDesktop) {
                    val primaryTabs = remember {
                        listOf(
                            Triple(AdminDedicatedPage.DASHBOARD, "Overview", "admin_tab_overview"),
                            Triple(AdminDedicatedPage.ORDERS, "Orders", "admin_tab_live_orders"),
                            Triple(AdminDedicatedPage.RIDERS, "Riders", "admin_tab_riders"),
                            Triple(AdminDedicatedPage.PAYMENTS_SETTLEMENTS, "Finance", "admin_tab_settlements")
                        )
                    }
                    val isMoreTabActive = primaryTabs.none { it.first == currentPage }
                    HorizontalDivider(color = palette.hairlineBorder)
                    NavigationBar(
                        containerColor = palette.surface,
                        tonalElevation = 0.dp
                    ) {
                        primaryTabs.forEach { (page, label, tag) ->
                            val selected = currentPage == page
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentPage = page },
                                icon = {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = palette.accentTeal,
                                    selectedTextColor = palette.accentTeal,
                                    indicatorColor = palette.accentTeal.copy(alpha = 0.14f),
                                    unselectedIconColor = palette.textSecondary,
                                    unselectedTextColor = palette.textSecondary
                                ),
                                modifier = Modifier.testTag(tag)
                            )
                        }
                        NavigationBarItem(
                            selected = isMoreTabActive || showMoreModulesSheet,
                            onClick = { showMoreModulesSheet = true },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = "More Admin Modules"
                                )
                            },
                            label = {
                                Text(
                                    text = "More",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isMoreTabActive) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accentTeal,
                                selectedTextColor = palette.accentTeal,
                                indicatorColor = palette.accentTeal.copy(alpha = 0.14f),
                                unselectedIconColor = palette.textSecondary,
                                unselectedTextColor = palette.textSecondary
                            ),
                            modifier = Modifier.testTag("admin_tab_more_modules")
                        )
                    }
                }
            }

            // More Modules Modal Bottom Sheet
            if (showMoreModulesSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val secondaryModules = remember {
                    listOf(
                        AdminDedicatedPage.LIVE_TRACKING,
                        AdminDedicatedPage.CUSTOMERS,
                        AdminDedicatedPage.KYC,
                        AdminDedicatedPage.COMMISSION_EARNINGS,
                        AdminDedicatedPage.COMPLAINTS_SOS,
                        AdminDedicatedPage.REPORTS,
                        AdminDedicatedPage.NOTIFICATIONS,
                        AdminDedicatedPage.SETTINGS
                    )
                }
                ModalBottomSheet(
                    onDismissRequest = { showMoreModulesSheet = false },
                    sheetState = sheetState,
                    containerColor = palette.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Admin Command Modules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Jump directly to any operations, audit, or configuration module",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        secondaryModules.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { modPage ->
                                    val isCurrent = currentPage == modPage
                                    Surface(
                                        color = if (isCurrent) palette.accentTeal.copy(alpha = 0.10f) else palette.surfaceSubtle,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCurrent) palette.accentTeal else palette.hairlineBorder
                                        ),
                                        onClick = {
                                            currentPage = modPage
                                            showMoreModulesSheet = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag(modPage.legacyTabTag)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AdminIconBadge(
                                                icon = modPage.icon,
                                                tint = if (isCurrent) palette.accentTeal else palette.accentIndigo,
                                                sizeDp = 34
                                            )
                                            Text(
                                                text = modPage.shortLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        HorizontalDivider(color = palette.hairlineBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { AdminExecutiveThemeManager.toggleDarkMode(context) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDarkMode) "Light Theme" else "Dark Theme",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    hasSensitivePiiPermission = !hasSensitivePiiPermission
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasSensitivePiiPermission) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasSensitivePiiPermission) "Mask PII" else "Unmask PII",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 4. MOBILE SLIDE-OVER OVERLAY DRAWER (< 768.dp) WITH DARK BACKDROP & CLOSE BUTTON
            if (!isDesktop && isSidebarExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .clickable { isSidebarExpanded = false }
                ) {
                    AdminEmeraldNavigationMenuContent(
                        currentPage = currentPage,
                        onSelectPage = { page ->
                            currentPage = page
                            isSidebarExpanded = false
                        },
                        onCloseMobileDrawer = { isSidebarExpanded = false },
                        filteredOrdersCount = filteredOrdersByGlobal.size,
                        activeTrackingCount = filteredOrdersByGlobal.count {
                            it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
                        },
                        filteredRidersCount = filteredRidersByGlobal.size,
                        verifiedKycBadge = "${riders.count { it.verificationStatus == RiderVerificationStatus.VERIFIED }}/${riders.size}",
                        unresolvedSosCount = sosAlerts.count { !it.isResolved },
                        fcmCount = fcmNotifications.size,
                        isSyncLoading = isSyncLoading,
                        onSyncBackend = {
                            scope.launch {
                                isSyncLoading = true
                                AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                                RiderBatteryPowerMonitor.refreshFromSystem(context)
                                delay(350L)
                                isSyncLoading = false
                            }
                        },
                        modifier = Modifier
                            .width(276.dp)
                            .fillMaxHeight()
                            .clickable(enabled = false) {}
                            .testTag("admin_dark_teal_sidebar")
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminEmeraldNavigationMenuContent(
    currentPage: AdminDedicatedPage,
    onSelectPage: (AdminDedicatedPage) -> Unit,
    onCloseMobileDrawer: (() -> Unit)?,
    filteredOrdersCount: Int,
    activeTrackingCount: Int,
    filteredRidersCount: Int,
    verifiedKycBadge: String,
    unresolvedSosCount: Int,
    fcmCount: Int,
    isSyncLoading: Boolean,
    onSyncBackend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SidebarDarkTeal,
        shadowElevation = 12.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NAVIGATION MENU",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SidebarMintText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Quick Delivery SaaS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onCloseMobileDrawer != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SidebarMintText.copy(alpha = 0.4f)),
                        onClick = onCloseMobileDrawer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sidebar Drawer",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(2.dp))

            AdminDedicatedPage.entries.forEach { page ->
                val isSelected = currentPage == page
                val badgeText = when (page) {
                    AdminDedicatedPage.ORDERS -> "$filteredOrdersCount"
                    AdminDedicatedPage.LIVE_TRACKING -> "$activeTrackingCount"
                    AdminDedicatedPage.RIDERS -> "$filteredRidersCount"
                    AdminDedicatedPage.KYC -> verifiedKycBadge
                    AdminDedicatedPage.COMPLAINTS_SOS -> "$unresolvedSosCount"
                    AdminDedicatedPage.NOTIFICATIONS -> "$fcmCount"
                    else -> null
                }

                Surface(
                    color = if (isSelected) SidebarActiveTeal else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    onClick = { onSelectPage(page) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(page.navTestTag)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = page.title,
                                tint = if (isSelected) Color.White else SidebarMintText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = page.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.90f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (badgeText != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isSelected) Color.White.copy(alpha = 0.22f) else SidebarDeepTeal,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else SidebarMintText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onSyncBackend,
                border = BorderStroke(1.dp, SidebarMintText.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_sidebar_sync_backend_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Live Data",
                    tint = SidebarMintText,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSyncLoading) "Syncing Live Cloud..." else "Sync Live DB",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/* ============================================================================
   PAGE 0: DASHBOARD OVERVIEW (KPIs, Revenue & Order Charts, Live Map, Recent Orders, Activity Feed)
   ============================================================================ */
@Composable
private fun EnterpriseDashboardOverviewPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    totalGmvInr: Double,
    companyCommission20Inr: Double,
    riderEarnings80Inr: Double,
    totalTipsInr: Int,
    tipsByOrder: Map<String, Int>,
    sosAlerts: List<SosSupportAlert>,
    fcmNotifications: List<FcmPushMessageRecord>,
    selfieCheckIns: List<RiderLiveSelfieCheckInRecord>,
    callLogs: List<DirectCallLogEntry>,
    walletTransactions: List<WalletTransaction>,
    overspeedState: RiderOverspeedState,
    batteryState: RiderBatteryPowerState,
    isAutoPilotEnabled: Boolean,
    onToggleAutoPilot: (Boolean) -> Unit,
    lastAutoActionLog: String,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit,
    hasSensitivePiiPermission: Boolean,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean,
    isDesktop: Boolean,
    palette: AdminExecColors,
    repository: SharedOrderRepository,
    onNavigateToPage: (AdminDedicatedPage) -> Unit
) {
    val context = LocalContext.current
    var recentOrdersSearch by remember { mutableStateOf("") }
    var recentOrdersFilter by remember { mutableStateOf("ALL") }

    val registrationsByKey by RiderRegistrationApprovalManager.registrationsByKey.collectAsState()
    val disputeRecords by QuickDeliveryOperationsManager.disputeRecords.collectAsState()
    val withdrawalRequests by QuickDeliveryOperationsManager.withdrawalRequests.collectAsState()

    val pendingKycCount = remember(registrationsByKey) {
        registrationsByKey.values.count { it.approvalStatus == RiderApprovalWorkflowStatus.PENDING_APPROVAL }
    }
    val openDisputesCount = remember(disputeRecords) {
        disputeRecords.count { it.status == DisputeStatus.OPEN }
    }
    val pendingWithdrawalsCount = remember(withdrawalRequests) {
        withdrawalRequests.count { it.status == WithdrawalStatus.PENDING }
    }

    val activeOrdersCount = orders.count {
        it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
    }
    val deliveredCount = orders.count { it.orderStatus == OrderStatus.DELIVERED }
    val onlineRidersCount = riders.count { it.isOnline }
    val unresolvedSosCount = sosAlerts.count { !it.isResolved }

    val filteredRecentOrders = remember(orders, recentOrdersSearch, recentOrdersFilter) {
        orders.filter { o ->
            val matchesStatus = when (recentOrdersFilter) {
                "ACTIVE" -> o.orderStatus != OrderStatus.DELIVERED && o.orderStatus != OrderStatus.CANCELLED
                "DELIVERED" -> o.orderStatus == OrderStatus.DELIVERED
                "SEARCHING" -> o.orderStatus == OrderStatus.SEARCHING_RIDER
                else -> true
            }
            val q = recentOrdersSearch.trim()
            val matchesQuery = q.isEmpty() ||
                o.orderId.contains(q, ignoreCase = true) ||
                o.customerName.contains(q, ignoreCase = true) ||
                (o.assignedRiderName?.contains(q, ignoreCase = true) == true) ||
                o.pickupAddress.fullAddress.contains(q, ignoreCase = true) ||
                o.dropAddress.fullAddress.contains(q, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    val recentOrderHeaders = remember {
        listOf(
            "Order ID",
            "Customer",
            "Customer Phone",
            "Assigned Rider",
            "Vehicle",
            "Total Fare",
            "20% Admin",
            "80% Rider",
            "Payment",
            "Status"
        )
    }
    val recentOrderRows = remember(filteredRecentOrders, hasSensitivePiiPermission) {
        filteredRecentOrders.map { o ->
            val fare = parseOrderFareDouble(o.totalAmount)
            val comm20 = parseOrderFareDouble(o.platformCommission).let { if (it > 0.0) it else fare * 0.20 }
            val earn80 = parseOrderFareDouble(o.riderEarning).let { if (it > 0.0) it else fare * 0.80 }
            listOf(
                o.orderId,
                o.customerName,
                maskPhoneForAdmin(o.customerPhone, hasSensitivePiiPermission),
                o.assignedRiderName ?: "Searching Rider",
                o.vehicleType.title,
                o.totalAmount,
                String.format(Locale.US, "₹%.0f", comm20),
                String.format(Locale.US, "₹%.0f", earn80),
                o.paymentMethod.name.replace("_", " "),
                o.orderStatus.name
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_overview_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val waitingUnassignedOrders = orders.filter { it.orderStatus == OrderStatus.SEARCHING_RIDER }
        if (waitingUnassignedOrders.isNotEmpty()) {
            item(key = "enterprise_overview_unassigned_red_alert") {
                AdminUnassignedOrdersAlertBanner(
                    searchingOrders = waitingUnassignedOrders,
                    onOpenOrdersTab = { onNavigateToPage(AdminDedicatedPage.ORDERS) }
                )
            }
        }

        item(key = "enterprise_overview_cod_pending_alert") {
            val overviewCodSettlements by QuickDeliveryOperationsManager.codSettlements.collectAsState()
            val overviewFlags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
            AdminCodPendingAlertBanner(
                riders = riders,
                orders = orders,
                settlements = overviewCodSettlements,
                flags = overviewFlags,
                onOpenPaymentsPage = { onNavigateToPage(AdminDedicatedPage.PAYMENTS_SETTLEMENTS) }
            )
        }

        item(key = "enterprise_overview_pending_rider_approvals") {
            RiderRegistrationApprovalManager.ensureLoaded(context)
            com.example.ui.rider.AdminPendingRiderApprovalsSection()
        }

        // 1. Executive Command Summary Hero Card
        item(key = "enterprise_overview_hero_summary") {
            AdminExecutiveHeroSummaryCard(
                totalGmvInr = totalGmvInr,
                companyCommission20Inr = companyCommission20Inr,
                riderEarnings80Inr = riderEarnings80Inr,
                orders = orders,
                onlineRidersCount = onlineRidersCount,
                totalRidersCount = riders.size,
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel
            )
        }

        // 1B. Priority Action Strip (1-Tap Jump to KYC, Disputes, Payouts, SOS)
        item(key = "enterprise_overview_priority_strip") {
            AdminPriorityActionStrip(
                pendingKycCount = pendingKycCount,
                openDisputesCount = openDisputesCount,
                pendingWithdrawalsCount = pendingWithdrawalsCount,
                unresolvedSosCount = unresolvedSosCount,
                palette = palette,
                onNavigateToPage = onNavigateToPage
            )
        }

        // 2. Uniform 2x2 SaaS KPI Cards Grid
        item(key = "dashboard_kpi_cards_grid") {
            val kpiCard1: @Composable (Modifier) -> Unit = { mod ->
                DashboardMetricKpiCard(
                    title = "Total GMV Revenue",
                    primaryValue = String.format(Locale.US, "₹%.0f", totalGmvInr),
                    subtitle = "${orders.size} Orders • ₹$totalTipsInr Tips",
                    accentColor = palette.accentTeal,
                    icon = Icons.Default.AccountBalanceWallet,
                    palette = palette,
                    onClick = { onNavigateToPage(AdminDedicatedPage.PAYMENTS_SETTLEMENTS) },
                    modifier = mod
                )
            }
            val kpiCard2: @Composable (Modifier) -> Unit = { mod ->
                DashboardMetricKpiCard(
                    title = "20% Platform Share",
                    primaryValue = String.format(Locale.US, "₹%.0f", companyCommission20Inr),
                    subtitle = String.format(Locale.US, "80%% Rider: ₹%.0f", riderEarnings80Inr),
                    accentColor = palette.accentAmber,
                    icon = Icons.Default.PieChart,
                    palette = palette,
                    onClick = { onNavigateToPage(AdminDedicatedPage.COMMISSION_EARNINGS) },
                    modifier = mod
                )
            }
            val kpiCard3: @Composable (Modifier) -> Unit = { mod ->
                DashboardMetricKpiCard(
                    title = "Active Live Orders",
                    primaryValue = "$activeOrdersCount Active",
                    subtitle = "$deliveredCount Done • ${orders.count { it.orderStatus == OrderStatus.SEARCHING_RIDER }} Searching",
                    accentColor = palette.accentIndigo,
                    icon = Icons.Default.LocalShipping,
                    palette = palette,
                    onClick = { onNavigateToPage(AdminDedicatedPage.ORDERS) },
                    modifier = mod
                )
            }
            val kpiCard4: @Composable (Modifier) -> Unit = { mod ->
                DashboardMetricKpiCard(
                    title = "Online Fleet GPS",
                    primaryValue = "$onlineRidersCount / ${riders.size} Online",
                    subtitle = "${selfieCheckIns.size} Live Selfies • ${batteryState.levelPercent}% Bat",
                    accentColor = if (unresolvedSosCount > 0 || overspeedState.isAlarmRinging) palette.accentCrimson else palette.accentEmerald,
                    icon = if (unresolvedSosCount > 0) Icons.Default.Warning else Icons.AutoMirrored.Filled.DirectionsBike,
                    palette = palette,
                    onClick = { onNavigateToPage(AdminDedicatedPage.RIDERS) },
                    modifier = mod
                )
            }

            if (isDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    kpiCard1(Modifier.weight(1f))
                    kpiCard2(Modifier.weight(1f))
                    kpiCard3(Modifier.weight(1f))
                    kpiCard4(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        kpiCard1(Modifier.weight(1f))
                        kpiCard2(Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        kpiCard3(Modifier.weight(1f))
                        kpiCard4(Modifier.weight(1f))
                    }
                }
            }
        }

        // 2B. Daily Unit Economics (P&L) Snapshot Strip
        item(key = "dashboard_unit_economics_pnl_strip") {
            val disputes by QuickDeliveryOperationsManager.disputeRecords.collectAsState()
            val refundedDisputesInr = remember(disputes) {
                disputes.filter { it.status == DisputeStatus.REFUNDED }.sumOf { it.refundAmountInr }
            }
            val promoSubsidyInr = remember(orders) {
                orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { o ->
                    if (o.packageDetails.description.contains("Promo", ignoreCase = true) ||
                        o.packageDetails.description.contains("FIRST", ignoreCase = true) ||
                        o.packageDetails.description.contains("SAVE", ignoreCase = true)
                    ) 25.0 else 0.0
                }
            }
            val netAdminMarginInr = companyCommission20Inr - promoSubsidyInr - refundedDisputesInr
            val netMarginPct = if (totalGmvInr > 0.0) (netAdminMarginInr / totalGmvInr * 100.0) else 0.0

            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_unit_economics_pnl_card")
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
                        Text(
                            text = "Unit Economics & Net P&L Margin",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = (if (netAdminMarginInr >= 0.0) palette.accentEmerald else palette.accentCrimson).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%% Margin", netMarginPct),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netAdminMarginInr >= 0.0) palette.accentEmerald else palette.accentCrimson,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = palette.surfaceSubtle,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Gross Comm.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(Locale.US, "₹%.0f", companyCommission20Inr),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.accentTeal,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            color = palette.surfaceSubtle,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Refund/Promo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(Locale.US, "-₹%.0f", promoSubsidyInr + refundedDisputesInr),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.accentCrimson,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            color = palette.surfaceSubtle,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Net P&L",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(Locale.US, "₹%.0f", netAdminMarginInr),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netAdminMarginInr >= 0.0) palette.accentEmerald else palette.accentCrimson,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1B. Live Stuck Order Auto-Dispatch Alert & Pin-Code Demand Heatmap
        item(key = "dashboard_stuck_alert_and_heatmap") {
            AdminStuckOrderAlertAndHeatmapCards(
                orders = orders,
                riders = riders,
                repository = repository
            )
        }

        // 2. Revenue (80% Rider / 20% Admin) & Order Status Funnel Cards (Stacked on Mobile, 2-Column on Desktop)
        item(key = "dashboard_revenue_and_order_charts") {
            DashboardRevenueAndOrdersChartsCard(
                orders = orders,
                totalGmvInr = totalGmvInr,
                companyCommission20Inr = companyCommission20Inr,
                riderEarnings80Inr = riderEarnings80Inr,
                isDesktop = isDesktop,
                palette = palette
            )
        }

        // 3. Live Orders / Riders Map & Autonomous Dispatch Engine
        item(key = "dashboard_live_orders_riders_map") {
            DashboardLiveOrdersRidersMapCard(
                orders = orders,
                riders = riders,
                isAutoPilotEnabled = isAutoPilotEnabled,
                onToggleAutoPilot = onToggleAutoPilot,
                lastAutoActionLog = lastAutoActionLog,
                onExecuteAutoStepNow = onExecuteAutoStepNow,
                onSpawnNewLiveOrder = onSpawnNewLiveOrder,
                onOpenFullTracking = { onNavigateToPage(AdminDedicatedPage.LIVE_TRACKING) }
            )
        }

        // 4. Professional Live Orders & Enterprise Dispatch Cards (All orders right below Radar)
        item(key = "dashboard_live_dispatch_cards") {
            DashboardEnterpriseDispatchCardsSection(
                orders = filteredRecentOrders,
                riders = riders,
                tipsByOrder = tipsByOrder,
                hasSensitivePiiPermission = hasSensitivePiiPermission,
                repository = repository,
                context = context,
                palette = palette,
                onExecuteAutoStepNow = onExecuteAutoStepNow,
                onSpawnNewLiveOrder = onSpawnNewLiveOrder,
                onViewAllOrders = { onNavigateToPage(AdminDedicatedPage.ORDERS) }
            )
        }

        // 5. Searchable, Filterable, Paginated Recent Orders Table with .xlsx Export
        item(key = "dashboard_recent_orders_table") {
            AdminEnterprisePaginatedTableCard(
                title = "Live Orders & Enterprise Dispatch Table (.xlsx)",
                subtitle = "Full-width enterprise ledger with Order IDs, customer/rider details, route & 80/20 payout breakdown",
                exportFilePrefix = "QuickDelivery_Recent_Orders",
                sheetName = "Recent_Orders",
                headers = recentOrderHeaders,
                allRows = recentOrderRows,
                filterChips = listOf(
                    "ALL" to "All (${orders.size})",
                    "ACTIVE" to "Active ($activeOrdersCount)",
                    "SEARCHING" to "Searching (${orders.count { it.orderStatus == OrderStatus.SEARCHING_RIDER }})",
                    "DELIVERED" to "Delivered ($deliveredCount)"
                ),
                selectedFilterKey = recentOrdersFilter,
                onFilterSelected = { recentOrdersFilter = it },
                searchQuery = recentOrdersSearch,
                onSearchQueryChange = { recentOrdersSearch = it },
                searchPlaceholder = "Search recent orders by ID, Customer, Rider, Pickup/Drop...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                testTagPrefix = "admin_dashboard_recent_orders"
            )
        }

        // 6. Real-Time Activity Feed (FCM Push Events, Calls, Live Selfies, Wallet Settlements)
        item(key = "dashboard_live_activity_feed") {
            DashboardRealtimeActivityFeedCard(
                fcmNotifications = fcmNotifications,
                selfieCheckIns = selfieCheckIns,
                callLogs = callLogs,
                walletTransactions = walletTransactions,
                palette = palette,
                onViewAllNotifications = { onNavigateToPage(AdminDedicatedPage.NOTIFICATIONS) }
            )
        }
    }
}

@Composable
private fun DashboardMetricKpiCard(
    title: String,
    primaryValue: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector = Icons.Default.Payments,
    palette: AdminExecColors = adminExecPalette(AdminExecutiveThemeManager.isDarkMode.collectAsState().value),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, palette.hairlineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                AdminIconBadge(icon = icon, tint = accentColor, sizeDp = 28)
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardRevenueAndOrdersChartsCard(
    orders: List<CustomerOrder>,
    totalGmvInr: Double,
    companyCommission20Inr: Double,
    riderEarnings80Inr: Double,
    isDesktop: Boolean = false,
    palette: AdminExecColors = adminExecPalette(false)
) {
    val chartOrders = remember(orders) { orders.take(7).reversed() }
    val searchingCount = orders.count { it.orderStatus == OrderStatus.SEARCHING_RIDER }
    val assignedCount = orders.count { it.orderStatus == OrderStatus.RIDER_ASSIGNED || it.orderStatus == OrderStatus.ARRIVED_AT_PICKUP }
    val transitCount = orders.count { it.orderStatus == OrderStatus.PICKED_UP || it.orderStatus == OrderStatus.IN_TRANSIT }
    val deliveredCount = orders.count { it.orderStatus == OrderStatus.DELIVERED }
    val total = orders.size.coerceAtLeast(1)

    val revenueChartPanel: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            color = palette.surfaceSubtle,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, palette.hairlineBorder),
            modifier = mod
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Revenue (80% Rider / 20% Admin) & Volume",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "GMV: ₹%.0f • 80%% Rider: ₹%.0f • 20%% Admin: ₹%.0f",
                            totalGmvInr,
                            riderEarnings80Inr,
                            companyCommission20Inr
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SidebarActiveTeal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Emerald 80% • Gold 20%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SidebarActiveTeal,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val itemsToDraw = if (chartOrders.isEmpty()) listOf(100.0) else chartOrders.map { parseOrderFareDouble(it.totalAmount).coerceAtLeast(20.0) }
                    val maxFare = (itemsToDraw.maxOrNull() ?: 200.0).coerceAtLeast(100.0)
                    val count = itemsToDraw.size.coerceAtLeast(1)
                    val slotWidth = w / count
                    val barWidth = (slotWidth * 0.52f).coerceAtMost(42f)

                    itemsToDraw.forEachIndexed { idx, fare ->
                        val xCenter = slotWidth * idx + slotWidth * 0.5f
                        val totalBarHeight = ((fare / maxFare).toFloat() * (h * 0.85f)).coerceAtLeast(12f)
                        val riderHeight = totalBarHeight * 0.80f
                        val commHeight = totalBarHeight * 0.20f

                        // Rider 80% base portion
                        drawRoundRect(
                            color = SidebarActiveTeal,
                            topLeft = Offset(xCenter - barWidth / 2f, h - riderHeight),
                            size = Size(barWidth, riderHeight),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                        // Admin 20% top cap portion
                        drawRoundRect(
                            color = DashGold,
                            topLeft = Offset(xCenter - barWidth / 2f, h - totalBarHeight),
                            size = Size(barWidth, commHeight.coerceAtLeast(4f)),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    chartOrders.forEach { ord ->
                        Text(
                            text = "#${ord.orderId.takeLast(4)} (${ord.totalAmount})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    val orderStatusFunnelPanel: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            color = palette.surfaceSubtle,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, palette.hairlineBorder),
            modifier = mod
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Order Status Funnel (${orders.size} Total)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DashEmerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "LIVE PIPELINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SidebarActiveTeal,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                OrderStageProgressBar(
                    label = "Searching",
                    badgeText = "SEARCHING RIDER",
                    count = searchingCount,
                    total = total,
                    color = DashOrange
                )
                OrderStageProgressBar(
                    label = "Assigned",
                    badgeText = "RIDER ASSIGNED",
                    count = assignedCount,
                    total = total,
                    color = SidebarActiveTeal
                )
                OrderStageProgressBar(
                    label = "In Transit",
                    badgeText = "IN TRANSIT",
                    count = transitCount,
                    total = total,
                    color = Color(0xFF2563EB)
                )
                OrderStageProgressBar(
                    label = "Delivered",
                    badgeText = "DELIVERED",
                    count = deliveredCount,
                    total = total,
                    color = DashEmerald
                )
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.hairlineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_dashboard_charts_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    revenueChartPanel(Modifier.weight(1.25f))
                    orderStatusFunnelPanel(Modifier.weight(1f))
                }
            } else {
                // Stacked vertically (grid-cols-1) on mobile so Order Status Funnel labels never wrap mid-word
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    revenueChartPanel(Modifier.fillMaxWidth())
                    orderStatusFunnelPanel(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun OrderStageProgressBar(
    label: String,
    badgeText: String,
    count: Int,
    total: Int,
    color: Color
) {
    val ratio = (count.toFloat() / total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val pct = (ratio * 100f).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DashNavy,
                    maxLines = 1,
                    softWrap = false
                )
                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count Orders ($pct%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = Color(0xFFE2E8F0),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun DashboardEnterpriseDispatchCardsSection(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    tipsByOrder: Map<String, Int>,
    hasSensitivePiiPermission: Boolean,
    repository: SharedOrderRepository,
    context: android.content.Context,
    palette: AdminExecColors,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit,
    onViewAllOrders: () -> Unit
) {
    var selectedOrderForSheet by remember { mutableStateOf<CustomerOrder?>(null) }
    var orderToReassign by remember { mutableStateOf<CustomerOrder?>(null) }
    var orderToForceCancel by remember { mutableStateOf<CustomerOrder?>(null) }
    var cancelReasonText by remember { mutableStateOf("Cancelled by Admin Dispatch Control") }

    selectedOrderForSheet?.let { detailOrder ->
        AdminOrderDetailBottomSheet(
            order = detailOrder,
            tipInr = tipsByOrder[detailOrder.orderId] ?: 0,
            hasSensitivePiiPermission = hasSensitivePiiPermission,
            palette = palette,
            onDismiss = { selectedOrderForSheet = null },
            onReassign = { orderToReassign = it },
            onForceCancel = { orderToForceCancel = it },
            onForceReleaseToQueue = { ord ->
                QuickDeliveryOperationsManager.adminForceReleaseOrderToSearching(
                    context = context,
                    repository = repository,
                    orderId = ord.orderId,
                    reason = "Emergency Admin Queue Release"
                )
            },
            onAdvanceStage = { ord ->
                adminAuthenticateOrderStageAndSync(
                    context = context,
                    repository = repository,
                    ord = ord,
                    fallbackAdvance = onExecuteAutoStepNow
                )
            }
        )
    }

    orderToReassign?.let { targetOrder ->
        AlertDialog(
            onDismissRequest = { orderToReassign = null },
            title = { Text("Reassign Order #${targetOrder.orderId}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select an available partner to assign this delivery:",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    if (riders.isEmpty()) {
                        Text("No riders registered yet.", style = MaterialTheme.typography.bodySmall, color = DashCrimson)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            riders.forEach { r ->
                                Surface(
                                    color = if (r.isOnline) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (r.isOnline) DashEmerald else Color(0xFFCBD5E1)),
                                    onClick = {
                                        QuickDeliveryOperationsManager.reassignOrderToRider(
                                            context = context,
                                            repository = repository,
                                            orderId = targetOrder.orderId,
                                            targetRider = r
                                        )
                                        orderToReassign = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${r.name} (${r.riderId})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("${r.vehicleType.title} • ${r.vehicleNumber} • ${if (r.isOnline) "ONLINE" else "OFFLINE"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                                        }
                                        Text("Assign", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = SidebarActiveTeal)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { orderToReassign = null }) {
                    Text("Close")
                }
            }
        )
    }

    orderToForceCancel?.let { targetOrder ->
        AlertDialog(
            onDismissRequest = { orderToForceCancel = null },
            title = { Text("Force Cancel #${targetOrder.orderId}?", fontWeight = FontWeight.ExtraBold, color = DashCrimson) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This immediately cancels the active delivery and syncs across Customer & Rider apps.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = cancelReasonText,
                        onValueChange = { cancelReasonText = it },
                        label = { Text("Cancellation Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        QuickDeliveryOperationsManager.cancelOrderWithReason(
                            context = context,
                            repository = repository,
                            orderId = targetOrder.orderId,
                            reason = cancelReasonText,
                            cancelledByRole = "Admin"
                        )
                        orderToForceCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DashCrimson)
                ) {
                    Text("Force Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToForceCancel = null }) {
                    Text("Keep Active")
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.hairlineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AdminIconBadge(icon = Icons.Default.LocalShipping, tint = palette.accentTeal, sizeDp = 34)
                    Column {
                        Text(
                            text = "All Orders Full Details & Live Dispatch (${orders.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Complete order route, Customer/Rider phone, package, payment & OTP — tap to reassign or edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Button(
                    onClick = onViewAllOrders,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentTeal),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "All Orders",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            orders.forEach { order ->
                val fare = parseOrderFareDouble(order.totalAmount)
                val earn80 = parseOrderFareDouble(order.riderEarning).let { if (it > 0.0) it else fare * 0.80 }
                val isDelivered = order.orderStatus == OrderStatus.DELIVERED
                val isCancelled = order.orderStatus == OrderStatus.CANCELLED
                val statusColor = when {
                    isCancelled -> palette.accentCrimson
                    isDelivered -> palette.accentEmerald
                    else -> palette.accentAmber
                }

                Surface(
                    color = palette.surfaceSubtle,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.hairlineBorder),
                    onClick = { selectedOrderForSheet = order },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "#${order.orderId}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "• ${order.vehicleType.title} • ${String.format(Locale.US, "%.1f km", order.distanceKm)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = order.orderStatus.name.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Inspect Order Details",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "👤 Customer: ${order.customerName} (${maskPhoneForAdmin(order.customerPhone, hasSensitivePiiPermission)})  •  🛵 Rider: ${order.assignedRiderName ?: "Searching nearby..."}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "📍 Pickup: ${order.pickupAddress.fullAddress}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "🏁 Drop: ${order.dropAddress.fullAddress}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📦 ${order.packageDetails.packageType.title} (${order.packageDetails.weightKg} kg) • 💳 ${order.paymentMethod.shortBadge}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%s • Rider ₹%.0f", order.totalAmount, earn80),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.accentTeal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardLiveOrdersRidersMapCard(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    isAutoPilotEnabled: Boolean,
    onToggleAutoPilot: (Boolean) -> Unit,
    lastAutoActionLog: String,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit,
    onOpenFullTracking: () -> Unit
) {
    val radarTransition = rememberInfiniteTransition(label = "dashboardMapPulse")
    val routeProgress by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "routeProgress"
    )
    val activeOrders = remember(orders) {
        orders.filter { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
    }
    val onlineRiders = remember(riders) { riders.filter { it.isOnline } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SidebarDeepTeal),
        border = BorderStroke(1.dp, Color(0xFF1E3A4C)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_next_level_autopilot_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Orders & Riders GPS Telemetry Map",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${onlineRiders.size} Online Riders • ${activeOrders.size} Active Routes • Live Sync Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = SidebarMintText
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenFullTracking,
                        border = BorderStroke(1.dp, SidebarMintText),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Full Map →",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Rich Vector City Cartography & Live Fleet Radar Map (Zero dummy routes when empty)
            AdminLiveVectorCityMapCanvas(
                activeOrders = activeOrders,
                onlineRidersCount = onlineRiders.size,
                sweepProgress = routeProgress,
                topStartLabel = lastAutoActionLog,
                bottomEndLabel = if (activeOrders.isNotEmpty()) "🟢 Live GPS Synced" else "📡 Radar Active",
                mapHeight = 195.dp
            )

            // Live Radar Active Orders Telemetry Strip directly inside Radar card
            if (activeOrders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeOrders.forEach { liveOrd ->
                        Surface(
                            color = Color(0xFF0A2E3B),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "#${liveOrd.orderId} • ${liveOrd.orderStatus.displayTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF5EEAD4),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "${liveOrd.assignedRiderName ?: "Searching Rider"} • ${liveOrd.totalAmount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExecuteAutoStepNow,
                    colors = ButtonDefaults.buttonColors(containerColor = SidebarActiveTeal),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_execute_auto_step_btn")
                ) {
                    Text(
                        text = "Advance Active Order",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSpawnNewLiveOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = DashOrange),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_spawn_auto_order_btn")
                ) {
                    Text(
                        text = "Sync Cloud Orders",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardRealtimeActivityFeedCard(
    fcmNotifications: List<FcmPushMessageRecord>,
    selfieCheckIns: List<RiderLiveSelfieCheckInRecord>,
    callLogs: List<DirectCallLogEntry>,
    walletTransactions: List<WalletTransaction>,
    palette: AdminExecColors = adminExecPalette(false),
    onViewAllNotifications: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.hairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_dashboard_activity_feed_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Operations & FCM Activity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Live stream of FCM alerts, selfie check-ins, calls & settlements",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onViewAllNotifications,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("FCM Console →", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }
            }

            fcmNotifications.take(3).forEach { push ->
                ActivityFeedRow(
                    badge = "FCM • ${push.targetRole.name}",
                    title = push.title,
                    subtitle = "${push.body} (#${push.orderId} • ${push.timestampFormatted})",
                    badgeColor = SidebarActiveTeal
                )
            }
            selfieCheckIns.take(2).forEach { selfie ->
                ActivityFeedRow(
                    badge = "LIVE SELFIE",
                    title = "${selfie.riderName} (${selfie.riderId}) verified online photo",
                    subtitle = "${selfie.vehicleType} • ${selfie.vehiclePlate} • GPS ${selfie.gpsCoordinates} (${selfie.capturedAt})",
                    badgeColor = DashEmerald
                )
            }
            callLogs.take(2).forEach { call ->
                ActivityFeedRow(
                    badge = "DIAL CALL",
                    title = "${call.callerName} (${call.callerRole}) → ${call.receiverName} (${call.receiverRole})",
                    subtitle = "Order #${call.orderId} • ${call.timestamp}",
                    badgeColor = DashOrange
                )
            }
            walletTransactions.take(2).forEach { tx ->
                ActivityFeedRow(
                    badge = "LEDGER",
                    title = "${tx.title} (${tx.formattedAmount()})",
                    subtitle = "${tx.subtitle} • ${tx.timestamp}",
                    badgeColor = DashGold
                )
            }
        }
    }
}

@Composable
private fun ActivityFeedRow(
    badge: String,
    title: String,
    subtitle: String,
    badgeColor: Color
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = badgeColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DashNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ============================================================================
   PAGE 1: DEDICATED ORDERS PAGE (Searchable, Filterable, Paginated + .xlsx Export)
   ============================================================================ */
@Composable
private fun EnterpriseOrdersPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    tipsByOrder: Map<String, Int>,
    repository: SharedOrderRepository,
    hasSensitivePiiPermission: Boolean,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean,
    palette: AdminExecColors,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var selectedOrderForSheet by remember { mutableStateOf<CustomerOrder?>(null) }
    var orderToReassign by remember { mutableStateOf<CustomerOrder?>(null) }
    var orderToForceCancel by remember { mutableStateOf<CustomerOrder?>(null) }
    var cancelReasonText by remember { mutableStateOf("Cancelled by Admin Orders Desk") }

    selectedOrderForSheet?.let { detailOrder ->
        AdminOrderDetailBottomSheet(
            order = detailOrder,
            tipInr = tipsByOrder[detailOrder.orderId] ?: 0,
            hasSensitivePiiPermission = hasSensitivePiiPermission,
            palette = palette,
            onDismiss = { selectedOrderForSheet = null },
            onReassign = { orderToReassign = it },
            onForceCancel = { orderToForceCancel = it },
            onForceReleaseToQueue = { ord ->
                QuickDeliveryOperationsManager.adminForceReleaseOrderToSearching(
                    context = context,
                    repository = repository,
                    orderId = ord.orderId,
                    reason = "Emergency Admin Orders Desk Release"
                )
            },
            onAdvanceStage = { ord ->
                adminAuthenticateOrderStageAndSync(
                    context = context,
                    repository = repository,
                    ord = ord,
                    fallbackAdvance = onExecuteAutoStepNow
                )
            }
        )
    }

    orderToReassign?.let { targetOrder ->
        AlertDialog(
            onDismissRequest = { orderToReassign = null },
            title = { Text("Reassign Order #${targetOrder.orderId}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose a registered partner to assign this active delivery:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    if (riders.isEmpty()) {
                        Text("No riders available.", style = MaterialTheme.typography.bodySmall, color = DashCrimson)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            riders.forEach { r ->
                                Surface(
                                    color = if (r.isOnline) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (r.isOnline) DashEmerald else Color(0xFFCBD5E1)),
                                    onClick = {
                                        QuickDeliveryOperationsManager.reassignOrderToRider(
                                            context = context,
                                            repository = repository,
                                            orderId = targetOrder.orderId,
                                            targetRider = r
                                        )
                                        orderToReassign = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${r.name} (${r.riderId})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("${r.vehicleType.title} • ${r.vehicleNumber} • ${if (r.isOnline) "ONLINE" else "OFFLINE"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                                        }
                                        Text("Assign", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = SidebarActiveTeal)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { orderToReassign = null }) {
                    Text("Close")
                }
            }
        )
    }

    orderToForceCancel?.let { targetOrder ->
        AlertDialog(
            onDismissRequest = { orderToForceCancel = null },
            title = { Text("Force Cancel #${targetOrder.orderId}?", fontWeight = FontWeight.ExtraBold, color = DashCrimson) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This immediately cancels the order and updates Customer & Rider apps.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = cancelReasonText,
                        onValueChange = { cancelReasonText = it },
                        label = { Text("Cancellation Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        QuickDeliveryOperationsManager.cancelOrderWithReason(
                            context = context,
                            repository = repository,
                            orderId = targetOrder.orderId,
                            reason = cancelReasonText,
                            cancelledByRole = "Admin"
                        )
                        orderToForceCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DashCrimson)
                ) {
                    Text("Force Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToForceCancel = null }) {
                    Text("Keep Active")
                }
            }
        )
    }

    val filteredOrders = remember(orders, searchQuery, statusFilter) {
        orders.filter { o ->
            val matchesStatus = when (statusFilter) {
                "SEARCHING" -> o.orderStatus == OrderStatus.SEARCHING_RIDER
                "ASSIGNED" -> o.orderStatus == OrderStatus.RIDER_ASSIGNED || o.orderStatus == OrderStatus.ARRIVED_AT_PICKUP
                "IN_TRANSIT" -> o.orderStatus == OrderStatus.PICKED_UP || o.orderStatus == OrderStatus.IN_TRANSIT
                "DELIVERED" -> o.orderStatus == OrderStatus.DELIVERED
                "CANCELLED" -> o.orderStatus == OrderStatus.CANCELLED
                else -> true
            }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                o.orderId.contains(q, ignoreCase = true) ||
                o.customerName.contains(q, ignoreCase = true) ||
                o.customerPhone.contains(q, ignoreCase = true) ||
                (o.assignedRiderName?.contains(q, ignoreCase = true) == true) ||
                o.pickupAddress.fullAddress.contains(q, ignoreCase = true) ||
                o.dropAddress.fullAddress.contains(q, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    val headers = remember {
        listOf(
            "Order ID",
            "Customer Name",
            "Customer Phone",
            "Assigned Rider",
            "Rider Phone",
            "Vehicle",
            "Weight (kg)",
            "Distance (km)",
            "Pickup Address",
            "Drop Address",
            "Total Fare",
            "20% Commission",
            "80% Rider Share",
            "Payment Method",
            "Status"
        )
    }

    val rows = remember(filteredOrders, hasSensitivePiiPermission) {
        filteredOrders.map { o ->
            val fare = parseOrderFareDouble(o.totalAmount)
            val comm20 = parseOrderFareDouble(o.platformCommission).let { if (it > 0.0) it else fare * 0.20 }
            val earn80 = parseOrderFareDouble(o.riderEarning).let { if (it > 0.0) it else fare * 0.80 }
            listOf(
                o.orderId,
                o.customerName,
                maskPhoneForAdmin(o.customerPhone, hasSensitivePiiPermission),
                o.assignedRiderName ?: "Searching Rider",
                o.assignedRiderPhone?.let { maskPhoneForAdmin(it, hasSensitivePiiPermission) } ?: "Unassigned",
                o.vehicleType.title,
                "${o.packageDetails.weightKg} kg",
                String.format(Locale.US, "%.1f km", o.distanceKm),
                o.pickupAddress.fullAddress,
                o.dropAddress.fullAddress,
                o.totalAmount,
                String.format(Locale.US, "₹%.0f", comm20),
                String.format(Locale.US, "₹%.0f", earn80),
                o.paymentMethod.name.replace("_", " "),
                o.orderStatus.name
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_live_orders_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Action Bar
        item(key = "orders_page_action_header") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AdminIconBadge(icon = Icons.Default.LocalShipping, tint = Color(0xFF34D399), sizeDp = 36)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Orders Management & Live Dispatch",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${orders.size} Total • ${orders.count { it.orderStatus == OrderStatus.DELIVERED }} Delivered • ${orders.count { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }} Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExecuteAutoStepNow,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentTeal),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Advance Next",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onSpawnNewLiveOrder,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentAmber),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Sync Live",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Paginated & Exportable Orders Table
        item(key = "orders_paginated_xlsx_table") {
            AdminEnterprisePaginatedTableCard(
                title = "All Orders Master Table (.xlsx Export)",
                subtitle = "Search, filter by order stage, and export to Excel (.xlsx)",
                exportFilePrefix = "QuickDelivery_Orders_Master",
                sheetName = "Orders",
                headers = headers,
                allRows = rows,
                filterChips = listOf(
                    "ALL" to "All (${orders.size})",
                    "SEARCHING" to "Searching (${orders.count { it.orderStatus == OrderStatus.SEARCHING_RIDER }})",
                    "ASSIGNED" to "Assigned (${orders.count { it.orderStatus == OrderStatus.RIDER_ASSIGNED || it.orderStatus == OrderStatus.ARRIVED_AT_PICKUP }})",
                    "IN_TRANSIT" to "In Transit (${orders.count { it.orderStatus == OrderStatus.PICKED_UP || it.orderStatus == OrderStatus.IN_TRANSIT }})",
                    "DELIVERED" to "Delivered (${orders.count { it.orderStatus == OrderStatus.DELIVERED }})"
                ),
                selectedFilterKey = statusFilter,
                onFilterSelected = { statusFilter = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search orders by Order ID, Customer, Rider, Pickup/Drop Address...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                testTagPrefix = "admin_orders_table"
            )
        }

        // Compact Interactive Per-Order Cards (Tap to open Full Timeline & OTP Modal Bottom Sheet)
        items(filteredOrders, key = { it.orderId }) { order ->
            val isDelivered = order.orderStatus == OrderStatus.DELIVERED
            val isCancelled = order.orderStatus == OrderStatus.CANCELLED
            val statusColor = when {
                isCancelled -> palette.accentCrimson
                isDelivered -> palette.accentEmerald
                else -> palette.accentAmber
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                onClick = { selectedOrderForSheet = order },
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${order.orderId} • ${order.customerName} (${maskPhoneForAdmin(order.customerPhone, hasSensitivePiiPermission)})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${order.vehicleType.title} • Rider: ${order.assignedRiderName ?: "Searching"} • ${order.totalAmount} (80% ${order.riderEarning})",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = statusColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = order.orderStatus.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Order Details",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "${order.pickupAddress.fullAddress.substringBefore(",")} → ${order.dropAddress.fullAddress.substringBefore(",")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isDelivered && !isCancelled) {
                        val riderRequestedAuth = order.latestRiderQuickNote
                            ?.contains("OTP Auth", ignoreCase = true) == true
                        Surface(
                            color = if (riderRequestedAuth) Color(0xFFFEF3C7) else palette.surfaceSubtle,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (riderRequestedAuth) palette.accentAmber else palette.hairlineBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🔐 Synced Random OTPs (Zero SMS) • Pickup: ${order.pickupOtp} • Drop: ${order.deliveryOtp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                if (riderRequestedAuth) {
                                    Text(
                                        text = "🔔 Rider requested Admin Sync-Auth to proceed!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                val adminQuickBtnLabel = when (order.orderStatus) {
                                    OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP ->
                                        "✅ Authenticate Pickup OTP (${order.pickupOtp}) & Sync Rider"
                                    OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT ->
                                        "✅ Authenticate Drop OTP (${order.deliveryOtp}) & Sync Rider"
                                    else -> null
                                }
                                if (adminQuickBtnLabel != null) {
                                    Button(
                                        onClick = {
                                            adminAuthenticateOrderStageAndSync(
                                                context = context,
                                                repository = repository,
                                                ord = order,
                                                fallbackAdvance = onExecuteAutoStepNow
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentEmerald),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_card_auth_otp_btn_${order.orderId}")
                                    ) {
                                        Text(
                                            text = adminQuickBtnLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun adminAuthenticateOrderStageAndSync(
    context: android.content.Context,
    repository: SharedOrderRepository,
    ord: CustomerOrder,
    fallbackAdvance: () -> Unit
) {
    val ts = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    when (ord.orderStatus) {
        OrderStatus.SEARCHING_RIDER -> {
            AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
            AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                repository = repository,
                orderId = ord.orderId
            )
            val updatedOrder = repository.orders.value.firstOrNull { it.orderId == ord.orderId } ?: ord
            FcmOrderNotificationManager.dispatchOrderStatusPair(
                context = context,
                order = updatedOrder,
                postToSystemTray = true
            )
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> {
            repository.verifyPickupOtpByRider(ord.orderId, ord.pickupOtp, ord.assignedRiderId)
            if (OrderProofRegistry.getPickupProof(ord.orderId) == null) {
                OrderProofRegistry.savePickupProof(
                    PickupProof(
                        orderId = ord.orderId,
                        riderId = ord.assignedRiderId ?: "ADMIN-SYNC",
                        riderName = ord.assignedRiderName ?: "Rider Partner",
                        packagePhotoUrl = "admin_sync_otp_verified",
                        packagePhotoSummary = "Admin Sync-Authenticated Pickup OTP (${ord.pickupOtp})",
                        senderName = ord.customerName,
                        senderSignatureSvg = "ADMIN_SYNC_AUTH_${ord.pickupOtp}",
                        verifiedOtp = ord.pickupOtp,
                        latitude = ord.pickupAddress.lat,
                        longitude = ord.pickupAddress.lng,
                        capturedAt = ts
                    )
                )
            }
            QuickDeliveryOperationsManager.recordOrderTimelineEvent(
                context = context,
                orderId = ord.orderId,
                title = "Admin Sync-Authenticated Pickup OTP (${ord.pickupOtp})",
                detail = "Verified remotely from Admin App (Zero SMS) — Rider unlocked for Drop",
                actorRole = "ADMIN"
            )
            val updatedOrder = repository.orders.value.firstOrNull { it.orderId == ord.orderId } ?: ord
            FcmOrderNotificationManager.dispatchOrderStatusPair(
                context = context,
                order = updatedOrder,
                postToSystemTray = true
            )
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> {
            repository.verifyDeliveryOtpByRider(ord.orderId, ord.deliveryOtp, ord.assignedRiderId)
            if (OrderProofRegistry.getDeliveryProof(ord.orderId) == null) {
                val fareInr = ord.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
                OrderProofRegistry.saveDeliveryProof(
                    DeliveryProof(
                        orderId = ord.orderId,
                        riderId = ord.assignedRiderId ?: "ADMIN-SYNC",
                        riderName = ord.assignedRiderName ?: "Rider Partner",
                        deliveryPhotoUrl = "admin_sync_otp_verified",
                        deliveryPhotoSummary = "Admin Sync-Authenticated Drop OTP (${ord.deliveryOtp})",
                        recipientName = ord.dropAddress.contactName.ifBlank { ord.customerName },
                        recipientSignatureSvg = "ADMIN_SYNC_AUTH_${ord.deliveryOtp}",
                        verifiedOtp = ord.deliveryOtp,
                        codAmountCollectedInr = if (ord.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY) fareInr else 0.0,
                        latitude = ord.dropAddress.lat,
                        longitude = ord.dropAddress.lng,
                        capturedAt = ts
                    )
                )
            }
            QuickDeliveryOperationsManager.recordOrderTimelineEvent(
                context = context,
                orderId = ord.orderId,
                title = "Admin Sync-Authenticated Delivery OTP (${ord.deliveryOtp})",
                detail = "Delivery verified remotely from Admin App (Zero SMS) — Customer & Rider synced",
                actorRole = "ADMIN"
            )
            val delivered = ord.copy(orderStatus = OrderStatus.DELIVERED)
            MotivationAndPromoManager.notifySuccessfulDeliveryMotivationForBoth(
                order = delivered,
                forceTrigger = true
            )
            FcmOrderNotificationManager.dispatchOrderStatusPair(
                context = context,
                order = delivered,
                postToSystemTray = true
            )
            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        }
        else -> fallbackAdvance()
    }
}

/* ============================================================================
   PAGE 2: DEDICATED LIVE TRACKING PAGE (Interactive Map + Fleet GPS & ETA Stream)
   ============================================================================ */
@Composable
private fun EnterpriseLiveTrackingPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    overspeedState: RiderOverspeedState,
    batteryState: RiderBatteryPowerState,
    isAutoPilotEnabled: Boolean,
    onToggleAutoPilot: (Boolean) -> Unit,
    lastAutoActionLog: String,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit
) {
    var trackingFilter by remember { mutableStateOf("ALL") }
    val activeOrders = remember(orders) {
        orders.filter { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
    }
    val onlineRiders = remember(riders) { riders.filter { it.isOnline } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_live_tracking_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "live_tracking_map_hero") {
            DashboardLiveOrdersRidersMapCard(
                orders = orders,
                riders = riders,
                isAutoPilotEnabled = isAutoPilotEnabled,
                onToggleAutoPilot = onToggleAutoPilot,
                lastAutoActionLog = lastAutoActionLog,
                onExecuteAutoStepNow = onExecuteAutoStepNow,
                onSpawnNewLiveOrder = onSpawnNewLiveOrder,
                onOpenFullTracking = {}
            )
        }

        // Filter Chips for Live Tracking View
        item(key = "live_tracking_filter_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All Active Corridors (${activeOrders.size}) & Online Riders (${onlineRiders.size})",
                    "ORDERS" to "Active Orders (${activeOrders.size})",
                    "RIDERS" to "Online Rider GPS (${onlineRiders.size})"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = trackingFilter == key,
                        onClick = { trackingFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SidebarDarkTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (trackingFilter != "RIDERS") {
            items(activeOrders, key = { "track_ord_${it.orderId}" }) { order ->
                val approxEtaMins = when (order.orderStatus) {
                    OrderStatus.SEARCHING_RIDER -> 18
                    OrderStatus.RIDER_ASSIGNED -> 14
                    OrderStatus.ARRIVED_AT_PICKUP -> 11
                    OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> 7
                    else -> 5
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
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
                                text = "Live Order #${order.orderId} • ${order.vehicleType.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SidebarDarkTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, DashEmerald)
                            ) {
                                Text(
                                    text = "ETA ~$approxEtaMins mins • ${order.orderStatus.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DashEmerald,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Rider: ${order.assignedRiderName ?: "Auto-Matching"} • Pickup GPS: (${String.format(Locale.US, "%.4f, %.4f", order.pickupAddress.lat, order.pickupAddress.lng)}) → Drop GPS: (${String.format(Locale.US, "%.4f, %.4f", order.dropAddress.lat, order.dropAddress.lng)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }

        if (trackingFilter != "ORDERS") {
            items(onlineRiders, key = { "track_rider_${it.riderId}" }) { rider ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rider.name} (${rider.riderId}) • ${rider.vehicleType.title} (${rider.vehicleNumber})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DashNavy
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Live GPS: %.5f, %.5f • Speed: %d/%d km/h • Battery: %d%%",
                                    rider.currentLat,
                                    rider.currentLng,
                                    overspeedState.currentSpeedKmh,
                                    overspeedState.speedLimitKmh,
                                    batteryState.levelPercent
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "GPS LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DashEmerald,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================================
   PAGE 3: DEDICATED RIDERS PAGE (Searchable, Filterable, Paginated Table + .xlsx + Full Cards)
   ============================================================================ */
@Composable
private fun EnterpriseRidersPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    tipsByOrder: Map<String, Int>,
    selfieCheckIns: List<RiderLiveSelfieCheckInRecord>,
    overspeedState: RiderOverspeedState,
    batteryState: RiderBatteryPowerState,
    weeklyTransfersUsed: Int,
    repository: SharedOrderRepository,
    hasSensitivePiiPermission: Boolean,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean,
    palette: AdminExecColors
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var riderFilter by remember { mutableStateOf("ALL") }
    var selectedRiderForSheet by remember { mutableStateOf<RiderProfile?>(null) }

    selectedRiderForSheet?.let { rider ->
        AdminRiderDetailBottomSheet(
            rider = rider,
            hasSensitivePiiPermission = hasSensitivePiiPermission,
            palette = palette,
            onDismiss = { selectedRiderForSheet = null }
        )
    }

    val filteredRiders = remember(riders, searchQuery, riderFilter) {
        riders.filter { r ->
            val vault = RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(r)
            val matchesFilter = when (riderFilter) {
                "ONLINE" -> r.isOnline
                "VERIFIED" -> r.verificationStatus == RiderVerificationStatus.VERIFIED
                "DEBT_WARNING" -> r.commissionDebtInr >= 100.0
                else -> true
            }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                r.name.contains(q, ignoreCase = true) ||
                r.riderId.contains(q, ignoreCase = true) ||
                r.mobile.contains(q, ignoreCase = true) ||
                r.vehicleNumber.contains(q, ignoreCase = true) ||
                r.vehicleType.title.contains(q, ignoreCase = true) ||
                vault.aadhaarNumber.contains(q, ignoreCase = true) ||
                vault.panNumber.contains(q, ignoreCase = true) ||
                vault.drivingLicenseNumber.contains(q, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val headers = remember {
        listOf(
            "Rider ID",
            "Full Name",
            "Mobile",
            "Vehicle Type",
            "RC Plate",
            "Online Status",
            "Live Selfie Check-In",
            "KYC Status",
            "Aadhaar",
            "PAN",
            "Driving License",
            "Wallet (₹)",
            "COD Due (₹ / ₹200 Limit)",
            "Deliveries",
            "Live GPS"
        )
    }

    val rows = remember(filteredRiders, selfieCheckIns, hasSensitivePiiPermission) {
        filteredRiders.map { r ->
            val vault = RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(r)
            val selfie = RiderLiveSelfieCallAndExcelManager.getLatestSelfieForRider(r.riderId, r.name)
            listOf(
                r.riderId,
                r.name,
                maskPhoneForAdmin(r.mobile, hasSensitivePiiPermission),
                r.vehicleType.title,
                vault.rcNumberPlate,
                if (r.isOnline) "ONLINE" else "OFFLINE",
                "VERIFIED (${selfie.capturedAt})",
                r.verificationStatus.name,
                maskDocForAdmin(vault.aadhaarNumber, hasSensitivePiiPermission),
                maskDocForAdmin(vault.panNumber, hasSensitivePiiPermission),
                maskDocForAdmin(vault.drivingLicenseNumber, hasSensitivePiiPermission),
                String.format(Locale.US, "₹%.0f", r.walletBalanceInr),
                String.format(Locale.US, "₹%.0f / ₹200", r.commissionDebtInr),
                "${r.totalDeliveries}",
                String.format(Locale.US, "%.4f, %.4f", r.currentLat, r.currentLng)
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_riders_all_data_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "enterprise_riders_pending_approvals") {
            com.example.ui.rider.AdminPendingRiderApprovalsSection()
        }

        // Rider Rejection Rate & Fraud/Safety Guard Alerts Card
        item(key = "enterprise_riders_fraud_guard_alerts") {
            var guardBannerNote by remember { mutableStateOf<String?>(null) }
            val riderRiskEntries = remember(riders, orders, overspeedState) {
                riders.map { r ->
                    val declCount = orders.count { r.riderId in it.rejectedByRiderIds }
                    val debtRisk = r.commissionDebtInr >= 100.0
                    val highReject = declCount >= 3
                    Triple(r, declCount, debtRisk || highReject || overspeedState.isAlarmRinging)
                }.sortedByDescending { it.second }
            }
            val flaggedRiders = riderRiskEntries.filter { it.third }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (flaggedRiders.isNotEmpty()) Color(0xFFFEF2F2) else palette.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (flaggedRiders.isNotEmpty()) palette.accentCrimson.copy(alpha = 0.45f) else palette.hairlineBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_riders_fraud_guard_card")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (flaggedRiders.isNotEmpty()) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (flaggedRiders.isNotEmpty()) palette.accentCrimson else palette.accentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Rider Rejection & Fraud/Safety Guard",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = (if (flaggedRiders.isNotEmpty()) palette.accentCrimson else palette.accentEmerald).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = if (flaggedRiders.isEmpty()) "Healthy Fleet" else "${flaggedRiders.size} Flagged",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (flaggedRiders.isEmpty()) palette.accentEmerald else palette.accentCrimson,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    riderRiskEntries.take(4).forEach { (riderItem, declines, _) ->
                        val riskTags = buildList {
                            if (declines > 0) add("$declines Declines")
                            if (riderItem.commissionDebtInr >= 150.0) add("COD Due ₹${riderItem.commissionDebtInr.toInt()}/₹200")
                            if (overspeedState.isAlarmRinging) add("Speed >60 km/h")
                        }.joinToString(" • ").ifBlank { "0 Declines • Clean Record" }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${riderItem.name} (${riderItem.riderId})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = riskTags,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (declines >= 3 || riderItem.commissionDebtInr >= 100.0) palette.accentCrimson else palette.textSecondary
                                )
                            }
                            if (declines >= 2 || riderItem.commissionDebtInr >= 100.0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        QuickDeliveryOperationsManager.sendBroadcastFromAdmin(
                                            context = context,
                                            message = "Compliance Alert for ${riderItem.name}: Please reduce order declines & clear pending COD/debt dues to stay prioritized.",
                                            target = "RIDER",
                                            repository = repository
                                        )
                                        guardBannerNote = "Warning broadcast pushed to ${riderItem.name}"
                                    },
                                    border = BorderStroke(1.dp, palette.accentCrimson),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Warn",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentCrimson,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    guardBannerNote?.let { note ->
                        Text(
                            text = "✓ $note",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentEmerald
                        )
                    }
                }
            }
        }

        // Paginated Riders Table with .xlsx Export
        item(key = "riders_paginated_xlsx_table") {
            AdminEnterprisePaginatedTableCard(
                title = "Riders Fleet Master Directory (.xlsx Export)",
                subtitle = "${riders.count { it.isOnline }} Online • ${selfieCheckIns.size} Live Selfies • Weekly Transfers $weeklyTransfersUsed/${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS}",
                exportFilePrefix = "QuickDelivery_Riders_Fleet",
                sheetName = "Riders",
                headers = headers,
                allRows = rows,
                filterChips = listOf(
                    "ALL" to "All Riders (${riders.size})",
                    "ONLINE" to "Online (${riders.count { it.isOnline }})",
                    "VERIFIED" to "KYC Verified (${riders.count { it.verificationStatus == RiderVerificationStatus.VERIFIED }})",
                    "DEBT_WARNING" to "Near ₹200 COD Limit (${riders.count { it.commissionDebtInr >= 150.0 }})"
                ),
                selectedFilterKey = riderFilter,
                onFilterSelected = { riderFilter = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search Rider by Name, ID, Mobile, Vehicle Plate, Aadhaar, PAN, DL...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                testTagPrefix = "admin_riders_table"
            )
        }

        // Compact Interactive Rider Fleet Cards (Tap to open Live Selfie, 6-Doc Vault & Call Sheet)
        items(filteredRiders, key = { it.riderId }) { rider ->
            val docVault = remember(rider) {
                RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(rider)
            }
            val riderTipsEarned = orders.filter { it.assignedRiderId == rider.riderId }
                .sumOf { tipsByOrder[it.orderId] ?: 0 }

            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                onClick = { selectedRiderForSheet = rider },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_rider_full_data_card_${rider.riderId}")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AdminIconBadge(
                                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                                tint = if (rider.isOnline) palette.accentEmerald else palette.textSecondary,
                                sizeDp = 36
                            )
                            Column {
                                Text(
                                    text = "${rider.name} (${rider.riderId}) • ${rider.vehicleType.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${maskPhoneForAdmin(rider.mobile, hasSensitivePiiPermission)} • ${docVault.rcNumberPlate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = (if (rider.isOnline) palette.accentEmerald else palette.textSecondary).copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = if (rider.isOnline) "ONLINE" else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (rider.isOnline) palette.accentEmerald else palette.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Rider Profile Sheet",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Wallet: ₹%.0f • COD Due: ₹%.0f/₹200 • Tips: ₹%d • %d Trips",
                                rider.walletBalanceInr,
                                rider.commissionDebtInr,
                                riderTipsEarned,
                                rider.totalDeliveries
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accentTeal,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = {
                                val updated = rider.copy(
                                    walletBalanceInr = rider.walletBalanceInr + 500.0,
                                    commissionDebtInr = 0.0,
                                    accountStatus = "ACTIVE",
                                    isOnline = true,
                                    shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT
                                )
                                val newList = riders.map { if (it.riderId == rider.riderId) updated else it }
                                AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newList)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "+₹500 / Clear Debt",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================================
   PAGE 4: DEDICATED CUSTOMERS PAGE (Searchable, Filterable, Paginated + .xlsx Export)
   ============================================================================ */
@Composable
private fun EnterpriseCustomersPage(
    orders: List<CustomerOrder>,
    tipsByOrder: Map<String, Int>,
    riderRatings: Map<String, RiderDeliveryFeedback>,
    suggestions: List<AppSuggestionEntry>,
    totalGmvInr: Double,
    hasSensitivePiiPermission: Boolean,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterKey by remember { mutableStateOf("ALL") }

    data class CustomerDirectoryRow(
        val customerId: String,
        val name: String,
        val phone: String,
        val email: String,
        val pickupAddress: String,
        val dropAddress: String,
        val orderCount: Int,
        val totalSpendInr: Double,
        val tipsInr: Int,
        val paymentPreference: String
    )

    val context = LocalContext.current
    val registeredCustomers = remember(orders.size) {
        MotivationAndPromoManager.getRegisteredCustomersSnapshot(context)
    }

    val customerDirectory = remember(orders, tipsByOrder, registeredCustomers) {
        val list = mutableListOf<CustomerDirectoryRow>()
        val regByPhone = registeredCustomers.associateBy { it.phone.removePrefix("+91").trim() }
        val grouped = orders.groupBy { it.customerPhone.removePrefix("+91").trim().ifBlank { it.customerId } }
        val seenPhones = HashSet<String>()

        grouped.forEach { (phoneClean, custOrders) ->
            if (custOrders.isNotEmpty()) {
                seenPhones.add(phoneClean)
                val sample = custOrders.first()
                val displayPhone = if (phoneClean.all(Char::isDigit) && phoneClean.length >= 10) "+91 $phoneClean" else sample.customerPhone.ifBlank { "Not Provided" }
                val activeCachedPhone = MotivationAndPromoManager.cachedCustomerPhone.removePrefix("+91").trim()
                val syncedEmail = regByPhone[phoneClean]?.email.orEmpty()
                val emailValue = when {
                    syncedEmail.isNotBlank() -> syncedEmail
                    activeCachedPhone.isNotBlank() && activeCachedPhone == phoneClean && MotivationAndPromoManager.cachedCustomerEmail.isNotBlank() ->
                        MotivationAndPromoManager.cachedCustomerEmail
                    else -> "Not Shared"
                }
                list.add(
                    CustomerDirectoryRow(
                        customerId = sample.customerId,
                        name = sample.customerName.ifBlank { regByPhone[phoneClean]?.name.orEmpty().ifBlank { "Customer" } },
                        phone = displayPhone,
                        email = emailValue,
                        pickupAddress = sample.pickupAddress.fullAddress,
                        dropAddress = sample.dropAddress.fullAddress,
                        orderCount = custOrders.size,
                        totalSpendInr = custOrders.filter { it.orderStatus != OrderStatus.CANCELLED }
                            .sumOf { parseOrderFareDouble(it.totalAmount) },
                        tipsInr = custOrders.sumOf { tipsByOrder[it.orderId] ?: 0 },
                        paymentPreference = sample.paymentMethod.name.replace("_", " ")
                    )
                )
            }
        }

        registeredCustomers.forEachIndexed { idx, reg ->
            val phoneClean = reg.phone.removePrefix("+91").trim()
            val dedupKey = phoneClean.ifBlank { reg.email.lowercase() }
            if (dedupKey.isNotBlank() && dedupKey !in seenPhones) {
                seenPhones.add(dedupKey)
                val displayPhone = if (phoneClean.all(Char::isDigit) && phoneClean.length >= 10) "+91 $phoneClean" else "Not Provided"
                list.add(
                    CustomerDirectoryRow(
                        customerId = "CUST-${phoneClean.takeLast(4).ifBlank { (idx + 1).toString() }}",
                        name = reg.name.ifBlank { "Registered Customer" },
                        phone = displayPhone,
                        email = reg.email.ifBlank { "Not Shared" },
                        pickupAddress = "Registered Account (No orders placed yet)",
                        dropAddress = "—",
                        orderCount = 0,
                        totalSpendInr = 0.0,
                        tipsInr = 0,
                        paymentPreference = "Not Ordered Yet"
                    )
                )
            }
        }
        list
    }

    val filteredCustomers = remember(customerDirectory, searchQuery, filterKey) {
        customerDirectory.filter { c ->
            val matchesFilter = when (filterKey) {
                "REPEAT" -> c.orderCount >= 2
                "PREPAID" -> c.paymentPreference.contains("UPI", ignoreCase = true) || c.paymentPreference.contains("Prepaid", ignoreCase = true)
                else -> true
            }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                c.name.contains(q, ignoreCase = true) ||
                c.customerId.contains(q, ignoreCase = true) ||
                c.phone.contains(q, ignoreCase = true) ||
                c.email.contains(q, ignoreCase = true) ||
                c.pickupAddress.contains(q, ignoreCase = true) ||
                c.dropAddress.contains(q, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val headers = remember {
        listOf(
            "Customer ID",
            "Full Name",
            "Mobile Number",
            "Email",
            "Saved Pickup Address",
            "Saved Drop Address",
            "Total Orders",
            "Total Spend (₹)",
            "Tips Given (₹)",
            "Payment Preference"
        )
    }

    val rows = remember(filteredCustomers, hasSensitivePiiPermission) {
        filteredCustomers.map { c ->
            listOf(
                c.customerId,
                c.name,
                maskPhoneForAdmin(c.phone, hasSensitivePiiPermission),
                c.email,
                c.pickupAddress,
                c.dropAddress,
                "${c.orderCount}",
                String.format(Locale.US, "₹%.0f", c.totalSpendInr),
                "₹${c.tipsInr}",
                c.paymentPreference
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_customers_all_data_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "customers_paginated_xlsx_table") {
            AdminEnterprisePaginatedTableCard(
                title = "Customers Master Directory (.xlsx Export)",
                subtitle = "${customerDirectory.size} Registered Customers • Total GMV ₹${totalGmvInr.toInt()} • ${riderRatings.size} Ratings • ${suggestions.count { it.senderRole == "CUSTOMER" }} Suggestions",
                exportFilePrefix = "QuickDelivery_Customers_Directory",
                sheetName = "Customers",
                headers = headers,
                allRows = rows,
                filterChips = listOf(
                    "ALL" to "All Customers (${customerDirectory.size})",
                    "REPEAT" to "Repeat Bookers (${customerDirectory.count { it.orderCount >= 2 }})",
                    "PREPAID" to "UPI / Prepaid"
                ),
                selectedFilterKey = filterKey,
                onFilterSelected = { filterKey = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search Customer by Name, ID, Phone, Email, Pickup/Drop Address...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                testTagPrefix = "admin_customers_table"
            )
        }

        if (filteredCustomers.isEmpty()) {
            item(key = "customers_empty_state") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No customer records yet — live customer details appear automatically once real orders are placed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(filteredCustomers, key = { it.customerId }) { cust ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_customer_full_data_card_${cust.customerId}")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${cust.name} (${cust.customerId})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DashNavy
                            )
                            Text(
                                text = "${maskPhoneForAdmin(cust.phone, hasSensitivePiiPermission)} • ${cust.email}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFF0FDFA),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SidebarActiveTeal)
                        ) {
                            Text(
                                text = "${cust.orderCount} Orders • ₹${cust.totalSpendInr.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SidebarDarkTeal,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Pickup: ${cust.pickupAddress}\nDrop: ${cust.dropAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF334155)
                    )

                    DirectDialPadCallButton(
                        orderId = "ADMIN-CUST-${cust.customerId}",
                        callerRole = "ADMIN",
                        callerName = "Admin Control Center",
                        receiverRole = "CUSTOMER",
                        receiverName = cust.name,
                        phoneNumber = cust.phone,
                        buttonTestTag = "admin_call_customer_btn_${cust.customerId}"
                    )
                }
            }
        }
    }
}

/* ============================================================================
   PAGE 5: DEDICATED KYC VERIFICATION PAGE
   ============================================================================ */
@Composable
private fun EnterpriseKycPage(
    riders: List<RiderProfile>,
    selfieCheckIns: List<RiderLiveSelfieCheckInRecord>,
    repository: SharedOrderRepository,
    hasSensitivePiiPermission: Boolean,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { RiderRegistrationApprovalManager.ensureLoaded(context) }
    val regRecords by RiderRegistrationApprovalManager.registrationsByKey.collectAsState()
    var kycSearch by remember { mutableStateOf("") }
    var kycFilter by remember { mutableStateOf("ALL") }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var zoomPreviewDoc by remember { mutableStateOf<Triple<String, Bitmap, String>?>(null) }

    if (zoomPreviewDoc != null) {
        val (docLabel, docBitmap, clarityMeta) = zoomPreviewDoc!!
        AdminKycPhotoZoomPreviewDialog(
            docTitle = docLabel,
            bitmap = docBitmap,
            clarityInfo = clarityMeta,
            onDismiss = { zoomPreviewDoc = null }
        )
    }

    val filteredRiders = remember(riders, kycSearch, kycFilter) {
        riders.filter { r ->
            val matchesStatus = when (kycFilter) {
                "VERIFIED" -> r.verificationStatus == RiderVerificationStatus.VERIFIED
                "PENDING" -> r.verificationStatus != RiderVerificationStatus.VERIFIED
                else -> true
            }
            val q = kycSearch.trim()
            val matchesQuery = q.isEmpty() ||
                r.name.contains(q, ignoreCase = true) ||
                r.riderId.contains(q, ignoreCase = true) ||
                r.vehicleNumber.contains(q, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    val headers = remember {
        listOf(
            "Rider ID",
            "Rider Name",
            "KYC Status",
            "Aadhaar Number",
            "PAN Number",
            "Driving License",
            "Vehicle RC",
            "Insurance Policy",
            "PUC Certificate",
            "Live Selfie Check-In"
        )
    }

    val rows = remember(filteredRiders, selfieCheckIns, hasSensitivePiiPermission) {
        filteredRiders.map { r ->
            val vault = RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(r)
            val selfie = RiderLiveSelfieCallAndExcelManager.getLatestSelfieForRider(r.riderId, r.name)
            listOf(
                r.riderId,
                r.name,
                r.verificationStatus.name,
                maskDocForAdmin(vault.aadhaarNumber, hasSensitivePiiPermission),
                maskDocForAdmin(vault.panNumber, hasSensitivePiiPermission),
                maskDocForAdmin(vault.drivingLicenseNumber, hasSensitivePiiPermission),
                vault.rcNumberPlate,
                vault.insurancePolicyNumber,
                vault.pucCertificateNumber,
                "VERIFIED (${selfie.capturedAt})"
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_kyc_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        statusBanner?.let { msg ->
            item(key = "kyc_action_banner") {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DashEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        item(key = "enterprise_kyc_pending_approvals") {
            com.example.ui.rider.AdminPendingRiderApprovalsSection()
        }

        item(key = "kyc_doc_expiry_tracker") {
            AdminRiderDocExpiryCard(riders = riders)
        }

        item(key = "kyc_paginated_table") {
            AdminEnterprisePaginatedTableCard(
                title = "Rider KYC & Document Compliance Queue (.xlsx Export)",
                subtitle = "Aadhaar, PAN, Driving License, Vehicle RC, Insurance, PUC & Live Selfie Verification",
                exportFilePrefix = "QuickDelivery_KYC_Audit",
                sheetName = "KYC_Verification",
                headers = headers,
                allRows = rows,
                filterChips = listOf(
                    "ALL" to "All Partners (${riders.size})",
                    "VERIFIED" to "Verified (${riders.count { it.verificationStatus == RiderVerificationStatus.VERIFIED }})",
                    "PENDING" to "Pending Review (${riders.count { it.verificationStatus != RiderVerificationStatus.VERIFIED }})"
                ),
                selectedFilterKey = kycFilter,
                onFilterSelected = { kycFilter = it },
                searchQuery = kycSearch,
                onSearchQueryChange = { kycSearch = it },
                searchPlaceholder = "Search KYC by Rider Name, ID, Vehicle Plate...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                testTagPrefix = "admin_kyc_table"
            )
        }

        items(filteredRiders, key = { "kyc_${it.riderId}" }) { rider ->
            val vault = RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(rider)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🛡️ ${rider.name} (${rider.riderId})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DashNavy
                            )
                            Text(
                                text = "Aadhaar: ${maskDocForAdmin(vault.aadhaarNumber, hasSensitivePiiPermission)} • PAN: ${maskDocForAdmin(vault.panNumber, hasSensitivePiiPermission)} • DL: ${maskDocForAdmin(vault.drivingLicenseNumber, hasSensitivePiiPermission)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DashEmerald)
                        ) {
                            Text(
                                text = rider.verificationStatus.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DashEmerald,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val matchedRegRecord = remember(regRecords, rider.riderId, rider.mobile, rider.name) {
                        val cleanMobile = rider.mobile.filter(Char::isDigit).takeLast(10)
                        regRecords.values.firstOrNull { reg ->
                            (rider.riderId.isNotBlank() && reg.riderId.equals(rider.riderId, ignoreCase = true)) ||
                                (cleanMobile.length == 10 && reg.phone.filter(Char::isDigit).takeLast(10) == cleanMobile) ||
                                (rider.name.isNotBlank() && reg.name.equals(rider.name, ignoreCase = true))
                        }
                    }

                    if (matchedRegRecord != null) {
                        val kycThumbs = listOf(
                            "Aadhaar" to matchedRegRecord.aadhaarImageBase64,
                            "PAN" to matchedRegRecord.panImageBase64,
                            "DL" to matchedRegRecord.dlImageBase64,
                            "RC" to matchedRegRecord.rcImageBase64,
                            "Selfie" to matchedRegRecord.selfieImageBase64
                        ).filter { it.second.isNotBlank() }

                        if (kycThumbs.isNotEmpty()) {
                            Text(
                                text = "Uploaded HD Documents (Tap to Zoom & Save to Phone):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SidebarDarkTeal
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                kycThumbs.forEach { (label, b64) ->
                                    val bmp = remember(b64) { RiderRegistrationApprovalManager.decodeBase64ToBitmap(b64) }
                                    if (bmp != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    zoomPreviewDoc = Triple(
                                                        "${rider.name} — $label Photo",
                                                        bmp,
                                                        matchedRegRecord.clarityScoresSummary.ifBlank { "${bmp.width}×${bmp.height}px Clear" }
                                                    )
                                                }
                                                .testTag("admin_registered_kyc_thumb_${rider.riderId}_${label.lowercase()}")
                                        ) {
                                            Box {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Zoom $label Photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(68.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.55f),
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(3.dp)
                                                        .size(18.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.ZoomIn,
                                                            contentDescription = "Zoom",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "$label 🔍",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SidebarDarkTeal,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val updated = riders.map {
                                    if (it.riderId == rider.riderId) {
                                        it.copy(
                                            verificationStatus = RiderVerificationStatus.VERIFIED,
                                            accountStatus = "ACTIVE",
                                            isOnline = true
                                        )
                                    } else {
                                        it
                                    }
                                }
                                AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, updated)
                                statusBanner = "✅ Approved all 6 KYC documents & Live Selfie for ${rider.name} (${rider.riderId})"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DashEmerald),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "✓ Approve KYC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val updated = riders.map {
                                    if (it.riderId == rider.riderId) {
                                        it.copy(
                                            verificationStatus = RiderVerificationStatus.PENDING,
                                            accountStatus = "RE_KYC_REQUIRED",
                                            isOnline = false
                                        )
                                    } else {
                                        it
                                    }
                                }
                                AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, updated)
                                val matchingReg = regRecords.values.firstOrNull {
                                    it.riderId == rider.riderId ||
                                        (rider.mobile.isNotBlank() && it.phone == rider.mobile.filter(Char::isDigit).takeLast(10))
                                }
                                if (matchingReg != null) {
                                    RiderRegistrationApprovalManager.rejectRiderByAdmin(
                                        context = context,
                                        riderKey = matchingReg.riderKey,
                                        reason = "Admin requested fresh e-KYC document verification.",
                                        rejectedDocFields = listOf("AADHAAR", "PAN", "DL", "RC", "SELFIE")
                                    )
                                }
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                statusBanner = "🔄 Flagged ${rider.name} (${rider.riderId}) for Re-KYC verification and paused duty until re-approved."
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Re-Verify e-KYC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================================
   PAGE 6: DEDICATED PAYMENTS & SETTLEMENTS PAGE (Paginated Table + .xlsx Export)
   ============================================================================ */
@Composable
private fun EnterprisePaymentsSettlementsPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    walletTransactions: List<WalletTransaction>,
    totalGmvInr: Double,
    companyCommission20Inr: Double,
    riderEarnings80Inr: Double,
    totalTipsInr: Int,
    repository: SharedOrderRepository,
    hasSensitivePiiPermission: Boolean,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        QuickDeliveryOperationsManager.ensureLoaded(context)
        RiderRegistrationApprovalManager.ensureLoaded(context)
    }
    val withdrawalRequests by QuickDeliveryOperationsManager.withdrawalRequests.collectAsState()
    val registrationsMap by RiderRegistrationApprovalManager.registrationsByKey.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var paymentFilter by remember { mutableStateOf("ALL") }
    var settlementNotice by remember { mutableStateOf<String?>(null) }

    val prepaidGmv = remember(orders) {
        orders.filter { it.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY && it.orderStatus != OrderStatus.CANCELLED }
            .sumOf { parseOrderFareDouble(it.totalAmount) }
    }
    val codGmv = remember(orders) {
        orders.filter { it.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY && it.orderStatus != OrderStatus.CANCELLED }
            .sumOf { parseOrderFareDouble(it.totalAmount) }
    }

    val paymentHeaders = remember {
        listOf(
            "Txn / Order ID",
            "Type",
            "Customer / Rider",
            "Payment Method",
            "Gross Amount",
            "80% Rider Credit",
            "20% Admin Cut",
            "Settlement Bank / UPI",
            "Status",
            "Timestamp"
        )
    }

    val allPaymentRows = remember(orders, walletTransactions, paymentFilter, searchQuery, hasSensitivePiiPermission) {
        val orderPaymentRows = orders.map { o ->
            val fare = parseOrderFareDouble(o.totalAmount)
            val comm20 = parseOrderFareDouble(o.platformCommission).let { if (it > 0.0) it else fare * 0.20 }
            val earn80 = parseOrderFareDouble(o.riderEarning).let { if (it > 0.0) it else fare * 0.80 }
            val mode = o.paymentMethod.name.replace("_", " ")
            listOf(
                o.orderId,
                if (o.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY) "COD_COLLECTION" else "ONLINE_GATEWAY",
                "${o.customerName} → ${o.assignedRiderName ?: "Rider"}",
                mode,
                o.totalAmount,
                String.format(Locale.US, "₹%.0f", earn80),
                String.format(Locale.US, "₹%.0f", comm20),
                riders.firstOrNull { it.riderId == o.assignedRiderId }?.verifiedUpiId?.takeIf { it.isNotBlank() }?.let { maskDocForAdmin(it, hasSensitivePiiPermission) } ?: "Not Linked",
                if (o.orderStatus == OrderStatus.DELIVERED) "SETTLED" else "IN_ESCROW",
                o.createdAt
            )
        }
        val walletRows = walletTransactions.map { tx ->
            val fmtAmt = tx.formattedAmount()
            listOf(
                tx.transactionId,
                "WALLET_LEDGER",
                tx.title,
                tx.subtitle,
                fmtAmt,
                if (tx.isCredit) fmtAmt else "₹0",
                if (!tx.isCredit) fmtAmt else "₹0",
                "Rider Wallet",
                "POSTED",
                tx.timestamp
            )
        }
        val combined = orderPaymentRows + walletRows
        combined.filter { row ->
            val matchesFilter = when (paymentFilter) {
                "ONLINE" -> row[1] == "ONLINE_GATEWAY"
                "COD" -> row[1] == "COD_COLLECTION"
                "WALLET" -> row[1] == "WALLET_LEDGER"
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() || row.any { it.contains(q, ignoreCase = true) }
            matchesFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_settlements_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // KPI Summary Row
        item(key = "payments_kpi_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardMetricKpiCard(
                    title = "ONLINE PREPAID",
                    primaryValue = String.format(Locale.US, "₹%.0f", prepaidGmv),
                    subtitle = "UPI / Card / NetBanking",
                    accentColor = SidebarActiveTeal,
                    onClick = { paymentFilter = "ONLINE" },
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricKpiCard(
                    title = "COD CASH COLLECTED",
                    primaryValue = String.format(Locale.US, "₹%.0f", codGmv),
                    subtitle = "20% Auto-Deducted from Wallet",
                    accentColor = DashOrange,
                    onClick = { paymentFilter = "COD" },
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricKpiCard(
                    title = "NET RIDER PAYOUTS",
                    primaryValue = String.format(Locale.US, "₹%.0f", riderEarnings80Inr + totalTipsInr),
                    subtitle = "80% Share + ₹$totalTipsInr Tips",
                    accentColor = DashEmerald,
                    onClick = { paymentFilter = "ALL" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        settlementNotice?.let { msg ->
            item(key = "payments_settlement_notice") {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DashEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        item(key = "payments_porter_cod_cash_handover_ledger") {
            val liveCodSettlements by QuickDeliveryOperationsManager.codSettlements.collectAsState()
            val liveCodFlags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
            AdminCodCashHandoverLedgerCard(
                riders = riders,
                orders = orders,
                settlements = liveCodSettlements,
                flags = liveCodFlags,
                onVerifyClaim = { claim ->
                    QuickDeliveryOperationsManager.verifyRiderCodTransferClaimByAdmin(context, claim.settlementId)
                    repository.setRiderCommissionDebt(claim.riderId, 0.0)
                    val unlockedFleet = repository.riders.value.map { r ->
                        if (r.riderId == claim.riderId) r.copy(accountStatus = "ACTIVE", commissionDebtInr = 0.0) else r
                    }
                    AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, unlockedFleet)
                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                    settlementNotice = "✅ Verified ₹${claim.amountInr.toInt()} transfer from ${claim.riderName} (Ref: ${claim.referenceUtr}). Ledger reset to ₹0 (FREE) & Rider unlocked!"
                },
                onMarkFullPaidResetZero = { rider, ledger ->
                    QuickDeliveryOperationsManager.markCodSettledByAdmin(
                        context = context,
                        riderId = rider.riderId,
                        riderName = rider.name,
                        riderPhone = rider.mobile,
                        amountInr = ledger.unpaidCommissionDueInr,
                        referenceUtr = "ADMIN_CLEARED_ZERO"
                    )
                    repository.setRiderCommissionDebt(rider.riderId, 0.0)
                    val unlockedFleet = repository.riders.value.map { r ->
                        if (r.riderId == rider.riderId) r.copy(accountStatus = "ACTIVE", commissionDebtInr = 0.0) else r
                    }
                    AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, unlockedFleet)
                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                    settlementNotice = "✅ Marked ₹${ledger.unpaidCommissionDueInr.toInt()} COD commission PAID for ${rider.name}. Balance is now ₹0 (🟢 FREE)!"
                },
                onReverseSettlement = { settled ->
                    QuickDeliveryOperationsManager.reverseCodSettlementByAdmin(context, settled.settlementId)
                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                    settlementNotice = "⚠️ Reversed ₹${settled.amountInr.toInt()} auto-cleared UTR (${settled.referenceUtr}) for ${settled.riderName}. Ledger due restored & auto-lock re-applied if ≥ limit!"
                }
            )
        }

        item(key = "payments_cod_settlement_ledger") {
            AdminCodSettlementLedgerCard(
                orders = orders,
                riders = riders
            )
        }

        // Rider Payout Withdrawal Requests Queue
        item(key = "payments_withdrawal_requests_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, SidebarActiveTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val pendingWithdrawals = withdrawalRequests.count { it.status == WithdrawalStatus.PENDING }
                    Text(
                        text = "💸 Rider Payout Withdrawal Requests ($pendingWithdrawals Pending)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SidebarDarkTeal
                    )
                    if (withdrawalRequests.isEmpty()) {
                        Text(
                            text = "No rider payout withdrawal requests submitted yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        withdrawalRequests.take(10).forEach { req ->
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${req.riderName} (${req.riderId}) • ₹${String.format(Locale.US, "%.0f", req.amountInr)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DashNavy
                                            )
                                            val modeLabel = if (req.payoutMode == "WEEKLY_AUTO") "📅 Weekly Auto" else "⚡ Instant UPI/IMPS"
                                            Text(
                                                text = "$modeLabel • ${req.payoutDestination} • ${req.createdAtLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF475569)
                                            )
                                            val riderBankRec = remember(registrationsMap, req.riderId, req.riderPhone) {
                                                RiderRegistrationApprovalManager.getRecordForRiderProfile(req.riderId, req.riderPhone)
                                            }
                                            if (riderBankRec != null && riderBankRec.hasBankAccountDetails) {
                                                val displayAcc = if (hasSensitivePiiPermission) {
                                                    riderBankRec.bankAccountNumber
                                                } else {
                                                    riderBankRec.maskedBankAccountNumber
                                                }
                                                Text(
                                                    text = "🏦 ${riderBankRec.bankName} • Holder: ${riderBankRec.bankAccountHolderName} • A/C: $displayAcc • IFSC: ${riderBankRec.bankIfscCode}" +
                                                        if (riderBankRec.bankUpiId.isNotBlank()) " • UPI: ${riderBankRec.bankUpiId}" else "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF065F46)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = when (req.status) {
                                                WithdrawalStatus.PAID -> Color(0xFFECFDF5)
                                                WithdrawalStatus.REJECTED -> Color(0xFFFEF2F2)
                                                WithdrawalStatus.PENDING -> Color(0xFFFFF7ED)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = req.status.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when (req.status) {
                                                    WithdrawalStatus.PAID -> DashEmerald
                                                    WithdrawalStatus.REJECTED -> DashCrimson
                                                    WithdrawalStatus.PENDING -> DashOrange
                                                },
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (req.status == WithdrawalStatus.PENDING) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    QuickDeliveryOperationsManager.updateWithdrawalStatusByAdmin(
                                                        context = context,
                                                        requestId = req.requestId,
                                                        status = WithdrawalStatus.PAID,
                                                        note = "Paid to ${req.payoutDestination}"
                                                    )
                                                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                                    settlementNotice = "✅ Approved & marked PAID: ₹${String.format(Locale.US, "%.0f", req.amountInr)} to ${req.riderName} (${req.payoutDestination})"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DashEmerald),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "✓ Approve & Paid",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    QuickDeliveryOperationsManager.updateWithdrawalStatusByAdmin(
                                                        context = context,
                                                        requestId = req.requestId,
                                                        status = WithdrawalStatus.REJECTED,
                                                        note = "Verification mismatch — contact Admin support"
                                                    )
                                                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                                    settlementNotice = "❌ Rejected withdrawal ${req.requestId} for ${req.riderName}"
                                                },
                                                border = BorderStroke(1.dp, DashCrimson),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "✕ Reject",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DashCrimson,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Paginated Payments & Settlements Table with .xlsx Export
        item(key = "payments_paginated_xlsx_table") {
            AdminEnterprisePaginatedTableCard(
                title = "Payments, Gateway & Wallet Settlements Table (.xlsx Export)",
                subtitle = "Complete reconciliation of Online Prepaid, COD Cash & Wallet Ledger",
                exportFilePrefix = "QuickDelivery_Payments_Settlements",
                sheetName = "Payments_Settlements",
                headers = paymentHeaders,
                allRows = allPaymentRows,
                filterChips = listOf(
                    "ALL" to "All Transactions (${allPaymentRows.size})",
                    "ONLINE" to "Online Gateway",
                    "COD" to "Cash on Delivery (COD)",
                    "WALLET" to "Wallet Ledger (${walletTransactions.size})"
                ),
                selectedFilterKey = paymentFilter,
                onFilterSelected = { paymentFilter = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search payments by Order ID, Customer, Rider, UPI, Method...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                testTagPrefix = "admin_payments_table"
            )
        }

        // Partner Payout Settlement Cards
        items(riders, key = { "payout_${it.riderId}" }) { rider ->
            val vault = RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(rider)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${rider.name} (${rider.riderId})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DashNavy
                        )
                        Text(
                            text = "Bank: ${vault.bankName} • A/C: ${maskDocForAdmin(vault.bankAccountNumber, hasSensitivePiiPermission)} • UPI: ${vault.upiId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Wallet Balance: ₹%.0f • Pending COD Debt: ₹%.0f",
                                rider.walletBalanceInr,
                                rider.commissionDebtInr
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DashEmerald
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            repository.setRiderCommissionDebt(rider.riderId, 0.0)
                            MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                            settlementNotice = "✅ Cleared COD commission debt for ${rider.name} (${vault.upiId})"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SidebarActiveTeal),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Mark Settled",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================================
   PAGE 7: DEDICATED 20% COMPANY COMMISSION / 80% RIDER EARNINGS PAGE
   ============================================================================ */
@Composable
private fun EnterpriseCommissionAndRiderEarningsPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    tipsByOrder: Map<String, Int>,
    totalGmvInr: Double,
    companyCommission20Inr: Double,
    riderEarnings80Inr: Double,
    totalTipsInr: Int,
    repository: SharedOrderRepository,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var splitFilter by remember { mutableStateOf("ALL") }

    val filteredOrders = remember(orders, searchQuery, splitFilter) {
        orders.filter { o ->
            val matchesFilter = when (splitFilter) {
                "COD" -> o.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY
                "PREPAID" -> o.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
                "DELIVERED" -> o.orderStatus == OrderStatus.DELIVERED
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                o.orderId.contains(q, ignoreCase = true) ||
                o.customerName.contains(q, ignoreCase = true) ||
                (o.assignedRiderName?.contains(q, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }
    }

    val earningsHeaders = remember {
        listOf(
            "Order ID",
            "Assigned Rider",
            "Vehicle",
            "Payment Mode",
            "100% Gross Fare",
            "20% Company Commission",
            "80% Net Rider Earning",
            "100% Customer Tip",
            "Total Rider Take-Home",
            "Accounting Rule",
            "Status"
        )
    }

    val earningsRows = remember(filteredOrders, tipsByOrder) {
        filteredOrders.map { o ->
            val gross = parseOrderFareDouble(o.totalAmount)
            val comm20 = parseOrderFareDouble(o.platformCommission).let { if (it > 0.0) it else gross * 0.20 }
            val rider80 = parseOrderFareDouble(o.riderEarning).let { if (it > 0.0) it else gross * 0.80 }
            val tip = tipsByOrder[o.orderId] ?: 0
            val totalRiderTakeHome = rider80 + tip
            val rule = if (o.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY) {
                "COD: Rider keeps 100% cash; 20% (₹${comm20.toInt()}) debited from wallet"
            } else {
                "Prepaid: 80% (₹${rider80.toInt()}) + Tip credited to wallet"
            }
            listOf(
                o.orderId,
                o.assignedRiderName ?: "Searching Rider",
                o.vehicleType.title,
                o.paymentMethod.name.replace("_", " "),
                String.format(Locale.US, "₹%.0f", gross),
                String.format(Locale.US, "₹%.0f (20%%)", comm20),
                String.format(Locale.US, "₹%.0f (80%%)", rider80),
                "₹$tip (100%)",
                String.format(Locale.US, "₹%.0f", totalRiderTakeHome),
                rule,
                o.orderStatus.name
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_commission_earnings_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 20% / 80% Split Hero Card
        item(key = "commission_80_20_hero") {
            Card(
                colors = CardDefaults.cardColors(containerColor = SidebarDarkTeal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💰 20% Company Commission / 80% Rider Earnings Split Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Strict automated revenue split on every order + 100% customer tips to rider + ₹200 automatic COD commission limit enforcement.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SidebarMintText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardMetricKpiCard(
                            title = "100% GROSS ORDER FARE",
                            primaryValue = String.format(Locale.US, "₹%.0f", totalGmvInr),
                            subtitle = "${orders.size} Orders • Tap: All",
                            accentColor = SidebarActiveTeal,
                            onClick = { splitFilter = "ALL" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_kpi_gross_fare_btn")
                        )
                        DashboardMetricKpiCard(
                            title = "20% COMPANY COMMISSION",
                            primaryValue = String.format(Locale.US, "₹%.0f", companyCommission20Inr),
                            subtitle = "20% Share • Tap: COD",
                            accentColor = DashGold,
                            onClick = { splitFilter = "COD" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_kpi_company_commission_btn")
                        )
                        DashboardMetricKpiCard(
                            title = "80% RIDER EARNINGS",
                            primaryValue = String.format(Locale.US, "₹%.0f", riderEarnings80Inr),
                            subtitle = "+ ₹$totalTipsInr Tips • Prepaid",
                            accentColor = DashEmerald,
                            onClick = { splitFilter = "PREPAID" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_kpi_rider_earnings_btn")
                        )
                    }

                    // Visual 80% / 20% Proportional Split Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "80% Rider (₹${riderEarnings80Inr.toInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6EE7B7),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "20% Admin (₹${companyCommission20Inr.toInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFDE68A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .fillMaxHeight()
                                    .background(DashEmerald)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(0.2f)
                                    .fillMaxHeight()
                                    .background(DashGold)
                            )
                        }
                    }
                }
            }
        }

        // 2. Paginated 20% / 80% Earnings Table with .xlsx Export
        item(key = "commission_80_20_paginated_table") {
            AdminEnterprisePaginatedTableCard(
                title = "20% Commission & 80% Rider Earnings Ledger (.xlsx Export)",
                subtitle = "Per-order breakdown of 100% Fare, 20% Admin Commission, 80% Rider Share & 100% Tips",
                exportFilePrefix = "QuickDelivery_Earnings_20_80_Split",
                sheetName = "Earnings_20_80",
                headers = earningsHeaders,
                allRows = earningsRows,
                filterChips = listOf(
                    "ALL" to "All Orders (${orders.size})",
                    "PREPAID" to "Online Prepaid (80% Credited)",
                    "COD" to "Cash on Delivery (20% Debited)",
                    "DELIVERED" to "Delivered"
                ),
                selectedFilterKey = splitFilter,
                onFilterSelected = { splitFilter = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search by Order ID, Rider Name, Customer, Payment Mode...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                testTagPrefix = "admin_earnings_80_20_table"
            )
        }

        // 3. Rider ₹200 COD Commission Limit Monitor
        item(key = "rider_debt_cap_monitor_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🛡️ ₹200 COD Commission Limit Monitor (Auto-Dispatch Eligibility)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SidebarDarkTeal
                    )
                    riders.forEach { r ->
                        val isOverCap = r.commissionDebtInr >= 200.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${r.name} (${r.riderId}) • Wallet: ₹${r.walletBalanceInr.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DashNavy
                                )
                                Text(
                                    text = "20% COD Commission Due: ₹${r.commissionDebtInr.toInt()} / ₹200 Limit • ${if (isOverCap) "AUTO-LOCKED AT LIMIT" else "ELIGIBLE FOR DISPATCH"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverCap) DashCrimson else DashEmerald
                                )
                            }
                            if (r.commissionDebtInr > 0.0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { repository.setRiderCommissionDebt(r.riderId, 0.0) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = "Settle 20% Debt",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================================
   PAGE 8: DEDICATED COMPLAINTS & SOS PAGE
   ============================================================================ */
@Composable
private fun EnterpriseComplaintsAndSosPage(
    orders: List<CustomerOrder> = emptyList(),
    sosAlerts: List<SosSupportAlert>,
    suggestions: List<AppSuggestionEntry>,
    riderRatings: Map<String, RiderDeliveryFeedback>,
    customerRatings: Map<String, CustomerRatingByRider>,
    overspeedState: RiderOverspeedState,
    batteryState: RiderBatteryPowerState
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    val supportTickets by QuickDeliveryOperationsManager.supportTickets.collectAsState()
    val unresolvedCount = sosAlerts.count { !it.isResolved }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_complaints_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "complaints_dispute_refund_desk") {
            AdminDisputeRefundDeskCard(orders = orders)
        }

        // Emergency SOS Live Incident Console
        item(key = "complaints_sos_header_card") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (unresolvedCount > 0) Color(0xFFFEF2F2) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, if (unresolvedCount > 0) DashCrimson else SidebarActiveTeal),
                modifier = Modifier.fillMaxWidth()
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
                                text = "🚨 Rider Emergency SOS & Safety Telemetry Console",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (unresolvedCount > 0) DashCrimson else SidebarDarkTeal
                            )
                            Text(
                                text = "$unresolvedCount Active SOS Alerts • Speed Guard: ${overspeedState.currentSpeedKmh}/${overspeedState.speedLimitKmh} km/h • Battery: ${batteryState.levelPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (unresolvedCount > 0) DashCrimson else DashEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (unresolvedCount > 0) "$unresolvedCount ACTIVE SOS" else "ALL CLEAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (unresolvedCount > 0) Color.White else DashEmerald,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (sosAlerts.isEmpty()) {
                        Text(
                            text = "✅ No emergency SOS incidents logged. All online riders are operating safely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DashEmerald
                        )
                    } else {
                        sosAlerts.forEach { alert ->
                            Surface(
                                color = if (alert.isResolved) Color(0xFFF8FAFC) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (alert.isResolved) Color(0xFFCBD5E1) else DashCrimson),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${alert.alertId} • ${alert.reporterRole}: ${alert.reporterName} (Order #${alert.orderId})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (alert.isResolved) DashNavy else DashCrimson
                                        )
                                        Text(
                                            text = "📍 Live GPS: ${alert.gpsCoordinates} • Route: ${alert.routeSummary} • ${alert.triggeredAt}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (!alert.isResolved) {
                                        Button(
                                            onClick = {
                                                SosSafetySupportManager.resolveSosAlert(context, alert.alertId)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DashEmerald),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Resolve SOS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "✓ RESOLVED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DashEmerald,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Customer & Rider In-App Support Tickets Queue
        item(key = "complaints_support_tickets_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SidebarActiveTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val openTickets = supportTickets.count { !it.isResolved }
                    Text(
                        text = "🎧 Customer & Rider Helpdesk Tickets ($openTickets Open)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SidebarDarkTeal
                    )
                    Text(
                        text = "📧 Auto-Mail Desk: ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL} (Admin App + Google Workspace)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F766E)
                    )
                    if (supportTickets.isEmpty()) {
                        Text(
                            text = "No support tickets filed yet. Tickets submitted from Customer or Rider Help screens appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        supportTickets.forEach { ticket ->
                            var replyDraft by remember(ticket.ticketId) {
                                mutableStateOf(ticket.adminReply.ifBlank { "Issue checked & resolved by Quick Delivery Support." })
                            }
                            Surface(
                                color = if (ticket.isResolved) Color(0xFFF8FAFC) else Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (ticket.isResolved) Color(0xFFCBD5E1) else DashGold),
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
                                        Text(
                                            text = "${ticket.ticketId} • ${ticket.senderRole}: ${ticket.senderName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DashNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (ticket.isResolved) "✓ RESOLVED" else ticket.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ticket.isResolved) DashEmerald else DashOrange,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    if (ticket.orderId.isNotBlank() || ticket.senderPhone.isNotBlank()) {
                                        Text(
                                            text = buildString {
                                                if (ticket.orderId.isNotBlank()) append("Order #${ticket.orderId} • ")
                                                if (ticket.senderPhone.isNotBlank()) append("${ticket.senderPhone} • ")
                                                append(ticket.createdAtLabel)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Text(ticket.message, style = MaterialTheme.typography.bodySmall, color = DashNavy)
                                    if (!ticket.isResolved) {
                                        OutlinedTextField(
                                            value = replyDraft,
                                            onValueChange = { replyDraft = it },
                                            label = { Text("Admin Reply to ${ticket.senderRole}") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = {
                                                QuickDeliveryOperationsManager.resolveSupportTicketByAdmin(
                                                    context = context,
                                                    ticketId = ticket.ticketId,
                                                    adminReply = replyDraft
                                                )
                                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DashEmerald),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("✓ Resolve Ticket & Send Reply", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (ticket.adminReply.isNotBlank()) {
                                        Text("Admin Reply: ${ticket.adminReply}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val subject = "[Ticket #${ticket.ticketId}] ${ticket.senderRole}: ${ticket.category}"
                                            val body = buildString {
                                                appendLine("Ticket ID: ${ticket.ticketId}")
                                                appendLine("Role: ${ticket.senderRole}")
                                                appendLine("Name: ${ticket.senderName}")
                                                if (ticket.senderPhone.isNotBlank()) appendLine("Phone: ${ticket.senderPhone}")
                                                if (ticket.orderId.isNotBlank()) appendLine("Order ID: #${ticket.orderId}")
                                                appendLine("Message: ${ticket.message}")
                                                if (replyDraft.isNotBlank()) appendLine("\nAdmin Note: $replyDraft")
                                            }
                                            runCatching {
                                                context.startActivity(
                                                    com.example.data.cloud.QuickDeliveryBrandLinks.buildSupportMailtoIntent(
                                                        subject = subject,
                                                        body = body
                                                    )
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = SidebarDarkTeal)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Forward / Log in ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SidebarDarkTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mutual Ratings & Suggestions Panel (Preserves all existing ratings & suggestion box functionality)
        item(key = "complaints_mutual_ratings_and_suggestions") {
            AdminMutualRatingsAndSuggestionsPanel(initialExpanded = true)
        }
    }
}

/* ============================================================================
   PAGE 9: DEDICATED REPORTS & MASTER EXCEL (.XLSX) WORKBOOK EXPORT PAGE
   ============================================================================ */
@Composable
private fun EnterpriseReportsAndWorkbookPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    walletTransactions: List<WalletTransaction>,
    tipsByOrder: Map<String, Int>,
    totalGmvInr: Double,
    companyCommission20Inr: Double,
    riderEarnings80Inr: Double,
    hasSensitivePiiPermission: Boolean,
    dateRangeLabel: String,
    cityFilterLabel: String
) {
    val context = LocalContext.current
    var masterExportResult by remember { mutableStateOf<XlsxExportResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_reports_list")
    ) {
        // Multi-Sheet Master .xlsx Export Ribbon at top of Reports page
        Card(
            colors = CardDefaults.cardColors(containerColor = SidebarDarkTeal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📑 Enterprise Reports & Multi-Sheet .xlsx Workbook Generator",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Date Filter: $dateRangeLabel • City: $cityFilterLabel • Exports 5-Sheet OpenXML (.xlsx) Workbook",
                        style = MaterialTheme.typography.labelSmall,
                        color = SidebarMintText
                    )

                    Button(
                        onClick = {
                            val ordersSheet = XlsxSheetSpec(
                                sheetName = "Orders",
                                headers = listOf("Order ID", "Customer", "Phone", "Rider", "Vehicle", "Total Fare", "20% Commission", "80% Rider", "Status"),
                                rows = orders.map { o ->
                                    listOf(
                                        o.orderId,
                                        o.customerName,
                                        maskPhoneForAdmin(o.customerPhone, hasSensitivePiiPermission),
                                        o.assignedRiderName ?: "Searching",
                                        o.vehicleType.title,
                                        o.totalAmount,
                                        o.platformCommission,
                                        o.riderEarning,
                                        o.orderStatus.name
                                    )
                                }
                            )
                            val ridersSheet = XlsxSheetSpec(
                                sheetName = "Riders",
                                headers = listOf("Rider ID", "Name", "Mobile", "Vehicle", "Plate", "Wallet", "Debt", "Deliveries"),
                                rows = riders.map { r ->
                                    listOf(
                                        r.riderId,
                                        r.name,
                                        maskPhoneForAdmin(r.mobile, hasSensitivePiiPermission),
                                        r.vehicleType.title,
                                        r.vehicleNumber,
                                        String.format(Locale.US, "₹%.0f", r.walletBalanceInr),
                                        String.format(Locale.US, "₹%.0f", r.commissionDebtInr),
                                        "${r.totalDeliveries}"
                                    )
                                }
                            )
                            val customersSheet = XlsxSheetSpec(
                                sheetName = "Customers",
                                headers = listOf("Customer ID", "Name", "Phone", "Pickup Address", "Drop Address", "Order Fare"),
                                rows = orders.map { o ->
                                    listOf(
                                        o.customerId,
                                        o.customerName,
                                        maskPhoneForAdmin(o.customerPhone, hasSensitivePiiPermission),
                                        o.pickupAddress.fullAddress,
                                        o.dropAddress.fullAddress,
                                        o.totalAmount
                                    )
                                }
                            )
                            val earningsSheet = XlsxSheetSpec(
                                sheetName = "Earnings_20_80",
                                headers = listOf("Order ID", "Rider", "Gross Fare", "20% Company", "80% Rider", "Tip (100%)"),
                                rows = orders.map { o ->
                                    listOf(
                                        o.orderId,
                                        o.assignedRiderName ?: "Searching",
                                        o.totalAmount,
                                        o.platformCommission,
                                        o.riderEarning,
                                        "₹${tipsByOrder[o.orderId] ?: 0}"
                                    )
                                }
                            )
                            val paymentsSheet = XlsxSheetSpec(
                                sheetName = "Payments",
                                headers = listOf("Txn ID", "Title", "Subtitle", "Amount", "Timestamp"),
                                rows = walletTransactions.map { tx ->
                                    listOf(tx.transactionId, tx.title, tx.subtitle, tx.formattedAmount(), tx.timestamp)
                                }
                            )
                            masterExportResult = AdminExcelXlsxExporter.exportWorkbookToXlsx(
                                context = context,
                                filePrefix = "QuickDelivery_Master_5Sheet_Report",
                                sheets = listOf(ordersSheet, ridersSheet, customersSheet, earningsSheet, paymentsSheet),
                                launchShareSheet = false
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_reports_export_master_xlsx_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export 5-Sheet .xlsx",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                masterExportResult?.let { res ->
                    val locationNote = if (res.publicDownloadLocation.isNotBlank()) {
                        "📁 Saved to Phone Storage: ${res.publicDownloadLocation} (${res.sheetCount} sheets • ${res.rowCount} rows)"
                    } else {
                        "✅ Generated ${res.fileName} (${res.sheetCount} sheets • ${res.rowCount} rows • ${res.sizeBytes} bytes) at ${res.absolutePath}"
                    }
                    Text(
                        text = locationNote,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                }
            }
        }

        // Interactive Online Excel Spreadsheet Workbook (Preserves all existing online sheet features)
        Box(modifier = Modifier.weight(1f)) {
            AdminOnlineExcelSpreadsheetPage(
                orders = orders,
                riders = riders,
                tipsByOrder = tipsByOrder,
                totalGmvInr = totalGmvInr,
                platformCommissionInr = companyCommission20Inr,
                riderShareInr = riderEarnings80Inr
            )
        }
    }
}

/* ============================================================================
   PAGE 10: DEDICATED NOTIFICATIONS (FCM) PAGE
   ============================================================================ */
@Composable
private fun EnterpriseNotificationsPage(
    orders: List<CustomerOrder>,
    fcmNotifications: List<FcmPushMessageRecord>,
    dateRangeLabel: String,
    cityFilterLabel: String,
    isLoading: Boolean,
    onOrderSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var broadcastTitle by remember { mutableStateOf("⚡ Quick Delivery Express Alert") }
    var broadcastBody by remember { mutableStateOf("All active deliveries are on schedule with live GPS tracking.") }
    var selectedTargetApp by remember { mutableStateOf("ALL") }
    var targetPinInput by remember { mutableStateOf("") }
    var broadcastConfirmNote by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var targetFilter by remember { mutableStateOf("ALL") }

    val filteredPushes = remember(fcmNotifications, searchQuery, targetFilter) {
        fcmNotifications.filter { item ->
            val matchesTarget = when (targetFilter) {
                "CUSTOMER" -> item.targetRole == FcmTargetAppRole.CUSTOMER || item.targetRole == FcmTargetAppRole.BOTH
                "RIDER" -> item.targetRole == FcmTargetAppRole.RIDER || item.targetRole == FcmTargetAppRole.BOTH
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.title.contains(q, ignoreCase = true) ||
                item.body.contains(q, ignoreCase = true) ||
                item.orderId.contains(q, ignoreCase = true)
            matchesTarget && matchesQuery
        }
    }

    val headers = remember {
        listOf("Message ID", "Order ID", "Target App", "Order Status", "Title", "Body", "FCM Topic", "Timestamp")
    }
    val rows = remember(filteredPushes) {
        filteredPushes.map { p ->
            listOf(
                p.messageId,
                p.orderId,
                p.targetRole.name,
                p.orderStatus.name,
                p.title,
                p.body,
                p.fcmTopic,
                p.timestampFormatted
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_notifications_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Targeted FCM Push & Live Banner Broadcast Console
        item(key = "fcm_broadcast_console_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Targeted Real-Time Broadcast & FCM Push",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SidebarDarkTeal
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All Apps",
                            "CUSTOMER" to "Customer App",
                            "RIDER" to "All Riders",
                            "ONLINE_RIDERS" to "Online Riders Only",
                            "PIN" to "PIN-Code Zone"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedTargetApp == key,
                                onClick = { selectedTargetApp = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SidebarDarkTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (selectedTargetApp == "PIN") {
                        OutlinedTextField(
                            value = targetPinInput,
                            onValueChange = { targetPinInput = it.filter(Char::isDigit).take(6) },
                            label = { Text("Target 6-Digit PIN Code (e.g. 110001)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_broadcast_pin_input")
                        )
                    }

                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Notification Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = broadcastBody,
                        onValueChange = { broadcastBody = it },
                        label = { Text("Notification Message Body") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val sampleOrder = orders.firstOrNull()
                                val roleEnum = when (selectedTargetApp) {
                                    "CUSTOMER" -> FcmTargetAppRole.CUSTOMER
                                    "RIDER", "ONLINE_RIDERS" -> FcmTargetAppRole.RIDER
                                    else -> FcmTargetAppRole.BOTH
                                }
                                val cleanPin = targetPinInput.filter(Char::isDigit).take(6)
                                val resolvedAdminTarget = when (selectedTargetApp) {
                                    "PIN" -> if (cleanPin.isNotBlank()) "PIN:$cleanPin" else "ALL"
                                    else -> selectedTargetApp
                                }
                                val msgBody = broadcastBody.ifBlank { "New real-time order update available." }
                                val msgTitle = broadcastTitle.ifBlank { "Quick Delivery Update" }
                                FcmOrderNotificationManager.dispatchIncomingFcmMessage(
                                    context = context,
                                    orderId = sampleOrder?.orderId ?: "ORD-BROADCAST",
                                    orderStatus = sampleOrder?.orderStatus ?: OrderStatus.IN_TRANSIT,
                                    targetRole = roleEnum,
                                    title = msgTitle,
                                    body = msgBody,
                                    isDeliveryCompletion = false,
                                    fcmTopic = roleEnum.topicName
                                )
                                QuickDeliveryOperationsManager.sendBroadcastFromAdmin(
                                    context = context,
                                    message = "$msgTitle: $msgBody",
                                    target = resolvedAdminTarget
                                )
                                broadcastConfirmNote = "Targeted alert dispatched to $resolvedAdminTarget"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SidebarActiveTeal),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_fcm_send_broadcast_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Send Push",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val sampleOrder = orders.firstOrNull()
                                if (sampleOrder != null) {
                                    FcmOrderNotificationManager.dispatchOrderStatusPair(
                                        context = context,
                                        order = sampleOrder.copy(orderStatus = OrderStatus.DELIVERED),
                                        postToSystemTray = true
                                    )
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Test Delivery FCM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    broadcastConfirmNote?.let { msg ->
                        Text(
                            text = "✓ $msg",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }

        // 2. Paginated FCM Notification History Table with .xlsx Export
        item(key = "fcm_history_paginated_table") {
            AdminEnterprisePaginatedTableCard(
                title = "FCM Push Notifications Log (.xlsx Export)",
                subtitle = "Real-time Customer & Rider order status and delivery completion push notifications",
                exportFilePrefix = "QuickDelivery_FCM_Notifications",
                sheetName = "FCM_Notifications",
                headers = headers,
                allRows = rows,
                filterChips = listOf(
                    "ALL" to "All Pushes (${fcmNotifications.size})",
                    "CUSTOMER" to "Customer App",
                    "RIDER" to "Rider App"
                ),
                selectedFilterKey = targetFilter,
                onFilterSelected = { targetFilter = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search FCM push log by Order ID, Title, Message, Topic...",
                dateRangeLabel = dateRangeLabel,
                cityFilterLabel = cityFilterLabel,
                isLoading = isLoading,
                onRowClicked = { _, row ->
                    val oid = row.getOrNull(1).orEmpty().trim()
                    if (oid.isNotBlank() && !oid.contains("BROADCAST", ignoreCase = true)) {
                        onOrderSelected(oid)
                    }
                },
                testTagPrefix = "admin_notifications_table"
            )
        }
    }
}

/* ============================================================================
   PAGE 11: DEDICATED SETTINGS, ADMIN PERMISSIONS & VEHICLE PRICING PAGE
   ============================================================================ */
@Composable
private fun EnterpriseSettingsPage(
    repository: SharedOrderRepository,
    pricingConfig: PricingConfig,
    vehiclePolicy: AdminVehicleAndWeightPolicy,
    hasSensitivePiiPermission: Boolean,
    onToggleSensitivePii: (Boolean) -> Unit,
    adminSessionToken: String,
    onRefreshAdminToken: () -> Unit,
    onResetOrders: () -> Unit,
    onExitToCustomer: () -> Unit
) {
    val context = LocalContext.current
    val automationConfig by repository.automationConfig.collectAsState()
    val liveSyncStatus by MultiPhoneLiveSyncManager.syncStatus.collectAsState()
    var autoDispatchEnabled by remember(automationConfig) { mutableStateOf(automationConfig.autoMatchEligibleRiders) }
    var syncTestFeedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_settings_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Live 3-Phone Server Connectivity & Anti-Fake-Order Security Firewall Card
        item(key = "settings_live_sync_security_card") {
            val remoteFlagsForFirewall by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
            var showThreatLog by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, if (liveSyncStatus.isConnected) DashEmerald else DashCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_live_sync_security_card")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌐 Live 3-Phone Server Sync & Security Firewall",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SidebarDarkTeal
                            )
                            Text(
                                text = if (liveSyncStatus.isConnected) {
                                    "🟢 Connected (${liveSyncStatus.lastLatencyMs} ms) • Host: ${liveSyncStatus.activeEndpointHost.ifBlank { "Live Cloud Server" }}"
                                } else {
                                    "🔴 Offline / Reconnecting (${liveSyncStatus.lastSyncError.ifBlank { "Check Internet" }})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (liveSyncStatus.isConnected) DashEmerald else DashCrimson
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (liveSyncStatus.isConnected) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${liveSyncStatus.connectedPhonesCount} PHONE(S)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (liveSyncStatus.isConnected) Color(0xFF065F46) else DashCrimson,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Active Roles: ${liveSyncStatus.connectedRolesSummary} • Orders Synced: ${liveSyncStatus.syncedOrdersCount} • Last Sync: ${liveSyncStatus.lastSyncTimeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "🛡️ Firewall Blocked: Fake Orders = ${liveSyncStatus.blockedFakeOrdersCount} • Rate-Limited = ${liveSyncStatus.rateLimitedRequestsCount} • Unauthorized/Tamper = ${liveSyncStatus.rejectedInvalidPayloadsCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F766E)
                    )

                    if (syncTestFeedback != null) {
                        Text(
                            text = syncTestFeedback.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DashEmerald
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                syncTestFeedback = "✅ Live ping sent at ${liveSyncStatus.lastSyncTimeLabel} — Roles online: ${liveSyncStatus.connectedRolesSummary}"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SidebarActiveTeal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_test_3phone_sync_btn")
                        ) {
                            Text(
                                text = "Test Live Sync",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = { showThreatLog = !showThreatLog },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_toggle_threat_log_btn")
                        ) {
                            Text(
                                text = if (showThreatLog) "Hide Threat Log" else "Threat Log (${liveSyncStatus.recentBlockedEvents.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    if (showThreatLog) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_threat_log_panel")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Live Server Security Audit Trail",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SidebarDarkTeal
                                )
                                if (liveSyncStatus.recentBlockedEvents.isEmpty()) {
                                    Text(
                                        text = "No fake-order, brute-force, or unauthorized tamper events recorded yet.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                } else {
                                    liveSyncStatus.recentBlockedEvents.take(8).forEach { ev ->
                                        val digits10 = ev.actor.filter(Char::isDigit).takeLast(10)
                                        val canBlockActorPhone = digits10.length == 10 &&
                                            digits10.first() in '6'..'9' &&
                                            !remoteFlagsForFirewall.isCustomerPhoneBlocked(digits10)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "[${ev.type}] ${ev.actor}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DashCrimson
                                                )
                                                Text(
                                                    text = ev.detail,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                            if (canBlockActorPhone) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val currentBlocked = remoteFlagsForFirewall.getBlockedCustomerPhonesList()
                                                        val updatedCsv = (currentBlocked + digits10).distinct().joinToString(",")
                                                        QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(
                                                            context = context,
                                                            updated = remoteFlagsForFirewall.copy(blockedCustomerPhonesCsv = updatedCsv),
                                                            repository = repository
                                                        )
                                                        syncTestFeedback = "🚫 +91 $digits10 added to Fraud Blocklist & synced to server"
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Block", style = MaterialTheme.typography.labelSmall, color = DashCrimson, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFE2E8F0))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Admin RBAC Permissions & Sensitive PII Protection Card
        item(key = "settings_rbac_permissions_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
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
                                text = "Admin RBAC Permissions & Sensitive Data Protection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SidebarDarkTeal
                            )
                            Text(
                                text = "Active Session Token: ${adminSessionToken.take(18)}... • Protects Customer/Rider Phones, Aadhaar, PAN, DL & Bank Accounts",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                        Switch(
                            checked = hasSensitivePiiPermission,
                            onCheckedChange = onToggleSensitivePii,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DashEmerald
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefreshAdminToken,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rotate Admin Session Token", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onToggleSensitivePii(!hasSensitivePiiPermission) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (hasSensitivePiiPermission) "Mask All Sensitive PII" else "Unmask PII (Admin Auth)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Live Vehicle & Fare Pricing Configuration (Bike, Scooter, Auto + 20 kg Limit)
        item(key = "settings_vehicle_pricing_card") {
            AdminVehiclePricingAndWeightControlCard(
                repository = repository,
                pricingConfig = pricingConfig,
                vehiclePolicy = vehiclePolicy
            )
        }

        // 2B. Dynamic Customer Promo Coupon Manager (Synced Live to Customer Checkout)
        item(key = "settings_promo_coupon_manager") {
            AdminPromoCouponManagerCard()
        }

        // 2C. Master Feature Switches (Scheduled, Multi-Drop, Surge, Insurance, Referral, COD Cap, Commission %, Maintenance, Broadcast & PIN)
        item(key = "settings_remote_feature_switches") {
            AdminRemoteFeatureSwitchesCard(repository = repository)
        }

        // 2D. Peak-Hour Auto-Surge Scheduler + Porter Helper & Batch Controls
        item(key = "settings_peak_auto_surge") {
            AdminPeakHourAutoSurgeCard(repository = repository)
        }

        // 3. System Automation, Safety Guards & Administrative Actions
        item(key = "settings_system_guards_and_reset") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Autonomous Dispatch, Safety Guards & Session Control",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SidebarDarkTeal
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Zero-Partiality Fair Auto-Dispatch", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Round-robin fair assignment across online riders within 5 km & <₹200 COD limit", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = autoDispatchEnabled,
                            onCheckedChange = { newVal ->
                                autoDispatchEnabled = newVal
                                val token = repository.issueAdminPreviewSessionToken()
                                scope.launch {
                                    repository.updateAdminAutomationConfig(
                                        automationConfig.copy(autoMatchEligibleRiders = newVal),
                                        token
                                    )
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onResetOrders,
                            colors = ButtonDefaults.buttonColors(containerColor = DashCrimson),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Orders Archive", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onExitToCustomer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sign Out Admin", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
