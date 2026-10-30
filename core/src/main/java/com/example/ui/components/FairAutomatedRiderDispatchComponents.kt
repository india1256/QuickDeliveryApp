package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.RiderProfile
import com.example.data.repository.SharedOrderRepository
import java.util.Locale

private val FairEmerald = Color(0xFF059669)
private val FairEmeraldBg = Color(0xFFECFDF5)
private val FairNavy = Color(0xFF0D1B2A)
private val FairTeal = Color(0xFF00A896)
private val FairOrange = Color(0xFFFF6B35)
private val FairAmber = Color(0xFFD97706)

/**
 * Displays the Zero-Partiality Automated Rider Discovery & Fair Queue Status.
 * Shows how every online rider receives automatic orders fairly in rotation without bias,
 * while respecting active-order locks, vehicle/cargo compatibility, and the ₹150 debt cap.
 */
@Composable
fun FairAutoRiderDispatchSummaryCard(
    repository: SharedOrderRepository,
    orders: List<CustomerOrder>,
    riders: List<RiderProfile>,
    roleView: String = "CUSTOMER",
    selectedRiderId: String? = null,
    onSelectRiderProfile: ((RiderProfile) -> Unit)? = null,
    onStatusMessage: ((String, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
    }

    val auditLog by AutomatedFairRiderAssignmentEngine.dispatchAuditLog.collectAsState()
    val activeOffers by AutomatedFairRiderAssignmentEngine.activeOffersByOrder.collectAsState()
    val searchingOrders = orders.filter { it.orderStatus == OrderStatus.SEARCHING_RIDER && it.assignedRiderId.isNullOrBlank() }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, FairEmerald.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("fair_auto_rider_dispatch_card_$roleView")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = FairEmerald,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = "Zero-Partiality Fair Auto-Dispatch",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Auto Rider Discovery • Zero-Partiality Fair Queue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = FairNavy
                        )
                        Text(
                            text = "Sabhi Riders Ko Barabar Automatic Order • ₹150 Debt & Active-Order Guard",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = FairEmerald
                        )
                    }
                }

                Surface(
                    color = FairEmeraldBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, FairEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = FairEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "100% FAIR AUTO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = FairEmerald
                        )
                    }
                }
            }

            // Horizontal Fair Queue Strip showing all verified riders & their fair order count
            Text(
                text = if (roleView == "RIDER") {
                    "Tap any Rider below to view their live dashboard & fair turn (${riders.size} Active Fleet Riders):"
                } else {
                    "Live Multi-Rider Fair Rotation Queue (${riders.count { it.isOnline }}/${riders.size} Online):"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                riders.forEach { rider ->
                    val activeOrderForRider = orders.firstOrNull { o ->
                        o.assignedRiderId == rider.riderId &&
                            o.orderStatus in listOf(
                                OrderStatus.RIDER_ASSIGNED,
                                OrderStatus.ARRIVED_AT_PICKUP,
                                OrderStatus.PICKED_UP,
                                OrderStatus.IN_TRANSIT
                            )
                    }
                    val fairCount = AutomatedFairRiderAssignmentEngine.getRiderFairAssignedCount(rider.riderId) +
                        rider.completedOrdersToday
                    val isSelected = selectedRiderId == rider.riderId
                    val isDebtBlocked = rider.commissionDebtInr > 150.0

                    val badgeColor = when {
                        !rider.isOnline -> Color(0xFF64748B)
                        isDebtBlocked -> Color(0xFFDC2626)
                        activeOrderForRider != null -> FairOrange
                        else -> FairEmerald
                    }

                    val statusLabel = when {
                        !rider.isOnline -> "OFFLINE"
                        isDebtBlocked -> "DEBT > ₹150"
                        activeOrderForRider != null -> "BUSY (#${activeOrderForRider.orderId})"
                        else -> "READY (NEXT)"
                    }

                    Surface(
                        color = if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) FairOrange else badgeColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .let { mod ->
                                if (onSelectRiderProfile != null) {
                                    mod.clickable { onSelectRiderProfile(rider) }
                                } else {
                                    mod
                                }
                            }
                            .testTag("fair_queue_rider_chip_${rider.riderId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (rider.vehicleType.name.contains("TRUCK") || rider.vehicleType.name == "AUTO") {
                                        Icons.Default.LocalShipping
                                    } else {
                                        Icons.AutoMirrored.Filled.DirectionsBike
                                    },
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = rider.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FairNavy
                                )
                            }
                            Text(
                                text = "${rider.vehicleType.title} • Fair Orders: $fairCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = badgeColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "Debt ₹%.0f", rider.commissionDebtInr),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // If any order is currently in SEARCHING_RIDER, show 1-click Auto-Retry button
            if (searchingOrders.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, FairAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⏳ ${searchingOrders.size} Order(s) in SEARCHING_RIDER Queue",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Waiting for an eligible online rider with 0 active orders & debt ≤ ₹150.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                        Button(
                            onClick = {
                                val results = AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                                val assignedCount = results.count { it.assignedRider != null }
                                val msg = if (assignedCount > 0) {
                                    "Auto-assigned $assignedCount searching order(s) to next eligible rider!"
                                } else {
                                    results.firstOrNull()?.message ?: "Still searching for an available eligible rider."
                                }
                                onStatusMessage?.invoke(msg, assignedCount == 0)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FairAmber),
                            modifier = Modifier.testTag("retry_searching_orders_fair_btn")
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry Auto-Match", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Show active sequential offer banner if any order is in sequential offer mode
            activeOffers.values.firstOrNull()?.let { offer ->
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📡 Offer #${offer.attemptNumber} → ${offer.offeredRiderName} (${AutomatedFairRiderAssignmentEngine.offerTimeoutSeconds}s Timeout)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = offer.statusNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    AutomatedFairRiderAssignmentEngine.claimOrderAtomicallyByRider(
                                        repository = repository,
                                        orderId = offer.orderId,
                                        riderId = offer.offeredRiderId
                                    ).onSuccess {
                                        onStatusMessage?.invoke("Order #${offer.orderId} accepted by ${offer.offeredRiderName}!", false)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FairEmerald),
                                modifier = Modifier.testTag("accept_sequential_offer_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val nextRes = AutomatedFairRiderAssignmentEngine.handleOfferTimeoutAndAdvanceNext(
                                        repository = repository,
                                        orderId = offer.orderId,
                                        timedOutRiderId = offer.offeredRiderId,
                                        offerOnlyWaitForRiderConsent = false
                                    )
                                    onStatusMessage?.invoke(nextRes.message, nextRes.assignedRider == null)
                                },
                                modifier = Modifier.testTag("timeout_skip_next_rider_btn")
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Next Rider", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Show latest automatic dispatch decision soCustomer/Rider/Admin can verify zero partiality
            auditLog.firstOrNull()?.let { latest ->
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Latest Auto-Dispatch (${latest.timestampLabel}): ${latest.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        modifier = Modifier.weight(1f)
                    )
                    if (roleView == "RIDER") {
                        // Allow Rider to pass an assigned-before-pickup order to the next eligible rider fairly
                        val rejectableOrder = orders.firstOrNull {
                            it.orderStatus == OrderStatus.RIDER_ASSIGNED &&
                                (selectedRiderId == null || it.assignedRiderId == selectedRiderId)
                        }
                        if (rejectableOrder != null && !rejectableOrder.assignedRiderId.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val reassignRes = AutomatedFairRiderAssignmentEngine.rejectOrderAndAutoReassignNext(
                                        repository = repository,
                                        orderId = rejectableOrder.orderId,
                                        rejectingRiderId = rejectableOrder.assignedRiderId!!,
                                        reason = "Passed to next fair-queue rider"
                                    )
                                    onStatusMessage?.invoke(reassignRes.message, reassignRes.assignedRider == null)
                                },
                                modifier = Modifier.testTag("pass_order_to_next_fair_rider_btn")
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pass to Next Rider", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
