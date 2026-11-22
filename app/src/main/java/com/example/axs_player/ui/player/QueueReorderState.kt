package com.example.axs_player.ui.player

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.axs_player.playback.QueueItem

class QueueReorderState(private val listState: LazyListState) {

    var draggedId by mutableStateOf<Long?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set

    fun start(id: Long) {
        draggedId = id
        dragOffset = 0f
    }

    fun drag(delta: Float) {
        dragOffset += delta
    }

    fun onScrolled(consumed: Float) {
        dragOffset += consumed
    }

    fun stop() {
        draggedId = null
        dragOffset = 0f
    }

    fun reordered(items: List<QueueItem>): List<QueueItem>? {
        val id = draggedId ?: return null
        val visible = listState.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.key == id } ?: return null
        val fromIndex = items.indexOfFirst { it.id == id }
        if (fromIndex != dragged.index) return null
        val center = dragged.offset + dragOffset + dragged.size / 2f
        val target = visible.firstOrNull { it.key != id && center >= it.offset && center < it.offset + it.size }
            ?: return null
        val toIndex = items.indexOfFirst { it.id == target.key }
        if (toIndex < 0) return null
        dragOffset += dragged.offset - target.offset
        return items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    fun autoScrollDelta(edge: Float, maxSpeed: Float): Float {
        val id = draggedId ?: return 0f
        val layoutInfo = listState.layoutInfo
        val dragged = layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return 0f
        val top = dragged.offset + dragOffset
        val bottom = top + dragged.size
        val startDistance = top - layoutInfo.viewportStartOffset
        val endDistance = layoutInfo.viewportEndOffset - bottom
        return when {
            startDistance < edge -> -maxSpeed * ((edge - startDistance) / edge).coerceIn(0f, 1f)
            endDistance < edge -> maxSpeed * ((edge - endDistance) / edge).coerceIn(0f, 1f)
            else -> 0f
        }
    }
}
