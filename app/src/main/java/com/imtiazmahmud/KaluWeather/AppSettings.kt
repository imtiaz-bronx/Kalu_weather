package com.imtiazmahmud.KaluWeather

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

// =====================================================================
//  SETTINGS + FAVORITES that are remembered after the app closes.
//  "mutableStateOf" = when this changes, redraw every screen that uses it.
//  SharedPreferences = a small notebook on the phone where we save values.
//  Favorites are saved as JSON text, the same format websites use.
// =====================================================================
object AppSettings {
    var fahrenheit by mutableStateOf(false)
    var iconStyle by mutableStateOf("fill")          // fill, flat, line, monochrome
    var favorites by mutableStateOf<List<Place>>(emptyList())

    private const val FILE = "kalu_settings"

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        fahrenheit = prefs.getBoolean("fahrenheit", false)
        iconStyle = prefs.getString("iconStyle", "fill") ?: "fill"
        favorites = favoritesFromJson(prefs.getString("favorites", "[]") ?: "[]")
    }

    fun save(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("fahrenheit", fahrenheit)
            .putString("iconStyle", iconStyle)
            .putString("favorites", favoritesToJson(favorites))
            .apply()
    }

    fun isFavorite(place: Place): Boolean = favorites.any { it.key == place.key }

    fun toggleFavorite(context: Context, place: Place) {
        favorites = if (isFavorite(place)) {
            favorites.filterNot { it.key == place.key }
        } else {
            favorites + place
        }
        save(context)
    }

    private fun favoritesToJson(list: List<Place>): String {
        val array = JSONArray()
        list.forEach { p ->
            array.put(
                JSONObject()
                    .put("name", p.name)
                    .put("region", p.region)
                    .put("lat", p.lat)
                    .put("lon", p.lon)
                    .put("country", p.country)
            )
        }
        return array.toString()
    }

    private fun favoritesFromJson(text: String): List<Place> = try {
        val array = JSONArray(text)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Place(
                name = o.getString("name"),
                region = o.optString("region", ""),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                country = o.optString("country", ""),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
