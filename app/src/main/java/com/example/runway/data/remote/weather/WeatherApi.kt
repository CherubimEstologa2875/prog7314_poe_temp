package com.example.runway.data.remote.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun current(
        @Query("latitude") latitude: Double = PRETORIA_LATITUDE,
        @Query("longitude") longitude: Double = PRETORIA_LONGITUDE,
        @Query("current", encoded = true) fields: String = "temperature_2m,weather_code",
    ): WeatherResponse

    companion object {
        const val PRETORIA_LATITUDE = -25.7479
        const val PRETORIA_LONGITUDE = 28.2293

        val instance: WeatherApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(WeatherApi::class.java)
        }
    }
}

@Serializable
data class WeatherResponse(val current: CurrentWeather)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("weather_code") val code: Int,
)

// References:
// 1. Open-Meteo. 2026. Weather Forecast API. [Online] Available at: https://open-meteo.com/en/docs [Accessed 27 October 2026].