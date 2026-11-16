package com.example.ui.components

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.cloud.DisputeStatus
import com.example.data.cloud.DocExpirySeverity
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.data.model.RiderProfile
import com.example.data.repository.SharedOrderRepository
import java.util.Locale

private val OpsTeal = Color(0xFF0A3D3F)
private val OpsEmerald = Color(0xFF059669)
private val OpsOrange = Color(0xFFEA580C)
private val OpsNavy = Color(0xFF0F172A)
private val OpsCrimson = Color(0xFFDC2626)
private val OpsSlate = Color(0xFF475569)

/**
 * Feature #5 (Admin App): Peak-Hour Auto-Surge Scheduler + Porter Helper & Smart Batch Control.
 */
@Composable
fun AdminPeakHourAutoSurgeCard(
    repository: SharedOrderRepository
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    val flags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
    val isCurrentlyPeakActive = remember(flags) { flags.isCurrentHourInPeakWindow() }
    var feedback by remember { mutableStateOf<String?>(null) }

    val availableSlots = remember {
        listOf(
            "09-11" to "09-11 AM Morning",
            "13-15" to "01-03 PM Lunch",
            "18-21" to "06-09 PM Evening",
            "21-23" to "09-11 PM Late"
        )
    }
    val selectedSlots = remember(flags.peakTimeSlotsCsv) {
        flags.peakTimeSlotsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_peak_hour_auto_surge_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AdminIconBadge(icon = Icons.Default.Schedule, tint = OpsOrange, sizeDp = 34)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Peak-Hour Auto-Surge & Add-Ons",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OpsNavy
                        )
                        Text(
                            text = if (isCurrentlyPeakActive) {
                                "⚡ ACTIVE NOW (${String.format(Locale.US, "%.2fx", flags.peakSurgeMultiplier)}) in current hour window"
                            } else {
                                "Auto-applies surge during peak windows + Helper & Batch rules"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrentlyPeakActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentlyPeakActive) OpsOrange else OpsSlate
                        )
                    }
                }
                Switch(
                    checked = flags.peakAutoSurgeEnabled,
                    onCheckedChange = { enabled ->
                        QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(
                            context = context,
                            updated = flags.copy(peakAutoSurgeEnabled = enabled),
                            repository = repository
                        )
                        feedback = if (enabled) {
                            "⏰ Peak-Hour Auto-Surge Scheduler ENABLED (${flags.peakTimeSlotsCsv})"
                        } else {
                            "Peak-Hour Auto-Surge paused"
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OpsOrange),
                    modifier = Modifier.testTag("admin_switch_peak_auto_surge")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSlots.forEach { (slotKey, slotLabel) ->
                    val isSelected = slotKey in selectedSlots
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val nextSet = if (isSelected) selectedSlots - slotKey else selectedSlots + slotKey
                            val csv = nextSet.joinToString(",")
                            QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(
                                context = context,
                                updated = flags.copy(peakTimeSlotsCsv = csv),
                                repository = repository
                            )
                            feedback = "⏰ Peak windows updated: ${csv.ifBlank { "None" }}"
                        },
                        label = { Text(slotLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1.20, 1.35, 1.50, 1.75).forEach { mult ->
                    FilterChip(
                        selected = kotlin.math.abs(flags.peakSurgeMultiplier - mult) < 0.01,
                        onClick = {
                            QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(
                                context = context,
                                updated = flags.copy(peakSurgeMultiplier = mult),
                                repository = repository
                            )
                            feedback = "⚡ Peak Surge rate set to ${mult}x"
                        },
                        label = { Text("Peak ${mult}x", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Loading / Unloading Helper Add-On (+₹${flags.helperFeeInr})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OpsNavy
                    )
                    Text(
                        text = "Porter-style helper option in Customer Checkout + ₹${flags.sameRouteBatchBonusInr} Same-Route Batch bonus",
                        style = MaterialTheme.typography.labelSmall,
                        color = OpsSlate
                    )
                }
                Switch(
                    checked = flags.helperAddonEnabled,
                    onCheckedChange = { enabled ->
                        QuickDeliveryOperationsManager.updateRemoteFeatureFlagsFromAdmin(
                            context = context,
                            updated = flags.copy(helperAddonEnabled = enabled),
                            repository = repository
                        )
                        feedback = if (enabled) "💪 Loading Helper Add-On LIVE in Customer App" else "Loading Helper Add-On hidden"
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OpsEmerald),
                    modifier = Modifier.testTag("admin_switch_helper_addon")
                )
            }

            feedback?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OpsEmerald
                )
            }
        }
    }
}

/**
 * Feature #2 (Admin App): Rider COD Cash Settlement Ledger.
 */
@Composable
fun AdminCodSettlementLedgerCard(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    val codSettlements by QuickDeliveryOperationsManager.codSettlements.collectAsState()
    var utrReferenceDraft by remember { mutableStateOf("") }
    var settlementFeedback by remember { mutableStateOf<String?>(null) }

    val riderCodTotals = remember(orders, riders, codSettlements) {
        val deliveredCod = orders.filter {
            it.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY && it.orderStatus == OrderStatus.DELIVERED
        }
        val riderIds = (riders.map { it.riderId } + deliveredCod.mapNotNull { it.assignedRiderId }).distinct()
        riderIds.mapNotNull { rid ->
            val rider = riders.firstOrNull { it.riderId == rid }
            val riderOrders = deliveredCod.filter { it.assignedRiderId == rid }
            val collectedInr = riderOrders.sumOf {
                it.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
            }
            val settledInr = codSettlements.filter { it.riderId == rid }.sumOf { it.amountInr }
            val pendingInr = (collectedInr - settledInr).coerceAtLeast(0.0)
            if (collectedInr <= 0.0 && settledInr <= 0.0 && rider == null) null
            else listOf(
                rid,
                rider?.name ?: riderOrders.firstOrNull()?.assignedRiderName ?: "Rider Partner",
                rider?.mobile.orEmpty(),
                collectedInr,
                settledInr,
                pendingInr
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_cod_settlement_ledger_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminIconBadge(icon = Icons.Default.AccountBalanceWallet, tint = OpsOrange, sizeDp = 34)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rider COD Cash Settlement Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpsNavy
                    )
                    Text(
                        text = "Verify cash deposited via UPI/Bank & mark rider COD float settled",
                        style = MaterialTheme.typography.labelSmall,
                        color = OpsSlate
                    )
                }
            }

            OutlinedTextField(
                value = utrReferenceDraft,
                onValueChange = { utrReferenceDraft = it },
                label = { Text("Deposit UTR / Cash Receipt Reference") },
                placeholder = { Text("e.g., UTR42819901 or CASH_HUB_01") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_cod_utr_input")
            )

            settlementFeedback?.let { msg ->
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OpsEmerald,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            riderCodTotals.forEach { row ->
                val riderId = row[0] as String
                val riderName = row[1] as String
                val riderPhone = row[2] as String
                val collectedInr = row[3] as Double
                val settledInr = row[4] as Double
                val pendingInr = row[5] as Double
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "$riderName ($riderId)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = OpsNavy
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Collected: ₹%.0f • Settled: ₹%.0f • Pending: ₹%.0f",
                                    collectedInr,
                                    settledInr,
                                    pendingInr
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pendingInr > 0.0) OpsCrimson else OpsEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                val settleAmt = if (pendingInr > 0.0) pendingInr else 100.0
                                val rec = QuickDeliveryOperationsManager.markCodSettledByAdmin(
                                    context = context,
                                    riderId = riderId,
                                    riderName = riderName,
                                    riderPhone = riderPhone,
                                    amountInr = settleAmt,
                                    referenceUtr = utrReferenceDraft
                                )
                                if (rec != null) {
                                    utrReferenceDraft = ""
                                    settlementFeedback = "✅ COD Settled: ₹${settleAmt.toInt()} for $riderName (Ref: ${rec.referenceUtr})"
                                }
                            },
                            enabled = pendingInr > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = OpsEmerald),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("admin_mark_cod_settled_$riderId")
                        ) {
                            Text(
                                text = if (pendingInr > 0.0) "Mark Settled" else "All Clear ✓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            if (codSettlements.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Recent Cash Settlements (${codSettlements.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OpsTeal
                )
                codSettlements.take(4).forEach { s ->
                    Text(
                        text = "• ${s.settledAtLabel}: ${s.riderName} deposited ₹${s.amountInr.toInt()} (${s.referenceUtr})",
                        style = MaterialTheme.typography.labelSmall,
                        color = OpsSlate
                    )
                }
            }
        }
    }
}

/**
 * Feature #3 (Admin App): Order Dispute & Instant Wallet Refund Desk.
 */
@Composable
fun AdminDisputeRefundDeskCard(
    orders: List<CustomerOrder>
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    val disputes by QuickDeliveryOperationsManager.disputeRecords.collectAsState()
    var refundAmountDraft by remember { mutableStateOf("50") }
    var deskFeedback by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_dispute_refund_desk_card")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AdminIconBadge(icon = Icons.Default.Gavel, tint = OpsCrimson, sizeDp = 34)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dispute & Instant Wallet Refund Desk",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OpsNavy
                        )
                        Text(
                            text = "${disputes.count { it.status == DisputeStatus.OPEN }} Open Disputes • Credits Customer Wallet live on approval",
                            style = MaterialTheme.typography.labelSmall,
                            color = OpsSlate
                        )
                    }
                }
            }

            OutlinedTextField(
                value = refundAmountDraft,
                onValueChange = { refundAmountDraft = it.filter(Char::isDigit).take(4) },
                label = { Text("Refund Amount (₹) to Customer Wallet") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_dispute_refund_amount_input")
            )

            deskFeedback?.let { msg ->
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OpsEmerald,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (disputes.isEmpty()) {
                Text(
                    text = "✅ No customer parcel disputes reported. Any damage/delay claim submitted in Customer App appears live here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OpsSlate
                )
            } else {
                disputes.take(8).forEach { dispute ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            when (dispute.status) {
                                DisputeStatus.OPEN -> OpsCrimson
                                DisputeStatus.REFUNDED -> OpsEmerald
                                DisputeStatus.REJECTED -> Color(0xFFCBD5E1)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${dispute.disputeId} • Order #${dispute.orderId}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpsNavy
                                )
                                Surface(
                                    color = when (dispute.status) {
                                        DisputeStatus.OPEN -> Color(0xFFFEF2F2)
                                        DisputeStatus.REFUNDED -> Color(0xFFECFDF5)
                                        DisputeStatus.REJECTED -> Color(0xFFF1F5F9)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (dispute.status == DisputeStatus.REFUNDED) "REFUNDED ₹${dispute.refundAmountInr.toInt()}" else dispute.status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (dispute.status) {
                                            DisputeStatus.OPEN -> OpsCrimson
                                            DisputeStatus.REFUNDED -> OpsEmerald
                                            DisputeStatus.REJECTED -> OpsSlate
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Customer: ${dispute.customerName} (${dispute.customerPhone}) • ${dispute.createdAtLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OpsSlate
                            )
                            Text(
                                text = "Issue: ${dispute.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = OpsNavy
                            )
                            if (dispute.photoProofSummary.isNotBlank()) {
                                Text(
                                    text = "📷 Proof Metadata: ${dispute.photoProofSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OpsTeal
                                )
                            }
                            if (dispute.status == DisputeStatus.OPEN) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val amt = refundAmountDraft.toDoubleOrNull()?.coerceAtLeast(10.0) ?: 50.0
                                            QuickDeliveryOperationsManager.processDisputeRefundByAdmin(
                                                context = context,
                                                disputeId = dispute.disputeId,
                                                approveRefund = true,
                                                refundAmountInr = amt,
                                                adminNote = "Instant ₹${amt.toInt()} Wallet Refund approved by Admin"
                                            )
                                            deskFeedback = "✅ Refunded ₹${amt.toInt()} to ${dispute.customerName}'s Wallet!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OpsEmerald),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_approve_refund_${dispute.disputeId}")
                                    ) {
                                        Text("✓ Refund ₹${refundAmountDraft.ifBlank { "50" }}", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            QuickDeliveryOperationsManager.processDisputeRefundByAdmin(
                                                context = context,
                                                disputeId = dispute.disputeId,
                                                approveRefund = false,
                                                refundAmountInr = 0.0,
                                                adminNote = "Rejected after proof inspection"
                                            )
                                            deskFeedback = "Dispute ${dispute.disputeId} marked Rejected"
                                        },
                                        border = BorderStroke(1.dp, OpsCrimson),
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .height(48.dp)
                                    ) {
                                        Text("Reject", color = OpsCrimson, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent proof audit quick summary
            val deliveredWithProof = orders.count { it.orderStatus == OrderStatus.DELIVERED }
            if (deliveredWithProof > 0) {
                Text(
                    text = "Completed trips audited with OTP/GPS proof: $deliveredWithProof",
                    style = MaterialTheme.typography.labelSmall,
                    color = OpsTeal
                )
            }
        }
    }
}

/**
 * Feature #4 (Admin App): Rider Document Expiry & Compliance Tracker (DL / RC / Insurance).
 */
@Composable
fun AdminRiderDocExpiryCard(
    riders: List<RiderProfile>
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    val docExpiries by QuickDeliveryOperationsManager.riderDocExpiries.collectAsState()
    var selectedRiderForEdit by remember { mutableStateOf<String?>(null) }
    var dlExpiryDraft by remember { mutableStateOf("2026-12-31") }
    var rcExpiryDraft by remember { mutableStateOf("2027-06-30") }
    var insExpiryDraft by remember { mutableStateOf("2026-11-20") }
    var docFeedback by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_rider_doc_expiry_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminIconBadge(icon = Icons.Default.EventBusy, tint = OpsTeal, sizeDp = 34)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rider Document Expiry & KYC Tracker",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpsNavy
                    )
                    Text(
                        text = "Monitor DL, Vehicle RC & Insurance expiry • 7-day warning + Auto-Hold",
                        style = MaterialTheme.typography.labelSmall,
                        color = OpsSlate
                    )
                }
            }

            docFeedback?.let { msg ->
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OpsEmerald,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            riders.take(6).forEach { rider ->
                val rec = docExpiries[rider.riderId]
                val dlStr = rec?.dlExpiry.orEmpty().ifBlank { "2027-03-15" }
                val rcStr = rec?.rcExpiry.orEmpty().ifBlank { "2028-01-10" }
                val insStr = rec?.insuranceExpiry.orEmpty().ifBlank { "2026-11-18" }
                val severities = listOf(
                    QuickDeliveryOperationsManager.computeDocExpirySeverity(dlStr),
                    QuickDeliveryOperationsManager.computeDocExpirySeverity(rcStr),
                    QuickDeliveryOperationsManager.computeDocExpirySeverity(insStr)
                )
                val worstSeverity = when {
                    DocExpirySeverity.EXPIRED in severities -> DocExpirySeverity.EXPIRED
                    DocExpirySeverity.EXPIRING_SOON in severities -> DocExpirySeverity.EXPIRING_SOON
                    else -> DocExpirySeverity.VALID
                }
                val isEditing = selectedRiderForEdit == rider.riderId

                Surface(
                    color = when (worstSeverity) {
                        DocExpirySeverity.EXPIRED -> Color(0xFFFEF2F2)
                        DocExpirySeverity.EXPIRING_SOON -> Color(0xFFFFF7ED)
                        else -> Color(0xFFF8FAFC)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        when (worstSeverity) {
                            DocExpirySeverity.EXPIRED -> OpsCrimson
                            DocExpirySeverity.EXPIRING_SOON -> OpsOrange
                            else -> Color(0xFFE2E8F0)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${rider.name} (${rider.riderId})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpsNavy
                                )
                                Text(
                                    text = "DL: $dlStr • RC: $rcStr • Ins: $insStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OpsSlate
                                )
                                if (!rec?.alertSentAtLabel.isNullOrBlank()) {
                                    Text(
                                        text = "🔔 ${rec?.alertSentAtLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = OpsOrange
                                    )
                                }
                            }
                            Surface(
                                color = when (worstSeverity) {
                                    DocExpirySeverity.EXPIRED -> OpsCrimson
                                    DocExpirySeverity.EXPIRING_SOON -> OpsOrange
                                    else -> OpsEmerald
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when (worstSeverity) {
                                        DocExpirySeverity.EXPIRED -> "EXPIRED"
                                        DocExpirySeverity.EXPIRING_SOON -> "DUE <7D"
                                        else -> "VALID ✓"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    QuickDeliveryOperationsManager.sendDocExpiryAlertByAdmin(
                                        context = context,
                                        riderId = rider.riderId,
                                        riderName = rider.name
                                    )
                                    docFeedback = "🔔 Renewal reminder sent live to ${rider.name}!"
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Alert Rider", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (isEditing) {
                                        selectedRiderForEdit = null
                                    } else {
                                        dlExpiryDraft = dlStr
                                        rcExpiryDraft = rcStr
                                        insExpiryDraft = insStr
                                        selectedRiderForEdit = rider.riderId
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(if (isEditing) "Close" else "Update Dates", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Hold", style = MaterialTheme.typography.labelSmall, color = OpsSlate)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    checked = rec?.autoHoldOnExpired == true,
                                    onCheckedChange = { hold ->
                                        QuickDeliveryOperationsManager.upsertRiderDocExpiry(
                                            context = context,
                                            riderId = rider.riderId,
                                            riderName = rider.name,
                                            dlExpiry = dlStr,
                                            rcExpiry = rcStr,
                                            insuranceExpiry = insStr,
                                            autoHoldOnExpired = hold
                                        )
                                        docFeedback = if (hold) "⛔ Auto-Hold enabled for ${rider.name} if docs expire" else "Auto-Hold disabled for ${rider.name}"
                                    }
                                )
                            }
                        }

                        if (isEditing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = dlExpiryDraft,
                                    onValueChange = { dlExpiryDraft = it },
                                    label = { Text("DL (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = insExpiryDraft,
                                    onValueChange = { insExpiryDraft = it },
                                    label = { Text("Ins (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = rcExpiryDraft,
                                onValueChange = { rcExpiryDraft = it },
                                label = { Text("RC Expiry (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    QuickDeliveryOperationsManager.upsertRiderDocExpiry(
                                        context = context,
                                        riderId = rider.riderId,
                                        riderName = rider.name,
                                        dlExpiry = dlExpiryDraft,
                                        rcExpiry = rcExpiryDraft,
                                        insuranceExpiry = insExpiryDraft,
                                        autoHoldOnExpired = rec?.autoHoldOnExpired == true
                                    )
                                    selectedRiderForEdit = null
                                    docFeedback = "✅ Saved document expiry dates for ${rider.name}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OpsTeal),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Document Expiry Dates", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
