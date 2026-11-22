package com.example.axs_player.ui.screens.catalog

import android.net.Uri
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
import com.example.axs_player.data.stats.ListeningStats
import com.example.axs_player.data.stats.topUp
import com.example.axs_player.navigation.ARTIST_ID_ARGUMENT
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.AlbumItemData
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.screens.toAlbumItemData
import com.example.axs_player.ui.screens.toTrackItemData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TOP_TRACK_LIMIT = 5
private const val ALBUM_LIMIT = 6

class ArtistViewModel(
    savedStateHandle: SavedStateHandle,
    dao: LibraryDao,
    stats: ListeningStats,
    images: ImageStore,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[ARTIST_ID_ARGUMENT])

    private val topTracks: StateFlow<List<TrackListItem>> = combine(
        stats.artistTopTracks(artistId, TOP_TRACK_LIMIT),
        dao.observeArtistTracks(artistId)
    ) { top, all ->
        topUp(top.map { it.track }, all, TOP_TRACK_LIMIT) { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artist: StateFlow<ArtistEntity?> = dao.observeArtist(artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val photos: StateFlow<List<Uri>> = combine(artist, images.artistPhotos) { current, photos ->
        current?.nameKey?.let { photos[it] }.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val media: StateFlow<List<ImageStore.AlbumMedia>> = combine(artist, images.mediaVersion) { current, _ ->
        current?.id?.let(images::artistMedia).orEmpty()
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tracks: StateFlow<List<TrackItemData>> = combine(topTracks, playback.currentTrackId) { tracks, trackId ->
        tracks.map { it.toTrackItemData(images, trackId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<AlbumItemData>> = dao.observeArtistAlbums(artistId)
        .map { albums -> albums.take(ALBUM_LIMIT).map { it.toAlbumItemData(images) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(trackId: String) {
        libraryPlayback.play(topTracks.value, trackId)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ArtistViewModel(
                    createSavedStateHandle(),
                    application.database.libraryDao(),
                    application.listeningStats,
                    application.imageStore,
                    application.playbackController,
                    application.libraryPlayback
                )
            }
        }
    }
}
