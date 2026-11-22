package com.example.axs_player.ui.player

import android.net.Uri
import com.example.axs_player.ui.components.AlbumItemData
import com.example.axs_player.ui.components.AlbumTrackItemData
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.components.ArtistItemData

data class TrackInfo(
    val artists: List<ArtistItemData> = emptyList(),
    val albums: List<AlbumItemData> = emptyList(),
    val year: Int? = null,
    val albumPosition: Int? = null,
    val composer: String? = null,
    val description: String? = null,
    val lyrics: String? = null,
    val lives: List<AlbumTrackItemData> = emptyList(),
    val originalTrack: TrackItemData? = null,
    val albumTextureUri: Uri? = null,
    val albumTextureScale: Float = 1f,
    val albumTextureOffsetX: Float = 0f,
    val albumTextureOffsetY: Float = 0f,
    val albumTextureOpacity: Float = 1f
)
