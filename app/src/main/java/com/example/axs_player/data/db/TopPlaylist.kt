package com.example.axs_player.data.db

import androidx.room.Embedded

data class TopPlaylist(
    @Embedded val playlist: PlaylistListItem,
    val trackPlays: Int,
    val artistPlays: Int,
    val score: Double
)
