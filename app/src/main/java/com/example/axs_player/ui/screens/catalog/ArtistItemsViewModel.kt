package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.ArtistEntity
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.navigation.ARTIST_CATEGORY_ARGUMENT
import com.example.axs_player.navigation.ARTIST_ID_ARGUMENT
import com.example.axs_player.navigation.ArtistCategory
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.AlbumItemData
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.screens.toAlbumItemData
import com.example.axs_player.ui.screens.toTrackItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ArtistItemsViewModel(
    savedStateHandle: SavedStateHandle,
    dao: LibraryDao,
    images: ImageStore,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[ARTIST_ID_ARGUMENT])

    val category: ArtistCategory =
        ArtistCategory.fromPath(savedStateHandle[ARTIST_CATEGORY_ARGUMENT]) ?: ArtistCategory.TRACKS

    private val artistTracks: StateFlow<List<TrackListItem>> = dao.observeArtistTracks(artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artist: StateFlow<ArtistEntity?> = dao.observeArtist(artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tracks: StateFlow<List<TrackItemData>> = combine(artistTracks, playback.currentTrackId) { tracks, trackId ->
        tracks.map { it.toTrackItemData(images, trackId) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<AlbumItemData>> = dao.observeArtistAlbums(artistId)
        .map { albums -> albums.map { it.toAlbumItemData(images) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(trackId: String) {
        libraryPlayback.play(artistTracks.value, trackId)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ArtistItemsViewModel(
                    createSavedStateHandle(),
                    application.database.libraryDao(),
                    application.imageStore,
                    application.playbackController,
                    application.libraryPlayback
                )
            }
        }
    }
}
