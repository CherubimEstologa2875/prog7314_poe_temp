package com.example.runway.ui.profile

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.model.ThemeOption
import com.example.runway.fake.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts on the default settings`() {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        assertEquals(RunwaySettings(), viewModel.uiState.value.settings)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `restores whatever was saved before`() = runTest {
        val saved = RunwaySettings(
            theme = ThemeOption.NIGHT,
            language = RunwaySettings.LANGUAGE_ZULU,
            notifyWash = false,
            defaultWearLimit = 12,
        )
        val viewModel = SettingsViewModel(FakeSettingsRepository(saved))

        val job = collect(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(saved, viewModel.uiState.value.settings)

        job.cancel()
    }

    @Test
    fun `changing the theme is saved`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val job = collect(viewModel)

        viewModel.onThemeSelected(ThemeOption.NIGHT)

        assertEquals(ThemeOption.NIGHT, repository.currentSettings().theme)
        assertEquals(1, repository.updateCount)

        job.cancel()
    }

    @Test
    fun `a wear limit under the minimum is clamped`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val job = collect(viewModel)

        viewModel.onWearLimitChanged(0)

        assertEquals(RunwaySettings.MIN_WEAR_LIMIT, repository.currentSettings().defaultWearLimit)

        job.cancel()
    }

    @Test
    fun `a wear limit over the maximum is clamped`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val job = collect(viewModel)

        viewModel.onWearLimitChanged(99)

        assertEquals(RunwaySettings.MAX_WEAR_LIMIT, repository.currentSettings().defaultWearLimit)

        job.cancel()
    }

    @Test
    fun `a language we do not support is ignored`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val job = collect(viewModel)

        viewModel.onLanguageSelected("fr")

        assertEquals(RunwaySettings.LANGUAGE_ENGLISH, repository.currentSettings().language)
        assertEquals(0, repository.updateCount)

        job.cancel()
    }

    @Test
    fun `a supported language is saved`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val job = collect(viewModel)

        viewModel.onLanguageSelected(RunwaySettings.LANGUAGE_AFRIKAANS)

        assertEquals(RunwaySettings.LANGUAGE_AFRIKAANS, repository.currentSettings().language)

        job.cancel()
    }

    @Test
    fun `turning a notification off is saved`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val job = collect(viewModel)

        viewModel.onNotifySwapChanged(false)

        assertFalse(repository.currentSettings().notifySwap)

        job.cancel()
    }

    @Test
    fun `resetting puts everything back to the defaults`() = runTest {
        val changed = RunwaySettings(
            theme = ThemeOption.DAY,
            biometricsEnabled = true,
            notifyWeather = false,
            defaultWearLimit = 20,
        )
        val repository = FakeSettingsRepository(changed)
        val viewModel = SettingsViewModel(repository)
        val job = collect(viewModel)

        viewModel.onResetToDefaults()

        assertEquals(RunwaySettings(), repository.currentSettings())
        assertEquals(1, repository.resetCount)

        job.cancel()
    }

    // uiState only produces values while something is collecting it.
    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: SettingsViewModel) =
        launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
}
