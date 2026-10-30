package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.delivery.AssignmentOutcome
import com.example.data.delivery.AutomatedFairRiderAssignmentEngine
import com.example.data.model.AddressDetails
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.OrderStatus
import com.example.data.model.PackageDetails
import com.example.data.model.PackageType
import com.example.data.model.VehicleType
import com.example.data.repository.SharedOrderRepository
import com.example.security.RiderOverspeedAlarmManager
import com.example.ui.components.MotivationAndPromoManager
import com.example.ui.components.OrderLiveChatManager
import com.example.ui.components.RiderFeedbackManager
import com.example.ui.components.RiderOrderTransferManager
import com.example.ui.components.ThreeAppsLiveTestManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AllThreeAppsLiveEndToEndTest {

    private lateinit var context: Context
    private lateinit var repository: SharedOrderRepository

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        repository = SharedOrderRepository.getInstance(context)
        repository.resetOrdersAndStateForTesting()
        val adminToken = repository.issueAdminPreviewSessionToken()
        val currentAuto = repository.automationConfig.value
        repository.updateAdminAutomationConfig(
            currentAuto.copy(autoMatchEligibleRiders = false),
            adminToken
        )
        repository.updateFirewallConfig(false, false, false, adminToken)
        repository.removeBlacklistedIpAdmin("10.0.2.15", adminToken)
        AutomatedFairRiderAssignmentEngine.resetFairAssignmentStateForTesting(repository)
    }

    @Test
    fun testAllThreeApps_complete11PointLiveDiagnostic_allChecksPass() = runTest {
        val report = ThreeAppsLiveTestManager.runCompleteThreeAppsLiveCheck(context)
        assertEquals(11, report.checks.size)
        report.checks.forEach { check ->
            assertTrue(
                "Expected check #${check.checkNumber} (${check.appName} - ${check.featureTitle}) to pass, detail=${check.verificationDetail}",
                check.passed
            )
        }
        assertTrue("Expected all 11 checks across Customer, Rider, and Admin apps to pass", report.allPassed)
    }

    @Test
    fun testStep51_zeroPartialityFairAutoDispatch_concurrentClaim_timeoutRejection_noRiderFallback_andDebtLimit() = runTest {
        // 1. Verify full balanced 5-rider fleet is active and online
        AutomatedFairRiderAssignmentEngine.ensureBalancedActiveRiderFleet(repository)
        assertEquals(5, repository.riders.value.size)

        // 2. ZERO PARTIALITY / FAIR ROTATION TEST:
        // Create 3 consecutive Bike orders and verify 3 DIFFERENT Bike riders receive them fairly (no single-rider bias!)
        val bikeOrder1 = repository.createCustomerOrder(
            customerId = "CUST-1011",
            customerName = "Aarav Sharma",
            customerPhone = "9876500001",
            pickupAddress = AddressDetails("Indiranagar 12th Main", "HAL 2nd Stage", "Aarav", "9876500001", lat = 12.9815, lng = 77.6382),
            dropAddress = AddressDetails("Koramangala 5th Block", "Jyoti Nivas", "Rohan", "9845012345", lat = 12.9395, lng = 77.6290),
            packageDetails = PackageDetails(PackageType.DOCUMENTS, "1.5", "Parcel 1"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "FAIR-ORD-1"
        ).getOrThrow()

        val res1 = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
            repository = repository,
            orderId = bikeOrder1.orderId,
            triggerId = "TRIG-FAIR-1"
        )
        assertEquals(AssignmentOutcome.ASSIGNED_TO_RIDER, res1.outcome)
        val assignedRider1 = res1.assignedRider?.riderId
        assertNotNull(assignedRider1)

        // Duplicate trigger test: firing TRIG-FAIR-1 again must be ignored idempotently
        val dupRes = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
            repository = repository,
            orderId = bikeOrder1.orderId,
            triggerId = "TRIG-FAIR-1"
        )
        assertTrue(dupRes.duplicateTriggerIgnored)
        assertEquals(AssignmentOutcome.DUPLICATE_TRIGGER_IGNORED, dupRes.outcome)

        // Commission deduplication lock test
        assertTrue(AutomatedFairRiderAssignmentEngine.tryAcquireSingleCommissionLockForOrder(bikeOrder1.orderId))
        assertFalse(AutomatedFairRiderAssignmentEngine.tryAcquireSingleCommissionLockForOrder(bikeOrder1.orderId))

        // Create 2nd Bike order -> must go to a DIFFERENT rider (Rider 1 is busy + has higher fair count)
        val bikeOrder2 = repository.createCustomerOrder(
            customerId = "CUST-1002",
            customerName = "Meera Nair",
            customerPhone = "9876500002",
            pickupAddress = AddressDetails("Ulsoor Lake", "Trinity", "Meera", "9876500002", lat = 12.9762, lng = 77.6205),
            dropAddress = AddressDetails("Domlur Flyover", "Bridge", "Kiran", "9845022222", lat = 12.9610, lng = 77.6387),
            packageDetails = PackageDetails(PackageType.GROCERIES, "3.0", "Parcel 2"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "FAIR-ORD-2"
        ).getOrThrow()

        val res2 = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
            repository = repository,
            orderId = bikeOrder2.orderId,
            triggerId = "TRIG-FAIR-2"
        )
        assertEquals(AssignmentOutcome.ASSIGNED_TO_RIDER, res2.outcome)
        val assignedRider2 = res2.assignedRider?.riderId
        assertNotNull(assignedRider2)
        assertNotEquals("Second order must not go to the same rider", assignedRider1, assignedRider2)

        // Create 3rd Bike order -> must go to the 3rd distinct Bike rider
        val bikeOrder3 = repository.createCustomerOrder(
            customerId = "CUST-1003",
            customerName = "Kabir Kapoor",
            customerPhone = "9876500003",
            pickupAddress = AddressDetails("MG Road Boulevard", "Metro", "Kabir", "9876500003", lat = 12.9756, lng = 77.6066),
            dropAddress = AddressDetails("HSR Layout Sector 2", "Park", "Neha", "9845033333", lat = 12.9121, lng = 77.6446),
            packageDetails = PackageDetails(PackageType.ELECTRONICS, "2.5", "Parcel 3"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "FAIR-ORD-3"
        ).getOrThrow()

        val res3 = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
            repository = repository,
            orderId = bikeOrder3.orderId,
            triggerId = "TRIG-FAIR-3"
        )
        assertEquals(AssignmentOutcome.ASSIGNED_TO_RIDER, res3.outcome)
        val assignedRider3 = res3.assignedRider?.riderId
        assertNotNull(assignedRider3)
        assertEquals(3, setOf(assignedRider1, assignedRider2, assignedRider3).size)

        // Complete all 3 Bike deliveries so riders become free again, and verify fair rotation still remembers order counts
        listOf(bikeOrder1.orderId to assignedRider1, bikeOrder2.orderId to assignedRider2, bikeOrder3.orderId to assignedRider3).forEach { (oid, rid) ->
            val ord = repository.orders.value.first { it.orderId == oid }
            assertTrue(repository.verifyPickupOtpByRider(oid, ord.pickupOtp, rid).isSuccess)
            assertTrue(repository.verifyDeliveryOtpByRider(oid, ord.deliveryOtp, rid).isSuccess)
        }

        // 3. SEQUENTIAL OFFER TIMEOUT & REJECTION REASSIGNMENT TEST:
        val sequentialOrder = repository.createCustomerOrder(
            customerId = "CUST-1004",
            customerName = "Divya Menon",
            customerPhone = "9876500004",
            pickupAddress = AddressDetails("Jayanagar 4th Block", "Complex", "Divya", "9876500004", lat = 12.9299, lng = 77.5824),
            dropAddress = AddressDetails("JP Nagar 6th Phase", "Circle", "Arjun", "9845044444", lat = 12.9063, lng = 77.5857),
            packageDetails = PackageDetails(PackageType.DOCUMENTS, "1.0", "Sequential Offer Parcel"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "SEQ-ORD-4"
        ).getOrThrow()

        val offerRes1 = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(
            repository = repository,
            orderId = sequentialOrder.orderId,
            offerOnlyWaitForRiderConsent = true
        )
        assertEquals(AssignmentOutcome.OFFERED_TO_RIDER_PENDING_RESPONSE, offerRes1.outcome)
        val firstOfferedRiderId = offerRes1.offeredState?.offeredRiderId
        assertNotNull(firstOfferedRiderId)

        // Simulate timeout on first offered rider -> must offer to the NEXT eligible rider
        val afterTimeoutRes = AutomatedFairRiderAssignmentEngine.handleOfferTimeoutAndAdvanceNext(
            repository = repository,
            orderId = sequentialOrder.orderId,
            timedOutRiderId = firstOfferedRiderId,
            offerOnlyWaitForRiderConsent = true
        )
        assertEquals(AssignmentOutcome.OFFERED_TO_RIDER_PENDING_RESPONSE, afterTimeoutRes.outcome)
        val secondOfferedRiderId = afterTimeoutRes.offeredState?.offeredRiderId
        assertNotNull(secondOfferedRiderId)
        assertNotEquals(firstOfferedRiderId, secondOfferedRiderId)

        // Simulate rejection by second rider -> must auto-assign to the 3rd eligible rider
        val afterRejectRes = AutomatedFairRiderAssignmentEngine.rejectOrderAndAutoReassignNext(
            repository = repository,
            orderId = sequentialOrder.orderId,
            rejectingRiderId = secondOfferedRiderId!!,
            reason = "Rider on break",
            offerOnlyWaitForRiderConsent = false
        )
        assertEquals(AssignmentOutcome.ASSIGNED_TO_RIDER, afterRejectRes.outcome)
        val thirdAssignedRiderId = afterRejectRes.assignedRider?.riderId
        assertNotNull(thirdAssignedRiderId)
        assertNotEquals(firstOfferedRiderId, thirdAssignedRiderId)
        assertNotEquals(secondOfferedRiderId, thirdAssignedRiderId)

        // Complete sequentialOrder so all riders are free again
        val seqOrdNow = repository.orders.value.first { it.orderId == sequentialOrder.orderId }
        assertTrue(repository.verifyPickupOtpByRider(seqOrdNow.orderId, seqOrdNow.pickupOtp, thirdAssignedRiderId).isSuccess)
        assertTrue(repository.verifyDeliveryOtpByRider(seqOrdNow.orderId, seqOrdNow.deliveryOtp, thirdAssignedRiderId).isSuccess)

        // 4. CONCURRENT RIDER ACCEPTANCE ATOMIC LOCK TEST:
        repository.setRiderCommissionDebt("RIDER-101", 0.0)
        val raceOrder = repository.createCustomerOrder(
            customerId = "CUST-1005",
            customerName = "Rakesh Jain",
            customerPhone = "9876500005",
            pickupAddress = AddressDetails("BTM Layout 2nd Stage", "Water Tank", "Rakesh", "9876500005", lat = 12.9166, lng = 77.6101),
            dropAddress = AddressDetails("Bellandur Gate", "Junction", "Sonia", "9845055555", lat = 12.9304, lng = 77.6784),
            packageDetails = PackageDetails(PackageType.DOCUMENTS, "1.0", "Race Parcel"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "RACE-ORD-5"
        ).getOrThrow()

        val claimWinner = AutomatedFairRiderAssignmentEngine.claimOrderAtomicallyByRider(
            repository = repository,
            orderId = raceOrder.orderId,
            riderId = "RIDER-101"
        )
        val claimLoser = AutomatedFairRiderAssignmentEngine.claimOrderAtomicallyByRider(
            repository = repository,
            orderId = raceOrder.orderId,
            riderId = "RIDER-102"
        )
        assertTrue("First atomic claim must succeed", claimWinner.isSuccess)
        assertTrue("Concurrent second claim on already-assigned order must fail", claimLoser.isFailure)
        assertEquals("RIDER-101", repository.orders.value.first { it.orderId == raceOrder.orderId }.assignedRiderId)

        // Complete raceOrder
        val raceOrdNow = repository.orders.value.first { it.orderId == raceOrder.orderId }
        assertTrue(repository.verifyPickupOtpByRider(raceOrdNow.orderId, raceOrdNow.pickupOtp, "RIDER-101").isSuccess)
        assertTrue(repository.verifyDeliveryOtpByRider(raceOrdNow.orderId, raceOrdNow.deliveryOtp, "RIDER-101").isSuccess)

        // 5. ₹150 COMMISSION DEBT LIMIT ENFORCEMENT IN AUTO-DISCOVERY:
        repository.setRiderCommissionDebt("RIDER-101", 155.0)
        val debtCheckOrder = repository.createCustomerOrder(
            customerId = "CUST-1006",
            customerName = "Pooja Hegde",
            customerPhone = "9876500006",
            pickupAddress = AddressDetails("Malleswaram 8th Cross", "Temple", "Pooja", "9876500006", lat = 13.0031, lng = 77.5643),
            dropAddress = AddressDetails("Rajajinagar Metro", "Gate 1", "Nitin", "9845066666", lat = 12.9982, lng = 77.5530),
            packageDetails = PackageDetails(PackageType.DOCUMENTS, "1.0", "Debt Check Parcel"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "DEBT-ORD-6"
        ).getOrThrow()
        val debtRanked = AutomatedFairRiderAssignmentEngine.rankEligibleRidersForOrder(repository, debtCheckOrder)
        val r101Eval = debtRanked.first { it.rider.riderId == "RIDER-101" }
        assertFalse("RIDER-101 with ₹155 debt (> ₹150 cap) must be ineligible", r101Eval.isEligible)
        val debtAssignRes = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(repository, debtCheckOrder.orderId)
        assertEquals(AssignmentOutcome.ASSIGNED_TO_RIDER, debtAssignRes.outcome)
        assertNotEquals("RIDER-101", debtAssignRes.assignedRider?.riderId)

        // Reset RIDER-101 debt back to ₹50
        repository.setRiderCommissionDebt("RIDER-101", 50.0)

        // 6. NO AVAILABLE RIDER -> KEEP IN SEARCHING_RIDER & AUTO-ASSIGN WHEN RIDER COMES ONLINE:
        val allRidersOffline = repository.riders.value.map { it.copy(isOnline = false) }
        AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, allRidersOffline)

        val searchingOrder = repository.createCustomerOrder(
            customerId = "CUST-1007",
            customerName = "Sanjay Gupta",
            customerPhone = "9876500007",
            pickupAddress = AddressDetails("Whitefield ITPL", "Main Gate", "Sanjay", "9876500007", lat = 12.9850, lng = 77.7360),
            dropAddress = AddressDetails("Marathahalli Bridge", "Plaza", "Varun", "9845077777", lat = 12.9569, lng = 77.7011),
            packageDetails = PackageDetails(PackageType.DOCUMENTS, "1.0", "Waiting Parcel"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "SEARCH-ORD-7"
        ).getOrThrow()

        val noRiderRes = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(repository, searchingOrder.orderId)
        assertEquals(AssignmentOutcome.KEPT_IN_SEARCHING_NO_ELIGIBLE_RIDER, noRiderRes.outcome)
        val stillSearching = repository.orders.value.first { it.orderId == searchingOrder.orderId }
        assertEquals(OrderStatus.SEARCHING_RIDER, stillSearching.orderStatus)
        assertNull("Order must NOT be falsely marked assigned when no rider is available", stillSearching.assignedRiderId)

        // Now bring RIDER-102 online -> retryAllSearchingOrdersFairly must immediately assign the waiting order!
        val r102BackOnline = repository.riders.value.map {
            if (it.riderId == "RIDER-102") it.copy(isOnline = true) else it
        }
        AutomatedFairRiderAssignmentEngine.updateRepositoryRidersList(repository, r102BackOnline)
        val retryResults = AutomatedFairRiderAssignmentEngine.retryAllSearchingOrdersFairly(repository)
        assertEquals(1, retryResults.size)
        assertEquals("RIDER-102", retryResults.first().assignedRider?.riderId)
        assertEquals(OrderStatus.RIDER_ASSIGNED, repository.orders.value.first { it.orderId == searchingOrder.orderId }.orderStatus)
    }

    @Test
    fun testCustomerRiderAndAdmin_endToEndWorkflow_codNoOtp_prepaidOtp_chat_transfer3xWeek_overspeed_andAdminRatings() = runTest {
        // 1. Customer & Rider Storage Login Persistence
        MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
            context = context,
            rolePrefix = "customer",
            name = "Aarav Sharma",
            phone = "9876543210",
            email = "aarav@quickdelivery.in",
            storageAllowed = true
        )
        MotivationAndPromoManager.grantPhoneStorageAndSaveLogin(
            context = context,
            rolePrefix = "rider",
            name = "Vikram Singh",
            phone = "9845067890",
            email = "vikram@quickdelivery.in",
            storageAllowed = true
        )
        assertTrue(MotivationAndPromoManager.customerStoragePermissionGranted.value)
        assertTrue(MotivationAndPromoManager.riderStoragePermissionGranted.value)

        // 2. Customer books a Cash on Delivery (COD) order -> Automatic Fair Discovery assigns an eligible rider
        val codOrder = repository.createCustomerOrder(
            customerId = "CUST-1001",
            customerName = "Aarav Sharma",
            customerPhone = "9876543210",
            pickupAddress = AddressDetails("Indiranagar 100ft Rd", "Metro", "Aarav", "9876543210", lat = 12.9784, lng = 77.6408),
            dropAddress = AddressDetails("Koramangala 4th Block", "Signal", "Rohan", "9845012345", lat = 12.9352, lng = 77.6245),
            packageDetails = PackageDetails(PackageType.GROCERIES, "2.0", "COD Parcel"),
            vehicleType = VehicleType.BIKE,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY
        ).getOrThrow()

        val autoAssign = AutomatedFairRiderAssignmentEngine.autoDiscoverAndAssignOrder(repository, codOrder.orderId)
        assertEquals(AssignmentOutcome.ASSIGNED_TO_RIDER, autoAssign.outcome)
        val assignedCod = repository.orders.value.first { it.orderId == codOrder.orderId }
        assertEquals(OrderStatus.RIDER_ASSIGNED, assignedCod.orderStatus)
        assertNotNull(assignedCod.assignedRiderName)
        assertNotNull(assignedCod.assignedRiderPhone)

        // 3. Real-Time Chat between Customer and Assigned Rider during delivery
        OrderLiveChatManager.sendChatMessage(
            context = context,
            order = assignedCod,
            senderRole = "CUSTOMER",
            senderName = "Aarav Sharma",
            messageText = "Please come to gate 2."
        )
        OrderLiveChatManager.sendChatMessage(
            context = context,
            order = assignedCod,
            senderRole = "RIDER",
            senderName = assignedCod.assignedRiderName ?: "Vikram Rathore",
            messageText = "Reaching gate 2 in 2 minutes!"
        )
        val orderMsgs = OrderLiveChatManager.messagesByOrder.value[codOrder.orderId].orEmpty()
        assertTrue(orderMsgs.any { it.senderRole == "CUSTOMER" && it.messageText.contains("gate 2") })
        assertTrue(orderMsgs.any { it.senderRole == "RIDER" && it.messageText.contains("Reaching gate 2") })

        // 4. Complete COD Pickup & Delivery using the assigned rider's identity
        assertTrue(repository.verifyPickupOtpByRider(codOrder.orderId, assignedCod.pickupOtp, assignedCod.assignedRiderId).isSuccess)
        assertTrue(repository.verifyDeliveryOtpByRider(codOrder.orderId, assignedCod.deliveryOtp, assignedCod.assignedRiderId).isSuccess)
        val deliveredCod = repository.orders.value.first { it.orderId == codOrder.orderId }
        assertEquals(OrderStatus.DELIVERED, deliveredCod.orderStatus)

        // 5. Mutual Ratings (Customer -> Rider & Rider -> Customer) & Suggestion Box for Admin Web
        RiderFeedbackManager.submitFeedback(
            context = context,
            orderId = codOrder.orderId,
            riderId = assignedCod.assignedRiderId ?: "RIDER-101",
            riderName = assignedCod.assignedRiderName ?: "Vikram Rathore",
            customerName = "Aarav Sharma",
            starRating = 5,
            complimentTags = listOf("Fast Delivery"),
            comment = "Excellent delivery",
            tipAmountInr = 30
        )
        RiderFeedbackManager.submitCustomerRatingByRider(
            context = context,
            orderId = codOrder.orderId,
            riderId = assignedCod.assignedRiderId ?: "RIDER-101",
            riderName = assignedCod.assignedRiderName ?: "Vikram Rathore",
            customerName = "Aarav Sharma",
            starRating = 5,
            behaviorTags = listOf("Polite Customer"),
            comment = "Polite customer"
        )
        assertEquals(5, RiderFeedbackManager.feedbackByOrder.value[codOrder.orderId]?.starRating)
        assertEquals(5, RiderFeedbackManager.customerRatingsByOrder.value[codOrder.orderId]?.starRating)

        // 6. Rider Over-Speed Phone Alarm Guard (Rings > limit, stops <= limit)
        RiderOverspeedAlarmManager.updateSpeedLimit(context, 45)
        RiderOverspeedAlarmManager.setSimulatedSpeedKmh(context, 58)
        assertTrue(RiderOverspeedAlarmManager.state.value.isAlarmRinging)
        RiderOverspeedAlarmManager.setSimulatedSpeedKmh(context, 38)
        assertFalse(RiderOverspeedAlarmManager.state.value.isAlarmRinging)

        // 7. Rider Weekly Order Transfer Quota (Max 3 transfers per week, 4th is locked until next week)
        val candidate = RiderOrderTransferManager.backupRiders.first()
        while (RiderOrderTransferManager.weeklyTransfersUsed.value < 3) {
            val idx = RiderOrderTransferManager.weeklyTransfersUsed.value + 1
            val res = RiderOrderTransferManager.transferOrderToAnotherRider(
                context = context,
                orderId = "QD-TRANS-$idx",
                fromRiderId = "RIDER-101",
                targetRider = candidate,
                reason = "Vehicle Breakdown (Gadi Kharab)"
            )
            assertTrue(res.isSuccess)
        }
        assertEquals(3, RiderOrderTransferManager.weeklyTransfersUsed.value)
        // 4th attempt in the same week must fail and remain locked until next week
        val fourthAttempt = RiderOrderTransferManager.transferOrderToAnotherRider(
            context = context,
            orderId = "QD-TRANS-4",
            fromRiderId = "RIDER-101",
            targetRider = candidate,
            reason = "Vehicle Breakdown (Gadi Kharab)"
        )
        assertTrue("Expected 4th transfer in the same week to be locked until next week", fourthAttempt.isFailure)

        // 8. Customer Order History Verification (Past Completed + Active Orders with Status, Date, and Final Price)
        val historyOrders = repository.orders.value
        val completedHistoryOrder = historyOrders.first { it.orderId == codOrder.orderId }
        assertEquals(OrderStatus.DELIVERED, completedHistoryOrder.orderStatus)
        assertTrue(completedHistoryOrder.createdAt.isNotBlank())
        assertTrue(completedHistoryOrder.totalAmount.startsWith("₹"))
        assertTrue(historyOrders.any { it.orderStatus != OrderStatus.DELIVERED })

        // 9. Backend 20 kg Parcel Weight Limit Enforcement (> 20 kg blocked, <= 20 kg allowed)
        val overWeightCheck = AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg("25.0")
        assertTrue("Expected parcel > 20 kg to be rejected", overWeightCheck.isFailure)
        assertTrue(overWeightCheck.exceptionOrNull()?.message?.contains("20 kg") == true)

        val blockedOverWeightOrder = AutomatedFairRiderAssignmentEngine.createCustomerOrderWithBackendValidation(
            repository = repository,
            customerId = "CUST-1001",
            customerName = "Aarav Sharma",
            customerPhone = "9876543210",
            pickupAddress = AddressDetails("Indiranagar 100ft Rd", "Metro", "Aarav", "9876543210", lat = 12.9784, lng = 77.6408),
            dropAddress = AddressDetails("Koramangala 4th Block", "Signal", "Rohan", "9845012345", lat = 12.9352, lng = 77.6245),
            packageDetails = PackageDetails(PackageType.ELECTRONICS, "22.5", "Heavy 22.5 kg Parcel"),
            vehicleType = VehicleType.AUTO,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY
        )
        assertTrue("Expected backend order creation to block parcel > 20 kg", blockedOverWeightOrder.isFailure)

        val valid20KgOrder = AutomatedFairRiderAssignmentEngine.createCustomerOrderWithBackendValidation(
            repository = repository,
            customerId = "CUST-1020",
            customerName = "Rohan Kulkarni",
            customerPhone = "9876543220",
            pickupAddress = AddressDetails("HSR Layout Sector 1", "BDA Complex", "Rohan", "9876543220", lat = 12.9121, lng = 77.6446),
            dropAddress = AddressDetails("BTM Layout 2nd Stage", "Udupi Garden", "Kiran", "9845099887", lat = 12.9166, lng = 77.6101),
            packageDetails = PackageDetails(PackageType.ELECTRONICS, "20", "Valid 20 kg Parcel"),
            vehicleType = VehicleType.AUTO,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY,
            idempotencyKey = "VALID-20KG-ORDER-TEST"
        )
        assertTrue(
            "Expected 20 kg parcel to be accepted, error=${valid20KgOrder.exceptionOrNull()?.message}",
            valid20KgOrder.isSuccess
        )

        // 10. Synchronized Customer App & Admin Web Fare Estimates (Bike -> Scooter -> Auto) & Live Policy Sync
        val bikeBreakdown = AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
            pickupLat = 12.9784,
            pickupLng = 77.6408,
            dropLat = 12.9352,
            dropLng = 77.6245,
            weightKgInput = "4.0",
            vehicleType = VehicleType.BIKE
        )
        val scooterBreakdown = AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
            pickupLat = 12.9784,
            pickupLng = 77.6408,
            dropLat = 12.9352,
            dropLng = 77.6245,
            weightKgInput = "4.0",
            vehicleType = VehicleType.SCOOTER
        )
        val autoBreakdown = AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
            pickupLat = 12.9784,
            pickupLng = 77.6408,
            dropLat = 12.9352,
            dropLng = 77.6245,
            weightKgInput = "4.0",
            vehicleType = VehicleType.AUTO
        )
        assertTrue("Expected positive route distance km", bikeBreakdown.distanceKm > 0.0)
        assertTrue(
            "Expected Bike < Scooter < Auto total fare for identical route and weight",
            bikeBreakdown.totalFareInr < scooterBreakdown.totalFareInr &&
                scooterBreakdown.totalFareInr < autoBreakdown.totalFareInr
        )
        assertTrue(bikeBreakdown.formattedTotalFare.startsWith("₹"))

        // Admin updates Bike/Scooter/Auto pricing and max weight limit -> verify immediate synchronization
        val syncedPricing = AutomatedFairRiderAssignmentEngine.updateLivePricingAndWeightPolicyFromAdmin(
            repository = repository,
            bikeBaseFare = 42.0,
            bikeRatePerKm = 15.0,
            scooterBaseFare = 52.0,
            scooterRatePerKm = 18.0,
            autoBaseFare = 75.0,
            autoRatePerKm = 25.0,
            maxAllowedParcelWeightKg = 15.0
        )
        assertEquals(42.0, repository.pricingConfig.value.bikeBaseFare, 0.01)
        assertEquals(52.0, repository.pricingConfig.value.scooterBaseFare, 0.01)
        assertEquals(75.0, repository.pricingConfig.value.autoBaseFare, 0.01)
        assertEquals(15.0, AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.value.maxAllowedParcelWeightKg, 0.01)

        val updatedBikeBreakdown = AutomatedFairRiderAssignmentEngine.calculateDetailedVehicleFare(
            pickupLat = 12.9784,
            pickupLng = 77.6408,
            dropLat = 12.9352,
            dropLng = 77.6245,
            weightKgInput = "4.0",
            vehicleType = VehicleType.BIKE,
            pricingConfigOverride = syncedPricing
        )
        assertTrue(
            "Expected updated Admin Bike pricing to increase Customer Bike fare estimate",
            updatedBikeBreakdown.totalFareInr > bikeBreakdown.totalFareInr
        )
        // Weight above Admin's 15 kg policy must be rejected even though <= 20 kg
        val policyWeightCheck = AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg("18.0", VehicleType.BIKE)
        assertTrue("Expected 18 kg parcel to be blocked when Admin sets 15 kg limit", policyWeightCheck.isFailure)

        // Reset back to defaults (20 kg max limit)
        AutomatedFairRiderAssignmentEngine.resetPricingAndWeightPolicyToDefaults(repository)
        assertEquals(20.0, AutomatedFairRiderAssignmentEngine.vehicleAndWeightPolicy.value.maxAllowedParcelWeightKg, 0.01)
    }

    @Test
    fun testAdminEnterpriseSuite_12DedicatedPages_xlsxExport_andFcmNotifications() = runTest {
        // 1. Verify all 12 dedicated Admin Web navigation pages exist
        val pages = com.example.ui.components.AdminDedicatedPage.entries
        assertEquals(12, pages.size)

        // 2. Verify genuine OpenXML (.xlsx) export for Orders, Riders, Customers, Earnings (20%/80%), and Payments
        val exportResult = com.example.ui.components.AdminExcelXlsxExporter.exportWorkbookToXlsx(
            context = context,
            filePrefix = "QuickDelivery_Test_Export",
            sheets = listOf(
                com.example.ui.components.XlsxSheetSpec(
                    sheetName = "Orders",
                    headers = listOf("Order ID", "Customer", "Total Fare", "20% Commission", "80% Rider"),
                    rows = repository.orders.value.map {
                        listOf(it.orderId, it.customerName, it.totalAmount, it.platformCommission, it.riderEarning)
                    }
                ),
                com.example.ui.components.XlsxSheetSpec(
                    sheetName = "Riders",
                    headers = listOf("Rider ID", "Name", "Wallet", "Debt"),
                    rows = repository.riders.value.map {
                        listOf(it.riderId, it.name, "${it.walletBalanceInr}", "${it.commissionDebtInr}")
                    }
                )
            ),
            launchShareSheet = false
        )
        assertTrue(exportResult.fileName.endsWith(".xlsx"))
        assertEquals(2, exportResult.sheetCount)
        assertTrue(exportResult.sizeBytes > 100L)
        val outFile = java.io.File(exportResult.absolutePath)
        assertTrue("Expected .xlsx file to exist on disk", outFile.exists())

        // 3. Verify FCM real-time order status & delivery completion notifications for Customer & Rider
        val sampleOrder = repository.orders.value.first()
        com.example.ui.components.FcmOrderNotificationManager.dispatchOrderStatusPair(
            context = context,
            order = sampleOrder.copy(orderStatus = OrderStatus.IN_TRANSIT),
            postToSystemTray = false
        )
        com.example.ui.components.FcmOrderNotificationManager.dispatchOrderStatusPair(
            context = context,
            order = sampleOrder.copy(orderStatus = OrderStatus.DELIVERED),
            postToSystemTray = false
        )
        val fcmHistory = com.example.ui.components.FcmOrderNotificationManager.fcmMessages.value
        assertTrue(
            fcmHistory.any {
                it.targetRole == com.example.ui.components.FcmTargetAppRole.CUSTOMER &&
                    it.orderStatus == OrderStatus.DELIVERED &&
                    it.isDeliveryCompletion
            }
        )
        assertTrue(
            fcmHistory.any {
                it.targetRole == com.example.ui.components.FcmTargetAppRole.RIDER &&
                    it.orderStatus == OrderStatus.DELIVERED &&
                    it.isDeliveryCompletion
            }
        )
    }
}

