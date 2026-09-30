package com.imtiazmahmud.KaluWeather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// =====================================================================
//  PAGE 1: TODAY
//  Top: your website's layout. Below: detailed weather in tiles.
// =====================================================================
val RainBlue = Color(0xFF2F80ED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayPage(state: UiState, refreshing: Boolean, onRefresh: () -> Unit, onRetry: () -> Unit) {
    // ANIMATION: Crossfade smoothly swaps between loading, error, and the weather.
    Crossfade(targetState = state, label = "today") { s ->
        when (s) {
            UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Ink)
            }
            is UiState.Error -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.message, color = Ink, fontFamily = Serif, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onRetry) { Text("Try again", color = Ink) }
            }
            // Pull down on the page to refresh.
            is UiState.Ready -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                WeatherPage(s.weather)
            }
        }
    }
}

@Composable
fun WeatherPage(w: Weather) {
    val moon = moonInfo()
    val browser = LocalUriHandler.current
    val today = w.daily.firstOrNull()

    // ANIMATION: the big temperature counts up from 0 to the real value.
    val target = mainTemp(w.temperatureC)
    val shownTemp = remember { Animatable(0f) }
    LaunchedEffect(target) {
        shownTemp.animateTo(target.toFloat(), tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // ---------- Title + ☆ favorite ----------
        Row(Modifier.fillMaxWidth().fadeInUp(0), verticalAlignment = Alignment.Top) {
            Text(
                "${placeLabel(w.place)} Weather (${mainUnit()})",
                fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink,
                modifier = Modifier.weight(1f).padding(top = 6.dp),
            )
            FavoriteStar(w.place)
        }
        Text(
            "Updated ${formatIso(w.updated, "MMM d, hh:mm a")}",
            fontFamily = Serif, fontSize = 14.sp, color = Faint, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().fadeInUp(0),
        )

        // ---------- Big icon + temperature ----------
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).fadeInUp(1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MeteoconIcon(meteoconFor(w.code, w.isDay), size = 130.dp)
            Spacer(Modifier.weight(1f))
            Text(
                "${shownTemp.value.roundToInt()}${mainUnit()}",
                fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 58.sp, color = Ink,
            )
            Text(
                "  (${otherTemp(w.temperatureC)}${otherUnit()})",
                fontFamily = Serif, fontSize = 18.sp, color = Muted,
                modifier = Modifier.padding(top = 18.dp),
            )
        }

        Text(
            describe(w.code),
            fontFamily = FontFamily.Monospace, fontStyle = FontStyle.Italic, fontSize = 22.sp, color = Ink,
            modifier = Modifier.padding(top = 4.dp).fadeInUp(2),
        )
        Text(
            buildAnnotatedString {
                append("Feels like ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink)) {
                    append("${mainTemp(w.feelsLikeC)}${mainUnit()}")
                }
                append("  (${otherTemp(w.feelsLikeC)}${otherUnit()})")
            },
            fontFamily = Serif, fontSize = 19.sp, color = Muted,
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp).fadeInUp(2),
        )

        // ---------- The rows from your website ----------
        Column(Modifier.fadeInUp(3)) {
            LineDivider()
            DetailRow(
                "humidity", "Humidity: ${w.humidity}%",
                "windsock", "Wind: ${w.windMph.roundToInt()} mph ${compass(w.windDirection)}"
            )
            LineDivider()
            DetailRow(
                "sunrise", "Sunrise: ${formatIso(w.sunrise, "h:mm a")}",
                "sunset", "Sunset: ${formatIso(w.sunset, "h:mm a")}"
            )
            LineDivider()
            DetailRow(
                moon.icon, moon.name,
                null, "${moon.illuminationPercent}% illuminated",
                rightColor = Navy, rightSize = 17.sp
            )
            val air = w.air
            if (air != null && (air.usAqi != null || air.dust != null)) {
                LineDivider()
                DetailRow(
                    "smoke-particles", air.usAqi?.let { "Air Quality: $it (${aqiLabel(it)})" } ?: "Air Quality: –",
                    "dust-day", air.dust?.let { "Dust: ${it.roundToInt()} µg/m³ (${dustLabel(it)})" } ?: "Dust: –"
                )
            }
            LineDivider()
            DetailRow(
                "barometer", "Pressure: ${w.pressureHpa.roundToInt()} hPa",
                "uv-index", "UV Index: ${"%.1f".format(w.uvIndex)} (${uvLabel(w.uvIndex)})"
            )
            LineDivider()
        }

        // ---------- Next 24 hours (now with rain chance) ----------
        Text(
            "Next 24 hours",
            fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).fadeInUp(4),
        )
        LazyRow(Modifier.fillMaxWidth().fadeInUp(4)) {
            itemsIndexed(w.hourly) { index, hour ->
                Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (index == 0) "Now" else formatIso(hour.time, "h a"),
                        fontFamily = Serif, fontSize = 14.sp, color = Muted,
                    )
                    MeteoconIcon(meteoconFor(hour.code, hour.isDay), size = 44.dp)
                    Text("${mainTemp(hour.temp)}°", fontFamily = Serif, fontSize = 16.sp, color = Ink)
                    RainChance(hour.precipChance)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LineDivider()

        // ---------- Next 6 days: tap a day to open its details ----------
        var selectedDay by remember { mutableStateOf<Int?>(null) }
        var lastShownDay by remember { mutableIntStateOf(1) }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp).fadeInUp(5)) {
            w.daily.drop(1).take(6).forEachIndexed { j, day ->
                val index = j + 1
                val selected = selectedDay == index
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) SoftBlue else Color.Transparent)
                        .clickable {
                            selectedDay = if (selected) null else index
                            if (!selected) lastShownDay = index
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(dayName(day.date), fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                    MeteoconIcon(meteoconFor(day.code, isDay = true), size = 56.dp)
                    Text("${mainTemp(day.max)}°", fontFamily = Serif, fontSize = 17.sp, color = Ink)
                    Text("${mainTemp(day.min)}°", fontFamily = Serif, fontSize = 17.sp, color = Faint)
                    RainChance(day.precipChance)
                }
            }
        }
        Text(
            "Tap a day for details",
            fontFamily = Serif, fontSize = 12.sp, color = Faint, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        // ANIMATION: the day details slide open and closed.
        AnimatedVisibility(
            visible = selectedDay != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            w.daily.getOrNull(lastShownDay)?.let { DayDetail(it) }
        }

        // ---------- More weather (tiles) ----------
        SectionTitle("More weather now")
        StatGrid(buildList {
            add(Stat(
                "wind-beaufort-${beaufort(w.windMph)}", "Wind",
                speedMain(w.windMph), "${speedOther(w.windMph)} · ${beaufortName(beaufort(w.windMph))}",
            ))
            add(Stat(
                windDirectionIcon(w.windDirection), "Wind direction",
                "From ${compass(w.windDirection)}", "${w.windDirection}°",
            ))
            w.windGustsMph?.let { add(Stat("umbrella-wind", "Wind gusts", speedMain(it), speedOther(it))) }
            today?.precipChance?.let { add(Stat("umbrella", "Rain chance", "$it%", "Highest today")) }
            today?.precipSumMm?.let { add(Stat("raindrops", "Rain today", precipMain(it), precipOther(it))) }
            today?.snowfallCm?.takeIf { it > 0 }?.let { add(Stat("snowflake", "Snow today", snowMain(it), snowOther(it))) }
            w.cloudCover?.let { add(Stat("cloudy", "Cloud cover", "$it%", cloudLabel(it))) }
            w.visibilityM?.let { add(Stat("mist", "Visibility", distanceMain(it), distanceOther(it))) }
            w.dewPointC?.let {
                add(Stat("thermometer-raindrop", "Dew point", "${mainTemp(it)}${mainUnit()}", "${otherTemp(it)}${otherUnit()}"))
            }
            add(Stat(
                pressureIcon(w.pressureHpa), "Pressure", "${w.pressureHpa.roundToInt()} hPa",
                w.surfacePressureHpa?.let { "At ground: ${it.roundToInt()} hPa" } ?: "At sea level",
            ))
            today?.uvMax?.let { add(Stat(uvIcon(it), "UV today", "%.1f".format(it), "${uvLabel(it)} (max)")) }
            today?.daylightSec?.let { day ->
                add(Stat("horizon", "Daylight", duration(day), today?.sunshineSec?.let { "Sunshine ${duration(it)}" } ?: ""))
            }
        })

        // ---------- Air quality ----------
        w.air?.let { air ->
            SectionTitle("Air quality")
            StatGrid(buildList {
                air.usAqi?.let { add(Stat("smoke-particles", "US AQI", "$it", aqiLabel(it))) }
                air.euAqi?.let { add(Stat("smoke-particles", "European AQI", "$it", euAqiLabel(it))) }
                air.pm25?.let { add(Stat("dust", "PM2.5", "${it.roundToInt()} µg/m³", "Fine particles")) }
                air.pm10?.let { add(Stat("dust", "PM10", "${it.roundToInt()} µg/m³", "Coarse particles")) }
                air.ozone?.let { add(Stat("haze", "Ozone (O₃)", "${it.roundToInt()} µg/m³")) }
                air.no2?.let { add(Stat("smoke", "Nitrogen dioxide", "${it.roundToInt()} µg/m³", "NO₂")) }
                air.so2?.let { add(Stat("smoke", "Sulphur dioxide", "${it.roundToInt()} µg/m³", "SO₂")) }
                air.co?.let { add(Stat("smoke", "Carbon monoxide", "${it.roundToInt()} µg/m³", "CO")) }
                air.dust?.let { add(Stat("dust-day", "Dust", "${it.roundToInt()} µg/m³", dustLabel(it))) }
            })

            // Pollen only exists for Europe, so this section often stays hidden.
            if (air.pollen.isNotEmpty()) {
                SectionTitle("Pollen")
                StatGrid(air.pollen.map { (name, grains) ->
                    val icon = when (name) {
                        "Grass" -> "pollen-grass"
                        "Birch", "Alder", "Olive" -> "pollen-tree"
                        else -> "pollen-weed"
                    }
                    Stat(icon, name, pollenLabel(grains), "${grains.roundToInt()} grains/m³")
                })
            }
        }

        // ---------- Credit (required by Open-Meteo's license) ----------
        Text(
            "Weather data by Open-Meteo.com (CC BY 4.0)",
            fontFamily = Serif, fontSize = 12.sp, color = Faint, textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp)
                .clickable { browser.openUri("https://open-meteo.com/") },
        )
    }
}

/** Small blue "30%" under an hour or a day. Hidden when there's no rain. */
@Composable
private fun RainChance(percent: Int?) {
    if (percent != null && percent > 0) {
        Text("$percent%", fontFamily = Serif, fontSize = 12.sp, color = RainBlue)
    } else {
        Text(" ", fontSize = 12.sp)   // keeps all columns the same height
    }
}

/** The box that slides open when you tap a day. */
@Composable
private fun DayDetail(day: DayForecast) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(
            "${longDayName(day.date)} · ${describe(day.code)}",
            fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        StatGrid(buildList {
            add(Stat(
                "thermometer", "High / Low",
                "${mainTemp(day.max)}° / ${mainTemp(day.min)}°",
                "${otherTemp(day.max)}° / ${otherTemp(day.min)}${otherUnit()}",
            ))
            day.precipChance?.let { add(Stat("umbrella", "Rain chance", "$it%")) }
            day.precipSumMm?.let { add(Stat("raindrops", "Rain", precipMain(it), precipOther(it))) }
            day.snowfallCm?.takeIf { it > 0 }?.let { add(Stat("snowflake", "Snow", snowMain(it), snowOther(it))) }
            day.windMaxMph?.let {
                val from = day.windDirDominant?.let { d -> " · from ${compass(d)}" } ?: ""
                add(Stat("wind-beaufort-${beaufort(it)}", "Max wind", speedMain(it), speedOther(it) + from))
            }
            day.gustMaxMph?.let { add(Stat("umbrella-wind", "Max gusts", speedMain(it), speedOther(it))) }
            day.uvMax?.let { add(Stat(uvIcon(it), "UV max", "%.1f".format(it), uvLabel(it))) }
            if (day.sunrise.isNotBlank()) {
                add(Stat("sunrise", "Sunrise", formatIso(day.sunrise, "h:mm a"), "Sunset ${formatIso(day.sunset, "h:mm a")}"))
            }
            day.daylightSec?.let {
                add(Stat("horizon", "Daylight", duration(it), day.sunshineSec?.let { s -> "Sunshine ${duration(s)}" } ?: ""))
            }
        })
    }
}

// =====================================================================
//  TILES: a reusable grid of small boxes, two per row.
// =====================================================================
data class Stat(val icon: String, val label: String, val value: String, val sub: String = "")

@Composable
fun StatGrid(stats: List<Stat>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEach { pair ->
            // IntrinsicSize.Min makes both tiles in a row the same height.
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { StatTile(it, Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatTile(stat: Stat, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .border(1.dp, Line, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MeteoconIcon(stat.icon, size = 34.dp)
            Spacer(Modifier.width(4.dp))
            Text(stat.label, fontFamily = Serif, fontSize = 13.sp, color = Muted)
        }
        Text(stat.value, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        if (stat.sub.isNotBlank()) {
            Text(stat.sub, fontFamily = Serif, fontSize = 13.sp, color = Muted)
        }
    }
}

@Composable
fun DetailRow(
    leftIcon: String, leftText: String,
    rightIcon: String?, rightText: String,
    rightColor: Color = Ink, rightSize: TextUnit = 14.sp,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            MeteoconIcon(leftIcon, size = 34.dp)
            Spacer(Modifier.width(6.dp))
            Text(leftText, fontFamily = Serif, fontSize = 14.sp, color = Ink)
        }
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (rightIcon != null) {
                MeteoconIcon(rightIcon, size = 34.dp)
                Spacer(Modifier.width(6.dp))
            }
            Text(rightText, fontFamily = Serif, fontSize = rightSize, color = rightColor, textAlign = TextAlign.End)
        }
    }
}

// ---- Preview: click "Split" at the top right of the editor ----
private val previewWeather = Weather(
    place = Place("The Bronx", "New York", 40.84, -73.86),
    updated = "2026-09-29T19:45",
    temperatureC = 17.0, feelsLikeC = 17.0, humidity = 83,
    windMph = 5.0, windDirection = 315, code = 3, isDay = false,
    pressureHpa = 1016.0, uvIndex = 0.0,
    sunrise = "2026-09-29T06:50", sunset = "2026-09-29T18:40",
    hourly = (20..23).map { HourForecast("2026-09-29T$it:00", 3, false, 17.0, precipChance = 20) },
    daily = listOf("2026-09-29", "2026-09-30", "2026-10-01", "2026-10-02",
        "2026-10-03", "2026-10-04", "2026-10-05")
        .map { DayForecast(it, code = 2, max = 22.0, min = 15.0, precipChance = 10, uvMax = 4.2) },
    windGustsMph = 12.0, cloudCover = 75, precipitationMm = 0.0,
    visibilityM = 24_000.0, dewPointC = 12.0, surfacePressureHpa = 1010.0,
    air = AirQuality(usAqi = 64, euAqi = 30, pm25 = 12.0, pm10 = 18.0, ozone = 40.0, dust = 0.0),
)

@Preview(showBackground = true, widthDp = 380, heightDp = 1600, name = "Today")
@Composable
private fun TodayPreview() {
    MaterialTheme { WeatherPage(previewWeather) }
}
