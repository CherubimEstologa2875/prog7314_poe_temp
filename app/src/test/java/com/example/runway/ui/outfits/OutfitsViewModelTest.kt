package com.example.runway.ui.outfits

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.fake.FakeItemRepository
import com.example.runway.fake.FakeOutfitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

// SCRUM-146 and 141: the Outfits grid and its loading and failure states.
class OutfitsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val outfits = FakeOutfitRepository()
    private val items = FakeItemRepository()

    @Test
    fun `loads the saved outfits`() = runTest {
        outfits.outfits = listOf(Outfit(id = "a", name = "A"), Outfit(id = "b", name = "B"))
        val state = OutfitsViewModel(outfits, items).uiState.value

        assertEquals(listOf("a", "b"), state.outfits.map { it.id })
        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
    }

    @Test
    fun `nothing loaded at all shows a retry, not an empty wardrobe message`() = runTest {
        outfits.listError = IOException("offline")
        val state = OutfitsViewModel(outfits, items).uiState.value

        assertTrue(state.loadFailed)
        assertTrue(state.outfits.isEmpty())
    }

    @Test
    fun `a failed refresh keeps the list and just says so`() = runTest {
        outfits.outfits = listOf(Outfit(id = "a", name = "A"))
        val vm = OutfitsViewModel(outfits, items)
        outfits.listError = IOException("offline")

        vm.refresh()

        assertEquals(1, vm.uiState.value.outfits.size)
        assertFalse(vm.uiState.value.loadFailed)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `retrying after a failure clears it`() = runTest {
        outfits.listError = IOException("offline")
        val vm = OutfitsViewModel(outfits, items)
        outfits.listError = null

        vm.refresh()

        assertFalse(vm.uiState.value.loadFailed)
    }

    @Test
    fun `item photos are available for the thumbnails`() = runTest {
        items.emit(listOf(Item(id = "shirt", name = "Shirt", category = "TOP", imagePath = "/p.png")))
        val state = OutfitsViewModel(outfits, items).uiState.value

        assertEquals("/p.png", state.itemsById["shirt"]?.imagePath)
    }
}
