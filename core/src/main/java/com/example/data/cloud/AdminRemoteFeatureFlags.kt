package com.example.data.cloud

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.data.model.CustomerOrder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Admin-controlled Remote Feature Switches ("Master Switches") synced in real time across
 * Admin Console -> Live Sync Server -> Customer ("Quick Delivery") & Rider ("Partner Quick Delivery").
 *
 * Future-rollout features default to OFF so Admin can activate them anytime with 1 tap.
 */
data class AdminRemoteFeatureFlags(
    val scheduledDeliveryEnabled: Boolean = false,
    val multiDropEnabled: Boolean = false,
    val surgePricingEnabled: Boolean = false,
    val surgeMultiplier: Double = 1.25,
    val parcelInsuranceEnabled: Boolean = false,
    val insuranceFeeInr: Int = 15,
    val referralWalletEnabled: Boolean = false,
    val referralBonusInr: Int = 50,
    val codEnabled: Boolean = true,
    val maxCodLimitInr: Int = 2000,
    val platformCommissionPercent: Int = 20,
    val maintenanceBannerEnabled: Boolean = false,
    val maintenanceMessage: String = "",
    val serviceablePinCodesCsv: String = "",
    val riderTargetMissionEnabled: Boolean = false,
    val riderMissionTargetOrders: Int = 5,
    val riderMissionBonusInr: Int = 150,
    val adminBroadcastMessage: String = "",
    val adminBroadcastTarget: String = "ALL", // ALL, CUSTOMER, RIDER
    val adminBroadcastEpochMs: Long = 0L,
    val adminSecurityPin: String = "",
    val waitTimeAndCancelRuleEnabled: Boolean = false,
    val freeWaitMinutes: Int = 5,
    val waitFeePerMinuteInr: Int = 5,
    val cancellationPenaltyInr: Int = 30,
    val liveGatewayModeEnabled: Boolean = false,
    val gatewayProvider: String = "RAZORPAY",
    val peakAutoSurgeEnabled: Boolean = false,
    val peakTimeSlotsCsv: String = "09-11,18-21",
    val peakSurgeMultiplier: Double = 1.35,
    val helperAddonEnabled: Boolean = true,
    val helperFeeInr: Int = 60,
    val sameRouteBatchBonusInr: Int = 25,
    val activeSurgeZoneCsv: String = "",
    val updatedAtEpochMs: Long = 0L
) {
    val riderSharePercent: Int
        get() = (100 - platformCommissionPercent.coerceIn(0, 40)).coerceIn(60, 100)

    fun isCurrentHourInPeakWindow(
        hourOfDay24: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): Boolean {
        if (!peakAutoSurgeEnabled) return false
        val slots = peakTimeSlotsCsv.split(',').map { it.trim() }.filter { it.contains('-') }
        return slots.any { slot ->
            val parts = slot.split('-')
            val startH = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return@any false
            val endH = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return@any false
            hourOfDay24 in startH until endH
        }
    }

    fun effectiveSurgeMultiplier(): Double {
        val manual = if (surgePricingEnabled) surgeMultiplier.coerceIn(1.0, 3.0) else 1.0
        val peak = if (isCurrentHourInPeakWindow()) peakSurgeMultiplier.coerceIn(1.0, 3.0) else 1.0
        return maxOf(manual, peak)
    }

    fun isPinCodeAllowed(pinCode: String): Boolean {
        val raw = serviceablePinCodesCsv.trim()
        if (raw.isEmpty()) return true
        val allowed = raw.split(',', ' ', '\n', ';')
            .map { it.filter(Char::isDigit).trim() }
            .filter { it.length == 6 }
            .toSet()
        if (allowed.isEmpty()) return true
        val cleanPin = pinCode.filter(Char::isDigit).trim()
        return cleanPin in allowed
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("scheduledDeliveryEnabled", scheduledDeliveryEnabled)
        put("multiDropEnabled", multiDropEnabled)
        put("surgePricingEnabled", surgePricingEnabled)
        put("surgeMultiplier", surgeMultiplier)
        put("parcelInsuranceEnabled", parcelInsuranceEnabled)
        put("insuranceFeeInr", insuranceFeeInr)
        put("referralWalletEnabled", referralWalletEnabled)
        put("referralBonusInr", referralBonusInr)
        put("codEnabled", codEnabled)
        put("maxCodLimitInr", maxCodLimitInr)
        put("platformCommissionPercent", platformCommissionPercent)
        put("maintenanceBannerEnabled", maintenanceBannerEnabled)
        put("maintenanceMessage", maintenanceMessage)
        put("serviceablePinCodesCsv", serviceablePinCodesCsv)
        put("riderTargetMissionEnabled", riderTargetMissionEnabled)
        put("riderMissionTargetOrders", riderMissionTargetOrders)
        put("riderMissionBonusInr", riderMissionBonusInr)
        put("adminBroadcastMessage", adminBroadcastMessage)
        put("adminBroadcastTarget", adminBroadcastTarget)
        put("adminBroadcastEpochMs", adminBroadcastEpochMs)
        put("adminSecurityPin", adminSecurityPin)
        put("waitTimeAndCancelRuleEnabled", waitTimeAndCancelRuleEnabled)
        put("freeWaitMinutes", freeWaitMinutes)
        put("waitFeePerMinuteInr", waitFeePerMinuteInr)
        put("cancellationPenaltyInr", cancellationPenaltyInr)
        put("liveGatewayModeEnabled", liveGatewayModeEnabled)
        put("gatewayProvider", gatewayProvider)
        put("peakAutoSurgeEnabled", peakAutoSurgeEnabled)
        put("peakTimeSlotsCsv", peakTimeSlotsCsv)
        put("peakSurgeMultiplier", peakSurgeMultiplier)
        put("helperAddonEnabled", helperAddonEnabled)
        put("helperFeeInr", helperFeeInr)
        put("sameRouteBatchBonusInr", sameRouteBatchBonusInr)
        put("activeSurgeZoneCsv", activeSurgeZoneCsv)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        fun fromJson(obj: JSONObject): AdminRemoteFeatureFlags = AdminRemoteFeatureFlags(
            scheduledDeliveryEnabled = obj.optBoolean("scheduledDeliveryEnabled", false),
            multiDropEnabled = obj.optBoolean("multiDropEnabled", false),
            surgePricingEnabled = obj.optBoolean("surgePricingEnabled", false),
            surgeMultiplier = obj.optDouble("surgeMultiplier", 1.25).coerceIn(1.0, 3.0),
            parcelInsuranceEnabled = obj.optBoolean("parcelInsuranceEnabled", false),
            insuranceFeeInr = obj.optInt("insuranceFeeInr", 15).coerceIn(5, 200),
            referralWalletEnabled = obj.optBoolean("referralWalletEnabled", false),
            referralBonusInr = obj.optInt("referralBonusInr", 50).coerceIn(10, 500),
            codEnabled = obj.optBoolean("codEnabled", true),
            maxCodLimitInr = obj.optInt("maxCodLimitInr", 2000).coerceIn(100, 50000),
            platformCommissionPercent = obj.optInt("platformCommissionPercent", 20).coerceIn(0, 40),
            maintenanceBannerEnabled = obj.optBoolean("maintenanceBannerEnabled", false),
            maintenanceMessage = obj.optString("maintenanceMessage", ""),
            serviceablePinCodesCsv = obj.optString("serviceablePinCodesCsv", ""),
            riderTargetMissionEnabled = obj.optBoolean("riderTargetMissionEnabled", false),
            riderMissionTargetOrders = obj.optInt("riderMissionTargetOrders", 5).coerceIn(1, 50),
            riderMissionBonusInr = obj.optInt("riderMissionBonusInr", 150).coerceIn(20, 2000),
            adminBroadcastMessage = obj.optString("adminBroadcastMessage", ""),
            adminBroadcastTarget = obj.optString("adminBroadcastTarget", "ALL").uppercase().ifBlank { "ALL" },
            adminBroadcastEpochMs = obj.optLong("adminBroadcastEpochMs", 0L),
            adminSecurityPin = obj.optString("adminSecurityPin", "").filter(Char::isDigit).take(4),
            waitTimeAndCancelRuleEnabled = obj.optBoolean("waitTimeAndCancelRuleEnabled", false),
            freeWaitMinutes = obj.optInt("freeWaitMinutes", 5).coerceIn(1, 20),
            waitFeePerMinuteInr = obj.optInt("waitFeePerMinuteInr", 5).coerceIn(1, 50),
            cancellationPenaltyInr = obj.optInt("cancellationPenaltyInr", 30).coerceIn(0, 200),
            liveGatewayModeEnabled = obj.optBoolean("liveGatewayModeEnabled", false),
            gatewayProvider = obj.optString("gatewayProvider", "RAZORPAY").uppercase().ifBlank { "RAZORPAY" },
            peakAutoSurgeEnabled = obj.optBoolean("peakAutoSurgeEnabled", false),
            peakTimeSlotsCsv = obj.optString("peakTimeSlotsCsv", "09-11,18-21"),
            peakSurgeMultiplier = obj.optDouble("peakSurgeMultiplier", 1.35).coerceIn(1.0, 3.0),
            helperAddonEnabled = obj.optBoolean("helperAddonEnabled", true),
            helperFeeInr = obj.optInt("helperFeeInr", 60).coerceIn(20, 300),
            sameRouteBatchBonusInr = obj.optInt("sameRouteBatchBonusInr", 25).coerceIn(10, 200),
            activeSurgeZoneCsv = obj.optString("activeSurgeZoneCsv", ""),
            updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
        )
    }
}

/**
 * Rider-only Voice Order Alert Manager & Smart Filter Preferences.
 * Strictly guarded so Customer and Admin apps NEVER trigger TTS voice announcements.
 */
object RiderSmartToolsPrefs {
    private const val PREFS_NAME = "quick_delivery_rider_smart_tools_v1"
    private const val KEY_VOICE_ALERT = "voice_alert_enabled"
    private const val KEY_MIN_PAYOUT = "min_payout_filter_inr"
    private const val KEY_AREA_KEYWORD = "preferred_area_keyword"

    private var loaded = false

    private val _voiceAlertEnabled = MutableStateFlow(true)
    val voiceAlertEnabled: StateFlow<Boolean> = _voiceAlertEnabled.asStateFlow()

    private val _minPayoutFilterInr = MutableStateFlow(0)
    val minPayoutFilterInr: StateFlow<Int> = _minPayoutFilterInr.asStateFlow()

    private val _preferredAreaKeyword = MutableStateFlow("")
    val preferredAreaKeyword: StateFlow<String> = _preferredAreaKeyword.asStateFlow()

    private val spokenOrderIds = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var ttsEngine: TextToSpeech? = null
    @Volatile
    private var ttsReady = false

    private val isRobolectricTest: Boolean by lazy {
        runCatching {
            Class.forName("org.robolectric.Robolectric")
            true
        }.getOrDefault(false)
    }

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _voiceAlertEnabled.value = prefs.getBoolean(KEY_VOICE_ALERT, true)
            _minPayoutFilterInr.value = prefs.getInt(KEY_MIN_PAYOUT, 0)
            _preferredAreaKeyword.value = prefs.getString(KEY_AREA_KEYWORD, "").orEmpty()
        }
    }

    fun setVoiceAlertEnabled(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        _voiceAlertEnabled.value = enabled
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_VOICE_ALERT, enabled).apply()
        }
        if (!enabled) {
            runCatching { ttsEngine?.stop() }
        }
    }

    fun setMinPayoutFilter(context: Context, minInr: Int) {
        ensureLoaded(context)
        _minPayoutFilterInr.value = minInr
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MIN_PAYOUT, minInr).apply()
        }
    }

    fun setPreferredAreaKeyword(context: Context, keyword: String) {
        ensureLoaded(context)
        _preferredAreaKeyword.value = keyword
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_AREA_KEYWORD, keyword).apply()
        }
    }

    fun speakNewOrderIfRider(context: Context, order: CustomerOrder) {
        if (isRobolectricTest) return
        ensureLoaded(context)
        if (!_voiceAlertEnabled.value) return
        val activeRole = MultiPhoneLiveSyncManager.lockedPhoneRole.value
        if (activeRole == "CUSTOMER" || activeRole == "ADMIN") return
        if (!spokenOrderIds.add(order.orderId)) return

        val pickupArea = order.pickupAddress.fullAddress.substringBefore(",").take(32).ifBlank { "Pickup Point" }
        val earningText = order.riderEarning.replace("₹", "").trim().ifBlank { "0" }
        val announcement = "Naya order aaya hai! Pickup $pickupArea, earning $earningText rupees."

        runCatching {
            val appCtx = context.applicationContext
            if (ttsEngine == null) {
                ttsEngine = TextToSpeech(appCtx) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ttsReady = true
                        runCatching {
                            ttsEngine?.language = Locale("en", "IN")
                            ttsEngine?.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "qd_${order.orderId}")
                        }
                    }
                }
            } else if (ttsReady) {
                ttsEngine?.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "qd_${order.orderId}")
            }
        }
    }

    fun markOrderSpokenReset(orderId: String) {
        spokenOrderIds.remove(orderId)
    }
}
