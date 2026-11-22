package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.axs_player.ui.theme.BackgroundGray
import kotlin.math.max
import kotlin.math.roundToInt

enum class CoverScale { CROP, STRETCH }

@Composable
fun CoverImage(uri: Uri?, modifier: Modifier = Modifier, scale: CoverScale = CoverScale.CROP) {
    val resolver = LocalContext.current.contentResolver
    var sizePx by remember { mutableIntStateOf(0) }
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri, sizePx) {
        if (uri != null && sizePx > 0) bitmap = CoverCache.load(resolver, uri, sizePx)
    }

    Spacer(
        modifier = modifier
            .background(BackgroundGray)
            .onSizeChanged { sizePx = max(it.width, it.height) }
            .drawBehind {
                val image = bitmap ?: uri?.let { CoverCache.get(it, max(size.width, size.height).roundToInt()) }
                image?.let {
                    when (scale) {
                        CoverScale.CROP -> drawCropped(it)
                        CoverScale.STRETCH -> drawImage(
                            image = it,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        )
                    }
                }
            }
    )
}

private fun DrawScope.drawCropped(image: ImageBitmap) {
    val scale = max(size.width / image.width, size.height / image.height)
    val sourceWidth = (size.width / scale).roundToInt().coerceIn(1, image.width)
    val sourceHeight = (size.height / scale).roundToInt().coerceIn(1, image.height)
    drawImage(
        image = image,
        srcOffset = IntOffset((image.width - sourceWidth) / 2, (image.height - sourceHeight) / 2),
        srcSize = IntSize(sourceWidth, sourceHeight),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    )
}
