package com.example.runway.domain.model

import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.min

// How two colours sit relative to one another on the colour wheel.
// Ranked by how easy the pairing is to wear, e.g. low-contrast first and complete opposite hues last
enum class ColourRelationship(val score: Int) {
    NEUTRAL(95),
    TONAL(88),
    ANALOGOUS(82),
    TRIADIC(74),
    COMPLEMENTARY(66),
}

data class ColourMatch(
    val item: Item,
    val relationship: ColourRelationship,
    val score: Int,
)

// Scores one colour against another by the angle between their hues
// (HTML Color Codes, 2026; Android Open Source Project, 2026).
object ColourMatcher {

    fun relate(reference: PaletteColour, candidate: PaletteColour): ColourRelationship {
        if (reference.neutral || candidate.neutral) return ColourRelationship.NEUTRAL

        return when (hueGap(reference, candidate)) {
            in 150f..180f -> ColourRelationship.COMPLEMENTARY
            in 90f..150f -> ColourRelationship.TRIADIC
            in 30f..90f -> ColourRelationship.ANALOGOUS
            else -> ColourRelationship.TONAL
        }
    }

    // e.g. 350 and 10 should be 20 degrees apart rather than 340
    private fun hueGap(a: PaletteColour, b: PaletteColour): Float {
        val gap = abs(hsl(a)[0] - hsl(b)[0])
        return min(gap, 360f - gap)
    }

    // Splits the channels by hand so this works in unit tests, where android.graphics.Color is only a stub.
    private fun hsl(colour: PaletteColour): FloatArray {
        val argb = colour.argb
        return FloatArray(3).also {
            ColorUtils.RGBToHSL((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF, it)
        }
    }
}

/* Reference List
1. HTML Color Codes. 2026. Color Wheel. [Online]. Available at: https://htmlcolorcodes.com/color-wheel [Accessed 17 September 2026].
2. Android Open Source Project. 2026. ColorUtils. [Online]. Available at: https://developer.android.com/reference/androidx/core/graphics/ColorUtils [Accessed 17 September 2026].
*/