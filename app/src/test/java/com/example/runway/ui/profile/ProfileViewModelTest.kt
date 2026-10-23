package com.example.runway.ui.profile

import com.example.runway.MainDispatcherRule
import com.example.runway.data.auth.AuthSession
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.model.WardrobeSummary
import com.example.runway.fake.FakeItemRepository
import com.example.runway.fake.FakeProfileRepository
import com.example.runway.fake.FakeWardrobeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.YearMonth
import java.time.ZoneOffset

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = FakeItemRepository()
    private val wardrobe = FakeWardrobeRepository()
    private val profiles = FakeProfileRepository()

    private val session = AuthSession(
        id = "uid-1",
        email = "divan@example.com",
        displayName = "Divan Fourie",
        photoUrl = null,
        signedInAtMillis = 0L,
        biometricEnabled = false,
    )

    private fun viewModel(currentSession: AuthSession? = session) = ProfileViewModel(
        itemRepository = items,
        wardrobeRepository = wardrobe,
        profileRepository = profiles,
        currentSession = { currentSession },
        zone = ZoneOffset.UTC,
    )

    @Test
    fun `shows the session straight away without waiting for the API`() {
        val state = viewModel().uiState.value

        assertEquals("Divan Fourie", state.name)
        assertEquals("divan@example.com", state.email)
        assertEquals("DF", state.initials)
        assertNull(state.memberSince)
    }

    @Test
    fun `the profile call fills in the member since month`() = runTest {
        profiles.profile = UserProfile(
            displayName = "Divan Fourie",
            email = "divan@example.com",
            createdAt = "2026-08-14T10:15:30Z",
        )
        val vm = viewModel()
        vm.refresh()

        assertEquals(YearMonth.of(2026, 8), vm.uiState.value.memberSince)
    }

    @Test
    fun `a blank name from the API does not wipe the one we had`() = runTest {
        profiles.profile = UserProfile(displayName = "", email = "", createdAt = null)
        val vm = viewModel()
        vm.refresh()

        assertEquals("Divan Fourie", vm.uiState.value.name)
        assertEquals("DF", vm.uiState.value.initials)
    }

    @Test
    fun `a bad date is ignored rather than crashing`() = runTest {
        profiles.profile = UserProfile(displayName = "Divan", createdAt = "last tuesday")
        val vm = viewModel()
        vm.refresh()

        assertNull(vm.uiState.value.memberSince)
    }

    @Test
    fun `a failed profile call keeps the session details`() = runTest {
        profiles.error = IOException("offline")
        val vm = viewModel()
        vm.refresh()

        assertEquals("Divan Fourie", vm.uiState.value.name)
        assertNull(vm.uiState.value.memberSince)
    }

    @Test
    fun `stats come from the API, or from Room when it is down`() = runTest {
        items.emit(listOf(Item(id = "a", name = "Shirt", category = "TOP", purchasePrice = 250.0)))

        wardrobe.summary = WardrobeSummary(itemCount = 7, outfitCount = 2, totalValue = 900.0, needsWashCount = 1)
        val online = viewModel().apply { refresh() }
        assertEquals(7, online.uiState.value.summary.itemCount)
        assertEquals(1, online.uiState.value.needsWashCount)

        wardrobe.error = IOException("offline")
        val offline = viewModel().apply { refresh() }
        assertEquals(1, offline.uiState.value.summary.itemCount)
        assertEquals(250.0, offline.uiState.value.summary.totalValue, 0.001)
        assertNull(offline.uiState.value.summary.outfitCount)
    }

    @Test
    fun `no session shows nothing rather than crashing`() {
        val state = viewModel(currentSession = null).uiState.value

        assertEquals("", state.name)
        assertEquals("", state.initials)
    }
}
