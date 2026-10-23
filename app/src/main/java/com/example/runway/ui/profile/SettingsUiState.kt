package com.example.runway.ui.profile

import com.example.runway.domain.model.RunwaySettings

data class SettingsUiState(
    val settings: RunwaySettings = RunwaySettings(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
)
