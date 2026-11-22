package com.example.axs_player.data.db

import androidx.room.Embedded

data class AlbumTrackListItem(
    @Embedded val track: TrackListItem,
    val discNumber: Int,
    val position: Int
)
