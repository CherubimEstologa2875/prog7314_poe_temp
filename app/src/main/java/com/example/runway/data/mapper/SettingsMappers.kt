package com.example.runway.data.mapper

import com.example.runway.data.remote.api.SettingsDto
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.model.ThemeOption

// Conversions between the settings domain model and the API DTO

fun RunwaySettings.toDto(units: String): SettingsDto = SettingsDto(
    theme = theme.toApiTheme(),
    language = language,
    units = units,
    notificationsEnabled = notifyWash || notifySwap || notifyWeather,
    biometricEnabled = biometricsEnabled,
    defaultWearLimit = defaultWearLimit,
)

fun SettingsDto.applyTo(local: RunwaySettings): RunwaySettings = local.copy(
    theme = theme.toThemeOption(local.theme),
    language = language.takeIf { it in RunwaySettings.SUPPORTED_LANGUAGES } ?: local.language,
    biometricsEnabled = biometricEnabled,
    defaultWearLimit = defaultWearLimit,
    notifyWash = notificationsEnabled && local.notifyWash,
    notifySwap = notificationsEnabled && local.notifySwap,
    notifyWeather = notificationsEnabled && local.notifyWeather,
)

private fun ThemeOption.toApiTheme(): String = when (this) {
    ThemeOption.DAY -> THEME_LIGHT
    ThemeOption.NIGHT -> THEME_DARK
    ThemeOption.SYSTEM -> THEME_SYSTEM
}

// An unrecognised theme keeps the current one instead of resetting it.
private fun String.toThemeOption(fallback: ThemeOption): ThemeOption = when (this) {
    THEME_LIGHT -> ThemeOption.DAY
    THEME_DARK -> ThemeOption.NIGHT
    THEME_SYSTEM -> ThemeOption.SYSTEM
    else -> fallback
}

private const val THEME_LIGHT = "LIGHT"
private const val THEME_DARK = "DARK"
private const val THEME_SYSTEM = "SYSTEM"
