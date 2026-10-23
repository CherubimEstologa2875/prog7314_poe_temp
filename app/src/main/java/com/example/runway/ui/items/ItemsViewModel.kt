package com.example.runway.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.model.Item
import com.example.runway.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel for the wardrobe list.

class ItemsViewModel(
    private val repository: ItemRepository
) : ViewModel() {
    // Errors raised by user actions, as opposed to by the data stream itself.
    private val actionError = MutableStateFlow<String?>(null)
    private val syncing = MutableStateFlow(false)

    val uiState: StateFlow<ItemsUiState> =
        combine(
            repository.observeItems()
                .map<List<Item>, Result<List<Item>>> { Result.success(it) }
                .catch { emit(Result.failure(it)) },
            actionError.asStateFlow(),
            syncing.asStateFlow(),
        ) { result, error, isSyncing ->
            result.fold(
                onSuccess = { items ->
                    ItemsUiState(
                        items = items,
                        isLoading = false,
                        isSyncing = isSyncing,
                        errorMessage = error,
                    )
                },
                onFailure = { throwable ->
                    ItemsUiState(
                        isLoading = false,
                        isSyncing = isSyncing,
                        errorMessage = throwable.message ?: "Could not load your wardrobe.",
                    )
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemsUiState()
        )

    init {
        refresh()
    }

    // Syncs with the Runway API.
    fun refresh() {
        viewModelScope.launch {
            syncing.value = true
            repository.refresh().onFailure { actionError.value = it.toUserMessage(queuesOffline = true) }
            syncing.value = false
        }
    }

    fun onAddItem(
        name: String,
        category: String,
        colour: String? = null,
        brand: String? = null,
        size: String? = null,
        purchasePrice: Double? = null,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            actionError.value = "A name is required."
            return
        }
        if (category.isBlank()) {
            actionError.value = "Choose a category."
            return
        }
        if (purchasePrice != null && purchasePrice < 0) {
            actionError.value = "A price cannot be negative."
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.save(
                    Item(
                        name = cleanName,
                        category = category,
                        colour = colour?.trim()?.ifBlank { null },
                        brand = brand?.trim()?.ifBlank { null },
                        size = size?.trim()?.ifBlank { null },
                        purchasePrice = purchasePrice,
                    )
                )
            }
                .onFailure { actionError.value = it.message ?: "Could not save the item." }
                .onSuccess { actionError.value = null }
        }
    }

    fun onDeleteItem(id: String) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { actionError.value = it.message ?: "Could not delete the item." }
        }
    }

    fun onErrorShown() {
        actionError.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                ItemsViewModel(app.container.itemRepository)
            }
        }
    }
}
