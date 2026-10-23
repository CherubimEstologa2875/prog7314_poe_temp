package com.example.runway.domain.model

/** Everything the user can change on the settings screen. */
data class RunwaySettings(
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val language: String = LANGUAGE_ENGLISH,
    val biometricsEnabled: Boolean = false,
    val notifyWash: Boolean = true,
    val notifySwap: Boolean = true,
    val notifyWeather: Boolean = true,
    val defaultWearLimit: Int = DEFAULT_WEAR_LIMIT,
) {
    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_AFRIKAANS = "af"
        const val LANGUAGE_ZULU = "zu"

        /** The languages we plan to ship. Anything else is treated as invalid. */
        val SUPPORTED_LANGUAGES = listOf(LANGUAGE_ENGLISH, LANGUAGE_AFRIKAANS, LANGUAGE_ZULU)

        const val MIN_WEAR_LIMIT = 1
        const val MAX_WEAR_LIMIT = 60
        const val DEFAULT_WEAR_LIMIT = 3
    }
}

enum class ThemeOption {
    DAY,
    NIGHT,
    SYSTEM,
}
