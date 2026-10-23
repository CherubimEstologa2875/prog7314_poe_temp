package com.example.runway.ui.outfits

import com.example.runway.domain.model.Item
import com.example.runway.domain.model.OutfitLayer

data class OutfitBuilderUiState(
    val layers: List<OutfitLayer> = emptyList(),
    val selectedId: String? = null,
    val name: String = "",
    val occasion: String = OutfitBuilderViewModel.OCCASIONS.first(),
    val trayCategory: String = OutfitBuilderViewModel.CATEGORIES.first(),
    val wardrobe: List<Item> = emptyList(),
    val isEditing: Boolean = false,
    // Only true while an existing outfit is being fetched for editing.
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    // Set once saved, so the screen can move on to the outfit's detail page.
    val savedOutfitId: String? = null,
    val errorMessage: String? = null,
) {
    val canSave: Boolean get() = layers.isNotEmpty() && !isSaving && !isLoading

    val selectedLayer: OutfitLayer? get() = layers.firstOrNull { it.itemId == selectedId }

    val selectedItem: Item? get() = wardrobe.firstOrNull { it.id == selectedId }

    val trayItems: List<Item> get() = wardrobe.filter { it.category.equals(trayCategory, ignoreCase = true) }

    val itemsById: Map<String, Item> get() = wardrobe.associateBy { it.id }
}
