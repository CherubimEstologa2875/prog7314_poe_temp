package com.example.runway.domain.model

import androidx.core.graphics.ColorUtils

// One colour an item can be tagged with, either built in or added by the user.
data class PaletteColour(
    val label: String,
    val argb: Int,
    val neutral: Boolean = false,
    val custom: Boolean = false,
)

fun ItemColour.asPaletteColour(): PaletteColour = PaletteColour(label, argb, neutral)

// The colours on offer: the 25 built in, plus any the user has added.
object ColourPalette {

    val builtIn: List<PaletteColour> = ItemColour.entries.map { it.asPaletteColour() }

    fun all(custom: List<PaletteColour> = emptyList()): List<PaletteColour> = builtIn + custom

    // Matches however the label happened to be spaced or capitalised when it was saved.
    fun parse(value: String?, custom: List<PaletteColour> = emptyList()): PaletteColour? {
        val cleaned = key(value ?: return null)
        if (cleaned.isEmpty()) return null
        return all(custom).firstOrNull { key(it.label) == cleaned }
    }

    fun key(label: String): String = label.trim().uppercase().replace(" ", "")

    // Greys and off-whites carry almost no hue, so matching them by angle would mean nothing.
    fun neutralFor(argb: Int): Boolean {
        val hsl = FloatArray(3)
        ColorUtils.RGBToHSL((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF, hsl)
        return hsl[1] < MIN_SATURATION || hsl[2] < MIN_LIGHTNESS || hsl[2] > MAX_LIGHTNESS
    }

    private const val MIN_SATURATION = 0.18f
    private const val MIN_LIGHTNESS = 0.12f
    private const val MAX_LIGHTNESS = 0.92f
}
