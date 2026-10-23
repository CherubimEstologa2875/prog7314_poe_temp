package com.example.runway.ui.itemdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel for one garment's detail screen.

class ItemDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ItemRepository
) : ViewModel() {
    private val itemId: String = checkNotNull(savedStateHandle[NavArgs.ITEM_ID]) {
        "ItemDetailViewModel requires a ${NavArgs.ITEM_ID} argument"
    }

    val uiState: StateFlow<ItemDetailUiState> = repository.observeItem(itemId)
        .map { ItemDetailUiState(item = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemDetailUiState()
        )

    fun onDelete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(itemId)
            onDeleted()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                ItemDetailViewModel(createSavedStateHandle(), app.container.itemRepository)
            }
        }
    }
}
