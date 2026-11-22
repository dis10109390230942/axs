package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.navigation.ArtistCategory
import com.example.axs_player.ui.components.AlbumGrid
import com.example.axs_player.ui.components.TrackList
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun ArtistItemsScreen(
    onOpenAlbum: (albumId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistItemsViewModel = viewModel(factory = ArtistItemsViewModel.Factory)
) {
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val screenModifier = modifier
        .fillMaxSize()
        .background(PageBackground)
    val contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
    val header: @Composable () -> Unit = {
        Text(
            text = artist?.name.orEmpty(),
            color = TextBlack,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }

    when (viewModel.category) {
        ArtistCategory.TRACKS -> TrackList(
            tracks = tracks,
            onTrackClick = { viewModel.play(it.id) },
            modifier = screenModifier,
            contentPadding = contentPadding,
            header = header
        )
        ArtistCategory.ALBUMS -> AlbumGrid(
            albums = albums,
            onAlbumClick = { onOpenAlbum(it.id) },
            modifier = screenModifier,
            contentPadding = contentPadding,
            header = header
        )
    }
}
