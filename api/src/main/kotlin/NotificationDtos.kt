package com.example

import kotlinx.serialization.Serializable

// Request and response bodies for the notification endpoints (SCRUM-161).

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val platform: String = "android",
) {
    fun validate() {
        if (token.isBlank()) throw ValidationException("token is required")
        // FCM tokens are a few hundred characters. Anything far longer is not one.
        if (token.length > MAX_TOKEN_LENGTH) throw ValidationException("token is too long")
        if (platform !in PLATFORMS) throw ValidationException("platform must be android")
    }

    companion object {
        const val MAX_TOKEN_LENGTH = 4096
        val PLATFORMS = setOf("android")
    }
}

@Serializable
data class TestNotificationRequest(
    val type: String = NotificationService.TYPE_GENERAL,
) {
    fun validate() {
        if (type !in NotificationService.TYPES) {
            throw ValidationException("type must be one of ${NotificationService.TYPES.joinToString()}")
        }
    }

    fun toPush(): PushNotification = PushNotification(
        type = type,
        title = "Runway test notification",
        body = "Notifications are working on this device.",
    )
}

// How a send went across all of the user's devices.
@Serializable
data class SendReportResponse(
    val sent: Int,
    val removed: Int,
    val failed: Int,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
