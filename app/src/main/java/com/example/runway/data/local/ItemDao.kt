package com.example.runway.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Room data access object for the wardrobe.

@Dao
interface ItemDao {
    // Soft-deleted rows exist for sync purposes only and are never shown.
    @Query("SELECT * FROM items WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE deleted = 0 AND category = :category ORDER BY updatedAt DESC")
    fun observeByCategory(category: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id AND deleted = 0")
    fun observeById(id: String): Flow<ItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    // Used by a sync, which writes a whole page of server rows at once.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)

    // The outbox: local changes the API has not accepted yet.
    @Query("SELECT * FROM items WHERE pendingSync = 1")
    suspend fun pendingSync(): List<ItemEntity>

    // Flags a row deleted and queues it for the server.
    @Query("UPDATE items SET deleted = 1, pendingSync = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Long)

    // Read before a sync overwrites a row, so the local photo survives it.
    @Query("SELECT imagePath FROM items WHERE id = :id")
    suspend fun imagePathFor(id: String): String?

    // An empty table means a fresh install or a rebuilt cache, so the next sync pulls everything.
    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    // How many local changes are still waiting for the server, for the offline banner.
    @Query("SELECT COUNT(*) FROM items WHERE pendingSync = 1")
    fun observePendingCount(): Flow<Int>

    // Removes a row outright.
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun hardDelete(id: String)
}
