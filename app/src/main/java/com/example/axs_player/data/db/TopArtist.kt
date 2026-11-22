package com.example.axs_player.data.db

import androidx.room.Embedded

data class TopArtist(
    @Embedded val artist: ArtistEntity,
    val plays: Int
)
