package com.imtiazmahmud.KaluWeather

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =====================================================================
//  PAGE 3: EXPLORE  (boxes like on your website)
//  Each card downloads its own data with rememberLoad { ... }.
//  LocalUriHandler opens links in the phone's web browser.
//  Want another card? Copy one of these, change the data, add it below.
// =====================================================================
@Composable
fun ExplorePage() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        PageTitle("Explore", Modifier.fadeInUp(0))
        Spacer(Modifier.height(14.dp))
        EarthquakeCard(Modifier.fadeInUp(1))
        Spacer(Modifier.height(16.dp))
        ArtCard(Modifier.fadeInUp(2))
        Spacer(Modifier.height(16.dp))
        HistoryCard(Modifier.fadeInUp(3))
        Spacer(Modifier.height(24.dp))
    }
}

/** Card title in your website's style: bold words + grey date. */
@Composable
private fun CardTitle(bold: String, grey: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink)) { append(bold) }
            append(" ")
            withStyle(SpanStyle(color = Muted, fontSize = 17.sp)) { append(grey) }
        },
        fontFamily = Serif, fontSize = 20.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

private fun todayLabel() = SimpleDateFormat("MMM d, yy", Locale.US).format(Date())

// ---------------- Earthquakes (USGS) ----------------
@Composable
fun EarthquakeCard(modifier: Modifier = Modifier) {
    val browser = LocalUriHandler.current
    val load = rememberLoad { ExploreRepository.topEarthquakes() }

    InfoCard(modifier) {
        CardTitle("Earthquakes Today Top 10:", todayLabel())
        when (load) {
            Load.Loading -> LoadingBar()
            is Load.Failed -> Text("Couldn't load earthquakes right now.", fontFamily = Serif, color = Muted)
            is Load.Done -> {
                if (load.value.isEmpty()) {
                    Text("No earthquakes above M 2.5 in the last 24 hours.", fontFamily = Serif, color = Muted)
                }
                load.value.forEach { quake ->
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("M ${"%.1f".format(Locale.US, quake.mag)}")
                            }
                            append(" - ${quake.place}")
                        },
                        fontFamily = Serif, fontSize = 17.sp, color = Ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = quake.url.isNotBlank()) { browser.openUri(quake.url) }
                            .padding(vertical = 5.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LinkText("View USGS Map") { browser.openUri("https://earthquake.usgs.gov/earthquakes/map/") }
    }
}

// ---------------- Art of the Day (The Met, NYC) ----------------
@Composable
fun ArtCard(modifier: Modifier = Modifier) {
    val browser = LocalUriHandler.current
    val load = rememberLoad { ExploreRepository.artOfTheDay() }

    InfoCard(modifier) {
        CardTitle("Art of the Day:", "The Met, NYC")
        when (load) {
            Load.Loading -> LoadingBar()
            is Load.Failed -> Text("Couldn't load today's artwork.", fontFamily = Serif, color = Muted)
            is Load.Done -> {
                val art = load.value
                val ratio = art.image.width.toFloat() / art.image.height.coerceAtLeast(1)
                Image(
                    bitmap = art.image,
                    contentDescription = art.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .aspectRatio(ratio),
                )
                Text(art.title, fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = Ink)
                if (art.artist.isNotBlank()) {
                    Text(art.artist, fontFamily = Serif, fontSize = 16.sp, color = Ink)
                }
                if (art.date.isNotBlank()) {
                    Text(art.date, fontFamily = Serif, fontSize = 15.sp, color = Muted)
                }
                if (art.url.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    LinkText("View at The Met") { browser.openUri(art.url) }
                }
            }
        }
    }
}

// ---------------- On This Day (Wikipedia) ----------------
@Composable
fun HistoryCard(modifier: Modifier = Modifier) {
    val browser = LocalUriHandler.current
    val load = rememberLoad { ExploreRepository.onThisDay() }
    val pageName = SimpleDateFormat("MMMM_d", Locale.US).format(Date())   // e.g. "September_29"

    InfoCard(modifier) {
        CardTitle("On This Day:", SimpleDateFormat("MMMM d", Locale.US).format(Date()))
        when (load) {
            Load.Loading -> LoadingBar()
            is Load.Failed -> Text("Couldn't load history right now.", fontFamily = Serif, color = Muted)
            is Load.Done -> load.value.forEach { event ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${event.year}") }
                        append(" - ${event.text}")
                    },
                    fontFamily = Serif, fontSize = 16.sp, color = Ink, lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = event.url.isNotBlank()) { browser.openUri(event.url) }
                        .padding(vertical = 5.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LinkText("More on Wikipedia") {
            browser.openUri("https://en.wikipedia.org/wiki/Wikipedia:Selected_anniversaries/$pageName")
        }
    }
}
