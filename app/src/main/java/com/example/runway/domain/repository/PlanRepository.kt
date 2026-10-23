package com.example.runway.domain.repository

import com.example.runway.domain.model.OutfitPlan
import java.time.LocalDate

interface PlanRepository {
    // Both days included.
    suspend fun between(from: LocalDate, to: LocalDate): Result<List<OutfitPlan>>

    // Replaces whatever was planned for that day.
    suspend fun plan(date: LocalDate, outfitId: String): Result<OutfitPlan>

    suspend fun clear(date: LocalDate): Result<Unit>
}
