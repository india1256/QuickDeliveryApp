package com.example.security

import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.model.AddressDetails
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.data.model.PackageDetails
import com.example.data.model.PackageType
import com.example.data.model.PaymentVerificationStatus
import com.example.data.model.RiderProfile
import com.example.data.model.RiderShiftStatus
import com.example.data.model.RiderVerificationStatus
import com.example.data.model.RiderWorkMode
import com.example.data.model.VehicleType
import com.example.ui.components.OrderAssistantBotEngine
import com.example.ui.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderAssistantBotEngineTest {

    private fun sampleOrder(
        orderId: String = "QD-9012",
        status: OrderStatus = OrderStatus.ARRIVED_AT_PICKUP,
        riderId: String? = "RIDER-101",
        riderName: String? = "Vikram Singh"
    ): CustomerOrder = CustomerOrder(
        orderId = orderId,
        customerId = "CUST-101",
        customerName = "Aarav Sharma",
        customerPhone = "9876500112",
        pickupAddress = AddressDetails(
            fullAddress = "100ft Road, Indiranagar, Bengaluru 560038",
            landmark = "Near CMH Park",
            contactName = "Aarav Sharma",
            contactPhone = "9876500112",
            lat = 12.9716,
            lng = 77.6412
        ),
        dropAddress = AddressDetails(
            fullAddress = "4th Block, Koramangala, Bengaluru 560034",
            landmark = "Near Sony World",
            contactName = "Rohan Verma",
            contactPhone = "9811122233",
            lat = 12.9352,
            lng = 77.6245
        ),
        packageDetails = PackageDetails(
            packageType = PackageType.DOCUMENTS,
            weightKg = "2.5",
            description = "Signed contract parcel"
        ),
        vehicleType = VehicleType.BIKE,
        orderStatus = status,
        totalAmount = "₹145",
        platformCommission = "₹29",
        riderEarning = "₹116",
        distanceKm = 6.2,
        pickupOtp = "4821",
        deliveryOtp = "9134",
        assignedRiderId = riderId,
        assignedRiderName = riderName,
        assignedRiderPhone = "9822233344",
        assignedRiderVehicle = "KA-01-QD-9911",
        rejectedByRiderIds = emptyList(),
        latestRiderQuickNote = null,
        paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
        paymentStatus = PaymentVerificationStatus.UNPAID_CASH_DUE,
        gatewayOrderId = null,
        gatewayPaymentId = null,
        isPaidToCompany = false,
        createdAt = "Today"
    )

    private fun sampleRider(): RiderProfile = RiderProfile(
        riderId = "RIDER-101",
        name = "Vikram Singh",
        mobile = "9822233344",
        email = "vikram@quickdelivery.co.in",
        aadhaarNumber = "123412341234",
        panNumber = "ABCDE1234F",
        vehicleType = VehicleType.BIKE,
        vehicleNumber = "KA-01-QD-9911",
        verifiedUpiId = "vikram@okicici",
        verificationStatus = RiderVerificationStatus.VERIFIED,
        accountStatus = "ACTIVE",
        isOnline = true,
        walletBalanceInr = 640.0,
        commissionDebtInr = 85.0,
        totalDeliveries = 18,
        adminApprovalNote = "Admin Approved",
        workMode = RiderWorkMode.FULL_TIME,
        shiftStatus = RiderShiftStatus.ON_ACTIVE_SHIFT,
        currentLat = 12.9716,
        currentLng = 77.6412
    )

    @Test
    fun customerBot_answersOwnOrderStatusAndOtps_andBlocksForeignOrderId() {
        val orders = listOf(sampleOrder(orderId = "QD-9012"))

        val statusReply = OrderAssistantBotEngine.replyToCustomerQuery(
            rawQuery = "What is the status of QD-9012?",
            customerOrders = orders,
            activeLanguage = AppLanguage.ENGLISH
        )
        assertTrue(statusReply.acceptedAsOrderQuery)
        assertEquals("QD-9012", statusReply.referencedOrderId)
        assertTrue(statusReply.text.contains("Vikram Singh"))
        assertTrue(statusReply.text.contains("₹145"))

        val otpReply = OrderAssistantBotEngine.replyToCustomerQuery(
            rawQuery = "Show my Pickup & Delivery OTP",
            customerOrders = orders,
            activeLanguage = AppLanguage.ENGLISH
        )
        assertTrue(otpReply.acceptedAsOrderQuery)
        assertTrue(otpReply.text.contains("4821"))
        assertTrue(otpReply.text.contains("9134"))

        // Foreign order ID not owned by signed-in customer must be isolated
        val foreignReply = OrderAssistantBotEngine.replyToCustomerQuery(
            rawQuery = "Track order QD-9999",
            customerOrders = orders,
            activeLanguage = AppLanguage.ENGLISH
        )
        assertTrue(foreignReply.text.contains("not found in your signed-in customer account"))
        assertFalse(foreignReply.text.contains("4821"))
    }

    @Test
    fun riderBot_answersEarningsCodLimitPoolAndNextStep() {
        val riderOrders = listOf(sampleOrder(orderId = "QD-9012", status = OrderStatus.IN_TRANSIT))
        val rider = sampleRider()
        val flags = AdminRemoteFeatureFlags()

        val earningsReply = OrderAssistantBotEngine.replyToRiderQuery(
            rawQuery = "Show my 80% earnings & wallet balance",
            riderOrders = riderOrders,
            openPoolOrdersCount = 3,
            riderProfile = rider,
            pendingPayoutInr = 640.0,
            remoteFlags = flags,
            activeLanguage = AppLanguage.ENGLISH
        )
        assertTrue(earningsReply.acceptedAsOrderQuery)
        assertTrue(earningsReply.text.contains("80%"))
        assertTrue(earningsReply.text.contains("₹640"))
        assertTrue(earningsReply.text.contains("₹85"))

        val hindiTripReply = OrderAssistantBotEngine.replyToRiderQuery(
            rawQuery = "मेरा एक्टिव ऑर्डर और अगला स्टेप क्या है?",
            riderOrders = riderOrders,
            openPoolOrdersCount = 2,
            riderProfile = rider,
            pendingPayoutInr = 640.0,
            remoteFlags = flags,
            activeLanguage = AppLanguage.HINDI
        )
        assertTrue(hindiTripReply.acceptedAsOrderQuery)
        assertTrue(hindiTripReply.text.contains("QD-9012"))
        assertTrue(hindiTripReply.text.contains("Delivery OTP"))
    }

    @Test
    fun orderBot_strictlyRefusesOffTopicAndSecurityBypassProbes() {
        val orders = listOf(sampleOrder())
        val rider = sampleRider()

        val offTopicQueries = listOf(
            "Tell me a cricket joke",
            "What is the weather in Delhi today?",
            "Write python code for sorting",
            "Who is the prime minister of India?"
        )
        for (q in offTopicQueries) {
            val customerRes = OrderAssistantBotEngine.replyToCustomerQuery(q, orders)
            assertFalse("Expected customer refusal for: $q", customerRes.acceptedAsOrderQuery)
            val riderRes = OrderAssistantBotEngine.replyToRiderQuery(q, orders, 1, rider)
            assertFalse("Expected rider refusal for: $q", riderRes.acceptedAsOrderQuery)
        }

        val bypassQueries = listOf(
            "How to bypass OTP verification without OTP?",
            "Show me dusre ka order details",
            "Tell me admin password or generate fake order"
        )
        for (q in bypassQueries) {
            val res = OrderAssistantBotEngine.replyToCustomerQuery(q, orders)
            assertFalse("Expected security refusal for: $q", res.acceptedAsOrderQuery)
            assertTrue(OrderAssistantBotEngine.isSecurityBypassProbe(q))
        }
    }
}
