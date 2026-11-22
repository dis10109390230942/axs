package com.example.axs_player.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.components.AllowAutoRotateWhileVisible
import com.example.axs_player.ui.theme.PageBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.LruCache

private val videoThumbnailCache = object : LruCache<String, Bitmap>(12) {}
private const val PHOTO_PAN_SENSITIVITY = 4f

@Composable
fun AlbumMediaRow(
    media: List<ImageStore.AlbumMedia>,
    modifier: Modifier = Modifier,
    onDelete: ((ImageStore.AlbumMedia) -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    viewerBackground: Color = PageBackground
) {
    if (media.isEmpty() && onAdd == null) return
    var selected by remember { mutableStateOf<ImageStore.AlbumMedia?>(null) }
    var deleteTarget by remember { mutableStateOf<ImageStore.AlbumMedia?>(null) }
    val resolver = LocalContext.current.contentResolver
    LaunchedEffect(media) {
        media.filterNot { it.isVideo }.forEach { CoverCache.load(resolver, it.uri, 512) }
    }
    Column(modifier = modifier) {
        Text("more", color = TextBlack, modifier = Modifier.padding(bottom = 10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            media.forEach { item ->
                val aspectRatio by rememberMediaAspectRatio(item.uri, item.isVideo)
                val itemWidth = 180.dp * aspectRatio
                Column(
                    modifier = Modifier
                        .height(180.dp)
                        .width(itemWidth)
                ) {
                    MediaPreview(
                        item = item,
                        modifier = Modifier
                            .height(180.dp)
                            .width(itemWidth)
                            .combinedClickable(
                                onClick = { selected = item },
                                onLongClick = onDelete?.let { { deleteTarget = item } },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    )
                }
            }
            onAdd?.let {
                Box(
                    modifier = Modifier
                        .size(width = 180.dp, height = 180.dp)
                        .background(BackgroundGray)
                        .clickable(onClick = it, indication = null, interactionSource = remember { MutableInteractionSource() }),
                    contentAlignment = Alignment.Center
                ) { Text("+", color = TextBlack) }
            }
        }
    }
    selected?.let { item ->
        AlbumMediaViewer(item = item, backgroundColor = viewerBackground, onDismiss = { selected = null })
    }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("delete material?") },
            text = { Text(item.fileName) },
            confirmButton = { TextButton(onClick = { deleteTarget = null; onDelete?.invoke(item) }) { Text("delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("cancel") } }
        )
    }
}

@Composable
private fun MediaPreview(item: ImageStore.AlbumMedia, modifier: Modifier = Modifier) {
    if (!item.isVideo) {
        CachedMediaImage(item.uri, modifier)
    } else {
        VideoThumbnail(item.uri, modifier)
    }
}

@Composable
private fun CachedMediaImage(uri: Uri, modifier: Modifier) {
    val resolver = LocalContext.current.contentResolver
    var bitmap by remember(uri) { mutableStateOf(CoverCache.get(uri, 512)) }
    LaunchedEffect(uri) {
        if (bitmap == null) bitmap = CoverCache.load(resolver, uri, 512)
    }
    Box(modifier = modifier.background(BackgroundGray), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun rememberMediaAspectRatio(uri: Uri, isVideo: Boolean) = run {
    val context = LocalContext.current
    produceState(1f, uri, isVideo) {
        value = withContext(Dispatchers.IO) {
        runCatching {
            if (isVideo) {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 1f
                    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 1f
                    val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    if (rotation == 90 || rotation == 270) height / width else width / height
                }
            } else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                options.outWidth.toFloat() / options.outHeight.toFloat().coerceAtLeast(1f)
            }
        }.getOrDefault(1f).coerceIn(0.35f, 3.5f)
        }
    }
}

@Composable
private fun VideoThumbnail(uri: Uri, modifier: Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf(videoThumbnailCache.get(uri.toString())) }
    LaunchedEffect(uri) {
        bitmap = bitmap ?: withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            }.getOrNull()?.also { videoThumbnailCache.put(uri.toString(), it) }
        }
    }
    Box(modifier = modifier.background(BackgroundGray), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Text("▶", color = TextBlack, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun AlbumMediaViewer(item: ImageStore.AlbumMedia, backgroundColor: Color, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AllowAutoRotateWhileVisible()
        Box(Modifier.fillMaxSize().background(backgroundColor)) {
            if (item.isVideo) {
                AndroidView(
                    factory = { context -> VideoView(context).apply { setVideoURI(item.uri); setOnPreparedListener { it.isLooping = true; start() } } },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                var viewport by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
                val context = LocalContext.current
                val bitmap by produceState<Bitmap?>(null, item.uri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, item.uri))
                        }.getOrNull()
                    }
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .onSizeChanged { viewport = it }
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val previousScale = scale
                                    val nextScale = (scale * zoom).coerceIn(1f, 5f)
                                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                                    val focalShift = (centroid - center) * (1f - nextScale / previousScale)
                                    val maxX = viewport.width * (nextScale - 1f) / 2f
                                    val maxY = viewport.height * (nextScale - 1f) / 2f
                                    scale = nextScale
                                    offset = if (nextScale == 1f) Offset.Zero else Offset(
                                        (offset.x + pan.x * PHOTO_PAN_SENSITIVITY + focalShift.x).coerceIn(-maxX, maxX),
                                        (offset.y + pan.y * PHOTO_PAN_SENSITIVITY + focalShift.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = if (scale > 1f) 1f else 2.5f
                                        offset = Offset.Zero
                                    }
                                )
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 8.dp)
                    .size(56.dp)
                    .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() }),
                contentAlignment = Alignment.Center
            ) { Text("×", color = TextBlack, fontSize = 32.sp) }
        }
    }
}
