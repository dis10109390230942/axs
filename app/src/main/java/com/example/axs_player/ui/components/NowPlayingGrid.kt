package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.axs_player.R
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.CoverOverlay
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

private val GRID_GAP = 10.dp
private const val GRID_COLUMNS = 3
private val BADGE_SIZE = 20.dp
private val BADGE_ICON_SIZE = 8.dp
private val PLAYING_BORDER_WIDTH = 2.dp

data class NowPlayingItemData(
    val id: String,
    val coverUri: Uri?,
    val isAlbum: Boolean,
    val isPlaying: Boolean
)

@Composable
fun NowPlayingGrid(
    items: List<NowPlayingItemData>,
    onItemClick: (NowPlayingItemData) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val full = maxWidth
        val cell = (full - GRID_GAP * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val half = (full - GRID_GAP) / 2
        val feature = cell * 2 + GRID_GAP
        Column(verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
            when (items.size) {
                1 -> GridCell(item = items[0], size = full, onClick = onItemClick)
                2 -> GridRow(items = items, size = half, onClick = onItemClick)
                3 -> FeatureRow(
                    feature = items[0],
                    column = items.subList(1, 3),
                    featureSize = feature,
                    cellSize = cell,
                    onClick = onItemClick
                )
                4 -> {
                    GridRow(items = items.subList(0, 2), size = half, onClick = onItemClick)
                    GridRow(items = items.subList(2, 4), size = half, onClick = onItemClick)
                }
                5 -> {
                    GridRow(items = items.subList(1, 3), size = half, onClick = onItemClick)
                    FeatureRow(
                        feature = items[0],
                        column = items.subList(3, 5),
                        featureSize = feature,
                        cellSize = cell,
                        onClick = onItemClick
                    )
                }
                else -> {
                    GridRow(items = items.subList(1, 4), size = cell, onClick = onItemClick)
                    FeatureRow(
                        feature = items[0],
                        column = items.subList(4, 6),
                        featureSize = feature,
                        cellSize = cell,
                        onClick = onItemClick
                    )
                }
            }
        }
    }
}

@Composable
private fun GridRow(items: List<NowPlayingItemData>, size: Dp, onClick: (NowPlayingItemData) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
        items.forEach { item -> GridCell(item = item, size = size, onClick = onClick) }
    }
}

@Composable
private fun FeatureRow(
    feature: NowPlayingItemData,
    column: List<NowPlayingItemData>,
    featureSize: Dp,
    cellSize: Dp,
    onClick: (NowPlayingItemData) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
        GridCell(item = feature, size = featureSize, onClick = onClick)
        Column(verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
            column.forEach { item -> GridCell(item = item, size = cellSize, onClick = onClick) }
        }
    }
}

@Composable
private fun GridCell(item: NowPlayingItemData, size: Dp, onClick: (NowPlayingItemData) -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(
                onClick = { onClick(item) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        CoverImage(uri = item.coverUri, modifier = Modifier.fillMaxSize())
        if (item.isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CoverOverlay)
                    .border(PLAYING_BORDER_WIDTH, BackgroundGray)
            )
            PlayingBadge(modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun PlayingBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BADGE_SIZE)
            .background(PageBackground)
            .border(PLAYING_BORDER_WIDTH, BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play_badge),
            contentDescription = null,
            tint = TextBlack,
            modifier = Modifier.size(BADGE_ICON_SIZE)
        )
    }
}
