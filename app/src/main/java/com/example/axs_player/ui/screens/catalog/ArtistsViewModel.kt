package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.ui.components.ArtistItemData
import com.example.axs_player.ui.screens.toArtistItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class ArtistsViewModel(dao: LibraryDao, images: ImageStore) : ViewModel() {

    val artists: StateFlow<List<ArtistItemData>> = combine(dao.observeArtists(), images.artistPhotos) { artists, photos ->
        artists.map { it.toArtistItemData(photos) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ArtistsViewModel(application.database.libraryDao(), application.imageStore)
            }
        }
    }
}
