package com.example.runway.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// SCRUM-143: reading a saved colour back into the palette.
class ItemColourTest {

    @Test
    fun `labels are matched whatever the case`() {
        assertEquals(ItemColour.NAVY, ItemColour.parse("navy"))
        assertEquals(ItemColour.NAVY, ItemColour.parse("NAVY"))
    }

    @Test
    fun `spaces are ignored`() {
        assertEquals(ItemColour.LIGHT_BLUE, ItemColour.parse("Light blue"))
        assertEquals(ItemColour.LIGHT_BLUE, ItemColour.parse("lightblue"))
        assertEquals(ItemColour.LIGHT_BLUE, ItemColour.parse("  light  blue "))
    }

    @Test
    fun `colours outside the palette aren't guessed at`() {
        assertNull(ItemColour.parse("turquoise-ish"))
        assertNull(ItemColour.parse(""))
        assertNull(ItemColour.parse(null))
    }

    @Test
    fun `every palette colour reads back as itself`() {
        ItemColour.entries.forEach { assertEquals(it, ItemColour.parse(it.label)) }
    }
}
