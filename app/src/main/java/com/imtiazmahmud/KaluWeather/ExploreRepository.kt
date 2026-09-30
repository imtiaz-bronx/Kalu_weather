package com.imtiazmahmud.KaluWeather

import java.util.Calendar
import java.util.Locale

// =====================================================================
//  DATA FOR THE EXPLORE PAGE (all free, no API keys)
//  - Earthquakes: USGS (U.S. Geological Survey)
//  - Art of the Day: The Metropolitan Museum of Art, New York
//  - On This Day: Wikipedia
//  Results are kept in memory for a while (a "cache"), so switching
//  tabs doesn't download everything again.
// =====================================================================
object ExploreRepository {

    private class Entry(val savedAt: Long, val value: Any)
    private val cache = mutableMapOf<String, Entry>()

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(name: String, maxAgeMinutes: Int, download: suspend () -> T): T {
        val entry = cache[name]
        if (entry != null && System.currentTimeMillis() - entry.savedAt < maxAgeMinutes * 60_000L) {
            return entry.value as T
        }
        val fresh = download()
        cache[name] = Entry(System.currentTimeMillis(), fresh)
        return fresh
    }

    /** The 5 strongest earthquakes of the last 24 hours (magnitude 2.5+). */
    suspend fun topEarthquakes(): List<Quake> = cached("quakes", maxAgeMinutes = 10) {
        val json = Http.getJson("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.geojson")
        val features = json.getJSONArray("features")
        (0 until features.length()).mapNotNull { i ->
            val p = features.getJSONObject(i).getJSONObject("properties")
            if (p.isNull("mag")) {
                null
            } else {
                Quake(
                    mag = p.getDouble("mag"),
                    place = if (p.isNull("place")) "Unknown location" else p.getString("place"),
                    time = p.optLong("time"),
                    url = if (p.isNull("url")) "" else p.getString("url"),
                )
            }
        }.sortedByDescending { it.mag }.take(10)
    }

    /** One highlighted painting from The Met, a different one each day. */
    suspend fun artOfTheDay(): Artwork = cached("art", maxAgeMinutes = 12 * 60) {
        val search = Http.getJson(
            "https://collectionapi.metmuseum.org/public/collection/v1/search" +
                "?isHighlight=true&hasImages=true&q=painting"
        )
        val ids = search.optJSONArray("objectIDs")
        if (ids == null || ids.length() == 0) error("No artworks found")

        // Same number all day, different number tomorrow.
        val cal = Calendar.getInstance()
        val seed = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.DAY_OF_YEAR)

        // Some objects have no picture, so try a few in a row.
        var lastError: Exception? = null
        for (attempt in 0 until 8) {
            val id = ids.getInt((seed + attempt) % ids.length())
            try {
                val o = Http.getJson("https://collectionapi.metmuseum.org/public/collection/v1/objects/$id")
                val imageUrl = o.optString("primaryImageSmall", "")
                if (imageUrl.isBlank()) continue
                return@cached Artwork(
                    title = o.optString("title", "Untitled"),
                    artist = o.optString("artistDisplayName", ""),
                    date = o.optString("objectDate", ""),
                    url = o.optString("objectURL", ""),
                    image = Http.getImage(imageUrl),
                )
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No artwork with a picture today")
    }

    /** A few important events that happened on today's date in history. */
    suspend fun onThisDay(): List<HistoryEvent> = cached("history", maxAgeMinutes = 12 * 60) {
        val cal = Calendar.getInstance()
        val mm = "%02d".format(Locale.US, cal.get(Calendar.MONTH) + 1)
        val dd = "%02d".format(Locale.US, cal.get(Calendar.DAY_OF_MONTH))
        val json = Http.getJson("https://en.wikipedia.org/api/rest_v1/feed/onthisday/selected/$mm/$dd")
        val events = json.optJSONArray("selected") ?: error("Nothing found for today")
        (0 until events.length()).map { i ->
            val e = events.getJSONObject(i)
            val link = e.optJSONArray("pages")
                ?.optJSONObject(0)
                ?.optJSONObject("content_urls")
                ?.optJSONObject("mobile")
                ?.optString("page", "") ?: ""
            HistoryEvent(year = e.optInt("year"), text = e.optString("text", ""), url = link)
        }.sortedByDescending { it.year }.take(4)
    }
}
