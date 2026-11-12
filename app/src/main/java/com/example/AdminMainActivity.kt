package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.repository.SharedOrderRepository
import com.example.security.AppPermissionManager
import com.example.security.OrderAlertSoundManager
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.components.AdminEnterpriseControlCenter
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.theme.QuickDeliveryTheme

/**
 * Standalone Admin Enterprise Application Activity ("QD Admin Console").
 */
class AdminMainActivity : ComponentActivity() {
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
        com.example.data.cloud.MultiPhoneLiveSyncManager.startBackgroundMultiPhoneSync(this, "ADMIN")
        enableEdgeToEdge()
        setContent {
            QuickDeliveryTheme {
                LocalizedContextProvider {
                    val repository = remember {
                        SharedOrderRepository.getInstance(this@AdminMainActivity).also { repo ->
                            AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
                        }
                    }
                    val orders by repository.orders.collectAsState()
                    BackHandler(enabled = true) {
                        OrderAlertSoundManager.stopOrderAlert()
                    }
                    QuickDeliveryCloudSyncBar(showCardUi = false)
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AdminLivePermissionsAndTrackingBar(
                            onClearAllOrders = {
                                repository.resetOrdersAndStateForTesting()
                                com.example.data.cloud.MultiPhoneLiveSyncManager.notifyOrdersReset(this@AdminMainActivity)
                            }
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            AdminEnterpriseControlCenter(
                                repository = repository,
                                orders = orders,
                                onResetOrders = {
                                    repository.resetOrdersAndStateForTesting()
                                    com.example.data.cloud.MultiPhoneLiveSyncManager.notifyOrdersReset(this@AdminMainActivity)
                                },
                                onExitToCustomer = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
