package com.example.axs_player.playback

import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val AVAILABLE_COMMANDS = Player.Commands.Builder()
    .addAll(
        Player.COMMAND_PLAY_PAUSE,
        Player.COMMAND_PREPARE,
        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
        Player.COMMAND_SET_MEDIA_ITEM,
        Player.COMMAND_SET_REPEAT_MODE,
        Player.COMMAND_CHANGE_MEDIA_ITEMS,
        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
        Player.COMMAND_GET_TIMELINE,
        Player.COMMAND_GET_METADATA,
        Player.COMMAND_RELEASE
    )
    .build()

@OptIn(UnstableApi::class)
class SessionPlayer(private val controller: PlaybackController) : SimpleBasePlayer(Looper.getMainLooper()) {

    private val scope = MainScope()
    private var playlistQueueContent: List<String> = emptyList()
    private var playlist: List<SimpleBasePlayer.MediaItemData> = emptyList()
    private var lastStateUpdate = 0L
    private var lastContainerTracksVersion: Long = -1

    init {
        scope.launch { controller.state.collect { invalidateState() } }
    }

    override fun getState(): SimpleBasePlayer.State {
        val playback = controller.state.value
        val builder = SimpleBasePlayer.State.Builder()
            .setAvailableCommands(AVAILABLE_COMMANDS)
            .setPlayWhenReady(playback.isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setRepeatMode(playback.repeatMode.playerRepeatMode)
        if (playback.queue.isEmpty()) {
            return builder.setPlaybackState(Player.STATE_IDLE).build()
        }
        return builder
            .setPlaybackState(Player.STATE_READY)
            .setPlaylist(playlistOf(playback.queue))
            .setCurrentMediaItemIndex(playback.currentIndex)
            .setContentPositionMs(SimpleBasePlayer.PositionSupplier { controller.positionMs() })
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) controller.play() else controller.pause()
        return done()
    }

    override fun handlePrepare(): ListenableFuture<*> = done()

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        controller.seekTo(mediaItemIndex, positionMs)
        return done()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> {
        val tracks = mediaItems.mapNotNull { it.toPlayableTrack() }
        controller.setQueue(
            tracks = tracks,
            startIndex = if (startIndex == C.INDEX_UNSET) 0 else startIndex,
            playWhenReady = controller.state.value.isPlaying
        )
        if (startPositionMs != C.TIME_UNSET) controller.seekTo(startPositionMs)
        return done()
    }

    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {
        controller.insertIntoQueue(index, mediaItems.mapNotNull { it.toPlayableTrack() })
        return done()
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        controller.removeQueueItems(fromIndex, toIndex)
        return done()
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        controller.moveQueueItems(fromIndex, toIndex, newIndex)
        return done()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        controller.setRepeatMode(RepeatMode.fromPlayerRepeatMode(repeatMode))
        return done()
    }

    override fun handleRelease(): ListenableFuture<*> {
        scope.cancel()
        return done()
    }

    private fun playlistOf(queue: List<QueueItem>): List<SimpleBasePlayer.MediaItemData> {
        Log.d("SessionPlayer", "playlistOf called: queueSize=${queue.size}")
        val queueContent = queue.map { it.track.trackId }
        val currentVersion = controller.containerTracksVersion()
        if (queueContent != playlistQueueContent || currentVersion != lastContainerTracksVersion) {
            Log.d("SessionPlayer", "Queue or container tracks changed, rebuilding playlist")
            playlistQueueContent = queueContent
            lastContainerTracksVersion = currentVersion
            playlist = queue.map { item ->
                val effectiveDurationMs = if (item.track.isContainerTrack) {
                    // For container tracks, prefer manifest cache over stale DB duration.
                    // If cache is empty, estimateDuration will compute from blob length.
                    val cached = controller.getContainerTrackDuration(item.track.trackId)
                        ?.takeIf { it > 0L }
                    Log.d("SessionPlayer", "Container track ${item.track.trackId}: cachedDuration=${controller.getContainerTrackDuration(item.track.trackId)}, effective=$cached")
                    cached
                } else {
                    item.track.durationMs?.takeIf { it > 0L }
                }
                // For container tracks, seeking is always possible (we have byte boundaries and seekable DataSource)
                // Even if duration is unknown, the extractor with constant bitrate seeking can handle it
                val isSeekable = if (item.track.isContainerTrack) true else effectiveDurationMs?.let { it > 0L } == true
                // For UI, provide estimated duration if true duration unknown
                val displayDurationMs = effectiveDurationMs ?: estimateDuration(item.track)
                Log.d("SessionPlayer", "Track ${item.track.trackId}: displayDuration=$displayDurationMs, isSeekable=$isSeekable")
                SimpleBasePlayer.MediaItemData.Builder(item.id)
                    .setMediaItem(item.toMediaItem(displayDurationMs))
                    .setDurationUs(displayDurationMs?.times(1000) ?: C.TIME_UNSET)
                    .setIsSeekable(isSeekable)
                    .build()
            }
        }
        return playlist
    }

    private fun estimateDuration(track: PlayableTrack): Long? {
        // Estimate duration from blob length and mime type for container tracks
        if (!track.isContainerTrack) return null
        val controller = this.controller
        val streamInfo = controller.getContainerTrackInfo(track.trackId)
        val blobLength = streamInfo?.blobLength?.takeIf { it > 0 } ?: track.containerBlobLength.takeIf { it > 0 }
        val mime = track.containerBlobName?.substringAfterLast('.')?.lowercase()
        return if (blobLength != null) {
            // Rough bitrate estimates by format (bits per second)
            val bitrateBps = when (mime) {
                "mp3" -> 192_000L   // ~192 kbps typical
                "flac" -> 700_000L  // ~700 kbps typical lossless
                "m4a", "aac", "mp4" -> 128_000L  // ~128 kbps typical
                "ogg", "opus" -> 96_000L  // ~96 kbps typical
                "wav" -> 1_411_200L  // 44.1kHz 16-bit stereo
                else -> 128_000L  // default assumption
            }
            // duration (ms) = (bytes * 8) / bitrate_bps * 1000
            (blobLength * 8 * 1000 / bitrateBps).coerceAtLeast(1000)
        } else null
    }

    private fun done(): ListenableFuture<*> = Futures.immediateVoidFuture()
}
