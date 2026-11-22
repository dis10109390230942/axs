package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TrackActiveBackground

private const val BACKGROUND_TRANSITION_MS = 600
private const val HIGHLIGHT_LIGHTNESS_SHIFT = 0.12f
private const val HIGHLIGHT_MIN_LIGHTNESS = 0.3f

@Immutable
data class CoverColors(val background: Color, val highlight: Color)

@Composable
fun rememberCoverColor(uri: Uri?): Color? {
    val resolver = LocalContext.current.contentResolver
    var color by remember { mutableStateOf(uri?.let(CoverCache::color)) }

    LaunchedEffect(uri) {
        color = uri?.let { CoverCache.color(it) ?: CoverCache.loadColor(resolver, it) }
    }

    return color
}

@Composable
fun rememberCoverColors(uri: Uri?): CoverColors {
    val color = rememberCoverColor(uri)
    val background by animateColorAsState(
        targetValue = color ?: PageBackground,
        animationSpec = tween(BACKGROUND_TRANSITION_MS),
        label = "coverBackground"
    )
    val highlight by animateColorAsState(
        targetValue = color?.let(::highlightColor) ?: TrackActiveBackground,
        animationSpec = tween(BACKGROUND_TRANSITION_MS),
        label = "coverHighlight"
    )
    return CoverColors(background, highlight)
}

private fun highlightColor(background: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(background.toArgb(), hsl)
    hsl[2] = (hsl[2] - HIGHLIGHT_LIGHTNESS_SHIFT).coerceAtLeast(HIGHLIGHT_MIN_LIGHTNESS)
    return Color(ColorUtils.HSLToColor(hsl))
}
