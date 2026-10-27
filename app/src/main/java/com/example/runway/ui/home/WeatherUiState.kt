package com.example.runway.ui.home

import androidx.annotation.StringRes
import com.example.runway.domain.model.Item

data class WeatherUiState(
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val temperature: Int? = null,
    @StringRes val condition: Int? = null,
    @StringRes val advice: Int? = null,
    val items: List<Item> = emptyList(),
)