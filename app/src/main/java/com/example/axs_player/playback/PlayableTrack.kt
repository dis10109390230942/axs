package com.example.axs_player.playback

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

private const val EXTRA_TRACK_ID = "track_id"
private const val EXTRA_DURATION_MS = "duration_ms"
private const val EXTRA_HAS_VIDEO = "has_video"
private const val EXTRA_PLAYBACK_VOLUME = "playback_volume"

data class PlayableTrack(
    val trackId: String,
    val uri: Uri,
    val title: String,
    val displayTitle: String = title,
    val artists: String?,
    val artworkUri: Uri?,
    var durationMs: Long?,
    val hasVideo: Boolean,
    val playbackVolume: Float = 1f,
    val isContainerTrack: Boolean = false,
    val containerAlbumId: String? = null,
    val containerBlobName: String? = null,
    val containerUri: Uri? = null,
    val containerBlobOffset: Long = -1,
    val containerBlobLength: Long = -1,
    var resolvedUri: Uri? = null
) {
    val effectiveUri: Uri
        get() = resolvedUri ?: uri
}

fun PlayableTrack.toMediaItem(mediaId: String): MediaItem = toMediaItem(mediaId, durationMs)

fun PlayableTrack.toMediaItem(mediaId: String, effectiveDurationMs: Long?): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setUri(effectiveUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(displayTitle.takeIf { it != title })
            .setArtist(artists)
            .setArtworkUri(artworkUri)
            .setExtras(
                bundleOf(EXTRA_TRACK_ID to trackId, EXTRA_HAS_VIDEO to hasVideo).apply {
                    effectiveDurationMs?.let { putLong(EXTRA_DURATION_MS, it) }
                    putFloat(EXTRA_PLAYBACK_VOLUME, playbackVolume)
                }
            )
            .build()
    )
    .build()

fun MediaItem.toPlayableTrack(): PlayableTrack? {
    val uri = localConfiguration?.uri ?: return null
    val extras = mediaMetadata.extras
    return PlayableTrack(
        trackId = extras?.getString(EXTRA_TRACK_ID) ?: mediaId,
        uri = uri,
        title = mediaMetadata.title?.toString().orEmpty(),
        displayTitle = mediaMetadata.subtitle?.toString() ?: mediaMetadata.title?.toString().orEmpty(),
        artists = mediaMetadata.artist?.toString(),
        artworkUri = mediaMetadata.artworkUri,
        durationMs = extras?.takeIf { it.containsKey(EXTRA_DURATION_MS) }?.getLong(EXTRA_DURATION_MS),
        hasVideo = extras?.getBoolean(EXTRA_HAS_VIDEO) ?: false,
        playbackVolume = extras?.getFloat(EXTRA_PLAYBACK_VOLUME, 1f) ?: 1f
    )
}
