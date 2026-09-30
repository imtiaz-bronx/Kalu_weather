package com.imtiazmahmud.KaluWeather

import androidx.compose.ui.graphics.ImageBitmap
import java.util.Locale

// =====================================================================
//  DATA CLASSES: simple containers that hold related values together.
//  A "?" after a type (Double?) means "this might be missing".
//  Open-Meteo doesn't have every value for every place, so we allow it.
// =====================================================================

data class Place(
    val name: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    val country: String = "",
)

/** A short id for a place, e.g. "40.84,-73.86". Used to spot duplicates. */
val Place.key: String
    get() = "%.2f,%.2f".format(Locale.US, lat, lon)

data class DayForecast(
    val date: String,
    val code: Int,
    val max: Double,
    val min: Double,
    val sunrise: String = "",
    val sunset: String = "",
    val precipChance: Int? = null,        // %
    val precipSumMm: Double? = null,      // mm
    val snowfallCm: Double? = null,       // cm
    val windMaxMph: Double? = null,
    val gustMaxMph: Double? = null,
    val windDirDominant: Int? = null,     // degrees the wind comes FROM
    val uvMax: Double? = null,
    val daylightSec: Double? = null,
    val sunshineSec: Double? = null,
)

data class HourForecast(
    val time: String,
    val code: Int,
    val isDay: Boolean,
    val temp: Double,
    val precipChance: Int? = null,
)

data class AirQuality(
    val usAqi: Int? = null,
    val euAqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val ozone: Double? = null,
    val no2: Double? = null,
    val co: Double? = null,
    val so2: Double? = null,
    val dust: Double? = null,
    val pollen: Map<String, Double> = emptyMap(),   // e.g. "Grass" -> 12.0 (Europe only)
)

data class Weather(
    val place: Place,
    val updated: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val windMph: Double,
    val windDirection: Int,
    val code: Int,
    val isDay: Boolean,
    val pressureHpa: Double,
    val uvIndex: Double,
    val sunrise: String,
    val sunset: String,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>,
    // extra details
    val windGustsMph: Double? = null,
    val cloudCover: Int? = null,
    val precipitationMm: Double? = null,
    val visibilityM: Double? = null,
    val dewPointC: Double? = null,
    val surfacePressureHpa: Double? = null,
    val air: AirQuality? = null,
)

/** Just enough weather to show next to a favorite city. */
data class QuickWeather(val tempC: Double, val code: Int, val isDay: Boolean)

data class Quake(val mag: Double, val place: String, val time: Long, val url: String)

data class Artwork(
    val title: String,
    val artist: String,
    val date: String,
    val url: String,
    val image: ImageBitmap,
)

data class HistoryEvent(val year: Int, val text: String, val url: String)

/** The Today page is always in one of these three states. */
sealed interface UiState {
    data object Loading : UiState
    data class Ready(val weather: Weather) : UiState
    data class Error(val message: String) : UiState
}

/** Same idea, but reusable for any kind of data (earthquakes, art, ...). */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Done<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}
