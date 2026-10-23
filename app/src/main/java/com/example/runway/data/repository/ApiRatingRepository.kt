package com.example.runway.data.repository

import com.example.runway.data.remote.api.CreateRatingDto
import com.example.runway.data.remote.api.RatingDto
import com.example.runway.data.remote.api.RatingSummaryDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.domain.model.OutfitRating
import com.example.runway.domain.model.RatingSummary
import com.example.runway.domain.repository.RatingRepository

// Confidence ratings, read and written through the Runway REST API.

class ApiRatingRepository(
    private val api: RunwayApi,
) : RatingRepository {

    override suspend fun listForOutfit(outfitId: String): Result<RatingSummary> = runCatching {
        api.getOutfitRatings(outfitId).toDomain()
    }

    override suspend fun rate(outfitId: String, score: Int, note: String?): Result<OutfitRating> =
        runCatching {
            api.rateOutfit(
                outfitId,
                CreateRatingDto(score = score, note = note?.trim()?.takeIf { it.isNotEmpty() }),
            ).toDomain()
        }
}

private fun RatingDto.toDomain(): OutfitRating = OutfitRating(
    id = id,
    outfitId = outfitId,
    score = score.toInt(),
    note = note,
    ratedAt = ratedAt,
)

private fun RatingSummaryDto.toDomain(): RatingSummary = RatingSummary(
    outfitId = outfitId,
    count = count,
    average = average,
    ratings = ratings.map { it.toDomain() },
)
