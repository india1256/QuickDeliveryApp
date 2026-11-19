package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AddressDetails
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.GatewayCallbackStatus
import com.example.data.model.OrderStatus
import com.example.data.model.PackageDetails
import com.example.data.model.PackageType
import com.example.data.model.PaymentVerificationStatus
import com.example.data.model.UpiAppOption
import com.example.data.model.VehicleType
import com.example.data.model.WalletAccountRole
import com.example.data.repository.SharedOrderRepository
import com.example.security.RiderBatteryPowerMonitor
import com.example.ui.auth.findActivity
import com.example.ui.wallet.WalletViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityAndPricingIntegrationTest {

    private lateinit var sharedRepo: SharedOrderRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedRepo = SharedOrderRepository.getInstance(context)
        sharedRepo.resetOrdersAndStateForTesting()
        RiderBatteryPowerMonitor.clearSimulation(context)
    }

    @Test
    fun testStep41CompletePaymentRiderWalletCommissionDebtLimitAndAdminCompanyWallet() = runTest {
        val adminToken = sharedRepo.issueAdminPreviewSessionToken()

        val currentAuto = sharedRepo.automationConfig.value
        sharedRepo.updateAdminAutomationConfig(currentAuto.copy(autoMatchEligibleRiders = false), adminToken)

        // 1. Online Gateway Payment verification & tamper rejection
        val unverifiedOnlineOrder = sharedRepo.createCustomerOrder(
            customerId = "CUST-4101",
            customerName = "Kunal Verma",
            customerPhone = "9876541234",
            pickupAddress = AddressDetails("Indiranagar 100ft", "Metro", "Kunal", "9876541234", lat = 12.9814, lng = 77.6438),
            dropAddress = AddressDetails("Koramangala 4th Block", "Signal", "Rohan", "9845012345", lat = 12.9382, lng = 77.6275),
            packageDetails = PackageDetails(PackageType.DOCUMENTS, "1.0", "Contract"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.GATEWAY_UPI,
            selectedUpiApp = UpiAppOption.GOOGLE_PAY,
            verifiedGatewayOrderId = null
        ).getOrThrow()

        assertFalse(unverifiedOnlineOrder.isPaidToCompany)
        assertEquals(PaymentVerificationStatus.PENDING_GATEWAY_VERIFICATION, unverifiedOnlineOrder.paymentStatus)

        val expectedFareInr = unverifiedOnlineOrder.totalAmount.replace("₹", "").replace(",", "").trim().toDouble()

        val gwInit = sharedRepo.initiateGatewayPaymentOrder(
            purpose = "CUSTOMER_ORDER",
            referenceId = unverifiedOnlineOrder.orderId,
            amountInr = expectedFareInr,
            paymentMethod = CustomerPaymentMethod.GATEWAY_UPI,
            selectedUpiApp = UpiAppOption.GOOGLE_PAY
        ).getOrThrow()

        val tamperedVerify = sharedRepo.verifyAndProcessGatewayCallback(
            gatewayOrderId = gwInit.gatewayOrderId,
            gatewayPaymentId = "pay_tampered_001",
            callbackStatus = GatewayCallbackStatus.SUCCESS,
            providedSignature = "deadbeef_invalid_hmac_signature"
        )
        assertTrue(tamperedVerify.isFailure)

        val validSigSuccess = sharedRepo.signGatewayCallback(
            gatewayOrderId = gwInit.gatewayOrderId,
            gatewayPaymentId = "pay_valid_001",
            callbackStatus = GatewayCallbackStatus.SUCCESS
        ).getOrThrow()
        val verifiedGw = sharedRepo.verifyAndProcessGatewayCallback(
            gatewayOrderId = gwInit.gatewayOrderId,
            gatewayPaymentId = "pay_valid_001",
            callbackStatus = GatewayCallbackStatus.SUCCESS,
            providedSignature = validSigSuccess
        ).getOrThrow()
        assertTrue(verifiedGw.signatureVerified)

        val updatedOnlineOrder = sharedRepo.orders.value.first { it.orderId == unverifiedOnlineOrder.orderId }
        assertTrue(updatedOnlineOrder.isPaidToCompany)
        assertEquals(PaymentVerificationStatus.VERIFIED_PAID_TO_COMPANY, updatedOnlineOrder.paymentStatus)

        // 2. Direct-to-Rider UPI order creation & ₹150 commission debt limit check
        val directUpiOrder = sharedRepo.createCustomerOrder(
            customerId = "CUST-1001",
            customerName = "Aarav Sharma",
            customerPhone = "9876543210",
            pickupAddress = AddressDetails("MG Road", "Metro", "Aarav", "9876543210", lat = 12.9750, lng = 77.6060),
            dropAddress = AddressDetails("HSR Layout", "Sector 2", "Kiran", "9845099999", lat = 12.9121, lng = 77.6446),
            packageDetails = PackageDetails(PackageType.GROCERIES, "2.0", "Direct UPI Parcel"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.DIRECT_RIDER_UPI,
            idempotencyKey = "IDEMP-DIRECT-UPI-41"
        ).getOrThrow()
        assertFalse(directUpiOrder.isPaidToCompany)

        // Settle initial ₹90 debt to ₹0
        sharedRepo.repayRiderCommissionOnline(
            riderId = "RIDER-101",
            amountInr = 90.0,
            paymentMethod = CustomerPaymentMethod.GATEWAY_UPI,
            selectedUpiApp = UpiAppOption.PHONEPE,
            idempotencyKey = "REPAY-INIT-90"
        ).getOrThrow()
        assertEquals(0.0, sharedRepo.riders.value.first { it.riderId == "RIDER-101" }.commissionDebtInr, 0.01)

        // Set debt to ₹155.00 (> ₹150.00 limit) -> must block new order eligibility
        val riderAt155 = sharedRepo.setRiderCommissionDebt("RIDER-101", 155.0).getOrThrow()
        val blockedAt155 = sharedRepo.checkRiderEligibilityForNewOrder(riderAt155, projectedOrderCommissionInr = 0.0)
        assertTrue(blockedAt155.isFailure)

        // Repay ₹100.00 online via Paytm -> Debt drops to ₹55.00
        val repayTx = sharedRepo.repayRiderCommissionOnline(
            riderId = "RIDER-101",
            amountInr = 100.0,
            paymentMethod = CustomerPaymentMethod.GATEWAY_UPI,
            selectedUpiApp = UpiAppOption.PAYTM,
            idempotencyKey = "REPAY-100-PAYTM"
        ).getOrThrow()
        assertEquals(5500L, repayTx.commissionDebtAfterPaise)

        // 3. Admin Company Wallet RBAC & manual ledger adjustment
        assertTrue(sharedRepo.fetchCompanyWalletSummary("INVALID_TOKEN").isFailure)
        val adjEntry = sharedRepo.recordAdminManualLedgerAdjustment(
            amountInr = 25.0,
            isCreditToCompany = true,
            reason = "Audited Reconciliation Credit",
            adminToken = adminToken
        ).getOrThrow()
        assertEquals(2500L, adjEntry.amountPaise)
    }

    @Test
    fun testRiderBatteryPowerMonitorAndSharedWalletViewModel() = runTest {
        val walletVm = WalletViewModel(
            repository = sharedRepo,
            initialAccountId = "RIDER-101",
            initialRole = WalletAccountRole.RIDER
        )
        assertEquals(148000L, walletVm.uiState.value.balancePaise)
        assertEquals(15000L, walletVm.uiState.value.maxCommissionDebtPaise)

        // Simulate 15% critical battery state (< 20%) during active delivery: GPS update frequency throttles to 15s / 20m
        val criticalState = RiderBatteryPowerMonitor.simulateBatteryState(
            levelPercent = 15,
            isCharging = false,
            isPowerSaveMode = true
        )
        assertTrue(criticalState.isCriticalPower)
        assertEquals(15, criticalState.levelPercent)
        assertTrue("GPS must be throttled when battery is below 20%", criticalState.isGpsThrottled)
        assertEquals(15000L, criticalState.gpsUpdateIntervalMs)
        assertEquals(25.0f, criticalState.gpsMinDistanceMeters, 0.01f)
        assertTrue(criticalState.isGpsThrottled)

        // Healthy battery (80%, charging): Standard 2s / 1m GPS update frequency
        val healthyState = RiderBatteryPowerMonitor.simulateBatteryState(
            levelPercent = 80,
            isCharging = true,
            isPowerSaveMode = false
        )
        assertFalse(healthyState.isCriticalPower)
        assertFalse(healthyState.isGpsThrottled)
        assertEquals(2000L, healthyState.gpsUpdateIntervalMs)
        assertEquals(3.0f, healthyState.gpsMinDistanceMeters, 0.01f)

        // Low battery but plugged into charger (15%, charging): GPS is NOT throttled
        val chargingLowState = RiderBatteryPowerMonitor.simulateBatteryState(
            levelPercent = 15,
            isCharging = true,
            isPowerSaveMode = false
        )
        assertFalse("Charging battery must not throttle GPS updates", chargingLowState.isGpsThrottled)
        assertEquals(2000L, chargingLowState.gpsUpdateIntervalMs)

        // Boundary test: exactly 20% is normal, 19% (< 20%) is throttled
        val at20State = RiderBatteryPowerMonitor.simulateBatteryState(
            levelPercent = 20,
            isCharging = false,
            isPowerSaveMode = false
        )
        assertFalse(at20State.isGpsThrottled)
        assertEquals(2000L, at20State.gpsUpdateIntervalMs)

        val below20State = RiderBatteryPowerMonitor.simulateBatteryState(
            levelPercent = 19,
            isCharging = false,
            isPowerSaveMode = false
        )
        assertTrue("Battery below 20% (19%) must throttle GPS updates", below20State.isGpsThrottled)
        assertEquals(15000L, below20State.gpsUpdateIntervalMs)
    }

    @Test
    fun testRateLimitingInputSanitizationAndRoleSessionTokenSecurity() = runTest {
        com.example.security.QuickDeliverySecurityGuard.resetRateLimitsForTesting()

        // 1. Input Sanitization against XSS & NoSQL injection
        val dirtyInput = "<script>alert('hack')</script> Indiranagar \$where javascript:evil() 100ft Road"
        val cleanInput = com.example.security.QuickDeliverySecurityGuard.sanitizeText(dirtyInput, 200)
        assertFalse(cleanInput.contains("<script", ignoreCase = true))
        assertFalse(cleanInput.contains("javascript:", ignoreCase = true))
        assertFalse(cleanInput.contains("\$where", ignoreCase = true))
        assertTrue(cleanInput.contains("Indiranagar"))

        val context = ApplicationProvider.getApplicationContext<Context>()

        // 2. Role-bound HMAC-SHA256 Session Token verification & tamper protection
        val customerToken = com.example.security.QuickDeliverySecurityGuard.issueRoleSessionToken(
            context = context,
            role = "CUSTOMER",
            name = "Aarav Sharma",
            phone = "9876543210",
            email = "aarav@example.com"
        )
        assertTrue(
            com.example.security.QuickDeliverySecurityGuard.verifyRoleSessionToken(
                context = context,
                token = customerToken,
                expectedRole = "CUSTOMER",
                expectedPhone = "9876543210",
                expectedEmail = "aarav@example.com"
            )
        )
        // Must reject role privilege escalation (using a CUSTOMER token for ADMIN or RIDER)
        assertFalse(
            com.example.security.QuickDeliverySecurityGuard.verifyRoleSessionToken(
                context = context,
                token = customerToken,
                expectedRole = "ADMIN",
                expectedPhone = "9876543210",
                expectedEmail = "aarav@example.com"
            )
        )
        // Must reject tampered signature
        assertFalse(
            com.example.security.QuickDeliverySecurityGuard.verifyRoleSessionToken(
                context = context,
                token = customerToken + "tampered",
                expectedRole = "CUSTOMER",
                expectedPhone = "9876543210",
                expectedEmail = "aarav@example.com"
            )
        )

        // 3. Order Placement Rate Limiting & Duplicate Burst Spam Prevention
        val firstOrder = com.example.security.QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
            actorKey = "SPAM-TEST-USER-1",
            pickupAddress = "MG Road Metro",
            dropAddress = "Koramangala 4th Block",
            vehicleType = "BIKE",
            activePendingOrdersCount = 0
        )
        assertTrue(firstOrder.isSuccess)

        // Immediate identical duplicate route within 8s cooldown must be blocked
        val duplicateBurst = com.example.security.QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
            actorKey = "SPAM-TEST-USER-1",
            pickupAddress = "MG Road Metro",
            dropAddress = "Koramangala 4th Block",
            vehicleType = "BIKE",
            activePendingOrdersCount = 1
        )
        assertTrue(duplicateBurst.isFailure)

        // Flooding > 5 orders within 60 seconds must be rate-limited
        for (i in 2..5) {
            val allowed = com.example.security.QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
                actorKey = "SPAM-TEST-USER-1",
                pickupAddress = "Pickup Point $i",
                dropAddress = "Drop Point $i",
                vehicleType = "BIKE",
                activePendingOrdersCount = 0
            )
            assertTrue(allowed.isSuccess)
        }
        val sixthRapidOrder = com.example.security.QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
            actorKey = "SPAM-TEST-USER-1",
            pickupAddress = "Pickup Point 6",
            dropAddress = "Drop Point 6",
            vehicleType = "BIKE",
            activePendingOrdersCount = 0
        )
        assertTrue(sixthRapidOrder.isFailure)

        com.example.security.QuickDeliverySecurityGuard.resetRateLimitsForTesting()
    }

    @Test
    fun testMobileApkDownloadHelperAndLocalBrowserServer() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val outcome = com.example.security.MobileApkDownloadHelper.triggerApkDownload(
            context = context,
            variant = com.example.security.MobileApkVariant.CUSTOMER,
            openMobileBrowser = false
        )
        assertEquals("QuickDelivery-Customer.apk", outcome.fileName)
        assertTrue(outcome.fileSizeBytes > 0L)
        assertTrue(outcome.browserDownloadUrl.startsWith("http://127.0.0.1:"))

        val port = com.example.security.MobileApkDownloadHelper.ensureLocalBrowserServerRunning(context)
        assertTrue(port > 0)

        val connection = java.net.URL("http://127.0.0.1:$port/apks/QuickDelivery-Customer.apk")
            .openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        assertEquals(200, connection.responseCode)
        assertEquals("application/vnd.android.package-archive", connection.contentType)
        val disposition = connection.getHeaderField("Content-Disposition").orEmpty()
        assertTrue(disposition.contains("attachment"))
        assertTrue(disposition.contains("QuickDelivery-Customer.apk"))
        val downloadedBytes = connection.inputStream.use { it.readBytes() }
        assertTrue(downloadedBytes.isNotEmpty())
        connection.disconnect()
    }

    @Test
    fun testAdminUserIdAndPasswordCredentialsSecurity() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.security.AdminCredentialsManager.clearForTesting(context)
        assertFalse(com.example.security.AdminCredentialsManager.credentialsState.value.isConfigured)

        // Reject short User ID, weak password, and non-allowlisted recovery email
        assertTrue(
            com.example.security.AdminCredentialsManager.saveCredentials(
                context, "abc", "StrongPass123", "ravindergupta141@gmail.com"
            ).isFailure
        )
        assertTrue(
            com.example.security.AdminCredentialsManager.saveCredentials(
                context, "ravinder_admin", "12345", "ravindergupta141@gmail.com"
            ).isFailure
        )
        assertTrue(
            com.example.security.AdminCredentialsManager.saveCredentials(
                context, "ravinder_admin", "StrongPass123", "random@hacker.com"
            ).isFailure
        )

        // Valid save succeeds with 32-char salt and 64-char stretched SHA-256 hash
        val saved = com.example.security.AdminCredentialsManager.saveCredentials(
            context, "Ravinder_Admin", "Quick@2026", "ravindergupta141@gmail.com"
        ).getOrThrow()
        assertEquals("ravinder_admin", saved.userId)
        assertEquals(32, saved.saltHex.length)
        assertEquals(64, saved.hashHex.length)
        assertTrue(com.example.security.AdminCredentialsManager.credentialsState.value.isConfigured)

        // Valid login works by User ID (case-insensitive) and by allowlisted recovery email
        val loginById = com.example.security.AdminCredentialsManager.verifyLocalLogin(
            context, "RAVINDER_ADMIN", "Quick@2026"
        )
        assertTrue(loginById.isSuccess)
        assertEquals("ravindergupta141@gmail.com", loginById.getOrThrow())

        val loginByEmail = com.example.security.AdminCredentialsManager.verifyLocalLogin(
            context, "ravindergupta141@gmail.com", "Quick@2026"
        )
        assertTrue(loginByEmail.isSuccess)

        // Wrong password or wrong User ID must fail
        assertTrue(
            com.example.security.AdminCredentialsManager.verifyLocalLogin(
                context, "ravinder_admin", "WrongPass999"
            ).isFailure
        )
        assertTrue(
            com.example.security.AdminCredentialsManager.verifyLocalLogin(
                context, "other_admin", "Quick@2026"
            ).isFailure
        )

        // Current password verification for Settings password change
        assertTrue(com.example.security.AdminCredentialsManager.verifyCurrentPassword(context, "Quick@2026"))
        assertFalse(com.example.security.AdminCredentialsManager.verifyCurrentPassword(context, "WrongPass999"))

        com.example.security.AdminCredentialsManager.clearForTesting(context)
    }

    @Test
    fun testAdminMasterSyncUpdateGateRateCardAndFraudBlocklist() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val flags = com.example.data.cloud.AdminRemoteFeatureFlags(
            latestAppVersionName = "6.4.0",
            latestAppVersionCode = 78,
            forceUpdateRequired = true,
            updateNotes = "Critical speed upgrade",
            emergencyBookingsPaused = true,
            operatingHoursEnabled = true,
            operatingStartHour = 7,
            operatingEndHour = 23,
            nightSurchargeEnabled = true,
            nightStartHour = 23,
            nightEndHour = 6,
            nightSurchargeMultiplier = 1.25,
            remoteRateCardOverrideEnabled = true,
            bikeEnabled = true,
            scooterEnabled = false,
            autoEnabled = true,
            bikeBaseFareInr = 40.0,
            bikeRatePerKmInr = 12.0,
            smsOtpGatewayEnabled = true,
            blockedCustomerPhonesCsv = "9876543210, +919988776655",
            masterSyncRevision = 5
        )

        // 1. JSON round-trip preserves all Schema v4 controls
        val roundTripped = com.example.data.cloud.AdminRemoteFeatureFlags.fromJson(flags.toJson())
        assertEquals("6.4.0", roundTripped.latestAppVersionName)
        assertEquals(78, roundTripped.latestAppVersionCode)
        assertTrue(roundTripped.forceUpdateRequired)
        assertTrue(roundTripped.emergencyBookingsPaused)
        assertTrue(roundTripped.smsOtpGatewayEnabled)
        assertEquals(5, roundTripped.masterSyncRevision)

        // 2. App Update Gate (semver & versionCode)
        assertTrue(roundTripped.isUpdateAvailableForApp("6.3.7", 77))
        assertFalse(roundTripped.isUpdateAvailableForApp("6.4.0", 78))
        assertFalse(roundTripped.isUpdateAvailableForApp("6.5.0", 80))

        // 3. Operating Hours & Night Surcharge windows
        assertTrue(roundTripped.isWithinOperatingHours(10))
        assertFalse(roundTripped.isWithinOperatingHours(2))
        assertTrue(roundTripped.isCurrentHourInNightWindow(23))
        assertTrue(roundTripped.isCurrentHourInNightWindow(3))
        assertFalse(roundTripped.isCurrentHourInNightWindow(14))

        // 4. Fake Customer / COD Fraud Blocklist
        assertTrue(roundTripped.isCustomerPhoneBlocked("9876543210"))
        assertTrue(roundTripped.isCustomerPhoneBlocked("+91 99887 76655"))
        assertFalse(roundTripped.isCustomerPhoneBlocked("9123456789"))

        // 5. Live Rate Card & Vehicle Availability propagation
        com.example.data.cloud.QuickDeliveryOperationsManager.applyPricingOverridesFromFlags(roundTripped, sharedRepo)
        assertEquals(40.0, sharedRepo.pricingConfig.value.bikeBaseFare, 0.01)
        assertEquals(12.0, sharedRepo.pricingConfig.value.bikeRatePerKm, 0.01)
        assertFalse(com.example.data.delivery.AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.value.scooterEnabled)
        assertTrue(com.example.data.delivery.AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.value.bikeEnabled)

        // Restore defaults cleanly for subsequent tests
        com.example.data.delivery.AutomatedFairRiderAssignmentEngine.resetPricingAndWeightPolicyToDefaults(sharedRepo)
    }

    @Test
    fun testGoogleSignInActivityUnwrappingAndCleanAuthLabels() {
        // 1. Nested ContextWrapper (like LocalizedActivityContextWrapper) must cleanly unwrap to the real Activity
        val controller = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        val realActivity = controller.get()
        val localizedWrapper = object : android.content.ContextWrapper(realActivity) {}
        val doubleWrapped = android.content.ContextWrapper(localizedWrapper)

        val unwrapped = doubleWrapped.findActivity()
        assertEquals(realActivity, unwrapped)

        // Application context (not an Activity) must safely return null instead of throwing ClassCastException
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(null, appContext.findActivity())

        // 2. Customer & Rider Sign-In labels must never contain the confusing word "Address"
        val englishStrings = com.example.ui.i18n.DynamicUiStringsCatalog.forLanguage(com.example.ui.i18n.AppLanguage.ENGLISH)
        assertEquals("Mobile Number or Email *", englishStrings.mobileOrEmailLabel)
        assertFalse(englishStrings.mobileOrEmailLabel.contains("Address", ignoreCase = true))
        assertFalse(englishStrings.emailAddressLabel.contains("Address", ignoreCase = true))

        val hindiStrings = com.example.ui.i18n.DynamicUiStringsCatalog.forLanguage(com.example.ui.i18n.AppLanguage.HINDI)
        assertEquals("मोबाइल नंबर या ईमेल *", hindiStrings.mobileOrEmailLabel)
        assertFalse(hindiStrings.mobileOrEmailLabel.contains("पता"))
        assertFalse(hindiStrings.emailAddressLabel.contains("पता"))
    }
}



