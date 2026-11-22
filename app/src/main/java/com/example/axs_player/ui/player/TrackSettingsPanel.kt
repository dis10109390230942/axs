package com.example.axs_player.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.playback.PlayableTrack
import kotlin.math.roundToInt
import com.example.axs_player.ui.components.contrastColor

private val SWIPE_THRESHOLD = 48.dp

@Composable
fun TrackSettingsPanel(
    track: PlayableTrack,
    volume: Float,
    backgroundColor: Color,
    onVolumeChange: (Float) -> Unit,
    onClose: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnClose by rememberUpdatedState(onClose)
    BackHandler(onBack = currentOnClose)

    val textColor = backgroundColor.contrastColor()
    val mutedColor = textColor.copy(alpha = 0.65f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(onClick = {}, indication = null, interactionSource = remember { MutableInteractionSource() })
            .pointerInput(Unit) {
                val threshold = SWIPE_THRESHOLD.toPx()
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        when {
                            dragged > threshold -> onOpenInfo()
                            dragged < -threshold -> currentOnClose()
                        }
                    }
                ) { _, dragAmount -> dragged += dragAmount }
            }
            .padding(start = 10.dp, end = 10.dp, top = 100.dp)
    ) {
        Text("track settings", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(
            text = track.displayTitle,
            color = textColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 24.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("track volume", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${(volume * 100).roundToInt()}%", color = textColor, fontSize = 14.sp)
        }
        Slider(
            value = volume.coerceIn(0f, 2f),
            onValueChange = onVolumeChange,
            valueRange = 0f..2f,
            modifier = Modifier.fillMaxWidth()
        )
        Text("0%–200%", color = mutedColor, fontSize = 12.sp)
    }
}
