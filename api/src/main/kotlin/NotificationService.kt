package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

// Push notifications for the Runway API (SCRUM-161, SCRUM-163).
// Messages go out through Firebase Cloud Messaging using the Admin SDK (Firebase, 2026a).
// Each message is data-only, so the app decides how to show it whether it is open or not (Firebase, 2026b).

// One push, before it is addressed to a device.
data class PushNotification(
    val type: String,
    val title: String,
    val body: String,
) {
    fun toData(): Map<String, String> = mapOf(
        KEY_TYPE to type,
        KEY_TITLE to title,
        KEY_BODY to body,
    )

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}

// What happened to a single send.
enum class SendOutcome {
    DELIVERED,

    // FCM no longer knows the token - the app was uninstalled or its data cleared.
    DEAD_TOKEN,

    // Anything else: FCM was unreachable, quota, a server error. The token is kept.
    FAILED,
}

// Sends one message to one device. An interface so the tests can fake FCM.
fun interface PushSender {
    suspend fun send(token: String, notification: PushNotification): SendOutcome
}

// Where each user's device tokens are kept. An interface so the tests can use a map.
interface DeviceTokenStore {
    suspend fun tokensFor(uid: String): List<String>
    suspend fun save(uid: String, token: String, platform: String)
    suspend fun delete(uid: String, token: String)
}

class NotificationService(
    store: DeviceTokenStore? = null,
    sender: PushSender? = null,
) {
    // Lazy so the route tests can build the service without Firebase being set up.
    private val store: DeviceTokenStore by lazy { store ?: FirestoreDeviceTokenStore() }
    private val sender: PushSender by lazy { sender ?: FcmPushSender() }

    suspend fun registerToken(uid: String, body: DeviceTokenRequest) {
        body.validate()
        store.save(uid, body.token.trim(), body.platform)
    }

    suspend fun removeToken(uid: String, token: String) {
        if (token.isBlank()) throw ValidationException("token is required")
        store.delete(uid, token.trim())
    }

    // Sends to every device the user has signed in on, and forgets tokens FCM has dropped.
    suspend fun sendToUser(uid: String, notification: PushNotification): SendReportResponse {
        var sent = 0
        var removed = 0
        var failed = 0

        for (token in store.tokensFor(uid)) {
            when (sender.send(token, notification)) {
                SendOutcome.DELIVERED -> sent++
                SendOutcome.DEAD_TOKEN -> {
                    store.delete(uid, token)
                    removed++
                }
                SendOutcome.FAILED -> failed++
            }
        }

        logger.info("push '${notification.type}' to $uid: sent=$sent removed=$removed failed=$failed")
        return SendReportResponse(sent = sent, removed = removed, failed = failed)
    }

    // For pushes triggered by another request, like a wear log. The request does not wait,
    // and a failed push never fails the request that caused it.
    fun sendInBackground(uid: String, notification: PushNotification) {
        backgroundScope.launch {
            runCatching { sendToUser(uid, notification) }
                .onFailure { logger.warn("background push to $uid failed: ${it.message}") }
        }
    }

    // A garment has just reached its wear limit.
    fun washDue(uid: String, itemName: String) = sendInBackground(
        uid,
        PushNotification(
            type = TYPE_WASH,
            title = "Time for a wash",
            body = "$itemName has reached its wear limit.",
        ),
    )

    companion object {
        const val TYPE_GENERAL = "general"
        const val TYPE_WASH = "wash"
        const val TYPE_SWAP = "swap"
        const val TYPE_WEATHER = "weather"
        val TYPES = setOf(TYPE_GENERAL, TYPE_WASH, TYPE_SWAP, TYPE_WEATHER)

        private val logger = LoggerFactory.getLogger(NotificationService::class.java)
        private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

// Tokens live at deviceTokens/{token} with the owner's uid as a field. The token is the
// document id, so a phone has exactly one owner: when someone else signs in on it, saving
// the token moves it to them and the previous user stops getting pushes on that phone.
class FirestoreDeviceTokenStore(firestore: Firestore? = null) : DeviceTokenStore {
    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val tokens get() = db.collection(Collections.DEVICE_TOKENS)

    override suspend fun tokensFor(uid: String): List<String> =
        tokens.whereEqualTo(Fields.OWNER_UID, uid).get().await().documents.map { it.id }

    override suspend fun save(uid: String, token: String, platform: String) {
        tokens.document(token)
            .set(mapOf(Fields.OWNER_UID to uid, "platform" to platform, Fields.UPDATED_AT to Timestamp.now()))
            .await()
    }

    // Only the owner can remove a token, so one user cannot unsubscribe another's phone.
    override suspend fun delete(uid: String, token: String) {
        val ref = tokens.document(token)
        val owner = ref.get().await().getString(Fields.OWNER_UID)
        if (owner == uid) ref.delete().await()
    }
}

class FcmPushSender : PushSender {
    override suspend fun send(token: String, notification: PushNotification): SendOutcome {
        val message = Message.builder()
            .setToken(token)
            .putAllData(notification.toData())
            // High priority wakes the app so it can show the alert even if it was closed.
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build()
            )
            .build()

        return try {
            FirebaseMessaging.getInstance().sendAsync(message).await()
            SendOutcome.DELIVERED
        } catch (e: FirebaseMessagingException) {
            outcomeFor(e.messagingErrorCode)
        }
    }

    companion object {
        // UNREGISTERED: the app was removed. INVALID_ARGUMENT: the token is malformed.
        // SENDER_ID_MISMATCH: the token belongs to another Firebase project.
        // None of these will ever succeed, so the token is dropped (Firebase, 2026c).
        fun outcomeFor(code: MessagingErrorCode?): SendOutcome = when (code) {
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
            MessagingErrorCode.SENDER_ID_MISMATCH -> SendOutcome.DEAD_TOKEN
            else -> SendOutcome.FAILED
        }
    }
}

/* Reference List
Firebase, 2026a. Send messages to specific devices. [online] Available at: <https://firebase.google.com/docs/cloud-messaging/send-message> [Accessed 29 October 2026].
Firebase, 2026b. About FCM messages. [online] Available at: <https://firebase.google.com/docs/cloud-messaging/concept-options> [Accessed 29 October 2026].
Firebase, 2026c. Best practices for FCM registration token management. [online] Available at: <https://firebase.google.com/docs/cloud-messaging/manage-tokens> [Accessed 29 October 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
