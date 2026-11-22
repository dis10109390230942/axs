package com.example.axs_player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "artists", indices = [Index(value = ["nameKey"], unique = true)])
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameKey: String,
    val bio: String?,
    val createdAt: Long,
    val fullName: String? = null,
    val birthDate: String? = null,
    val deathDate: String? = null,
    val birthPlace: String? = null,
    val countries: String? = null
)
