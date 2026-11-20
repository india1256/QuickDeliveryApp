package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.DeliveryProofAndGpsManager
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.AddressDetails
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.GatewayCallbackStatus
import com.example.data.model.GatewayPaymentOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PackageDetails
import com.example.data.model.PackageType
import com.example.data.model.UpiAppOption
import com.example.data.model.VehicleType
import com.example.data.pricing.ServerFareEngine
import com.example.data.repository.SharedOrderRepository
import com.example.ui.auth.QuickDeliveryTopOptionsDialog
import com.example.ui.components.CustomerOrderMoreNotificationBanner
import com.example.ui.components.DirectDialPadCallButton
import com.example.ui.components.DualDeliveryCelebrationMotivationBanner
import com.example.ui.components.FairAutoRiderDispatchSummaryCard
import com.example.ui.components.FcmOrderNotificationManager
import com.example.ui.components.FcmRealtimeNotificationCenterCard
import com.example.ui.components.FcmTargetAppRole
import com.example.ui.components.LiveRiderContinuousTrackingCard
import com.example.ui.components.MotivationAndPromoManager
import com.example.ui.components.PhoneStorageLoginBackupCard
import com.example.ui.components.QuickDeliveryDeleteAccountDialog
import com.example.ui.components.QuickDeliveryMandatoryPermissionsGateScreen
import com.example.ui.components.QuickDeliveryNextLevelLaunchOverlay
import com.example.ui.components.QuickDeliveryCuteBunnyLogo
import com.example.ui.components.QuickDeliveryOfficialLogo
import com.example.ui.components.QuickDeliveryPrivacyPolicyDialog
import com.example.ui.components.QuickDeliverySpeedingVehicleIntro
import com.example.ui.components.QuickDeliverySuggestionBoxCard
import com.example.ui.components.QuickDeliveryUserAuthScreen
import com.example.ui.components.CustomerProfileDpDialog
import com.example.ui.components.CustomerProfilePhotoManager
import com.example.ui.components.IndianPhonePrefixBadge
import com.example.ui.components.RiderFeedbackManager
import com.example.ui.components.evaluateIndianMobileError
import com.example.ui.components.hasAllMandatoryAppPermissions
import com.example.ui.components.processIndianMobileInput
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.InlineLanguageSelectorBar
import com.example.ui.i18n.LanguageSwitcherPill
import com.example.ui.i18n.LocalizedContextProvider
import com.example.ui.i18n.rememberDynamicUiStrings
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PrimaryTeal = Color(0xFF078B8D)
private val BrightTeal = Color(0xFF078B8D)
private val DeepTeal = Color(0xFF045D5E)
private val BrandOrange = Color(0xFFFF7A1A)
private const val QuickDeliveryTagline = "FAST. SIMPLE. SECURE."
private val SoftOrangeSurface = Color(0xFFFFF4EE)
private val SoftTealSurface = Color(0xFFE6F6F4)
private val DeepNavy = Color(0xFF0D1B2A)
private val SlateMuted = Color(0xFF475569)
private val SoftBg = Color(0xFFF4F7F9)
private val VerifiedGreen = Color(0xFF059669)

/**
 * Customer App Flow:
 * 0. Speeding Vehicle Intro Animation -> New/Old Customer Sign Up or Sign In
 * 1. Step 1: Address & Vehicle Selection -> Click Next
 * 2. Step 2: Parcel Photo Capture Option (Right after Address Next!) -> Click Next
 * 3. Step 3: Payment Option (Single Payment Icon reveals UPI, Card, Net Banking) -> Confirm & Book
 * 4. Step 4: Continuous Live Tracking (From Rider Accept all the way until Delivered) + Customer Promo Notification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAppScreen(
    modifier: Modifier = Modifier
) = LocalizedContextProvider {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiStrings = rememberDynamicUiStrings()
    val repository = remember {
        SharedOrderRepository.getInstance(context).also { repo ->
            AutomatedFairRiderAssignmentEngine.ensureCleanInitialDatabaseState(repo)
        }
    }

    val orders by repository.orders.collectAsState()
    val riders by repository.riders.collectAsState()
    val pricingConfig by repository.pricingConfig.collectAsState()
    val gatewayConfig = remember { repository.getGatewayMerchantConfig() }

    // 0. Intro Animation & Customer Sign Up / Sign In State (cached in session so tab switches are 0ms instant)
    remember(context) { MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context) }
    remember(context) { QuickDeliveryOperationsManager.ensureLoaded(context) }
    remember(context) { CustomerProfilePhotoManager.ensureLoaded(context) }
    val customerProfileBitmap by CustomerProfilePhotoManager.profilePhotoBitmap.collectAsState()
    var showSpeedIntro by remember { mutableStateOf(!MotivationAndPromoManager.customerIntroCompleted) }

    // 1 = Address & Route, 2 = Parcel Photo (after Address Next), 3 = Payment, 4 = Live Tracking
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    val customerMainListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val clearFocusOnUserScrollConnection = remember(focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && kotlin.math.abs(available.y) > 8f) {
                    focusManager.clearFocus(force = false)
                }
                return Offset.Zero
            }
        }
    }
    val savedAddresses by QuickDeliveryOperationsManager.savedAddresses.collectAsState()
    val adminPromoCoupons by QuickDeliveryOperationsManager.promoCoupons.collectAsState()
    val customerSupportTickets by QuickDeliveryOperationsManager.supportTickets.collectAsState()
    val isCustomerStorageGranted by MotivationAndPromoManager.customerStoragePermissionGranted.collectAsState()
    val remoteFlags by QuickDeliveryOperationsManager.remoteFeatureFlags.collectAsState()
    val walletBonusInr by QuickDeliveryOperationsManager.customerWalletBonusInr.collectAsState()

    var isScheduledDelivery by rememberSaveable { mutableStateOf(false) }
    var scheduledDateLabel by rememberSaveable { mutableStateOf("Today") }
    var scheduledTimeSlot by rememberSaveable { mutableStateOf("3 PM - 6 PM") }
    var isMultiDropEnabled by rememberSaveable { mutableStateOf(false) }
    var extraStopAddress by rememberSaveable { mutableStateOf("") }
    var isParcelInsured by rememberSaveable { mutableStateOf(false) }
    var isHelperAdded by rememberSaveable { mutableStateOf(false) }
    var useWalletBonus by rememberSaveable { mutableStateOf(false) }

    // Customer profile & booking form state (preserved across back navigation & tab switches)
    var customerName by rememberSaveable { mutableStateOf(MotivationAndPromoManager.cachedCustomerName) }
    var customerPhone by rememberSaveable { mutableStateOf(MotivationAndPromoManager.cachedCustomerPhone) }
    var customerEmail by rememberSaveable { mutableStateOf(MotivationAndPromoManager.cachedCustomerEmail) }

    LaunchedEffect(customerPhone, customerEmail) {
        QuickDeliveryOperationsManager.setActiveCustomerAccount(context, customerPhone, customerEmail)
    }

    var pickupHouseFlat by rememberSaveable { mutableStateOf("") }
    var pickupBuilding by rememberSaveable { mutableStateOf("") }
    var pickupAddressText by rememberSaveable { mutableStateOf("") }
    var pickupPinCode by rememberSaveable { mutableStateOf("") }
    var pickupLandmark by rememberSaveable { mutableStateOf("") }
    var pickupLat by rememberSaveable { mutableDoubleStateOf(0.0) }
    var pickupLng by rememberSaveable { mutableDoubleStateOf(0.0) }
    var pickupLiveGpsVerified by rememberSaveable { mutableStateOf(false) }

    var dropHouseFlat by rememberSaveable { mutableStateOf("") }
    var dropBuilding by rememberSaveable { mutableStateOf("") }
    var dropAddressText by rememberSaveable { mutableStateOf("") }
    var dropPinCode by rememberSaveable { mutableStateOf("") }
    var dropLandmark by rememberSaveable { mutableStateOf("") }
    var dropLat by rememberSaveable { mutableDoubleStateOf(0.0) }
    var dropLng by rememberSaveable { mutableDoubleStateOf(0.0) }
    var dropLiveGpsVerified by rememberSaveable { mutableStateOf(false) }
    var pendingGpsTarget by rememberSaveable { mutableStateOf("PICKUP") }
    var isCapturingPickupLocation by remember { mutableStateOf(false) }
    var isCapturingDropLocation by remember { mutableStateOf(false) }
    var isResolvingStep1Route by remember { mutableStateOf(false) }

    var selectedPickupSavedTag by rememberSaveable { mutableStateOf("Home") }
    var selectedDropSavedTag by rememberSaveable { mutableStateOf("Office") }
    var receiverFullName by rememberSaveable { mutableStateOf("") }
    var receiverMobilePhone by rememberSaveable { mutableStateOf("") }
    var receiverPhoneOverLimit by remember { mutableStateOf(false) }

    val fullPickupAddressFormatted = remember(
        pickupHouseFlat,
        pickupBuilding,
        pickupAddressText,
        pickupPinCode,
        pickupLandmark
    ) {
        val baseParts = listOf(
            pickupHouseFlat.trim(),
            pickupBuilding.trim(),
            pickupAddressText.trim(),
            pickupLandmark.trim()
        )
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        val pinTrimmed = pickupPinCode.trim()
        if (pinTrimmed.isNotEmpty()) {
            if (baseParts.isNotEmpty()) "$baseParts - $pinTrimmed" else pinTrimmed
        } else {
            baseParts
        }
    }

    val fullDropAddressFormatted = remember(
        dropHouseFlat,
        dropBuilding,
        dropAddressText,
        dropPinCode,
        dropLandmark
    ) {
        val baseParts = listOf(
            dropHouseFlat.trim(),
            dropBuilding.trim(),
            dropAddressText.trim(),
            dropLandmark.trim()
        )
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        val pinTrimmed = dropPinCode.trim()
        if (pinTrimmed.isNotEmpty()) {
            if (baseParts.isNotEmpty()) "$baseParts - $pinTrimmed" else pinTrimmed
        } else {
            baseParts
        }
    }

    val vehiclePolicy by AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.collectAsState()

    var selectedPackageType by rememberSaveable { mutableStateOf(PackageType.DOCUMENTS) }
    var selectedWeightBracketLabel by rememberSaveable { mutableStateOf("Up to 5kg") }
    var packageWeightKg by rememberSaveable { mutableStateOf("1.0") }
    var packageDescription by rememberSaveable { mutableStateOf("") }
    var selectedInstructionsCsv by rememberSaveable { mutableStateOf("") }
    var selectedVehicle by rememberSaveable { mutableStateOf(VehicleType.BIKE) }

    // Keep selectedVehicle synchronized if Admin disables the currently selected vehicle
    LaunchedEffect(vehiclePolicy) {
        if (!vehiclePolicy.isVehicleEnabled(selectedVehicle)) {
            val fallback = listOf(VehicleType.BIKE, VehicleType.SCOOTER, VehicleType.AUTO)
                .firstOrNull { vehiclePolicy.isVehicleEnabled(it) }
                ?: VehicleType.BIKE
            selectedVehicle = fallback
        }
    }

    val activeMaxWeightKg = remember(vehiclePolicy, selectedVehicle) {
        vehiclePolicy.maxWeightForVehicle(selectedVehicle)
    }

    val weightValidationResult = remember(packageWeightKg, selectedVehicle, vehiclePolicy) {
        AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg(packageWeightKg, selectedVehicle)
    }
    val parsedWeightKg = remember(packageWeightKg) {
        packageWeightKg.trim().toDoubleOrNull()
    }
    val isWeightOverLimit = remember(parsedWeightKg, activeMaxWeightKg) {
        parsedWeightKg != null && (
            parsedWeightKg > AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_KG ||
                parsedWeightKg > activeMaxWeightKg
            )
    }
    val weightErrorMessage = remember(weightValidationResult) {
        weightValidationResult.exceptionOrNull()?.message
    }

    // Step 2 Parcel Photo state (captured right after Address -> Next)
    var currentBookingPhotoSummary by remember { mutableStateOf<String?>(null) }
    val orderPhotoProofMap = remember { mutableStateMapOf<String, String>() }

    // Step 3 Payment state: Single Online Payment Icon expands UPI, Card, and Net Banking
    var showOnlinePaymentOptions by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf(CustomerPaymentMethod.GATEWAY_UPI) }
    var selectedUpiApp by remember { mutableStateOf(UpiAppOption.GOOGLE_PAY) }
    var cardOrBankDetails by remember { mutableStateOf("") }
    var verifiedGatewayOrder by remember { mutableStateOf<GatewayPaymentOrder?>(null) }
    var selectedRiderTipInr by remember { mutableIntStateOf(0) }

    var statusBanner by remember { mutableStateOf<String?>(null) }
    var isErrorBanner by remember { mutableStateOf(false) }

    // Auto-clear non-error status banners and any resolved permission prompts so text never gets stuck on screen
    LaunchedEffect(statusBanner, isErrorBanner) {
        if (statusBanner != null && !isErrorBanner) {
            delay(3800)
            statusBanner = null
        }
    }

    // Clear stale permission/status banners when storage permission is granted
    LaunchedEffect(isCustomerStorageGranted) {
        if (isCustomerStorageGranted && statusBanner?.contains("permission", ignoreCase = true) == true) {
            statusBanner = null
            isErrorBanner = false
        }
    }

    val customerPostDeliveryMotivation by MotivationAndPromoManager.customerPostDeliveryMotivation.collectAsState()
    var liveRiderGpsCoordinates by rememberSaveable { mutableStateOf<String?>(null) }
    var showOrderHistoryScreen by rememberSaveable { mutableStateOf(false) }
    var showBookingConfirmationScreen by rememberSaveable { mutableStateOf(false) }
    var selectedTrackingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var latestBookedOrder by remember { mutableStateOf<CustomerOrder?>(null) }

    // Filter orders strictly to the logged-in customer's phone / email identity so different numbers see fresh state
    val normalizedCustomerPhone = remember(customerPhone) {
        com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(customerPhone)
    }
    val myCustomerId = remember(normalizedCustomerPhone, customerEmail) {
        if (normalizedCustomerPhone.length == 10) {
            "CUST-$normalizedCustomerPhone"
        } else if (customerEmail.isNotBlank()) {
            "CUST-EM-${customerEmail.trim().lowercase(Locale.US).hashCode().toUInt()}"
        } else {
            "CUST-GUEST"
        }
    }
    val customerOrders = remember(orders, normalizedCustomerPhone, myCustomerId) {
        orders.filter { ord ->
            val ordPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(ord.customerPhone)
            val ordPickupPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(ord.pickupAddress.contactPhone)
            when {
                normalizedCustomerPhone.length == 10 ->
                    ordPhone == normalizedCustomerPhone ||
                        (ordPhone.isBlank() && ordPickupPhone == normalizedCustomerPhone) ||
                        ord.customerId == myCustomerId
                else -> ord.customerId == myCustomerId
            }
        }
    }

    val pendingNotificationTap by FcmOrderNotificationManager.pendingNotificationTap.collectAsState()

    // Observe this customer's orders for automatic delivery motivation notifications & real-time FCM push updates
    LaunchedEffect(customerOrders) {
        MotivationAndPromoManager.observeOrdersForDeliveryMotivation(customerOrders)
        FcmOrderNotificationManager.observeOrdersAndDispatchFcmNotifications(context, customerOrders)
    }

    val activeOrder: CustomerOrder? = remember(customerOrders, selectedTrackingOrderId, latestBookedOrder, normalizedCustomerPhone, myCustomerId) {
        customerOrders.firstOrNull { it.orderId == selectedTrackingOrderId }
            ?: latestBookedOrder?.takeIf { latest ->
                val latestPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(latest.customerPhone)
                (normalizedCustomerPhone.length == 10 && latestPhone == normalizedCustomerPhone) || latest.customerId == myCustomerId
            }?.let { latest ->
                customerOrders.firstOrNull { it.orderId == latest.orderId } ?: latest
            }
            ?: customerOrders.firstOrNull()
    }

    var lastAlertedDeclineKey by remember { mutableStateOf("") }
    LaunchedEffect(
        activeOrder?.orderId,
        activeOrder?.orderStatus,
        activeOrder?.rejectedByRiderIds?.size
    ) {
        val ord = activeOrder ?: return@LaunchedEffect
        val declineCount = ord.rejectedByRiderIds.size
        val isDeclinedReSearch = ord.orderStatus == OrderStatus.SEARCHING_RIDER &&
            (declineCount > 0 || ord.latestRiderQuickNote?.contains("Declined", ignoreCase = true) == true)
        val key = "${ord.orderId}_$declineCount"
        if (isDeclinedReSearch && key != lastAlertedDeclineKey) {
            lastAlertedDeclineKey = key
            isErrorBanner = false
            val reasonNote = ord.latestRiderQuickNote?.takeIf { it.isNotBlank() } ?: "Previous rider declined"
            statusBanner = "🔄 Order #${ord.orderId}: $reasonNote — Searching for new nearby Rider..."
        }
    }

    var customLabelDialogSide by remember { mutableStateOf<String?>(null) }
    var customLabelDraft by remember { mutableStateOf("") }

    val receiverContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                runCatching {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val pickedName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty().trim() else ""
                            val pickedNum = if (numIdx >= 0) {
                                cursor.getString(numIdx).orEmpty().filter(Char::isDigit).takeLast(10)
                            } else ""
                            if (pickedName.isNotBlank()) receiverFullName = pickedName
                            if (pickedNum.length == 10) {
                                receiverMobilePhone = pickedNum
                                receiverPhoneOverLimit = false
                            }
                            isErrorBanner = false
                            statusBanner = "📇 Contact selected: ${pickedName.ifBlank { pickedNum }}"
                        }
                    }
                }.onFailure {
                    isErrorBanner = true
                    statusBanner = "Could not read selected phone contact. Enter details manually."
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val summary = DeliveryProofAndGpsManager.formatCameraProofSummary(bitmap, "Parcel")
            currentBookingPhotoSummary = summary
            activeOrder?.let { orderPhotoProofMap[it.orderId] = summary }
            isErrorBanner = false
            statusBanner = "Parcel photo attached — Ready for payment"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        // Immediately clear any prior permission prompt from the screen
        statusBanner = null
        isErrorBanner = false
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            val fallbackSummary = "Parcel Photo Attached"
            currentBookingPhotoSummary = fallbackSummary
            isErrorBanner = false
            statusBanner = "Parcel photo attached"
        }
    }

    val applyLiveGpsCoordinates: (String, Double, Double) -> Unit = { target, lat, lng ->
        if (target == "DROP") {
            dropLat = lat
            dropLng = lng
            dropLiveGpsVerified = true
            isCapturingDropLocation = true
            scope.launch {
                val geocoded = DeliveryProofAndGpsManager.reverseGeocodeDetailed(context, lat, lng)
                isCapturingDropLocation = false
                if (geocoded != null) {
                    if (geocoded.houseFlat.isNotBlank()) {
                        dropHouseFlat = geocoded.houseFlat
                    }
                    if (geocoded.building.isNotBlank()) {
                        dropBuilding = geocoded.building
                    }
                    if (geocoded.areaStreet.isNotBlank()) {
                        dropAddressText = geocoded.areaStreet
                    }
                    if (geocoded.pinCode.isNotBlank()) {
                        dropPinCode = geocoded.pinCode
                    }
                    if (geocoded.landmark.isNotBlank()) {
                        dropLandmark = geocoded.landmark
                    }
                    isErrorBanner = false
                    statusBanner = "📍 Exact drop current address captured!"
                } else {
                    isErrorBanner = false
                    statusBanner = "📍 Drop GPS locked! Please fill your House / Building details below."
                }
            }
        } else {
            pickupLat = lat
            pickupLng = lng
            pickupLiveGpsVerified = true
            isCapturingPickupLocation = true
            scope.launch {
                val geocoded = DeliveryProofAndGpsManager.reverseGeocodeDetailed(context, lat, lng)
                isCapturingPickupLocation = false
                if (geocoded != null) {
                    if (geocoded.houseFlat.isNotBlank()) {
                        pickupHouseFlat = geocoded.houseFlat
                    }
                    if (geocoded.building.isNotBlank()) {
                        pickupBuilding = geocoded.building
                    }
                    if (geocoded.areaStreet.isNotBlank()) {
                        pickupAddressText = geocoded.areaStreet
                    }
                    if (geocoded.pinCode.isNotBlank()) {
                        pickupPinCode = geocoded.pinCode
                    }
                    if (geocoded.landmark.isNotBlank()) {
                        pickupLandmark = geocoded.landmark
                    }
                    isErrorBanner = false
                    statusBanner = "📍 Exact current pickup address captured!"
                } else {
                    isErrorBanner = false
                    statusBanner = "📍 Pickup GPS locked! Please fill your House / Building details below."
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        statusBanner = null
        isErrorBanner = false
        if (granted) {
            if (pendingGpsTarget == "DROP") isCapturingDropLocation = true else isCapturingPickupLocation = true
            scope.launch {
                val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                if (coords != null) {
                    applyLiveGpsCoordinates(pendingGpsTarget, coords.first, coords.second)
                } else {
                    if (pendingGpsTarget == "DROP") isCapturingDropLocation = false else isCapturingPickupLocation = false
                    isErrorBanner = true
                    statusBanner = if (!DeliveryProofAndGpsManager.isLocationServiceEnabled(context)) {
                        "Location (GPS) is OFF. Please turn ON GPS to auto-capture your exact address, or enter it below."
                    } else {
                        "Unable to capture GPS fix right now. Please enable High-Accuracy Location or enter your address manually."
                    }
                }
            }
        } else {
            isErrorBanner = true
            statusBanner = "Please allow Location permission to auto-capture your exact address, or enter your address below."
        }
    }

    val effectivePricingConfig = remember(pricingConfig, remoteFlags) {
        val activeSurge = remoteFlags.effectiveSurgeMultiplier()
        pricingConfig.copy(
            surgeMultiplier = if (activeSurge > 1.0) activeSurge else pricingConfig.surgeMultiplier,
            platformCommissionPercent = remoteFlags.platformCommissionPercent.toDouble().coerceIn(0.0, 40.0)
        )
    }

    val liveDetailedFare = remember(
        pickupLat,
        pickupLng,
        dropLat,
        dropLng,
        selectedPackageType,
        packageWeightKg,
        packageDescription,
        selectedVehicle,
        effectivePricingConfig,
        vehiclePolicy
    ) {
        AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropLat = dropLat,
            dropLng = dropLng,
            weightKgInput = packageWeightKg.ifBlank { "1.0" },
            vehicleType = selectedVehicle,
            packageType = selectedPackageType,
            packageDescription = packageDescription.ifBlank { "Parcel" },
            pricingConfigOverride = effectivePricingConfig,
            policyOverride = vehiclePolicy
        )
    }

    var promoCodeInput by rememberSaveable { mutableStateOf("") }
    var appliedPromoCode by rememberSaveable { mutableStateOf("") }
    var promoFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val appliedCouponObj = remember(remoteFlags.promoCodeEnabled, adminPromoCoupons, appliedPromoCode) {
        if (!remoteFlags.promoCodeEnabled || appliedPromoCode.isBlank()) null
        else adminPromoCoupons.firstOrNull { it.code.equals(appliedPromoCode, ignoreCase = true) && it.isActive }
    }
    val promoDiscountInr = remember(remoteFlags.promoCodeEnabled, appliedCouponObj, liveDetailedFare.totalFareInr) {
        if (!remoteFlags.promoCodeEnabled) 0.0
        else appliedCouponObj?.computeDiscountInr(liveDetailedFare.totalFareInr) ?: 0.0
    }

    val extraStopAddonInr = if (remoteFlags.multiDropEnabled && isMultiDropEnabled) 40.0 else 0.0
    val insuranceAddonInr = if (remoteFlags.parcelInsuranceEnabled && isParcelInsured) remoteFlags.insuranceFeeInr.toDouble() else 0.0
    val helperAddonInr = if (remoteFlags.helperAddonEnabled && isHelperAdded) remoteFlags.helperFeeInr.toDouble() else 0.0
    val totalRemoteAddonInr = extraStopAddonInr + insuranceAddonInr + helperAddonInr
    val extraAddonSummaryLabel = remember(extraStopAddonInr, insuranceAddonInr, helperAddonInr) {
        listOfNotNull(
            if (extraStopAddonInr > 0.0) "Extra Stop" else null,
            if (insuranceAddonInr > 0.0) "Parcel Insurance" else null,
            if (helperAddonInr > 0.0) "Loading Helper" else null
        ).joinToString(" + ")
    }
    val walletDiscountInr = remember(remoteFlags.referralWalletEnabled, useWalletBonus, walletBonusInr, liveDetailedFare.totalFareInr) {
        if (remoteFlags.referralWalletEnabled && useWalletBonus && walletBonusInr > 0) {
            minOf(walletBonusInr.toDouble(), (liveDetailedFare.totalFareInr * 0.5).coerceAtLeast(10.0))
        } else 0.0
    }

    val b2bAccountsList by QuickDeliveryOperationsManager.b2bAccounts.collectAsState()
    val cleanPhoneForB2b = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(customerPhone)
    val approvedB2bAccount = remember(b2bAccountsList, cleanPhoneForB2b, remoteFlags.b2bPostpaidEnabled) {
        if (!remoteFlags.b2bPostpaidEnabled) null
        else b2bAccountsList.firstOrNull {
            it.status == com.example.data.cloud.B2bDukaanStatus.APPROVED &&
                (cleanPhoneForB2b.isBlank() || it.ownerPhone == cleanPhoneForB2b)
        }
    }
    var useB2bPostpaid by rememberSaveable { mutableStateOf(false) }

    val liveQuote = liveDetailedFare.quote
    val liveFareAmountInr = (liveDetailedFare.totalFareInr + totalRemoteAddonInr - promoDiscountInr - walletDiscountInr).coerceAtLeast(15.0)
    val totalWithTipInr = liveFareAmountInr + selectedRiderTipInr
    val isCodAllowedForOrder = remoteFlags.codEnabled && totalWithTipInr <= remoteFlags.maxCodLimitInr

    LaunchedEffect(remoteFlags, isCodAllowedForOrder, approvedB2bAccount, totalWithTipInr) {
        if (!remoteFlags.scheduledDeliveryEnabled) isScheduledDelivery = false
        if (!remoteFlags.multiDropEnabled) isMultiDropEnabled = false
        if (!remoteFlags.parcelInsuranceEnabled) isParcelInsured = false
        if (!remoteFlags.helperAddonEnabled) isHelperAdded = false
        if (!remoteFlags.referralWalletEnabled) useWalletBonus = false
        if (!remoteFlags.promoCodeEnabled) {
            appliedPromoCode = ""
            promoFeedbackMessage = null
        }
        if (!remoteFlags.b2bPostpaidEnabled || approvedB2bAccount == null || approvedB2bAccount.availableCreditInr < totalWithTipInr) {
            useB2bPostpaid = false
        }
        if (!isCodAllowedForOrder && selectedPaymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY) {
            selectedPaymentMethod = CustomerPaymentMethod.GATEWAY_UPI
        }
    }

    val formattedDistanceKm = remember(liveDetailedFare.distanceKm) {
        String.format(Locale.US, "%.1f", liveDetailedFare.distanceKm)
    }
    val estimatedTripEtaMins = remember(liveDetailedFare.distanceKm, selectedVehicle) {
        val avgSpeedKmPerHr = when (selectedVehicle) {
            VehicleType.BIKE -> 24.0
            VehicleType.SCOOTER -> 22.0
            VehicleType.AUTO -> 18.0
            VehicleType.MINI_TRUCK -> 16.0
            VehicleType.BIG_TRUCK -> 14.0
        }
        ((liveDetailedFare.distanceKm / avgSpeedKmPerHr) * 60.0).toInt().coerceIn(8, 120)
    }

    var showCustomerTopOptionsDialog by remember { mutableStateOf(false) }
    var showCustomerProfileDialog by remember { mutableStateOf(false) }
    var attemptedStep1Next by rememberSaveable { mutableStateOf(false) }
    var showCustomerLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showCustomerSupportTicketDialog by remember { mutableStateOf(false) }
    var showCustomerPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showCustomerDeleteAccountDialog by remember { mutableStateOf(false) }
    var showGstInvoiceDialogForOrder by remember { mutableStateOf<CustomerOrder?>(null) }
    var orderPendingCancellation by remember { mutableStateOf<CustomerOrder?>(null) }
    var isCustomerLoggedIn by remember { mutableStateOf(MotivationAndPromoManager.customerAuthenticatedInSession) }
    var allCustomerPermissionsGranted by remember { mutableStateOf(hasAllMandatoryAppPermissions(context)) }

    val resetCustomerTransientBookingState = {
        selectedTrackingOrderId = null
        latestBookedOrder = null
        showOrderHistoryScreen = false
        showBookingConfirmationScreen = false
        currentStep = 1
        attemptedStep1Next = false
        pickupHouseFlat = ""
        pickupBuilding = ""
        pickupAddressText = ""
        pickupPinCode = ""
        pickupLandmark = ""
        pickupLat = 0.0
        pickupLng = 0.0
        pickupLiveGpsVerified = false
        dropHouseFlat = ""
        dropBuilding = ""
        dropAddressText = ""
        dropPinCode = ""
        dropLandmark = ""
        dropLat = 0.0
        dropLng = 0.0
        dropLiveGpsVerified = false
        receiverFullName = ""
        receiverMobilePhone = ""
        receiverPhoneOverLimit = false
        packageDescription = ""
        selectedInstructionsCsv = ""
        currentBookingPhotoSummary = null
        verifiedGatewayOrder = null
        promoCodeInput = ""
        appliedPromoCode = ""
        promoFeedbackMessage = null
        selectedRiderTipInr = 0
        liveRiderGpsCoordinates = null
        useWalletBonus = false
        isScheduledDelivery = false
        isMultiDropEnabled = false
        extraStopAddress = ""
        isParcelInsured = false
        isHelperAdded = false
    }

    val validateStep1MandatoryFields: () -> String? = {
        val weightCheck = AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg(
            packageWeightKg,
            selectedVehicle
        )
        when {
            remoteFlags.emergencyBookingsPaused ->
                remoteFlags.emergencyPauseReason.ifBlank { "New bookings are temporarily paused by Operations. Please try again shortly." }
            remoteFlags.operatingHoursEnabled && !remoteFlags.isWithinOperatingHours() ->
                "Service hours are ${remoteFlags.operatingHoursLabel()}. Please book during active working hours."
            remoteFlags.isCustomerPhoneBlocked(customerPhone) || remoteFlags.isCustomerPhoneBlocked(receiverMobilePhone) ->
                "This mobile number is currently restricted from placing orders. Contact support@quickdelivery.co.in."
            !remoteFlags.isVehicleEnabled(selectedVehicle) ->
                "${selectedVehicle.title} is temporarily unavailable. Please choose another vehicle."
            pickupHouseFlat.isBlank() -> "Pickup House / Flat No. (*) is mandatory"
            pickupBuilding.isBlank() -> "Pickup Building / Society (*) is mandatory"
            pickupAddressText.isBlank() -> "Pickup Area / Locality / Street (*) is mandatory"
            pickupPinCode.length != 6 -> "Please enter a valid 6-digit Pickup PIN Code (*)"
            !remoteFlags.isPinCodeAllowed(pickupPinCode) -> "Pickup PIN $pickupPinCode is currently outside Admin serviceable PIN zones."
            receiverFullName.isBlank() -> "Receiver Name (*) is mandatory"
            receiverMobilePhone.length != 10 || com.example.security.QuickDeliverySecurityGuard.isFakeOrDummyPhone(receiverMobilePhone) ->
                "Please enter a valid 10-digit Receiver Mobile Number (+91) (*)"
            dropHouseFlat.isBlank() -> "Drop House / Flat No. (*) is mandatory"
            dropBuilding.isBlank() -> "Drop Building / Society (*) is mandatory"
            dropAddressText.isBlank() -> "Drop Area / Locality / Street (*) is mandatory"
            dropPinCode.length != 6 -> "Please enter a valid 6-digit Drop PIN Code (*)"
            !remoteFlags.isPinCodeAllowed(dropPinCode) -> "Drop PIN $dropPinCode is currently outside Admin serviceable PIN zones."
            remoteFlags.multiDropEnabled && isMultiDropEnabled && extraStopAddress.isBlank() -> "Please enter Extra Stop address or turn off Multi-Drop."
            weightCheck.isFailure -> weightCheck.exceptionOrNull()?.message
                ?: AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_ERROR_MESSAGE
            packageDescription.isBlank() -> "Parcel Note / Item Description (*) is mandatory"
            else -> null
        }
    }

    val hasCustomerBackHistory = orderPendingCancellation != null ||
        showGstInvoiceDialogForOrder != null ||
        showCustomerDeleteAccountDialog ||
        showCustomerPrivacyPolicyDialog ||
        showCustomerSupportTicketDialog ||
        showCustomerLogoutConfirmDialog ||
        showCustomerProfileDialog ||
        showCustomerTopOptionsDialog ||
        showBookingConfirmationScreen ||
        showOrderHistoryScreen ||
        showOnlinePaymentOptions ||
        currentStep > 1

    BackHandler(enabled = hasCustomerBackHistory) {
        when {
            orderPendingCancellation != null -> orderPendingCancellation = null
            showGstInvoiceDialogForOrder != null -> showGstInvoiceDialogForOrder = null
            showCustomerDeleteAccountDialog -> showCustomerDeleteAccountDialog = false
            showCustomerPrivacyPolicyDialog -> showCustomerPrivacyPolicyDialog = false
            showCustomerSupportTicketDialog -> showCustomerSupportTicketDialog = false
            showCustomerLogoutConfirmDialog -> showCustomerLogoutConfirmDialog = false
            showCustomerProfileDialog -> showCustomerProfileDialog = false
            showCustomerTopOptionsDialog -> showCustomerTopOptionsDialog = false
            showBookingConfirmationScreen -> showBookingConfirmationScreen = false
            showOrderHistoryScreen -> showOrderHistoryScreen = false
            showOnlinePaymentOptions -> showOnlinePaymentOptions = false
            currentStep > 1 -> currentStep -= 1
        }
    }

    // 1. Show Speeding Vehicle Animation first when Customer App opens
    if (showSpeedIntro) {
        QuickDeliverySpeedingVehicleIntro(
            appTitle = "Quick Delivery",
            appSubtitle = "FAST. SIMPLE. SECURE. • Superfast Parcel Delivery",
            accentColor = BrightTeal,
            onAnimationFinished = {
                MotivationAndPromoManager.customerIntroCompleted = true
                showSpeedIntro = false
            }
        )
        return@LocalizedContextProvider
    }

    if (!isCustomerLoggedIn) {
        QuickDeliveryUserAuthScreen(
            roleTitle = "Customer",
            accentColor = PrimaryTeal,
            prefsKeyPrefix = "customer",
            context = context,
            onAuthenticated = { name, phone, email ->
                val prevCleanPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(customerPhone)
                val nextCleanPhone = com.example.security.QuickDeliverySecurityGuard.sanitizePhoneNumber(phone)
                val accountChanged = (nextCleanPhone != prevCleanPhone) ||
                    (prevCleanPhone.isBlank() && !email.equals(customerEmail, ignoreCase = true))
                if (accountChanged) {
                    resetCustomerTransientBookingState()
                }
                customerName = name
                customerPhone = phone
                customerEmail = email
                QuickDeliveryOperationsManager.setActiveCustomerAccount(context, phone, email)
                MotivationAndPromoManager.customerAuthenticatedInSession = true
                isCustomerLoggedIn = true
                allCustomerPermissionsGranted = hasAllMandatoryAppPermissions(context)
                statusBanner = "Welcome, $name! Plan your delivery."
            }
        )
        return@LocalizedContextProvider
    }

    if (!allCustomerPermissionsGranted) {
        QuickDeliveryMandatoryPermissionsGateScreen(
            roleTitle = "Customer",
            prefsKeyPrefix = "customer",
            accentColor = PrimaryTeal,
            onAllPermissionsGranted = {
                allCustomerPermissionsGranted = true
            },
            onLogout = {
                MotivationAndPromoManager.logoutCustomer(context)
                resetCustomerTransientBookingState()
                isCustomerLoggedIn = false
            }
        )
        return@LocalizedContextProvider
    }

    // 3. Dedicated Customer Order History Screen (Past & Active Deliveries scoped to this Customer account)
    if (showOrderHistoryScreen) {
        CustomerOrderHistoryScreen(
            customerName = customerName,
            orders = customerOrders,
            onBack = { showOrderHistoryScreen = false },
            onSelectOrderToTrack = { selectedOrder ->
                selectedTrackingOrderId = selectedOrder.orderId
                showOrderHistoryScreen = false
                statusBanner = null
                currentStep = 3
            },
            onReorderRoute = { pastOrder ->
                pickupAddressText = pastOrder.pickupAddress.fullAddress
                pickupLandmark = pastOrder.pickupAddress.landmark
                pickupLat = pastOrder.pickupAddress.lat
                pickupLng = pastOrder.pickupAddress.lng
                pickupLiveGpsVerified = pastOrder.pickupAddress.lat != 0.0 && pastOrder.pickupAddress.lng != 0.0
                dropAddressText = pastOrder.dropAddress.fullAddress
                dropLandmark = pastOrder.dropAddress.landmark
                dropLat = pastOrder.dropAddress.lat
                dropLng = pastOrder.dropAddress.lng
                dropLiveGpsVerified = pastOrder.dropAddress.lat != 0.0 && pastOrder.dropAddress.lng != 0.0
                selectedVehicle = pastOrder.vehicleType
                selectedPackageType = pastOrder.packageDetails.packageType
                packageWeightKg = pastOrder.packageDetails.weightKg
                packageDescription = pastOrder.packageDetails.description
                showOrderHistoryScreen = false
                currentStep = 1
                isErrorBanner = false
                statusBanner = "Route loaded from Order #${pastOrder.orderId} — Review and book!"
            },
            modifier = modifier
        )
        return@LocalizedContextProvider
    }

    val executeCustomerBooking: () -> Unit = executeBooking@{
        runCatching {
            val weightBackendCheck = AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg(
                packageWeightKg,
                selectedVehicle
            )
            if (weightBackendCheck.isFailure) {
                isErrorBanner = true
                statusBanner = weightBackendCheck.exceptionOrNull()?.message
                    ?: AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_ERROR_MESSAGE
                showBookingConfirmationScreen = false
                currentStep = 1
                return@executeBooking
            }

            var gatewayOrderToUse = verifiedGatewayOrder
            if (selectedPaymentMethod.isCompanyCollectedGateway &&
                gatewayOrderToUse?.status != GatewayCallbackStatus.SUCCESS
            ) {
                val instrumentLabel = when (selectedPaymentMethod) {
                    CustomerPaymentMethod.GATEWAY_UPI -> selectedUpiApp.displayName
                    CustomerPaymentMethod.GATEWAY_CREDIT_CARD,
                    CustomerPaymentMethod.GATEWAY_DEBIT_CARD ->
                        cardOrBankDetails.ifBlank { "Card Payment" }
                    CustomerPaymentMethod.GATEWAY_NET_BANKING ->
                        cardOrBankDetails.ifBlank { "NetBanking" }
                    else -> "COD"
                }
                val initiatedGw = repository.initiateGatewayPaymentOrder(
                    purpose = "CUSTOMER_ORDER",
                    referenceId = "PREBOOK-${System.currentTimeMillis()}",
                    amountInr = liveFareAmountInr,
                    paymentMethod = selectedPaymentMethod,
                    selectedUpiApp = if (selectedPaymentMethod == CustomerPaymentMethod.GATEWAY_UPI) {
                        selectedUpiApp
                    } else {
                        null
                    },
                    instrumentDetailsLabel = instrumentLabel
                ).getOrNull()
                if (initiatedGw != null) {
                    val payId = "pay_${System.currentTimeMillis()}"
                    val validSig = repository.signGatewayCallback(
                        gatewayOrderId = initiatedGw.gatewayOrderId,
                        gatewayPaymentId = payId,
                        callbackStatus = GatewayCallbackStatus.SUCCESS
                    ).getOrNull()
                    if (validSig != null) {
                        gatewayOrderToUse = repository.verifyAndProcessGatewayCallback(
                            gatewayOrderId = initiatedGw.gatewayOrderId,
                            gatewayPaymentId = payId,
                            callbackStatus = GatewayCallbackStatus.SUCCESS,
                            providedSignature = validSig
                        ).getOrNull()
                        verifiedGatewayOrder = gatewayOrderToUse
                    }
                }
            }

            val resolvedPickupAddress = fullPickupAddressFormatted.ifBlank {
                pickupAddressText.ifBlank { "Pickup Location" }
            }
            val resolvedDropAddress = fullDropAddressFormatted.ifBlank {
                dropAddressText.ifBlank { "Drop Location" }
            }

            val enrichedDescription = buildString {
                append(packageDescription.ifBlank { "Express Parcel" })
                if (selectedInstructionsCsv.isNotBlank()) {
                    append(" [Instructions: ${selectedInstructionsCsv.trim()}]")
                }
                if (remoteFlags.scheduledDeliveryEnabled && isScheduledDelivery) {
                    append(" [Scheduled: $scheduledDateLabel $scheduledTimeSlot]")
                }
                if (remoteFlags.multiDropEnabled && isMultiDropEnabled && extraStopAddress.isNotBlank()) {
                    append(" [Extra Stop: ${extraStopAddress.trim()}]")
                }
                if (remoteFlags.parcelInsuranceEnabled && isParcelInsured) {
                    append(" [Insured ₹5,000]")
                }
                if (remoteFlags.helperAddonEnabled && isHelperAdded) {
                    append(" [Helper Needed]")
                }
                if (remoteFlags.b2bPostpaidEnabled && useB2bPostpaid && approvedB2bAccount != null) {
                    append(" [B2B Postpaid: ${approvedB2bAccount.accountId} | GSTIN: ${approvedB2bAccount.gstin}]")
                }
            }

            val bookingResult = AutomatedFairRiderAssignmentEngine.createCustomerOrderWithBackendValidation(
                repository = repository,
                customerId = myCustomerId,
                customerName = customerName.ifBlank { "Customer" },
                customerPhone = normalizedCustomerPhone,
                pickupAddress = AddressDetails(
                    fullAddress = resolvedPickupAddress,
                    landmark = pickupLandmark,
                    contactName = customerName.ifBlank { "Customer" },
                    contactPhone = normalizedCustomerPhone,
                    lat = pickupLat,
                    lng = pickupLng
                ),
                dropAddress = AddressDetails(
                    fullAddress = resolvedDropAddress,
                    landmark = dropLandmark,
                    contactName = receiverFullName.ifBlank { "Receiver" },
                    contactPhone = receiverMobilePhone.trim(),
                    lat = dropLat,
                    lng = dropLng
                ),
                packageDetails = PackageDetails(
                    packageType = selectedPackageType,
                    weightKg = packageWeightKg.ifBlank { "1.0" },
                    description = enrichedDescription
                ),
                vehicleType = selectedVehicle,
                paymentMethod = selectedPaymentMethod,
                selectedUpiApp = if (selectedPaymentMethod == CustomerPaymentMethod.GATEWAY_UPI) {
                    selectedUpiApp
                } else {
                    null
                },
                verifiedGatewayOrderId = gatewayOrderToUse?.gatewayOrderId
            )

            bookingResult.fold(
                onSuccess = { createdOrder ->
                    var newOrder = createdOrder
                    if (newOrder.paymentMethod.isCompanyCollectedGateway &&
                        newOrder.paymentStatus != com.example.data.model.PaymentVerificationStatus.VERIFIED_PAID_TO_COMPANY &&
                        gatewayOrderToUse?.status == GatewayCallbackStatus.SUCCESS
                    ) {
                        val exactOrderFareInr = newOrder.totalAmount
                            .replace("₹", "")
                            .replace(",", "")
                            .trim()
                            .toDoubleOrNull()
                            ?.takeIf { it > 0.0 }
                            ?: liveFareAmountInr
                        val boundGw = repository.initiateGatewayPaymentOrder(
                            purpose = "CUSTOMER_ORDER",
                            referenceId = newOrder.orderId,
                            amountInr = exactOrderFareInr,
                            paymentMethod = newOrder.paymentMethod,
                            selectedUpiApp = if (newOrder.paymentMethod == CustomerPaymentMethod.GATEWAY_UPI) {
                                selectedUpiApp
                            } else {
                                null
                            },
                            instrumentDetailsLabel = gatewayOrderToUse?.instrumentDetailsLabel
                                ?: newOrder.paymentMethod.shortBadge
                        ).getOrNull()
                        if (boundGw != null) {
                            val payId = gatewayOrderToUse?.gatewayPaymentId?.takeIf { it.isNotBlank() }
                                ?: "pay_${System.currentTimeMillis()}"
                            val sig = repository.signGatewayCallback(
                                gatewayOrderId = boundGw.gatewayOrderId,
                                gatewayPaymentId = payId,
                                callbackStatus = GatewayCallbackStatus.SUCCESS
                            ).getOrNull()
                            if (sig != null) {
                                repository.verifyAndProcessGatewayCallback(
                                    gatewayOrderId = boundGw.gatewayOrderId,
                                    gatewayPaymentId = payId,
                                    callbackStatus = GatewayCallbackStatus.SUCCESS,
                                    providedSignature = sig
                                )
                                repository.orders.value.firstOrNull { it.orderId == newOrder.orderId }?.let { verified ->
                                    newOrder = verified
                                }
                            }
                        }
                    }
                    // Auto-save real Pickup & Drop addresses under the chosen Home/Office/Shop label for 1-tap future bookings
                    if (pickupAddressText.isNotBlank() && pickupPinCode.length == 6) {
                        runCatching {
                            QuickDeliveryOperationsManager.saveCustomerAddress(
                                context = context,
                                label = selectedPickupSavedTag.ifBlank { "Home" },
                                houseFlat = pickupHouseFlat,
                                building = pickupBuilding,
                                areaStreet = pickupAddressText,
                                pinCode = pickupPinCode,
                                landmark = pickupLandmark,
                                lat = pickupLat,
                                lng = pickupLng
                            )
                        }
                    }
                    if (dropAddressText.isNotBlank() && dropPinCode.length == 6) {
                        runCatching {
                            QuickDeliveryOperationsManager.saveCustomerAddress(
                                context = context,
                                label = selectedDropSavedTag.ifBlank { "Office" },
                                houseFlat = dropHouseFlat,
                                building = dropBuilding,
                                areaStreet = dropAddressText,
                                pinCode = dropPinCode,
                                landmark = dropLandmark,
                                lat = dropLat,
                                lng = dropLng
                            )
                        }
                    }
                    if (walletDiscountInr > 0.0) {
                        QuickDeliveryOperationsManager.consumeWalletBonus(context, walletDiscountInr.toInt())
                    }
                    if (remoteFlags.b2bPostpaidEnabled && useB2bPostpaid && approvedB2bAccount != null) {
                        QuickDeliveryOperationsManager.chargeOrderToB2bPostpaid(
                            context = context,
                            accountId = approvedB2bAccount.accountId,
                            amountInr = totalWithTipInr
                        )
                        if (com.example.data.cloud.QuickDeliveryFirebaseInitializer.ensureInitialized(context) != null &&
                            runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull() != null
                        ) {
                            scope.launch {
                                runCatching {
                                    com.example.data.cloud.QuickDeliveryCloudRepository(context)
                                        .chargeB2bPostpaidOrder(approvedB2bAccount.accountId, kotlin.math.round(totalWithTipInr * 100.0).toLong())
                                }
                            }
                        }
                    }
                    latestBookedOrder = newOrder
                    selectedTrackingOrderId = newOrder.orderId
                    currentBookingPhotoSummary?.let {
                        orderPhotoProofMap[newOrder.orderId] = it
                    }
                    if (selectedRiderTipInr > 0) {
                        RiderFeedbackManager.recordOrderTip(
                            context = context,
                            orderId = newOrder.orderId,
                            tipAmountInr = selectedRiderTipInr
                        )
                    }
                    // Keep order in SEARCHING_RIDER until a Rider Partner manually taps Accept in the Rider App
                    com.example.security.OrderAlertSoundManager.stopOrderAlert()
                    com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                    val refreshedOrder = repository.orders.value.firstOrNull { it.orderId == newOrder.orderId }
                    if (refreshedOrder != null) {
                        latestBookedOrder = refreshedOrder
                    }
                    isErrorBanner = false
                    val tipNote = if (selectedRiderTipInr > 0) " + ₹$selectedRiderTipInr Rider Tip" else ""
                    val promoNote = if (promoDiscountInr > 0.0 && appliedPromoCode.isNotBlank()) " ($appliedPromoCode -₹${promoDiscountInr.toInt()})" else ""
                    statusBanner = "Order #${newOrder.orderId} Confirmed$tipNote$promoNote! Waiting for Rider Partner to Accept."
                    showBookingConfirmationScreen = false
                    currentStep = 3
                },
                onFailure = { err ->
                    isErrorBanner = true
                    statusBanner = err.message ?: "Unable to book order"
                    showBookingConfirmationScreen = false
                }
            )
        }.onFailure { unexpectedErr ->
            isErrorBanner = true
            statusBanner = unexpectedErr.message ?: "Error while confirming order. Please check address details."
            showBookingConfirmationScreen = false
        }
    }

    // Dedicated Booking Confirmation Screen
    if (showBookingConfirmationScreen) {
        CustomerBookingConfirmationScreen(
            customerName = customerName,
            customerPhone = customerPhone,
            receiverName = receiverFullName,
            receiverPhone = receiverMobilePhone,
            pickupHouseFlat = pickupHouseFlat,
            pickupBuilding = pickupBuilding,
            pickupArea = pickupAddressText,
            pickupPinCode = pickupPinCode,
            pickupLandmark = pickupLandmark,
            pickupFullAddress = fullPickupAddressFormatted,
            dropHouseFlat = dropHouseFlat,
            dropBuilding = dropBuilding,
            dropArea = dropAddressText,
            dropPinCode = dropPinCode,
            dropLandmark = dropLandmark,
            dropFullAddress = fullDropAddressFormatted,
            selectedVehicle = selectedVehicle,
            packageWeightKg = packageWeightKg,
            selectedPackageType = selectedPackageType,
            packageDescription = packageDescription,
            hasPhotoProof = currentBookingPhotoSummary != null,
            distanceKm = liveDetailedFare.distanceKm,
            detailedFare = liveDetailedFare,
            tipInr = selectedRiderTipInr,
            selectedPaymentMethod = selectedPaymentMethod,
            selectedUpiApp = selectedUpiApp,
            appliedPromoCode = appliedPromoCode,
            promoDiscountInr = promoDiscountInr,
            extraAddonInr = totalRemoteAddonInr,
            extraAddonLabel = extraAddonSummaryLabel,
            walletDiscountInr = walletDiscountInr,
            isB2bPostpaidSelected = remoteFlags.b2bPostpaidEnabled && useB2bPostpaid && approvedB2bAccount != null,
            b2bShopName = approvedB2bAccount?.shopName.orEmpty(),
            onBack = { showBookingConfirmationScreen = false },
            onConfirmBooking = executeCustomerBooking,
            modifier = modifier
        )
        return@LocalizedContextProvider
    }

    LaunchedEffect(pendingNotificationTap?.eventSeq) {
        val tap = pendingNotificationTap ?: return@LaunchedEffect
        val tapOrderId = tap.orderId.trim()
        showBookingConfirmationScreen = false
        showOrderHistoryScreen = false
        showCustomerTopOptionsDialog = false
        showCustomerSupportTicketDialog = false
        val matchedOrder = if (tapOrderId.isNotBlank()) {
            customerOrders.firstOrNull { it.orderId.equals(tapOrderId, ignoreCase = true) }
        } else null
        if (matchedOrder != null) {
            selectedTrackingOrderId = matchedOrder.orderId
            currentStep = 3
        } else if (activeOrder != null) {
            currentStep = 3
        } else {
            currentStep = 1
        }
        MultiPhoneLiveSyncManager.triggerImmediateSync(context)
        isErrorBanner = false
        statusBanner = when {
            tap.title.isNotBlank() -> tap.title
            tapOrderId.isNotBlank() -> "📦 Opened live tracking for Order #$tapOrderId"
            else -> "🔔 Notification opened"
        }
        runCatching { customerMainListState.animateScrollToItem(0) }
        FcmOrderNotificationManager.consumeNotificationTap(tap.eventSeq)
    }

    if (showCustomerTopOptionsDialog) {
        QuickDeliveryTopOptionsDialog(
            onDismiss = { showCustomerTopOptionsDialog = false },
            showMobileCloudApkCard = false,
            dialogTitle = uiStrings.menuAndLanguageTitle,
            extraTopContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // TOP PROFILE & PHOTO (DP) CARD — Placed right at the very top of the 3-lines menu
                    Text(
                        text = uiStrings.sectionProfile,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal
                    )
                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            showCustomerProfileDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = SoftTealSurface,
                        border = BorderStroke(1.5.dp, PrimaryTeal.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_verified_profile_summary_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(50.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryTeal.copy(alpha = 0.16f),
                                    border = BorderStroke(2.dp, BrandOrange),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val dpBmp = customerProfileBitmap
                                    if (dpBmp != null) {
                                        Image(
                                            bitmap = dpBmp.asImageBitmap(),
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
                                                contentDescription = "Customer Profile",
                                                tint = PrimaryTeal,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    color = BrandOrange,
                                    shape = CircleShape,
                                    border = BorderStroke(1.5.dp, Color.White),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Add or Change Profile Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customerName.ifBlank { "My Profile" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepNavy
                                )
                                val cleanContactSummary = listOfNotNull(
                                    customerPhone.removePrefix("+91").trim().takeIf { it.isNotBlank() }?.let { "+91 $it" },
                                    customerEmail.trim().takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                if (cleanContactSummary.isNotBlank()) {
                                    Text(
                                        text = cleanContactSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    color = BrandOrange,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (customerProfileBitmap != null) "📷 Change Photo (DP)" else "📷 Add Profile Photo (DP)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // SECTION 1: HOME & BOOKING / ORDERS
                    Text(
                        text = uiStrings.sectionHomeBooking,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateMuted
                    )
                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            currentStep = 1
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryTeal.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_menu_home_booking_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryTeal,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Book Parcel",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiStrings.bookParcelTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = uiStrings.bookParcelSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // SECTION 2: TRACKING & DELIVERY
                    Text(
                        text = uiStrings.sectionTrackingDelivery,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateMuted
                    )
                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            if (activeOrder != null) {
                                currentStep = 3
                            } else {
                                statusBanner = "Book an order first to view live GPS tracking."
                                currentStep = 1
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_menu_live_tracking_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BrandOrange.copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Live Tracking",
                                        tint = BrandOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiStrings.liveOrderTrackingTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = if (activeOrder != null) "${uiStrings.activeOrderLabel} #${activeOrder.orderId}" else uiStrings.liveOrderTrackingSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // SECTION 3: HISTORY
                    Text(
                        text = uiStrings.sectionHistory,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateMuted
                    )
                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            showOrderHistoryScreen = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_open_order_history_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryTeal.copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Order History",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiStrings.orderHistoryReceiptsTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "${customerOrders.size} ${uiStrings.deliveriesRecordedSuffix}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // SECTION 5: NOTIFICATIONS & SUPPORT
                    Text(
                        text = uiStrings.sectionNotificationsSupport,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateMuted
                    )
                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            showCustomerSupportTicketDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_menu_support_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryTeal.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = uiStrings.helpSupportDeskTitle,
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiStrings.helpSupportDeskTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = uiStrings.helpSupportDeskSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    // SECTION 6: SETTINGS, PRIVACY & ACCOUNT
                    Text(
                        text = uiStrings.sectionSettingsPrivacy,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateMuted
                    )
                    InlineLanguageSelectorBar(
                        accentColor = PrimaryTeal,
                        testTagPrefix = "customer_lang_chip"
                    )

                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            showCustomerPrivacyPolicyDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_menu_privacy_policy_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryTeal.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = uiStrings.privacyPolicyTermsTitle,
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiStrings.privacyPolicyTermsTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = uiStrings.privacyPolicyTermsSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            showCustomerLogoutConfirmDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_logout_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Logout",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiStrings.logoutTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = uiStrings.logoutConfirmTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            showCustomerDeleteAccountDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_delete_account_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFBE123C).copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = uiStrings.deleteAccountTitle,
                                        tint = Color(0xFFBE123C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiStrings.deleteAccountTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9F1239)
                                )
                                Text(
                                    text = uiStrings.deleteAccountSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF881337)
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (showCustomerLogoutConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomerLogoutConfirmDialog = false },
            title = {
                Text(
                    text = uiStrings.logoutConfirmTitle,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Text(
                    text = uiStrings.logoutConfirmMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCustomerLogoutConfirmDialog = false
                        MotivationAndPromoManager.logoutCustomer(context)
                        resetCustomerTransientBookingState()
                        isCustomerLoggedIn = false
                        statusBanner = "${uiStrings.logoutTitle} ✓"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_customer_logout_btn")
                ) {
                    Text(uiStrings.logoutTitle, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomerLogoutConfirmDialog = false }
                ) {
                    Text(uiStrings.cancelBtn)
                }
            }
        )
    }

    if (showCustomerProfileDialog) {
        CustomerProfileDpDialog(
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            accentColor = PrimaryTeal,
            secondaryColor = BrandOrange,
            onDismiss = { showCustomerProfileDialog = false }
        )
    }

    if (showCustomerSupportTicketDialog) {
        var ticketCategory by remember { mutableStateOf("Order Delay") }
        var ticketOrderId by remember {
            mutableStateOf(customerOrders.firstOrNull { it.orderStatus != OrderStatus.CANCELLED }?.orderId.orEmpty())
        }
        var ticketMessage by remember { mutableStateOf("") }
        val myTickets = remember(customerSupportTickets, customerPhone, customerName) {
            customerSupportTickets.filter { t ->
                t.senderRole == "CUSTOMER" &&
                    (customerPhone.isBlank() || t.senderPhone == customerPhone || t.senderName == customerName)
            }
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomerSupportTicketDialog = false },
            title = {
                Text(
                    text = "Help & Support Desk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Send an issue directly to Quick Delivery Ops or email ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Order Delay", "Address / OTP", "Payment / Refund", "Damaged Item").forEach { cat ->
                            FilterChip(
                                selected = ticketCategory == cat,
                                onClick = { ticketCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = ticketOrderId,
                        onValueChange = { ticketOrderId = it },
                        label = { Text("Order ID (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ticketMessage,
                        onValueChange = { ticketMessage = it },
                        label = { Text("Describe your issue *") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_support_ticket_input")
                    )
                    Button(
                        onClick = {
                            val created = QuickDeliveryOperationsManager.submitSupportTicket(
                                context = context,
                                senderRole = "CUSTOMER",
                                senderName = customerName.ifBlank { "Customer" },
                                senderPhone = customerPhone,
                                orderId = ticketOrderId,
                                category = ticketCategory,
                                message = ticketMessage
                            )
                            if (created != null) {
                                com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                ticketMessage = ""
                                isErrorBanner = false
                                statusBanner = "✅ Ticket ${created.ticketId} sent to Admin Desk & queued for ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}."
                            }
                        },
                        enabled = ticketMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_submit_support_ticket_btn")
                    ) {
                        Text("Submit Ticket (Admin + Mail Alert)", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val mailSubject = "[Quick Delivery Help] $ticketCategory${if (ticketOrderId.isNotBlank()) " • Order #$ticketOrderId" else ""}"
                            val mailBody = buildString {
                                appendLine("Customer Name: ${customerName.ifBlank { "Customer" }}")
                                appendLine("Mobile: ${customerPhone.ifBlank { "Not provided" }}")
                                if (ticketOrderId.isNotBlank()) appendLine("Order ID: #$ticketOrderId")
                                appendLine("Category: $ticketCategory")
                                appendLine()
                                appendLine("Issue Details:")
                                appendLine(ticketMessage.ifBlank { "(Describe your issue here)" })
                            }
                            runCatching {
                                context.startActivity(
                                    com.example.data.cloud.QuickDeliveryBrandLinks.buildSupportMailtoIntent(
                                        subject = mailSubject,
                                        body = mailBody
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_email_support_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Support",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Email ${com.example.data.cloud.QuickDeliveryBrandLinks.SUPPORT_EMAIL}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                    if (myTickets.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Your Tickets (${myTickets.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        myTickets.take(5).forEach { ticket ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (ticket.isResolved) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${ticket.ticketId} • ${ticket.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy
                                        )
                                        Text(
                                            text = if (ticket.isResolved) "RESOLVED" else "OPEN",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (ticket.isResolved) VerifiedGreen else BrandOrange
                                        )
                                    }
                                    Text(
                                        text = ticket.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF334155)
                                    )
                                    if (ticket.adminReply.isNotBlank()) {
                                        Text(
                                            text = "Admin Reply: ${ticket.adminReply}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VerifiedGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showCustomerSupportTicketDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCustomerPrivacyPolicyDialog) {
        QuickDeliveryPrivacyPolicyDialog(
            context = context,
            onDismiss = { showCustomerPrivacyPolicyDialog = false }
        )
    }

    if (showCustomerDeleteAccountDialog) {
        QuickDeliveryDeleteAccountDialog(
            roleTitle = "Customer",
            accountIdentifier = customerEmail.ifBlank { customerPhone.ifBlank { customerName } },
            onConfirmDelete = {
                showCustomerDeleteAccountDialog = false
                CustomerProfilePhotoManager.clearProfilePhoto(context)
                MotivationAndPromoManager.deleteUserAccountData(context, "customer")
                resetCustomerTransientBookingState()
                isCustomerLoggedIn = false
                statusBanner = "Your customer account data has been deleted from this device."
            },
            onDismiss = { showCustomerDeleteAccountDialog = false }
        )
    }

    showGstInvoiceDialogForOrder?.let { invOrder ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGstInvoiceDialogForOrder = null },
            title = {
                Text(
                    text = "Tax Invoice • #${invOrder.orderId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Delivery Logistics",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = "Date: ${invOrder.createdAt.ifBlank { "Live Order" }} • Status: ${invOrder.orderStatus.displayTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    HorizontalDivider()
                    Text(
                        text = "Customer: ${invOrder.customerName} (${invOrder.customerPhone})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepNavy
                    )
                    Text(
                        text = "Pickup: ${invOrder.pickupAddress.fullAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Drop: ${invOrder.dropAddress.fullAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )
                    HorizontalDivider()
                    Text(
                        text = "Vehicle: ${invOrder.vehicleType.title} • Package: ${invOrder.packageDetails.packageType.title} (${invOrder.packageDetails.weightKg} kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepNavy
                    )
                    Text(
                        text = "Payment Mode: ${invOrder.paymentMethod.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    Surface(
                        color = SoftTealSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Paid / Payable (incl. GST)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = invOrder.totalAmount,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryTeal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tipAmt = RiderFeedbackManager.tipsByOrder.value[invOrder.orderId] ?: 0
                        com.example.security.QuickDeliveryPdfGenerator.generateAndShareCustomerReceiptPdf(
                            context = context,
                            order = invOrder,
                            pickupProof = com.example.data.model.OrderProofRegistry.getPickupProof(invOrder.orderId),
                            deliveryProof = com.example.data.model.OrderProofRegistry.getDeliveryProof(invOrder.orderId),
                            tipAmountInr = tipAmt
                        ).onFailure {
                            isErrorBanner = true
                            statusBanner = "Unable to export PDF receipt right now."
                        }
                        showGstInvoiceDialogForOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    modifier = Modifier.testTag("customer_share_pdf_receipt_btn")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download PDF Receipt", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGstInvoiceDialogForOrder = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (customLabelDialogSide != null) {
        val side = customLabelDialogSide!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { customLabelDialogSide = null },
            title = {
                Text(
                    text = "Save Custom ${if (side == "PICKUP") "Pickup" else "Drop"} Label",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a nickname (e.g., Warehouse, Godown, Mom's Home):",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    OutlinedTextField(
                        value = customLabelDraft,
                        onValueChange = { if (it.length <= 20) customLabelDraft = it },
                        label = { Text("Custom Place Nickname") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_address_label_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = customLabelDraft.trim().length >= 2,
                    onClick = {
                        val label = customLabelDraft.trim()
                        if (side == "PICKUP") {
                            selectedPickupSavedTag = label
                            if (pickupAddressText.isNotBlank()) {
                                QuickDeliveryOperationsManager.saveCustomerAddress(
                                    context = context,
                                    label = label,
                                    houseFlat = pickupHouseFlat,
                                    building = pickupBuilding,
                                    areaStreet = pickupAddressText,
                                    pinCode = pickupPinCode,
                                    landmark = pickupLandmark,
                                    lat = pickupLat,
                                    lng = pickupLng
                                )
                            }
                        } else {
                            selectedDropSavedTag = label
                            if (dropAddressText.isNotBlank()) {
                                QuickDeliveryOperationsManager.saveCustomerAddress(
                                    context = context,
                                    label = label,
                                    houseFlat = dropHouseFlat,
                                    building = dropBuilding,
                                    areaStreet = dropAddressText,
                                    pinCode = dropPinCode,
                                    landmark = dropLandmark,
                                    lat = dropLat,
                                    lng = dropLng
                                )
                            }
                        }
                        isErrorBanner = false
                        statusBanner = "📍 Custom label \"$label\" ready!"
                        customLabelDraft = ""
                        customLabelDialogSide = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    modifier = Modifier.testTag("confirm_custom_address_label_btn")
                ) {
                    Text("Save Label", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { customLabelDialogSide = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    orderPendingCancellation?.let { cancelTarget ->
        var selectedCancelReason by remember(cancelTarget.orderId) {
            mutableStateOf("Rider taking too long")
        }
        val cancelReasons = listOf(
            "Rider taking too long",
            "Entered wrong address",
            "Booked by mistake",
            "Changed delivery plan"
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { orderPendingCancellation = null },
            title = {
                Text(
                    text = "Cancel Order #${cancelTarget.orderId}?",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    val paidTotalInr = cancelTarget.totalAmount.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.toInt() ?: 0
                    val isAssignedStage = cancelTarget.orderStatus == OrderStatus.RIDER_ASSIGNED ||
                        cancelTarget.orderStatus == OrderStatus.ARRIVED_AT_PICKUP
                    val cancelFeeInr = if (remoteFlags.waitTimeAndCancelRuleEnabled && isAssignedStage) {
                        remoteFlags.cancellationPenaltyInr.coerceAtLeast(0)
                    } else 0
                    val isPrepaidOrder = cancelTarget.paymentMethod != CustomerPaymentMethod.CASH_ON_DELIVERY
                    val estimatedRefundInr = if (isPrepaidOrder) (paidTotalInr - cancelFeeInr).coerceAtLeast(0) else 0

                    Text(
                        text = if (cancelTarget.orderStatus == OrderStatus.SEARCHING_RIDER) {
                            "No rider has picked up this parcel yet. Select a reason to cancel with zero fee:"
                        } else {
                            "Parcel is not picked up yet. Select a reason to cancel before rider pickup:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    Surface(
                        color = if (cancelFeeInr > 0) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (cancelFeeInr > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth().testTag("cancel_refund_breakdown_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Cancellation Fee & Refund Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (cancelFeeInr > 0) Color(0xFFB91C1C) else Color(0xFF065F46)
                            )
                            Text(
                                text = "• Order Total: ${cancelTarget.totalAmount} (${cancelTarget.paymentMethod.shortBadge})",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepNavy
                            )
                            Text(
                                text = if (cancelFeeInr > 0) "• Cancellation Charge: ₹$cancelFeeInr (Rider assigned)" else "• Cancellation Charge: ₹0 (Free cancel before dispatch)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (cancelFeeInr > 0) Color(0xFFB91C1C) else Color(0xFF059669)
                            )
                            Text(
                                text = if (isPrepaidOrder) {
                                    "• Instant Wallet / Original UPI Refund: ₹$estimatedRefundInr (within 24h)"
                                } else {
                                    "• Cash on Delivery: ₹0 charged"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy
                            )
                        }
                    }
                    cancelReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCancelReason = reason }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedCancelReason == reason,
                                onClick = { selectedCancelReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFDC2626))
                            )
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepNavy,
                                fontWeight = if (selectedCancelReason == reason) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        QuickDeliveryOperationsManager.cancelOrderWithReason(
                            context = context,
                            repository = repository,
                            orderId = cancelTarget.orderId,
                            reason = selectedCancelReason,
                            cancelledByRole = "Customer"
                        )
                        isErrorBanner = false
                        statusBanner = "Order #${cancelTarget.orderId} cancelled ($selectedCancelReason)."
                        orderPendingCancellation = null
                        currentStep = 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_cancel_order_btn")
                ) {
                    Text("Confirm Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { orderPendingCancellation = null }) {
                    Text("Keep Order")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SoftBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickDeliveryCuteBunnyLogo(size = 36.dp, borderColor = BrandOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "Quick Delivery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = uiStrings.appTagline,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD166),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCustomerTopOptionsDialog = true },
                        modifier = Modifier.testTag("customer_top_bar_three_lines_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Customer Menu & Language Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTeal)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = customerMainListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(clearFocusOnUserScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Post-Delivery Celebratory Motivational Notification Banner (sent automatically to Customer after every delivery!)
            if (!customerPostDeliveryMotivation.isNullOrBlank()) {
                item(key = "customer_post_delivery_motivation_item") {
                    DualDeliveryCelebrationMotivationBanner(
                        roleLabel = "CUSTOMER",
                        message = customerPostDeliveryMotivation,
                        accentColor = PrimaryTeal,
                        onDismiss = { MotivationAndPromoManager.dismissCustomerPostDeliveryMotivation() },
                        onClick = {
                            MotivationAndPromoManager.dismissCustomerPostDeliveryMotivation()
                            if (activeOrder != null) {
                                currentStep = 3
                            } else {
                                showOrderHistoryScreen = true
                            }
                        }
                    )
                }
            }

            // Background MultiPhoneLiveSyncManager stays active silently; technical latency strip hidden from customer UI.

            // Remote Admin Maintenance / Broadcast Announcements
            val activeZonePin = pickupPinCode.ifBlank { dropPinCode }
            val broadcastTargetOk = remoteFlags.adminBroadcastTarget == "ALL" ||
                remoteFlags.adminBroadcastTarget == "CUSTOMER" ||
                (remoteFlags.adminBroadcastTarget.startsWith("PIN:") &&
                    activeZonePin.isNotBlank() &&
                    remoteFlags.adminBroadcastTarget.removePrefix("PIN:").trim() == activeZonePin.trim())
            if (remoteFlags.emergencyBookingsPaused ||
                (remoteFlags.operatingHoursEnabled && !remoteFlags.isWithinOperatingHours())
            ) {
                item(key = "customer_service_pause_banner") {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_service_pause_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (remoteFlags.emergencyBookingsPaused) {
                                    "Service Temporarily Paused"
                                } else {
                                    "Outside Service Working Hours (${remoteFlags.operatingHoursLabel()})"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = if (remoteFlags.emergencyBookingsPaused) {
                                    remoteFlags.emergencyPauseReason.ifBlank {
                                        "New bookings are temporarily paused by Operations. Active deliveries continue normally."
                                    }
                                } else {
                                    "Bookings reopen during ${remoteFlags.operatingHoursLabel()}. Thank you for your patience."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }
            if (remoteFlags.maintenanceBannerEnabled ||
                (remoteFlags.adminBroadcastMessage.isNotBlank() && broadcastTargetOk)
            ) {
                item(key = "customer_remote_alerts_banner") {
                    CustomerRemoteAlertsBanner(
                        flags = remoteFlags,
                        activePinCode = activeZonePin,
                        onClick = {
                            if (activeOrder != null) {
                                currentStep = 3
                            } else {
                                currentStep = 1
                            }
                        }
                    )
                }
            }

            // 4-Step Animated Wizard Progress Header
            item(key = "customer_wizard_progress_header") {
                CustomerWizardProgressHeader(
                    currentStep = currentStep,
                    hasBookedOrder = activeOrder != null,
                    uiStrings = uiStrings,
                    onSelectStep = { targetStep ->
                        if (targetStep == 2) {
                            attemptedStep1Next = true
                            val validationError = validateStep1MandatoryFields()
                            if (validationError != null) {
                                isErrorBanner = true
                                statusBanner = validationError
                                currentStep = 1
                            } else {
                                statusBanner = null
                                isErrorBanner = false
                                isResolvingStep1Route = true
                                scope.launch {
                                    if (!pickupLiveGpsVerified) {
                                        val pickupQuery = listOf(pickupHouseFlat, pickupBuilding, pickupAddressText, pickupPinCode)
                                            .filter { it.isNotBlank() }
                                            .joinToString(", ")
                                        DeliveryProofAndGpsManager.forwardGeocodeAddress(context, pickupQuery)?.let { coords ->
                                            pickupLat = coords.first
                                            pickupLng = coords.second
                                        }
                                    }
                                    if (!dropLiveGpsVerified) {
                                        val dropQuery = listOf(dropHouseFlat, dropBuilding, dropAddressText, dropPinCode)
                                            .filter { it.isNotBlank() }
                                            .joinToString(", ")
                                        DeliveryProofAndGpsManager.forwardGeocodeAddress(context, dropQuery)?.let { coords ->
                                            dropLat = coords.first
                                            dropLng = coords.second
                                        }
                                    }
                                    isResolvingStep1Route = false
                                    currentStep = 2
                                }
                            }
                        } else if (targetStep != 1 && weightErrorMessage != null) {
                            isErrorBanner = true
                            statusBanner = weightErrorMessage
                            currentStep = 1
                        } else {
                            statusBanner = null
                            isErrorBanner = false
                            currentStep = targetStep
                        }
                    }
                )
            }

            // (statusBanner moved outside LazyColumn as floating overlay so showing/clearing it never shifts scroll position)

            // ACTIVE ORDER QUICK CALL & LIVE TRACKING STRIP (Always accessible on Customer Main Screen)
            if (activeOrder != null && currentStep == 1) {
                item(key = "customer_active_order_quick_call_strip") {
                    val isDeclinedReSearching = activeOrder.orderStatus == OrderStatus.SEARCHING_RIDER &&
                        (activeOrder.rejectedByRiderIds.isNotEmpty() ||
                            activeOrder.latestRiderQuickNote?.contains("Declined", ignoreCase = true) == true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDeclinedReSearching) Color(0xFFFFF7ED) else Color(0xFFF0FDFA)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isDeclinedReSearching) Color(0xFFEA580C) else PrimaryTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_main_active_order_call_card")
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
                                        text = if (isDeclinedReSearching) {
                                            "🔄 ${uiStrings.activeOrderLabel} #${activeOrder.orderId} • Searching New Rider"
                                        } else {
                                            "🟢 ${uiStrings.activeOrderLabel} #${activeOrder.orderId} • ${activeOrder.orderStatus.displayTitle}"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDeclinedReSearching) Color(0xFF9A3412) else DeepNavy
                                    )
                                    Text(
                                        text = when {
                                            !activeOrder.assignedRiderName.isNullOrBlank() ->
                                                "Rider: ${activeOrder.assignedRiderName} (${activeOrder.assignedRiderPhone.orEmpty()})"
                                            isDeclinedReSearching ->
                                                activeOrder.latestRiderQuickNote?.takeIf { it.isNotBlank() }
                                                    ?: "Previous rider declined — Finding another nearby Rider..."
                                            else -> uiStrings.waitingForRiderText
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDeclinedReSearching) Color(0xFFB45309) else SlateMuted
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { currentStep = 3 },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("customer_main_open_live_map_btn")
                                ) {
                                    Text(
                                        text = uiStrings.liveTrackArrow,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            if (!activeOrder.assignedRiderName.isNullOrBlank()) {
                                CustomerProximityOtpAlertBanner(order = activeOrder)
                                DirectDialPadCallButton(
                                    orderId = activeOrder.orderId,
                                    callerRole = "CUSTOMER",
                                    callerName = customerName,
                                    receiverRole = "Rider",
                                    receiverName = activeOrder.assignedRiderName.orEmpty(),
                                    phoneNumber = activeOrder.assignedRiderPhone ?: "",
                                    buttonTestTag = "customer_home_call_rider_dial_btn"
                                )
                            }
                        }
                    }
                }
            }

            // STEP 1: MODULAR CARDS INSIDE LAZYCOLUMN (Prevents focus/measurement scroll jumps)
            if (currentStep == 1) {
                item(key = "customer_step1_header_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.16f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PrimaryTeal, BrightTeal)
                                    )
                                )
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiStrings.step1HeaderTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = uiStrings.step1HeaderSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = BrandOrange,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${selectedVehicle.etaMinutes} ${uiStrings.minPickupSuffix}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "customer_step1_pickup_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.16f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PICKUP HEADER + SAVED ADDRESS TAGS (Home, Office, Shop)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimaryTeal)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = uiStrings.pickupDetailsHeading,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = PrimaryTeal
                                                    )
                                                    Text(
                                                        text = uiStrings.pickupDetailsSubheading,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SlateMuted
                                                    )
                                                }
                                            }
                                            if (pickupLiveGpsVerified) {
                                                Surface(
                                                    color = Color(0xFFDCFCE7),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, VerifiedGreen)
                                                ) {
                                                    Text(
                                                        text = uiStrings.gpsCapturedBadge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = VerifiedGreen,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // PROMINENT 1-TAP AUTO-CAPTURE EXACT CURRENT ADDRESS BUTTON
                                        Button(
                                            enabled = !isCapturingPickupLocation,
                                            onClick = {
                                                pendingGpsTarget = "PICKUP"
                                                if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
                                                    isCapturingPickupLocation = true
                                                    scope.launch {
                                                        val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                                                        if (coords != null) {
                                                            applyLiveGpsCoordinates("PICKUP", coords.first, coords.second)
                                                        } else {
                                                            isCapturingPickupLocation = false
                                                            isErrorBanner = true
                                                            statusBanner = if (!DeliveryProofAndGpsManager.isLocationServiceEnabled(context)) {
                                                                "Location (GPS) is OFF. Please turn ON GPS to auto-capture your exact address, or enter it below."
                                                            } else {
                                                                "Unable to capture GPS fix right now. Please enable High-Accuracy Location or enter your address manually."
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    locationPermissionLauncher.launch(
                                                        arrayOf(
                                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                                        )
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PrimaryTeal,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("customer_add_live_pickup_location_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "Auto-Capture Current Address",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isCapturingPickupLocation) {
                                                    uiStrings.detectingAddressBtn
                                                } else {
                                                    uiStrings.autoCaptureCurrentAddrBtn
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        // Saved Pickup Address Tags (Home, Office, Shop + Custom Nicknames + Save Current)
                                        val pickupTagsList = remember(savedAddresses, selectedPickupSavedTag) {
                                            (listOf("Home", "Office", "Shop") + savedAddresses.map { it.label } + listOf(selectedPickupSavedTag))
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                                .distinctBy { it.lowercase() }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            pickupTagsList.forEach { tag ->
                                                val savedMatch = savedAddresses.firstOrNull {
                                                    it.label.equals(tag, ignoreCase = true)
                                                }
                                                val activeTag = selectedPickupSavedTag.equals(tag, ignoreCase = true)
                                                val displayTagLabel = when (tag) {
                                                    "Home" -> uiStrings.tagHome
                                                    "Office" -> uiStrings.tagOffice
                                                    "Shop" -> uiStrings.tagShop
                                                    else -> tag
                                                }
                                                FilterChip(
                                                    selected = activeTag,
                                                    onClick = {
                                                        selectedPickupSavedTag = tag
                                                        if (savedMatch != null) {
                                                            pickupHouseFlat = savedMatch.houseFlat
                                                            pickupBuilding = savedMatch.building
                                                            pickupAddressText = savedMatch.areaStreet
                                                            pickupPinCode = savedMatch.pinCode
                                                            pickupLandmark = savedMatch.landmark
                                                            if (savedMatch.lat != 0.0 && savedMatch.lng != 0.0) {
                                                                pickupLat = savedMatch.lat
                                                                pickupLng = savedMatch.lng
                                                                pickupLiveGpsVerified = true
                                                            }
                                                            isErrorBanner = false
                                                            statusBanner = "📍 Loaded saved $tag pickup address"
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            text = if (savedMatch != null) "📍 $displayTagLabel ✓" else "📍 $displayTagLabel",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = PrimaryTeal,
                                                        selectedLabelColor = Color.White
                                                    ),
                                                    modifier = Modifier.testTag("customer_pickup_tag_${tag.lowercase()}")
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    customLabelDraft = ""
                                                    customLabelDialogSide = "PICKUP"
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("customer_add_custom_pickup_label_btn")
                                            ) {
                                                Text(
                                                    text = "+ Custom",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryTeal,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            if (pickupAddressText.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        QuickDeliveryOperationsManager.saveCustomerAddress(
                                                            context = context,
                                                            label = selectedPickupSavedTag,
                                                            houseFlat = pickupHouseFlat,
                                                            building = pickupBuilding,
                                                            areaStreet = pickupAddressText,
                                                            pinCode = pickupPinCode,
                                                            landmark = pickupLandmark,
                                                            lat = pickupLat,
                                                            lng = pickupLng
                                                        )
                                                        isErrorBanner = false
                                                        statusBanner = "💾 Pickup address saved under $selectedPickupSavedTag"
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.testTag("customer_save_pickup_address_btn")
                                                ) {
                                                    Text(
                                                        text = "💾 ${uiStrings.saveBtn} $selectedPickupSavedTag",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryTeal,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }

                                        // Pickup Row 1: House / Flat No. + Building / Society Name
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = pickupHouseFlat,
                                                onValueChange = { pickupHouseFlat = it },
                                                label = { Text(uiStrings.houseFlatLabel) },
                                                isError = attemptedStep1Next && pickupHouseFlat.isBlank(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryTeal,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("pickup_house_flat_input")
                                            )
                                            OutlinedTextField(
                                                value = pickupBuilding,
                                                onValueChange = { pickupBuilding = it },
                                                label = { Text(uiStrings.buildingSocietyLabel) },
                                                isError = attemptedStep1Next && pickupBuilding.isBlank(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryTeal,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1.15f)
                                                    .testTag("pickup_building_input")
                                            )
                                        }

                                        // Pickup Row 2: Area / Locality Address
                                        OutlinedTextField(
                                            value = pickupAddressText,
                                            onValueChange = {
                                                pickupAddressText = it
                                                pickupLiveGpsVerified = false
                                                pickupLat = 0.0
                                                pickupLng = 0.0
                                            },
                                            label = { Text(uiStrings.pickupAreaLabel) },
                                            isError = attemptedStep1Next && pickupAddressText.isBlank(),
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = PrimaryTeal
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryTeal,
                                                unfocusedContainerColor = Color.White,
                                                focusedContainerColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("pickup_address_input")
                                        )

                                        CustomerAddressAutocompleteRow(
                                            query = pickupAddressText,
                                            savedAddresses = savedAddresses,
                                            pastOrders = customerOrders,
                                            isPickup = true,
                                            accentColor = PrimaryTeal,
                                            onSelectSaved = { saved ->
                                                pickupHouseFlat = saved.houseFlat
                                                pickupBuilding = saved.building
                                                pickupAddressText = saved.areaStreet
                                                pickupPinCode = saved.pinCode
                                                pickupLandmark = saved.landmark
                                                if (saved.lat != 0.0 && saved.lng != 0.0) {
                                                    pickupLat = saved.lat
                                                    pickupLng = saved.lng
                                                    pickupLiveGpsVerified = true
                                                }
                                            },
                                            onSelectRecentText = { recentText ->
                                                pickupAddressText = recentText
                                                val extractedPin = recentText.substringAfterLast("-").filter(Char::isDigit).take(6)
                                                if (extractedPin.length == 6 && pickupPinCode.isBlank()) pickupPinCode = extractedPin
                                            }
                                        )

                                        // Pickup Row 3: 6-Digit PIN Code + Landmark
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = pickupPinCode,
                                                onValueChange = { input ->
                                                    pickupPinCode = input.filter { it.isDigit() }.take(6)
                                                },
                                                label = { Text(uiStrings.pinCodeLabel) },
                                                isError = attemptedStep1Next && pickupPinCode.length != 6,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryTeal,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(0.85f)
                                                    .testTag("pickup_pincode_input")
                                            )
                                            OutlinedTextField(
                                                value = pickupLandmark,
                                                onValueChange = { pickupLandmark = it },
                                                label = { Text(uiStrings.landmarkOptionalLabel) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryTeal,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1.35f)
                                                    .testTag("pickup_landmark_input")
                                            )
                                        }
                        }
                    }
                }

                item(key = "customer_step1_drop_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.22f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // DROP HEADER + LIVE GPS OPTION
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(BrandOrange)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = uiStrings.dropDetailsHeading,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = BrandOrange
                                                    )
                                                    Text(
                                                        text = uiStrings.dropDetailsSubheading,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SlateMuted
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = SoftOrangeSurface,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .clickable(enabled = !isCapturingDropLocation) {
                                                        pendingGpsTarget = "DROP"
                                                        if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
                                                            isCapturingDropLocation = true
                                                            scope.launch {
                                                                val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                                                                if (coords != null) {
                                                                    applyLiveGpsCoordinates("DROP", coords.first, coords.second)
                                                                } else {
                                                                    isCapturingDropLocation = false
                                                                    isErrorBanner = true
                                                                    statusBanner = if (!DeliveryProofAndGpsManager.isLocationServiceEnabled(context)) {
                                                                        "Location (GPS) is OFF. Please turn ON GPS to auto-capture exact drop address, or enter it below."
                                                                    } else {
                                                                        "Unable to capture GPS fix right now. Please enter drop address manually."
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            locationPermissionLauncher.launch(
                                                                arrayOf(
                                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                                )
                                                            )
                                                        }
                                                    }
                                                    .testTag("customer_add_live_drop_location_btn")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MyLocation,
                                                        contentDescription = "Auto-Capture Drop Current Address",
                                                        tint = BrandOrange,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isCapturingDropLocation) {
                                                            uiStrings.detectingShort
                                                        } else if (dropLiveGpsVerified) {
                                                            uiStrings.addressCapturedBadge
                                                        } else {
                                                            uiStrings.autoCaptureDropBtn
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandOrange
                                                    )
                                                }
                                            }
                                        }

                                        // Saved Drop Address Tags (Home, Office, Shop + Custom Nicknames + Save Current)
                                        val dropTagsList = remember(savedAddresses, selectedDropSavedTag) {
                                            (listOf("Home", "Office", "Shop") + savedAddresses.map { it.label } + listOf(selectedDropSavedTag))
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                                .distinctBy { it.lowercase() }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            dropTagsList.forEach { tag ->
                                                val savedMatch = savedAddresses.firstOrNull {
                                                    it.label.equals(tag, ignoreCase = true)
                                                }
                                                val activeTag = selectedDropSavedTag.equals(tag, ignoreCase = true)
                                                val displayDropTagLabel = when (tag) {
                                                    "Home" -> uiStrings.tagHome
                                                    "Office" -> uiStrings.tagOffice
                                                    "Shop" -> uiStrings.tagShop
                                                    else -> tag
                                                }
                                                FilterChip(
                                                    selected = activeTag,
                                                    onClick = {
                                                        selectedDropSavedTag = tag
                                                        if (savedMatch != null) {
                                                            dropHouseFlat = savedMatch.houseFlat
                                                            dropBuilding = savedMatch.building
                                                            dropAddressText = savedMatch.areaStreet
                                                            dropPinCode = savedMatch.pinCode
                                                            dropLandmark = savedMatch.landmark
                                                            if (savedMatch.lat != 0.0 && savedMatch.lng != 0.0) {
                                                                dropLat = savedMatch.lat
                                                                dropLng = savedMatch.lng
                                                                dropLiveGpsVerified = true
                                                            }
                                                            isErrorBanner = false
                                                            statusBanner = "🏁 Loaded saved $tag drop address"
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            text = if (savedMatch != null) "🏁 $displayDropTagLabel ✓" else "🏁 $displayDropTagLabel",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = BrandOrange,
                                                        selectedLabelColor = Color.White
                                                    ),
                                                    modifier = Modifier.testTag("customer_drop_tag_${tag.lowercase()}")
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    customLabelDraft = ""
                                                    customLabelDialogSide = "DROP"
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("customer_add_custom_drop_label_btn")
                                            ) {
                                                Text(
                                                    text = "+ Custom",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandOrange,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            if (dropAddressText.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        QuickDeliveryOperationsManager.saveCustomerAddress(
                                                            context = context,
                                                            label = selectedDropSavedTag,
                                                            houseFlat = dropHouseFlat,
                                                            building = dropBuilding,
                                                            areaStreet = dropAddressText,
                                                            pinCode = dropPinCode,
                                                            landmark = dropLandmark,
                                                            lat = dropLat,
                                                            lng = dropLng
                                                        )
                                                        isErrorBanner = false
                                                        statusBanner = "💾 Drop address saved under $selectedDropSavedTag"
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.testTag("customer_save_drop_address_btn")
                                                ) {
                                                    Text(
                                                        text = "💾 ${uiStrings.saveBtn} $selectedDropSavedTag",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandOrange,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }

                                        // Receiver Name & Strict 10-Digit (+91) Phone Inputs + 1-Tap Phonebook Contact Picker
                                        val receiverPhoneError = evaluateIndianMobileError(
                                            digits = receiverMobilePhone,
                                            overLimitAttempted = receiverPhoneOverLimit
                                        ) ?: if (attemptedStep1Next && receiverMobilePhone.length != 10) {
                                            "Enter exact 10-digit Receiver Mobile Number"
                                        } else null
                                        val isReceiverPhoneValid = receiverMobilePhone.length == 10 &&
                                            !com.example.security.QuickDeliverySecurityGuard.isFakeOrDummyPhone(receiverMobilePhone)

                                        OutlinedTextField(
                                            value = receiverFullName,
                                            onValueChange = { receiverFullName = it },
                                            label = { Text(uiStrings.receiverNameLabel) },
                                            isError = attemptedStep1Next && receiverFullName.isBlank(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandOrange,
                                                unfocusedContainerColor = Color.White,
                                                focusedContainerColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("receiver_name_input")
                                        )

                                        OutlinedTextField(
                                            value = receiverMobilePhone,
                                            onValueChange = { input ->
                                                val parsed = processIndianMobileInput(input)
                                                receiverMobilePhone = parsed.sanitizedDigits
                                                receiverPhoneOverLimit = parsed.overLimitAttempted
                                            },
                                            label = { Text(uiStrings.receiverPhoneLabel) },
                                            placeholder = { Text("10-digit mobile number") },
                                            leadingIcon = { IndianPhonePrefixBadge(accentColor = BrandOrange) },
                                            isError = receiverPhoneError != null,
                                            supportingText = receiverPhoneError?.let { errText ->
                                                {
                                                    Text(
                                                        text = errText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFFDC2626),
                                                        modifier = Modifier.testTag("receiver_phone_error_text")
                                                    )
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isReceiverPhoneValid && !receiverPhoneOverLimit) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Valid 10-digit receiver mobile",
                                                            tint = VerifiedGreen,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            runCatching {
                                                                val pickIntent = Intent(
                                                                    Intent.ACTION_PICK,
                                                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                                                )
                                                                receiverContactPickerLauncher.launch(pickIntent)
                                                            }.onFailure {
                                                                isErrorBanner = true
                                                                statusBanner = "No Contacts app available on this device."
                                                            }
                                                        },
                                                        modifier = Modifier.testTag("pick_receiver_contact_btn")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Contacts,
                                                            contentDescription = "Pick from Contacts",
                                                            tint = BrandOrange
                                                        )
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandOrange,
                                                unfocusedContainerColor = Color.White,
                                                focusedContainerColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("receiver_phone_input")
                                        )

                                        // Drop Row 1: House / Flat No. + Building / Society Name
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = dropHouseFlat,
                                                onValueChange = { dropHouseFlat = it },
                                                label = { Text(uiStrings.houseFlatLabel) },
                                                isError = attemptedStep1Next && dropHouseFlat.isBlank(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = BrandOrange,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("drop_house_flat_input")
                                            )
                                            OutlinedTextField(
                                                value = dropBuilding,
                                                onValueChange = { dropBuilding = it },
                                                label = { Text(uiStrings.buildingSocietyLabel) },
                                                isError = attemptedStep1Next && dropBuilding.isBlank(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = BrandOrange,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1.15f)
                                                    .testTag("drop_building_input")
                                            )
                                        }

                                        // Drop Row 2: Area / Locality Address
                                        OutlinedTextField(
                                            value = dropAddressText,
                                            onValueChange = {
                                                dropAddressText = it
                                                dropLiveGpsVerified = false
                                                dropLat = 0.0
                                                dropLng = 0.0
                                            },
                                            label = { Text(uiStrings.dropAreaLabel) },
                                            isError = attemptedStep1Next && dropAddressText.isBlank(),
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = BrandOrange
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandOrange,
                                                unfocusedContainerColor = Color.White,
                                                focusedContainerColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("drop_address_input")
                                        )

                                        CustomerAddressAutocompleteRow(
                                            query = dropAddressText,
                                            savedAddresses = savedAddresses,
                                            pastOrders = customerOrders,
                                            isPickup = false,
                                            accentColor = BrandOrange,
                                            onSelectSaved = { saved ->
                                                dropHouseFlat = saved.houseFlat
                                                dropBuilding = saved.building
                                                dropAddressText = saved.areaStreet
                                                dropPinCode = saved.pinCode
                                                dropLandmark = saved.landmark
                                                if (saved.lat != 0.0 && saved.lng != 0.0) {
                                                    dropLat = saved.lat
                                                    dropLng = saved.lng
                                                    dropLiveGpsVerified = true
                                                }
                                            },
                                            onSelectRecentText = { recentText ->
                                                dropAddressText = recentText
                                                val extractedPin = recentText.substringAfterLast("-").filter(Char::isDigit).take(6)
                                                if (extractedPin.length == 6 && dropPinCode.isBlank()) dropPinCode = extractedPin
                                            }
                                        )

                                        // Drop Row 3: 6-Digit PIN Code + Landmark
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = dropPinCode,
                                                onValueChange = { input ->
                                                    dropPinCode = input.filter { it.isDigit() }.take(6)
                                                },
                                                label = { Text(uiStrings.pinCodeLabel) },
                                                isError = attemptedStep1Next && dropPinCode.length != 6,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = BrandOrange,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(0.85f)
                                                    .testTag("drop_pincode_input")
                                            )
                                            OutlinedTextField(
                                                value = dropLandmark,
                                                onValueChange = { dropLandmark = it },
                                                label = { Text(uiStrings.landmarkOptionalLabel) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = BrandOrange,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1.35f)
                                                    .testTag("drop_landmark_input")
                                            )
                                        }
                        }
                    }
                }

                // Vehicle Selection Section (Bike -> Scooter, plus Auto only when enabled by Admin)
                item(key = "customer_step1_vehicle_card") {
                    val isAutoVisible = vehiclePolicy.autoEnabled && remoteFlags.autoEnabled
                    val orderedCustomerVehicles = remember(isAutoVisible) {
                        if (isAutoVisible) {
                            listOf(
                                VehicleType.BIKE,
                                VehicleType.SCOOTER,
                                VehicleType.AUTO
                            )
                        } else {
                            listOf(
                                VehicleType.BIKE,
                                VehicleType.SCOOTER
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.16f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = uiStrings.selectVehicleHeading,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepNavy
                                            )
                                            Text(
                                                text = if (isAutoVisible) {
                                                    uiStrings.selectVehicleSubheading
                                                } else {
                                                    "Bike & Scooter • Instant Fare"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlateMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = SoftTealSurface,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$formattedDistanceKm ${uiStrings.kmRouteSuffix}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PrimaryTeal,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        orderedCustomerVehicles.forEachIndexed { index, vehicle ->
                                            val isSelected = selectedVehicle == vehicle
                                            val vehicleFareDetails = remember(
                                                pickupLat,
                                                pickupLng,
                                                dropLat,
                                                dropLng,
                                                selectedPackageType,
                                                packageWeightKg,
                                                packageDescription,
                                                vehicle,
                                                effectivePricingConfig,
                                                vehiclePolicy
                                            ) {
                                                AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
                                                    pickupLat = pickupLat,
                                                    pickupLng = pickupLng,
                                                    dropLat = dropLat,
                                                    dropLng = dropLng,
                                                    weightKgInput = packageWeightKg.ifBlank { "1.0" },
                                                    vehicleType = vehicle,
                                                    packageType = selectedPackageType,
                                                    packageDescription = packageDescription.ifBlank { "Parcel" },
                                                    pricingConfigOverride = effectivePricingConfig,
                                                    policyOverride = vehiclePolicy
                                                )
                                            }
                                            val isVehicleEnabled = vehicleFareDetails.isVehicleEnabledByAdmin
                                            val cardBg by animateColorAsState(
                                                targetValue = when {
                                                    !isVehicleEnabled -> Color(0xFFF1F5F9)
                                                    isSelected -> SoftTealSurface
                                                    else -> Color.White
                                                },
                                                animationSpec = tween(220),
                                                label = "vehicleCardBg_${vehicle.name}"
                                            )
                                            val borderColor by animateColorAsState(
                                                targetValue = when {
                                                    !isVehicleEnabled -> Color(0xFFCBD5E1)
                                                    isSelected -> BrandOrange
                                                    else -> Color(0xFFCBD5E1)
                                                },
                                                animationSpec = tween(220),
                                                label = "vehicleBorder_${vehicle.name}"
                                            )
                                            val cardElevation by animateDpAsState(
                                                targetValue = if (isSelected && isVehicleEnabled) 4.dp else 1.dp,
                                                label = "vehicleElevation_${vehicle.name}"
                                            )
                                            val shortVehicleLabel = when (vehicle) {
                                                VehicleType.BIKE -> uiStrings.vehicleBike
                                                VehicleType.SCOOTER -> uiStrings.vehicleScooter
                                                VehicleType.AUTO -> uiStrings.vehicleAuto
                                                else -> vehicle.title
                                            }
                                            val vehicleTagline = when {
                                                !isVehicleEnabled -> uiStrings.pausedLabel
                                                vehicle == VehicleType.BIKE -> "3 mins"
                                                vehicle == VehicleType.SCOOTER -> "5 mins"
                                                vehicle == VehicleType.AUTO -> "7 mins"
                                                else -> vehicle.capacity
                                            }

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                                                elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (isVehicleEnabled) {
                                                            selectedVehicle = vehicle
                                                            if (isErrorBanner && statusBanner?.contains("disabled", ignoreCase = true) == true) {
                                                                isErrorBanner = false
                                                                statusBanner = null
                                                            }
                                                        } else {
                                                            isErrorBanner = true
                                                            statusBanner = "${vehicle.title} is currently paused by Admin. Please choose an active vehicle."
                                                        }
                                                    }
                                                    .testTag("vehicle_chip_${vehicle.name}")
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 9.dp, vertical = 11.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    // Top step badge + selected indicator
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            color = when {
                                                                !isVehicleEnabled -> Color(0xFFE2E8F0)
                                                                isSelected -> BrandOrange
                                                                else -> SoftBg
                                                            },
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        ) {
                                                            Text(
                                                                text = "${index + 1}. $vehicleTagline",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected && isVehicleEnabled) Color.White else SlateMuted,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        if (isSelected && isVehicleEnabled) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.CheckCircle,
                                                                contentDescription = "Selected",
                                                                tint = PrimaryTeal,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }

                                                    // Vehicle Icon Badge (Scooter uses 🛵 instead of ⚡ to highlight front floorboard space)
                                                    Surface(
                                                        color = when {
                                                            !isVehicleEnabled -> Color(0xFFE2E8F0)
                                                            isSelected -> PrimaryTeal
                                                            else -> SoftBg
                                                        },
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(42.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            if (vehicle == VehicleType.SCOOTER) {
                                                                Text(
                                                                    text = "🛵",
                                                                    style = MaterialTheme.typography.titleMedium
                                                                )
                                                            } else {
                                                                Icon(
                                                                    imageVector = when (vehicle) {
                                                                        VehicleType.BIKE -> Icons.Default.TwoWheeler
                                                                        else -> Icons.Default.LocalShipping
                                                                    },
                                                                    contentDescription = shortVehicleLabel,
                                                                    tint = when {
                                                                        !isVehicleEnabled -> SlateMuted
                                                                        isSelected -> Color.White
                                                                        else -> DeepNavy
                                                                    },
                                                                    modifier = Modifier.size(21.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Vehicle Name (Bike / Scooter / Auto)
                                                    Text(
                                                        text = shortVehicleLabel,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isVehicleEnabled) DeepNavy else SlateMuted,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    // Base + Per-KM Rate from Live Admin Config
                                                    Text(
                                                        text = "₹${vehicleFareDetails.baseFareInr.toInt()} + ${vehicleFareDetails.formattedPerKmRate}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected && isVehicleEnabled) PrimaryTeal else SlateMuted,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    // Capacity & Front Floorboard Space Note (Scooter highlights food & larger parcels)
                                                    val capacityLine = when (vehicle) {
                                                        VehicleType.BIKE -> "≤${vehicleFareDetails.maxWeightKgForVehicle.toInt()}kg • Small parcels"
                                                        VehicleType.SCOOTER -> "≤${vehicleFareDetails.maxWeightKgForVehicle.toInt()}kg • Front space (Food/Box)"
                                                        else -> "≤${vehicleFareDetails.maxWeightKgForVehicle.toInt()}kg • 90×60cm"
                                                    }
                                                    Text(
                                                        text = capacityLine,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (vehicle == VehicleType.SCOOTER && isSelected) PrimaryTeal else SlateMuted,
                                                        fontWeight = if (vehicle == VehicleType.SCOOTER) FontWeight.SemiBold else FontWeight.Normal,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )

                                                    // Live Price Estimate Pill
                                                    Surface(
                                                        color = when {
                                                            !isVehicleEnabled -> SlateMuted
                                                            isSelected -> BrandOrange
                                                            else -> DeepNavy
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier
                                                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                                                .testTag("vehicle_fare_estimate_${vehicle.name}")
                                                        ) {
                                                            Text(
                                                                text = if (isVehicleEnabled) {
                                                                    vehicleFareDetails.formattedTotalFare
                                                                } else {
                                                                    uiStrings.pausedLabel
                                                                },
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis
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

                // Package Type Chips, Weight & Instructions
                item(key = "customer_step1_parcel_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.16f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = uiStrings.parcelCategoryHeading,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepNavy
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PackageType.entries.forEach { pkg ->
                                            val isSelected = selectedPackageType == pkg
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedPackageType = pkg },
                                                label = {
                                                    Text(
                                                        text = pkg.title,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrandOrange,
                                                    selectedLabelColor = Color.White
                                                ),
                                                modifier = Modifier.testTag("pkg_chip_${pkg.name}")
                                            )
                                        }
                                    }

                                    // Weight Bracket Selector Pills (Up to 5kg, 5-15kg, 15-20kg)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Up to 5kg" to "3.5",
                                            "5-15kg" to "10.0",
                                            "15-20kg" to "18.0"
                                        ).forEach { (bracket, presetKg) ->
                                            val activeBracket = selectedWeightBracketLabel == bracket
                                            FilterChip(
                                                selected = activeBracket,
                                                onClick = {
                                                    selectedWeightBracketLabel = bracket
                                                    packageWeightKg = presetKg
                                                },
                                                label = {
                                                    Text(
                                                        text = bracket,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PrimaryTeal,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                // Weight & Parcel Note (Strict 20 kg Max Parcel Weight Limit)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = packageWeightKg,
                                        onValueChange = { input ->
                                            packageWeightKg = input
                                            val check = AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg(input)
                                            if (check.isFailure) {
                                                isErrorBanner = true
                                                statusBanner = check.exceptionOrNull()?.message
                                            } else if (isErrorBanner && statusBanner?.contains("20 kg", ignoreCase = true) == true) {
                                                isErrorBanner = false
                                                statusBanner = null
                                            }
                                        },
                                        label = { Text(uiStrings.weightKgLabel) },
                                        isError = weightErrorMessage != null || (attemptedStep1Next && packageWeightKg.isBlank()),
                                        supportingText = {
                                            Text(
                                                text = if (isWeightOverLimit) {
                                                    "Max 20 kg!"
                                                } else if (weightErrorMessage != null) {
                                                    "0.1 – 20 kg"
                                                } else {
                                                    uiStrings.weightUpTo20Hint
                                                },
                                                color = if (weightErrorMessage != null) Color(0xFFB91C1C) else SlateMuted,
                                                fontWeight = if (weightErrorMessage != null) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (weightErrorMessage != null) Color(0xFFDC2626) else PrimaryTeal,
                                            errorBorderColor = Color(0xFFDC2626)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("package_weight_input")
                                    )
                                    OutlinedTextField(
                                        value = packageDescription,
                                        onValueChange = { packageDescription = it },
                                        label = { Text(uiStrings.parcelNoteLabel) },
                                        isError = attemptedStep1Next && packageDescription.isBlank(),
                                        supportingText = {
                                            Text(
                                                text = uiStrings.parcelNoteHint,
                                                color = if (attemptedStep1Next && packageDescription.isBlank()) Color(0xFFB91C1C) else SlateMuted
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryTeal,
                                            errorBorderColor = Color(0xFFDC2626)
                                        ),
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("package_description_input")
                                    )
                                }

                                // Delivery / Handling Instructions Chips (Swiggy/Zomato/Porter style, synced directly to Rider card)
                                val instructionPresets = remember {
                                    listOf(
                                        "Call on Arrival",
                                        "Leave at Gate",
                                        "Don't Ring Bell",
                                        "Fragile Parcel",
                                        "Keep Upright"
                                    )
                                }
                                val selectedInstructionsList = remember(selectedInstructionsCsv) {
                                    selectedInstructionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                }
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = uiStrings.deliveryInstructionsTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepNavy
                                        )
                                        Text(
                                            text = uiStrings.deliveryInstructionsSub,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateMuted
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            instructionPresets.forEach { preset ->
                                                val isChosen = selectedInstructionsList.any { it.equals(preset, ignoreCase = true) }
                                                FilterChip(
                                                    selected = isChosen,
                                                    onClick = {
                                                        val mutable = selectedInstructionsList.toMutableList()
                                                        if (isChosen) {
                                                            mutable.removeAll { it.equals(preset, ignoreCase = true) }
                                                        } else {
                                                            mutable.add(preset)
                                                        }
                                                        selectedInstructionsCsv = mutable.joinToString(", ")
                                                    },
                                                    label = {
                                                        Text(
                                                            text = if (isChosen) "✓ $preset" else preset,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = PrimaryTeal,
                                                        selectedLabelColor = Color.White
                                                    ),
                                                    modifier = Modifier.testTag("customer_instruction_chip_${preset.lowercase().replace(" ", "_")}")
                                                )
                                            }
                                        }
                                    }
                                }

                                // Clear Inline Error Alert Card when parcel weight exceeds 20 kg or is invalid
                                AnimatedVisibility(
                                    visible = weightErrorMessage != null,
                                    enter = fadeIn(tween(180)) + expandVertically(),
                                    exit = fadeOut(tween(150)) + shrinkVertically()
                                ) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("customer_weight_limit_error_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = "Weight Limit Exceeded",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Booking Blocked • Max 20 kg Parcel Limit",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF991B1B)
                                                )
                                                Text(
                                                    text = weightErrorMessage
                                                        ?: AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_ERROR_MESSAGE,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFB91C1C)
                                                )
                                            }
                                        }
                                    }
                        }
                    }
                }
                }

                // Repeat Last Order & Admin-Enabled Smart Add-Ons
                item(key = "customer_step1_enhancements_card") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        customerOrders.firstOrNull()?.let { prevOrder ->
                                    CustomerRepeatLastOrderCard(
                                        lastOrder = prevOrder,
                                        onRepeatRoute = { pastOrder ->
                                            pickupAddressText = pastOrder.pickupAddress.fullAddress
                                            pickupLandmark = pastOrder.pickupAddress.landmark
                                            pickupLat = pastOrder.pickupAddress.lat
                                            pickupLng = pastOrder.pickupAddress.lng
                                            pickupLiveGpsVerified = pastOrder.pickupAddress.lat != 0.0
                                            dropAddressText = pastOrder.dropAddress.fullAddress
                                            dropLandmark = pastOrder.dropAddress.landmark
                                            dropLat = pastOrder.dropAddress.lat
                                            dropLng = pastOrder.dropAddress.lng
                                            dropLiveGpsVerified = pastOrder.dropAddress.lat != 0.0
                                            receiverFullName = pastOrder.dropAddress.contactName
                                            receiverMobilePhone = pastOrder.dropAddress.contactPhone.filter(Char::isDigit).takeLast(10)
                                            selectedVehicle = pastOrder.vehicleType
                                            selectedPackageType = pastOrder.packageDetails.packageType
                                            packageWeightKg = pastOrder.packageDetails.weightKg
                                            packageDescription = pastOrder.packageDetails.description
                                            isErrorBanner = false
                                            statusBanner = "🔁 Route loaded from Order #${pastOrder.orderId}"
                                        }
                                    )
                                }

                                // Admin-Controlled Add-Ons: Scheduled Delivery & Multi-Drop Extra Stop
                                CustomerScheduledAndMultiDropCard(
                                    flags = remoteFlags,
                                    isScheduled = isScheduledDelivery,
                                    onToggleScheduled = { isScheduledDelivery = it },
                                    selectedDateLabel = scheduledDateLabel,
                                    onSelectDateLabel = { scheduledDateLabel = it },
                                    selectedTimeSlot = scheduledTimeSlot,
                                    onSelectTimeSlot = { scheduledTimeSlot = it },
                                    isMultiDrop = isMultiDropEnabled,
                                    onToggleMultiDrop = { isMultiDropEnabled = it },
                                    extraStopAddress = extraStopAddress,
                                    onExtraStopChange = { extraStopAddress = it },
                                    extraStopFeeInr = 40
                                )

                                // Porter-Style Loading / Unloading Helper Add-On
                                CustomerHelperAddonCard(
                                    flags = remoteFlags,
                                    isHelperAdded = isHelperAdded,
                                    onToggleHelper = { isHelperAdded = it }
                                )

                        // Admin-Controlled Referral & Wallet Rewards Card
                        CustomerReferralWalletCard(
                            flags = remoteFlags,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            walletBonusInr = walletBonusInr,
                            useWalletBonus = useWalletBonus,
                            onToggleUseWallet = { useWalletBonus = it }
                        )
                    }
                }

                // Premium Instant Full ₹ Price Breakdown Card (Step 1)
                item(key = "customer_step1_fare_card") {
                    Surface(
                        color = DeepNavy,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_step1_fare_breakdown_card")
                    ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = BrightTeal.copy(alpha = 0.22f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = uiStrings.liveFareBreakdownBadge,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = BrightTeal,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    if (liveDetailedFare.surgeMultiplier > 1.0) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = BrandOrange.copy(alpha = 0.25f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "${String.format(Locale.US, "%.2fx", liveDetailedFare.surgeMultiplier)} Surge",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BrandOrange,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${selectedVehicle.title} • 📍 $formattedDistanceKm km (~$estimatedTripEtaMins min ETA) • ${liveDetailedFare.weightKg} kg",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = BrandOrange,
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text(
                                                    text = String.format(Locale.US, "₹%.0f", liveFareAmountInr),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier
                                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                                        .testTag("customer_live_fare_total")
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                                        // Itemized ₹ Rows: Base Fare + Distance + Weight + GST + Interactive Promo Coupon
                                        CustomerFareLineRow(
                                            label = "${selectedVehicle.title} ${uiStrings.baseFareLabel}",
                                            value = liveDetailedFare.formattedBaseFare
                                        )
                                        CustomerFareLineRow(
                                            label = "${uiStrings.distanceChargeLabel} ($formattedDistanceKm km × ${liveDetailedFare.formattedPerKmRate})",
                                            value = liveDetailedFare.formattedDistanceCharge
                                        )
                                        CustomerFareLineRow(
                                            label = "${uiStrings.weightChargeLabel} (${liveDetailedFare.weightKg} kg ≤ ${activeMaxWeightKg.toInt()} kg)",
                                            value = liveDetailedFare.formattedWeightCharge
                                        )
                                        CustomerFareLineRow(
                                            label = uiStrings.gstAndFeeLabel,
                                            value = liveDetailedFare.formattedTaxesAndFees
                                        )
                                        if (totalRemoteAddonInr > 0.0) {
                                            CustomerFareLineRow(
                                                label = extraAddonSummaryLabel,
                                                value = String.format(Locale.US, "+ ₹%.0f", totalRemoteAddonInr)
                                            )
                                        }
                                        if (promoDiscountInr > 0.0 && appliedPromoCode.isNotBlank()) {
                                            CustomerFareLineRow(
                                                label = "Promo Discount ($appliedPromoCode)",
                                                value = String.format(Locale.US, "- ₹%.0f", promoDiscountInr)
                                            )
                                        }
                                        if (walletDiscountInr > 0.0) {
                                            CustomerFareLineRow(
                                                label = "Referral Wallet Bonus",
                                                value = String.format(Locale.US, "- ₹%.0f", walletDiscountInr)
                                            )
                                        }

                                        // Interactive Promo Code Box (Admin-controlled: hidden by default until enabled in Admin Future Switches)
                                        if (remoteFlags.promoCodeEnabled) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = promoCodeInput,
                                                    onValueChange = { promoCodeInput = it.uppercase().filter(Char::isLetterOrDigit) },
                                                    placeholder = { Text(uiStrings.promoCodePlaceholder, color = Color.White.copy(alpha = 0.55f)) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = BrightTeal,
                                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("customer_promo_code_input")
                                                )
                                                if (appliedPromoCode.isNotBlank()) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            appliedPromoCode = ""
                                                            promoCodeInput = ""
                                                            promoFeedbackMessage = null
                                                        },
                                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = uiStrings.removeBtn,
                                                            color = Color(0xFFFECACA),
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            val match = QuickDeliveryOperationsManager.findActiveCoupon(promoCodeInput)
                                                            if (match != null) {
                                                                appliedPromoCode = match.code
                                                                val est = match.computeDiscountInr(liveDetailedFare.totalFareInr)
                                                                promoFeedbackMessage = "✅ Applied ${match.code}: Saved ₹${est.toInt()}"
                                                            } else {
                                                                promoFeedbackMessage = "Invalid or paused promo code. Check active Admin offers."
                                                            }
                                                        },
                                                        enabled = promoCodeInput.length >= 3,
                                                        colors = ButtonDefaults.buttonColors(containerColor = BrightTeal),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                        modifier = Modifier.testTag("customer_apply_promo_btn")
                                                    ) {
                                                        Text(
                                                            text = uiStrings.applyBtn,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                            val activeCoupons = remember(adminPromoCoupons) {
                                                adminPromoCoupons.filter { it.isActive }
                                            }
                                            if (activeCoupons.isNotEmpty() && appliedPromoCode.isBlank()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    activeCoupons.take(4).forEach { coupon ->
                                                        Surface(
                                                            onClick = {
                                                                promoCodeInput = coupon.code
                                                                appliedPromoCode = coupon.code
                                                                val est = coupon.computeDiscountInr(liveDetailedFare.totalFareInr)
                                                                promoFeedbackMessage = "✅ Applied ${coupon.code}: Saved ₹${est.toInt()}"
                                                            },
                                                            color = BrightTeal.copy(alpha = 0.22f),
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(1.dp, BrightTeal.copy(alpha = 0.5f))
                                                        ) {
                                                            Text(
                                                                text = "🏷️ ${coupon.code} (${if (coupon.discountPercent > 0) "${coupon.discountPercent}% OFF" else "₹${coupon.flatDiscountInr} OFF"})",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            promoFeedbackMessage?.let { msg ->
                                                Text(
                                                    text = msg,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (msg.startsWith("✅")) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                    }
                }

                // Gated B2B Dukaan Monthly Postpaid & Bulk Dispatch Card (Strictly hidden until Admin turns ON)
                if (remoteFlags.b2bPostpaidEnabled) {
                    item(key = "customer_b2b_dukaan_step1_item") {
                        com.example.ui.components.CustomerB2bDukaanStep1Card(
                            flags = remoteFlags,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            pickupAddress = fullPickupAddressFormatted.ifBlank { pickupAddressText },
                            onBulkOrderSubmit = { dropList, shopName ->
                                val basePickup = fullPickupAddressFormatted.ifBlank { pickupAddressText.ifBlank { "Dukaan Hub" } }
                                scope.launch {
                                    var createdCount = 0
                                    dropList.forEachIndexed { idx, dropAddr ->
                                        val res = AutomatedFairRiderAssignmentEngine.createCustomerOrderWithBackendValidation(
                                            repository = repository,
                                            customerId = myCustomerId,
                                            customerName = "$shopName (Bulk ${idx + 1}/${dropList.size})",
                                            customerPhone = normalizedCustomerPhone,
                                            pickupAddress = AddressDetails(
                                                fullAddress = basePickup,
                                                landmark = pickupLandmark,
                                                contactName = customerName.ifBlank { shopName },
                                                contactPhone = normalizedCustomerPhone,
                                                lat = pickupLat,
                                                lng = pickupLng
                                            ),
                                            dropAddress = AddressDetails(
                                                fullAddress = dropAddr,
                                                landmark = "",
                                                contactName = receiverFullName.ifBlank { "Bulk Stop ${idx + 1}" },
                                                contactPhone = receiverMobilePhone.ifBlank { normalizedCustomerPhone },
                                                lat = dropLat,
                                                lng = dropLng
                                            ),
                                            packageDetails = PackageDetails(
                                                packageType = selectedPackageType,
                                                weightKg = packageWeightKg.ifBlank { "2.0" },
                                                description = "B2B Bulk Dispatch #${idx + 1} [$shopName]"
                                            ),
                                            vehicleType = selectedVehicle,
                                            paymentMethod = selectedPaymentMethod
                                        )
                                        if (res.isSuccess) createdCount++
                                    }
                                    if (createdCount > 0) {
                                        com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                        isErrorBanner = false
                                        statusBanner = "🏢 Dispatched $createdCount B2B bulk stops for $shopName!"
                                        currentStep = 3
                                    }
                                }
                            }
                        )
                    }
                }

                // Step 1 Validation Error & Action Buttons
                item(key = "customer_step1_cta_card") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val inlineStep1ValidationError = if (attemptedStep1Next) validateStep1MandatoryFields() else null
                                if (inlineStep1ValidationError != null) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.2.dp, Color(0xFFDC2626)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("customer_step1_mandatory_error_box")
                                    ) {
                                        Text(
                                            text = "⚠️ $inlineStep1ValidationError",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }

                                // Step 1 Primary Action Button (Enforces mandatory * fields + max 20 kg limit)
                                Button(
                                    enabled = !isResolvingStep1Route,
                                    onClick = {
                                        attemptedStep1Next = true
                                        val validationError = validateStep1MandatoryFields()
                                        if (validationError != null) {
                                            isErrorBanner = true
                                            statusBanner = validationError
                                        } else {
                                            isErrorBanner = false
                                            statusBanner = null
                                            isResolvingStep1Route = true
                                            scope.launch {
                                                if (!pickupLiveGpsVerified) {
                                                    val pickupQuery = listOf(pickupHouseFlat, pickupBuilding, pickupAddressText, pickupPinCode)
                                                        .filter { it.isNotBlank() }
                                                        .joinToString(", ")
                                                    DeliveryProofAndGpsManager.forwardGeocodeAddress(context, pickupQuery)?.let { coords ->
                                                        pickupLat = coords.first
                                                        pickupLng = coords.second
                                                    }
                                                }
                                                if (!dropLiveGpsVerified) {
                                                    val dropQuery = listOf(dropHouseFlat, dropBuilding, dropAddressText, dropPinCode)
                                                        .filter { it.isNotBlank() }
                                                        .joinToString(", ")
                                                    DeliveryProofAndGpsManager.forwardGeocodeAddress(context, dropQuery)?.let { coords ->
                                                        dropLat = coords.first
                                                        dropLng = coords.second
                                                    }
                                                }
                                                isResolvingStep1Route = false
                                                currentStep = 2
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (weightErrorMessage != null) Color(0xFFDC2626) else BrandOrange
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("customer_step1_next_btn")
                                ) {
                                    Text(
                                        text = when {
                                            isResolvingStep1Route -> uiStrings.verifyingRouteBtn
                                            isWeightOverLimit -> "Booking Blocked: Max 20 kg Allowed"
                                            else -> uiStrings.nextParcelPhotoBtn
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    if (!isWeightOverLimit && !isResolvingStep1Route) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }

                                if (activeOrder != null) {
                                    OutlinedButton(
                                        onClick = {
                                            statusBanner = null
                                            currentStep = 3
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.2.dp, PrimaryTeal),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .testTag("customer_jump_to_active_order_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = PrimaryTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = String.format(Locale.US, uiStrings.viewActiveOrderTrackBtn, activeOrder.orderId),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryTeal
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // STEP 2: PARCEL PHOTO CAPTURE OPTION (RIGHT AFTER ADDRESS -> NEXT!)
            if (currentStep == 2) {
                item(key = "customer_step2_photo_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(PrimaryTeal, BrightTeal)
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            IconButton(
                                                onClick = { currentStep = 1 },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("customer_step2_back_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back to Address",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color.White.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "STEP 2 OF 4",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = BrandOrange,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = uiStrings.safeProofBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = uiStrings.step2HeaderTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = uiStrings.step2HeaderSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Route & Package Quick Recap
                                Surface(
                                    color = SoftBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = PrimaryTeal,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "${selectedPackageType.title} • $packageWeightKg kg",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DeepNavy
                                                )
                                                Text(
                                                    text = "$fullPickupAddressFormatted → $fullDropAddressFormatted",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SlateMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = liveQuote.totalAmount,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandOrange,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Visual Camera Capture Studio Card
                                Surface(
                                    color = if (currentBookingPhotoSummary != null) Color(0xFFF0FDF4) else SoftTealSurface,
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (currentBookingPhotoSummary != null) VerifiedGreen else PrimaryTeal.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = if (currentBookingPhotoSummary != null) VerifiedGreen else PrimaryTeal,
                                            shape = CircleShape,
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (currentBookingPhotoSummary != null) {
                                                        Icons.Default.CheckCircle
                                                    } else {
                                                        Icons.Default.CameraAlt
                                                    },
                                                    contentDescription = "Parcel Camera",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (currentBookingPhotoSummary != null) {
                                                uiStrings.photoAttachedVerifiedTitle
                                            } else {
                                                uiStrings.captureOrAttachPhotoTitle
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepNavy
                                        )

                                        Text(
                                            text = currentBookingPhotoSummary ?: uiStrings.photoHelpDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (currentBookingPhotoSummary != null) VerifiedGreen else SlateMuted
                                        )

                                        Button(
                                            onClick = {
                                                statusBanner = null
                                                isErrorBanner = false
                                                if (DeliveryProofAndGpsManager.hasCameraPermission(context)) {
                                                    cameraLauncher.launch(null)
                                                } else {
                                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("customer_post_booking_photo_btn")
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (currentBookingPhotoSummary == null) {
                                                    uiStrings.clickParcelPhotoBtn
                                                } else {
                                                    uiStrings.retakeParcelPhotoBtn
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Admin-Controlled Parcel Insurance Add-On Checkbox
                                CustomerParcelInsuranceCard(
                                    flags = remoteFlags,
                                    isInsured = isParcelInsured,
                                    onToggleInsured = { isParcelInsured = it }
                                )

                                // Payment Method Selector Card (UPI, COD, Card, Net Banking)
                                Surface(
                                    color = SoftBg,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("customer_step2_payment_method_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Payment,
                                                contentDescription = "Payment Method",
                                                tint = PrimaryTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (remoteFlags.liveGatewayModeEnabled) {
                                                    "${uiStrings.selectPaymentMethodTitle} (${remoteFlags.gatewayProvider})"
                                                } else {
                                                    uiStrings.selectPaymentMethodTitle
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepNavy
                                            )
                                        }

                                        val paymentChoices = listOf(
                                            CustomerPaymentMethod.GATEWAY_UPI to "UPI",
                                            CustomerPaymentMethod.CASH_ON_DELIVERY to (
                                                if (!remoteFlags.codEnabled) "Cash (${uiStrings.pausedLabel})"
                                                else if (totalWithTipInr > remoteFlags.maxCodLimitInr) "Cash (Max ₹${remoteFlags.maxCodLimitInr})"
                                                else uiStrings.cashCodLabel
                                            ),
                                            CustomerPaymentMethod.GATEWAY_DEBIT_CARD to "Debit Card",
                                            CustomerPaymentMethod.GATEWAY_CREDIT_CARD to "Credit Card",
                                            CustomerPaymentMethod.GATEWAY_NET_BANKING to "Net Banking"
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            paymentChoices.forEach { (method, label) ->
                                                val isChoiceEnabled = method != CustomerPaymentMethod.CASH_ON_DELIVERY || isCodAllowedForOrder
                                                FilterChip(
                                                    selected = selectedPaymentMethod == method,
                                                    enabled = isChoiceEnabled,
                                                    onClick = { selectedPaymentMethod = method },
                                                    label = { Text(label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                                                    modifier = Modifier.testTag("customer_pay_method_${method.name}")
                                                )
                                            }
                                        }
                                        if (!isCodAllowedForOrder) {
                                            Text(
                                                text = if (!remoteFlags.codEnabled) {
                                                    "Cash on Delivery is paused by Admin — please pay via UPI or Card."
                                                } else {
                                                    "COD is capped at ₹${remoteFlags.maxCodLimitInr} by Admin — choose UPI or Card for this order."
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB45309),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (selectedPaymentMethod == CustomerPaymentMethod.GATEWAY_UPI) {
                                            Text(
                                                text = uiStrings.preferredUpiLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SlateMuted
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                UpiAppOption.entries.forEach { upiOption ->
                                                    FilterChip(
                                                        selected = selectedUpiApp == upiOption,
                                                        onClick = { selectedUpiApp = upiOption },
                                                        label = { Text(upiOption.displayName, maxLines = 1, softWrap = false) },
                                                        modifier = Modifier.testTag("customer_upi_app_${upiOption.name}")
                                                    )
                                                }
                                            }
                                        } else if (selectedPaymentMethod == CustomerPaymentMethod.CASH_ON_DELIVERY) {
                                            Text(
                                                text = "Pay cash directly to the Rider upon delivery pickup/drop-off.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF065F46)
                                            )
                                        }

                                        if (remoteFlags.b2bPostpaidEnabled && approvedB2bAccount != null) {
                                            val canUseCredit = approvedB2bAccount.availableCreditInr >= totalWithTipInr
                                            FilterChip(
                                                selected = useB2bPostpaid,
                                                enabled = canUseCredit,
                                                onClick = { useB2bPostpaid = !useB2bPostpaid },
                                                label = {
                                                    Text(
                                                        text = if (canUseCredit) {
                                                            "🏢 B2B Monthly Postpaid (₹0 Now • ${approvedB2bAccount.shopName})"
                                                        } else {
                                                            "🏢 B2B Credit Low (Avail ₹${approvedB2bAccount.availableCreditInr.toInt()})"
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                modifier = Modifier.testTag("customer_b2b_postpaid_chip")
                                            )
                                        }

                                        HorizontalDivider(color = Color(0xFFE2E8F0))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Redeem,
                                                contentDescription = "Optional Rider Tip",
                                                tint = BrandOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = uiStrings.tipPartnerTitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepNavy
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(0, 20, 30, 50).forEach { tipVal ->
                                                FilterChip(
                                                    selected = selectedRiderTipInr == tipVal,
                                                    onClick = { selectedRiderTipInr = tipVal },
                                                    label = {
                                                        Text(
                                                            text = if (tipVal == 0) uiStrings.noTipLabel else "+₹$tipVal",
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    },
                                                    modifier = Modifier.testTag("customer_tip_chip_$tipVal")
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (currentBookingPhotoSummary == null) {
                                            currentBookingPhotoSummary = "Parcel Photo Confirmed (${selectedPackageType.title})"
                                        }
                                        val weightBackendCheck = AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg(
                                            packageWeightKg,
                                            selectedVehicle
                                        )
                                        if (weightBackendCheck.isFailure) {
                                            isErrorBanner = true
                                            statusBanner = weightBackendCheck.exceptionOrNull()?.message
                                                ?: AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_ERROR_MESSAGE
                                            currentStep = 1
                                            return@Button
                                        }
                                        isErrorBanner = false
                                        statusBanner = null
                                        showBookingConfirmationScreen = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .padding(bottom = 6.dp)
                                        .testTag("customer_step2_book_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(Locale.US, uiStrings.reviewConfirmBookingBtn, totalWithTipInr),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STEP 3: CONTINUOUS LIVE RIDER TRACKING (FROM RIDER ACCEPT UNTIL DELIVERED!)
            if (currentStep == 3) {
                item(key = "customer_step3_live_tracking_card") {
                    if (activeOrder != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LiveRiderContinuousTrackingCard(
                                order = activeOrder,
                                photoProofSummary = orderPhotoProofMap[activeOrder.orderId] ?: currentBookingPhotoSummary,
                                liveRiderGpsCoordinates = liveRiderGpsCoordinates,
                                onRefreshLiveGps = {
                                    if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
                                        scope.launch {
                                            val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                                            liveRiderGpsCoordinates = if (coords != null) {
                                                String.format(Locale.US, "%.5f, %.5f", coords.first, coords.second)
                                            } else {
                                                null
                                            }
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                onSimulateOrAdvanceStep = {
                                    when (activeOrder.orderStatus) {
                                        OrderStatus.SEARCHING_RIDER -> {
                                            AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
                                            AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                                                repository = repository,
                                                orderId = activeOrder.orderId
                                            )
                                        }
                                        OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> {
                                            repository.verifyPickupOtpByRider(
                                                activeOrder.orderId,
                                                activeOrder.pickupOtp,
                                                activeOrder.assignedRiderId
                                            )
                                        }
                                        OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> {
                                            repository.verifyDeliveryOtpByRider(
                                                activeOrder.orderId,
                                                activeOrder.deliveryOtp,
                                                activeOrder.assignedRiderId
                                            )
                                            AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                                            MotivationAndPromoManager.notifySuccessfulDeliveryMotivationForBoth(
                                                order = activeOrder.copy(orderStatus = OrderStatus.DELIVERED),
                                                forceTrigger = true
                                            )
                                        }
                                        else -> Unit
                                    }
                                },
                                onBookNewOrder = {
                                    statusBanner = null
                                    currentBookingPhotoSummary = null
                                    currentStep = 1
                                }
                            )

                            if (activeOrder.orderStatus == OrderStatus.SEARCHING_RIDER) {
                                com.example.ui.components.CustomerSearchingCountdownCard(
                                    order = activeOrder,
                                    onRetrySearch = {
                                        AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                                        com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                        isErrorBanner = false
                                        statusBanner = "📡 Re-pinging nearby Riders & Admin dispatch queue..."
                                    },
                                    onCancelFree = {
                                        orderPendingCancellation = activeOrder
                                    }
                                )
                            }

                            CustomerProximityOtpAlertBanner(
                                order = activeOrder,
                                otpEnabled = remoteFlags.pickupDropOtpEnabled
                            )

                            if (activeOrder.orderStatus == OrderStatus.DELIVERED) {
                                Button(
                                    onClick = {
                                        val shared = com.example.security.DigitalReceiptShareHelper.shareDigitalReceiptOnWhatsApp(
                                            context = context,
                                            order = activeOrder
                                        )
                                        isErrorBanner = !shared
                                        statusBanner = if (shared) {
                                            "🧾 Digital Delivery Bill ready to share on WhatsApp!"
                                        } else {
                                            "Could not open share sheet on this device."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("customer_share_whatsapp_receipt_btn_${activeOrder.orderId}")
                                ) {
                                    Text(
                                        text = "🧾 Share Digital Bill on WhatsApp (Free)",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }

                                com.example.ui.components.PostDeliveryRiderFeedbackSection(
                                    orderId = activeOrder.orderId,
                                    riderId = activeOrder.assignedRiderId ?: "",
                                    riderName = activeOrder.assignedRiderName ?: "Delivery Partner",
                                    customerName = customerName.ifBlank { activeOrder.customerName },
                                    onFeedbackSubmitted = {
                                        com.example.data.cloud.MultiPhoneLiveSyncManager.triggerImmediateSync(context)
                                        isErrorBanner = false
                                        statusBanner = "⭐ Rating & tip synced with Rider and Admin!"
                                    }
                                )
                            }

                            // Verified Rider Profile & Download GST Invoice + Cancel Option
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
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
                                                text = if (!activeOrder.assignedRiderName.isNullOrBlank()) {
                                                    "Assigned Rider: ${activeOrder.assignedRiderName}"
                                                } else {
                                                    "Waiting for nearby Rider Partner to Accept..."
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (remoteFlags.pickupDropOtpEnabled) {
                                                    "Pickup OTP: ${activeOrder.pickupOtp} • Drop OTP: ${activeOrder.deliveryOtp}"
                                                } else {
                                                    "Direct Pickup & Drop Active • Fast Handover (No OTP Needed)"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BrightTeal
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { showGstInvoiceDialogForOrder = activeOrder },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("customer_download_gst_invoice_btn")
                                        ) {
                                            Text(
                                                text = uiStrings.viewGstInvoiceBtn,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    CustomerShareLiveTrackingButton(order = activeOrder)

                                    CustomerSendReceiverOtpCard(
                                        order = activeOrder,
                                        otpEnabled = remoteFlags.pickupDropOtpEnabled
                                    )

                                    CustomerOrderDisputeCard(
                                        order = activeOrder,
                                        photoProofSummary = orderPhotoProofMap[activeOrder.orderId]
                                    )

                                    if (activeOrder.orderStatus == OrderStatus.SEARCHING_RIDER ||
                                        activeOrder.orderStatus == OrderStatus.RIDER_ASSIGNED
                                    ) {
                                        OutlinedButton(
                                            onClick = { orderPendingCancellation = activeOrder },
                                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFECACA)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("customer_cancel_searching_order_btn")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(uiStrings.cancelBookingBtn, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = uiStrings.noActiveOrderTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = uiStrings.noActiveOrderDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                                Button(
                                    onClick = { currentStep = 1 },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = uiStrings.goToStep1Btn,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STEP 4 TAB: DEDICATED DELIVERY HISTORY TAB (Past Completed Deliveries with Cost & Date Summary)
            if (currentStep == 4) {
                item(key = "customer_step4_delivery_history_tab") {
                    CustomerDeliveryHistoryTabSection(
                        customerName = customerName,
                        orders = customerOrders,
                        onSelectOrderToTrack = { selectedOrder ->
                            selectedTrackingOrderId = selectedOrder.orderId
                            statusBanner = null
                            currentStep = 3
                        },
                        onReorderRoute = { pastOrder ->
                            pickupAddressText = pastOrder.pickupAddress.fullAddress
                            pickupLandmark = pastOrder.pickupAddress.landmark
                            pickupLat = pastOrder.pickupAddress.lat
                            pickupLng = pastOrder.pickupAddress.lng
                            dropAddressText = pastOrder.dropAddress.fullAddress
                            dropLandmark = pastOrder.dropAddress.landmark
                            dropLat = pastOrder.dropAddress.lat
                            dropLng = pastOrder.dropAddress.lng
                            selectedVehicle = pastOrder.vehicleType
                            selectedPackageType = pastOrder.packageDetails.packageType
                            packageWeightKg = pastOrder.packageDetails.weightKg
                            packageDescription = pastOrder.packageDetails.description
                            currentStep = 1
                            isErrorBanner = false
                            statusBanner = "Route loaded from Order #${pastOrder.orderId} — Review and book!"
                        },
                        onOpenFullHistoryScreen = {
                            showOrderHistoryScreen = true
                        },
                        onBookNewDelivery = {
                            statusBanner = null
                            currentStep = 1
                        }
                    )
                }
            }
        }
    }
    AnimatedVisibility(
        visible = statusBanner != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Surface(
            color = if (isErrorBanner) Color(0xFFFEF2F2) else SoftTealSurface,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 6.dp,
            border = BorderStroke(
                1.dp,
                if (isErrorBanner) Color(0xFFEF4444) else BrightTeal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!isErrorBanner && activeOrder != null) {
                        currentStep = 3
                    }
                    statusBanner = null
                }
                .testTag("customer_status_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isErrorBanner) Icons.Default.Shield else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isErrorBanner) Color(0xFFB91C1C) else VerifiedGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusBanner.orEmpty(),
                        color = if (isErrorBanner) Color(0xFFB91C1C) else DeepNavy,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = { statusBanner = null },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notification",
                        tint = SlateMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    QuickDeliveryNextLevelLaunchOverlay(
        appTitle = "Quick Delivery",
        appRoleBadge = "Live Rider Map Tracking • Instant City Courier",
        accentColor = BrightTeal,
        secondaryAccentColor = BrandOrange
    )
    }
}

@Composable
private fun CustomerWizardProgressHeader(
    currentStep: Int,
    hasBookedOrder: Boolean,
    uiStrings: com.example.ui.i18n.DynamicUiStrings,
    onSelectStep: (Int) -> Unit
) {
    val steps = listOf(
        1 to uiStrings.step1Tab,
        2 to uiStrings.step2Tab,
        3 to uiStrings.step3Tab,
        4 to uiStrings.step4Tab
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            steps.forEach { (stepNum, title) ->
                val isSelected = currentStep == stepNum
                val isCompleted = stepNum < 4 && currentStep > stepNum && currentStep != 4
                val isEnabled = stepNum <= 2 || stepNum == 4 || hasBookedOrder

                val pillBg by animateColorAsState(
                    targetValue = when {
                        isSelected -> BrandOrange
                        isCompleted -> SoftTealSurface
                        isEnabled -> SoftBg
                        else -> Color(0xFFE2E8F0)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "wizardPillBg"
                )
                val pillElevation by animateDpAsState(
                    targetValue = if (isSelected) 3.dp else 0.dp,
                    label = "wizardPillElevation"
                )

                Surface(
                    color = pillBg,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = pillElevation,
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isSelected -> BrandOrange
                            isCompleted -> PrimaryTeal.copy(alpha = 0.45f)
                            else -> Color.Transparent
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isEnabled) { onSelectStep(stepNum) }
                        .testTag("wizard_step_tab_$stepNum")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "✓ ${title.substringAfter(". ")}" else title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = when {
                                isSelected -> Color.White
                                isCompleted -> PrimaryTeal
                                isEnabled -> DeepNavy
                                else -> Color(0xFF94A3B8)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerFareLineRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun CustomerLightFareRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
            maxLines = 1,
            softWrap = false
        )
    }
}

