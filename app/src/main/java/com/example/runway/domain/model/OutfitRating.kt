package com.example.runway.domain.model

// How confident the user felt in an outfit, 1 to 5.

data class OutfitRating(
    val id: String = "",
    val outfitId: String = "",
    val score: Int = 0,
    val note: String? = null,
    val ratedAt: String? = null,
)

// Every rating on one outfit, plus the figures the detail screen shows.
data class RatingSummary(
    val outfitId: String = "",
    val count: Int = 0,
    // Null until the outfit has been rated once, which is not the same as a zero score.
    val average: Double? = null,
    val ratings: List<OutfitRating> = emptyList(),
) {
    val hasRatings: Boolean get() = count > 0

    // The whole-star value the read-only star row shows.
    val roundedAverage: Int get() = average?.let { Math.round(it).toInt() } ?: 0

    companion object {
        const val MIN_SCORE = 1
        const val MAX_SCORE = 5
    }
}
