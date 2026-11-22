package com.example.axs_player.data.db

import androidx.room.Embedded

data class TopTrack(
    @Embedded val track: TrackListItem,
    val plays: Int
)
