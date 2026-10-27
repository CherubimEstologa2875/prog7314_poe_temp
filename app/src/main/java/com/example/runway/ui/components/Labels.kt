package com.example.runway.ui.components

import android.content.Context
import androidx.annotation.StringRes
import com.example.runway.R
import com.example.runway.domain.model.ItemColour

// Categories, occasions and colours are saved in English so the data stays the same on every phone.
// These helpers swap them for the right words in whatever language the app is set to.
object Labels {

    // Anything we do not recognise is shown tidied up rather than guessed at.
    fun category(context: Context, value: String): String {
        val res = when (value.trim().uppercase()) {
            "TOP", "TOPS" -> R.string.rw_category_top
            "BOTTOM", "BOTTOMS" -> R.string.rw_category_bottom
            "OUTERWEAR" -> R.string.rw_category_outerwear
            "DRESS", "DRESSES" -> R.string.rw_category_dress
            "SHOES", "SHOE" -> R.string.rw_category_shoes
            "OTHER" -> R.string.rw_category_other
            else -> return value.lowercase().replaceFirstChar { it.uppercase() }
        }
        return context.getString(res)
    }

    fun occasion(context: Context, value: String?): String? {
        val res = when (value) {
            null -> return null
            "Casual" -> R.string.rw_occasion_casual
            "Work" -> R.string.rw_occasion_work
            "Evening" -> R.string.rw_occasion_evening
            "Sport" -> R.string.rw_occasion_sport
            "Formal" -> R.string.rw_occasion_formal
            else -> return value
        }
        return context.getString(res)
    }

    // Colours the user named themselves are shown exactly as they typed them.
    fun colour(context: Context, label: String?): String? {
        if (label.isNullOrBlank()) return null
        val builtIn = ItemColour.parse(label) ?: return label
        return context.getString(colourRes(builtIn))
    }

    @StringRes
    private fun colourRes(colour: ItemColour): Int = when (colour) {
        ItemColour.WHITE -> R.string.rw_colour_white
        ItemColour.CREAM -> R.string.rw_colour_cream
        ItemColour.BEIGE -> R.string.rw_colour_beige
        ItemColour.TAUPE -> R.string.rw_colour_taupe
        ItemColour.TAN -> R.string.rw_colour_tan
        ItemColour.CAMEL -> R.string.rw_colour_camel
        ItemColour.BROWN -> R.string.rw_colour_brown
        ItemColour.BLACK -> R.string.rw_colour_black
        ItemColour.CHARCOAL -> R.string.rw_colour_charcoal
        ItemColour.GREY -> R.string.rw_colour_grey
        ItemColour.NAVY -> R.string.rw_colour_navy
        ItemColour.BLUE -> R.string.rw_colour_blue
        ItemColour.LIGHT_BLUE -> R.string.rw_colour_light_blue
        ItemColour.TEAL -> R.string.rw_colour_teal
        ItemColour.GREEN -> R.string.rw_colour_green
        ItemColour.SAGE -> R.string.rw_colour_sage
        ItemColour.OLIVE -> R.string.rw_colour_olive
        ItemColour.YELLOW -> R.string.rw_colour_yellow
        ItemColour.MUSTARD -> R.string.rw_colour_mustard
        ItemColour.ORANGE -> R.string.rw_colour_orange
        ItemColour.RED -> R.string.rw_colour_red
        ItemColour.BURGUNDY -> R.string.rw_colour_burgundy
        ItemColour.PINK -> R.string.rw_colour_pink
        ItemColour.PURPLE -> R.string.rw_colour_purple
        ItemColour.LILAC -> R.string.rw_colour_lilac
    }
}
