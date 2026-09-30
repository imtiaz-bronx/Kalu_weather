package com.imtiazmahmud.KaluWeather

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =====================================================================
//  LOOK & FEEL: colors, fonts, and small reusable building blocks.
//  Change a color here and it changes on every page.
// =====================================================================
val PageBg = Color.White
val Ink = Color.Black
val Muted = Color(0xFF6B6B6B)
val Faint = Color(0xFF9A9A9A)
val Navy = Color(0xFF14163F)
val Line = Color(0xFFD6D6D6)
val SoftBlue = Color(0xFFEAF2FB)
val CardBg = Color(0xFFF8EEEE)       // the soft pink of your website's boxes
val CardBorder = Color(0xFFA0A0A0)
val LinkPurple = Color(0xFF6A1B9A)
val Gold = Color(0xFFF2B01E)
val Serif = FontFamily.Serif

const val ICON_VERSION = "3.0.0-next.10"     // Meteocons version on their CDN

fun iconUrl(name: String, style: String) =
    "https://cdn.meteocons.com/$ICON_VERSION/lottie/$style/$name.json"

/** An animated Meteocons icon that loops forever. */
@Composable
fun MeteoconIcon(
    name: String,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    style: String = AppSettings.iconStyle,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Url(iconUrl(name, style)))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier.size(size),
    )
}

/**
 * ANIMATION: fade in while sliding up a little.
 * "order" adds a small delay, so items appear one after another.
 */
@Composable
fun Modifier.fadeInUp(order: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order * 80L)
        progress.animateTo(1f, tween(durationMillis = 500, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 40f
    }
}

/**
 * Downloads something once when it first appears on screen, and tells
 * us whether it's still Loading, Done, or Failed.
 */
@Composable
fun <T> rememberLoad(download: suspend () -> T): Load<T> {
    var state by remember { mutableStateOf<Load<T>>(Load.Loading) }
    LaunchedEffect(Unit) {
        state = try {
            Load.Done(download())
        } catch (e: CancellationException) {
            throw e   // the page was closed; stop quietly
        } catch (e: Exception) {
            Load.Failed(e.message ?: "Something went wrong")
        }
    }
    return state
}

@Composable
fun LineDivider() = HorizontalDivider(color = Line, thickness = 1.dp)

@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Ink, modifier = modifier)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(
        text, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink,
        modifier = modifier.padding(top = 24.dp, bottom = 10.dp),
    )

/** An underlined purple link, like on a website. */
@Composable
fun LinkText(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Text(
        text, fontFamily = Serif, fontSize = 17.sp, color = LinkPurple,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable(onClick = onClick),
    )

@Composable
fun LoadingBar() =
    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 12.dp), color = Ink)

/** A pink box with a grey border, like the boxes on your website. */
@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(CardBg)
            .border(1.dp, CardBorder)
            .padding(18.dp),
        content = content,
    )
}

/** A tappable card. ANIMATION: its border and background fade when selected. */
@Composable
fun ChoiceCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(if (selected) Ink else Line, label = "border")
    val background by animateColorAsState(if (selected) SoftBlue else PageBg, label = "bg")
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(background)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.invoke()
        Text(label, fontFamily = Serif, fontSize = 14.sp, color = Ink, textAlign = TextAlign.Center)
    }
}

/** ☆ / ★ button. ANIMATION: bounces when tapped. */
@Composable
fun FavoriteStar(place: Place, size: TextUnit = 28.sp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val isFav = AppSettings.isFavorite(place)

    Text(
        if (isFav) "★" else "☆",
        fontSize = size,
        color = if (isFav) Gold else Faint,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .clickable {
                AppSettings.toggleFavorite(context, place)
                scope.launch {
                    scale.snapTo(0.5f)
                    scale.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                    )
                }
            }
            .padding(6.dp),
    )
}
