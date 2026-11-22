package com.example.axs_player.ui.screens.catalog

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import android.graphics.ImageDecoder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.data.db.AlbumDetails
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.components.TrackActionsBottomSheet
import com.example.axs_player.ui.components.MarkdownText
import com.example.axs_player.ui.components.AlbumMediaRow
import com.example.axs_player.ui.components.CoverImage
import com.example.axs_player.ui.components.rememberCoverColors
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.OldStandardTT
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.theme.TextWhite

private val COVER_SIZE = 300.dp
private val COVER_BORDER_WIDTH = 2.dp
private const val RELEASE_TYPE = "album"

@Composable
fun AlbumScreen(
    onEdit: () -> Unit,
    onOpenDescription: () -> Unit,
    onEditTrack: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.Factory)
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val coverUri by viewModel.coverUri.collectAsStateWithLifecycle()
    val textureUri by viewModel.textureUri.collectAsStateWithLifecycle()
    val coverUris by viewModel.coverUris.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val textColorMode by viewModel.textColorMode.collectAsStateWithLifecycle()
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    val coverColors = rememberCoverColors(coverUri)
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val textColor = if (textColorMode == "white") TextWhite else TextBlack
    val metaColor = textColor.copy(alpha = 0.7f)

    Box(Modifier.fillMaxSize().background(coverColors.background)) {
        val context = LocalContext.current
        val textureBitmap = remember(textureUri) {
            textureUri?.let { runCatching { ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE } }.getOrNull()?.asImageBitmap() }
        }
        textureBitmap?.let { bitmap ->
            val details = album
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val scale = details?.backgroundTextureScale ?: 1f
                Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = (details?.backgroundTextureOffsetX ?: 0f) * constraints.maxWidth,
                    translationY = (details?.backgroundTextureOffsetY ?: 0f) * constraints.maxHeight
                ), contentScale = ContentScale.Crop,
                    alpha = details?.backgroundTextureOpacity ?: 1f)
            }
        }
        LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 10.dp, top = statusBarHeight + 60.dp, end = 10.dp, bottom = 20.dp)
    ) {
        album?.let { details ->
            item {
                AlbumHeader(album = details, coverUri = coverUri, coverUris = coverUris, onEdit = onEdit, onOpenDescription = onOpenDescription, linkColor = coverColors.highlight, textColor = textColor, metaColor = metaColor)
            }
        }
        items(items, key = { it.id }) { track ->
            TrackItem(
                track = track,
                onClick = { viewModel.play(track.id) },
                onMore = { selectedTrackId = track.id },
                highlightColor = coverColors.highlight,
                textColor = textColor
            )
        }
        if (tracks.isNotEmpty()) {
            item {
                ReleaseSummary(tracks.size, tracks.sumOf { it.track.durationMs ?: 0L })
            }
        }
        if (media.isNotEmpty()) {
            item {
                Spacer(Modifier.height(30.dp))
                AlbumMediaRow(media = media, viewerBackground = coverColors.background)
            }
        }
    }
        if (selectedTrackId != null) {
            TrackActionsBottomSheet(selectedTrackId!!, onEdit = { onEditTrack(selectedTrackId!!); selectedTrackId = null }, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onDismiss = { selectedTrackId = null })
        }
    }
}

@Composable
private fun AlbumHeader(album: AlbumDetails, coverUri: Uri?, coverUris: List<Uri>, onEdit: () -> Unit, onOpenDescription: () -> Unit, linkColor: androidx.compose.ui.graphics.Color, textColor: Color, metaColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        album.artists?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = metaColor,
                fontSize = RELEASE_META_FONT_SIZE,
                fontFamily = OldStandardTT,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReleaseMeta(text = RELEASE_TYPE, color = metaColor)
            if (album.isContainerOwned) {
                ReleaseMeta(text = "•", color = metaColor)
                ReleaseMeta(text = "AXS", color = metaColor)
            }
            album.year?.let {
                ReleaseMeta(text = "•", color = metaColor)
                ReleaseMeta(text = it.toString(), color = metaColor)
            }
        }
        Spacer(modifier = Modifier.height(11.dp))
        val covers = coverUris.ifEmpty { listOfNotNull(coverUri) }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(COVER_SIZE)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = (maxWidth - COVER_SIZE) / 2),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(covers, key = { it.toString() }) { cover ->
                    CoverImage(
                        uri = cover,
                        modifier = Modifier
                            .size(COVER_SIZE)
                            .border(COVER_BORDER_WIDTH, BackgroundGray)
                            .combinedClickable(onLongClick = onEdit, onClick = {}, indication = null, interactionSource = remember { MutableInteractionSource() })
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Column(
            modifier = Modifier.width(COVER_SIZE),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReleaseTitle(title = album.title, color = textColor)
            album.description?.takeIf { it.isNotBlank() }?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = onOpenDescription,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                ) {
                    MarkdownText(
                        text = it,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        linkColor = linkColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextClick = onOpenDescription,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}