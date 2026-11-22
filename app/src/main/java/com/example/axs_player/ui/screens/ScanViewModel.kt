package com.example.axs_player.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.library.ImportState
import com.example.axs_player.data.library.LibraryImporter
import kotlinx.coroutines.flow.StateFlow

class ScanViewModel(importer: LibraryImporter) : ViewModel() {

    val importState: StateFlow<ImportState> = importer.state

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ScanViewModel(application.libraryImporter)
            }
        }
    }
}
