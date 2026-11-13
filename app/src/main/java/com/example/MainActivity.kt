package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.example.security.ApkDownloadOutcome
import com.example.security.MobileApkDownloadHelper
import com.example.security.MobileApkVariant
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SharedOrderRepository
import com.example.security.AppPermissionManager
import com.example.security.OrderAlertSoundManager
import com.example.security.RiderBatteryPowerMonitorCard
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.rider.RiderPartnerCompactScreen
import com.example.ui.screens.CustomerAppScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.theme.QuickDeliveryTheme

import androidx.compose.material.icons.filled.MoreVert
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.auth.QuickDeliveryTopOptionsDialog
import com.example.ui.components.AdminEnterpriseControlCenter
import com.example.ui.components.AdminMutualRatingsAndSuggestionsPanel
import com.example.ui.components.AdminProfessionalLiveMirrorScreen
import com.example.ui.components.AdminSosSupportLiveBanner
import com.example.ui.components.AdminWebExecutiveLiveRibbon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val wrapped = runCatching { AppLanguageManager.wrapActivityBaseContext(newBase) }.getOrDefault(newBase)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPermissionManager.installCrashPreventionGuard()
        OrderAlertSoundManager.ensureChannels(this)
        AppPermissionManager.inspectPermissions(this)
        com.example.data.cloud.MultiPhoneLiveSyncManager.resolveLockedPhoneRole(this, BuildConfig.APP_ROLE)
        com.example.data.cloud.MultiPhoneLiveSyncManager.startBackgroundMultiPhoneSync(this, BuildConfig.APP_ROLE)
        enableEdgeToEdge()

        setContent {
            QuickDeliveryTheme {
                LocalizedContextProvider {
                    val activeRole by com.example.data.cloud.MultiPhoneLiveSyncManager.lockedPhoneRole.collectAsState()
                    val effectiveRole = if (BuildConfig.APP_ROLE != "UNIFIED") BuildConfig.APP_ROLE else activeRole

                    when (effectiveRole) {
                        "CUSTOMER" -> {
                            BackHandler(enabled = true) {
                                OrderAlertSoundManager.stopOrderAlert()
                            }
                            QuickDeliveryCloudSyncBar(showCardUi = false)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                CustomerAppScreen()
                            }
                        }
                        "RIDER" -> {
                            BackHandler(enabled = true) {
                                OrderAlertSoundManager.stopOrderAlert()
                            }
                            QuickDeliveryCloudSyncBar(showCardUi = false)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                RiderPartnerCompactScreen(modifier = Modifier.fillMaxSize())
                            }
                        }
                        "ADMIN" -> {
                            val repository = remember {
                                SharedOrderRepository.getInstance(this@MainActivity).also { repo ->
                                    com.example.data.delivery.AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
                                }
                            }
                            val orders by repository.orders.collectAsState()
                            BackHandler(enabled = true) {
                                OrderAlertSoundManager.stopOrderAlert()
                            }
                            QuickDeliveryCloudSyncBar(showCardUi = false)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                AdminLivePermissionsAndTrackingBar()
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    AdminEnterpriseControlCenter(
                                        repository = repository,
                                        orders = orders,
                                        onResetOrders = {
                                            repository.resetOrdersAndStateForTesting()
                                            com.example.data.cloud.MultiPhoneLiveSyncManager.notifyOrdersReset(this@MainActivity)
                                        },
                                        onExitToCustomer = {}
                                    )
                                }
                            }
                        }
                        else -> QuickDeliveryUnifiedHub()
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLivePermissionsAndTrackingBar(
    onClearAllOrders: () -> Unit = {}
) {
    val context = LocalContext.current
    val syncStatus by com.example.data.cloud.MultiPhoneLiveSyncManager.syncStatus.collectAsState()
    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    val hasLocationPerm = remember(permissionRefreshTick) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val hasCameraPerm = remember(permissionRefreshTick) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val hasNotificationPerm = remember(permissionRefreshTick) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val allAdminPermsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefreshTick++
    }

    val allGranted = hasLocationPerm &&
        hasCameraPerm &&
        hasNotificationPerm

    Surface(
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_live_permissions_tracking_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Fleet Tracking (${syncStatus.connectedPhonesCount} Connected • ${syncStatus.syncedOrdersCount} Orders)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "GPS: ${if (hasLocationPerm) "Active" else "Off"} • Alerts: ${if (hasNotificationPerm) "Active" else "Off"} • Camera: ${if (hasCameraPerm) "Ready" else "Off"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                if (!allGranted) {
                    OutlinedButton(
                        onClick = {
                            val perms = mutableListOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                android.Manifest.permission.CAMERA
                            )
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            allAdminPermsLauncher.launch(perms.toTypedArray())
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_grant_all_permissions_btn")
                    ) {
                        Text(
                            "Allow Permissions",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultiPhoneLiveSyncTopRibbon(currentRole: String) {
    val context = LocalContext.current
    val syncStatus by com.example.data.cloud.MultiPhoneLiveSyncManager.syncStatus.collectAsState()
    var showRoleSwitcher by rememberSaveable { mutableStateOf(false) }

    val roleBadgeText = when (currentRole) {
        "CUSTOMER" -> "🛍️ Customer App"
        "RIDER" -> "🛵 Rider Partner App"
        "ADMIN" -> "🛡️ Admin Live Console"
        else -> "⚡ All-In-One Hub"
    }

    Surface(
        color = Color(0xFF022C22),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$roleBadgeText • ${if (syncStatus.isConnected) "🟢 Live (${syncStatus.connectedPhonesCount} Phones)" else "🟡 Connecting..."}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6EE7B7)
                    )
                    Text(
                        text = "Active Orders: ${syncStatus.syncedOrdersCount} • Sync: ${syncStatus.lastSyncTimeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA7F3D0)),
                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("live_sync_now_btn")
                    ) {
                        Text("🔄 Sync", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (BuildConfig.APP_ROLE == "UNIFIED") {
                        OutlinedButton(
                            onClick = { showRoleSwitcher = !showRoleSwitcher },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDE68A)),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("change_locked_phone_role_btn")
                        ) {
                            Text("📱 Role", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showRoleSwitcher) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            com.example.data.cloud.MultiPhoneLiveSyncManager.setLockedPhoneRole(context, "CUSTOMER")
                            showRoleSwitcher = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA7F3D0)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🛍️ Customer", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            com.example.data.cloud.MultiPhoneLiveSyncManager.setLockedPhoneRole(context, "RIDER")
                            showRoleSwitcher = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDE68A)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🛵 Rider", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            com.example.data.cloud.MultiPhoneLiveSyncManager.setLockedPhoneRole(context, "ADMIN")
                            showRoleSwitcher = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF99F6E4)),
                        border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🛡️ Admin", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            com.example.data.cloud.MultiPhoneLiveSyncManager.setLockedPhoneRole(context, "UNIFIED")
                            showRoleSwitcher = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡ Hub", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickDeliveryUnifiedHub() {
    val context = LocalContext.current
    val repository = remember {
        SharedOrderRepository.getInstance(context).also { repo ->
            com.example.data.delivery.AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
        }
    }
    val orders by repository.orders.collectAsState()
    var selectedAppTab by rememberSaveable { mutableIntStateOf(0) }
    val tabHistoryStack = remember { mutableStateListOf<Int>() }

    val navigateToTab: (Int) -> Unit = { targetTab ->
        if (targetTab != selectedAppTab) {
            tabHistoryStack.add(selectedAppTab)
            selectedAppTab = targetTab
        }
    }

    // Root-level BackHandler: pops tab history stack smoothly and prevents Activity finish/recreation at the root screen.
    // Child composables (CustomerAppScreen, RiderPartnerCompactScreen, AdminEnterpriseControlCenter) register their own
    // inner BackHandlers after this one, so inner sub-screens/dialogs/steps always pop first in LIFO order.
    BackHandler(enabled = true) {
        OrderAlertSoundManager.stopOrderAlert()
        if (tabHistoryStack.isNotEmpty()) {
            selectedAppTab = tabHistoryStack.removeAt(tabHistoryStack.lastIndex)
        } else if (selectedAppTab != 0) {
            selectedAppTab = 0
        }
    }

    // Keep real-time Cloud Firestore & ParcelDeliveryStateManager sync active in the background with 0dp height
    QuickDeliveryCloudSyncBar(showCardUi = false)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedAppTab == 0,
                    onClick = { navigateToTab(0) },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Customer App") },
                    label = { Text("Customer") },
                    modifier = Modifier.testTag("switch_customer_app_tab")
                )
                NavigationBarItem(
                    selected = selectedAppTab == 1,
                    onClick = { navigateToTab(1) },
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = "Rider App") },
                    label = { Text("Rider Partner") },
                    modifier = Modifier.testTag("switch_rider_app_tab")
                )
                NavigationBarItem(
                    selected = selectedAppTab == 2,
                    onClick = { navigateToTab(2) },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Web") },
                    label = { Text("Admin Web") },
                    modifier = Modifier.testTag("switch_admin_app_tab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            var showApkDownloadPanel by rememberSaveable { mutableStateOf(false) }
            var selectedVariantForSaf by remember { mutableStateOf(MobileApkVariant.CUSTOMER) }
            var lastDownloadOutcome by remember { mutableStateOf<ApkDownloadOutcome?>(null) }

            val safCreateDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
            ) { destinationUri: Uri? ->
                if (destinationUri != null) {
                    val result = MobileApkDownloadHelper.writeApkToSafUri(context, destinationUri)
                    if (result.isSuccess) {
                        val mb = String.format("%.1f MB", (result.getOrDefault(0L)).toDouble() / (1024.0 * 1024.0))
                        lastDownloadOutcome = ApkDownloadOutcome(
                            fileName = selectedVariantForSaf.fileName,
                            fileSizeBytes = result.getOrDefault(0L),
                            savedDownloadsUri = destinationUri,
                            browserDownloadUrl = "",
                            statusMessage = "✅ ${selectedVariantForSaf.fileName} ($mb) saved to selected folder!"
                        )
                    }
                }
            }

            // Direct 3 Separate APK Download & Standalone App Launcher Banner (collapsed by default to keep screen clean)
            Surface(
                color = Color(0xFF042F2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📲 Multi-Phone Live Sync & Role Selector",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showApkDownloadPanel = !showApkDownloadPanel },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFA7F3D0)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("toggle_direct_apk_download_panel_btn")
                        ) {
                            Text(
                                if (showApkDownloadPanel) "Hide Options" else "📱 Phone Role / APKs",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AnimatedVisibility(visible = showApkDownloadPanel) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedVariantForSaf = MobileApkVariant.CUSTOMER
                                        lastDownloadOutcome = MobileApkDownloadHelper.triggerApkDownload(
                                            context = context,
                                            variant = MobileApkVariant.CUSTOMER,
                                            openMobileBrowser = true
                                        )
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF059669),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_customer_apk_btn")
                                ) {
                                    Text(
                                        "📥 Customer.apk",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedVariantForSaf = MobileApkVariant.RIDER
                                        lastDownloadOutcome = MobileApkDownloadHelper.triggerApkDownload(
                                            context = context,
                                            variant = MobileApkVariant.RIDER,
                                            openMobileBrowser = true
                                        )
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFFD97706),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_rider_apk_btn")
                                ) {
                                    Text(
                                        "📥 Rider.apk",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedVariantForSaf = MobileApkVariant.ADMIN
                                        lastDownloadOutcome = MobileApkDownloadHelper.triggerApkDownload(
                                            context = context,
                                            variant = MobileApkVariant.ADMIN,
                                            openMobileBrowser = true
                                        )
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF0D9488),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_admin_apk_btn")
                                ) {
                                    Text(
                                        "📥 Admin.apk",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }
                            }

                            lastDownloadOutcome?.let { outcome ->
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF34D399)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("apk_download_status_banner")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = outcome.statusMessage,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFECFDF5)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    safCreateDocumentLauncher.launch(selectedVariantForSaf.fileName)
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color(0xFF10B981),
                                                    contentColor = Color(0xFF042F2E)
                                                ),
                                                border = BorderStroke(1.dp, Color.White),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("save_apk_custom_folder_btn")
                                            ) {
                                                Text(
                                                    "📂 Save As Folder",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    MobileApkDownloadHelper.shareApkFile(context, selectedVariantForSaf)
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color(0xFF0F172A),
                                                    contentColor = Color(0xFFA7F3D0)
                                                ),
                                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("share_apk_whatsapp_btn")
                                            ) {
                                                Text(
                                                    "📤 Share / Send APK",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        com.example.data.cloud.MultiPhoneLiveSyncManager.setLockedPhoneRole(context, "CUSTOMER")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA7F3D0)),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_standalone_customer_activity_btn")
                                ) {
                                    Text("🛍️ Customer Only", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        com.example.data.cloud.MultiPhoneLiveSyncManager.setLockedPhoneRole(context, "RIDER")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDE68A)),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_standalone_rider_activity_btn")
                                ) {
                                    Text("🛵 Rider Only", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        com.example.data.cloud.MultiPhoneLiveSyncManager.setLockedPhoneRole(context, "ADMIN")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF99F6E4)),
                                    border = BorderStroke(1.dp, Color(0xFF99F6E4).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_standalone_admin_activity_btn")
                                ) {
                                    Text("🛡️ Admin Only", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            if (selectedAppTab == 2) {
                AdminLivePermissionsAndTrackingBar(
                    onClearAllOrders = {
                        repository.resetOrdersAndStateForTesting()
                        com.example.data.cloud.MultiPhoneLiveSyncManager.notifyOrdersReset(context)
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedAppTab) {
                    0 -> CustomerAppScreen()
                    1 -> RiderPartnerCompactScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> AdminEnterpriseControlCenter(
                        repository = repository,
                        orders = orders,
                        onResetOrders = {
                            repository.resetOrdersAndStateForTesting()
                            com.example.data.cloud.MultiPhoneLiveSyncManager.notifyOrdersReset(context)
                        },
                        onExitToCustomer = {
                            if (tabHistoryStack.isNotEmpty()) {
                                selectedAppTab = tabHistoryStack.removeAt(tabHistoryStack.lastIndex)
                            } else {
                                selectedAppTab = 0
                            }
                        }
                    )
                }
            }
        }
    }
}
