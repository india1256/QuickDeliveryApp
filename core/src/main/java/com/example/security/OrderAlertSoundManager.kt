package com.example.security

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.model.SecurityThreatEvent
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
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

data class AlertCapabilityStatus(
    val notificationPermissionGranted: Boolean,
    val alarmStreamVolume: Int,
    val alarmStreamMaxVolume: Int,
    val ringerModeLabel: String,
    val dndBlocksAlarms: Boolean,
    val hasVibratorHardware: Boolean
) {
    fun isAlarmAudible(): Boolean = alarmStreamVolume > 0 && !dndBlocksAlarms

    fun summaryText(): String = buildString {
        append("Notifications: ")
        append(if (notificationPermissionGranted) "Allowed" else "Off")
        append(" • Alarm Vol: $alarmStreamVolume/$alarmStreamMaxVolume ($ringerModeLabel)")
        if (dndBlocksAlarms) append(" • DND Blocking Alarms")
        if (hasVibratorHardware) append(" • Vibrator Ready")
    }
}

/**
 * Unified Order & Security Alert Sound + Vibration Manager.
 *
 * Ensures Rider incoming order ringtone & vibration:
 * 1. Start loudly on `SEARCHING_RIDER` orders not yet accepted/rejected.
 * 2. Stop immediately & automatically as soon as the order is accepted, rejected, cancelled, or cleared.
 * 3. Never loop forever if cancelled (never swallows CancellationException; uses short pulse vibrations).
 */
object OrderAlertSoundManager {
    private const val CHANNEL_ID_RIDER_ALARM = "quick_delivery_rider_order_alarm_v2"
    private const val CHANNEL_ID_CUSTOMER_UPDATES = "quick_delivery_customer_updates_silent_v3"
    private const val CHANNEL_ID_ADMIN_SECURITY_SIREN = "quick_delivery_admin_security_siren_v2"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var alarmLoopJob: Job? = null

    @Volatile
    private var activeRingtone: Ringtone? = null

    @Volatile
    private var activeToneGenerator: ToneGenerator? = null

    @Volatile
    private var activeVibrator: Vibrator? = null

    @Volatile
    private var appContextRef: Context? = null

    @Volatile
    private var shouldAlarmKeepRinging: Boolean = false

    @Volatile
    private var securitySirenJob: Job? = null

    @Volatile
    private var securityRingtone: Ringtone? = null

    @Volatile
    private var securityVibrator: Vibrator? = null

    // Orders already accepted, rejected, completed, or muted — never re-ring for these!
    private val handledOrMutedOrderIds = ConcurrentHashMap.newKeySet<String>()

    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging: StateFlow<Boolean> = _isAlarmRinging.asStateFlow()

    private val _activeRingingOrderId = MutableStateFlow<String?>(null)
    val activeRingingOrderId: StateFlow<String?> = _activeRingingOrderId.asStateFlow()

    private val _isSecurityAttackSirenRinging = MutableStateFlow(false)
    val isSecurityAttackSirenRinging: StateFlow<Boolean> = _isSecurityAttackSirenRinging.asStateFlow()

    private val _activeSecurityAttackAlert = MutableStateFlow<SecurityThreatEvent?>(null)
    val activeSecurityAttackAlert: StateFlow<SecurityThreatEvent?> = _activeSecurityAttackAlert.asStateFlow()

    private fun resolveVibrator(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun shouldAlertForOrder(orderId: String): Boolean {
        return orderId.isNotBlank() && !handledOrMutedOrderIds.contains(orderId)
    }

    /**
     * Marks an order as accepted / rejected / handled and stops all ringing and vibration immediately.
     */
    @Synchronized
    fun markOrderHandledAndStopAlert(orderId: String?) {
        if (!orderId.isNullOrBlank()) {
            handledOrMutedOrderIds.add(orderId)
        }
        stopOrderAlert()
    }

    fun inspectPlatformCapabilities(context: Context): AlertCapabilityStatus {
        val appCtx = context.applicationContext ?: context
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appCtx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val audioMgr = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val vol = runCatching { audioMgr?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 1 }.getOrDefault(1)
        val maxVol = runCatching { audioMgr?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 7 }.getOrDefault(7)
        val ringerLabel = when (runCatching { audioMgr?.ringerMode }.getOrNull()) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Normal"
        }
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val filter = runCatching { nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL }
            .getOrDefault(NotificationManager.INTERRUPTION_FILTER_ALL)
        val dndBlocks = filter == NotificationManager.INTERRUPTION_FILTER_NONE
        val hasVib = resolveVibrator(appCtx)?.hasVibrator() == true
        return AlertCapabilityStatus(
            notificationPermissionGranted = notifGranted,
            alarmStreamVolume = vol,
            alarmStreamMaxVolume = maxVol,
            ringerModeLabel = ringerLabel,
            dndBlocksAlarms = dndBlocks,
            hasVibratorHardware = hasVib
        )
    }

    fun ensureChannels(context: Context) {
        val appCtx = context.applicationContext ?: context
        appContextRef = appCtx
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            val riderChannel = NotificationChannel(
                CHANNEL_ID_RIDER_ALARM,
                "Rider Order Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts for incoming parcel delivery requests."
                enableVibration(true)
            }
            val custChannel = NotificationChannel(
                CHANNEL_ID_CUSTOMER_UPDATES,
                "Delivery Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent delivery status notifications."
                setSound(null, null)
                enableVibration(false)
            }
            val adminChannel = NotificationChannel(
                CHANNEL_ID_ADMIN_SECURITY_SIREN,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Security alert channel for Admin console."
            }
            nm.createNotificationChannel(riderChannel)
            nm.createNotificationChannel(custChannel)
            nm.createNotificationChannel(adminChannel)
        }
    }

    @Synchronized
    fun notifySecurityAttackDetected(threat: SecurityThreatEvent, context: Context? = null) {
        _activeSecurityAttackAlert.value = threat
        if (context != null) {
            postSystemNotification(
                context = context,
                title = "Security Alert: ${threat.threatType}",
                body = "${threat.detectedRule} (${threat.actionTaken})",
                urgent = true
            )
        }
    }

    @Synchronized
    fun startSecurityAttackSiren(context: Context, threat: SecurityThreatEvent? = null) {
        val appCtx = context.applicationContext ?: context
        appContextRef = appCtx
        if (threat != null) {
            _activeSecurityAttackAlert.value = threat
        }
        if (_isSecurityAttackSirenRinging.value && securitySirenJob?.isActive == true) return
        _isSecurityAttackSirenRinging.value = true
        val vib = resolveVibrator(appCtx)
        securityVibrator = vib
        securitySirenJob?.cancel()
        securitySirenJob = scope.launch {
            try {
                repeat(12) {
                    if (!isActive || !_isSecurityAttackSirenRinging.value) return@launch
                    runCatching {
                        val tg = ToneGenerator(AudioManager.STREAM_ALARM, 90)
                        tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 350)
                        delay(380)
                        tg.release()
                    }
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib?.vibrate(VibrationEffect.createOneShot(250L, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vib?.vibrate(250L)
                        }
                    }
                    delay(500)
                }
            } finally {
                runCatching { securityRingtone?.stop() }
                runCatching { vib?.cancel() }
                _isSecurityAttackSirenRinging.value = false
            }
        }
    }

    @Synchronized
    fun stopSecurityAttackSiren(clearAlertBanner: Boolean = true) {
        _isSecurityAttackSirenRinging.value = false
        if (clearAlertBanner) {
            _activeSecurityAttackAlert.value = null
        }
        securitySirenJob?.cancel()
        securitySirenJob = null
        runCatching { securityRingtone?.stop() }
        securityRingtone = null
        runCatching { securityVibrator?.cancel() }
        securityVibrator = null
    }

    @Synchronized
    fun startLoudOrderAlert(context: Context, orderId: String) {
        if (orderId.isBlank() || handledOrMutedOrderIds.contains(orderId)) return
        val appCtx = context.applicationContext ?: context
        val pkg = appCtx.packageName.lowercase()
        val lockedRole = com.example.data.cloud.MultiPhoneLiveSyncManager.lockedPhoneRole.value.uppercase()
        if (lockedRole == "CUSTOMER" ||
            lockedRole == "ADMIN" ||
            pkg.endsWith(".cust01") ||
            pkg.endsWith(".customer") ||
            pkg.endsWith(".admn03") ||
            pkg.endsWith(".admin")
        ) {
            stopOrderAlert()
            return
        }
        appContextRef = appCtx
        _activeRingingOrderId.value = orderId

        if (shouldAlarmKeepRinging && _isAlarmRinging.value && alarmLoopJob?.isActive == true) {
            return
        }

        shouldAlarmKeepRinging = true
        _isAlarmRinging.value = true

        val vib = resolveVibrator(appCtx)
        activeVibrator = vib
        alarmLoopJob?.cancel()

        alarmLoopJob = scope.launch {
            var tg: ToneGenerator? = null
            try {
                tg = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 100) }.getOrNull()
                activeToneGenerator = tg
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = runCatching {
                    ringtoneUri?.let { uri ->
                        RingtoneManager.getRingtone(appCtx, uri)?.apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                isLooping = false
                            }
                            audioAttributes = AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        }
                    }
                }.getOrNull()
                activeRingtone = ringtone

                // Auto-limit maximum ring duration (~14 cycles * 850ms ≈ 12s) and stop IMMEDIATELY when accepted/rejected
                var cycles = 0
                while (isActive && shouldAlarmKeepRinging && _isAlarmRinging.value && cycles < 14) {
                    if (!shouldAlertForOrder(orderId)) break
                    cycles++
                    runCatching {
                        if (ringtone?.isPlaying == false) {
                            ringtone.play()
                        }
                    }
                    runCatching {
                        tg?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                    }
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib?.vibrate(VibrationEffect.createOneShot(400L, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vib?.vibrate(400L)
                        }
                    }
                    delay(850L)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Stop safely instead of infinite retry loop
            } finally {
                shouldAlarmKeepRinging = false
                _isAlarmRinging.value = false
                _activeRingingOrderId.value = null
                runCatching { activeRingtone?.stop() }
                activeRingtone = null
                runCatching { activeToneGenerator?.stopTone() }
                runCatching { activeToneGenerator?.release() }
                activeToneGenerator = null
                runCatching { tg?.stopTone() }
                runCatching { tg?.release() }
                runCatching { vib?.cancel() }
                runCatching { activeVibrator?.cancel() }
                activeVibrator = null
            }
        }
    }

    @Synchronized
    fun stopOrderAlert() {
        shouldAlarmKeepRinging = false
        _isAlarmRinging.value = false
        _activeRingingOrderId.value = null

        val job = alarmLoopJob
        alarmLoopJob = null
        runCatching { job?.cancel() }

        runCatching { activeRingtone?.stop() }
        activeRingtone = null

        runCatching { activeToneGenerator?.stopTone() }
        runCatching { activeToneGenerator?.release() }
        activeToneGenerator = null

        runCatching { activeVibrator?.cancel() }
        activeVibrator = null

        // Extra safeguard: cancel system vibrator directly via saved application context
        appContextRef?.let { ctx ->
            runCatching { resolveVibrator(ctx)?.cancel() }
        }
    }

    fun postSystemNotification(
        context: Context,
        title: String,
        body: String,
        urgent: Boolean = false,
        notificationId: Int = ((System.currentTimeMillis() % 100000L).toInt())
    ) {
        val appCtx = context.applicationContext ?: context
        appContextRef = appCtx
        ensureChannels(appCtx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appCtx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        runCatching {
            val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = if (urgent) CHANNEL_ID_RIDER_ALARM else CHANNEL_ID_CUSTOMER_UPDATES
            val builder = NotificationCompat.Builder(appCtx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
                .setSilent(!urgent)
                .setAutoCancel(true)
            nm.notify(notificationId, builder.build())
        }
    }
}
