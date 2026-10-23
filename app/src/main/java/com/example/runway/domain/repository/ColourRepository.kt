package com.example.runway.domain.repository

import com.example.runway.domain.model.PaletteColour

// The colours available when tagging an item.
interface ColourRepository {

    // Only the colours the user added.
    fun custom(): List<PaletteColour>

    // The built-in colours followed by the custom ones, in the order they show in.
    fun palette(): List<PaletteColour>

    // Saves a new colour and returns it. A label already in the palette is reused instead.
    fun add(label: String, argb: Int): PaletteColour
}
