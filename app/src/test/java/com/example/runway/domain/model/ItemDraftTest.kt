package com.example.runway.domain.model

import com.example.runway.domain.model.ItemDraft.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCRUM-142: the checks on the add-item form.
class ItemDraftTest {

    private val valid = ItemDraft(name = "Linen shirt", category = "TOP")

    @Test
    fun `a name on its own is enough`() {
        assertTrue(valid.isValid)
        assertTrue(valid.problems.isEmpty())
    }

    @Test
    fun `a blank or spaces-only name is missing`() {
        assertTrue(Problem.NAME_MISSING in valid.copy(name = "").problems)
        assertTrue(Problem.NAME_MISSING in valid.copy(name = "    ").problems)
    }

    @Test
    fun `a name longer than the API allows is caught`() {
        val atLimit = valid.copy(name = "a".repeat(ItemDraft.MAX_NAME))
        val overLimit = valid.copy(name = "a".repeat(ItemDraft.MAX_NAME + 1))

        assertTrue(atLimit.isValid)
        assertTrue(Problem.NAME_TOO_LONG in overLimit.problems)
    }

    @Test
    fun `the price is optional`() {
        assertTrue(valid.copy(priceText = "").isValid)
        assertNull(valid.copy(priceText = "").price)
    }

    @Test
    fun `a price that isn't a number is flagged rather than dropped`() {
        assertTrue(Problem.PRICE_NOT_A_NUMBER in valid.copy(priceText = "12.5.3").problems)
        assertTrue(Problem.PRICE_NOT_A_NUMBER in valid.copy(priceText = "cheap").problems)
    }

    @Test
    fun `a negative price is flagged`() {
        assertTrue(Problem.PRICE_NEGATIVE in valid.copy(priceText = "-10").problems)
    }

    @Test
    fun `a comma works as the decimal point`() {
        val draft = valid.copy(priceText = "349,99")
        assertTrue(draft.isValid)
        assertEquals(349.99, draft.price!!, 0.001)
    }

    @Test
    fun `the wear limit must be inside the settings range`() {
        assertTrue(valid.copy(wearLimit = RunwaySettings.MIN_WEAR_LIMIT).isValid)
        assertTrue(valid.copy(wearLimit = RunwaySettings.MAX_WEAR_LIMIT).isValid)
        assertTrue(Problem.WEAR_LIMIT_OUT_OF_RANGE in valid.copy(wearLimit = 0).problems)
        assertTrue(Problem.WEAR_LIMIT_OUT_OF_RANGE in valid.copy(wearLimit = 61).problems)
    }

    @Test
    fun `several problems are reported together`() {
        val draft = ItemDraft(name = "", priceText = "-5", wearLimit = 0)
        assertEquals(
            setOf(Problem.NAME_MISSING, Problem.PRICE_NEGATIVE, Problem.WEAR_LIMIT_OUT_OF_RANGE),
            draft.problems,
        )
    }

    @Test
    fun `the saved item is trimmed and blanks become null`() {
        val item = ItemDraft(
            name = "  Linen shirt  ",
            category = "TOP",
            colour = ItemColour.NAVY.asPaletteColour(),
            brand = "   ",
            size = " M ",
            priceText = "450",
            wearLimit = 5,
        ).toItem(imagePath = "/photos/shirt.png")

        assertEquals("Linen shirt", item.name)
        assertEquals("Navy", item.colour)
        assertNull(item.brand)
        assertEquals("M", item.size)
        assertEquals(450.0, item.purchasePrice!!, 0.001)
        assertEquals(5, item.wearLimit)
        assertEquals("/photos/shirt.png", item.imagePath)
    }

    @Test
    fun `no colour picked saves no colour`() {
        assertNull(valid.toItem(imagePath = null).colour)
        assertFalse(valid.copy(name = "").isValid)
    }
}
