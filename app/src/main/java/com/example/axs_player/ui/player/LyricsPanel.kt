package com.example.axs_player.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.components.contrastColor

private val SWIPE_CLOSE_THRESHOLD = 48.dp

@Composable
fun LyricsPanel(text: String?, backgroundColor: Color, onClose: (PanelDirection) -> Unit, modifier: Modifier = Modifier) {
    val currentOnClose by rememberUpdatedState(onClose)
    BackHandler(onBack = { onClose(PanelDirection.LEFT) })
    val textColor = backgroundColor.contrastColor()
    val mutedColor = textColor.copy(alpha = 0.6f)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(onClick = {}, indication = null, interactionSource = remember { MutableInteractionSource() })
            .pointerInput(Unit) {
                val threshold = SWIPE_CLOSE_THRESHOLD.toPx()
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -threshold) currentOnClose(PanelDirection.LEFT) }
                ) { _, dragAmount -> dragged += dragAmount }
            }
            .padding(start = 10.dp, top = 100.dp, end = 10.dp, bottom = 100.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("track lyric", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(
            text = text?.takeIf { it.isNotBlank() } ?: "–",
            color = if (text.isNullOrBlank()) mutedColor else textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
    }
}
