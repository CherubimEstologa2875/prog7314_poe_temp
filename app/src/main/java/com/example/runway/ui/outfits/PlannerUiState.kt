package com.example.runway.ui.outfits

import com.example.runway.domain.model.Outfit
import java.time.LocalDate
import java.time.YearMonth

// One square on the planner calendar.
data class PlannerDay(
    val date: LocalDate,
    // False for the days from the months either side that fill out the grid.
    val inMonth: Boolean,
    val isToday: Boolean,
    val outfit: Outfit? = null,
)

data class PlannerUiState(
    val month: YearMonth = YearMonth.now(),
    val days: List<PlannerDay> = emptyList(),
    // The next few planned days from today onwards.
    val upcoming: List<PlannerDay> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
)
