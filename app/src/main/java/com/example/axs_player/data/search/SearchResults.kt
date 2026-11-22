package com.example.axs_player.data.search

import com.example.axs_player.data.db.AlbumListItem
import com.example.axs_player.data.db.ArtistEntity
import com.example.axs_player.data.db.TrackListItem

enum class SearchCategory(val path: String) {
    ARTISTS("artists"),
    TRACKS("tracks"),
    ALBUMS("albums");

    companion object {
        fun fromPath(path: String?): SearchCategory? = entries.firstOrNull { it.path == path }
    }
}

sealed interface TopMatch {
    data class Artist(val artist: ArtistEntity) : TopMatch
    data class Album(val album: AlbumListItem) : TopMatch
    data class Track(val track: TrackListItem) : TopMatch
}

data class SearchResults(
    val top: TopMatch?,
    val artists: List<ArtistEntity>,
    val tracks: List<TrackListItem>,
    val albums: List<AlbumListItem>
) {
    val isEmpty: Boolean get() = artists.isEmpty() && tracks.isEmpty() && albums.isEmpty()

    companion object {
        val EMPTY = SearchResults(null, emptyList(), emptyList(), emptyList())
    }
}
