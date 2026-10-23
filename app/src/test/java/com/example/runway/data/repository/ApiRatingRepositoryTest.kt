package com.example.runway.data.repository

import com.example.runway.data.remote.api.RatingDto
import com.example.runway.data.remote.api.RatingSummaryDto
import com.example.runway.fake.FakeRunwayApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCRUM-144: ratings going to and coming back from the API.
class ApiRatingRepositoryTest {

    private val api = FakeRunwayApi()
    private val repository = ApiRatingRepository(api)

    @Test
    fun `a summary comes back with its ratings in order`() = runTest {
        api.ratingSummary = RatingSummaryDto(
            outfitId = "o1",
            count = 2,
            average = 3.5,
            ratings = listOf(
                RatingDto(id = "r2", outfitId = "o1", score = 4, note = "felt great"),
                RatingDto(id = "r1", outfitId = "o1", score = 3),
            ),
        )

        val summary = repository.listForOutfit("o1").getOrThrow()

        assertEquals(2, summary.count)
        assertEquals(3.5, summary.average!!, 0.001)
        assertEquals(listOf(4, 3), summary.ratings.map { it.score })
        assertEquals("felt great", summary.ratings[0].note)
    }

    @Test
    fun `an outfit nobody has rated has no average`() = runTest {
        api.ratingSummary = RatingSummaryDto(outfitId = "o1")

        val summary = repository.listForOutfit("o1").getOrThrow()

        assertFalse(summary.hasRatings)
        assertNull(summary.average)
    }

    @Test
    fun `rating sends the score and note`() = runTest {
        val saved = repository.rate("o1", 5, "best outfit yet").getOrThrow()

        assertEquals(5, api.ratingSent?.score)
        assertEquals("best outfit yet", api.ratingSent?.note)
        assertEquals(5, saved.score)
        assertEquals("o1", saved.outfitId)
    }

    @Test
    fun `a failed call comes back as a failure, not a crash`() = runTest {
        val broken = ApiRatingRepository(object : com.example.runway.data.remote.api.RunwayApi by api {
            override suspend fun getOutfitRatings(id: String): RatingSummaryDto = throw java.io.IOException("offline")
        })

        assertTrue(broken.listForOutfit("o1").isFailure)
    }
}
