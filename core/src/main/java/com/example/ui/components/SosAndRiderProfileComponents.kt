package com.example.ui.components

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.cloud.DeliveryProofAndGpsManager
import com.example.data.model.CustomerOrder
import com.example.data.model.RiderProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val SosCrimson = Color(0xFFDC2626)
private val SosDarkRed = Color(0xFF991B1B)
private val SosLightBg = Color(0xFFFEF2F2)
private val TrustTeal = Color(0xFF00A896)
private val TrustNavy = Color(0xFF0D1B2A)
private val TrustEmerald = Color(0xFF059669)
private val TransferOrange = Color(0xFFFF6B35)

data class SosSupportAlert(
    val alertId: String,
    val orderId: String,
    val reporterRole: String, // "CUSTOMER" or "RIDER"
    val reporterName: String,
    val gpsCoordinates: String,
    val routeSummary: String,
    val triggeredAt: String,
    val isResolved: Boolean = false
)

data class BackupRiderCandidate(
    val riderId: String,
    val name: String,
    val mobileNumber: String,
    val vehiclePlate: String
)

data class OrderTransferRecord(
    val orderId: String,
    val fromRiderId: String,
    val toRiderId: String,
    val toRiderName: String,
    val toRiderMobile: String,
    val toRiderVehiclePlate: String,
    val reason: String,
    val transferredAt: String
)

/**
 * Manages the Rider Order Transfer Facility (e.g., when a Rider's vehicle breaks down).
 * Strictly enforces a maximum of 3 transfers per 7-day week.
 * Once all 3 transfers are used in a week, order transfer is LOCKED for the remainder of the week
 * and unlocks automatically next week!
 */
object RiderOrderTransferManager {
    const val MAX_WEEKLY_TRANSFERS = 3
    private const val ONE_WEEK_MS = 7L * 24L * 60L * 60L * 1000L
    private const val PREFS_NAME = "quick_delivery_order_transfer_prefs"
    private const val KEY_WEEK_START_MS = "transfer_week_start_ms"
    private const val KEY_WEEKLY_USED_COUNT = "transfer_weekly_used_count"
    private const val KEY_TRANSFERS_BY_ORDER_JSON = "transfers_by_order_json"

    val backupRiders = listOf(
        BackupRiderCandidate("RIDER-102", "Aman Verma", "+91 98112 33445", "KA-01-EQ-4421"),
        BackupRiderCandidate("RIDER-103", "Rohit Kumar", "+91 98223 44556", "KA-03-JM-7812"),
        BackupRiderCandidate("RIDER-104", "Deepak Yadav", "+91 98334 55667", "KA-05-NB-9104")
    )

    val transferReasons = listOf(
        "Vehicle Breakdown (Gadi Kharab)",
        "Flat Tyre / Fuel Issue",
        "Health / Personal Emergency",
        "Heavy Traffic / Route Blocked"
    )

    private val _weeklyTransfersUsed = MutableStateFlow(0)
    val weeklyTransfersUsed: StateFlow<Int> = _weeklyTransfersUsed.asStateFlow()

    private val _weekStartEpochMs = MutableStateFlow(System.currentTimeMillis())
    val weekStartEpochMs: StateFlow<Long> = _weekStartEpochMs.asStateFlow()

    private val _transfersByOrder = MutableStateFlow<Map<String, OrderTransferRecord>>(emptyMap())
    val transfersByOrder: StateFlow<Map<String, OrderTransferRecord>> = _transfersByOrder.asStateFlow()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        var savedWeekStart = prefs.getLong(KEY_WEEK_START_MS, 0L)
        var savedCount = prefs.getInt(KEY_WEEKLY_USED_COUNT, 0)

        if (savedWeekStart <= 0L) {
            savedWeekStart = now
            prefs.edit().putLong(KEY_WEEK_START_MS, savedWeekStart).apply()
        } else if (now - savedWeekStart >= ONE_WEEK_MS) {
            // New week reached — automatically unlock order transfers for the new week!
            savedWeekStart = now
            savedCount = 0
            prefs.edit()
                .putLong(KEY_WEEK_START_MS, savedWeekStart)
                .putInt(KEY_WEEKLY_USED_COUNT, 0)
                .apply()
        }

        _weekStartEpochMs.value = savedWeekStart
        _weeklyTransfersUsed.value = savedCount.coerceIn(0, MAX_WEEKLY_TRANSFERS)

        val raw = prefs.getString(KEY_TRANSFERS_BY_ORDER_JSON, null)
        if (raw != null) {
            runCatching {
                val root = JSONObject(raw)
                val parsed = mutableMapOf<String, OrderTransferRecord>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val orderId = keys.next()
                    val obj = root.getJSONObject(orderId)
                    parsed[orderId] = OrderTransferRecord(
                        orderId = orderId,
                        fromRiderId = obj.optString("fromRiderId", "RIDER-101"),
                        toRiderId = obj.optString("toRiderId", "RIDER-102"),
                        toRiderName = obj.optString("toRiderName", "Aman Verma"),
                        toRiderMobile = obj.optString("toRiderMobile", "+91 98112 33445"),
                        toRiderVehiclePlate = obj.optString("toRiderVehiclePlate", "KA-01-EQ-4421"),
                        reason = obj.optString("reason", "Vehicle Breakdown (Gadi Kharab)"),
                        transferredAt = obj.optString("transferredAt", "")
                    )
                }
                _transfersByOrder.value = parsed
            }
        }
    }

    fun nextWeekUnlockDateFormatted(weekStartMs: Long = _weekStartEpochMs.value): String {
        val unlockEpochMs = weekStartMs + ONE_WEEK_MS
        return SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date(unlockEpochMs))
    }

    fun transferOrderToAnotherRider(
        context: Context,
        orderId: String,
        fromRiderId: String,
        targetRider: BackupRiderCandidate,
        reason: String
    ): Result<OrderTransferRecord> {
        ensureLoaded(context)
        val currentUsed = _weeklyTransfersUsed.value
        if (currentUsed >= MAX_WEEKLY_TRANSFERS) {
            val unlockDate = nextWeekUnlockDateFormatted()
            return Result.failure(
                IllegalStateException(
                    "Weekly Order Transfer limit reached ($MAX_WEEKLY_TRANSFERS/$MAX_WEEKLY_TRANSFERS used). Locked for this week — unlocks next week on $unlockDate."
                )
            )
        }

        val timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val record = OrderTransferRecord(
            orderId = orderId,
            fromRiderId = fromRiderId,
            toRiderId = targetRider.riderId,
            toRiderName = targetRider.name,
            toRiderMobile = targetRider.mobileNumber,
            toRiderVehiclePlate = targetRider.vehiclePlate,
            reason = reason,
            transferredAt = timestamp
        )

        val newUsedCount = (currentUsed + 1).coerceAtMost(MAX_WEEKLY_TRANSFERS)
        _weeklyTransfersUsed.value = newUsedCount

        val updatedMap = _transfersByOrder.value.toMutableMap().apply {
            put(orderId, record)
        }
        _transfersByOrder.value = updatedMap

        val appCtx = context.applicationContext
        ioScope.launch {
            runCatching {
                val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val root = JSONObject()
                updatedMap.forEach { (id, item) ->
                    root.put(
                        id,
                        JSONObject().apply {
                            put("orderId", item.orderId)
                            put("fromRiderId", item.fromRiderId)
                            put("toRiderId", item.toRiderId)
                            put("toRiderName", item.toRiderName)
                            put("toRiderMobile", item.toRiderMobile)
                            put("toRiderVehiclePlate", item.toRiderVehiclePlate)
                            put("reason", item.reason)
                            put("transferredAt", item.transferredAt)
                        }
                    )
                }
                prefs.edit()
                    .putInt(KEY_WEEKLY_USED_COUNT, newUsedCount)
                    .putString(KEY_TRANSFERS_BY_ORDER_JSON, root.toString())
                    .apply()
            }
        }

        return Result.success(record)
    }
}

/**
 * Shared reactive manager for SOS Emergency Support Alerts with live GPS coordinates,
 * synced across Customer, Rider, and Admin Support screens.
 */
object SosSafetySupportManager {
    private const val PREFS_NAME = "quick_delivery_sos_safety_prefs"
    private const val KEY_SOS_ALERTS_JSON = "sos_alerts_json"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var isLoaded = false

    private val _sosAlerts = MutableStateFlow<List<SosSupportAlert>>(emptyList())
    val sosAlerts: StateFlow<List<SosSupportAlert>> = _sosAlerts.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (isLoaded) return
        isLoaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SOS_ALERTS_JSON, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<SosSupportAlert>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsed.add(
                    SosSupportAlert(
                        alertId = obj.optString("alertId", "SOS-101"),
                        orderId = obj.optString("orderId", ""),
                        reporterRole = obj.optString("reporterRole", "CUSTOMER"),
                        reporterName = obj.optString("reporterName", "User"),
                        gpsCoordinates = obj.optString("gpsCoordinates", "12.97840, 77.64080"),
                        routeSummary = obj.optString("routeSummary", ""),
                        triggeredAt = obj.optString("triggeredAt", ""),
                        isResolved = obj.optBoolean("isResolved", false)
                    )
                )
            }
            _sosAlerts.value = parsed
        }
    }

    fun triggerSosAlert(
        context: Context,
        orderId: String,
        reporterRole: String,
        reporterName: String,
        gpsCoordinates: String,
        routeSummary: String
    ): SosSupportAlert {
        ensureLoaded(context)
        val timestamp = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.US).format(Date())
        val alertId = "SOS-${(System.currentTimeMillis() % 90000) + 10000}"
        val newAlert = SosSupportAlert(
            alertId = alertId,
            orderId = orderId,
            reporterRole = reporterRole,
            reporterName = reporterName,
            gpsCoordinates = gpsCoordinates,
            routeSummary = routeSummary,
            triggeredAt = timestamp,
            isResolved = false
        )
        val filtered = _sosAlerts.value.filterNot {
            !it.isResolved && it.orderId == orderId && it.reporterRole == reporterRole
        }
        val updated = listOf(newAlert) + filtered
        _sosAlerts.value = updated
        persist(context, updated)
        return newAlert
    }

    fun updateAlertGpsCoordinates(
        context: Context,
        alertId: String,
        gpsCoordinates: String
    ) {
        ensureLoaded(context)
        val updated = _sosAlerts.value.map { alert ->
            if (alert.alertId == alertId) alert.copy(gpsCoordinates = gpsCoordinates) else alert
        }
        _sosAlerts.value = updated
        persist(context, updated)
    }

    fun resolveSosAlert(context: Context, alertId: String) {
        ensureLoaded(context)
        val updated = _sosAlerts.value.map { alert ->
            if (alert.alertId == alertId) alert.copy(isResolved = true) else alert
        }
        _sosAlerts.value = updated
        persist(context, updated)
    }

    private fun persist(context: Context, list: List<SosSupportAlert>) {
        val appCtx = context.applicationContext
        ioScope.launch {
            runCatching {
                val arr = JSONArray()
                list.take(30).forEach { item ->
                    arr.put(
                        JSONObject().apply {
                            put("alertId", item.alertId)
                            put("orderId", item.orderId)
                            put("reporterRole", item.reporterRole)
                            put("reporterName", item.reporterName)
                            put("gpsCoordinates", item.gpsCoordinates)
                            put("routeSummary", item.routeSummary)
                            put("triggeredAt", item.triggeredAt)
                            put("isResolved", item.isResolved)
                        }
                    )
                }
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SOS_ALERTS_JSON, arr.toString())
                    .apply()
            }
        }
    }
}

/**
 * Compact Inline 'Rider Profile' View for Customers once an order is accepted.
 * Shows the assigned Rider's Photo, Full Name, and Mobile Number (for the rider going to pick up the parcel).
 * Note: Star ratings are strictly visible ONLY in the Admin Web Panel, not here.
 */
@Composable
fun AssignedRiderProfileTrustCard(
    order: CustomerOrder,
    riderProfile: RiderProfile? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) {
        RiderOrderTransferManager.ensureLoaded(context)
    }

    val transfersByOrder by RiderOrderTransferManager.transfersByOrder.collectAsState()
    val transferRecord = transfersByOrder[order.orderId]

    val riderId = transferRecord?.toRiderId ?: order.assignedRiderId ?: riderProfile?.riderId ?: "RIDER-101"
    val riderName = transferRecord?.toRiderName ?: order.assignedRiderName ?: riderProfile?.name ?: "Vikram Singh"
    val rawMobile = transferRecord?.toRiderMobile
        ?: order.assignedRiderPhone?.takeIf { it.isNotBlank() }
        ?: riderProfile?.mobile?.takeIf { it.isNotBlank() }
        ?: "+91 98450 67890"
    val formattedMobile = if (rawMobile.startsWith("+")) rawMobile else "+91 $rawMobile"
    val vehiclePlate = transferRecord?.toRiderVehiclePlate
        ?: riderProfile?.vehicleNumber?.takeIf { it.isNotBlank() }
        ?: "KA-01-QD-1024"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, TrustTeal.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("assigned_rider_profile_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Strip: Assigned Pickup Rider Label + KYC Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified Pickup Rider",
                        tint = TrustEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PARCEL PICKUP RIDER DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TrustEmerald
                    )
                }
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✓ KYC Verified",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Main Profile Row: Rider Photo + Rider Name + Rider Mobile Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RiderVerifiedPhotoAvatar(
                    riderName = riderName,
                    size = 64.dp,
                    modifier = Modifier.testTag("assigned_rider_photo_${order.orderId}")
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = riderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TrustNavy,
                        modifier = Modifier.testTag("assigned_rider_name_${order.orderId}")
                    )

                    // Prominent Rider Mobile Number Badge (Tap to open Phone Dial Pad)
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TrustTeal),
                        onClick = {
                            RiderLiveSelfieCallAndExcelManager.launchDialPadAndRecordCallOnline(
                                context = context,
                                orderId = order.orderId,
                                callerRole = "CUSTOMER",
                                callerName = order.customerName,
                                receiverRole = "RIDER",
                                receiverName = riderName,
                                rawPhoneNumber = formattedMobile
                            )
                        },
                        modifier = Modifier.testTag("assigned_rider_mobile_badge_${order.orderId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Rider Mobile Number",
                                tint = TrustTeal,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mobile: $formattedMobile (Tap to Call)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TrustNavy
                            )
                        }
                    }

                    // Vehicle & Partner ID Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${order.vehicleType.title} ($vehiclePlate) • ID: $riderId",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            // 1-Click Direct Phone Dial Pad Button for Customer to Call Rider
            DirectDialPadCallButton(
                orderId = order.orderId,
                callerRole = "CUSTOMER",
                callerName = order.customerName,
                receiverRole = "Rider",
                receiverName = riderName,
                phoneNumber = formattedMobile,
                buttonTestTag = "customer_call_rider_dial_btn_${order.orderId}"
            )

            if (transferRecord != null) {
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TransferOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔄 Order transferred to backup rider ${transferRecord.toRiderName} (${transferRecord.reason})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Rider Order Transfer Facility Card (for vehicle breakdown / emergency).
 * Allows a Rider to transfer an active order to another Rider up to 3 times per week.
 * If all 3 weekly transfers are used in the week, the transfer option locks until next week!
 */
@Composable
fun RiderOrderTransferFacilityCard(
    orderId: String,
    currentRiderId: String,
    onOrderTransferred: (OrderTransferRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { RiderOrderTransferManager.ensureLoaded(context) }

    val weeklyUsed by RiderOrderTransferManager.weeklyTransfersUsed.collectAsState()
    val weekStartMs by RiderOrderTransferManager.weekStartEpochMs.collectAsState()
    val transfersByOrder by RiderOrderTransferManager.transfersByOrder.collectAsState()

    val existingTransfer = transfersByOrder[orderId]
    val remainingThisWeek = (RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS - weeklyUsed).coerceAtLeast(0)
    val isLockedForWeek = weeklyUsed >= RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS
    val nextWeekUnlockText = remember(weekStartMs) {
        RiderOrderTransferManager.nextWeekUnlockDateFormatted(weekStartMs)
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf(RiderOrderTransferManager.transferReasons.first()) }
    var selectedCandidate by remember { mutableStateOf(RiderOrderTransferManager.backupRiders.first()) }
    var localFeedbackMsg by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLockedForWeek) Color(0xFFFEF2F2) else Color(0xFFFFF7ED)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isLockedForWeek) SosCrimson else TransferOrange.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rider_order_transfer_card_$orderId")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isLockedForWeek) Icons.Default.Lock else Icons.Default.SwapHoriz,
                        contentDescription = "Order Transfer Facility",
                        tint = if (isLockedForWeek) SosCrimson else TransferOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Order Transfer (Vehicle Breakdown / Emergency)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TrustNavy
                        )
                        Text(
                            text = if (isLockedForWeek) {
                                "🔒 3/3 Weekly Transfers Used — Locked until Next Week ($nextWeekUnlockText)"
                            } else {
                                "Weekly Quota: $weeklyUsed / ${RiderOrderTransferManager.MAX_WEEKLY_TRANSFERS} Used ($remainingThisWeek left this week)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLockedForWeek) SosDarkRed else Color(0xFF9A3412),
                            modifier = Modifier.testTag("rider_transfer_quota_status_$orderId")
                        )
                    }
                }

                if (!isLockedForWeek) {
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.testTag("rider_toggle_transfer_btn_$orderId")
                    ) {
                        Text(
                            text = if (expanded) "Close" else "Transfer",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (existingTransfer != null) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TrustEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Transferred to ${existingTransfer.toRiderName} (${existingTransfer.toRiderMobile}) • Reason: ${existingTransfer.reason}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (isLockedForWeek) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SosCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_transfer_locked_banner_$orderId")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SosCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "You have used all 3 order transfers this week. Order transfer will automatically unlock next week ($nextWeekUnlockText).",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SosDarkRed
                        )
                    }
                }
            } else {
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = TransferOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Select Reason (e.g. Gadi Kharab / Breakdown):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TrustNavy
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RiderOrderTransferManager.transferReasons.forEach { reason ->
                                FilterChip(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason },
                                    label = { Text(reason, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Text(
                            text = "Select Nearby Available Rider to Transfer Order:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TrustNavy
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RiderOrderTransferManager.backupRiders.forEach { candidate ->
                                FilterChip(
                                    selected = selectedCandidate.riderId == candidate.riderId,
                                    onClick = { selectedCandidate = candidate },
                                    label = {
                                        Text(
                                            text = "${candidate.name} (${candidate.mobileNumber})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("transfer_candidate_${candidate.riderId}")
                                )
                            }
                        }

                        if (localFeedbackMsg != null) {
                            Text(
                                text = localFeedbackMsg.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }

                        Button(
                            onClick = {
                                RiderOrderTransferManager.transferOrderToAnotherRider(
                                    context = context,
                                    orderId = orderId,
                                    fromRiderId = currentRiderId,
                                    targetRider = selectedCandidate,
                                    reason = selectedReason
                                ).onSuccess { record ->
                                    expanded = false
                                    localFeedbackMsg = "Transferred to ${record.toRiderName}!"
                                    onOrderTransferred(record)
                                }.onFailure { err ->
                                    localFeedbackMsg = err.message
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TransferOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_confirm_order_transfer_btn_$orderId")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transfer Order to ${selectedCandidate.name} (${remainingThisWeek} left this week)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Illustrated Rider Portrait Photo Avatar with Verified Partner ring and badge.
 */
@Composable
fun RiderVerifiedPhotoAvatar(
    riderName: String,
    size: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(2.5.dp, TrustEmerald),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            ) {
                val w = this.size.width
                val h = this.size.height

                // Warm gradient portrait backdrop
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00A896), Color(0xFF0D1B2A))
                    )
                )

                // Rider shoulders / uniform jacket
                val shouldersPath = Path().apply {
                    moveTo(w * 0.14f, h)
                    quadraticTo(w * 0.5f, h * 0.62f, w * 0.86f, h)
                    close()
                }
                drawPath(path = shouldersPath, color = Color(0xFFFF6B35))

                // Collar accent
                drawCircle(
                    color = Color.White,
                    radius = w * 0.11f,
                    center = Offset(w * 0.5f, h * 0.78f)
                )

                // Rider head / face
                drawCircle(
                    color = Color(0xFFFDE68A),
                    radius = w * 0.22f,
                    center = Offset(w * 0.5f, h * 0.44f)
                )

                // Rider protective helmet crown
                val helmetPath = Path().apply {
                    moveTo(w * 0.26f, h * 0.40f)
                    quadraticTo(w * 0.5f, h * 0.12f, w * 0.74f, h * 0.40f)
                    close()
                }
                drawPath(path = helmetPath, color = Color(0xFFFF6B35))

                // Helmet visor stripe
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.30f, h * 0.38f),
                    end = Offset(w * 0.70f, h * 0.38f),
                    strokeWidth = 4f
                )
            }
        }

        // Online & Verified Check Badge on bottom-right corner of photo
        Surface(
            color = TrustEmerald,
            shape = CircleShape,
            border = BorderStroke(1.5.dp, Color.White),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified Rider Photo",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * SOS Safety Button & Live Status Banner for Customer Tracking Screen and Rider Active Order Screen.
 * Activates on Hold (Long-Press) or Double-Tap (or rapid second tap within 2.5 seconds) and alerts
 * the Support System with current GPS coordinates.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SosSafetyTriggerButtonAndBanner(
    orderId: String,
    reporterRole: String, // Strictly "RIDER" only
    reporterName: String,
    fallbackGpsCoordinates: String,
    routeSummary: String,
    onSosDispatched: ((SosSupportAlert) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // SOS is strictly for Rider App only, never Customer App
    if (reporterRole.equals("CUSTOMER", ignoreCase = true)) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    remember(context) { SosSafetySupportManager.ensureLoaded(context) }

    val allAlerts by SosSafetySupportManager.sosAlerts.collectAsState()
    val activeAlert = remember(allAlerts, orderId, reporterRole) {
        allAlerts.firstOrNull { !it.isResolved && it.orderId == orderId && it.reporterRole == reporterRole }
    }

    var lastSingleTapEpochMs by remember { mutableLongStateOf(0L) }
    var showHoldOrDoubleTapHint by remember { mutableStateOf(false) }
    var pendingAlertIdForPermission by remember { mutableStateOf<String?>(null) }

    val roleTagPrefix = if (reporterRole.equals("CUSTOMER", ignoreCase = true)) "customer" else "rider"

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val targetAlertId = pendingAlertIdForPermission
        if (granted && targetAlertId != null) {
            scope.launch {
                val liveCoords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                if (liveCoords != null) {
                    val formatted = String.format(
                        Locale.US,
                        "%.5f, %.5f (Verified Device GPS)",
                        liveCoords.first,
                        liveCoords.second
                    )
                    SosSafetySupportManager.updateAlertGpsCoordinates(context, targetAlertId, formatted)
                }
            }
        }
    }

    val fireSosNow = {
        showHoldOrDoubleTapHint = false
        val createdAlert = SosSafetySupportManager.triggerSosAlert(
            context = context,
            orderId = orderId,
            reporterRole = reporterRole,
            reporterName = reporterName,
            gpsCoordinates = fallbackGpsCoordinates,
            routeSummary = routeSummary
        )
        pendingAlertIdForPermission = createdAlert.alertId
        onSosDispatched?.invoke(createdAlert)

        if (DeliveryProofAndGpsManager.hasLocationPermission(context)) {
            scope.launch {
                val liveCoords = DeliveryProofAndGpsManager.fetchLiveGpsLocation(context)
                if (liveCoords != null) {
                    val formatted = String.format(
                        Locale.US,
                        "%.5f, %.5f (Live Device GPS)",
                        liveCoords.first,
                        liveCoords.second
                    )
                    SosSafetySupportManager.updateAlertGpsCoordinates(context, createdAlert.alertId, formatted)
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = if (activeAlert != null) SosDarkRed else SosCrimson,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFFFECACA)),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastSingleTapEpochMs <= 2500L) {
                            lastSingleTapEpochMs = 0L
                            fireSosNow()
                        } else {
                            lastSingleTapEpochMs = now
                            showHoldOrDoubleTapHint = true
                        }
                    },
                    onDoubleClick = {
                        lastSingleTapEpochMs = 0L
                        fireSosNow()
                    },
                    onLongClick = {
                        lastSingleTapEpochMs = 0L
                        fireSosNow()
                    }
                )
                .testTag("${roleTagPrefix}_sos_safety_btn_$orderId")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "SOS Emergency Safety Button",
                                tint = SosCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (activeAlert != null) {
                                "🚨 SOS ALERT ACTIVE — TAP 2X TO REFRESH GPS"
                            } else {
                                "🚨 SOS Safety Button (Hold or Double-Tap)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (showHoldOrDoubleTapHint && activeAlert == null) {
                                "Tap once more or hold now to alert Support with live GPS!"
                            } else {
                                "Alerts 24x7 Support & Admin with your current GPS coordinates"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFEE2E2)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS Coordinates Attached",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = activeAlert != null) {
            if (activeAlert != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SosLightBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, SosCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_active_alert_card_$orderId")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = SosCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Emergency Support Alerted (${activeAlert.alertId})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SosDarkRed
                                )
                            }
                            Text(
                                text = activeAlert.triggeredAt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SosDarkRed
                            )
                        }

                        Text(
                            text = "📍 Dispatched GPS: ${activeAlert.gpsCoordinates}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TrustNavy,
                            modifier = Modifier.testTag("sos_dispatched_gps_text_$orderId")
                        )
                        Text(
                            text = "Synced to Support & Admin Dispatch • Role: ${activeAlert.reporterRole} (${activeAlert.reporterName})",
                            style = MaterialTheme.typography.bodySmall,
                            color = SosDarkRed
                        )

                        OutlinedButton(
                            onClick = {
                                SosSafetySupportManager.resolveSosAlert(context, activeAlert.alertId)
                            },
                            border = BorderStroke(1.dp, SosCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sos_resolve_btn_$orderId")
                        ) {
                            Text(
                                text = "I Am Safe — Resolve SOS Alert",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SosCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live SOS Support Monitor Banner displayed in the Admin Support view whenever a Customer or Rider
 * triggers an SOS safety alert with GPS coordinates.
 */
@Composable
fun AdminSosSupportLiveBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { SosSafetySupportManager.ensureLoaded(context) }

    val allAlerts by SosSafetySupportManager.sosAlerts.collectAsState()
    val unresolvedAlerts = remember(allAlerts) {
        allAlerts.filter { !it.isResolved }
    }

    if (unresolvedAlerts.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = SosLightBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, SosCrimson),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("admin_live_sos_support_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Active SOS Support Alerts",
                        tint = SosCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🚨 LIVE SOS SAFETY ALERTS (${unresolvedAlerts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SosDarkRed
                    )
                }
                Text(
                    text = "24x7 Support Feed",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SosCrimson
                )
            }

            unresolvedAlerts.forEach { alert ->
                HorizontalDivider(color = SosCrimson.copy(alpha = 0.25f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${alert.alertId} • ${alert.reporterRole}: ${alert.reporterName} (Order #${alert.orderId})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TrustNavy
                        )
                        Text(
                            text = "📍 GPS: ${alert.gpsCoordinates}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SosDarkRed
                        )
                        if (alert.routeSummary.isNotBlank()) {
                            Text(
                                text = alert.routeSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            SosSafetySupportManager.resolveSosAlert(context, alert.alertId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SosCrimson),
                        modifier = Modifier.testTag("admin_resolve_sos_btn_${alert.alertId}")
                    ) {
                        Text("Resolve", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
