package com.example.axs_player.ui.screens.search

import android.net.Uri
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.search.SearchHistoryStore
import com.example.axs_player.data.search.SearchResults
import com.example.axs_player.data.search.TopMatch
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.ui.components.AlbumItemData
import com.example.axs_player.ui.components.ArtistItemData
import com.example.axs_player.ui.components.TopResultData
import com.example.axs_player.ui.components.TopResultKind
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.screens.toAlbumItemData
import com.example.axs_player.ui.screens.toArtistItemData
import com.example.axs_player.ui.screens.toTrackItemData

data class SearchResultsData(
    val top: TopResultData?,
    val artists: List<ArtistItemData>,
    val tracks: List<TrackItemData>,
    val albums: List<AlbumItemData>
) {
    val isEmpty: Boolean get() = artists.isEmpty() && tracks.isEmpty() && albums.isEmpty()

    companion object {
        val EMPTY = SearchResultsData(null, emptyList(), emptyList(), emptyList())
    }
}

internal fun SearchResults.toData(
    images: ImageStore,
    photos: Map<String, List<Uri>>,
    currentTrackId: String?,
    limit: Int = Int.MAX_VALUE
) = SearchResultsData(
    top = top?.toTopResultData(images, photos),
    artists = artists.take(limit).map { it.toArtistItemData(photos) },
    tracks = tracks.take(limit).map { it.toTrackItemData(images, currentTrackId) },
    albums = albums.take(limit).map { it.toAlbumItemData(images) }
)

internal fun playSearchTracks(
    trackId: String,
    tracks: List<TrackListItem>,
    playback: LibraryPlayback,
    history: SearchHistoryStore
) {
    playback.play(tracks, trackId)
    history.add(trackId)
}

private fun TopMatch.toTopResultData(images: ImageStore, photos: Map<String, List<Uri>>): TopResultData =
    when (this) {
        is TopMatch.Artist -> TopResultData(
            artist.id,
            TopResultKind.ARTIST,
            artist.name,
            "artist",
            photos[artist.nameKey]?.firstOrNull()
        )
        is TopMatch.Album -> TopResultData(
            album.id,
            TopResultKind.ALBUM,
            album.title,
            subtitle("album", album.artists),
            images.coverUri(album.coverFileName)
        )
        is TopMatch.Track -> TopResultData(
            track.id,
            TopResultKind.TRACK,
            com.example.axs_player.ui.components.trackDisplayTitle(track.title, track.subtitle),
            subtitle("track", track.artists),
            images.coverUri(track.coverFileName)
        )
    }

private fun subtitle(type: String, artists: String?): String =
    listOfNotNull(type, artists?.takeIf { it.isNotBlank() }).joinToString(" • ")
