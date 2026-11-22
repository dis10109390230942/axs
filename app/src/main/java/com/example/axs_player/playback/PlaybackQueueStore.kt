package com.example.axs_player.playback

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val LOG_TAG = "PlaybackQueueStore"
private const val FILE_NAME = "playback_queue.json"
private const val KEY_TRACKS = "tracks"
private const val KEY_CURRENT_INDEX = "currentIndex"
private const val KEY_POSITION_MS = "positionMs"
private const val KEY_TRACK_ID = "trackId"
private const val KEY_URI = "uri"
private const val KEY_TITLE = "title"
private const val KEY_ARTISTS = "artists"
private const val KEY_ARTWORK_URI = "artworkUri"
private const val KEY_DURATION_MS = "durationMs"
private const val KEY_HAS_VIDEO = "hasVideo"
private const val KEY_PLAYBACK_VOLUME = "playbackVolume"
private const val KEY_SHUFFLE = "shuffle"
private const val KEY_REPEAT_MODE = "repeatMode"
private const val KEY_ORIGINAL_ORDER = "originalOrder"

data class SavedQueue(
    val tracks: List<PlayableTrack>,
    val currentIndex: Int,
    val positionMs: Long,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val originalOrder: List<Int> = emptyList()
)

class PlaybackQueueStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    suspend fun load(): SavedQueue? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching { decode(JSONObject(file.readText())) }
            .onFailure { Log.e(LOG_TAG, "Не удалось прочитать сохранённую очередь", it) }
            .getOrNull()
    }

    suspend fun save(queue: SavedQueue) = withContext(Dispatchers.IO) {
        runCatching {
            val temporary = File(file.parentFile, "$FILE_NAME.tmp")
            temporary.writeText(encode(queue).toString())
            if (!temporary.renameTo(file)) {
                file.delete()
                temporary.renameTo(file)
            }
        }.onFailure { Log.e(LOG_TAG, "Не удалось сохранить очередь", it) }
    }

    private fun encode(queue: SavedQueue): JSONObject = JSONObject()
        .put(KEY_CURRENT_INDEX, queue.currentIndex)
        .put(KEY_POSITION_MS, queue.positionMs)
        .put(KEY_SHUFFLE, queue.isShuffleEnabled)
        .put(KEY_REPEAT_MODE, queue.repeatMode.name)
        .put(KEY_ORIGINAL_ORDER, JSONArray(queue.originalOrder))
        .put(KEY_TRACKS, JSONArray().apply { queue.tracks.forEach { put(encode(it)) } })

    private fun encode(track: PlayableTrack): JSONObject = JSONObject()
        .put(KEY_TRACK_ID, track.trackId)
        .put(KEY_URI, track.uri.toString())
        .put(KEY_TITLE, track.title)
        .put(KEY_ARTISTS, track.artists)
        .put(KEY_ARTWORK_URI, track.artworkUri?.toString())
        .put(KEY_DURATION_MS, track.durationMs)
        .put(KEY_HAS_VIDEO, track.hasVideo)
        .put(KEY_PLAYBACK_VOLUME, track.playbackVolume)

    private fun decode(json: JSONObject): SavedQueue {
        val tracks = json.getJSONArray(KEY_TRACKS)
        return SavedQueue(
            tracks = List(tracks.length()) { decodeTrack(tracks.getJSONObject(it)) },
            currentIndex = json.getInt(KEY_CURRENT_INDEX),
            positionMs = json.getLong(KEY_POSITION_MS),
            isShuffleEnabled = json.optBoolean(KEY_SHUFFLE, false),
            repeatMode = RepeatMode.entries.firstOrNull { it.name == json.optString(KEY_REPEAT_MODE) } ?: RepeatMode.OFF,
            originalOrder = json.optJSONArray(KEY_ORIGINAL_ORDER)
                ?.let { order -> List(order.length()) { order.getInt(it) } }
                .orEmpty()
        )
    }

    private fun decodeTrack(json: JSONObject) = PlayableTrack(
        trackId = json.getString(KEY_TRACK_ID),
        uri = json.getString(KEY_URI).toUri(),
        title = json.getString(KEY_TITLE),
        artists = json.optString(KEY_ARTISTS).ifEmpty { null },
        artworkUri = json.optString(KEY_ARTWORK_URI).ifEmpty { null }?.toUri(),
        durationMs = json.optLong(KEY_DURATION_MS, -1L).takeIf { it >= 0L },
        hasVideo = json.optBoolean(KEY_HAS_VIDEO, false),
        playbackVolume = json.optDouble(KEY_PLAYBACK_VOLUME, 1.0).toFloat()
    )
}
