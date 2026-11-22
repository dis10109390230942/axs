package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.OldStandardTT
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

internal val RELEASE_META_FONT_SIZE = 16.sp
private val RELEASE_TITLE_WIDTH = 235.dp

@Composable
internal fun ReleaseMeta(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = RELEASE_META_FONT_SIZE,
        fontFamily = OldStandardTT,
        fontWeight = FontWeight.Normal
    )
}

@Composable
internal fun ReleaseTitle(title: String) {
    Text(
        text = "«$title»",
        color = TextBlack,
        fontSize = 20.sp,
        fontFamily = OldStandardTT,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(RELEASE_TITLE_WIDTH)
    )
}

@Composable
internal fun ReleaseSummary(trackCount: Int, totalDurationMs: Long) {
    Text(
        text = releaseSummary(trackCount, totalDurationMs),
        color = TextMuted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 10.dp, top = 20.dp, end = 10.dp)
    )
}

private fun releaseSummary(trackCount: Int, totalDurationMs: Long): String {
    val tracks = if (trackCount == 1) "1 track" else "$trackCount tracks"
    val totalMinutes = (totalDurationMs + 30_000L) / 60_000L
    if (totalMinutes <= 0L) return tracks
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val duration = when {
        hours == 0L -> "$minutes min"
        minutes == 0L -> "$hours h"
        else -> "$hours h $minutes min"
    }
    return "$tracks • $duration"
}
