package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.axs_player.R
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.ProgressAccent
import com.example.axs_player.ui.theme.TextBlack

private val SWIPE_UP_THRESHOLD = 24.dp
private val SWIPE_SKIP_THRESHOLD = 48.dp

@Composable
fun MiniPlayer(
    title: String,
    artists: String?,
    coverUri: Uri?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onExpand: () -> Unit,
    onSwipeToNext: () -> Unit,
    onSwipeToPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnExpand by rememberUpdatedState(onExpand)
    val currentOnSwipeToNext by rememberUpdatedState(onSwipeToNext)
    val currentOnSwipeToPrevious by rememberUpdatedState(onSwipeToPrevious)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onExpand,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .pointerInput(Unit) {
                val threshold = SWIPE_UP_THRESHOLD.toPx()
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -threshold) currentOnExpand() }
                ) { _, dragAmount -> dragged += dragAmount }
            }
            .pointerInput(Unit) {
                val threshold = SWIPE_SKIP_THRESHOLD.toPx()
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        when {
                            dragged < -threshold -> currentOnSwipeToNext()
                            dragged > threshold -> currentOnSwipeToPrevious()
                        }
                    }
                ) { _, dragAmount -> dragged += dragAmount }
            }
            .background(BackgroundGray)
            .height(IntrinsicSize.Min)
            .padding(start = 10.dp, top = 10.dp, end = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(uri = coverUri, modifier = Modifier.size(44.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "«$title»",
                    color = TextBlack,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artists.orEmpty(),
                    color = TextBlack.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(ProgressAccent)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    onClick = onPlayPauseClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (isPlaying) "pause" else "play",
                    tint = TextBlack,
                    modifier = Modifier.size(28.dp)
                )
        }
    }
}