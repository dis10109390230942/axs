package com.example.axs_player.ui.player

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
enum class VideoScale(val resizeMode: Int) {
    STRETCH(AspectRatioFrameLayout.RESIZE_MODE_FILL),
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT)
}

@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(player: Player, scale: VideoScale, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShutterBackgroundColor(Color.TRANSPARENT)
                setKeepContentOnPlayerReset(true)
                artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_OFF
            }
        },
        update = { view ->
            view.resizeMode = scale.resizeMode
            view.player = player
        },
        onRelease = { view -> view.player = null },
        modifier = modifier
    )
}
