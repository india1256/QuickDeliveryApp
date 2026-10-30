package com.example.ui.delivery

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.delivery.CrossPlatformDeliveryEngine
import com.example.data.delivery.DeliveryActorRole
import com.example.data.delivery.ParcelDeliveryStage
import com.example.data.delivery.ParcelDeliveryStateManager
import com.example.data.delivery.ParcelDeliveryTrackingModel
import com.example.data.delivery.ParcelDeliveryUiState
import com.example.data.delivery.TargetMobilePlatform

/**
 * Shared Material 3 parcel lifecycle stepper & state transition card for Customer (:app) and Rider (:rider) apps.
 */
@Composable
fun ParcelDeliveryStatusStepperCard(
    stateManager: ParcelDeliveryStateManager = ParcelDeliveryStateManager.getInstance(),
    onStageAdvanced: ((ParcelDeliveryTrackingModel) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by stateManager.uiState.collectAsState()

    val activeTracking: ParcelDeliveryTrackingModel? = when (val state = uiState) {
        is ParcelDeliveryUiState.Active -> state.tracking
        is ParcelDeliveryUiState.TransitionError -> state.tracking
        else -> null
    }

    if (activeTracking == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parcel_delivery_status_stepper_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Parcel Delivery Status",
                        tint = Color(0xFF0D1B2A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Parcel ${activeTracking.orderCode} • ${activeTracking.currentStage.displayLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1B2A),
                            modifier = Modifier.testTag("parcel_delivery_current_stage_text")
                        )
                        Text(
                            text = activeTracking.currentStage.customerSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0D1B2A).copy(alpha = 0.72f)
                        )
                    }
                }

                val nextStage = activeTracking.nextStage
                if (nextStage != null) {
                    Button(
                        onClick = {
                            val result = stateManager.advanceToNextStage(
                                orderId = activeTracking.orderId,
                                actorRole = DeliveryActorRole.RIDER,
                                actorId = activeTracking.assignedRiderId ?: "RIDER-101"
                            )
                            result.getOrNull()?.let { updated ->
                                onStageAdvanced?.invoke(updated)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                        modifier = Modifier.testTag("advance_parcel_stage_button")
                    ) {
                        Text(
                            text = activeTracking.currentStage.riderActionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("parcel_delivered_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Delivered",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delivered",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { activeTracking.currentStage.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("parcel_delivery_progress_bar"),
                color = Color(0xFF00A896),
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParcelDeliveryStage.entries.forEach { stage ->
                    val reached = stage.stepIndex <= activeTracking.currentStage.stepIndex
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (reached) Color(0xFF00A896) else Color(0xFFCBD5E1))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stage.displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (stage == activeTracking.currentStage) FontWeight.Bold else FontWeight.Normal,
                            color = if (reached) Color(0xFF0D1B2A) else Color(0xFF64748B)
                        )
                    }
                }
            }

            if (uiState is ParcelDeliveryUiState.TransitionError) {
                val err = uiState as ParcelDeliveryUiState.TransitionError
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = err.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.testTag("parcel_transition_error_text")
                )
            }
        }
    }
}

/**
 * Interactive iOS (Customer + Rider) Cross-Platform Companion & Live Preview Dialog.
 * Displays the bundled iOS Safari PWA (`apple-mobile-web-app-capable`) for both Customer and Rider
 * alongside the shared Kotlin Multiplatform (`:core`) contract details.
 */
@Composable
fun IosCrossPlatformCompanionDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val iosPlatform = TargetMobilePlatform.IOS

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("ios_companion_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.PhoneIphone,
                            contentDescription = "iOS App Companion",
                            tint = Color(0xFF0D1B2A),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Quick Delivery • iOS (Customer & Rider)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D1B2A)
                            )
                            Text(
                                text = "Customer: ${iosPlatform.customerAppIdentifier} • Rider: ${iosPlatform.riderAppIdentifier}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_ios_companion_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close iOS Preview")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFEFF8F7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Shared Firebase Project: ${CrossPlatformDeliveryEngine.SHARED_FIREBASE_PROJECT_ID} • 20% Commission • ₹150 Debt Cap",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D1B2A)
                        )
                        Text(
                            text = "Works on iPhone & iPad via Safari 'Add to Home Screen' (PWA) + KMP Shared Core.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF059669)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF4F7FA))
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .testTag("ios_pwa_webview"),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                loadDataWithBaseURL(
                                    "https://quickdelivery.ios.local/",
                                    CrossPlatformDeliveryEngine.IOS_PWA_HTML,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = buildString {
                                appendLine("Quick Delivery for iOS (iPhone & iPad)")
                                appendLine("• Customer App ID: ${iosPlatform.customerAppIdentifier}")
                                appendLine("• Rider App ID: ${iosPlatform.riderAppIdentifier}")
                                appendLine("• Shared Firebase Project: ${CrossPlatformDeliveryEngine.SHARED_FIREBASE_PROJECT_ID}")
                                appendLine("• Shared Firestore DB: ${CrossPlatformDeliveryEngine.SHARED_FIRESTORE_DATABASE_ID}")
                                appendLine("Open quick_delivery_ios.html in iPhone Safari and tap Share -> Add to Home Screen.")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Quick Delivery iOS (Customer + Rider)")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share iOS App Setup"))
                        },
                        modifier = Modifier.testTag("share_ios_app_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share iOS App", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share for iPhone Safari", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B2A))
                    ) {
                        Text("Done", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}

