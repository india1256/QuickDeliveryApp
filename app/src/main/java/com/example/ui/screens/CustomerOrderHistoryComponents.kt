package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.ui.components.PostDeliveryRiderFeedbackSection
import com.example.ui.components.RiderFeedbackManager
import java.util.Locale

private val HistoryTeal = Color(0xFF008080)
private val HistoryOrange = Color(0xFFFF6B35)
private val HistoryNavy = Color(0xFF0D1B2A)
private val HistorySoftBg = Color(0xFFF4F7F9)
private val DeliveredGreen = Color(0xFF059669)
private val ActiveBlue = Color(0xFF0284C7)
private val SearchingAmber = Color(0xFFD97706)
private val CancelledRed = Color(0xFFDC2626)

enum class OrderHistoryFilter(val label: String, val tagSuffix: String) {
    ALL("All Orders", "all"),
    PAST_DELIVERED("Past / Delivered", "past"),
    ACTIVE("Active", "active")
}

/**
 * Compact Order History entry card shown on the Customer main screen for 1-tap access to
 * past and active deliveries with status, date, and final price.
 */
@Composable
fun CustomerOrderHistorySummaryBanner(
    orders: List<CustomerOrder>,
    onOpenOrderHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveredCount = remember(orders) {
        orders.count { it.orderStatus == OrderStatus.DELIVERED }
    }
    val activeCount = remember(orders) {
        orders.count { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
    }
    val latestOrder = orders.firstOrNull()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HistoryTeal.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenOrderHistory() }
            .testTag("customer_order_history_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = HistoryTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Order History",
                        tint = HistoryTeal,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Order History (${orders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = HistoryNavy
                    )
                    Text(
                        text = if (latestOrder != null) {
                            "Latest: #${latestOrder.orderId} • ${latestOrder.orderStatus.displayTitle} • ${latestOrder.totalAmount}"
                        } else {
                            "$deliveredCount Delivered • $activeCount Active"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onOpenOrderHistory,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.2.dp, HistoryOrange),
                modifier = Modifier.testTag("customer_view_order_history_btn")
            ) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = HistoryOrange
                )
            }
        }
    }
}

/**
 * Full-Screen Customer Order History Screen displaying past and active deliveries,
 * including order status, date/timestamp, final price, pickup/drop route, and reorder/track actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrderHistoryScreen(
    customerName: String,
    orders: List<CustomerOrder>,
    onBack: () -> Unit,
    onSelectOrderToTrack: (CustomerOrder) -> Unit,
    onReorderRoute: (CustomerOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    var selectedFilter by remember { mutableStateOf(OrderHistoryFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val tipsByOrder by RiderFeedbackManager.tipsByOrder.collectAsState()
    val feedbackByOrder by RiderFeedbackManager.feedbackByOrder.collectAsState()

    val filteredOrders = remember(orders, selectedFilter, searchQuery) {
        orders.filter { order ->
            val matchesFilter = when (selectedFilter) {
                OrderHistoryFilter.ALL -> true
                OrderHistoryFilter.PAST_DELIVERED ->
                    order.orderStatus == OrderStatus.DELIVERED || order.orderStatus == OrderStatus.CANCELLED
                OrderHistoryFilter.ACTIVE ->
                    order.orderStatus != OrderStatus.DELIVERED && order.orderStatus != OrderStatus.CANCELLED
            }
            val query = searchQuery.trim().lowercase(Locale.US)
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                order.orderId.lowercase(Locale.US).contains(query) ||
                    order.pickupAddress.fullAddress.lowercase(Locale.US).contains(query) ||
                    order.dropAddress.fullAddress.lowercase(Locale.US).contains(query) ||
                    order.orderStatus.displayTitle.lowercase(Locale.US).contains(query) ||
                    (order.assignedRiderName?.lowercase(Locale.US)?.contains(query) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    val deliveredOrdersCount = remember(orders) {
        orders.count { it.orderStatus == OrderStatus.DELIVERED }
    }
    val activeOrdersCount = remember(orders) {
        orders.count { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
    }
    val totalSpendInr = remember(orders, tipsByOrder) {
        orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { order ->
            val basePrice = order.totalAmount
                .replace("₹", "")
                .replace(",", "")
                .trim()
                .toDoubleOrNull() ?: 0.0
            val tip = (tipsByOrder[order.orderId] ?: 0).toDouble()
            basePrice + tip
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_order_history_screen"),
        containerColor = HistorySoftBg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("customer_order_history_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Booking",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Order History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$customerName • ${orders.size} Deliveries",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HistoryTeal)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Summary Metrics Card
            item(key = "order_history_summary_metrics") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_order_history_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HistoryStatColumn(
                            label = "Total Orders",
                            value = "${orders.size}",
                            valueColor = HistoryNavy
                        )
                        HistoryStatColumn(
                            label = "Completed",
                            value = "$deliveredOrdersCount",
                            valueColor = DeliveredGreen
                        )
                        HistoryStatColumn(
                            label = "Active",
                            value = "$activeOrdersCount",
                            valueColor = ActiveBlue
                        )
                        HistoryStatColumn(
                            label = "Total Value",
                            value = String.format(Locale.US, "₹%.0f", totalSpendInr),
                            valueColor = HistoryTeal
                        )
                    }
                }
            }

            // 2. Filter Chips & Search Input
            item(key = "order_history_filters_and_search") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderHistoryFilter.entries.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HistoryTeal,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("order_history_filter_${filter.tagSuffix}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by Order ID, address, or status") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_history_search_input")
                    )
                }
            }

            // 3. Empty State or Order List
            if (filteredOrders.isEmpty()) {
                item(key = "order_history_empty_state") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_order_history_empty_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No matching deliveries found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HistoryNavy
                            )
                            Text(
                                text = "Your past and active deliveries will appear here with order status, date, and final price.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredOrders,
                    key = { it.orderId }
                ) { order ->
                    CustomerOrderHistoryItemCard(
                        order = order,
                        tipAmountInr = tipsByOrder[order.orderId] ?: 0,
                        starRating = feedbackByOrder[order.orderId]?.starRating,
                        onTrackOrViewOrder = { onSelectOrderToTrack(order) },
                        onReorderRoute = { onReorderRoute(order) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryStatColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun CustomerOrderHistoryItemCard(
    order: CustomerOrder,
    tipAmountInr: Int,
    starRating: Int?,
    onTrackOrViewOrder: () -> Unit,
    onReorderRoute: () -> Unit
) {
    val statusColor = when (order.orderStatus) {
        OrderStatus.DELIVERED -> DeliveredGreen
        OrderStatus.IN_TRANSIT, OrderStatus.PICKED_UP, OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> ActiveBlue
        OrderStatus.SEARCHING_RIDER -> SearchingAmber
        OrderStatus.CANCELLED -> CancelledRed
    }

    val baseAmount = order.totalAmount
        .replace("₹", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull() ?: 0.0
    val finalPriceFormatted = if (tipAmountInr > 0) {
        String.format(Locale.US, "₹%.0f (incl. ₹%d tip)", baseAmount + tipAmountInr, tipAmountInr)
    } else {
        order.totalAmount
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_history_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Order ID, Date, and Order Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = HistoryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Order #${order.orderId} • ${order.vehicleType.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = HistoryNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Date: ${order.createdAt}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569),
                            modifier = Modifier.testTag("customer_history_order_date_${order.orderId}")
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("customer_history_order_status_${order.orderId}")
                ) {
                    Text(
                        text = order.orderStatus.displayTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Price & Payment Method Banner
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Final Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = finalPriceFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = HistoryNavy,
                            modifier = Modifier.testTag("customer_history_order_price_${order.orderId}")
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = order.paymentMethod.displayTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HistoryTeal
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f km • %s", order.distanceKm, order.packageDetails.packageType.title),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Pickup & Drop Route Details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = HistoryTeal,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pickup: ${order.pickupAddress.fullAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HistoryNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DeliveredGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Drop: ${order.dropAddress.fullAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HistoryNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Assigned Rider & Rating Info (if available)
            if (!order.assignedRiderName.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rider: ${order.assignedRiderName} (${order.assignedRiderVehicle ?: "Verified Partner"})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    if (starRating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "$starRating/5",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            if (order.orderStatus == OrderStatus.DELIVERED) {
                PostDeliveryRiderFeedbackSection(
                    orderId = order.orderId,
                    riderId = order.assignedRiderId ?: "RIDER-101",
                    riderName = order.assignedRiderName ?: "Vikram Singh",
                    customerName = order.customerName
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Action Buttons: View/Track Order & Reorder Same Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReorderRoute,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("customer_history_reorder_btn_${order.orderId}")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Book Again", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onTrackOrViewOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = HistoryTeal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("customer_history_track_btn_${order.orderId}")
                ) {
                    Text(
                        text = if (order.orderStatus == OrderStatus.DELIVERED) "View Receipt" else "Live Track",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class PastCompletedDeliverySummaryItem(
    val orderId: String,
    val dateFormatted: String,
    val costInr: Int,
    val vehicleLabel: String,
    val pickupSummary: String,
    val dropSummary: String,
    val riderName: String,
    val riderId: String,
    val distanceKm: Double
)

/**
 * Dedicated 'Delivery History' tab content in the Customer App showing past completed deliveries
 * with a summary of total cost (₹) and date, plus per-delivery cards and rider rating/feedback.
 */
@Composable
fun CustomerDeliveryHistoryTabSection(
    customerName: String,
    orders: List<CustomerOrder>,
    onSelectOrderToTrack: (CustomerOrder) -> Unit,
    onReorderRoute: (CustomerOrder) -> Unit,
    onOpenFullHistoryScreen: () -> Unit,
    onBookNewDelivery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tipsByOrder by RiderFeedbackManager.tipsByOrder.collectAsState()
    val feedbackByOrder by RiderFeedbackManager.feedbackByOrder.collectAsState()
    var showOnlyCompleted by remember { mutableStateOf(true) }

    val completedLiveOrders = remember(orders) {
        orders.filter { it.orderStatus == OrderStatus.DELIVERED }
    }

    val archivedCompletedDeliveries = remember {
        emptyList<PastCompletedDeliverySummaryItem>()
    }

    val liveCompletedCostInr = remember(completedLiveOrders, tipsByOrder) {
        completedLiveOrders.sumOf { order ->
            val base = order.totalAmount.replace("₹", "").replace(",", "").trim().toDoubleOrNull()?.toInt() ?: 0
            base + (tipsByOrder[order.orderId] ?: 0)
        }
    }
    val archivedCompletedCostInr = remember(archivedCompletedDeliveries, tipsByOrder) {
        archivedCompletedDeliveries.sumOf { item ->
            item.costInr + (tipsByOrder[item.orderId] ?: 0)
        }
    }
    val totalCompletedCount = completedLiveOrders.size + archivedCompletedDeliveries.size
    val totalCompletedCostInr = liveCompletedCostInr + archivedCompletedCostInr
    val latestCompletedDate = completedLiveOrders.firstOrNull()?.createdAt
        ?: archivedCompletedDeliveries.firstOrNull()?.dateFormatted
        ?: "No deliveries yet"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_delivery_history_tab_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card of Past Completed Deliveries (Cost & Date Summary)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, HistoryTeal.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_delivery_history_summary_card")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = HistoryTeal.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Delivery History",
                                tint = HistoryTeal,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Delivery History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = HistoryNavy
                            )
                            Text(
                                text = "Past Completed Deliveries • Cost & Date Summary",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenFullHistoryScreen,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HistoryTeal),
                        modifier = Modifier.testTag("customer_history_tab_full_screen_btn")
                    ) {
                        Text(
                            text = "Full View",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HistoryTeal
                        )
                    }
                }

                // Cost & Date Summary Strip
                Surface(
                    color = Color(0xFFF0FDFA),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HistoryTeal.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HistoryStatColumn(
                            label = "Completed",
                            value = "$totalCompletedCount Deliveries",
                            valueColor = DeliveredGreen
                        )
                        HistoryStatColumn(
                            label = "Total Cost Spent",
                            value = "₹$totalCompletedCostInr",
                            valueColor = HistoryOrange
                        )
                        HistoryStatColumn(
                            label = "Latest Date",
                            value = latestCompletedDate.substringBefore("•").trim(),
                            valueColor = HistoryNavy
                        )
                    }
                }

                // Filter Toggle: Past Completed Deliveries vs All Session Orders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showOnlyCompleted,
                        onClick = { showOnlyCompleted = true },
                        label = {
                            Text(
                                text = "Completed Deliveries ($totalCompletedCount)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HistoryTeal,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("history_tab_filter_completed")
                    )
                    FilterChip(
                        selected = !showOnlyCompleted,
                        onClick = { showOnlyCompleted = false },
                        label = {
                            Text(
                                text = "All Orders (${orders.size + archivedCompletedDeliveries.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HistoryTeal,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("history_tab_filter_all")
                    )
                }
            }
        }

        // Live Session Orders (Filtered by Completed or All)
        val visibleLiveOrders = if (showOnlyCompleted) completedLiveOrders else orders
        visibleLiveOrders.forEach { liveOrder ->
            CustomerOrderHistoryItemCard(
                order = liveOrder,
                tipAmountInr = tipsByOrder[liveOrder.orderId] ?: 0,
                starRating = feedbackByOrder[liveOrder.orderId]?.starRating,
                onTrackOrViewOrder = { onSelectOrderToTrack(liveOrder) },
                onReorderRoute = { onReorderRoute(liveOrder) }
            )
        }

        // Past Completed Deliveries List with Cost, Date & Rider Rating/Feedback
        archivedCompletedDeliveries.forEach { pastItem ->
            val tipInr = tipsByOrder[pastItem.orderId] ?: 0
            val savedRating = feedbackByOrder[pastItem.orderId]?.starRating
            val totalItemCost = pastItem.costInr + tipInr

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DeliveredGreen.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("past_completed_delivery_card_${pastItem.orderId}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = HistoryTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Order #${pastItem.orderId} • ${pastItem.vehicleLabel}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HistoryNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Completed Date: ${pastItem.dateFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Surface(
                            color = DeliveredGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Delivered ✓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeliveredGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Cost & Distance Summary Box
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Delivery Cost Paid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = if (tipInr > 0) "₹$totalItemCost (incl. ₹$tipInr tip)" else "₹$totalItemCost",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HistoryNavy
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.1f km • Paid via UPI", pastItem.distanceKm),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HistoryTeal
                                )
                                Text(
                                    text = "Rider: ${pastItem.riderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Route Summary
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = HistoryTeal,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pickup: ${pastItem.pickupSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = HistoryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = DeliveredGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Drop: ${pastItem.dropSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = HistoryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (savedRating != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Rider Rating:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$savedRating / 5 ★",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    // Post-Delivery Rider Rating & Feedback System for Completed Order
                    PostDeliveryRiderFeedbackSection(
                        orderId = pastItem.orderId,
                        riderId = pastItem.riderId,
                        riderName = pastItem.riderName,
                        customerName = customerName
                    )
                }
            }
        }

        Button(
            onClick = onBookNewDelivery,
            colors = ButtonDefaults.buttonColors(containerColor = HistoryOrange),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_tab_book_new_delivery_btn")
        ) {
            Text(
                text = "+ Book New Parcel Delivery",
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

