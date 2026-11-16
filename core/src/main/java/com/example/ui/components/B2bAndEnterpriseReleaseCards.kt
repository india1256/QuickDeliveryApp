package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.cloud.B2bDukaanAccountRecord
import com.example.data.cloud.B2bDukaanStatus
import com.example.data.cloud.CloudB2bDukaanAccount
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryCloudRepository
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.repository.SharedOrderRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Locale
import kotlinx.coroutines.launch

private val EntTeal = Color(0xFF0A3D3F)
private val EntEmerald = Color(0xFF059669)
private val EntOrange = Color(0xFFEA580C)
private val EntNavy = Color(0xFF0F172A)
private val EntSlate = Color(0xFF64748B)
private val EntIndigo = Color(0xFF4338CA)

/**
 * Admin Future Expansion Switches (B2B Postpaid Default OFF, Razorpay Route 80/20 Split Default OFF,
 * Auto-KYC Instant API Default OFF), B2B Dukaan Credit Approval Desk, GST Invoice Viewer, and
 * Google Play Store (.aab) Launch Readiness Checklist.
 */
@Composable
fun AdminB2bAndEnterpriseReleaseCard(
    repository: SharedOrderRepository,
    flags: AdminRemoteFeatureFlags,
    onPushFlagUpdate: ((AdminRemoteFeatureFlags) -> AdminRemoteFeatureFlags, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val b2bAccounts by QuickDeliveryOperationsManager.b2bAccounts.collectAsState()
    var selectedInvoiceAccount by remember { mutableStateOf<B2bDukaanAccountRecord?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // A. Future Expansion Toggles (All Default OFF until Admin activates)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_future_expansion_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminIconBadge(icon = Icons.Default.Apartment, tint = EntIndigo, sizeDp = 34)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Future Scale & Enterprise Modules",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EntNavy
                        )
                        Text(
                            text = "All 3 switches stay OFF by default — activate anytime when scaling up",
                            style = MaterialTheme.typography.labelSmall,
                            color = EntSlate
                        )
                    }
                }

                feedbackBanner?.let { note ->
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EntEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // 1. B2B / Dukaan Monthly Postpaid & Bulk Booking (Default OFF)
                EnterpriseSwitchItem(
                    title = "B2B / Dukaan Monthly Postpaid & Bulk Order",
                    subtitle = if (flags.b2bPostpaidEnabled) {
                        "LIVE in Customer App • Credit cap ₹${flags.b2bDefaultCreditLimitInr} / ${flags.b2bBillingCycleDays}d cycle"
                    } else {
                        "Default OFF (Future Option) • Keeps Customer UI clean until you turn ON"
                    },
                    checked = flags.b2bPostpaidEnabled,
                    onCheckedChange = { enabled ->
                        onPushFlagUpdate(
                            { it.copy(b2bPostpaidEnabled = enabled) },
                            if (enabled) "✅ B2B Dukaan Monthly Postpaid & Bulk Order ACTIVATED!" else "B2B Postpaid turned OFF (hidden in Customer app)"
                        )
                    },
                    testTag = "admin_switch_b2b_postpaid"
                )
                if (flags.b2bPostpaidEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10000, 25000, 50000, 100000).forEach { limit ->
                            FilterChip(
                                selected = flags.b2bDefaultCreditLimitInr == limit,
                                onClick = {
                                    onPushFlagUpdate(
                                        { it.copy(b2bDefaultCreditLimitInr = limit) },
                                        "🏢 Default B2B Credit Limit set to ₹$limit"
                                    )
                                },
                                label = { Text("Limit ₹${limit / 1000}k", fontWeight = FontWeight.Bold) }
                            )
                        }
                        listOf(15, 30).forEach { days ->
                            FilterChip(
                                selected = flags.b2bBillingCycleDays == days,
                                onClick = {
                                    onPushFlagUpdate(
                                        { it.copy(b2bBillingCycleDays = days) },
                                        "📅 B2B Billing Cycle set to $days days"
                                    )
                                },
                                label = { Text("${days}d Bill", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 2. Razorpay Route 80/20 Auto-Split Switch (Default OFF)
                EnterpriseSwitchItem(
                    title = "Razorpay Route 80/20 Auto-Split (API)",
                    subtitle = if (flags.razorpayRouteSplitEnabled) {
                        "Auto-Split API Mode ON • Routes 80% Rider / 20% Admin on gateway capture"
                    } else {
                        "Default OFF • ₹0 Extra Cost Manual Payout Desk Active"
                    },
                    checked = flags.razorpayRouteSplitEnabled,
                    onCheckedChange = { enabled ->
                        onPushFlagUpdate(
                            { it.copy(razorpayRouteSplitEnabled = enabled) },
                            if (enabled) "⚡ Razorpay Route 80/20 Auto-Split switch ON (configure key in Secrets)" else "Free Manual Payout Desk active"
                        )
                    },
                    testTag = "admin_switch_razorpay_route_split"
                )

                // 3. Auto-KYC Instant API (DigiLocker / Surepass) (Default OFF)
                EnterpriseSwitchItem(
                    title = "Auto-KYC Instant API (DigiLocker / DL-RC)",
                    subtitle = if (flags.autoKycApiEnabled) {
                        "Auto-KYC API Check ON • Pre-screens DL, RC & Aadhaar formats"
                    } else {
                        "Default OFF • Free Admin Manual Selfie & Doc Approval Active"
                    },
                    checked = flags.autoKycApiEnabled,
                    onCheckedChange = { enabled ->
                        onPushFlagUpdate(
                            { it.copy(autoKycApiEnabled = enabled) },
                            if (enabled) "🪪 Auto-KYC Instant API mode ON" else "Free Manual Admin KYC Review active"
                        )
                    },
                    testTag = "admin_switch_auto_kyc_api"
                )
            }
        }

        // B. B2B Dukaan Credit Desk (Shown whenever B2B is ON OR accounts exist)
        if (flags.b2bPostpaidEnabled || b2bAccounts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_b2b_credit_desk_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = EntIndigo, modifier = Modifier.size(20.dp))
                        Text(
                            text = "B2B Dukaan Credit & Monthly GST Desk (${b2bAccounts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EntNavy
                        )
                    }
                    if (b2bAccounts.isEmpty()) {
                        Text(
                            text = "No Dukaan applications yet. Once turned ON, local shops can apply for Monthly Postpaid Credit right from the Customer App.",
                            style = MaterialTheme.typography.labelSmall,
                            color = EntSlate
                        )
                    } else {
                        b2bAccounts.forEach { acc ->
                            val statusColor = when (acc.status) {
                                B2bDukaanStatus.APPROVED -> EntEmerald
                                B2bDukaanStatus.SUSPENDED -> Color(0xFFDC2626)
                                B2bDukaanStatus.PENDING_ADMIN_APPROVAL -> EntOrange
                            }
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                                text = acc.shopName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EntNavy
                                            )
                                            Text(
                                                text = "${acc.ownerName} • ${acc.ownerPhone} • GSTIN: ${acc.gstin}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EntSlate
                                            )
                                        }
                                        Surface(
                                            color = statusColor.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = acc.status.name.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    val ratio = if (acc.approvedCreditLimitInr > 0) {
                                        (acc.usedCreditInr / acc.approvedCreditLimitInr.toDouble()).toFloat().coerceIn(0f, 1f)
                                    } else 0f
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        color = statusColor,
                                        trackColor = Color(0xFFE2E8F0),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "Due: ₹%.2f / Limit ₹%d • Orders: %d • Inv: %s",
                                            acc.usedCreditInr,
                                            acc.approvedCreditLimitInr,
                                            acc.totalBilledOrders,
                                            acc.gstInvoiceNumber
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EntNavy
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (acc.status != B2bDukaanStatus.APPROVED) {
                                            Button(
                                                onClick = {
                                                    val limit = flags.b2bDefaultCreditLimitInr.coerceAtLeast(10000)
                                                    QuickDeliveryOperationsManager.approveOrManageB2bAccountByAdmin(
                                                        context = context,
                                                        accountId = acc.accountId,
                                                        newStatus = B2bDukaanStatus.APPROVED,
                                                        creditLimitInr = limit
                                                    )
                                                    if (QuickDeliveryFirebaseInitializer.ensureInitialized(context) != null &&
                                                        runCatching { Firebase.auth.currentUser }.getOrNull() != null
                                                    ) {
                                                        scope.launch {
                                                            runCatching {
                                                                QuickDeliveryCloudRepository(context)
                                                                    .approveOrUpdateB2bAccountByAdmin(acc.accountId, "APPROVED", limit)
                                                            }
                                                        }
                                                    }
                                                    feedbackBanner = "✅ Approved ${acc.shopName} with ₹$limit Monthly Credit!"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EntEmerald),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Approve (₹${flags.b2bDefaultCreditLimitInr})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (acc.usedCreditInr > 0.0) {
                                            Button(
                                                onClick = {
                                                    QuickDeliveryOperationsManager.approveOrManageB2bAccountByAdmin(
                                                        context = context,
                                                        accountId = acc.accountId,
                                                        newStatus = B2bDukaanStatus.APPROVED,
                                                        creditLimitInr = acc.approvedCreditLimitInr,
                                                        settleMonthlyBill = true
                                                    )
                                                    if (QuickDeliveryFirebaseInitializer.ensureInitialized(context) != null &&
                                                        runCatching { Firebase.auth.currentUser }.getOrNull() != null
                                                    ) {
                                                        scope.launch {
                                                            runCatching {
                                                                QuickDeliveryCloudRepository(context)
                                                                    .approveOrUpdateB2bAccountByAdmin(acc.accountId, "APPROVED", acc.approvedCreditLimitInr, resetUsedCreditToZero = true)
                                                            }
                                                        }
                                                    }
                                                    feedbackBanner = "💵 Monthly bill settled & limit restored for ${acc.shopName}!"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EntTeal),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Mark Bill Paid", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { selectedInvoiceAccount = acc },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("GST Invoice", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        if (acc.status == B2bDukaanStatus.APPROVED) {
                                            OutlinedButton(
                                                onClick = {
                                                    QuickDeliveryOperationsManager.approveOrManageB2bAccountByAdmin(
                                                        context = context,
                                                        accountId = acc.accountId,
                                                        newStatus = B2bDukaanStatus.SUSPENDED,
                                                        creditLimitInr = acc.approvedCreditLimitInr
                                                    )
                                                    feedbackBanner = "Paused credit line for ${acc.shopName}"
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Pause", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // C. Google Play Store Launch Readiness (.aab Production Checklist)
        Surface(
            color = Color(0xFFF0F9FF),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_play_store_readiness_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color(0xFF0369A1), modifier = Modifier.size(20.dp))
                    Text(
                        text = "Google Play Store Release (.AAB) Readiness",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EntNavy
                    )
                }
                Text(
                    text = "✓ Cloud Firestore Named Database & Zero-Trust Rules Live\n" +
                        "✓ Google Credential Manager Sign-In + App Check Ready\n" +
                        "✓ Photo Picker (No Broad Storage Permission) + Play Policy Safe\n" +
                        "✓ Custom Domain Legal Pages (${com.example.data.cloud.QuickDeliveryBrandLinks.PRIMARY_DOMAIN})\n" +
                        "✓ Signed .aab Bundle Generator Ready (3 Standalone Package IDs)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0C4A6E)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(com.example.data.cloud.QuickDeliveryBrandLinks.DELETE_ACCOUNT_URL)
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Account Deletion URL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(com.example.data.cloud.QuickDeliveryBrandLinks.TERMS_OF_SERVICE_URL)
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Terms of Service", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    selectedInvoiceAccount?.let { acc ->
        val invoiceText = remember(acc) { QuickDeliveryOperationsManager.buildB2bConsolidatedGstInvoiceText(acc) }
        AlertDialog(
            onDismissRequest = { selectedInvoiceAccount = null },
            title = {
                Text(
                    text = "Consolidated Monthly GST Invoice",
                    fontWeight = FontWeight.ExtraBold,
                    color = EntNavy
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = invoiceText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = EntNavy
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(invoiceText))
                        feedbackBanner = "📄 GST Invoice copied for ${acc.shopName}!"
                        selectedInvoiceAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EntTeal)
                ) {
                    Text("Copy Tax Invoice")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInvoiceAccount = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun EnterpriseSwitchItem(
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
                color = EntNavy
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = EntSlate
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EntEmerald
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

/**
 * Customer App Step-1 B2B Dukaan Monthly Postpaid & Bulk Order Hub.
 * STRICTLY HIDDEN when flags.b2bPostpaidEnabled == false (Admin default OFF).
 */
@Composable
fun CustomerB2bDukaanStep1Card(
    flags: AdminRemoteFeatureFlags,
    customerName: String,
    customerPhone: String,
    pickupAddress: String,
    onBulkOrderSubmit: (dropAddresses: List<String>, shopName: String) -> Unit
) {
    if (!flags.b2bPostpaidEnabled) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cleanPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(customerPhone)
    val b2bAccounts by QuickDeliveryOperationsManager.b2bAccounts.collectAsState()
    val myAccount = remember(b2bAccounts, cleanPhone) {
        b2bAccounts.firstOrNull {
            cleanPhone.isNotBlank() && it.ownerPhone == cleanPhone
        } ?: b2bAccounts.firstOrNull()
    }

    var showRegisterDialog by remember { mutableStateOf(false) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var showInvoiceDialog by remember { mutableStateOf(false) }

    var shopNameInput by remember { mutableStateOf(myAccount?.shopName ?: "") }
    var gstinInput by remember { mutableStateOf(myAccount?.gstin ?: "") }
    var hubAddressInput by remember { mutableStateOf(myAccount?.pickupAddress ?: pickupAddress) }
    var bulkDrop1 by remember { mutableStateOf("") }
    var bulkDrop2 by remember { mutableStateOf("") }
    var bulkDrop3 by remember { mutableStateOf("") }
    var localStatus by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, Color(0xFFA5B4FC)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_b2b_dukaan_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Apartment, contentDescription = null, tint = EntIndigo, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = myAccount?.shopName ?: "Dukaan / B2B Monthly Postpaid",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EntNavy
                    )
                    Text(
                        text = when (myAccount?.status) {
                            B2bDukaanStatus.APPROVED -> String.format(
                                Locale.US,
                                "Approved • Available Credit ₹%.0f / ₹%d (%dd Cycle)",
                                myAccount.availableCreditInr,
                                myAccount.approvedCreditLimitInr,
                                flags.b2bBillingCycleDays
                            )
                            B2bDukaanStatus.PENDING_ADMIN_APPROVAL -> "KYC Under Admin Review • Approval unlocks ₹0-now monthly billing"
                            B2bDukaanStatus.SUSPENDED -> "Monthly Credit Paused by Admin"
                            null -> "Book parcels all month & settle 1 consolidated GST bill"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = EntIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            localStatus?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = EntEmerald,
                    fontWeight = FontWeight.Bold
                )
            }

            if (myAccount == null) {
                Button(
                    onClick = { showRegisterDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EntIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("customer_b2b_apply_btn")
                ) {
                    Text("Activate Free B2B Dukaan Credit Line", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (myAccount.status == B2bDukaanStatus.APPROVED) {
                        Button(
                            onClick = { showBulkDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EntIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("customer_b2b_bulk_btn")
                        ) {
                            Text("Bulk Dispatch (2-3 Stops)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = { showInvoiceDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("customer_b2b_invoice_btn")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GST Invoice", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Dukaan / B2B Postpaid Enrollment", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shopNameInput,
                        onValueChange = { shopNameInput = it },
                        label = { Text("Shop / Business Name*") },
                        placeholder = { Text("e.g., Gupta Electronics & Wholesalers") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gstinInput,
                        onValueChange = { gstinInput = it.uppercase() },
                        label = { Text("GSTIN (Optional or URD)") },
                        placeholder = { Text("07AAACQ1234F1Z5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hubAddressInput,
                        onValueChange = { hubAddressInput = it },
                        label = { Text("Shop Pickup Hub Address*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rec = QuickDeliveryOperationsManager.registerOrUpdateB2bDukaanRequest(
                            context = context,
                            shopName = shopNameInput,
                            ownerName = customerName.ifBlank { "Store Owner" },
                            ownerPhone = customerPhone.ifBlank { "9876543210" },
                            gstin = gstinInput,
                            pickupAddress = hubAddressInput.ifBlank { pickupAddress.ifBlank { "Shop Hub" } }
                        )
                        if (QuickDeliveryFirebaseInitializer.ensureInitialized(context) != null &&
                            runCatching { Firebase.auth.currentUser }.getOrNull() != null
                        ) {
                            scope.launch {
                                runCatching {
                                    QuickDeliveryCloudRepository(context).createB2bAccountRequest(
                                        CloudB2bDukaanAccount(
                                            accountId = rec.accountId,
                                            shopName = rec.shopName,
                                            ownerName = rec.ownerName,
                                            ownerPhone = rec.ownerPhone,
                                            gstin = rec.gstin,
                                            pickupAddress = rec.pickupAddress,
                                            gstInvoiceNumber = rec.gstInvoiceNumber
                                        )
                                    )
                                }
                            }
                        }
                        localStatus = "✅ Application submitted (${rec.accountId}) — Waiting for Admin Approval."
                        showRegisterDialog = false
                    },
                    enabled = shopNameInput.trim().length >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = EntIndigo)
                ) {
                    Text("Submit B2B Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBulkDialog && myAccount != null) {
        AlertDialog(
            onDismissRequest = { showBulkDialog = false },
            title = { Text("Bulk Dukaan Dispatch", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dispatch up to 3 customer drops together from ${myAccount.shopName}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = EntSlate
                    )
                    OutlinedTextField(
                        value = bulkDrop1,
                        onValueChange = { bulkDrop1 = it },
                        label = { Text("Drop Address 1*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bulkDrop2,
                        onValueChange = { bulkDrop2 = it },
                        label = { Text("Drop Address 2*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bulkDrop3,
                        onValueChange = { bulkDrop3 = it },
                        label = { Text("Drop Address 3 (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val drops = listOf(bulkDrop1, bulkDrop2, bulkDrop3).map { it.trim() }.filter { it.length >= 3 }
                        if (drops.isNotEmpty()) {
                            onBulkOrderSubmit(drops, myAccount.shopName)
                            bulkDrop1 = ""
                            bulkDrop2 = ""
                            bulkDrop3 = ""
                            showBulkDialog = false
                        }
                    },
                    enabled = bulkDrop1.trim().length >= 3 && bulkDrop2.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = EntIndigo)
                ) {
                    Text("Dispatch All Drops")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showInvoiceDialog && myAccount != null) {
        val invText = remember(myAccount) { QuickDeliveryOperationsManager.buildB2bConsolidatedGstInvoiceText(myAccount) }
        AlertDialog(
            onDismissRequest = { showInvoiceDialog = false },
            title = { Text("Dukaan Monthly GST Bill", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = invText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )
            },
            confirmButton = {
                Button(onClick = { showInvoiceDialog = false }) { Text("Done") }
            }
        )
    }
}
