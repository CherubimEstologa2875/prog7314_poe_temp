package com.example.runway.ui.wardrobe

import com.example.runway.domain.model.ColourMatch
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.PaletteColour

data class ColourMatcherUiState(
    val reference: Item? = null,
    val referenceColour: PaletteColour? = null,
    val matches: List<ColourMatch> = emptyList(),
    val isLoading: Boolean = true
)