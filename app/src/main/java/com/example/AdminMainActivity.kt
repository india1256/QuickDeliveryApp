package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.repository.SharedOrderRepository
import com.example.security.AppPermissionManager
import com.example.security.OrderAlertSoundManager
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.components.AdminEnterpriseControlCenter
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.theme.QuickDeliveryTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Standalone Admin Enterprise Application Activity ("Quick Delivery Admin").
 * Gated behind Allowlisted Admin Sign-In for production Play Store readiness.
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
        QuickDeliveryOperationsManager.ensureLoaded(this)
        OrderAlertSoundManager.ensureChannels(this)
        OrderAlertSoundManager.stopOrderAlert()
        AppPermissionManager.inspectPermissions(this)
        MultiPhoneLiveSyncManager.resolveLockedPhoneRole(this, "ADMIN")
        MultiPhoneLiveSyncManager.startBackgroundMultiPhoneSync(this, "ADMIN")
        enableEdgeToEdge()
        setContent {
            QuickDeliveryTheme {
                LocalizedContextProvider {
                    val signedInAdminEmail by QuickDeliveryOperationsManager.signedInAdminEmail.collectAsState()
                    val isAdminAuthorized = QuickDeliveryOperationsManager.isEmailAllowlistedForAdmin(signedInAdminEmail)

                    QuickDeliveryCloudSyncBar(showCardUi = false)

                    if (!isAdminAuthorized) {
                        AdminAllowlistSecurityGateScreen(
                            onAdminAuthenticated = { verifiedEmail ->
                                QuickDeliveryOperationsManager.saveSignedInAdminSession(
                                    this@AdminMainActivity,
                                    verifiedEmail
                                )
                            }
                        )
                    } else {
                        val repository = remember {
                            SharedOrderRepository.getInstance(this@AdminMainActivity).also { repo ->
                                AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
                            }
                        }
                        val orders by repository.orders.collectAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            AdminLivePermissionsAndTrackingBar()
                            Box(modifier = Modifier.weight(1f)) {
                                AdminEnterpriseControlCenter(
                                    repository = repository,
                                    orders = orders,
                                    onResetOrders = {
                                        repository.resetOrdersAndStateForTesting()
                                        MultiPhoneLiveSyncManager.notifyOrdersReset(this@AdminMainActivity)
                                    },
                                    onExitToCustomer = {
                                        runCatching { Firebase.auth.signOut() }
                                        QuickDeliveryOperationsManager.signOutAdminSession(this@AdminMainActivity)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAllowlistSecurityGateScreen(
    onAdminAuthenticated: (String) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A2E36))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF0F766E)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_allowlist_login_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(0xFF0F766E).copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Security",
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Text(
                    text = "Quick Delivery Admin Console",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Authorized Ops Sign-In Required • Protected Operations, KYC & Settlement Access",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )

                HorizontalDivider()

                GoogleSignInButton(
                    onAuthSuccess = {
                        val googleEmail = runCatching { Firebase.auth.currentUser?.email }.getOrNull()
                        if (QuickDeliveryOperationsManager.isEmailAllowlistedForAdmin(googleEmail)) {
                            authError = null
                            onAdminAuthenticated(googleEmail.orEmpty())
                        } else {
                            runCatching { Firebase.auth.signOut() }
                            authError = "Access Denied (${googleEmail ?: "Unknown"}): Not in Quick Delivery Admin allowlist."
                        }
                    },
                    onAuthError = { msg ->
                        authError = msg
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  OR VERIFY ADMIN EMAIL  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        authError = null
                    },
                    label = { Text("Authorized Admin Email *") },
                    placeholder = { Text("admin@quickdelivery.in") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0F766E))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_login_email_input")
                )

                authError?.let { err ->
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val clean = emailInput.trim().lowercase()
                        if (QuickDeliveryOperationsManager.isEmailAllowlistedForAdmin(clean)) {
                            authError = null
                            onAdminAuthenticated(clean)
                        } else {
                            authError = "Access Denied: Only allowlisted Admin emails are permitted."
                        }
                    },
                    enabled = emailInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_verify_email_signin_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In to Admin Console", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
