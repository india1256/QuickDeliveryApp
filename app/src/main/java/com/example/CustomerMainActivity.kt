package com.example

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
import com.example.security.AppPermissionManager
import com.example.security.OrderAlertSoundManager
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.auth.QuickDeliveryTopOptionsDialog
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
        QuickDeliveryFirebaseInitializer.ensureInitialized(this)
        OrderAlertSoundManager.ensureChannels(this)
        AppPermissionManager.inspectPermissions(this)
        enableEdgeToEdge()
        setContent {
            QuickDeliveryTheme {
                LocalizedContextProvider {
                    BackHandler(enabled = true) {
                        OrderAlertSoundManager.stopOrderAlert()
                    }
                    QuickDeliveryCloudSyncBar(showCardUi = false)
                    Box(modifier = Modifier.fillMaxSize()) {
                        CustomerAppScreen()
                    }
                }
            }
        }
    }
}
