package com.example.runway.ui.outfits

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Outfit
import com.example.runway.fake.FakeOutfitRepository
import com.example.runway.fake.FakePlanRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// SCRUM-146: the planner calendar.
class PlannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // A Saturday, in the middle of September.
    private val today = LocalDate.of(2026, 9, 19)
    private val plans = FakePlanRepository()
    private val outfits = FakeOutfitRepository().apply {
        outfits = listOf(Outfit(id = "o1", name = "Friday dinner"), Outfit(id = "o2", name = "Office"))
    }

    private fun viewModel() = PlannerViewModel(plans, outfits) { today }

    @Test
    fun `the grid is six full weeks starting on a Monday`() {
        val grid = PlannerViewModel.gridFor(YearMonth.of(2026, 9))

        assertEquals(42, grid.size)
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        // 1 September 2026 is a Tuesday, so the grid starts on 31 August.
        assertEquals(LocalDate.of(2026, 8, 31), grid.first())
    }

    @Test
    fun `a month that starts on a Monday starts on its own first`() {
        assertEquals(LocalDate.of(2026, 6, 1), PlannerViewModel.gridFor(YearMonth.of(2026, 6)).first())
    }

    @Test
    fun `opens on this month with today marked`() = runTest {
        val state = viewModel().uiState.value

        assertEquals(YearMonth.of(2026, 9), state.month)
        assertEquals(listOf(today), state.days.filter { it.isToday }.map { it.date })
        assertFalse(state.days.first().inMonth)
    }

    @Test
    fun `planned days show their outfit`() = runTest {
        plans.plans[today.plusDays(2)] = "o2"
        val state = viewModel().uiState.value

        assertEquals("Office", state.days.first { it.date == today.plusDays(2) }.outfit?.name)
        assertNull(state.days.first { it.date == today }.outfit)
    }

    @Test
    fun `a plan whose outfit was deleted just shows as an empty day`() = runTest {
        plans.plans[today] = "deleted-outfit"
        val state = viewModel().uiState.value

        assertNull(state.days.first { it.date == today }.outfit)
        assertTrue(state.upcoming.isEmpty())
    }

    @Test
    fun `upcoming lists the next plans in date order, from today`() = runTest {
        plans.plans[today.minusDays(1)] = "o1"
        plans.plans[today.plusDays(5)] = "o1"
        plans.plans[today.plusDays(1)] = "o2"

        val upcoming = viewModel().uiState.value.upcoming

        assertEquals(listOf(today.plusDays(1), today.plusDays(5)), upcoming.map { it.date })
    }

    @Test
    fun `moving months reloads that month`() = runTest {
        val vm = viewModel()
        vm.onNextMonth()

        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.month)
        // Each reload asks for the visible grid, plus the next few weeks for Upcoming.
        val october = PlannerViewModel.gridFor(YearMonth.of(2026, 10))
        assertTrue(plans.requestedRanges.contains(october.first() to october.last()))

        vm.onPreviousMonth()
        vm.onPreviousMonth()
        assertEquals(YearMonth.of(2026, 8), vm.uiState.value.month)
    }

    @Test
    fun `a failed load is reported and can be retried`() = runTest {
        plans.listError = IOException("offline")
        val vm = viewModel()
        assertTrue(vm.uiState.value.loadFailed)

        plans.listError = null
        vm.refresh()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals(42, vm.uiState.value.days.size)
    }
}
