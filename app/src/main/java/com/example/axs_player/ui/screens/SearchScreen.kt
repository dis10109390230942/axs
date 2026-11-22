package com.example.axs_player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.data.search.SearchCategory
import com.example.axs_player.ui.components.AlbumItem
import com.example.axs_player.ui.components.ArtistItem
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SearchInput
import com.example.axs_player.ui.components.TopResultItem
import com.example.axs_player.ui.components.TopResultKind
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.screens.search.SearchResultsData
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

private const val ARTIST_COLUMNS = 3
private const val ALBUM_COLUMNS = 2

@Composable
fun SearchScreen(
    onOpenAlbum: (albumId: String) -> Unit,
    onOpenArtist: (artistId: String) -> Unit,
    onViewAll: (category: SearchCategory, query: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
    ) {
        item {
            SearchInput(value = query, onValueChange = viewModel::onQueryChange)
        }
        val currentContent = content
        when {
            query.isBlank() -> if (recents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    TrackColumn(tracks = recents, onTrackClick = viewModel::playRecent)
                }
            }
            currentContent == null -> Unit
            currentContent.isEmpty -> item {
                Spacer(modifier = Modifier.height(30.dp))
                Text(text = "nothing found", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            else -> item {
                Spacer(modifier = Modifier.height(30.dp))
                SearchResultsContent(
                    content = currentContent,
                    onTrackClick = viewModel::playResult,
                    onOpenAlbum = onOpenAlbum,
                    onOpenArtist = onOpenArtist,
                    onViewAll = { onViewAll(it, query.trim()) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    content: SearchResultsData,
    onTrackClick: (trackId: String) -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onOpenArtist: (artistId: String) -> Unit,
    onViewAll: (SearchCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
        content.top?.let { top ->
            Section(title = "top result") {
                TopResultItem(
                    result = top,
                    onClick = {
                        when (top.kind) {
                            TopResultKind.TRACK -> onTrackClick(top.id)
                            TopResultKind.ALBUM -> onOpenAlbum(top.id)
                            TopResultKind.ARTIST -> onOpenArtist(top.id)
                        }
                    }
                )
            }
        }
        if (content.artists.isNotEmpty()) {
            Section(
                title = "artists",
                hasMore = content.artists.size > SEARCH_SECTION_LIMIT,
                onViewAll = { onViewAll(SearchCategory.ARTISTS) }
            ) {
                val artists = content.artists.take(SEARCH_SECTION_LIMIT)
                CardRows(count = artists.size, columns = ARTIST_COLUMNS) { index, cardModifier ->
                    val artist = artists[index]
                    ArtistItem(artist = artist, onClick = { onOpenArtist(artist.id) }, modifier = cardModifier)
                }
            }
        }
        if (content.tracks.isNotEmpty()) {
            Section(
                title = "tracks",
                hasMore = content.tracks.size > SEARCH_SECTION_LIMIT,
                onViewAll = { onViewAll(SearchCategory.TRACKS) }
            ) {
                TrackColumn(tracks = content.tracks.take(SEARCH_SECTION_LIMIT), onTrackClick = onTrackClick)
            }
        }
        if (content.albums.isNotEmpty()) {
            Section(
                title = "albums",
                hasMore = content.albums.size > SEARCH_SECTION_LIMIT,
                onViewAll = { onViewAll(SearchCategory.ALBUMS) }
            ) {
                val albums = content.albums.take(SEARCH_SECTION_LIMIT)
                CardRows(count = albums.size, columns = ALBUM_COLUMNS) { index, cardModifier ->
                    val album = albums[index]
                    AlbumItem(album = album, onClick = { onOpenAlbum(album.id) }, modifier = cardModifier)
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    hasMore: Boolean = false,
    onViewAll: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, color = TextBlack, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        content()
        if (hasMore) PrimaryButton(text = "view all", onClick = onViewAll)
    }
}

@Composable
private fun TrackColumn(tracks: List<TrackItemData>, onTrackClick: (trackId: String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tracks.forEach { track ->
            TrackItem(track = track, onClick = { onTrackClick(track.id) })
        }
    }
}

@Composable
private fun CardRows(
    count: Int,
    columns: Int,
    card: @Composable (index: Int, modifier: Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (0 until count).chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { index -> card(index, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
