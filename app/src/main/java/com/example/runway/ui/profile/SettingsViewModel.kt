package com.example.runway.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.model.ThemeOption
import com.example.runway.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val actionError = MutableStateFlow<String?>(null)
    private val syncing = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(
            repository.observeSettings(),
            actionError.asStateFlow(),
            syncing.asStateFlow(),
        ) { settings, error, isSyncing ->
            SettingsUiState(
                settings = settings,
                isLoading = false,
                isSyncing = isSyncing,
                errorMessage = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    init {
        refresh()
    }

    // Reads the stored settings back from the API
    fun refresh() {
        viewModelScope.launch {
            syncing.value = true
            repository.refresh().onFailure { actionError.value = it.toUserMessage() }
            syncing.value = false
        }
    }

    fun onThemeSelected(theme: ThemeOption) = save { it.copy(theme = theme) }

    fun onBiometricsChanged(enabled: Boolean) = save { it.copy(biometricsEnabled = enabled) }

    fun onNotifyWashChanged(enabled: Boolean) = save { it.copy(notifyWash = enabled) }

    fun onNotifySwapChanged(enabled: Boolean) = save { it.copy(notifySwap = enabled) }

    fun onNotifyWeatherChanged(enabled: Boolean) = save { it.copy(notifyWeather = enabled) }

    /** Ignores a language we do not support rather than saving something we cannot show. */
    fun onLanguageSelected(language: String) {
        if (language !in RunwaySettings.SUPPORTED_LANGUAGES) return
        save { it.copy(language = language) }
    }

    /** The stepper can run past either end, so the value is clamped before it is saved. */
    fun onWearLimitChanged(limit: Int) {
        val safeLimit = limit.coerceIn(RunwaySettings.MIN_WEAR_LIMIT, RunwaySettings.MAX_WEAR_LIMIT)
        save { it.copy(defaultWearLimit = safeLimit) }
    }

    fun onResetToDefaults() {
        push { repository.resetToDefaults() }
    }

    fun onErrorShown() {
        actionError.value = null
    }

    private fun save(change: (RunwaySettings) -> RunwaySettings) {
        push { repository.update(change(repository.currentSettings())) }
    }

    // The device keeps the change either way, so a failed push is reported rather than undone.
    private fun push(write: suspend () -> Unit) {
        viewModelScope.launch {
            syncing.value = true
            runCatching { write() }
                .onFailure { actionError.value = it.toUserMessage() }
                .onSuccess { actionError.value = null }
            syncing.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                SettingsViewModel(app.container.settingsRepository)
            }
        }
    }
}
