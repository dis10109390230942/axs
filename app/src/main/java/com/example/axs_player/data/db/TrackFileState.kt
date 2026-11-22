package com.example.axs_player.data.db

data class TrackFileState(
    val id: String,
    val fileName: String,
    val contentHash: String,
    val available: Boolean
)
