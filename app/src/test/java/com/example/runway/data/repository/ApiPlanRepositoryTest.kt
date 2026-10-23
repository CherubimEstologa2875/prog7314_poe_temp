package com.example.runway.data.repository

import com.example.runway.data.remote.api.PlanDto
import com.example.runway.domain.model.OutfitPlan
import com.example.runway.fake.FakeRunwayApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// SCRUM-145: the planner's calls to the API.
class ApiPlanRepositoryTest {

    private val api = FakeRunwayApi()
    private val repository = ApiPlanRepository(api)

    @Test
    fun `dates go to the API as YYYY-MM-DD`() = runTest {
        repository.between(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 12))
        assertEquals("2026-09-01" to "2026-10-12", api.planRequest)
    }

    @Test
    fun `plans come back as days, and an unreadable date is skipped`() = runTest {
        api.plans = listOf(
            PlanDto(date = "2026-09-22", outfitId = "o1"),
            PlanDto(date = "someday", outfitId = "o2"),
        )

        val plans = repository.between(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)).getOrThrow()

        assertEquals(listOf(OutfitPlan(LocalDate.of(2026, 9, 22), "o1")), plans)
    }

    @Test
    fun `planning a day sends the outfit`() = runTest {
        val plan = repository.plan(LocalDate.of(2026, 9, 22), "o1").getOrThrow()

        assertEquals("2026-09-22", api.setPlans.single().first)
        assertEquals("o1", api.setPlans.single().second.outfitId)
        assertEquals(LocalDate.of(2026, 9, 22), plan.date)
    }

    @Test
    fun `clearing a day sends just the date`() = runTest {
        repository.clear(LocalDate.of(2026, 9, 22))
        assertEquals(listOf("2026-09-22"), api.clearedPlans)
    }
}
