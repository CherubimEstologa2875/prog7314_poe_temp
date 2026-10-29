package com.example.runway.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.runway.R
import com.example.runway.domain.model.NotificationType

// Android 8+ needs a channel for every notification. One per alert type lets the user
// mute, say, weather suggestions in Android's settings without losing swap requests
// (Android Developers, 2026a).

object NotificationChannels {

    /** Safe to call on every launch: creating an existing channel only updates its name. */
    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = NotificationType.entries.map { type ->
            NotificationChannel(type.channelId, context.getString(nameFor(type)), importanceFor(type)).apply {
                description = context.getString(descriptionFor(type))
            }
        }
        manager.createNotificationChannels(channels)
    }

    /** False when the user switched this type's channel off in Android's settings. */
    fun isChannelOn(context: Context, type: NotificationType): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return true
        val channel = manager.getNotificationChannel(type.channelId) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun nameFor(type: NotificationType) = when (type) {
        NotificationType.GENERAL -> R.string.rw_channel_general
        NotificationType.WASH -> R.string.rw_settings_notify_wash
        NotificationType.SWAP -> R.string.rw_settings_notify_swap
        NotificationType.WEATHER -> R.string.rw_settings_notify_weather
    }

    private fun descriptionFor(type: NotificationType) = when (type) {
        NotificationType.GENERAL -> R.string.rw_channel_general_desc
        NotificationType.WASH -> R.string.rw_channel_wash_desc
        NotificationType.SWAP -> R.string.rw_channel_swap_desc
        NotificationType.WEATHER -> R.string.rw_channel_weather_desc
    }

    // Swap requests come from another person and may need an answer, so they pop up.
    // Weather suggestions are nice-to-have, so they arrive quietly.
    private fun importanceFor(type: NotificationType) = when (type) {
        NotificationType.SWAP -> NotificationManager.IMPORTANCE_HIGH
        NotificationType.WEATHER -> NotificationManager.IMPORTANCE_LOW
        else -> NotificationManager.IMPORTANCE_DEFAULT
    }
}

/* Reference List
Android Developers, 2026a. Create and manage notification channels. [online] Available at: <https://developer.android.com/develop/ui/views/notifications/channels> [Accessed 29 October 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
