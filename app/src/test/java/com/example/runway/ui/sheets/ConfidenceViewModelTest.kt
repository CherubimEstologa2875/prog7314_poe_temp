package com.example.runway.ui.sheets

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Outfit
import com.example.runway.fake.FakeOutfitRepository
import com.example.runway.fake.FakeRatingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConfidenceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        ratings: FakeRatingRepository = FakeRatingRepository(),
        outfits: FakeOutfitRepository = FakeOutfitRepository(),
    ) = ConfidenceViewModel("outfit-1", ratings, outfits)

    @Test
    fun `starts with no score chosen and cannot save`() {
        val state = viewModel().uiState.value
        assertEquals(0, state.score)
        assertFalse(state.canSave)
    }

    @Test
    fun `shows the outfit name in the prompt`() = runTest {
        val outfits = FakeOutfitRepository(Outfit(id = "outfit-1", name = "Friday dinner"))
        val vm = viewModel(outfits = outfits)
        assertEquals("Friday dinner", vm.uiState.value.outfitName)
    }

    @Test
    fun `picking a score allows saving`() {
        val vm = viewModel()
        vm.onScoreChanged(4)
        assertEquals(4, vm.uiState.value.score)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `every score from one to five is accepted`() {
        val vm = viewModel()
        (1..5).forEach { score ->
            vm.onScoreChanged(score)
            assertEquals(score, vm.uiState.value.score)
        }
    }

    @Test
    fun `a score outside one to five is ignored`() {
        val vm = viewModel()
        vm.onScoreChanged(3)
        vm.onScoreChanged(0)
        vm.onScoreChanged(6)
        vm.onScoreChanged(-2)
        // The last valid score stands rather than being overwritten by nonsense.
        assertEquals(3, vm.uiState.value.score)
    }

    @Test
    fun `saving without a score never reaches the repository`() {
        val ratings = FakeRatingRepository()
        viewModel(ratings = ratings).onSave()
        assertEquals(0, ratings.rateCount)
    }

    @Test
    fun `a rating is saved against the right outfit`() = runTest {
        val ratings = FakeRatingRepository()
        val vm = viewModel(ratings = ratings)

        vm.onScoreChanged(5)
        vm.onNoteChanged("  felt great  ")
        vm.onSave()

        assertEquals(1, ratings.rateCount)
        assertEquals("outfit-1", ratings.lastSaved?.outfitId)
        assertEquals(5, ratings.lastSaved?.score)
        assertEquals("felt great", ratings.lastSaved?.note)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `a blank note is stored as no note at all`() = runTest {
        val ratings = FakeRatingRepository()
        val vm = viewModel(ratings = ratings)

        vm.onScoreChanged(3)
        vm.onNoteChanged("   ")
        vm.onSave()

        assertNull(ratings.lastSaved?.note)
    }

    @Test
    fun `a failed save is reported and does not close the sheet`() = runTest {
        val ratings = FakeRatingRepository()
        ratings.rateError = IOException("no connection")
        val vm = viewModel(ratings = ratings)

        vm.onScoreChanged(4)
        vm.onSave()

        assertNull(vm.uiState.value.savedAt)
        // Ratings have no offline queue, so we must not promise a later sync.
        assertEquals(
            "You are offline. Please try again once you have a connection.",
            vm.uiState.value.errorMessage,
        )
    }

    @Test
    fun `an outfit whose name cannot be loaded can still be rated`() = runTest {
        val outfits = FakeOutfitRepository()
        outfits.getError = IOException("no connection")
        val ratings = FakeRatingRepository()
        val vm = viewModel(ratings = ratings, outfits = outfits)

        vm.onScoreChanged(4)
        vm.onSave()

        assertEquals("", vm.uiState.value.outfitName)
        assertEquals(1, ratings.rateCount)
    }
}
