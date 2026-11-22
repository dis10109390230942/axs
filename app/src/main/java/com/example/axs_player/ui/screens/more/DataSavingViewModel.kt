package com.example.axs_player.ui.screens.more

import android.app.Application
import android.net.Uri
import java.io.File
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.library.ImportState
import com.example.axs_player.data.library.LibraryFolder
import com.example.axs_player.data.library.LibraryImporter
import com.example.axs_player.data.search.SearchHistoryStore
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.ui.components.CoverCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageUsage(
    val otherBytes: Long = 0L,
    val dataBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val freeBytes: Long = 0L
) {
    val totalBytes: Long get() = otherBytes + dataBytes + cacheBytes + freeBytes
    val otherFraction: Float get() = fraction(otherBytes)
    val dataFraction: Float get() = fraction(dataBytes)
    val cacheFraction: Float get() = fraction(cacheBytes)

    private fun fraction(bytes: Long): Float =
        if (totalBytes > 0L) bytes.toFloat() / totalBytes else 0f
}

class DataSavingViewModel(
    private val application: Application,
    private val folder: LibraryFolder,
    private val images: ImageStore,
    private val importer: LibraryImporter,
    private val playback: PlaybackController,
    private val history: SearchHistoryStore
) : ViewModel() {

    private val _usage = MutableStateFlow(StorageUsage())
    val usage: StateFlow<StorageUsage> = _usage.asStateFlow()
    val treeUri: StateFlow<Uri?> = folder.treeUri
    val importState: StateFlow<ImportState> = importer.state

    init {
        refresh()
        viewModelScope.launch {
            importer.state.collect { if (it is ImportState.Finished) load() }
        }
    }

    fun selectFolder(tree: Uri) {
        val app = application as AxsApplication
        app.portableLibrary.cancelPendingExport()
        folder.select(tree)
        viewModelScope.launch(Dispatchers.IO) {
            if (!app.portableLibrary.restore()) {
                app.database.clearAllTables()
                app.imageStore.clear()
            }
            importer.start()
        }
    }

    fun rescan() {
        importer.start()
    }

    fun removeAllData() {
        viewModelScope.launch {
            playback.setQueue(emptyList())
            history.clear()
            importer.deleteAll().join()
            CoverCache.clear()
            load()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                application.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            CoverCache.clear()
            load()
        }
    }

    private fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _usage.value = withContext(Dispatchers.IO) { read() }
    }

    private fun read(): StorageUsage {
        val volume = application.getExternalFilesDir(null) ?: application.filesDir
        val stats = StatFs(volume.path)
        val total = stats.totalBytes
        val free = stats.availableBytes
        val data = folder.totalSize() + images.totalSize()
        val cache = directorySize(application.cacheDir)
        return StorageUsage(
            otherBytes = (total - free - data - cache).coerceAtLeast(0L),
            dataBytes = data,
            cacheBytes = cache,
            freeBytes = free
        )
    }

    private fun directorySize(file: File): Long =
        if (file.isDirectory) file.listFiles()?.sumOf(::directorySize) ?: 0L else file.length()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                DataSavingViewModel(
                    application,
                    application.libraryFolder,
                    application.imageStore,
                    application.libraryImporter,
                    application.playbackController,
                    application.searchHistory
                )
            }
        }
    }
}
