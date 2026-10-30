package com.example.security

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RiderOverspeedState(
    val currentSpeedKmh: Int = 0,
    val speedLimitKmh: Int = 60,
    val isAlarmRinging: Boolean = false,
    val isSafetySimulationMode: Boolean = false,
    val isLiveGpsListening: Boolean = false
) {
    val isOverspeeding: Boolean
        get() = currentSpeedKmh > speedLimitKmh
}

/**
 * Monitors Rider vehicle speed in km/h via real Android GPS [LocationManager] telemetry.
 *
 * - Automatically triggers the Phone Alarm (audible siren tone + system alarm ringtone + vibration)
 *   as soon as `currentSpeedKmh > speedLimitKmh` (60 km/h).
 * - Automatically stops the Phone Alarm immediately once `currentSpeedKmh <= speedLimitKmh`.
 */
object RiderOverspeedAlarmManager {
    const val DEFAULT_SPEED_LIMIT_KMH = 60
    val selectableSpeedLimits = listOf(40, 50, 60)

    private val alarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var alarmLoopJob: Job? = null
    private var activeRingtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var activeLocationListener: LocationListener? = null

    private val _state = MutableStateFlow(RiderOverspeedState())
    val state: StateFlow<RiderOverspeedState> = _state.asStateFlow()

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startLiveGpsSpeedMonitoring(context: Context) {
        if (!hasLocationPermission(context)) return
        val appCtx = context.applicationContext
        val locationManager = appCtx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        stopLiveGpsSpeedMonitoring(appCtx)

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!_state.value.isSafetySimulationMode) {
                    val speedKmh = if (location.hasSpeed()) {
                        (location.speed * 3.6f).roundToInt().coerceAtLeast(0)
                    } else {
                        0
                    }
                    evaluateSpeedAndAlarm(appCtx, speedKmh, isSimulated = false)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        activeLocationListener = listener
        runCatching {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    listener
                )
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    listener
                )
            }
            _state.value = _state.value.copy(
                isLiveGpsListening = true,
                isSafetySimulationMode = false
            )
        }
    }

    fun stopLiveGpsSpeedMonitoring(context: Context) {
        val appCtx = context.applicationContext
        val locationManager = appCtx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        activeLocationListener?.let { listener ->
            runCatching { locationManager?.removeUpdates(listener) }
        }
        activeLocationListener = null
        _state.value = _state.value.copy(isLiveGpsListening = false)
    }

    fun updateSpeedLimit(context: Context, newLimitKmh: Int) {
        val clampedLimit = newLimitKmh.coerceIn(30, 80)
        _state.value = _state.value.copy(speedLimitKmh = clampedLimit)
        evaluateSpeedAndAlarm(
            context = context,
            speedKmh = _state.value.currentSpeedKmh,
            isSimulated = _state.value.isSafetySimulationMode
        )
    }

    fun setSimulatedSpeedKmh(context: Context, simulatedSpeedKmh: Int) {
        evaluateSpeedAndAlarm(
            context = context,
            speedKmh = simulatedSpeedKmh.coerceIn(0, 120),
            isSimulated = true
        )
    }

    fun switchToLiveGpsMode(context: Context) {
        evaluateSpeedAndAlarm(context, speedKmh = 0, isSimulated = false)
        if (hasLocationPermission(context)) {
            startLiveGpsSpeedMonitoring(context)
        }
    }

    private fun evaluateSpeedAndAlarm(
        context: Context,
        speedKmh: Int,
        isSimulated: Boolean
    ) {
        val limit = _state.value.speedLimitKmh
        val overSpeed = speedKmh > limit
        if (overSpeed) {
            _state.value = _state.value.copy(
                currentSpeedKmh = speedKmh,
                isAlarmRinging = true,
                isSafetySimulationMode = isSimulated
            )
            startPhoneOverspeedAlarm(context.applicationContext)
        } else {
            // Speed dropped to or below limit -> Immediately stop phone alarm!
            stopPhoneOverspeedAlarm()
            _state.value = _state.value.copy(
                currentSpeedKmh = speedKmh,
                isAlarmRinging = false,
                isSafetySimulationMode = isSimulated
            )
        }
    }

    private fun startPhoneOverspeedAlarm(appContext: Context) {
        if (alarmLoopJob?.isActive == true) return
        alarmLoopJob = alarmScope.launch {
            runCatching {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (alarmUri != null && activeRingtone == null) {
                    activeRingtone = RingtoneManager.getRingtone(appContext, alarmUri)
                }
            }
            while (isActive && _state.value.isOverspeeding) {
                runCatching {
                    val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_ALARM, 100).also {
                        toneGenerator = it
                    }
                    tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 380)
                }
                runCatching {
                    if (activeRingtone?.isPlaying == false) {
                        activeRingtone?.play()
                    }
                }
                triggerSafetyVibrationPulse(appContext)
                delay(650)
            }
            stopPhoneOverspeedAlarm()
        }
    }

    fun stopPhoneOverspeedAlarm() {
        alarmLoopJob?.cancel()
        alarmLoopJob = null
        runCatching {
            activeRingtone?.stop()
        }
        runCatching {
            toneGenerator?.stopTone()
        }
    }

    private fun triggerSafetyVibrationPulse(context: Context) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(250L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(250L)
            }
        }
    }
}

private val AlarmRed = Color(0xFFDC2626)
private val AlarmDarkRed = Color(0xFF991B1B)

/**
 * Silent Background Over-Speed Alarm Monitor:
 * - Monitors real GPS speed via [LocationManager] in the background with a 60 km/h speed limit.
 * - Hidden on the Rider screen during normal riding (<= 60 km/h).
 * - Automatically rings the phone alarm and shows an urgent warning banner ONLY if the Rider crosses 60 km/h,
 *   and automatically stops the alarm and hides the banner as soon as speed drops to <= 60 km/h.
 */
@Composable
fun RiderOverspeedSafetyCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overspeedState by RiderOverspeedAlarmManager.state.collectAsState()

    DisposableEffect(context) {
        if (RiderOverspeedAlarmManager.hasLocationPermission(context)) {
            RiderOverspeedAlarmManager.startLiveGpsSpeedMonitoring(context)
        }
        onDispose {
            RiderOverspeedAlarmManager.stopLiveGpsSpeedMonitoring(context)
            RiderOverspeedAlarmManager.stopPhoneOverspeedAlarm()
        }
    }

    AnimatedVisibility(visible = overspeedState.isOverspeeding) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, AlarmRed),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("rider_overspeed_alarm_card")
        ) {
            Surface(
                color = AlarmRed,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .testTag("rider_overspeed_active_alarm_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Over-Speed Alarm Ringing",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🚨 SPEED LIMIT 60 KM/H CROSSED (${overspeedState.currentSpeedKmh} km/h)!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Kripya speed 60 km/h se kam karein — Speed kam hote hi alarm apne aap band ho jayega!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    }
                    Surface(
                        color = AlarmDarkRed,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("rider_current_speed_badge")
                    ) {
                        Text(
                            text = "${overspeedState.currentSpeedKmh} km/h",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
