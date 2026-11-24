package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.cloud.DisputeStatus
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.model.CustomerOrder

private val EnhTeal = Color(0xFF078B8D)
private val EnhOrange = Color(0xFFFF7A1A)
private val EnhNavy = Color(0xFF0D1B2A)
private val EnhSlate = Color(0xFF475569)
private val EnhGreen = Color(0xFF059669)

@Composable
fun CustomerRemoteAlertsBanner(
    flags: AdminRemoteFeatureFlags,
    activePinCode: String = "",
    onClick: (() -> Unit)? = null
) {
    var dismissedEpochMs by remember { mutableLongStateOf(0L) }
    val targetMatches = flags.adminBroadcastTarget == "ALL" ||
        flags.adminBroadcastTarget == "CUSTOMER" ||
        (flags.adminBroadcastTarget.startsWith("PIN:") &&
            activePinCode.isNotBlank() &&
            flags.adminBroadcastTarget.removePrefix("PIN:").trim() == activePinCode.trim())
    val showBroadcast = flags.adminBroadcastMessage.isNotBlank() &&
        targetMatches &&
        flags.adminBroadcastEpochMs > 0L &&
        flags.adminBroadcastEpochMs != dismissedEpochMs

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (flags.maintenanceBannerEnabled) {
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, Color(0xFFD97706)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                    .testTag("customer_maintenance_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Service Notice",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = flags.maintenanceMessage.ifBlank {
                                "Scheduled service window active. Delivery ETAs may vary."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
        }

        if (showBroadcast) {
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                    .testTag("customer_admin_broadcast_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF1D4ED8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quick Delivery Announcement",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = flags.adminBroadcastMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E40AF)
                        )
                    }
                    IconButton(onClick = { dismissedEpochMs = flags.adminBroadcastEpochMs }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss announcement", tint = Color(0xFF1D4ED8))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerRepeatLastOrderCard(
    lastOrder: CustomerOrder,
    onRepeatRoute: (CustomerOrder) -> Unit
) {
    Surface(
        color = Color(0xFFF0FDFA),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, EnhTeal.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_repeat_last_order_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Repeat Last Order • #${lastOrder.orderId}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = EnhTeal
                )
                Text(
                    text = "${lastOrder.pickupAddress.fullAddress.substringBefore(",")} → ${lastOrder.dropAddress.fullAddress.substringBefore(",")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EnhNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = { onRepeatRoute(lastOrder) },
                border = BorderStroke(1.dp, EnhTeal),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("customer_repeat_last_order_btn")
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, tint = EnhTeal, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Fill Route",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EnhTeal,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun CustomerScheduledAndMultiDropCard(
    flags: AdminRemoteFeatureFlags,
    isScheduled: Boolean,
    onToggleScheduled: (Boolean) -> Unit,
    selectedDateLabel: String,
    onSelectDateLabel: (String) -> Unit,
    selectedTimeSlot: String,
    onSelectTimeSlot: (String) -> Unit,
    isMultiDrop: Boolean,
    onToggleMultiDrop: (Boolean) -> Unit,
    extraStopAddress: String,
    onExtraStopChange: (String) -> Unit,
    extraStopFeeInr: Int
) {
    if (!flags.scheduledDeliveryEnabled && !flags.multiDropEnabled) return

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_remote_booking_addons_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (flags.scheduledDeliveryEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = EnhTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Schedule Pickup Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EnhNavy)
                            Text(
                                text = if (isScheduled) "$selectedDateLabel • $selectedTimeSlot" else "Immediate pickup (Now)",
                                style = MaterialTheme.typography.labelSmall,
                                color = EnhSlate
                            )
                        }
                    }
                    Switch(
                        checked = isScheduled,
                        onCheckedChange = onToggleScheduled,
                        modifier = Modifier.testTag("customer_schedule_toggle_switch")
                    )
                }

                if (isScheduled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Today", "Tomorrow").forEach { day ->
                            FilterChip(
                                selected = selectedDateLabel == day,
                                onClick = { onSelectDateLabel(day) },
                                label = { Text(day, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("10 AM - 12 PM", "12 PM - 3 PM", "3 PM - 6 PM", "6 PM - 9 PM").forEach { slot ->
                            FilterChip(
                                selected = selectedTimeSlot == slot,
                                onClick = { onSelectTimeSlot(slot) },
                                label = { Text(slot, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }

            if (flags.multiDropEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = EnhOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Extra Stop / Round-Trip (+₹$extraStopFeeInr)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EnhNavy)
                            Text("Add an intermediate stop along this route", style = MaterialTheme.typography.labelSmall, color = EnhSlate)
                        }
                    }
                    Switch(
                        checked = isMultiDrop,
                        onCheckedChange = onToggleMultiDrop,
                        modifier = Modifier.testTag("customer_multidrop_toggle_switch")
                    )
                }
                if (isMultiDrop) {
                    OutlinedTextField(
                        value = extraStopAddress,
                        onValueChange = onExtraStopChange,
                        label = { Text("Extra Stop / Return Point Address *") },
                        placeholder = { Text("Shop / Gate / Landmark for 2nd stop") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_extra_stop_input")
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerReferralWalletCard(
    flags: AdminRemoteFeatureFlags,
    customerName: String,
    customerPhone: String,
    walletBonusInr: Int,
    useWalletBonus: Boolean,
    onToggleUseWallet: (Boolean) -> Unit
) {
    if (!flags.referralWalletEnabled) return
    val context = LocalContext.current
    val ownCode = remember(customerName, customerPhone) {
        QuickDeliveryOperationsManager.buildPersonalReferralCode(customerName, customerPhone)
    }
    var friendCodeInput by remember { mutableStateOf("") }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, EnhOrange.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_referral_wallet_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = EnhOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Refer & Earn ₹${flags.referralBonusInr} Wallet Bonus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EnhNavy
                        )
                        Text(
                            text = "Your Code: $ownCode • Balance: ₹$walletBonusInr",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EnhOrange
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Send parcels fast with Quick Delivery! Use my invite code $ownCode to earn ₹${flags.referralBonusInr} off: ${com.example.data.cloud.QuickDeliveryBrandLinks.CUSTOMER_APK_URL}"
                            )
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Invite", tint = EnhOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Invite",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EnhOrange,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = friendCodeInput,
                    onValueChange = { friendCodeInput = it.uppercase().filter(Char::isLetterOrDigit) },
                    label = { Text("Have a friend's code?") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        QuickDeliveryOperationsManager.claimReferralBonus(context, friendCodeInput, ownCode)
                            .onSuccess { credited ->
                                feedbackMsg = "✅ ₹$credited added to your Referral Wallet!"
                                friendCodeInput = ""
                            }
                            .onFailure { err ->
                                feedbackMsg = err.message
                            }
                    },
                    enabled = friendCodeInput.length >= 5,
                    colors = ButtonDefaults.buttonColors(containerColor = EnhOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "Claim",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (walletBonusInr > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use ₹$walletBonusInr Wallet Bonus on this order",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EnhGreen,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = useWalletBonus,
                        onCheckedChange = onToggleUseWallet
                    )
                }
            }

            feedbackMsg?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (msg.startsWith("✅")) EnhGreen else Color(0xFFB91C1C)
                )
            }
        }
    }
}

@Composable
fun CustomerParcelInsuranceCard(
    flags: AdminRemoteFeatureFlags,
    isInsured: Boolean,
    onToggleInsured: (Boolean) -> Unit
) {
    if (!flags.parcelInsuranceEnabled) return
    Surface(
        color = if (isInsured) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, if (isInsured) EnhGreen else Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_parcel_insurance_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isInsured,
                onCheckedChange = onToggleInsured,
                modifier = Modifier.testTag("customer_parcel_insurance_checkbox")
            )
            Icon(Icons.Default.Shield, contentDescription = null, tint = EnhGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Parcel Damage Protection Cover",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = EnhNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = EnhGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "+₹${flags.insuranceFeeInr}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EnhGreen,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Optional cover up to ₹5,000 against accidental transit damage or loss",
                    style = MaterialTheme.typography.labelSmall,
                    color = EnhSlate
                )
            }
        }
    }
}

@Composable
fun CustomerShareLiveTrackingButton(
    order: CustomerOrder,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val summary = buildString {
                appendLine("Quick Delivery Live Tracking • #${order.orderId}")
                appendLine("Status: ${order.orderStatus.displayTitle}")
                appendLine("Pickup: ${order.pickupAddress.fullAddress}")
                appendLine("Drop: ${order.dropAddress.fullAddress}")
                if (!order.assignedRiderName.isNullOrBlank()) {
                    appendLine("Rider: ${order.assignedRiderName} (${order.assignedRiderPhone.orEmpty()})")
                }
                append("Track / Download Quick Delivery: ${com.example.data.cloud.QuickDeliveryBrandLinks.CUSTOMER_APK_URL}")
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Track Parcel #${order.orderId} - Quick Delivery")
                putExtra(Intent.EXTRA_TEXT, summary)
            }
            runCatching {
                context.startActivity(Intent.createChooser(shareIntent, "Share Live Tracking"))
            }
        },
        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("customer_share_live_tracking_btn")
    ) {
        Icon(Icons.Default.Share, contentDescription = "Share Tracking", tint = Color(0xFF7DD3FC), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Share Live Order Tracking",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CustomerHelperAddonCard(
    flags: AdminRemoteFeatureFlags,
    isHelperAdded: Boolean,
    onToggleHelper: (Boolean) -> Unit
) {
    if (!flags.helperAddonEnabled) return
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHelperAdded) Color(0xFFFFF7ED) else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, if (isHelperAdded) EnhOrange else Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_helper_addon_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "💪 Loading / Unloading Helper (+₹${flags.helperFeeInr})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EnhNavy
                )
                Text(
                    text = "Porter-style doorstep lift & heavy-parcel carry assistance by Rider",
                    style = MaterialTheme.typography.labelSmall,
                    color = EnhSlate
                )
            }
            Switch(
                checked = isHelperAdded,
                onCheckedChange = onToggleHelper,
                modifier = Modifier.testTag("customer_helper_addon_switch")
            )
        }
    }
}

@Composable
fun CustomerSendReceiverOtpCard(
    order: CustomerOrder,
    otpEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receiverName = order.dropAddress.contactName.ifBlank { "Receiver" }
    val buildShareMessage: () -> String = {
        buildString {
            appendLine("Hi $receiverName! Your parcel (#${order.orderId}) is on the way via Quick Delivery.")
            appendLine("Pickup: ${order.pickupAddress.fullAddress}")
            appendLine("Drop: ${order.dropAddress.fullAddress}")
            if (!order.assignedRiderName.isNullOrBlank()) {
                val riderPhonePart = order.assignedRiderPhone?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                appendLine("Rider Partner: ${order.assignedRiderName}$riderPhonePart")
            }
            if (otpEnabled && order.paymentMethod != com.example.data.model.CustomerPaymentMethod.CASH_ON_DELIVERY) {
                appendLine("DROP OTP (Share with Rider at Delivery): ${order.deliveryOtp}")
            } else if (order.paymentMethod == com.example.data.model.CustomerPaymentMethod.CASH_ON_DELIVERY) {
                appendLine("Payment Mode: Cash on Delivery (${order.totalAmount})")
            } else {
                appendLine("Payment Mode: Paid Online (${order.totalAmount}) — Direct Handover")
            }
            append("Track / Download Quick Delivery: ${com.example.data.cloud.QuickDeliveryBrandLinks.CUSTOMER_APK_URL}")
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1-Tap Direct WhatsApp Share (₹0 Free, falls back to WhatsApp Business or standard share sheet)
        Button(
            onClick = {
                val summary = buildShareMessage()
                val receiverDigits = order.dropAddress.contactPhone.filter(Char::isDigit).takeLast(10)
                val openedWhatsApp = runCatching {
                    val waIntent = if (receiverDigits.length == 10) {
                        val encoded = android.net.Uri.encode(summary)
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://wa.me/91$receiverDigits?text=$encoded")
                        ).apply {
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(Intent.EXTRA_TEXT, summary)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    context.startActivity(waIntent)
                    true
                }.getOrElse {
                    runCatching {
                        val waBizIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp.w4b")
                            putExtra(Intent.EXTRA_TEXT, summary)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(waBizIntent)
                        true
                    }.getOrDefault(false)
                }
                if (!openedWhatsApp) {
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            if (otpEnabled) "Quick Delivery Order #${order.orderId} & Drop OTP" else "Quick Delivery Order #${order.orderId}"
                        )
                        putExtra(Intent.EXTRA_TEXT, summary)
                    }
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(
                                fallbackIntent,
                                if (otpEnabled) "Share Order & OTP on WhatsApp" else "Share Order Details on WhatsApp"
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("customer_whatsapp_share_otp_btn")
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share on WhatsApp", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (otpEnabled) "💬 Share Order & OTP on WhatsApp" else "💬 Share Live Order on WhatsApp",
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = {
                val summary = buildShareMessage()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        if (otpEnabled) "Quick Delivery Drop OTP (${order.deliveryOtp}) - #${order.orderId}" else "Quick Delivery Tracking - #${order.orderId}"
                    )
                    putExtra(Intent.EXTRA_TEXT, summary)
                }
                runCatching {
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            if (otpEnabled) "Send Tracking & Drop OTP to Receiver" else "Send Live Tracking to Receiver"
                        )
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EnhOrange),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("customer_send_receiver_otp_btn")
        ) {
            Icon(Icons.Default.Share, contentDescription = "Send Receiver Details", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (otpEnabled) {
                    "Send Tracking & Drop OTP (${order.deliveryOtp}) to $receiverName"
                } else {
                    "Send Live Tracking to $receiverName"
                },
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CustomerOrderDisputeCard(
    order: CustomerOrder,
    photoProofSummary: String?
) {
    val context = LocalContext.current
    val disputes by QuickDeliveryOperationsManager.disputeRecords.collectAsState()
    val orderDispute = remember(disputes, order.orderId) {
        disputes.firstOrNull { it.orderId == order.orderId }
    }
    var showForm by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Delay in Delivery") }
    var noteInput by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_order_dispute_card")
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
                Text(
                    text = if (orderDispute != null) {
                        "⚖️ Dispute #${orderDispute.disputeId} (${orderDispute.status.name})"
                    } else {
                        "Need help with delay or parcel issue?"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (orderDispute == null) {
                    OutlinedButton(
                        onClick = { showForm = !showForm },
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("customer_toggle_dispute_form_btn")
                    ) {
                        Text(
                            text = if (showForm) "Close" else "Report Dispute",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFECACA)
                        )
                    }
                }
            }

            if (orderDispute != null) {
                val info = when (orderDispute.status) {
                    DisputeStatus.REFUNDED -> "✅ Admin approved instant wallet refund: +₹${orderDispute.refundAmountInr.toInt()} credited! (${orderDispute.adminNote})"
                    DisputeStatus.REJECTED -> "❌ Closed by Admin: ${orderDispute.adminNote}"
                    DisputeStatus.OPEN -> "⏳ Sent to Admin Dispute Desk: ${orderDispute.reason}"
                }
                Text(
                    text = info,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (orderDispute.status == DisputeStatus.REFUNDED) Color(0xFF34D399) else Color(0xFFCBD5E1)
                )
            } else if (showForm) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Delay in Delivery", "Parcel Damaged", "Wrong Charges / COD", "Rider Behaviour").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Describe issue for Admin Refund Desk", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("e.g., Delayed by 45 mins or box corner crushed") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_dispute_note_input")
                )
                Button(
                    onClick = {
                        val reason = if (noteInput.isBlank()) selectedCategory else "$selectedCategory: ${noteInput.trim()}"
                        val created = QuickDeliveryOperationsManager.submitOrderDispute(
                            context = context,
                            orderId = order.orderId,
                            customerPhone = order.customerPhone,
                            customerName = order.customerName,
                            reason = reason,
                            photoProofSummary = photoProofSummary.orEmpty()
                        )
                        if (created != null) {
                            showForm = false
                            noteInput = ""
                            feedback = "✅ Dispute #${created.disputeId} sent to Admin Refund Desk!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("customer_submit_dispute_btn")
                ) {
                    Text("Submit Dispute / Request Wallet Refund", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            feedback?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399)
                )
            }
        }
    }
}

@Composable
fun CustomerProximityOtpAlertBanner(
    order: CustomerOrder,
    otpEnabled: Boolean = false
) {
    val riderCoordsMap by QuickDeliveryOperationsManager.riderLiveCoordsByOrder.collectAsState()
    val unreachableMap by QuickDeliveryOperationsManager.unreachableByOrder.collectAsState()
    val unreachableRec = unreachableMap[order.orderId]

    val etaInfo = remember(order, riderCoordsMap) {
        QuickDeliveryOperationsManager.computeLiveRiderEtaKmAndMins(order)
    }
    val distMeters = etaInfo?.first?.let { (it * 1000.0).toInt().coerceAtLeast(10) }
    val isNearby = (etaInfo != null && etaInfo.first <= 0.25) ||
        order.orderStatus == com.example.data.model.OrderStatus.ARRIVED_AT_PICKUP

    val isDropStage = order.orderStatus == com.example.data.model.OrderStatus.PICKED_UP ||
        order.orderStatus == com.example.data.model.OrderStatus.IN_TRANSIT
    val activeOtp = if (isDropStage) order.deliveryOtp else order.pickupOtp
    val otpLabel = if (isDropStage) "Drop OTP" else "Pickup OTP"

    if (isNearby && order.orderStatus != com.example.data.model.OrderStatus.DELIVERED && order.orderStatus != com.example.data.model.OrderStatus.CANCELLED) {
        Surface(
            color = Color(0xFFECFDF5),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, EnhGreen),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_proximity_otp_alert_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = when {
                            otpEnabled && distMeters != null -> "🏍️ Rider only ${distMeters}m away — Keep $otpLabel Ready!"
                            otpEnabled -> "🏍️ Rider Reached Gate — Keep $otpLabel Ready!"
                            distMeters != null -> "🏍️ Rider only ${distMeters}m away — Please Be Ready!"
                            else -> "🏍️ Rider Reached Gate — Ready for Direct Handover!"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF065F46)
                    )
                    Text(
                        text = "${order.assignedRiderName ?: "Rider Partner"} is arriving now",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF047857)
                    )
                }
                if (otpEnabled) {
                    Surface(
                        color = EnhGreen,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = activeOtp,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (unreachableRec != null &&
        order.orderStatus != com.example.data.model.OrderStatus.DELIVERED &&
        order.orderStatus != com.example.data.model.OrderStatus.CANCELLED
    ) {
        Surface(
            color = Color(0xFFFEF2F2),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_unreachable_alert_banner")
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                Text(
                    text = "📵 Rider tried reaching you (${unreachableRec.reportedAtLabel}): Please answer call or share gate access.",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
            }
        }
    }
}

@Composable
fun CustomerAddressAutocompleteRow(
    query: String,
    savedAddresses: List<com.example.data.cloud.SavedCustomerAddress>,
    pastOrders: List<CustomerOrder>,
    isPickup: Boolean,
    accentColor: Color,
    onSelectSaved: (com.example.data.cloud.SavedCustomerAddress) -> Unit,
    onSelectRecentText: (String) -> Unit
) {
    val cleanQuery = query.trim()
    if (cleanQuery.length < 2) return

    val matchingSaved = remember(cleanQuery, savedAddresses) {
        savedAddresses.filter { addr ->
            (addr.areaStreet.contains(cleanQuery, ignoreCase = true) ||
                addr.label.contains(cleanQuery, ignoreCase = true) ||
                addr.landmark.contains(cleanQuery, ignoreCase = true)) &&
                !addr.areaStreet.equals(cleanQuery, ignoreCase = true)
        }.take(2)
    }

    val matchingRecent = remember(cleanQuery, pastOrders, isPickup, matchingSaved) {
        pastOrders.mapNotNull { ord ->
            val raw = if (isPickup) ord.pickupAddress.fullAddress else ord.dropAddress.fullAddress
            raw.trim().takeIf { text ->
                text.contains(cleanQuery, ignoreCase = true) &&
                    !text.equals(cleanQuery, ignoreCase = true) &&
                    matchingSaved.none { s -> text.contains(s.areaStreet, ignoreCase = true) }
            }
        }.distinct().take(2)
    }

    if (matchingSaved.isEmpty() && matchingRecent.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isPickup) "pickup_address_autocomplete_list" else "drop_address_autocomplete_list"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        matchingSaved.forEach { addr ->
            Surface(
                color = accentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                onClick = { onSelectSaved(addr) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${addr.label}: ${addr.areaStreet} (${addr.pinCode})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EnhNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        matchingRecent.forEach { text ->
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                onClick = { onSelectRecentText(text) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = EnhSlate, modifier = Modifier.size(14.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = EnhNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


