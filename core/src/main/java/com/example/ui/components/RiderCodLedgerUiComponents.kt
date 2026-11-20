package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.cloud.RiderCodLedgerSnapshot
import com.example.data.cloud.RiderCodSettlementRecord
import com.example.data.model.CustomerOrder
import com.example.data.model.RiderProfile
import java.util.Locale

private val LedgerNavy = Color(0xFF0F172A)
private val LedgerSlate = Color(0xFF475569)
private val LedgerTeal = Color(0xFF0F766E)
private val LedgerEmerald = Color(0xFF059669)
private val LedgerAmber = Color(0xFFD97706)
private val LedgerCrimson = Color(0xFFDC2626)

/**
 * Porter-style Rider COD Ledger Balance & ₹200 20% Commission Lock Card.
 * - ₹0 Due  -> Green FREE badge (Ready for orders)
 * - ₹1-199  -> Amber balance progress bar
 * - ≥ ₹200  -> Red LOCKED state blocking new orders until Rider transfers commission to Admin & Admin verifies ₹0 reset
 */
@Composable
fun RiderLedgerBalanceCard(
    ledger: RiderCodLedgerSnapshot,
    adminUpiId: String,
    adminAccountLabel: String,
    isDarkMode: Boolean,
    onSubmitTransferClaim: (amountInr: Double, referenceUtr: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTransferDialog by remember { mutableStateOf(false) }
    var utrDraft by remember { mutableStateOf("") }
    var amountDraft by remember(ledger.unpaidCommissionDueInr) {
        mutableStateOf(
            if (ledger.unpaidCommissionDueInr >= 1.0) ledger.unpaidCommissionDueInr.toInt().toString() else ""
        )
    }

    val statusColor = when {
        ledger.isLockedAtLimit -> LedgerCrimson
        ledger.isFreeZeroDue -> LedgerEmerald
        else -> LedgerAmber
    }
    val statusBadgeText = when {
        ledger.isLockedAtLimit -> "🔴 AUTO-LOCKED • Pay ₹${ledger.unpaidCommissionDueInr.toInt()}"
        ledger.withdrawableWalletBalanceInr >= 1.0 -> "🟢 FREE • Wallet +₹${ledger.withdrawableWalletBalanceInr.toInt()}"
        ledger.isFreeZeroDue -> "🟢 FREE • ₹0 Due"
        else -> "🟡 Due -₹${ledger.unpaidCommissionDueInr.toInt()} / ₹${ledger.codLimitInr.toInt()}"
    }
    val cardBg = when {
        ledger.isLockedAtLimit -> if (isDarkMode) Color(0xFF2B1215) else Color(0xFFFEF2F2)
        ledger.isFreeZeroDue -> if (isDarkMode) Color(0xFF0D281E) else Color(0xFFF0FDF4)
        else -> if (isDarkMode) Color(0xFF1E293B) else Color.White
    }
    val headingColor = if (isDarkMode) Color.White else LedgerNavy
    val subColor = if (isDarkMode) Color(0xFFCBD5E1) else LedgerSlate

    if (showTransferDialog) {
        val payAmt = amountDraft.toDoubleOrNull()?.coerceAtLeast(1.0) ?: ledger.unpaidCommissionDueInr.coerceAtLeast(1.0)
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Transfer to Admin",
                    tint = LedgerTeal,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Transfer ${ledger.commissionPercent}% COD Commission",
                    fontWeight = FontWeight.ExtraBold,
                    color = LedgerNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Send ₹${ledger.unpaidCommissionDueInr.toInt()} company commission to Admin's account & enter your 6+ character UPI UTR / Payment Ref. Your Ledger resets to ₹0 (🟢 FREE) & AUTO-UNLOCKS immediately — no manual Admin wait!",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerSlate
                    )
                    Surface(
                        color = Color(0xFFF0FDFA),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ADMIN SETTLEMENT ACCOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = LedgerTeal
                            )
                            Text(
                                text = if (adminUpiId.isNotBlank()) {
                                    "UPI ID: $adminUpiId"
                                } else {
                                    "UPI ID not set yet — ask Admin or pay at Cash Desk"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = LedgerNavy
                            )
                            if (adminAccountLabel.isNotBlank()) {
                                Text(
                                    text = "Account: $adminAccountLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerSlate
                                )
                            }
                        }
                    }

                    if (adminUpiId.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val upiUri = Uri.Builder()
                                    .scheme("upi")
                                    .authority("pay")
                                    .appendQueryParameter("pa", adminUpiId.trim())
                                    .appendQueryParameter("pn", adminAccountLabel.ifBlank { "Quick Delivery Admin" })
                                    .appendQueryParameter("am", String.format(Locale.US, "%.2f", payAmt))
                                    .appendQueryParameter("cu", "INR")
                                    .appendQueryParameter("tn", "COD Commission ${ledger.riderId}")
                                    .build()
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LedgerTeal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rider_cod_open_upi_app_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = "Open UPI App",
                                tint = LedgerTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pay ₹${payAmt.toInt()} via UPI App",
                                fontWeight = FontWeight.Bold,
                                color = LedgerTeal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amountDraft,
                        onValueChange = { raw -> amountDraft = raw.filter(Char::isDigit).take(6) },
                        label = { Text("Amount Transferred (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_cod_transfer_amount_input")
                    )
                    val hasValidUtrRef = utrDraft.trim().count { it.isLetterOrDigit() } >= 6
                    OutlinedTextField(
                        value = utrDraft,
                        onValueChange = { utrDraft = it.take(40) },
                        label = { Text("UPI UTR / Transaction Ref ID *") },
                        placeholder = { Text("e.g., 432198765432 (min 6 chars)") },
                        isError = utrDraft.isNotBlank() && !hasValidUtrRef,
                        supportingText = {
                            Text(
                                text = if (hasValidUtrRef) {
                                    "⚡ Valid Ref ID — Account will AUTO-UNLOCK instantly!"
                                } else {
                                    "Enter 6+ char UPI UTR / Ref ID for instant Auto-Unlock"
                                },
                                color = if (hasValidUtrRef) LedgerEmerald else LedgerSlate,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_cod_transfer_utr_input")
                    )
                }
            },
            confirmButton = {
                val hasValidUtrRef = utrDraft.trim().count { it.isLetterOrDigit() } >= 6
                Button(
                    onClick = {
                        val finalAmt = amountDraft.toDoubleOrNull() ?: ledger.unpaidCommissionDueInr
                        if (finalAmt > 0.0 && hasValidUtrRef) {
                            onSubmitTransferClaim(finalAmt, utrDraft.trim())
                            utrDraft = ""
                            showTransferDialog = false
                        }
                    },
                    enabled = (amountDraft.toDoubleOrNull() ?: 0.0) > 0.0 && hasValidUtrRef,
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerEmerald),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("rider_cod_submit_transfer_claim_btn")
                ) {
                    Text(
                        text = "Pay & Auto-Unlock",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (ledger.isLockedAtLimit) 2.dp else 1.dp, statusColor.copy(alpha = if (ledger.isLockedAtLimit) 0.9f else 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rider_cod_ledger_balance_card")
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
                        imageVector = if (ledger.isLockedAtLimit) Icons.Default.Lock else Icons.Default.AccountBalance,
                        contentDescription = "Ledger Balance",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unified Wallet & COD Ledger (${ledger.commissionPercent}%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = headingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Auto-locks at -₹${ledger.codLimitInr.toInt()} • Online orders auto-clear COD due first",
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress bar toward ₹200 COD limit
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { ledger.progressFraction },
                    color = statusColor,
                    trackColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (ledger.withdrawableWalletBalanceInr >= 1.0) {
                            "Net Balance: +₹${ledger.withdrawableWalletBalanceInr.toInt()} (COD Due ₹0)"
                        } else {
                            "Commission Due: -₹${ledger.unpaidCommissionDueInr.toInt()} / ₹${ledger.codLimitInr.toInt()}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = when {
                            ledger.isLockedAtLimit -> "AUTO-LOCKED"
                            ledger.isFreeZeroDue -> "FREE"
                            else -> "₹${ledger.remainingBeforeLockInr.toInt()} left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Automatic Online/Prepaid -> COD Offset Banner
            if (ledger.prepaidAutoOffsetAppliedInr >= 1.0) {
                Surface(
                    color = LedgerEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LedgerEmerald.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_cod_prepaid_auto_offset_banner")
                ) {
                    val extraWalletText = if (ledger.withdrawableWalletBalanceInr >= 1.0) {
                        " • +₹${ledger.withdrawableWalletBalanceInr.toInt()} credited to Wallet!"
                    } else {
                        "!"
                    }
                    Text(
                        text = "⚡ Auto-Adjusted: ₹${ledger.prepaidAutoOffsetAppliedInr.toInt()} COD due automatically cleared from Online order ${100 - ledger.commissionPercent}% earnings$extraWalletText",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF065F46),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 3 compact tiles: COD Cash | Online 80% (or Your 80%) | Wallet / Paid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedgerMiniMetric(
                    label = "COD Cash",
                    value = "₹${ledger.totalCodCashCollectedInr.toInt()}",
                    valueColor = headingColor,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )
                LedgerMiniMetric(
                    label = if (ledger.prepaidRider80CreditsInr >= 1.0) {
                        "Online ${100 - ledger.commissionPercent}%"
                    } else {
                        "Your ${100 - ledger.commissionPercent}%"
                    },
                    value = if (ledger.prepaidRider80CreditsInr >= 1.0) {
                        "+₹${ledger.prepaidRider80CreditsInr.toInt()}"
                    } else {
                        "₹${ledger.rider80ShareKeptInr.toInt()}"
                    },
                    valueColor = LedgerEmerald,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )
                LedgerMiniMetric(
                    label = if (ledger.withdrawableWalletBalanceInr >= 1.0) "Wallet Bal" else "Paid Admin",
                    value = if (ledger.withdrawableWalletBalanceInr >= 1.0) {
                        "+₹${ledger.withdrawableWalletBalanceInr.toInt()}"
                    } else {
                        "₹${ledger.verifiedPaidToAdminInr.toInt()}"
                    },
                    valueColor = if (ledger.withdrawableWalletBalanceInr >= 1.0) LedgerEmerald else LedgerTeal,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )
            }

            if (ledger.isLockedAtLimit) {
                Surface(
                    color = LedgerCrimson.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LedgerCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_cod_locked_warning_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Account Auto-Locked",
                            tint = LedgerCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "⛔ -₹${ledger.codLimitInr.toInt()} COD Limit Complete! Account is AUTO-LOCKED. Transfer ₹${ledger.unpaidCommissionDueInr.toInt()} via UPI & enter UTR to AUTO-UNLOCK immediately back to ₹0 (🟢 FREE)!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LedgerCrimson
                        )
                    }
                }
            }

            ledger.pendingClaimRecord?.let { claim ->
                Surface(
                    color = LedgerAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LedgerAmber.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_cod_pending_claim_banner")
                ) {
                    Text(
                        text = "⏳ Transfer Claim Submitted: ₹${claim.amountInr.toInt()} (Ref: ${claim.referenceUtr}) • Waiting for verification.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFF92400E),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (ledger.unpaidCommissionDueInr >= 1.0) {
                Button(
                    onClick = {
                        amountDraft = ledger.unpaidCommissionDueInr.toInt().toString()
                        showTransferDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ledger.isLockedAtLimit) LedgerCrimson else LedgerTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rider_cod_transfer_to_admin_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = "Pay and Auto-Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pay ₹${ledger.unpaidCommissionDueInr.toInt()} & Auto-Unlock Instantly",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerMiniMetric(
    label: String,
    value: String,
    valueColor: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkMode) Color(0xFF94A3B8) else LedgerSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Rider End-of-Day Shift Summary & 1-Tap WhatsApp Slip Card (₹0 Free).
 */
@Composable
fun RiderEndOfDayShiftSlipCard(
    riderName: String,
    riderId: String,
    todayTripsCount: Int,
    todayNetEarningInr: Int,
    ledger: RiderCodLedgerSnapshot,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rider_eod_shift_slip_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Shift Summary Slip",
                    tint = LedgerTeal,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Shift Slip ($todayTripsCount trips • ₹$todayNetEarningInr)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else LedgerNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Share COD cash & 80/20 settlement slip on WhatsApp",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkMode) Color(0xFFCBD5E1) else LedgerSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    val slip = QuickDeliveryOperationsManager.buildRiderShiftSummarySlipText(
                        riderName = riderName.ifBlank { "Rider Partner" },
                        riderId = riderId.ifBlank { "RIDER" },
                        todayTripsCount = todayTripsCount,
                        todayNetEarningInr = todayNetEarningInr,
                        ledger = ledger
                    )
                    runCatching {
                        val intent = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Quick Delivery Shift Slip — $riderName")
                                putExtra(Intent.EXTRA_TEXT, slip)
                            },
                            "Share Daily Shift Slip"
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LedgerEmerald),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("rider_share_shift_slip_whatsapp_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Shift Slip",
                    tint = LedgerEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Share Slip",
                    fontWeight = FontWeight.Bold,
                    color = LedgerEmerald,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Compact Admin Overview alert when any Rider is LOCKED at ₹200 COD limit or has submitted a transfer claim.
 */
@Composable
fun AdminCodPendingAlertBanner(
    riders: List<RiderProfile>,
    orders: List<CustomerOrder>,
    settlements: List<RiderCodSettlementRecord>,
    flags: AdminRemoteFeatureFlags,
    onOpenPaymentsPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val withdrawalRequests by QuickDeliveryOperationsManager.withdrawalRequests.collectAsState()
    val pendingClaimsCount = settlements.count { it.status == "PENDING_ADMIN" }
    val lockedRidersCount = riders.count { rider ->
        QuickDeliveryOperationsManager.computeRiderCodLedgerSnapshot(
            riderId = rider.riderId,
            riderCommissionDebtInr = rider.commissionDebtInr,
            orders = orders,
            settlements = settlements,
            flags = flags,
            withdrawalRequests = withdrawalRequests
        ).isLockedAtLimit
    }
    if (pendingClaimsCount == 0 && lockedRidersCount == 0) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, LedgerAmber),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_cod_pending_alert_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💵 Rider COD Commission Action Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF92400E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$pendingClaimsCount pending transfer claim(s) • $lockedRidersCount rider(s) locked at ₹${flags.riderCodCommissionLimitInr} limit",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerNavy
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenPaymentsPage,
                colors = ButtonDefaults.buttonColors(containerColor = LedgerAmber),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("admin_cod_alert_jump_payments_btn")
            ) {
                Text(
                    text = "Verify →",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Admin Daily COD Cash Handover & ₹200 Limit Verification Ledger Card.
 * Lets Admin verify Rider UPI/Cash transfer claims & reset Rider ledger to ₹0 (🟢 FREE) in 1 tap.
 */
@Composable
fun AdminCodCashHandoverLedgerCard(
    riders: List<RiderProfile>,
    orders: List<CustomerOrder>,
    settlements: List<RiderCodSettlementRecord>,
    flags: AdminRemoteFeatureFlags,
    onVerifyClaim: (RiderCodSettlementRecord) -> Unit,
    onMarkFullPaidResetZero: (RiderProfile, RiderCodLedgerSnapshot) -> Unit,
    onReverseSettlement: ((RiderCodSettlementRecord) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val withdrawalRequests by QuickDeliveryOperationsManager.withdrawalRequests.collectAsState()
    val pendingClaims = remember(settlements) {
        settlements.filter { it.status == "PENDING_ADMIN" }
    }
    val autoSettledAuditList = remember(settlements) {
        settlements.filter { it.status == "AUTO_SETTLED" }.take(5)
    }
    val riderLedgers = remember(riders, orders, settlements, flags, withdrawalRequests) {
        riders.map { rider ->
            rider to QuickDeliveryOperationsManager.computeRiderCodLedgerSnapshot(
                riderId = rider.riderId,
                riderCommissionDebtInr = rider.commissionDebtInr,
                orders = orders,
                settlements = settlements,
                flags = flags,
                withdrawalRequests = withdrawalRequests
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_cod_cash_handover_ledger_card")
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
                        text = "⚡ Autopilot COD & Wallet Ledger (₹${flags.riderCodCommissionLimitInr} Lock)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = LedgerNavy
                    )
                    Text(
                        text = "Auto-locks at -₹${flags.riderCodCommissionLimitInr}, auto-offsets via Online orders & auto-unlocks on valid UTR",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerSlate
                    )
                }
                Surface(
                    color = if (pendingClaims.isNotEmpty()) LedgerAmber.copy(alpha = 0.16f) else LedgerEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (pendingClaims.isNotEmpty()) "${pendingClaims.size} Pending" else "Autopilot ON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (pendingClaims.isNotEmpty()) LedgerAmber else LedgerEmerald,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (pendingClaims.isNotEmpty()) {
                Text(
                    text = "⏳ Pending Rider Transfer Claims (Verify Payment First)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = LedgerAmber
                )
                pendingClaims.forEach { claim ->
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LedgerAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pending_cod_claim_${claim.settlementId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${claim.riderName} (${claim.riderId})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LedgerNavy
                                    )
                                    Text(
                                        text = "Ref / UTR: ${claim.referenceUtr} • ${claim.settledAtLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerSlate
                                    )
                                }
                                Text(
                                    text = "₹${claim.amountInr.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = LedgerTeal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Button(
                                onClick = { onVerifyClaim(claim) },
                                colors = ButtonDefaults.buttonColors(containerColor = LedgerEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_verify_cod_claim_btn_${claim.settlementId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Verify Claim",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verify & Reset to ₹0 (Unlock Rider)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            if (autoSettledAuditList.isNotEmpty()) {
                Text(
                    text = "⚡ Recent Auto-Cleared Payments (Autopilot Audit Trail)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = LedgerEmerald
                )
                autoSettledAuditList.forEach { rec ->
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LedgerEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_auto_settled_cod_${rec.settlementId}")
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
                                        text = "${rec.riderName} (${rec.riderId}) • ₹${rec.amountInr.toInt()}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerNavy
                                    )
                                    Text(
                                        text = "UTR: ${rec.referenceUtr} • ${rec.settledAtLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerSlate
                                    )
                                }
                                Surface(
                                    color = LedgerEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "⚡ Auto-Cleared",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LedgerEmerald,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (onReverseSettlement != null) {
                                OutlinedButton(
                                    onClick = { onReverseSettlement(rec) },
                                    border = BorderStroke(1.dp, LedgerCrimson),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_reverse_cod_settlement_btn_${rec.settlementId}")
                                ) {
                                    Text(
                                        text = "Reverse & Re-Lock (Disputed UTR)",
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerCrimson,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            if (riderLedgers.isEmpty()) {
                Text(
                    text = "No active fleet riders yet. Once riders deliver COD or Online orders, their unified automatic 80/20 ledger appears live here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerSlate
                )
            } else {
                riderLedgers.forEach { (rider, ledger) ->
                    val rowColor = when {
                        ledger.isLockedAtLimit -> LedgerCrimson
                        ledger.isFreeZeroDue -> LedgerEmerald
                        else -> LedgerAmber
                    }
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (ledger.isLockedAtLimit) LedgerCrimson else Color(0xFFE2E8F0)),
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
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append("COD: ₹${ledger.totalCodCashCollectedInr.toInt()}")
                                            if (ledger.prepaidRider80CreditsInr >= 1.0) {
                                                append(" • Online 80%: +₹${ledger.prepaidRider80CreditsInr.toInt()}")
                                            }
                                            if (ledger.prepaidAutoOffsetAppliedInr >= 1.0) {
                                                append(" • Auto-Offset: ₹${ledger.prepaidAutoOffsetAppliedInr.toInt()}")
                                            }
                                            append(" • Paid: ₹${ledger.verifiedPaidToAdminInr.toInt()}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerSlate
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = rowColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = when {
                                            ledger.isLockedAtLimit -> "🔴 LOCKED -₹${ledger.unpaidCommissionDueInr.toInt()}"
                                            ledger.withdrawableWalletBalanceInr >= 1.0 -> "🟢 +₹${ledger.withdrawableWalletBalanceInr.toInt()} Wallet"
                                            ledger.isFreeZeroDue -> "🟢 FREE ₹0"
                                            else -> "🟡 Due -₹${ledger.unpaidCommissionDueInr.toInt()}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = rowColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (ledger.unpaidCommissionDueInr >= 1.0) {
                                OutlinedButton(
                                    onClick = { onMarkFullPaidResetZero(rider, ledger) },
                                    border = BorderStroke(1.dp, LedgerEmerald),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_mark_cod_paid_reset_zero_${rider.riderId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Mark Paid",
                                        tint = LedgerEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Mark ₹${ledger.unpaidCommissionDueInr.toInt()} Paid (Reset ₹0 FREE)",
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerEmerald,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
