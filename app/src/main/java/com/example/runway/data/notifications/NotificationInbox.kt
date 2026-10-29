package com.example.runway.data.notifications

import android.content.Context
import androidx.core.content.edit
import com.example.runway.domain.model.InboxNotification
import com.example.runway.domain.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// The recent alerts shown on the Notifications screen (SCRUM-162).
// Kept even when Android notifications are blocked, so the user can still see what arrived.

class NotificationInbox(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val entries = MutableStateFlow(read())

    val notifications: StateFlow<List<InboxNotification>> = entries.asStateFlow()

    /** Called from the messaging service's thread, so writes are serialised. */
    @Synchronized
    fun add(type: NotificationType, title: String, body: String, receivedAtMillis: Long = System.currentTimeMillis()) {
        val entry = InboxNotification(
            id = receivedAtMillis,
            type = type,
            title = title,
            body = body,
            receivedAtMillis = receivedAtMillis,
        )
        save((listOf(entry) + entries.value).take(MAX_ENTRIES))
    }

    @Synchronized
    fun clear() = save(emptyList())

    private fun save(list: List<InboxNotification>) {
        val stored = list.map { StoredEntry(it.id, it.type.wireName, it.title, it.body, it.receivedAtMillis) }
        preferences.edit { putString(KEY_ENTRIES, json.encodeToString(ListSerializer(StoredEntry.serializer()), stored)) }
        entries.value = list
    }

    // A corrupt or older-format list is dropped rather than crashing the screen.
    private fun read(): List<InboxNotification> {
        val raw = preferences.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoredEntry.serializer()), raw).map {
                InboxNotification(it.id, NotificationType.fromWire(it.type), it.title, it.body, it.receivedAt)
            }
        }.getOrDefault(emptyList())
    }

    @Serializable
    private data class StoredEntry(
        val id: Long,
        val type: String,
        val title: String,
        val body: String,
        val receivedAt: Long,
    )

    private companion object {
        const val PREFS_NAME = "runway_notifications"
        const val KEY_ENTRIES = "inbox"
        const val MAX_ENTRIES = 30
        val json = Json { ignoreUnknownKeys = true }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
