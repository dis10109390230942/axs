package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.screens.toTrackItemData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.example.axs_player.playback.PlaybackController

class LivesViewModel(dao: LibraryDao, images: ImageStore, playback: PlaybackController) : ViewModel() {
    private val tracks: StateFlow<List<TrackListItem>> = dao.observeLiveParents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val items: StateFlow<List<TrackItemData>> = combine(tracks, playback.currentTrackId) { tracks, currentId ->
        tracks.map { it.toTrackItemData(images, currentId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                LivesViewModel(app.database.libraryDao(), app.imageStore, app.playbackController)
            }
        }
    }
}
