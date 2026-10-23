package com.example.runway.ui.home

import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.WardrobeSummary

enum class Greeting {
    MORNING, AFTERNOON, EVENING;

    companion object {
        fun forHour(hour: Int): Greeting = when {
            hour < 12 -> MORNING
            hour < 18 -> AFTERNOON
            else -> EVENING
        }
    }
}

data class HomeUiState(
    val greeting: Greeting = Greeting.MORNING,
    val firstName: String = "",
    val summary: WardrobeSummary = WardrobeSummary(),
    val recentItems: List<Item> = emptyList(),
    val hasItems: Boolean = false,
    val todaysPick: Outfit? = null,
    // True when today's pick came from the planner rather than the daily rotation.
    val isPlanned: Boolean = false,
    // Photo paths for the pick's collage.
    val itemsById: Map<String, Item> = emptyMap(),
    // Whether there is more than one outfit to cycle through.
    val canSuggestAnother: Boolean = false,
    val isLoadingOutfits: Boolean = true,
    val outfitsFailed: Boolean = false,
    val isWearing: Boolean = false,
    // Set once a wear is logged, so the screen can open the confidence sheet.
    val wornOutfitId: String? = null,
    val errorMessage: String? = null,
) {
    val needsWashCount: Int get() = summary.needsWashCount ?: 0
}
