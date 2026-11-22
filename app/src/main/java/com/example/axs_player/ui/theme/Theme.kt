package com.example.axs_player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = darkColorScheme(
    primary = ActiveGray,
    secondary = BackgroundGray,
    background = PageBackground,
    surface = BackgroundGray,
    onPrimary = TextBlack,
    onSecondary = TextWhite,
    onBackground = TextBlack,
    onSurface = TextWhite
)

@Composable
fun Axs_playerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content
    )
}
