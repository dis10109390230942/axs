package com.example.axs_player.ui.screens.catalog

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.ArtistEntity
import com.example.axs_player.data.db.LibraryDao
import com.example.axs_player.data.library.ImageDownloader
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.navigation.ARTIST_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ArtistForm(
    val name: String,
    val fullName: String,
    val birthDate: String,
    val deathDate: String,
    val birthPlace: String,
    val countries: String
)

data class PhotoLinkDialog(
    val link: String = "",
    val isLoading: Boolean = false,
    val isFailed: Boolean = false
)

class ArtistEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: LibraryDao,
    private val images: ImageStore
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[ARTIST_ID_ARGUMENT])
    private val artist: Flow<ArtistEntity?> = dao.observeArtist(artistId)

    private val _form = MutableStateFlow<ArtistForm?>(null)
    val form: StateFlow<ArtistForm?> = _form.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _photoLink = MutableStateFlow<PhotoLinkDialog?>(null)
    val photoLink: StateFlow<PhotoLinkDialog?> = _photoLink.asStateFlow()
    private var photoLinkJob: Job? = null

    val bio: StateFlow<String> = artist
        .map { it?.bio.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val photos: StateFlow<List<Uri>> = combine(artist, images.artistPhotos) { current, photos ->
        current?.nameKey?.let { photos[it] }.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _media = MutableStateFlow(emptyList<ImageStore.AlbumMedia>())
    val media: StateFlow<List<ImageStore.AlbumMedia>> = _media.asStateFlow()

    init {
        viewModelScope.launch {
            val current = artist.first()
            _form.value = current?.toForm()
            current?.let { _media.value = images.artistMedia(it.id) }
        }
    }

    fun onNameChange(value: String) = update { it.copy(name = value) }

    fun onFullNameChange(value: String) = update { it.copy(fullName = value) }

    fun onBirthDateChange(value: String) = update { it.copy(birthDate = value) }

    fun onDeathDateChange(value: String) = update { it.copy(deathDate = value) }

    fun onBirthPlaceChange(value: String) = update { it.copy(birthPlace = value) }

    fun onCountriesChange(value: String) = update { it.copy(countries = value) }

    fun addPhoto(source: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val nameKey = artist.first()?.nameKey ?: return@launch
            images.saveArtistPhoto(nameKey, source)
        }
    }

    fun openPhotoLink() {
        _photoLink.value = PhotoLinkDialog()
    }

    fun onPhotoLinkChange(value: String) {
        _photoLink.update { it?.copy(link = value, isFailed = false) }
    }

    fun closePhotoLink() {
        photoLinkJob?.cancel()
        photoLinkJob = null
        _photoLink.value = null
    }

    fun uploadPhotoLink() {
        val dialog = _photoLink.value?.takeIf { !it.isLoading && it.link.isNotBlank() } ?: return
        _photoLink.value = dialog.copy(isLoading = true, isFailed = false)
        photoLinkJob = viewModelScope.launch {
            val nameKey = artist.first()?.nameKey
            val isAdded = nameKey != null && withContext(Dispatchers.IO) {
                val bytes = ImageDownloader.download(dialog.link) ?: return@withContext false
                ensureActive()
                images.saveArtistPhoto(nameKey, bytes)
            }
            _photoLink.update { current -> if (isAdded) null else current?.copy(isLoading = false, isFailed = true) }
        }
    }

    fun deletePhoto(photo: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            images.deleteArtistPhoto(photo)
        }
    }

    fun addMedia(source: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            images.saveArtistMedia(artistId, source)?.let { _media.value = images.artistMedia(artistId) }
        }
    }

    fun deleteMedia(media: ImageStore.AlbumMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            if (images.deleteArtistMedia(media)) _media.value = images.artistMedia(artistId)
        }
    }

    fun save() {
        val form = _form.value ?: return
        viewModelScope.launch {
            val current = artist.first() ?: return@launch
            val updated = current.copy(
                name = form.name.trim().ifEmpty { current.name },
                fullName = form.fullName.trim().ifEmpty { null },
                birthDate = form.birthDate.trim().ifEmpty { null },
                deathDate = form.deathDate.trim().ifEmpty { null },
                birthPlace = form.birthPlace.trim().ifEmpty { null },
                countries = form.countries.trim().ifEmpty { null }
            )
            if (updated != current) dao.updateArtist(updated)
            _isSaved.value = true
        }
    }

    private fun update(transform: (ArtistForm) -> ArtistForm) {
        _form.update { current -> current?.let(transform) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                ArtistEditViewModel(
                    createSavedStateHandle(),
                    application.database.libraryDao(),
                    application.imageStore
                )
            }
        }
    }
}

private fun ArtistEntity.toForm() = ArtistForm(
    name = name,
    fullName = fullName.orEmpty(),
    birthDate = birthDate.orEmpty(),
    deathDate = deathDate.orEmpty(),
    birthPlace = birthPlace.orEmpty(),
    countries = countries.orEmpty()
)
