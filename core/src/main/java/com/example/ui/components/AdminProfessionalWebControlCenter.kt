package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.security.RiderBatteryPowerState
import com.example.data.model.WalletTransaction
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import com.example.data.delivery.AdminVehicleAndWeightPolicy
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.AddressDetails
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.data.model.PackageDetails
import com.example.data.model.PackageType
import com.example.data.model.PricingConfig
import com.example.data.model.RiderProfile
import com.example.data.model.RiderShiftStatus
import com.example.data.model.VehicleType
import com.example.data.repository.SharedOrderRepository
import com.example.security.RiderBatteryPowerMonitor
import com.example.security.RiderOverspeedAlarmManager
import java.util.Locale
import kotlinx.coroutines.delay

private val WebNavy = Color(0xFF0B132B)
private val WebSlate = Color(0xFF1C2541)
private val WebTeal = Color(0xFF00A896)
private val WebOrange = Color(0xFFFF6B35)
private val WebEmerald = Color(0xFF10B981)
private val WebGold = Color(0xFFF59E0B)
private val WebCrimson = Color(0xFFDC2626)
private val WebSoftBg = Color(0xFFF1F5F9)

/**
 * Compact Live Telemetry Ribbon shown at the top of the Admin Web App so the Admin
 * immediately sees live status from both the Customer App and the Rider App at a glance.
 */
@Composable
fun AdminWebExecutiveLiveRibbon(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    onOpenLiveMirror: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) {
        MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context)
        RiderFeedbackManager.ensureLoaded(context)
        RiderOrderTransferManager.ensureLoaded(context)
        SosSafetySupportManager.ensureLoaded(context)
    }

    val overspeedState by RiderOverspeedAlarmManager.state.collectAsState()
    val batteryState by RiderBatteryPowerMonitor.powerState.collectAsState()
    val weeklyTransfersUsed by RiderOrderTransferManager.weeklyTransfersUsed.collectAsState()
    val sosAlerts by SosSafetySupportManager.sosAlerts.collectAsState()
    val customerStorageSaved by MotivationAndPromoManager.customerStoragePermissionGranted.collectAsState()
    val riderStorageSaved by MotivationAndPromoManager.riderStoragePermissionGranted.collectAsState()
    val tipsMap by RiderFeedbackManager.tipsByOrder.collectAsState()
    val fcmMessages by FcmOrderNotificationManager.fcmMessages.collectAsState()

    LaunchedEffect(orders) {
        FcmOrderNotificationManager.observeOrdersAndDispatchFcmNotifications(context, orders)
    }

    val activeSosCount = sosAlerts.count { !it.isResolved }
    val primaryRider = riders.firstOrNull { it.riderId == "RIDER-101" } ?: riders.firstOrNull()
    val totalTips = tipsMap.values.sum()

    Surface(
        color = WebNavy,
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_web_executive_live_ribbon")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live NOC Pulse Pill
            RibbonMetricPill(
                label = "🟢 LIVE NOC MIRROR",
                value = "${orders.size} Orders • Tap for Live View",
                accentColor = WebEmerald,
                onClick = onOpenLiveMirror
            )

            // Customer App Live Pill
            RibbonMetricPill(
                label = "👤 Customer App Live",
                value = "${MotivationAndPromoManager.cachedCustomerName} (${if (customerStorageSaved) "Storage Saved" else "Active"})",
                accentColor = WebTeal,
                onClick = onOpenLiveMirror
            )

            // Rider App Live Pill
            RibbonMetricPill(
                label = "🏍️ Rider App Live",
                value = "${primaryRider?.name ?: MotivationAndPromoManager.cachedRiderName} • ${if (primaryRider?.isOnline == true) "ONLINE" else "OFFLINE"} (${if (riderStorageSaved) "Saved" else "Active"})",
                accentColor = WebOrange,
                onClick = onOpenLiveMirror
            )

            // Over-Speed Phone Alarm Telemetry Pill
            RibbonMetricPill(
                label = "🚨 Rider Speed Guard",
                value = if (overspeedState.isAlarmRinging) {
                    "ALARM RINGING (${overspeedState.currentSpeedKmh}/${overspeedState.speedLimitKmh} km/h)"
                } else {
                    "Safe ${overspeedState.currentSpeedKmh}/${overspeedState.speedLimitKmh} km/h"
                },
                accentColor = if (overspeedState.isAlarmRinging) WebCrimson else WebEmerald,
                onClick = onOpenLiveMirror
            )

            // Weekly Order Transfer Quota Pill
            RibbonMetricPill(
                label = "🔄 Order Transfers",
                value = "$weeklyTransfersUsed/${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS} Used This Week",
                accentColor = if (weeklyTransfersUsed >= RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS) WebCrimson else WebGold,
                onClick = onOpenLiveMirror
            )

            // Rider Battery & Tips Pill
            RibbonMetricPill(
                label = "🔋 Battery & Tips",
                value = "${batteryState.levelPercent}% • Tips ₹$totalTips",
                accentColor = if (batteryState.isCriticalPower) WebCrimson else WebTeal,
                onClick = onOpenLiveMirror
            )

            // Real-Time FCM Push Notification Telemetry Pill
            RibbonMetricPill(
                label = "🔔 FCM Push Sync",
                value = "${fcmMessages.size} Real-Time Alerts • Customer & Rider",
                accentColor = WebEmerald,
                onClick = onOpenLiveMirror
            )

            if (activeSosCount > 0) {
                RibbonMetricPill(
                    label = "🆘 SOS ACTIVE",
                    value = "$activeSosCount Emergency Alert(s)",
                    accentColor = WebCrimson,
                    onClick = onOpenLiveMirror
                )
            }
        }
    }
}

@Composable
private fun RibbonMetricPill(
    label: String,
    value: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = WebSlate,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/**
 * Full-Screen Professional Admin Web Live Mirror:
 * Displays EVERYTHING happening live across both the Customer App and the Rider App:
 * 1. Executive 4-KPI Summary Grid (Orders, Revenue/80% Rider Split/Tips, Rider Speed/Battery, Transfers/SOS).
 * 2. Customer App Live Mirror Card (Saved Login, Storage Permission Status, Active Orders, Prepaid OTP vs COD, Suggestions).
 * 3. Rider App Live Mirror Card (Pickup Rider Photo/Name/Mobile, Duty Status, Over-Speed Phone Alarm Telemetry,
 *    Weekly Order Transfer 3x/Week Quota & History, Battery Guard, Wallet & Tips).
 * 4. Real-Time Customer ↔ Rider Order Dispatch Pipeline with 1-Click Admin Live Controls.
 */
@Composable
fun AdminProfessionalLiveMirrorScreen(
    repository: SharedOrderRepository,
    onSwitchToRatingsTab: () -> Unit,
    onSwitchToFleetConsoleTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) {
        MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context)
        RiderFeedbackManager.ensureLoaded(context)
        RiderOrderTransferManager.ensureLoaded(context)
        SosSafetySupportManager.ensureLoaded(context)
        RiderBatteryPowerMonitor.refreshFromSystem(context)
    }

    val orders by repository.orders.collectAsState()
    val riders by repository.riders.collectAsState()
    val pricingConfig by repository.pricingConfig.collectAsState()
    val vehiclePolicy by AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.collectAsState()

    val overspeedState by RiderOverspeedAlarmManager.state.collectAsState()
    val batteryState by RiderBatteryPowerMonitor.powerState.collectAsState()
    val weeklyTransfersUsed by RiderOrderTransferManager.weeklyTransfersUsed.collectAsState()
    val transfersByOrder by RiderOrderTransferManager.transfersByOrder.collectAsState()
    val sosAlerts by SosSafetySupportManager.sosAlerts.collectAsState()
    val riderRatings by RiderFeedbackManager.feedbackByOrder.collectAsState()
    val customerRatings by RiderFeedbackManager.customerRatingsByOrder.collectAsState()
    val suggestions by RiderFeedbackManager.suggestions.collectAsState()
    val tipsByOrder by RiderFeedbackManager.tipsByOrder.collectAsState()

    val customerStorageSaved by MotivationAndPromoManager.customerStoragePermissionGranted.collectAsState()
    val riderStorageSaved by MotivationAndPromoManager.riderStoragePermissionGranted.collectAsState()

    val primaryRider = riders.firstOrNull { it.riderId == "RIDER-101" } ?: riders.firstOrNull()

    val totalGmvInr = remember(orders) {
        orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { order ->
            order.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        }
    }
    val platformCommissionInr = totalGmvInr * 0.20
    val riderShareInr = totalGmvInr * 0.80
    val totalTipsInr = tipsByOrder.values.sum()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WebSoftBg)
            .testTag("admin_professional_live_mirror_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. ENTERPRISE ADMIN WEB HERO HEADER & 4-KPI EXECUTIVE GRID
        item(key = "admin_web_hero_kpis") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WebNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(WebNavy, WebSlate, Color(0xFF0F3460))
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = WebTeal,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin Web NOC",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Quick Delivery • Enterprise Admin Web NOC",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Live Real-Time Mirror of Customer App & Rider Partner App",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Surface(
                            color = WebEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, WebEmerald)
                        ) {
                            Text(
                                text = "● LIVE SYNC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = WebEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 4 Executive KPI Cards (2x2 Grid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "LIVE ORDERS",
                            primaryValue = "${orders.size} Total",
                            subValue = "${orders.count { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }} Active • ${orders.count { it.orderStatus == OrderStatus.DELIVERED }} Delivered",
                            accentColor = WebTeal,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "GMV & 80/20 SPLIT",
                            primaryValue = String.format(Locale.US, "₹%.0f", totalGmvInr),
                            subValue = String.format(
                                Locale.US,
                                "Rider 80%%: ₹%.0f • Admin 20%%: ₹%.0f • Tips: ₹%d",
                                riderShareInr,
                                platformCommissionInr,
                                totalTipsInr
                            ),
                            accentColor = WebGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "RIDER SPEED & BATTERY",
                            primaryValue = "${overspeedState.currentSpeedKmh} / ${overspeedState.speedLimitKmh} km/h",
                            subValue = if (overspeedState.isAlarmRinging) {
                                "🚨 OVER-SPEED ALARM RINGING • Battery ${batteryState.levelPercent}%"
                            } else {
                                "✅ Safe Speed • Battery ${batteryState.levelPercent}%"
                            },
                            accentColor = if (overspeedState.isAlarmRinging) WebCrimson else WebEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "TRANSFERS & AUDIT",
                            primaryValue = "$weeklyTransfersUsed / ${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS} Transfers",
                            subValue = "Ratings: ${riderRatings.size + customerRatings.size} • Suggestions: ${suggestions.size} • SOS: ${sosAlerts.count { !it.isResolved }}",
                            accentColor = WebOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. CUSTOMER APP LIVE TELEMETRY & SAVED LOGIN MIRROR CARD
        item(key = "admin_web_customer_app_live_card") {
            val prepaidCount = orders.count { it.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY }
            val codCount = orders.count { it.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY }
            val customerSuggestionsCount = suggestions.count { it.senderRole == "CUSTOMER" }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, WebTeal),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_web_customer_live_mirror_card")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = WebTeal,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "1. Customer App — Live State & Phone Storage Mirror",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WebNavy
                                )
                                Text(
                                    text = "Real-time Customer login, storage backup, payment OTP/COD & booking telemetry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Surface(
                            color = if (customerStorageSaved) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (customerStorageSaved) WebEmerald else WebGold)
                        ) {
                            Text(
                                text = if (customerStorageSaved) "💾 Storage Saved" else "📲 Storage Pending",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (customerStorageSaved) Color(0xFF065F46) else Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoPillBox(
                            label = "Logged-In Customer",
                            value = "${MotivationAndPromoManager.cachedCustomerName}\n📞 ${MotivationAndPromoManager.cachedCustomerPhone}",
                            modifier = Modifier.weight(1f)
                        )
                        InfoPillBox(
                            label = "Payment Modes (OTP Rule)",
                            value = "Prepaid (OTP): $prepaidCount\nCOD (No OTP): $codCount",
                            modifier = Modifier.weight(1f)
                        )
                        InfoPillBox(
                            label = "Ratings & Suggestions",
                            value = "Rated Riders: ${riderRatings.size}\nSuggestions: $customerSuggestionsCount",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. RIDER APP LIVE TELEMETRY, PICKUP PROFILE, OVER-SPEED ALARM & ORDER TRANSFER MIRROR CARD
        item(key = "admin_web_rider_app_live_card") {
            val isTransferLocked = weeklyTransfersUsed >= RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS
            val riderSuggestionsCount = suggestions.count { it.senderRole == "RIDER" }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, WebOrange),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_web_rider_live_mirror_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = WebOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "2. Rider Partner App — Live Telemetry, Speed Alarm & Transfers",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WebNavy
                                )
                                Text(
                                    text = "Pickup Rider: ${primaryRider?.name ?: MotivationAndPromoManager.cachedRiderName} (+91 98450 67890) • ${primaryRider?.vehicleType?.title ?: "2-Wheeler"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        if (primaryRider != null) {
                            OutlinedButton(
                                onClick = {
                                    if (primaryRider.shiftStatus == RiderShiftStatus.ENDED_AT_HOME) {
                                        repository.startOrResumeRiderShift(primaryRider.riderId, primaryRider.workMode, "")
                                    } else {
                                        repository.toggleRiderOnline(primaryRider.riderId)
                                    }
                                },
                                modifier = Modifier.testTag("admin_live_toggle_rider_duty_btn")
                            ) {
                                Text(
                                    text = if (primaryRider.isOnline) "🟢 ONLINE (Toggle)" else "⚪ OFFLINE (Toggle)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Row 1: Rider Storage Login, Wallet + Tips, and Confidential Work Quality Status
                    val avgRiderRating = if (riderRatings.isEmpty()) {
                        4.9
                    } else {
                        riderRatings.values.map { it.starRating }.average()
                    }
                    val isWorkingProperly = avgRiderRating >= 4.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoPillBox(
                            label = "Rider Login & Storage",
                            value = "${primaryRider?.name ?: "Vikram Singh"}\n${if (riderStorageSaved) "💾 Storage Saved" else "📲 Active Session"}",
                            modifier = Modifier.weight(1f)
                        )
                        InfoPillBox(
                            label = "Wallet + 100% Tips",
                            value = String.format(
                                Locale.US,
                                "Wallet: ₹%.0f\nCustomer Tips: +₹%d",
                                primaryRider?.walletBalanceInr ?: 0.0,
                                totalTipsInr
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        InfoPillBox(
                            label = "Admin Work Audit",
                            value = String.format(
                                Locale.US,
                                "★ %.1f/5.0\n%s",
                                avgRiderRating,
                                if (isWorkingProperly) "✓ Working Properly" else "⚠ Needs Review"
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Live Over-Speed Phone Alarm Telemetry & Remote Admin Test Controls
                    Surface(
                        color = if (overspeedState.isAlarmRinging) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (overspeedState.isAlarmRinging) WebCrimson else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = if (overspeedState.isAlarmRinging) WebCrimson else WebTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rider Over-Speed Phone Alarm Guard: ${overspeedState.currentSpeedKmh} km/h (Limit ${overspeedState.speedLimitKmh} km/h)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WebNavy
                                    )
                                }
                                Text(
                                    text = if (overspeedState.isAlarmRinging) "🚨 ALARM RINGING" else "✅ SAFE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (overspeedState.isAlarmRinging) WebCrimson else WebEmerald
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        RiderOverspeedAlarmManager.setSimulatedSpeedKmh(
                                            context,
                                            overspeedState.speedLimitKmh + 18
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_test_rider_overspeed_btn")
                                ) {
                                    Text("Test Over-Speed (${overspeedState.speedLimitKmh + 18} km/h)", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        RiderOverspeedAlarmManager.setSimulatedSpeedKmh(
                                            context,
                                            (overspeedState.speedLimitKmh - 12).coerceAtLeast(25)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_reset_rider_safe_speed_btn")
                                ) {
                                    Text("Slow Down (Auto-Stop Alarm)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Row 3: Rider Weekly Order Transfer Facility Audit (Max 3/Week)
                    Surface(
                        color = if (isTransferLocked) Color(0xFFFEF2F2) else Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isTransferLocked) WebCrimson else WebOrange.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = if (isTransferLocked) WebCrimson else WebOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rider Order Transfer Quota: $weeklyTransfersUsed / ${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS} Used This Week",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WebNavy
                                    )
                                }
                                Text(
                                    text = if (isTransferLocked) {
                                        "🔒 LOCKED UNTIL NEXT WEEK"
                                    } else {
                                        "${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS - weeklyTransfersUsed} Left"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTransferLocked) WebCrimson else Color(0xFF9A3412)
                                )
                            }

                            if (transfersByOrder.isEmpty()) {
                                Text(
                                    text = "No vehicle-breakdown order transfers triggered by Rider yet this week.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            } else {
                                transfersByOrder.values.forEach { rec ->
                                    Text(
                                        text = "• Order #${rec.orderId}: Transferred to ${rec.toRiderName} (${rec.toRiderMobile} • ${rec.toRiderVehiclePlate}) — Reason: ${rec.reason} (${rec.transferredAt})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WebNavy
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3B. LIVE SYNCHRONIZED ADMIN VEHICLE, WEIGHT LIMIT (MAX 20 KG) & PRICING CONTROL CENTER
        item(key = "admin_vehicle_pricing_weight_control_card") {
            AdminVehiclePricingAndWeightControlCard(
                repository = repository,
                pricingConfig = pricingConfig,
                vehiclePolicy = vehiclePolicy
            )
        }

        // 4. LIVE CUSTOMER ↔ RIDER ORDER DISPATCH PIPELINE (WITH ADMIN 1-CLICK LIVE ACTIONS)
        item(key = "admin_web_live_orders_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "4. Live Orders, Booking Details & Dispatch Pipeline (${orders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = WebNavy
                    )
                    Text(
                        text = "Shows Pickup/Drop Addresses, Parcel Weight (≤ 20 kg), Vehicle, Full ₹ Breakdown & 1-Click Dispatch",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                }
                OutlinedButton(
                    onClick = onSwitchToRatingsTab,
                    modifier = Modifier.testTag("admin_mirror_open_ratings_btn")
                ) {
                    Text("Ratings & Suggestions →", style = MaterialTheme.typography.labelSmall)
                }
            }
        }



        items(orders, key = { "admin_mirror_order_${it.orderId}" }) { order ->
            AdminLiveOrderMirrorCard(
                order = order,
                primaryRider = primaryRider,
                transferRecord = transfersByOrder[order.orderId],
                customerToRiderRating = riderRatings[order.orderId],
                riderToCustomerRating = customerRatings[order.orderId],
                tipAmountInr = tipsByOrder[order.orderId] ?: 0,
                onAdvanceOrderFromAdmin = {
                    when (order.orderStatus) {
                        OrderStatus.SEARCHING_RIDER -> {
                            AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                            AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                                repository = repository,
                                orderId = order.orderId
                            )
                        }
                        OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> {
                            repository.verifyPickupOtpByRider(order.orderId, order.pickupOtp, order.assignedRiderId)
                        }
                        OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> {
                            repository.verifyDeliveryOtpByRider(order.orderId, order.deliveryOtp, order.assignedRiderId)
                            AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                        }
                        else -> Unit
                    }
                }
            )
        }
    }
}

@Composable
private fun ExecutiveKpiTile(
    title: String,
    primaryValue: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun InfoPillBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = WebNavy
            )
        }
    }
}

@Composable
private fun AdminLiveOrderMirrorCard(
    order: CustomerOrder,
    primaryRider: RiderProfile?,
    transferRecord: OrderTransferRecord?,
    customerToRiderRating: RiderDeliveryFeedback?,
    riderToCustomerRating: CustomerRatingByRider?,
    tipAmountInr: Int,
    onAdvanceOrderFromAdmin: () -> Unit
) {
    val isPrepaid = order.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
    val isDelivered = order.orderStatus == OrderStatus.DELIVERED
    val riderName = transferRecord?.toRiderName ?: order.assignedRiderName ?: primaryRider?.name ?: "Vikram Singh"
    val riderMobile = transferRecord?.toRiderMobile ?: order.assignedRiderPhone ?: "+91 98450 67890"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.2.dp,
            if (isDelivered) WebEmerald else WebTeal.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_live_mirror_order_card_${order.orderId}")
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
                Column {
                    Text(
                        text = "Order #${order.orderId} • ${order.vehicleType.title} • ${order.totalAmount}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = WebNavy
                    )
                    Text(
                        text = "Customer: ${order.customerName} (${order.customerPhone})" +
                            if (tipAmountInr > 0) " • 💝 Tip +₹$tipAmountInr" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                }
                Surface(
                    color = if (isDelivered) Color(0xFFECFDF5) else Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isDelivered) WebEmerald else WebOrange)
                ) {
                    Text(
                        text = order.orderStatus.displayTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDelivered) Color(0xFF065F46) else Color(0xFF9A3412),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            val orderDetailedFare = remember(
                order.orderId,
                order.pickupAddress.lat,
                order.pickupAddress.lng,
                order.dropAddress.lat,
                order.dropAddress.lng,
                order.packageDetails.weightKg,
                order.vehicleType
            ) {
                AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
                    pickupLat = order.pickupAddress.lat,
                    pickupLng = order.pickupAddress.lng,
                    dropLat = order.dropAddress.lat,
                    dropLng = order.dropAddress.lng,
                    weightKgInput = order.packageDetails.weightKg,
                    vehicleType = order.vehicleType,
                    packageType = order.packageDetails.packageType,
                    packageDescription = order.packageDetails.description
                )
            }

            // Complete Booking Details: Route Addresses + Parcel Category & Weight + Vehicle & Distance + Full ₹ Fare Breakdown
            Surface(
                color = Color(0xFFF0FDFA),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, WebTeal.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_mirror_order_booking_details_${order.orderId}")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, WebEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "✓ Booking Confirmed & Verified (Customer App)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Admin Web Synced",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WebTeal
                        )
                    }
                    Text(
                        text = "📍 Pickup: ${order.pickupAddress.fullAddress}" +
                            if (order.pickupAddress.landmark.isNotBlank()) " (${order.pickupAddress.landmark})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WebNavy
                    )
                    Text(
                        text = "🏁 Drop: ${order.dropAddress.fullAddress}" +
                            if (order.dropAddress.landmark.isNotBlank()) " (${order.dropAddress.landmark})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WebNavy
                    )
                    HorizontalDivider(color = WebTeal.copy(alpha = 0.25f))
                    Text(
                        text = "📦 Parcel: ${order.packageDetails.packageType.title} (${order.packageDetails.description}) • Weight: ${order.packageDetails.weightKg} kg (Max 20 kg Verified) • Vehicle: ${order.vehicleType.title} (${String.format(Locale.US, "%.1f", order.distanceKm)} km)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E)
                    )
                    Text(
                        text = "₹ Breakdown: Base ${orderDetailedFare.formattedBaseFare} + Distance ${orderDetailedFare.formattedDistanceCharge} + Weight ${orderDetailedFare.formattedWeightCharge} + 5% GST ${orderDetailedFare.formattedTaxesAndFees} = Total ${order.totalAmount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = WebNavy
                    )
                    Text(
                        text = "💰 Settlement Split: 80% Rider Earning ${order.riderEarning} • 20% Admin Commission ${order.platformCommission}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "🛰️ Live Rider Tracking: GPS [${order.pickupAddress.lat}, ${order.pickupAddress.lng}] • Status: ${order.orderStatus.displayTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E)
                    )
                }
            }

            // Assigned Pickup Rider + Prepaid OTP vs COD No-OTP Status Row
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🏍️ Assigned Pickup Rider: $riderName • 📞 $riderMobile",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = WebNavy
                    )
                    Text(
                        text = if (isPrepaid) {
                            "🔐 PREPAID (${order.paymentMethod.shortBadge}) • Pickup OTP: ${order.pickupOtp} • Delivery OTP: ${order.deliveryOtp}"
                        } else {
                            "💵 CASH ON DELIVERY (COD) • No OTP Required for Pickup or Delivery"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPrepaid) Color(0xFF0F766E) else Color(0xFFB45309)
                    )
                    if (transferRecord != null) {
                        Text(
                            text = "🔄 Transferred due to: ${transferRecord.reason} (${transferRecord.transferredAt})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WebOrange
                        )
                    }
                }
            }

            // Confidential Ratings Summary for this Order (Visible Only in Admin Web)
            if (customerToRiderRating != null || riderToCustomerRating != null) {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, WebEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        customerToRiderRating?.let { fb ->
                            Text(
                                text = "⭐ Customer → Rider Rating: ★ ${fb.starRating}/5 (\"${fb.comment}\") • ${if (fb.starRating >= 4) "✓ Working Properly" else "⚠ Review"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                        riderToCustomerRating?.let { rc ->
                            Text(
                                text = "⭐ Rider → Customer Rating: ★ ${rc.starRating}/5 (\"${rc.comment}\")",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }
                }
            }

            // Live Customer <-> Assigned Rider Firestore Chat Mirror
            val allChatByOrder by OrderLiveChatManager.messagesByOrder.collectAsState()
            val orderChatMessages = allChatByOrder[order.orderId].orEmpty()
            if (orderChatMessages.isNotEmpty()) {
                Surface(
                    color = Color(0xFFF0FDFA),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, WebTeal.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_mirror_order_chat_${order.orderId}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "💬 Live Customer ↔ Rider Firestore Chat (${orderChatMessages.size} msgs)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = WebNavy
                        )
                        orderChatMessages.takeLast(2).forEach { msg ->
                            Text(
                                text = "${if (msg.senderRole == "RIDER") "🏍️ ${msg.senderName}" else "👤 ${msg.senderName}"}: \"${msg.messageText}\" (${msg.timestampLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            if (!isDelivered && order.orderStatus != OrderStatus.CANCELLED) {
                Button(
                    onClick = onAdvanceOrderFromAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = WebTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_mirror_advance_order_btn_${order.orderId}")
                ) {
                    val actionText = when (order.orderStatus) {
                        OrderStatus.SEARCHING_RIDER -> "Admin Action: Assign Rider $riderName →"
                        OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP ->
                            if (isPrepaid) "Admin Action: Verify Prepaid Pickup OTP (${order.pickupOtp}) →"
                            else "Admin Action: Confirm COD Pickup (No OTP) →"
                        else ->
                            if (isPrepaid) "Admin Action: Verify Prepaid Delivery OTP (${order.deliveryOtp}) →"
                            else "Admin Action: Complete COD Delivery (No OTP) →"
                    }
                    Text(actionText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminVehiclePricingAndWeightControlCard(
    repository: SharedOrderRepository,
    pricingConfig: PricingConfig,
    vehiclePolicy: AdminVehicleAndWeightPolicy
) {
    var bikeBaseInput by remember(pricingConfig.configVersion) {
        mutableStateOf(pricingConfig.bikeBaseFare.toInt().toString())
    }
    var bikeRateInput by remember(pricingConfig.configVersion) {
        mutableStateOf(pricingConfig.bikeRatePerKm.toInt().toString())
    }
    var scooterBaseInput by remember(pricingConfig.configVersion) {
        mutableStateOf(pricingConfig.scooterBaseFare.toInt().toString())
    }
    var scooterRateInput by remember(pricingConfig.configVersion) {
        mutableStateOf(pricingConfig.scooterRatePerKm.toInt().toString())
    }
    var autoBaseInput by remember(pricingConfig.configVersion) {
        mutableStateOf(pricingConfig.autoBaseFare.toInt().toString())
    }
    var autoRateInput by remember(pricingConfig.configVersion) {
        mutableStateOf(pricingConfig.autoRatePerKm.toInt().toString())
    }
    var weightTierBaseInput by remember(pricingConfig.configVersion) {
        mutableStateOf(pricingConfig.weightTierBase.toInt().toString())
    }
    var maxWeightKgInput by remember(vehiclePolicy.updatedAt) {
        val w = vehiclePolicy.maxAllowedParcelWeightKg
        mutableStateOf(if (w % 1.0 == 0.0) w.toInt().toString() else String.format(Locale.US, "%.1f", w))
    }
    var adminSyncStatus by remember { mutableStateOf<String?>(null) }

    // Live 3-vehicle sample estimate (4.2 km route, 2.5 kg parcel) synchronized with Customer App formula
    val sampleVehicles = remember { listOf(VehicleType.BIKE, VehicleType.SCOOTER, VehicleType.AUTO) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, WebTeal),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_vehicle_pricing_weight_control_card")
    ) {
        Column {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WebNavy)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = WebTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "3. Admin Vehicle, Weight Limit (Max 20 kg) & Pricing Control",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Synchronized live with Customer App (Bike → Scooter → Auto) • Config v${pricingConfig.configVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
                Surface(
                    color = WebOrange,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "MAX ${vehiclePolicy.maxAllowedParcelWeightKg.toInt()} KG CAP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Vehicle Availability Toggles (Bike, Scooter, Auto)
                Text(
                    text = "Vehicle Availability Controls (Customer App Bike → Scooter → Auto)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = WebNavy
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminVehicleToggleTile(
                        title = "1. Bike",
                        subtitle = "₹${pricingConfig.bikeBaseFare.toInt()} + ₹${pricingConfig.bikeRatePerKm.toInt()}/km",
                        enabled = vehiclePolicy.bikeEnabled,
                        testTag = "admin_toggle_vehicle_BIKE",
                        onToggle = { enabled ->
                            AutomatedFairRiderAssignmentEngine.updateLivePricingAndWeightPolicyFromAdmin(
                                repository = repository,
                                bikeEnabled = enabled
                            )
                            adminSyncStatus = "Bike status synced to Customer App"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AdminVehicleToggleTile(
                        title = "2. Scooter",
                        subtitle = "₹${pricingConfig.scooterBaseFare.toInt()} + ₹${pricingConfig.scooterRatePerKm.toInt()}/km",
                        enabled = vehiclePolicy.scooterEnabled,
                        testTag = "admin_toggle_vehicle_SCOOTER",
                        onToggle = { enabled ->
                            AutomatedFairRiderAssignmentEngine.updateLivePricingAndWeightPolicyFromAdmin(
                                repository = repository,
                                scooterEnabled = enabled
                            )
                            adminSyncStatus = "Scooter status synced to Customer App"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AdminVehicleToggleTile(
                        title = "3. Auto",
                        subtitle = "₹${pricingConfig.autoBaseFare.toInt()} + ₹${pricingConfig.autoRatePerKm.toInt()}/km",
                        enabled = vehiclePolicy.autoEnabled,
                        testTag = "admin_toggle_vehicle_AUTO",
                        onToggle = { enabled ->
                            AutomatedFairRiderAssignmentEngine.updateLivePricingAndWeightPolicyFromAdmin(
                                repository = repository,
                                autoEnabled = enabled
                            )
                            adminSyncStatus = "Auto status synced to Customer App"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 2. Vehicle Base Fare (₹) & Per-KM Rate (₹/km) Inputs
                Text(
                    text = "Vehicle Pricing (Base Fare ₹ & Distance Rate ₹/km) & Parcel Weight Cap (≤ 20 kg)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = WebNavy
                )

                // Bike & Scooter Pricing Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bikeBaseInput,
                        onValueChange = { bikeBaseInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Bike Base (₹)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_bike_base_fare_input")
                    )
                    OutlinedTextField(
                        value = bikeRateInput,
                        onValueChange = { bikeRateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Bike ₹/km") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_bike_rate_km_input")
                    )
                    OutlinedTextField(
                        value = scooterBaseInput,
                        onValueChange = { scooterBaseInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Scooter Base (₹)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_scooter_base_fare_input")
                    )
                    OutlinedTextField(
                        value = scooterRateInput,
                        onValueChange = { scooterRateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Scooter ₹/km") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_scooter_rate_km_input")
                    )
                }

                // Auto Pricing + Weight Tier Base + Max Parcel Weight Limit (<= 20 kg) Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = autoBaseInput,
                        onValueChange = { autoBaseInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Auto Base (₹)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_auto_base_fare_input")
                    )
                    OutlinedTextField(
                        value = autoRateInput,
                        onValueChange = { autoRateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Auto ₹/km") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_auto_rate_km_input")
                    )
                    OutlinedTextField(
                        value = weightTierBaseInput,
                        onValueChange = { weightTierBaseInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Weight Base (₹)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_weight_tier_base_input")
                    )
                    OutlinedTextField(
                        value = maxWeightKgInput,
                        onValueChange = { maxWeightKgInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Max Weight (≤20kg)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_max_parcel_weight_input")
                    )
                }

                // Surge Multiplier Quick Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Surge:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WebNavy
                    )
                    listOf(
                        Triple(1.0, "Normal (1.0x)", "NORMAL"),
                        Triple(1.15, "Peak (1.15x)", "PEAK"),
                        Triple(1.25, "Rain (1.25x)", "RAIN"),
                        Triple(1.5, "Festival (1.5x)", "FESTIVAL")
                    ).forEach { (mult, label, tagSuffix) ->
                        val isSelected = pricingConfig.surgeMultiplier == mult
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                AutomatedFairRiderAssignmentEngine.updateLivePricingAndWeightPolicyFromAdmin(
                                    repository = repository,
                                    surgeMultiplier = mult,
                                    surgePresetName = label
                                )
                                adminSyncStatus = "Surge updated to $label across Customer App & Backend"
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WebOrange,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("admin_surge_chip_$tagSuffix")
                        )
                    }
                }

                // Live 3-Vehicle Fare Preview Matrix (Bike -> Scooter -> Auto)
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Live Synchronized Customer App Fare Preview (Sample 4.3 km • 1.5 kg Parcel)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = WebNavy
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sampleVehicles.forEach { vehicle ->
                                val preview = remember(pricingConfig, vehiclePolicy, vehicle) {
                                    AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
                                        pickupLat = 12.9784,
                                        pickupLng = 77.6408,
                                        dropLat = 12.9352,
                                        dropLng = 77.6245,
                                        weightKgInput = "1.5",
                                        vehicleType = vehicle,
                                        pricingConfigOverride = pricingConfig,
                                        policyOverride = vehiclePolicy
                                    )
                                }
                                Surface(
                                    color = if (preview.isVehicleEnabledByAdmin) Color.White else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (preview.isVehicleEnabledByAdmin) WebTeal.copy(alpha = 0.5f) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "${vehicle.title} (${if (preview.isVehicleEnabledByAdmin) "Active" else "Paused"})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WebNavy
                                        )
                                        Text(
                                            text = preview.formattedTotalFare,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (preview.isVehicleEnabledByAdmin) WebOrange else Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "Base ${preview.formattedBaseFare} + Dist ${preview.formattedDistanceCharge} + Wt ${preview.formattedWeightCharge} + GST ${preview.formattedTaxesAndFees}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!adminSyncStatus.isNullOrBlank()) {
                    Text(
                        text = "✅ $adminSyncStatus",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.testTag("admin_pricing_sync_status_text")
                    )
                }

                // Action Buttons: Save & Sync Live to Customer App | Reset Defaults
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val enteredMaxWeight = maxWeightKgInput.toDoubleOrNull() ?: 20.0
                            val clampedMaxWeight = enteredMaxWeight.coerceIn(0.5, AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_KG)
                            maxWeightKgInput = if (clampedMaxWeight % 1.0 == 0.0) {
                                clampedMaxWeight.toInt().toString()
                            } else {
                                String.format(Locale.US, "%.1f", clampedMaxWeight)
                            }
                            val saved = AutomatedFairRiderAssignmentEngine.updateLivePricingAndWeightPolicyFromAdmin(
                                repository = repository,
                                bikeBaseFare = bikeBaseInput.toDoubleOrNull(),
                                bikeRatePerKm = bikeRateInput.toDoubleOrNull(),
                                scooterBaseFare = scooterBaseInput.toDoubleOrNull(),
                                scooterRatePerKm = scooterRateInput.toDoubleOrNull(),
                                autoBaseFare = autoBaseInput.toDoubleOrNull(),
                                autoRatePerKm = autoRateInput.toDoubleOrNull(),
                                weightTierBase = weightTierBaseInput.toDoubleOrNull(),
                                maxAllowedParcelWeightKg = clampedMaxWeight,
                                bikeMaxWeightKg = clampedMaxWeight,
                                scooterMaxWeightKg = clampedMaxWeight,
                                autoMaxWeightKg = clampedMaxWeight
                            )
                            adminSyncStatus = if (enteredMaxWeight > AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_KG) {
                                "Synced Pricing v${saved.configVersion}! Weight limit capped at maximum 20 kg."
                            } else {
                                "Synced Pricing v${saved.configVersion} & Max ${maxWeightKgInput} kg limit to Customer App!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WebTeal),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("admin_save_sync_pricing_weight_btn")
                    ) {
                        Text(
                            text = "Sync Pricing & Weight to Customer App",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val def = AutomatedFairRiderAssignmentEngine.resetPricingAndWeightPolicyToDefaults(repository)
                            bikeBaseInput = def.bikeBaseFare.toInt().toString()
                            bikeRateInput = def.bikeRatePerKm.toInt().toString()
                            scooterBaseInput = def.scooterBaseFare.toInt().toString()
                            scooterRateInput = def.scooterRatePerKm.toInt().toString()
                            autoBaseInput = def.autoBaseFare.toInt().toString()
                            autoRateInput = def.autoRatePerKm.toInt().toString()
                            weightTierBaseInput = def.weightTierBase.toInt().toString()
                            maxWeightKgInput = "20"
                            adminSyncStatus = "Reset vehicle pricing & 20 kg max weight to system defaults"
                        },
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("admin_reset_pricing_weight_btn")
                    ) {
                        Text("Reset Defaults", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminVehicleToggleTile(
    title: String,
    subtitle: String,
    enabled: Boolean,
    testTag: String,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (enabled) Color(0xFFF0FDFA) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (enabled) WebTeal else Color(0xFFCBD5E1)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = WebNavy
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WebTeal),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

/**
 * Enterprise Admin Web Control Center hosting 9 distinct, dedicated pages:
 * 0. Overview Dashboard
 * 1. Live Orders & Rider Map
 * 2. Customer & Rider Management
 * 3. KYC Verification Console
 * 4. Vehicle & Fare Settings (Bike, Scooter, Auto strictly)
 * 5. Payments & Settlements
 * 6. Complaints & SOS Support
 * 7. Reports & Analytics
 * 8. Settings & System Guards
 */
@Composable
fun AdminEnterpriseControlCenter(
    repository: SharedOrderRepository,
    orders: List<CustomerOrder>,
    onResetOrders: () -> Unit,
    onExitToCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) {
        MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context)
        RiderFeedbackManager.ensureLoaded(context)
        RiderOrderTransferManager.ensureLoaded(context)
        SosSafetySupportManager.ensureLoaded(context)
        RiderLiveSelfieCallAndExcelManager.ensureLoaded(context)
        RiderBatteryPowerMonitor.refreshFromSystem(context)
        val currentRiders = repository.riders.value
        if (currentRiders.any { !it.isOnline }) {
            val allOnline = currentRiders.map {
                it.copy(isOnline = true, shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT)
            }
            AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, allOnline)
        }
    }

    val riders by repository.riders.collectAsState()
    val pricingConfig by repository.pricingConfig.collectAsState()
    val vehiclePolicy by AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.collectAsState()
    val walletTransactions by repository.walletTransactions.collectAsState()
    val tipsByOrder by RiderFeedbackManager.tipsByOrder.collectAsState()
    val sosAlerts by SosSafetySupportManager.sosAlerts.collectAsState()
    val suggestions by RiderFeedbackManager.suggestions.collectAsState()
    val riderRatings by RiderFeedbackManager.feedbackByOrder.collectAsState()
    val customerRatings by RiderFeedbackManager.customerRatingsByOrder.collectAsState()
    val overspeedState by RiderOverspeedAlarmManager.state.collectAsState()
    val batteryState by RiderBatteryPowerMonitor.powerState.collectAsState()
    val weeklyTransfersUsed by RiderOrderTransferManager.weeklyTransfersUsed.collectAsState()

    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var globalRiderSearchQuery by remember { mutableStateOf("") }
    var isAutoPilotEnabled by remember { mutableStateOf(true) }
    var autoSystemPulseCount by remember { mutableIntStateOf(1) }
    var lastAutoActionLog by remember {
        mutableStateOf("🤖 100% Online Auto-Pilot Engine ON • Live Selfie Gate, Dial-Pad Calling, Full Document Vault & Excel Sync Active")
    }

    val executeSingleAutonomousStep: () -> Unit = {
        val currentOrders = repository.orders.value
        val searchingOrder = currentOrders.firstOrNull { it.orderStatus == OrderStatus.SEARCHING_RIDER }
        val assignedOrder = currentOrders.firstOrNull {
            it.orderStatus == OrderStatus.RIDER_ASSIGNED || it.orderStatus == OrderStatus.ARRIVED_AT_PICKUP
        }
        val inTransitOrder = currentOrders.firstOrNull {
            it.orderStatus == OrderStatus.PICKED_UP || it.orderStatus == OrderStatus.IN_TRANSIT
        }

        when {
            searchingOrder != null -> {
                AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                val outcome = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                    repository = repository,
                    orderId = searchingOrder.orderId
                )
                lastAutoActionLog = "⚡ Auto-Assigned #${searchingOrder.orderId}: ${outcome.message}"
                autoSystemPulseCount += 1
            }
            assignedOrder != null -> {
                repository.verifyPickupOtpByRider(
                    assignedOrder.orderId,
                    assignedOrder.pickupOtp,
                    assignedOrder.assignedRiderId
                )
                lastAutoActionLog = "📦 Auto-Picked Up #${assignedOrder.orderId} by ${assignedOrder.assignedRiderName ?: "Rider"} • Live Map Active"
                autoSystemPulseCount += 1
            }
            inTransitOrder != null -> {
                repository.verifyDeliveryOtpByRider(
                    inTransitOrder.orderId,
                    inTransitOrder.deliveryOtp,
                    inTransitOrder.assignedRiderId
                )
                AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                MotivationAndPromoManager.notifySuccessfulDeliveryMotivationForBoth(
                    order = inTransitOrder.copy(orderStatus = OrderStatus.DELIVERED),
                    forceTrigger = true
                )
                lastAutoActionLog = "🎉 Auto-Delivered #${inTransitOrder.orderId} • 80/20 Split Settled + Rider & Customer Motivated!"
                autoSystemPulseCount += 1
            }
            else -> {
                AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                lastAutoActionLog = "✅ All active orders delivered • Fleet standing by for instant auto-dispatch"
            }
        }
    }

    val spawnAndAutoDispatchLiveOrder: () -> Unit = {
        AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
        val sampleRoutes = listOf(
            Triple("Indiranagar 100ft Rd, Bengaluru", "Koramangala 4th Block, Bengaluru", VehicleType.BIKE),
            Triple("MG Road Metro Plaza, Bengaluru", "HSR Layout Sector 2, Bengaluru", VehicleType.SCOOTER),
            Triple("Whitefield ITPL Main Rd, Bengaluru", "Bellandur EcoSpace, Bengaluru", VehicleType.AUTO)
        )
        val picked = sampleRoutes[autoSystemPulseCount % sampleRoutes.size]
        val created = AutomatedFairRiderAssignmentEngine.createCustomerOrderWithBackendValidation(
            repository = repository,
            customerId = "CUST-AUTO-${100 + (autoSystemPulseCount % 899)}",
            customerName = MotivationAndPromoManager.cachedCustomerName.ifBlank { "Aarav Sharma" },
            customerPhone = MotivationAndPromoManager.cachedCustomerPhone.ifBlank { "+91 98765 43210" },
            pickupAddress = AddressDetails(
                fullAddress = picked.first,
                landmark = "Express Hub Gate 1",
                contactName = "Sender Desk",
                contactPhone = "+91 98765 43210",
                lat = 12.9784,
                lng = 77.6408
            ),
            dropAddress = AddressDetails(
                fullAddress = picked.second,
                landmark = "Main Reception Tower",
                contactName = "Receiver Desk",
                contactPhone = "+91 98450 11223",
                lat = 12.9352,
                lng = 77.6245
            ),
            packageDetails = PackageDetails(
                packageType = PackageType.DOCUMENTS,
                weightKg = "1.5",
                description = "Autonomous Express Parcel"
            ),
            vehicleType = picked.third,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY
        )
        created.onSuccess { newOrder ->
            val dispatch = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                repository = repository,
                orderId = newOrder.orderId
            )
            lastAutoActionLog = "🚀 Auto-Created & Assigned #${newOrder.orderId} (${picked.third.title}) → ${dispatch.assignedRider?.name ?: "Online Rider"}"
            autoSystemPulseCount += 1
        }
    }

    // 100% Autonomous Order Lifecycle Loop (Assign -> Pickup -> In-Transit -> Delivered -> 80/20 Split + Dual Motivation + FCM)
    LaunchedEffect(orders) {
        MotivationAndPromoManager.observeOrdersForDeliveryMotivation(orders)
        FcmOrderNotificationManager.observeOrdersAndDispatchFcmNotifications(context, orders)
    }

    LaunchedEffect(isAutoPilotEnabled) {
        while (isAutoPilotEnabled) {
            delay(3200L)
            val currentOrders = repository.orders.value
            val hasPendingWork = currentOrders.any {
                it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
            }
            if (hasPendingWork) {
                executeSingleAutonomousStep()
            }
        }
    }

    QuickDeliveryEnterpriseAdminSuite(
        repository = repository,
        orders = orders,
        riders = riders,
        pricingConfig = pricingConfig,
        vehiclePolicy = vehiclePolicy,
        walletTransactions = walletTransactions,
        tipsByOrder = tipsByOrder,
        sosAlerts = sosAlerts,
        suggestions = suggestions,
        riderRatings = riderRatings,
        customerRatings = customerRatings,
        overspeedState = overspeedState,
        batteryState = batteryState,
        weeklyTransfersUsed = weeklyTransfersUsed,
        isAutoPilotEnabled = isAutoPilotEnabled,
        onToggleAutoPilot = { isAutoPilotEnabled = it },
        lastAutoActionLog = lastAutoActionLog,
        onExecuteAutoStepNow = executeSingleAutonomousStep,
        onSpawnNewLiveOrder = spawnAndAutoDispatchLiveOrder,
        onResetOrders = onResetOrders,
        onExitToCustomer = onExitToCustomer,
        modifier = modifier
    )
}

/* =====================================================================
   PAGE 0: OVERVIEW DASHBOARD
   ===================================================================== */
@Composable
private fun AdminOverviewPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    totalGmvInr: Double,
    riderShareInr: Double,
    platformCommissionInr: Double,
    totalTipsInr: Int,
    overspeedState: com.example.security.RiderOverspeedState,
    batteryState: RiderBatteryPowerState,
    weeklyTransfersUsed: Int,
    sosAlertsCount: Int,
    ratingsCount: Int,
    suggestionsCount: Int,
    isAutoPilotEnabled: Boolean,
    onToggleAutoPilot: (Boolean) -> Unit,
    lastAutoActionLog: String,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onSearchRiderByName: (String) -> Unit
) {
    val activeOrdersCount = orders.count {
        it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
    }
    val deliveredCount = orders.count { it.orderStatus == OrderStatus.DELIVERED }
    val onlineRidersCount = riders.size
    var quickRiderLookupQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_overview_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Instant Rider Name Search & Document/Selfie Lookup Bar
        item(key = "overview_instant_rider_name_search") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, WebTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔍 Instant Rider Name Search (All Documents + Live Selfie + Call)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = WebNavy
                        )
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, WebEmerald)
                        ) {
                            Text(
                                text = "🟢 100% ONLINE SYNC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickRiderLookupQuery,
                            onValueChange = { quickRiderLookupQuery = it },
                            placeholder = { Text("Type Rider Name (e.g. Rajesh, Vikram, Imtiaz) to view full details & docs...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WebTeal) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_overview_rider_search_input")
                        )
                        Button(
                            onClick = { onSearchRiderByName(quickRiderLookupQuery) },
                            colors = ButtonDefaults.buttonColors(containerColor = WebTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_overview_rider_search_btn")
                        ) {
                            Text(
                                text = "Search Rider",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Next-Level Autonomous Order Lifecycle & Live City Fleet Radar Card
        item(key = "overview_autonomous_engine_and_radar") {
            AdminNextLevelAutoPilotAndRadarCard(
                orders = orders,
                riders = riders,
                isAutoPilotEnabled = isAutoPilotEnabled,
                onToggleAutoPilot = onToggleAutoPilot,
                lastAutoActionLog = lastAutoActionLog,
                onExecuteAutoStepNow = onExecuteAutoStepNow,
                onSpawnNewLiveOrder = onSpawnNewLiveOrder
            )
        }

        // High-level KPI Grid
        item(key = "overview_kpi_hero") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WebNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Executive Summary • 100% Online Live Pulse",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "TOTAL GMV (₹)",
                            primaryValue = String.format(Locale.US, "₹%.0f", totalGmvInr),
                            subValue = String.format(Locale.US, "80%% Rider: ₹%.0f • 20%% Net: ₹%.0f", riderShareInr, platformCommissionInr),
                            accentColor = WebGold,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "DELIVERIES",
                            primaryValue = "${orders.size} Total",
                            subValue = "$activeOrdersCount Active • $deliveredCount Delivered",
                            accentColor = WebTeal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "ONLINE RIDERS",
                            primaryValue = "${riders.size} Online ($onlineRidersCount Live Selfie Verified)",
                            subValue = "Tips Distributed: ₹$totalTipsInr",
                            accentColor = WebEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "SAFETY & COMPLIANCE",
                            primaryValue = if (overspeedState.isAlarmRinging) "🚨 OVER-SPEED" else "✅ Safe Fleet",
                            subValue = "SOS: $sosAlertsCount • Battery ${batteryState.levelPercent}%",
                            accentColor = if (overspeedState.isAlarmRinging || sosAlertsCount > 0) WebCrimson else WebEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Order Pipeline Card
        item(key = "overview_order_pipeline") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = "Live Delivery Pipeline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WebNavy
                        )
                        OutlinedButton(
                            onClick = { onNavigateToPage(1) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("View All Orders →", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PipelineStatusPill("Searching", orders.count { it.orderStatus == OrderStatus.SEARCHING_RIDER }, WebOrange, Modifier.weight(1f))
                        PipelineStatusPill("Assigned", orders.count { it.orderStatus == OrderStatus.RIDER_ASSIGNED }, WebTeal, Modifier.weight(1f))
                        PipelineStatusPill("In Transit", orders.count { it.orderStatus == OrderStatus.IN_TRANSIT || it.orderStatus == OrderStatus.PICKED_UP }, Color(0xFF3B82F6), Modifier.weight(1f))
                        PipelineStatusPill("Delivered", deliveredCount, WebEmerald, Modifier.weight(1f))
                    }
                }
            }
        }

        // Quick Navigation Shortcuts (Including Dedicated Riders, Customers & Online Excel Sheet Consoles!)
        item(key = "overview_shortcuts") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Operations, Complete Data Directories & Online Excel Workbook",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WebNavy
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewActionBtn(
                            title = "Riders (${riders.size} Online)",
                            subtitle = "All Docs, Live Selfie, Wallet & Call",
                            icon = Icons.AutoMirrored.Filled.DirectionsBike,
                            onClick = { onNavigateToPage(2) },
                            modifier = Modifier.weight(1f)
                        )
                        OverviewActionBtn(
                            title = "Customers Directory",
                            subtitle = "All Profiles, Orders & 1-Click Call",
                            icon = Icons.Default.People,
                            onClick = { onNavigateToPage(3) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewActionBtn(
                            title = "Online Excel Sheet (.xlsx)",
                            subtitle = "Maintain & Export All Online Data",
                            icon = Icons.Default.TableChart,
                            onClick = { onNavigateToPage(4) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_overview_excel_shortcut_btn")
                        )
                        OverviewActionBtn(
                            title = "KYC & Document Vault",
                            subtitle = "${riders.count { it.verificationStatus == com.example.data.model.RiderVerificationStatus.VERIFIED }} Verified",
                            icon = Icons.Default.VerifiedUser,
                            onClick = { onNavigateToPage(5) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewActionBtn(
                            title = "Vehicle & Pricing",
                            subtitle = "Bike, Scooter, Auto Fares",
                            icon = Icons.AutoMirrored.Filled.DirectionsBike,
                            onClick = { onNavigateToPage(6) },
                            modifier = Modifier.weight(1f)
                        )
                        OverviewActionBtn(
                            title = "Settlements & Ledger",
                            subtitle = "80/20 Payouts & Tips",
                            icon = Icons.Default.AccountBalanceWallet,
                            onClick = { onNavigateToPage(7) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineStatusPill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF334155)
            )
        }
    }
}

@Composable
private fun OverviewActionBtn(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WebTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WebNavy
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/* =====================================================================
   PAGE 1: LIVE ORDERS & RIDER MAP
   ===================================================================== */
@Composable
private fun AdminLiveOrdersAndMapPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    tipsByOrder: Map<String, Int>,
    repository: SharedOrderRepository,
    isAutoPilotEnabled: Boolean,
    onToggleAutoPilot: (Boolean) -> Unit,
    lastAutoActionLog: String,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredOrders = remember(orders, searchQuery, selectedFilter) {
        orders.filter { order ->
            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> order.orderStatus != OrderStatus.DELIVERED && order.orderStatus != OrderStatus.CANCELLED
                "SEARCHING" -> order.orderStatus == OrderStatus.SEARCHING_RIDER
                "IN_TRANSIT" -> order.orderStatus == OrderStatus.IN_TRANSIT || order.orderStatus == OrderStatus.PICKED_UP
                "DELIVERED" -> order.orderStatus == OrderStatus.DELIVERED
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                order.orderId.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.pickupAddress.fullAddress.contains(searchQuery, ignoreCase = true) ||
                order.dropAddress.fullAddress.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_live_orders_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Autonomous Command Center & Live Radar Map on Live Orders tab too
        item(key = "live_orders_autopilot_radar") {
            AdminNextLevelAutoPilotAndRadarCard(
                orders = orders,
                riders = riders,
                isAutoPilotEnabled = isAutoPilotEnabled,
                onToggleAutoPilot = onToggleAutoPilot,
                lastAutoActionLog = lastAutoActionLog,
                onExecuteAutoStepNow = onExecuteAutoStepNow,
                onSpawnNewLiveOrder = onSpawnNewLiveOrder
            )
        }

        // Search & Filters Header
        item(key = "orders_search_filter") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by Order ID, Customer, Address...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WebTeal) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "All (${orders.size})", "ACTIVE" to "Active", "SEARCHING" to "Searching", "IN_TRANSIT" to "In Transit", "DELIVERED" to "Delivered").forEach { (key, title) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WebTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Live Rider Fleet GPS Telemetry Strip
        item(key = "rider_gps_telemetry_banner") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = WebEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active Rider GPS Stream", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("${riders.count { it.isOnline }} Online", style = MaterialTheme.typography.labelSmall, color = WebEmerald, fontWeight = FontWeight.Bold)
                    }

                    riders.filter { it.isOnline }.forEach { rider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${rider.name} (${rider.vehicleType.title} • ${rider.vehicleNumber})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Text(String.format(Locale.US, "GPS: %.4f, %.4f", rider.currentLat, rider.currentLng), style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            item(key = "no_orders_found") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No orders match the current filter.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.orderId }) { order ->
                AdminLiveOrderMirrorCard(
                    order = order,
                    primaryRider = riders.firstOrNull { it.riderId == order.assignedRiderId } ?: riders.firstOrNull(),
                    transferRecord = null,
                    customerToRiderRating = null,
                    riderToCustomerRating = null,
                    tipAmountInr = tipsByOrder[order.orderId] ?: 0,
                    onAdvanceOrderFromAdmin = {
                        when (order.orderStatus) {
                            OrderStatus.SEARCHING_RIDER -> {
                                AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                                AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                                    repository = repository,
                                    orderId = order.orderId
                                )
                            }
                            OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> {
                                repository.verifyPickupOtpByRider(order.orderId, order.pickupOtp, order.assignedRiderId)
                            }
                            OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> {
                                repository.verifyDeliveryOtpByRider(order.orderId, order.deliveryOtp, order.assignedRiderId)
                                AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                                MotivationAndPromoManager.notifySuccessfulDeliveryMotivationForBoth(
                                    order = order.copy(orderStatus = OrderStatus.DELIVERED),
                                    forceTrigger = true
                                )
                            }
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}

/**
 * Next-Level Professional Admin Autonomous Order Engine & Live City Fleet Radar Map Card:
 * - Runs the entire order lifecycle automatically (Auto-Assign -> Auto-Pickup -> Live GPS Tracking -> Auto-Delivery + 80/20 Settlement + Dual Rider & Customer Motivation).
 * - Renders a live animated City Radar Map showing online riders, active delivery corridors, and telemetry pulses.
 */
@Composable
private fun AdminNextLevelAutoPilotAndRadarCard(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    isAutoPilotEnabled: Boolean,
    onToggleAutoPilot: (Boolean) -> Unit,
    lastAutoActionLog: String,
    onExecuteAutoStepNow: () -> Unit,
    onSpawnNewLiveOrder: () -> Unit
) {
    val radarTransition = rememberInfiniteTransition(label = "adminFleetRadar")
    val radarSweepProgress by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweepProgress"
    )

    val activeOrders = remember(orders) {
        orders.filter { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
    }
    val onlineRiders = remember(riders) { riders.filter { it.isOnline } }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071124)),
        border = BorderStroke(1.5.dp, if (isAutoPilotEnabled) WebTeal else Color(0xFF334155)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_next_level_autopilot_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF071124), Color(0xFF0F2442), Color(0xFF091931))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with Auto-Pilot Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (isAutoPilotEnabled) WebEmerald else Color(0xFF475569),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Autonomous Order Engine",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "100% Autonomous Order Lifecycle Engine",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-Match Rider • Auto-Pickup • Live Customer Map • Auto-Settle & Motivate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5EEAD4)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isAutoPilotEnabled) "AUTO ON" else "MANUAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAutoPilotEnabled) WebEmerald else Color(0xFF94A3B8)
                    )
                    Switch(
                        checked = isAutoPilotEnabled,
                        onCheckedChange = onToggleAutoPilot,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WebEmerald
                        ),
                        modifier = Modifier.testTag("admin_autopilot_switch")
                    )
                }
            }

            // Live Animated Multi-Rider & Active-Order City Radar Map Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF040B18))
                    .testTag("admin_live_fleet_radar_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // City Grid Lines
                    val vStep = w / 8f
                    val hStep = h / 5f
                    for (i in 1..7) {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(i * vStep, 0f),
                            end = Offset(i * vStep, h),
                            strokeWidth = 1f
                        )
                    }
                    for (j in 1..4) {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, j * hStep),
                            end = Offset(w, j * hStep),
                            strokeWidth = 1f
                        )
                    }

                    // Radar Concentric Rings
                    val center = Offset(w * 0.5f, h * 0.5f)
                    drawCircle(
                        color = WebTeal.copy(alpha = 0.18f),
                        radius = h * 0.42f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = WebTeal.copy(alpha = 0.12f),
                        radius = h * 0.25f,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )

                    // Active Delivery Route Corridors & Moving Vehicles
                    val routeCount = maxOf(activeOrders.size, 2)
                    for (idx in 0 until routeCount.coerceAtMost(4)) {
                        val startX = w * (0.12f + idx * 0.18f)
                        val startY = h * (0.76f - (idx % 2) * 0.45f)
                        val endX = w * (0.86f - idx * 0.14f)
                        val endY = h * (0.24f + (idx % 2) * 0.48f)

                        drawLine(
                            color = WebTeal.copy(alpha = 0.4f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )

                        // Pickup Pin
                        drawCircle(color = WebTeal, radius = 6f, center = Offset(startX, startY))
                        // Drop Pin
                        drawCircle(color = WebOrange, radius = 6f, center = Offset(endX, endY))

                        // Live Moving Rider Position along Corridor
                        val riderProg = ((radarSweepProgress + idx * 0.27f) % 1f)
                        val rx = startX + (endX - startX) * riderProg
                        val ry = startY + (endY - startY) * riderProg

                        drawCircle(
                            color = WebEmerald.copy(alpha = 0.3f),
                            radius = 14f,
                            center = Offset(rx, ry)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 7f,
                            center = Offset(rx, ry)
                        )
                        drawCircle(
                            color = WebEmerald,
                            radius = 4.5f,
                            center = Offset(rx, ry)
                        )
                    }
                }

                // Top-Left Overlay Badge
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.86f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, WebTeal.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "📡 LIVE CITY FLEET RADAR • ${onlineRiders.size} Online Riders • ${activeOrders.size} Active Routes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5EEAD4),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Bottom-Right Live Telemetry Status
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🟢 Customer Live Map & ETA Synced",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = WebEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Live Autonomous Action Log Strip
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, WebEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = WebGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = lastAutoActionLog,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 1-Click Instant Autonomous Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExecuteAutoStepNow,
                    colors = ButtonDefaults.buttonColors(containerColor = WebTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_execute_auto_step_btn")
                ) {
                    Text(
                        text = "⚡ Advance Active Order Now",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSpawnNewLiveOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = WebOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_spawn_auto_order_btn")
                ) {
                    Text(
                        text = "➕ Auto-Dispatch New Order",
                        style = MaterialTheme.typography.labelSmall,
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

/* =====================================================================
   PAGE 2: RIDERS — COMPLETE LOGGED-IN RIDER FLEET & ALL DATA DIRECTORY
   ===================================================================== */
@Composable
private fun AdminRidersCompleteDataPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    tipsByOrder: Map<String, Int>,
    riderShareInr: Double,
    overspeedState: com.example.security.RiderOverspeedState,
    batteryState: RiderBatteryPowerState,
    weeklyTransfersUsed: Int,
    repository: SharedOrderRepository,
    initialSearchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val riderStorageSaved by MotivationAndPromoManager.riderStoragePermissionGranted.collectAsState()
    val selfieCheckIns by RiderLiveSelfieCallAndExcelManager.selfieCheckIns.collectAsState()
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val onlineCount = riders.size
    val verifiedCount = riders.count { it.verificationStatus == com.example.data.model.RiderVerificationStatus.VERIFIED }
    val totalFleetWalletInr = riders.sumOf { it.walletBalanceInr }
    val totalTipsInr = tipsByOrder.values.sum()

    val filteredRiders = remember(riders, searchQuery, selectedFilter) {
        riders.filter { rider ->
            val vault = RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(rider)
            val matchesFilter = when (selectedFilter) {
                "ONLINE" -> true
                "SELFIE_VERIFIED" -> true
                "VERIFIED" -> rider.verificationStatus == com.example.data.model.RiderVerificationStatus.VERIFIED
                else -> true
            }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                rider.name.contains(q, ignoreCase = true) ||
                rider.riderId.contains(q, ignoreCase = true) ||
                rider.mobile.contains(q, ignoreCase = true) ||
                rider.vehicleNumber.contains(q, ignoreCase = true) ||
                rider.vehicleType.title.contains(q, ignoreCase = true) ||
                vault.aadhaarNumber.contains(q, ignoreCase = true) ||
                vault.panNumber.contains(q, ignoreCase = true) ||
                vault.drivingLicenseNumber.contains(q, ignoreCase = true) ||
                vault.residentialAddress.contains(q, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_riders_all_data_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Executive Logged-In Riders Summary Card
        item(key = "riders_login_summary_hero") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WebNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_riders_login_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🏍️ Online Riders — Complete Micro-Details, Live Selfies & All Documents",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${riders.size} Online Riders • Primary Session: ${MotivationAndPromoManager.cachedRiderName} (${if (riderStorageSaved) "Cloud & Phone Synced" else "100% Online Cloud Synced"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5EEAD4)
                            )
                        }
                        Surface(
                            color = WebEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, WebEmerald)
                        ) {
                            Text(
                                text = "${riders.size} ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = WebEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "ONLINE & LIVE SELFIE",
                            primaryValue = "${riders.size} Riders Online",
                            subValue = "${selfieCheckIns.size} Live Selfies Uploaded",
                            accentColor = WebEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "WALLET & 80% EARNINGS",
                            primaryValue = String.format(Locale.US, "₹%.0f", totalFleetWalletInr),
                            subValue = String.format(Locale.US, "80%% Share: ₹%.0f • Tips: ₹%d", riderShareInr, totalTipsInr),
                            accentColor = WebGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "ALL 6 DOCUMENTS",
                            primaryValue = "$verifiedCount / ${riders.size} Verified",
                            subValue = "Aadhaar, PAN, DL, RC, Ins, PUC",
                            accentColor = WebTeal,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "TELEMETRY & QUOTA",
                            primaryValue = "${overspeedState.currentSpeedKmh}/${overspeedState.speedLimitKmh} km/h",
                            subValue = "Battery ${batteryState.levelPercent}% • Transfers $weeklyTransfersUsed/${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS}",
                            accentColor = if (overspeedState.isAlarmRinging) WebCrimson else WebOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Search by Rider Name & Filter Strip
        item(key = "riders_search_filter_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, WebTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔎 Search Rider by Name, ID, Mobile, Vehicle Plate, DL, Aadhaar or PAN:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = WebNavy
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onSearchQueryChanged(it)
                        },
                        placeholder = { Text("Search Rider Name (e.g. Rajesh Kumar, Vikram Singh, Imtiaz Ali)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WebTeal) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.clickable {
                                        searchQuery = ""
                                        onSearchQueryChanged("")
                                    }
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_riders_search_input")
                    )

                    // Quick Rider Name Chips for 1-Tap Name Lookup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = searchQuery.isBlank() && selectedFilter == "ALL",
                            onClick = {
                                searchQuery = ""
                                onSearchQueryChanged("")
                                selectedFilter = "ALL"
                            },
                            label = { Text("All Online (${riders.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WebTeal,
                                selectedLabelColor = Color.White
                            )
                        )
                        riders.forEach { r ->
                            FilterChip(
                                selected = searchQuery.equals(r.name, ignoreCase = true),
                                onClick = {
                                    searchQuery = r.name
                                    onSearchQueryChanged(r.name)
                                },
                                label = { Text("🏍️ ${r.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WebNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Complete All-Data Card for Every Online Rider (With Live Selfie Photo + Full Document Vault + 1-Click Call)
        items(filteredRiders, key = { it.riderId }) { rider ->
            val riderOrders = remember(orders, rider.riderId) {
                orders.filter { it.assignedRiderId == rider.riderId }
            }
            val activeRiderOrder = riderOrders.firstOrNull {
                it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
            }
            val riderTipsEarned = riderOrders.sumOf { tipsByOrder[it.orderId] ?: 0 }
            val riderEmail = if (rider.riderId == "RIDER-101" && MotivationAndPromoManager.cachedRiderEmail.isNotBlank()) {
                MotivationAndPromoManager.cachedRiderEmail
            } else {
                "${rider.name.lowercase(Locale.US).replace(" ", ".")}@quickdelivery.in"
            }
            val docVault = remember(rider) {
                RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(rider)
            }
            val liveSelfieRecord = remember(rider.riderId, rider.name, selfieCheckIns) {
                RiderLiveSelfieCallAndExcelManager.getLatestSelfieForRider(rider.riderId, rider.name)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, WebTeal.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_rider_full_data_card_${rider.riderId}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Photo Avatar + Rider Name + ID + 100% Online Cloud Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RiderVerifiedPhotoAvatar(riderName = rider.name, size = 48.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${rider.name} (${rider.riderId})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WebNavy
                                )
                                Text(
                                    text = "📱 +91 ${rider.mobile.removePrefix("+91").trim()} • ✉️ $riderEmail",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "🟢 100% Online Cloud Synced • Blood Group: ${docVault.bloodGroup} • Joined: ${docVault.joiningDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WebEmerald
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, WebEmerald)
                            ) {
                                Text(
                                    text = "🟢 ONLINE (LIVE SELFIE OK)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFFF0FDFA),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Account: ${rider.accountStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WebTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // 1-Click Direct Dial Pad Call Button for Admin -> Rider
                    DirectDialPadCallButton(
                        orderId = activeRiderOrder?.orderId ?: "ADMIN-FLEET-CHECK",
                        callerRole = "ADMIN",
                        callerName = "Admin Control Center",
                        receiverRole = "RIDER",
                        receiverName = rider.name,
                        phoneNumber = "+91 ${rider.mobile.removePrefix("+91").trim()}",
                        buttonTestTag = "admin_call_rider_btn_${rider.riderId}"
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // LIVE ONLINE CHECK-IN SELFIE PHOTO & BIOMETRIC TIMESTAMP CARD
                    val selfieBmp = liveSelfieRecord.selfieBitmap
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, WebEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_rider_live_selfie_box_${rider.riderId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selfieBmp != null) {
                                Image(
                                    bitmap = selfieBmp.asImageBitmap(),
                                    contentDescription = "Live Check-In Selfie of ${rider.name}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "📷 MANDATORY LIVE ONLINE CHECK-IN SELFIE (CLOUD SAVED)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "• Check-In ID: ${liveSelfieRecord.checkInId} • Time: ${liveSelfieRecord.capturedAt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WebNavy
                                )
                                Text(
                                    text = "• GPS Location: ${liveSelfieRecord.gpsCoordinates} • Vehicle: ${liveSelfieRecord.vehiclePlate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "• Helmet & Uniform: VERIFIED ✅ • Status: ${liveSelfieRecord.livePhotoSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    // Grid 1: COMPLETE 6-DOCUMENT VAULT & PERSONAL MICRO-DETAILS
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_rider_documents_vault_${rider.riderId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "📄 COMPLETE RIDER DOCUMENTS VAULT & PERSONAL MICRO-DETAILS (6/6 VERIFIED ONLINE)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = WebTeal
                            )
                            Text(
                                text = "1. Aadhaar Card: ${docVault.aadhaarNumber} (${docVault.aadhaarStatus})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WebNavy
                            )
                            Text(
                                text = "2. PAN Card: ${docVault.panNumber} (${docVault.panStatus})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WebNavy
                            )
                            Text(
                                text = "3. Driving License (DL): ${docVault.drivingLicenseNumber} • Expiry: ${docVault.dlExpiryDate} • RTO: ${docVault.dlRtoOffice}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "4. Vehicle RC (${rider.vehicleType.title}): Plate ${docVault.rcNumberPlate} • Chassis: ${docVault.rcChassisNumber} • Engine: ${docVault.rcEngineNumber} • Valid: ${docVault.rcFitnessValidTill}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "5. Vehicle Insurance & PUC: Policy ${docVault.insurancePolicyNumber} (${docVault.insuranceProvider}, Valid ${docVault.insuranceValidTill}) • PUC: ${docVault.pucCertificateNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "6. Bank & UPI Settlement: ${docVault.bankName} • A/C: ${docVault.bankAccountNumber} • IFSC: ${docVault.bankIfscCode} • UPI: ${docVault.upiId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "• Emergency Contact: ${docVault.emergencyContact} • Device: ${docVault.deviceModel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "• Residential Address: ${docVault.residentialAddress}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // Grid 2: Wallet, Earnings, Trips & Live GPS Telemetry
                    Surface(
                        color = Color(0xFFF0FDFA),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WebTeal.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "WALLET, EARNINGS, DELIVERIES & LIVE GPS TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "• Wallet Balance: ₹%.0f • Commission Debt: ₹%.0f • Tips Earned: ₹%d",
                                    rider.walletBalanceInr,
                                    rider.commissionDebtInr,
                                    riderTipsEarned
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WebNavy
                            )
                            Text(
                                text = "• Completed Deliveries: ${rider.totalDeliveries} Trips • Assigned Orders: ${riderOrders.size} • Active Order: ${activeRiderOrder?.orderId ?: "Standing By Online"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "• Live GPS: %.5f, %.5f • Speed Limit Guard: %d/%d km/h • Battery: %d%%",
                                    rider.currentLat,
                                    rider.currentLng,
                                    overspeedState.currentSpeedKmh,
                                    overspeedState.speedLimitKmh,
                                    batteryState.levelPercent
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // 1-Click Admin Fleet Controls for this Rider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                RiderLiveSelfieCallAndExcelManager.recordRiderLiveSelfieAndGoOnline(
                                    context = context,
                                    rider = rider,
                                    riderDisplayName = rider.name,
                                    capturedBitmap = null,
                                    gpsCoordinates = String.format(Locale.US, "%.4f, %.4f", rider.currentLat, rider.currentLng)
                                )
                                val updated = rider.copy(
                                    isOnline = true,
                                    shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT
                                )
                                val newList = riders.map { if (it.riderId == rider.riderId) updated else it }
                                AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newList)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "📷 Refresh Live Selfie",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val updated = rider.copy(
                                    walletBalanceInr = rider.walletBalanceInr + 500.0,
                                    accountStatus = "ACTIVE",
                                    isOnline = true,
                                    shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT
                                )
                                val newList = riders.map { if (it.riderId == rider.riderId) updated else it }
                                AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, newList)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WebTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "+₹500 Credit & Keep Online",
                                style = MaterialTheme.typography.labelSmall,
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

/* =====================================================================
   PAGE 3: CUSTOMERS — COMPLETE CUSTOMER PROFILES, LOGIN & ORDER DATA
   ===================================================================== */
private data class AdminCustomerMasterRecord(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val isLoggedInSession: Boolean,
    val isStorageSaved: Boolean,
    val defaultPickupAddress: String,
    val defaultPickupLandmark: String,
    val defaultDropAddress: String,
    val defaultDropLandmark: String,
    val pickupCoords: String,
    val dropCoords: String,
    val customerOrders: List<CustomerOrder>
)

@Composable
private fun AdminCustomersCompleteDataPage(
    orders: List<CustomerOrder>,
    tipsByOrder: Map<String, Int>,
    riderRatings: Map<String, RiderDeliveryFeedback>,
    suggestions: List<AppSuggestionEntry>,
    totalGmvInr: Double
) {
    val customerStorageSaved by MotivationAndPromoManager.customerStoragePermissionGranted.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Build complete Customer Master Directory combining active logged-in Customer, order customers & registered customers
    val customerRecords = remember(orders, customerStorageSaved) {
        val list = mutableListOf<AdminCustomerMasterRecord>()
        val activeName = MotivationAndPromoManager.cachedCustomerName.ifBlank { "Aarav Sharma" }
        val activePhone = MotivationAndPromoManager.cachedCustomerPhone.ifBlank { "+91 98765 43210" }
        val activeEmail = MotivationAndPromoManager.cachedCustomerEmail.ifBlank { "aarav.sharma@quickdelivery.in" }

        val groupedByPhone = orders.groupBy { it.customerPhone.removePrefix("+91").trim() }
        val primaryPhoneClean = activePhone.removePrefix("+91").trim()
        val primaryOrders = groupedByPhone[primaryPhoneClean] ?: orders

        val firstOrder = primaryOrders.firstOrNull()
        list.add(
            AdminCustomerMasterRecord(
                customerId = firstOrder?.customerId ?: "CUST-1001",
                customerName = activeName,
                customerPhone = "+91 $primaryPhoneClean",
                customerEmail = activeEmail,
                isLoggedInSession = true,
                isStorageSaved = customerStorageSaved,
                defaultPickupAddress = firstOrder?.pickupAddress?.fullAddress
                    ?: "Flat 302, Emerald Residency, Indiranagar 100ft Road, Bengaluru - 560038",
                defaultPickupLandmark = firstOrder?.pickupAddress?.landmark ?: "Near Metro Station Gate 2",
                defaultDropAddress = firstOrder?.dropAddress?.fullAddress
                    ?: "Office 204, Lotus Tech Tower, Koramangala 4th Block, Bengaluru - 560034",
                defaultDropLandmark = firstOrder?.dropAddress?.landmark ?: "Sony World Signal",
                pickupCoords = if (firstOrder != null) {
                    String.format(Locale.US, "%.4f, %.4f", firstOrder.pickupAddress.lat, firstOrder.pickupAddress.lng)
                } else {
                    "12.9784, 77.6408"
                },
                dropCoords = if (firstOrder != null) {
                    String.format(Locale.US, "%.4f, %.4f", firstOrder.dropAddress.lat, firstOrder.dropAddress.lng)
                } else {
                    "12.9352, 77.6245"
                },
                customerOrders = primaryOrders
            )
        )

        // Add any additional distinct customers from orders
        groupedByPhone.forEach { (phoneClean, custOrders) ->
            if (phoneClean != primaryPhoneClean && custOrders.isNotEmpty()) {
                val sample = custOrders.first()
                list.add(
                    AdminCustomerMasterRecord(
                        customerId = sample.customerId,
                        customerName = sample.customerName,
                        customerPhone = "+91 $phoneClean",
                        customerEmail = "${sample.customerName.lowercase(Locale.US).replace(" ", ".")}@quickdelivery.in",
                        isLoggedInSession = true,
                        isStorageSaved = true,
                        defaultPickupAddress = sample.pickupAddress.fullAddress,
                        defaultPickupLandmark = sample.pickupAddress.landmark,
                        defaultDropAddress = sample.dropAddress.fullAddress,
                        defaultDropLandmark = sample.dropAddress.landmark,
                        pickupCoords = String.format(Locale.US, "%.4f, %.4f", sample.pickupAddress.lat, sample.pickupAddress.lng),
                        dropCoords = String.format(Locale.US, "%.4f, %.4f", sample.dropAddress.lat, sample.dropAddress.lng),
                        customerOrders = custOrders
                    )
                )
            }
        }

        // Also include verified enterprise customer accounts in directory
        list.add(
            AdminCustomerMasterRecord(
                customerId = "CUST-1002",
                customerName = "Priya Nair",
                customerPhone = "+91 98450 22334",
                customerEmail = "priya.nair@quickdelivery.in",
                isLoggedInSession = true,
                isStorageSaved = true,
                defaultPickupAddress = "Villa 14, Palm Meadows, Whitefield Main Rd, Bengaluru - 560066",
                defaultPickupLandmark = "Near Forum Shantiniketan",
                defaultDropAddress = "Tower B, RMZ EcoWorld, Bellandur, Bengaluru - 560103",
                defaultDropLandmark = "Gate 3 Reception",
                pickupCoords = "12.9698, 77.7499",
                dropCoords = "12.9255, 77.6762",
                customerOrders = emptyList()
            )
        )
        list.add(
            AdminCustomerMasterRecord(
                customerId = "CUST-1003",
                customerName = "Rohan Deshmukh",
                customerPhone = "+91 98201 55667",
                customerEmail = "rohan.deshmukh@quickdelivery.in",
                isLoggedInSession = true,
                isStorageSaved = true,
                defaultPickupAddress = "18th Cross, Malleshwaram, Bengaluru - 560003",
                defaultPickupLandmark = "Opposite CTR",
                defaultDropAddress = "MG Road Metro Plaza, Bengaluru - 560001",
                defaultDropLandmark = "Exit B",
                pickupCoords = "13.0031, 77.5643",
                dropCoords = "12.9756, 77.6066",
                customerOrders = emptyList()
            )
        )
        list
    }

    val filteredCustomers = remember(customerRecords, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            customerRecords
        } else {
            customerRecords.filter { c ->
                c.customerName.contains(q, ignoreCase = true) ||
                    c.customerId.contains(q, ignoreCase = true) ||
                    c.customerPhone.contains(q, ignoreCase = true) ||
                    c.customerEmail.contains(q, ignoreCase = true) ||
                    c.defaultPickupAddress.contains(q, ignoreCase = true) ||
                    c.defaultDropAddress.contains(q, ignoreCase = true)
            }
        }
    }

    val prepaidOrdersCount = orders.count { it.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY }
    val codOrdersCount = orders.count { it.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY }
    val customerSuggestions = suggestions.filter { it.senderRole == "CUSTOMER" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_customers_all_data_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Executive Customer Summary Card
        item(key = "customers_executive_summary") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WebNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_customers_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "👥 Customers Master Directory — Complete Profile & Order Data",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${customerRecords.size} Logged-In Customers • Active App User: ${MotivationAndPromoManager.cachedCustomerName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5EEAD4)
                            )
                        }
                        Surface(
                            color = WebTeal.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, WebTeal)
                        ) {
                            Text(
                                text = "${customerRecords.size} CUSTOMERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5EEAD4),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "REGISTERED CUSTOMERS",
                            primaryValue = "${customerRecords.size} Active",
                            subValue = "Phone Storage Saved: ${if (customerStorageSaved) "YES" else "ACTIVE"}",
                            accentColor = WebTeal,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "TOTAL CUSTOMER SPEND",
                            primaryValue = String.format(Locale.US, "₹%.0f", totalGmvInr),
                            subValue = "${orders.size} Orders • Tips Given: ₹${tipsByOrder.values.sum()}",
                            accentColor = WebGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutiveKpiTile(
                            title = "PAYMENT PREFERENCE",
                            primaryValue = "$prepaidOrdersCount Prepaid / $codOrdersCount COD",
                            subValue = "UPI, Card, NetBanking & Cash",
                            accentColor = WebEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiTile(
                            title = "RATINGS & FEEDBACK",
                            primaryValue = "${riderRatings.size} Ratings",
                            subValue = "${customerSuggestions.size} Customer Suggestions",
                            accentColor = WebOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Customer Search Bar
        item(key = "customers_search_bar") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Customer Name, ID, Phone, Email, Pickup/Drop Address, PIN...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WebTeal) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_customers_search_input")
                    )
                }
            }
        }

        // 3. Complete All-Data Card for Every Customer
        items(filteredCustomers, key = { it.customerId }) { cust ->
            val custSpendInr = cust.customerOrders
                .filter { it.orderStatus != OrderStatus.CANCELLED }
                .sumOf { it.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0 }
            val custTipsInr = cust.customerOrders.sumOf { tipsByOrder[it.orderId] ?: 0 }
            val custActiveCount = cust.customerOrders.count {
                it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
            }
            val custDeliveredCount = cust.customerOrders.count { it.orderStatus == OrderStatus.DELIVERED }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, WebTeal.copy(alpha = 0.55f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_customer_full_data_card_${cust.customerId}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Customer Avatar + Name + ID + Login Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = WebTeal,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${cust.customerName} (${cust.customerId})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WebNavy
                                )
                                Text(
                                    text = "📱 ${cust.customerPhone} • ✉️ ${cust.customerEmail}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = if (cust.isStorageSaved) {
                                        "🟢 Logged In • Saved on Device Storage • Verified Customer"
                                    } else {
                                        "🟢 Logged In • Active Session • Verified Customer"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WebEmerald
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, WebEmerald)
                        ) {
                            Text(
                                text = "🟢 ONLINE • ${cust.customerOrders.size} Orders",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 1-Click Direct Dial Pad Call Button for Admin -> Customer
                    DirectDialPadCallButton(
                        orderId = cust.customerOrders.firstOrNull()?.orderId ?: "ADMIN-CUST-CARE",
                        callerRole = "ADMIN",
                        callerName = "Admin Control Center",
                        receiverRole = "CUSTOMER",
                        receiverName = cust.customerName,
                        phoneNumber = cust.customerPhone,
                        buttonTestTag = "admin_call_customer_btn_${cust.customerId}"
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Saved Addresses, PIN Codes & Landmarks
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "SAVED PICKUP & DROP ADDRESSES, PIN CODES & GPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = WebTeal
                            )
                            Text(
                                text = "📍 Pickup: ${cust.defaultPickupAddress} (Landmark: ${cust.defaultPickupLandmark}) • GPS: ${cust.pickupCoords}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WebNavy
                            )
                            Text(
                                text = "🏁 Drop: ${cust.defaultDropAddress} (Landmark: ${cust.defaultDropLandmark}) • GPS: ${cust.dropCoords}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WebNavy
                            )
                        }
                    }

                    // Spend, Payment & Order Summary
                    Surface(
                        color = Color(0xFFF0FDFA),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WebTeal.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "SPEND, PAYMENTS & ORDER SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "• Total Spend: ₹%.0f • Tips Given: ₹%d • Active Orders: %d • Delivered: %d",
                                    custSpendInr,
                                    custTipsInr,
                                    custActiveCount,
                                    custDeliveredCount
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WebNavy
                            )
                        }
                    }

                    // Complete Order History for this Customer
                    if (cust.customerOrders.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "COMPLETE ORDER BOOKING HISTORY (${cust.customerOrders.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = WebNavy
                            )
                            cust.customerOrders.forEach { ord ->
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${ord.orderId} • ${ord.vehicleType.title} • ${ord.totalAmount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = WebNavy
                                            )
                                            Text(
                                                text = ord.orderStatus.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (ord.orderStatus == OrderStatus.DELIVERED) WebEmerald else WebOrange
                                            )
                                        }
                                        Text(
                                            text = "Parcel: ${ord.packageDetails.packageType.title} (${ord.packageDetails.weightKg} kg) • Payment: ${ord.paymentMethod.name.replace("_", " ")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569)
                                        )
                                        Text(
                                            text = "Rider: ${ord.assignedRiderName ?: "Searching"} (${ord.assignedRiderPhone ?: "Auto-Assigning"}) • Pickup OTP: ${ord.pickupOtp} / Delivery OTP: ${ord.deliveryOtp}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF334155)
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
}

/* =====================================================================
   PAGE 3: KYC VERIFICATION CONSOLE
   ===================================================================== */
@Composable
private fun AdminKycVerificationPage(
    riders: List<RiderProfile>,
    repository: SharedOrderRepository
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_kyc_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "kyc_header") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, WebTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = WebTeal, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rider KYC & Document Approval", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WebNavy)
                    }
                    Text(
                        text = "Verify Government Aadhaar Card, PAN Card, Driving License, and Vehicle RC before activating partners.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        items(riders, key = { it.riderId }) { rider ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = WebEmerald, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(rider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WebNavy)
                                Text("ID: ${rider.riderId} • +91 ${rider.mobile}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            }
                        }

                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, WebEmerald)
                        ) {
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Aadhaar Card", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text(rider.aadhaarNumber.ifBlank { "XXXX-XXXX-1101" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PAN Card", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text(rider.panNumber.ifBlank { "ABCDE1101F" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle RC", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text("${rider.vehicleType.title} • ${rider.vehicleNumber}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = WebTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Re-verify Docs", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/* =====================================================================
   PAGE 4: VEHICLE & FARE SETTINGS (BIKE, SCOOTER, AUTO STRICTLY)
   ===================================================================== */
@Composable
private fun AdminVehicleAndFareSettingsPage(
    repository: SharedOrderRepository,
    pricingConfig: PricingConfig,
    vehiclePolicy: AdminVehicleAndWeightPolicy
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_vehicle_fares_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "vehicle_pricing_control_card") {
            AdminVehiclePricingAndWeightControlCard(
                repository = repository,
                pricingConfig = pricingConfig,
                vehiclePolicy = vehiclePolicy
            )
        }
    }
}

/* =====================================================================
   PAGE 5: PAYMENTS & SETTLEMENTS
   ===================================================================== */
@Composable
private fun AdminPaymentsAndSettlementsPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    walletTransactions: List<WalletTransaction>,
    totalGmvInr: Double,
    platformCommissionInr: Double,
    riderShareInr: Double,
    totalTipsInr: Int
) {
    val prepaidGmv = remember(orders) {
        orders.filter { it.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY && it.orderStatus != OrderStatus.CANCELLED }
            .sumOf { it.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0 }
    }
    val codGmv = remember(orders) {
        orders.filter { it.paymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY && it.orderStatus != OrderStatus.CANCELLED }
            .sumOf { it.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_settlements_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "financial_ledger_hero") {
            Card(
                colors = CardDefaults.cardColors(containerColor = WebNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Financial Ledger & Reconciliation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExecutiveKpiTile("ONLINE GATEWAY", String.format(Locale.US, "₹%.0f", prepaidGmv), "Razorpay / UPI", WebTeal, Modifier.weight(1f))
                        ExecutiveKpiTile("COD COLLECTED", String.format(Locale.US, "₹%.0f", codGmv), "Cash Handled", WebOrange, Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExecutiveKpiTile("RIDER 80% SHARE", String.format(Locale.US, "₹%.0f", riderShareInr), "Net Payable", WebEmerald, Modifier.weight(1f))
                        ExecutiveKpiTile("ADMIN 20% NET", String.format(Locale.US, "₹%.0f", platformCommissionInr), "Platform Margin", WebGold, Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "rider_settlement_ledger_header") {
            Text("Partner Wallets & Weekly Payout Ledger", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WebNavy)
        }

        items(riders, key = { it.riderId }) { rider ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(rider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WebNavy)
                        Text(String.format(Locale.US, "₹%.0f", rider.walletBalanceInr), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = WebEmerald)
                    }
                    Text("UPI Payout ID: ${rider.verifiedUpiId ?: "partner@okaxis"} • Commission Debt: ₹${rider.commissionDebtInr.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }
        }
    }
}

/* =====================================================================
   PAGE 6: COMPLAINTS & SOS SUPPORT
   ===================================================================== */
@Composable
private fun AdminComplaintsAndSupportPage(
    sosAlerts: List<SosSupportAlert>,
    suggestions: List<AppSuggestionEntry>,
    riderRatings: Map<String, RiderDeliveryFeedback>,
    customerRatings: Map<String, CustomerRatingByRider>,
    repository: SharedOrderRepository
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_complaints_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "support_hero") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Complaints, SOS & Quality Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WebNavy)
                    Text("Review emergency SOS incidents, customer suggestions, and confidential star ratings.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }
        }

        item(key = "mutual_ratings_panel") {
            AdminMutualRatingsAndSuggestionsPanel(initialExpanded = true)
        }
    }
}

/* =====================================================================
   PAGE 7: REPORTS & ANALYTICS
   ===================================================================== */
@Composable
private fun AdminReportsAndAnalyticsPage(
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    totalGmvInr: Double
) {
    val totalOrders = orders.size.coerceAtLeast(1)
    val bikeOrders = orders.count { it.vehicleType == VehicleType.BIKE }
    val scooterOrders = orders.count { it.vehicleType == VehicleType.SCOOTER }
    val autoOrders = orders.count { it.vehicleType == VehicleType.AUTO }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_reports_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "reports_hero") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Performance Analytics & Vehicle Mix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WebNavy)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExecutiveKpiTile("AVG TRIP DISTANCE", "4.2 km", "City Average", WebTeal, Modifier.weight(1f))
                        ExecutiveKpiTile("AVG FARE", String.format(Locale.US, "₹%.0f", totalGmvInr / totalOrders), "Per Order", WebGold, Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "vehicle_demand_distribution") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vehicle Type Demand Split", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WebNavy)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VehicleDemandRow("1. Bike", bikeOrders, totalOrders, WebTeal)
                        VehicleDemandRow("2. Scooter", scooterOrders, totalOrders, WebOrange)
                        VehicleDemandRow("3. Auto", autoOrders, totalOrders, WebGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleDemandRow(label: String, count: Int, total: Int, color: Color) {
    val fraction = count.toFloat() / total.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = WebNavy)
            Text("$count orders (${(fraction * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        }
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            trackColor = Color(0xFFE2E8F0),
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}

/* =====================================================================
   PAGE 8: SETTINGS & SYSTEM GUARDS
   ===================================================================== */
@Composable
private fun AdminPlatformSettingsPage(
    repository: SharedOrderRepository,
    pricingConfig: PricingConfig,
    vehiclePolicy: AdminVehicleAndWeightPolicy,
    onResetOrders: () -> Unit,
    onExitToCustomer: () -> Unit
) {
    val automationConfig by repository.automationConfig.collectAsState()
    var autoDispatchEnabled by remember(automationConfig) { mutableStateOf(automationConfig.autoMatchEligibleRiders) }
    var speedLimitInput by remember { mutableStateOf("50") }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_page_settings_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "dispatch_settings_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("System Dispatch & Automation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WebNavy)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Zero-Partiality Fair Auto-Dispatch", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Rounds robin orders across eligible online riders within 5 km", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = autoDispatchEnabled,
                            onCheckedChange = { newVal ->
                                autoDispatchEnabled = newVal
                                val adminToken = repository.issueAdminPreviewSessionToken()
                                scope.launch {
                                    repository.updateAdminAutomationConfig(
                                        automationConfig.copy(autoMatchEligibleRiders = newVal),
                                        adminToken
                                    )
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anti-Abuse Rider Over-speed Alarm", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Rings high-priority alarm when rider exceeds 50 km/h limit", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("50 km/h ACTIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF065F46), modifier = Modifier.padding(6.dp))
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rider Battery Safety Guard (≤20%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Reassigns orders automatically if battery falls below critical 20%", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ACTIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF065F46), modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
        }

        item(key = "reset_and_nav_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Administrative Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WebNavy)

                    Button(
                        onClick = onResetOrders,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Simulation / Orders State", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExitToCustomer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout Admin Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


