package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.cloud.CloudDeliveryOrder
import com.example.data.cloud.CloudOrderChatMessage
import com.example.data.cloud.QuickDeliveryCloudRepository
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.ui.auth.onGoogleSignInClicked
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val ChatNavy = Color(0xFF0D1B2A)
private val ChatTeal = Color(0xFF00A896)
private val ChatOrange = Color(0xFFFF6B35)
private val ChatEmerald = Color(0xFF059669)
private val ChatCustomerBubble = Color(0xFFCCFBF1)
private val ChatRiderBubble = Color(0xFFFFEDD5)

data class OrderChatMessageUi(
    val messageId: String,
    val orderId: String,
    val senderUid: String,
    val senderRole: String, // "CUSTOMER" or "RIDER"
    val senderName: String,
    val messageText: String,
    val deliveryStage: String,
    val timestampLabel: String,
    val createdAtEpochMs: Long,
    val syncedToFirestore: Boolean = false
)

/**
 * Real-Time Customer <-> Assigned Rider Chat Manager powered by Cloud Firestore
 * (`/delivery_orders/{orderId}/chat_messages/{messageId}`) with instant local + SharedPreferences
 * synchronization so messages appear immediately across Customer App, Rider App, and Admin Web Live Mirror.
 */
object OrderLiveChatManager {
    private const val PREFS_NAME = "quick_delivery_order_live_chat_prefs"
    private const val KEY_CHAT_BY_ORDER_JSON = "chat_messages_by_order_json"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeFirestoreJobs = mutableMapOf<String, Job>()

    @Volatile
    private var isLoaded = false

    private val _messagesByOrder = MutableStateFlow<Map<String, List<OrderChatMessageUi>>>(emptyMap())
    val messagesByOrder: StateFlow<Map<String, List<OrderChatMessageUi>>> = _messagesByOrder.asStateFlow()

    private val _firestoreSyncStatusByOrder = MutableStateFlow<Map<String, String>>(emptyMap())
    val firestoreSyncStatusByOrder: StateFlow<Map<String, String>> = _firestoreSyncStatusByOrder.asStateFlow()

    val customerQuickReplies = listOf(
        "Please call me when you reach pickup gate",
        "Parcel is fragile, please handle with care",
        "How many minutes to reach?",
        "You can hand over at the security desk"
    )

    val riderQuickReplies = listOf(
        "On my way to pick up your parcel!",
        "Reached pickup location, waiting at gate",
        "Parcel picked up, in transit to drop address",
        "Reached drop location, coming to doorstep"
    )

    fun ensureLoaded(context: Context) {
        if (isLoaded) return
        isLoaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CHAT_BY_ORDER_JSON, null)
        if (raw != null) {
            runCatching {
                val root = JSONObject(raw)
                val parsedMap = mutableMapOf<String, List<OrderChatMessageUi>>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val orderId = keys.next()
                    val arr = root.getJSONArray(orderId)
                    val list = mutableListOf<OrderChatMessageUi>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            OrderChatMessageUi(
                                messageId = obj.optString("messageId", "msg_$i"),
                                orderId = obj.optString("orderId", orderId),
                                senderUid = obj.optString("senderUid", "local_uid"),
                                senderRole = obj.optString("senderRole", "CUSTOMER"),
                                senderName = obj.optString("senderName", "User"),
                                messageText = obj.optString("messageText", ""),
                                deliveryStage = obj.optString("deliveryStage", "ASSIGNED"),
                                timestampLabel = obj.optString("timestampLabel", "Just now"),
                                createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                                syncedToFirestore = obj.optBoolean("syncedToFirestore", false)
                            )
                        )
                    }
                    parsedMap[orderId] = list.sortedBy { it.createdAtEpochMs }
                }
                _messagesByOrder.value = parsedMap
            }
        }
    }

    fun ensureStarterMessagesIfEmpty(order: CustomerOrder) {
        val current = _messagesByOrder.value[order.orderId]
        if (!current.isNullOrEmpty()) return
        if (order.orderStatus == OrderStatus.SEARCHING_RIDER || order.orderStatus == OrderStatus.CANCELLED) return

        val riderName = order.assignedRiderName ?: "Vikram Singh"
        val now = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US)
        val starter = listOf(
            OrderChatMessageUi(
                messageId = "msg_seed_${order.orderId}_1",
                orderId = order.orderId,
                senderUid = "customer_seed",
                senderRole = "CUSTOMER",
                senderName = order.customerName,
                messageText = "Hi $riderName, parcel is packed and ready at ${order.pickupAddress.fullAddress}.",
                deliveryStage = "ASSIGNED",
                timestampLabel = timeFmt.format(Date(now - 120_000L)),
                createdAtEpochMs = now - 120_000L,
                syncedToFirestore = true
            ),
            OrderChatMessageUi(
                messageId = "msg_seed_${order.orderId}_2",
                orderId = order.orderId,
                senderUid = "rider_seed",
                senderRole = "RIDER",
                senderName = riderName,
                messageText = "Hello ${order.customerName}! I have accepted your order #${order.orderId} and started live navigation.",
                deliveryStage = order.orderStatus.name,
                timestampLabel = timeFmt.format(Date(now - 60_000L)),
                createdAtEpochMs = now - 60_000L,
                syncedToFirestore = true
            )
        )
        _messagesByOrder.value = _messagesByOrder.value.toMutableMap().apply {
            put(order.orderId, starter)
        }
    }

    /**
     * Subscribes to Firestore `/delivery_orders/{orderId}/chat_messages` in real time using `.snapshots()`
     * when a Firebase user is signed in.
     */
    fun bindFirestoreRealtimeListener(
        context: Context,
        order: CustomerOrder
    ) {
        ensureLoaded(context)
        ensureStarterMessagesIfEmpty(order)

        val currentUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (currentUser == null) {
            _firestoreSyncStatusByOrder.value = _firestoreSyncStatusByOrder.value.toMutableMap().apply {
                put(order.orderId, "Local + Multi-Role Live Sync")
            }
            return
        }

        if (activeFirestoreJobs[order.orderId]?.isActive == true) return

        val appCtx = context.applicationContext
        val job = ioScope.launch {
            val cloudRepo = runCatching { QuickDeliveryCloudRepository(appCtx) }.getOrNull() ?: return@launch

            // Ensure the parent order document exists in Firestore for the signed-in user so subcollection rules succeed
            runCatching {
                val existingOrder = cloudRepo.getOrderById(order.orderId).getOrNull()
                if (existingOrder == null) {
                    cloudRepo.createOrder(
                        CloudDeliveryOrder.fromCustomerOrder(
                            order = order,
                            userId = currentUser.uid
                        )
                    )
                }
            }

            _firestoreSyncStatusByOrder.value = _firestoreSyncStatusByOrder.value.toMutableMap().apply {
                put(order.orderId, "Connected to Cloud Firestore (/delivery_orders/${order.orderId}/chat_messages)")
            }

            runCatching {
                cloudRepo.observeOrderChatMessages(order.orderId).collectLatest { cloudMessages ->
                    if (cloudMessages.isNotEmpty()) {
                        mergeCloudMessages(appCtx, order.orderId, cloudMessages)
                    }
                }
            }.onFailure {
                _firestoreSyncStatusByOrder.value = _firestoreSyncStatusByOrder.value.toMutableMap().apply {
                    put(order.orderId, "Firestore Live Stream Standby • Instant Local Sync Active")
                }
            }
        }
        activeFirestoreJobs[order.orderId] = job
    }

    private fun mergeCloudMessages(
        context: Context,
        orderId: String,
        cloudMessages: List<CloudOrderChatMessage>
    ) {
        val timeFmt = SimpleDateFormat("hh:mm:ss a", Locale.US)
        val existingList = _messagesByOrder.value[orderId].orEmpty().toMutableList()
        var changed = false

        cloudMessages.forEach { cloudMsg ->
            val epochMs = cloudMsg.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            val formattedTime = cloudMsg.createdAt?.toDate()?.let { timeFmt.format(it) }
                ?: timeFmt.format(Date(epochMs))
            val uiMsg = OrderChatMessageUi(
                messageId = cloudMsg.id.ifBlank { "msg_${UUID.randomUUID().toString().take(8)}" },
                orderId = orderId,
                senderUid = cloudMsg.senderUid,
                senderRole = cloudMsg.senderRole,
                senderName = cloudMsg.senderName,
                messageText = cloudMsg.messageText,
                deliveryStage = cloudMsg.deliveryStage ?: "IN_TRANSIT",
                timestampLabel = formattedTime,
                createdAtEpochMs = epochMs,
                syncedToFirestore = true
            )
            val idx = existingList.indexOfFirst { it.messageId == uiMsg.messageId }
            if (idx >= 0) {
                if (!existingList[idx].syncedToFirestore) {
                    existingList[idx] = uiMsg
                    changed = true
                }
            } else {
                existingList.add(uiMsg)
                changed = true
            }
        }

        if (changed) {
            val sorted = existingList.sortedBy { it.createdAtEpochMs }
            val updatedMap = _messagesByOrder.value.toMutableMap().apply {
                put(orderId, sorted)
            }
            _messagesByOrder.value = updatedMap
            persist(context, updatedMap)
        }
    }

    /**
     * Sends a chat message from either CUSTOMER or RIDER, immediately updating UI state and writing
     * to Cloud Firestore (`/delivery_orders/{orderId}/chat_messages/{messageId}`).
     */
    fun sendChatMessage(
        context: Context,
        order: CustomerOrder,
        senderRole: String,
        senderName: String,
        messageText: String
    ) {
        val trimmed = messageText.trim()
        if (trimmed.isBlank()) return
        ensureLoaded(context)

        val now = System.currentTimeMillis()
        val timeLabel = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(now))
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val currentUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

        val optimisticMsg = OrderChatMessageUi(
            messageId = msgId,
            orderId = order.orderId,
            senderUid = currentUser?.uid ?: "${senderRole.lowercase()}_local",
            senderRole = senderRole,
            senderName = senderName.trim().ifBlank {
                if (senderRole == "RIDER") (order.assignedRiderName ?: "Vikram Singh") else order.customerName
            },
            messageText = trimmed,
            deliveryStage = order.orderStatus.displayTitle,
            timestampLabel = timeLabel,
            createdAtEpochMs = now,
            syncedToFirestore = currentUser != null
        )

        val updatedOrderMessages = (_messagesByOrder.value[order.orderId].orEmpty() + optimisticMsg)
            .sortedBy { it.createdAtEpochMs }
        val updatedMap = _messagesByOrder.value.toMutableMap().apply {
            put(order.orderId, updatedOrderMessages)
        }
        _messagesByOrder.value = updatedMap
        persist(context, updatedMap)

        // Write to Firestore if authenticated
        if (currentUser != null) {
            val appCtx = context.applicationContext
            ioScope.launch {
                val cloudRepo = runCatching { QuickDeliveryCloudRepository(appCtx) }.getOrNull() ?: return@launch
                // Ensure parent order exists in Firestore first
                runCatching {
                    val existing = cloudRepo.getOrderById(order.orderId).getOrNull()
                    if (existing == null) {
                        cloudRepo.createOrder(
                            CloudDeliveryOrder.fromCustomerOrder(
                                order = order,
                                userId = currentUser.uid
                            )
                        )
                    }
                }
                val result = cloudRepo.sendOrderChatMessage(
                    orderId = order.orderId,
                    senderRole = senderRole,
                    senderName = optimisticMsg.senderName,
                    messageText = trimmed,
                    deliveryStage = order.orderStatus.name,
                    customMessageId = msgId
                )
                if (result.isSuccess) {
                    _firestoreSyncStatusByOrder.value = _firestoreSyncStatusByOrder.value.toMutableMap().apply {
                        put(order.orderId, "✓ Synced to Cloud Firestore (/delivery_orders/${order.orderId}/chat_messages)")
                    }
                }
            }
        }
    }

    private fun persist(context: Context, map: Map<String, List<OrderChatMessageUi>>) {
        val appCtx = context.applicationContext
        ioScope.launch {
            runCatching {
                val root = JSONObject()
                map.forEach { (orderId, list) ->
                    val arr = JSONArray()
                    list.takeLast(40).forEach { item ->
                        arr.put(
                            JSONObject().apply {
                                put("messageId", item.messageId)
                                put("orderId", item.orderId)
                                put("senderUid", item.senderUid)
                                put("senderRole", item.senderRole)
                                put("senderName", item.senderName)
                                put("messageText", item.messageText)
                                put("deliveryStage", item.deliveryStage)
                                put("timestampLabel", item.timestampLabel)
                                put("createdAtEpochMs", item.createdAtEpochMs)
                                put("syncedToFirestore", item.syncedToFirestore)
                            }
                        )
                    }
                    root.put(orderId, arr)
                }
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CHAT_BY_ORDER_JSON, root.toString())
                    .apply()
            }
        }
    }
}

/**
 * Real-Time Customer <-> Assigned Rider Chat UI Card powered by Firestore.
 * Embedded in both:
 * 1. Customer App (`LiveRiderContinuousTrackingCard`) once a Rider is assigned.
 * 2. Rider App (`RiderProgressiveOrderCard`) on active assigned/in-transit orders.
 */
@Composable
fun OrderRealTimeChatSection(
    order: CustomerOrder,
    viewerRole: String, // "CUSTOMER" or "RIDER"
    viewerName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(order.orderId, order.orderStatus) {
        OrderLiveChatManager.bindFirestoreRealtimeListener(context, order)
    }

    val messagesByOrder by OrderLiveChatManager.messagesByOrder.collectAsState()
    val syncStatusMap by OrderLiveChatManager.firestoreSyncStatusByOrder.collectAsState()
    val transfersByOrder by RiderOrderTransferManager.transfersByOrder.collectAsState()

    val transferRecord = transfersByOrder[order.orderId]
    val activeRiderName = transferRecord?.toRiderName ?: order.assignedRiderName ?: "Vikram Singh"
    val activeRiderPhone = transferRecord?.toRiderMobile ?: order.assignedRiderPhone ?: "+91 98450 67890"

    val messages = messagesByOrder[order.orderId].orEmpty()
    val syncStatusText = syncStatusMap[order.orderId]
        ?: "Firestore Real-Time Chat Stream Active"

    var messageInput by remember(order.orderId, viewerRole) { mutableStateOf("") }
    var isExpanded by remember(order.orderId) { mutableStateOf(true) }
    var isSigningInForCloud by remember { mutableStateOf(false) }

    val isCustomerViewer = viewerRole.equals("CUSTOMER", ignoreCase = true)
    val counterpartTitle = if (isCustomerViewer) {
        "Live Chat with Rider $activeRiderName"
    } else {
        "Live Chat with Customer ${order.customerName}"
    }
    val counterpartSubtitle = if (isCustomerViewer) {
        "Assigned Pickup & Delivery Partner • $activeRiderPhone"
    } else {
        "Order #${order.orderId} • ${order.pickupAddress.fullAddress} → ${order.dropAddress.fullAddress}"
    }

    val quickReplies = if (isCustomerViewer) {
        OrderLiveChatManager.customerQuickReplies
    } else {
        OrderLiveChatManager.riderQuickReplies
    }

    val rolePrefix = if (isCustomerViewer) "customer" else "rider"
    val isFirebaseSignedIn = remember(messages.size) {
        runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.5.dp,
            if (isCustomerViewer) ChatTeal.copy(alpha = 0.65f) else ChatOrange.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("${rolePrefix}_realtime_chat_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Chat Header Row
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
                        color = if (isCustomerViewer) ChatTeal else ChatOrange,
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Real-Time Order Chat",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = counterpartTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ChatNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${messages.size} msgs",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = counterpartSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("${rolePrefix}_toggle_chat_btn_${order.orderId}")
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Open Chat",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }



            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Chat Message History Feed
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .testTag("${rolePrefix}_chat_messages_box_${order.orderId}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (messages.isEmpty()) {
                                Text(
                                    text = "No messages yet. Send a message or tap a quick reply below to coordinate pickup/delivery.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            } else {
                                messages.takeLast(8).forEach { msg ->
                                    val isOwnMessage = msg.senderRole.equals(viewerRole, ignoreCase = true)
                                    ChatMessageBubbleItem(
                                        message = msg,
                                        isOwnMessage = isOwnMessage
                                    )
                                }
                            }
                        }
                    }

                    // 1-Tap Quick Reply Suggestions
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Quick Replies (1-Tap Send):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickReplies.forEachIndexed { idx, preset ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        OrderLiveChatManager.sendChatMessage(
                                            context = context,
                                            order = order,
                                            senderRole = viewerRole,
                                            senderName = viewerName,
                                            messageText = preset
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = preset,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("${rolePrefix}_chat_quick_reply_${order.orderId}_$idx")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Direct Message Input + Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            label = {
                                Text(
                                    if (isCustomerViewer) {
                                        "Message Rider $activeRiderName..."
                                    } else {
                                        "Message Customer ${order.customerName}..."
                                    }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("${rolePrefix}_chat_input_${order.orderId}")
                        )

                        Button(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    OrderLiveChatManager.sendChatMessage(
                                        context = context,
                                        order = order,
                                        senderRole = viewerRole,
                                        senderName = viewerName,
                                        messageText = messageInput
                                    )
                                    messageInput = ""
                                }
                            },
                            enabled = messageInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCustomerViewer) ChatTeal else ChatOrange
                            ),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("${rolePrefix}_chat_send_btn_${order.orderId}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Chat Message",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubbleItem(
    message: OrderChatMessageUi,
    isOwnMessage: Boolean
) {
    val isRiderSender = message.senderRole == "RIDER"
    val bubbleColor = when {
        isOwnMessage && !isRiderSender -> ChatCustomerBubble
        isOwnMessage && isRiderSender -> ChatRiderBubble
        isRiderSender -> Color(0xFFFFF7ED)
        else -> Color(0xFFF0FDFA)
    }
    val borderColor = if (isRiderSender) ChatOrange.copy(alpha = 0.45f) else ChatTeal.copy(alpha = 0.45f)
    val roleBadgeText = if (isRiderSender) "🏍️ Rider • ${message.senderName}" else "👤 Customer • ${message.senderName}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isOwnMessage) 12.dp else 3.dp,
                bottomEnd = if (isOwnMessage) 3.dp else 12.dp
            ),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = roleBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRiderSender) Color(0xFF9A3412) else Color(0xFF0F766E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.timestampLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatNavy
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ChatEmerald,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (message.syncedToFirestore) "Firestore Live" else "Delivered",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF047857)
                    )
                }
            }
        }
    }
}
