package com.example.runway.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.data.remote.weather.CurrentWeather
import com.example.runway.data.remote.weather.WeatherApi
import com.example.runway.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class WeatherViewModel(
    itemRepository: ItemRepository,
    private val weatherApi: WeatherApi = WeatherApi.instance,
) : ViewModel() {
    private val weather = MutableStateFlow<CurrentWeather?>(null)
    private val failed = MutableStateFlow(false)

    val uiState: StateFlow<WeatherUiState> =
        combine(itemRepository.observeItems(), weather, failed) { items, current, hasFailed ->
            if (current == null) return@combine WeatherUiState(isLoading = !hasFailed, failed = hasFailed)

            val needsJacket = current.temperature < COLD_BELOW || isWet(current.code)
            val isHot = current.temperature > HOT_ABOVE
            WeatherUiState(
                isLoading = false,
                temperature = current.temperature.roundToInt(),
                condition = conditionFor(current.code),
                advice = when {
                    needsJacket -> R.string.rw_weather_advice_jacket
                    isHot -> R.string.rw_weather_advice_light
                    else -> R.string.rw_weather_advice_mild
                },
                items = items.filter { item ->
                    val isOuterwear = item.category.equals(OUTERWEAR, ignoreCase = true)
                    !item.archived && when {
                        needsJacket -> isOuterwear
                        isHot -> !isOuterwear
                        else -> true
                    }
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeatherUiState(),
        )

    init {
        load()
    }

    fun load() {
        failed.value = false
        viewModelScope.launch {
            runCatching { weatherApi.current().current }
                .onSuccess { weather.value = it }
                .onFailure { failed.value = true }
        }
    }

    companion object {
        const val COLD_BELOW = 15
        const val HOT_ABOVE = 25
        const val OUTERWEAR = "OUTERWEAR"

        // WMO weather codes as used by Open-Meteo
        private fun isWet(code: Int) = code in 51..67 || code in 80..82 || code >= 95

        private fun conditionFor(code: Int) = when {
            code <= 1 -> R.string.rw_weather_sunny
            code >= 95 -> R.string.rw_weather_stormy
            isWet(code) -> R.string.rw_weather_rainy
            else -> R.string.rw_weather_cloudy
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                WeatherViewModel(app.container.itemRepository)
            }
        }
    }
}