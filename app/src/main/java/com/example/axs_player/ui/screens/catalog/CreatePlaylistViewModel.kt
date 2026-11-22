package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDatabase
import com.example.axs_player.data.db.PlaylistEntity
import com.example.axs_player.data.db.PlaylistItemEntity
import com.example.axs_player.data.db.TrackListItem
import com.example.axs_player.data.search.LibrarySearch
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val NAME_MAX_LENGTH = 100
private const val TYPING_DEBOUNCE_MS = 200L

enum class CreatePlaylistStep { NAME, TRACKS }

class CreatePlaylistViewModel(
    private val database: LibraryDatabase,
    search: LibrarySearch
) : ViewModel() {

    private val dao = database.libraryDao()

    private val _step = MutableStateFlow(CreatePlaylistStep.NAME)
    val step: StateFlow<CreatePlaylistStep> = _step.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selected = MutableStateFlow<List<String>>(emptyList())
    val selected: StateFlow<List<String>> = _selected.asStateFlow()

    @OptIn(FlowPreview::class)
    val tracks: StateFlow<List<TrackListItem>> = combine(
        _query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { if (it.isEmpty()) 0L else TYPING_DEBOUNCE_MS },
        search.index.filterNotNull()
    ) { text, index -> if (text.isEmpty()) index.allTracks() else index.search(text).tracks }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onNameChange(value: String) {
        _name.value = value.take(NAME_MAX_LENGTH)
    }

    fun openTracks() {
        if (_name.value.isNotBlank()) _step.value = CreatePlaylistStep.TRACKS
    }

    fun backToName() {
        _step.value = CreatePlaylistStep.NAME
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggle(trackId: String) {
        _selected.update { if (trackId in it) it - trackId else it + trackId }
    }

    fun create(onCreated: () -> Unit) {
        val playlistName = _name.value.trim()
        if (playlistName.isEmpty()) return
        val trackIds = _selected.value
        viewModelScope.launch {
            val playlistId = UUID.randomUUID().toString()
            database.withTransaction {
                dao.insertPlaylist(PlaylistEntity(playlistId, playlistName, System.currentTimeMillis()))
                dao.insertPlaylistItems(
                    trackIds.mapIndexed { position, trackId ->
                        PlaylistItemEntity(UUID.randomUUID().toString(), playlistId, trackId, position)
                    }
                )
            }
            reset()
            onCreated()
        }
    }

    fun reset() {
        _step.value = CreatePlaylistStep.NAME
        _name.value = ""
        _query.value = ""
        _selected.value = emptyList()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                CreatePlaylistViewModel(application.database, application.librarySearch)
            }
        }
    }
}
