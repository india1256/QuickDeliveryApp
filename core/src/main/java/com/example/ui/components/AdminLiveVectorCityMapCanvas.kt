package com.example.ui.components

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rich Vector City Cartography & Live Fleet Radar Map used across Admin Control Centers.
 * Renders stylized parks, waterway curve, urban building blocks, dual-lane expressways,
 * central roundabout, rotating live radar sweep, and real active order Bezier routes.
 * Strictly renders ZERO fake/dummy routes when there are no active customer orders.
 */
@Composable
fun AdminLiveVectorCityMapCanvas(
    activeOrders: List<CustomerOrder>,
    onlineRidersCount: Int,
    sweepProgress: Float,
    topStartLabel: String,
    bottomEndLabel: String? = null,
    mapHeight: Dp = 185.dp,
    modifier: Modifier = Modifier
) {
    val labelTextPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 22f
            isFakeBoldText = true
            textAlign = AndroidPaint.Align.CENTER
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF041A21), Color(0xFF072833), Color(0xFF03141C))
                )
            )
            .testTag("admin_live_fleet_radar_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.50f
            val cy = h * 0.52f

            // 1. Stylized Green Urban Park Zones
            val parkColor = Color(0xFF0D4437)
            val parkStroke = Color(0xFF1A6B57).copy(alpha = 0.65f)
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(w * 0.05f, h * 0.14f),
                size = Size(w * 0.16f, h * 0.28f),
                cornerRadius = CornerRadius(18f, 18f)
            )
            drawRoundRect(
                color = parkStroke,
                topLeft = Offset(w * 0.05f, h * 0.14f),
                size = Size(w * 0.16f, h * 0.28f),
                cornerRadius = CornerRadius(18f, 18f),
                style = Stroke(width = 1.4f)
            )
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(w * 0.76f, h * 0.10f),
                size = Size(w * 0.18f, h * 0.24f),
                cornerRadius = CornerRadius(20f, 20f)
            )
            drawRoundRect(
                color = parkColor.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.34f, h * 0.72f),
                size = Size(w * 0.17f, h * 0.20f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // 2. Flowing Stylized City River / Waterway Curve
            val riverPath = Path().apply {
                moveTo(-w * 0.05f, h * 0.84f)
                cubicTo(w * 0.24f, h * 0.62f, w * 0.56f, h * 0.96f, w * 1.05f, h * 0.56f)
            }
            drawPath(
                path = riverPath,
                color = Color(0xFF0B4F78).copy(alpha = 0.78f),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )
            drawPath(
                path = riverPath,
                color = Color(0xFF38BDF8).copy(alpha = 0.28f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            // 3. Subtle Urban City Building Blocks
            val blockColor = Color(0xFF0E313D)
            val blockBorder = Color(0xFF1D4F5E).copy(alpha = 0.5f)
            val blocks = listOf(
                Triple(0.25f, 0.18f, Pair(0.08f, 0.12f)),
                Triple(0.36f, 0.16f, Pair(0.09f, 0.10f)),
                Triple(0.56f, 0.18f, Pair(0.11f, 0.11f)),
                Triple(0.10f, 0.52f, Pair(0.10f, 0.13f)),
                Triple(0.26f, 0.44f, Pair(0.08f, 0.11f)),
                Triple(0.62f, 0.44f, Pair(0.10f, 0.12f)),
                Triple(0.79f, 0.42f, Pair(0.12f, 0.12f)),
                Triple(0.56f, 0.74f, Pair(0.10f, 0.11f))
            )
            blocks.forEach { (bx, by, dims) ->
                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(w * bx, h * by),
                    size = Size(w * dims.first, h * dims.second),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = blockBorder,
                    topLeft = Offset(w * bx, h * by),
                    size = Size(w * dims.first, h * dims.second),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1f)
                )
            }

            // 4. Secondary City Street Grid
            val streetColor = Color(0xFF163D49).copy(alpha = 0.72f)
            for (i in 1..7) {
                val x = w * (i / 8f)
                drawLine(streetColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.2f)
            }
            for (j in 1..4) {
                val y = h * (j / 5f)
                drawLine(streetColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
            }

            // 5. Dual-Lane Arterial Expressways & Ring Road
            val mainHwyHorizontal = Path().apply {
                moveTo(0f, h * 0.36f)
                cubicTo(w * 0.30f, h * 0.31f, w * 0.65f, h * 0.44f, w, h * 0.34f)
            }
            val mainHwyDiagonal = Path().apply {
                moveTo(w * 0.20f, 0f)
                cubicTo(w * 0.38f, h * 0.38f, w * 0.58f, h * 0.62f, w * 0.74f, h)
            }
            listOf(mainHwyHorizontal, mainHwyDiagonal).forEach { hwy ->
                drawPath(hwy, color = Color(0xFF234D5B), style = Stroke(width = 7.5f, cap = StrokeCap.Round))
                drawPath(hwy, color = Color(0xFF102A34), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
                drawPath(
                    hwy,
                    color = Color(0xFFF59E0B).copy(alpha = 0.32f),
                    style = Stroke(
                        width = 1.3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                    )
                )
            }

            // 6. Central City Roundabout & Concentric Radar Range Rings
            val outerRing = minOf(w, h) * 0.43f
            val midRing = minOf(w, h) * 0.27f
            val innerRing = minOf(w, h) * 0.12f
            drawCircle(
                color = Color(0xFF14B8A6).copy(alpha = 0.24f),
                radius = outerRing,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = Color(0xFF14B8A6).copy(alpha = 0.18f),
                radius = midRing,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
            )
            drawCircle(
                color = Color(0xFF2DD4BF).copy(alpha = 0.22f),
                radius = innerRing,
                center = Offset(cx, cy),
                style = Stroke(width = 1.4f)
            )

            // 7. Smooth 360-degree Rotating Live Radar Sweep Cone + Glowing Needle
            val sweepStartDeg = (sweepProgress * 360f) % 360f
            val sweepArcDeg = 54f
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = 0.30f),
                        Color(0xFF14B8A6).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = outerRing
                ),
                startAngle = sweepStartDeg - sweepArcDeg,
                sweepAngle = sweepArcDeg,
                useCenter = true,
                topLeft = Offset(cx - outerRing, cy - outerRing),
                size = Size(outerRing * 2f, outerRing * 2f)
            )
            val rad = (sweepStartDeg * PI / 180.0)
            val beamEnd = Offset(
                x = cx + (outerRing * cos(rad)).toFloat(),
                y = cy + (outerRing * sin(rad)).toFloat()
            )
            drawLine(
                color = Color(0xFF5EEAD4).copy(alpha = 0.72f),
                start = Offset(cx, cy),
                end = beamEnd,
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
            drawCircle(color = Color(0xFF5EEAD4), radius = 3.5f, center = Offset(cx, cy))

            // 8. Real Online Idle Riders (subtle GPS pings on the map when onlineRidersCount > 0)
            val idleDots = onlineRidersCount.coerceIn(0, 6)
            for (rIdx in 0 until idleDots) {
                val angle = (rIdx * 60.0 + 25.0) * PI / 180.0
                val distFactor = if (rIdx % 2 == 0) 0.32f else 0.21f
                val rx = cx + (minOf(w, h) * distFactor * cos(angle)).toFloat()
                val ry = cy + (minOf(w, h) * distFactor * sin(angle)).toFloat()
                drawCircle(color = Color(0xFF10B981).copy(alpha = 0.25f), radius = 10f, center = Offset(rx, ry))
                drawCircle(color = Color(0xFF34D399), radius = 4f, center = Offset(rx, ry))
            }

            // 9. REAL Active Order Curved Bezier Delivery Routes + Teardrop Pins + Pulsing Rider Puck + #OrderId Pill
            // Strictly draws ONLY real active orders (ZERO dummy routes when activeOrders is empty)
            val radarOrders = activeOrders.take(12)
            radarOrders.forEachIndexed { idx, order ->
                val hashSeed = (order.orderId.hashCode() and 0x7FFFFFFF) % 100
                val rowIdx = idx / 3
                val colIdx = idx % 3
                val startX = w * (0.12f + colIdx * 0.14f + (hashSeed % 5) * 0.012f).coerceIn(0.09f, 0.46f)
                val startY = h * (0.24f + ((idx + rowIdx) % 3) * 0.24f).coerceIn(0.20f, 0.80f)
                val endX = w * (0.88f - colIdx * 0.12f - (hashSeed % 4) * 0.01f).coerceIn(0.54f, 0.91f)
                val endY = h * (0.76f - ((idx + rowIdx) % 3) * 0.24f).coerceIn(0.20f, 0.82f)
                val ctrlX = (startX + endX) * 0.5f
                val ctrlY = ((startY + endY) * 0.5f) + if (idx % 2 == 0) -h * 0.20f else h * 0.20f

                val routePath = Path().apply {
                    moveTo(startX, startY)
                    quadraticTo(ctrlX, ctrlY, endX, endY)
                }

                // Glowing route halo
                drawPath(
                    path = routePath,
                    color = Color(0xFF14B8A6).copy(alpha = 0.24f),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
                // Crisp dashed route line
                drawPath(
                    path = routePath,
                    color = Color(0xFF2DD4BF),
                    style = Stroke(
                        width = 3.0f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 7f), 0f)
                    )
                )

                // Custom Teardrop Pickup Pin (Teal)
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(startX - 7f, startY + 2f),
                    size = Size(14f, 5f)
                )
                val pickupPinPath = Path().apply {
                    moveTo(startX, startY + 4f)
                    lineTo(startX - 7f, startY - 7f)
                    lineTo(startX + 7f, startY - 7f)
                    close()
                }
                drawPath(pickupPinPath, color = Color(0xFF14B8A6))
                drawCircle(color = Color(0xFF14B8A6), radius = 8.5f, center = Offset(startX, startY - 10f))
                drawCircle(color = Color.White, radius = 3.5f, center = Offset(startX, startY - 10f))

                // Custom Teardrop Drop Pin (Orange)
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(endX - 7f, endY + 2f),
                    size = Size(14f, 5f)
                )
                val dropPinPath = Path().apply {
                    moveTo(endX, endY + 4f)
                    lineTo(endX - 7f, endY - 7f)
                    lineTo(endX + 7f, endY - 7f)
                    close()
                }
                drawPath(dropPinPath, color = Color(0xFFF97316))
                drawCircle(color = Color(0xFFF97316), radius = 8.5f, center = Offset(endX, endY - 10f))
                drawCircle(color = Color.White, radius = 3.5f, center = Offset(endX, endY - 10f))

                // Exact quadratic Bezier point for live moving Rider vehicle puck
                val t = ((sweepProgress + idx * 0.21f) % 1f)
                val oneMinusT = 1f - t
                val rx = oneMinusT * oneMinusT * startX + 2f * oneMinusT * t * ctrlX + t * t * endX
                val ry = oneMinusT * oneMinusT * startY + 2f * oneMinusT * t * ctrlY + t * t * endY

                val pulseRadius = 15f + (sin(t * PI * 6.0).toFloat() * 4f)
                drawCircle(color = Color(0xFF10B981).copy(alpha = 0.32f), radius = pulseRadius, center = Offset(rx, ry))
                drawCircle(color = Color.White, radius = 8f, center = Offset(rx, ry))
                drawCircle(color = Color(0xFF059669), radius = 5f, center = Offset(rx, ry))
                drawCircle(color = Color(0xFFFEF08A), radius = 2f, center = Offset(rx, ry))

                // Compact live Order ID tag directly above moving rider marker
                val shortTag = "#${order.orderId.takeLast(6)}"
                val pillW = (shortTag.length * 13f + 18f).coerceAtLeast(66f)
                val pillLeft = (rx - pillW / 2f).coerceIn(6f, w - pillW - 6f)
                val pillTop = (ry - 34f).coerceIn(28f, h - 24f)
                drawRoundRect(
                    color = Color(0xFF022C22).copy(alpha = 0.90f),
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillW, 22f),
                    cornerRadius = CornerRadius(11f, 11f)
                )
                drawRoundRect(
                    color = Color(0xFF34D399).copy(alpha = 0.8f),
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillW, 22f),
                    cornerRadius = CornerRadius(11f, 11f),
                    style = Stroke(width = 1.2f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    shortTag,
                    pillLeft + pillW / 2f,
                    pillTop + 16f,
                    labelTextPaint
                )
            }
        }

        // Top-Start Live Telemetry Chip
        Surface(
            color = Color(0xFF062129).copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = topStartLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5EEAD4),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Clean Empty-State Radar Pill (when zero real active orders exist)
        if (activeOrders.isEmpty()) {
            Surface(
                color = Color(0xFF04171E).copy(alpha = 0.86f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "Live GPS Radar Ready • Waiting for customer orders",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCCFBF1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Active Route Legend Pill
            Surface(
                color = Color(0xFF04171E).copy(alpha = 0.86f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "● Pickup  ● Drop  🛵 ${activeOrders.size} Live",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (!bottomEndLabel.isNullOrBlank()) {
            Surface(
                color = Color(0xFF062129).copy(alpha = 0.88f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = bottomEndLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF34D399),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
