package com.example.runway.data.notifications

import android.util.Log
import com.example.runway.RunwayApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

// Receives pushes from Firebase Cloud Messaging (SCRUM-161, SCRUM-162).
// The API sends data-only messages, so onMessageReceived runs whether the app is open,
// in the background or was closed (Firebase, 2026).

class RunwayMessagingService : FirebaseMessagingService() {

    private val container get() = (application as RunwayApplication).container

    // FCM issues a new token on first launch, after the app's data is cleared, and now and then.
    // Deprecated from firebase-messaging 25.1.0 (see PushTokenRepository) but still called.
    @Deprecated("Firebase deprecated onNewToken in firebase-messaging 25.1.0")
    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM issued a new token")
        container.appScope.launch { container.pushTokenRepository.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Signed out: whatever this is was meant for the last person who used the phone.
        if (FirebaseAuth.getInstance().currentUser == null) {
            Log.d(TAG, "ignored a push while signed out")
            return
        }
        val data = message.data
        // A test sent from the Firebase console uses a notification block instead of data.
        val title = data[KEY_TITLE] ?: message.notification?.title
        val body = data[KEY_BODY] ?: message.notification?.body
        container.notifier.handle(data[KEY_TYPE], title, body)
    }

    private companion object {
        const val TAG = "RunwayMessaging"
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}

/* Reference List
Firebase, 2026. Receive messages in an Android app. [online] Available at: <https://firebase.google.com/docs/cloud-messaging/android/receive> [Accessed 29 October 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
