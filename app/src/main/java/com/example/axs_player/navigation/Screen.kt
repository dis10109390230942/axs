package com.example.axs_player.navigation

import android.net.Uri
import com.example.axs_player.data.search.SearchCategory

sealed class Screen(val route: String) {
    data object Scan : Screen("scan")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object SearchResults : Screen("search/{$SEARCH_CATEGORY_ARGUMENT}?$SEARCH_QUERY_ARGUMENT={$SEARCH_QUERY_ARGUMENT}") {
        fun route(category: SearchCategory, query: String): String =
            "search/${category.path}?$SEARCH_QUERY_ARGUMENT=${Uri.encode(query)}"
    }
    data object Catalog : Screen("catalog")
    data object More : Screen("more")

    data object FavoriteSongs : Screen("catalog/favorite_songs")
    data object Lives : Screen("catalog/lives")
    data object LiveTrack : Screen("catalog/lives/{$TRACK_ID_ARGUMENT}") {
        fun route(trackId: String): String = "catalog/lives/$trackId"
    }
    data object Albums : Screen("catalog/albums")
    data object Album : Screen("catalog/albums/{$ALBUM_ID_ARGUMENT}") {
        fun route(albumId: String): String = "catalog/albums/$albumId"
    }
    data object AlbumEdit : Screen("catalog/albums/{$ALBUM_ID_ARGUMENT}/edit") {
        fun route(albumId: String): String = "catalog/albums/$albumId/edit"
    }
    data object AlbumTextureEdit : Screen("catalog/albums/{$ALBUM_ID_ARGUMENT}/edit/texture") {
        fun route(albumId: String): String = "catalog/albums/$albumId/edit/texture"
    }
    data object AlbumDescription : Screen("catalog/albums/{$ALBUM_ID_ARGUMENT}/description") {
        fun route(albumId: String): String = "catalog/albums/$albumId/description"
    }
    data object AlbumDescriptionEdit : Screen("catalog/albums/{$ALBUM_ID_ARGUMENT}/edit/description") {
        fun route(albumId: String): String = "catalog/albums/$albumId/edit/description"
    }
    data object TrackEdit : Screen("catalog/tracks/{$TRACK_ID_ARGUMENT}/edit") {
        fun route(trackId: String): String = "catalog/tracks/$trackId/edit"
    }
    data object TrackDescriptionEdit : Screen("catalog/tracks/{$TRACK_ID_ARGUMENT}/edit/description") {
        fun route(trackId: String): String = "catalog/tracks/$trackId/edit/description"
    }
    data object TrackLyricsEdit : Screen("catalog/tracks/{$TRACK_ID_ARGUMENT}/lyrics") {
        fun route(trackId: String): String = "catalog/tracks/$trackId/lyrics"
    }
    data object Playlists : Screen("catalog/playlists")
    data object Playlist : Screen("catalog/playlists/{$PLAYLIST_ID_ARGUMENT}") {
        fun route(playlistId: String): String = "catalog/playlists/$playlistId"
    }
    data object Artists : Screen("catalog/artists")
    data object Artist : Screen("catalog/artists/{$ARTIST_ID_ARGUMENT}") {
        fun route(artistId: String): String = "catalog/artists/$artistId"
    }
    data object ArtistItems : Screen("catalog/artists/{$ARTIST_ID_ARGUMENT}/{$ARTIST_CATEGORY_ARGUMENT}") {
        fun route(artistId: String, category: ArtistCategory): String =
            "catalog/artists/$artistId/${category.path}"
    }
    data object ArtistBio : Screen("catalog/artist_bio/{$ARTIST_ID_ARGUMENT}") {
        fun route(artistId: String): String = "catalog/artist_bio/$artistId"
    }
    data object ArtistEdit : Screen("catalog/artist_edit/{$ARTIST_ID_ARGUMENT}") {
        fun route(artistId: String): String = "catalog/artist_edit/$artistId"
    }
    data object ArtistBioEdit : Screen("catalog/artist_edit/{$ARTIST_ID_ARGUMENT}/bio") {
        fun route(artistId: String): String = "catalog/artist_edit/$artistId/bio"
    }
    data object ListeningHistory : Screen("catalog/listening_history")

    data object Playback : Screen("more/playback")
    data object DataSaving : Screen("more/data_saving")
}

const val ALBUM_ID_ARGUMENT = "albumId"
const val ARTIST_ID_ARGUMENT = "artistId"
const val ARTIST_CATEGORY_ARGUMENT = "artistCategory"
const val PLAYLIST_ID_ARGUMENT = "playlistId"
const val TRACK_ID_ARGUMENT = "trackId"
const val SEARCH_CATEGORY_ARGUMENT = "category"
const val SEARCH_QUERY_ARGUMENT = "query"
