package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.db.PlaylistEntity
import com.example.axs_player.data.db.PlaylistTrackListItem
import com.example.axs_player.navigation.PLAYLIST_ID_ARGUMENT
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.AlbumTrackItemData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PlaylistViewModel(
    savedStateHandle: SavedStateHandle,
    dao: LibraryDao,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle[PLAYLIST_ID_ARGUMENT])

    val playlist: StateFlow<PlaylistEntity?> = dao.observePlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val tracks: StateFlow<List<PlaylistTrackListItem>> = dao.observePlaylistTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items: StateFlow<List<AlbumTrackItemData>> = combine(tracks, playback.currentTrackId) { tracks, trackId ->
        tracks.mapIndexed { index, item ->
            AlbumTrackItemData(
                id = item.itemId,
                number = index + 1,
                title = item.track.title,
                subtitle = item.track.subtitle,
                isPlaying = item.track.id == trackId,
                hasVideo = item.track.hasVideo
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(itemId: String) {
        val items = tracks.value
        libraryPlayback.play(items.map { it.track }, items.indexOfFirst { it.itemId == itemId })
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                PlaylistViewModel(
                    createSavedStateHandle(),
                    application.database.libraryDao(),
                    application.playbackController,
                    application.libraryPlayback
                )
            }
        }
    }
}
