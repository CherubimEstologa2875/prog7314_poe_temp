package com.example.runway.domain.model

// What kind of alert a push carries, and the rules for whether to show it (SCRUM-162).
// Kept free of Android classes so the rules can be unit tested on the JVM.

enum class NotificationType(val wireName: String) {
    GENERAL("general"),
    WASH("wash"),
    SWAP("swap"),
    WEATHER("weather");

    /** Each type has its own channel, so the user can also mute one in Android's settings. */
    val channelId: String get() = wireName

    companion object {
        /** A type this version of the app does not know still arrives, as a general alert. */
        fun fromWire(value: String?): NotificationType =
            entries.firstOrNull { it.wireName == value } ?: GENERAL
    }
}

/** What the app does with one incoming push. */
enum class DeliveryDecision {
    /** Show it as a system notification and keep it in the in-app list. */
    SHOW,

    /** Android notifications are blocked. Keep it in the in-app list only, so nothing is lost. */
    IN_APP_ONLY,

    /** The user turned this kind of alert off in Runway's settings. Drop it. */
    TURNED_OFF,
}

object NotificationRules {

    fun isEnabled(type: NotificationType, settings: RunwaySettings): Boolean = when (type) {
        NotificationType.WASH -> settings.notifyWash
        NotificationType.SWAP -> settings.notifySwap
        NotificationType.WEATHER -> settings.notifyWeather
        // Account and test messages cannot be turned off.
        NotificationType.GENERAL -> true
    }

    fun decide(
        type: NotificationType,
        settings: RunwaySettings,
        systemNotificationsAllowed: Boolean,
    ): DeliveryDecision = when {
        !isEnabled(type, settings) -> DeliveryDecision.TURNED_OFF
        !systemNotificationsAllowed -> DeliveryDecision.IN_APP_ONLY
        else -> DeliveryDecision.SHOW
    }
}

/** One alert in the in-app list on the Notifications screen. */
data class InboxNotification(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val receivedAtMillis: Long,
)
