package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

private const val COVER_FRACTION = 167.727f / 205f
private const val STACK_SIZE = 3
private val COVER_BORDER_WIDTH = 1.dp

data class PlaylistItemData(
    val id: String,
    val name: String,
    val trackCount: Int,
    val covers: List<Uri?>
)

@Composable
fun PlaylistItem(
    playlist: PlaylistItemData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        PlaylistCovers(covers = playlist.covers)
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = playlist.name,
                color = TextBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (playlist.trackCount == 1) "1 track" else "${playlist.trackCount} tracks",
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PlaylistCovers(covers: List<Uri?>, modifier: Modifier = Modifier) {
    Layout(
        content = {
            (STACK_SIZE - 1 downTo 0).forEach { index ->
                CoverImage(
                    uri = covers.getOrNull(index),
                    modifier = Modifier.border(COVER_BORDER_WIDTH, BackgroundGray)
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / COVER_FRACTION)
    ) { measurables, constraints ->
        val coverSize = constraints.maxHeight
        val step = (constraints.maxWidth - coverSize) / (STACK_SIZE - 1)
        val placeables = measurables.map { it.measure(Constraints.fixed(coverSize, coverSize)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { position, placeable -> placeable.place(step * position, 0) }
        }
    }
}
