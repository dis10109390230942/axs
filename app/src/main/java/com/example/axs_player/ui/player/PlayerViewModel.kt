package com.example.axs_player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.C
import androidx.media3.common.Player
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.playback.PlaybackState
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.ui.screens.toAlbumItemData
import com.example.axs_player.ui.screens.toArtistItemData
import com.example.axs_player.ui.screens.toTrackItemData
import com.example.axs_player.ui.components.trackDisplayTitle
import kotlin.math.roundToLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val PROGRESS_INTERVAL_MS = 250L

private data class TrackInfoSources(
    val artists: List<com.example.axs_player.data.db.ArtistEntity>,
    val albums: List<com.example.axs_player.data.db.AlbumListItem>,
    val details: com.example.axs_player.data.db.TrackDetails?,
    val lives: List<com.example.axs_player.data.db.AlbumTrackListItem>,
    val original: com.example.axs_player.data.db.TrackListItem?,
    val texture: com.example.axs_player.data.db.AlbumTextureInfo? = null
)

class PlayerViewModel(
    private val playback: PlaybackController,
    private val dao: LibraryDao,
    private val images: ImageStore,
    private val libraryPlayback: LibraryPlayback,
    private val containerSync: com.example.axs_player.data.library.AxsContainerSync? = null
) : ViewModel() {

    val state: StateFlow<PlaybackState> = playback.state
    val activePlayer: StateFlow<Player> = playback.activePlayer

    @OptIn(ExperimentalCoroutinesApi::class)
    val trackVolume: StateFlow<Float> = playback.currentTrackId
        .flatMapLatest { trackId -> trackId?.let(dao::observeTrackPlaybackVolume) ?: flowOf(null) }
        .map { it ?: 1f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    fun setTrackVolume(volume: Float) {
        val trackId = playback.currentTrackId.value ?: return
        val normalized = volume.coerceIn(0f, 2f)
        playback.setCurrentTrackVolume(normalized)
        viewModelScope.launch {
            dao.updateTrackPlaybackVolume(trackId, normalized)
            containerSync?.saveBackTrackAlbum(trackId)
        }
    }

    fun loadTrackVolume(trackId: String) {
        viewModelScope.launch {
            val volume = dao.trackPlaybackVolume(trackId) ?: 1f
            playback.setCurrentTrackVolume(volume)
        }
    }

    fun playTrack(trackId: String) {
        viewModelScope.launch {
            val track = dao.track(trackId) ?: return@launch
            libraryPlayback.play(listOf(track), trackId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val trackInfo: StateFlow<TrackInfo> = playback.currentTrackId
        .flatMapLatest { trackId -> trackId?.let(::observeTrackInfo) ?: flowOf(TrackInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackInfo())

    @OptIn(ExperimentalCoroutinesApi::class)
    val progress: StateFlow<PlaybackProgress> = playback.state
        .flatMapLatest { state ->
            flow {
                do {
                    emit(progressOf(state))
                    delay(PROGRESS_INTERVAL_MS)
                } while (state.isPlaying)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackProgress())

    fun togglePlayPause() {
        playback.togglePlayPause()
    }

    fun skipToNext() {
        playback.skipToNext()
    }

    fun skipToPrevious() {
        playback.skipToPrevious()
    }

    fun skipToPreviousTrack() {
        playback.skipToPreviousTrack()
    }

    fun toggleShuffle() {
        playback.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playback.cycleRepeatMode()
    }

    fun skipTo(queueItemId: Long) {
        playback.skipTo(queueItemId)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playback.moveQueueItem(fromIndex, toIndex)
    }

    fun removeFromQueue(queueItemId: Long) {
        playback.removeFromQueue(queueItemId)
    }

    fun seekTo(fraction: Float) {
        val durationMs = durationMs(playback.state.value) ?: return
        playback.seekTo((fraction.coerceIn(0f, 1f) * durationMs).roundToLong())
    }

    private fun observeTrackInfo(trackId: String): Flow<TrackInfo> = combine(
        dao.observeTrackArtists(trackId),
        dao.observeTrackAlbums(trackId),
        dao.observeTrackDetails(trackId),
        dao.observeLiveTracks(trackId),
        dao.observeOriginalTrack(trackId)
    ) { artists, albums, details, lives, original ->
        TrackInfoSources(artists, albums, details, lives, original)
    }.combine(dao.observeTrackAlbumTexture(trackId)) { source, texture -> source.copy(texture = texture) }
        .combine(images.artistPhotos) { source, photos ->
        TrackInfo(
            artists = source.artists.map { it.toArtistItemData(photos) },
            albums = source.albums.map { it.toAlbumItemData(images) },
            year = source.details?.year,
            albumPosition = source.details?.albumPosition,
            composer = source.details?.composer,
            description = source.details?.description,
            lyrics = source.details?.lyrics,
            lives = source.lives.map { live ->
                com.example.axs_player.ui.components.AlbumTrackItemData(
                    id = live.track.id,
                    number = live.position,
                    title = trackDisplayTitle(live.track.title, live.track.subtitle),
                    hasVideo = live.track.hasVideo
                )
            },
            originalTrack = source.original?.toTrackItemData(images, null),
            albumTextureUri = images.albumTextureUri(source.texture?.fileName),
            albumTextureScale = source.texture?.scale ?: 1f,
            albumTextureOffsetX = source.texture?.offsetX ?: 0f,
            albumTextureOffsetY = source.texture?.offsetY ?: 0f,
            albumTextureOpacity = source.texture?.opacity ?: 1f
        )
    }

    private fun progressOf(state: PlaybackState): PlaybackProgress {
        val durationMs = durationMs(state) ?: 0L
        val positionMs = playback.positionMs().let { if (durationMs > 0L) it.coerceAtMost(durationMs) else it }
        return PlaybackProgress(positionMs, durationMs)
    }

    private fun durationMs(state: PlaybackState): Long? =
        playback.durationMs().takeIf { it != C.TIME_UNSET && it > 0L }
            ?: state.currentItem?.track?.durationMs?.takeIf { it > 0L }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                PlayerViewModel(
                    application.playbackController,
                    application.database.libraryDao(),
                    application.imageStore,
                    application.libraryPlayback,
                    application.containerSync
                )
            }
        }
    }
}
