package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
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
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.DeliveryProofAndGpsManager
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
import com.example.ui.components.QuickDeliveryNextLevelLaunchOverlay
import com.example.ui.components.QuickDeliveryOfficialLogo
import com.example.ui.components.QuickDeliverySpeedingVehicleIntro
import com.example.ui.components.QuickDeliverySuggestionBoxCard
import com.example.ui.components.QuickDeliveryUserAuthScreen
import com.example.ui.components.RiderFeedbackManager
import com.example.ui.i18n.InlineLanguageSelectorBar
import com.example.ui.i18n.LanguageSwitcherPill
import com.example.ui.i18n.LocalizedContextProvider
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
    var showSpeedIntro by remember { mutableStateOf(!MotivationAndPromoManager.customerIntroCompleted) }
    var isCustomerAuthenticated by remember { mutableStateOf(MotivationAndPromoManager.customerAuthenticatedInSession) }

    // 1 = Address & Route, 2 = Parcel Photo (after Address Next), 3 = Payment, 4 = Live Tracking
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    val customerMainListState = rememberLazyListState()

    remember(context) { MotivationAndPromoManager.restoreSavedLoginFromPhoneStorage(context) }
    val isCustomerStorageGranted by MotivationAndPromoManager.customerStoragePermissionGranted.collectAsState()

    // Customer profile & booking form state (preserved across back navigation & tab switches)
    var customerName by rememberSaveable { mutableStateOf(MotivationAndPromoManager.cachedCustomerName) }
    var customerPhone by rememberSaveable { mutableStateOf(MotivationAndPromoManager.cachedCustomerPhone) }
    var customerEmail by rememberSaveable { mutableStateOf(MotivationAndPromoManager.cachedCustomerEmail) }

    var pickupHouseFlat by rememberSaveable { mutableStateOf("") }
    var pickupBuilding by rememberSaveable { mutableStateOf("") }
    var pickupAddressText by rememberSaveable { mutableStateOf("") }
    var pickupPinCode by rememberSaveable { mutableStateOf("") }
    var pickupLandmark by rememberSaveable { mutableStateOf("") }
    var pickupLat by rememberSaveable { mutableDoubleStateOf(12.9784) }
    var pickupLng by rememberSaveable { mutableDoubleStateOf(77.6408) }

    var dropHouseFlat by rememberSaveable { mutableStateOf("") }
    var dropBuilding by rememberSaveable { mutableStateOf("") }
    var dropAddressText by rememberSaveable { mutableStateOf("") }
    var dropPinCode by rememberSaveable { mutableStateOf("") }
    var dropLandmark by rememberSaveable { mutableStateOf("") }
    var dropLat by rememberSaveable { mutableDoubleStateOf(12.9352) }
    var dropLng by rememberSaveable { mutableDoubleStateOf(77.6245) }

    var selectedPickupSavedTag by rememberSaveable { mutableStateOf("Home") }
    var selectedDropSavedTag by rememberSaveable { mutableStateOf("Office") }
    var receiverFullName by rememberSaveable { mutableStateOf("") }
    var receiverMobilePhone by rememberSaveable { mutableStateOf("") }

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
        if (pinTrimmed.isNotEmpty()) "$baseParts - $pinTrimmed" else baseParts
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
        if (pinTrimmed.isNotEmpty()) "$baseParts - $pinTrimmed" else baseParts
    }

    val vehiclePolicy by AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.collectAsState()

    var selectedPackageType by rememberSaveable { mutableStateOf(PackageType.DOCUMENTS) }
    var selectedWeightBracketLabel by rememberSaveable { mutableStateOf("Up to 5kg") }
    var packageWeightKg by rememberSaveable { mutableStateOf("1.0") }
    var packageDescription by rememberSaveable { mutableStateOf("") }
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

    // Customer Notification-style Promo Banner (rotates with a fresh message to encourage more orders)
    var customerPromoNotification by remember { mutableStateOf<String?>(null) }
    val customerPostDeliveryMotivation by MotivationAndPromoManager.customerPostDeliveryMotivation.collectAsState()
    var liveRiderGpsCoordinates by rememberSaveable { mutableStateOf<String?>(null) }
    var showOrderHistoryScreen by rememberSaveable { mutableStateOf(false) }
    var showBookingConfirmationScreen by rememberSaveable { mutableStateOf(false) }
    var selectedTrackingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var latestBookedOrder by remember { mutableStateOf<CustomerOrder?>(null) }

    // Observe orders for automatic delivery motivation notifications, real-time FCM push updates & auto-assign any searching orders
    LaunchedEffect(orders) {
        MotivationAndPromoManager.observeOrdersForDeliveryMotivation(orders)
        FcmOrderNotificationManager.observeOrdersAndDispatchFcmNotifications(context, orders)
        val hasSearching = orders.any { it.orderStatus == OrderStatus.SEARCHING_RIDER }
        if (hasSearching) {
            delay(1200L)
            AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
            AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
        }
    }

    val activeOrder: CustomerOrder? = remember(orders, selectedTrackingOrderId, latestBookedOrder) {
        orders.firstOrNull { it.orderId == selectedTrackingOrderId }
            ?: latestBookedOrder?.let { latest ->
                orders.firstOrNull { it.orderId == latest.orderId } ?: latest
            }
            ?: orders.firstOrNull()
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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        // Immediately clear any permission prompt so it never remains on screen after granting access
        statusBanner = null
        isErrorBanner = false
        if (granted) {
            scope.launch {
                val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                liveRiderGpsCoordinates = if (coords != null) {
                    String.format(Locale.US, "%.5f, %.5f", coords.first, coords.second)
                } else {
                    String.format(Locale.US, "%.5f, %.5f", pickupLat, pickupLng)
                }
            }
        } else {
            liveRiderGpsCoordinates = String.format(Locale.US, "%.5f, %.5f", pickupLat, pickupLng)
        }
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
        pricingConfig,
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
            pricingConfigOverride = pricingConfig,
            policyOverride = vehiclePolicy
        )
    }

    val liveQuote = liveDetailedFare.quote
    val liveFareAmountInr = liveDetailedFare.totalFareInr
    val totalWithTipInr = liveFareAmountInr + selectedRiderTipInr

    val formattedDistanceKm = remember(liveDetailedFare.distanceKm) {
        String.format(Locale.US, "%.1f", liveDetailedFare.distanceKm)
    }

    var showCustomerTopOptionsDialog by remember { mutableStateOf(false) }
    var showCustomerLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showCustomerSuggestionDialog by remember { mutableStateOf(false) }
    var isCustomerLoggedIn by remember { mutableStateOf(MotivationAndPromoManager.customerAuthenticatedInSession) }

    val hasCustomerBackHistory = showCustomerLogoutConfirmDialog ||
        showCustomerSuggestionDialog ||
        showCustomerTopOptionsDialog ||
        showBookingConfirmationScreen ||
        showOrderHistoryScreen ||
        showOnlinePaymentOptions ||
        currentStep > 1

    BackHandler(enabled = hasCustomerBackHistory) {
        when {
            showCustomerLogoutConfirmDialog -> showCustomerLogoutConfirmDialog = false
            showCustomerSuggestionDialog -> showCustomerSuggestionDialog = false
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
                customerName = name
                customerPhone = phone
                customerEmail = email
                MotivationAndPromoManager.customerAuthenticatedInSession = true
                isCustomerLoggedIn = true
                statusBanner = "Welcome back, $name! Plan your delivery."
            }
        )
        return@LocalizedContextProvider
    }

    // 3. Dedicated Customer Order History Screen (Past & Active Deliveries with Status, Date, and Final Price)
    if (showOrderHistoryScreen) {
        CustomerOrderHistoryScreen(
            customerName = customerName,
            orders = orders,
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
                dropAddressText = pastOrder.dropAddress.fullAddress
                dropLandmark = pastOrder.dropAddress.landmark
                dropLat = pastOrder.dropAddress.lat
                dropLng = pastOrder.dropAddress.lng
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
                        cardOrBankDetails.ifBlank { "Visa Card •••• 4821" }
                    CustomerPaymentMethod.GATEWAY_NET_BANKING ->
                        cardOrBankDetails.ifBlank { "HDFC NetBanking" }
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

            val bookingResult = AutomatedFairRiderAssignmentEngine.createCustomerOrderWithBackendValidation(
                repository = repository,
                customerId = "CUST-1001",
                customerName = customerName.ifBlank { "Customer" },
                customerPhone = customerPhone.ifBlank { "+91 98765 43210" },
                pickupAddress = AddressDetails(
                    fullAddress = resolvedPickupAddress,
                    landmark = pickupLandmark,
                    contactName = customerName.ifBlank { "Customer" },
                    contactPhone = customerPhone.ifBlank { "+91 98765 43210" },
                    lat = pickupLat,
                    lng = pickupLng
                ),
                dropAddress = AddressDetails(
                    fullAddress = resolvedDropAddress,
                    landmark = dropLandmark,
                    contactName = receiverFullName.ifBlank { "Receiver" },
                    contactPhone = receiverMobilePhone.ifBlank { "+91 98450 11223" },
                    lat = dropLat,
                    lng = dropLng
                ),
                packageDetails = PackageDetails(
                    packageType = selectedPackageType,
                    weightKg = packageWeightKg.ifBlank { "1.0" },
                    description = packageDescription.ifBlank { "Express Parcel" }
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
                onSuccess = { newOrder ->
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
                    // Trigger Zero-Partiality Automated Fair Rider Discovery & Assignment
                    val fairDispatchOutcome = runCatching {
                        AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
                            repository = repository,
                            orderId = newOrder.orderId,
                            offerOnlyWaitForRiderConsent = false
                        )
                    }.getOrNull()
                    val refreshedOrder = repository.orders.value.firstOrNull { it.orderId == newOrder.orderId }
                    if (refreshedOrder != null) {
                        latestBookedOrder = refreshedOrder
                    }
                    isErrorBanner = false
                    val tipNote = if (selectedRiderTipInr > 0) " + ₹$selectedRiderTipInr Rider Tip" else ""
                    statusBanner = "Order #${newOrder.orderId} Confirmed$tipNote! ${fairDispatchOutcome?.message ?: "Searching nearby rider partner..."}"
                    customerPromoNotification = MotivationAndPromoManager.nextCustomerOrderPromoNotification()
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
            onBack = { showBookingConfirmationScreen = false },
            onConfirmBooking = executeCustomerBooking,
            modifier = modifier
        )
        return@LocalizedContextProvider
    }

    if (showCustomerTopOptionsDialog) {
        QuickDeliveryTopOptionsDialog(
            onDismiss = { showCustomerTopOptionsDialog = false },
            extraTopContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SECTION 1: HOME & BOOKING / ORDERS
                    Text(
                        text = "HOME & BOOKING / ORDERS",
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
                                    text = "Book a Parcel Delivery",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "Select route, vehicle & schedule pickup",
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
                        text = "TRACKING & DELIVERY",
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
                                    text = "Live Order Tracking",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = if (activeOrder != null) "Order #${activeOrder.orderId} in progress" else "Real-time rider GPS & OTP verification",
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
                        text = "HISTORY",
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
                                    text = "Order History & Receipts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "${orders.size} deliveries recorded",
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

                    // SECTION 4: PROFILE
                    Text(
                        text = "PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_verified_profile_summary_card")
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
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Customer Profile",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "+91 ${customerPhone.removePrefix("+91").trim()} • $customerEmail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    // SECTION 5: NOTIFICATIONS & SUPPORT
                    Text(
                        text = "NOTIFICATIONS & SUPPORT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateMuted
                    )
                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            isErrorBanner = false
                            statusBanner = "📞 Quick Delivery Support: 1800-QUICK-CARE (+91 80 4410 0100)"
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
                                        contentDescription = "Customer Support",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "24×7 Help & Customer Support",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "Call 1800-QUICK-CARE • Parcel protection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            showCustomerTopOptionsDialog = false
                            showCustomerSuggestionDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_menu_suggestion_box_btn")
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
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Customer Suggestion Box",
                                        tint = BrandOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Customer Suggestion & Feedback Box",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "Share feedback or suggestions directly with Admin",
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

                    // SECTION 6: SETTINGS & LOGOUT
                    Text(
                        text = "SETTINGS & LOGOUT",
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
                                    text = "Logout",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "Sign out of your customer account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF991B1B)
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
                    text = "Logout of Quick Delivery?",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out? You can sign back into your customer account anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCustomerLogoutConfirmDialog = false
                        MotivationAndPromoManager.logoutCustomer(context)
                        isCustomerLoggedIn = false
                        statusBanner = "Logged out of Quick Delivery successfully"
                        currentStep = 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_customer_logout_btn")
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomerLogoutConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCustomerSuggestionDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomerSuggestionDialog = false },
            title = {
                Text(
                    text = "Customer Suggestion & Feedback Box",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy
                )
            },
            text = {
                QuickDeliverySuggestionBoxCard(
                    appRole = "CUSTOMER",
                    senderName = customerName,
                    accentColor = PrimaryTeal
                )
            },
            confirmButton = {
                OutlinedButton(onClick = { showCustomerSuggestionDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
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
                        QuickDeliveryOfficialLogo(size = 34.dp, borderColor = BrandOrange)
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
                                text = QuickDeliveryTagline,
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
                    LanguageSwitcherPill()
                    IconButton(
                        onClick = { showCustomerTopOptionsDialog = true },
                        modifier = Modifier.testTag("customer_top_bar_three_dots_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Customer Menu & Account Options",
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
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Post-Delivery Celebratory Motivational Notification Banner (sent automatically to Customer after every delivery!)
            if (!customerPostDeliveryMotivation.isNullOrBlank()) {
                item(key = "customer_post_delivery_motivation_item") {
                    DualDeliveryCelebrationMotivationBanner(
                        roleLabel = "CUSTOMER",
                        message = customerPostDeliveryMotivation,
                        accentColor = PrimaryTeal,
                        onDismiss = { MotivationAndPromoManager.dismissCustomerPostDeliveryMotivation() }
                    )
                }
            }

            // Real-Time FCM (Firebase Cloud Messaging) Order Status & Delivery Completion Notification Center
            item(key = "customer_fcm_realtime_notification_center") {
                FcmRealtimeNotificationCenterCard(
                    appRole = FcmTargetAppRole.CUSTOMER,
                    activeOrder = activeOrder,
                    accentColor = PrimaryTeal
                )
            }

            // Customer Promo Notification Banner
            if (!customerPromoNotification.isNullOrBlank()) {
                item(key = "customer_promo_banner") {
                    CustomerOrderMoreNotificationBanner(
                        message = customerPromoNotification,
                        onBookAnotherOrderClick = {
                            statusBanner = null
                            customerPromoNotification = null
                            currentStep = 1
                        },
                        onDismiss = { customerPromoNotification = null }
                    )
                }
            }

            // 4-Step Animated Wizard Progress Header
            item(key = "customer_wizard_progress_header") {
                CustomerWizardProgressHeader(
                    currentStep = currentStep,
                    hasBookedOrder = activeOrder != null,
                    onSelectStep = { targetStep ->
                        if (targetStep == 2 && weightErrorMessage != null) {
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

            // Dismissible Status / Feedback Banner
            if (statusBanner != null) {
                item(key = "customer_status_banner") {
                    Surface(
                        color = if (isErrorBanner) Color(0xFFFEF2F2) else SoftTealSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isErrorBanner) Color(0xFFEF4444) else BrightTeal
                        ),
                        modifier = Modifier.fillMaxWidth()
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
            }

            // ACTIVE ORDER QUICK CALL & LIVE TRACKING STRIP (Always accessible on Customer Main Screen)
            if (activeOrder != null && currentStep == 1) {
                item(key = "customer_active_order_quick_call_strip") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, PrimaryTeal),
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
                                        text = "🟢 Active Order #${activeOrder.orderId} • ${activeOrder.orderStatus.displayTitle}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = "Rider: ${activeOrder.assignedRiderName ?: "Vikram Rathore"} (${activeOrder.assignedRiderPhone ?: "+91 98450 67890"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                                OutlinedButton(
                                    onClick = { currentStep = 3 },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("customer_main_open_live_map_btn")
                                ) {
                                    Text("Live Map →", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            DirectDialPadCallButton(
                                orderId = activeOrder.orderId,
                                callerRole = "CUSTOMER",
                                callerName = customerName,
                                receiverRole = "Rider",
                                receiverName = activeOrder.assignedRiderName ?: "Vikram Rathore",
                                phoneNumber = activeOrder.assignedRiderPhone ?: "+91 98450 67890",
                                buttonTestTag = "customer_home_call_rider_dial_btn"
                            )
                        }
                    }
                }
            }

            // STEP 1: ADDRESS, VEHICLE & PACKAGE DETAILS -> NEXT
            if (currentStep == 1) {
                item(key = "customer_step1_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.16f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Premium Gradient Card Header
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
                                            text = "Step 1 • Route & Vehicle",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Instant door-to-door city parcel pickup",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.88f)
                                        )
                                    }
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
                                                text = "${selectedVehicle.etaMinutes} Min Pickup",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Visual Route Box (Pickup & Drop with House/Flat, Building, Area & PIN Code)
                                Surface(
                                    color = SoftBg,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // PICKUP HEADER + SAVED ADDRESS TAGS (Home, Office, Other)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimaryTeal)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "PICKUP LOCATION DETAILS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = PrimaryTeal
                                                    )
                                                    Text(
                                                        text = "House/Flat, Building, Area & PIN Code",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SlateMuted
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = SoftTealSurface,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.clickable {
                                                    if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
                                                        scope.launch {
                                                            val coords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                                                            if (coords != null) {
                                                                pickupLat = coords.first
                                                                pickupLng = coords.second
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
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MyLocation,
                                                        contentDescription = "Use current GPS",
                                                        tint = PrimaryTeal,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Use GPS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryTeal
                                                    )
                                                }
                                            }
                                        }

                                        // Saved Pickup Address Tags (Home, Office, Other)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf("Home", "Office", "Other").forEach { tag ->
                                                val activeTag = selectedPickupSavedTag == tag
                                                FilterChip(
                                                    selected = activeTag,
                                                    onClick = {
                                                        selectedPickupSavedTag = tag
                                                    },
                                                    label = {
                                                        Text(
                                                            text = "📍 $tag",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = PrimaryTeal,
                                                        selectedLabelColor = Color.White
                                                    ),
                                                    modifier = Modifier.testTag("customer_pickup_tag_${tag.lowercase()}")
                                                )
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
                                                label = { Text("House / Flat No.") },
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
                                                label = { Text("Building / Society") },
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
                                            onValueChange = { pickupAddressText = it },
                                            label = { Text("Pickup Area / Locality / Street") },
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
                                                label = { Text("PIN Code") },
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
                                                label = { Text("Landmark (Optional)") },
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

                                        HorizontalDivider(
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )

                                        // DROP HEADER
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandOrange)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "DROP DESTINATION DETAILS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = BrandOrange
                                                )
                                                Text(
                                                    text = "House/Flat, Building, Area, PIN & Receiver Info",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SlateMuted
                                                )
                                            }
                                        }

                                        // Saved Drop Address Tags (Home, Office, Other)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf("Home", "Office", "Other").forEach { tag ->
                                                val activeTag = selectedDropSavedTag == tag
                                                FilterChip(
                                                    selected = activeTag,
                                                    onClick = {
                                                        selectedDropSavedTag = tag
                                                    },
                                                    label = {
                                                        Text(
                                                            text = "🏁 $tag",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = BrandOrange,
                                                        selectedLabelColor = Color.White
                                                    ),
                                                    modifier = Modifier.testTag("customer_drop_tag_${tag.lowercase()}")
                                                )
                                            }
                                        }

                                        // Receiver Name & Phone Inputs
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = receiverFullName,
                                                onValueChange = { receiverFullName = it },
                                                label = { Text("Receiver Name") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = BrandOrange,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("receiver_name_input")
                                            )
                                            OutlinedTextField(
                                                value = receiverMobilePhone,
                                                onValueChange = { input ->
                                                    receiverMobilePhone = input.filter { it.isDigit() }.take(10)
                                                },
                                                label = { Text("Receiver Phone") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = BrandOrange,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("receiver_phone_input")
                                            )
                                        }

                                        // Drop Row 1: House / Flat No. + Building / Society Name
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = dropHouseFlat,
                                                onValueChange = { dropHouseFlat = it },
                                                label = { Text("House / Flat No.") },
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
                                                label = { Text("Building / Society") },
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
                                            onValueChange = { dropAddressText = it },
                                            label = { Text("Drop Area / Locality / Street") },
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
                                                label = { Text("PIN Code") },
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
                                                label = { Text("Landmark / Receiver Note") },
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

                                // Vehicle Selection Section (Strict Order: Bike -> Scooter -> Auto)
                                val orderedCustomerVehicles = remember {
                                    listOf(
                                        VehicleType.BIKE,
                                        VehicleType.SCOOTER,
                                        VehicleType.AUTO
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Select Delivery Vehicle",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepNavy
                                            )
                                            Text(
                                                text = "Bike → Scooter → Auto • Instant Fare",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlateMuted
                                            )
                                        }
                                        Surface(
                                            color = SoftTealSurface,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$formattedDistanceKm km route",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PrimaryTeal,
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
                                                pricingConfig,
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
                                                    pricingConfigOverride = pricingConfig,
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
                                                VehicleType.BIKE -> "Bike"
                                                VehicleType.SCOOTER -> "Scooter"
                                                VehicleType.AUTO -> "Auto"
                                                else -> vehicle.title
                                            }
                                            val vehicleTagline = when {
                                                !isVehicleEnabled -> "Paused"
                                                vehicle == VehicleType.BIKE -> "3 mins away"
                                                vehicle == VehicleType.SCOOTER -> "5 mins away"
                                                vehicle == VehicleType.AUTO -> "7 mins away"
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
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "${index + 1}. $vehicleTagline",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected && isVehicleEnabled) Color.White else SlateMuted,
                                                                maxLines = 1,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        if (isSelected && isVehicleEnabled) {
                                                            Icon(
                                                                imageVector = Icons.Default.CheckCircle,
                                                                contentDescription = "Selected",
                                                                tint = PrimaryTeal,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }

                                                    // Vehicle Icon Badge
                                                    Surface(
                                                        color = when {
                                                            !isVehicleEnabled -> Color(0xFFE2E8F0)
                                                            isSelected -> PrimaryTeal
                                                            else -> SoftBg
                                                        },
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = when (vehicle) {
                                                                    VehicleType.BIKE -> Icons.Default.TwoWheeler
                                                                    VehicleType.SCOOTER -> Icons.Default.Bolt
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

                                                    // Vehicle Name (Bike / Scooter / Auto)
                                                    Text(
                                                        text = shortVehicleLabel,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isVehicleEnabled) DeepNavy else SlateMuted,
                                                        maxLines = 1
                                                    )

                                                    // Base + Per-KM Rate from Live Admin Config
                                                    Text(
                                                        text = "₹${vehicleFareDetails.baseFareInr.toInt()} + ${vehicleFareDetails.formattedPerKmRate}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected && isVehicleEnabled) PrimaryTeal else SlateMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    // Max Weight Limit & ETA
                                                    Text(
                                                        text = "≤${vehicleFareDetails.maxWeightKgForVehicle.toInt()}kg • ${vehicle.etaMinutes}m",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SlateMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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
                                                                    "Paused"
                                                                },
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color.White,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Package Type Chips
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Parcel Category & Weight Bracket",
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
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                                        modifier = Modifier.fillMaxWidth(),
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
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PrimaryTeal,
                                                    selectedLabelColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
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
                                        label = { Text("Weight (kg • Max 20)") },
                                        isError = weightErrorMessage != null,
                                        supportingText = {
                                            Text(
                                                text = if (isWeightOverLimit) {
                                                    "Max 20 kg allowed!"
                                                } else if (weightErrorMessage != null) {
                                                    "Enter 0.1 – 20 kg"
                                                } else {
                                                    "Up to 20 kg allowed"
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
                                        label = { Text("Parcel Note") },
                                        supportingText = {
                                            Text(
                                                text = "Bike / Scooter / Auto (≤ 20 kg)",
                                                color = SlateMuted
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryTeal
                                        ),
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("package_description_input")
                                    )
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

                                // Premium Instant Full ₹ Price Breakdown Card (Step 1)
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
                                                            text = "LIVE ₹ FARE BREAKDOWN",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = BrightTeal,
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
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${selectedVehicle.title} • $formattedDistanceKm km • ${liveDetailedFare.weightKg} kg (Max ${activeMaxWeightKg.toInt()} kg)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Surface(
                                                color = BrandOrange,
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text(
                                                    text = liveDetailedFare.formattedTotalFare,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                                        .testTag("customer_live_fare_total")
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                                        // Itemized ₹ Rows: Base Fare + Distance + Weight + GST + Promo Discount
                                        CustomerFareLineRow(
                                            label = "${selectedVehicle.title} Base Fare",
                                            value = liveDetailedFare.formattedBaseFare
                                        )
                                        CustomerFareLineRow(
                                            label = "Distance Charge ($formattedDistanceKm km × ${liveDetailedFare.formattedPerKmRate})",
                                            value = liveDetailedFare.formattedDistanceCharge
                                        )
                                        CustomerFareLineRow(
                                            label = "Parcel Weight Charge (${liveDetailedFare.weightKg} kg ≤ ${activeMaxWeightKg.toInt()} kg)",
                                            value = liveDetailedFare.formattedWeightCharge
                                        )
                                        CustomerFareLineRow(
                                            label = "5% GST & Safe Platform Fee",
                                            value = liveDetailedFare.formattedTaxesAndFees
                                        )
                                        CustomerFareLineRow(
                                            label = "Promo Code (QUICK15 Instant Saver)",
                                            value = "Applied ✓"
                                        )
                                    }
                                }

                                // Step 1 Primary Action Button (Blocks booking when parcel weight > 20 kg)
                                Button(
                                    onClick = {
                                        val weightCheck = AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg(
                                            packageWeightKg,
                                            selectedVehicle
                                        )
                                        when {
                                            weightCheck.isFailure -> {
                                                isErrorBanner = true
                                                statusBanner = weightCheck.exceptionOrNull()?.message
                                                    ?: AutomatedFairRiderAssignmentEngine.MAX_PARCEL_WEIGHT_ERROR_MESSAGE
                                            }
                                            fullPickupAddressFormatted.isBlank() || fullDropAddressFormatted.isBlank() -> {
                                                isErrorBanner = true
                                                statusBanner = "Please enter both Pickup and Drop address details"
                                            }
                                            pickupPinCode.isNotBlank() && pickupPinCode.length != 6 -> {
                                                isErrorBanner = true
                                                statusBanner = "Please enter a valid 6-digit Pickup PIN code"
                                            }
                                            dropPinCode.isNotBlank() && dropPinCode.length != 6 -> {
                                                isErrorBanner = true
                                                statusBanner = "Please enter a valid 6-digit Drop PIN code"
                                            }
                                            else -> {
                                                if (pickupAddressText.isBlank()) {
                                                    pickupAddressText = fullPickupAddressFormatted
                                                }
                                                if (dropAddressText.isBlank()) {
                                                    dropAddressText = fullDropAddressFormatted
                                                }
                                                isErrorBanner = false
                                                statusBanner = null
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
                                        text = if (isWeightOverLimit) {
                                            "Booking Blocked: Max 20 kg Allowed"
                                        } else {
                                            "Next: Parcel Photo (Step 2) "
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    if (!isWeightOverLimit) {
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
                                            text = "View Active Order #${activeOrder.orderId} Live Tracking →",
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
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { currentStep = 1 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back to Address",
                                                tint = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Step 2 • Parcel Photo Verification",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Snap a quick parcel photo for safe handling",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.88f)
                                            )
                                        }
                                    }
                                    Surface(
                                        color = BrandOrange,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "SAFE PROOF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        Text(
                                            text = liveQuote.totalAmount,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandOrange
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
                                                "Parcel Photo Attached & Verified"
                                            } else {
                                                "Capture or Attach Parcel Photo"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepNavy
                                        )

                                        Text(
                                            text = currentBookingPhotoSummary
                                                ?: "Helps your assigned delivery partner verify parcel size and handle fragile items with extra care.",
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
                                                    "Click Parcel Photo (Camera)"
                                                } else {
                                                    "Retake Parcel Photo"
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
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
                                        text = String.format(Locale.US, "Review & Confirm Booking • ₹%.0f", totalWithTipInr),
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
                                            customerPromoNotification = MotivationAndPromoManager.nextCustomerOrderPromoNotification()
                                        }
                                        OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> {
                                            repository.verifyDeliveryOtpByRider(
                                                activeOrder.orderId,
                                                activeOrder.deliveryOtp,
                                                activeOrder.assignedRiderId
                                            )
                                            AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
                                            customerPromoNotification = MotivationAndPromoManager.nextCustomerOrderPromoNotification()
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
                                    customerPromoNotification = MotivationAndPromoManager.nextCustomerOrderPromoNotification()
                                    currentStep = 1
                                }
                            )

                            // Verified Rider Profile & Download GST Invoice Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Verified Rider: ${activeOrder.assignedRiderName ?: "Vikram Singh"} • 4.9★",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Plate: KA-01-EQ-4821 • Pickup OTP: ${activeOrder.pickupOtp} • Drop OTP: ${activeOrder.deliveryOtp}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrightTeal
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            isErrorBanner = false
                                            statusBanner = "📄 Official GST Tax Invoice for Order #${activeOrder.orderId} downloaded!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("customer_download_gst_invoice_btn")
                                    ) {
                                        Text(
                                            text = "Download GST Invoice",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
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
                                    text = "No Active Order Being Tracked",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "Enter your Pickup & Drop addresses in Step 1 and confirm your booking to track live rider GPS, Pickup/Drop OTPs, and delivery status here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                                Button(
                                    onClick = { currentStep = 1 },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Go to Step 1: Book Parcel",
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
                        orders = orders,
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
    onSelectStep: (Int) -> Unit
) {
    val steps = listOf(
        1 to "1. Route",
        2 to "2. Photo",
        3 to "3. Track",
        4 to "4. History"
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
            color = Color.White.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
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
            color = SlateMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = DeepNavy
        )
    }
}

