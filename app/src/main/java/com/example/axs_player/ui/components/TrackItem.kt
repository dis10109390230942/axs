package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import com.example.axs_player.R
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.theme.TrackActiveBackground

private val NUMBER_WIDTH = 31.dp

enum class TrackItemVariant { LIBRARY, RELEASE }

data class AlbumTrackItemData(
    val id: String,
    val number: Int?,
    val title: String,
    val subtitle: String? = null,
    val isPlaying: Boolean = false,
    val hasVideo: Boolean = false
)

fun trackDisplayTitle(title: String, subtitle: String?): String =
    subtitle?.trim()?.takeIf { it.isNotEmpty() }?.let { "$title ($it)" } ?: title

data class TrackItemData(
    val id: String,
    val title: String,
    val artists: String?,
    val coverUri: Uri?,
    val isPlaying: Boolean = false,
    val hasVideo: Boolean = false
)

@Composable
fun TrackItem(
    track: TrackItemData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMore: Boolean = true,
    onMore: (() -> Unit)? = null,
    highlightColor: Color = TrackActiveBackground
) {
    TrackItemContent(
        modifier = modifier,
        isPlaying = track.isPlaying,
        highlightColor = highlightColor,
        onClick = onClick,
        variant = TrackItemVariant.LIBRARY,
        title = track.title,
        artists = track.artists,
        coverUri = track.coverUri,
        number = null,
        hasVideo = track.hasVideo,
        trailingIcon = if (showMore) R.drawable.ic_more else null,
        onMore = onMore
    )
}

@Composable
fun TrackItem(
    track: AlbumTrackItemData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = R.drawable.ic_more_vertical,
    onMore: (() -> Unit)? = null,
    highlightColor: Color = TrackActiveBackground
) {
    TrackItemContent(
        modifier = modifier,
        isPlaying = track.isPlaying,
        highlightColor = highlightColor,
        onClick = onClick,
        variant = TrackItemVariant.RELEASE,
        title = trackDisplayTitle(track.title, track.subtitle),
        artists = null,
        coverUri = null,
        number = track.number,
        hasVideo = track.hasVideo,
        trailingIcon = trailingIcon,
        onMore = onMore
    )
}

@Composable
private fun TrackItemContent(
    modifier: Modifier,
    isPlaying: Boolean,
    highlightColor: Color,
    onClick: () -> Unit,
    variant: TrackItemVariant,
    title: String,
    artists: String?,
    coverUri: Uri?,
    number: Int?,
    hasVideo: Boolean,
    @DrawableRes trailingIcon: Int?,
    onMore: (() -> Unit)?
) {
    if (variant == TrackItemVariant.RELEASE) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(if (isPlaying) highlightColor else Color.Transparent)
                .clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() })
                .padding(start = 10.dp, top = 12.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = number?.toString().orEmpty(), color = TextBlack, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.width(NUMBER_WIDTH))
                Text(text = "«$title»", color = TextBlack, fontSize = 16.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (hasVideo) Icon(painter = painterResource(R.drawable.ic_clip), contentDescription = null, tint = TextBlack, modifier = Modifier.padding(start = 4.dp).size(20.dp))
            }
            trailingIcon?.let { Icon(painter = painterResource(it), contentDescription = null, tint = TextBlack, modifier = if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier) }
        }
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(if (isPlaying) highlightColor else Color.Transparent)
            .padding(end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            uri = coverUri,
            modifier = Modifier
                .size(60.dp)
                .border(1.dp, BackgroundGray)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "«$title»",
                color = TextBlack,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val visibleArtists = artists?.takeIf { it.isNotBlank() }
            if (visibleArtists != null || hasVideo) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleArtists?.let {
                        Text(
                            text = it,
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (hasVideo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clip),
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        if (trailingIcon != null) {
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = null,
                tint = TextBlack,
                modifier = Modifier
                    .size(if (trailingIcon == R.drawable.ic_more) 24.dp else 24.dp)
                    .then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
            )
        }
    }
}
