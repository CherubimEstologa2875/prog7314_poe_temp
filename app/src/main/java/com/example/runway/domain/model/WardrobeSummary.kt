package com.example.runway.domain.model

// Wardrobe-wide totals for the Home and Profile stat tiles.
data class WardrobeSummary(
    val itemCount: Int = 0,
    // Null when we only have local data, since outfits live on the server.
    val outfitCount: Int? = null,
    val totalWears: Long = 0,
    val totalValue: Double = 0.0,
    // Null when offline, so the laundry alert hides instead of showing a wrong number.
    val needsWashCount: Int? = null,
) {
    companion object {
        // Fallback worked out from the items in Room when the API can't be reached.
        fun fromItems(items: List<Item>, outfitCount: Int? = null): WardrobeSummary {
            val active = items.filterNot { it.archived }
            return WardrobeSummary(
                itemCount = active.size,
                outfitCount = outfitCount,
                totalWears = active.sumOf { it.wearCount },
                totalValue = active.sumOf { it.purchasePrice ?: 0.0 },
            )
        }
    }
}
