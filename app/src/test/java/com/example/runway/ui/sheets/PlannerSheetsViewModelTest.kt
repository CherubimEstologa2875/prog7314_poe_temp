package com.example.runway.ui.sheets

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Outfit
import com.example.runway.fake.FakeOutfitRepository
import com.example.runway.fake.FakePlanRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

// SCRUM-146: the two planner sheets, the day view and the picker.
class PlannerSheetsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 19)
    private val plans = FakePlanRepository()
    private val outfits = FakeOutfitRepository().apply {
        outfits = listOf(Outfit(id = "o1", name = "Friday dinner"), Outfit(id = "o2", name = "Office"))
    }

    @Test
    fun `the day sheet shows the planned outfit`() = runTest {
        plans.plans[today] = "o1"
        val state = DayDetailViewModel(today, plans, outfits).uiState.value

        assertEquals("Friday dinner", state.outfit?.name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `an empty day has nothing planned`() = runTest {
        val state = DayDetailViewModel(today, plans, outfits).uiState.value
        assertNull(state.outfit)
        assertFalse(state.loadFailed)
    }

    @Test
    fun `clearing a day removes its plan`() = runTest {
        plans.plans[today] = "o1"
        val vm = DayDetailViewModel(today, plans, outfits)

        vm.onClear()

        assertTrue(vm.uiState.value.cleared)
        assertTrue(plans.plans.isEmpty())
    }

    @Test
    fun `the day sheet reports a failed load`() = runTest {
        plans.listError = IOException("offline")
        assertTrue(DayDetailViewModel(today, plans, outfits).uiState.value.loadFailed)
    }

    @Test
    fun `picking for a day lists the outfits and plans the one chosen`() = runTest {
        val vm = PickOutfitViewModel(today, plans, outfits)

        assertEquals(listOf("o1", "o2"), vm.uiState.value.outfits.map { it.id })

        vm.onPickOutfit("o2")

        assertEquals("o2", plans.plans[today])
        assertEquals(today, vm.uiState.value.plannedFor)
    }


    @Test
    fun `a failed plan shows an error and leaves the sheet open`() = runTest {
        plans.planError = IOException("offline")
        val vm = PickOutfitViewModel(today, plans, outfits)

        vm.onPickOutfit("o1")

        assertNull(vm.uiState.value.plannedFor)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `no outfits loaded means the picker can say why`() = runTest {
        outfits.listError = IOException("offline")
        assertTrue(PickOutfitViewModel(today, plans, outfits).uiState.value.loadFailed)
    }
}
