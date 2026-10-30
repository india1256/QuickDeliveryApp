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

        // Simulate 15% critical battery state during active delivery
        val criticalState = RiderBatteryPowerMonitor.simulateBatteryState(
            levelPercent = 15,
            isCharging = false,
            isPowerSaveMode = true
        )
        assertTrue(criticalState.isCriticalPower)
        assertEquals(15, criticalState.levelPercent)

        val healthyState = RiderBatteryPowerMonitor.simulateBatteryState(
            levelPercent = 80,
            isCharging = true,
            isPowerSaveMode = false
        )
        assertFalse(healthyState.isCriticalPower)
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

        // 2. Role-bound HMAC-SHA256 Session Token verification & tamper protection
        val customerToken = com.example.security.QuickDeliverySecurityGuard.issueRoleSessionToken(
            role = "CUSTOMER",
            name = "Aarav Sharma",
            phone = "9876543210",
            email = "aarav@example.com"
        )
        assertTrue(
            com.example.security.QuickDeliverySecurityGuard.verifyRoleSessionToken(
                token = customerToken,
                expectedRole = "CUSTOMER",
                expectedPhone = "9876543210",
                expectedEmail = "aarav@example.com"
            )
        )
        // Must reject role privilege escalation (using a CUSTOMER token for ADMIN or RIDER)
        assertFalse(
            com.example.security.QuickDeliverySecurityGuard.verifyRoleSessionToken(
                token = customerToken,
                expectedRole = "ADMIN",
                expectedPhone = "9876543210",
                expectedEmail = "aarav@example.com"
            )
        )
        // Must reject tampered signature
        assertFalse(
            com.example.security.QuickDeliverySecurityGuard.verifyRoleSessionToken(
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
            currentActivePendingOrders = 0
        )
        assertTrue(firstOrder.isSuccess)

        // Immediate identical duplicate route within 8s cooldown must be blocked
        val duplicateBurst = com.example.security.QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
            actorKey = "SPAM-TEST-USER-1",
            pickupAddress = "MG Road Metro",
            dropAddress = "Koramangala 4th Block",
            vehicleType = "BIKE",
            currentActivePendingOrders = 1
        )
        assertTrue(duplicateBurst.isFailure)

        // Flooding > 5 orders within 60 seconds must be rate-limited
        for (i in 2..5) {
            val allowed = com.example.security.QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
                actorKey = "SPAM-TEST-USER-1",
                pickupAddress = "Pickup Point $i",
                dropAddress = "Drop Point $i",
                vehicleType = "BIKE",
                currentActivePendingOrders = 0
            )
            assertTrue(allowed.isSuccess)
        }
        val sixthRapidOrder = com.example.security.QuickDeliverySecurityGuard.checkAndRecordOrderPlacement(
            actorKey = "SPAM-TEST-USER-1",
            pickupAddress = "Pickup Point 6",
            dropAddress = "Drop Point 6",
            vehicleType = "BIKE",
            currentActivePendingOrders = 0
        )
        assertTrue(sixthRapidOrder.isFailure)

        com.example.security.QuickDeliverySecurityGuard.resetRateLimitsForTesting()
    }
}

