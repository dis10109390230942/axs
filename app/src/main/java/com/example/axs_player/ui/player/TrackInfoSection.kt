package com.example.axs_player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.components.ARTIST_PHOTO_RATIO
import com.example.axs_player.ui.components.ArtistItemData
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.components.CoverImage
import com.example.axs_player.ui.components.InfoRow
import com.example.axs_player.ui.components.MarkdownText
import com.example.axs_player.ui.components.coverShadow
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TrackActiveBackground
import com.example.axs_player.ui.theme.TextBlack

private val SECTION_SPACING = 30.dp
private val TITLE_SPACING = 10.dp
private val CARD_SPACING = 10.dp
private const val MIN_COLUMNS = 2
private const val MAX_COLUMNS = 4
private const val ALBUM_CARD_RATIO = 1f

@Composable
fun TrackInfoSection(
    info: TrackInfo,
    onArtistClick: (artistId: String) -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    onTrackClick: (trackId: String) -> Unit,
    onEditTrack: (trackId: String) -> Unit,
    linkColor: Color = TrackActiveBackground,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)
    ) {
        if (info.artists.isNotEmpty()) {
            CardGrid(title = "track artists", count = info.artists.size) { index, cardModifier ->
                val artist = info.artists[index]
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist.id) },
                    modifier = cardModifier
                )
            }
        }
        if (info.albums.isNotEmpty()) {
            CardGrid(title = "track albums", count = info.albums.size) { index, cardModifier ->
                val album = info.albums[index]
                CoverImage(
                    uri = album.coverUri,
                    modifier = cardModifier
                        .aspectRatio(ALBUM_CARD_RATIO)
                        .coverShadow()
                        .clickable(
                            onClick = { onAlbumClick(album.id) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoRow(label = "year", value = info.year?.toString())
            InfoRow(label = "number in the album", value = info.albumPosition?.toString())
            InfoRow(label = "composer", value = info.composer)
            if (!info.description.isNullOrBlank()) {
                DescriptionRow(info.description, linkColor)
            }
        }
        if (info.lives.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CardGridTitle(text = "lives")
                info.lives.forEach { live ->
                    TrackItem(
                        track = live,
                        onClick = { onTrackClick(live.id) },
                        onMore = { onEditTrack(live.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        info.originalTrack?.let { original ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CardGridTitle(text = "original track")
                TrackItem(
                    track = original,
                    onClick = { onTrackClick(original.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DescriptionRow(text: String, linkColor: Color) {
    var expanded by remember(text) { mutableStateOf(false) }
    MarkdownText(
        text = text,
        color = TextBlack,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        linkColor = linkColor,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { expanded = !expanded }, indication = null, interactionSource = remember { MutableInteractionSource() })
    )
}

@Composable
private fun CardGrid(
    title: String,
    count: Int,
    card: @Composable (index: Int, modifier: Modifier) -> Unit
) {
    val columns = count.coerceIn(MIN_COLUMNS, MAX_COLUMNS)

    Column(verticalArrangement = Arrangement.spacedBy(TITLE_SPACING)) {
        CardGridTitle(text = title)
        Column(verticalArrangement = Arrangement.spacedBy(CARD_SPACING)) {
            (0 until count).chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CARD_SPACING)
                ) {
                    row.forEach { index -> card(index, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(artist: ArtistItemData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(ARTIST_PHOTO_RATIO)
            .coverShadow()
            .background(BackgroundGray)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (artist.photoUri != null) {
            CoverImage(uri = artist.photoUri, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = artist.name,
                color = TextBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun CardGridTitle(text: String) {
    Text(text = text, color = TextBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}
