package com.example.runway.fake

import com.example.runway.domain.model.OutfitRating
import com.example.runway.domain.model.RatingSummary
import com.example.runway.domain.repository.RatingRepository

// In-memory ratings so the sheet can be tested with no API and no device.
class FakeRatingRepository : RatingRepository {

    private val saved = mutableListOf<OutfitRating>()

    var rateCount = 0
        private set

    // Set these to make either call fail, so the error paths can be tested.
    var rateError: Throwable? = null
    var listError: Throwable? = null

    val lastSaved: OutfitRating? get() = saved.lastOrNull()

    fun seed(vararg ratings: OutfitRating) {
        saved.addAll(ratings)
    }

    override suspend fun listForOutfit(outfitId: String): Result<RatingSummary> {
        listError?.let { return Result.failure(it) }

        val forOutfit = saved.filter { it.outfitId == outfitId }
        return Result.success(
            RatingSummary(
                outfitId = outfitId,
                count = forOutfit.size,
                average = forOutfit.map { it.score }
                    .takeIf { it.isNotEmpty() }
                    ?.let { scores -> Math.round(scores.average() * 10.0) / 10.0 },
                ratings = forOutfit,
            )
        )
    }

    override suspend fun rate(outfitId: String, score: Int, note: String?): Result<OutfitRating> {
        rateCount++
        rateError?.let { return Result.failure(it) }

        val rating = OutfitRating(
            id = "rating-${saved.size + 1}",
            outfitId = outfitId,
            score = score,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
        )
        saved.add(rating)
        return Result.success(rating)
    }
}
