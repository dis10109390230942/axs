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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ArtistBioViewModel(savedStateHandle: SavedStateHandle, dao: LibraryDao) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[ARTIST_ID_ARGUMENT])

    val bio: StateFlow<String> = dao.observeArtist(artistId)
        .map { it?.bio.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ArtistBioViewModel(createSavedStateHandle(), application.database.libraryDao())
            }
        }
    }
}
