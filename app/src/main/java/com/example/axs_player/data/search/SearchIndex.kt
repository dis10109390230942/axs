package com.example.axs_player.data.search

import com.example.axs_player.data.db.AlbumListItem
import com.example.axs_player.data.db.ArtistEntity
import com.example.axs_player.data.db.TrackListItem
import java.util.Locale

private const val SECONDARY_MATCH_PENALTY = 4
private const val ARTIST_PRIORITY = 0
private const val ALBUM_PRIORITY = 1
private const val TRACK_PRIORITY = 2

private class Entry<T>(val item: T, val primary: String, val secondary: String)

private class Ranked<T>(val item: T, val score: Int, val length: Int)

private class Candidate(val score: Int, val priority: Int, val match: TopMatch)

class SearchIndex private constructor(
    private val artists: List<Entry<ArtistEntity>>,
    private val tracks: List<Entry<TrackListItem>>,
    private val albums: List<Entry<AlbumListItem>>
) {

    private val tracksById = tracks.associateBy { it.item.id }

    fun search(rawQuery: String): SearchResults {
        val query = normalize(rawQuery)
        if (query.isEmpty()) return SearchResults.EMPTY
        val artistMatches = rank(artists, query)
        val trackMatches = rank(tracks, query)
        val albumMatches = rank(albums, query)
        val top = listOfNotNull(
            artistMatches.firstOrNull()?.let { Candidate(it.score, ARTIST_PRIORITY, TopMatch.Artist(it.item)) },
            albumMatches.firstOrNull()?.let { Candidate(it.score, ALBUM_PRIORITY, TopMatch.Album(it.item)) },
            trackMatches.firstOrNull()?.let { Candidate(it.score, TRACK_PRIORITY, TopMatch.Track(it.item)) }
        ).minWithOrNull(compareBy<Candidate>({ it.score }, { it.priority }))?.match
        return SearchResults(
            top = top,
            artists = artistMatches.map { it.item },
            tracks = trackMatches.map { it.item },
            albums = albumMatches.map { it.item }
        )
    }

    fun tracksById(ids: List<String>): List<TrackListItem> = ids.mapNotNull { tracksById[it]?.item }

    fun allTracks(): List<TrackListItem> = tracks.map { it.item }

    companion object {
        fun build(
            tracks: List<TrackListItem>,
            albums: List<AlbumListItem>,
            artists: List<ArtistEntity>
        ): SearchIndex = SearchIndex(
            artists = artists.map { Entry(it, normalize(it.name), "") },
            tracks = tracks.map { Entry(it, normalize(it.title), normalize(it.artists.orEmpty())) },
            albums = albums.map { Entry(it, normalize(it.title), normalize(it.artists.orEmpty())) }
        )
    }
}

private fun <T> rank(entries: List<Entry<T>>, query: String): List<Ranked<T>> =
    entries
        .mapNotNull { entry ->
            val score = listOfNotNull(
                matchScore(entry.primary, query),
                matchScore(entry.secondary, query)?.plus(SECONDARY_MATCH_PENALTY)
            ).minOrNull() ?: return@mapNotNull null
            Ranked(entry.item, score, entry.primary.length)
        }
        .sortedWith(compareBy<Ranked<T>>({ it.score }, { it.length }))

private fun matchScore(text: String, query: String): Int? {
    if (text.isEmpty()) return null
    if (text == query) return 0
    if (text.startsWith(query)) return 1
    val index = text.indexOf(query)
    if (index < 0) return null
    return if (text[index - 1].isLetterOrDigit()) 3 else 2
}

private fun normalize(text: String): String = text.trim().lowercase(Locale.ROOT).replace('ё', 'е')
