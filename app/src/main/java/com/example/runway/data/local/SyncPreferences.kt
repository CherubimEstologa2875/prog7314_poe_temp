package com.example.runway.data.local

import android.content.Context

// Stores the last successful sync time per user (IIE, 2026).
// The sync cursor the item repository reads and writes, so tests can swap in a fake.
interface SyncStore {
    fun lastSyncedAt(uid: String): String?
    fun setLastSyncedAt(uid: String, isoInstant: String)
    fun clear(uid: String)
}

class SyncPreferences(context: Context) : SyncStore {
    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // ISO-8601 instant of the last successful sync, or null if never.
    override fun lastSyncedAt(uid: String): String? = prefs.getString(key(uid), null)

    override fun setLastSyncedAt(uid: String, isoInstant: String) {
        prefs.edit().putString(key(uid), isoInstant).apply()
    }

    // Forgets the sync position, forcing the next refresh to fetch everything.
    override fun clear(uid: String) {
        prefs.edit().remove(key(uid)).apply()
    }

    private fun key(uid: String) = "$KEY_PREFIX$uid"

    private companion object {
        const val FILE_NAME = "runway_sync"
        const val KEY_PREFIX = "last_synced_at_"
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
