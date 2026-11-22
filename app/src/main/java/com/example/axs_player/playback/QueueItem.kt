package com.example.axs_player.playback

import androidx.media3.common.MediaItem

data class QueueItem(
    val id: Long,
    val track: PlayableTrack
) {
    val mediaId: String get() = id.toString()

    fun toMediaItem(): MediaItem = track.toMediaItem(mediaId)

    fun toMediaItem(effectiveDurationMs: Long?): MediaItem = track.toMediaItem(mediaId, effectiveDurationMs)
}
