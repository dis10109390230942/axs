package com.example.axs_player.playback

import com.example.axs_player.data.db.PlayEventEntity
import com.example.axs_player.data.db.StatsDao
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val POLL_INTERVAL_MS = 1_000L
private const val MIN_LISTENED_MS = 30_000L
private const val RESTART_TOLERANCE_MS = 2_000L

class PlayHistoryRecorder(
    private val controller: PlaybackController,
    private val dao: StatsDao
) {

    private var queueItemId: Long? = null
    private var listenedMs = 0L
    private var lastPositionMs = 0L
    private var isCounted = false

    suspend fun run() {
        controller.state.map { it.isPlaying }.distinctUntilChanged().collectLatest { isPlaying ->
            while (isPlaying) {
                delay(POLL_INTERVAL_MS)
                tick()
            }
        }
    }

    private suspend fun tick() {
        val item = controller.state.value.currentItem
        if (item == null) {
            queueItemId = null
            return
        }
        val position = controller.positionMs()
        if (item.id != queueItemId) {
            queueItemId = item.id
            listenedMs = 0L
            lastPositionMs = position
            isCounted = false
            return
        }
        val delta = position - lastPositionMs
        lastPositionMs = position
        when {
            delta < 0L -> if (position <= RESTART_TOLERANCE_MS) {
                listenedMs = 0L
                isCounted = false
            }
            delta <= POLL_INTERVAL_MS * 2 -> listenedMs += delta
        }
        if (isCounted || listenedMs < threshold()) return
        isCounted = true
        dao.insertPlayEvent(
            PlayEventEntity(trackId = item.track.trackId, playedAt = System.currentTimeMillis())
        )
    }

    private fun threshold(): Long {
        val duration = controller.durationMs()
        return if (duration > 0L) min(MIN_LISTENED_MS, duration / 2) else MIN_LISTENED_MS
    }
}
