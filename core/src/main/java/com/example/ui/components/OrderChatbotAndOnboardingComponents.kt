package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.cloud.AdminRemoteFeatureFlags
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentVerificationStatus
import com.example.data.model.RiderProfile
import com.example.security.QuickDeliverySecurityGuard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppLanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class OrderBotRole {
    CUSTOMER,
    RIDER
}

data class OrderBotChatMessage(
    val id: String,
    val isFromBot: Boolean,
    val text: String,
    val timestampLabel: String,
    val isRefusal: Boolean = false
)

data class OrderBotReply(
    val text: String,
    val acceptedAsOrderQuery: Boolean,
    val referencedOrderId: String? = null
)

/**
 * Strictly order-scoped assistant engine shared by Customer App & Rider Partner App.
 * Guarantees:
 * 1. Only responds to Quick Delivery order, tracking, OTP, fare/earning, payment/cancellation,
 *    proof/signature, and partner dispatch/payout/KYC-order questions.
 * 2. Refuses off-topic chitchat, trivia, coding, politics, jokes, and OTP/security bypass requests.
 * 3. Strictly enforces per-account isolation (never exposes another user's order or hidden admin secrets).
 */
object OrderAssistantBotEngine {

    private val SECURITY_BYPASS_KEYWORDS = listOf(
        "bypass otp", "skip otp", "fake order", "generate fake", "admin pin", "admin password",
        "other user", "other customer", "dusre ka order", "kisi aur ka", "hack", "sql injection",
        "steal", "secret key", "hmac", "without otp", "bina otp"
    )

    private val EXPLICIT_OFF_TOPIC_KEYWORDS = listOf(
        "joke", "chutkula", "shayari", "poem", "story", "kahani", "song", "gana", "movie",
        "cricket", "ipl", "football", "match score", "weather", "mausam", "recipe", "khana kaise",
        "prime minister", "president", "election", "politics", "stock market", "bitcoin", "crypto",
        "python", "javascript", "java code", "html code", "homework", "essay", "translate",
        "girlfriend", "boyfriend", "love letter", "astrology", "horoscope", "rashifal",
        "capital of", "who won", "gk question"
    )

    private val ORDER_DOMAIN_KEYWORDS = listOf(
        "order", "orders", "parcel", "delivery", "deliver", "pickup", "pick up", "drop",
        "track", "tracking", "status", "where", "kahan", "kidhar", "kab", "eta", "time", "delay",
        "otp", "code", "verify", "verification", "pin", "signature", "sign", "proof", "photo",
        "fare", "price", "rate", "cost", "charge", "kitna", "kiraya", "bhada", "paisa", "paise",
        "rupee", "gst", "tax", "fee", "discount", "promo", "coupon", "offer",
        "weight", "kg", "kilo", "heavy", "wajan", "vajan", "size",
        "bike", "scooter", "auto", "vehicle", "gadi",
        "rider", "partner", "driver", "raider", "assigned", "accept", "reject", "decline",
        "payment", "pay", "upi", "cod", "cash", "card", "netbanking", "paid", "unpaid",
        "cancel", "cancellation", "refund", "invoice", "receipt", "bill",
        "wallet", "earning", "earnings", "kamai", "commission", "payout", "withdraw", "cashout",
        "bank", "settlement", "due", "limit", "suspended", "kyc", "selfie", "online", "offline",
        "route", "address", "landmark", "pata", "pincode", "pin code", "km", "distance",
        "receiver", "sender", "support", "ticket", "insurance", "helper", "scheduled", "multi",
        "book", "booking", "bhejna", "bhejni", "qd-", "ord-", "ऑर्डर", "पार्सल", "डिलीवरी",
        "राइडर", "किराया", "कहाँ", "ओटीपी", "रद्द", "भुगतान", "कमाई", "बिल"
    )

    private val GREETING_KEYWORDS = setOf(
        "hi", "hii", "hello", "hey", "namaste", "namaskar", "good morning",
        "good afternoon", "good evening", "help", "madad", "start", "menu"
    )

    private val HINDI_HINT_KEYWORDS = listOf(
        "mera", "meri", "mere", "kahan", "kidhar", "kitna", "kitne", "kab", "kaise", "kya",
        "kaun", "batao", "bataiye", "chahiye", "hai", "hoga", "kare", "karna", "paise",
        "bhada", "kiraya", "raider", "kamai", "samajh", "madad", "ऑर्डर", "पार्सल", "कहाँ",
        "कितना", "कैसे", "बताओ", "मदद"
    )

    fun extractMentionedOrderId(query: String): String? {
        val regex = Regex("""\b((?:QD|ORD)-[A-Za-z0-9-]+)\b""", RegexOption.IGNORE_CASE)
        return regex.find(query)?.groupValues?.getOrNull(1)?.uppercase(Locale.US)
    }

    fun prefersHindiReply(query: String, activeLanguage: AppLanguage): Boolean {
        val hasDevanagari = query.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) return true
        val lower = query.lowercase(Locale.US)
        if (HINDI_HINT_KEYWORDS.any { token -> Regex("\\b${Regex.escape(token)}\\b").containsMatchIn(lower) }) {
            return true
        }
        return activeLanguage == AppLanguage.HINDI
    }

    private fun containsOrderSignal(cleanLower: String): Boolean {
        if (ORDER_DOMAIN_KEYWORDS.any { kw ->
                when {
                    kw.any { it in '\u0900'..'\u097F' } -> cleanLower.contains(kw)
                    kw.endsWith("-") -> cleanLower.contains(kw)
                    else -> Regex("\\b${Regex.escape(kw)}\\b").containsMatchIn(cleanLower)
                }
            }
        ) {
            return true
        }
        return false
    }

    fun isOrderRelatedQuery(query: String): Boolean {
        val clean = query.trim().lowercase(Locale.US)
        if (clean.isBlank()) return false
        if (SECURITY_BYPASS_KEYWORDS.any { clean.contains(it) }) return false
        if (extractMentionedOrderId(clean) != null) return true
        if (clean in GREETING_KEYWORDS) return true
        val hasOffTopicSignal = EXPLICIT_OFF_TOPIC_KEYWORDS.any { clean.contains(it) }
        if (hasOffTopicSignal) return false
        return containsOrderSignal(clean)
    }

    fun isSecurityBypassProbe(query: String): Boolean {
        val clean = query.trim().lowercase(Locale.US)
        return SECURITY_BYPASS_KEYWORDS.any { clean.contains(it) }
    }

    fun quickPromptsFor(role: OrderBotRole, isHindi: Boolean): List<String> = when (role) {
        OrderBotRole.CUSTOMER -> if (isHindi) {
            listOf(
                "मेरे एक्टिव ऑर्डर का स्टेटस क्या है?",
                "मेरा Pickup और Delivery OTP क्या है?",
                "किराया, वज़न लिमिट और GST नियम बताओ",
                "ऑर्डर कैंसिल या रिफंड कैसे होगा?",
                "राइडर कब तक पहुंचेगा?"
            )
        } else {
            listOf(
                "What is my active order status?",
                "Show my Pickup & Delivery OTP",
                "Explain fare rates, 20 kg limit & GST",
                "How do cancellation & refunds work?",
                "Who is my assigned Rider?"
            )
        }
        OrderBotRole.RIDER -> if (isHindi) {
            listOf(
                "मेरा एक्टिव ऑर्डर और अगला स्टेप क्या है?",
                "Pickup और Drop OTP + Signature नियम क्या हैं?",
                "मेरी 80% कमाई और वॉलेट बैलेंस कितना है?",
                "₹200 COD कमीशन लिमिट कैसे काम करती है?",
                "पूल में कितने नए ऑर्डर उपलब्ध हैं?"
            )
        } else {
            listOf(
                "What is my active order & next step?",
                "Explain Pickup/Drop OTP & proof rules",
                "Show my 80% earnings & wallet balance",
                "How does the COD commission limit work?",
                "How many open orders are searching?"
            )
        }
    }

    fun welcomeMessage(role: OrderBotRole, isHindi: Boolean, ordersCount: Int): String = when (role) {
        OrderBotRole.CUSTOMER -> if (isHindi) {
            "नमस्ते! मैं Quick Delivery **Order Assistant** हूँ।\n" +
                "मैं **सिर्फ आपके ऑर्डर** से जुड़ी मदद करता हूँ (लाइव स्टेटस, Pickup/Drop OTP, किराया, पेमेंट, कैंसिलेशन और GST बिल)।\n" +
                if (ordersCount > 0) "आपके खाते में अभी $ordersCount ऑर्डर रिकॉर्ड हैं — नीचे कोई सवाल चुनें या लिखें।"
                else "नया ऑर्डर बुक करने, किराया जानने या ट्रैकिंग के लिए नीचे सवाल पूछें।"
        } else {
            "Hello! I am the Quick Delivery **Order Assistant**.\n" +
                "I strictly answer **order-related queries only** (live status, Pickup/Drop OTPs, fare rates, payment/refunds, and GST invoices).\n" +
                if (ordersCount > 0) "You have $ordersCount order(s) linked to your account — tap a chip or type your order question below."
                else "Ask about booking steps, vehicle rates, weight limits, or your orders below."
        }
        OrderBotRole.RIDER -> if (isHindi) {
            "नमस्ते पार्टनर! मैं Quick Delivery **Rider Order Bot** हूँ।\n" +
                "मैं **सिर्फ आपके ऑर्डर और ट्रिप** से जुड़ी जानकारी देता हूँ (एक्टिव ऑर्डर स्टेप, OTP + फोटो प्रूफ, 80% कमाई, COD लिमिट और पूल ऑर्डर)।"
        } else {
            "Hello Partner! I am the Quick Delivery **Rider Order Bot**.\n" +
                "I strictly help with **orders & deliveries** (active trip next steps, Pickup/Drop OTP + signature proof, 80% earnings, COD commission limits, and available pool orders)."
        }
    }

    fun replyToCustomerQuery(
        rawQuery: String,
        customerOrders: List<CustomerOrder>,
        selectedOrderId: String? = null,
        remoteFlags: AdminRemoteFeatureFlags = AdminRemoteFeatureFlags(),
        activeLanguage: AppLanguage = AppLanguageManager.currentLanguage.value
    ): OrderBotReply {
        val cleanQuery = QuickDeliverySecurityGuard.sanitizeText(rawQuery, 400)
        val useHindi = prefersHindiReply(cleanQuery, activeLanguage)

        if (isSecurityBypassProbe(cleanQuery)) {
            return OrderBotReply(
                text = if (useHindi) {
                    "🔒 सुरक्षा नीति: मैं OTP बायपास करने, नकली ऑर्डर बनाने या किसी दूसरे ग्राहक के ऑर्डर का डेटा दिखाने की अनुमति नहीं देता। सहायता केवल आपके सत्यापित ऑर्डर तक सीमित है।"
                } else {
                    "🔒 Security Policy: I cannot bypass OTP verification, generate unverified orders, or share another customer's order information. Assistance is strictly limited to your own verified orders."
                },
                acceptedAsOrderQuery = false
            )
        }

        val lower = cleanQuery.lowercase(Locale.US)
        if (!isOrderRelatedQuery(cleanQuery)) {
            return OrderBotReply(
                text = if (useHindi) {
                    "मैं Quick Delivery **Order Assistant** हूँ और **केवल ऑर्डर से जुड़े सवालों** का ही जवाब देता हूँ।\n" +
                        "कृपया ऑर्डर स्टेटस, लाइव ट्रैकिंग, Pickup/Delivery OTP, किराया, कैंसिलेशन या GST बिल के बारे में पूछें।"
                } else {
                    "I am the Quick Delivery **Order Assistant** and **only answer order-related questions**.\n" +
                        "Please ask about order status, live tracking, Pickup/Delivery OTPs, fare breakdown, payment/cancellation, or GST invoices."
                },
                acceptedAsOrderQuery = false
            )
        }

        if (lower in GREETING_KEYWORDS) {
            return OrderBotReply(
                text = welcomeMessage(OrderBotRole.CUSTOMER, useHindi, customerOrders.size),
                acceptedAsOrderQuery = true
            )
        }

        val mentionedId = extractMentionedOrderId(cleanQuery)
        if (mentionedId != null && customerOrders.none { it.orderId.equals(mentionedId, ignoreCase = true) }) {
            return OrderBotReply(
                text = if (useHindi) {
                    "⚠️ ऑर्डर #$mentionedId आपके साइन-इन किए गए ग्राहक खाते में नहीं मिला। सुरक्षा कारणों से आप केवल अपने ही ऑर्डर की जानकारी देख सकते हैं।"
                } else {
                    "⚠️ Order #$mentionedId was not found in your signed-in customer account. For privacy isolation, you can only query your own orders."
                },
                acceptedAsOrderQuery = true,
                referencedOrderId = mentionedId
            )
        }

        val activeOrder = customerOrders.firstOrNull {
            it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
        }
        val targetOrder = when {
            mentionedId != null -> customerOrders.firstOrNull { it.orderId.equals(mentionedId, ignoreCase = true) }
            !selectedOrderId.isNullOrBlank() -> customerOrders.firstOrNull { it.orderId == selectedOrderId }
            else -> activeOrder ?: customerOrders.firstOrNull()
        }

        // 1. OTP Queries (only reveals customer's own order OTP)
        if (lower.contains("otp") || lower.contains("code") || lower.contains("verify") || lower.contains("pin") || lower.contains("ओटीपी")) {
            if (targetOrder == null) {
                return OrderBotReply(
                    text = if (useHindi) {
                        "आपके खाते में अभी कोई ऑर्डर नहीं है। जैसे ही आप Step 1-2 से ऑर्डर बुक करेंगे, आपको 4-अंकों का **Pickup OTP** और **Delivery OTP** मिल जाएगा। केवल असली राइडर को हैंडओवर के समय ही OTP बताएं।"
                    } else {
                        "You do not have an order yet. Once you confirm a booking in Step 2, you will receive a 4-digit **Pickup OTP** (for parcel handover) and **Delivery OTP** (for receiver drop-off). Share OTPs only at physical handover."
                    },
                    acceptedAsOrderQuery = true
                )
            }
            val pickupOtpLine = targetOrder.pickupOtp.ifBlank { "Pending" }
            val dropOtpLine = targetOrder.deliveryOtp.ifBlank { "Pending" }
            return OrderBotReply(
                text = if (useHindi) {
                    "🔐 **ऑर्डर #${targetOrder.orderId} OTP विवरण:**\n" +
                        "• स्थिति: ${statusText(targetOrder.orderStatus, true)}\n" +
                        "• **Pickup OTP:** `$pickupOtpLine` (पार्सल सौंपते समय राइडर को दें)\n" +
                        "• **Delivery OTP:** `$dropOtpLine` (रिसीवर को डिलीवरी के समय बताएं)\n" +
                        "⚠️ सुरक्षा चेतावनी: बिना पार्सल लिए/दिए किसी अनजान कॉल पर OTP शेयर न करें।"
                } else {
                    "🔐 **Order #${targetOrder.orderId} OTP Details:**\n" +
                        "• Current Stage: ${statusText(targetOrder.orderStatus, false)}\n" +
                        "• **Pickup OTP:** `$pickupOtpLine` (share only when Rider collects parcel)\n" +
                        "• **Delivery OTP:** `$dropOtpLine` (for recipient at final drop-off)\n" +
                        "⚠️ Safety rule: Never share your OTP over phone before physical handover."
                },
                acceptedAsOrderQuery = true,
                referencedOrderId = targetOrder.orderId
            )
        }

        // 2. Fare, Vehicle Rates, Weight & GST
        if (lower.contains("fare") || lower.contains("price") || lower.contains("rate") || lower.contains("gst") ||
            lower.contains("weight") || lower.contains("kg") || lower.contains("wajan") || lower.contains("vajan") ||
            lower.contains("kiraya") || lower.contains("bhada") || lower.contains("kitna") || lower.contains("promo") ||
            lower.contains("coupon") || lower.contains("किराया") || lower.contains("bike") || lower.contains("scooter") || lower.contains("auto")
        ) {
            val orderFareSummary = targetOrder?.let { ord ->
                if (useHindi) {
                    "\n\n🧾 **आपका ऑर्डर #${ord.orderId} किराया:** ${ord.totalAmount} • दूरी: ${String.format(Locale.US, "%.1f", ord.distanceKm)} km • वाहन: ${ord.vehicleType.title} • वज़न: ${ord.packageDetails.weightKg} kg"
                } else {
                    "\n\n🧾 **Your Order #${ord.orderId} Fare:** ${ord.totalAmount} • Distance: ${String.format(Locale.US, "%.1f", ord.distanceKm)} km • Vehicle: ${ord.vehicleType.title} • Weight: ${ord.packageDetails.weightKg} kg"
                }
            }.orEmpty()
            return OrderBotReply(
                text = if (useHindi) {
                    "💰 **लाइव किराया और पार्सल नियम:**\n" +
                        "• **Bike:** बेस ₹${remoteFlags.bikeBaseFareInr.roundToInt()} + ₹${remoteFlags.bikeRatePerKmInr.roundToInt()}/km\n" +
                        "• **Scooter:** बेस ₹${remoteFlags.scooterBaseFareInr.roundToInt()} + ₹${remoteFlags.scooterRatePerKmInr.roundToInt()}/km\n" +
                        "• **Auto:** बेस ₹${remoteFlags.autoBaseFareInr.roundToInt()} + ₹${remoteFlags.autoRatePerKmInr.roundToInt()}/km\n" +
                        "• **अधिकतम वज़न:** प्रति पार्सल **20 kg** तक अनुमति है।\n" +
                        "• **टैक्स:** 5% GST और प्लेटफॉर्म सुरक्षा शुल्क स्टेप 1 में बुकिंग से पहले साफ दिखता है।" + orderFareSummary
                } else {
                    "💰 **Live Fare & Parcel Rules:**\n" +
                        "• **Bike:** Base ₹${remoteFlags.bikeBaseFareInr.roundToInt()} + ₹${remoteFlags.bikeRatePerKmInr.roundToInt()}/km\n" +
                        "• **Scooter:** Base ₹${remoteFlags.scooterBaseFareInr.roundToInt()} + ₹${remoteFlags.scooterRatePerKmInr.roundToInt()}/km\n" +
                        "• **Auto:** Base ₹${remoteFlags.autoBaseFareInr.roundToInt()} + ₹${remoteFlags.autoRatePerKmInr.roundToInt()}/km\n" +
                        "• **Max Weight Limit:** Up to **20 kg** per parcel.\n" +
                        "• **Tax & Offers:** Transparent 5% GST + upfront promo discounts before you confirm booking." + orderFareSummary
                },
                acceptedAsOrderQuery = true,
                referencedOrderId = targetOrder?.orderId
            )
        }

        // 3. Cancellation, Refund, Payment & GST Invoice
        if (lower.contains("cancel") || lower.contains("refund") || lower.contains("invoice") ||
            lower.contains("receipt") || lower.contains("bill") || lower.contains("payment") ||
            lower.contains("upi") || lower.contains("cod") || lower.contains("रद्द") || lower.contains("बिल")
        ) {
            val orderPayInfo = targetOrder?.let { ord ->
                if (useHindi) {
                    "\n\n💳 **ऑर्डर #${ord.orderId} पेमेंट:** ${ord.paymentMethod.shortBadge} (${ord.paymentStatus.name.replace('_', ' ')}) • कुल: ${ord.totalAmount}"
                } else {
                    "\n\n💳 **Order #${ord.orderId} Payment:** ${ord.paymentMethod.shortBadge} (${ord.paymentStatus.name.replace('_', ' ')}) • Total: ${ord.totalAmount}"
                }
            }.orEmpty()
            return OrderBotReply(
                text = if (useHindi) {
                    "🧾 **कैंसिलेशन, पेमेंट और GST बिल नियम:**\n" +
                        "• **कैंसिलेशन:** राइडर के पिकअप से पहले Step 3 में 'Cancel Booking' से ऑर्डर रद्द कर सकते हैं। फ्री वेट टाइम ${remoteFlags.freeWaitMinutes} मिनट है।\n" +
                        "• **पेमेंट विकल्प:** UPI (GPay/PhonePe/Paytm), कार्ड, नेटबैंकिंग या Cash (COD • अधिकतम ₹${remoteFlags.maxCodLimitInr})।\n" +
                        "• **GST रसीद:** सभी पुराने और सक्रिय ऑर्डर का GST बिल **Step 4 (History)** या मेनू → Order History में मिलता है।" + orderPayInfo
                } else {
                    "🧾 **Cancellation, Payment & GST Invoice Policy:**\n" +
                        "• **Cancel / Refund:** You can cancel in Step 3 before parcel pickup. Free wait time at pickup is ${remoteFlags.freeWaitMinutes} mins.\n" +
                        "• **Payment Methods:** Verified UPI, Cards, NetBanking, or Cash on Delivery (max COD ₹${remoteFlags.maxCodLimitInr}).\n" +
                        "• **GST Receipts:** Download itemized GST invoices anytime under **Step 4 (History)** or Menu → Order History." + orderPayInfo
                },
                acceptedAsOrderQuery = true,
                referencedOrderId = targetOrder?.orderId
            )
        }

        // 4. How to Book / New Order Instructions
        if ((lower.contains("book") || lower.contains("how") || lower.contains("kaise") || lower.contains("new order") || lower.contains("naya order")) &&
            targetOrder == null
        ) {
            return OrderBotReply(
                text = if (useHindi) {
                    "📦 **नया पार्सल ऑर्डर बुक करने के 4 आसान चरण:**\n" +
                        "1. **Step 1 (पता व वाहन):** पिकअप और ड्रॉप पता (या Auto GPS) डालें, रिसीवर फोन और वज़न (अधिकतम 20 kg) चुनें।\n" +
                        "2. **Step 2 (फ़ोटो व पेमेंट):** पार्सल की सुरक्षित फ़ोटो खींचें और UPI/Card/COD चुनकर कन्फर्म करें।\n" +
                        "3. **Step 3 (लाइव ट्रैक):** राइडर की लाइव लोकेशन और Pickup/Drop OTP देखें।\n" +
                        "4. **Step 4 (हिस्ट्री):** डिलीवरी के बाद GST बिल और डिजिटल साइन प्रूफ देखें।"
                } else {
                    "📦 **4 Easy Steps to Book a Parcel Order:**\n" +
                        "1. **Step 1 (Route & Vehicle):** Enter Pickup & Drop address (or tap Auto-Capture GPS), Receiver details & Weight (max 20 kg).\n" +
                        "2. **Step 2 (Photo & Payment):** Snap a mandatory parcel safety photo and pick UPI, Card, or Cash (COD).\n" +
                        "3. **Step 3 (Live Track):** Track Rider GPS & share 4-digit Pickup/Drop OTPs at handover.\n" +
                        "4. **Step 4 (History):** View GST invoices and digital signature delivery proofs."
                },
                acceptedAsOrderQuery = true
            )
        }

        // 5. Default / Status / Tracking / Rider Details
        if (targetOrder == null) {
            return OrderBotReply(
                text = if (useHindi) {
                    "आपके खाते में अभी कोई सक्रिय या पुराना ऑर्डर नहीं है। Step 1 में पिकअप और ड्रॉप पता भरकर अपना पहला ऑर्डर बुक करें!"
                } else {
                    "There are no active or past orders on your account yet. Fill Pickup & Drop details in Step 1 to book your first delivery!"
                },
                acceptedAsOrderQuery = true
            )
        }

        val riderText = if (!targetOrder.assignedRiderName.isNullOrBlank()) {
            val vehicleStr = targetOrder.assignedRiderVehicle?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            "${targetOrder.assignedRiderName}$vehicleStr"
        } else {
            if (useHindi) "नज़दीकी राइडर खोजा जा रहा है..." else "Searching nearby Rider Partner..."
        }

        return OrderBotReply(
            text = if (useHindi) {
                "📦 **ऑर्डर #${targetOrder.orderId} लाइव विवरण:**\n" +
                    "• **स्थिति:** ${statusText(targetOrder.orderStatus, true)}\n" +
                    "• **पिकअप:** ${targetOrder.pickupAddress.fullAddress}\n" +
                    "• **ड्रॉप:** ${targetOrder.dropAddress.fullAddress} (${String.format(Locale.US, "%.1f", targetOrder.distanceKm)} km)\n" +
                    "• **राइडर:** $riderText\n" +
                    "• **कुल किराया:** ${targetOrder.totalAmount} (${targetOrder.paymentMethod.shortBadge})\n" +
                    "• **नेक्स्ट स्टेप:** ${customerNextStepHint(targetOrder, true)}"
            } else {
                "📦 **Order #${targetOrder.orderId} Live Summary:**\n" +
                    "• **Status:** ${statusText(targetOrder.orderStatus, false)}\n" +
                    "• **Pickup:** ${targetOrder.pickupAddress.fullAddress}\n" +
                    "• **Drop:** ${targetOrder.dropAddress.fullAddress} (${String.format(Locale.US, "%.1f", targetOrder.distanceKm)} km)\n" +
                    "• **Rider Partner:** $riderText\n" +
                    "• **Total Fare:** ${targetOrder.totalAmount} (${targetOrder.paymentMethod.shortBadge})\n" +
                    "• **Next Step:** ${customerNextStepHint(targetOrder, false)}"
            },
            acceptedAsOrderQuery = true,
            referencedOrderId = targetOrder.orderId
        )
    }

    fun replyToRiderQuery(
        rawQuery: String,
        riderOrders: List<CustomerOrder>,
        openPoolOrdersCount: Int,
        riderProfile: RiderProfile?,
        pendingPayoutInr: Double = 0.0,
        selectedOrderId: String? = null,
        remoteFlags: AdminRemoteFeatureFlags = AdminRemoteFeatureFlags(),
        activeLanguage: AppLanguage = AppLanguageManager.currentLanguage.value
    ): OrderBotReply {
        val cleanQuery = QuickDeliverySecurityGuard.sanitizeText(rawQuery, 400)
        val useHindi = prefersHindiReply(cleanQuery, activeLanguage)

        if (isSecurityBypassProbe(cleanQuery)) {
            return OrderBotReply(
                text = if (useHindi) {
                    "🔒 सुरक्षा नीति: बिना ग्राहक के 4-अंक OTP, लाइव कैमरा फ़ोटो और डिजिटल सिग्नेचर के पिकअप या डिलीवरी पूरी नहीं की जा सकती। साथ ही आप अपना खुद का बनाया ऑर्डर स्वीकार नहीं कर सकते।"
                } else {
                    "🔒 Security Policy: Pickups and deliveries strictly require the real 4-digit OTP, live camera photo, and digital signature. Self-order acceptance and security bypasses are blocked."
                },
                acceptedAsOrderQuery = false
            )
        }

        val lower = cleanQuery.lowercase(Locale.US)
        if (!isOrderRelatedQuery(cleanQuery)) {
            return OrderBotReply(
                text = if (useHindi) {
                    "मैं Quick Delivery **Rider Order Bot** हूँ और **केवल ऑर्डर व डिलीवरी से जुड़े सवालों** का जवाब देता हूँ।\n" +
                        "कृपया एक्टिव ट्रिप, OTP व साइन प्रूफ स्टेप, 80% कमाई, COD लिमिट या नए ऑर्डर पूल के बारे में पूछें।"
                } else {
                    "I am the Quick Delivery **Rider Order Bot** and **strictly answer order & delivery queries only**.\n" +
                        "Please ask about active trips, Pickup/Drop OTP & proof rules, 80% earnings, COD limits, or open pool orders."
                },
                acceptedAsOrderQuery = false
            )
        }

        if (lower in GREETING_KEYWORDS) {
            return OrderBotReply(
                text = welcomeMessage(OrderBotRole.RIDER, useHindi, riderOrders.size),
                acceptedAsOrderQuery = true
            )
        }

        val mentionedId = extractMentionedOrderId(cleanQuery)
        val activeRiderOrder = riderOrders.firstOrNull {
            it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
        }
        val targetOrder = when {
            mentionedId != null -> riderOrders.firstOrNull { it.orderId.equals(mentionedId, ignoreCase = true) }
            !selectedOrderId.isNullOrBlank() -> riderOrders.firstOrNull { it.orderId == selectedOrderId }
            else -> activeRiderOrder ?: riderOrders.firstOrNull()
        }

        // 1. Earnings, 80/20 Commission, COD Limit & Payout
        if (lower.contains("earn") || lower.contains("wallet") || lower.contains("commission") ||
            Regex("\\bcod\\b").containsMatchIn(lower) || lower.contains("limit") || lower.contains("payout") ||
            lower.contains("withdraw") || lower.contains("cashout") || lower.contains("kamai") ||
            lower.contains("paisa") || lower.contains("paise") || lower.contains("कमाई") || lower.contains("80%")
        ) {
            val walletBal = riderProfile?.walletBalanceInr ?: 0.0
            val codDueInr = riderProfile?.commissionDebtInr ?: 0.0
            val deliveredCount = riderOrders.count { it.orderStatus == OrderStatus.DELIVERED }
            val activeOrdEarn = targetOrder?.let {
                if (useHindi) "\n• **ऑर्डर #${it.orderId} आपकी कमाई:** ${it.riderEarning} (कुल किराया ${it.totalAmount} का ${remoteFlags.riderSharePercent}%)"
                else "\n• **Order #${it.orderId} Your Earning:** ${it.riderEarning} (${remoteFlags.riderSharePercent}% of ${it.totalAmount})"
            }.orEmpty()
            return OrderBotReply(
                text = if (useHindi) {
                    "💰 **राइडर कमाई और ₹${remoteFlags.riderCodCommissionLimitInr} COD लिमिट नियम:**\n" +
                        "• **पार्टनर शेयर:** हर ऑर्डर का सीधा **${remoteFlags.riderSharePercent}%** आपको मिलता है (सिर्फ ${remoteFlags.platformCommissionPercent}% प्लेटफॉर्म शुल्क) + **100% ग्राहक टिप**।\n" +
                        "• **आपका वॉलेट बैलेंस:** ₹${walletBal.roundToInt()} | बकाया COD कमीशन: ₹${codDueInr.roundToInt()} | विड्रॉल योग्य पेआउट: ₹${pendingPayoutInr.roundToInt()} | आज पूरी डिलीवरी: $deliveredCount\n" +
                        "• **COD कमीशन नियम:** जब कैश ऑर्डर का बकाया ₹${remoteFlags.riderCodCommissionLimitInr} तक पहुँचता है तो ID लॉक से बचने के लिए डैशबोर्ड से एडमिन को कमीशन ट्रांसफर करें।$activeOrdEarn"
                } else {
                    "💰 **Partner Earnings & ₹${remoteFlags.riderCodCommissionLimitInr} COD Limit Rules:**\n" +
                        "• **Fair Split:** You earn **${remoteFlags.riderSharePercent}%** of every fare (${remoteFlags.platformCommissionPercent}% platform fee) + **100% of customer tips**.\n" +
                        "• **Your Wallet:** ₹${walletBal.roundToInt()} • COD Commission Due: ₹${codDueInr.roundToInt()} • Pending Payout: ₹${pendingPayoutInr.roundToInt()} • Delivered Trips: $deliveredCount\n" +
                        "• **COD Lock Rule:** Settle Admin commission dues before reaching -₹${remoteFlags.riderCodCommissionLimitInr} to stay unlocked and keep receiving orders.$activeOrdEarn"
                },
                acceptedAsOrderQuery = true,
                referencedOrderId = targetOrder?.orderId
            )
        }

        // 2. Pickup / Drop OTP & Proof Rules
        if (lower.contains("otp") || lower.contains("proof") || lower.contains("signature") ||
            lower.contains("photo") || lower.contains("sign") || lower.contains("ओटीपी")
        ) {
            val stepHint = when (targetOrder?.orderStatus) {
                OrderStatus.RIDER_ASSIGNED -> if (useHindi) "अभी: पिकअप लोकेशन पहुँचकर 'Reached Pickup Gate' दबाएं, फिर Sender से 4-अंक **Pickup OTP** लें।" else "Current step for #${targetOrder.orderId}: Tap 'Reached Pickup Gate' on arrival, then collect the 4-digit **Pickup OTP** from Sender."
                OrderStatus.ARRIVED_AT_PICKUP -> if (useHindi) "अभी: Sender से 4-अंक **Pickup OTP** पूछकर डालें, पार्सल की लाइव फ़ोटो और Sender का सिग्नेचर लें।" else "Current step for #${targetOrder.orderId}: Enter Sender's 4-digit **Pickup OTP**, capture live parcel photo & Sender digital signature."
                OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> if (useHindi) "अभी: ड्रॉप लोकेशन पर रिसीवर से 4-अंक **Delivery OTP** लें, डिलीवरी फ़ोटो और रिसीवर सिग्नेचर अपलोड करें।" else "Current step for #${targetOrder.orderId}: At drop location, enter Receiver's 4-digit **Delivery OTP**, capture drop photo & Receiver signature."
                else -> if (useHindi) "हर ऑर्डर में 2 OTP चरण अनिवार्य हैं: (1) Pickup OTP + फोटो + सेंडर साइन, (2) Delivery OTP + ड्रॉप फोटो + रिसीवर साइन।" else "Every order enforces 2 mandatory checkpoints: (1) Pickup OTP + Parcel Photo + Sender Signature, (2) Delivery OTP + Drop Photo + Receiver Signature."
            }
            return OrderBotReply(
                text = if (useHindi) {
                    "🔐 **डिलीवरी OTP और प्रूफ गाइड:**\n$stepHint\n💡 नोट: OTP हमेशा ग्राहक/सेंडर से मौके पर पूछें — सिस्टम स्वतः सुरक्षा कारणों से राइडर को OTP नहीं दिखाता।"
                } else {
                    "🔐 **Rider OTP & Proof Checklist:**\n$stepHint\n💡 Note: For security, OTPs are held by the Customer/Receiver and never pre-shown to Riders."
                },
                acceptedAsOrderQuery = true,
                referencedOrderId = targetOrder?.orderId
            )
        }

        // 3. Available Pool / Searching Orders / Duty Eligibility
        if (lower.contains("pool") || lower.contains("available") || lower.contains("new order") ||
            lower.contains("search") || lower.contains("online") || lower.contains("kyc") ||
            lower.contains("selfie") || lower.contains("मिलेगा") || lower.contains("उपलब्ध")
        ) {
            val dutyLabel = if (riderProfile?.isOnline == true) {
                if (useHindi) "🟢 ONLINE (ऑर्डर अलर्ट चालू)" else "🟢 ONLINE (Ready for dispatch)"
            } else {
                if (useHindi) "⚪ OFFLINE (ड्यूटी स्विच से लाइव सेल्फी के साथ Online आएं)" else "⚪ OFFLINE (Toggle Duty switch + Live Selfie to go Online)"
            }
            return OrderBotReply(
                text = if (useHindi) {
                    "🛵 **लाइव ऑर्डर पूल व ड्यूटी स्थिति:**\n" +
                        "• **आपकी ड्यूटी:** $dutyLabel\n" +
                        "• **उपलब्ध पूल ऑर्डर:** अभी **$openPoolOrdersCount** नए ऑर्डर राइडर की प्रतीक्षा कर रहे हैं।\n" +
                        "• **नियम:** नया ऑर्डर आने पर आप **Accept** कर सकते हैं या कारण चुनकर **Decline** कर सकते हैं — कोई जबरदस्ती नहीं है।"
                } else {
                    "🛵 **Live Order Pool & Duty Status:**\n" +
                        "• **Your Duty:** $dutyLabel\n" +
                        "• **Open Searching Pool:** **$openPoolOrdersCount** order(s) waiting for Rider acceptance right now.\n" +
                        "• **Fair Dispatch:** You can freely **Accept** or **Decline with a reason** on any offered order."
                },
                acceptedAsOrderQuery = true
            )
        }

        // 4. Mentioned Order ID missing
        if (mentionedId != null && targetOrder == null) {
            return OrderBotReply(
                text = if (useHindi) {
                    "⚠️ ऑर्डर #$mentionedId आपकी ट्रिप सूची में नहीं मिला। सक्रिय या असाइन किए गए ऑर्डर नंबर से पूछें।"
                } else {
                    "⚠️ Order #$mentionedId is not assigned to you or in your active list."
                },
                acceptedAsOrderQuery = true,
                referencedOrderId = mentionedId
            )
        }

        // 5. Default Active Trip Summary
        if (targetOrder == null) {
            return OrderBotReply(
                text = if (useHindi) {
                    "अभी आपके पास कोई सक्रिय ट्रिप नहीं है। पूल में **$openPoolOrdersCount** ऑर्डर उपलब्ध हैं — नए ऑर्डर पाने के लिए टॉप बार से **ONLINE** हो जाएं।"
                } else {
                    "You have no active trip right now. There are **$openPoolOrdersCount** open order(s) searching in the pool — switch **ONLINE** in the top header to accept deliveries."
                },
                acceptedAsOrderQuery = true
            )
        }

        return OrderBotReply(
            text = if (useHindi) {
                "🛵 **ट्रिप #${targetOrder.orderId} लाइव सारांश:**\n" +
                    "• **स्थिति:** ${statusText(targetOrder.orderStatus, true)}\n" +
                    "• **पिकअप:** ${targetOrder.pickupAddress.fullAddress} (${targetOrder.pickupAddress.contactName})\n" +
                    "• **ड्रॉप:** ${targetOrder.dropAddress.fullAddress} (${targetOrder.dropAddress.contactName})\n" +
                    "• **दूरी व कमाई:** ${String.format(Locale.US, "%.1f", targetOrder.distanceKm)} km • आपकी कमाई: **${targetOrder.riderEarning}**\n" +
                    "• **पेमेंट प्रकार:** ${targetOrder.paymentMethod.shortBadge} (${targetOrder.paymentStatus.name.replace('_', ' ')})\n" +
                    "• **अगला कदम:** ${riderNextStepHint(targetOrder, true)}"
            } else {
                "🛵 **Trip #${targetOrder.orderId} Live Summary:**\n" +
                    "• **Status:** ${statusText(targetOrder.orderStatus, false)}\n" +
                    "• **Pickup:** ${targetOrder.pickupAddress.fullAddress} (${targetOrder.pickupAddress.contactName})\n" +
                    "• **Drop:** ${targetOrder.dropAddress.fullAddress} (${targetOrder.dropAddress.contactName})\n" +
                    "• **Distance & Earning:** ${String.format(Locale.US, "%.1f", targetOrder.distanceKm)} km • Your Earning: **${targetOrder.riderEarning}**\n" +
                    "• **Payment:** ${targetOrder.paymentMethod.shortBadge} (${targetOrder.paymentStatus.name.replace('_', ' ')})\n" +
                    "• **Next Step:** ${riderNextStepHint(targetOrder, false)}"
            },
            acceptedAsOrderQuery = true,
            referencedOrderId = targetOrder.orderId
        )
    }

    private fun statusText(status: OrderStatus, hindi: Boolean): String = when (status) {
        OrderStatus.SEARCHING_RIDER -> if (hindi) "राइडर खोजा जा रहा है" else "Searching for Rider"
        OrderStatus.RIDER_ASSIGNED -> if (hindi) "राइडर असाइन हो गया (पिकअप के लिए निकलें)" else "Rider Assigned (Heading to Pickup)"
        OrderStatus.ARRIVED_AT_PICKUP -> if (hindi) "राइडर पिकअप गेट पर पहुँच गया" else "Arrived at Pickup Gate"
        OrderStatus.PICKED_UP -> if (hindi) "पार्सल पिकअप हो चुका है" else "Parcel Picked Up"
        OrderStatus.IN_TRANSIT -> if (hindi) "रास्ते में (डिलीवरी के लिए जा रहा है)" else "In Transit to Drop"
        OrderStatus.DELIVERED -> if (hindi) "सफलतापूर्वक डिलीवर हो गया" else "Delivered Successfully"
        OrderStatus.CANCELLED -> if (hindi) "रद्द (Cancelled)" else "Cancelled"
    }

    private fun customerNextStepHint(order: CustomerOrder, hindi: Boolean): String = when (order.orderStatus) {
        OrderStatus.SEARCHING_RIDER -> if (hindi) "नज़दीकी राइडर द्वारा स्वीकार करने की प्रतीक्षा करें।" else "Waiting for a nearby Rider Partner to accept."
        OrderStatus.RIDER_ASSIGNED -> if (hindi) "राइडर आ रहा है। पिकअप के समय Pickup OTP (${order.pickupOtp}) तैयार रखें।" else "Rider is on the way. Keep Pickup OTP (${order.pickupOtp}) ready."
        OrderStatus.ARRIVED_AT_PICKUP -> if (hindi) "राइडर गेट पर है — पार्सल सौंपते समय Pickup OTP (${order.pickupOtp}) दें और साइन करें।" else "Rider is at your pickup gate — share Pickup OTP (${order.pickupOtp}) & sign."
        OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT -> if (hindi) "रिसीवर को Delivery OTP (${order.deliveryOtp}) शेयर करें ताकि ड्रॉप पर वेरिफाई हो सके।" else "Share Delivery OTP (${order.deliveryOtp}) with Receiver for final handover."
        OrderStatus.DELIVERED -> if (hindi) "Step 4 (History) से अपना GST बिल और डिलीवरी प्रूफ देख सकते हैं।" else "View your GST Invoice and Delivery Proof in Step 4 (History)."
        OrderStatus.CANCELLED -> if (hindi) "आप Step 1 से नया ऑर्डर कभी भी बुक कर सकते हैं।" else "You can place a new booking anytime in Step 1."
    }

    private fun riderNextStepHint(order: CustomerOrder, hindi: Boolean): String = when (order.orderStatus) {
        OrderStatus.SEARCHING_RIDER -> if (hindi) "ऑर्डर स्वीकार करने के लिए Accept दबाएं।" else "Tap Accept to claim this order."
        OrderStatus.RIDER_ASSIGNED -> if (hindi) "पिकअप पते पर पहुँचकर 'Reached Pickup Gate' दबाएं।" else "Navigate to pickup & tap 'Reached Pickup Gate'."
        OrderStatus.ARRIVED_AT_PICKUP -> if (hindi) "Sender का Pickup OTP डालें + पार्सल फ़ोटो + साइन अपलोड करें।" else "Verify Sender's Pickup OTP + Parcel Photo + Sender Signature."
        OrderStatus.PICKED_UP -> if (hindi) "Start Transit / ड्रॉप नेविगेशन शुरू करें।" else "Proceed to drop location & mark In Transit."
        OrderStatus.IN_TRANSIT -> if (hindi) "Receiver से Delivery OTP लें + ड्रॉप फ़ोटो + साइन से डिलीवरी पूरी करें।" else "Verify Receiver's Delivery OTP + Drop Photo + Receiver Signature."
        OrderStatus.DELIVERED -> if (hindi) "ट्रिप पूरी हुई! आपकी 80% कमाई वॉलेट में जुड़ गई है।" else "Trip completed! Your 80% earning is credited to Wallet."
        OrderStatus.CANCELLED -> if (hindi) "पूल में उपलब्ध अन्य ऑर्डर देखें।" else "Check open pool for your next delivery."
    }
}

/**
 * Interactive Modal Chatbot UI strictly scoped to Quick Delivery Order questions
 * for both Customer App and Rider Partner App.
 */
@Composable
fun QuickDeliveryOrderChatbotDialog(
    role: OrderBotRole,
    orders: List<CustomerOrder>,
    openPoolOrdersCount: Int = 0,
    riderProfile: RiderProfile? = null,
    pendingPayoutInr: Double = 0.0,
    remoteFlags: AdminRemoteFeatureFlags = AdminRemoteFeatureFlags(),
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val activeLang by AppLanguageManager.currentLanguage.collectAsState()
    val isHindiUi = activeLang == AppLanguage.HINDI
    var selectedOrderId by remember(orders.size) {
        mutableStateOf(
            orders.firstOrNull {
                it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED
            }?.orderId ?: orders.firstOrNull()?.orderId
        )
    }
    var userInput by remember { mutableStateOf("") }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    val messages = remember(role) {
        mutableStateListOf(
            OrderBotChatMessage(
                id = "bot_welcome",
                isFromBot = true,
                text = OrderAssistantBotEngine.welcomeMessage(role, isHindiUi, orders.size),
                timestampLabel = timeFmt.format(Date())
            )
        )
    }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val sendQuery: (String) -> Unit = { queryText ->
        val trimmed = queryText.trim()
        if (trimmed.isNotEmpty()) {
            val nowLabel = timeFmt.format(Date())
            messages.add(
                OrderBotChatMessage(
                    id = "usr_${System.currentTimeMillis()}_${messages.size}",
                    isFromBot = false,
                    text = trimmed,
                    timestampLabel = nowLabel
                )
            )
            val reply = when (role) {
                OrderBotRole.CUSTOMER -> OrderAssistantBotEngine.replyToCustomerQuery(
                    rawQuery = trimmed,
                    customerOrders = orders,
                    selectedOrderId = selectedOrderId,
                    remoteFlags = remoteFlags,
                    activeLanguage = activeLang
                )
                OrderBotRole.RIDER -> OrderAssistantBotEngine.replyToRiderQuery(
                    rawQuery = trimmed,
                    riderOrders = orders,
                    openPoolOrdersCount = openPoolOrdersCount,
                    riderProfile = riderProfile,
                    pendingPayoutInr = pendingPayoutInr,
                    selectedOrderId = selectedOrderId,
                    remoteFlags = remoteFlags,
                    activeLanguage = activeLang
                )
            }
            if (!reply.referencedOrderId.isNullOrBlank() &&
                orders.any { it.orderId.equals(reply.referencedOrderId, ignoreCase = true) }
            ) {
                selectedOrderId = orders.first { it.orderId.equals(reply.referencedOrderId, ignoreCase = true) }.orderId
            }
            messages.add(
                OrderBotChatMessage(
                    id = "bot_${System.currentTimeMillis()}_${messages.size}",
                    isFromBot = true,
                    text = reply.text,
                    timestampLabel = timeFmt.format(Date()),
                    isRefusal = !reply.acceptedAsOrderQuery
                )
            )
            userInput = ""
        }
    }

    val tagPrefix = if (role == OrderBotRole.RIDER) "rider" else "customer"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.45f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f)
                .imePadding()
                .testTag("${tagPrefix}_order_chatbot_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0D1B2A), accentColor.copy(alpha = 0.92f))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
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
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Order Assistant Bot",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (role == OrderBotRole.RIDER) {
                                        "Rider Order Assistant Bot"
                                    } else {
                                        "Order Assistant Bot"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isHindiUi) {
                                        "केवल ऑर्डर, OTP, ट्रैकिंग और पेमेंट सहायता"
                                    } else {
                                        "Order-Only Scope • Status, OTP, Tracking & Fares"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    messages.clear()
                                    messages.add(
                                        OrderBotChatMessage(
                                            id = "bot_reset_${System.currentTimeMillis()}",
                                            isFromBot = true,
                                            text = OrderAssistantBotEngine.welcomeMessage(role, isHindiUi, orders.size),
                                            timestampLabel = timeFmt.format(Date())
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("${tagPrefix}_order_chatbot_clear_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Chat",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("${tagPrefix}_order_chatbot_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Order Bot",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Order Context Selector Chips (if orders exist)
                if (orders.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindiUi) "ऑर्डर चुनें:" else "Order:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        orders.take(6).forEach { ord ->
                            val isSelected = selectedOrderId == ord.orderId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedOrderId = ord.orderId
                                    sendQuery("Status of #${ord.orderId}")
                                },
                                label = {
                                    Text(
                                        text = "#${ord.orderId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }

                // Chat Messages Area
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isFromBot) Arrangement.Start else Arrangement.End
                        ) {
                            Surface(
                                color = when {
                                    !msg.isFromBot -> accentColor
                                    msg.isRefusal -> Color(0xFFFFF7ED)
                                    else -> Color.White
                                },
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.isFromBot) 4.dp else 16.dp,
                                    bottomEnd = if (msg.isFromBot) 16.dp else 4.dp
                                ),
                                border = when {
                                    msg.isRefusal -> BorderStroke(1.dp, Color(0xFFF97316))
                                    msg.isFromBot -> BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    else -> null
                                },
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (msg.isFromBot) Color(0xFF0F172A) else Color.White,
                                        fontWeight = if (msg.isFromBot) FontWeight.Normal else FontWeight.SemiBold
                                    )
                                    Text(
                                        text = msg.timestampLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (msg.isFromBot) Color(0xFF94A3B8) else Color.White.copy(alpha = 0.78f),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick-Ask Order Suggestion Chips
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OrderAssistantBotEngine.quickPromptsFor(role, isHindiUi).forEachIndexed { idx, prompt ->
                        Surface(
                            onClick = { sendQuery(prompt) },
                            color = accentColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("${tagPrefix}_order_bot_chip_$idx")
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Input Row
                Surface(
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            placeholder = {
                                Text(
                                    text = if (isHindiUi) {
                                        "केवल ऑर्डर, OTP या किराया पूछें..."
                                    } else {
                                        "Ask only about orders, OTP, tracking..."
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendQuery(userInput) }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("${tagPrefix}_order_bot_input")
                        )
                        Surface(
                            onClick = { sendQuery(userInput) },
                            enabled = userInput.isNotBlank(),
                            color = if (userInput.isNotBlank()) accentColor else Color(0xFFCBD5E1),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("${tagPrefix}_order_bot_send_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Order Question",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class AppGuidePage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val highlights: List<String>
)

/**
 * Detailed First-Time New Account Sign-Up App Walkthrough Screen shown for BOTH
 * Customer ("Quick Delivery") & Rider Partner ("Partner Quick Delivery"), complete
 * with an always-accessible Skip option so users can read app details or skip immediately.
 */
@Composable
fun QuickDeliveryNewUserAppGuideScreen(
    roleTitle: String,
    prefsKeyPrefix: String,
    accentColor: Color,
    userName: String,
    onFinishOrSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLang by AppLanguageManager.currentLanguage.collectAsState()
    val isHindi = activeLang == AppLanguage.HINDI
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = remember(prefsKeyPrefix, isHindi) {
        if (prefsKeyPrefix == "rider") {
            if (isHindi) {
                listOf(
                    AppGuidePage(
                        title = "1. पार्टनर ऐप में स्वागत है • 80% सीधी कमाई",
                        subtitle = "हर डिलीवरी का 80% किराया और 100% कस्टमर टिप सीधे आपकी कमाई है।",
                        icon = Icons.Default.AccountBalanceWallet,
                        highlights = listOf(
                            "80% पार्टनर शेयर + सिर्फ 20% पारदर्शी प्लेटफॉर्म शुल्क",
                            "रोज़ाना 4 ट्रिप पर ₹80 और 6 ट्रिप पर ₹150 अतिरिक्त बोनस",
                            "बैंक खाता / UPI लिंक करके तेज़ सेम-डे पेआउट पाएं"
                        )
                    ),
                    AppGuidePage(
                        title = "2. KYC सत्यापन और बैंक सेटअप",
                        subtitle = "ऑर्डर शुरू करने के लिए एक बार दस्तावेज़ और बैंक विवरण जोड़ें।",
                        icon = Icons.Default.VerifiedUser,
                        highlights = listOf(
                            "आधार, पैन, ड्राइविंग लाइसेंस और वाहन RC की फ़ोटो अपलोड करें",
                            "Admin वेरिफ़िकेशन पूरा होते ही LIVE ऑर्डर मिलना शुरू होंगे",
                            "ऑनलाइन ड्यूटी पर आते समय सुरक्षित लाइव सेल्फी चेक-इन अनिवार्य"
                        )
                    ),
                    AppGuidePage(
                        title = "3. OTP + फ़ोटो + सिग्नेचर सुरक्षा",
                        subtitle = "फर्जी ऑर्डर और विवाद से बचने के लिए डबल वेरिफ़िकेशन।",
                        icon = Icons.Default.Lock,
                        highlights = listOf(
                            "नए ऑर्डर आप अपनी मर्जी से Accept या कारण चुनकर Reject कर सकते हैं",
                            "पिकअप पर 4-अंक Pickup OTP + पार्सल फ़ोटो + सेंडर डिजिटल साइन अनिवार्य",
                            "ड्रॉप पर 4-अंक Delivery OTP + ड्रॉप फ़ोटो + रिसीवर साइन से पेआउट पक्का"
                        )
                    ),
                    AppGuidePage(
                        title = "4. Rider Order Bot और नाइट मोड",
                        subtitle = "ऑर्डर, COD लिमिट और ऑफ़लाइन फोन बैकअप हमेशा साथ।",
                        icon = Icons.Default.SmartToy,
                        highlights = listOf(
                            "इन-बिल्ट Order Chatbot सिर्फ आपके ऑर्डर, OTP स्टेप और कमाई बताता है",
                            "₹200 COD कमीशन लिमिट अलर्ट ताकि आपकी पार्टनर ID कभी ब्लॉक न हो",
                            "फ़ोन स्टोरेज में प्रोफ़ाइल व ऑर्डर का एन्क्रिप्टेड बैकअप सुरक्षित रहता है"
                        )
                    )
                )
            } else {
                listOf(
                    AppGuidePage(
                        title = "1. Partner Earnings • Keep 80% Per Trip",
                        subtitle = "Earn 80% of every delivery fare plus 100% of customer tips directly.",
                        icon = Icons.Default.AccountBalanceWallet,
                        highlights = listOf(
                            "Transparent 80% Partner share & 20% platform commission",
                            "Daily trip bonuses: ₹80 for 4 trips & ₹150 for 6 completed trips",
                            "Link Bank Account / UPI for fast same-day payout requests"
                        )
                    ),
                    AppGuidePage(
                        title = "2. Quick KYC & Live Selfie Duty",
                        subtitle = "Verified partners stay protected with quick onboarding.",
                        icon = Icons.Default.VerifiedUser,
                        highlights = listOf(
                            "Upload Aadhaar, PAN, Driving Licence & Vehicle RC once for Admin approval",
                            "Go Online anytime with quick Live Selfie check-in & turn Offline anytime",
                            "Fair ride dispatch with zero forced locking — accept or decline with a reason"
                        )
                    ),
                    AppGuidePage(
                        title = "3. OTP + Photo + Signature Protection",
                        subtitle = "Zero disputes on pickups, cash collection, or drop-offs.",
                        icon = Icons.Default.Lock,
                        highlights = listOf(
                            "Pickup checkpoint: 4-digit Pickup OTP + live package photo + Sender signature",
                            "Delivery checkpoint: 4-digit Delivery OTP + drop photo + Receiver signature",
                            "Real-time Admin support desk, SOS button & COD commission tracker"
                        )
                    ),
                    AppGuidePage(
                        title = "4. Order Assistant Bot & Offline Backup",
                        subtitle = "Order-only bot & dark mode built for busy riders.",
                        icon = Icons.Default.SmartToy,
                        highlights = listOf(
                            "Built-in Order Assistant Bot answers active trip, OTP & COD limit questions",
                            "High-contrast Night Dark Mode inside Menu for easy night riding",
                            "Encrypted profile & delivery backup stored safely in Phone Storage"
                        )
                    )
                )
            }
        } else {
            if (isHindi) {
                listOf(
                    AppGuidePage(
                        title = "1. Quick Delivery में स्वागत है!",
                        subtitle = "तेज़, पारदर्शी और सुरक्षित घर-से-घर पार्सल डिलीवरी ऐप।",
                        icon = Icons.Default.LocalShipping,
                        highlights = listOf(
                            "Step 1: ऑटो GPS से पिकअप व ड्रॉप पता भरें और Bike / Scooter / Auto चुनें",
                            "20 kg तक के पार्सल के लिए बुकिंग से पहले ही सटीक किराया और 5% GST देखें",
                            "तुरंत डिलीवरी या शेड्यूल स्लॉट, मल्टी-ड्रॉप और हेल्पर विकल्प चुनें"
                        )
                    ),
                    AppGuidePage(
                        title = "2. पार्सल फ़ोटो व सुरक्षित पेमेंट",
                        subtitle = "Step 2 में पार्सल सुरक्षा फ़ोटो और पसंदीदा भुगतान चुनें।",
                        icon = Icons.Default.CameraAlt,
                        highlights = listOf(
                            "पार्सल की साफ फ़ोटो जोड़ें ताकि राइडर सावधानी से सामान संभाले",
                            "UPI (GPay, PhonePe, Paytm), कार्ड, नेटबैंकिंग या Cash (COD) से भुगतान करें",
                            "वैकल्पिक ₹5,000 पार्सल इंश्योरेंस और प्रोमो कोड छूट उपलब्ध"
                        )
                    ),
                    AppGuidePage(
                        title = "3. लाइव GPS और 4-अंक OTP सुरक्षा",
                        subtitle = "Step 3 में राइडर की लाइव लोकेशन और दोनों OTP देखें।",
                        icon = Icons.Default.MyLocation,
                        highlights = listOf(
                            "पिकअप के समय राइडर को केवल **Pickup OTP** देकर हैंडओवर करें",
                            "डिलीवरी के समय रिसीवर **Delivery OTP** और डिजिटल सिग्नेचर से पार्सल लेगा",
                            "राइडर से लाइव चैट, कॉल और देरी अलर्ट सीधे स्क्रीन पर मिलते हैं"
                        )
                    ),
                    AppGuidePage(
                        title = "4. Order Chatbot, GST बिल और बैकअप",
                        subtitle = "Step 4 में बिल और इन-बिल्ट ऑर्डर चैटबॉट का उपयोग करें।",
                        icon = Icons.Default.ReceiptLong,
                        highlights = listOf(
                            "टॉप बार में **Order Bot** से सिर्फ ऑर्डर स्टेटस, OTP व किराए की जानकारी पाएं",
                            "हर डिलीवरी का GST बिल और सिग्नेचर प्रूफ History में सेव रहता है",
                            "आपका डेटा एन्क्रिप्टेड रूप में फ़ोन स्टोरेज में सुरक्षित बैकअप रहता है"
                        )
                    )
                )
            } else {
                listOf(
                    AppGuidePage(
                        title = "1. Welcome to Quick Delivery!",
                        subtitle = "Fast, transparent, and secure door-to-door parcel delivery.",
                        icon = Icons.Default.LocalShipping,
                        highlights = listOf(
                            "Step 1: Capture Pickup & Drop addresses via GPS and choose Bike, Scooter, or Auto",
                            "See exact upfront fare & 5% GST before booking (up to 20 kg per parcel)",
                            "Optional Scheduled slot, Multi-drop stop, Parcel Insurance & Helper add-on"
                        )
                    ),
                    AppGuidePage(
                        title = "2. Safety Photo & Flexible Payment",
                        subtitle = "Verify parcel condition & pick your preferred payment method in Step 2.",
                        icon = Icons.Default.CameraAlt,
                        highlights = listOf(
                            "Attach a live parcel safety photo before rider dispatch",
                            "Pay securely via UPI (GPay/PhonePe/Paytm), Card, NetBanking, or Cash (COD)",
                            "Apply active Admin promo coupons for instant booking discounts"
                        )
                    ),
                    AppGuidePage(
                        title = "3. Live GPS Tracking & Dual-OTP Security",
                        subtitle = "Track every meter in Step 3 with verified handover protection.",
                        icon = Icons.Default.MyLocation,
                        highlights = listOf(
                            "Share your 4-digit **Pickup OTP** only when Rider collects your parcel",
                            "Recipient confirms final handover with **Delivery OTP** + digital signature",
                            "Built-in live Rider-Customer chat, direct call & Admin support desk"
                        )
                    ),
                    AppGuidePage(
                        title = "4. Order Assistant Bot, GST Receipts & Vault",
                        subtitle = "Order-only chatbot & encrypted phone storage backup ready.",
                        icon = Icons.Default.ReceiptLong,
                        highlights = listOf(
                            "Tap **Order Bot** anytime to ask about your orders, OTPs, fares & refunds",
                            "Download itemized GST invoices & proof records from Step 4 (History)",
                            "Account & orders stay safely backed up in encrypted Phone Storage"
                        )
                    )
                )
            }
        }
    }

    val totalPages = pages.size
    val page = pages[currentPage.coerceIn(0, totalPages - 1)]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF132A42), accentColor.copy(alpha = 0.85f))
                )
            )
            .padding(16.dp)
            .testTag("${prefsKeyPrefix}_new_user_app_guide_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Prominent Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    QuickDeliveryCuteBunnyLogo(size = 38.dp, borderColor = accentColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (userName.isNotBlank()) "Welcome, $userName!" else "Welcome to Quick Delivery",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$roleTitle • App Guide (${currentPage + 1}/$totalPages)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
                TextButton(
                    onClick = onFinishOrSkip,
                    modifier = Modifier.testTag("${prefsKeyPrefix}_app_guide_skip_top_btn")
                ) {
                    Text(
                        text = if (isHindi) "स्किप करें →" else "Skip →",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD166)
                    )
                }
            }

            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentPage + 1).toFloat() / totalPages.toFloat() },
                color = Color(0xFFFFD166),
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(vertical = 0.dp)
            )

            // Main Feature Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "app_guide_page_anim"
                ) { idx ->
                    val activePage = pages[idx.coerceIn(0, totalPages - 1)]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = accentColor.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = activePage.icon,
                                        contentDescription = activePage.title,
                                        tint = accentColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "नया अकाउंट गाइड • ${idx + 1}/$totalPages" else "NEW ACCOUNT GUIDE • ${idx + 1}/$totalPages",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Text(
                            text = activePage.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D1B2A)
                        )

                        Text(
                            text = activePage.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            activePage.highlights.forEach { bullet ->
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = bullet,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }

                        // Step dot pills inside card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(totalPages) { dotIdx ->
                                Surface(
                                    color = if (dotIdx == idx) accentColor else Color(0xFFCBD5E1),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (dotIdx == idx) 10.dp else 8.dp)
                                        .clickable { currentPage = dotIdx }
                                ) {}
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar: Skip & Next / Start
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("${prefsKeyPrefix}_app_guide_prev_btn")
                    ) {
                        Text(
                            text = if (isHindi) "← पीछे" else "← Back",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onFinishOrSkip,
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("${prefsKeyPrefix}_app_guide_skip_btn")
                    ) {
                        Text(
                            text = if (isHindi) "स्किप करें" else "Skip",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage += 1
                        } else {
                            onFinishOrSkip()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD166),
                        contentColor = Color(0xFF0D1B2A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("${prefsKeyPrefix}_app_guide_next_btn")
                ) {
                    Text(
                        text = if (currentPage < totalPages - 1) {
                            if (isHindi) "आगे जानें →" else "Next Details →"
                        } else {
                            if (isHindi) "शुरू करें →" else "Get Started →"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
