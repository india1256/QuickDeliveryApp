package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.cloud.ServerHealthSnapshot
import com.example.data.model.CustomerOrder
import com.example.security.QuickDeliveryCrashShield
import com.example.security.LiveNetworkStatusMonitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ResNavy = Color(0xFF0F172A)
private val ResSlate = Color(0xFF475569)
private val ResEmerald = Color(0xFF059669)
private val ResTeal = Color(0xFF0F766E)
private val ResAmber = Color(0xFFD97706)
private val ResCrimson = Color(0xFFDC2626)

/**
 * Compact, high-visibility Internet Status & Auto-Sync Recovery Banner for Customer, Rider & Admin.
 */
@Composable
fun LiveNetworkStatusBanner(modifier: Modifier = Modifier) {
    val isOnline by LiveNetworkStatusMonitor.isOnline.collectAsState()
    val justReconnected by LiveNetworkStatusMonitor.justReconnected.collectAsState()
    val visible = !isOnline || justReconnected

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val bgColor = if (!isOnline) ResCrimson else ResEmerald
        val icon = if (!isOnline) Icons.Default.WifiOff else Icons.Default.CloudDone
        val message = if (!isOnline) {
            "No Internet — Reconnecting automatically..."
        } else {
            "Back Online — Syncing live orders..."
        }
        Surface(
            color = bgColor,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(if (!isOnline) "network_offline_banner" else "network_reconnected_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (!isOnline) "Offline" else "Reconnected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Next-launch safe Crash Recovery dialog mounted in Customer, Rider & Admin activities.
 * Lets the user send a formatted crash report to Admin Support with 1 tap or dismiss cleanly.
 */
@Composable
fun CrashRecoveryDialogOverlay(appRole: String) {
    val context = LocalContext.current
    val pendingCrash by QuickDeliveryCrashShield.pendingCrashReport.collectAsState()
    val record = pendingCrash ?: return

    AlertDialog(
        onDismissRequest = { QuickDeliveryCrashShield.clearPendingCrashReport(context) },
        icon = {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "App Recovery",
                tint = ResAmber,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "App Recovered Safely",
                fontWeight = FontWeight.ExtraBold,
                color = ResNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quick Delivery closed unexpectedly last time (${record.timestampLabel}). You can send this safe diagnostic log to Admin so it can be fixed fast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ResSlate
                )
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Report ID: ${record.crashId} • ${record.appRole} v${record.appVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = record.exceptionSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = ResNavy,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val summary = QuickDeliveryCrashShield.buildShareableCrashSummary(record)
                    runCatching {
                        QuickDeliveryOperationsManager.submitSupportTicket(
                            context = context,
                            senderRole = appRole.uppercase(),
                            senderName = "$appRole User",
                            senderPhone = "",
                            orderId = record.crashId,
                            category = "Crash Report (${record.appRole})",
                            message = summary.take(800)
                        )
                        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                    }
                    runCatching {
                        val shareIntent = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Quick Delivery Crash Report ${record.crashId}")
                                putExtra(Intent.EXTRA_TEXT, summary)
                            },
                            "Send Crash Report to Admin"
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(shareIntent)
                    }
                    QuickDeliveryCrashShield.clearPendingCrashReport(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ResTeal),
                modifier = Modifier.testTag("crash_recovery_send_report_btn")
            ) {
                Text("Send Report to Admin", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { QuickDeliveryCrashShield.clearPendingCrashReport(context) },
                modifier = Modifier.testTag("crash_recovery_dismiss_btn")
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold, color = ResSlate)
            }
        }
    )
}

/**
 * Customer Step 3 Smart Countdown Card when searching for an available delivery partner.
 * Tracks live elapsed wait time, provides 1-tap "Retry Search" (re-syncs immediately),
 * and lets Customer cancel for ₹0 Free if no rider is nearby yet.
 */
@Composable
fun CustomerSearchingCountdownCard(
    order: CustomerOrder,
    onRetrySearch: () -> Unit,
    onCancelFree: () -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSec by remember(order.orderId) { mutableIntStateOf(0) }
    var retryCount by remember(order.orderId) { mutableIntStateOf(0) }
    val targetWindowSec = 180 // 3 minutes smart countdown window

    LaunchedEffect(order.orderId, retryCount) {
        elapsedSec = 0
        while (elapsedSec < targetWindowSec) {
            delay(1_000L)
            elapsedSec += 1
        }
    }

    val mins = elapsedSec / 60
    val secs = elapsedSec % 60
    val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val progress = (elapsedSec.toFloat() / targetWindowSec.toFloat()).coerceIn(0f, 1f)
    val isHighWait = elapsedSec >= targetWindowSec

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHighWait) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isHighWait) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_searching_countdown_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHighWait) {
                            "All Nearby Riders Are Busy Right Now"
                        } else {
                            "Finding Nearest ${order.vehicleType.title} Rider..."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isHighWait) ResCrimson else Color(0xFF92400E)
                    )
                    Text(
                        text = if (isHighWait) {
                            "Tap Retry Search to ping riders & alert Admin, or cancel free (₹0)."
                        } else {
                            "Alerting verified partners nearby • Admin also monitors live queue"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ResSlate
                    )
                }
                Surface(
                    color = if (isHighWait) ResCrimson else ResAmber,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "⏱ $timerText",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = if (isHighWait) ResCrimson else ResAmber,
                trackColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        retryCount += 1
                        onRetrySearch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ResTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("customer_retry_rider_search_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Search",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Search", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onCancelFree,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ResCrimson),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("customer_cancel_free_searching_btn")
                ) {
                    Text(
                        text = "Cancel Free (₹0)",
                        fontWeight = FontWeight.Bold,
                        color = ResCrimson
                    )
                }
            }
        }
    }
}

/**
 * Prominent Red Alert Banner at the top of Admin Executive Overview whenever 1 or more orders
 * are waiting in SEARCHING_RIDER state with no rider assigned yet.
 */
@Composable
fun AdminUnassignedOrdersAlertBanner(
    searchingOrders: List<CustomerOrder>,
    onOpenOrdersTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (searchingOrders.isEmpty()) return
    val newest = searchingOrders.first()
    val areaPreview = newest.pickupAddress.fullAddress.substringBefore(",").take(34).ifBlank { "Pickup Point" }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, ResCrimson),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_unassigned_orders_alert_banner")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = ResCrimson,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Unassigned Order Alert",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🚨 ${searchingOrders.size} Unassigned Order(s) Waiting!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = ResCrimson
                    )
                    Text(
                        text = "${newest.orderId} • ${newest.customerName.ifBlank { "Customer" }} ($areaPreview) needs a rider",
                        style = MaterialTheme.typography.labelSmall,
                        color = ResNavy,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Button(
                onClick = onOpenOrdersTab,
                colors = ButtonDefaults.buttonColors(containerColor = ResCrimson),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_unassigned_orders_jump_btn")
            ) {
                Text(
                    text = "Assign Rider Now in Live Orders →",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private const val ADMIN_BACKUP_PREFS = "quick_delivery_admin_backups_v1"
private const val KEY_LAST_BACKUP_JSON = "last_backup_json"
private const val KEY_LAST_BACKUP_TIME = "last_backup_time"

/**
 * Live Render Server Health Monitor & 1-Tap Cloud Backup / Restore Protection Card for Admin.
 * Protects zero-cost Render Free Tier deployments against sudden restarts by caching clean JSON
 * snapshots on the Admin phone and allowing 1-tap restore back to quickdelivery.co.in.
 */
@Composable
fun AdminServerHealthAndBackupCard(
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences(ADMIN_BACKUP_PREFS, Context.MODE_PRIVATE)
    }

    var health by remember { mutableStateOf<ServerHealthSnapshot?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var isEmailingBackup by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var lastBackupTime by remember { mutableStateOf(prefs.getString(KEY_LAST_BACKUP_TIME, "").orEmpty()) }
    var hasLocalBackup by remember {
        mutableStateOf(!prefs.getString(KEY_LAST_BACKUP_JSON, "").isNullOrBlank())
    }

    fun runHealthPing() {
        if (isPinging) return
        isPinging = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                MultiPhoneLiveSyncManager.pingServerHealth(context)
            }
            health = result
            isPinging = false
        }
    }

    LaunchedEffect(Unit) {
        runHealthPing()
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Cloud Backup?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "This will upload your saved snapshot ($lastBackupTime) back to quickdelivery.co.in and sync all orders, riders, and settings across every phone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        val savedJson = prefs.getString(KEY_LAST_BACKUP_JSON, "").orEmpty()
                        if (savedJson.isBlank()) {
                            onFeedback("No saved backup snapshot found on this device yet.")
                            return@Button
                        }
                        isRestoring = true
                        scope.launch {
                            val res = withContext(Dispatchers.IO) {
                                MultiPhoneLiveSyncManager.restoreServerBackupSnapshot(context, savedJson)
                            }
                            isRestoring = false
                            res.onSuccess { msg ->
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                onFeedback("✅ $msg")
                                runHealthPing()
                            }.onFailure { err ->
                                onFeedback("⚠️ Restore failed: ${err.message ?: "Server unreachable"}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ResTeal),
                    modifier = Modifier.testTag("admin_confirm_restore_backup_btn")
                ) {
                    Text("Restore Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_server_health_backup_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Server Health",
                        tint = ResTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Cloud Server Health & Backup Shield",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ResNavy
                        )
                        Text(
                            text = "1-tap JSON backup & instant restore after Render restart",
                            style = MaterialTheme.typography.labelSmall,
                            color = ResSlate
                        )
                    }
                }
                OutlinedButton(
                    onClick = { runHealthPing() },
                    enabled = !isPinging,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_ping_server_health_btn")
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Check Health",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }

            // Live Health Status Pill
            val statusColor = when {
                health == null -> ResAmber
                health!!.isOnline -> ResEmerald
                health!!.isWakingUp -> ResAmber
                else -> ResCrimson
            }
            val statusLabel = when {
                isPinging -> "🟡 Checking quickdelivery.co.in..."
                health == null -> "🟡 Tap Ping to inspect live server status"
                health!!.isOnline -> "🟢 Server Online (${health!!.latencyMs} ms) • v${health!!.serverVersion}"
                health!!.isWakingUp -> "🟡 Server Waking Up (Render cold start) — tap Ping again in 15s"
                else -> "🔴 Server Offline (${health!!.errorMessage.ifBlank { "No response" }})"
            }
            Surface(
                color = statusColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    health?.takeIf { it.isOnline }?.let { h ->
                        Text(
                            text = "Orders: ${h.totalOrders} (${h.searchingOrders} searching) • Riders: ${h.registeredRiders} • Devices: ${h.connectedDevices}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ResNavy
                        )
                        if (h.permanentCloudProvider.isNotBlank()) {
                            val cloudLabel = when (h.permanentCloudProvider) {
                                "SUPABASE_REST" -> "☁️ Permanent Cloud: Supabase Connected (Auto-Save ON)"
                                "UPSTASH_REDIS" -> "☁️ Permanent Cloud: Upstash Redis Connected (Auto-Save ON)"
                                "FIREBASE_RTDB" -> "☁️ Permanent Cloud: Firebase RTDB Connected (Auto-Save ON)"
                                "MONGODB_DATA_API" -> "☁️ Permanent Cloud: MongoDB Atlas Connected (Auto-Save ON)"
                                else -> "☁️ Cloud Storage: Render Live + 24h Auto Gmail Backup"
                            }
                            Text(
                                text = cloudLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ResTeal
                            )
                        }
                    }
                    if (lastBackupTime.isNotBlank()) {
                        Text(
                            text = "💾 Last Saved Device Backup: $lastBackupTime",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ResTeal
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isExporting) return@Button
                        isExporting = true
                        scope.launch {
                            val res = withContext(Dispatchers.IO) {
                                MultiPhoneLiveSyncManager.exportServerBackupSnapshot(context)
                            }
                            isExporting = false
                            res.onSuccess { jsonStr ->
                                val stamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
                                prefs.edit()
                                    .putString(KEY_LAST_BACKUP_JSON, jsonStr)
                                    .putString(KEY_LAST_BACKUP_TIME, stamp)
                                    .apply()
                                lastBackupTime = stamp
                                hasLocalBackup = true
                                onFeedback("💾 Cloud Backup saved safely on this phone ($stamp)!")
                            }.onFailure { err ->
                                onFeedback("⚠️ Backup export failed: ${err.message ?: "Server unreachable"}")
                            }
                        }
                    },
                    enabled = !isExporting && !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = ResTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_save_cloud_backup_btn")
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Save Cloud Backup",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Backup", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                    }
                }

                OutlinedButton(
                    onClick = { showRestoreConfirm = true },
                    enabled = hasLocalBackup && !isRestoring && !isExporting,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (hasLocalBackup) ResAmber else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_restore_cloud_backup_btn")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Restore Backup",
                            tint = if (hasLocalBackup) ResAmber else ResSlate,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Restore",
                            fontWeight = FontWeight.Bold,
                            color = if (hasLocalBackup) ResAmber else ResSlate,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 1-Tap Email Cloud Backup directly to Admin Gmail
            OutlinedButton(
                onClick = {
                    if (isEmailingBackup) return@OutlinedButton
                    isEmailingBackup = true
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            MultiPhoneLiveSyncManager.triggerAdminEmailBackup(context)
                        }
                        isEmailingBackup = false
                        res.onSuccess { msg ->
                            onFeedback("📧 $msg")
                            runHealthPing()
                        }.onFailure { err ->
                            onFeedback("⚠️ Email backup failed: ${err.message ?: "Configure email key on Render"}")
                        }
                    }
                },
                enabled = !isEmailingBackup,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ResTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_email_backup_gmail_btn")
            ) {
                if (isEmailingBackup) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Backup to Gmail",
                        tint = ResTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Email Backup to Admin Gmail Now",
                        fontWeight = FontWeight.Bold,
                        color = ResTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hasLocalBackup) {
                OutlinedButton(
                    onClick = {
                        val savedJson = prefs.getString(KEY_LAST_BACKUP_JSON, "").orEmpty()
                        if (savedJson.isNotBlank()) {
                            runCatching {
                                val share = Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "Quick Delivery Backup Snapshot ($lastBackupTime)"
                                        )
                                        putExtra(Intent.EXTRA_TEXT, savedJson)
                                    },
                                    "Share Backup JSON (WhatsApp / Drive)"
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(share)
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_share_backup_json_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Backup JSON",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Saved Backup JSON (Drive / WhatsApp)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
