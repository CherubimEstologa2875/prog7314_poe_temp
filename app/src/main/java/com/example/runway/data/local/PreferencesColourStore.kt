package com.example.runway.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.runway.domain.model.ColourPalette
import com.example.runway.domain.model.PaletteColour
import com.example.runway.domain.repository.ColourRepository

// Keeps the user's own colours on this device. The item stores the colour's name, which syncs as usual.
class PreferencesColourStore(context: Context) : ColourRepository {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    override fun custom(): List<PaletteColour> =
        preferences.getString(KEY_COLOURS, null).orEmpty()
            .split(RECORD)
            .filter { it.isNotBlank() }
            .mapNotNull { decode(it) }

    override fun palette(): List<PaletteColour> = ColourPalette.all(custom())

    override fun add(label: String, argb: Int): PaletteColour {
        val clean = label.trim().replace(RECORD, " ").replace(FIELD, " ")
        val existing = ColourPalette.parse(clean, custom())
        if (existing != null) return existing

        val colour = PaletteColour(clean, argb, neutral = ColourPalette.neutralFor(argb), custom = true)
        val saved = custom() + colour
        preferences.edit {
            putString(KEY_COLOURS, saved.joinToString(RECORD) { "${it.label}$FIELD${it.argb}" })
        }
        return colour
    }

    private fun decode(record: String): PaletteColour? {
        val parts = record.split(FIELD)
        if (parts.size != 2) return null
        val argb = parts[1].toIntOrNull() ?: return null
        return PaletteColour(parts[0], argb, neutral = ColourPalette.neutralFor(argb), custom = true)
    }

    private companion object {
        const val PREFS_NAME = "runway_colours"
        const val KEY_COLOURS = "custom"
        // Stripped from a label before saving, so they can never appear inside one.
        const val RECORD = "\n"
        const val FIELD = "|"
    }
}
