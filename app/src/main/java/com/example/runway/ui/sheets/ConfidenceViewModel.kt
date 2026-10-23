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
import com.example.runway.domain.model.RatingSummary
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.domain.repository.RatingRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConfidenceViewModel(
    private val outfitId: String,
    private val ratingRepository: RatingRepository,
    private val outfitRepository: OutfitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfidenceUiState())
    val uiState: StateFlow<ConfidenceUiState> = _uiState.asStateFlow()

    init {
        loadOutfitName()
    }

    // Only for the prompt. A failure here must not stop the user rating.
    private fun loadOutfitName() {
        viewModelScope.launch {
            outfitRepository.get(outfitId).onSuccess { outfit ->
                _uiState.value = _uiState.value.copy(outfitName = outfit.name)
            }
        }
    }

    // Out-of-range scores are ignored rather than clamped: a 7 is a bug, not a 5.
    fun onScoreChanged(score: Int) {
        if (score < RatingSummary.MIN_SCORE || score > RatingSummary.MAX_SCORE) return
        _uiState.value = _uiState.value.copy(score = score)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return

        _uiState.value = state.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            ratingRepository.rate(outfitId, state.score, state.note)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        savedAt = System.currentTimeMillis(),
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.toUserMessage(),
                    )
                }
        }
    }

    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                ConfidenceViewModel(
                    outfitId = checkNotNull(handle[NavArgs.OUTFIT_ID]) {
                        "ConfidenceViewModel needs an ${NavArgs.OUTFIT_ID} argument"
                    },
                    ratingRepository = app.container.ratingRepository,
                    outfitRepository = app.container.outfitRepository,
                )
            }
        }
    }
}
