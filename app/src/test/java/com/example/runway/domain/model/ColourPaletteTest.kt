package com.example.runway.domain.model

import com.example.runway.fake.FakeColourRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The palette a custom colour joins, rather than being snapped to the nearest preset.
class ColourPaletteTest {

    private val mint = PaletteColour("Mint", 0xFF9FE3C4.toInt(), custom = true)

    @Test
    fun `the built-in colours are the whole enum`() {
        assertEquals(ItemColour.entries.size, ColourPalette.builtIn.size)
    }

    @Test
    fun `a custom colour is found by name alongside the built-in ones`() {
        assertEquals(ItemColour.NAVY.label, ColourPalette.parse("navy", listOf(mint))?.label)
        assertEquals(mint, ColourPalette.parse("Mint", listOf(mint)))
    }

    @Test
    fun `spacing and capitalisation do not matter`() {
        assertEquals(mint, ColourPalette.parse("  mint ", listOf(mint)))
        assertEquals(ItemColour.LIGHT_BLUE.label, ColourPalette.parse("lightblue")?.label)
    }

    @Test
    fun `an unknown or blank colour is nothing`() {
        assertNull(ColourPalette.parse("chartreuse"))
        assertNull(ColourPalette.parse("   "))
        assertNull(ColourPalette.parse(null))
    }

    @Test
    fun `colours with almost no hue count as neutrals`() {
        assertTrue(ColourPalette.neutralFor(0xFF9AA0A6.toInt()))
        assertTrue(ColourPalette.neutralFor(0xFFFFFFFF.toInt()))
        assertTrue(ColourPalette.neutralFor(0xFF000000.toInt()))
        assertFalse(ColourPalette.neutralFor(0xFFC62031.toInt()))
    }

    @Test
    fun `adding a colour that is already there reuses it`() {
        val colours = FakeColourRepository()
        val first = colours.add("Mint", 0xFF9FE3C4.toInt())
        val again = colours.add("mint", 0xFF000000.toInt())

        assertEquals(first, again)
        assertEquals(1, colours.custom().size)
    }

    @Test
    fun `an added colour joins the palette after the built-in ones`() {
        val colours = FakeColourRepository()
        colours.add("Mint", 0xFF9FE3C4.toInt())

        assertEquals(ColourPalette.builtIn.size + 1, colours.palette().size)
        assertEquals("Mint", colours.palette().last().label)
    }
}
