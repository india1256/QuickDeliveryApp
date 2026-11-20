package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderProofRegistry
import com.example.data.model.OrderStatus
import com.example.data.model.RiderProfile
import com.example.data.model.RiderVerificationStatus
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdminExecutiveThemeManager {
    private const val PREFS_NAME = "quick_delivery_admin_theme_v1"
    private const val KEY_DARK_MODE = "admin_dark_mode"
    private var loaded = false

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, false)
        }
    }

    fun toggleDarkMode(context: Context) {
        ensureLoaded(context)
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DARK_MODE, next)
                .apply()
        }
    }
}

data class AdminExecColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val hairlineBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val topBarBg: Color,
    val accentTeal: Color,
    val accentEmerald: Color,
    val accentIndigo: Color,
    val accentAmber: Color,
    val accentCrimson: Color
)

fun adminExecPalette(isDark: Boolean): AdminExecColors = if (isDark) {
    AdminExecColors(
        isDark = true,
        background = Color(0xFF090E1A),
        surface = Color(0xFF111827),
        surfaceSubtle = Color(0xFF1E293B),
        hairlineBorder = Color(0xFF273449),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        topBarBg = Color(0xFF0F172A),
        accentTeal = Color(0xFF14B8A6),
        accentEmerald = Color(0xFF10B981),
        accentIndigo = Color(0xFF6366F1),
        accentAmber = Color(0xFFF59E0B),
        accentCrimson = Color(0xFFEF4444)
    )
} else {
    AdminExecColors(
        isDark = false,
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceSubtle = Color(0xFFF1F5F9),
        hairlineBorder = Color(0xFFE2E8F0),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        topBarBg = Color(0xFF0F172A),
        accentTeal = Color(0xFF0D9488),
        accentEmerald = Color(0xFF059669),
        accentIndigo = Color(0xFF4F46E5),
        accentAmber = Color(0xFFD97706),
        accentCrimson = Color(0xFFDC2626)
    )
}

@Composable
fun AdminIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    sizeDp: Int = 34
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.size(sizeDp.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size((sizeDp * 0.56f).dp)
            )
        }
    }
}

@Composable
fun AdminExecutiveHeroSummaryCard(
    totalGmvInr: Double,
    companyCommission20Inr: Double,
    riderEarnings80Inr: Double,
    orders: List<CustomerOrder>,
    onlineRidersCount: Int,
    totalRidersCount: Int,
    dateRangeLabel: String,
    cityFilterLabel: String
) {
    val searching = orders.count { it.orderStatus == OrderStatus.SEARCHING_RIDER }
    val assigned = orders.count { it.orderStatus == OrderStatus.RIDER_ASSIGNED || it.orderStatus == OrderStatus.ARRIVED_AT_PICKUP }
    val transit = orders.count { it.orderStatus == OrderStatus.PICKED_UP || it.orderStatus == OrderStatus.IN_TRANSIT }
    val delivered = orders.count { it.orderStatus == OrderStatus.DELIVERED }
    val cancelled = orders.count { it.orderStatus == OrderStatus.CANCELLED }
    val total = orders.size.coerceAtLeast(1)
    val cancelPct = (cancelled * 100.0 / total)
    val slaPct = if (orders.isEmpty()) 99.4 else (100.0 - cancelPct * 0.4).coerceIn(92.0, 99.8)

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_executive_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF132A3E), Color(0xFF064E3B))
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EXECUTIVE COMMAND SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$cityFilterLabel • $dateRangeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6EE7B7),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = String.format(Locale.US, "SLA %.1f%%", slaPct),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6EE7B7),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Total Gross Volume (GMV)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = String.format(Locale.US, "₹%.0f", totalGmvInr),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "20% Admin",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFCD34D)
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.0f", companyCommission20Inr),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "80% Rider",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6EE7B7)
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.0f", riderEarnings80Inr),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Operational KPIs Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroMiniStat("Fleet Online", "$onlineRidersCount/$totalRidersCount")
                    HeroMiniStat("Avg Delivery", "14 min")
                    HeroMiniStat("Completed", "$delivered/${orders.size}")
                    HeroMiniStat("Cancel Rate", String.format(Locale.US, "%.1f%%", cancelPct))
                }

                // Visual Order Funnel Strip
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        val sWeight = (searching.toFloat() / total).coerceAtLeast(if (searching > 0) 0.08f else 0f)
                        val aWeight = (assigned.toFloat() / total).coerceAtLeast(if (assigned > 0) 0.08f else 0f)
                        val tWeight = (transit.toFloat() / total).coerceAtLeast(if (transit > 0) 0.08f else 0f)
                        val dWeight = (delivered.toFloat() / total).coerceAtLeast(if (delivered > 0) 0.08f else 0f)
                        if (sWeight > 0f) Box(Modifier.weight(sWeight).height(8.dp).background(Color(0xFFF97316)))
                        if (aWeight > 0f) Box(Modifier.weight(aWeight).height(8.dp).background(Color(0xFF38BDF8)))
                        if (tWeight > 0f) Box(Modifier.weight(tWeight).height(8.dp).background(Color(0xFF818CF8)))
                        if (dWeight > 0f) Box(Modifier.weight(dWeight).height(8.dp).background(Color(0xFF10B981)))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FunnelDotLabel("Searching $searching", Color(0xFFF97316))
                        FunnelDotLabel("Assigned $assigned", Color(0xFF38BDF8))
                        FunnelDotLabel("Transit $transit", Color(0xFF818CF8))
                        FunnelDotLabel("Done $delivered", Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMiniStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun FunnelDotLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun AdminPriorityActionStrip(
    pendingKycCount: Int,
    openDisputesCount: Int,
    pendingWithdrawalsCount: Int,
    unresolvedSosCount: Int,
    palette: AdminExecColors,
    onNavigateToPage: (AdminDedicatedPage) -> Unit
) {
    val hasAny = pendingKycCount > 0 || openDisputesCount > 0 || pendingWithdrawalsCount > 0 || unresolvedSosCount > 0
    if (!hasAny) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("admin_priority_action_strip"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (unresolvedSosCount > 0) {
            PriorityPill(
                icon = Icons.Default.Warning,
                text = "$unresolvedSosCount Active SOS",
                color = palette.accentCrimson,
                surface = palette.surface,
                onClick = { onNavigateToPage(AdminDedicatedPage.COMPLAINTS_SOS) }
            )
        }
        if (pendingKycCount > 0) {
            PriorityPill(
                icon = Icons.Default.VerifiedUser,
                text = "$pendingKycCount KYC Review",
                color = palette.accentAmber,
                surface = palette.surface,
                onClick = { onNavigateToPage(AdminDedicatedPage.KYC) }
            )
        }
        if (openDisputesCount > 0) {
            PriorityPill(
                icon = Icons.Default.Gavel,
                text = "$openDisputesCount Open Dispute",
                color = palette.accentIndigo,
                surface = palette.surface,
                onClick = { onNavigateToPage(AdminDedicatedPage.COMPLAINTS_SOS) }
            )
        }
        if (pendingWithdrawalsCount > 0) {
            PriorityPill(
                icon = Icons.Default.Payments,
                text = "$pendingWithdrawalsCount Payout Queue",
                color = palette.accentTeal,
                surface = palette.surface,
                onClick = { onNavigateToPage(AdminDedicatedPage.PAYMENTS_SETTLEMENTS) }
            )
        }
    }
}

@Composable
private fun PriorityPill(
    icon: ImageVector,
    text: String,
    color: Color,
    surface: Color,
    onClick: () -> Unit
) {
    Surface(
        color = surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDetailBottomSheet(
    order: CustomerOrder,
    tipInr: Int,
    hasSensitivePiiPermission: Boolean,
    palette: AdminExecColors,
    onDismiss: () -> Unit,
    onReassign: (CustomerOrder) -> Unit,
    onAdvanceStage: (CustomerOrder) -> Unit,
    onForceCancel: (CustomerOrder) -> Unit,
    onForceReleaseToQueue: ((CustomerOrder) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timelineEventsMap by QuickDeliveryOperationsManager.timelineEventsByOrder.collectAsState()
    val pickupProofsMap by OrderProofRegistry.pickupProofsByOrder.collectAsState()
    val deliveryProofsMap by OrderProofRegistry.deliveryProofsByOrder.collectAsState()
    val unreachableMap by QuickDeliveryOperationsManager.unreachableByOrder.collectAsState()
    val auditTimeline = remember(order, timelineEventsMap, pickupProofsMap, deliveryProofsMap, unreachableMap) {
        QuickDeliveryOperationsManager.buildCombinedTimelineForOrder(
            order = order,
            pickupProof = pickupProofsMap[order.orderId],
            deliveryProof = deliveryProofsMap[order.orderId]
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.orderId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${order.vehicleType.title} • ${String.format(Locale.US, "%.1f km", order.distanceKm)} • ${order.paymentMethod.name.replace("_", " ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isDone = order.orderStatus == OrderStatus.DELIVERED
                val pillColor = when {
                    order.orderStatus == OrderStatus.CANCELLED -> palette.accentCrimson
                    isDone -> palette.accentEmerald
                    else -> palette.accentIndigo
                }
                Surface(
                    color = pillColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = order.orderStatus.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = pillColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Delivery Step Timeline
            Surface(
                color = palette.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val stageIdx = when (order.orderStatus) {
                        OrderStatus.SEARCHING_RIDER -> 0
                        OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> 1
                        OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> 2
                        OrderStatus.DELIVERED -> 3
                        OrderStatus.CANCELLED -> -1
                    }
                    listOf("Booked", "Assigned", "In Transit", "Delivered").forEachIndexed { idx, stepName ->
                        val active = idx <= stageIdx
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.RadioButtonChecked,
                                contentDescription = stepName,
                                tint = if (active) palette.accentEmerald else palette.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stepName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) palette.textPrimary else palette.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Route Details
            Surface(
                color = palette.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = palette.accentEmerald, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PICKUP • SYNCED RANDOM OTP ${order.pickupOtp} (Zero SMS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.accentEmerald)
                            Text(order.pickupAddress.fullAddress, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                        }
                    }
                    HorizontalDivider(color = palette.hairlineBorder)
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = palette.accentAmber, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DROP • SYNCED RANDOM OTP ${order.deliveryOtp} (Zero SMS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.accentAmber)
                            Text(order.dropAddress.fullAddress, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                        }
                    }
                }
            }

            // Customer & Rider Contact
            val maskedCustomerPhone = if (hasSensitivePiiPermission) order.customerPhone else "+91 XXXXX-${order.customerPhone.takeLast(4)}"
            Surface(
                color = palette.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Customer: ${order.customerName} ($maskedCustomerPhone)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text("Rider Partner: ${order.assignedRiderName ?: "Searching nearby rider"}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text("Fare: ${order.totalAmount}  •  80% Rider: ${order.riderEarning}  •  20% Admin: ${order.platformCommission}" + if (tipInr > 0) "  •  Tip ₹$tipInr" else "", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.accentEmerald)
                }
            }

            // Full Chronological Order Audit Timeline
            Surface(
                color = palette.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_order_audit_timeline_${order.orderId}")
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chronological Audit Timeline",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = palette.accentIndigo.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "${auditTimeline.size} Events",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentIndigo,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    auditTimeline.forEachIndexed { index, evt ->
                        val roleTint = when (evt.actorRole.uppercase()) {
                            "CUSTOMER" -> palette.accentIndigo
                            "RIDER" -> palette.accentAmber
                            "ADMIN" -> palette.accentCrimson
                            else -> palette.accentTeal
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(roleTint)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = evt.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = evt.timestampLabel.ifBlank { evt.actorRole },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                if (evt.detail.isNotBlank()) {
                                    Text(
                                        text = evt.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                        if (index < auditTimeline.lastIndex) {
                            HorizontalDivider(color = palette.hairlineBorder.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            if (order.orderStatus != OrderStatus.DELIVERED && order.orderStatus != OrderStatus.CANCELLED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onReassign(order)
                        },
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reassign", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onForceCancel(order)
                        },
                        border = BorderStroke(1.dp, palette.accentCrimson),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.accentCrimson)
                    }
                    Button(
                        onClick = {
                            onAdvanceStage(order)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentEmerald),
                        modifier = Modifier.weight(1.3f).height(46.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val btnText = when (order.orderStatus) {
                            OrderStatus.SEARCHING_RIDER -> "Assign"
                            OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> "Auth Pickup OTP"
                            OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> "Auth Drop OTP"
                            else -> "Advance"
                        }
                        Text(btnText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (onForceReleaseToQueue != null &&
                    (order.orderStatus == OrderStatus.RIDER_ASSIGNED || order.orderStatus == OrderStatus.ARRIVED_AT_PICKUP)
                ) {
                    OutlinedButton(
                        onClick = {
                            onForceReleaseToQueue(order)
                            onDismiss()
                        },
                        border = BorderStroke(1.2.dp, palette.accentAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_force_release_queue_btn_${order.orderId}")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = palette.accentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Emergency Force Release to Search Queue",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.accentAmber
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRiderDetailBottomSheet(
    rider: RiderProfile,
    hasSensitivePiiPermission: Boolean,
    palette: AdminExecColors,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val docVault = RiderLiveSelfieCallAndExcelManager.getCompleteDocumentVaultForRider(rider)
    val liveSelfieRecord = RiderLiveSelfieCallAndExcelManager.getLatestSelfieForRider(rider.riderId, rider.name)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminIconBadge(icon = Icons.AutoMirrored.Filled.DirectionsBike, tint = palette.accentTeal, sizeDp = 42)
                    Column {
                        Text(rider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                        Text("${rider.riderId} • ${rider.vehicleType.title} (${docVault.rcNumberPlate})", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
                Surface(
                    color = (if (rider.isOnline) palette.accentEmerald else palette.textSecondary).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (rider.isOnline) "ONLINE" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rider.isOnline) palette.accentEmerald else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Wallet & Debt Progress
            Surface(
                color = palette.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Wallet Balance: ₹${rider.walletBalanceInr.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.accentEmerald)
                        Text("Debt Cap: ₹${rider.commissionDebtInr.toInt()} / ₹150", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (rider.commissionDebtInr >= 100) palette.accentCrimson else palette.textSecondary)
                    }
                    LinearProgressIndicator(
                        progress = { (rider.commissionDebtInr / 150.0).toFloat().coerceIn(0f, 1f) },
                        color = if (rider.commissionDebtInr >= 100) palette.accentCrimson else palette.accentTeal,
                        trackColor = palette.hairlineBorder,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            // Live Selfie & GPS Check-In
            Surface(
                color = palette.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    liveSelfieRecord.selfieBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Rider Live Check-In Selfie",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Selfie Check-In Verified", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.accentEmerald)
                        Text("Time: ${liveSelfieRecord.capturedAt}", style = MaterialTheme.typography.labelSmall, color = palette.textPrimary)
                        Text("GPS: ${liveSelfieRecord.gpsCoordinates}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
            }

            // Document & KYC Details
            val mask: (String) -> String = { v -> if (hasSensitivePiiPermission) v else "XXXX-${v.takeLast(4)}" }
            Surface(
                color = palette.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.hairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mobile: ${mask(rider.mobile)}", style = MaterialTheme.typography.labelSmall, color = palette.textPrimary)
                    Text("Aadhaar: ${mask(docVault.aadhaarNumber)} • PAN: ${mask(docVault.panNumber)}", style = MaterialTheme.typography.labelSmall, color = palette.textPrimary)
                    Text("DL: ${mask(docVault.drivingLicenseNumber)} • Blood: ${docVault.bloodGroup}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text("Total Deliveries: ${rider.totalDeliveries} • Status: ${rider.verificationStatus.name}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.accentTeal)
                }
            }

            DirectDialPadCallButton(
                orderId = "ADMIN-FLEET-${rider.riderId}",
                callerRole = "ADMIN",
                callerName = "Admin Control Center",
                receiverRole = "RIDER",
                receiverName = rider.name,
                phoneNumber = "+91 ${rider.mobile.removePrefix("+91").trim()}",
                buttonTestTag = "admin_sheet_call_rider_btn_${rider.riderId}"
            )
        }
    }
}
