package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryBrandLinks
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.data.repository.SharedOrderRepository

private val SyncTeal = Color(0xFF0A3D3F)
private val SyncEmerald = Color(0xFF10B981)
private val SyncOrange = Color(0xFFF97316)
private val SyncNavy = Color(0xFF0F172A)
private val SyncCrimson = Color(0xFFDC2626)
private val SyncSlate = Color(0xFF64748B)
private val SyncIndigo = Color(0xFF4338CA)

/**
 * Drop-in Remote App Update / Force Update dialog for Customer & Rider apps.
 * Stays completely invisible unless Admin publishes a newer version from Admin Settings.
 */
@Composable
fun AppRemoteUpdateGateOverlay(
    appRole: String,
    currentVersionName: String,
    currentVersionCode: Int
) {
    val context = LocalContext.current
    val flags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
    var dismissedVersionKey by remember { mutableStateOf("") }

    val updateAvailable = remember(
        flags.latestAppVersionName,
        flags.latestAppVersionCode,
        currentVersionName,
        currentVersionCode
    ) {
        flags.isUpdateAvailableForApp(currentVersionName, currentVersionCode)
    }
    val versionKey = "${flags.latestAppVersionName}_${flags.latestAppVersionCode}"
    val shouldShow = updateAvailable && (flags.forceUpdateRequired || dismissedVersionKey != versionKey)

    if (!shouldShow) return

    val targetUrl = remember(appRole, flags.customerUpdateUrl, flags.riderUpdateUrl) {
        if (appRole.equals("RIDER", ignoreCase = true)) {
            flags.riderUpdateUrl.ifBlank { QuickDeliveryBrandLinks.RIDER_APK_URL }
        } else {
            flags.customerUpdateUrl.ifBlank { QuickDeliveryBrandLinks.CUSTOMER_APK_URL }
        }
    }
    val targetVersionLabel = flags.latestAppVersionName.trim().ifBlank { "Build #${flags.latestAppVersionCode}" }

    AlertDialog(
        onDismissRequest = {
            if (!flags.forceUpdateRequired) {
                dismissedVersionKey = versionKey
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !flags.forceUpdateRequired,
            dismissOnClickOutside = !flags.forceUpdateRequired
        ),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "App Update Available",
                tint = if (flags.forceUpdateRequired) SyncCrimson else SyncTeal,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (flags.forceUpdateRequired) {
                    "Required Update (v$targetVersionLabel)"
                } else {
                    "New App Update Available (v$targetVersionLabel)"
                },
                fontWeight = FontWeight.ExtraBold,
                color = SyncNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You are on v$currentVersionName. Update now to get the latest improvements and security fixes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SyncSlate
                )
                if (flags.updateNotes.isNotBlank()) {
                    Surface(
                        color = Color(0xFFF0FDFA),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = flags.updateNotes,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SyncTeal,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (flags.forceUpdateRequired) SyncCrimson else SyncTeal
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("app_remote_update_now_btn")
            ) {
                Text("Update Now", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = if (!flags.forceUpdateRequired) {
            {
                TextButton(
                    onClick = { dismissedVersionKey = versionKey },
                    modifier = Modifier.testTag("app_remote_update_later_btn")
                ) {
                    Text("Later", fontWeight = FontWeight.Bold, color = SyncSlate)
                }
            }
        } else {
            null
        }
    )
}

/**
 * Top-of-Settings Admin Master Sync, Daily Business KPI Strip, 1-Click App Update Push,
 * Live Rate Card & Vehicle Sync, Emergency Pause / Working Hours / Future Modules,
 * and Fake Customer / COD Fraud Blocklist — neatly organized in collapsible mobile-friendly groups.
 */
@Composable
fun AdminMasterSyncAndAppUpdateCard(
    repository: SharedOrderRepository,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val flags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
    val syncStatus by MultiPhoneLiveSyncManager.syncStatus.collectAsState()
    val orders by repository.orders.collectAsState()
    val codSettlements by QuickDeliveryOperationsManager.codSettlements.collectAsState()

    // Collapsible sections (Rate Card expanded by default for 1-click Kiraya / Fare edits)
    var expandAppUpdate by remember { mutableStateOf(false) }
    var expandRateCard by remember { mutableStateOf(true) }
    var expandFutureOps by remember { mutableStateOf(false) }
    var expandBlocklist by remember { mutableStateOf(false) }
    var expandGoogleSignInHelper by remember { mutableStateOf(false) }

    // Draft states
    var versionNameDraft by remember(flags.latestAppVersionName) { mutableStateOf(flags.latestAppVersionName) }
    var versionCodeDraft by remember(flags.latestAppVersionCode) {
        mutableStateOf(if (flags.latestAppVersionCode > 0) flags.latestAppVersionCode.toString() else "")
    }
    var updateNotesDraft by remember(flags.updateNotes) { mutableStateOf(flags.updateNotes) }
    var customerUrlDraft by remember(flags.customerUpdateUrl) { mutableStateOf(flags.customerUpdateUrl) }
    var riderUrlDraft by remember(flags.riderUpdateUrl) { mutableStateOf(flags.riderUpdateUrl) }

    var bikeBaseDraft by remember(flags.bikeBaseFareInr) { mutableStateOf(flags.bikeBaseFareInr.toInt().toString()) }
    var bikeKmDraft by remember(flags.bikeRatePerKmInr) { mutableStateOf(flags.bikeRatePerKmInr.toInt().toString()) }
    var scooterBaseDraft by remember(flags.scooterBaseFareInr) { mutableStateOf(flags.scooterBaseFareInr.toInt().toString()) }
    var scooterKmDraft by remember(flags.scooterRatePerKmInr) { mutableStateOf(flags.scooterRatePerKmInr.toInt().toString()) }
    var autoBaseDraft by remember(flags.autoBaseFareInr) { mutableStateOf(flags.autoBaseFareInr.toInt().toString()) }
    var autoKmDraft by remember(flags.autoRatePerKmInr) { mutableStateOf(flags.autoRatePerKmInr.toInt().toString()) }
    var codLimitDraft by remember(flags.riderCodCommissionLimitInr) { mutableStateOf(flags.riderCodCommissionLimitInr.toString()) }
    var adminUpiDraft by remember(flags.adminUpiSettlementId) { mutableStateOf(flags.adminUpiSettlementId) }
    var adminAccountLabelDraft by remember(flags.adminSettlementAccountLabel) { mutableStateOf(flags.adminSettlementAccountLabel) }

    var pauseReasonDraft by remember(flags.emergencyPauseReason) { mutableStateOf(flags.emergencyPauseReason) }
    var blockPhoneDraft by remember { mutableStateOf("") }
    var blockPhoneError by remember { mutableStateOf<String?>(null) }

    fun pushFlags(transform: (AdminRemoteFeatureFlags) -> AdminRemoteFeatureFlags, note: String) {
        val next = transform(flags).copy(masterSyncRevision = flags.masterSyncRevision + 1)
        QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(context, next, repository)
        onFeedback(note)
    }

    // Live Business KPI numbers from real orders
    val kpiSummary = remember(orders, codSettlements, flags.platformCommissionPercent) {
        val validOrders = orders.filter { it.orderStatus != OrderStatus.CANCELLED }
        val totalGmv = validOrders.sumOf {
            it.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        }
        val netProfit = totalGmv * (flags.platformCommissionPercent.toDouble() / 100.0)
        val deliveredCod = validOrders
            .filter { it.orderStatus == OrderStatus.DELIVERED && it.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY }
            .sumOf { it.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0 }
        val settledCod = codSettlements
            .filter { it.status == "VERIFIED" }
            .sumOf { it.amountInr }
        val pendingCod = (deliveredCod * (flags.platformCommissionPercent.toDouble() / 100.0) - settledCod).coerceAtLeast(0.0)
        QuadKpi(validOrders.size, totalGmv.toInt(), netProfit.toInt(), pendingCod.toInt())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. 1-CLICK MASTER SYNC ALL APPS BANNER + LIVE BUSINESS SUMMARY
        Card(
            colors = CardDefaults.cardColors(containerColor = SyncTeal),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_master_sync_card")
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
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Master Sync",
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Master Sync & Live Control",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Rev #${flags.masterSyncRevision} • ${syncStatus.connectedPhonesCount} active device(s) • ${syncStatus.lastSyncTimeLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }

                // Live 4-tile Business Summary strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryMiniTile("Orders", "${kpiSummary.orders}", Modifier.weight(1f))
                    SummaryMiniTile("GMV", "₹${kpiSummary.gmvInr}", Modifier.weight(1f))
                    SummaryMiniTile("Net Profit", "₹${kpiSummary.profitInr}", Modifier.weight(1f))
                    SummaryMiniTile("COD Due", "₹${kpiSummary.pendingCodInr}", Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        pushFlags(
                            { it },
                            "🚀 Master Sync Rev #${flags.masterSyncRevision + 1} pushed to all Customer & Rider apps!"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SyncEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_master_sync_all_apps_btn")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1-Click Sync All Apps Now", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        // 2. COLLAPSIBLE GROUP 1: 1-CLICK APP UPDATE & FORCE UPDATE CONTROLLER
        CollapsibleAdminSectionCard(
            title = "App Update & Force Update Push",
            subtitle = if (flags.latestAppVersionName.isNotBlank() || flags.latestAppVersionCode > 0) {
                "ACTIVE: v${flags.latestAppVersionName.ifBlank { flags.latestAppVersionCode.toString() }} (${if (flags.forceUpdateRequired) "FORCE" else "Soft"})"
            } else {
                "Push new APK / Play Store updates to all Customer & Rider phones"
            },
            icon = Icons.Default.SystemUpdate,
            iconTint = if (flags.forceUpdateRequired) SyncCrimson else SyncTeal,
            expanded = expandAppUpdate,
            onToggle = { expandAppUpdate = !expandAppUpdate },
            testTag = "admin_section_app_update"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = versionNameDraft,
                    onValueChange = { versionNameDraft = it.trim().take(16) },
                    label = { Text("Target Version") },
                    placeholder = { Text("e.g., 6.4.0") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_update_version_name_input")
                )
                OutlinedTextField(
                    value = versionCodeDraft,
                    onValueChange = { versionCodeDraft = it.filter(Char::isDigit).take(5) },
                    label = { Text("Build Code") },
                    placeholder = { Text("e.g., 78") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_update_version_code_input")
                )
            }
            OutlinedTextField(
                value = updateNotesDraft,
                onValueChange = { updateNotesDraft = it.take(200) },
                label = { Text("What's New Message") },
                placeholder = { Text("e.g., Faster tracking, new security fixes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customerUrlDraft,
                onValueChange = { customerUrlDraft = it.trim() },
                label = { Text("Customer Update Link (APK / Play Store)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = riderUrlDraft,
                onValueChange = { riderUrlDraft = it.trim() },
                label = { Text("Rider Update Link (APK / Play Store)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            CompactSwitchRow(
                title = "Force Update (Required)",
                subtitle = if (flags.forceUpdateRequired) {
                    "Old apps MUST update before continuing"
                } else {
                    "Soft prompt — users can tap Later"
                },
                checked = flags.forceUpdateRequired,
                onCheckedChange = { req ->
                    pushFlags(
                        { it.copy(forceUpdateRequired = req) },
                        if (req) "⚠️ Force Update mode ON" else "Soft Update mode selected"
                    )
                },
                testTag = "admin_switch_force_update"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val codeInt = versionCodeDraft.toIntOrNull() ?: 0
                        pushFlags(
                            {
                                it.copy(
                                    latestAppVersionName = versionNameDraft.trim(),
                                    latestAppVersionCode = codeInt,
                                    updateNotes = updateNotesDraft.trim(),
                                    customerUpdateUrl = customerUrlDraft.trim().ifBlank { QuickDeliveryBrandLinks.CUSTOMER_APK_URL },
                                    riderUpdateUrl = riderUrlDraft.trim().ifBlank { QuickDeliveryBrandLinks.RIDER_APK_URL }
                                )
                            },
                            "📲 Update Alert (v${versionNameDraft.ifBlank { codeInt.toString() }}) pushed to all apps!"
                        )
                    },
                    enabled = versionNameDraft.isNotBlank() || (versionCodeDraft.toIntOrNull() ?: 0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = SyncTeal),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_push_update_alert_btn")
                ) {
                    Text(
                        text = "Push Update Alert",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = {
                        versionNameDraft = ""
                        versionCodeDraft = ""
                        updateNotesDraft = ""
                        pushFlags(
                            {
                                it.copy(
                                    latestAppVersionName = "",
                                    latestAppVersionCode = 0,
                                    forceUpdateRequired = false,
                                    updateNotes = ""
                                )
                            },
                            "Update prompt cleared across all apps"
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "Clear",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 3. COLLAPSIBLE GROUP 2: LIVE FARE / KIRAYA EDITOR, VEHICLE AVAILABILITY & NIGHT SURCHARGE
        CollapsibleAdminSectionCard(
            title = "Live Fare / Kiraya & Vehicle Switch (1-Click Sync)",
            subtitle = "Bike ₹${flags.bikeBaseFareInr.toInt()}+₹${flags.bikeRatePerKmInr.toInt()}/km • Scooter ₹${flags.scooterBaseFareInr.toInt()}+₹${flags.scooterRatePerKmInr.toInt()}/km • Auto ${if (flags.autoEnabled) "ON" else "OFF"}",
            icon = Icons.Default.LocalShipping,
            iconTint = SyncOrange,
            expanded = expandRateCard,
            onToggle = { expandRateCard = !expandRateCard },
            testTag = "admin_section_rate_card"
        ) {
            Text(
                text = "Active Vehicles (1-Tap Hide/Show in Customer App)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SyncNavy
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = flags.bikeEnabled,
                    onClick = {
                        val nextBike = !flags.bikeEnabled
                        if (!nextBike && !flags.scooterEnabled && !flags.autoEnabled) {
                            onFeedback("At least 1 vehicle must remain active.")
                        } else {
                            pushFlags({ it.copy(bikeEnabled = nextBike) }, "Bike ${if (nextBike) "ENABLED" else "HIDDEN"} live")
                        }
                    },
                    label = { Text("Bike", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("admin_vehicle_chip_bike")
                )
                FilterChip(
                    selected = flags.scooterEnabled,
                    onClick = {
                        val nextScooter = !flags.scooterEnabled
                        if (!flags.bikeEnabled && !nextScooter && !flags.autoEnabled) {
                            onFeedback("At least 1 vehicle must remain active.")
                        } else {
                            pushFlags({ it.copy(scooterEnabled = nextScooter) }, "Scooter ${if (nextScooter) "ENABLED" else "HIDDEN"} live")
                        }
                    },
                    label = { Text("Scooter", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("admin_vehicle_chip_scooter")
                )
                FilterChip(
                    selected = flags.autoEnabled,
                    onClick = {
                        val nextAuto = !flags.autoEnabled
                        if (!flags.bikeEnabled && !flags.scooterEnabled && !nextAuto) {
                            onFeedback("At least 1 vehicle must remain active.")
                        } else {
                            pushFlags({ it.copy(autoEnabled = nextAuto) }, "Auto ${if (nextAuto) "ENABLED" else "HIDDEN"} live")
                        }
                    },
                    label = { Text("Auto", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("admin_vehicle_chip_auto")
                )
            }

            RateRowInputs("Bike", bikeBaseDraft, { bikeBaseDraft = it }, bikeKmDraft, { bikeKmDraft = it })
            RateRowInputs("Scooter", scooterBaseDraft, { scooterBaseDraft = it }, scooterKmDraft, { scooterKmDraft = it })
            RateRowInputs("Auto", autoBaseDraft, { autoBaseDraft = it }, autoKmDraft, { autoKmDraft = it })

            Button(
                onClick = {
                    pushFlags(
                        {
                            it.copy(
                                remoteRateCardOverrideEnabled = true,
                                bikeBaseFareInr = (bikeBaseDraft.toDoubleOrNull() ?: it.bikeBaseFareInr).coerceIn(10.0, 2000.0),
                                bikeRatePerKmInr = (bikeKmDraft.toDoubleOrNull() ?: it.bikeRatePerKmInr).coerceIn(2.0, 500.0),
                                scooterBaseFareInr = (scooterBaseDraft.toDoubleOrNull() ?: it.scooterBaseFareInr).coerceIn(10.0, 2000.0),
                                scooterRatePerKmInr = (scooterKmDraft.toDoubleOrNull() ?: it.scooterRatePerKmInr).coerceIn(2.0, 500.0),
                                autoBaseFareInr = (autoBaseDraft.toDoubleOrNull() ?: it.autoBaseFareInr).coerceIn(15.0, 3000.0),
                                autoRatePerKmInr = (autoKmDraft.toDoubleOrNull() ?: it.autoRatePerKmInr).coerceIn(3.0, 600.0)
                            )
                        },
                        "💰 Live Rate Card synced across Customer & Rider apps!"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SyncOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_sync_rate_card_btn")
            ) {
                Text("Sync Live Rate Card to All Apps", fontWeight = FontWeight.Bold, color = Color.White)
            }

            HorizontalDivider()

            CompactSwitchRow(
                title = "Night Surcharge (11 PM – 6 AM • ${flags.nightSurchargeMultiplier}x)",
                subtitle = "Default OFF • Automatically adds night multiplier after 11 PM",
                checked = flags.nightSurchargeEnabled,
                onCheckedChange = { enabled ->
                    pushFlags(
                        { it.copy(nightSurchargeEnabled = enabled) },
                        if (enabled) "🌙 Night Surcharge (${flags.nightSurchargeMultiplier}x) ACTIVATED" else "Night Surcharge turned OFF"
                    )
                },
                testTag = "admin_switch_night_surcharge"
            )
            if (flags.nightSurchargeEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.15, 1.20, 1.30, 1.50).forEach { mult ->
                        FilterChip(
                            selected = kotlin.math.abs(flags.nightSurchargeMultiplier - mult) < 0.01,
                            onClick = {
                                pushFlags({ it.copy(nightSurchargeMultiplier = mult) }, "🌙 Night multiplier set to ${mult}x")
                            },
                            label = { Text("${mult}x", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Rider COD ${flags.platformCommissionPercent}% Commission Limit & Admin Transfer Account",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SyncNavy
            )
            Text(
                text = "Rider auto-locks at ₹${flags.riderCodCommissionLimitInr} unpaid commission until paid to your UPI/Account and reset to ₹0 (FREE).",
                style = MaterialTheme.typography.labelSmall,
                color = SyncSlate
            )
            OutlinedTextField(
                value = codLimitDraft,
                onValueChange = { codLimitDraft = it.filter(Char::isDigit).take(4) },
                label = { Text("Rider COD Commission Lock Limit (₹)") },
                placeholder = { Text("200") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_cod_commission_limit_input")
            )
            OutlinedTextField(
                value = adminUpiDraft,
                onValueChange = { adminUpiDraft = it.trim().take(64) },
                label = { Text("Your Admin UPI ID (for Rider COD Transfers)") },
                placeholder = { Text("Enter your real UPI ID (optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_cod_upi_settlement_input")
            )
            OutlinedTextField(
                value = adminAccountLabelDraft,
                onValueChange = { adminAccountLabelDraft = it.take(64) },
                label = { Text("Admin Account / Payee Name") },
                placeholder = { Text("e.g., Business / Owner Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_cod_account_label_input")
            )
            Button(
                onClick = {
                    val limitVal = (codLimitDraft.toIntOrNull() ?: 200).coerceIn(50, 5000)
                    codLimitDraft = limitVal.toString()
                    pushFlags(
                        {
                            it.copy(
                                riderCodCommissionLimitInr = limitVal,
                                adminUpiSettlementId = adminUpiDraft.trim(),
                                adminSettlementAccountLabel = adminAccountLabelDraft.trim()
                            )
                        },
                        "💵 COD Lock Limit (₹$limitVal) & Admin Settlement Account synced to all Riders!"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SyncTeal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_sync_cod_limit_btn")
            ) {
                Text(
                    text = "Sync COD Limit & Admin UPI to Riders",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 4. COLLAPSIBLE GROUP 3: EMERGENCY PAUSE, WORKING HOURS & FUTURE MODULES
        CollapsibleAdminSectionCard(
            title = "Emergency Pause, Timings & Future Switches",
            subtitle = when {
                flags.emergencyBookingsPaused -> "⚠️ NEW BOOKINGS PAUSED"
                flags.operatingHoursEnabled -> "Timings ON (${flags.operatingHoursLabel()})"
                else -> "Service Open 24x7 • Future paid modules ready (OFF)"
            },
            icon = if (flags.emergencyBookingsPaused) Icons.Default.PauseCircle else Icons.Default.Schedule,
            iconTint = if (flags.emergencyBookingsPaused) SyncCrimson else SyncIndigo,
            expanded = expandFutureOps,
            onToggle = { expandFutureOps = !expandFutureOps },
            testTag = "admin_section_future_modules"
        ) {
            CompactSwitchRow(
                title = "Emergency Pause New Bookings",
                subtitle = "Stops new orders immediately (ongoing deliveries keep running)",
                checked = flags.emergencyBookingsPaused,
                onCheckedChange = { paused ->
                    pushFlags(
                        {
                            it.copy(
                                emergencyBookingsPaused = paused,
                                emergencyPauseReason = pauseReasonDraft.trim()
                                    .ifBlank { "New bookings are temporarily paused by Operations. Please try again shortly." }
                            )
                        },
                        if (paused) "🛑 Emergency Booking Pause ACTIVATED live!" else "✅ Bookings RESUMED live!"
                    )
                },
                testTag = "admin_switch_emergency_pause"
            )
            if (flags.emergencyBookingsPaused) {
                OutlinedTextField(
                    value = pauseReasonDraft,
                    onValueChange = { pauseReasonDraft = it.take(160) },
                    label = { Text("Pause Notice Shown to Customers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            CompactSwitchRow(
                title = "Service Working Hours (${flags.operatingHoursLabel()})",
                subtitle = "Default OFF (24x7) • Turn ON to accept bookings only during selected hours",
                checked = flags.operatingHoursEnabled,
                onCheckedChange = { enabled ->
                    pushFlags(
                        { it.copy(operatingHoursEnabled = enabled) },
                        if (enabled) "⏰ Working Hours (${flags.operatingHoursLabel()}) ENABLED" else "Service set to 24x7 Open"
                    )
                },
                testTag = "admin_switch_operating_hours"
            )
            if (flags.operatingHoursEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(7, 23, "7 AM – 11 PM"),
                        Triple(6, 22, "6 AM – 10 PM"),
                        Triple(8, 22, "8 AM – 10 PM"),
                        Triple(9, 21, "9 AM – 9 PM")
                    ).forEach { (startH, endH, label) ->
                        FilterChip(
                            selected = flags.operatingStartHour == startH && flags.operatingEndHour == endH,
                            onClick = {
                                pushFlags(
                                    { it.copy(operatingStartHour = startH, operatingEndHour = endH) },
                                    "⏰ Working Hours set to $label"
                                )
                            },
                            label = { Text(label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Future Modules & Customer Features (Keep OFF now — 1-Tap ON later)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SyncIndigo
            )
            CompactSwitchRow(
                title = "Auto (3-Wheeler) Vehicle Option",
                subtitle = "Default OFF (Bike & Scooter active) • Turn ON anytime to show Auto in Customer App",
                checked = flags.autoEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(autoEnabled = on) },
                        if (on) "🛺 Auto (3-Wheeler) ENABLED in Customer App!" else "Auto hidden from Customer App (Bike & Scooter active)"
                    )
                },
                testTag = "admin_future_switch_auto_vehicle"
            )
            CompactSwitchRow(
                title = "Promo Codes & Discount Coupons",
                subtitle = "Default OFF (Hidden in Customer App) • Turn ON to show Promo Code box & offers",
                checked = flags.promoCodeEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(promoCodeEnabled = on) },
                        if (on) "🏷️ Promo Codes ENABLED in Customer App!" else "Promo Codes hidden from Customer App"
                    )
                },
                testTag = "admin_future_switch_promo_codes"
            )
            CompactSwitchRow(
                title = "Refer & Earn Program (₹${flags.referralBonusInr} Bonus)",
                subtitle = "Default OFF (Hidden in Customer & Rider Apps) • Turn ON later for referral growth",
                checked = flags.referralWalletEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(referralWalletEnabled = on) },
                        if (on) "🎁 Refer & Earn ENABLED across Customer & Rider Apps!" else "Refer & Earn hidden from Customer & Rider Apps"
                    )
                },
                testTag = "admin_future_switch_refer_earn"
            )
            CompactSwitchRow(
                title = "Online Payment Gateway Live Mode (${flags.gatewayProvider})",
                subtitle = "Default OFF (COD & Direct UPI) • Turn ON once Razorpay/Cashfree KYC is active",
                checked = flags.liveGatewayModeEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(liveGatewayModeEnabled = on) },
                        if (on) "💳 Online Payment Gateway LIVE Mode ON" else "Online Payment Gateway set to OFF (COD/UPI mode)"
                    )
                },
                testTag = "admin_switch_live_gateway"
            )
            CompactSwitchRow(
                title = "SMS / WhatsApp OTP Gateway",
                subtitle = "Default OFF (₹0 Email OTP active) • Turn ON when MSG91 / Fast2SMS is configured",
                checked = flags.smsOtpGatewayEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(smsOtpGatewayEnabled = on) },
                        if (on) "📱 SMS OTP Gateway flag ENABLED" else "SMS OTP Gateway OFF (₹0 Email OTP active)"
                    )
                },
                testTag = "admin_switch_sms_otp_gateway"
            )
            CompactSwitchRow(
                title = "Razorpay 80/20 Auto Route Split",
                subtitle = "Default OFF • Automatic Rider/Company settlement split via Razorpay Route",
                checked = flags.razorpayRouteSplitEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(razorpayRouteSplitEnabled = on) },
                        if (on) "⚡ Razorpay Route Split ENABLED" else "Razorpay Route Split OFF"
                    )
                },
                testTag = "admin_switch_route_split"
            )
            CompactSwitchRow(
                title = "🔐 Pickup & Drop OTP Verification System",
                subtitle = "Default OFF (Direct Handover active) • Turn ON in 1 click to require 4-digit Pickup & Drop OTPs on Customer & Rider apps",
                checked = flags.pickupDropOtpEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(pickupDropOtpEnabled = on) },
                        if (on) "🔐 Pickup & Drop OTP Verification ACTIVATED across Customer & Rider apps!" else "Pickup & Drop OTP turned OFF (Direct Handover active)"
                    )
                },
                testTag = "admin_switch_pickup_drop_otp_master"
            )
            CompactSwitchRow(
                title = "Auto-KYC Instant Verification API",
                subtitle = "Default OFF (Manual Admin KYC Approval active)",
                checked = flags.autoKycApiEnabled,
                onCheckedChange = { on ->
                    pushFlags(
                        { it.copy(autoKycApiEnabled = on) },
                        if (on) "🛡️ Auto-KYC API Mode ENABLED" else "Manual Admin KYC Approval active"
                    )
                },
                testTag = "admin_switch_auto_kyc_api"
            )
        }

        // 5. COLLAPSIBLE GROUP 4: FAKE CUSTOMER / COD FRAUD BLOCKLIST
        val blockedNumbers = remember(flags.blockedCustomerPhonesCsv) { flags.getBlockedCustomerPhonesList() }
        CollapsibleAdminSectionCard(
            title = "Fake Customer / COD Fraud Blocklist (${blockedNumbers.size})",
            subtitle = if (blockedNumbers.isEmpty()) {
                "Block repeat fake COD numbers in 1 tap (synced live)"
            } else {
                "${blockedNumbers.size} number(s) currently blocked from placing orders"
            },
            icon = Icons.Default.Block,
            iconTint = SyncCrimson,
            expanded = expandBlocklist,
            onToggle = { expandBlocklist = !expandBlocklist },
            testTag = "admin_section_blocked_customers"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = blockPhoneDraft,
                    onValueChange = { raw ->
                        val digits = raw.filter(Char::isDigit)
                        blockPhoneError = if (digits.length > 10) "Enter 10 digits only" else null
                        blockPhoneDraft = digits.take(10)
                    },
                    prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = SyncTeal) },
                    label = { Text("10-Digit Mobile") },
                    placeholder = { Text("10-digit mobile") },
                    isError = blockPhoneError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_block_customer_phone_input")
                )
                Button(
                    onClick = {
                        if (blockPhoneDraft.length != 10) {
                            blockPhoneError = "Enter exact 10 digits"
                            return@Button
                        }
                        val nextList = (blockedNumbers + blockPhoneDraft).distinct()
                        val added = blockPhoneDraft
                        blockPhoneDraft = ""
                        blockPhoneError = null
                        pushFlags(
                            { it.copy(blockedCustomerPhonesCsv = nextList.joinToString(",")) },
                            "🚫 +91 $added added to Fraud Blocklist & synced!"
                        )
                    },
                    enabled = blockPhoneDraft.length == 10,
                    colors = ButtonDefaults.buttonColors(containerColor = SyncCrimson),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("admin_block_customer_phone_btn")
                ) {
                    Text("Block", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                }
            }
            blockPhoneError?.let { err ->
                Text(text = err, style = MaterialTheme.typography.labelSmall, color = SyncCrimson)
            }
            if (blockedNumbers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    blockedNumbers.forEach { phone ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                val nextList = blockedNumbers.filterNot { it == phone }
                                pushFlags(
                                    { it.copy(blockedCustomerPhonesCsv = nextList.joinToString(",")) },
                                    "✅ +91 $phone unblocked"
                                )
                            },
                            label = { Text("+91 $phone ✕", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        // 6. COLLAPSIBLE GROUP 5: STANDALONE GOOGLE SIGN-IN SETUP (FIREBASE SHA-1 HELPER)
        val copyToClipboard: (String, String) -> Unit = { label, value ->
            runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText(label, value))
            }
            onFeedback("📋 Copied $label")
        }
        val signingSha1 = "E2:99:79:4D:C8:AE:C8:C3:1F:61:05:E0:FD:92:34:E6:CF:1A:10:CE"
        CollapsibleAdminSectionCard(
            title = "Standalone Google Sign-In Setup (Firebase SHA-1)",
            subtitle = "1-tap copy 3 Package IDs & SHA-1 to register standalone APKs in Firebase Console",
            icon = Icons.Default.Security,
            iconTint = SyncTeal,
            expanded = expandGoogleSignInHelper,
            onToggle = { expandGoogleSignInHelper = !expandGoogleSignInHelper },
            testTag = "admin_section_firebase_sha1_helper"
        ) {
            Text(
                text = "Register these 3 standalone Android package names with the SHA-1 below in Firebase Project Settings so Sign in with Google works on every standalone APK:",
                style = MaterialTheme.typography.labelSmall,
                color = SyncSlate
            )
            listOf(
                "Customer APK Package" to "com.aistudio.quickdelivery.cust01",
                "Rider APK Package" to "com.aistudio.quickdelivery.ride02",
                "Admin APK Package" to "com.aistudio.quickdelivery.admn03",
                "Signing SHA-1 Fingerprint" to signingSha1
            ).forEach { (label, value) ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SyncNavy
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.labelSmall,
                                color = SyncTeal
                            )
                        }
                        OutlinedButton(
                            onClick = { copyToClipboard(label, value) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("admin_copy_${label.lowercase().replace(" ", "_")}")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            Button(
                onClick = {
                    runCatching {
                        val url = "https://console.firebase.google.com/project/gen-lang-client-0738232271/settings/general"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SyncTeal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_open_firebase_console_btn")
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open Firebase Console", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Firebase Console Settings →", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // 7. LIVE SERVER HEALTH MONITOR & 1-TAP BACKUP / RESTORE SHIELD
        AdminServerHealthAndBackupCard(onFeedback = onFeedback)
    }
}

private data class QuadKpi(
    val orders: Int,
    val gmvInr: Int,
    val profitInr: Int,
    val pendingCodInr: Int
)

@Composable
private fun SummaryMiniTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCCFBF1),
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CollapsibleAdminSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onToggle,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminIconBadge(icon = icon, tint = iconTint, sizeDp = 34)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SyncNavy
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = SyncSlate
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse section" else "Expand section",
                        tint = SyncSlate
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                content()
            }
        }
    }
}

@Composable
private fun CompactSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = SyncNavy
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SyncSlate
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SyncEmerald
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun RateRowInputs(
    vehicleLabel: String,
    baseValue: String,
    onBaseChange: (String) -> Unit,
    kmValue: String,
    onKmChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = baseValue,
            onValueChange = { onBaseChange(it.filter(Char::isDigit).take(4)) },
            label = { Text("$vehicleLabel Base (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = kmValue,
            onValueChange = { onKmChange(it.filter(Char::isDigit).take(3)) },
            label = { Text("$vehicleLabel /km (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
