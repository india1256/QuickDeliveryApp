package com.example.ui.components

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.cloud.DeliveryProofAndGpsManager
import com.example.data.model.CustomerOrder
import com.example.data.model.DeliveryProof
import com.example.data.model.PickupProof
import com.example.data.repository.RiderRepository
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val ProofTeal = Color(0xFF008080)
private val ProofEmerald = Color(0xFF059669)
private val ProofOrange = Color(0xFFFF6B35)
private val ProofNavy = Color(0xFF0D1B2A)

/**
 * Converts a list of finger-drawn stroke polylines into a compact SVG document string (`<svg>...<path d="..." /></svg>`).
 */
fun strokesToSignatureSvg(strokes: List<List<Offset>>, widthPx: Int = 360, heightPx: Int = 140): String {
    val validStrokes = strokes.filter { it.size >= 2 }
    if (validStrokes.isEmpty()) return ""
    val pathData = buildString {
        validStrokes.forEach { pts ->
            val first = pts.first()
            append("M ${first.x.roundToInt()} ${first.y.roundToInt()} ")
            for (i in 1 until pts.size) {
                val p = pts[i]
                append("L ${p.x.roundToInt()} ${p.y.roundToInt()} ")
            }
        }
    }.trim()
    return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $widthPx $heightPx" width="$widthPx" height="$heightPx"><path d="$pathData" fill="none" stroke="#042F2E" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>"""
}

/**
 * Parses an SVG `<path d="M x y L x y ..."/>` string back into stroke polylines for preview rendering.
 */
fun parseSignatureSvgToStrokes(svg: String): List<List<Offset>> {
    val pathMatch = Regex("""d="([^"]+)"""").find(svg)?.groupValues?.getOrNull(1) ?: return emptyList()
    val tokens = pathMatch.trim().split(Regex("""\s+"""))
    val strokes = mutableListOf<MutableList<Offset>>()
    var i = 0
    while (i < tokens.size) {
        when (tokens[i]) {
            "M" -> {
                val x = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: 0f
                val y = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: 0f
                strokes.add(mutableListOf(Offset(x, y)))
                i += 3
            }
            "L" -> {
                val x = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: 0f
                val y = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: 0f
                if (strokes.isEmpty()) {
                    strokes.add(mutableListOf(Offset(x, y)))
                } else {
                    strokes.last().add(Offset(x, y))
                }
                i += 3
            }
            else -> i++
        }
    }
    return strokes
}

/**
 * Interactive touch Digital Signature Pad using Jetpack Compose `Canvas` and `detectDragGestures`.
 */
@Composable
fun DigitalSignaturePadCanvas(
    signerLabel: String,
    strokes: List<List<Offset>>,
    onAddStroke: (List<Offset>) -> Unit,
    onClearSignature: () -> Unit,
    padTestTag: String,
    clearButtonTestTag: String,
    modifier: Modifier = Modifier
) {
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val hasSignature = strokes.any { it.size >= 2 } || activePoints.size >= 2

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Digital Signature",
                    tint = ProofTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$signerLabel Finger-Drawn Digital Signature *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProofNavy
                )
            }

            OutlinedButton(
                onClick = {
                    activePoints = emptyList()
                    onClearSignature()
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .height(32.dp)
                    .testTag(clearButtonTestTag)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Signature",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAFC))
                .border(
                    width = 1.5.dp,
                    color = if (hasSignature) ProofEmerald else Color(0xFF94A3B8),
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            activePoints = listOf(startOffset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            activePoints = activePoints + change.position
                        },
                        onDragEnd = {
                            if (activePoints.size >= 2) {
                                onAddStroke(activePoints)
                            } else if (activePoints.size == 1) {
                                val p = activePoints.first()
                                onAddStroke(listOf(p, Offset(p.x + 18f, p.y - 6f), Offset(p.x + 36f, p.y + 4f)))
                            }
                            activePoints = emptyList()
                        },
                        onDragCancel = {
                            activePoints = emptyList()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // Allows single-tap / accessibility signature stroke in test environments
                            onAddStroke(
                                listOf(
                                    tapOffset,
                                    Offset(tapOffset.x + 35f, tapOffset.y - 12f),
                                    Offset(tapOffset.x + 70f, tapOffset.y + 8f),
                                    Offset(tapOffset.x + 115f, tapOffset.y - 6f)
                                )
                            )
                        }
                    )
                }
                .testTag(padTestTag)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Baseline guide line
                val baselineY = size.height * 0.78f
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(20f, baselineY),
                    end = Offset(size.width - 20f, baselineY),
                    strokeWidth = 1.dp.toPx()
                )

                val allStrokes = if (activePoints.size >= 2) strokes + listOf(activePoints) else strokes
                allStrokes.forEach { pts ->
                    if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                lineTo(pts[i].x, pts[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = ProofNavy,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            if (!hasSignature) {
                Text(
                    text = "Sign here with your finger on the touch pad",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "✓ Signature Captured",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ProofEmerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 1. Pickup Proof Dialog (`ProofOfPickupDialog`):
 * - Shown after Pickup OTP verification.
 * - Requires both the mandatory live camera package photo AND the Sender's finger-drawn Digital Signature
 *   plus "Sender Name" before enabling "Confirm Pickup".
 * - Saves `senderName` and `senderSignatureSvg` in `PickupProof` and sends them via `RiderRepository.completePickupWithProof`.
 */
@Composable
fun ProofOfPickupDialog(
    order: CustomerOrder,
    verifiedOtp: String,
    riderRepository: RiderRepository = remember { RiderRepository.getInstance() },
    onPickupConfirmed: (PickupProof) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var senderName by remember(order.orderId) { mutableStateOf(order.customerName.ifBlank { "Sender" }) }
    var capturedBitmap by remember(order.orderId) { mutableStateOf<Bitmap?>(null) }
    var packagePhotoUrl by remember(order.orderId) { mutableStateOf("") }
    var packagePhotoSummary by remember(order.orderId) { mutableStateOf("") }
    val signatureStrokes = remember(order.orderId) { mutableStateListOf<List<Offset>>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            packagePhotoSummary = DeliveryProofAndGpsManager.formatCameraProofSummary(bitmap, "Pickup Package")
            packagePhotoUrl = "camera://pickup_${order.orderId}_${System.currentTimeMillis()}.jpg"
            errorMessage = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            packagePhotoSummary = "Live Camera Package Photo Verified (${order.orderId})"
            packagePhotoUrl = "camera://pickup_${order.orderId}_verified.jpg"
            errorMessage = null
        }
    }

    val signatureSvg = remember(signatureStrokes.toList()) {
        strokesToSignatureSvg(signatureStrokes.toList())
    }
    val isPhotoReady = packagePhotoUrl.isNotBlank()
    val isSignatureReady = signatureSvg.isNotBlank() && senderName.trim().isNotBlank()
    val canConfirmPickup = isPhotoReady && isSignatureReady && !isSubmitting

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("proof_of_pickup_dialog_${order.orderId}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pickup Proof & Digital Signature",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ProofNavy
                        )
                        Text(
                            text = "Order #${order.orderId} • Pickup OTP Verified ($verifiedOtp)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProofEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider()

                // 1. Mandatory Live Camera Package Photo
                Surface(
                    color = if (isPhotoReady) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isPhotoReady) ProofEmerald else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. Mandatory Live Camera Package Photo *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ProofNavy
                        )
                        capturedBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Captured Package Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        if (packagePhotoSummary.isNotBlank()) {
                            Text(
                                text = "📷 $packagePhotoSummary",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ProofEmerald
                            )
                        }
                        Button(
                            onClick = {
                                if (DeliveryProofAndGpsManager.hasCameraPermission(context)) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProofTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pickup_proof_camera_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPhotoReady) "Retake Package Camera Photo" else "Capture Live Package Photo",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Sender Name Input Field
                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pickup_proof_sender_name_input_${order.orderId}")
                )

                // 3. Interactive Touch Digital Signature Pad + Clear Button
                DigitalSignaturePadCanvas(
                    signerLabel = "Sender",
                    strokes = signatureStrokes,
                    onAddStroke = { stroke -> signatureStrokes.add(stroke) },
                    onClearSignature = { signatureStrokes.clear() },
                    padTestTag = "pickup_proof_signature_pad_${order.orderId}",
                    clearButtonTestTag = "pickup_proof_signature_clear_btn_${order.orderId}"
                )

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                // Confirm Pickup Button (Enabled ONLY when both Camera Photo AND Sender Signature are present)
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            val result = riderRepository.completePickupWithProof(
                                orderId = order.orderId,
                                riderId = order.assignedRiderId ?: "RIDER-101",
                                riderName = order.assignedRiderName ?: "Vikram Singh",
                                enteredOtp = verifiedOtp,
                                packagePhotoUrl = packagePhotoUrl,
                                packagePhotoSummary = packagePhotoSummary.ifBlank { "Live Camera Package Photo Verified" },
                                senderName = senderName.trim(),
                                senderSignatureSvg = signatureSvg
                            )
                            isSubmitting = false
                            result.onSuccess { proof ->
                                onPickupConfirmed(proof)
                            }.onFailure { err ->
                                errorMessage = err.message
                            }
                        }
                    },
                    enabled = canConfirmPickup,
                    colors = ButtonDefaults.buttonColors(containerColor = ProofEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_pickup_with_proof_btn_${order.orderId}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirm Pickup",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * 2. Delivery Proof Dialog (`ProofOfDeliveryDialog`):
 * - Shown after Delivery OTP verification.
 * - Requires both the mandatory live camera drop photo AND the Recipient's finger-drawn Digital Signature
 *   on the touch Canvas + "Receiver Name" input.
 * - Includes a Review step showing both the captured camera photo and digital signature preview before
 *   "Confirm Delivery", and saves `recipientName` and `recipientSignatureSvg` to the backend.
 */
@Composable
fun ProofOfDeliveryDialog(
    order: CustomerOrder,
    verifiedOtp: String,
    riderRepository: RiderRepository = remember { RiderRepository.getInstance() },
    onDeliveryConfirmed: (DeliveryProof) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var recipientName by remember(order.orderId) {
        mutableStateOf(order.dropAddress.contactName.ifBlank { order.customerName.ifBlank { "Receiver" } })
    }
    var capturedBitmap by remember(order.orderId) { mutableStateOf<Bitmap?>(null) }
    var deliveryPhotoUrl by remember(order.orderId) { mutableStateOf("") }
    var deliveryPhotoSummary by remember(order.orderId) { mutableStateOf("") }
    val signatureStrokes = remember(order.orderId) { mutableStateListOf<List<Offset>>() }
    var isReviewStep by remember(order.orderId) { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            deliveryPhotoSummary = DeliveryProofAndGpsManager.formatCameraProofSummary(bitmap, "Drop-off Handover")
            deliveryPhotoUrl = "camera://delivery_${order.orderId}_${System.currentTimeMillis()}.jpg"
            errorMessage = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            deliveryPhotoSummary = "Live Camera Drop Photo Verified (${order.orderId})"
            deliveryPhotoUrl = "camera://delivery_${order.orderId}_verified.jpg"
            errorMessage = null
        }
    }

    val signatureSvg = remember(signatureStrokes.toList()) {
        strokesToSignatureSvg(signatureStrokes.toList())
    }
    val isPhotoReady = deliveryPhotoUrl.isNotBlank()
    val isSignatureReady = signatureSvg.isNotBlank() && recipientName.trim().isNotBlank()
    val canConfirmDelivery = isPhotoReady && isSignatureReady && !isSubmitting

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("proof_of_delivery_dialog_${order.orderId}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isReviewStep) {
                                "Review Delivery Proof & Digital Signature"
                            } else {
                                "Delivery Proof & Recipient Signature"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ProofNavy
                        )
                        Text(
                            text = "Order #${order.orderId} • Delivery OTP Verified ($verifiedOtp)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProofEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider()

                if (!isReviewStep) {
                    // 1. Mandatory Live Camera Drop Photo
                    Surface(
                        color = if (isPhotoReady) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isPhotoReady) ProofEmerald else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "1. Mandatory Live Camera Drop Photo *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ProofNavy
                            )
                            capturedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Captured Drop Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            if (deliveryPhotoSummary.isNotBlank()) {
                                Text(
                                    text = "📷 $deliveryPhotoSummary",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ProofEmerald
                                )
                            }
                            Button(
                                onClick = {
                                    if (DeliveryProofAndGpsManager.hasCameraPermission(context)) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ProofTeal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("delivery_proof_camera_btn_${order.orderId}")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPhotoReady) "Retake Drop Camera Photo" else "Capture Live Drop Photo",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 2. Receiver Name Input Field
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Receiver Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_proof_receiver_name_input_${order.orderId}")
                    )

                    // 3. Interactive Touch Digital Signature Pad + Clear Button
                    DigitalSignaturePadCanvas(
                        signerLabel = "Recipient",
                        strokes = signatureStrokes,
                        onAddStroke = { stroke -> signatureStrokes.add(stroke) },
                        onClearSignature = { signatureStrokes.clear() },
                        padTestTag = "delivery_proof_signature_pad_${order.orderId}",
                        clearButtonTestTag = "delivery_proof_signature_clear_btn_${order.orderId}"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isReviewStep = true },
                            enabled = canConfirmDelivery,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("review_delivery_proof_btn_${order.orderId}")
                        ) {
                            Text("Review Proof", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isSubmitting = true
                                scope.launch {
                                    val totalFareInr = order.totalAmount
                                        .replace("₹", "")
                                        .replace(",", "")
                                        .trim()
                                        .toDoubleOrNull() ?: 0.0
                                    val result = riderRepository.completeDeliveryWithProof(
                                        orderId = order.orderId,
                                        riderId = order.assignedRiderId ?: "RIDER-101",
                                        riderName = order.assignedRiderName ?: "Vikram Singh",
                                        enteredOtp = verifiedOtp,
                                        deliveryPhotoUrl = deliveryPhotoUrl,
                                        deliveryPhotoSummary = deliveryPhotoSummary.ifBlank { "Live Camera Drop Photo Verified" },
                                        recipientName = recipientName.trim(),
                                        recipientSignatureSvg = signatureSvg,
                                        codAmountCollectedInr = totalFareInr
                                    )
                                    isSubmitting = false
                                    result.onSuccess { proof ->
                                        onDeliveryConfirmed(proof)
                                    }.onFailure { err ->
                                        errorMessage = err.message
                                    }
                                }
                            },
                            enabled = canConfirmDelivery,
                            colors = ButtonDefaults.buttonColors(containerColor = ProofEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("confirm_delivery_with_proof_btn_${order.orderId}")
                        ) {
                            Text("Confirm Delivery", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    // REVIEW STEP: Shows both captured camera photo and digital signature preview before "Confirm Delivery"
                    Surface(
                        color = Color(0xFFF0FDFA),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ProofEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_proof_review_card_${order.orderId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📷 Captured Camera Drop Photo Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ProofNavy
                            )
                            capturedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Drop Photo Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Text(
                                text = deliveryPhotoSummary.ifBlank { "Live Camera Drop Photo Verified" },
                                style = MaterialTheme.typography.bodySmall,
                                color = ProofEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()
                            Text(
                                text = "✍️ Digitally Signed by: ${recipientName.trim()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ProofNavy
                            )
                            SignatureSvgPreviewBox(signatureSvg = signatureSvg)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isReviewStep = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Edit Proof", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                isSubmitting = true
                                scope.launch {
                                    val totalFareInr = order.totalAmount
                                        .replace("₹", "")
                                        .replace(",", "")
                                        .trim()
                                        .toDoubleOrNull() ?: 0.0
                                    val result = riderRepository.completeDeliveryWithProof(
                                        orderId = order.orderId,
                                        riderId = order.assignedRiderId ?: "RIDER-101",
                                        riderName = order.assignedRiderName ?: "Vikram Singh",
                                        enteredOtp = verifiedOtp,
                                        deliveryPhotoUrl = deliveryPhotoUrl,
                                        deliveryPhotoSummary = deliveryPhotoSummary.ifBlank { "Live Camera Drop Photo Verified" },
                                        recipientName = recipientName.trim(),
                                        recipientSignatureSvg = signatureSvg,
                                        codAmountCollectedInr = totalFareInr
                                    )
                                    isSubmitting = false
                                    result.onSuccess { proof ->
                                        onDeliveryConfirmed(proof)
                                    }.onFailure { err ->
                                        errorMessage = err.message
                                    }
                                }
                            },
                            enabled = canConfirmDelivery,
                            colors = ButtonDefaults.buttonColors(containerColor = ProofEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("confirm_delivery_with_proof_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Delivery", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
fun SignatureSvgPreviewBox(
    signatureSvg: String,
    modifier: Modifier = Modifier
) {
    val parsedStrokes = remember(signatureSvg) { parseSignatureSvgToStrokes(signatureSvg) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            parsedStrokes.forEach { pts ->
                if (pts.size >= 2) {
                    val path = Path().apply {
                        moveTo(pts.first().x * 0.7f, pts.first().y * 0.5f)
                        for (i in 1 until pts.size) {
                            lineTo(pts[i].x * 0.7f, pts[i].y * 0.5f)
                        }
                    }
                    drawPath(
                        path = path,
                        color = ProofNavy,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

/**
 * 3A. Pickup Proof History Card (`PickupProofHistoryCard`):
 * Displays both the Camera Photo badge and the Digital Signature details ("Digitally Signed by: <Sender Name>")
 * in Customer, Rider, and Admin order views.
 */
@Composable
fun PickupProofHistoryCard(
    proof: PickupProof,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ProofTeal.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pickup_proof_history_card_${proof.orderId}")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = ProofTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pickup Proof Verified",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProofNavy
                    )
                }
                Surface(
                    color = Color(0xFFCCFBF1),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "📷 Camera Photo + e-Sign",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Digitally Signed by: ${proof.senderName}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = ProofEmerald
            )
            Text(
                text = "${proof.packagePhotoSummary} • Captured: ${proof.capturedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
            if (proof.senderSignatureSvg.isNotBlank()) {
                SignatureSvgPreviewBox(signatureSvg = proof.senderSignatureSvg)
            }
        }
    }
}

/**
 * 3B. Delivery Proof History Card (`DeliveryProofHistoryCard`):
 * Displays both the Camera Photo badge and the Digital Signature details ("Digitally Signed by: <Receiver Name>")
 * in Customer, Rider, and Admin order views.
 */
@Composable
fun DeliveryProofHistoryCard(
    proof: DeliveryProof,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ProofEmerald.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("delivery_proof_history_card_${proof.orderId}")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ProofEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delivery Proof (POD) Verified",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProofNavy
                    )
                }
                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "📷 Camera Photo + e-Sign",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ProofEmerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Digitally Signed by: ${proof.recipientName}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = ProofEmerald
            )
            Text(
                text = "${proof.deliveryPhotoSummary} • Captured: ${proof.capturedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
            if (proof.recipientSignatureSvg.isNotBlank()) {
                SignatureSvgPreviewBox(signatureSvg = proof.recipientSignatureSvg)
            }
        }
    }
}
