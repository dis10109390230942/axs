package com.example.axs_player.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextWhite

private const val CONTRAST_THRESHOLD = 4.5f

fun Color.contrastColor(): Color {
    val argb = toArgb()
    if ((argb shr 24) and 0xff == 0) return TextBlack
    return if (relativeLuminance(argb) < CONTRAST_THRESHOLD) TextWhite else TextBlack
}

private fun relativeLuminance(argb: Int): Float {
    val r = (AndroidColor.red(argb) / 255f).toDouble()
    val g = (AndroidColor.green(argb) / 255f).toDouble()
    val b = (AndroidColor.blue(argb) / 255f).toDouble()
    val sR = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
    val sG = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
    val sB = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
    return (0.2126 * sR + 0.7152 * sG + 0.0722 * sB).toFloat()
}
