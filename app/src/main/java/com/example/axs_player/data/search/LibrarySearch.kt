package com.example.axs_player.data.search

import com.example.axs_player.data.db.LibraryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

private const val TYPING_DEBOUNCE_MS = 200L
private const val INDEX_KEEP_ALIVE_MS = 60_000L

class LibrarySearch(dao: LibraryDao, scope: CoroutineScope) {

    val index: StateFlow<SearchIndex?> = combine(
        dao.observeTracks(),
        dao.observeAlbums(),
        dao.observeArtists()
    ) { tracks, albums, artists -> SearchIndex.build(tracks, albums, artists) }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(INDEX_KEEP_ALIVE_MS), null)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun results(query: Flow<String>): Flow<SearchResults?> =
        query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { if (it.isEmpty()) 0L else TYPING_DEBOUNCE_MS }
            .combine(index.filterNotNull()) { text, searchIndex -> text to searchIndex }
            .mapLatest { (text, searchIndex) -> if (text.isEmpty()) null else searchIndex.search(text) }
            .flowOn(Dispatchers.Default)
}
