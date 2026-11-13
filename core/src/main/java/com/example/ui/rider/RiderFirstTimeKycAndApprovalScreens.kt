package com.example.ui.rider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.RiderApprovalWorkflowStatus
import com.example.data.cloud.RiderRegistrationApprovalManager
import com.example.data.cloud.RiderRegistrationRecord
import com.example.data.model.VehicleType
import kotlinx.coroutines.delay

private enum class UploadTargetField {
    AADHAAR,
    PAN,
    DRIVING_LICENCE,
    VEHICLE_RC,
    LIVE_SELFIE
}

private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
    }
}.getOrNull()

/**
 * Mandatory first-time Rider KYC Document Upload Screen.
 * Blocks access to Online duty until all required documents are uploaded and approved by Admin.
 */
@Composable
fun RiderFirstTimeKycUploadScreen(
    riderName: String,
    riderPhone: String,
    riderEmail: String,
    existingRecord: RiderRegistrationRecord?,
    onDocumentsSubmitted: (RiderRegistrationRecord) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedVehicle by remember {
        mutableStateOf(
            runCatching { VehicleType.valueOf(existingRecord?.vehicleType ?: VehicleType.BIKE.name) }
                .getOrDefault(VehicleType.BIKE)
        )
    }
    var vehicleNumber by remember { mutableStateOf(existingRecord?.vehicleNumber.orEmpty()) }
    var aadhaarNumber by remember { mutableStateOf(existingRecord?.aadhaarNumber.orEmpty()) }
    var panNumber by remember { mutableStateOf(existingRecord?.panNumber.orEmpty()) }
    var dlNumber by remember { mutableStateOf(existingRecord?.drivingLicenseNumber.orEmpty()) }

    var aadhaarBitmap by remember {
        mutableStateOf(RiderRegistrationApprovalManager.decodeBase64ToBitmap(existingRecord?.aadhaarImageBase64))
    }
    var panBitmap by remember {
        mutableStateOf(RiderRegistrationApprovalManager.decodeBase64ToBitmap(existingRecord?.panImageBase64))
    }
    var dlBitmap by remember {
        mutableStateOf(RiderRegistrationApprovalManager.decodeBase64ToBitmap(existingRecord?.dlImageBase64))
    }
    var rcBitmap by remember {
        mutableStateOf(RiderRegistrationApprovalManager.decodeBase64ToBitmap(existingRecord?.rcImageBase64))
    }
    var selfieBitmap by remember {
        mutableStateOf(RiderRegistrationApprovalManager.decodeBase64ToBitmap(existingRecord?.selfieImageBase64))
    }

    var activeTarget by remember { mutableStateOf<UploadTargetField?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val assignBitmapForTarget: (Bitmap?) -> Unit = { bmp ->
        if (bmp != null) {
            when (activeTarget) {
                UploadTargetField.AADHAAR -> aadhaarBitmap = bmp
                UploadTargetField.PAN -> panBitmap = bmp
                UploadTargetField.DRIVING_LICENCE -> dlBitmap = bmp
                UploadTargetField.VEHICLE_RC -> rcBitmap = bmp
                UploadTargetField.LIVE_SELFIE -> selfieBitmap = bmp
                null -> Unit
            }
            validationError = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        assignBitmapForTarget(bmp)
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            validationError = "Camera permission is needed to take a photo, or choose Upload Photo."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = decodeUriToBitmap(context, uri)
            if (bmp != null) {
                assignBitmapForTarget(bmp)
            } else {
                validationError = "Unable to read selected image. Please try another photo."
            }
        }
    }

    val launchCameraFor: (UploadTargetField) -> Unit = { target ->
        activeTarget = target
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchGalleryFor: (UploadTargetField) -> Unit = { target ->
        activeTarget = target
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val uploadedCount = listOf(aadhaarBitmap, panBitmap, dlBitmap, rcBitmap, selfieBitmap).count { it != null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F8F9))
            .testTag("rider_first_time_kyc_upload_screen")
    ) {
        Surface(
            color = Color(0xFF0F1E36),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Partner KYC Onboarding ($uploadedCount/5)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "$riderName • $riderPhone",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                OutlinedButton(
                    onClick = onSignOut,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("rider_kyc_signout_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sign Out", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (existingRecord?.approvalStatus == RiderApprovalWorkflowStatus.REJECTED) {
                item {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                            Column {
                                Text(
                                    text = "Previous Verification Rejected by Admin",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = existingRecord.rejectionReason.ifBlank { "Please re-upload clear document photos and resubmit." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, QuickDeliveryOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Upload All Documents for Admin Approval",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9A3412)
                        )
                        Text(
                            text = "First-time Rider Partners must upload Aadhaar, PAN, Driving Licence, Vehicle RC, and Live Selfie. You can go Online after Admin verifies & approves your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7C2D12)
                        )
                    }
                }
            }

            // Vehicle Type & Plate
            item {
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
                        Text(
                            text = "Vehicle Details *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F1E36)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(VehicleType.BIKE, VehicleType.SCOOTER, VehicleType.AUTO).forEach { v ->
                                FilterChip(
                                    selected = selectedVehicle == v,
                                    onClick = { selectedVehicle = v },
                                    label = { Text(v.title, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = QuickDeliveryOrange,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = vehicleNumber,
                            onValueChange = {
                                vehicleNumber = it.uppercase().take(16)
                                validationError = null
                            },
                            label = { Text("Vehicle Registration No. (RC Plate) *") },
                            placeholder = { Text("e.g. DL01AB1234") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kyc_vehicle_number_input")
                        )
                    }
                }
            }

            // 1. Aadhaar Card
            item {
                KycDocUploadCard(
                    title = "1. Aadhaar Card *",
                    subtitle = "12-digit Aadhaar Number & Front Photo",
                    numberValue = aadhaarNumber,
                    onNumberChange = {
                        aadhaarNumber = it.filter { ch -> ch.isDigit() || ch == '-' || ch == ' ' }.take(16)
                        validationError = null
                    },
                    numberLabel = "Aadhaar Number *",
                    bitmap = aadhaarBitmap,
                    onTakePhoto = { launchCameraFor(UploadTargetField.AADHAAR) },
                    onPickGallery = { launchGalleryFor(UploadTargetField.AADHAAR) },
                    testTagPrefix = "kyc_aadhaar"
                )
            }

            // 2. PAN Card
            item {
                KycDocUploadCard(
                    title = "2. PAN Card *",
                    subtitle = "10-digit PAN Number & Card Photo",
                    numberValue = panNumber,
                    onNumberChange = {
                        panNumber = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(10)
                        validationError = null
                    },
                    numberLabel = "PAN Number *",
                    bitmap = panBitmap,
                    onTakePhoto = { launchCameraFor(UploadTargetField.PAN) },
                    onPickGallery = { launchGalleryFor(UploadTargetField.PAN) },
                    testTagPrefix = "kyc_pan"
                )
            }

            // 3. Driving Licence
            item {
                KycDocUploadCard(
                    title = "3. Driving Licence (DL) *",
                    subtitle = "DL Number & Valid Licence Photo",
                    numberValue = dlNumber,
                    onNumberChange = {
                        dlNumber = it.uppercase().take(20)
                        validationError = null
                    },
                    numberLabel = "Driving Licence Number *",
                    bitmap = dlBitmap,
                    onTakePhoto = { launchCameraFor(UploadTargetField.DRIVING_LICENCE) },
                    onPickGallery = { launchGalleryFor(UploadTargetField.DRIVING_LICENCE) },
                    testTagPrefix = "kyc_dl"
                )
            }

            // 4. Vehicle RC Photo
            item {
                KycDocUploadCard(
                    title = "4. Vehicle RC Certificate Photo *",
                    subtitle = "Upload clear photo of Vehicle Registration (RC)",
                    numberValue = null,
                    onNumberChange = null,
                    numberLabel = "",
                    bitmap = rcBitmap,
                    onTakePhoto = { launchCameraFor(UploadTargetField.VEHICLE_RC) },
                    onPickGallery = { launchGalleryFor(UploadTargetField.VEHICLE_RC) },
                    testTagPrefix = "kyc_rc"
                )
            }

            // 5. Live Selfie
            item {
                KycDocUploadCard(
                    title = "5. Rider Profile Selfie *",
                    subtitle = "Clear front face photo for verification",
                    numberValue = null,
                    onNumberChange = null,
                    numberLabel = "",
                    bitmap = selfieBitmap,
                    onTakePhoto = { launchCameraFor(UploadTargetField.LIVE_SELFIE) },
                    onPickGallery = { launchGalleryFor(UploadTargetField.LIVE_SELFIE) },
                    testTagPrefix = "kyc_selfie"
                )
            }

            if (validationError != null) {
                item {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        when {
                            vehicleNumber.trim().length < 4 -> {
                                validationError = "Please enter a valid Vehicle Registration Number (*)."
                                return@Button
                            }
                            aadhaarNumber.filter(Char::isDigit).length < 12 -> {
                                validationError = "Please enter a valid 12-digit Aadhaar Number (*)."
                                return@Button
                            }
                            aadhaarBitmap == null -> {
                                validationError = "Please upload or capture Aadhaar Card photo (*)."
                                return@Button
                            }
                            panNumber.trim().length < 10 -> {
                                validationError = "Please enter your 10-character PAN Number (*)."
                                return@Button
                            }
                            panBitmap == null -> {
                                validationError = "Please upload or capture PAN Card photo (*)."
                                return@Button
                            }
                            dlNumber.trim().length < 6 -> {
                                validationError = "Please enter your Driving Licence (DL) Number (*)."
                                return@Button
                            }
                            dlBitmap == null -> {
                                validationError = "Please upload or capture Driving Licence photo (*)."
                                return@Button
                            }
                            rcBitmap == null -> {
                                validationError = "Please upload or capture Vehicle RC photo (*)."
                                return@Button
                            }
                            selfieBitmap == null -> {
                                validationError = "Please capture or upload your Live Selfie photo (*)."
                                return@Button
                            }
                        }
                        val submitted = RiderRegistrationApprovalManager.submitRiderKycDocuments(
                            context = context,
                            name = riderName,
                            phone = riderPhone,
                            email = riderEmail,
                            vehicleType = selectedVehicle,
                            vehicleNumber = vehicleNumber,
                            aadhaarNumber = aadhaarNumber,
                            panNumber = panNumber,
                            drivingLicenseNumber = dlNumber,
                            aadhaarBitmap = aadhaarBitmap,
                            panBitmap = panBitmap,
                            dlBitmap = dlBitmap,
                            rcBitmap = rcBitmap,
                            selfieBitmap = selfieBitmap
                        )
                        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                        onDocumentsSubmitted(submitted)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("rider_submit_kyc_for_admin_approval_btn")
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Documents for Admin Approval →",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun KycDocUploadCard(
    title: String,
    subtitle: String,
    numberValue: String?,
    onNumberChange: ((String) -> Unit)?,
    numberLabel: String,
    bitmap: Bitmap?,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    testTagPrefix: String
) {
    val isDone = bitmap != null
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDone) Color(0xFFF0FDF4) else Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, if (isDone) Color(0xFF10B981) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F1E36)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
                Surface(
                    color = if (isDone) Color(0xFF10B981) else Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isDone) "Uploaded ✓" else "Required",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) Color.White else Color(0xFFEA580C),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (numberValue != null && onNumberChange != null) {
                OutlinedTextField(
                    value = numberValue,
                    onValueChange = onNumberChange,
                    label = { Text(numberLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_number_input")
                )
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "$title uploaded photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = QuickDeliveryOrange),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_camera_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDone) "Retake Photo" else "Camera",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onPickGallery,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_gallery_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Upload Photo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Shown while Rider is waiting for Admin verification (`PENDING_APPROVAL`) OR
 * immediately when Admin approves and Rider must sign in again before going Online.
 */
@Composable
fun RiderPendingAdminApprovalScreen(
    record: RiderRegistrationRecord,
    onRequireReLogin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isApproved = record.approvalStatus == RiderApprovalWorkflowStatus.APPROVED

    LaunchedEffect(record.riderKey, record.approvalStatus) {
        if (!isApproved) {
            while (true) {
                MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                delay(2000L)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F8F9))
            .padding(16.dp)
            .testTag(if (isApproved) "rider_admin_approved_relogin_screen" else "rider_pending_approval_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.5.dp,
                if (isApproved) Color(0xFF10B981) else Color(0xFFF59E0B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = if (isApproved) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = if (isApproved) "Approved" else "Pending Approval",
                            tint = if (isApproved) Color(0xFF10B981) else Color(0xFFD97706),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = if (isApproved) {
                        "🎉 Account Approved by Admin!"
                    } else {
                        "⏳ Verification Pending"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isApproved) Color(0xFF065F46) else Color(0xFF0F1E36)
                )

                Text(
                    text = if (isApproved) {
                        "Congratulations ${record.name}! Admin has verified and approved your documents. Please sign in again to activate your account and go Online."
                    } else {
                        "Hello ${record.name}, your documents have been sent to Admin. Until Admin approves your KYC, your account status stays PENDING and Online duty is locked."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Partner: ${record.name} (${record.phone})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F1E36)
                        )
                        Text(
                            text = "Vehicle: ${record.vehicleType} • ${record.vehicleNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "Documents Uploaded: Aadhaar ✓ • PAN ✓ • DL ✓ • RC ✓ • Selfie ✓",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = if (isApproved) "Status: APPROVED ✅" else "Status: PENDING ADMIN APPROVAL ⏳",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isApproved) Color(0xFF059669) else Color(0xFFD97706)
                        )
                    }
                }

                if (isApproved) {
                    Button(
                        onClick = onRequireReLogin,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("rider_relogin_after_approval_btn")
                    ) {
                        Text(
                            text = "Sign In Again to Go Online →",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF008080)
                        )
                        Text(
                            text = "Auto-checking Admin approval live...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { MultiPhoneLiveSyncManager.triggerImmediateSync(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("rider_check_approval_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("rider_pending_signout_btn")
                        ) {
                            Text("Sign Out", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared Admin Console Widget displaying newly registered Riders waiting for KYC approval,
 * complete with uploaded photo thumbnails and 1-tap Approve / Reject actions.
 */
@Composable
fun AdminPendingRiderApprovalsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val registrationsMap by RiderRegistrationApprovalManager.registrationsByKey.collectAsState()
    val pendingList = remember(registrationsMap) {
        registrationsMap.values
            .filter { it.approvalStatus == RiderApprovalWorkflowStatus.PENDING_APPROVAL }
            .sortedByDescending { it.submittedAtEpochMs }
    }
    var rejectingRecord by remember { mutableStateOf<RiderRegistrationRecord?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("Please upload clearer document photos.") }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    if (rejectingRecord != null) {
        val target = rejectingRecord!!
        AlertDialog(
            onDismissRequest = { rejectingRecord = null },
            title = { Text("Reject Rider KYC (${target.name})", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = rejectionReasonInput,
                    onValueChange = { rejectionReasonInput = it },
                    label = { Text("Reason for Rejection") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        RiderRegistrationApprovalManager.rejectRiderByAdmin(context, target.riderKey, rejectionReasonInput)
                        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                        actionFeedback = "❌ Rejected ${target.name} (${target.phone})"
                        rejectingRecord = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reject Rider")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingRecord = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pendingList.isEmpty() && actionFeedback == null) {
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_pending_rider_approvals_card")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "New Rider KYC Approval Requests (${pendingList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F1E36)
                        )
                        Text(
                            text = "Verify uploaded Aadhaar, PAN, DL, RC & Selfie before Rider can go Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            if (actionFeedback != null) {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionFeedback.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            pendingList.forEach { reg ->
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pending_rider_item_${reg.riderKey}")
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${reg.name} (${reg.vehicleType} • ${reg.vehicleNumber})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F1E36)
                                )
                                Text(
                                    text = "📱 ${reg.phone} • ✉️ ${reg.email} • Submitted: ${reg.submittedAtLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569)
                                )
                            }
                            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "PENDING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Aadhaar: ${reg.aadhaarNumber} • PAN: ${reg.panNumber} • DL: ${reg.drivingLicenseNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )

                        // Uploaded Document Thumbnails Row
                        val thumbs = listOf(
                            "Aadhaar" to reg.aadhaarImageBase64,
                            "PAN" to reg.panImageBase64,
                            "DL" to reg.dlImageBase64,
                            "RC" to reg.rcImageBase64,
                            "Selfie" to reg.selfieImageBase64
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            thumbs.forEach { (label, b64) ->
                                val bmp = remember(b64) { RiderRegistrationApprovalManager.decodeBase64ToBitmap(b64) }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "$label Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Doc", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                                        }
                                    }
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFFDE68A))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { rejectingRecord = reg },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_reject_rider_btn_${reg.riderKey}")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    RiderRegistrationApprovalManager.approveRiderByAdmin(context, reg.riderKey)
                                    MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                    actionFeedback = "✅ Approved ${reg.name}! Rider can now re-login and go Online."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("admin_approve_rider_btn_${reg.riderKey}")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve Rider", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
