package com.example.axs_player.playback

import android.net.Uri
import com.example.axs_player.data.db.AlbumEntity
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.library.AxsContainer
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.library.LibraryFolder
import com.example.axs_player.ui.components.trackDisplayTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LibraryPlayback(
    private val folder: LibraryFolder,
    private val images: ImageStore,
    private val dao: LibraryDao,
    private val playback: PlaybackController
) {

    fun play(tracks: List<TrackListItem>, trackId: String) = play(tracks, tracks.indexOfFirst { it.id == trackId })

    fun play(tracks: List<TrackListItem>, startIndex: Int) {
        playback.playbackScope.launch {
            val start = tracks.getOrNull(startIndex)?.let { playable(it) } ?: return@launch
            val before = tracks.take(startIndex).mapNotNull { playable(it) }
            val after = tracks.drop(startIndex + 1).mapNotNull { playable(it) }
            playback.setQueue(before + start + after, before.size)
        }
    }

    suspend fun withActualArtwork(saved: SavedQueue): SavedQueue {
        val covers = dao.trackCovers(saved.tracks.map { it.trackId }.distinct()).associate { it.id to it.coverFileName }
        val volumes = saved.tracks.map { it.trackId }.distinct().associateWith { id -> dao.trackPlaybackVolume(id) ?: 1f }
        return saved.copy(tracks = saved.tracks.map { savedTrack ->
            savedTrack.copy(
                artworkUri = images.coverUri(covers[savedTrack.trackId]),
                playbackVolume = volumes[savedTrack.trackId] ?: savedTrack.playbackVolume
            )
        })
    }

    suspend fun playNext(trackId: String): Boolean = enqueue(trackId, playback.state.value.currentIndex + 1)

    suspend fun enqueue(trackId: String): Boolean = enqueue(trackId, playback.state.value.queue.size)

    private suspend fun enqueue(trackId: String, index: Int): Boolean {
        val track = dao.track(trackId)?.let { playable(it) } ?: return false
        playback.insertIntoQueue(index, listOf(track))
        return true
    }

    private suspend fun playable(track: TrackListItem): PlayableTrack? {
        // Check if track is in container-only album
        val albumId = dao.firstAlbumIdForTrack(track.id) ?: return null
        val album = dao.albumEntity(albumId) ?: return null
        
        val playbackVolume = dao.trackPlaybackVolume(track.id) ?: 1f
        
        if (album.isContainerOnly && album.containerFileName != null) {
            // For container tracks, we'll use a placeholder URI and extract on demand
            return PlayableTrack(
                trackId = track.id,
                uri = android.net.Uri.parse("axs-container://${album.id}/track/${track.fileName}"),
                title = track.title,
                displayTitle = trackDisplayTitle(track.title, track.subtitle),
                artists = track.artists.orEmpty(),
                artworkUri = images.coverUri(track.coverFileName),
                durationMs = track.durationMs,
                hasVideo = track.hasVideo,
                playbackVolume = playbackVolume,
                isContainerTrack = true,
                containerAlbumId = album.id,
                containerBlobName = track.fileName
            )
        }
        
        // Normal track from file system
        return folder.trackUris.value[track.fileName]?.let { uri ->
            PlayableTrack(
                trackId = track.id,
                uri = uri,
                title = track.title,
                displayTitle = trackDisplayTitle(track.title, track.subtitle),
                artists = track.artists.orEmpty(),
                artworkUri = images.coverUri(track.coverFileName),
                durationMs = track.durationMs,
                hasVideo = track.hasVideo,
                playbackVolume = playbackVolume
            )
        }
    }
}