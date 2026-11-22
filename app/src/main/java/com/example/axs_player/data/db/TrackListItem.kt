package com.example.axs_player.data.db

data class TrackListItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val artists: String?,
    val coverFileName: String?,
    val fileName: String,
    val durationMs: Long?,
    val hasVideo: Boolean
)
