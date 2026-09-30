package com.imtiazmahmud.KaluWeather

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch

// =====================================================================
//  MAIN ACTIVITY + APP SHELL
//  This file only starts the app and switches between the 4 pages.
//  Each page lives in its own file: TodayPage.kt, PlacesPage.kt,
//  ExplorePage.kt, SettingsPage.kt.
// =====================================================================

const val CITY = "The Bronx"   // used only if we can't get the phone's location

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
        )
        setContent {
            MaterialTheme {
                KaluApp()
            }
        }
    }
}

private data class NavTab(val label: String, val icon: String)

private val tabs = listOf(
    NavTab("Today", "clear-day"),
    NavTab("Places", "compass"),
    NavTab("Explore", "falling-stars"),
    NavTab("Settings", "thermometer"),
)

@Composable
fun KaluApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var chosenPlace by remember { mutableStateOf<Place?>(null) }   // null = use my location
    var state by remember { mutableStateOf<UiState>(UiState.Loading) }
    var refreshing by remember { mutableStateOf(false) }
    var askedPermission by remember { mutableStateOf(false) }

    suspend fun load(showSpinner: Boolean = true) {
        if (showSpinner) state = UiState.Loading else refreshing = true
        state = try {
            val place = chosenPlace
                ?: LocationHelper.currentPlace(context)
                ?: WeatherRepository.findCity(CITY)
            UiState.Ready(WeatherRepository.forecast(place))
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Something went wrong")
        }
        refreshing = false
    }

    val askPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> scope.launch { load() } }

    // Runs when the app opens, and again every time chosenPlace changes.
    LaunchedEffect(chosenPlace) {
        if (chosenPlace == null && !LocationHelper.hasPermission(context) && !askedPermission) {
            askedPermission = true
            askPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            load()
        }
    }

    Scaffold(
        containerColor = PageBg,
        bottomBar = {
            Column {
                LineDivider()
                NavigationBar(containerColor = PageBg, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { index, t ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { NavIcon(t.icon, playing = tab == index) },
                            label = { Text(t.label, fontFamily = Serif) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SoftBlue,
                                selectedTextColor = Ink,
                                unselectedTextColor = Faint,
                            ),
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // ANIMATION: slide + fade between pages.
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { width -> width / 4 * direction } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> -width / 4 * direction } + fadeOut())
            },
            label = "pages",
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> TodayPage(
                    state = state,
                    refreshing = refreshing,
                    onRefresh = { scope.launch { load(showSpinner = false) } },
                    onRetry = { scope.launch { load() } },
                )
                1 -> PlacesPage(onPick = { place ->
                    if (place == null && chosenPlace == null) scope.launch { load() }
                    chosenPlace = place
                    tab = 0
                })
                2 -> ExplorePage()
                else -> SettingsPage()
            }
        }
    }
}

/** Tab icon: only animates while its tab is selected. */
@Composable
private fun NavIcon(name: String, playing: Boolean) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url(iconUrl(name, AppSettings.iconStyle))
    )
    LottieAnimation(
        composition = composition,
        isPlaying = playing,
        iterations = LottieConstants.IterateForever,
        modifier = Modifier.size(34.dp),
    )
}
