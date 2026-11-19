package com.example

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.repository.SharedOrderRepository
import com.example.security.AdminCredentialsManager
import com.example.security.AppPermissionManager
import com.example.security.LiveNetworkStatusMonitor
import com.example.security.OrderAlertSoundManager
import com.example.security.QuickDeliveryCrashShield
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.auth.QuickDeliveryCloudSyncBar
import com.example.ui.components.AdminEnterpriseControlCenter
import com.example.ui.components.CrashRecoveryDialogOverlay
import com.example.ui.components.FcmOrderNotificationManager
import com.example.ui.components.LiveNetworkStatusBanner
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.theme.QuickDeliveryTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Standalone Admin Enterprise Application Activity ("Quick Delivery Admin").
 * Gated behind User ID + Password authentication with allowlisted Email OTP / Google recovery.
 */
class AdminMainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val wrapped = runCatching { AppLanguageManager.wrapActivityBaseContext(newBase) }.getOrDefault(newBase)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPermissionManager.installCrashPreventionGuard()
        QuickDeliveryCrashShield.install(this, "ADMIN", BuildConfig.VERSION_NAME)
        LiveNetworkStatusMonitor.startMonitoring(this)
        QuickDeliveryFirebaseInitializer.ensureInitialized(this)
        QuickDeliveryOperationsManager.ensureLoaded(this)
        AdminCredentialsManager.ensureLoaded(this)
        OrderAlertSoundManager.ensureChannels(this)
        OrderAlertSoundManager.stopOrderAlert()
        AppPermissionManager.inspectPermissions(this)
        MultiPhoneLiveSyncManager.resolveLockedPhoneRole(this, "ADMIN")
        MultiPhoneLiveSyncManager.startBackgroundMultiPhoneSync(this, "ADMIN")
        FcmOrderNotificationManager.handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            QuickDeliveryTheme {
                LocalizedContextProvider {
                    val signedInAdminEmail by QuickDeliveryOperationsManager.signedInAdminEmail.collectAsState()
                    var sessionAdminEmail by remember { mutableStateOf<String?>(null) }
                    val activeAdminEmail = signedInAdminEmail ?: sessionAdminEmail
                    val remoteFlags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
                    val isAdminAuthorized = QuickDeliveryOperationsManager.isEmailAllowlistedForAdmin(activeAdminEmail)
                    var unlockedAdminPin by remember(activeAdminEmail) { mutableStateOf("") }
                    val requiresPinUnlock = remoteFlags.adminSecurityPin.length == 4 &&
                        unlockedAdminPin != remoteFlags.adminSecurityPin

                    QuickDeliveryCloudSyncBar(showCardUi = false)
                    CrashRecoveryDialogOverlay(appRole = "ADMIN")

                    if (!isAdminAuthorized) {
                        AdminCredentialsAuthScreen(
                            onAdminAuthenticated = { verifiedEmail, keepSignedIn ->
                                sessionAdminEmail = verifiedEmail
                                if (keepSignedIn) {
                                    QuickDeliveryOperationsManager.saveSignedInAdminSession(
                                        this@AdminMainActivity,
                                        verifiedEmail
                                    )
                                } else {
                                    QuickDeliveryOperationsManager.signOutAdminSession(this@AdminMainActivity)
                                }
                            }
                        )
                    } else if (requiresPinUnlock) {
                        AdminSecurityPinUnlockScreen(
                            expectedPin = remoteFlags.adminSecurityPin,
                            onPinUnlocked = { unlockedAdminPin = remoteFlags.adminSecurityPin },
                            onSignOut = {
                                unlockedAdminPin = ""
                                sessionAdminEmail = null
                                runCatching { Firebase.auth.signOut() }
                                QuickDeliveryOperationsManager.signOutAdminSession(this@AdminMainActivity)
                            }
                        )
                    } else {
                        val repository = remember {
                            SharedOrderRepository.getInstance(this@AdminMainActivity).also { repo ->
                                AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
                            }
                        }
                        val orders by repository.orders.collectAsState()
                        val hasAdminPerms = remember(this@AdminMainActivity) {
                            val fine = androidx.core.content.ContextCompat.checkSelfPermission(
                                this@AdminMainActivity,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val cam = androidx.core.content.ContextCompat.checkSelfPermission(
                                this@AdminMainActivity,
                                android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            fine && cam
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            LiveNetworkStatusBanner()
                            if (!hasAdminPerms) {
                                AdminLivePermissionsAndTrackingBar()
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AdminEnterpriseControlCenter(
                                    repository = repository,
                                    orders = orders,
                                    onResetOrders = {
                                        repository.resetOrdersAndStateForTesting()
                                        MultiPhoneLiveSyncManager.notifyOrdersReset(this@AdminMainActivity)
                                    },
                                    onExitToCustomer = {
                                        unlockedAdminPin = ""
                                        sessionAdminEmail = null
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

private enum class AdminAuthMode {
    SIGN_IN,
    SETUP,
    RESET
}

@Composable
private fun AdminCredentialsAuthScreen(
    onAdminAuthenticated: (verifiedEmail: String, keepSignedIn: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credsState by AdminCredentialsManager.credentialsState.collectAsState()

    var remoteStatus by remember { mutableStateOf<MultiPhoneLiveSyncManager.AdminRemoteCredStatus?>(null) }
    var hasManualModeSwitch by remember { mutableStateOf(false) }
    var authMode by remember {
        mutableStateOf(if (credsState.isConfigured) AdminAuthMode.SIGN_IN else AdminAuthMode.SETUP)
    }

    // Sign-In state
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }
    var keepSignedIn by remember { mutableStateOf(true) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutUntilMs by remember { mutableLongStateOf(0L) }
    var remainingLockSec by remember { mutableIntStateOf(0) }

    // Setup / Reset verification state
    val defaultOwnerEmail = remember(credsState.recoveryEmail) {
        credsState.recoveryEmail.ifBlank { QuickDeliveryOperationsManager.ALLOWED_ADMIN_EMAILS.first() }
    }
    var emailInput by remember(defaultOwnerEmail) { mutableStateOf(defaultOwnerEmail) }
    var otpInput by remember { mutableStateOf("") }
    var otpSentToEmail by remember { mutableStateOf<String?>(null) }
    var resendCooldownSec by remember { mutableIntStateOf(0) }
    var verifiedOwnerEmail by remember { mutableStateOf<String?>(null) }
    var verifiedProofType by remember { mutableStateOf("EMAIL_OTP") }

    // Step 2: Create / Reset credentials state
    var setupUserId by remember(credsState.userId) { mutableStateOf(credsState.userId) }
    var setupPassword by remember { mutableStateOf("") }
    var setupConfirmPassword by remember { mutableStateOf("") }
    var showSetupPassword by remember { mutableStateOf(false) }
    var showSetupConfirmPassword by remember { mutableStateOf(false) }

    // Feedback & busy state
    var isBusy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authInfo by remember { mutableStateOf<String?>(null) }

    val hasAnyConfigured = credsState.isConfigured || (remoteStatus?.configured == true)
    val isLockedOut = remainingLockSec > 0

    LaunchedEffect(Unit) {
        AdminCredentialsManager.ensureLoaded(context)
        val localReady = AdminCredentialsManager.credentialsState.value.isConfigured
        val serverRes = MultiPhoneLiveSyncManager.fetchAdminCredentialsStatus(context).getOrNull()
        remoteStatus = serverRes
        if (!hasManualModeSwitch) {
            authMode = if (localReady || serverRes?.configured == true) {
                AdminAuthMode.SIGN_IN
            } else {
                AdminAuthMode.SETUP
            }
        }
        if (setupUserId.isBlank() && !serverRes?.userId.isNullOrBlank()) {
            setupUserId = serverRes?.userId.orEmpty()
        }
    }

    LaunchedEffect(lockoutUntilMs) {
        while (lockoutUntilMs > System.currentTimeMillis()) {
            remainingLockSec = (((lockoutUntilMs - System.currentTimeMillis()) + 999L) / 1000L)
                .toInt()
                .coerceAtLeast(1)
            delay(1000L)
        }
        remainingLockSec = 0
    }

    LaunchedEffect(resendCooldownSec) {
        if (resendCooldownSec > 0) {
            delay(1000L)
            resendCooldownSec -= 1
        }
    }

    fun switchMode(next: AdminAuthMode) {
        hasManualModeSwitch = true
        authMode = next
        authError = null
        authInfo = null
        otpInput = ""
        otpSentToEmail = null
        verifiedOwnerEmail = null
        setupPassword = ""
        setupConfirmPassword = ""
        if (next == AdminAuthMode.RESET && setupUserId.isBlank()) {
            setupUserId = credsState.userId.ifBlank { remoteStatus?.userId.orEmpty() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A2E36))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
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
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Security",
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = "Quick Delivery Admin Console",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when (authMode) {
                        AdminAuthMode.SIGN_IN -> "Protected Ops Sign-In • Enter Admin User ID & Password"
                        AdminAuthMode.SETUP -> "First-Time Admin Setup • Verify Owner Email & Create Login"
                        AdminAuthMode.RESET -> "Reset Admin Password • Verify Owner Email via OTP or Google"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )

                // Mode selector chips when setting up or signing in
                if (authMode != AdminAuthMode.RESET) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = authMode == AdminAuthMode.SIGN_IN,
                            onClick = { switchMode(AdminAuthMode.SIGN_IN) },
                            label = {
                                Text(
                                    text = "Admin Login",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_mode_signin_chip")
                        )
                        FilterChip(
                            selected = authMode == AdminAuthMode.SETUP,
                            onClick = { switchMode(AdminAuthMode.SETUP) },
                            label = {
                                Text(
                                    text = if (hasAnyConfigured) "Setup / Reset" else "First-Time Setup",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_mode_setup_chip")
                        )
                    }
                } else {
                    TextButton(
                        onClick = { switchMode(AdminAuthMode.SIGN_IN) },
                        modifier = Modifier.testTag("admin_back_to_signin_btn")
                    ) {
                        Text(
                            text = "← Back to Admin User ID & Password Login",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )
                    }
                }

                HorizontalDivider()

                authInfo?.let { info ->
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

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

                if (authMode == AdminAuthMode.SIGN_IN) {
                    if (!hasAnyConfigured) {
                        Surface(
                            color = Color(0xFFFFF7ED),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF97316)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "New Admin device? If you haven't created your Admin User ID & Password yet, tap \"First-Time Setup\" above.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9A3412),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = {
                            loginIdentifier = it
                            authError = null
                        },
                        enabled = !isBusy && !isLockedOut,
                        label = { Text("Admin User ID or Email *") },
                        placeholder = { Text("e.g., ravinder_admin") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F766E))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_login_userid_input")
                    )

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = {
                            loginPassword = it
                            authError = null
                        },
                        enabled = !isBusy && !isLockedOut,
                        label = { Text("Password *") },
                        placeholder = { Text("Enter Admin password") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF0F766E))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                Icon(
                                    imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showLoginPassword) "Hide password" else "Show password",
                                    tint = Color(0xFF475569)
                                )
                            }
                        },
                        visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_login_password_input")
                    )

                    // Compact single-row Keep me signed in toggle
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Keep me signed in",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Switch(
                                checked = keepSignedIn,
                                onCheckedChange = { keepSignedIn = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0F766E)
                                ),
                                modifier = Modifier.testTag("admin_keep_signed_in_switch")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanIdent = loginIdentifier.trim()
                            if (cleanIdent.isBlank() || loginPassword.isEmpty()) {
                                authError = "Please enter both Admin User ID and Password."
                                return@Button
                            }
                            if (isLockedOut) return@Button
                            scope.launch {
                                isBusy = true
                                authError = null
                                authInfo = null

                                val localRes = if (credsState.isConfigured) {
                                    AdminCredentialsManager.verifyLocalLogin(context, cleanIdent, loginPassword)
                                } else {
                                    Result.failure(IllegalStateException("Not configured locally"))
                                }

                                if (localRes.isSuccess) {
                                    failedAttempts = 0
                                    isBusy = false
                                    onAdminAuthenticated(localRes.getOrThrow(), keepSignedIn)
                                    return@launch
                                }

                                // Fallback to private Render server verification (supports multi-phone & password changes)
                                val serverRes = MultiPhoneLiveSyncManager.verifyAdminCredentialsOnServer(
                                    context = context,
                                    identifier = cleanIdent,
                                    plainPassword = loginPassword
                                )
                                isBusy = false
                                serverRes.onSuccess { verified ->
                                    AdminCredentialsManager.cacheServerVerifiedCredentials(
                                        context = context,
                                        userId = verified.userId,
                                        recoveryEmail = verified.recoveryEmail,
                                        saltHex = verified.saltHex,
                                        hashHex = verified.hashHex,
                                        updatedAtEpochMs = verified.updatedAtEpochMs
                                    )
                                    failedAttempts = 0
                                    onAdminAuthenticated(verified.recoveryEmail, keepSignedIn)
                                }.onFailure { err ->
                                    val msg = err.message.orEmpty()
                                    if (msg.contains("First-Time Setup", ignoreCase = true) && !credsState.isConfigured) {
                                        authError = msg
                                    } else {
                                        failedAttempts += 1
                                        if (failedAttempts >= 5) {
                                            lockoutUntilMs = System.currentTimeMillis() + 30_000L
                                            failedAttempts = 0
                                            authError = "Too many wrong password attempts. Locked for 30 seconds."
                                        } else {
                                            authError = "Incorrect Admin User ID or Password (${5 - failedAttempts} tries left)."
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isBusy && !isLockedOut && loginIdentifier.isNotBlank() && loginPassword.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_password_signin_btn")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...", fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isLockedOut) "Locked (${remainingLockSec}s)" else "Sign In to Admin Console",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    TextButton(
                        onClick = { switchMode(AdminAuthMode.RESET) },
                        modifier = Modifier.testTag("admin_forgot_password_btn")
                    ) {
                        Text(
                            text = "Forgot Password? Reset via Email OTP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )
                    }
                } else {
                    // SETUP or RESET mode: Step 1 (Owner Email OTP / Google) -> Step 2 (Set User ID & Password)
                    if (verifiedOwnerEmail == null) {
                        Text(
                            text = "Step 1 of 2 • Verify Authorized Owner Email",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                authError = null
                            },
                            enabled = !isBusy,
                            label = { Text("Authorized Admin Email *") },
                            placeholder = { Text("ravindergupta141@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF0F766E))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_login_email_input")
                        )

                        Button(
                            onClick = {
                                val cleanEmail = emailInput.trim().lowercase()
                                if (!QuickDeliveryOperationsManager.isEmailAllowlistedForAdmin(cleanEmail)) {
                                    authError = "Access Denied: Only allowlisted Admin owner emails can configure Admin login."
                                    return@Button
                                }
                                scope.launch {
                                    isBusy = true
                                    authError = null
                                    authInfo = null
                                    val res = MultiPhoneLiveSyncManager.sendSignUpEmailOtp(
                                        context = context,
                                        email = cleanEmail,
                                        role = "ADMIN"
                                    )
                                    isBusy = false
                                    res.onSuccess {
                                        otpSentToEmail = cleanEmail
                                        resendCooldownSec = 30
                                        authInfo = "6-digit OTP sent to $cleanEmail. Check Inbox or Spam folder."
                                    }.onFailure { err ->
                                        authError = err.message ?: "Could not send Email OTP. Try Google Sign-In below."
                                    }
                                }
                            },
                            enabled = !isBusy && resendCooldownSec == 0 && emailInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_send_otp_btn")
                        ) {
                            if (isBusy && otpSentToEmail == null) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sending OTP...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Text(
                                    text = when {
                                        resendCooldownSec > 0 -> "Resend OTP in ${resendCooldownSec}s"
                                        otpSentToEmail != null -> "Resend Email OTP"
                                        else -> "Send Verification OTP to Email"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (otpSentToEmail != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { next ->
                                        otpInput = next.filter(Char::isDigit).take(6)
                                        authError = null
                                    },
                                    enabled = !isBusy,
                                    label = { Text("6-Digit OTP *") },
                                    placeholder = { Text("123456") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_otp_input")
                                )
                                Button(
                                    onClick = {
                                        val targetEmail = otpSentToEmail ?: emailInput.trim().lowercase()
                                        scope.launch {
                                            isBusy = true
                                            authError = null
                                            val res = MultiPhoneLiveSyncManager.verifySignUpEmailOtp(
                                                context = context,
                                                email = targetEmail,
                                                otp = otpInput,
                                                role = "ADMIN"
                                            )
                                            isBusy = false
                                            res.onSuccess {
                                                verifiedOwnerEmail = targetEmail
                                                verifiedProofType = "EMAIL_OTP"
                                                authInfo = "Owner email verified! Now create your Admin User ID & Password."
                                            }.onFailure { err ->
                                                authError = err.message ?: "Invalid or expired OTP. Please retry."
                                            }
                                        }
                                    },
                                    enabled = !isBusy && otpInput.length == 6,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("admin_verify_otp_btn")
                                ) {
                                    Text("Verify", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "  OR VERIFY WITH GOOGLE  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        GoogleSignInButton(
                            onAuthSuccess = {
                                val googleEmail = runCatching { Firebase.auth.currentUser?.email }.getOrNull()?.trim()?.lowercase()
                                if (QuickDeliveryOperationsManager.isEmailAllowlistedForAdmin(googleEmail)) {
                                    authError = null
                                    verifiedOwnerEmail = googleEmail
                                    verifiedProofType = "GOOGLE_SIGNIN"
                                    authInfo = "Google Owner account verified! Set your Admin User ID & Password below."
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
                    } else {
                        // Step 2: Create / Reset Admin User ID & Password
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Verified Owner: $verifiedOwnerEmail",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }

                        Text(
                            text = if (authMode == AdminAuthMode.RESET) {
                                "Step 2 of 2 • Set New Admin User ID & Password"
                            } else {
                                "Step 2 of 2 • Create Your Admin User ID & Password"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = setupUserId,
                            onValueChange = {
                                setupUserId = it.replace(" ", "").take(32)
                                authError = null
                            },
                            enabled = !isBusy,
                            label = { Text("Admin User ID (4-32 chars) *") },
                            placeholder = { Text("e.g., ravinder_admin") },
                            supportingText = { Text("Letters, numbers, dot (.), underscore (_), or hyphen (-)") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F766E))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_setup_userid_input")
                        )

                        OutlinedTextField(
                            value = setupPassword,
                            onValueChange = {
                                setupPassword = it
                                authError = null
                            },
                            enabled = !isBusy,
                            label = { Text("New Password (min 6 chars) *") },
                            placeholder = { Text("Create strong password") },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF0F766E))
                            },
                            trailingIcon = {
                                IconButton(onClick = { showSetupPassword = !showSetupPassword }) {
                                    Icon(
                                        imageVector = if (showSetupPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showSetupPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (showSetupPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_setup_password_input")
                        )

                        OutlinedTextField(
                            value = setupConfirmPassword,
                            onValueChange = {
                                setupConfirmPassword = it
                                authError = null
                            },
                            enabled = !isBusy,
                            label = { Text("Confirm Password *") },
                            placeholder = { Text("Re-enter password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0F766E))
                            },
                            trailingIcon = {
                                IconButton(onClick = { showSetupConfirmPassword = !showSetupConfirmPassword }) {
                                    Icon(
                                        imageVector = if (showSetupConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showSetupConfirmPassword) "Hide confirm password" else "Show confirm password"
                                    )
                                }
                            },
                            visualTransformation = if (showSetupConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_setup_confirm_password_input")
                        )

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Keep me signed in",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Switch(
                                    checked = keepSignedIn,
                                    onCheckedChange = { keepSignedIn = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0F766E)
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val ownerEmail = verifiedOwnerEmail ?: return@Button
                                val idErr = AdminCredentialsManager.validateUserId(setupUserId)
                                if (idErr != null) {
                                    authError = idErr
                                    return@Button
                                }
                                val passErr = AdminCredentialsManager.validatePassword(setupPassword)
                                if (passErr != null) {
                                    authError = passErr
                                    return@Button
                                }
                                if (setupPassword != setupConfirmPassword) {
                                    authError = "Passwords do not match. Please check and retry."
                                    return@Button
                                }
                                scope.launch {
                                    isBusy = true
                                    authError = null
                                    val savedRes = AdminCredentialsManager.saveCredentials(
                                        context = context,
                                        rawUserId = setupUserId,
                                        plainPassword = setupPassword,
                                        recoveryEmail = ownerEmail
                                    )
                                    savedRes.onSuccess { saved ->
                                        // Push salted stretched hash to private backend for multi-phone login
                                        MultiPhoneLiveSyncManager.saveAdminCredentialsToServer(
                                            context = context,
                                            userId = saved.userId,
                                            recoveryEmail = saved.recoveryEmail,
                                            saltHex = saved.saltHex,
                                            hashHex = saved.hashHex,
                                            authProof = verifiedProofType
                                        )
                                        isBusy = false
                                        onAdminAuthenticated(saved.recoveryEmail, keepSignedIn)
                                    }.onFailure { err ->
                                        isBusy = false
                                        authError = err.message ?: "Could not save Admin credentials."
                                    }
                                }
                            },
                            enabled = !isBusy && setupUserId.length >= 4 && setupPassword.length >= 6 && setupConfirmPassword.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_setup_save_btn")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving Credentials...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (authMode == AdminAuthMode.RESET) "Reset Password & Sign In" else "Save & Enter Admin Console",
                                    fontWeight = FontWeight.Bold,
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

@Composable
private fun AdminSecurityPinUnlockScreen(
    expectedPin: String,
    onPinUnlocked: () -> Unit,
    onSignOut: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutUntilMs by remember { mutableLongStateOf(0L) }
    var remainingLockSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(lockoutUntilMs) {
        while (lockoutUntilMs > System.currentTimeMillis()) {
            remainingLockSec = (((lockoutUntilMs - System.currentTimeMillis()) + 999L) / 1000L).toInt().coerceAtLeast(1)
            delay(1000L)
        }
        remainingLockSec = 0
    }

    val isLockedOut = remainingLockSec > 0

    fun verifyPinAttempt(candidate: String) {
        if (isLockedOut) return
        if (candidate == expectedPin) {
            failedAttempts = 0
            pinError = null
            onPinUnlocked()
        } else {
            failedAttempts += 1
            pinInput = ""
            if (failedAttempts >= 5) {
                lockoutUntilMs = System.currentTimeMillis() + 30_000L
                failedAttempts = 0
                pinError = "Too many wrong attempts. Locked for 30s."
            } else {
                pinError = "Incorrect 4-digit Admin PIN (${5 - failedAttempts} tries left)"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A2E36))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF0F766E)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_pin_unlock_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(0xFF0F766E).copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Admin PIN Lock",
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Text(
                    text = "Enter 4-Digit Admin PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = if (isLockedOut) {
                        "Security cooldown active — retry in ${remainingLockSec}s"
                    } else {
                        "Security Lock is enabled in Admin Settings"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLockedOut) Color(0xFFDC2626) else Color(0xFF475569)
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { next ->
                        if (isLockedOut) return@OutlinedTextField
                        val clean = next.filter(Char::isDigit).take(4)
                        pinInput = clean
                        pinError = null
                        if (clean.length == 4) {
                            verifyPinAttempt(clean)
                        }
                    },
                    enabled = !isLockedOut,
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_unlock_input")
                )
                pinError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
                Button(
                    onClick = { verifyPinAttempt(pinInput) },
                    enabled = !isLockedOut && pinInput.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_pin_unlock_submit_btn")
                ) {
                    Text(
                        if (isLockedOut) "Locked (${remainingLockSec}s)" else "Unlock Admin Console",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch Admin Account")
                }
            }
        }
    }
}

