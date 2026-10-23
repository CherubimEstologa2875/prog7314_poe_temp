package com.example.runway.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.model.ThemeOption
import com.example.runway.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Saves settings in its own preferences file, so signing out does not wipe them. */
class PreferencesSettingsStore(context: Context) : SettingsRepository {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    // Seeded from disk so the first read already has the saved values.
    private val settings = MutableStateFlow(read())

    override fun observeSettings(): Flow<RunwaySettings> = settings.asStateFlow()

    override fun currentSettings(): RunwaySettings = settings.value

    override suspend fun update(settings: RunwaySettings) {
        preferences.edit {
            putString(KEY_THEME, settings.theme.name)
            putString(KEY_LANGUAGE, settings.language)
            putBoolean(KEY_BIOMETRICS, settings.biometricsEnabled)
            putBoolean(KEY_NOTIFY_WASH, settings.notifyWash)
            putBoolean(KEY_NOTIFY_SWAP, settings.notifySwap)
            putBoolean(KEY_NOTIFY_WEATHER, settings.notifyWeather)
            putInt(KEY_WEAR_LIMIT, settings.defaultWearLimit)
        }
        this.settings.value = settings
    }

    override suspend fun resetToDefaults() {
        preferences.edit { clear() }
        settings.value = RunwaySettings()
    }

    private fun read(): RunwaySettings {
        val defaults = RunwaySettings()
        return RunwaySettings(
            theme = readTheme(defaults.theme),
            language = preferences.getString(KEY_LANGUAGE, defaults.language) ?: defaults.language,
            biometricsEnabled = preferences.getBoolean(KEY_BIOMETRICS, defaults.biometricsEnabled),
            notifyWash = preferences.getBoolean(KEY_NOTIFY_WASH, defaults.notifyWash),
            notifySwap = preferences.getBoolean(KEY_NOTIFY_SWAP, defaults.notifySwap),
            notifyWeather = preferences.getBoolean(KEY_NOTIFY_WEATHER, defaults.notifyWeather),
            defaultWearLimit = preferences.getInt(KEY_WEAR_LIMIT, defaults.defaultWearLimit),
        )
    }

    // A saved name we no longer recognise falls back to the default instead of crashing.
    private fun readTheme(fallback: ThemeOption): ThemeOption {
        val saved = preferences.getString(KEY_THEME, null) ?: return fallback
        return ThemeOption.entries.firstOrNull { it.name == saved } ?: fallback
    }

    private companion object {
        const val PREFS_NAME = "runway_settings"
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_BIOMETRICS = "biometrics_enabled"
        const val KEY_NOTIFY_WASH = "notify_wash"
        const val KEY_NOTIFY_SWAP = "notify_swap"
        const val KEY_NOTIFY_WEATHER = "notify_weather"
        const val KEY_WEAR_LIMIT = "default_wear_limit"
    }
}
