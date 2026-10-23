package com.example.runway.ui.sheets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.domain.repository.PlanRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PickOutfitUiState(
    // The day an outfit is being chosen for.
    val date: LocalDate? = null,
    val outfits: List<Outfit> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    // Set once the day is planned, so the sheet can say so and close.
    val plannedFor: LocalDate? = null,
    val errorMessage: String? = null,
)

// Picks an outfit for one day. The other direction is a date picker on the outfit screen.
class PickOutfitViewModel(
    private val date: LocalDate,
    private val planRepository: PlanRepository,
    private val outfitRepository: OutfitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickOutfitUiState(date = date))
    val uiState: StateFlow<PickOutfitUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            outfitRepository.list()
                .onSuccess { outfits -> _uiState.update { it.copy(isLoading = false, outfits = outfits) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
        }
    }

    fun onPickOutfit(outfitId: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            planRepository.plan(date, outfitId)
                .onSuccess { _uiState.update { it.copy(isSaving = false, plannedFor = date) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.toUserMessage()) } }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                val iso = checkNotNull(handle.get<String>(NavArgs.DATE_ISO)) {
                    "PickOutfitViewModel requires a ${NavArgs.DATE_ISO} argument"
                }
                PickOutfitViewModel(
                    date = LocalDate.parse(iso),
                    planRepository = app.container.planRepository,
                    outfitRepository = app.container.outfitRepository,
                )
            }
        }
    }
}
