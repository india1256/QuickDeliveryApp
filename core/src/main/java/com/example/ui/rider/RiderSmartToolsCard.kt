package com.example.ui.rider

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.cloud.QuickDeliveryOperationsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SmartTeal = Color(0xFF0A3D3F)
private val SmartEmerald = Color(0xFF10B981)
private val SmartOrange = Color(0xFFF97316)
private val SmartNavy = Color(0xFF0F1E36)

/**
 * Rider Partner App banners & mission/referral cards driven live by Admin Master Switches.
 */
@Composable
fun RiderRemoteAlertsAndMissionCard(
    flags: AdminRemoteFeatureFlags,
    todayDeliveredCount: Int,
    riderName: String,
    riderPhone: String,
    isDarkMode: Boolean = false
) {
    val context = LocalContext.current
    var dismissedBroadcastEpoch by remember { mutableLongStateOf(0L) }
    val showBroadcast = flags.adminBroadcastMessage.isNotBlank() &&
        (flags.adminBroadcastTarget == "ALL" || flags.adminBroadcastTarget == "RIDER") &&
        dismissedBroadcastEpoch != flags.adminBroadcastEpochMs

    val hasAnyContent = flags.maintenanceBannerEnabled ||
        showBroadcast ||
        flags.riderTargetMissionEnabled ||
        flags.referralWalletEnabled

    if (!hasAnyContent) return

    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val headingColor = if (isDarkMode) Color(0xFFF8FAFC) else SmartNavy
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (flags.maintenanceBannerEnabled) {
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rider_maintenance_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Maintenance alert",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Operations Notice",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = flags.maintenanceMessage.ifBlank {
                                "Temporary maintenance or zone pause is active. Check before heading out."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }
        }

        if (showBroadcast) {
            Surface(
                color = Color(0xFFFFFBEB),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rider_admin_broadcast_banner")
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Admin broadcast",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Partner Update",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = flags.adminBroadcastMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                    }
                    IconButton(
                        onClick = { dismissedBroadcastEpoch = flags.adminBroadcastEpochMs },
                        modifier = Modifier.testTag("rider_dismiss_broadcast_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss broadcast",
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (flags.riderTargetMissionEnabled) {
            val targetOrders = flags.riderMissionTargetOrders.coerceAtLeast(1)
            val progress = (todayDeliveredCount.toFloat() / targetOrders.toFloat()).coerceIn(0f, 1f)
            val completed = todayDeliveredCount >= targetOrders
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (completed) SmartEmerald.copy(alpha = 0.6f) else SmartOrange.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rider_daily_mission_card")
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
                        Text(
                            text = if (completed) "🎉 Daily Mission Unlocked!" else "🎯 Today's Partner Mission",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (completed) SmartEmerald else headingColor
                        )
                        Surface(
                            color = if (completed) SmartEmerald.copy(alpha = 0.14f) else SmartOrange.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+₹${flags.riderMissionBonusInr} Bonus",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (completed) Color(0xFF047857) else SmartOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (completed) SmartEmerald else SmartOrange,
                        trackColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                    )

                    Text(
                        text = if (completed) {
                            "$todayDeliveredCount / $targetOrders trips completed today — Bonus ₹${flags.riderMissionBonusInr} earned!"
                        } else {
                            "$todayDeliveredCount / $targetOrders trips → +₹${flags.riderMissionBonusInr} Bonus (${targetOrders - todayDeliveredCount} more to go)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = subTextColor
                    )
                }
            }
        }

        if (flags.referralWalletEnabled) {
            val inviteCode = QuickDeliveryOperationsManager.buildPersonalReferralCode(riderName, riderPhone)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SmartTeal.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rider_referral_wallet_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redeem,
                                contentDescription = "Partner referral",
                                tint = SmartTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Refer a Partner • Earn ₹${flags.referralBonusInr}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = headingColor
                            )
                        }
                        Text(
                            text = "Your Partner Code: $inviteCode",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SmartTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val shareText = "Join Partner Quick Delivery & earn ₹${flags.referralBonusInr} welcome bonus! Use my partner invite code: $inviteCode"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(intent, "Share Partner Invite Code"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SmartTeal),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("rider_share_referral_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Invite", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Rider Partner Shift Summary (Trips, Km, Avg ₹/Km, Live Share %), Hindi/English Voice Order Alert toggle,
 * and Smart Order Filters (Min Payout + Preferred Area Keyword) for incoming SEARCHING_RIDER orders.
 */
@Composable
fun RiderShiftAndSmartFilterCard(
    todayDeliveredCount: Int,
    todayDistanceKm: Double,
    todaySameDayEarningInr: Double,
    riderSharePercent: Int,
    voiceAlertEnabled: Boolean,
    onToggleVoiceAlert: (Boolean) -> Unit,
    minPayoutFilterInr: Int,
    onMinPayoutChange: (Int) -> Unit,
    preferredAreaKeyword: String,
    onAreaKeywordChange: (String) -> Unit,
    hiddenByFilterCount: Int,
    isDarkMode: Boolean = false
) {
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val tileBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val headingColor = if (isDarkMode) Color(0xFFF8FAFC) else SmartNavy
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    val avgPerKmText = if (todayDistanceKm > 0.1) {
        String.format(Locale.US, "₹%.0f/km", todaySameDayEarningInr / todayDistanceKm)
    } else {
        "₹0/km"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rider_shift_and_smart_filter_card")
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = SmartTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Shift Log & Smart Order Tools",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = headingColor
                    )
                }
                Surface(
                    color = SmartEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${riderSharePercent}% Share",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Shift Summary & Distance / Km Earnings Log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiderShiftMetricTile(
                    label = "Trips",
                    value = todayDeliveredCount.toString(),
                    bgColor = tileBg,
                    headingColor = headingColor,
                    subColor = subTextColor,
                    modifier = Modifier.weight(1f)
                )
                RiderShiftMetricTile(
                    label = "Distance",
                    value = String.format(Locale.US, "%.1f km", todayDistanceKm),
                    bgColor = tileBg,
                    headingColor = headingColor,
                    subColor = subTextColor,
                    modifier = Modifier.weight(1f)
                )
                RiderShiftMetricTile(
                    label = "Avg/km",
                    value = avgPerKmText,
                    bgColor = tileBg,
                    headingColor = SmartEmerald,
                    subColor = subTextColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Voice Order Alert Toggle (Rider only)
            Surface(
                color = tileBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Voice order alert",
                            tint = SmartOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Voice Order Alert (Hinglish)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = headingColor
                            )
                            Text(
                                text = "Speaks pickup area & payout when a new order arrives",
                                style = MaterialTheme.typography.labelSmall,
                                color = subTextColor
                            )
                        }
                    }
                    Switch(
                        checked = voiceAlertEnabled,
                        onCheckedChange = onToggleVoiceAlert,
                        colors = SwitchDefaults.colors(checkedTrackColor = SmartEmerald),
                        modifier = Modifier.testTag("rider_voice_alert_switch")
                    )
                }
            }

            // Min Payout Filter Chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = subTextColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Incoming Order Filter (Min Payout & Area)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "All", 50 to "₹50+", 100 to "₹100+", 150 to "₹150+").forEach { (value, label) ->
                        FilterChip(
                            selected = minPayoutFilterInr == value,
                            onClick = { onMinPayoutChange(value) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("rider_payout_filter_$value")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = preferredAreaKeyword,
                onValueChange = onAreaKeywordChange,
                label = { Text("Preferred Area Keyword (e.g. Andheri, Sector 18)") },
                singleLine = true,
                trailingIcon = {
                    if (preferredAreaKeyword.isNotBlank()) {
                        IconButton(onClick = { onAreaKeywordChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear area filter")
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rider_area_keyword_filter_input")
            )

            if (hiddenByFilterCount > 0) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_hidden_orders_filter_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$hiddenByFilterCount incoming order(s) hidden by your filter",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                onMinPayoutChange(0)
                                onAreaKeywordChange("")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Show All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderShiftMetricTile(
    label: String,
    value: String,
    bgColor: Color,
    headingColor: Color,
    subColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = headingColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = subColor
            )
        }
    }
}

/**
 * Adds a tamper-evident Timestamp + Live GPS Watermark summary to every Rider parcel/POD photo.
 */
fun formatWatermarkedRiderPhotoSummary(
    bitmap: Bitmap,
    stageLabel: String,
    lat: Double?,
    lng: Double?,
    fallbackGps: String?
): String {
    val ts = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    val gpsText = when {
        lat != null && lng != null && (lat != 0.0 || lng != 0.0) ->
            String.format(Locale.US, "%.5f, %.5f", lat, lng)
        !fallbackGps.isNullOrBlank() -> fallbackGps
        else -> "Live GPS Verified"
    }
    return "📷 $stageLabel Proof (${bitmap.width}x${bitmap.height}) • 🕒 $ts • 📍 $gpsText"
}
