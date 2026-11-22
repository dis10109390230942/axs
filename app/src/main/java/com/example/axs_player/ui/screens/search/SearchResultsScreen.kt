package com.example.axs_player.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.data.search.SearchCategory
import com.example.axs_player.ui.components.AlbumGrid
import com.example.axs_player.ui.components.ArtistGrid
import com.example.axs_player.ui.components.TrackActionsBottomSheet
import com.example.axs_player.ui.components.TrackList
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun SearchResultsScreen(
    onOpenAlbum: (albumId: String) -> Unit,
    onOpenArtist: (artistId: String) -> Unit,
    onEditTrack: (trackId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchResultsViewModel = viewModel(factory = SearchResultsViewModel.Factory)
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    val screenModifier = modifier
        .fillMaxSize()
        .background(PageBackground)
    val contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
    val header: @Composable () -> Unit = {
        Text(text = viewModel.category.path, color = TextBlack, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }

    when (viewModel.category) {
        SearchCategory.ARTISTS -> ArtistGrid(
            artists = content.artists,
            onArtistClick = { onOpenArtist(it.id) },
            modifier = screenModifier,
            contentPadding = contentPadding,
            header = header
        )
        SearchCategory.TRACKS -> TrackList(
            tracks = content.tracks,
            onTrackClick = { viewModel.play(it.id) },
            modifier = screenModifier,
            contentPadding = contentPadding,
            onMore = { selectedTrackId = it.id },
            header = header
        )
        SearchCategory.ALBUMS -> AlbumGrid(
            albums = content.albums,
            onAlbumClick = { onOpenAlbum(it.id) },
            modifier = screenModifier,
            contentPadding = contentPadding,
            header = header
        )
    }
    if (selectedTrackId != null) {
        TrackActionsBottomSheet(selectedTrackId!!, onEdit = { onEditTrack(selectedTrackId!!); selectedTrackId = null }, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onDismiss = { selectedTrackId = null })
    }
}
