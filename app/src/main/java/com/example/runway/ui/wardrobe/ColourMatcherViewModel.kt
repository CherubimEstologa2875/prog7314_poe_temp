package com.example.runway.ui.wardrobe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.runway.RunwayApplication
import com.example.runway.domain.model.ColourMatch
import com.example.runway.domain.model.ColourMatcher
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.ColourPalette
import com.example.runway.domain.model.PaletteColour
import com.example.runway.domain.repository.ColourRepository
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// The garment the user came from is the reference every other item is measured against.

class ColourMatcherViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ItemRepository,
    colourRepository: ColourRepository,
) : ViewModel() {
    private val itemId: String = checkNotNull(savedStateHandle[NavArgs.ITEM_ID]) {
        "ColourMatcherViewModel requires a ${NavArgs.ITEM_ID} argument"
    }

    private val custom = colourRepository.custom()

    val uiState: StateFlow<ColourMatcherUiState> =
        combine(
            repository.observeItem(itemId),
            repository.observeItems(),
        ) { reference, wardrobe ->
            val colour = ColourPalette.parse(reference?.colour, custom)
            ColourMatcherUiState(
                reference = reference,
                referenceColour = colour,
                matches = if (reference == null || colour == null) {
                    emptyList()
                } else {
                    rank(reference, colour, wardrobe)
                },
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ColourMatcherUiState()
        )

    fun scoreFor(itemId: String): Int? = uiState.value.matches
        .firstOrNull { it.item.id == itemId }
        ?.score

    // Same category is left out, and since scores tie often the least worn wins
    private fun rank(reference: Item, colour: PaletteColour, wardrobe: List<Item>): List<ColourMatch> =
        wardrobe
            .filter { it.id != reference.id && !it.category.equals(reference.category, ignoreCase = true) }
            .mapNotNull { item ->
                ColourPalette.parse(item.colour, custom)?.let { candidate ->
                    val relationship = ColourMatcher.relate(colour, candidate)
                    ColourMatch(item, relationship, relationship.score)
                }
            }
            .sortedWith(compareByDescending<ColourMatch> { it.score }.thenBy { it.item.wearCount })

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                ColourMatcherViewModel(
                    createSavedStateHandle(),
                    app.container.itemRepository,
                    app.container.colourRepository,
                )
            }
        }
    }
}
