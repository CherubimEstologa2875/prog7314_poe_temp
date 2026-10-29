package com.example.runway.data.notifications

import android.util.Log
import com.example.runway.data.remote.api.DeviceTokenDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.SendReportDto
import com.example.runway.data.remote.api.TestNotificationDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

// Keeps the API's copy of this device's FCM token up to date (SCRUM-161).
// FCM can replace the token at any time, so it is sent on every sign-in, every launch
// and whenever FCM hands over a new one (Firebase, 2026).

// getToken() is deprecated from firebase-messaging 25.1.0 in favour of registration by
// Firebase installation ID. It still works, and the move is left until Firebase documents it.
@Suppress("DEPRECATION")
class PushTokenRepository(
    private val api: RunwayApi,
    private val isSignedIn: () -> Boolean = { FirebaseAuth.getInstance().currentUser != null },
    private val fetchToken: suspend () -> String = { FirebaseMessaging.getInstance().token.await() },
) {

    /**
     * Sends the token to the API. Pass the token when FCM has just handed it over;
     * otherwise it is fetched. Does nothing while signed out - the API needs to know
     * whose device it is, and signing in calls this again.
     */
    suspend fun register(token: String? = null): Result<Unit> {
        if (!isSignedIn()) return Result.success(Unit)
        return runCatching {
            val value = token ?: fetchToken()
            api.registerDeviceToken(DeviceTokenDto(token = value))
        }.onSuccess {
            Log.d(TAG, "device registered for push")
        }.onFailure {
            // Not fatal: the next launch or sign-in tries again.
            Log.w(TAG, "could not register for push: ${it.message}")
        }
    }

    /**
     * Stops pushes to this device. Must run before Firebase sign-out, because the API
     * call needs the user's ID token. Waits a few seconds at most, so sign-out still
     * works offline. If the call fails, nothing leaks: the app ignores pushes while
     * signed out, and the next sign-in on this phone moves the token to that user.
     *
     * The local token is deliberately kept. Deleting it makes FCM issue a new one,
     * which could be registered to the old user in the moment before sign-out finishes.
     */
    suspend fun unregister(): Boolean {
        val removed = withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
            runCatching {
                api.removeDeviceToken(DeviceTokenDto(token = fetchToken()))
            }.isSuccess
        } == true
        Log.d(TAG, "token removed from API: $removed")
        return removed
    }

    /** Asks the API to push a test alert to this user's devices. */
    suspend fun sendTest(): Result<SendReportDto> = runCatching {
        // Register first, so the test also proves this device's token reached the API.
        register().getOrThrow()
        api.sendTestNotification(TestNotificationDto())
    }

    private companion object {
        const val TAG = "PushTokenRepository"
        const val UNREGISTER_TIMEOUT_MS = 3_000L
    }
}

/* Reference List
Firebase, 2026. Best practices for FCM registration token management. [online] Available at: <https://firebase.google.com/docs/cloud-messaging/manage-tokens> [Accessed 29 October 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
