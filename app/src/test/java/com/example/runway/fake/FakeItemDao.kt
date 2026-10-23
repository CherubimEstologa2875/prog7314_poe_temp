package com.example.runway.fake

import com.example.runway.data.local.ItemDao
import com.example.runway.data.local.ItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Room's item table as a plain map, so the repository can be tested without a database.
class FakeItemDao : ItemDao {

    private val rows = MutableStateFlow<Map<String, ItemEntity>>(emptyMap())

    val all: List<ItemEntity> get() = rows.value.values.toList()

    fun row(id: String): ItemEntity? = rows.value[id]

    fun put(vararg entities: ItemEntity) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override fun observeAll(): Flow<List<ItemEntity>> =
        rows.map { map -> map.values.filterNot { it.deleted }.sortedByDescending { it.updatedAt } }

    override fun observeByCategory(category: String): Flow<List<ItemEntity>> =
        observeAll().map { list -> list.filter { it.category == category } }

    override fun observeById(id: String): Flow<ItemEntity?> =
        rows.map { map -> map[id]?.takeUnless { it.deleted } }

    override suspend fun upsert(item: ItemEntity) = put(item)

    override suspend fun upsertAll(items: List<ItemEntity>) = put(*items.toTypedArray())

    override suspend fun pendingSync(): List<ItemEntity> = rows.value.values.filter { it.pendingSync }

    override suspend fun markDeleted(id: String, updatedAt: Long) {
        val row = rows.value[id] ?: return
        put(row.copy(deleted = true, pendingSync = true, updatedAt = updatedAt))
    }

    override suspend fun imagePathFor(id: String): String? = rows.value[id]?.imagePath

    override suspend fun count(): Int = rows.value.size

    override fun observePendingCount(): Flow<Int> = rows.map { map -> map.values.count { it.pendingSync } }

    override suspend fun hardDelete(id: String) {
        rows.value = rows.value - id
    }
}
