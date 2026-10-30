package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

data class RiderDeliveryFeedback(
    val orderId: String,
    val riderId: String,
    val riderName: String,
    val customerName: String,
    val starRating: Int,
    val complimentTags: List<String>,
    val comment: String,
    val submittedAt: String,
    val tipAmountInr: Int = 0
)

data class CustomerRatingByRider(
    val orderId: String,
    val riderId: String,
    val riderName: String,
    val customerName: String,
    val starRating: Int,
    val behaviorTags: List<String>,
    val comment: String,
    val submittedAt: String
)

data class AppSuggestionEntry(
    val suggestionId: String,
    val senderRole: String, // "CUSTOMER" or "RIDER"
    val senderName: String,
    val category: String,
    val message: String,
    val submittedAt: String
)

/**
 * Shared reactive manager for:
 * 1. Customer -> Rider Ratings (Visible ONLY in Admin Web / Admin Panel)
 * 2. Rider -> Customer Ratings (Visible ONLY in Admin Web / Admin Panel)
 * 3. Customer & Rider Suggestion Box Submissions (Synced to Admin Web / Admin Panel)
 * 4. 100% Rider-kept Customer Tips
 */
object RiderFeedbackManager {
    private const val PREFS_NAME = "quick_delivery_rider_feedback_prefs"
    private const val KEY_FEEDBACK_JSON = "feedback_by_order_json"
    private const val KEY_CUSTOMER_RATINGS_JSON = "customer_ratings_by_order_json"
    private const val KEY_SUGGESTIONS_JSON = "app_suggestions_json"
    private const val KEY_TIPS_JSON = "tips_by_order_json"

    private val _feedbackByOrder = MutableStateFlow<Map<String, RiderDeliveryFeedback>>(emptyMap())
    val feedbackByOrder: StateFlow<Map<String, RiderDeliveryFeedback>> = _feedbackByOrder.asStateFlow()

    private val _customerRatingsByOrder = MutableStateFlow<Map<String, CustomerRatingByRider>>(emptyMap())
    val customerRatingsByOrder: StateFlow<Map<String, CustomerRatingByRider>> = _customerRatingsByOrder.asStateFlow()

    private val _suggestions = MutableStateFlow<List<AppSuggestionEntry>>(emptyList())
    val suggestions: StateFlow<List<AppSuggestionEntry>> = _suggestions.asStateFlow()

    private val _tipsByOrder = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tipsByOrder: StateFlow<Map<String, Int>> = _tipsByOrder.asStateFlow()

    val availableComplimentTags = listOf(
        "Fast Delivery",
        "Polite & Friendly",
        "Safe Parcel Handling",
        "On-Time Arrival",
        "Followed Instructions"
    )

    val availableCustomerBehaviorTags = listOf(
        "Polite Customer",
        "Parcel Ready on Time",
        "Accurate Address",
        "Quick Handover",
        "Respectful Communication"
    )

    val suggestionCategories = listOf(
        "App Feature",
        "Delivery & Route",
        "Payout & Pricing",
        "Safety & Support"
    )

    val quickTipOptionsInr = listOf(0, 10, 20, 30, 50)

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var isLoaded = false

    fun ensureLoaded(context: Context) {
        if (isLoaded) return
        isLoaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (_tipsByOrder.value.isEmpty()) {
            val rawTips = prefs.getString(KEY_TIPS_JSON, null)
            if (rawTips != null) {
                runCatching {
                    val root = JSONObject(rawTips)
                    val parsedTips = mutableMapOf<String, Int>()
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val orderId = keys.next()
                        parsedTips[orderId] = root.optInt(orderId, 0).coerceAtLeast(0)
                    }
                    _tipsByOrder.value = parsedTips
                }
            }
        }
        if (_feedbackByOrder.value.isEmpty()) {
            val raw = prefs.getString(KEY_FEEDBACK_JSON, null)
            if (raw != null) {
                runCatching {
                    val root = JSONObject(raw)
                    val parsed = mutableMapOf<String, RiderDeliveryFeedback>()
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val orderId = keys.next()
                        val obj = root.getJSONObject(orderId)
                        val tagsJson = obj.optJSONArray("complimentTags") ?: JSONArray()
                        val tags = mutableListOf<String>()
                        for (i in 0 until tagsJson.length()) {
                            tags.add(tagsJson.getString(i))
                        }
                        val tipInr = obj.optInt("tipAmountInr", _tipsByOrder.value[orderId] ?: 0)
                        parsed[orderId] = RiderDeliveryFeedback(
                            orderId = orderId,
                            riderId = obj.optString("riderId", "RIDER-101"),
                            riderName = obj.optString("riderName", "Vikram Singh"),
                            customerName = obj.optString("customerName", "Customer"),
                            starRating = obj.optInt("starRating", 5).coerceIn(1, 5),
                            complimentTags = tags,
                            comment = obj.optString("comment", ""),
                            submittedAt = obj.optString("submittedAt", ""),
                            tipAmountInr = tipInr
                        )
                    }
                    _feedbackByOrder.value = parsed
                }
            }
        }
        if (_customerRatingsByOrder.value.isEmpty()) {
            val rawCust = prefs.getString(KEY_CUSTOMER_RATINGS_JSON, null)
            if (rawCust != null) {
                runCatching {
                    val root = JSONObject(rawCust)
                    val parsed = mutableMapOf<String, CustomerRatingByRider>()
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val orderId = keys.next()
                        val obj = root.getJSONObject(orderId)
                        val tagsJson = obj.optJSONArray("behaviorTags") ?: JSONArray()
                        val tags = mutableListOf<String>()
                        for (i in 0 until tagsJson.length()) {
                            tags.add(tagsJson.getString(i))
                        }
                        parsed[orderId] = CustomerRatingByRider(
                            orderId = orderId,
                            riderId = obj.optString("riderId", "RIDER-101"),
                            riderName = obj.optString("riderName", "Vikram Singh"),
                            customerName = obj.optString("customerName", "Customer"),
                            starRating = obj.optInt("starRating", 5).coerceIn(1, 5),
                            behaviorTags = tags,
                            comment = obj.optString("comment", ""),
                            submittedAt = obj.optString("submittedAt", "")
                        )
                    }
                    _customerRatingsByOrder.value = parsed
                }
            }
        }
        if (_suggestions.value.isEmpty()) {
            val rawSug = prefs.getString(KEY_SUGGESTIONS_JSON, null)
            if (rawSug != null) {
                runCatching {
                    val arr = JSONArray(rawSug)
                    val parsed = mutableListOf<AppSuggestionEntry>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        parsed.add(
                            AppSuggestionEntry(
                                suggestionId = obj.optString("suggestionId", "SUG-$i"),
                                senderRole = obj.optString("senderRole", "CUSTOMER"),
                                senderName = obj.optString("senderName", "User"),
                                category = obj.optString("category", "App Feature"),
                                message = obj.optString("message", ""),
                                submittedAt = obj.optString("submittedAt", "")
                            )
                        )
                    }
                    _suggestions.value = parsed
                }
            }
        }
    }

    fun recordOrderTip(
        context: Context,
        orderId: String,
        tipAmountInr: Int
    ) {
        ensureLoaded(context)
        val sanitizedTip = tipAmountInr.coerceAtLeast(0)
        val updatedTips = _tipsByOrder.value.toMutableMap().apply {
            put(orderId, sanitizedTip)
        }
        _tipsByOrder.value = updatedTips
        persistTips(context, updatedTips)

        val existingFeedback = _feedbackByOrder.value[orderId]
        if (existingFeedback != null && existingFeedback.tipAmountInr != sanitizedTip) {
            val updatedFeedback = _feedbackByOrder.value.toMutableMap().apply {
                put(orderId, existingFeedback.copy(tipAmountInr = sanitizedTip))
            }
            _feedbackByOrder.value = updatedFeedback
            persist(context, updatedFeedback)
        }
    }

    fun getTipForOrder(orderId: String): Int {
        return _tipsByOrder.value[orderId] ?: _feedbackByOrder.value[orderId]?.tipAmountInr ?: 0
    }

    fun submitFeedback(
        context: Context,
        orderId: String,
        riderId: String,
        riderName: String,
        customerName: String,
        starRating: Int,
        complimentTags: List<String>,
        comment: String,
        tipAmountInr: Int = getTipForOrder(orderId)
    ): RiderDeliveryFeedback {
        ensureLoaded(context)
        val sanitizedTip = tipAmountInr.coerceAtLeast(0)
        val timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val entry = RiderDeliveryFeedback(
            orderId = orderId,
            riderId = riderId,
            riderName = riderName,
            customerName = customerName,
            starRating = starRating.coerceIn(1, 5),
            complimentTags = complimentTags,
            comment = comment.trim(),
            submittedAt = timestamp,
            tipAmountInr = sanitizedTip
        )
        val updated = _feedbackByOrder.value.toMutableMap().apply {
            put(orderId, entry)
        }
        _feedbackByOrder.value = updated
        persist(context, updated)

        val updatedTips = _tipsByOrder.value.toMutableMap().apply {
            put(orderId, sanitizedTip)
        }
        _tipsByOrder.value = updatedTips
        persistTips(context, updatedTips)
        return entry
    }

    fun submitCustomerRatingByRider(
        context: Context,
        orderId: String,
        riderId: String,
        riderName: String,
        customerName: String,
        starRating: Int,
        behaviorTags: List<String>,
        comment: String
    ): CustomerRatingByRider {
        ensureLoaded(context)
        val timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val entry = CustomerRatingByRider(
            orderId = orderId,
            riderId = riderId,
            riderName = riderName,
            customerName = customerName,
            starRating = starRating.coerceIn(1, 5),
            behaviorTags = behaviorTags,
            comment = comment.trim(),
            submittedAt = timestamp
        )
        val updated = _customerRatingsByOrder.value.toMutableMap().apply {
            put(orderId, entry)
        }
        _customerRatingsByOrder.value = updated
        persistCustomerRatings(context, updated)
        return entry
    }

    fun submitSuggestion(
        context: Context,
        senderRole: String,
        senderName: String,
        category: String,
        message: String
    ): AppSuggestionEntry {
        ensureLoaded(context)
        val timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val entry = AppSuggestionEntry(
            suggestionId = "SUG-${(System.currentTimeMillis() % 90000) + 10000}",
            senderRole = senderRole,
            senderName = senderName,
            category = category,
            message = message.trim(),
            submittedAt = timestamp
        )
        val updated = listOf(entry) + _suggestions.value
        _suggestions.value = updated
        persistSuggestions(context, updated)
        return entry
    }

    fun averageRatingForRider(riderId: String): Double {
        val list = _feedbackByOrder.value.values.filter { it.riderId == riderId || riderId.isBlank() }
        if (list.isEmpty()) return 4.9
        return list.map { it.starRating }.average()
    }

    private fun persistTips(context: Context, map: Map<String, Int>) {
        val appCtx = context.applicationContext
        ioScope.launch {
            runCatching {
                val root = JSONObject()
                map.forEach { (orderId, tip) ->
                    root.put(orderId, tip)
                }
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_TIPS_JSON, root.toString())
                    .apply()
            }
        }
    }

    private fun persist(context: Context, map: Map<String, RiderDeliveryFeedback>) {
        val appCtx = context.applicationContext
        ioScope.launch {
            runCatching {
                val root = JSONObject()
                map.forEach { (orderId, item) ->
                    val obj = JSONObject().apply {
                        put("orderId", item.orderId)
                        put("riderId", item.riderId)
                        put("riderName", item.riderName)
                        put("customerName", item.customerName)
                        put("starRating", item.starRating)
                        put("complimentTags", JSONArray(item.complimentTags))
                        put("comment", item.comment)
                        put("submittedAt", item.submittedAt)
                        put("tipAmountInr", item.tipAmountInr)
                    }
                    root.put(orderId, obj)
                }
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_FEEDBACK_JSON, root.toString())
                    .apply()
            }
        }
    }

    private fun persistCustomerRatings(context: Context, map: Map<String, CustomerRatingByRider>) {
        val appCtx = context.applicationContext
        ioScope.launch {
            runCatching {
                val root = JSONObject()
                map.forEach { (orderId, item) ->
                    val obj = JSONObject().apply {
                        put("orderId", item.orderId)
                        put("riderId", item.riderId)
                        put("riderName", item.riderName)
                        put("customerName", item.customerName)
                        put("starRating", item.starRating)
                        put("behaviorTags", JSONArray(item.behaviorTags))
                        put("comment", item.comment)
                        put("submittedAt", item.submittedAt)
                    }
                    root.put(orderId, obj)
                }
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CUSTOMER_RATINGS_JSON, root.toString())
                    .apply()
            }
        }
    }

    private fun persistSuggestions(context: Context, list: List<AppSuggestionEntry>) {
        val appCtx = context.applicationContext
        ioScope.launch {
            runCatching {
                val arr = JSONArray()
                list.take(50).forEach { item ->
                    arr.put(
                        JSONObject().apply {
                            put("suggestionId", item.suggestionId)
                            put("senderRole", item.senderRole)
                            put("senderName", item.senderName)
                            put("category", item.category)
                            put("message", item.message)
                            put("submittedAt", item.submittedAt)
                        }
                    )
                }
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SUGGESTIONS_JSON, arr.toString())
                    .apply()
            }
        }
    }
}

private val StarGold = Color(0xFFF59E0B)
private val FeedbackTeal = Color(0xFF00A896)
private val FeedbackNavy = Color(0xFF0D1B2A)

/**
 * Customer -> Rider Star Rating, Comment & Tip Section.
 * IMPORTANT: The rating itself is sent privately to the Admin Web / Admin Panel ONLY
 * so Admin can monitor whether the Rider is working properly.
 */
@Composable
fun PostDeliveryRiderFeedbackSection(
    orderId: String,
    riderId: String,
    riderName: String,
    customerName: String,
    onFeedbackSubmitted: ((RiderDeliveryFeedback) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { RiderFeedbackManager.ensureLoaded(context) }

    val feedbackMap by RiderFeedbackManager.feedbackByOrder.collectAsState()
    val tipsMap by RiderFeedbackManager.tipsByOrder.collectAsState()
    val existingFeedback = feedbackMap[orderId]
    val currentOrderTip = tipsMap[orderId] ?: existingFeedback?.tipAmountInr ?: 0

    var isEditing by remember(existingFeedback == null) { mutableStateOf(existingFeedback == null) }
    var selectedStars by remember(existingFeedback) { mutableIntStateOf(existingFeedback?.starRating ?: 5) }
    var selectedTags by remember(existingFeedback) {
        mutableStateOf(existingFeedback?.complimentTags?.toSet() ?: setOf("Fast Delivery", "Polite & Friendly"))
    }
    var commentText by remember(existingFeedback) { mutableStateOf(existingFeedback?.comment ?: "") }
    var selectedTipInr by remember(existingFeedback, currentOrderTip) {
        mutableIntStateOf(currentOrderTip)
    }

    val ratingDescription = when (selectedStars) {
        1 -> "1 Star — Needs Improvement (Sent to Admin)"
        2 -> "2 Stars — Below Average (Sent to Admin)"
        3 -> "3 Stars — Good Delivery (Sent to Admin)"
        4 -> "4 Stars — Very Good Experience (Sent to Admin)"
        else -> "5 Stars — Excellent Rider Service! (Sent to Admin)"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, StarGold.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_delivery_feedback_section_$orderId")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isEditing && existingFeedback != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Rating Submitted",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Rider Rating & Feedback Submitted (${existingFeedback.starRating}/5 ★)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FeedbackNavy
                            )
                            Text(
                                text = "Rider: $riderName • Synced with Admin Quality Panel (${existingFeedback.submittedAt})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.testTag("edit_rider_feedback_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Update Rating", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Star Row & Feedback Summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (star in 1..5) {
                        Icon(
                            imageVector = if (star <= existingFeedback.starRating) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (star <= existingFeedback.starRating) StarGold else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (existingFeedback.complimentTags.isNotEmpty()) {
                        Text(
                            text = existingFeedback.complimentTags.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = FeedbackTeal
                        )
                    }
                }

                if (existingFeedback.comment.isNotBlank()) {
                    Text(
                        text = "\"${existingFeedback.comment}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = FeedbackNavy
                    )
                }

                if (currentOrderTip > 0) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StarGold)
                    ) {
                        Text(
                            text = "💝 Tip Credited to Rider: ₹$currentOrderTip",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Interactive Star Rating, Tip & Feedback Form after Delivery Completion
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rate Rider $riderName & Delivery Feedback",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = FeedbackNavy
                    )
                }
                Text(
                    text = "Share your rating and feedback for this completed delivery (synced to Admin Quality Panel).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )

                // 5-Star Interactive Selector (48dp accessible touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val isSelected = star <= selectedStars
                        IconButton(
                            onClick = { selectedStars = star },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("rider_star_rating_$star")
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Rate $star stars",
                                tint = if (isSelected) StarGold else Color(0xFF94A3B8),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Text(
                    text = ratingDescription,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                HorizontalDivider()

                // Quick Compliment Tags
                Text(
                    text = "Rider Performance Tags (Sent to Admin):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FeedbackNavy
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RiderFeedbackManager.availableComplimentTags.forEach { tag ->
                        val selected = selectedTags.contains(tag)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTags = if (selected) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("feedback_tag_${tag.replace(" ", "_")}")
                        )
                    }
                }

                // Rider Tip Selector (100% goes to Rider)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Redeem,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add a Tip for $riderName (100% goes to Rider):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FeedbackNavy
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RiderFeedbackManager.quickTipOptionsInr.forEach { tipOption ->
                        val isSelected = selectedTipInr == tipOption
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTipInr = tipOption
                                RiderFeedbackManager.recordOrderTip(context, orderId, tipOption)
                            },
                            label = {
                                Text(
                                    text = if (tipOption == 0) "No Tip" else "+₹$tipOption",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("customer_feedback_tip_chip_$tipOption")
                        )
                    }
                }

                // Written Comment Box
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Note for Admin about Rider performance (optional)") },
                    placeholder = { Text("e.g., Rider delivered safely and on time!") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_feedback_comment_input")
                )

                Button(
                    onClick = {
                        val saved = RiderFeedbackManager.submitFeedback(
                            context = context,
                            orderId = orderId,
                            riderId = riderId,
                            riderName = riderName,
                            customerName = customerName,
                            starRating = selectedStars,
                            complimentTags = selectedTags.toList(),
                            comment = commentText.ifBlank {
                                if (selectedTags.isNotEmpty()) {
                                    selectedTags.joinToString(", ")
                                } else {
                                    "Good delivery service"
                                }
                            },
                            tipAmountInr = selectedTipInr
                        )
                        isEditing = false
                        onFeedbackSubmitted?.invoke(saved)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FeedbackTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_rider_feedback_btn")
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedTipInr > 0) {
                            "Send Rating to Admin & ₹$selectedTipInr Tip"
                        } else {
                            "Send $selectedStars-Star Rating to Admin"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Rider -> Customer Star Rating & Comment Section inside the Rider App.
 * Strictly visible ONLY in the Admin Web / Admin Panel!
 */
@Composable
fun RiderRateCustomerSection(
    orderId: String,
    riderId: String,
    riderName: String,
    customerName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { RiderFeedbackManager.ensureLoaded(context) }

    val customerRatingsMap by RiderFeedbackManager.customerRatingsByOrder.collectAsState()
    val existingRating = customerRatingsMap[orderId]

    var isEditing by remember(existingRating == null) { mutableStateOf(existingRating == null) }
    var selectedStars by remember(existingRating) { mutableIntStateOf(existingRating?.starRating ?: 5) }
    var selectedTags by remember(existingRating) {
        mutableStateOf(existingRating?.behaviorTags?.toSet() ?: setOf("Polite Customer", "Parcel Ready on Time"))
    }
    var commentText by remember(existingRating) { mutableStateOf(existingRating?.comment ?: "") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rider_rate_customer_section_$orderId")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isEditing && existingRating != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Customer Rating Sent to Admin Web Panel",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FeedbackNavy
                            )
                            Text(
                                text = "🔒 Visible only to Admin (${existingRating.submittedAt})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.testTag("rider_edit_customer_rating_btn_$orderId")
                    ) {
                        Text("Edit", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rate Customer $customerName (Visible Only in Admin Web)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = FeedbackNavy
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val isSelected = star <= selectedStars
                        IconButton(
                            onClick = { selectedStars = star },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("rider_to_customer_star_${orderId}_$star")
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Rate Customer $star stars",
                                tint = if (isSelected) StarGold else Color(0xFF94A3B8),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RiderFeedbackManager.availableCustomerBehaviorTags.forEach { tag ->
                        val selected = selectedTags.contains(tag)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTags = if (selected) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Note for Admin about Customer (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_to_customer_comment_$orderId")
                )

                Button(
                    onClick = {
                        RiderFeedbackManager.submitCustomerRatingByRider(
                            context = context,
                            orderId = orderId,
                            riderId = riderId,
                            riderName = riderName,
                            customerName = customerName,
                            starRating = selectedStars,
                            behaviorTags = selectedTags.toList(),
                            comment = commentText.ifBlank { selectedTags.joinToString(", ") }
                        )
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_submit_customer_rating_btn_$orderId")
                ) {
                    Text(
                        text = "Send $selectedStars-Star Customer Rating to Admin",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Suggestion Box Card for both Customer App and Rider App.
 * Submissions are synced to the Admin Web / Admin Panel.
 */
@Composable
fun QuickDeliverySuggestionBoxCard(
    appRole: String, // "CUSTOMER" or "RIDER"
    senderName: String,
    accentColor: Color = FeedbackTeal,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { RiderFeedbackManager.ensureLoaded(context) }

    val allSuggestions by RiderFeedbackManager.suggestions.collectAsState()
    val roleSuggestionsCount = remember(allSuggestions, appRole) {
        allSuggestions.count { it.senderRole.equals(appRole, ignoreCase = true) }
    }

    var selectedCategory by remember { mutableStateOf(RiderFeedbackManager.suggestionCategories.first()) }
    var suggestionText by remember { mutableStateOf("") }
    var confirmationNote by remember { mutableStateOf<String?>(null) }

    val roleKey = appRole.lowercase(Locale.US)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("suggestion_box_card_$roleKey")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Suggestion Box",
                        tint = StarGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${if (appRole == "RIDER") "Rider" else "Customer"} Suggestion Box",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = FeedbackNavy
                        )
                        Text(
                            text = "Share ideas or feedback directly with the Admin Team",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                if (roleSuggestionsCount > 0) {
                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Sent: $roleSuggestionsCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiderFeedbackManager.suggestionCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = suggestionText,
                onValueChange = {
                    suggestionText = it
                    confirmationNote = null
                },
                label = { Text("Write your suggestion or improvement idea...") },
                placeholder = {
                    Text(
                        if (appRole == "RIDER") {
                            "e.g., Add more rest zones or night shift incentives"
                        } else {
                            "e.g., Add scheduled morning parcel pickup option"
                        }
                    )
                },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("suggestion_box_input_$roleKey")
            )

            if (confirmationNote != null) {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = confirmationNote.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val finalMsg = suggestionText.trim().ifBlank {
                        "Suggestion regarding $selectedCategory from $senderName"
                    }
                    val saved = RiderFeedbackManager.submitSuggestion(
                        context = context,
                        senderRole = appRole,
                        senderName = senderName,
                        category = selectedCategory,
                        message = finalMsg
                    )
                    suggestionText = ""
                    confirmationNote = "✓ Suggestion #${saved.suggestionId} sent to Admin Web Panel!"
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("suggestion_box_submit_btn_$roleKey")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Submit Suggestion to Admin",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Displays ONLY Customer Tips inside the Rider App for a delivered order.
 * Star ratings and review comments are strictly hidden here and visible ONLY in the Admin Web Panel!
 */
@Composable
fun RiderReceivedFeedbackCard(
    orderId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { RiderFeedbackManager.ensureLoaded(context) }

    val feedbackMap by RiderFeedbackManager.feedbackByOrder.collectAsState()
    val tipsMap by RiderFeedbackManager.tipsByOrder.collectAsState()
    val feedback = feedbackMap[orderId]
    val tipAmount = tipsMap[orderId] ?: feedback?.tipAmountInr ?: 0

    if (tipAmount <= 0) return

    Surface(
        color = Color(0xFFFFFBEB),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, StarGold),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rider_received_feedback_card_$orderId")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💝 Customer Tip Received (100% Yours)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = FeedbackNavy
            )
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, StarGold)
            ) {
                Text(
                    text = "+₹$tipAmount Tip",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF92400E),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Admin Web / Admin Panel Exclusive View:
 * Shows all confidential Customer -> Rider Ratings (so Admin can verify if the Rider is working properly),
 * Rider -> Customer Ratings, and Customer/Rider Suggestion Box submissions!
 */
@Composable
fun AdminMutualRatingsAndSuggestionsPanel(
    initialExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { RiderFeedbackManager.ensureLoaded(context) }

    val riderRatings by RiderFeedbackManager.feedbackByOrder.collectAsState()
    val customerRatings by RiderFeedbackManager.customerRatingsByOrder.collectAsState()
    val suggestions by RiderFeedbackManager.suggestions.collectAsState()

    var expandedSection by remember(initialExpanded) { mutableStateOf(initialExpanded) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, FeedbackTeal),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("admin_web_ratings_and_suggestions_panel")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Web Ratings & Suggestions",
                        tint = FeedbackTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Admin Web — Confidential Ratings & Suggestion Box",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = FeedbackNavy
                        )
                        Text(
                            text = "Rider Ratings (${riderRatings.size}) • Customer Ratings (${customerRatings.size}) • Suggestions (${suggestions.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569)
                        )
                    }
                }
                OutlinedButton(
                    onClick = { expandedSection = !expandedSection },
                    modifier = Modifier.testTag("admin_toggle_ratings_panel_btn")
                ) {
                    Text(if (expandedSection) "Hide ▲" else "View ▼", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (expandedSection) {
                HorizontalDivider()

                // 1. Customer -> Rider Ratings (Helps Admin know if Rider is working properly)
                Text(
                    text = "1. Customer → Rider Ratings (Admin-Only Rider Quality Audit)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FeedbackNavy
                )
                if (riderRatings.isEmpty()) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Default Audit Status: Rider Vikram Singh (★ 4.9/5.0) — Working Properly. New customer ratings will appear exclusively here for Admin review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    riderRatings.values.forEach { item ->
                        val isWorkingWell = item.starRating >= 4
                        Surface(
                            color = if (isWorkingWell) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isWorkingWell) Color(0xFF10B981) else Color(0xFFEF4444)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rider: ${item.riderName} (Order #${item.orderId}) • ★ ${item.starRating}/5",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FeedbackNavy
                                    )
                                    Text(
                                        text = if (isWorkingWell) "✓ Working Properly" else "⚠ Needs Admin Review",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWorkingWell) Color(0xFF065F46) else Color(0xFFB91C1C)
                                    )
                                }
                                Text(
                                    text = "Rated by Customer ${item.customerName}: \"${item.comment}\" (${item.complimentTags.joinToString(", ")})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }

                // 2. Rider -> Customer Ratings
                Text(
                    text = "2. Rider → Customer Ratings (Admin-Only)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FeedbackNavy
                )
                if (customerRatings.isEmpty()) {
                    Text(
                        text = "No Rider → Customer ratings submitted yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                } else {
                    customerRatings.values.forEach { item ->
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StarGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Customer: ${item.customerName} (Order #${item.orderId}) • ★ ${item.starRating}/5",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FeedbackNavy
                                )
                                Text(
                                    text = "Rated by Rider ${item.riderName}: \"${item.comment}\" (${item.behaviorTags.joinToString(", ")})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }

                // 3. Suggestion Box Feed (from Rider & Customer Apps)
                Text(
                    text = "3. Rider & Customer Suggestion Box Feed",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FeedbackNavy
                )
                if (suggestions.isEmpty()) {
                    Text(
                        text = "Suggestions submitted from the Customer App and Rider App Suggestion Boxes will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                } else {
                    suggestions.take(10).forEach { sug ->
                        Surface(
                            color = Color(0xFFF0FDFA),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FeedbackTeal.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "[${sug.senderRole}] ${sug.senderName} • ${sug.category} (${sug.submittedAt})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FeedbackNavy
                                )
                                Text(
                                    text = sug.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF0F766E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
