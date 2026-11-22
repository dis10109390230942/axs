package com.example.axs_player.navigation

enum class ArtistCategory(val path: String) {
    TRACKS("tracks"),
    ALBUMS("albums");

    companion object {
        fun fromPath(path: String?): ArtistCategory? = entries.firstOrNull { it.path == path }
    }
}
