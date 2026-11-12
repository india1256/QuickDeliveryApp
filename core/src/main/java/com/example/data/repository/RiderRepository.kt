package com.example.data.repository

import com.example.data.model.DeliveryProof
import com.example.data.model.OrderProofRegistry
import com.example.data.model.PickupProof
import com.example.data.network.QuickDeliveryApi
import com.example.data.network.QuickDeliveryProofBackendService
import com.example.data.network.UploadDeliveryProofRequest
import com.example.data.network.UploadPickupProofRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository coordinating Rider order pickup & delivery completion with mandatory
 * live camera photo + finger-drawn Digital Signature proof verification.
 */
class RiderRepository(
    private val sharedOrderRepository: SharedOrderRepository? = null,
    private val api: QuickDeliveryApi = QuickDeliveryProofBackendService
) {
    /**
     * Completes Order Pickup after Pickup OTP verification by uploading:
     * 1. Mandatory live camera package photo (`packagePhotoUrl`)
     * 2. Sender's name (`senderName`)
     * 3. Sender's finger-drawn digital signature SVG (`senderSignatureSvg`)
     * to `POST /api/v1/riders/orders/:orderId/proof-pickup` and advancing the order status to `PICKED_UP`.
     */
    suspend fun completePickupWithProof(
        orderId: String,
        riderId: String,
        riderName: String,
        enteredOtp: String,
        packagePhotoUrl: String,
        packagePhotoSummary: String = "Live Camera Package Photo Verified",
        senderName: String,
        senderSignatureSvg: String,
        latitude: Double = 12.9716,
        longitude: Double = 77.5946
    ): Result<PickupProof> {
        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        val request = UploadPickupProofRequest(
            orderId = orderId,
            riderId = riderId,
            riderName = riderName,
            packagePhotoUrl = packagePhotoUrl,
            packagePhotoSummary = packagePhotoSummary,
            senderName = senderName,
            senderSignatureSvg = senderSignatureSvg,
            verifiedOtp = enteredOtp,
            latitude = latitude,
            longitude = longitude,
            capturedAt = timestamp
        )

        val response = api.uploadPickupProof(orderId, request)
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success || body.pickupProof == null) {
            return Result.failure(
                IllegalArgumentException(body?.message ?: "Failed to upload pickup proof and digital signature.")
            )
        }

        val savedProof = body.pickupProof
        OrderProofRegistry.savePickupProof(savedProof)

        if (sharedOrderRepository != null) {
            val otpResult = sharedOrderRepository.verifyPickupOtpByRider(
                orderId,
                enteredOtp,
                riderId
            )
            if (otpResult.isFailure) {
                return Result.failure(
                    otpResult.exceptionOrNull() ?: IllegalStateException("Pickup OTP verification failed.")
                )
            }
        }

        return Result.success(savedProof)
    }

    /**
     * Completes Order Delivery after Delivery OTP verification by uploading:
     * 1. Mandatory live camera drop photo (`deliveryPhotoUrl`)
     * 2. Receiver's name (`recipientName`)
     * 3. Recipient's finger-drawn digital signature SVG (`recipientSignatureSvg`)
     * to `POST /api/v1/riders/orders/:orderId/proof-delivery` and advancing the order status to `DELIVERED`.
     */
    suspend fun completeDeliveryWithProof(
        orderId: String,
        riderId: String,
        riderName: String,
        enteredOtp: String,
        deliveryPhotoUrl: String,
        deliveryPhotoSummary: String = "Live Camera Drop Photo Verified",
        recipientName: String,
        recipientSignatureSvg: String,
        codAmountCollectedInr: Double = 0.0,
        latitude: Double = 12.9352,
        longitude: Double = 77.6245
    ): Result<DeliveryProof> {
        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        val request = UploadDeliveryProofRequest(
            orderId = orderId,
            riderId = riderId,
            riderName = riderName,
            deliveryPhotoUrl = deliveryPhotoUrl,
            deliveryPhotoSummary = deliveryPhotoSummary,
            recipientName = recipientName,
            recipientSignatureSvg = recipientSignatureSvg,
            verifiedOtp = enteredOtp,
            codAmountCollectedInr = codAmountCollectedInr,
            latitude = latitude,
            longitude = longitude,
            capturedAt = timestamp
        )

        val response = api.uploadDeliveryProof(orderId, request)
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success || body.deliveryProof == null) {
            return Result.failure(
                IllegalArgumentException(body?.message ?: "Failed to upload delivery proof and digital signature.")
            )
        }

        val savedProof = body.deliveryProof
        OrderProofRegistry.saveDeliveryProof(savedProof)

        if (sharedOrderRepository != null) {
            val otpResult = sharedOrderRepository.verifyDeliveryOtpByRider(
                orderId,
                enteredOtp,
                riderId
            )
            if (otpResult.isFailure) {
                return Result.failure(
                    otpResult.exceptionOrNull() ?: IllegalStateException("Delivery OTP verification failed.")
                )
            }
        }

        return Result.success(savedProof)
    }

    companion object {
        @Volatile
        private var instance: RiderRepository? = null

        fun getInstance(sharedOrderRepository: SharedOrderRepository? = null): RiderRepository {
            return instance ?: synchronized(this) {
                instance ?: RiderRepository(sharedOrderRepository).also { instance = it }
            }
        }
    }
}
