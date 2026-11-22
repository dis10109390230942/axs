package com.example.axs_player.data.db

data class AlbumListItem(
    val id: String,
    val title: String,
    val year: Int?,
    val artists: String?,
    val coverFileName: String?,
    val isContainerOwned: Boolean = false,
    val isContainerOnly: Boolean = false,
    val containerFileName: String? = null,
    val textColorMode: String = "black"
)
