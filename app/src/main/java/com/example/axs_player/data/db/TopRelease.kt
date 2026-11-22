package com.example.axs_player.data.db

data class TopRelease(
    val kind: ReleaseKind,
    val id: String,
    val coverFileName: String?,
    val plays: Int
)
