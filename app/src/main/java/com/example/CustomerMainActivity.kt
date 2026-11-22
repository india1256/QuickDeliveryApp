package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.cloud.QuickDeliveryOperationsManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.security.AppPermissionManager
import com.example.security.LiveNetworkStatusMonitor
import com.example.security.OrderAlertSoundManager
import com.example.security.QuickDeliveryCrashShield
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.auth.QuickDeliveryTopOptionsDialog
import com.example.ui.components.AppRemoteUpdateGateOverlay
import com.example.ui.components.CrashRecoveryDialogOverlay
import com.example.ui.components.FcmOrderNotificationManager
import com.example.ui.components.LiveNetworkStatusBanner
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.screens.CustomerAppScreen
import com.example.ui.theme.QuickDeliveryTheme

/**
 * Standalone Customer Application Activity ("Quick Delivery").
 */
class CustomerMainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val wrapped = runCatching { AppLanguageManager.wrapActivityBaseContext(newBase) }.getOrDefault(newBase)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPermissionManager.installCrashPreventionGuard()
        QuickDeliveryCrashShield.install(this, "CUSTOMER", BuildConfig.VERSION_NAME)
        LiveNetworkStatusMonitor.startMonitoring(this)
        QuickDeliveryFirebaseInitializer.ensureInitialized(this)
        QuickDeliveryOperationsManager.ensureLoaded(this)
        OrderAlertSoundManager.ensureChannels(this)
        OrderAlertSoundManager.stopOrderAlert()
        AppPermissionManager.inspectPermissions(this)
        MultiPhoneLiveSyncManager.resolveLockedPhoneRole(this, "CUSTOMER")
        MultiPhoneLiveSyncManager.startBackgroundMultiPhoneSync(this, "CUSTOMER")
        FcmOrderNotificationManager.handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            QuickDeliveryTheme {
                LocalizedContextProvider {
                    QuickDeliveryCloudSyncBar(showCardUi = false)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LiveNetworkStatusBanner()
                            Box(modifier = Modifier.weight(1f)) {
                                CustomerAppScreen()
                            }
                        }
                        AppRemoteUpdateGateOverlay(
                            appRole = "CUSTOMER",
                            currentVersionName = BuildConfig.VERSION_NAME,
                            currentVersionCode = BuildConfig.VERSION_CODE
                        )
                        CrashRecoveryDialogOverlay(appRole = "CUSTOMER")
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        FcmOrderNotificationManager.handleLaunchIntent(intent)
        MultiPhoneLiveSyncManager.triggerImmediateSync(this)
    }

    override fun onResume() {
        super.onResume()
        MultiPhoneLiveSyncManager.triggerImmediateSync(this)
        FcmOrderNotificationManager.handleLaunchIntent(intent)
    }
}
