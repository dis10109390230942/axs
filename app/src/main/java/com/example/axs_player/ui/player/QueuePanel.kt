package com.example.axs_player.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.axs_player.playback.QueueItem
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.components.trackDisplayTitle
import com.example.axs_player.ui.theme.TextBlack

private val SWIPE_CLOSE_THRESHOLD = 120.dp
private val AUTO_SCROLL_EDGE = 60.dp
private val AUTO_SCROLL_MAX_SPEED = 12.dp
private const val REMOVE_SWIPE_FRACTION = 0.3f

@Composable
fun QueuePanel(
    queue: List<QueueItem>,
    currentIndex: Int,
    backgroundColor: Color,
    highlightColor: Color,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (queueItemId: Long) -> Unit,
    onSelect: (queueItemId: Long) -> Unit,
    onClose: (PanelDirection) -> Unit,
    onCollapsePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnClose by rememberUpdatedState(onClose)
    val currentOnCollapsePlayer by rememberUpdatedState(onCollapsePlayer)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnRemove by rememberUpdatedState(onRemove)
    val currentQueue by rememberUpdatedState(queue)
    val currentId = queue.getOrNull(currentIndex)?.id
    var items by remember { mutableStateOf(queue) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    val reorder = remember(listState) { QueueReorderState(listState) }
    val density = LocalDensity.current
    val autoScrollEdge = with(density) { AUTO_SCROLL_EDGE.toPx() }
    val autoScrollMaxSpeed = with(density) { AUTO_SCROLL_MAX_SPEED.toPx() }
    val pullDownToCollapse = rememberPullDownToCollapse(SWIPE_CLOSE_THRESHOLD, onCollapsePlayer)

    fun finishDrag() {
        val id = reorder.draggedId ?: return
        val fromIndex = currentQueue.indexOfFirst { it.id == id }
        val toIndex = items.indexOfFirst { it.id == id }
        reorder.stop()
        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) currentOnMove(fromIndex, toIndex)
    }

    BackHandler(onBack = { onClose(PanelDirection.RIGHT) })
    LaunchedEffect(queue) {
        if (reorder.draggedId == null) items = queue
    }
    LaunchedEffect(reorder.draggedId != null) {
        while (reorder.draggedId != null) {
            withFrameNanos {}
            val delta = reorder.autoScrollDelta(autoScrollEdge, autoScrollMaxSpeed)
            if (delta != 0f) {
                reorder.onScrolled(listState.scrollBy(delta))
                reorder.reordered(items)?.let { items = it }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                onClick = {},
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .pointerInput(Unit) {
                val threshold = SWIPE_CLOSE_THRESHOLD.toPx()
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged > threshold) currentOnClose(PanelDirection.RIGHT) }
                ) { _, dragAmount -> dragged += dragAmount }
            }
            .padding(top = 100.dp)
    ) {
        Text(
            text = "queue",
            color = TextBlack,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(pullDownToCollapse),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isDragged = item.id == reorder.draggedId
                var swipeOffset by remember { mutableFloatStateOf(0f) }
                TrackItem(
                    track = TrackItemData(
                        id = item.mediaId,
                        title = item.track.displayTitle,
                        artists = item.track.artists,
                        coverUri = item.track.artworkUri,
                        isPlaying = item.id == currentId,
                        hasVideo = item.track.hasVideo
                    ),
                    onClick = { onSelect(item.id) },
                    showMore = false,
                    highlightColor = highlightColor,
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) reorder.dragOffset else 0f
                            translationX = swipeOffset
                        }
                        .then(if (isDragged) Modifier.background(backgroundColor) else Modifier)
                        .swipeLeftToRemove(
                            onOffsetChange = { swipeOffset = it },
                            onRemove = {
                                items = items.filterNot { it.id == item.id }
                                currentOnRemove(item.id)
                            }
                        )
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { reorder.start(item.id) },
                                onDragEnd = { finishDrag() },
                                onDragCancel = {
                                    items = currentQueue
                                    reorder.stop()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                reorder.drag(dragAmount.y)
                                reorder.reordered(items)?.let { items = it }
                            }
                        }
                )
            }
        }
    }
}

private fun Modifier.swipeLeftToRemove(onOffsetChange: (Float) -> Unit, onRemove: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var offset = 0f
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                if (overSlop < 0f) {
                    change.consume()
                    offset = overSlop
                    onOffsetChange(offset)
                }
            } ?: return@awaitEachGesture
            val completed = horizontalDrag(drag.id) { change ->
                offset = (offset + change.positionChange().x).coerceAtMost(0f)
                onOffsetChange(offset)
                change.consume()
            }
            if (completed && -offset > size.width * REMOVE_SWIPE_FRACTION) {
                onOffsetChange(-size.width.toFloat())
                onRemove()
            } else {
                onOffsetChange(0f)
            }
        }
    }
