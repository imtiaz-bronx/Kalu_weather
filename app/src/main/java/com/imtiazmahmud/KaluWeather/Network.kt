package com.imtiazmahmud.KaluWeather

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ONE place for all internet requests, so every other file stays short.
 * Some websites (Wikipedia, The Met) want to know which app is asking,
 * so we send a "User-Agent" name with every request.
 */
object Http {
    private const val USER_AGENT = "KaluWeather/1.0 (Android learning project)"

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
        }

    /** Downloads a web address and returns the text. */
    suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        val conn = open(url)
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads JSON and turns it into a JSONObject we can read. */
    suspend fun getJson(url: String): JSONObject = JSONObject(getText(url))

    /** Downloads a picture and turns it into something Compose can draw. */
    suspend fun getImage(url: String): ImageBitmap = withContext(Dispatchers.IO) {
        val conn = open(url)
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val bitmap = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                ?: error("Couldn't read the image")
            bitmap.asImageBitmap()
        } finally {
            conn.disconnect()
        }
    }
}
