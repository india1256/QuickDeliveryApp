package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.security.AppPermissionManager
import com.example.security.OrderAlertSoundManager
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.components.AppRemoteUpdateGateOverlay
import com.example.ui.components.FcmOrderNotificationManager
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.rider.RiderPartnerCompactScreen
import com.example.ui.theme.QuickDeliveryTheme

/**
 * Standalone Rider Partner Application Activity ("Partner Quick Delivery").
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
        QuickDeliveryOperationsManager.ensureLoaded(this)
        OrderAlertSoundManager.ensureChannels(this)
        AppPermissionManager.inspectPermissions(this)
        MultiPhoneLiveSyncManager.resolveLockedPhoneRole(this, "RIDER")
        MultiPhoneLiveSyncManager.startBackgroundMultiPhoneSync(this, "RIDER")
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
                        RiderPartnerCompactScreen(modifier = Modifier.fillMaxSize())
                        AppRemoteUpdateGateOverlay(
                            appRole = "RIDER",
                            currentVersionName = BuildConfig.VERSION_NAME,
                            currentVersionCode = BuildConfig.VERSION_CODE
                        )
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
