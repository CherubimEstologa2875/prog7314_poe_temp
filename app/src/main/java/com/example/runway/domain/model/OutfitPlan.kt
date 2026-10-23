package com.example.runway.domain.model

import java.time.LocalDate

// One outfit planned for one day.
data class OutfitPlan(
    val date: LocalDate,
    val outfitId: String,
)
