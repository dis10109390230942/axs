package com.example.axs_player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT playbackVolume FROM tracks WHERE id = :trackId")
    fun observeTrackPlaybackVolume(trackId: String): Flow<Float?>

    @Query("UPDATE tracks SET playbackVolume = :volume WHERE id = :trackId")
    suspend fun updateTrackPlaybackVolume(trackId: String, volume: Float)

    @Query("SELECT playbackVolume FROM tracks WHERE id = :trackId")
    suspend fun trackPlaybackVolume(trackId: String): Float?


    @Query("SELECT * FROM tracks")
    suspend fun allTracks(): List<TrackEntity>

    @Query("SELECT * FROM albums")
    suspend fun allAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM artists")
    suspend fun allArtists(): List<ArtistEntity>

    @Query("SELECT * FROM album_tracks")
    suspend fun allAlbumTracks(): List<AlbumTrackEntity>

    @Query("SELECT * FROM track_artists")
    suspend fun allTrackArtists(): List<TrackArtistEntity>

    @Query("SELECT * FROM album_artists")
    suspend fun allAlbumArtists(): List<AlbumArtistEntity>

    @Query("SELECT * FROM playlists")
    suspend fun allPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_items")
    suspend fun allPlaylistItems(): List<PlaylistItemEntity>

    @Query("SELECT * FROM play_events")
    suspend fun allPlayEvents(): List<PlayEventEntity>

    @Query(
        """
        UPDATE tracks
        SET parentTrackId = (
            SELECT parent.id FROM tracks parent
            WHERE lower(trim(parent.title)) = lower(trim(substr(tracks.title, 1, instr(lower(tracks.title), '(live') - 1)))
              AND parent.id != tracks.id
              AND lower(parent.title) NOT LIKE '%(live%'
            ORDER BY parent.addedAt ASC
            LIMIT 1
        )
        WHERE lower(title) LIKE '%(live%'
        """
    )
    suspend fun linkLiveTracksToParents()

    @Query("DELETE FROM album_tracks WHERE trackId IN (SELECT id FROM tracks WHERE parentTrackId IS NOT NULL)")
    suspend fun removeLiveTracksFromAlbums()

    @Query(
        """
        SELECT t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName
        FROM tracks t
        WHERE EXISTS (SELECT 1 FROM tracks live WHERE live.parentTrackId = t.id)
        ORDER BY t.addedAt DESC
        """
    )
    fun observeLiveParents(): Flow<List<TrackListItem>>

    @Query(
        """
        SELECT t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName,
            1 AS discNumber,
            ROW_NUMBER() OVER (ORDER BY t.addedAt ASC) AS position
        FROM tracks t
        WHERE t.parentTrackId = :parentTrackId
        ORDER BY t.addedAt ASC
        """
    )
    fun observeLiveTracks(parentTrackId: String): Flow<List<AlbumTrackListItem>>

    @Query(
        """
        SELECT t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName
        FROM tracks t
        WHERE t.id = (SELECT parentTrackId FROM tracks WHERE id = :trackId)
        """
    )
    fun observeOriginalTrack(trackId: String): Flow<TrackListItem?>

    @Query(
        """
        SELECT t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName
        FROM tracks t
        ORDER BY t.addedAt DESC
        """
    )
    fun observeTracks(): Flow<List<TrackListItem>>

    @Query(
        """
        SELECT al.id, al.title, al.year, al.coverFileName,
            al.textColorMode,
            $ALBUM_ARTISTS AS artists,
            al.isContainerOwned, al.isContainerOnly, al.containerFileName
        FROM albums al
        ORDER BY al.title COLLATE NOCASE
        """
    )
    fun observeAlbums(): Flow<List<AlbumListItem>>

    @Query(
        """
        SELECT al.id, al.title, al.year, al.description, al.coverFileName,
            al.backgroundTextureFileName, al.backgroundTextureScale, al.backgroundTextureFit,
            al.backgroundTextureOffsetX, al.backgroundTextureOffsetY, al.backgroundTextureBlendMode,
            al.backgroundTextureKeyColor, al.backgroundTextureTolerance, al.backgroundTextureOpacity,
            al.textColorMode,
            $ALBUM_ARTISTS AS artists,
            al.containerFileName, al.isContainerOwned, al.isContainerOnly, al.containerSyncState,
            al.containerManifestJson
        FROM albums al
        WHERE al.id = :albumId
        """
    )
    fun observeAlbum(albumId: String): Flow<AlbumDetails?>

    @Query(
        """
        SELECT t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo, atr.discNumber, atr.position,
            $TRACK_ARTISTS AS artists,
            COALESCE(t.coverFileName, al.coverFileName) AS coverFileName
        FROM album_tracks atr
        JOIN tracks t ON t.id = atr.trackId
        JOIN albums al ON al.id = atr.albumId
        WHERE atr.albumId = :albumId AND t.parentTrackId IS NULL
        ORDER BY atr.discNumber, atr.position
        """
    )
    fun observeAlbumTracks(albumId: String): Flow<List<AlbumTrackListItem>>

    @Query(
        """
        SELECT p.id, p.name,
            $PLAYLIST_COLUMNS
        FROM playlists p
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistListItem>>

    @Query("SELECT p.id, p.name, 0 AS trackCount, NULL AS firstCover, NULL AS secondCover, NULL AS thirdCover FROM playlists p ORDER BY p.createdAt DESC")
    fun observePlaylistChoices(): Flow<List<PlaylistListItem>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun nextPlaylistPosition(playlistId: String): Int

    @Query(
        """
        SELECT pi.id AS itemId, t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName
        FROM playlist_items pi
        JOIN tracks t ON t.id = pi.trackId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.position
        """
    )
    fun observePlaylistTracks(playlistId: String): Flow<List<PlaylistTrackListItem>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylist(playlistId: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE")
    fun observeArtists(): Flow<List<ArtistEntity>>

    @Query(
        """
        SELECT a.* FROM artists a
        ORDER BY (SELECT COUNT(*) FROM track_artists ta WHERE ta.artistId = a.id) DESC, a.name COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun observeArtistsByTrackCount(limit: Int): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :artistId")
    fun observeArtist(artistId: String): Flow<ArtistEntity?>

    @Query(
        """
        SELECT t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName
        FROM track_artists link
        JOIN tracks t ON t.id = link.trackId
        WHERE link.artistId = :artistId
        ORDER BY t.addedAt DESC
        """
    )
    fun observeArtistTracks(artistId: String): Flow<List<TrackListItem>>

    @Query(
        """
        SELECT al.id, al.title, al.year, al.coverFileName,
            al.textColorMode,
            $ALBUM_ARTISTS AS artists,
            al.isContainerOwned, al.isContainerOnly, al.containerFileName
        FROM album_artists link
        JOIN albums al ON al.id = link.albumId
        WHERE link.artistId = :artistId
        ORDER BY al.year DESC, al.createdAt DESC
        """
    )
    fun observeArtistAlbums(artistId: String): Flow<List<AlbumListItem>>

    @Query(
        """
        SELECT 'ALBUM' AS kind, al.id AS id, al.coverFileName AS coverFileName, al.createdAt AS addedAt
        FROM albums al
        UNION ALL
        SELECT 'TRACK' AS kind, t.id AS id, $TRACK_COVER AS coverFileName, t.addedAt AS addedAt
        FROM tracks t
        WHERE NOT EXISTS (SELECT 1 FROM album_tracks atr WHERE atr.trackId = t.id)
        ORDER BY addedAt DESC
        LIMIT :limit
        """
    )
    fun observeReleases(limit: Int): Flow<List<CatalogRelease>>

    @Query(
        """
        SELECT a.* FROM track_artists ta
        JOIN artists a ON a.id = ta.artistId
        WHERE ta.trackId = :trackId
        ORDER BY ta.position
        """
    )
    fun observeTrackArtists(trackId: String): Flow<List<ArtistEntity>>

    @Query(
        """
        SELECT al.id, al.title, al.year, al.coverFileName, NULL AS artists,
            al.isContainerOwned, al.isContainerOnly, al.containerFileName,
            al.textColorMode
        FROM album_tracks atr
        JOIN albums al ON al.id = atr.albumId
        WHERE atr.trackId = :trackId
        ORDER BY al.createdAt
        """
    )
    fun observeTrackAlbums(trackId: String): Flow<List<AlbumListItem>>

    @Query(
        """
        SELECT al.backgroundTextureFileName AS fileName, al.backgroundTextureScale AS scale,
            al.backgroundTextureOffsetX AS offsetX, al.backgroundTextureOffsetY AS offsetY,
            al.backgroundTextureOpacity AS opacity
        FROM album_tracks atr
        JOIN albums al ON al.id = atr.albumId
        WHERE atr.trackId = :trackId
        ORDER BY al.createdAt
        LIMIT 1
        """
    )
    fun observeTrackAlbumTexture(trackId: String): Flow<AlbumTextureInfo?>

    @Query("SELECT albumId FROM album_tracks WHERE trackId = :trackId ORDER BY rowid LIMIT 1")
    suspend fun firstAlbumIdForTrack(trackId: String): String?

    @Query("SELECT artistId FROM track_artists WHERE trackId = :trackId ORDER BY position LIMIT 1")
    suspend fun firstArtistIdForTrack(trackId: String): String?

    @Query(
        """
        SELECT t.year, t.composer, t.description, t.parentTrackId, t.lyrics,
            (SELECT atr.position FROM album_tracks atr
             JOIN albums al ON al.id = atr.albumId
             WHERE atr.trackId = t.id
             ORDER BY al.createdAt
             LIMIT 1) AS albumPosition
        FROM tracks t
        WHERE t.id = :trackId
        """
    )
    fun observeTrackDetails(trackId: String): Flow<TrackDetails?>

    @Query("SELECT * FROM album_tracks WHERE albumId = :albumId ORDER BY discNumber, position")
    suspend fun albumTracksForAlbum(albumId: String): List<AlbumTrackEntity>

    @Query("SELECT a.name FROM album_artists aa JOIN artists a ON a.id = aa.artistId WHERE aa.albumId = :albumId ORDER BY aa.position")
    suspend fun artistNamesForAlbum(albumId: String): List<String>

    @Query("SELECT a.name, ta.role FROM track_artists ta JOIN artists a ON a.id = ta.artistId WHERE ta.trackId = :trackId ORDER BY ta.position")
    suspend fun trackArtistDetails(trackId: String): List<TrackArtistDetail>

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    fun observeTrack(trackId: String): Flow<TrackEntity?>

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun trackEntity(trackId: String): TrackEntity?

    @Query("SELECT t.* FROM tracks t JOIN album_tracks atr ON atr.trackId = t.id WHERE atr.albumId = :albumId")
    suspend fun tracksInAlbum(albumId: String): List<TrackEntity>

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: String)

    @Query("UPDATE tracks SET title = :title, subtitle = :subtitle, trackDate = :trackDate, description = :description, year = :year, lyrics = :lyrics WHERE id = :trackId")
    suspend fun updateTrackMetadata(trackId: String, title: String, subtitle: String?, trackDate: String?, description: String?, year: Int?, lyrics: String?)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)

    @Query(
        """
        SELECT t.id, t.title, t.subtitle, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName
        FROM tracks t
        WHERE t.id = :trackId
        """
    )
    suspend fun track(trackId: String): TrackListItem?

    @Query("SELECT t.id, $TRACK_COVER AS coverFileName FROM tracks t WHERE t.id IN (:trackIds)")
    suspend fun trackCovers(trackIds: List<String>): List<TrackCover>

    @Query("UPDATE tracks SET coverFileName = :coverFileName WHERE id = :trackId")
    suspend fun updateTrackCover(trackId: String, coverFileName: String)

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun albumEntity(albumId: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE containerFileName = :fileName LIMIT 1")
    suspend fun findAlbumByContainerFileName(fileName: String): AlbumEntity?

    @Query("UPDATE albums SET containerSyncState = :syncState WHERE id = :albumId")
    suspend fun updateAlbumSyncState(albumId: String, syncState: String?)

    @Query("UPDATE albums SET containerManifestJson = :json WHERE id = :albumId")
    suspend fun updateAlbumManifestJson(albumId: String, json: String?)

    @Query("UPDATE albums SET coverFileName = :coverFileName WHERE id = :albumId AND coverFileName IS NULL")
    suspend fun updateAlbumCover(albumId: String, coverFileName: String)

    @Query("UPDATE albums SET description = :description WHERE id = :albumId")
    suspend fun updateAlbumDescription(albumId: String, description: String?)

    @Query("UPDATE albums SET textColorMode = :mode WHERE id = :albumId")
    suspend fun updateAlbumTextColorMode(albumId: String, mode: String)

    @Query("UPDATE albums SET backgroundTextureFileName = :fileName, backgroundTextureScale = :scale, backgroundTextureFit = :fit, backgroundTextureOffsetX = :offsetX, backgroundTextureOffsetY = :offsetY, backgroundTextureBlendMode = :blendMode, backgroundTextureKeyColor = :keyColor, backgroundTextureTolerance = :tolerance, backgroundTextureOpacity = :opacity WHERE id = :albumId")
    suspend fun updateAlbumBackgroundTexture(albumId: String, fileName: String?, scale: Float, fit: String, offsetX: Float, offsetY: Float, blendMode: String, keyColor: Int, tolerance: Float, opacity: Float)

    @Query("SELECT id, fileName, contentHash, available FROM tracks")
    suspend fun trackFileStates(): List<TrackFileState>

    @Query("SELECT * FROM tracks WHERE hasVideo = 1 AND (durationMs IS NULL OR durationMs <= 0)")
    suspend fun tracksMissingVideoDuration(): List<TrackEntity>

    @Query("UPDATE tracks SET durationMs = :durationMs WHERE id = :trackId")
    suspend fun updateTrackDuration(trackId: String, durationMs: Long)

    @Query("UPDATE tracks SET available = :available WHERE id IN (:ids)")
    suspend fun updateAvailability(ids: List<String>, available: Boolean)

    @Query("UPDATE tracks SET contentHash = :contentHash WHERE id = :id")
    suspend fun updateContentHash(id: String, contentHash: String)

    @Query("UPDATE tracks SET fileName = :fileName, fileSize = :fileSize, available = 1 WHERE id = :id")
    suspend fun relinkTrack(id: String, fileName: String, fileSize: Long)

    @Query("SELECT * FROM artists WHERE nameKey = :nameKey LIMIT 1")
    suspend fun findArtistByKey(nameKey: String): ArtistEntity?

    @Query("SELECT * FROM artists WHERE id = :id LIMIT 1")
    suspend fun findArtistById(id: String): ArtistEntity?

    @Query("SELECT al.* FROM albums al JOIN album_artists aa ON aa.albumId = al.id WHERE aa.artistId = :artistId")
    suspend fun albumsOfArtist(artistId: String): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE id NOT IN (SELECT albumId FROM album_artists)")
    suspend fun albumsWithoutArtists(): List<AlbumEntity>

    @Query("SELECT artistId FROM album_artists WHERE albumId = :albumId")
    suspend fun albumArtistIds(albumId: String): List<String>

    @Query("SELECT COALESCE(MAX(position), 0) FROM album_tracks WHERE albumId = :albumId AND discNumber = :discNumber")
    suspend fun maxAlbumPosition(albumId: String, discNumber: Int): Int

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun findTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE contentHash = :hash LIMIT 1")
    suspend fun findTrackByContentHash(hash: String): TrackEntity?

    @Query("DELETE FROM album_artists WHERE albumId = :albumId")
    suspend fun deleteAlbumArtists(albumId: String)

    @Query("DELETE FROM album_tracks WHERE albumId = :albumId")
    suspend fun deleteAlbumTracks(albumId: String)

    @Query("DELETE FROM track_artists WHERE trackId = :trackId")
    suspend fun deleteTrackArtists(trackId: String)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Insert
    suspend fun insertTrack(track: TrackEntity)

    @Insert
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Insert
    suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

    @Insert
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Insert
    suspend fun insertArtist(artist: ArtistEntity)

    @Insert
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Update
    suspend fun updateArtist(artist: ArtistEntity)

    @Insert
    suspend fun insertAlbumTrack(albumTrack: AlbumTrackEntity)

    @Insert
    suspend fun insertAlbumTracks(albumTracks: List<AlbumTrackEntity>)

    @Insert
    suspend fun insertTrackArtists(trackArtists: List<TrackArtistEntity>)

    @Insert
    suspend fun insertAlbumArtists(albumArtists: List<AlbumArtistEntity>)

    @Insert
    suspend fun insertPlayEvents(playEvents: List<PlayEventEntity>)
}
