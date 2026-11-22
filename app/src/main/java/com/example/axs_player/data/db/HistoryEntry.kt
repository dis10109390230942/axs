package com.example.axs_player.data.db

import androidx.room.Embedded

data class HistoryEntry(
    @Embedded val track: TrackListItem,
    val playedAt: Long
)
