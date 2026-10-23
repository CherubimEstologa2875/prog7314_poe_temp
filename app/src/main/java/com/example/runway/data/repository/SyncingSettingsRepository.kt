package com.example.runway.data.repository

import com.example.runway.data.mapper.applyTo
import com.example.runway.data.mapper.toDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

// Settings repository that keeps the device and the API in agreement
// The device answers every read and the API is built around it (Android Open Source Project, 2026).
class SyncingSettingsRepository(
    private val local: SettingsRepository,
    private val api: RunwayApi,
) : SettingsRepository {

    private var units: String = DEFAULT_UNITS

    override fun observeSettings(): Flow<RunwaySettings> = local.observeSettings()

    override fun currentSettings(): RunwaySettings = local.currentSettings()

    // Stored on the device first so the change stays even if the push fails
    override suspend fun update(settings: RunwaySettings) {
        local.update(settings)
        api.saveSettings(settings.toDto(units))
    }

    override suspend fun resetToDefaults() {
        local.resetToDefaults()
        api.saveSettings(RunwaySettings().toDto(units))
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val remote = api.getSettings()
        units = remote.units
        local.update(remote.applyTo(local.currentSettings()))
    }

    private companion object {
        const val DEFAULT_UNITS = "METRIC"
    }
}

/* Reference List
    1. Android Developers. 2026. Build an offline-first app. [Online] Available at: https://developer.android.com/topic/architecture/data-layer/offline-first. [Accessed 11 September 2026].
*/
