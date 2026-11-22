package com.example.axs_player.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.MiniPlayer

@Composable
fun MiniPlayerBar(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val track = state.currentItem?.track ?: return

    MiniPlayer(
        title = track.title,
        artists = track.artists,
        coverUri = track.artworkUri,
        isPlaying = state.isPlaying,
        progress = progress.fraction,
        onPlayPauseClick = viewModel::togglePlayPause,
        onExpand = onExpand,
        onSwipeToNext = viewModel::skipToNext,
        onSwipeToPrevious = viewModel::skipToPreviousTrack,
        modifier = modifier
    )
}
