package com.example.axs_player.data.library

class AudioTags(
    val title: String?,
    val artists: List<String>,
    val albumArtists: List<String>,
    val album: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val trackDate: String?,
    val durationMs: Long?,
    val artwork: ByteArray?,
    val hasVideo: Boolean,
    val composer: String?
)
