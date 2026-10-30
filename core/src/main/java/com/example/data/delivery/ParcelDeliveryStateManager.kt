package com.example.data.delivery

import com.example.data.cloud.CloudDeliveryOrder
import com.example.data.cloud.QuickDeliveryCloudRepository
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Canonical parcel delivery lifecycle stages shared across Customer (:app) and Rider (:rider) apps.
 */
enum class ParcelDeliveryStage(
    val firestoreStatus: String,
    val displayLabel: String,
    val customerSummary: String,
    val riderActionLabel: String,
    val stepIndex: Int,
    val progressFraction: Float
) {
    PENDING(
        firestoreStatus = "PENDING_ASSIGNMENT",
        displayLabel = "Pending",
        customerSummary = "Waiting for an available rider partner to accept your parcel.",
        riderActionLabel = "Assign to Rider",
        stepIndex = 0,
        progressFraction = 0.25f
    ),
    ASSIGNED(
        firestoreStatus = "ASSIGNED",
        displayLabel = "Assigned",
        customerSummary = "Rider assigned and heading to pickup location.",
        riderActionLabel = "Start In-Transit",
        stepIndex = 1,
        progressFraction = 0.50f
    ),
    IN_TRANSIT(
        firestoreStatus = "IN_TRANSIT",
        displayLabel = "In-Transit",
        customerSummary = "Parcel picked up and on the way to destination.",
        riderActionLabel = "Mark Delivered",
        stepIndex = 2,
        progressFraction = 0.75f
    ),
    DELIVERED(
        firestoreStatus = "DELIVERED",
        displayLabel = "Delivered",
        customerSummary = "Parcel delivered and verified at destination.",
        riderActionLabel = "Completed",
        stepIndex = 3,
        progressFraction = 1.00f
    );

    fun nextAllowedStage(): ParcelDeliveryStage? = when (this) {
        PENDING -> ASSIGNED
        ASSIGNED -> IN_TRANSIT
        IN_TRANSIT -> DELIVERED
        DELIVERED -> null
    }

    fun canTransitionTo(target: ParcelDeliveryStage): Boolean {
        return target == nextAllowedStage()
    }

    fun toCoreOrderStatus(): OrderStatus = when (this) {
        PENDING -> OrderStatus.SEARCHING_RIDER
        ASSIGNED -> OrderStatus.RIDER_ASSIGNED
        IN_TRANSIT -> OrderStatus.IN_TRANSIT
        DELIVERED -> OrderStatus.DELIVERED
    }

    companion object {
        fun fromFirestoreStatus(rawStatus: String?): ParcelDeliveryStage = when (rawStatus?.uppercase()) {
            "PENDING_ASSIGNMENT", "SEARCHING_RIDER", "PENDING" -> PENDING
            "ASSIGNED", "RIDER_ASSIGNED", "ARRIVED_AT_PICKUP" -> ASSIGNED
            "PICKED_UP", "IN_TRANSIT" -> IN_TRANSIT
            "DELIVERED" -> DELIVERED
            else -> PENDING
        }

        fun fromCoreOrderStatus(status: OrderStatus): ParcelDeliveryStage = when (status) {
            OrderStatus.SEARCHING_RIDER -> PENDING
            OrderStatus.RIDER_ASSIGNED, OrderStatus.ARRIVED_AT_PICKUP -> ASSIGNED
            OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> IN_TRANSIT
            OrderStatus.DELIVERED -> DELIVERED
            OrderStatus.CANCELLED -> PENDING
        }
    }
}

/**
 * Actor role initiating a parcel status transition.
 */
enum class DeliveryActorRole {
    CUSTOMER,
    RIDER,
    ADMIN
}

/**
 * Immutable audit record for each state transition in a parcel's delivery lifecycle.
 */
data class ParcelTransitionAuditEntry(
    val orderId: String,
    val fromStage: ParcelDeliveryStage,
    val toStage: ParcelDeliveryStage,
    val actorRole: DeliveryActorRole,
    val actorId: String,
    val proofNote: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

/**
 * Shared parcel delivery domain model tracking the current stage, assigned rider, proof notes,
 * live GPS coordinates, and transition history.
 */
data class ParcelDeliveryTrackingModel(
    val orderId: String,
    val orderCode: String,
    val customerName: String,
    val pickupAddress: String,
    val dropAddress: String,
    val currentStage: ParcelDeliveryStage = ParcelDeliveryStage.PENDING,
    val assignedRiderId: String? = null,
    val assignedRiderName: String? = null,
    val pickupProofNote: String? = null,
    val deliveryProofNote: String? = null,
    val lastKnownLat: Double? = null,
    val lastKnownLng: Double? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val transitionHistory: List<ParcelTransitionAuditEntry> = emptyList()
) {
    val isTerminal: Boolean
        get() = currentStage == ParcelDeliveryStage.DELIVERED

    val nextStage: ParcelDeliveryStage?
        get() = currentStage.nextAllowedStage()
}

/**
 * Type-safe UI state representation for Customer and Rider parcel tracking screens.
 */
sealed interface ParcelDeliveryUiState {
    data object Idle : ParcelDeliveryUiState

    data class Loading(
        val orderId: String,
        val targetStage: ParcelDeliveryStage? = null
    ) : ParcelDeliveryUiState

    data class Active(
        val tracking: ParcelDeliveryTrackingModel,
        val nextAllowedStage: ParcelDeliveryStage?,
        val statusBannerMessage: String
    ) : ParcelDeliveryUiState

    data class TransitionError(
        val tracking: ParcelDeliveryTrackingModel?,
        val attemptedStage: ParcelDeliveryStage,
        val reason: String
    ) : ParcelDeliveryUiState
}

/**
 * Shared UI state manager in `:core` that validates and coordinates parcel delivery status
 * transitions (`PENDING` -> `ASSIGNED` -> `IN_TRANSIT` -> `DELIVERED`) across Customer and Rider apps.
 */
class ParcelDeliveryStateManager {

    private val _trackedParcels = MutableStateFlow<Map<String, ParcelDeliveryTrackingModel>>(emptyMap())
    val trackedParcels: StateFlow<Map<String, ParcelDeliveryTrackingModel>> = _trackedParcels.asStateFlow()

    private val _activeOrderId = MutableStateFlow<String?>(null)
    val activeOrderId: StateFlow<String?> = _activeOrderId.asStateFlow()

    private val _uiState = MutableStateFlow<ParcelDeliveryUiState>(ParcelDeliveryUiState.Idle)
    val uiState: StateFlow<ParcelDeliveryUiState> = _uiState.asStateFlow()

    /**
     * Registers or updates a parcel in the shared state manager and refreshes the active UI state.
     */
    @Synchronized
    fun registerOrSyncParcel(model: ParcelDeliveryTrackingModel, setAsActive: Boolean = true): ParcelDeliveryTrackingModel {
        _trackedParcels.update { current ->
            val existing = current[model.orderId]
            val merged = if (existing != null) {
                model.copy(
                    transitionHistory = existing.transitionHistory.ifEmpty { model.transitionHistory },
                    pickupProofNote = model.pickupProofNote ?: existing.pickupProofNote,
                    deliveryProofNote = model.deliveryProofNote ?: existing.deliveryProofNote,
                    lastKnownLat = model.lastKnownLat ?: existing.lastKnownLat,
                    lastKnownLng = model.lastKnownLng ?: existing.lastKnownLng
                )
            } else {
                model
            }
            current + (model.orderId to merged)
        }
        val activeId = if (setAsActive || _activeOrderId.value == null) {
            _activeOrderId.value = model.orderId
            model.orderId
        } else {
            _activeOrderId.value
        }
        if (activeId == model.orderId) {
            val latest = _trackedParcels.value[model.orderId] ?: model
            _uiState.value = ParcelDeliveryUiState.Active(
                tracking = latest,
                nextAllowedStage = latest.nextStage,
                statusBannerMessage = latest.currentStage.customerSummary
            )
        }
        return _trackedParcels.value[model.orderId] ?: model
    }

    fun syncFromCustomerOrder(order: CustomerOrder, setAsActive: Boolean = false): ParcelDeliveryTrackingModel {
        AutomatedFairRiderAssignmentEngine.validateCustomerParcelWeightKg(order.packageDetails.weightKg).getOrThrow()
        val model = ParcelDeliveryTrackingModel(
            orderId = order.orderId,
            orderCode = order.orderId,
            customerName = order.customerName.ifBlank { "Customer" },
            pickupAddress = order.pickupAddress.formattedAddress(),
            dropAddress = order.dropAddress.formattedAddress(),
            currentStage = ParcelDeliveryStage.fromCoreOrderStatus(order.orderStatus),
            assignedRiderId = order.assignedRiderId,
            assignedRiderName = order.assignedRiderName
        )
        return registerOrSyncParcel(model, setAsActive = setAsActive)
    }

    fun syncFromCloudOrder(order: CloudDeliveryOrder, setAsActive: Boolean = false): ParcelDeliveryTrackingModel {
        val key = order.id.ifBlank { order.orderCode }
        val model = ParcelDeliveryTrackingModel(
            orderId = key,
            orderCode = order.orderCode.ifBlank { key },
            customerName = order.customerName.ifBlank { "Customer" },
            pickupAddress = order.pickupAddress,
            dropAddress = order.dropAddress,
            currentStage = ParcelDeliveryStage.fromFirestoreStatus(order.status),
            assignedRiderId = order.assignedRiderId ?: order.assignedRiderUid,
            pickupProofNote = order.pickupProofNote,
            deliveryProofNote = order.deliveryProofNote,
            lastKnownLat = order.riderLat,
            lastKnownLng = order.riderLng
        )
        return registerOrSyncParcel(model, setAsActive = setAsActive)
    }

    @Synchronized
    fun selectActiveParcel(orderId: String): Boolean {
        val existing = _trackedParcels.value[orderId] ?: return false
        _activeOrderId.value = orderId
        _uiState.value = ParcelDeliveryUiState.Active(
            tracking = existing,
            nextAllowedStage = existing.nextStage,
            statusBannerMessage = existing.currentStage.customerSummary
        )
        return true
    }

    /**
     * Validates and executes a parcel delivery stage transition (`PENDING` -> `ASSIGNED` -> `IN_TRANSIT` -> `DELIVERED`).
     */
    @Synchronized
    fun transitionTo(
        orderId: String,
        targetStage: ParcelDeliveryStage,
        actorRole: DeliveryActorRole,
        actorId: String,
        riderName: String? = null,
        proofNote: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<ParcelDeliveryTrackingModel> {
        val existing = _trackedParcels.value[orderId]
        if (existing == null) {
            val errorMsg = "Parcel $orderId is not registered in ParcelDeliveryStateManager."
            _uiState.value = ParcelDeliveryUiState.TransitionError(
                tracking = null,
                attemptedStage = targetStage,
                reason = errorMsg
            )
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        if (actorId.isBlank()) {
            val errorMsg = "Actor ID is required to transition parcel ${existing.orderCode}."
            _uiState.value = ParcelDeliveryUiState.TransitionError(
                tracking = existing,
                attemptedStage = targetStage,
                reason = errorMsg
            )
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        if (actorRole == DeliveryActorRole.CUSTOMER) {
            val errorMsg = "Customer role cannot directly advance rider delivery stage to ${targetStage.displayLabel}."
            _uiState.value = ParcelDeliveryUiState.TransitionError(
                tracking = existing,
                attemptedStage = targetStage,
                reason = errorMsg
            )
            return Result.failure(IllegalStateException(errorMsg))
        }

        if (!existing.currentStage.canTransitionTo(targetStage)) {
            val expected = existing.currentStage.nextAllowedStage()?.displayLabel ?: "None (Already Delivered)"
            val errorMsg = "Invalid transition from ${existing.currentStage.displayLabel} to ${targetStage.displayLabel}. Expected next stage: $expected."
            _uiState.value = ParcelDeliveryUiState.TransitionError(
                tracking = existing,
                attemptedStage = targetStage,
                reason = errorMsg
            )
            return Result.failure(IllegalStateException(errorMsg))
        }

        val auditEntry = ParcelTransitionAuditEntry(
            orderId = orderId,
            fromStage = existing.currentStage,
            toStage = targetStage,
            actorRole = actorRole,
            actorId = actorId,
            proofNote = proofNote,
            latitude = latitude,
            longitude = longitude,
            timestampEpochMs = nowEpochMs
        )

        val updatedModel = existing.copy(
            currentStage = targetStage,
            assignedRiderId = when (targetStage) {
                ParcelDeliveryStage.ASSIGNED -> actorId
                else -> existing.assignedRiderId ?: actorId
            },
            assignedRiderName = riderName ?: existing.assignedRiderName,
            pickupProofNote = if (targetStage == ParcelDeliveryStage.IN_TRANSIT && !proofNote.isNullOrBlank()) {
                proofNote
            } else {
                existing.pickupProofNote
            },
            deliveryProofNote = if (targetStage == ParcelDeliveryStage.DELIVERED && !proofNote.isNullOrBlank()) {
                proofNote
            } else {
                existing.deliveryProofNote
            },
            lastKnownLat = latitude ?: existing.lastKnownLat,
            lastKnownLng = longitude ?: existing.lastKnownLng,
            updatedAtEpochMs = nowEpochMs,
            transitionHistory = existing.transitionHistory + auditEntry
        )

        _trackedParcels.update { current -> current + (orderId to updatedModel) }
        _activeOrderId.value = orderId
        _uiState.value = ParcelDeliveryUiState.Active(
            tracking = updatedModel,
            nextAllowedStage = updatedModel.nextStage,
            statusBannerMessage = "Order ${updatedModel.orderCode} moved to ${targetStage.displayLabel}: ${targetStage.customerSummary}"
        )
        return Result.success(updatedModel)
    }

    /**
     * Advances the specified parcel to its immediate next lifecycle stage (`PENDING` -> `ASSIGNED` -> `IN_TRANSIT` -> `DELIVERED`).
     */
    @Synchronized
    fun advanceToNextStage(
        orderId: String,
        actorRole: DeliveryActorRole = DeliveryActorRole.RIDER,
        actorId: String = "RIDER-101",
        riderName: String? = "Vikram Singh",
        proofNote: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<ParcelDeliveryTrackingModel> {
        val current = _trackedParcels.value[orderId]
            ?: return Result.failure(IllegalArgumentException("Parcel $orderId not found."))
        val next = current.nextStage
            ?: return Result.failure(IllegalStateException("Parcel ${current.orderCode} is already ${current.currentStage.displayLabel}."))
        return transitionTo(
            orderId = orderId,
            targetStage = next,
            actorRole = actorRole,
            actorId = actorId,
            riderName = riderName,
            proofNote = proofNote,
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Performs a validated local stage transition and synchronizes the new stage to Cloud Firestore when available.
     */
    suspend fun transitionAndSyncToCloud(
        orderId: String,
        targetStage: ParcelDeliveryStage,
        actorRole: DeliveryActorRole,
        actorId: String,
        cloudRepository: QuickDeliveryCloudRepository,
        riderName: String? = null,
        proofNote: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<ParcelDeliveryTrackingModel> {
        _uiState.value = ParcelDeliveryUiState.Loading(orderId = orderId, targetStage = targetStage)
        val localResult = transitionTo(
            orderId = orderId,
            targetStage = targetStage,
            actorRole = actorRole,
            actorId = actorId,
            riderName = riderName,
            proofNote = proofNote,
            latitude = latitude,
            longitude = longitude
        )
        if (localResult.isFailure) {
            return localResult
        }
        val updated = localResult.getOrThrow()
        when (targetStage) {
            ParcelDeliveryStage.ASSIGNED -> {
                cloudRepository.claimOrderByRider(orderId = orderId, riderId = actorId)
            }
            ParcelDeliveryStage.IN_TRANSIT, ParcelDeliveryStage.DELIVERED -> {
                cloudRepository.updateRiderProofAndGps(
                    orderId = orderId,
                    newStatus = targetStage.firestoreStatus,
                    pickupProofNote = if (targetStage == ParcelDeliveryStage.IN_TRANSIT) proofNote else null,
                    deliveryProofNote = if (targetStage == ParcelDeliveryStage.DELIVERED) proofNote else null,
                    riderLat = latitude,
                    riderLng = longitude
                )
            }
            ParcelDeliveryStage.PENDING -> Unit
        }
        return Result.success(updated)
    }

    companion object {
        @Volatile
        private var INSTANCE: ParcelDeliveryStateManager? = null

        fun getInstance(): ParcelDeliveryStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ParcelDeliveryStateManager().also { INSTANCE = it }
            }
        }
    }
}
