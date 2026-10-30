package com.example.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.cloud.CloudDeliveryOrder
import com.example.data.cloud.CloudRiderWallet
import com.example.data.cloud.DeliveryProofAndGpsManager
import com.example.data.cloud.QuickDeliveryCloudRepository
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.cloud.QuickDeliveryLiveGatewayBridge
import com.example.data.delivery.ParcelDeliveryStage
import com.example.data.delivery.ParcelDeliveryStateManager
import com.example.data.repository.SharedOrderRepository
import com.example.ui.delivery.IosCrossPlatformCompanionDialog
import com.example.ui.delivery.ParcelDeliveryStatusStepperCard
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (QuickDeliveryFirebaseInitializer.ensureInitialized(context) == null) {
        onUnauthenticated()
        return
    }
    if (runCatching { Firebase.auth.currentUser }.getOrNull() != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        QuickDeliveryFirebaseInitializer.resolveWebClientId(context)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { errorMsg ->
                    isLoading = false
                    onAuthError(errorMsg)
                },
                scope = coroutineScope,
                onAuthCancelled = { isLoading = false }
            )
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B2A)),
        modifier = modifier.testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.Person, contentDescription = "Sign in with Google", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sign in with Google")
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    if (QuickDeliveryFirebaseInitializer.ensureInitialized(context) == null) {
        onAuthError("Firebase is not initialized for this package (${context.packageName}).")
        return
    }
    val clientId = try {
        QuickDeliveryFirebaseInitializer.resolveWebClientId(context)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: web client ID not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    runCatching { Firebase.auth.signOut() }
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Background Cross-Device Cloud Sync manager for Customer, Rider, and Admin screens.
 * Automatically synchronizes order and profile data in the background using the user's
 * logged-in account/credentials without rendering a manual Cloud Sync / Google Sign-In UI card.
 */
@Composable
fun QuickDeliveryCloudSyncBar(
    modifier: Modifier = Modifier,
    showCardUi: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseReady = remember(context) { QuickDeliveryFirebaseInitializer.ensureInitialized(context) != null }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val localRepo = remember(context) { SharedOrderRepository.getInstance(context) }
    val localOrders by localRepo.orders.collectAsState()
    val localRiders by localRepo.riders.collectAsState()
    val deliveryStateManager = remember { ParcelDeliveryStateManager.getInstance() }

    var currentUser by remember {
        mutableStateOf(if (firebaseReady) runCatching { Firebase.auth.currentUser }.getOrNull() else null)
    }
    var customerCloudOrders by remember { mutableStateOf<List<CloudDeliveryOrder>>(emptyList()) }
    var openPoolCloudOrders by remember { mutableStateOf<List<CloudDeliveryOrder>>(emptyList()) }
    var assignedRiderCloudOrders by remember { mutableStateOf<List<CloudDeliveryOrder>>(emptyList()) }
    var syncedLocalOrderSignatures by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var lastSyncedRiderProfileSignature by remember { mutableStateOf<String?>(null) }

    DisposableEffect(firebaseReady) {
        if (!firebaseReady) return@DisposableEffect onDispose {}
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Automatically attempt background sign-in using existing authorized credentials
    LaunchedEffect(firebaseReady) {
        if (!firebaseReady) return@LaunchedEffect
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { currentUser = runCatching { Firebase.auth.currentUser }.getOrNull() },
            onUnauthenticated = {},
            scope = scope
        )
    }

    // Observe customer's own orders in real time
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        val cloudRepo = QuickDeliveryCloudRepository(context)
        cloudRepo.observeCustomerOrders(uid)
            .catch { }
            .collect { list -> customerCloudOrders = list }
    }

    // Observe open unassigned orders so a Rider on another phone sees new bookings in real time
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid ?: return@LaunchedEffect
        val cloudRepo = QuickDeliveryCloudRepository(context)
        cloudRepo.observeAvailableOrdersForRider()
            .catch { }
            .collect { list -> openPoolCloudOrders = list }
    }

    // Observe orders assigned to this Rider UID in real time
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        val cloudRepo = QuickDeliveryCloudRepository(context)
        cloudRepo.observeAssignedOrdersForRider(uid)
            .catch { }
            .collect { list -> assignedRiderCloudOrders = list }
    }

    // Automatically synchronize all order and profile data in the background using the user's logged-in account/credentials
    LaunchedEffect(
        currentUser?.uid,
        localOrders,
        com.example.ui.components.MotivationAndPromoManager.cachedCustomerName,
        com.example.ui.components.MotivationAndPromoManager.cachedCustomerPhone,
        com.example.ui.components.MotivationAndPromoManager.cachedCustomerEmail,
        com.example.ui.components.MotivationAndPromoManager.cachedRiderName,
        com.example.ui.components.MotivationAndPromoManager.cachedRiderPhone,
        com.example.ui.components.MotivationAndPromoManager.cachedRiderEmail
    ) {
        com.example.ui.components.MotivationAndPromoManager.syncAccountAndOrdersInBackground(
            context = context,
            orders = localOrders,
            authEmail = currentUser?.email,
            authDisplayName = currentUser?.displayName
        )

        localOrders.forEachIndexed { index, localOrder ->
            deliveryStateManager.syncFromCustomerOrder(localOrder, setAsActive = index == 0)
        }

        val uid = currentUser?.uid ?: return@LaunchedEffect
        val cloudRepo = QuickDeliveryCloudRepository(context)
        localOrders.forEach { localOrder ->
            val safeId = localOrder.orderId.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_").take(64)
            val signature = "${localOrder.orderStatus.name}|${localOrder.assignedRiderId.orEmpty()}|${localOrder.totalAmount}"
            if (safeId.isNotBlank() && syncedLocalOrderSignatures[safeId] != signature) {
                val previousSig = syncedLocalOrderSignatures[safeId]
                val cloudOrder = CloudDeliveryOrder.fromCustomerOrder(localOrder, uid)
                val res = if (previousSig == null) {
                    cloudRepo.createOrder(cloudOrder)
                } else {
                    cloudRepo.updateOrderStatusByCustomer(
                        orderId = safeId,
                        newStatus = cloudOrder.status
                    ).map { safeId }
                }
                if (res.isSuccess) {
                    syncedLocalOrderSignatures = syncedLocalOrderSignatures + (safeId to signature)
                }
            }
        }
    }

    // Automatically synchronize logged-in Rider profile and wallet state in the background
    LaunchedEffect(
        currentUser?.uid,
        localRiders,
        com.example.ui.components.MotivationAndPromoManager.cachedRiderName
    ) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        val primaryRider = localRiders.firstOrNull() ?: return@LaunchedEffect
        val riderName = com.example.ui.components.MotivationAndPromoManager.cachedRiderName
            .ifBlank { primaryRider.name }
        val balancePaise = (primaryRider.walletBalanceInr * 100.0).toLong().coerceAtLeast(0L)
        val debtPaise = if (primaryRider.walletBalanceInr < 0.0) {
            (kotlin.math.abs(primaryRider.walletBalanceInr) * 100.0).toLong().coerceIn(0L, 15000L)
        } else {
            0L
        }
        val shiftStatus = if (primaryRider.isOnline) "ONLINE_ACTIVE" else "OFFLINE_RESTING"
        val profileSig = "${primaryRider.riderId}|$riderName|$balancePaise|$debtPaise|$shiftStatus"
        if (lastSyncedRiderProfileSignature != profileSig) {
            val cloudRepo = QuickDeliveryCloudRepository(context)
            val walletRes = cloudRepo.upsertRiderWallet(
                CloudRiderWallet(
                    id = uid,
                    userId = uid,
                    riderId = primaryRider.riderId,
                    riderName = riderName,
                    balancePaise = balancePaise,
                    commissionDebtPaise = debtPaise,
                    maxCommissionDebtPaise = 15000L,
                    workMode = "FULL_TIME",
                    shiftStatus = shiftStatus,
                    batteryPercent = 90L
                )
            )
            if (walletRes.isSuccess) {
                lastSyncedRiderProfileSignature = profileSig
            }
        }
    }

    LaunchedEffect(customerCloudOrders, assignedRiderCloudOrders, openPoolCloudOrders) {
        val allCloud = assignedRiderCloudOrders + customerCloudOrders + openPoolCloudOrders
        allCloud.forEachIndexed { index, cloudOrder ->
            deliveryStateManager.syncFromCloudOrder(cloudOrder, setAsActive = index == 0)
        }
    }
}

/**
 * Clean, compact options menu dialog opened from the top-right 3-dots (⋮) button.
 * Keeps background account & order synchronization active without displaying any manual
 * Cloud Sync / Sign in with Google UI card in the menu.
 */
@Composable
fun QuickDeliveryTopOptionsDialog(
    onDismiss: () -> Unit,
    extraTopContent: @Composable () -> Unit = {}
) {
    // Ensure background synchronization remains active using the user's logged-in account
    QuickDeliveryCloudSyncBar(showCardUi = false)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("three_dots_options_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More Options (⋮)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A)
                    )
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_three_dots_options_btn")
                    ) {
                        Text("Close", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    extraTopContent()
                }
            }
        }
    }
}




