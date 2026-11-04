package com.example.runway.fake

import com.example.runway.data.remote.weather.CurrentWeather
import com.example.runway.data.remote.weather.WeatherApi
import com.example.runway.data.remote.weather.WeatherResponse

// Stands in for the Open-Meteo weather service (Open-Meteo, 2026) so the weather screen can be tested offline.
// A fake is a working but test-only implementation, and fakes are preferred over mocks (Android Developers, 2026).
class FakeWeatherApi(
    var response: Result<WeatherResponse> = Result.success(WeatherResponse(CurrentWeather(20.0, 2)))
) : WeatherApi {

    var callCount = 0
        private set

    override suspend fun current(latitude: Double, longitude: Double, fields: String): WeatherResponse {
        callCount++
        return response.getOrThrow()
    }
}

/* Reference List
Android Developers, 2026. Use test doubles in Android. [online] Available at: <https://developer.android.com/training/testing/fundamentals/test-doubles> [Accessed 4 November 2026].
Open-Meteo, 2026. Weather Forecast API. [online] Available at: <https://open-meteo.com/en/docs> [Accessed 4 November 2026].
Square, Inc., n.d. Retrofit: A type-safe HTTP client for Android and Java. [online] Available at: <https://square.github.io/retrofit/> [Accessed 4 November 2026].
*/
