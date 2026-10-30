package com.example.data.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcelDeliveryStateManagerTest {

    @Test
    fun sequentialTransitions_pendingToAssignedToInTransitToDelivered_updatesModelAndUiState() {
        val manager = ParcelDeliveryStateManager()
        val initial = ParcelDeliveryTrackingModel(
            orderId = "ord_1001",
            orderCode = "QD-1001",
            customerName = "Aarav Sharma",
            pickupAddress = "Indiranagar 100ft Road, Bengaluru",
            dropAddress = "Koramangala 4th Block, Bengaluru",
            currentStage = ParcelDeliveryStage.PENDING
        )

        manager.registerOrSyncParcel(initial, setAsActive = true)
        val state0 = manager.uiState.value as ParcelDeliveryUiState.Active
        assertEquals(ParcelDeliveryStage.PENDING, state0.tracking.currentStage)
        assertEquals(ParcelDeliveryStage.ASSIGNED, state0.nextAllowedStage)

        // 1. Pending -> Assigned
        val assignedRes = manager.transitionTo(
            orderId = "ord_1001",
            targetStage = ParcelDeliveryStage.ASSIGNED,
            actorRole = DeliveryActorRole.RIDER,
            actorId = "RIDER-101",
            riderName = "Vikram Singh"
        )
        assertTrue(assignedRes.isSuccess)
        assertEquals(ParcelDeliveryStage.ASSIGNED, assignedRes.getOrThrow().currentStage)
        assertEquals("RIDER-101", assignedRes.getOrThrow().assignedRiderId)

        // 2. Assigned -> In-Transit
        val inTransitRes = manager.transitionTo(
            orderId = "ord_1001",
            targetStage = ParcelDeliveryStage.IN_TRANSIT,
            actorRole = DeliveryActorRole.RIDER,
            actorId = "RIDER-101",
            proofNote = "Pickup photo verified",
            latitude = 12.9716,
            longitude = 77.5946
        )
        assertTrue(inTransitRes.isSuccess)
        assertEquals(ParcelDeliveryStage.IN_TRANSIT, inTransitRes.getOrThrow().currentStage)
        assertEquals("Pickup photo verified", inTransitRes.getOrThrow().pickupProofNote)
        assertEquals(12.9716, inTransitRes.getOrThrow().lastKnownLat ?: 0.0, 0.0001)

        // 3. In-Transit -> Delivered
        val deliveredRes = manager.transitionTo(
            orderId = "ord_1001",
            targetStage = ParcelDeliveryStage.DELIVERED,
            actorRole = DeliveryActorRole.RIDER,
            actorId = "RIDER-101",
            proofNote = "Dropoff photo verified"
        )
        assertTrue(deliveredRes.isSuccess)
        val finalModel = deliveredRes.getOrThrow()
        assertEquals(ParcelDeliveryStage.DELIVERED, finalModel.currentStage)
        assertTrue(finalModel.isTerminal)
        assertNull(finalModel.nextStage)
        assertEquals(3, finalModel.transitionHistory.size)
    }

    @Test
    fun invalidTransitions_skippingStagesOrCustomerRole_emitsTransitionError() {
        val manager = ParcelDeliveryStateManager()
        manager.registerOrSyncParcel(
            ParcelDeliveryTrackingModel(
                orderId = "ord_1002",
                orderCode = "QD-1002",
                customerName = "Priya Nair",
                pickupAddress = "MG Road, Bengaluru",
                dropAddress = "HSR Layout, Bengaluru",
                currentStage = ParcelDeliveryStage.PENDING
            )
        )

        // Skipping ASSIGNED and jumping straight to DELIVERED must fail
        val skipRes = manager.transitionTo(
            orderId = "ord_1002",
            targetStage = ParcelDeliveryStage.DELIVERED,
            actorRole = DeliveryActorRole.RIDER,
            actorId = "RIDER-101"
        )
        assertTrue(skipRes.isFailure)
        assertTrue(manager.uiState.value is ParcelDeliveryUiState.TransitionError)

        // Customer role attempting to advance rider stage must fail
        val customerAttempt = manager.transitionTo(
            orderId = "ord_1002",
            targetStage = ParcelDeliveryStage.ASSIGNED,
            actorRole = DeliveryActorRole.CUSTOMER,
            actorId = "CUST-1"
        )
        assertTrue(customerAttempt.isFailure)
        assertTrue(manager.uiState.value is ParcelDeliveryUiState.TransitionError)
    }

    @Test
    fun crossPlatformDeliveryEngine_androidAndIosCustomerAndRiderParity() {
        assertEquals("com.aistudio.quickdelivery.customer", TargetMobilePlatform.ANDROID.customerAppIdentifier)
        assertEquals("com.aistudio.quickdelivery.rider", TargetMobilePlatform.ANDROID.riderAppIdentifier)
        assertEquals("com.aistudio.quickdelivery.customer.ios", TargetMobilePlatform.IOS.customerAppIdentifier)
        assertEquals("com.aistudio.quickdelivery.rider.ios", TargetMobilePlatform.IOS.riderAppIdentifier)

        val breakdown = CrossPlatformDeliveryEngine.calculateFare(distanceKm = 5.0, vehicleType = "BIKE")
        // Base ₹40 + (5 km * ₹12) = ₹100 (10000 paise) -> 20% commission (₹20 / 2000 paise), 80% rider (₹80 / 8000 paise)
        assertEquals(10000L, breakdown.totalAmountPaise)
        assertEquals(2000L, breakdown.platformCommissionPaise)
        assertEquals(8000L, breakdown.riderEarningPaise)

        // ₹150 (15000 paise) commission debt ceiling enforced identically on Android and iOS
        assertTrue(CrossPlatformDeliveryEngine.canRiderAcceptNextOrder(currentDebtPaise = 9000L, projectedCommissionPaise = 2000L))
        assertFalse(CrossPlatformDeliveryEngine.canRiderAcceptNextOrder(currentDebtPaise = 14000L, projectedCommissionPaise = 2000L))
    }
}

