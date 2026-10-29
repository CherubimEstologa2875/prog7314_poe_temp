package com.example.runway.data.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.runway.MainActivity
import com.example.runway.R
import com.example.runway.domain.model.DeliveryDecision
import com.example.runway.domain.model.NotificationRules
import com.example.runway.domain.model.NotificationType
import com.example.runway.domain.repository.SettingsRepository

// Turns an incoming push into what the user sees (SCRUM-162): a system notification that
// opens the Notifications screen when tapped, plus an entry in the in-app list
// (Android Developers, 2026c).

class RunwayNotifier(
    context: Context,
    private val settings: SettingsRepository,
    private val inbox: NotificationInbox,
) {
    private val appContext = context.applicationContext

    fun handle(typeName: String?, title: String?, body: String?): DeliveryDecision {
        val type = NotificationType.fromWire(typeName)
        val decision = NotificationRules.decide(
            type = type,
            settings = settings.currentSettings(),
            systemNotificationsAllowed = NotificationPermission.isAllowed(appContext),
        )
        Log.d(TAG, "push '${type.wireName}' -> $decision")
        if (decision == DeliveryDecision.TURNED_OFF) return decision

        // A push with no text still tells the user something arrived.
        val shownTitle = title?.takeIf { it.isNotBlank() } ?: appContext.getString(R.string.app_name)
        val shownBody = body.orEmpty()

        inbox.add(type, shownTitle, shownBody)
        if (decision == DeliveryDecision.SHOW) post(type, shownTitle, shownBody)
        return decision
    }

    // isAllowed() was checked in handle(), so the permission is held here.
    @SuppressLint("MissingPermission")
    private fun post(type: NotificationType, title: String, body: String) {
        val open = Intent(appContext, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_NOTIFICATIONS
            // Reuse the running activity instead of stacking a second copy.
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            appContext,
            type.ordinal,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(appContext, type.channelId)
            .setSmallIcon(R.drawable.ic_rw_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(if (type == NotificationType.SWAP) NotificationCompat.CATEGORY_SOCIAL else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(nextId(), notification)
        } catch (e: SecurityException) {
            // The permission was revoked between the check and the post. The alert is still in the in-app list.
            Log.w(TAG, "notification permission revoked mid-send: ${e.message}")
        }
    }

    // Unique per alert so a second one does not replace the first.
    private fun nextId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

    private companion object {
        const val TAG = "RunwayNotifier"
    }
}

/* Reference List
Android Developers, 2026c. Create a notification. [online] Available at: <https://developer.android.com/develop/ui/views/notifications/build-notification> [Accessed 29 October 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
