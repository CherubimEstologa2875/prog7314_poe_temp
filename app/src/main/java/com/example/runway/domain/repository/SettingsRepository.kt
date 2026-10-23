package com.example.runway.domain.repository

import com.example.runway.domain.model.RunwaySettings
import kotlinx.coroutines.flow.Flow

/** Where settings are stored. An interface so we can swap in Firestore later. */
interface SettingsRepository {

    fun observeSettings(): Flow<RunwaySettings>

    /** Read straight away without collecting, used when the app starts up. */
    fun currentSettings(): RunwaySettings

    suspend fun update(settings: RunwaySettings)

    suspend fun resetToDefaults()

    suspend fun refresh(): Result<Unit> = Result.success(Unit)
}
