package com.example.rider

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.repository.SharedOrderRepository
import com.example.security.AppPermissionManager
import com.example.security.OrderAlertSoundManager
import com.example.security.RiderBatteryPowerMonitorCard
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.auth.QuickDeliveryTopOptionsDialog
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.rider.RiderPartnerCompactScreen

private val RiderColorScheme = lightColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color.White,
    secondary = Color(0xFF00A896),
    onSecondary = Color.White,
    background = Color(0xFFF4F8F9),
    surface = Color.White
)

/**
 * Standalone Rider Partner Application Activity ("Quick Delivery Partner App").
 * Strictly separate from Customer and Admin applications.
 */
class RiderMainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val wrapped = runCatching { AppLanguageManager.wrapActivityBaseContext(newBase) }.getOrDefault(newBase)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPermissionManager.installCrashPreventionGuard()
        QuickDeliveryFirebaseInitializer.ensureInitialized(this)
        OrderAlertSoundManager.ensureChannels(this)
        AppPermissionManager.inspectPermissions(this)
        enableEdgeToEdge()
        setContent {
            val repository = remember { SharedOrderRepository.getInstance(this) }
            val orders by repository.orders.collectAsState()
            var showThreeDotsMenu by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = RiderColorScheme) {
                LocalizedContextProvider {
                    BackHandler(enabled = true) {
                        OrderAlertSoundManager.stopOrderAlert()
                    }
                    QuickDeliveryCloudSyncBar(showCardUi = false)
                    if (showThreeDotsMenu) {
                        QuickDeliveryTopOptionsDialog(
                            onDismiss = { showThreeDotsMenu = false },
                            extraTopContent = {
                                if (orders.isNotEmpty()) {
                                    RiderBatteryPowerMonitorCard(
                                        activeOrders = orders,
                                        onEmergencyReassignOrder = { activeOrder ->
                                            repository.releaseAssignedOrderBeforePickup(
                                                orderId = activeOrder.orderId,
                                                riderId = activeOrder.assignedRiderId ?: "RIDER-101",
                                                reason = "Emergency Critical Battery Reassignment (≤20% PowerManager Guard)"
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                    RiderPartnerCompactScreen(
                        modifier = Modifier.fillMaxSize(),
                        onOpenThreeDotsMenu = { showThreeDotsMenu = true }
                    )
                }
            }
        }
    }
}
