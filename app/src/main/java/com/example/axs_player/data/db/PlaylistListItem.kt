package com.example.axs_player.data.db

data class PlaylistListItem(
    val id: String,
    val name: String,
    val trackCount: Int,
    val firstCover: String?,
    val secondCover: String?,
    val thirdCover: String?
)
