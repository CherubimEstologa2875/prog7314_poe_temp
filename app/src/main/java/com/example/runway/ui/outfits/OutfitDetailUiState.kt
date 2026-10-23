package com.example.runway.ui.outfits

import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.RatingSummary

// Screen state for one saved outfit (IIE, 2026).

data class OutfitDetailUiState(
    val outfit: Outfit? = null,
    // The garments in the outfit, resolved from the wardrobe so they have names.
    val garments: List<Item> = emptyList(),
    val name: String = "",
    val ratings: RatingSummary = RatingSummary(),
    // True when the ratings couldn't be fetched, which is not the same as having none.
    val ratingsFailed: Boolean = false,
    // Set once a wear is logged, so the screen knows to ask for a rating.
    val wearLoggedAt: Long? = null,
    val isLoading: Boolean = true,
    // True when the outfit itself couldn't be fetched, so there's nothing to show.
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val savedAt: Long? = null,
    val deleted: Boolean = false,
    // The day this outfit was just scheduled for, so the screen can confirm it.
    val scheduledFor: java.time.LocalDate? = null,
    val errorMessage: String? = null,
) {
    // Only offer to save when there is something to save.
    val hasChanges: Boolean
        get() = outfit != null &&
            (name.trim() != outfit.name || garments.map { it.id } != outfit.itemIds)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
