package com.example.runway.ui.outfits

import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit

// Screen state for the saved outfits list (IIE, 2026).

data class OutfitsUiState(
    val outfits: List<Outfit> = emptyList(),
    // Photo paths for the collage thumbnails.
    val itemsById: Map<String, Item> = emptyMap(),
    val isLoading: Boolean = true,
    // True when nothing could be loaded at all, so the screen shows a retry instead of "no outfits".
    val loadFailed: Boolean = false,
    val errorMessage: String? = null,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
