package com.example.runway.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCRUM-144: the summary shown under an outfit's confidence stars.
class RatingSummaryTest {

    @Test
    fun `no ratings means nothing to show, not zero stars`() {
        val empty = RatingSummary(outfitId = "o1")
        assertFalse(empty.hasRatings)
        assertEquals(0, empty.roundedAverage)
    }

    @Test
    fun `the stars round the average to the nearest whole star`() {
        assertEquals(4, RatingSummary(count = 3, average = 3.7).roundedAverage)
        assertEquals(3, RatingSummary(count = 3, average = 3.2).roundedAverage)
        // Halves round up, so a 4.5 shows five stars.
        assertEquals(5, RatingSummary(count = 2, average = 4.5).roundedAverage)
    }

    @Test
    fun `any rating at all counts as having ratings`() {
        assertTrue(RatingSummary(count = 1, average = 1.0).hasRatings)
    }

    @Test
    fun `the valid range is one to five`() {
        assertEquals(1, RatingSummary.MIN_SCORE)
        assertEquals(5, RatingSummary.MAX_SCORE)
    }
}
