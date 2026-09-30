package com.imtiazmahmud.KaluWeather

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.roundToInt

// =====================================================================
//  WEATHER DATA from Open-Meteo.com (free, no API key).
//  Full list of what you can ask for: https://open-meteo.com/en/docs
//  To add a value: 1) add its name to the URL below, 2) read it in the
//  "parse" part, 3) add a field for it in Models.kt, 4) show it.
// =====================================================================
object WeatherRepository {

    // ---- small helpers: read a number, or null if it's missing ----
    private fun JSONObject.num(name: String): Double? =
        if (!has(name) || isNull(name)) null else optDouble(name).takeUnless { it.isNaN() }

    private fun JSONArray?.num(i: Int): Double? =
        if (this == null || i >= length() || isNull(i)) null else optDouble(i).takeUnless { it.isNaN() }

    private fun JSONArray?.str(i: Int): String =
        if (this == null || i >= length() || isNull(i)) "" else optString(i, "")

    /** Finds up to [count] places that match the text the user typed. */
    suspend fun searchCities(query: String, count: Int = 6): List<Place> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = Http.getJson("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=$count&language=en")
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            Place(
                name = r.getString("name"),
                region = r.optString("admin1", ""),
                lat = r.getDouble("latitude"),
                lon = r.getDouble("longitude"),
                country = r.optString("country", ""),
            )
        }
    }

    suspend fun findCity(query: String): Place =
        searchCities(query, 1).firstOrNull() ?: error("City \"$query\" not found")

    /** Small, fast request: just temperature + icon, for the favorites list. */
    suspend fun quickCurrent(place: Place): QuickWeather {
        val cur = Http.getJson(
            "https://api.open-meteo.com/v1/forecast?latitude=${place.lat}&longitude=${place.lon}" +
                "&current=temperature_2m,weather_code,is_day&timezone=auto"
        ).getJSONObject("current")
        return QuickWeather(
            tempC = cur.getDouble("temperature_2m"),
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
        )
    }

    /** Everything the Today page needs. */
    suspend fun forecast(place: Place): Weather {
        val json = Http.getJson(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.lat}&longitude=${place.lon}" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,is_day," +
                "wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl,surface_pressure," +
                "uv_index,cloud_cover,precipitation,visibility,dew_point_2m" +
                "&hourly=temperature_2m,weather_code,is_day,precipitation_probability" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
                "precipitation_sum,precipitation_probability_max,snowfall_sum," +
                "wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant," +
                "uv_index_max,daylight_duration,sunshine_duration" +
                "&wind_speed_unit=mph&timezone=auto&forecast_days=7"
        )
        val cur = json.getJSONObject("current")

        // ---- daily ----
        val d = json.getJSONObject("daily")
        val dates = d.getJSONArray("time")
        val daily = (0 until dates.length()).map { i ->
            DayForecast(
                date = dates.getString(i),
                code = d.optJSONArray("weather_code").num(i)?.toInt() ?: -1,
                max = d.optJSONArray("temperature_2m_max").num(i) ?: 0.0,
                min = d.optJSONArray("temperature_2m_min").num(i) ?: 0.0,
                sunrise = d.optJSONArray("sunrise").str(i),
                sunset = d.optJSONArray("sunset").str(i),
                precipChance = d.optJSONArray("precipitation_probability_max").num(i)?.roundToInt(),
                precipSumMm = d.optJSONArray("precipitation_sum").num(i),
                snowfallCm = d.optJSONArray("snowfall_sum").num(i),
                windMaxMph = d.optJSONArray("wind_speed_10m_max").num(i),
                gustMaxMph = d.optJSONArray("wind_gusts_10m_max").num(i),
                windDirDominant = d.optJSONArray("wind_direction_10m_dominant").num(i)?.roundToInt(),
                uvMax = d.optJSONArray("uv_index_max").num(i),
                daylightSec = d.optJSONArray("daylight_duration").num(i),
                sunshineSec = d.optJSONArray("sunshine_duration").num(i),
            )
        }

        // ---- hourly: keep the next 24 hours, starting with the current hour ----
        val h = json.getJSONObject("hourly")
        val hTimes = h.getJSONArray("time")
        val thisHour = cur.getString("time").take(13)          // e.g. "2026-09-29T20"
        val hourly = (0 until hTimes.length()).map { i ->
            HourForecast(
                time = hTimes.getString(i),
                code = h.optJSONArray("weather_code").num(i)?.toInt() ?: -1,
                isDay = h.optJSONArray("is_day").num(i) == 1.0,
                temp = h.optJSONArray("temperature_2m").num(i) ?: 0.0,
                precipChance = h.optJSONArray("precipitation_probability").num(i)?.roundToInt(),
            )
        }.filter { it.time.take(13) >= thisHour }.take(24)

        return Weather(
            place = place,
            updated = cur.getString("time"),
            temperatureC = cur.getDouble("temperature_2m"),
            feelsLikeC = cur.num("apparent_temperature") ?: cur.getDouble("temperature_2m"),
            humidity = cur.num("relative_humidity_2m")?.roundToInt() ?: 0,
            windMph = cur.num("wind_speed_10m") ?: 0.0,
            windDirection = cur.num("wind_direction_10m")?.roundToInt() ?: 0,
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
            pressureHpa = cur.num("pressure_msl") ?: 0.0,
            uvIndex = cur.num("uv_index") ?: 0.0,
            sunrise = daily.firstOrNull()?.sunrise ?: "",
            sunset = daily.firstOrNull()?.sunset ?: "",
            hourly = hourly,
            daily = daily,
            windGustsMph = cur.num("wind_gusts_10m"),
            cloudCover = cur.num("cloud_cover")?.roundToInt(),
            precipitationMm = cur.num("precipitation"),
            visibilityM = cur.num("visibility"),
            dewPointC = cur.num("dew_point_2m"),
            surfacePressureHpa = cur.num("surface_pressure"),
            air = airQuality(place),
        )
    }

    /** Air quality + pollen. Returns null if the service can't be reached. */
    private suspend fun airQuality(place: Place): AirQuality? {
        val base = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=${place.lat}&longitude=${place.lon}&timezone=auto"

        val aq = try {
            Http.getJson(
                "$base&current=us_aqi,european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide," +
                    "carbon_monoxide,sulphur_dioxide,dust"
            ).getJSONObject("current")
        } catch (e: Exception) {
            return null
        }

        // Pollen is only measured in Europe, so this often comes back empty.
        val pollenNames = listOf(
            "grass_pollen" to "Grass", "birch_pollen" to "Birch", "alder_pollen" to "Alder",
            "ragweed_pollen" to "Ragweed", "mugwort_pollen" to "Mugwort", "olive_pollen" to "Olive",
        )
        val pollen = try {
            val p = Http.getJson("$base&current=" + pollenNames.joinToString(",") { it.first })
                .getJSONObject("current")
            pollenNames.mapNotNull { (key, label) -> p.num(key)?.let { label to it } }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }

        return AirQuality(
            usAqi = aq.num("us_aqi")?.roundToInt(),
            euAqi = aq.num("european_aqi")?.roundToInt(),
            pm25 = aq.num("pm2_5"),
            pm10 = aq.num("pm10"),
            ozone = aq.num("ozone"),
            no2 = aq.num("nitrogen_dioxide"),
            co = aq.num("carbon_monoxide"),
            so2 = aq.num("sulphur_dioxide"),
            dust = aq.num("dust"),
            pollen = pollen,
        )
    }
}
