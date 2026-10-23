package com.example.runway.ui.sheets

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

data class DayDetailUiState(
    val date: LocalDate,
    val outfit: Outfit? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isClearing: Boolean = false,
    val cleared: Boolean = false,
    val errorMessage: String? = null,
)

class DayDetailViewModel(
    date: LocalDate,
    private val planRepository: PlanRepository,
    private val outfitRepository: OutfitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState(date = date))
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val date = _uiState.value.date
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val plan = planRepository.between(date, date).getOrElse {
                _uiState.update { state -> state.copy(isLoading = false, loadFailed = true) }
                return@launch
            }.firstOrNull()

            // If the planned outfit has since been deleted the day just reads as empty.
            val outfit = plan?.let { outfitRepository.get(it.outfitId).getOrNull() }
            _uiState.update { it.copy(isLoading = false, outfit = outfit) }
        }
    }

    fun onClear() {
        if (_uiState.value.isClearing) return
        _uiState.update { it.copy(isClearing = true) }
        viewModelScope.launch {
            planRepository.clear(_uiState.value.date)
                .onSuccess { _uiState.update { it.copy(isClearing = false, cleared = true) } }
                .onFailure { e -> _uiState.update { it.copy(isClearing = false, errorMessage = e.toUserMessage()) } }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                val date = LocalDate.parse(checkNotNull(createSavedStateHandle().get<String>(NavArgs.DATE_ISO)))
                DayDetailViewModel(date, app.container.planRepository, app.container.outfitRepository)
            }
        }
    }
}
