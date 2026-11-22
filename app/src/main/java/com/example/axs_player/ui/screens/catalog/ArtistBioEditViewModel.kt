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
import com.example.axs_player.navigation.ARTIST_ID_ARGUMENT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ArtistBioEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: LibraryDao
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[ARTIST_ID_ARGUMENT])

    private val _bio = MutableStateFlow<String?>(null)
    val bio: StateFlow<String?> = _bio.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        viewModelScope.launch {
            _bio.value = dao.observeArtist(artistId).first()?.bio.orEmpty()
        }
    }

    fun onBioChange(value: String) {
        _bio.value = value
    }

    fun save() {
        val value = _bio.value ?: return
        viewModelScope.launch {
            val current = dao.observeArtist(artistId).first() ?: return@launch
            val updated = current.copy(bio = value.trim().ifEmpty { null })
            if (updated != current) dao.updateArtist(updated)
            _isSaved.value = true
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ArtistBioEditViewModel(createSavedStateHandle(), application.database.libraryDao())
            }
        }
    }
}
