package com.example.runway.ui.itemdetail

import androidx.lifecycle.SavedStateHandle
import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Item
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

// SCRUM-146: one garment's detail screen.
class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeItemRepository()

    private fun viewModel(id: String) = ItemDetailViewModel(SavedStateHandle(mapOf(NavArgs.ITEM_ID to id)), repository)

    @Test
    fun `starts loading until the item arrives`() {
        assertTrue(viewModel("a").uiState.value.isLoading)
    }

    @Test
    fun `shows the item once it's read`() = runTest {
        repository.emit(listOf(Item(id = "a", name = "Shirt", category = "TOP")))
        val vm = viewModel("a")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertEquals("Shirt", vm.uiState.value.item?.name)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `an item that doesn't exist stops loading with nothing to show`() = runTest {
        val vm = viewModel("missing")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertNull(vm.uiState.value.item)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `deleting removes it and then tells the screen`() = runTest {
        repository.emit(listOf(Item(id = "a", name = "Shirt", category = "TOP")))
        var closed = false

        viewModel("a").onDelete { closed = true }

        assertTrue(closed)
        assertEquals(1, repository.deleteCount)
    }
}
