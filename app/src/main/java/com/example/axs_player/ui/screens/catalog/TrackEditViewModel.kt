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

data class TrackEditForm(val title: String, val subtitle: String, val trackDate: String, val description: String, val year: String, val lyrics: String, val isLive: Boolean)

class TrackEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: LibraryDao,
    private val containerSync: AxsContainerSync? = null
) : ViewModel() {
    private val trackId: String = checkNotNull(savedStateHandle[TRACK_ID_ARGUMENT])
    private val track = dao.observeTrack(trackId)
    val form: StateFlow<TrackEditForm?> = track.map { value ->
        value?.let { TrackEditForm(it.title, it.subtitle.orEmpty(), it.trackDate.orEmpty(), it.description.orEmpty(), it.year?.toString().orEmpty(), it.lyrics.orEmpty(), it.parentTrackId != null) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun save(title: String, subtitle: String, trackDate: String, description: String, year: String) {
        viewModelScope.launch {
            val current = track.first() ?: return@launch
            val parsedYear = year.trim().toIntOrNull()
            dao.updateTrackMetadata(trackId, title.trim().ifEmpty { current.title }, subtitle.trim().ifEmpty { null }, trackDate.trim().ifEmpty { null }, description.trim().ifEmpty { null }, if (current.parentTrackId != null) parsedYear else current.year, current.lyrics)
            containerSync?.saveBackTrackAlbum(trackId)
            _isSaved.value = true
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                TrackEditViewModel(createSavedStateHandle(), app.database.libraryDao(), app.containerSync)
            }
        }
    }
}
