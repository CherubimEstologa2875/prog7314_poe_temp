package com.example.runway.ui.outfits

import androidx.lifecycle.SavedStateHandle
import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.OutfitRating
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitLayer
import com.example.runway.fake.FakeItemRepository
import com.example.runway.fake.FakeOutfitRepository
import com.example.runway.fake.FakePlanRepository
import com.example.runway.fake.FakeRatingRepository
import com.example.runway.ui.navigation.NavArgs
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

// SCRUM-144 and 146: the outfit screen, including its confidence ratings.
class OutfitDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val outfit = Outfit(
        id = "o1",
        name = "Friday dinner",
        itemIds = listOf("shirt", "jeans"),
        occasion = "Evening",
        layers = listOf(OutfitLayer("shirt", x = 0.3f, y = 0.3f), OutfitLayer("jeans", x = 0.5f, y = 0.7f, z = 1)),
    )
    private val outfits = FakeOutfitRepository(outfit)
    private val items = FakeItemRepository()
    private val ratings = FakeRatingRepository()
    private val plans = FakePlanRepository()

    private fun viewModel() = OutfitDetailViewModel(
        SavedStateHandle(mapOf(NavArgs.OUTFIT_ID to "o1")),
        outfits,
        items,
        ratings,
        plans,
    )

    private fun wardrobe() = items.emit(
        listOf(Item(id = "shirt", name = "Shirt", category = "TOP"), Item(id = "jeans", name = "Jeans", category = "BOTTOM"))
    )

    @Test
    fun `loads the outfit and names its garments from the wardrobe`() = runTest {
        wardrobe()
        val state = viewModel().uiState.value

        assertEquals("Friday dinner", state.name)
        assertEquals(listOf("Shirt", "Jeans"), state.garments.map { it.name })
        assertFalse(state.isLoading)
    }

    @Test
    fun `an outfit with no ratings shows none, safely`() = runTest {
        wardrobe()
        val state = viewModel().uiState.value

        assertFalse(state.ratings.hasRatings)
        assertEquals(0, state.ratings.roundedAverage)
    }

    @Test
    fun `existing ratings are loaded with the outfit`() = runTest {
        wardrobe()
        ratings.seed(OutfitRating(outfitId = "o1", score = 5), OutfitRating(outfitId = "o1", score = 4))

        val state = viewModel().uiState.value

        assertTrue(state.ratings.hasRatings)
        assertEquals(5, state.ratings.roundedAverage)
    }

    @Test
    fun `a new rating shows up once the sheet closes`() = runTest {
        wardrobe()
        val vm = viewModel()
        ratings.seed(OutfitRating(outfitId = "o1", score = 3))

        vm.onRatingsChanged()

        assertEquals(1, vm.uiState.value.ratings.count)
    }

    @Test
    fun `wearing it asks for a rating`() = runTest {
        wardrobe()
        val vm = viewModel()

        vm.onWoreToday()

        assertEquals("o1", outfits.lastWornId)
        assertNotNull(vm.uiState.value.wearLoggedAt)
        vm.onWearHandled()
        assertNull(vm.uiState.value.wearLoggedAt)
    }

    @Test
    fun `a failed wear doesn't ask for a rating`() = runTest {
        wardrobe()
        outfits.wearError = IOException("offline")
        val vm = viewModel()

        vm.onWoreToday()

        assertNull(vm.uiState.value.wearLoggedAt)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `saving a rename keeps the canvas layout`() = runTest {
        wardrobe()
        val vm = viewModel()

        vm.onNameChanged("Saturday lunch")
        vm.onSave()

        assertEquals("Saturday lunch", outfits.updated?.name)
        assertEquals(outfit.layers, outfits.updated?.layers)
    }

    @Test
    fun `a blank name isn't saved`() = runTest {
        wardrobe()
        val vm = viewModel()

        vm.onNameChanged("   ")
        vm.onSave()

        assertNull(outfits.updated)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `an outfit that can't be fetched shows a retry instead of loading forever`() = runTest {
        outfits.getError = IOException("offline")
        val vm = viewModel()

        assertTrue(vm.uiState.value.loadFailed)
        assertFalse(vm.uiState.value.isLoading)

        outfits.getError = null
        wardrobe()
        vm.reload()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals("Friday dinner", vm.uiState.value.name)
    }

    @Test
    fun `ratings that fail to load are not reported as having none`() = runTest {
        ratings.listError = IOException("offline")
        val vm = viewModel()

        assertTrue(vm.uiState.value.ratingsFailed)
        assertFalse(vm.uiState.value.ratings.hasRatings)

        ratings.listError = null
        vm.onRatingsChanged()
        assertFalse(vm.uiState.value.ratingsFailed)
    }

    @Test
    fun `scheduling plans the outfit for the day picked`() = runTest {
        val vm = viewModel()
        val friday = LocalDate.of(2026, 9, 25)

        vm.onScheduled(friday)

        assertEquals("o1", plans.plans[friday])
        assertEquals(friday, vm.uiState.value.scheduledFor)
    }

    @Test
    fun `a schedule that fails says so and plans nothing`() = runTest {
        plans.planError = IOException("offline")
        val vm = viewModel()

        vm.onScheduled(LocalDate.of(2026, 9, 25))

        assertTrue(plans.plans.isEmpty())
        assertNull(vm.uiState.value.scheduledFor)
        assertNotNull(vm.uiState.value.errorMessage)
    }
}
