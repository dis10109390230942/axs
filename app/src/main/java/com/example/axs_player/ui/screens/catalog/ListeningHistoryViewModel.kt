package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.stats.ListeningStats
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.screens.toTrackItemData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val HISTORY_LIMIT = 100

class ListeningHistoryViewModel(
    stats: ListeningStats,
    images: ImageStore,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {

    private val tracks: StateFlow<List<TrackListItem>> = stats.recentTracks(HISTORY_LIMIT)
        .map { entries -> entries.map { it.track } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val items: StateFlow<List<TrackItemData>> = combine(tracks, playback.currentTrackId) { tracks, trackId ->
        tracks.map { it.toTrackItemData(images, trackId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(trackId: String) {
        libraryPlayback.play(tracks.value, trackId)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ListeningHistoryViewModel(
                    application.listeningStats,
                    application.imageStore,
                    application.playbackController,
                    application.libraryPlayback
                )
            }
        }
    }
}
