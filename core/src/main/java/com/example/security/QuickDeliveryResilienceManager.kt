package com.example.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.data.cloud.MultiPhoneLiveSyncManager
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerPaymentMethod
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Structured record persisted when QuickDeliveryCrashShield intercepts an uncaught exception.
 */
data class CrashReportRecord(
    val crashId: String,
    val appRole: String,
    val appVersion: String,
    val deviceModel: String,
    val androidSdk: Int,
    val timestampEpochMs: Long,
    val timestampLabel: String,
    val threadName: String,
    val exceptionSummary: String,
    val stackTracePreview: String
)

/**
 * Global Safe Crash Shield (Customer, Rider & Admin).
 * Captures uncaught exceptions safely into SharedPreferences before forwarding to Android's
 * previous default handler so the next launch displays a clean 1-tap "Send Report to Admin" dialog.
 */
object QuickDeliveryCrashShield {
    private const val PREFS_NAME = "quick_delivery_crash_shield_v1"
    private const val KEY_PENDING_CRASH_JSON = "pending_crash_json"

    @Volatile
    private var installed = false

    @Volatile
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    private val _pendingCrashReport = MutableStateFlow<CrashReportRecord?>(null)
    val pendingCrashReport: StateFlow<CrashReportRecord?> = _pendingCrashReport.asStateFlow()

    fun install(context: Context, appRole: String, appVersion: String = "6.7.2") {
        val appCtx = context.applicationContext
        loadPendingReport(appCtx)
        if (installed) return
        installed = true
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        previousHandler = prev
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                persistCrashRecord(
                    context = appCtx,
                    appRole = appRole,
                    appVersion = appVersion,
                    threadName = thread.name.orEmpty(),
                    throwable = throwable
                )
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

    fun loadPendingReport(context: Context): CrashReportRecord? {
        val raw = runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PENDING_CRASH_JSON, null)
        }.getOrNull()
        val parsed = raw?.takeIf { it.isNotBlank() }?.let { parseCrashRecord(it) }
        _pendingCrashReport.value = parsed
        return parsed
    }

    fun recordCrashForTesting(
        context: Context,
        appRole: String,
        exceptionSummary: String,
        stackTracePreview: String = "",
        appVersion: String = "6.7.2"
    ): CrashReportRecord {
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US).format(Date(now))
        val record = CrashReportRecord(
            crashId = "CR-${(now % 100000).toString().padStart(5, '0')}",
            appRole = appRole.uppercase(),
            appVersion = appVersion,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android Device" },
            androidSdk = Build.VERSION.SDK_INT,
            timestampEpochMs = now,
            timestampLabel = label,
            threadName = Thread.currentThread().name.orEmpty().ifBlank { "main" },
            exceptionSummary = exceptionSummary.take(240),
            stackTracePreview = stackTracePreview.take(1800)
        )
        saveRecordJson(context.applicationContext, record)
        _pendingCrashReport.value = record
        return record
    }

    fun clearPendingCrashReport(context: Context) {
        _pendingCrashReport.value = null
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PENDING_CRASH_JSON)
                .apply()
        }
    }

    fun buildShareableCrashSummary(record: CrashReportRecord): String = buildString {
        appendLine("Quick Delivery Error Report (${record.crashId})")
        appendLine("App Role: ${record.appRole} (v${record.appVersion})")
        appendLine("Time: ${record.timestampLabel}")
        appendLine("Device: ${record.deviceModel} (Android API ${record.androidSdk})")
        appendLine("Thread: ${record.threadName}")
        appendLine("Issue: ${record.exceptionSummary}")
        if (record.stackTracePreview.isNotBlank()) {
            appendLine()
            appendLine("Trace:")
            appendLine(record.stackTracePreview.take(900))
        }
    }.trim()

    private fun persistCrashRecord(
        context: Context,
        appRole: String,
        appVersion: String,
        threadName: String,
        throwable: Throwable
    ) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US).format(Date(now))
        val summary = "${throwable.javaClass.simpleName}: ${throwable.message ?: "Unexpected error"}"
        val record = CrashReportRecord(
            crashId = "CR-${(now % 100000).toString().padStart(5, '0')}",
            appRole = appRole.uppercase(),
            appVersion = appVersion,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android Device" },
            androidSdk = Build.VERSION.SDK_INT,
            timestampEpochMs = now,
            timestampLabel = label,
            threadName = threadName.ifBlank { "main" },
            exceptionSummary = summary.take(240),
            stackTracePreview = sw.toString().take(1800)
        )
        saveRecordJson(context, record)
    }

    private fun saveRecordJson(context: Context, record: CrashReportRecord) {
        val json = JSONObject().apply {
            put("crashId", record.crashId)
            put("appRole", record.appRole)
            put("appVersion", record.appVersion)
            put("deviceModel", record.deviceModel)
            put("androidSdk", record.androidSdk)
            put("timestampEpochMs", record.timestampEpochMs)
            put("timestampLabel", record.timestampLabel)
            put("threadName", record.threadName)
            put("exceptionSummary", record.exceptionSummary)
            put("stackTracePreview", record.stackTracePreview)
        }
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PENDING_CRASH_JSON, json.toString())
                .commit() // Synchronous commit so crash data survives process termination
        }
    }

    private fun parseCrashRecord(raw: String): CrashReportRecord? = runCatching {
        val obj = JSONObject(raw)
        CrashReportRecord(
            crashId = obj.optString("crashId", "CR-00000"),
            appRole = obj.optString("appRole", "APP"),
            appVersion = obj.optString("appVersion", "6.7.2"),
            deviceModel = obj.optString("deviceModel", "Android Device"),
            androidSdk = obj.optInt("androidSdk", Build.VERSION.SDK_INT),
            timestampEpochMs = obj.optLong("timestampEpochMs", 0L),
            timestampLabel = obj.optString("timestampLabel", ""),
            threadName = obj.optString("threadName", "main"),
            exceptionSummary = obj.optString("exceptionSummary", "Unexpected error"),
            stackTracePreview = obj.optString("stackTracePreview", "")
        )
    }.getOrNull()
}

/**
 * Live Internet Connectivity Monitor (API 24+ & Robolectric safe).
 * Tracks offline/online transitions cleanly and automatically triggers an immediate
 * cloud sync when connectivity returns.
 */
object LiveNetworkStatusMonitor {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _justReconnected = MutableStateFlow(false)
    val justReconnected: StateFlow<Boolean> = _justReconnected.asStateFlow()

    @Volatile
    private var monitoringStarted = false

    @Volatile
    private var wasOfflinePreviously = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val clearReconnectBannerRunnable = Runnable {
        _justReconnected.value = false
    }

    fun startMonitoring(context: Context) {
        val appCtx = context.applicationContext
        val currentlyConnected = queryCurrentNetworkOnline(appCtx)
        _isOnline.value = currentlyConnected
        if (!currentlyConnected) {
            wasOfflinePreviously = true
        }
        if (monitoringStarted) return
        monitoringStarted = true

        runCatching {
            val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val recoveredFromOffline = wasOfflinePreviously || !_isOnline.value
                    _isOnline.value = true
                    wasOfflinePreviously = false
                    if (recoveredFromOffline) {
                        _justReconnected.value = true
                        mainHandler.removeCallbacks(clearReconnectBannerRunnable)
                        mainHandler.postDelayed(clearReconnectBannerRunnable, 4_200L)
                        runCatching {
                            MultiPhoneLiveSyncManager.triggerImmediateSync(appCtx)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    val stillConnected = queryCurrentNetworkOnline(appCtx)
                    if (!stillConnected) {
                        wasOfflinePreviously = true
                        _isOnline.value = false
                        _justReconnected.value = false
                    }
                }
            }
            cm.registerNetworkCallback(request, callback)
        }
    }

    fun queryCurrentNetworkOnline(context: Context): Boolean = runCatching {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)
}

/**
 * Play-Policy Compliant Rider Pocket & Screen-Off Order Protection Manager.
 * - Shows a persistent, low-noise ongoing "On Duty — Waiting for Orders" notification while Online.
 * - Opens standard Android Battery Optimization Settings (zero prohibited manifest permissions).
 * - Runs a 2.5-second live Order Alert Ringtone + Vibration test so riders verify audio before shift.
 */
object RiderOnDutyGuardManager {
    private const val CHANNEL_ID_ONDUTY = "quick_delivery_rider_onduty_v1"
    private const val NOTIFICATION_ID_ONDUTY = 7401

    private val mainHandler = Handler(Looper.getMainLooper())

    fun updateOnDutyNotification(
        context: Context,
        isOnline: Boolean,
        riderName: String
    ) {
        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!isOnline) {
            runCatching { nm.cancel(NOTIFICATION_ID_ONDUTY) }
            return
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID_ONDUTY,
                    "Rider On-Duty Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps Quick Delivery Partner ready for incoming parcel orders while On Duty."
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
            val launchIntent = appCtx.packageManager.getLaunchIntentForPackage(appCtx.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    appCtx,
                    NOTIFICATION_ID_ONDUTY,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val cleanName = riderName.trim().ifBlank { "Partner" }
            val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID_ONDUTY)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("🟢 On Duty • Ready for New Orders")
                .setContentText("$cleanName is online. Loud ring & vibration alerts active.")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .apply { pendingIntent?.let { setContentIntent(it) } }
                .build()
            nm.notify(NOTIFICATION_ID_ONDUTY, notification)
        }
    }

    /**
     * Opens standard Android Battery Optimization settings (Play-Policy compliant — does NOT use
     * prohibited REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission).
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val intents = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
        for (intent in intents) {
            val launched = runCatching {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return true
        }
        return false
    }

    /**
     * Triggers a short 2.5-second test ringtone + vibration using OrderAlertSoundManager so riders
     * can confirm their phone is audible inside a pocket.
     */
    fun triggerTestOrderRingAlert(context: Context): String {
        val appCtx = context.applicationContext
        val caps = OrderAlertSoundManager.inspectPlatformCapabilities(appCtx)
        runCatching {
            OrderAlertSoundManager.startLoudOrderAlert(appCtx, "TEST-RING-${System.currentTimeMillis()}")
            mainHandler.postDelayed({
                runCatching { OrderAlertSoundManager.stopOrderAlert() }
            }, 2_500L)
        }
        return if (caps.isAlarmAudible()) {
            "🔊 Test Order Ringtone & Vibration played! Phone is ready for pocket alerts."
        } else {
            "⚠️ Alarm volume is muted or Do Not Disturb is ON — please increase Alarm volume."
        }
    }
}

/**
 * 1-Tap Digital Delivery Bill / WhatsApp Receipt Share Helper (₹0 free).
 */
object DigitalReceiptShareHelper {
    fun buildFormattedDigitalReceiptText(
        order: CustomerOrder,
        tipAmountInr: Int = 0
    ): String {
        val dateLabel = order.createdAt.ifBlank {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        }
        val paymentLabel = when (order.paymentMethod) {
            CustomerPaymentMethod.CASH_ON_DELIVERY -> "Cash on Delivery"
            else -> order.paymentMethod.name.replace('_', ' ')
        }
        val paymentStatus = order.paymentStatus.name.replace('_', ' ')
        val riderLine = order.assignedRiderName?.takeIf { it.isNotBlank() } ?: "Quick Delivery Partner"
        val receiverLine = order.dropAddress.contactName.takeIf { it.isNotBlank() }
            ?: order.customerName.ifBlank { "Receiver" }
        val distanceText = if (order.distanceKm > 0.0) {
            String.format(Locale.US, "%.1f km", order.distanceKm)
        } else {
            "City Route"
        }
        val totalNumeric = order.totalAmount.replace("₹", "").replace(",", "").trim().toIntOrNull() ?: 0
        val grandTotalText = if (tipAmountInr > 0 && totalNumeric > 0) {
            "₹${totalNumeric + tipAmountInr} (includes ₹$tipAmountInr Rider Tip)"
        } else {
            order.totalAmount.ifBlank { "₹0" }
        }

        return buildString {
            appendLine("🧾 *QUICK DELIVERY — DIGITAL BILL*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📦 Order ID: *${order.orderId}*")
            appendLine("📅 Date: $dateLabel")
            appendLine("✅ Status: ${order.orderStatus.name.replace('_', ' ')}")
            appendLine()
            appendLine("📍 *Pickup:* ${order.pickupAddress.fullAddress}")
            appendLine("🏁 *Drop:* ${order.dropAddress.fullAddress}")
            appendLine("👤 *Receiver:* $receiverLine")
            appendLine("🛵 *Vehicle:* ${order.vehicleType.title} ($distanceText)")
            appendLine("🙋 *Delivery Partner:* $riderLine")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 *Total Amount:* $grandTotalText")
            appendLine("💳 *Payment:* $paymentLabel ($paymentStatus)")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Thank you for choosing Quick Delivery!")
            append("Fast • Safe • Door-to-Door City Delivery")
        }
    }

    fun shareDigitalReceiptOnWhatsApp(
        context: Context,
        order: CustomerOrder,
        tipAmountInr: Int = 0
    ): Boolean {
        val billText = buildFormattedDigitalReceiptText(order, tipAmountInr)
        val encoded = runCatching { URLEncoder.encode(billText, "UTF-8") }.getOrDefault("")
        val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in whatsappPackages) {
            val directIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, billText)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launched = runCatching {
                context.startActivity(directIntent)
                true
            }.getOrDefault(false)
            if (launched) return true
        }
        // Fallback: wa.me link or standard Android Share Chooser
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Quick Delivery Bill — ${order.orderId}")
                putExtra(Intent.EXTRA_TEXT, billText)
            },
            "Share Digital Delivery Bill"
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(chooser)
            true
        }.getOrElse {
            if (encoded.isNotBlank()) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$encoded"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    true
                }.getOrDefault(false)
            } else {
                false
            }
        }
    }
}
