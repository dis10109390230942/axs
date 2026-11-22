package com.example.axs_player.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.ui.components.PlaylistItemData
import com.example.axs_player.ui.screens.toPlaylistItemData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(private val dao: LibraryDao, images: ImageStore) : ViewModel() {

    val playlists: StateFlow<List<PlaylistItemData>> = dao.observePlaylists()
        .map { playlists -> playlists.map { it.toPlaylistItemData(images) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(playlistId: String) {
        viewModelScope.launch { dao.deletePlaylist(playlistId) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                PlaylistsViewModel(application.database.libraryDao(), application.imageStore)
            }
        }
    }
}
