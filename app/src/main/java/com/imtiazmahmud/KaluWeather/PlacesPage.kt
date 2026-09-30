package com.imtiazmahmud.KaluWeather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// =====================================================================
//  PAGE 2: PLACES  (search for a city + your favorite cities)
//  onPick is a "callback": this page doesn't change the weather itself,
//  it tells KaluApp which place was picked (null = my location).
// =====================================================================
@Composable
fun PlacesPage(onPick: (Place?) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    // Current temperature for each favorite, filled in as downloads finish.
    val favorites = AppSettings.favorites
    val quick = remember { mutableStateMapOf<String, QuickWeather>() }
    LaunchedEffect(favorites) {
        favorites.filter { it.key !in quick }.forEach { place ->
            launch {
                try {
                    quick[place.key] = WeatherRepository.quickCurrent(place)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // no temperature for this one, that's fine
                }
            }
        }
    }

    fun search() {
        if (query.isBlank()) return
        focus.clearFocus()   // hides the keyboard
        scope.launch {
            searching = true
            message = null
            results = try {
                WeatherRepository.searchCities(query)
            } catch (e: Exception) {
                message = "Couldn't search: ${e.message}"
                emptyList()
            }
            if (results.isEmpty() && message == null) message = "No places found for \"$query\""
            searching = false
        }
    }

    // One scrolling list holds everything: search box, results, favorites.
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            PageTitle("Places", Modifier.fadeInUp(0))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search a city, e.g. Dhaka", fontFamily = Serif) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ink, cursorColor = Ink,
                    focusedTextColor = Ink, unfocusedTextColor = Ink,
                ),
                modifier = Modifier.fillMaxWidth().fadeInUp(1),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPick(null) }
                    .padding(vertical = 10.dp)
                    .fadeInUp(2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MeteoconIcon("compass", size = 36.dp)
                Spacer(Modifier.width(8.dp))
                Text("Use my location", fontFamily = Serif, fontSize = 17.sp, color = Ink)
            }
            LineDivider()
            if (searching) LoadingBar()
            message?.let {
                Text(it, fontFamily = Serif, color = Muted, modifier = Modifier.padding(top = 16.dp))
            }
        }

        // ---- Search results ----
        if (results.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Results", Modifier.weight(1f))
                    Text(
                        "Clear",
                        fontFamily = Serif, color = LinkPurple,
                        modifier = Modifier.padding(top = 14.dp).clickable { results = emptyList() },
                    )
                }
            }
            itemsIndexed(results, key = { i, p -> "result-$i-${p.key}" }) { i, place ->
                PlaceRow(place, onClick = { onPick(place) }, modifier = Modifier.fadeInUp(i)) {
                    FavoriteStar(place, size = 24.sp)
                }
                LineDivider()
            }
        }

        // ---- Favorites ----
        item { SectionTitle("★ Favorites", Modifier.fadeInUp(3)) }
        if (favorites.isEmpty()) {
            item {
                Text(
                    "No favorites yet. Tap ☆ next to a city name to save it here.",
                    fontFamily = Serif, fontSize = 15.sp, color = Muted,
                    modifier = Modifier.fadeInUp(3),
                )
            }
        } else {
            itemsIndexed(favorites, key = { _, p -> "fav-${p.key}" }) { i, place ->
                PlaceRow(place, onClick = { onPick(place) }, modifier = Modifier.fadeInUp(i + 3)) {
                    quick[place.key]?.let { q ->
                        MeteoconIcon(meteoconFor(q.code, q.isDay), size = 38.dp)
                        Text(
                            "${mainTemp(q.tempC)}°",
                            fontFamily = Serif, fontSize = 18.sp, color = Ink,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    FavoriteStar(place, size = 24.sp)
                }
                LineDivider()
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** One city in a list: name, region, and anything extra on the right. */
@Composable
fun PlaceRow(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(place.name, fontFamily = Serif, fontSize = 18.sp, color = Ink)
            val sub = placeSubtitle(place)
            if (sub.isNotBlank()) Text(sub, fontFamily = Serif, fontSize = 14.sp, color = Muted)
        }
        trailing()
    }
}
