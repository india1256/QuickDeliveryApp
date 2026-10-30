package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
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
        enableEdgeToEdge()

        setContent {
            QuickDeliveryTheme {
                LocalizedContextProvider {
                    when (BuildConfig.APP_ROLE) {
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                AdminEnterpriseControlCenter(
                                    repository = repository,
                                    orders = orders,
                                    onResetOrders = { repository.resetOrdersAndStateForTesting() },
                                    onExitToCustomer = {}
                                )
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
            // Live Web App URL & One-Tap Chrome Launcher Card
            Surface(
                color = Color(0xFF042F2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🌐 Web App Live Link",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA7F3D0)
                        )
                        Text(
                            text = "https://ais-dev-l6kwxrdc54lqesfozowtv2-809441526823.asia-southeast1.run.app",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://ais-dev-l6kwxrdc54lqesfozowtv2-809441526823.asia-southeast1.run.app")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFA7F3D0)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open Chrome", fontWeight = FontWeight.Bold)
                    }
                }
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
                        onResetOrders = { repository.resetOrdersAndStateForTesting() },
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
