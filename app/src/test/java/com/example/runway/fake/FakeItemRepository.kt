package com.example.runway.fake

import com.example.runway.domain.model.Item
import com.example.runway.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// In-memory items so the ViewModel can be tested with no Room, no API and no device.
class FakeItemRepository : ItemRepository {

    private val items = MutableStateFlow<List<Item>>(emptyList())

    var saveCount = 0
        private set
    var deleteCount = 0
        private set
    var refreshCount = 0
        private set

    // Set this to make refresh fail, so the error path can be tested.
    var refreshResult: Result<Unit> = Result.success(Unit)

    fun emit(next: List<Item>) {
        items.value = next
    }

    override fun observeItems(): Flow<List<Item>> = items

    override fun observeItem(id: String): Flow<Item?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun save(item: Item) {
        saveCount++
        items.value = items.value + item
    }

    override suspend fun delete(id: String) {
        deleteCount++
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun refresh(): Result<Unit> {
        refreshCount++
        return refreshResult
    }
}
