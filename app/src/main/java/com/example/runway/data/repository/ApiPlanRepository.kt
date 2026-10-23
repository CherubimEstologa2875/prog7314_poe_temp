package com.example.runway.data.repository

import com.example.runway.data.remote.api.PlanDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.SetPlanDto
import com.example.runway.domain.model.OutfitPlan
import com.example.runway.domain.repository.PlanRepository
import java.time.LocalDate

// The outfit planner, read and written through the Runway REST API.
class ApiPlanRepository(
    private val api: RunwayApi,
) : PlanRepository {

    override suspend fun between(from: LocalDate, to: LocalDate): Result<List<OutfitPlan>> = runCatching {
        // A row with a date we can't read is skipped rather than failing the whole month.
        api.getPlans(from.toString(), to.toString()).mapNotNull { it.toDomain() }
    }

    override suspend fun plan(date: LocalDate, outfitId: String): Result<OutfitPlan> = runCatching {
        api.setPlan(date.toString(), SetPlanDto(outfitId)).toDomain()
            ?: OutfitPlan(date, outfitId)
    }

    override suspend fun clear(date: LocalDate): Result<Unit> = runCatching {
        api.clearPlan(date.toString())
    }

    private fun PlanDto.toDomain(): OutfitPlan? =
        runCatching { OutfitPlan(LocalDate.parse(date), outfitId) }.getOrNull()
}
