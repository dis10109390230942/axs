package com.example.axs_player.ui.screens.catalog

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.AlbumDetails
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.library.AxsContainerSync
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.library.LibraryFolder
import com.example.axs_player.data.library.LibraryImporter
import com.example.axs_player.data.db.LibraryDatabase
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.navigation.ALBUM_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: LibraryDao,
    private val images: ImageStore,
    private val folder: LibraryFolder,
    private val database: LibraryDatabase,
    private val playback: PlaybackController,
    private val containerSync: com.example.axs_player.data.library.AxsContainerSync? = null,
    private val importer: LibraryImporter
) : ViewModel() {
    private val albumId: String = checkNotNull(savedStateHandle[ALBUM_ID_ARGUMENT])
    private val album = dao.observeAlbum(albumId)
    val current: StateFlow<AlbumDetails?> = album
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val coverUri: StateFlow<Uri?> = album
        .map { images.coverUri(it?.coverFileName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _photos = MutableStateFlow(images.albumPhotos(albumId))
    val photos: StateFlow<List<Uri>> = _photos.asStateFlow()
    private val _media = MutableStateFlow(images.albumMedia(albumId))
    val media: StateFlow<List<ImageStore.AlbumMedia>> = _media.asStateFlow()
    private val _textureUri = MutableStateFlow<Uri?>(null)
    val textureUri: StateFlow<Uri?> = _textureUri.asStateFlow()
    private val _textureScale = MutableStateFlow(1f)
    val textureScale: StateFlow<Float> = _textureScale.asStateFlow()
    private val _textureOffset = MutableStateFlow(Offset.Zero)
    val textureOffset: StateFlow<Offset> = _textureOffset.asStateFlow()
    private val _textureOpacity = MutableStateFlow(1f)
    val textureOpacity: StateFlow<Float> = _textureOpacity.asStateFlow()

    init {
        viewModelScope.launch {
            album.collect { current ->
                current ?: return@collect
                if (_textureUri.value == null && current.backgroundTextureFileName != null) {
                    _textureUri.value = images.albumTextureUri(current.backgroundTextureFileName)
                    _textureScale.value = current.backgroundTextureScale
                    _textureOffset.value = Offset(current.backgroundTextureOffsetX, current.backgroundTextureOffsetY)
                    _textureOpacity.value = current.backgroundTextureOpacity
                }
            }
        }
    }

    fun addTexture(source: Uri) {
        viewModelScope.launch {
            val existing = album.first() ?: return@launch
            val fileName = images.saveAlbumTexture(albumId, source) ?: return@launch
            _textureUri.value = images.albumTextureUri(fileName)
            existing.backgroundTextureFileName?.let(images::deleteAlbumTexture)
            persistTextureSettings(fileName)
            containerSync?.saveBackAlbum(albumId)
        }
    }

    fun clearTexture() {
        _textureUri.value = null
        viewModelScope.launch {
            album.first()?.backgroundTextureFileName?.let(images::deleteAlbumTexture)
            dao.updateAlbumBackgroundTexture(albumId, null, 1f, "cover", 0f, 0f, "none", -1, 32f, 1f)
            _textureScale.value = 1f
            _textureOffset.value = Offset.Zero
            _textureOpacity.value = 1f
            containerSync?.saveBackAlbum(albumId)
        }
    }

    fun setTextureScale(value: Float) {
        _textureScale.value = value.coerceIn(0.25f, 4f)
        persistTextureSettings()
        scheduleSaveBack()
    }
    fun setTextureOffset(value: Offset) {
        _textureOffset.value = Offset(value.x.coerceIn(-1f, 1f), value.y.coerceIn(-1f, 1f))
        persistTextureSettings()
        scheduleSaveBack()
    }

    fun setTextureOpacity(value: Float) {
        _textureOpacity.value = value.coerceIn(0f, 1f)
        persistTextureSettings()
        scheduleSaveBack()
    }

    private var _saveBackJob: kotlinx.coroutines.Job? = null
    private fun scheduleSaveBack() {
        _saveBackJob?.cancel()
        _saveBackJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // debounce 1 second
            containerSync?.saveBackAlbum(albumId)
        }
    }

    private fun persistTextureSettings() {
        viewModelScope.launch {
            persistTextureSettings(album.first()?.backgroundTextureFileName)
        }
    }

    private suspend fun persistTextureSettings(fileName: String?) {
        dao.updateAlbumBackgroundTexture(albumId, fileName, _textureScale.value, "cover", _textureOffset.value.x, _textureOffset.value.y, "none", -1, 32f, _textureOpacity.value)
    }
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun addPhoto(source: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = images.saveAlbumPhoto(albumId, source) ?: return@launch
            _photos.value = images.albumPhotos(albumId)
            val existing = album.first() ?: return@launch
            if (existing.coverFileName == null) {
                dao.updateAlbumCover(albumId, fileName)
            }
            containerSync?.saveBackAlbum(albumId)
        }
    }

    fun save(description: String) {
        viewModelScope.launch {
            val existing = album.first() ?: return@launch
            dao.updateAlbumDescription(albumId, description.trim().ifEmpty { null })
            persistTextureSettings(existing.backgroundTextureFileName)
            containerSync?.saveBackAlbum(albumId)
            _isSaved.value = true
        }
    }

    fun addMedia(source: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            images.saveAlbumMedia(albumId, source)?.let { _media.value = images.albumMedia(albumId) }
            containerSync?.saveBackAlbum(albumId)
        }
    }

    fun deleteMedia(media: ImageStore.AlbumMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            if (images.deleteAlbumMedia(media)) {
                _media.value = images.albumMedia(albumId)
                containerSync?.saveBackAlbum(albumId)
            }
        }
    }

    fun deletePhoto(photo: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (images.deleteAlbumPhoto(photo)) {
                _photos.value = images.albumPhotos(albumId)
                containerSync?.saveBackAlbum(albumId)
            }
        }
    }

    fun deleteAlbum(onComplete: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val layout = folder.layout() ?: return@launch
            val albumEntity = dao.albumEntity(albumId) ?: return@launch
            val containerFileName = albumEntity.containerFileName
            val isContainerOwned = albumEntity.isContainerOwned
            val textureFileName = albumEntity.backgroundTextureFileName
            val tracks = dao.tracksInAlbum(albumId)
            var allFilesDeleted = true
            tracks.forEach { track ->
                if (!folder.deleteTrack(track.fileName)) allFilesDeleted = false
            }
            if (!allFilesDeleted) {
                onFailure()
                return@launch
            }
            database.withTransaction {
                dao.deleteAlbum(albumId)
                tracks.forEach { dao.deleteTrack(it.id) }
            }
            withContext(Dispatchers.Main) { tracks.forEach { playback.removeTrack(it.id) } }
            images.deleteAlbumAssets(albumId, textureFileName)
            if (isContainerOwned && containerFileName != null) {
                folder.deleteContainer(layout, containerFileName)
            }
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    fun packToContainer(onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val containerName = importer.packAlbumToContainer(albumId)
                onResult(containerName)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                AlbumEditViewModel(
                    createSavedStateHandle(), app.database.libraryDao(), app.imageStore,
                    app.libraryFolder, app.database, app.playbackController, app.containerSync, app.libraryImporter
                )
            }
        }
    }
}
