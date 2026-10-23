package com.example.runway.ui.wardrobe

import androidx.lifecycle.SavedStateHandle
import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Item
import com.example.runway.fake.FakeColourRepository
import com.example.runway.fake.FakeItemRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// SCRUM-143: how the matcher picks and orders suggestions from the wardrobe.
class ColourMatcherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeItemRepository()
    private val colours = FakeColourRepository()

    private fun viewModel(referenceId: String = "shirt") =
        ColourMatcherViewModel(SavedStateHandle(mapOf(NavArgs.ITEM_ID to referenceId)), repository, colours)

    private fun item(id: String, category: String, colour: String?, wears: Long = 0) =
        Item(id = id, name = id, category = category, colour = colour, wearCount = wears)

    @Test
    fun `the item itself and its own category are left out`() = runTest {
        repository.emit(
            listOf(
                item("shirt", "TOP", "Blue"),
                item("other-top", "TOP", "Black"),
                item("jeans", "BOTTOM", "Navy"),
            )
        )
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertEquals(listOf("jeans"), vm.uiState.value.matches.map { it.item.id })
    }

    @Test
    fun `items with an unreadable colour are skipped`() = runTest {
        repository.emit(
            listOf(
                item("shirt", "TOP", "Blue"),
                item("mystery", "BOTTOM", "sort of greenish"),
                item("jeans", "BOTTOM", "Navy"),
            )
        )
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertEquals(listOf("jeans"), vm.uiState.value.matches.map { it.item.id })
    }

    @Test
    fun `best score first, and the less worn one wins a tie`() = runTest {
        repository.emit(
            listOf(
                item("shirt", "TOP", "Blue"),
                // Complementary to blue, the lowest score.
                item("orange-shorts", "BOTTOM", "Orange", wears = 0),
                // Two neutrals score the same, so wear count decides.
                item("worn-chinos", "BOTTOM", "Beige", wears = 9),
                item("new-chinos", "BOTTOM", "Beige", wears = 1),
            )
        )
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertEquals(
            listOf("new-chinos", "worn-chinos", "orange-shorts"),
            vm.uiState.value.matches.map { it.item.id },
        )
        assertEquals(95, vm.scoreFor("new-chinos"))
        assertNull(vm.scoreFor("shirt"))
    }

    @Test
    fun `a reference with no usable colour gives no matches`() = runTest {
        repository.emit(listOf(item("shirt", "TOP", null), item("jeans", "BOTTOM", "Navy")))
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        val state = vm.uiState.value
        assertNull(state.referenceColour)
        assertTrue(state.matches.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `a reference that no longer exists gives no matches`() = runTest {
        repository.emit(listOf(item("jeans", "BOTTOM", "Navy")))
        val vm = viewModel(referenceId = "deleted")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertNull(vm.uiState.value.reference)
        assertTrue(vm.uiState.value.matches.isEmpty())
    }
}
