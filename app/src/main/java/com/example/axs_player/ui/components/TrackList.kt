package com.example.axs_player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrackList(
    tracks: List<TrackItemData>,
    onTrackClick: (TrackItemData) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showMore: Boolean = true,
    onMore: ((TrackItemData) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        header?.let { item { it() } }
        items(tracks, key = { it.id }) { track ->
            TrackItem(track = track, onClick = { onTrackClick(track) }, showMore = showMore, onMore = onMore?.let { { it(track) } })
        }
    }
}
