package com.example.axs_player.ui.screens

import android.net.Uri
import com.example.axs_player.data.db.AlbumListItem
import com.example.axs_player.data.db.ArtistEntity
import com.example.axs_player.data.db.PlaylistListItem
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.ui.components.AlbumItemData
import com.example.axs_player.ui.components.ArtistItemData
import com.example.axs_player.ui.components.PlaylistItemData
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.components.trackDisplayTitle

internal fun TrackListItem.toTrackItemData(images: ImageStore, currentTrackId: String?) = TrackItemData(
    id = id,
    title = trackDisplayTitle(title, subtitle),
    artists = artists,
    coverUri = images.coverUri(coverFileName),
    isPlaying = id == currentTrackId,
    hasVideo = hasVideo
)

internal fun AlbumListItem.toAlbumItemData(images: ImageStore) = AlbumItemData(
    id = id,
    title = title,
    year = year,
    coverUri = images.coverUri(coverFileName),
    isContainerOwned = isContainerOwned
)

internal fun ArtistEntity.toArtistItemData(photos: Map<String, List<Uri>>) = ArtistItemData(
    id = id,
    name = name,
    photoUri = photos[nameKey]?.firstOrNull()
)

internal fun PlaylistListItem.toPlaylistItemData(images: ImageStore) = PlaylistItemData(
    id = id,
    name = name,
    trackCount = trackCount,
    covers = listOf(firstCover, secondCover, thirdCover).map(images::coverUri)
)
