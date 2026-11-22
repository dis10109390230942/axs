package com.example.axs_player.data.stats

import com.example.axs_player.data.db.HistoryEntry
import com.example.axs_player.data.db.StatsDao
import com.example.axs_player.data.db.TopArtist
import com.example.axs_player.data.db.TopPlaylist
import com.example.axs_player.data.db.TopRelease
import com.example.axs_player.data.db.TopTrack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.Flow

private val TOP_WINDOW: Duration = 90.days
private const val PLAYLIST_ARTIST_WEIGHT = 0.25

class ListeningStats(private val dao: StatsDao) {

    fun recentTracks(limit: Int): Flow<List<HistoryEntry>> = dao.observeRecentTracks(limit)

    fun artistTopTracks(artistId: String, limit: Int): Flow<List<TopTrack>> =
        dao.observeArtistTopTracks(artistId, windowStart(), limit)

    fun topArtists(limit: Int): Flow<List<TopArtist>> = dao.observeTopArtists(windowStart(), limit)

    fun topPlaylists(limit: Int): Flow<List<TopPlaylist>> =
        dao.observeTopPlaylists(windowStart(), PLAYLIST_ARTIST_WEIGHT, limit)

    fun topReleases(limit: Int): Flow<List<TopRelease>> = dao.observeTopReleases(windowStart(), limit)

    private fun windowStart(): Long = System.currentTimeMillis() - TOP_WINDOW.inWholeMilliseconds
}
