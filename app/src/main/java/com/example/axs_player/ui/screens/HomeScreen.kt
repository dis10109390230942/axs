package com.example.axs_player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.CoverImage
import com.example.axs_player.ui.components.NowPlayingGrid
import com.example.axs_player.ui.components.PlaylistCovers
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.theme.PageBackground

private val SECTION_GAP = 10.dp
private const val SECTION_COLUMNS = 3
private val ARTIST_HEIGHT = 180.dp

@Composable
fun HomeScreen(
    onOpenAlbum: (albumId: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit,
    onOpenArtist: (artistId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val releases by viewModel.releases.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP)
    ) {
        if (releases.isNotEmpty()) {
            SectionTitle(text = "now playing")
            NowPlayingGrid(
                items = releases,
                onItemClick = { item ->
                    if (item.isAlbum) onOpenAlbum(item.id) else viewModel.playTrack(item.id)
                }
            )
        }
        if (playlists.isNotEmpty()) {
            SectionTitle(text = "your playlists")
            SectionRow(items = playlists) { playlist, width ->
                PlaylistCovers(
                    covers = playlist.covers,
                    modifier = Modifier
                        .width(width)
                        .clickable(
                            onClick = { onOpenPlaylist(playlist.id) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )
            }
        }
        if (artists.isNotEmpty()) {
            SectionTitle(text = "your favourite artists")
            SectionRow(items = artists) { artist, width ->
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(ARTIST_HEIGHT)
                        .clickable(
                            onClick = { onOpenArtist(artist.id) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CoverImage(uri = artist.photoUri, modifier = Modifier.fillMaxSize())
                    if (artist.photoUri == null) {
                        Text(
                            text = artist.name,
                            textAlign = TextAlign.Center,
                            color = com.example.axs_player.ui.theme.TextBlack,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SectionRow(items: List<T>, content: @Composable (T, Dp) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val width = (maxWidth - SECTION_GAP * (SECTION_COLUMNS - 1)) / SECTION_COLUMNS
        Row(horizontalArrangement = Arrangement.spacedBy(SECTION_GAP)) {
            items.forEach { item -> content(item, width) }
        }
    }
}
