package com.example.runway.fake

import com.example.runway.data.local.SyncStore

class FakeSyncStore : SyncStore {

    val cursors = mutableMapOf<String, String>()

    override fun lastSyncedAt(uid: String): String? = cursors[uid]

    override fun setLastSyncedAt(uid: String, isoInstant: String) {
        cursors[uid] = isoInstant
    }

    override fun clear(uid: String) {
        cursors.remove(uid)
    }
}
