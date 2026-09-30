package com.imtiazmahmud.KaluWeather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================
//  PAGE 4: SETTINGS
//  Changing AppSettings instantly updates every page, because those
//  values are "state". save() stores them on the phone.
// =====================================================================
@Composable
fun SettingsPage() {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        PageTitle("Settings", Modifier.fadeInUp(0))

        SectionTitle("Temperature", Modifier.fadeInUp(1))
        Row(Modifier.fadeInUp(1), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceCard("°C  Celsius", selected = !AppSettings.fahrenheit, modifier = Modifier.weight(1f)) {
                AppSettings.fahrenheit = false
                AppSettings.save(context)
            }
            ChoiceCard("°F  Fahrenheit", selected = AppSettings.fahrenheit, modifier = Modifier.weight(1f)) {
                AppSettings.fahrenheit = true
                AppSettings.save(context)
            }
        }

        SectionTitle("Icon style", Modifier.fadeInUp(2))
        Row(Modifier.fadeInUp(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("fill" to "Fill", "flat" to "Flat", "line" to "Line", "monochrome" to "Mono")
                .forEach { (style, label) ->
                    ChoiceCard(
                        label,
                        selected = AppSettings.iconStyle == style,
                        modifier = Modifier.weight(1f),
                        icon = { MeteoconIcon("partly-cloudy-day-rain", size = 52.dp, style = style) },
                    ) {
                        AppSettings.iconStyle = style
                        AppSettings.save(context)
                    }
                }
        }

        SectionTitle("About", Modifier.fadeInUp(3))
        Text(
            "Weather data from Open-Meteo.com\n" +
                "Earthquakes from USGS\n" +
                "Art of the Day from The Metropolitan Museum of Art\n" +
                "On This Day from Wikipedia\n" +
                "Animated icons: Meteocons by Bas Milius (MIT License)\n" +
                "Built with Kotlin and Jetpack Compose by Imtiaz",
            fontFamily = Serif, fontSize = 15.sp, color = Muted, lineHeight = 22.sp,
            modifier = Modifier.fadeInUp(3),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800, name = "Settings")
@Composable
private fun SettingsPreview() {
    MaterialTheme { SettingsPage() }
}
