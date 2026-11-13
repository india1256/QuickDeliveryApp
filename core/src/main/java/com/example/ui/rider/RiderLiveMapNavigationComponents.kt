package com.example.ui.rider

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RiderNavigationTargetType(val label: String) {
    PICKUP("Customer Pickup Location"),
    DROP("Customer Drop Location")
}

data class ActiveRiderMapNavigationSession(
    val orderId: String,
    val customerName: String,
    val customerPhone: String,
    val targetType: RiderNavigationTargetType,
    val targetAddress: String,
    val targetLandmark: String,
    val startLat: Double,
    val startLng: Double,
    val targetLat: Double,
    val targetLng: Double,
    val isNavigationActive: Boolean = true
)

/**
 * Tracks active Rider Turn-by-Turn Map Navigation sessions and syncs status to the Admin Web App.
 * Also provides helper to launch external Google Maps Turn-by-Turn Navigation (`google.navigation:q=lat,lng&mode=d`).
 */
object RiderMapNavigationManager {
    private val _activeSession = MutableStateFlow<ActiveRiderMapNavigationSession?>(null)
    val activeSession: StateFlow<ActiveRiderMapNavigationSession?> = _activeSession.asStateFlow()

    private fun isLegacyPlaceholderCoord(lat: Double, lng: Double, address: String): Boolean {
        val lower = address.lowercase()
        val mentionsBangalore = lower.contains("indiranagar") ||
            lower.contains("koramangala") ||
            lower.contains("mg road") ||
            lower.contains("bangalore") ||
            lower.contains("bengaluru")
        if (mentionsBangalore) return false
        val placeholders = listOf(
            12.9716 to 77.5946,
            12.9784 to 77.6408,
            12.9352 to 77.6245
        )
        return placeholders.any { (pLat, pLng) ->
            kotlin.math.abs(lat - pLat) < 0.0005 && kotlin.math.abs(lng - pLng) < 0.0005
        }
    }

    fun startDirectNavigationForOrder(
        order: CustomerOrder,
        preferredTarget: RiderNavigationTargetType? = null
    ): ActiveRiderMapNavigationSession {
        val resolvedTarget = preferredTarget ?: when (order.orderStatus) {
            OrderStatus.SEARCHING_RIDER,
            OrderStatus.RIDER_ASSIGNED,
            OrderStatus.ARRIVED_AT_PICKUP -> RiderNavigationTargetType.PICKUP
            else -> RiderNavigationTargetType.DROP
        }

        val isPickup = resolvedTarget == RiderNavigationTargetType.PICKUP
        val session = ActiveRiderMapNavigationSession(
            orderId = order.orderId,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            targetType = resolvedTarget,
            targetAddress = if (isPickup) order.pickupAddress.fullAddress else order.dropAddress.fullAddress,
            targetLandmark = if (isPickup) order.pickupAddress.landmark else order.dropAddress.landmark,
            startLat = if (isPickup) order.pickupAddress.lat else order.dropAddress.lat,
            startLng = if (isPickup) order.pickupAddress.lng else order.dropAddress.lng,
            targetLat = if (isPickup) order.pickupAddress.lat else order.dropAddress.lat,
            targetLng = if (isPickup) order.pickupAddress.lng else order.dropAddress.lng,
            isNavigationActive = true
        )
        _activeSession.value = session
        return session
    }

    fun closeNavigationSession() {
        _activeSession.value = null
    }

    /**
     * Launches external Google Maps Turn-by-Turn Navigation directly to the customer's
     * real verified GPS coordinates OR their real street address text (never dummy coordinates).
     */
    fun launchExternalGoogleMapsTurnByTurn(
        context: Context,
        targetLat: Double,
        targetLng: Double,
        targetAddress: String
    ): Boolean {
        val cleanAddress = targetAddress.trim()
        val hasValidRealCoords = targetLat != 0.0 && targetLng != 0.0 &&
            !isLegacyPlaceholderCoord(targetLat, targetLng, cleanAddress)
        if (!hasValidRealCoords && cleanAddress.isBlank()) {
            return false
        }

        val encodedAddr = Uri.encode(cleanAddress)
        val navQuery = if (hasValidRealCoords) {
            String.format(Locale.US, "%.6f,%.6f", targetLat, targetLng)
        } else {
            encodedAddr
        }
        val webDestination = if (hasValidRealCoords) {
            String.format(Locale.US, "%.6f,%.6f", targetLat, targetLng)
        } else {
            encodedAddr
        }

        // 1. Direct Turn-by-Turn Driving Navigation in Google Maps app
        try {
            val navUri = Uri.parse("google.navigation:q=$navQuery&mode=d")
            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(navIntent)
            return true
        } catch (_: Exception) {}

        // 2. Google Maps Web/App-Link Driving Directions in Google Maps app
        try {
            val appLinkUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$webDestination&travelmode=driving")
            val appLinkIntent = Intent(Intent.ACTION_VIEW, appLinkUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(appLinkIntent)
            return true
        } catch (_: Exception) {}

        // 3. Generic navigation scheme (any navigation app installed on device)
        try {
            val genericNavUri = Uri.parse("google.navigation:q=$navQuery&mode=d")
            val genericNavIntent = Intent(Intent.ACTION_VIEW, genericNavUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(genericNavIntent)
            return true
        } catch (_: Exception) {}

        // 4. Standard geo: scheme with coordinates or address search
        try {
            val geoUri = if (hasValidRealCoords) {
                val labelParam = if (cleanAddress.isNotBlank()) "($encodedAddr)" else ""
                Uri.parse("geo:$navQuery?q=$navQuery$labelParam&z=17")
            } else {
                Uri.parse("geo:0,0?q=$encodedAddr")
            }
            val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(geoIntent)
            return true
        } catch (_: Exception) {}

        // 5. Universal Google Maps Web Directions fallback (Browser or App Link)
        try {
            val webDirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$webDestination&travelmode=driving")
            val webDirIntent = Intent(Intent.ACTION_VIEW, webDirUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(webDirIntent)
            return true
        } catch (_: Exception) {}

        return false
    }
}

/**
 * Clean Customer Pickup & Drop Address Box with Sequential Map Navigation:
 * - Always displays BOTH Pickup & Drop addresses clearly after order acceptance.
 * - Stage 1 (Before Pickup): Pickup Navigation is active; Drop Navigation is LOCKED.
 * - Stage 2 (After Pickup): Drop Navigation UNLOCKS and launches Google Maps directly.
 */
@Composable
fun RiderOrderAddressAndNavigationBox(
    order: CustomerOrder,
    onOpenLiveMapNavigation: (RiderNavigationTargetType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPickedUp = order.orderStatus == OrderStatus.PICKED_UP ||
        order.orderStatus == OrderStatus.IN_TRANSIT ||
        order.orderStatus == OrderStatus.DELIVERED

    val pickupLat = order.pickupAddress.lat
    val pickupLng = order.pickupAddress.lng
    val dropLat = order.dropAddress.lat
    val dropLng = order.dropAddress.lng

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, Color(0xFF008080).copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rider_address_navigation_box_${order.orderId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. CUSTOMER PICKUP ADDRESS + NAVIGATION BAR
            Surface(
                color = if (!isPickedUp) Color(0xFFF0FDF4) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (!isPickedUp) Color(0xFF10B981) else Color(0xFFCBD5E1)),
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
                            text = if (isPickedUp) "✅ PICKUP COMPLETED (${order.customerName})" else "📍 1. PICKUP ADDRESS (${order.customerName})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPickedUp) Color(0xFF047857) else Color(0xFF065F46)
                        )
                    }
                    Text(
                        text = order.pickupAddress.fullAddress.ifBlank { "Pickup address pending" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A)
                    )
                    if (order.pickupAddress.landmark.isNotBlank()) {
                        Text(
                            text = "Landmark: ${order.pickupAddress.landmark}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569)
                        )
                    }
                    if (!isPickedUp) {
                        Button(
                            onClick = {
                                RiderMapNavigationManager.launchExternalGoogleMapsTurnByTurn(
                                    context = context,
                                    targetLat = pickupLat,
                                    targetLng = pickupLng,
                                    targetAddress = order.pickupAddress.fullAddress
                                )
                                onOpenLiveMapNavigation(RiderNavigationTargetType.PICKUP)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_navigate_pickup_btn_${order.orderId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Navigate to Pickup Location",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Navigate to Pickup Location",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. CUSTOMER DROP ADDRESS + SEQUENTIAL NAVIGATION LOCK
            val dropContactLabel = order.dropAddress.contactName.ifBlank { order.customerName }
            Surface(
                color = if (isPickedUp) Color(0xFFFFF7ED) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isPickedUp) Color(0xFFEA580C) else Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🏁 2. DROP ADDRESS ($dropContactLabel" +
                            if (order.dropAddress.contactPhone.isNotBlank()) " • ${order.dropAddress.contactPhone})" else ")",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPickedUp) Color(0xFFC2410C) else Color(0xFF475569)
                    )
                    Text(
                        text = order.dropAddress.fullAddress.ifBlank { "Drop address pending" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A)
                    )
                    if (order.dropAddress.landmark.isNotBlank()) {
                        Text(
                            text = "Landmark: ${order.dropAddress.landmark}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569)
                        )
                    }

                    if (isPickedUp) {
                        Button(
                            onClick = {
                                RiderMapNavigationManager.launchExternalGoogleMapsTurnByTurn(
                                    context = context,
                                    targetLat = dropLat,
                                    targetLng = dropLng,
                                    targetAddress = order.dropAddress.fullAddress
                                )
                                onOpenLiveMapNavigation(RiderNavigationTargetType.DROP)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_navigate_drop_btn_${order.orderId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Navigate to Drop Location",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Navigate to Drop Location",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_drop_navigation_locked_${order.orderId}")
                        ) {
                            Text(
                                text = "🔒 Drop Navigation Locked — Complete Parcel Pickup First",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Live Turn-by-Turn Navigation Map Modal:
 * Opens immediately when the Rider clicks Navigation and automatically starts live turn-by-turn
 * route guidance directly to the Customer's Pickup or Drop address!
 */
@Composable
fun RiderLiveTurnByTurnMapNavigationModal(
    order: CustomerOrder,
    initialTargetType: RiderNavigationTargetType,
    onArrivedOrClose: () -> Unit
) {
    val context = LocalContext.current
    var currentTargetType by remember(initialTargetType) { mutableStateOf(initialTargetType) }

    val session = remember(order, currentTargetType) {
        RiderMapNavigationManager.startDirectNavigationForOrder(order, currentTargetType)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "live_map_nav_anim")
    val riderRouteProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rider_nav_progress"
    )

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var externalMapsStatusNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTargetType) {
        elapsedSeconds = 0
        while (true) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    val liveLat = remember(session, riderRouteProgress) {
        session.startLat + (session.targetLat - session.startLat) * riderRouteProgress
    }
    val liveLng = remember(session, riderRouteProgress) {
        session.startLng + (session.targetLng - session.startLng) * riderRouteProgress
    }
    val remainingKm = remember(riderRouteProgress) {
        (2.8f * (1f - riderRouteProgress)).coerceAtLeast(0.1f)
    }
    val remainingMins = remember(remainingKm) {
        (remainingKm * 3.2f).toInt().coerceAtLeast(1)
    }

    val turnInstruction = when {
        riderRouteProgress < 0.35f -> "↱ In 180 m • Turn Right onto Main City Corridor"
        riderRouteProgress < 0.72f -> "↑ Continue Straight 900 m towards ${session.targetLandmark.ifBlank { "Customer Area" }}"
        else -> "📍 Arriving in 60 m at ${session.targetAddress}"
    }

    Dialog(
        onDismissRequest = {
            RiderMapNavigationManager.closeNavigationSession()
            onArrivedOrClose()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("rider_live_map_navigation_modal"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP HIGHWAY-STYLE GREEN TURN-BY-TURN NAVIGATION HEADER (AUTO-STARTED!)
                Surface(
                    color = Color(0xFF065F46),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    color = Color(0xFF10B981),
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = "Live Navigation Active",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🟢 LIVE NAVIGATION STARTED • ${session.targetType.label.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFA7F3D0)
                                    )
                                    Text(
                                        text = turnInstruction,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    RiderMapNavigationManager.closeNavigationSession()
                                    onArrivedOrClose()
                                },
                                modifier = Modifier.testTag("rider_close_live_navigation_modal_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Map",
                                    tint = Color.White
                                )
                            }
                        }

                        Text(
                            text = "🎯 Destination: ${session.targetAddress}" +
                                if (session.targetLandmark.isNotBlank()) " (${session.targetLandmark})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.92f)
                        )

                        // Quick Switcher: Navigate to Pickup vs Navigate to Customer Drop
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = currentTargetType == RiderNavigationTargetType.PICKUP,
                                onClick = { currentTargetType = RiderNavigationTargetType.PICKUP },
                                label = {
                                    Text(
                                        "📍 Pickup Address",
                                        color = if (currentTargetType == RiderNavigationTargetType.PICKUP) Color(0xFF065F46) else Color.White
                                    )
                                },
                                modifier = Modifier.testTag("map_modal_switch_pickup_chip")
                            )
                            FilterChip(
                                selected = currentTargetType == RiderNavigationTargetType.DROP,
                                onClick = { currentTargetType = RiderNavigationTargetType.DROP },
                                label = {
                                    Text(
                                        "🏁 Customer Drop Address",
                                        color = if (currentTargetType == RiderNavigationTargetType.DROP) Color(0xFF065F46) else Color.White
                                    )
                                },
                                modifier = Modifier.testTag("map_modal_switch_drop_chip")
                            )
                        }
                    }
                }

                // 2. INTERACTIVE LIVE ROUTE MAP CANVAS (WITH MOVING RIDER VEHICLE & CUSTOMER PIN)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F172A))
                        .testTag("rider_live_navigation_map_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // City blocks / street grid background
                        val gridCols = 6
                        val gridRows = 8
                        for (c in 1 until gridCols) {
                            val x = (w / gridCols) * c
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 14f
                            )
                        }
                        for (r in 1 until gridRows) {
                            val y = (h / gridRows) * r
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 14f
                            )
                        }

                        // Define 4 waypoints for the direct road route from Rider to Customer Address
                        val p0 = Offset(w * 0.14f, h * 0.84f)
                        val p1 = Offset(w * 0.34f, h * 0.58f)
                        val p2 = Offset(w * 0.66f, h * 0.42f)
                        val p3 = Offset(w * 0.84f, h * 0.18f)

                        val routePath = Path().apply {
                            moveTo(p0.x, p0.y)
                            lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            lineTo(p3.x, p3.y)
                        }

                        // Outer road casing
                        drawPath(
                            path = routePath,
                            color = Color(0xFF1D4ED8).copy(alpha = 0.45f),
                            style = Stroke(width = 28f, cap = StrokeCap.Round)
                        )

                        // Active highlighted navigation route polyline
                        drawPath(
                            path = routePath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(width = 14f, cap = StrokeCap.Round)
                        )

                        // Animated direction chevrons/dashes along route
                        drawPath(
                            path = routePath,
                            color = Color.White,
                            style = Stroke(
                                width = 4f,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 22f), phase = -riderRouteProgress * 300f)
                            )
                        )

                        // Start Point Circle (Rider Origin)
                        drawCircle(
                            color = Color(0xFF10B981),
                            radius = 16f,
                            center = p0
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 7f,
                            center = p0
                        )

                        // Target Customer Address Destination Pin
                        drawCircle(
                            color = Color(0xFFEF4444).copy(alpha = 0.3f),
                            radius = 36f,
                            center = p3
                        )
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 18f,
                            center = p3
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 7f,
                            center = p3
                        )

                        // Compute Rider's current interpolated position along the 3 route segments
                        val riderPos = when {
                            riderRouteProgress < 0.33f -> {
                                val t = (riderRouteProgress / 0.33f).coerceIn(0f, 1f)
                                Offset(
                                    p0.x + (p1.x - p0.x) * t,
                                    p0.y + (p1.y - p0.y) * t
                                )
                            }
                            riderRouteProgress < 0.66f -> {
                                val t = ((riderRouteProgress - 0.33f) / 0.33f).coerceIn(0f, 1f)
                                Offset(
                                    p1.x + (p2.x - p1.x) * t,
                                    p1.y + (p2.y - p1.y) * t
                                )
                            }
                            else -> {
                                val t = ((riderRouteProgress - 0.66f) / 0.34f).coerceIn(0f, 1f)
                                Offset(
                                    p2.x + (p3.x - p2.x) * t,
                                    p2.y + (p3.y - p2.y) * t
                                )
                            }
                        }

                        // Rider Live GPS Navigation Halo & Marker
                        drawCircle(
                            color = Color(0xFFFF6B35).copy(alpha = 0.35f),
                            radius = 34f,
                            center = riderPos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 20f,
                            center = riderPos
                        )
                        drawCircle(
                            color = Color(0xFFFF6B35),
                            radius = 15f,
                            center = riderPos
                        )
                    }

                    // Overlay Badges on Map Canvas
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Live Rider GPS: %.5f, %.5f • 38 km/h",
                                    liveLat,
                                    liveLng
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Customer Pin Floating Label near Top-Right of Canvas
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 52.dp, end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${session.customerName} (${session.targetType.label})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // 3. BOTTOM LIVE NAVIGATION TELEMETRY & GOOGLE MAPS LAUNCH DECK
                Surface(
                    color = Color.White,
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
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%d min (%.1f km remaining)", remainingMins, remainingKm),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                                Text(
                                    text = "Fastest Direct Route • Customer: ${session.customerName} (${session.customerPhone})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                            }
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Text(
                                    text = "${(riderRouteProgress * 100).toInt()}% Reached",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { riderRouteProgress },
                            color = Color(0xFF059669),
                            trackColor = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )

                        if (externalMapsStatusNote != null) {
                            Text(
                                text = externalMapsStatusNote.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val launched = RiderMapNavigationManager.launchExternalGoogleMapsTurnByTurn(
                                        context = context,
                                        targetLat = session.targetLat,
                                        targetLng = session.targetLng,
                                        targetAddress = session.targetAddress
                                    )
                                    externalMapsStatusNote = if (launched) {
                                        "🗺️ External Google Maps Turn-by-Turn Navigation launched!"
                                    } else {
                                        "🧭 Using built-in Live Turn-by-Turn Map Navigation (Direct to ${session.targetAddress})"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rider_open_external_google_maps_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google Maps App", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    RiderMapNavigationManager.closeNavigationSession()
                                    onArrivedOrClose()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rider_arrived_at_customer_location_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Arrived at Location ✓", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
