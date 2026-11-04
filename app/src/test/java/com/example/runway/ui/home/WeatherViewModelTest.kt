package com.example.runway.ui.home

import com.example.runway.MainDispatcherRule
import com.example.runway.R
import com.example.runway.data.remote.weather.CurrentWeather
import com.example.runway.data.remote.weather.WeatherResponse
import com.example.runway.domain.model.Item
import com.example.runway.fake.FakeItemRepository
import com.example.runway.fake.FakeWeatherApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

// SCRUM-172: unit tests for the weather screen's outfit suggestions.
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    // Replaces Dispatchers.Main with a test dispatcher for each test (Android Developers, 2026a).
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val coat = Item(id = "1", name = "Black wool coat", category = "OUTERWEAR")
    private val tee = Item(id = "2", name = "White tee", category = "TOP")
    private val archivedParka = Item(id = "3", name = "Old parka", category = "outerwear", archived = true)

    // Codes are the WMO weather interpretation codes that Open-Meteo returns (Open-Meteo, 2026).
    private fun weather(temp: Double, code: Int) =
        Result.success(WeatherResponse(CurrentWeather(temperature = temp, code = code)))

    private fun repoWithItems() = FakeItemRepository().apply { emit(listOf(coat, tee, archivedParka)) }

    // A StateFlow made with stateIn only updates while it has a collector, so each test starts an empty one (Android Developers, 2026b).
    private fun TestScope.startCollecting(viewModel: WeatherViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
    }

    @Test
    fun `cold weather suggests a jacket and only shows outerwear`() = runTest {
        val viewModel = WeatherViewModel(repoWithItems(), FakeWeatherApi(weather(10.0, 2)))
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertEquals(R.string.rw_weather_advice_jacket, state.advice)
        assertEquals(listOf(coat), state.items)
    }

    @Test
    fun `rain on a warm day still suggests a jacket`() = runTest {
        val viewModel = WeatherViewModel(repoWithItems(), FakeWeatherApi(weather(22.0, 61)))
        startCollecting(viewModel)

        assertEquals(R.string.rw_weather_advice_jacket, viewModel.uiState.value.advice)
        assertEquals(R.string.rw_weather_rainy, viewModel.uiState.value.condition)
    }

    @Test
    fun `thunderstorms are shown as stormy`() = runTest {
        val viewModel = WeatherViewModel(repoWithItems(), FakeWeatherApi(weather(20.0, 95)))
        startCollecting(viewModel)

        assertEquals(R.string.rw_weather_stormy, viewModel.uiState.value.condition)
        assertEquals(R.string.rw_weather_advice_jacket, viewModel.uiState.value.advice)
    }

    @Test
    fun `hot weather suggests light clothes and hides outerwear`() = runTest {
        val viewModel = WeatherViewModel(repoWithItems(), FakeWeatherApi(weather(30.0, 0)))
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertEquals(R.string.rw_weather_advice_light, state.advice)
        assertEquals(R.string.rw_weather_sunny, state.condition)
        assertEquals(listOf(tee), state.items)
    }

    @Test
    fun `mild weather shows every item that is not archived`() = runTest {
        val viewModel = WeatherViewModel(repoWithItems(), FakeWeatherApi(weather(20.0, 2)))
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertEquals(R.string.rw_weather_advice_mild, state.advice)
        assertEquals(R.string.rw_weather_cloudy, state.condition)
        assertEquals(listOf(coat, tee), state.items)
    }

    @Test
    fun `exactly 15 and exactly 25 degrees count as mild`() = runTest {
        listOf(15.0, 25.0).forEach { temp ->
            val viewModel = WeatherViewModel(repoWithItems(), FakeWeatherApi(weather(temp, 2)))
            startCollecting(viewModel)
            assertEquals("at $temp", R.string.rw_weather_advice_mild, viewModel.uiState.value.advice)
        }
    }

    @Test
    fun `temperature is rounded for display`() = runTest {
        val viewModel = WeatherViewModel(repoWithItems(), FakeWeatherApi(weather(18.6, 2)))
        startCollecting(viewModel)

        assertEquals(19, viewModel.uiState.value.temperature)
    }

    @Test
    fun `a failed request shows the error state and retry recovers`() = runTest {
        val api = FakeWeatherApi(Result.failure(IOException("no network")))
        val viewModel = WeatherViewModel(repoWithItems(), api)
        startCollecting(viewModel)

        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.isLoading)

        api.response = weather(20.0, 2)
        viewModel.load()

        assertFalse(viewModel.uiState.value.failed)
        assertEquals(20, viewModel.uiState.value.temperature)
        assertEquals(2, api.callCount)
    }
}

/* Reference List
Android Developers, 2026a. Testing Kotlin coroutines on Android. [online] Available at: <https://developer.android.com/kotlin/coroutines/test> [Accessed 4 November 2026].
Android Developers, 2026b. Testing Kotlin flows on Android. [online] Available at: <https://developer.android.com/kotlin/flow/test> [Accessed 4 November 2026].
Open-Meteo, 2026. Weather Forecast API. [online] Available at: <https://open-meteo.com/en/docs> [Accessed 4 November 2026].
*/
