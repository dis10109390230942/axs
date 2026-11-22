package com.example.axs_player.ui.screens.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.ImageDecoder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
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
import com.example.axs_player.ui.components.MarkdownEditor
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.components.rememberCoverColors
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.theme.TextWhite

@Composable
fun AlbumEditScreen(onSaved: () -> Unit, onEditDescription: () -> Unit, onEditTexture: () -> Unit, modifier: Modifier = Modifier, viewModel: AlbumEditViewModel = viewModel(factory = AlbumEditViewModel.Factory)) {
    val album by viewModel.current.collectAsStateWithLifecycle()
    val coverUri by viewModel.coverUri.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    var description by remember(album?.id, album?.description) { mutableStateOf(album?.description.orEmpty()) }
    var photoToDelete by remember { mutableStateOf<Uri?>(null) }
    var isDeleteAlbumDialogVisible by remember { mutableStateOf(false) }
    var deleteAlbumError by remember { mutableStateOf(false) }
    var isPacking by remember { mutableStateOf(false) }
    var packError by remember { mutableStateOf<String?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::addPhoto) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::addMedia) }
    LaunchedEffect(isSaved) { if (isSaved) onSaved() }
    album?.let { current ->
        Column(modifier.fillMaxSize().background(PageBackground).verticalScroll(rememberScrollState()).padding(10.dp)) {
            SectionTitle("album edit")
            Spacer(Modifier.height(30.dp))
            SectionTitle("upload covers")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
                items(photos, key = { it.toString() }) { photo ->
                    val isPrimary = photos.firstOrNull() == photo
                    CoverImage(
                        photo,
                        Modifier
                            .size(180.dp)
                            .border(2.dp, BackgroundGray)
                            .then(if (isPrimary) Modifier else Modifier.clickable(onClick = { photoToDelete = photo }, indication = null, interactionSource = remember { MutableInteractionSource() }))
                    )
                }
                item { AddCoverBox { coverPicker.launch("image/*") } }
            }
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onEditTexture, modifier = Modifier.fillMaxWidth()) { Text("background texture settings", color = TextBlack) }
            Spacer(Modifier.height(20.dp))
            SectionTitle("text color")
            Spacer(Modifier.height(10.dp))
            TextColorModeSelector(currentMode = album?.textColorMode ?: "black", onChange = viewModel::setTextColorMode)
            Spacer(Modifier.height(20.dp))
            SectionTitle("description")
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .border(2.dp, BackgroundGray)
                    .clickable(onClick = onEditDescription, indication = null, interactionSource = remember { MutableInteractionSource() })
                    .padding(16.dp)
            ) {
                Text(description.ifBlank { "-" }, color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(20.dp))
            AlbumMediaRow(
                media = media,
                onDelete = viewModel::deleteMedia,
                onAdd = { mediaPicker.launch(arrayOf("image/*", "video/*")) }
            )
            Spacer(Modifier.height(20.dp))
            if (!current.isContainerOwned) {
                SectionTitle("pack to .axs container")
                Spacer(Modifier.height(10.dp))
                Text("Create a portable .axs container from this album. All files will be moved into the container and removed from the library folders.", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    text = if (isPacking) "packing..." else "pack to .axs container",
                    onClick = {
                        isPacking = true
                        packError = null
                        viewModel.packToContainer { containerName ->
                            isPacking = false
                            if (containerName != null) {
                                onSaved()
                            } else {
                                packError = "Failed to create container"
                            }
                        }
                    },
                    enabled = !isPacking
                )
                packError?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = androidx.compose.ui.graphics.Color.Red, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("save", onClick = { viewModel.save(description) })
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { isDeleteAlbumDialogVisible = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("delete album", color = androidx.compose.ui.graphics.Color.Red)
            }
        }
    }
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("delete cover?") },
            text = { Text("The cover will be deleted from the device.") },
            confirmButton = {
                TextButton(onClick = { photoToDelete = null; viewModel.deletePhoto(photo) }) { Text("delete") }
            },
            dismissButton = { TextButton(onClick = { photoToDelete = null }) { Text("cancel") } }
        )
    }
    if (isDeleteAlbumDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteAlbumDialogVisible = false },
            title = { Text("delete album?") },
            text = { Text("All track files in this album will be deleted from the device, along with the album and its tracks from the library.") },
            confirmButton = {
                TextButton(onClick = {
                    isDeleteAlbumDialogVisible = false
                    viewModel.deleteAlbum(onComplete = onSaved, onFailure = { deleteAlbumError = true })
                }) { Text("delete") }
            },
            dismissButton = { TextButton(onClick = { isDeleteAlbumDialogVisible = false }) { Text("cancel") } }
        )
    }
    if (deleteAlbumError) {
        AlertDialog(
            onDismissRequest = { deleteAlbumError = false },
            title = { Text("could not delete album") },
            text = { Text("At least one track file could not be deleted. The album was kept in the library.") },
            confirmButton = { TextButton(onClick = { deleteAlbumError = false }) { Text("ok") } }
        )
    }
}

@Composable
fun AlbumTextureEditScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: AlbumEditViewModel) {
    val textureUri by viewModel.textureUri.collectAsStateWithLifecycle()
    val textureScale by viewModel.textureScale.collectAsStateWithLifecycle()
    val textureOffset by viewModel.textureOffset.collectAsStateWithLifecycle()
    val textureOpacity by viewModel.textureOpacity.collectAsStateWithLifecycle()
    val coverUri by viewModel.coverUri.collectAsStateWithLifecycle()
    val coverColors = rememberCoverColors(coverUri)
    val texturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::addTexture) }
    Column(modifier.fillMaxSize().background(PageBackground).verticalScroll(rememberScrollState()).padding(10.dp)) {
        SectionTitle("background texture")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            if (textureUri != null) TextButton(onClick = viewModel::clearTexture) { Text("remove") }
            TextButton(onClick = { texturePicker.launch(arrayOf("image/*")) }) { Text(if (textureUri == null) "add texture" else "replace texture") }
        }
        TexturePreview(textureUri, textureScale, textureOffset.x, textureOffset.y, textureOpacity, coverColors.background, viewModel::setTextureOffset, viewModel::setTextureScale)
        Spacer(Modifier.height(12.dp))
        Text("opacity", color = TextBlack)
        Slider(value = textureOpacity, onValueChange = viewModel::setTextureOpacity, valueRange = 0f..1f)
        Text("${(textureOpacity * 100).toInt()}%", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("back", onClick = onBack)
    }
}

@Composable
private fun TexturePreview(uri: Uri?, scale: Float, offsetX: Float, offsetY: Float, opacity: Float, backgroundColor: androidx.compose.ui.graphics.Color, onDrag: (androidx.compose.ui.geometry.Offset) -> Unit, onScale: (Float) -> Unit) {
    val context = LocalContext.current
    val currentScale by rememberUpdatedState(scale)
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val sourceBitmap = remember(uri) {
        uri?.let { runCatching { ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE } }.getOrNull()?.asImageBitmap() }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(300.dp).background(backgroundColor).clipToBounds().pointerInput(uri) {
        detectTransformGestures { _, pan, zoom, _ ->
            onDrag(androidx.compose.ui.geometry.Offset(currentOffsetX + pan.x / size.width, currentOffsetY + pan.y / size.height))
            onScale(currentScale * zoom)
        }
    }, contentAlignment = Alignment.Center) {
        if (sourceBitmap == null) Text("album background", color = TextMuted)
        sourceBitmap?.let { image ->
            Image(image, contentDescription = "", modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX * constraints.maxWidth, translationY = offsetY * constraints.maxHeight), contentScale = ContentScale.Crop, alpha = opacity)
        }
    }
}

@Composable
private fun AddCoverBox(onClick: () -> Unit) {
    Text("+", fontSize = 28.sp, color = TextBlack, modifier = Modifier.size(180.dp).border(2.dp, BackgroundGray).clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() }).padding(70.dp))
}

@Composable
private fun TextColorModeSelector(currentMode: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("black" to "Black", "white" to "White")
    Box(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(options.first { it.first == currentMode }.second, color = TextBlack)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (mode, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = if (mode == currentMode) TextBlack else TextMuted) },
                    onClick = { onChange(mode); expanded = false }
                )
            }
        }
    }
}
