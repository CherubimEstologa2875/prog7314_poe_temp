package com.example.runway.data.repository

import android.util.Log
import com.example.runway.data.local.ItemDao
import com.example.runway.data.local.ItemEntity
import com.example.runway.data.local.SyncStore
import com.example.runway.data.mapper.toCreateDto
import com.example.runway.data.mapper.toDomain
import com.example.runway.data.mapper.toEntity
import com.example.runway.data.mapper.toUpdateDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.domain.model.Item
import com.example.runway.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.util.UUID

// Offline-first wardrobe: Room is the source of truth and Retrofit reconciles it with the API.
class OfflineItemRepository(
    private val dao: ItemDao,
    private val api: RunwayApi,
    private val syncPreferences: SyncStore,
    // Supplies the signed-in user's id.
    private val currentUid: () -> String?,
) : ItemRepository {
    override fun observeItems(): Flow<List<Item>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeItem(id: String): Flow<Item?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun save(item: Item) {
        val entity = item
            .copy(id = item.id.ifBlank { newLocalId() })
            .toEntity(pendingSync = true)

        dao.upsert(entity)

        // Best effort. Failure is expected offline and is not an error.
        runCatching { push(entity) }
    }

    override suspend fun delete(id: String) {
        dao.markDeleted(id, updatedAt = System.currentTimeMillis())

        val entity = dao.pendingSync().firstOrNull { it.id == id } ?: return
        runCatching { push(entity) }
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Cannot sync while signed out")

        pushPending()
        pull(uid)
    }.onFailure { throwable ->
        when (throwable) {
            is IOException -> Log.i(TAG, "Sync skipped, no usable connection")
            else -> Log.w(TAG, "Sync failed", throwable)
        }
    }

    // Sends everything in the outbox, oldest change first.
    private suspend fun pushPending() {
        dao.pendingSync()
            .sortedBy { it.updatedAt }
            .forEach { entity -> push(entity) }
    }

    // Sends one queued row and reconciles the local copy with the answer.
    private suspend fun push(entity: ItemEntity) {
        try {
            when {
                entity.deleted -> {
                    if (!entity.id.isLocalId()) api.deleteItem(entity.id)
                    dao.hardDelete(entity.id)
                }

                entity.id.isLocalId() -> {
                    val created = api.createItem(entity.toCreateDto())
                    // Swap the temporary id for the server's real one.
                    dao.hardDelete(entity.id)
                    // The photo follows the row onto its new server id.
                    dao.upsert(created.toEntity(entity.imagePath))
                }

                else -> {
                    val updated = api.updateItem(entity.id, entity.toUpdateDto())
                    dao.upsert(updated.toEntity(entity.imagePath))
                }
            }
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) {
                Log.i(TAG, "Item ${entity.id} no longer exists on the server; dropping local copy")
                dao.hardDelete(entity.id)
            } else {
                throw e
            }
        }
    }

    // Pulls everything that changed since the last successful sync, a page at a time.
    private suspend fun pull(uid: String) {
        // With nothing stored locally the cursor is meaningless, so fetch everything.
        val since = if (dao.count() == 0) null else syncPreferences.lastSyncedAt(uid)

        var cursor: String? = null
        var newest: Instant? = since?.let { runCatching { Instant.parse(it) }.getOrNull() }

        do {
            val page = api.getItems(updatedSince = since, cursor = cursor)

            val (removed, present) = page.data.partition { it.deleted }

            // Now that the deletion has been recorded locally, the row can go.
            removed.forEach { dao.hardDelete(it.id) }
            // Each row keeps whatever photo this device already had for it.
            dao.upsertAll(present.map { it.toEntity(dao.imagePathFor(it.id)) })

            val pageNewest = page.data
                .mapNotNull { dto -> dto.updatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } }
                .maxOrNull()

            if (pageNewest != null) {
                val current = newest
                newest = if (current == null || pageNewest.isAfter(current)) pageNewest else current
            }

            cursor = page.nextCursor
        } while (cursor != null)

        newest?.let { syncPreferences.setLastSyncedAt(uid, it.toString()) }
    }

    private fun newLocalId(): String = LOCAL_ID_PREFIX + UUID.randomUUID()

    private fun String.isLocalId(): Boolean = startsWith(LOCAL_ID_PREFIX)

    private companion object {
        const val TAG = "ItemRepository"
        const val LOCAL_ID_PREFIX = "local-"
        const val HTTP_NOT_FOUND = 404
    }
}
