package com.example.axs_player.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.db.ReleaseKind
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.stats.ListeningStats
import com.example.axs_player.data.stats.topUp
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.ArtistItemData
import com.example.axs_player.ui.components.NowPlayingItemData
import com.example.axs_player.ui.components.PlaylistItemData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val RELEASE_LIMIT = 6
private const val SECTION_LIMIT = 3
private const val MIN_POINTS_PER_SLOT = 2
private const val RELEASE_MIN_POINTS = RELEASE_LIMIT * MIN_POINTS_PER_SLOT
private const val SECTION_MIN_POINTS = SECTION_LIMIT * MIN_POINTS_PER_SLOT

private class Release(val kind: ReleaseKind, val id: String, val coverFileName: String?)

class HomeViewModel(
    private val dao: LibraryDao,
    images: ImageStore,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback,
    stats: ListeningStats
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val playingReleaseIds: Flow<Set<String>> = playback.currentTrackId
        .flatMapLatest { trackId ->
            if (trackId == null) {
                flowOf(emptySet())
            } else {
                dao.observeTrackAlbums(trackId).map { albums -> albums.mapTo(mutableSetOf(trackId)) { it.id } }
            }
        }

    val releases: StateFlow<List<NowPlayingItemData>> = combine(
        stats.topReleases(RELEASE_LIMIT),
        dao.observeReleases(RELEASE_LIMIT),
        playingReleaseIds
    ) { top, catalog, playing ->
        val ranked = top
            .takeIf { it.sumOf { release -> release.plays } >= RELEASE_MIN_POINTS }
            .orEmpty()
            .map { Release(it.kind, it.id, it.coverFileName) }
        val fallback = catalog.map { Release(it.kind, it.id, it.coverFileName) }
        topUp(ranked, fallback, RELEASE_LIMIT) { it.id }.map { release ->
            NowPlayingItemData(
                id = release.id,
                coverUri = images.coverUri(release.coverFileName),
                isAlbum = release.kind == ReleaseKind.ALBUM,
                isPlaying = release.id in playing
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<PlaylistItemData>> = combine(
        stats.topPlaylists(SECTION_LIMIT),
        dao.observePlaylists()
    ) { top, catalog ->
        val ranked = top
            .takeIf { it.sumOf { playlist -> playlist.score } >= SECTION_MIN_POINTS }
            .orEmpty()
            .map { it.playlist }
        topUp(ranked, catalog, SECTION_LIMIT) { it.id }.map { it.toPlaylistItemData(images) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<ArtistItemData>> = combine(
        stats.topArtists(SECTION_LIMIT),
        dao.observeArtistsByTrackCount(SECTION_LIMIT),
        images.artistPhotos
    ) { top, catalog, photos ->
        val ranked = top
            .takeIf { it.sumOf { artist -> artist.plays } >= SECTION_MIN_POINTS }
            .orEmpty()
            .map { it.artist }
        topUp(ranked, catalog, SECTION_LIMIT) { it.id }.map { it.toArtistItemData(photos) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playTrack(trackId: String) {
        viewModelScope.launch {
            val track = dao.track(trackId) ?: return@launch
            libraryPlayback.play(listOf(track), 0)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                HomeViewModel(
                    application.database.libraryDao(),
                    application.imageStore,
                    application.playbackController,
                    application.libraryPlayback,
                    application.listeningStats
                )
            }
        }
    }
}
