package com.example.axs_player.ui.screens.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.R
import com.example.axs_player.ui.components.CoverImage
import com.example.axs_player.ui.components.AlbumMediaRow
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.components.TextInput
import com.example.axs_player.ui.components.coverShadow
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.theme.TextPlaceholder

private val PHOTO_SIZE = 180.dp
private val BIO_HEIGHT = 124.dp
private val BORDER_WIDTH = 2.dp
private val FIELD_GAP = 10.dp
private val GROUP_GAP = 30.dp
private const val DATE_PLACEHOLDER = "dd.mm.yyyy"
private const val EMPTY_PLACEHOLDER = "-"
private const val BIO_MAX_LINES = 4

@Composable
fun ArtistEditScreen(
    onEditBio: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistEditViewModel = viewModel(factory = ArtistEditViewModel.Factory)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val photoLink by viewModel.photoLink.collectAsStateWithLifecycle()
    var photoToDelete by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { source ->
        source?.let(viewModel::addPhoto)
    }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
        source?.let(viewModel::addMedia)
    }

    LaunchedEffect(isSaved) {
        if (isSaved) onSaved()
    }

    form?.let { current ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PageBackground)
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
        ) {
            SectionTitle(text = "artist edit")
            Spacer(modifier = Modifier.height(GROUP_GAP))
            SectionTitle(text = "upload photos")
            Spacer(modifier = Modifier.height(FIELD_GAP))
            PhotoRow(
                photos = photos,
                onPhotoClick = { photoToDelete = it },
                onAdd = { photoPicker.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(FIELD_GAP))
            PrimaryButton(text = "upload by link", onClick = viewModel::openPhotoLink)
            Spacer(modifier = Modifier.height(GROUP_GAP))
            AlbumMediaRow(
                media = media,
                onDelete = viewModel::deleteMedia,
                onAdd = { mediaPicker.launch(arrayOf("image/*", "video/*")) }
            )
            Spacer(modifier = Modifier.height(GROUP_GAP))
            Field(label = "artist name", value = current.name, onValueChange = viewModel::onNameChange)
            Spacer(modifier = Modifier.height(FIELD_GAP))
            Field(label = "full name", value = current.fullName, onValueChange = viewModel::onFullNameChange)
            Spacer(modifier = Modifier.height(GROUP_GAP))
            Field(
                label = "date of birth",
                value = current.birthDate,
                onValueChange = viewModel::onBirthDateChange,
                placeholder = DATE_PLACEHOLDER
            )
            Spacer(modifier = Modifier.height(FIELD_GAP))
            Field(
                label = "date of death",
                value = current.deathDate,
                onValueChange = viewModel::onDeathDateChange,
                placeholder = DATE_PLACEHOLDER
            )
            Spacer(modifier = Modifier.height(FIELD_GAP))
            Field(label = "place of birth", value = current.birthPlace, onValueChange = viewModel::onBirthPlaceChange)
            Spacer(modifier = Modifier.height(GROUP_GAP))
            Field(label = "countries", value = current.countries, onValueChange = viewModel::onCountriesChange)
            Spacer(modifier = Modifier.height(GROUP_GAP))
            BioField(text = bio, onClick = onEditBio)
            Spacer(modifier = Modifier.height(GROUP_GAP))
            PrimaryButton(text = "save", onClick = viewModel::save)
        }
    }

    photoLink?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::closePhotoLink,
            title = { Text("upload photo by link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
                    TextInput(value = dialog.link, onValueChange = viewModel::onPhotoLinkChange, placeholder = "https://")
                    when {
                        dialog.isLoading -> Text("loading...")
                        dialog.isFailed -> Text("could not load the image")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::uploadPhotoLink,
                    enabled = !dialog.isLoading && dialog.link.isNotBlank()
                ) {
                    Text("upload")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closePhotoLink) {
                    Text("cancel")
                }
            }
        )
    }

    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("delete photo?") },
            text = { Text("The photo will be deleted from the device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoToDelete = null
                        viewModel.deletePhoto(photo)
                    }
                ) {
                    Text("delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("cancel")
                }
            }
        )
    }
}

@Composable
private fun PhotoRow(photos: List<Uri>, onPhotoClick: (Uri) -> Unit, onAdd: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
        items(photos, key = { it.toString() }) { photo ->
            CoverImage(
                uri = photo,
                modifier = Modifier
                    .size(PHOTO_SIZE)
                    .border(BORDER_WIDTH, BackgroundGray)
                    .clickable(
                        onClick = { onPhotoClick(photo) },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            )
        }
        item {
            AddPhotoBox(onClick = onAdd)
        }
    }
}

@Composable
private fun AddPhotoBox(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(PHOTO_SIZE)
            .coverShadow()
            .border(BORDER_WIDTH, BackgroundGray)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = TextBlack,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = EMPTY_PLACEHOLDER
) {
    Column(verticalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
        SectionTitle(text = label)
        TextInput(value = value, onValueChange = onValueChange, placeholder = placeholder)
    }
}

@Composable
private fun BioField(text: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
        SectionTitle(text = "bio")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BIO_HEIGHT)
                .border(BORDER_WIDTH, BackgroundGray)
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(16.dp)
        ) {
            Text(
                text = text.ifBlank { EMPTY_PLACEHOLDER },
                color = if (text.isBlank()) TextPlaceholder else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = BIO_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
