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
    val scheduledDeliveryEnabled: Boolean = true,
    val multiDropEnabled: Boolean = true,
    val surgePricingEnabled: Boolean = false,
    val surgeMultiplier: Double = 1.25,
    val parcelInsuranceEnabled: Boolean = true,
    val insuranceFeeInr: Int = 15,
    val referralWalletEnabled: Boolean = false,
    val referralBonusInr: Int = 50,
    val promoCodeEnabled: Boolean = false,
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
    val waitTimeAndCancelRuleEnabled: Boolean = true,
    val freeWaitMinutes: Int = 5,
    val waitFeePerMinuteInr: Int = 5,
    val cancellationPenaltyInr: Int = 30,
    val liveGatewayModeEnabled: Boolean = false,
    val gatewayProvider: String = "RAZORPAY",
    val peakAutoSurgeEnabled: Boolean = true,
    val peakTimeSlotsCsv: String = "09-11,18-21",
    val peakSurgeMultiplier: Double = 1.35,
    val helperAddonEnabled: Boolean = true,
    val helperFeeInr: Int = 60,
    val sameRouteBatchBonusInr: Int = 25,
    val activeSurgeZoneCsv: String = "",
    val b2bPostpaidEnabled: Boolean = false,
    val b2bDefaultCreditLimitInr: Int = 25000,
    val b2bBillingCycleDays: Int = 30,
    val razorpayRouteSplitEnabled: Boolean = false,
    val autoKycApiEnabled: Boolean = false,
    // Schema v4 — 1-Click App Update, Master Sync, Live Rate Card, Future Switches & Fraud Blocklist
    val latestAppVersionName: String = "",
    val latestAppVersionCode: Int = 0,
    val forceUpdateRequired: Boolean = false,
    val updateNotes: String = "",
    val customerUpdateUrl: String = QuickDeliveryBrandLinks.CUSTOMER_APK_URL,
    val riderUpdateUrl: String = QuickDeliveryBrandLinks.RIDER_APK_URL,
    val emergencyBookingsPaused: Boolean = false,
    val emergencyPauseReason: String = "New bookings are temporarily paused by Operations. Please try again shortly.",
    val operatingHoursEnabled: Boolean = false,
    val operatingStartHour: Int = 7,
    val operatingEndHour: Int = 23,
    val nightSurchargeEnabled: Boolean = false,
    val nightStartHour: Int = 23,
    val nightEndHour: Int = 6,
    val nightSurchargeMultiplier: Double = 1.20,
    val remoteRateCardOverrideEnabled: Boolean = false,
    val bikeEnabled: Boolean = true,
    val scooterEnabled: Boolean = true,
    val autoEnabled: Boolean = false,
    val bikeBaseFareInr: Double = 35.0,
    val bikeRatePerKmInr: Double = 10.0,
    val scooterBaseFareInr: Double = 45.0,
    val scooterRatePerKmInr: Double = 12.0,
    val autoBaseFareInr: Double = 85.0,
    val autoRatePerKmInr: Double = 18.0,
    val smsOtpGatewayEnabled: Boolean = false,
    val blockedCustomerPhonesCsv: String = "",
    val masterSyncRevision: Int = 1,
    val flagsSchemaVersion: Int = 5,
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

    fun isCurrentHourInNightWindow(
        hourOfDay24: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): Boolean {
        if (!nightSurchargeEnabled) return false
        val startH = nightStartHour.coerceIn(0, 23)
        val endH = nightEndHour.coerceIn(0, 23)
        return if (startH > endH) {
            hourOfDay24 >= startH || hourOfDay24 < endH
        } else {
            hourOfDay24 in startH until endH
        }
    }

    fun isWithinOperatingHours(
        hourOfDay24: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): Boolean {
        if (!operatingHoursEnabled) return true
        val startH = operatingStartHour.coerceIn(0, 23)
        val endH = operatingEndHour.coerceIn(1, 24)
        return if (startH < endH) {
            hourOfDay24 in startH until endH
        } else {
            hourOfDay24 >= startH || hourOfDay24 < endH
        }
    }

    fun operatingHoursLabel(): String {
        fun fmt(h: Int): String {
            val normalized = ((h % 24) + 24) % 24
            return when {
                normalized == 0 -> "12 AM"
                normalized < 12 -> "$normalized AM"
                normalized == 12 -> "12 PM"
                else -> "${normalized - 12} PM"
            }
        }
        return "${fmt(operatingStartHour)} – ${fmt(operatingEndHour)}"
    }

    fun isVehicleEnabled(vehicle: com.example.data.model.VehicleType): Boolean = when (vehicle) {
        com.example.data.model.VehicleType.BIKE -> bikeEnabled
        com.example.data.model.VehicleType.SCOOTER -> scooterEnabled
        com.example.data.model.VehicleType.AUTO -> autoEnabled
        else -> true
    }

    fun getBlockedCustomerPhonesList(): List<String> = blockedCustomerPhonesCsv
        .split(',', ' ', '\n', ';')
        .map { it.filter(Char::isDigit).takeLast(10) }
        .filter { it.length == 10 }
        .distinct()

    fun isCustomerPhoneBlocked(phone: String): Boolean {
        val clean = phone.filter(Char::isDigit).takeLast(10)
        if (clean.length != 10) return false
        return clean in getBlockedCustomerPhonesList()
    }

    fun isUpdateAvailableForApp(currentVersionName: String, currentVersionCode: Int): Boolean {
        if (latestAppVersionCode > 0 && latestAppVersionCode > currentVersionCode) return true
        val target = latestAppVersionName.trim().removePrefix("v").removePrefix("V")
        if (target.isBlank()) return false
        val current = currentVersionName.trim().removePrefix("v").removePrefix("V")
        return compareSemver(target, current) > 0
    }

    fun effectiveSurgeMultiplier(): Double {
        val manual = if (surgePricingEnabled) surgeMultiplier.coerceIn(1.0, 3.0) else 1.0
        val peak = if (isCurrentHourInPeakWindow()) peakSurgeMultiplier.coerceIn(1.0, 3.0) else 1.0
        val night = if (isCurrentHourInNightWindow()) nightSurchargeMultiplier.coerceIn(1.0, 3.0) else 1.0
        return maxOf(manual, peak, night)
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
        put("promoCodeEnabled", promoCodeEnabled)
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
        put("b2bPostpaidEnabled", b2bPostpaidEnabled)
        put("b2bDefaultCreditLimitInr", b2bDefaultCreditLimitInr)
        put("b2bBillingCycleDays", b2bBillingCycleDays)
        put("razorpayRouteSplitEnabled", razorpayRouteSplitEnabled)
        put("autoKycApiEnabled", autoKycApiEnabled)
        put("latestAppVersionName", latestAppVersionName)
        put("latestAppVersionCode", latestAppVersionCode)
        put("forceUpdateRequired", forceUpdateRequired)
        put("updateNotes", updateNotes)
        put("customerUpdateUrl", customerUpdateUrl)
        put("riderUpdateUrl", riderUpdateUrl)
        put("emergencyBookingsPaused", emergencyBookingsPaused)
        put("emergencyPauseReason", emergencyPauseReason)
        put("operatingHoursEnabled", operatingHoursEnabled)
        put("operatingStartHour", operatingStartHour)
        put("operatingEndHour", operatingEndHour)
        put("nightSurchargeEnabled", nightSurchargeEnabled)
        put("nightStartHour", nightStartHour)
        put("nightEndHour", nightEndHour)
        put("nightSurchargeMultiplier", nightSurchargeMultiplier)
        put("remoteRateCardOverrideEnabled", remoteRateCardOverrideEnabled)
        put("bikeEnabled", bikeEnabled)
        put("scooterEnabled", scooterEnabled)
        put("autoEnabled", autoEnabled)
        put("bikeBaseFareInr", bikeBaseFareInr)
        put("bikeRatePerKmInr", bikeRatePerKmInr)
        put("scooterBaseFareInr", scooterBaseFareInr)
        put("scooterRatePerKmInr", scooterRatePerKmInr)
        put("autoBaseFareInr", autoBaseFareInr)
        put("autoRatePerKmInr", autoRatePerKmInr)
        put("smsOtpGatewayEnabled", smsOtpGatewayEnabled)
        put("blockedCustomerPhonesCsv", blockedCustomerPhonesCsv)
        put("masterSyncRevision", masterSyncRevision)
        put("flagsSchemaVersion", flagsSchemaVersion)
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        const val CURRENT_FLAGS_SCHEMA_VERSION = 5

        fun compareSemver(a: String, b: String): Int {
            val pa = a.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            val pb = b.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            val maxLen = maxOf(pa.size, pb.size)
            for (i in 0 until maxLen) {
                val va = pa.getOrElse(i) { 0 }
                val vb = pb.getOrElse(i) { 0 }
                if (va != vb) return va.compareTo(vb)
            }
            return 0
        }

        fun fromJson(obj: JSONObject): AdminRemoteFeatureFlags {
            val schemaVer = obj.optInt("flagsSchemaVersion", 1)
            val isLegacyV1Schema = schemaVer < 2
            val isLegacyV2Schema = schemaVer < 3
            val isLegacyV4Schema = schemaVer < 5
            val bikeOn = obj.optBoolean("bikeEnabled", true)
            val scooterOn = obj.optBoolean("scooterEnabled", true)
            val autoOn = if (isLegacyV4Schema) false else obj.optBoolean("autoEnabled", false)
            val safeBikeOn = if (!bikeOn && !scooterOn && !autoOn) true else bikeOn
            return AdminRemoteFeatureFlags(
                scheduledDeliveryEnabled = if (isLegacyV1Schema) true else obj.optBoolean("scheduledDeliveryEnabled", true),
                multiDropEnabled = if (isLegacyV1Schema) true else obj.optBoolean("multiDropEnabled", true),
                surgePricingEnabled = obj.optBoolean("surgePricingEnabled", false),
                surgeMultiplier = obj.optDouble("surgeMultiplier", 1.25).coerceIn(1.0, 3.0),
                parcelInsuranceEnabled = if (isLegacyV1Schema) true else obj.optBoolean("parcelInsuranceEnabled", true),
                insuranceFeeInr = obj.optInt("insuranceFeeInr", 15).coerceIn(5, 200),
                referralWalletEnabled = if (isLegacyV4Schema) false else obj.optBoolean("referralWalletEnabled", false),
                referralBonusInr = obj.optInt("referralBonusInr", 50).coerceIn(10, 500),
                promoCodeEnabled = if (isLegacyV4Schema) false else obj.optBoolean("promoCodeEnabled", false),
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
                waitTimeAndCancelRuleEnabled = if (isLegacyV1Schema) true else obj.optBoolean("waitTimeAndCancelRuleEnabled", true),
                freeWaitMinutes = obj.optInt("freeWaitMinutes", 5).coerceIn(1, 20),
                waitFeePerMinuteInr = obj.optInt("waitFeePerMinuteInr", 5).coerceIn(1, 50),
                cancellationPenaltyInr = obj.optInt("cancellationPenaltyInr", 30).coerceIn(0, 200),
                liveGatewayModeEnabled = obj.optBoolean("liveGatewayModeEnabled", false),
                gatewayProvider = obj.optString("gatewayProvider", "RAZORPAY").uppercase().ifBlank { "RAZORPAY" },
                peakAutoSurgeEnabled = if (isLegacyV1Schema) true else obj.optBoolean("peakAutoSurgeEnabled", true),
                peakTimeSlotsCsv = obj.optString("peakTimeSlotsCsv", "09-11,18-21"),
                peakSurgeMultiplier = obj.optDouble("peakSurgeMultiplier", 1.35).coerceIn(1.0, 3.0),
                helperAddonEnabled = obj.optBoolean("helperAddonEnabled", true),
                helperFeeInr = obj.optInt("helperFeeInr", 60).coerceIn(20, 300),
                sameRouteBatchBonusInr = obj.optInt("sameRouteBatchBonusInr", 25).coerceIn(10, 200),
                activeSurgeZoneCsv = obj.optString("activeSurgeZoneCsv", ""),
                b2bPostpaidEnabled = if (isLegacyV2Schema) false else obj.optBoolean("b2bPostpaidEnabled", false),
                b2bDefaultCreditLimitInr = obj.optInt("b2bDefaultCreditLimitInr", 25000).coerceIn(5000, 500000),
                b2bBillingCycleDays = obj.optInt("b2bBillingCycleDays", 30).coerceIn(7, 45),
                razorpayRouteSplitEnabled = if (isLegacyV2Schema) false else obj.optBoolean("razorpayRouteSplitEnabled", false),
                autoKycApiEnabled = if (isLegacyV2Schema) false else obj.optBoolean("autoKycApiEnabled", false),
                latestAppVersionName = obj.optString("latestAppVersionName", "").trim().take(24),
                latestAppVersionCode = obj.optInt("latestAppVersionCode", 0).coerceAtLeast(0),
                forceUpdateRequired = obj.optBoolean("forceUpdateRequired", false),
                updateNotes = obj.optString("updateNotes", "").trim().take(240),
                customerUpdateUrl = obj.optString("customerUpdateUrl", QuickDeliveryBrandLinks.CUSTOMER_APK_URL)
                    .trim().ifBlank { QuickDeliveryBrandLinks.CUSTOMER_APK_URL },
                riderUpdateUrl = obj.optString("riderUpdateUrl", QuickDeliveryBrandLinks.RIDER_APK_URL)
                    .trim().ifBlank { QuickDeliveryBrandLinks.RIDER_APK_URL },
                emergencyBookingsPaused = obj.optBoolean("emergencyBookingsPaused", false),
                emergencyPauseReason = obj.optString(
                    "emergencyPauseReason",
                    "New bookings are temporarily paused by Operations. Please try again shortly."
                ).trim(),
                operatingHoursEnabled = obj.optBoolean("operatingHoursEnabled", false),
                operatingStartHour = obj.optInt("operatingStartHour", 7).coerceIn(0, 23),
                operatingEndHour = obj.optInt("operatingEndHour", 23).coerceIn(1, 24),
                nightSurchargeEnabled = obj.optBoolean("nightSurchargeEnabled", false),
                nightStartHour = obj.optInt("nightStartHour", 23).coerceIn(0, 23),
                nightEndHour = obj.optInt("nightEndHour", 6).coerceIn(0, 23),
                nightSurchargeMultiplier = obj.optDouble("nightSurchargeMultiplier", 1.20).coerceIn(1.0, 2.5),
                remoteRateCardOverrideEnabled = obj.optBoolean("remoteRateCardOverrideEnabled", false),
                bikeEnabled = safeBikeOn,
                scooterEnabled = scooterOn,
                autoEnabled = autoOn,
                bikeBaseFareInr = obj.optDouble("bikeBaseFareInr", 35.0).coerceIn(10.0, 2000.0),
                bikeRatePerKmInr = obj.optDouble("bikeRatePerKmInr", 10.0).coerceIn(2.0, 500.0),
                scooterBaseFareInr = obj.optDouble("scooterBaseFareInr", 45.0).coerceIn(10.0, 2000.0),
                scooterRatePerKmInr = obj.optDouble("scooterRatePerKmInr", 12.0).coerceIn(2.0, 500.0),
                autoBaseFareInr = obj.optDouble("autoBaseFareInr", 85.0).coerceIn(15.0, 3000.0),
                autoRatePerKmInr = obj.optDouble("autoRatePerKmInr", 18.0).coerceIn(3.0, 600.0),
                smsOtpGatewayEnabled = obj.optBoolean("smsOtpGatewayEnabled", false),
                blockedCustomerPhonesCsv = obj.optString("blockedCustomerPhonesCsv", "").trim(),
                masterSyncRevision = obj.optInt("masterSyncRevision", 1).coerceAtLeast(1),
                flagsSchemaVersion = CURRENT_FLAGS_SCHEMA_VERSION,
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
            )
        }
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
