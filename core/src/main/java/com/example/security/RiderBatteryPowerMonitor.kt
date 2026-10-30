package com.example.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RiderBatteryPowerState(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val criticalThresholdPercent: Int = 20,
    val isSimulatedOverride: Boolean = false
) {
    val isCriticalPower: Boolean
        get() = !isCharging && (levelPercent <= criticalThresholdPercent || isPowerSaveMode)
}

/**
 * Monitors device battery level and Android [PowerManager] power-save states during active Rider deliveries.
 * Triggers a high-priority modal alert when battery drops to or below 20% (or Power Save Mode activates)
 * while the rider has an active assigned delivery, offering one-tap emergency order reassignment.
 */
object RiderBatteryPowerMonitor {
    const val DEFAULT_CRITICAL_THRESHOLD_PERCENT = 20

    private val _powerState = MutableStateFlow(RiderBatteryPowerState())
    val powerState: StateFlow<RiderBatteryPowerState> = _powerState.asStateFlow()

    fun refreshFromSystem(context: Context): RiderBatteryPowerState {
        if (_powerState.value.isSimulatedOverride) {
            return _powerState.value
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode ?: false
        val isIgnoringOpt = runCatching {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }.getOrDefault(false)

        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val computedPercent = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            100
        }
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val updated = RiderBatteryPowerState(
            levelPercent = computedPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSave,
            isIgnoringBatteryOptimizations = isIgnoringOpt,
            criticalThresholdPercent = DEFAULT_CRITICAL_THRESHOLD_PERCENT,
            isSimulatedOverride = false
        )
        _powerState.value = updated
        return updated
    }

    fun simulateBatteryState(
        levelPercent: Int,
        isCharging: Boolean = false,
        isPowerSaveMode: Boolean = levelPercent <= DEFAULT_CRITICAL_THRESHOLD_PERCENT
    ): RiderBatteryPowerState {
        val simulated = RiderBatteryPowerState(
            levelPercent = levelPercent.coerceIn(1, 100),
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            isIgnoringBatteryOptimizations = _powerState.value.isIgnoringBatteryOptimizations,
            criticalThresholdPercent = DEFAULT_CRITICAL_THRESHOLD_PERCENT,
            isSimulatedOverride = true
        )
        _powerState.value = simulated
        return simulated
    }

    fun clearSimulation(context: Context? = null): RiderBatteryPowerState {
        _powerState.value = _powerState.value.copy(isSimulatedOverride = false)
        return if (context != null) {
            refreshFromSystem(context)
        } else {
            val reset = RiderBatteryPowerState()
            _powerState.value = reset
            reset
        }
    }
}

@Composable
fun RiderBatteryPowerMonitorCard(
    activeOrders: List<CustomerOrder>,
    onEmergencyReassignOrder: (CustomerOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val powerState by RiderBatteryPowerMonitor.powerState.collectAsState()
    var dismissedForOrderId by remember { mutableStateOf<String?>(null) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        RiderBatteryPowerMonitor.refreshFromSystem(context)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (ctx != null && !RiderBatteryPowerMonitor.powerState.value.isSimulatedOverride) {
                    RiderBatteryPowerMonitor.refreshFromSystem(ctx)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val activeDeliveryOrder = activeOrders.firstOrNull {
        it.orderStatus == OrderStatus.RIDER_ASSIGNED ||
            it.orderStatus == OrderStatus.PICKED_UP ||
            it.orderStatus == OrderStatus.IN_TRANSIT
    }

    val showCriticalModal = powerState.isCriticalPower &&
        activeDeliveryOrder != null &&
        dismissedForOrderId != activeDeliveryOrder.orderId

    if (showCriticalModal && activeDeliveryOrder != null) {
        AlertDialog(
            onDismissRequest = { dismissedForOrderId = activeDeliveryOrder.orderId },
            icon = {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = "Critical Battery Alert",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Critical Battery Alert (${powerState.levelPercent}%)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your device battery is at ${powerState.levelPercent}% " +
                            (if (powerState.isPowerSaveMode) "(System Power Save Mode Active) " else "") +
                            "during active delivery #${activeDeliveryOrder.orderId}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "To prevent stranding the customer's parcel if your phone powers off, you can immediately reassign this order to a nearby partner or connect your bike charger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEmergencyReassignOrder(activeDeliveryOrder)
                        dismissedForOrderId = activeDeliveryOrder.orderId
                        statusFeedback = "Emergency reassignment triggered for Order #${activeDeliveryOrder.orderId}."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("rider_battery_emergency_reassign_btn")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Emergency Reassign Order")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        dismissedForOrderId = activeDeliveryOrder.orderId
                        statusFeedback = "Alert acknowledged for Order #${activeDeliveryOrder.orderId}. Please connect charger."
                    },
                    modifier = Modifier.testTag("rider_battery_acknowledge_btn")
                ) {
                    Text("I'm Plugging In (Acknowledge)")
                }
            }
        )
    }

    val containerColor = if (powerState.isCriticalPower) Color(0xFFFEF2F2) else Color.White
    val borderColor = if (powerState.isCriticalPower) Color(0xFFDC2626) else Color(0xFFE2E8F0)
    val accentColor = when {
        powerState.isCriticalPower -> Color(0xFFDC2626)
        powerState.levelPercent <= 40 -> Color(0xFFD97706)
        else -> Color(0xFF10B981)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rider_battery_monitor_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            powerState.isCharging -> Icons.Default.BatteryChargingFull
                            powerState.isCriticalPower -> Icons.Default.BatteryAlert
                            else -> Icons.Default.BatteryFull
                        },
                        contentDescription = "Battery Status",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PowerManager Active Delivery Guard",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1B2A)
                        )
                        Text(
                            text = "Battery: ${powerState.levelPercent}% • " +
                                (if (powerState.isCharging) "Charging" else "Discharging") +
                                (if (powerState.isPowerSaveMode) " • Power Saver ON" else " • Normal Power"),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (powerState.isCriticalPower) "CRITICAL (≤20%)" else "HEALTHY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (powerState.levelPercent / 100f).coerceIn(0f, 1f) },
                color = accentColor,
                trackColor = Color(0xFFE2E8F0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = powerState.isSimulatedOverride && powerState.levelPercent == 15,
                    onClick = {
                        dismissedForOrderId = null
                        RiderBatteryPowerMonitor.simulateBatteryState(
                            levelPercent = 15,
                            isCharging = false,
                            isPowerSaveMode = true
                        )
                    },
                    label = { Text("Simulate 15% Critical", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("simulate_critical_battery_chip")
                )
                FilterChip(
                    selected = !powerState.isSimulatedOverride,
                    onClick = {
                        dismissedForOrderId = null
                        RiderBatteryPowerMonitor.clearSimulation(context)
                    },
                    label = { Text("Live Device Power", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("live_device_battery_chip")
                )
            }

            if (statusFeedback != null) {
                Text(
                    text = statusFeedback.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF065F46),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
