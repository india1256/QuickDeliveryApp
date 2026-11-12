package com.example.data.network

import com.example.data.model.DeliveryProof
import com.example.data.model.OrderProofRegistry
import com.example.data.model.PickupProof
import com.example.security.QuickDeliverySecurityGuard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Request payload for `POST /api/v1/riders/orders/:orderId/proof-pickup`
 */
data class UploadPickupProofRequest(
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
    val capturedAt: String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
)

data class UploadPickupProofResponse(
    val success: Boolean,
    val orderId: String,
    val status: String,
    val pickupProof: PickupProof?,
    val message: String
)

/**
 * Request payload for `POST /api/v1/riders/orders/:orderId/proof-delivery`
 */
data class UploadDeliveryProofRequest(
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
    val capturedAt: String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
)

data class UploadDeliveryProofResponse(
    val success: Boolean,
    val orderId: String,
    val status: String,
    val deliveryProof: DeliveryProof?,
    val message: String
)

/**
 * Retrofit API contract for Rider Pickup & Delivery Digital Signature + Camera Proof endpoints.
 */
interface QuickDeliveryApi {
    @POST("/api/v1/riders/orders/{orderId}/proof-pickup")
    suspend fun uploadPickupProof(
        @Path("orderId") orderId: String,
        @Body request: UploadPickupProofRequest
    ): Response<UploadPickupProofResponse>

    @POST("/api/v1/riders/orders/{orderId}/proof-delivery")
    suspend fun uploadDeliveryProof(
        @Path("orderId") orderId: String,
        @Body request: UploadDeliveryProofRequest
    ): Response<UploadDeliveryProofResponse>
}

/**
 * Embedded & Remote Backend Handler for:
 * - `POST /api/v1/riders/orders/:orderId/proof-pickup`
 * - `POST /api/v1/riders/orders/:orderId/proof-delivery`
 */
object QuickDeliveryProofBackendService : QuickDeliveryApi {

    override suspend fun uploadPickupProof(
        orderId: String,
        request: UploadPickupProofRequest
    ): Response<UploadPickupProofResponse> {
        val cleanOrderId = QuickDeliverySecurityGuard.sanitizeIdentifier(orderId.ifBlank { request.orderId }, 64)
        val cleanSenderName = QuickDeliverySecurityGuard.sanitizeText(request.senderName, 80)
        val cleanPhotoUrl = request.packagePhotoUrl.trim()
        val cleanSignatureSvg = request.senderSignatureSvg.trim()

        if (cleanPhotoUrl.isBlank()) {
            return Response.success(
                UploadPickupProofResponse(
                    success = false,
                    orderId = cleanOrderId,
                    status = "PHOTO_REQUIRED",
                    pickupProof = null,
                    message = "Mandatory live camera package photo is required before confirming pickup."
                )
            )
        }

        if (cleanSenderName.isBlank()) {
            return Response.success(
                UploadPickupProofResponse(
                    success = false,
                    orderId = cleanOrderId,
                    status = "SENDER_NAME_REQUIRED",
                    pickupProof = null,
                    message = "Sender Name is required for pickup proof verification."
                )
            )
        }

        if (!cleanSignatureSvg.contains("<svg") || !cleanSignatureSvg.contains("<path")) {
            return Response.success(
                UploadPickupProofResponse(
                    success = false,
                    orderId = cleanOrderId,
                    status = "SIGNATURE_REQUIRED",
                    pickupProof = null,
                    message = "Sender finger-drawn Digital Signature is required before confirming pickup."
                )
            )
        }

        val proof = PickupProof(
            orderId = cleanOrderId,
            riderId = request.riderId,
            riderName = QuickDeliverySecurityGuard.sanitizeText(request.riderName, 80).ifBlank { "Rider Partner" },
            packagePhotoUrl = cleanPhotoUrl,
            packagePhotoSummary = QuickDeliverySecurityGuard.sanitizeText(request.packagePhotoSummary, 160)
                .ifBlank { "Live Camera Package Photo Verified" },
            senderName = cleanSenderName,
            senderSignatureSvg = cleanSignatureSvg,
            verifiedOtp = request.verifiedOtp.trim(),
            latitude = request.latitude,
            longitude = request.longitude,
            capturedAt = request.capturedAt
        )

        OrderProofRegistry.savePickupProof(proof)

        return Response.success(
            UploadPickupProofResponse(
                success = true,
                orderId = cleanOrderId,
                status = "PICKED_UP",
                pickupProof = proof,
                message = "Pickup proof verified: Camera photo & Digital Signature by $cleanSenderName saved."
            )
        )
    }

    override suspend fun uploadDeliveryProof(
        orderId: String,
        request: UploadDeliveryProofRequest
    ): Response<UploadDeliveryProofResponse> {
        val cleanOrderId = QuickDeliverySecurityGuard.sanitizeIdentifier(orderId.ifBlank { request.orderId }, 64)
        val cleanRecipientName = QuickDeliverySecurityGuard.sanitizeText(request.recipientName, 80)
        val cleanPhotoUrl = request.deliveryPhotoUrl.trim()
        val cleanSignatureSvg = request.recipientSignatureSvg.trim()

        if (cleanPhotoUrl.isBlank()) {
            return Response.success(
                UploadDeliveryProofResponse(
                    success = false,
                    orderId = cleanOrderId,
                    status = "PHOTO_REQUIRED",
                    deliveryProof = null,
                    message = "Mandatory live camera drop photo is required before confirming delivery."
                )
            )
        }

        if (cleanRecipientName.isBlank()) {
            return Response.success(
                UploadDeliveryProofResponse(
                    success = false,
                    orderId = cleanOrderId,
                    status = "RECIPIENT_NAME_REQUIRED",
                    deliveryProof = null,
                    message = "Receiver Name is required for delivery proof verification."
                )
            )
        }

        if (!cleanSignatureSvg.contains("<svg") || !cleanSignatureSvg.contains("<path")) {
            return Response.success(
                UploadDeliveryProofResponse(
                    success = false,
                    orderId = cleanOrderId,
                    status = "SIGNATURE_REQUIRED",
                    deliveryProof = null,
                    message = "Recipient finger-drawn Digital Signature is required before confirming delivery."
                )
            )
        }

        val proof = DeliveryProof(
            orderId = cleanOrderId,
            riderId = request.riderId,
            riderName = QuickDeliverySecurityGuard.sanitizeText(request.riderName, 80).ifBlank { "Rider Partner" },
            deliveryPhotoUrl = cleanPhotoUrl,
            deliveryPhotoSummary = QuickDeliverySecurityGuard.sanitizeText(request.deliveryPhotoSummary, 160)
                .ifBlank { "Live Camera Drop Photo Verified" },
            recipientName = cleanRecipientName,
            recipientSignatureSvg = cleanSignatureSvg,
            verifiedOtp = request.verifiedOtp.trim(),
            codAmountCollectedInr = request.codAmountCollectedInr,
            latitude = request.latitude,
            longitude = request.longitude,
            capturedAt = request.capturedAt
        )

        OrderProofRegistry.saveDeliveryProof(proof)

        return Response.success(
            UploadDeliveryProofResponse(
                success = true,
                orderId = cleanOrderId,
                status = "DELIVERED",
                deliveryProof = proof,
                message = "Delivery proof verified: Camera photo & Digital Signature by $cleanRecipientName saved."
            )
        )
    }
}
