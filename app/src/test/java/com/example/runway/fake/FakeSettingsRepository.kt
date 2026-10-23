package com.example.runway.fake

import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory settings so the ViewModel can be tested without SharedPreferences. */
class FakeSettingsRepository(
    initial: RunwaySettings = RunwaySettings()
) : SettingsRepository {

    private val settings = MutableStateFlow(initial)

    var updateCount = 0
        private set
    var resetCount = 0
        private set

    override fun observeSettings(): Flow<RunwaySettings> = settings

    override fun currentSettings(): RunwaySettings = settings.value

    override suspend fun update(settings: RunwaySettings) {
        updateCount++
        this.settings.value = settings
    }

    override suspend fun resetToDefaults() {
        resetCount++
        settings.value = RunwaySettings()
    }
}
