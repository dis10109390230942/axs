package com.example.axs_player.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracks", indices = [Index("contentHash")])
data class TrackEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val fileSize: Long,
    val contentHash: String,
    val title: String,
    val subtitle: String? = null,
    val trackDate: String? = null,
    val year: Int?,
    val coverFileName: String?,
    val durationMs: Long?,
    val mimeType: String?,
    val available: Boolean,
    val addedAt: Long,
    @ColumnInfo(defaultValue = "0") val hasVideo: Boolean,
    val composer: String?,
    val parentTrackId: String?,
    val description: String?,
    val lyrics: String?,
    @ColumnInfo(defaultValue = "1.0") val playbackVolume: Float = 1f
)
