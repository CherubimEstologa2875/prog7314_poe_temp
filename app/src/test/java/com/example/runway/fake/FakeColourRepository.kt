package com.example.runway.fake

import com.example.runway.domain.model.ColourPalette
import com.example.runway.domain.model.PaletteColour
import com.example.runway.domain.repository.ColourRepository

// An in-memory palette, so tests can add a colour without touching preferences.
class FakeColourRepository(custom: List<PaletteColour> = emptyList()) : ColourRepository {

    var custom: List<PaletteColour> = custom

    override fun custom(): List<PaletteColour> = custom

    override fun palette(): List<PaletteColour> = ColourPalette.all(custom)

    override fun add(label: String, argb: Int): PaletteColour {
        ColourPalette.parse(label, custom)?.let { return it }
        val colour = PaletteColour(label.trim(), argb, neutral = ColourPalette.neutralFor(argb), custom = true)
        custom = custom + colour
        return colour
    }
}
