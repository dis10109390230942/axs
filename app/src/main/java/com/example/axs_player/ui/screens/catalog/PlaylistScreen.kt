package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.components.TrackActionsBottomSheet
import com.example.axs_player.ui.theme.PageBackground

private const val RELEASE_TYPE = "playlist"

@Composable
fun PlaylistScreen(
    onEditTrack: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = viewModel(factory = PlaylistViewModel.Factory)
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    var selectedTrackId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
    ) {
        playlist?.let { details ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ReleaseMeta(text = RELEASE_TYPE)
                    Spacer(modifier = Modifier.height(10.dp))
                    ReleaseTitle(title = details.name)
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
        items(items, key = { it.id }) { track ->
            TrackItem(track = track, onClick = { viewModel.play(track.id) }, onMore = { selectedTrackId = track.id })
        }
        if (tracks.isNotEmpty()) {
            item {
                ReleaseSummary(tracks.size, tracks.sumOf { it.track.durationMs ?: 0L })
            }
        }
    }
    if (selectedTrackId != null) {
        TrackActionsBottomSheet(selectedTrackId!!, onEdit = { onEditTrack(selectedTrackId!!); selectedTrackId = null }, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onDismiss = { selectedTrackId = null })
    }
}
