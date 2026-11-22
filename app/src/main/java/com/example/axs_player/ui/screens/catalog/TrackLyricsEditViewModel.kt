package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.library.AxsContainerSync
import com.example.axs_player.navigation.TRACK_ID_ARGUMENT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackLyricsEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: LibraryDao,
    private val containerSync: AxsContainerSync? = null
) : ViewModel() {
    private val trackId: String = checkNotNull(savedStateHandle[TRACK_ID_ARGUMENT])
    private val track = dao.observeTrack(trackId)
    val title: StateFlow<String> = track.map { it?.title.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics.asStateFlow()
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        viewModelScope.launch { _lyrics.value = track.first()?.lyrics.orEmpty() }
    }

    fun onLyricsChange(value: String) { _lyrics.value = value }

    fun save() {
        val value = _lyrics.value ?: return
        viewModelScope.launch {
            val current = track.first() ?: return@launch
            dao.updateTrackMetadata(current.id, current.title, current.subtitle, current.trackDate, current.description, current.year, value.trim().ifEmpty { null })
            containerSync?.saveBackTrackAlbum(current.id)
            _isSaved.value = true
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                TrackLyricsEditViewModel(createSavedStateHandle(), app.database.libraryDao(), app.containerSync)
            }
        }
    }
}
