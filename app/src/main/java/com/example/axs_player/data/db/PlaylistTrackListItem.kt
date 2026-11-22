package com.example.axs_player.data.db

import androidx.room.Embedded

data class PlaylistTrackListItem(
    val itemId: String,
    @Embedded val track: TrackListItem
)
