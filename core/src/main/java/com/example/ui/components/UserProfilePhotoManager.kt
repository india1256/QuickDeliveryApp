package com.example.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.cloud.DeliveryProofAndGpsManager
import com.example.security.QuickDeliverySecurityGuard
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Crash-safe avatar bitmap helpers: center-crops & scales any gallery/camera image
 * to 384x384 JPEG (~25KB) stored cleanly inside app-private filesDir.
 */
object ProfilePhotoStorageUtils {
    private const val MAX_AVATAR_PX = 384

    fun cropSquareAvatar(source: Bitmap, targetPx: Int = MAX_AVATAR_PX): Bitmap {
        val side = minOf(source.width, source.height).coerceAtLeast(1)
        val x = ((source.width - side) / 2).coerceAtLeast(0)
        val y = ((source.height - side) / 2).coerceAtLeast(0)
        val square = if (source.width == side && source.height == side) {
            source
        } else {
            Bitmap.createBitmap(source, x, y, side, side)
        }
        return if (square.width > targetPx) {
            Bitmap.createScaledBitmap(square, targetPx, targetPx, true)
        } else {
            square
        }
    }

    fun decodeUriToAvatarBitmap(context: Context, uri: Uri, targetPx: Int = MAX_AVATAR_PX): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            if (maxDim > targetPx * 2) {
                while ((maxDim / sampleSize) > targetPx * 2) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null
            cropSquareAvatar(decoded, targetPx)
        }.getOrNull()
    }

    fun saveJpegAvatar(context: Context, fileName: String, bitmap: Bitmap): Bitmap {
        val cropped = cropSquareAvatar(bitmap)
        runCatching {
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        }
        return cropped
    }

    fun loadJpegAvatar(context: Context, fileName: String): Bitmap? {
        return runCatching {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }

    fun deleteAvatarFile(context: Context, fileName: String) {
        runCatching {
            val file = File(context.filesDir, fileName)
            if (file.exists()) file.delete()
        }
    }
}

/**
 * Manages Customer Profile Picture (DP) persistence in app-private storage (zero storage permissions).
 */
object CustomerProfilePhotoManager {
    private const val PREFS_NAME = "quick_delivery_customer_profile_photo_prefs"
    private const val KEY_PHOTO_NOTE = "customer_profile_photo_note"
    private const val AVATAR_FILE_NAME = "customer_profile_dp.jpg"
    private var loaded = false

    private val _profilePhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val profilePhotoBitmap: StateFlow<Bitmap?> = _profilePhotoBitmap.asStateFlow()

    private val _lastUpdatedNote = MutableStateFlow<String?>(null)
    val lastUpdatedNote: StateFlow<String?> = _lastUpdatedNote.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (!loaded) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _profilePhotoBitmap.value = ProfilePhotoStorageUtils.loadJpegAvatar(context, AVATAR_FILE_NAME)
            _lastUpdatedNote.value = if (_profilePhotoBitmap.value != null) {
                prefs.getString(KEY_PHOTO_NOTE, null)
            } else {
                null
            }
            loaded = true
        }
    }

    fun saveProfilePhoto(context: Context, bitmap: Bitmap, sourceLabel: String = "Camera") {
        val saved = ProfilePhotoStorageUtils.saveJpegAvatar(context, AVATAR_FILE_NAME, bitmap)
        _profilePhotoBitmap.value = saved
        val timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val note = "Updated via $sourceLabel ($timestamp)"
        _lastUpdatedNote.value = note
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHOTO_NOTE, note)
            .apply()
        loaded = true
    }

    fun saveProfilePhotoFromUri(context: Context, uri: Uri): Boolean {
        val decoded = ProfilePhotoStorageUtils.decodeUriToAvatarBitmap(context, uri) ?: return false
        saveProfilePhoto(context, decoded, "Gallery")
        return true
    }

    fun clearProfilePhoto(context: Context) {
        ProfilePhotoStorageUtils.deleteAvatarFile(context, AVATAR_FILE_NAME)
        _profilePhotoBitmap.value = null
        _lastUpdatedNote.value = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PHOTO_NOTE)
            .apply()
    }
}

/**
 * Shared strict 10-digit Indian mobile number sanitization & live validation helper.
 */
data class IndianMobileInputResult(
    val sanitizedDigits: String,
    val overLimitAttempted: Boolean,
    val inlineError: String?,
    val isValid: Boolean
)

fun processIndianMobileInput(rawInput: String): IndianMobileInputResult {
    val trimmed = rawInput.trim()
    val rawDigits = trimmed.filter(Char::isDigit)
    val normalizedDigits = when {
        trimmed.startsWith("+91") && rawDigits.length >= 12 -> rawDigits.drop(2)
        trimmed.startsWith("91") && rawDigits.length == 12 -> rawDigits.drop(2)
        trimmed.startsWith("0") && rawDigits.length == 11 -> rawDigits.drop(1)
        else -> rawDigits
    }
    val overLimit = normalizedDigits.length > 10
    val finalDigits = normalizedDigits.take(10)
    val error = evaluateIndianMobileError(finalDigits, overLimit)
    return IndianMobileInputResult(
        sanitizedDigits = finalDigits,
        overLimitAttempted = overLimit,
        inlineError = error,
        isValid = finalDigits.length == 10 && !QuickDeliverySecurityGuard.isFakeOrDummyPhone(finalDigits)
    )
}

fun evaluateIndianMobileError(digits: String, overLimitAttempted: Boolean = false): String? {
    return when {
        overLimitAttempted -> "Only 10 digits allowed (cannot exceed 10 digits)"
        digits.isEmpty() -> null
        digits.length < 10 -> "Enter exact 10-digit mobile number (${digits.length}/10)"
        QuickDeliverySecurityGuard.isFakeOrDummyPhone(digits) -> "Enter a valid 10-digit Indian mobile number"
        else -> null
    }
}

/**
 * Always-visible "+91" leading indicator for mobile number fields across Customer & Rider apps.
 */
@Composable
fun IndianPhonePrefixBadge(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 12.dp, end = 6.dp)
    ) {
        Text(
            text = "+91",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(Color(0xFFCBD5E1))
        )
    }
}

/**
 * Customer Profile & DP Dialog: supports zero-permission Android Photo Picker (Gallery),
 * runtime-permission Camera capture, and Remove DP.
 */
@Composable
fun CustomerProfileDpDialog(
    customerName: String,
    customerPhone: String,
    customerEmail: String,
    accentColor: Color,
    secondaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    remember(context) { CustomerProfilePhotoManager.ensureLoaded(context) }
    val profilePhotoBitmap by CustomerProfilePhotoManager.profilePhotoBitmap.collectAsState()
    val lastUpdatedNote by CustomerProfilePhotoManager.lastUpdatedNote.collectAsState()
    var photoStatusMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            CustomerProfilePhotoManager.saveProfilePhoto(context, bitmap, "Camera")
            photoStatusMessage = "Profile photo updated via Camera"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            photoStatusMessage = "Camera permission required to take a DP photo"
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = CustomerProfilePhotoManager.saveProfilePhotoFromUri(context, uri)
            photoStatusMessage = if (saved) {
                "Profile photo updated from Gallery"
            } else {
                "Could not read selected image"
            }
        }
    }

    val launchCamera: () -> Unit = {
        if (DeliveryProofAndGpsManager.hasCameraPermission(context)) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cleanPhone = QuickDeliverySecurityGuard.sanitizePhoneNumber(customerPhone)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_profile_dp_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Profile & DP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1B2A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Profile",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clickable {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("customer_profile_dp_avatar_preview")
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = CircleShape,
                        border = BorderStroke(2.5.dp, secondaryColor),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val currentPhoto = profilePhotoBitmap
                        if (currentPhoto != null) {
                            Image(
                                bitmap = currentPhoto.asImageBitmap(),
                                contentDescription = "Customer Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Customer Avatar",
                                    tint = accentColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                    Surface(
                        color = secondaryColor,
                        shape = CircleShape,
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change DP",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = customerName.ifBlank { "Quick Delivery Customer" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (cleanPhone.isNotBlank()) {
                        Text(
                            text = "+91 $cleanPhone",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                    if (customerEmail.isNotBlank()) {
                        Text(
                            text = customerEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!lastUpdatedNote.isNullOrBlank()) {
                        Text(
                            text = lastUpdatedNote.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF059669)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = launchCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_capture_dp_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Camera",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, secondaryColor),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_upload_dp_btn")
                    ) {
                        Text(
                            text = "Gallery DP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor,
                            maxLines = 1
                        )
                    }
                }

                if (profilePhotoBitmap != null) {
                    TextButton(
                        onClick = {
                            CustomerProfilePhotoManager.clearProfilePhoto(context)
                            photoStatusMessage = "Profile photo removed"
                        },
                        modifier = Modifier.testTag("customer_remove_dp_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove DP",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remove DP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                if (!photoStatusMessage.isNullOrBlank()) {
                    Text(
                        text = photoStatusMessage.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669),
                        modifier = Modifier.testTag("customer_dp_status_text")
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
