package com.example.runway.ui.home

import com.example.runway.MainDispatcherRule
import com.example.runway.data.auth.AuthSession
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.WardrobeSummary
import com.example.runway.fake.FakeItemRepository
import com.example.runway.fake.FakeOutfitRepository
import com.example.runway.fake.FakePlanRepository
import com.example.runway.fake.FakeWardrobeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = FakeItemRepository()
    private val outfits = FakeOutfitRepository()
    private val wardrobe = FakeWardrobeRepository()
    private val plans = FakePlanRepository()

    private var now = LocalDateTime.of(2026, 9, 19, 9, 0)

    private val session = AuthSession(
        id = "uid-1",
        email = "divan@example.com",
        displayName = "Divan Fourie",
        photoUrl = null,
        signedInAtMillis = 0L,
        biometricEnabled = false,
    )

    private fun viewModel(currentSession: AuthSession? = session) = HomeViewModel(
        itemRepository = items,
        outfitRepository = outfits,
        wardrobeRepository = wardrobe,
        planRepository = plans,
        currentSession = { currentSession },
        clock = { now },
    )

    private fun outfit(id: String, pieces: Int = 2) =
        Outfit(id = id, name = "Outfit $id", itemIds = (1..pieces).map { "item-$it" })

    @Test
    fun `greets by time of day with the first name`() {
        now = now.withHour(9)
        assertEquals(Greeting.MORNING, viewModel().uiState.value.greeting)
        now = now.withHour(14)
        assertEquals(Greeting.AFTERNOON, viewModel().uiState.value.greeting)
        now = now.withHour(20)
        assertEquals(Greeting.EVENING, viewModel().uiState.value.greeting)

        assertEquals("Divan", viewModel().uiState.value.firstName)
    }

    @Test
    fun `falls back to the email when there is no display name`() {
        val vm = viewModel(session.copy(displayName = ""))
        assertEquals("divan", vm.uiState.value.firstName)
    }

    @Test
    fun `uses the server totals when the API answers`() = runTest {
        wardrobe.summary = WardrobeSummary(itemCount = 12, outfitCount = 4, totalWears = 30, totalValue = 2500.0, needsWashCount = 3)
        val vm = viewModel()
        vm.refresh()

        val state = vm.uiState.value
        assertEquals(12, state.summary.itemCount)
        assertEquals(4, state.summary.outfitCount)
        assertEquals(3, state.needsWashCount)
    }

    @Test
    fun `works the totals out from Room when the API is down`() = runTest {
        wardrobe.error = IOException("offline")
        outfits.listError = IOException("offline")
        items.emit(
            listOf(
                Item(id = "a", name = "Shirt", category = "TOP", wearCount = 3, purchasePrice = 300.0),
                Item(id = "b", name = "Jeans", category = "BOTTOM", wearCount = 5, purchasePrice = 700.0),
                Item(id = "c", name = "Old coat", category = "OUTER", wearCount = 9, purchasePrice = 1000.0, archived = true),
            )
        )
        val vm = viewModel()
        vm.refresh()

        val summary = vm.uiState.value.summary
        // The archived coat is left out.
        assertEquals(2, summary.itemCount)
        assertEquals(8L, summary.totalWears)
        assertEquals(1000.0, summary.totalValue, 0.001)
        // Nothing to go on offline, so these stay unknown rather than showing zero.
        assertNull(summary.outfitCount)
        assertNull(summary.needsWashCount)
        assertEquals(0, vm.uiState.value.needsWashCount)
    }

    @Test
    fun `recently added is newest first, capped, and skips archived items`() = runTest {
        val many = (1..12).map { Item(id = "i$it", name = "Item $it", category = "TOP", updatedAt = it.toLong()) }
        items.emit(many + Item(id = "gone", name = "Gone", category = "TOP", updatedAt = 99L, archived = true))
        val vm = viewModel()

        val recent = vm.uiState.value.recentItems
        assertEquals(HomeViewModel.RECENT_LIMIT, recent.size)
        assertEquals("i12", recent.first().id)
        assertFalse(recent.any { it.id == "gone" })
    }

    @Test
    fun `picks the same outfit all day and a different one tomorrow`() = runTest {
        outfits.outfits = listOf(outfit("a"), outfit("b"), outfit("c"))

        val today = viewModel().apply { refresh() }.uiState.value.todaysPick
        val againToday = viewModel().apply { refresh() }.uiState.value.todaysPick
        now = now.plusDays(1)
        val tomorrow = viewModel().apply { refresh() }.uiState.value.todaysPick

        assertEquals(today, againToday)
        assertTrue(today != tomorrow)
    }

    @Test
    fun `never picks an outfit with nothing in it`() = runTest {
        outfits.outfits = listOf(outfit("empty", pieces = 0), outfit("real"))
        val vm = viewModel()

        repeat(4) {
            vm.refresh()
            assertEquals("real", vm.uiState.value.todaysPick?.id)
            vm.onSuggestAnother()
        }
        assertFalse(vm.uiState.value.canSuggestAnother)
    }

    @Test
    fun `suggest another cycles through the outfits`() = runTest {
        outfits.outfits = listOf(outfit("a"), outfit("b"))
        val vm = viewModel()
        vm.refresh()

        val first = vm.uiState.value.todaysPick
        vm.onSuggestAnother()
        val second = vm.uiState.value.todaysPick
        vm.onSuggestAnother()

        assertTrue(vm.uiState.value.canSuggestAnother)
        assertTrue(first != second)
        assertEquals(first, vm.uiState.value.todaysPick)
    }

    @Test
    fun `no outfits leaves no pick and stops loading`() = runTest {
        outfits.outfits = emptyList()
        val vm = viewModel()
        vm.refresh()

        assertNull(vm.uiState.value.todaysPick)
        assertFalse(vm.uiState.value.isLoadingOutfits)
        assertFalse(vm.uiState.value.outfitsFailed)
    }

    @Test
    fun `a failed first load is reported so the screen can offer a retry`() = runTest {
        outfits.listError = IOException("offline")
        val vm = viewModel()
        vm.refresh()

        assertTrue(vm.uiState.value.outfitsFailed)
        assertNull(vm.uiState.value.todaysPick)
    }

    @Test
    fun `a failed reload keeps the pick that was already showing`() = runTest {
        outfits.outfits = listOf(outfit("a"))
        val vm = viewModel()
        vm.refresh()
        outfits.listError = IOException("offline")
        vm.refresh()

        assertEquals("a", vm.uiState.value.todaysPick?.id)
        assertFalse(vm.uiState.value.outfitsFailed)
    }

    @Test
    fun `wearing the pick logs it and asks for a rating`() = runTest {
        outfits.outfits = listOf(outfit("a"))
        val vm = viewModel()
        vm.refresh()
        val summaryCalls = wardrobe.callCount

        vm.onWearToday()

        assertEquals("a", outfits.lastWornId)
        assertEquals("a", vm.uiState.value.wornOutfitId)
        // Wear counts changed, so the items and totals are fetched again.
        assertEquals(1, items.refreshCount)
        assertTrue(wardrobe.callCount > summaryCalls)

        vm.onWearHandled()
        assertNull(vm.uiState.value.wornOutfitId)
    }

    @Test
    fun `a failed wear shows an error and does not ask for a rating`() = runTest {
        outfits.outfits = listOf(outfit("a"))
        outfits.wearError = IOException("offline")
        val vm = viewModel()
        vm.refresh()

        vm.onWearToday()

        assertNull(vm.uiState.value.wornOutfitId)
        assertFalse(vm.uiState.value.isWearing)
        assertEquals(
            "You are offline. Please try again once you have a connection.",
            vm.uiState.value.errorMessage,
        )
    }

    @Test
    fun `an outfit planned for today beats the daily rotation`() = runTest {
        outfits.outfits = listOf(outfit("a"), outfit("b"), outfit("c"))
        plans.plans[now.toLocalDate()] = "b"
        val vm = viewModel()
        vm.refresh()

        assertEquals("b", vm.uiState.value.todaysPick?.id)
        assertTrue(vm.uiState.value.isPlanned)
        // The plan is the plan, so there's nothing else to suggest.
        assertFalse(vm.uiState.value.canSuggestAnother)
    }

    @Test
    fun `a planner that can't be reached falls back to the rotation`() = runTest {
        outfits.outfits = listOf(outfit("a"), outfit("b"))
        plans.listError = IOException("offline")
        val vm = viewModel()
        vm.refresh()

        assertFalse(vm.uiState.value.isPlanned)
        assertTrue(vm.uiState.value.todaysPick != null)
        assertFalse(vm.uiState.value.outfitsFailed)
    }
}
