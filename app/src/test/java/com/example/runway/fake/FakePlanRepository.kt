package com.example.runway.fake

import com.example.runway.domain.model.OutfitPlan
import com.example.runway.domain.repository.PlanRepository
import java.time.LocalDate

// Planned days kept in a map, one outfit per day like the real API.
class FakePlanRepository : PlanRepository {

    val plans = mutableMapOf<LocalDate, String>()

    var listError: Throwable? = null
    var planError: Throwable? = null
    val requestedRanges = mutableListOf<Pair<LocalDate, LocalDate>>()

    override suspend fun between(from: LocalDate, to: LocalDate): Result<List<OutfitPlan>> {
        requestedRanges += from to to
        listError?.let { return Result.failure(it) }
        return Result.success(
            plans.filterKeys { it in from..to }.map { (date, outfitId) -> OutfitPlan(date, outfitId) }
        )
    }

    override suspend fun plan(date: LocalDate, outfitId: String): Result<OutfitPlan> {
        planError?.let { return Result.failure(it) }
        plans[date] = outfitId
        return Result.success(OutfitPlan(date, outfitId))
    }

    override suspend fun clear(date: LocalDate): Result<Unit> {
        plans.remove(date)
        return Result.success(Unit)
    }
}
