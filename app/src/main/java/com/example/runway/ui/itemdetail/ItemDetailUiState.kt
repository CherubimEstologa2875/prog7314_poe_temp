package com.example.runway.ui.itemdetail

import com.example.runway.domain.model.Item

data class ItemDetailUiState(
    val item: Item? = null,
    val isLoading: Boolean = true
)
