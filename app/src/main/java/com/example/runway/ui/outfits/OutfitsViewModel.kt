package com.example.runway.ui.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.OutfitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel for the saved outfits list (IIE, 2026).

class OutfitsViewModel(
    private val repository: OutfitRepository,
    itemRepository: ItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutfitsUiState())
    val uiState: StateFlow<OutfitsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            itemRepository.observeItems().collect { items ->
                _uiState.update { it.copy(itemsById = items.associateBy { item -> item.id }) }
            }
        }
    }

    // Reloaded on every return to the tab, because an outfit may have been saved
    // from the model screen or edited since this list was last read.
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.list()
                .onSuccess { outfits ->
                    _uiState.update { it.copy(outfits = outfits, isLoading = false, loadFailed = false, errorMessage = null) }
                }
                .onFailure { e ->
                    // With nothing on screen yet, show a retry. Otherwise keep the list and just say so.
                    _uiState.update {
                        if (it.outfits.isEmpty()) it.copy(isLoading = false, loadFailed = true)
                        else it.copy(isLoading = false, errorMessage = e.toUserMessage())
                    }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                OutfitsViewModel(app.container.outfitRepository, app.container.itemRepository)
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
