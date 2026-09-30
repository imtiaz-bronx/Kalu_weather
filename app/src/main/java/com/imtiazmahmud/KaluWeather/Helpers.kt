package com.imtiazmahmud.KaluWeather

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

// =====================================================================
//  SMALL HELPER FUNCTIONS (no screens here)
//  Turning weather codes into icons/words, moon maths, unit conversion,
//  date formatting.
// =====================================================================

/** Weather code (WMO, used by Open-Meteo) -> Meteocons icon name. */
fun meteoconFor(code: Int, isDay: Boolean): String {
    val dn = if (isDay) "day" else "night"
    return when (code) {
        0 -> "clear-$dn"
        1 -> "mostly-clear-$dn"
        2 -> "partly-cloudy-$dn"
        3 -> "overcast-$dn"
        45, 48 -> "fog-$dn"
        51, 53, 55 -> "drizzle"
        56, 57, 66, 67 -> "sleet"
        61, 63 -> "rain"
        65, 82 -> "extreme-rain"
        71, 73, 77 -> "snow"
        75 -> "extreme-snow"
        80, 81 -> "partly-cloudy-$dn-rain"
        85, 86 -> "partly-cloudy-$dn-snow"
        95 -> "thunderstorms-$dn-rain"
        96, 99 -> "thunderstorms-$dn-hail"
        else -> "not-available"
    }
}

fun describe(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63 -> "Rain"
    65 -> "Heavy rain"
    66, 67 -> "Freezing rain"
    71, 73 -> "Snow"
    75 -> "Heavy snow"
    77 -> "Snow grains"
    80, 81 -> "Rain showers"
    82 -> "Violent rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Unknown"
}

data class MoonInfo(val name: String, val icon: String, val illuminationPercent: Int)

/** Moon phase calculated on the phone, no internet needed. */
fun moonInfo(nowMillis: Long = System.currentTimeMillis()): MoonInfo {
    val synodicMonth = 29.530588853
    val knownNewMoon = 947_182_440_000L // Jan 6, 2000 18:14 UTC
    val days = (nowMillis - knownNewMoon) / 86_400_000.0
    val age = ((days % synodicMonth) + synodicMonth) % synodicMonth
    val illumination = ((1 - cos(2 * PI * age / synodicMonth)) / 2 * 100).roundToInt()
    val phases = listOf(
        "New Moon" to "moon-new",
        "Waxing Crescent" to "moon-waxing-crescent",
        "First Quarter" to "moon-first-quarter",
        "Waxing Gibbous" to "moon-waxing-gibbous",
        "Full Moon" to "moon-full",
        "Waning Gibbous" to "moon-waning-gibbous",
        "Last Quarter" to "moon-last-quarter",
        "Waning Crescent" to "moon-waning-crescent",
    )
    val (name, icon) = phases[((age / synodicMonth) * 8 + 0.5).toInt() % 8]
    return MoonInfo(name, icon, illumination)
}

fun compass(degrees: Int): String =
    listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[((degrees % 360 + 360) % 360 + 22) / 45 % 8]

fun aqiLabel(aqi: Int) = when {
    aqi <= 50 -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Unhealthy for sensitive groups"
    aqi <= 200 -> "Unhealthy"
    aqi <= 300 -> "Very unhealthy"
    else -> "Hazardous"
}

fun dustLabel(dust: Double) = when {
    dust < 50 -> "Low"
    dust < 150 -> "Moderate"
    else -> "High"
}

fun uvLabel(uv: Double) = when {
    uv < 3 -> "Low"
    uv < 6 -> "Moderate"
    uv < 8 -> "High"
    uv < 11 -> "Very High"
    else -> "Extreme"
}

// Temperatures: the big number follows the setting, the small one shows the other unit.
fun cToF(c: Double) = (c * 9 / 5 + 32).roundToInt()
fun mainTemp(c: Double) = if (AppSettings.fahrenheit) cToF(c) else c.roundToInt()
fun otherTemp(c: Double) = if (AppSettings.fahrenheit) c.roundToInt() else cToF(c)
fun mainUnit() = if (AppSettings.fahrenheit) "°F" else "°C"
fun otherUnit() = if (AppSettings.fahrenheit) "°C" else "°F"

private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

/** "2026-09-29T19:45" + "h:mm a" -> "7:45 PM" */
fun formatIso(iso: String, pattern: String): String =
    runCatching { SimpleDateFormat(pattern, Locale.US).format(isoFmt.parse(iso)!!) }.getOrDefault(iso)

/** "2026-09-30" -> "Wed" */
fun dayName(date: String): String =
    runCatching {
        SimpleDateFormat("EEE", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!)
    }.getOrDefault(date)

/** "The Bronx, New York" (or the country if there is no region). */
fun placeLabel(p: Place) =
    listOf(p.name, p.region.ifBlank { p.country }).filter { it.isNotBlank() }.joinToString(", ")

fun placeSubtitle(p: Place) =
    listOf(p.region, p.country).filter { it.isNotBlank() }.joinToString(", ")

// =====================================================================
//  EXTRA HELPERS for the detailed weather
//  Each value is shown twice, like your website: the unit you picked
//  in Settings first, the other unit second.
// =====================================================================
private fun fmt(value: Double, decimals: Int) = "%.${decimals}f".format(Locale.US, value)

/** Wind: km/h for °C people, mph for °F people. */
fun speedMain(mph: Double) =
    if (AppSettings.fahrenheit) "${mph.roundToInt()} mph" else "${(mph * 1.609344).roundToInt()} km/h"

fun speedOther(mph: Double) =
    if (AppSettings.fahrenheit) "${(mph * 1.609344).roundToInt()} km/h" else "${mph.roundToInt()} mph"

/** Rain/snow amounts: millimetres or inches. */
fun precipMain(mm: Double) = if (AppSettings.fahrenheit) "${fmt(mm / 25.4, 2)} in" else "${fmt(mm, 1)} mm"
fun precipOther(mm: Double) = if (AppSettings.fahrenheit) "${fmt(mm, 1)} mm" else "${fmt(mm / 25.4, 2)} in"

fun snowMain(cm: Double) = if (AppSettings.fahrenheit) "${fmt(cm / 2.54, 1)} in" else "${fmt(cm, 1)} cm"
fun snowOther(cm: Double) = if (AppSettings.fahrenheit) "${fmt(cm, 1)} cm" else "${fmt(cm / 2.54, 1)} in"

/** Visibility: kilometres or miles (one decimal when it's short). */
fun distanceMain(m: Double) = if (AppSettings.fahrenheit) miles(m) else km(m)
fun distanceOther(m: Double) = if (AppSettings.fahrenheit) km(m) else miles(m)
private fun km(m: Double) = if (m < 10_000) "${fmt(m / 1000, 1)} km" else "${(m / 1000).roundToInt()} km"
private fun miles(m: Double) = if (m < 16_000) "${fmt(m / 1609.344, 1)} mi" else "${(m / 1609.344).roundToInt()} mi"

/** Seconds -> "11 h 50 m" */
fun duration(seconds: Double): String {
    val totalMinutes = (seconds / 60).roundToInt()
    return "${totalMinutes / 60} h ${totalMinutes % 60} m"
}

/** Beaufort scale 0-12: how strong the wind feels, from "Calm" to "Hurricane". */
fun beaufort(mph: Double): Int {
    val limits = listOf(1, 4, 8, 13, 19, 25, 32, 39, 47, 55, 64, 73)
    val index = limits.indexOfFirst { mph < it }
    return if (index == -1) 12 else index
}

fun beaufortName(force: Int) = listOf(
    "Calm", "Light air", "Light breeze", "Gentle breeze", "Moderate breeze", "Fresh breeze",
    "Strong breeze", "Near gale", "Gale", "Strong gale", "Storm", "Violent storm", "Hurricane force",
)[force.coerceIn(0, 12)]

/** Wind direction arrow icon, e.g. "wind-direction-nw". */
fun windDirectionIcon(degrees: Int) = "wind-direction-${compass(degrees).lowercase(Locale.US)}"

fun uvIcon(uv: Double): String {
    val n = uv.roundToInt()
    return when {
        n <= 0 -> "uv-index"
        n >= 11 -> "uv-index-11-plus"
        else -> "uv-index-$n"
    }
}

fun pressureIcon(hpa: Double) = if (hpa >= 1013) "pressure-high" else "pressure-low"

fun euAqiLabel(aqi: Int) = when {
    aqi <= 20 -> "Good"
    aqi <= 40 -> "Fair"
    aqi <= 60 -> "Moderate"
    aqi <= 80 -> "Poor"
    aqi <= 100 -> "Very poor"
    else -> "Extremely poor"
}

/** Rough pollen levels (grains per cubic metre of air). */
fun pollenLabel(grains: Double) = when {
    grains < 1 -> "None"
    grains < 20 -> "Low"
    grains < 100 -> "Moderate"
    grains < 500 -> "High"
    else -> "Very high"
}

fun cloudLabel(percent: Int) = when {
    percent < 12 -> "Clear"
    percent < 38 -> "Mostly clear"
    percent < 63 -> "Partly cloudy"
    percent < 88 -> "Mostly cloudy"
    else -> "Overcast"
}

/** "2026-09-30" -> "Wednesday, Sep 30" */
fun longDayName(date: String): String =
    runCatching {
        SimpleDateFormat("EEEE, MMM d", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!)
    }.getOrDefault(date)
