package com.example.axs_player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    indices = [Index(value = ["containerFileName"])]
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val year: Int?,
    val description: String?,
    val coverFileName: String?,
    val backgroundTextureFileName: String? = null,
    val backgroundTextureScale: Float = 1f,
    val backgroundTextureFit: String = "cover",
    val backgroundTextureOffsetX: Float = 0f,
    val backgroundTextureOffsetY: Float = 0f,
    val backgroundTextureBlendMode: String = "multiply",
    val backgroundTextureKeyColor: Int = -1,
    val backgroundTextureTolerance: Float = 32f,
    val backgroundTextureOpacity: Float = 1f,
    val createdAt: Long,
    val containerFileName: String? = null,
    val isContainerOwned: Boolean = false,
    val isContainerOnly: Boolean = false,
    val containerSyncState: String? = null,
    val containerManifestJson: String? = null
)
