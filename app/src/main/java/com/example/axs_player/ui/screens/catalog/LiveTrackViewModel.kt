package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.AlbumTrackListItem
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.navigation.TRACK_ID_ARGUMENT
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.AlbumTrackItemData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LiveTrackViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: LibraryDao,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {
    private val parentId: String = checkNotNull(savedStateHandle[TRACK_ID_ARGUMENT])
    val parentTitle: StateFlow<String> = dao.observeTracks()
        .map { tracks -> tracks.firstOrNull { it.id == parentId }?.title.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    private val tracks: StateFlow<List<AlbumTrackListItem>> = dao.observeLiveTracks(parentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items: StateFlow<List<AlbumTrackItemData>> = combine(tracks, playback.currentTrackId) { lives, currentId ->
        lives.map { live -> AlbumTrackItemData(live.track.id, live.position, live.track.title, live.track.subtitle, live.track.id == currentId, live.track.hasVideo) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(id: String) = libraryPlayback.play(tracks.value.map { it.track }, id)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                LiveTrackViewModel(createSavedStateHandle(), app.database.libraryDao(), app.playbackController, app.libraryPlayback)
            }
        }
    }
}
