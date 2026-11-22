package com.example.axs_player.ui.player

import android.app.Activity
import android.graphics.ImageDecoder
import android.net.Uri
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.example.axs_player.R
import com.example.axs_player.playback.PlayableTrack
import com.example.axs_player.playback.PlaybackState
import com.example.axs_player.playback.RepeatMode
import com.example.axs_player.ui.components.CoverImage
import com.example.axs_player.ui.components.AllowAutoRotateWhileVisible
import com.example.axs_player.ui.components.rememberCoverColors
import com.example.axs_player.ui.components.ProgressBar
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TextBlack

private val CONTROL_HEIGHT = 40.dp
private val PLAYBACK_CONTROLS_HEIGHT = 56.dp
private val SKIP_ICON_SIZE = 32.dp
private val PLAY_PAUSE_ICON_SIZE = 42.dp
private val MODE_ICON_SIZE = 32.dp
private const val DIMMED_ICON_ALPHA = 0.4f
private val SEEK_BAR_HEIGHT = 24.dp
private val SWIPE_THRESHOLD = 48.dp
private val COVER_BORDER_WIDTH = 2.dp
private val PAGE_VERTICAL_PADDING = 100.dp

enum class PanelDirection { LEFT, RIGHT, UP, DOWN }

private fun panelEnter(direction: PanelDirection): EnterTransition = when (direction) {
    PanelDirection.LEFT -> slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(260))
    PanelDirection.RIGHT -> slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(260))
    PanelDirection.UP -> androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(260))
    PanelDirection.DOWN -> androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + fadeIn(tween(260))
}

private fun panelExit(direction: PanelDirection): ExitTransition = when (direction) {
    PanelDirection.LEFT -> slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(180))
    PanelDirection.RIGHT -> slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(180))
    PanelDirection.UP -> androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(180))
    PanelDirection.DOWN -> androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(180))
}

@Composable
fun PlayerScreen(
    onCollapse: () -> Unit,
    onOpenArtist: (artistId: String) -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onEditTrack: (trackId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val activePlayer by viewModel.activePlayer.collectAsStateWithLifecycle()
    val trackInfo by viewModel.trackInfo.collectAsStateWithLifecycle()
    val trackVolume by viewModel.trackVolume.collectAsStateWithLifecycle()
    val track = state.currentItem?.track
    var isQueueOpen by rememberSaveable { mutableStateOf(false) }
    var isInfoOpen by rememberSaveable { mutableStateOf(false) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isLyricsOpen by rememberSaveable { mutableStateOf(false) }
    var panelDirection by remember { mutableStateOf(PanelDirection.RIGHT) }
    var isClipMode by rememberSaveable(state.currentItem?.id) { mutableStateOf(track?.hasVideo == true) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(onBack = onCollapse)
    LaunchedEffect(track == null) {
        if (track == null) onCollapse()
    }
    LaunchedEffect(track?.trackId) {
        track?.trackId?.let(viewModel::loadTrackVolume)
    }
    val coverColors = rememberCoverColors(track?.artworkUri)
    val backgroundColor = coverColors.background
    if (track == null) return
    if (track.hasVideo && isClipMode) AllowAutoRotateWhileVisible()

    if (track.hasVideo && isClipMode && isLandscape) {
        FullscreenClip(player = activePlayer, modifier = modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlayerContent(
            track = track,
            state = state,
            progress = progress,
            backgroundColor = backgroundColor,
            textureUri = trackInfo.albumTextureUri,
            textureScale = trackInfo.albumTextureScale,
            textureOffsetX = trackInfo.albumTextureOffsetX,
            textureOffsetY = trackInfo.albumTextureOffsetY,
            textureOpacity = trackInfo.albumTextureOpacity,
            highlightColor = coverColors.highlight,
            player = activePlayer,
            isClipMode = track.hasVideo && isClipMode,
            onClipModeChange = { isClipMode = it },
            viewModel = viewModel,
            onCollapse = onCollapse,
            onOpenQueue = { panelDirection = PanelDirection.RIGHT; isQueueOpen = true },
            onOpenInfo = { panelDirection = PanelDirection.DOWN; isInfoOpen = true },
            onOpenLyrics = {
                panelDirection = PanelDirection.LEFT
                isInfoOpen = false
                isQueueOpen = false
                isLyricsOpen = true
            }
        )
        AnimatedVisibility(
            visible = isLyricsOpen,
            enter = panelEnter(panelDirection),
            exit = panelExit(panelDirection)
        ) {
            LyricsPanel(
                text = trackInfo.lyrics,
                backgroundColor = backgroundColor,
                onClose = { direction -> panelDirection = direction; isLyricsOpen = false }
            )
        }
        AnimatedVisibility(
            visible = isInfoOpen,
            enter = panelEnter(panelDirection),
            exit = panelExit(panelDirection)
        ) {
            TrackInfoPanel(
                info = trackInfo,
                backgroundColor = backgroundColor,
                linkColor = coverColors.highlight,
                onClose = { direction -> panelDirection = direction; isInfoOpen = false },
                onArtistClick = { artistId ->
                    isInfoOpen = false
                    onOpenArtist(artistId)
                },
                onAlbumClick = { albumId ->
                    isInfoOpen = false
                    onOpenAlbum(albumId)
                },
                onTrackClick = { trackId ->
                    isInfoOpen = false
                    viewModel.playTrack(trackId)
                },
                onEditTrack = { trackId ->
                    isInfoOpen = false
                    onEditTrack(trackId)
                },
                onOpenSettings = { isSettingsOpen = true },
                settingsContent = {
                    AnimatedVisibility(
                        visible = isSettingsOpen,
                        enter = panelEnter(PanelDirection.DOWN),
                        exit = panelExit(PanelDirection.UP)
                    ) {
                        TrackSettingsPanel(
                            track = track,
                            volume = trackVolume,
                            backgroundColor = backgroundColor,
                            onVolumeChange = viewModel::setTrackVolume,
                            onClose = { isSettingsOpen = false },
                            onOpenInfo = { isSettingsOpen = false }
                        )
                    }
                }
            )
        }
        AnimatedVisibility(
            visible = isQueueOpen,
            enter = panelEnter(panelDirection),
            exit = panelExit(panelDirection)
        ) {
            QueuePanel(
                queue = state.queue,
                currentIndex = state.currentIndex,
                backgroundColor = backgroundColor,
                highlightColor = coverColors.highlight,
                onMove = viewModel::moveQueueItem,
                onRemove = viewModel::removeFromQueue,
                onSelect = viewModel::skipTo,
                onClose = { direction -> panelDirection = direction; isQueueOpen = false },
                onCollapsePlayer = onCollapse
            )
        }
    }
}

@Composable
private fun PlayerContent(
    track: PlayableTrack,
    state: PlaybackState,
    progress: PlaybackProgress,
    backgroundColor: Color,
    textureUri: Uri?,
    textureScale: Float,
    textureOffsetX: Float,
    textureOffsetY: Float,
    textureOpacity: Float,
    highlightColor: Color,
    player: Player,
    isClipMode: Boolean,
    onClipModeChange: (Boolean) -> Unit,
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    val currentOnCollapse by rememberUpdatedState(onCollapse)
    val currentOnOpenQueue by rememberUpdatedState(onOpenQueue)
    val currentOnOpenInfo by rememberUpdatedState(onOpenInfo)
    val currentOnOpenLyrics by rememberUpdatedState(onOpenLyrics)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(onClick = {}, indication = null, interactionSource = remember { MutableInteractionSource() })
            .pointerInput(Unit) {
                val threshold = SWIPE_THRESHOLD.toPx()
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        when {
                            dragged > threshold -> currentOnCollapse()
                            dragged < -threshold -> currentOnOpenInfo()
                        }
                    }
                ) { _, dragAmount -> dragged += dragAmount }
            }
            .pointerInput(Unit) {
                val threshold = SWIPE_THRESHOLD.toPx()
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        when {
                            dragged < -threshold -> currentOnOpenQueue()
                            dragged > threshold -> currentOnOpenLyrics()
                        }
                    }
                ) { _, dragAmount -> dragged += dragAmount }
            }
    ) {
        AlbumTextureLayer(textureUri, textureScale, textureOffsetX, textureOffsetY, textureOpacity)
        PlayerPage(
            track = track,
            state = state,
            progress = progress,
            player = player,
            highlightColor = highlightColor,
            isClipMode = isClipMode,
            onClipModeChange = onClipModeChange,
            viewModel = viewModel,
            onCollapse = onCollapse,
            modifier = Modifier.fillMaxSize()
        )
        Y2kScanlines(modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun AlbumTextureLayer(uri: Uri?, scale: Float, offsetX: Float, offsetY: Float, opacity: Float) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        uri?.let { runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        }.getOrNull() }
    }
    bitmap?.let { image ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX * constraints.maxWidth,
                    translationY = offsetY * constraints.maxHeight
                ),
                contentScale = ContentScale.Crop,
                alpha = opacity
            )
        }
    }
}

@Composable
private fun Y2kScanlines(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "y2k-scanline").animateFloat(
        initialValue = 0.035f,
        targetValue = 0.09f,
        animationSpec = infiniteRepeatable(tween(1400), AnimationRepeatMode.Reverse),
        label = "y2k-scanline-alpha"
    )
    Canvas(modifier = modifier) {
        val spacing = 5.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.White.copy(alpha = pulse),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Square
            )
            y += spacing
        }
    }
}

@Composable
private fun PlayerPage(
    track: PlayableTrack,
    state: PlaybackState,
    progress: PlaybackProgress,
    player: Player,
    highlightColor: Color,
    isClipMode: Boolean,
    onClipModeChange: (Boolean) -> Unit,
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = PAGE_VERTICAL_PADDING)
    ) {
        IconButton(
            iconRes = R.drawable.ic_arrow_bottom,
            iconSize = 24.dp,
            description = "collapse",
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 60.dp, height = CONTROL_HEIGHT),
            alignment = Alignment.Center
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            TrackTitle(track = track)
            Cover(track = track, player = player, isClipMode = isClipMode)
            MediaModeSwitch(
                isVisible = track.hasVideo,
                isClipMode = isClipMode,
                activeColor = highlightColor,
                onClipModeChange = onClipModeChange
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            SeekBar(progress = progress, onSeek = viewModel::seekTo)
            Spacer(modifier = Modifier.height(65.dp))
            PlaybackControls(
                isPlaying = state.isPlaying,
                hasPrevious = state.hasPrevious,
                hasNext = state.hasNext,
                isShuffleEnabled = state.isShuffleEnabled,
                repeatMode = state.repeatMode,
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::skipToPrevious,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::skipToNext,
                onRepeat = viewModel::cycleRepeatMode
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            iconRes = if (isShuffleEnabled) R.drawable.ic_mix_active else R.drawable.ic_mix,
            iconSize = MODE_ICON_SIZE,
            description = if (isShuffleEnabled) "shuffle on" else "shuffle off",
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
            alignment = Alignment.CenterStart,
            height = PLAYBACK_CONTROLS_HEIGHT
        )
        IconButton(
            iconRes = R.drawable.ic_skip_previous,
            iconSize = SKIP_ICON_SIZE,
            description = "previous",
            onClick = onPrevious,
            modifier = Modifier.weight(1f),
            alignment = Alignment.Center,
            height = PLAYBACK_CONTROLS_HEIGHT,
            isDimmed = !hasPrevious
        )
        IconButton(
            iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            iconSize = PLAY_PAUSE_ICON_SIZE,
            description = if (isPlaying) "pause" else "play",
            onClick = onPlayPause,
            modifier = Modifier.weight(1f),
            alignment = Alignment.Center,
            height = PLAYBACK_CONTROLS_HEIGHT
        )
        IconButton(
            iconRes = R.drawable.ic_skip_next,
            iconSize = SKIP_ICON_SIZE,
            description = "next",
            onClick = onNext,
            modifier = Modifier.weight(1f),
            alignment = Alignment.Center,
            height = PLAYBACK_CONTROLS_HEIGHT,
            isDimmed = !hasNext
        )
        IconButton(
            iconRes = when (repeatMode) {
                RepeatMode.OFF -> R.drawable.ic_repeat
                RepeatMode.ALL -> R.drawable.ic_repeat_active
                RepeatMode.ONE -> R.drawable.ic_repeat_active_one
            },
            iconSize = MODE_ICON_SIZE,
            description = when (repeatMode) {
                RepeatMode.OFF -> "repeat off"
                RepeatMode.ALL -> "repeat queue"
                RepeatMode.ONE -> "repeat track"
            },
            onClick = onRepeat,
            modifier = Modifier.weight(1f),
            alignment = Alignment.CenterEnd,
            height = PLAYBACK_CONTROLS_HEIGHT
        )
    }
}

@Composable
private fun TrackTitle(track: PlayableTrack) {
    Text(
        text = buildString {
            track.artists?.takeIf { it.isNotBlank() }?.let { append("${it.lowercase()} - ") }
            append("«${track.displayTitle.lowercase()}»")
        },
        color = TextBlack,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MediaModeSwitch(
    isVisible: Boolean,
    isClipMode: Boolean,
    activeColor: Color,
    onClipModeChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        if (isVisible) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MediaModeButton(
                    text = "track",
                    isSelected = !isClipMode,
                    activeColor = activeColor,
                    onClick = { onClipModeChange(false) },
                    modifier = Modifier.weight(1f)
                )
                MediaModeButton(
                    text = "clip",
                    isSelected = isClipMode,
                    activeColor = activeColor,
                    onClick = { onClipModeChange(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MediaModeButton(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = TextBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FullscreenClip(player: Player, modifier: Modifier = Modifier) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insetsController.show(WindowInsetsCompat.Type.systemBars()) }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                onClick = {},
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        VideoSurface(player = player, scale = VideoScale.FIT, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun Cover(track: PlayableTrack, player: Player, isClipMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(COVER_BORDER_WIDTH, BackgroundGray)
    ) {
        if (track.hasVideo) {
            VideoSurface(player = player, scale = VideoScale.STRETCH, modifier = Modifier.fillMaxSize())
        }
        if (!isClipMode) {
            CoverImage(uri = track.artworkUri, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SeekBar(progress: PlaybackProgress, onSeek: (Float) -> Unit) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shownPositionMs = dragFraction?.let { (it * progress.durationMs).toLong() } ?: progress.positionMs

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SeekTrack(
            progress = dragFraction ?: progress.fraction,
            onSeek = { currentOnSeek(it) },
            onDrag = { dragFraction = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Timing(timeMs = shownPositionMs)
            Timing(timeMs = progress.durationMs)
        }
    }
}

@Composable
private fun Timing(timeMs: Long) {
    Text(
        text = formatPlaybackTime(timeMs),
        color = TextBlack,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SeekTrack(progress: Float, onSeek: (Float) -> Unit, onDrag: (Float?) -> Unit) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnDrag by rememberUpdatedState(onDrag)
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    ProgressBar(
        progress = progress,
        height = SEEK_BAR_HEIGHT,
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures { currentOnSeek((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragFraction = (it.x / size.width).coerceIn(0f, 1f)
                        currentOnDrag(dragFraction)
                    },
                    onDragEnd = {
                        dragFraction?.let(currentOnSeek)
                        dragFraction = null
                        currentOnDrag(null)
                    },
                    onDragCancel = {
                        dragFraction = null
                        currentOnDrag(null)
                    }
                ) { change, _ ->
                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    currentOnDrag(dragFraction)
                }
            }
    )
}

@Composable
private fun IconButton(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    iconSize: Dp? = null,
    height: Dp = CONTROL_HEIGHT,
    isDimmed: Boolean = false
) {
    Box(
        modifier = modifier
            .height(height)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = alignment
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (isDimmed) TextBlack.copy(alpha = DIMMED_ICON_ALPHA) else TextBlack,
            modifier = iconSize?.let { Modifier.size(it) } ?: Modifier
        )
    }
}
