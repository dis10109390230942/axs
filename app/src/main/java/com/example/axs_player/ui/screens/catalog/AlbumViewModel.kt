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
import com.example.axs_player.data.db.AlbumDetails
import com.example.axs_player.data.db.AlbumTrackListItem
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.navigation.ALBUM_ID_ARGUMENT
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.AlbumTrackItemData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumViewModel(
    savedStateHandle: SavedStateHandle,
    dao: LibraryDao,
    images: ImageStore,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle[ALBUM_ID_ARGUMENT])

    // Optimistic track ID - set immediately on play() click, before PlaybackController confirms
    private val _pendingTrackId = MutableStateFlow<String?>(null)
    val pendingTrackId: StateFlow<String?> = _pendingTrackId

    val album: StateFlow<AlbumDetails?> = dao.observeAlbum(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val coverUri: StateFlow<Uri?> = album
        .map { images.coverUri(it?.coverFileName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val textureUri: StateFlow<Uri?> = album
        .map { images.albumTextureUri(it?.backgroundTextureFileName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val coverUris: StateFlow<List<Uri>> = album
        .map { images.albumPhotos(albumId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val media: StateFlow<List<ImageStore.AlbumMedia>> = album
        .map { images.albumMedia(albumId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tracks: StateFlow<List<AlbumTrackListItem>> = dao.observeAlbumTracks(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items: StateFlow<List<AlbumTrackItemData>> = combine(tracks, playback.currentTrackId, _pendingTrackId) { tracks, currentTrackId, pendingTrackId ->
        val effectiveTrackId = pendingTrackId ?: currentTrackId
        tracks.mapIndexed { index, item ->
            AlbumTrackItemData(
                id = item.track.id,
                number = item.position.takeIf { it > 0 } ?: (index + 1),
                title = item.track.title,
                subtitle = item.track.subtitle,
                isPlaying = item.track.id == effectiveTrackId,
                hasVideo = item.track.hasVideo
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Clear pendingTrackId when PlaybackController confirms the track is playing
        viewModelScope.launch {
            playback.currentTrackId
                .collect { currentTrackId ->
                    if (currentTrackId != null && _pendingTrackId.value == currentTrackId) {
                        _pendingTrackId.value = null
                    }
                }
        }

        // Preload container tracks when album loads (for container-owned albums)
        // Use the source flow from DAO before stateIn to avoid StateFlow operator issues
        viewModelScope.launch {
            dao.observeAlbum(albumId)
                .filterNotNull()
                .filter { it.isContainerOwned && it.containerFileName != null }
                .distinctUntilChangedBy { it.id }
                .collect { album ->
                    playback.preloadAlbumContainerTracks(album.id)
                }
        }
    }

    fun play(trackId: String) {
        // Optimistic UI: immediately mark track as playing
        _pendingTrackId.value = trackId
        libraryPlayback.play(tracks.value.map { it.track }, trackId)
    }

    // Call when PlaybackController confirms the track is actually playing
    fun confirmTrackPlaying(trackId: String) {
        if (_pendingTrackId.value == trackId) {
            _pendingTrackId.value = null
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                AlbumViewModel(
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
