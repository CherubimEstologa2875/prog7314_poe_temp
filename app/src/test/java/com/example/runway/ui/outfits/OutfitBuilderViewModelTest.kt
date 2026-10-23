package com.example.runway.ui.outfits

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitCanvas
import com.example.runway.domain.model.OutfitLayer
import com.example.runway.fake.FakeItemRepository
import com.example.runway.fake.FakeOutfitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

// SCRUM-146: the outfit builder canvas and saving what's on it.
class OutfitBuilderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val outfits = FakeOutfitRepository()
    private val items = FakeItemRepository()

    private fun newBuilder() = OutfitBuilderViewModel(outfitId = null, outfitRepository = outfits, itemRepository = items)

    @Test
    fun `nothing on the canvas means nothing to save`() {
        val vm = newBuilder()
        assertFalse(vm.uiState.value.canSave)

        vm.onSave("New outfit")
        assertNull(outfits.created)
    }

    @Test
    fun `adding a piece selects it so it can be adjusted straight away`() {
        val vm = newBuilder()
        vm.onAdd("shirt")

        assertEquals("shirt", vm.uiState.value.selectedId)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `the sliders only change the selected piece and stay in range`() {
        val vm = newBuilder()
        vm.onAdd("shirt")
        vm.onAdd("jeans")

        vm.onScale(5f)
        vm.onRotate(20f)

        val jeans = vm.uiState.value.layers.first { it.itemId == "jeans" }
        val shirt = vm.uiState.value.layers.first { it.itemId == "shirt" }
        assertEquals(OutfitCanvas.MAX_SCALE, jeans.scale)
        assertEquals(20f, jeans.rotation)
        assertEquals(1f, shirt.scale)
    }

    @Test
    fun `removing the selected piece clears the selection`() {
        val vm = newBuilder()
        vm.onAdd("shirt")
        vm.onRemoveSelected()

        assertTrue(vm.uiState.value.layers.isEmpty())
        assertNull(vm.uiState.value.selectedId)
    }

    @Test
    fun `the tray only shows the chosen category`() = runTest {
        items.emit(
            listOf(
                Item(id = "shirt", name = "Shirt", category = "TOP"),
                Item(id = "jeans", name = "Jeans", category = "BOTTOM"),
                Item(id = "old", name = "Old top", category = "TOP", archived = true),
            )
        )
        val vm = newBuilder()
        vm.onTrayCategoryChanged("BOTTOM")

        assertEquals(listOf("jeans"), vm.uiState.value.trayItems.map { it.id })
        vm.onTrayCategoryChanged("TOP")
        // Archived items don't come back into the tray.
        assertEquals(listOf("shirt"), vm.uiState.value.trayItems.map { it.id })
    }

    @Test
    fun `saving sends the layout bottom to top with the occasion`() = runTest {
        val vm = newBuilder()
        vm.onAdd("shirt")
        vm.onAdd("jacket")
        vm.onSendBackward()
        vm.onNameChanged("Office Monday")
        vm.onOccasionChanged("Work")

        vm.onSave("New outfit")

        val saved = outfits.created!!
        assertEquals("Office Monday", saved.name)
        assertEquals("Work", saved.occasion)
        assertEquals(listOf("jacket", "shirt"), saved.itemIds)
        assertEquals("outfit-new", vm.uiState.value.savedOutfitId)
    }

    @Test
    fun `an unnamed outfit gets the default name`() = runTest {
        val vm = newBuilder()
        vm.onAdd("shirt")
        vm.onSave("New outfit")

        assertEquals("New outfit", outfits.created?.name)
    }

    @Test
    fun `editing loads the outfit and updates it instead of making a new one`() = runTest {
        outfits.outfits = listOf(
            Outfit(
                id = "o1",
                name = "Friday",
                itemIds = listOf("shirt"),
                occasion = "Evening",
                layers = listOf(OutfitLayer("shirt", x = 0.3f, y = 0.4f)),
            )
        )
        val vm = OutfitBuilderViewModel(outfitId = "o1", outfitRepository = outfits, itemRepository = items)

        assertTrue(vm.uiState.value.isEditing)
        assertEquals("Friday", vm.uiState.value.name)
        assertEquals(0.3f, vm.uiState.value.layers.single().x)

        vm.onAdd("jeans")
        vm.onSave("New outfit")

        assertNull(outfits.created)
        assertEquals(listOf("shirt", "jeans"), outfits.updated?.itemIds)
    }



    @Test
    fun `an outfit that can't be opened for editing says so`() = runTest {
        outfits.getError = IOException("offline")
        val vm = OutfitBuilderViewModel(outfitId = "o1", outfitRepository = outfits, itemRepository = items)

        assertTrue(vm.uiState.value.loadFailed)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `a failed save keeps everything on the canvas`() = runTest {
        outfits.saveError = IOException("offline")
        val vm = newBuilder()
        vm.onAdd("shirt")

        vm.onSave("New outfit")

        assertEquals(1, vm.uiState.value.layers.size)
        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.savedOutfitId)
    }
}
