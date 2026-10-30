package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuickDeliveryCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createClaimAndSyncOrderAcrossDevices_customerAndRider_emitsRealtimeUpdatesAndPersistsProof() = runBlocking {
        // 1. Customer (Alice) creates an order on Device A
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = QuickDeliveryCloudRepository(firestore)

        val uniqueCode = "QD-${UUID.randomUUID().toString().take(6)}"
        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(
                CloudDeliveryOrder(
                    orderCode = uniqueCode,
                    customerName = "Aarav Sharma",
                    customerPhone = "9876543210",
                    pickupAddress = "Indiranagar 100ft Road, Bengaluru",
                    dropAddress = "Koramangala 4th Block, Bengaluru",
                    paymentMethod = "GATEWAY_UPI",
                    paymentStatus = "PENDING_GATEWAY_VERIFICATION"
                )
            )
        }
        assertTrue(createRes.isSuccess)
        val orderId = createRes.getOrThrow()

        // Verify client cannot mark order VERIFIED_PAID_TO_COMPANY without server verification
        val spoofPaidRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateOrderStatusByCustomer(
                orderId = orderId,
                newStatus = "PENDING_ASSIGNMENT",
                newPaymentStatus = "VERIFIED_PAID_TO_COMPANY"
            )
        }
        assertTrue("Client must not be able to spoof VERIFIED_PAID_TO_COMPANY", spoofPaidRes.isFailure)

        // 2. Rider (Vikram) signs in on Device B, initializes wallet, and observes open pool in real time
        val riderUid = signInTestUser(RIDER_EMAIL)
        val riderRepo = QuickDeliveryCloudRepository(firestore)
        val walletRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            riderRepo.upsertRiderWallet(
                CloudRiderWallet(
                    riderId = "RIDER-101",
                    riderName = "Vikram Singh",
                    balancePaise = 148000L,
                    commissionDebtPaise = 9000L,
                    maxCommissionDebtPaise = 15000L,
                    shiftStatus = "ONLINE_ACTIVE"
                )
            )
        }
        assertTrue(walletRes.isSuccess)

        val openPool = withTimeout(FLOW_TIMEOUT_MS) {
            riderRepo.observeAvailableOrdersForRider().first { list ->
                list.any { it.id == orderId && it.orderCode == uniqueCode }
            }
        }
        assertTrue(openPool.any { it.id == orderId })

        // 3. Rider claims the order atomically
        val claimRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            riderRepo.claimOrderByRider(orderId = orderId, riderId = "RIDER-101")
        }
        assertTrue("Expected rider claim to succeed: ${claimRes.exceptionOrNull()}", claimRes.isSuccess)

        // 4. Rider persists Camera Proof and Live GPS coordinates
        val proofRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            riderRepo.updateRiderProofAndGps(
                orderId = orderId,
                newStatus = "IN_TRANSIT",
                pickupProofNote = "Parcel Photo Proof Verified (640x480px)",
                riderLat = 12.9716,
                riderLng = 77.5946
            )
        }
        assertTrue("Expected rider proof & GPS persistence to succeed", proofRes.isSuccess)

        // 5. Customer (Alice) on Device A sees the updated status, assigned rider, proof, and GPS in real time
        signInTestUser(ALICE_EMAIL)
        val updatedOrders = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCustomerOrders(aliceUid).first { list ->
                list.any {
                    it.id == orderId &&
                        it.status == "IN_TRANSIT" &&
                        it.assignedRiderUid == riderUid &&
                        it.pickupProofNote?.contains("Photo Proof Verified") == true &&
                        it.riderLat == 12.9716
                }
            }
        }
        assertTrue(updatedOrders.any { it.id == orderId && it.status == "IN_TRANSIT" })
    }

    @Test
    fun getOrderById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = QuickDeliveryCloudRepository(firestore)
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(
                CloudDeliveryOrder(
                    orderCode = "QD-PRIV-1",
                    customerName = "Aarav Sharma",
                    customerPhone = "9876543210",
                    pickupAddress = "MG Road, Bengaluru",
                    dropAddress = "HSR Layout, Bengaluru"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = QuickDeliveryCloudRepository(firestore)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getOrderById(orderId)
        }
        assertTrue(crossRead.isFailure)
        val fsEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeCustomerOrders_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = QuickDeliveryCloudRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCustomerOrders(aliceUid).first()
            }
            fail("Expected unauthenticated observeCustomerOrders to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun riderWallet_enforces150InrMaxCommissionDebtLimitAndLedgerCreation() = runBlocking {
        signInTestUser(RIDER_EMAIL)
        val repository = QuickDeliveryCloudRepository(firestore)

        val walletRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertRiderWallet(
                CloudRiderWallet(
                    riderId = "RIDER-101",
                    riderName = "Vikram Singh",
                    balancePaise = 148000L,
                    commissionDebtPaise = 14000L,
                    maxCommissionDebtPaise = 15000L
                )
            )
        }
        assertTrue(walletRes.isSuccess)

        // Adding ₹20 (2000 paise) exceeds ₹150 (15000 paise) -> must block atomically
        val overLimitRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordCommissionDebtOrBlockIfOverLimit(2000L)
        }
        assertTrue(overLimitRes.isFailure)

        // Adding ₹5 (500 paise) stays at ₹145 (14500 paise) <= ₹150 -> must succeed
        val withinLimitRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordCommissionDebtOrBlockIfOverLimit(500L)
        }
        assertTrue(withinLimitRes.isSuccess)
        assertEquals(14500L, withinLimitRes.getOrThrow().commissionDebtPaise)

        // Append-only Company Ledger creation (pending/receivable status allowed for client)
        val ledgerRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordCompanyLedgerEntry(
                CloudCompanyLedgerDoc(
                    entryType = "RIDER_COMMISSION_RECEIVABLE",
                    referenceId = "REPAY-${UUID.randomUUID().toString().take(6)}",
                    amountPaise = 5000L,
                    isCredit = true,
                    settlementStatus = "RECEIVABLE_FROM_RIDER",
                    description = "Rider 20% Commission Receivable Logged"
                )
            )
        }
        assertTrue(ledgerRes.isSuccess)
    }

    @Test
    fun realTimeOrderChat_customerAndAssignedRiderExchangeMessages_blocksUnauthorizedUser() = runBlocking {
        // 1. Customer (Alice) creates an order
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = QuickDeliveryCloudRepository(firestore)
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(
                CloudDeliveryOrder(
                    orderCode = "QD-CHAT-1",
                    customerName = "Aarav Sharma",
                    customerPhone = "9876543210",
                    pickupAddress = "Indiranagar 100ft Road, Bengaluru",
                    dropAddress = "Koramangala 4th Block, Bengaluru"
                )
            ).getOrThrow()
        }

        // 2. Rider (Vikram) signs in, initializes wallet, and claims the order
        signInTestUser(RIDER_EMAIL)
        val riderRepo = QuickDeliveryCloudRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            riderRepo.upsertRiderWallet(
                CloudRiderWallet(
                    riderId = "RIDER-101",
                    riderName = "Vikram Singh",
                    balancePaise = 148000L,
                    commissionDebtPaise = 5000L,
                    maxCommissionDebtPaise = 15000L,
                    shiftStatus = "ONLINE_ACTIVE"
                )
            ).getOrThrow()
            riderRepo.claimOrderByRider(orderId = orderId, riderId = "RIDER-101").getOrThrow()
        }

        // 3. Customer (Alice) sends a real-time chat message to the assigned rider
        signInTestUser(ALICE_EMAIL)
        val customerMsgRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendOrderChatMessage(
                orderId = orderId,
                senderRole = "CUSTOMER",
                senderName = "Aarav Sharma",
                messageText = "Hi Vikram, please call at the security gate.",
                deliveryStage = "ASSIGNED"
            )
        }
        assertTrue("Customer chat message should succeed: ${customerMsgRes.exceptionOrNull()}", customerMsgRes.isSuccess)

        // 4. Assigned Rider (Vikram) observes the customer's message in real time and replies
        signInTestUser(RIDER_EMAIL)
        val riderObservedMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            riderRepo.observeOrderChatMessages(orderId).first { list ->
                list.any { it.senderRole == "CUSTOMER" && it.messageText.contains("security gate") }
            }
        }
        assertEquals(1, riderObservedMsgs.size)

        val riderReplyRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            riderRepo.sendOrderChatMessage(
                orderId = orderId,
                senderRole = "RIDER",
                senderName = "Vikram Singh",
                messageText = "On my way sir, reaching pickup in 4 mins!",
                deliveryStage = "IN_TRANSIT"
            )
        }
        assertTrue("Assigned rider chat reply should succeed: ${riderReplyRes.exceptionOrNull()}", riderReplyRes.isSuccess)

        // 5. Customer (Alice) sees both chat messages ordered chronologically
        signInTestUser(ALICE_EMAIL)
        val fullConversation = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeOrderChatMessages(orderId).first { list ->
                list.size == 2 && list.any { it.senderRole == "RIDER" && it.messageText.contains("reaching pickup") }
            }
        }
        assertEquals(2, fullConversation.size)
        assertEquals("CUSTOMER", fullConversation[0].senderRole)
        assertEquals("RIDER", fullConversation[1].senderRole)

        // 6. Unrelated user (Bob) cannot read or send chat messages on this order
        signInTestUser(BOB_EMAIL)
        val bobRepo = QuickDeliveryCloudRepository(firestore)
        val bobSendRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.sendOrderChatMessage(
                orderId = orderId,
                senderRole = "CUSTOMER",
                senderName = "Bob Intruder",
                messageText = "Unauthorized chat attempt"
            )
        }
        assertTrue(bobSendRes.isFailure)
        val sendFsEx = bobSendRes.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(sendFsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, sendFsEx?.code)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeOrderChatMessages(orderId).first()
            }
            fail("Expected unauthorized user observeOrderChatMessages to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@quickdelivery.test"
        const val BOB_EMAIL = "bob@quickdelivery.test"
        const val RIDER_EMAIL = "rider101@quickdelivery.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
