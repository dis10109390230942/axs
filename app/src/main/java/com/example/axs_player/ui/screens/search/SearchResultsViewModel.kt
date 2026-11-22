package com.example.axs_player.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.search.LibrarySearch
import com.example.axs_player.data.search.SearchCategory
import com.example.axs_player.data.search.SearchHistoryStore
import com.example.axs_player.data.search.SearchResults
import com.example.axs_player.navigation.SEARCH_CATEGORY_ARGUMENT
import com.example.axs_player.navigation.SEARCH_QUERY_ARGUMENT
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SearchResultsViewModel(
    savedStateHandle: SavedStateHandle,
    search: LibrarySearch,
    private val history: SearchHistoryStore,
    images: ImageStore,
    playback: PlaybackController,
    private val libraryPlayback: LibraryPlayback
) : ViewModel() {

    val category: SearchCategory =
        SearchCategory.fromPath(savedStateHandle[SEARCH_CATEGORY_ARGUMENT]) ?: SearchCategory.TRACKS
    private val query: String = savedStateHandle[SEARCH_QUERY_ARGUMENT] ?: ""

    private val results: StateFlow<SearchResults> = search.index
        .filterNotNull()
        .map { it.search(query) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults.EMPTY)

    val content: StateFlow<SearchResultsData> = combine(
        results,
        images.artistPhotos,
        playback.currentTrackId
    ) { current, photos, trackId -> current.toData(images, photos, trackId) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResultsData.EMPTY)

    fun play(trackId: String) {
        playSearchTracks(trackId, results.value.tracks, libraryPlayback, history)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                SearchResultsViewModel(
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
