package com.example.runway.ui.items

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Item
import com.example.runway.fake.FakeItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in a loading state`() {
        val viewModel = ItemsViewModel(FakeItemRepository())
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `emits items from the repository once collected`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        val expected = listOf(Item(id = "item-1", name = "Black wool coat", category = "OUTERWEAR"))
        repository.emit(expected)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(expected, viewModel.uiState.value.items)

        job.cancel()
    }

    @Test
    fun `a blank name is rejected and never reaches the repository`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onAddItem(name = "   ", category = "TOP")

        assertEquals(0, repository.saveCount)
        assertEquals("A name is required.", viewModel.uiState.value.errorMessage)

        job.cancel()
    }

    @Test
    fun `a blank category is rejected`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onAddItem(name = "Black wool coat", category = "  ")

        assertEquals(0, repository.saveCount)
        assertEquals("Choose a category.", viewModel.uiState.value.errorMessage)

        job.cancel()
    }

    @Test
    fun `a negative price is rejected`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onAddItem(name = "Coat", category = "OUTERWEAR", purchasePrice = -1.0)

        assertEquals(0, repository.saveCount)
        assertEquals("A price cannot be negative.", viewModel.uiState.value.errorMessage)

        job.cancel()
    }

    @Test
    fun `a valid item is saved and the error is cleared`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onAddItem(name = "  Black wool coat  ", category = "OUTERWEAR", brand = " Country Road ")

        assertEquals(1, repository.saveCount)
        assertEquals(null, viewModel.uiState.value.errorMessage)

        val saved = viewModel.uiState.value.items.single()
        assertEquals("Black wool coat", saved.name)
        assertEquals("OUTERWEAR", saved.category)
        assertEquals("Country Road", saved.brand)

        job.cancel()
    }

    @Test
    fun `losing the connection tells the user their changes will sync later`() = runTest {
        val repository = FakeItemRepository()
        repository.refreshResult = Result.failure(IOException("socket closed"))
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.refresh()

        assertTrue(repository.refreshCount > 0)
        // The raw exception is never shown; toUserMessage turns it into this.
        assertEquals(
            "You are offline. Changes are saved and will sync later.",
            viewModel.uiState.value.errorMessage,
        )

        job.cancel()
    }
}
