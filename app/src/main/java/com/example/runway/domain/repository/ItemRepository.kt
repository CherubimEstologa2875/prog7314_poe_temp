package com.example.runway.domain.repository

import com.example.runway.domain.model.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Repository contract the ViewModels depend on.

interface ItemRepository {
    fun observeItems(): Flow<List<Item>>

    fun observeItem(id: String): Flow<Item?>

    // How many local changes haven't reached the server yet. Only the offline repository queues any.
    fun observePendingCount(): Flow<Int> = flowOf(0)

    suspend fun save(item: Item)

    suspend fun delete(id: String)

    // Pulls changes from the API and pushes anything queued locally.
    suspend fun refresh(): Result<Unit>
}
