package com.example.axs_player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert
    suspend fun insertPlayEvent(event: PlayEventEntity)

    @Query(
        """
        SELECT t.id, t.title, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName,
            MAX(e.playedAt) AS playedAt
        FROM play_events e
        JOIN tracks t ON t.id = e.trackId
        GROUP BY t.id
        ORDER BY playedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentTracks(limit: Int): Flow<List<HistoryEntry>>

    @Query(
        """
        SELECT t.id, t.title, t.fileName, t.durationMs, t.hasVideo,
            $TRACK_ARTISTS AS artists,
            $TRACK_COVER AS coverFileName,
            COUNT(*) AS plays
        FROM play_events e
        JOIN tracks t ON t.id = e.trackId
        JOIN track_artists link ON link.trackId = t.id
        WHERE link.artistId = :artistId AND e.playedAt >= :since
        GROUP BY t.id
        ORDER BY plays DESC, MAX(e.playedAt) DESC
        LIMIT :limit
        """
    )
    fun observeArtistTopTracks(artistId: String, since: Long, limit: Int): Flow<List<TopTrack>>

    @Query(
        """
        SELECT a.id, a.name, a.nameKey, a.bio, a.createdAt,
            COUNT(*) AS plays
        FROM play_events e
        JOIN track_artists ta ON ta.trackId = e.trackId
        JOIN artists a ON a.id = ta.artistId
        WHERE e.playedAt >= :since
        GROUP BY a.id
        ORDER BY plays DESC, MAX(e.playedAt) DESC
        LIMIT :limit
        """
    )
    fun observeTopArtists(since: Long, limit: Int): Flow<List<TopArtist>>

    @Query(
        """
        SELECT 'ALBUM' AS kind, al.id AS id, al.coverFileName AS coverFileName, COUNT(*) AS plays
        FROM play_events e
        JOIN album_tracks atr ON atr.trackId = e.trackId
        JOIN albums al ON al.id = atr.albumId
        WHERE e.playedAt >= :since
        GROUP BY al.id
        UNION ALL
        SELECT 'TRACK' AS kind, t.id AS id, $TRACK_COVER AS coverFileName, COUNT(*) AS plays
        FROM play_events e
        JOIN tracks t ON t.id = e.trackId
        WHERE e.playedAt >= :since
            AND NOT EXISTS (SELECT 1 FROM album_tracks atr WHERE atr.trackId = t.id)
        GROUP BY t.id
        ORDER BY plays DESC
        LIMIT :limit
        """
    )
    fun observeTopReleases(since: Long, limit: Int): Flow<List<TopRelease>>

    @Query(
        """
        SELECT p.id, p.name,
            $PLAYLIST_COLUMNS,
            COALESCE(tp.plays, 0) AS trackPlays,
            COALESCE(ap.plays, 0) AS artistPlays,
            COALESCE(tp.plays, 0) + COALESCE(ap.plays, 0) * :artistWeight AS score
        FROM playlists p
        LEFT JOIN (
            SELECT pi.playlistId AS playlistId, COUNT(*) AS plays
            FROM playlist_items pi
            JOIN play_events e ON e.trackId = pi.trackId
            WHERE e.playedAt >= :since
            GROUP BY pi.playlistId
        ) tp ON tp.playlistId = p.id
        LEFT JOIN (
            SELECT playlistId, COUNT(*) AS plays FROM (
                SELECT DISTINCT pi.playlistId AS playlistId, e.id AS eventId
                FROM playlist_items pi
                JOIN track_artists pa ON pa.trackId = pi.trackId
                JOIN track_artists ea ON ea.artistId = pa.artistId
                JOIN play_events e ON e.trackId = ea.trackId
                WHERE e.playedAt >= :since
                    AND e.trackId NOT IN (
                        SELECT other.trackId FROM playlist_items other WHERE other.playlistId = pi.playlistId
                    )
            ) events
            GROUP BY playlistId
        ) ap ON ap.playlistId = p.id
        WHERE COALESCE(tp.plays, 0) + COALESCE(ap.plays, 0) > 0
        ORDER BY score DESC, p.createdAt DESC
        LIMIT :limit
        """
    )
    fun observeTopPlaylists(since: Long, artistWeight: Double, limit: Int): Flow<List<TopPlaylist>>
}
