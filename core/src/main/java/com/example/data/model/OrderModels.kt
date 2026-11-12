package com.example.data.model

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mandatory Pickup Proof captured by the Rider at Pickup (after Pickup OTP verification).
 * Requires both a live camera package photo and the Sender's finger-drawn digital signature.
 */
data class PickupProof(
    val orderId: String,
    val riderId: String,
    val riderName: String,
    val packagePhotoUrl: String,
    val packagePhotoSummary: String = "Live Camera Package Photo Verified",
    val senderName: String,
    val senderSignatureSvg: String,
    val verifiedOtp: String,
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946,
    val capturedAt: String
) {
    val isComplete: Boolean
        get() = packagePhotoUrl.isNotBlank() &&
            senderName.trim().isNotBlank() &&
            senderSignatureSvg.contains("<path")
}

/**
 * Mandatory Delivery Proof (POD) captured by the Rider at Drop-off (after Delivery OTP verification).
 * Requires both a live camera drop photo and the Recipient's finger-drawn digital signature.
 */
data class DeliveryProof(
    val orderId: String,
    val riderId: String,
    val riderName: String,
    val deliveryPhotoUrl: String,
    val deliveryPhotoSummary: String = "Live Camera Drop Photo Verified",
    val recipientName: String,
    val recipientSignatureSvg: String,
    val verifiedOtp: String,
    val codAmountCollectedInr: Double = 0.0,
    val latitude: Double = 12.9352,
    val longitude: Double = 77.6245,
    val capturedAt: String
) {
    val isComplete: Boolean
        get() = deliveryPhotoUrl.isNotBlank() &&
            recipientName.trim().isNotBlank() &&
            recipientSignatureSvg.contains("<path")
}

/**
 * Thread-safe reactive store associating PickupProof and DeliveryProof records with each Order ID
 * across Customer, Rider, and Admin screens.
 */
object OrderProofRegistry {
    private val pickupProofsMap = ConcurrentHashMap<String, PickupProof>()
    private val deliveryProofsMap = ConcurrentHashMap<String, DeliveryProof>()

    private val _pickupProofsByOrder = MutableStateFlow<Map<String, PickupProof>>(emptyMap())
    val pickupProofsByOrder: StateFlow<Map<String, PickupProof>> = _pickupProofsByOrder.asStateFlow()

    private val _deliveryProofsByOrder = MutableStateFlow<Map<String, DeliveryProof>>(emptyMap())
    val deliveryProofsByOrder: StateFlow<Map<String, DeliveryProof>> = _deliveryProofsByOrder.asStateFlow()

    fun savePickupProof(proof: PickupProof) {
        pickupProofsMap[proof.orderId] = proof
        _pickupProofsByOrder.value = pickupProofsMap.toMap()
    }

    fun getPickupProof(orderId: String): PickupProof? = pickupProofsMap[orderId]

    fun saveDeliveryProof(proof: DeliveryProof) {
        deliveryProofsMap[proof.orderId] = proof
        _deliveryProofsByOrder.value = deliveryProofsMap.toMap()
    }

    fun getDeliveryProof(orderId: String): DeliveryProof? = deliveryProofsMap[orderId]

    fun clearForTesting() {
        pickupProofsMap.clear()
        deliveryProofsMap.clear()
        _pickupProofsByOrder.value = emptyMap()
        _deliveryProofsByOrder.value = emptyMap()
    }
}

val CustomerOrder.pickupProof: PickupProof?
    get() = OrderProofRegistry.getPickupProof(orderId)

val CustomerOrder.deliveryProof: DeliveryProof?
    get() = OrderProofRegistry.getDeliveryProof(orderId)
