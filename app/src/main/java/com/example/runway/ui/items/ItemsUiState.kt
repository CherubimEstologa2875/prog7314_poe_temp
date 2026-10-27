package com.example.runway.ui.items

import androidx.annotation.StringRes
import com.example.runway.domain.model.Item

// Screen state for the wardrobe list.

data class ItemsUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    // True while a sync is in flight.
    val isSyncing: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    // True when at least one item is waiting to reach the server.
    val hasUnsyncedChanges: Boolean get() = items.any { it.pendingSync }
}
