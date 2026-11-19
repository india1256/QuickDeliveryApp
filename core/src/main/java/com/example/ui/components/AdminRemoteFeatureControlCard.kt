package com.example.ui.components

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.RiderProfile
import com.example.data.repository.SharedOrderRepository
import java.util.Locale

private val CtrlTeal = Color(0xFF0A3D3F)
private val CtrlEmerald = Color(0xFF10B981)
private val CtrlOrange = Color(0xFFF97316)
private val CtrlNavy = Color(0xFF0F172A)
private val CtrlCrimson = Color(0xFFDC2626)
private val CtrlSlate = Color(0xFF64748B)

/**
 * Admin Settings Card: Master Feature Switches synced live across Customer & Rider Partner apps.
 * All future-rollout features start OFF by default so Admin can activate them anytime with 1 tap.
 */
@Composable
fun AdminRemoteFeatureSwitchesCard(
    repository: SharedOrderRepository
) {
    val context = LocalContext.current
    remember(context) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    val flags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()

    var maintenanceDraft by remember(flags.maintenanceMessage) { mutableStateOf(flags.maintenanceMessage) }
    var pinCodesDraft by remember(flags.serviceablePinCodesCsv) { mutableStateOf(flags.serviceablePinCodesCsv) }
    var broadcastDraft by remember(flags.adminBroadcastMessage) { mutableStateOf(flags.adminBroadcastMessage) }
    var broadcastTarget by remember(flags.adminBroadcastTarget) { mutableStateOf(flags.adminBroadcastTarget) }
    var securityPinDraft by remember(flags.adminSecurityPin) { mutableStateOf(flags.adminSecurityPin) }
    var feedbackNote by remember { mutableStateOf<String?>(null) }

    fun pushUpdate(transform: (AdminRemoteFeatureFlags) -> AdminRemoteFeatureFlags, note: String) {
        val next = transform(flags)
        QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(context, next, repository)
        feedbackNote = note
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_remote_feature_switches_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminIconBadge(icon = Icons.Default.Tune, tint = CtrlTeal, sizeDp = 36)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Feature Control & Remote Config",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = CtrlNavy
                    )
                    Text(
                        text = "Manage live capabilities across Customer & Rider Partner apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = CtrlSlate
                    )
                }
            }

            feedbackNote?.let { msg ->
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CtrlEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 1. Scheduled Delivery
            FeatureToggleRow(
                title = "Scheduled Delivery (Later Date & Slot)",
                subtitle = "Default OFF • Shows Now vs Schedule Later selector in Customer Step 1",
                checked = flags.scheduledDeliveryEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(scheduledDeliveryEnabled = enabled) },
                        if (enabled) "✅ Scheduled Delivery ACTIVATED for Customer & Rider" else "Scheduled Delivery paused"
                    )
                },
                testTag = "admin_switch_scheduled_delivery"
            )

            // 2. Multi-Drop / Round-Trip
            FeatureToggleRow(
                title = "Multi-Drop / Round-Trip Extra Stop",
                subtitle = "Default OFF • Allows customers to add an extra drop stop (+₹40)",
                checked = flags.multiDropEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(multiDropEnabled = enabled) },
                        if (enabled) "✅ Multi-Drop / Round-Trip ACTIVATED" else "Multi-Drop paused"
                    )
                },
                testTag = "admin_switch_multi_drop"
            )

            // 3. Dynamic Surge Pricing
            FeatureToggleRow(
                title = "Peak / Rain Surge Pricing (${String.format(Locale.US, "%.2fx", flags.surgeMultiplier)})",
                subtitle = "Default OFF • Applies surge multiplier live on fares & Rider payouts",
                checked = flags.surgePricingEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(surgePricingEnabled = enabled) },
                        if (enabled) "⚡ Surge Pricing ${flags.surgeMultiplier}x ACTIVATED live" else "Surge Pricing reset to 1.0x Normal"
                    )
                },
                testTag = "admin_switch_surge_pricing"
            )
            if (flags.surgePricingEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.15, 1.25, 1.5, 2.0).forEach { mult ->
                        FilterChip(
                            selected = kotlin.math.abs(flags.surgeMultiplier - mult) < 0.01,
                            onClick = {
                                pushUpdate(
                                    { it.copy(surgeMultiplier = mult) },
                                    "⚡ Surge multiplier set to ${mult}x live"
                                )
                            },
                            label = { Text("${mult}x", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // 4. Parcel Insurance Add-On
            FeatureToggleRow(
                title = "Parcel Damage Insurance (+₹${flags.insuranceFeeInr})",
                subtitle = "Default OFF • Optional up to ₹5,000 coverage checkbox in Checkout",
                checked = flags.parcelInsuranceEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(parcelInsuranceEnabled = enabled) },
                        if (enabled) "🛡️ Parcel Insurance Add-On ACTIVATED" else "Parcel Insurance hidden"
                    )
                },
                testTag = "admin_switch_parcel_insurance"
            )
            if (flags.parcelInsuranceEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 25, 50).forEach { fee ->
                        FilterChip(
                            selected = flags.insuranceFeeInr == fee,
                            onClick = {
                                pushUpdate(
                                    { it.copy(insuranceFeeInr = fee) },
                                    "🛡️ Insurance fee set to ₹$fee"
                                )
                            },
                            label = { Text("₹$fee", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // 5. Referral & Wallet Bonus
            FeatureToggleRow(
                title = "Referral & Wallet Bonus (₹${flags.referralBonusInr})",
                subtitle = "Default OFF • Invite & Earn card in Customer & Rider Partner apps",
                checked = flags.referralWalletEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(referralWalletEnabled = enabled) },
                        if (enabled) "🎁 Referral & Wallet Rewards ACTIVATED (₹${flags.referralBonusInr})" else "Referral Program paused"
                    )
                },
                testTag = "admin_switch_referral_wallet"
            )
            if (flags.referralWalletEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25, 50, 100, 150).forEach { bonus ->
                        FilterChip(
                            selected = flags.referralBonusInr == bonus,
                            onClick = {
                                pushUpdate(
                                    { it.copy(referralBonusInr = bonus) },
                                    "🎁 Referral bonus set to ₹$bonus"
                                )
                            },
                            label = { Text("₹$bonus", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // 6. Cash On Delivery (COD) Switch & Max Limit
            FeatureToggleRow(
                title = "Cash On Delivery (Max Cap ₹${flags.maxCodLimitInr})",
                subtitle = "Toggle COD ON/OFF or cap COD limit at Customer Checkout",
                checked = flags.codEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(codEnabled = enabled) },
                        if (enabled) "💵 Cash On Delivery ENABLED (Cap ₹${flags.maxCodLimitInr})" else "🚫 Cash On Delivery DISABLED (Prepaid UPI/Card only)"
                    )
                },
                testTag = "admin_switch_cod_enabled"
            )
            if (flags.codEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(500, 1000, 2000, 5000).forEach { cap ->
                        FilterChip(
                            selected = flags.maxCodLimitInr == cap,
                            onClick = {
                                pushUpdate(
                                    { it.copy(maxCodLimitInr = cap) },
                                    "💵 Max COD cap updated to ₹$cap"
                                )
                            },
                            label = { Text("Cap ₹$cap", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 7. Live Platform Commission % Slider / Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Live Revenue Split: ${flags.riderSharePercent}% Rider / ${flags.platformCommissionPercent}% Platform",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CtrlNavy
                )
                Text(
                    text = "Set 0% for Festival Zero-Commission offers or adjust anytime",
                    style = MaterialTheme.typography.labelSmall,
                    color = CtrlSlate
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 10, 15, 20, 25).forEach { pct ->
                        FilterChip(
                            selected = flags.platformCommissionPercent == pct,
                            onClick = {
                                pushUpdate(
                                    { it.copy(platformCommissionPercent = pct) },
                                    "💰 Split updated: ${100 - pct}% Rider / $pct% Admin"
                                )
                            },
                            label = {
                                Text(
                                    text = if (pct == 0) "0% (100% Rider)" else "$pct% Admin",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("admin_commission_chip_$pct")
                        )
                    }
                }
            }

            HorizontalDivider()

            // 8. Daily Rider Mission Target
            FeatureToggleRow(
                title = "Daily Rider Target Mission (+₹${flags.riderMissionBonusInr} for ${flags.riderMissionTargetOrders} trips)",
                subtitle = "Default OFF • Shows live trip progress & bonus tracker in Rider app",
                checked = flags.riderTargetMissionEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(riderTargetMissionEnabled = enabled) },
                        if (enabled) "🎯 Daily Rider Mission ACTIVATED" else "Daily Rider Mission paused"
                    )
                },
                testTag = "admin_switch_rider_mission"
            )
            if (flags.riderTargetMissionEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3 to 100, 5 to 150, 8 to 250, 10 to 400).forEach { (trips, bonus) ->
                        FilterChip(
                            selected = flags.riderMissionTargetOrders == trips,
                            onClick = {
                                pushUpdate(
                                    { it.copy(riderMissionTargetOrders = trips, riderMissionBonusInr = bonus) },
                                    "🎯 Mission updated: $trips trips → +₹$bonus"
                                )
                            },
                            label = { Text("$trips trips = ₹$bonus", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 9. Maintenance / Holiday Banner & Serviceable PIN Codes
            FeatureToggleRow(
                title = "Maintenance / Holiday Banner",
                subtitle = "Show an urgent notice banner across Customer & Rider apps",
                checked = flags.maintenanceBannerEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        {
                            it.copy(
                                maintenanceBannerEnabled = enabled,
                                maintenanceMessage = maintenanceDraft.trim()
                                    .ifBlank { "Scheduled service window active. Thank you for your patience." }
                            )
                        },
                        if (enabled) "⚠️ Maintenance Banner LIVE across apps" else "Maintenance Banner turned OFF"
                    )
                },
                testTag = "admin_switch_maintenance"
            )
            OutlinedTextField(
                value = maintenanceDraft,
                onValueChange = { maintenanceDraft = it },
                label = { Text("Maintenance / Holiday Message") },
                placeholder = { Text("e.g., Heavy rain delay in some sectors") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pinCodesDraft,
                onValueChange = { pinCodesDraft = it },
                label = { Text("Allowed Serviceable PIN Codes (Blank = All India)") },
                placeholder = { Text("e.g., 110001, 122001, 201301") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    pushUpdate(
                        {
                            it.copy(
                                maintenanceMessage = maintenanceDraft.trim(),
                                serviceablePinCodesCsv = pinCodesDraft.trim()
                            )
                        },
                        "📍 Maintenance note & Serviceable PIN codes synced live!"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CtrlTeal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save Notice & PIN Restrictions", fontWeight = FontWeight.Bold)
            }

            HorizontalDivider()

            // 10. 1-Tap Broadcast Announcement + Corporate Templates
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminIconBadge(icon = Icons.Default.Campaign, tint = CtrlOrange, sizeDp = 30)
                    Text(
                        text = "Live Fleet & Customer Broadcast",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CtrlNavy
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All Apps", "CUSTOMER" to "Customers", "RIDER" to "Riders").forEach { (key, label) ->
                        FilterChip(
                            selected = broadcastTarget == key,
                            onClick = { broadcastTarget = key },
                            label = { Text(label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Rain Surge" to "Weather Advisory: Heavy rain in key hubs. Dynamic surge active; allow 10 extra mins.",
                        "Peak Bonus" to "Rider Incentive: Complete 5 deliveries during peak hours today for +150 bonus.",
                        "Service Update" to "Quick Delivery Express: Zero-fee priority dispatch available in active zones."
                    ).forEach { (chipLabel, templateText) ->
                        FilterChip(
                            selected = broadcastDraft == templateText,
                            onClick = { broadcastDraft = templateText },
                            label = { Text(chipLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = broadcastDraft,
                    onValueChange = { broadcastDraft = it },
                    label = { Text("Announcement Message") },
                    placeholder = { Text("e.g., Monsoon Bonus ₹50 extra per trip today!") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_broadcast_message_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            QuickDeliveryOperationsManager.sendBroadcastFromAdmin(
                                context = context,
                                message = broadcastDraft,
                                target = broadcastTarget,
                                repository = repository
                            )
                            feedbackNote = "📢 Broadcast sent live to $broadcastTarget!"
                        },
                        enabled = broadcastDraft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CtrlOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("admin_send_broadcast_btn")
                    ) {
                        Text("Broadcast Now", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = {
                            broadcastDraft = ""
                            QuickDeliveryOperationsManager.sendBroadcastFromAdmin(
                                context = context,
                                message = "",
                                target = broadcastTarget,
                                repository = repository
                            )
                            feedbackNote = "Broadcast cleared"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Clear", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider()

            // 11. Admin 4-Digit Security PIN Lock
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CtrlTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (flags.adminSecurityPin.length == 4) {
                            "Admin App 4-Digit Security PIN: ACTIVE"
                        } else {
                            "Admin App 4-Digit Security PIN (Optional Lock)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CtrlNavy
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = securityPinDraft,
                        onValueChange = { securityPinDraft = it.filter(Char::isDigit).take(4) },
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("e.g., 2580") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            pushUpdate(
                                { it.copy(adminSecurityPin = securityPinDraft) },
                                if (securityPinDraft.length == 4) {
                                    "🔐 Admin 4-Digit Security PIN enabled!"
                                } else {
                                    "🔓 Admin PIN Lock disabled"
                                }
                            )
                        },
                        enabled = securityPinDraft.length == 4 || securityPinDraft.isEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = CtrlTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (securityPinDraft.length == 4) "Save PIN" else "Disable PIN", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider()

            // 12. Pickup Wait-Time Timer & Cancellation Penalty Rule
            FeatureToggleRow(
                title = "Pickup Wait-Time & Cancellation Rule (${flags.freeWaitMinutes}m free • ₹${flags.cancellationPenaltyInr} cancel)",
                subtitle = "Default OFF • Gives Rider a pickup gate wait timer & shows Customer cancellation rule notice",
                checked = flags.waitTimeAndCancelRuleEnabled,
                onCheckedChange = { enabled ->
                    pushUpdate(
                        { it.copy(waitTimeAndCancelRuleEnabled = enabled) },
                        if (enabled) "⏱️ Wait-Time & Cancellation Rule ACTIVATED!" else "Wait-Time & Cancellation Rule paused"
                    )
                },
                testTag = "admin_switch_wait_time_cancel_rule"
            )
            if (flags.waitTimeAndCancelRuleEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 10).forEach { mins ->
                        FilterChip(
                            selected = flags.freeWaitMinutes == mins,
                            onClick = {
                                pushUpdate(
                                    { it.copy(freeWaitMinutes = mins) },
                                    "⏱️ Free Pickup Wait set to $mins mins"
                                )
                            },
                            label = { Text("${mins}m free", fontWeight = FontWeight.Bold) }
                        )
                    }
                    listOf(2, 5, 10).forEach { fee ->
                        FilterChip(
                            selected = flags.waitFeePerMinuteInr == fee,
                            onClick = {
                                pushUpdate(
                                    { it.copy(waitFeePerMinuteInr = fee) },
                                    "⏱️ Overtime Wait fee set to ₹$fee/min"
                                )
                            },
                            label = { Text("₹$fee/min wait", fontWeight = FontWeight.Bold) }
                        )
                    }
                    listOf(20, 30, 50).forEach { pen ->
                        FilterChip(
                            selected = flags.cancellationPenaltyInr == pen,
                            onClick = {
                                pushUpdate(
                                    { it.copy(cancellationPenaltyInr = pen) },
                                    "⚠️ Post-accept cancellation rule set to ₹$pen"
                                )
                            },
                            label = { Text("₹$pen cancel", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 13. Cloud DB Backup Export & Legal Links
            Surface(
                color = Color(0xFFF0FDFA),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CtrlTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cloud Backup & Legal Links",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CtrlNavy
                        )
                    }
                    Text(
                        text = "• Domain: ${com.example.data.cloud.QuickDeliveryBrandLinks.PRIMARY_DOMAIN}\n• Backup Export: /api/backup-export active\n• Privacy Policy: ${com.example.data.cloud.QuickDeliveryBrandLinks.PRIVACY_POLICY_URL}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0F766E)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val ts = System.currentTimeMillis()
                                val adminDevId = "admin-console-${android.os.Build.MODEL.filter(Char::isLetterOrDigit).take(16)}"
                                val sig = com.example.security.QuickDeliverySecurityGuard.computeSyncIntegritySignature(
                                    deviceId = adminDevId,
                                    role = "ADMIN",
                                    timestampEpochMs = ts
                                )
                                val uri = Uri.parse(com.example.data.cloud.QuickDeliveryBrandLinks.BACKUP_EXPORT_URL)
                                    .buildUpon()
                                    .appendQueryParameter("deviceId", adminDevId)
                                    .appendQueryParameter("ts", ts.toString())
                                    .appendQueryParameter("sig", sig)
                                    .build()
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                                feedbackNote = "📦 Downloading signed Cloud DB backup JSON..."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CtrlTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("admin_export_cloud_backup_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup JSON", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse(com.example.data.cloud.QuickDeliveryBrandLinks.PRIVACY_POLICY_URL)
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("admin_open_privacy_policy_btn")
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Privacy Policy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider()

            // 15. Future Expansion Switches (B2B Postpaid Default OFF, 80/20 Route Split Default OFF, Auto-KYC API Default OFF), B2B Credit Desk & Play Store .aab Readiness
            AdminB2bAndEnterpriseReleaseCard(
                repository = repository,
                flags = flags,
                onPushFlagUpdate = { transform, note -> pushUpdate(transform, note) }
            )
        }
    }
}

@Composable
private fun FeatureToggleRow(
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = CtrlNavy
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = CtrlSlate
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CtrlEmerald
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

/**
 * Admin Overview Intelligence Cards:
 * 1. Live Stuck / Unassigned Order Alert with 1-Tap Auto-Reassign.
 * 2. Pin-Code / Area Demand Heatmap aggregated from real synced orders.
 */
@Composable
fun AdminStuckOrderAlertAndHeatmapCards(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    repository: SharedOrderRepository
) {
    val context = LocalContext.current
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    val waitingOrders = remember(orders) {
        orders.filter { it.orderStatus == OrderStatus.SEARCHING_RIDER }
    }

    val pinHeatmap = remember(orders) {
        val pinRegex = Regex("\\b\\d{6}\\b")
        val grouped = orders.groupBy { o ->
            pinRegex.find(o.pickupAddress.fullAddress)?.value
                ?: o.pickupAddress.fullAddress.substringAfterLast(",").trim().take(18).ifBlank { "Local Zone" }
        }
        grouped.map { (zone, list) ->
            val gmv = list.sumOf {
                it.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
            }
            Triple(zone, list.size, gmv)
        }.sortedByDescending { it.second }.take(5)
    }
    val maxZoneCount = (pinHeatmap.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Live Stuck / Waiting Order Alert Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (waitingOrders.isNotEmpty()) Color(0xFFFFF7ED) else Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.5.dp,
                if (waitingOrders.isNotEmpty()) CtrlOrange else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_stuck_order_alert_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (waitingOrders.isNotEmpty()) CtrlOrange else CtrlEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (waitingOrders.isNotEmpty()) {
                                    "⚠️ ${waitingOrders.size} Unassigned Order(s) Waiting for Rider"
                                } else {
                                    "✅ Zero Unassigned Orders — Dispatch Healthy"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = CtrlNavy
                            )
                            Text(
                                text = if (waitingOrders.isNotEmpty()) {
                                    "Orders waiting in queue: ${waitingOrders.take(3).joinToString { "#${it.orderId}" }}"
                                } else {
                                    "All booked orders are matched or completed"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = CtrlSlate
                            )
                        }
                    }
                    if (waitingOrders.isNotEmpty()) {
                        Button(
                            onClick = {
                                AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                                val onlineRider = riders.firstOrNull { it.isOnline && it.accountStatus == "ACTIVE" }
                                    ?: repository.riders.value.firstOrNull { it.isOnline }
                                val firstWaiting = waitingOrders.first()
                                val assigned = if (onlineRider != null) {
                                    QuickDeliveryOperationsManager.reassignOrderToRider(
                                        context = context,
                                        repository = repository,
                                        orderId = firstWaiting.orderId,
                                        targetRider = onlineRider
                                    )
                                } else {
                                    AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                                        repository = repository,
                                        orderId = firstWaiting.orderId
                                    ).assignedRider != null
                                }
                                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                actionFeedback = if (assigned) {
                                    "⚡ Order #${firstWaiting.orderId} dispatched to ${onlineRider?.name ?: "nearest Rider"}!"
                                } else {
                                    "No online rider available right now — waiting for a Partner to go Online."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CtrlOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-Assign", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                actionFeedback?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CtrlTeal
                    )
                }
            }
        }

        // 2. Live Supply vs Demand Zone Alert & Pin-Code Heatmap Card
        val onlineRidersCount = remember(riders) {
            riders.count { it.isOnline && it.accountStatus == "ACTIVE" }
        }
        val flags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
        val zoneDemandStats = remember(orders, onlineRidersCount) {
            val pinRegex = Regex("\\b\\d{6}\\b")
            val activeWaiting = orders.filter { it.orderStatus == OrderStatus.SEARCHING_RIDER }
            val waitingByZone = activeWaiting.groupBy { o ->
                pinRegex.find(o.pickupAddress.fullAddress)?.value
                    ?: o.pickupAddress.fullAddress.substringAfterLast(",").trim().take(18).ifBlank { "Local Zone" }
            }
            pinHeatmap.map { (zone, totalCount, gmv) ->
                val waitingInZone = waitingByZone[zone]?.size ?: 0
                Triple(zone, waitingInZone, totalCount to gmv)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (waitingOrders.size > onlineRidersCount && waitingOrders.isNotEmpty()) CtrlCrimson else Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_pincode_heatmap_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🗺️ Live Supply vs Demand & Zone Heatmap",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = CtrlNavy
                        )
                        Text(
                            text = "Online Riders: $onlineRidersCount • Waiting Orders: ${waitingOrders.size}" +
                                if (flags.surgePricingEnabled) " • Surge LIVE (${flags.surgeMultiplier}x)" else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (waitingOrders.size > onlineRidersCount) CtrlCrimson else CtrlTeal
                        )
                    }
                    Button(
                        onClick = {
                            val topZone = zoneDemandStats.firstOrNull()?.first ?: "ALL"
                            val nextFlags = flags.copy(
                                surgePricingEnabled = !flags.surgePricingEnabled,
                                surgeMultiplier = maxOf(flags.surgeMultiplier, 1.35),
                                activeSurgeZoneCsv = if (!flags.surgePricingEnabled) topZone else ""
                            )
                            QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(
                                context = context,
                                updated = nextFlags,
                                repository = repository
                            )
                            actionFeedback = if (nextFlags.surgePricingEnabled) {
                                "⚡ Zone $topZone Surge (${nextFlags.surgeMultiplier}x) ACTIVATED live!"
                            } else {
                                "Zone Surge turned OFF (1.0x Normal)"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flags.surgePricingEnabled) CtrlEmerald else CtrlOrange
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("admin_zone_surge_trigger_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (flags.surgePricingEnabled) "Surge ON ✓" else "Surge Zone",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (zoneDemandStats.isEmpty()) {
                    Text(
                        text = "Zone demand & rider supply ratio will populate automatically as orders arrive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CtrlSlate
                    )
                } else {
                    zoneDemandStats.forEach { (zone, waitingInZone, countAndGmv) ->
                        val (count, gmv) = countAndGmv
                        val ratio = (count.toFloat() / maxZoneCount.toFloat()).coerceIn(0.1f, 1f)
                        val isUnderserved = waitingInZone > onlineRidersCount || waitingInZone >= 2
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isUnderserved) "🔥 Zone $zone (Low Rider Supply)" else "Zone $zone",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnderserved) CtrlCrimson else CtrlNavy
                                )
                                Text(
                                    text = "$count orders ($waitingInZone waiting) • ₹${gmv.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isUnderserved) CtrlOrange else CtrlTeal
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = if (isUnderserved || ratio > 0.7f) CtrlOrange else CtrlEmerald,
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
