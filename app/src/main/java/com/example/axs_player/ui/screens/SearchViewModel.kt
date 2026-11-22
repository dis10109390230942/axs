package com.example.axs_player.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.search.LibrarySearch
import com.example.axs_player.data.search.SearchHistoryStore
import com.example.axs_player.data.search.SearchResults
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.TrackItemData
import com.example.axs_player.ui.screens.search.SearchResultsData
import com.example.axs_player.ui.screens.search.playSearchTracks
import com.example.axs_player.ui.screens.search.toData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

private const val KEY_QUERY = "query"
internal const val SEARCH_SECTION_LIMIT = 6

class SearchViewModel(
    private val savedStateHandle: SavedStateHandle,
    search: LibrarySearch,
    private val history: SearchHistoryStore,
    images: ImageStore,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    private val results: StateFlow<SearchResults?> = search.results(query)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val recentTracks: StateFlow<List<TrackListItem>> =
        combine(history.trackIds, search.index.filterNotNull()) { ids, index -> index.tracksById(ids) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val content: StateFlow<SearchResultsData?> = combine(
        results,
        images.artistPhotos,
        playback.currentTrackId
    ) { current, photos, trackId ->
        current?.toData(images, photos, trackId, SEARCH_SECTION_LIMIT + 1)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recents: StateFlow<List<TrackItemData>> = combine(recentTracks, playback.currentTrackId) { tracks, trackId ->
        tracks.map { it.toTrackItemData(images, trackId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun playRecent(trackId: String) {
        playSearchTracks(trackId, recentTracks.value, libraryPlayback, history)
    }

    fun playResult(trackId: String) {
        playSearchTracks(trackId, results.value?.tracks.orEmpty(), libraryPlayback, history)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                SearchViewModel(
                    createSavedStateHandle(),
                    application.librarySearch,
                    application.searchHistory,
                    application.imageStore,
                    application.playbackController,
                    application.libraryPlayback
                )
            }
        }
    }
}
