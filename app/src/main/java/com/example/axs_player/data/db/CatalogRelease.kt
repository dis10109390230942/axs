package com.example.axs_player.data.db

data class CatalogRelease(
    val kind: ReleaseKind,
    val id: String,
    val coverFileName: String?,
    val addedAt: Long
)
