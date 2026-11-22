package com.example.axs_player.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.ProgressFillBottom
import com.example.axs_player.ui.theme.ProgressFillMiddle
import com.example.axs_player.ui.theme.ProgressFillTop
import com.example.axs_player.ui.theme.ProgressInnerShadow

private val ProgressFill = Brush.verticalGradient(
    0f to ProgressFillTop,
    0.5f to ProgressFillMiddle,
    1f to ProgressFillBottom
)
private val InnerShadowOffsetY = 1.dp
private val InnerShadowBlur = 4.dp

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 20.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(BackgroundGray)
            .innerShadow()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(ProgressFill)
        )
    }
}

private fun Modifier.innerShadow(): Modifier = drawWithCache {
    val blur = InnerShadowBlur.toPx()
    val offsetY = InnerShadowOffsetY.toPx()
    val frame = Path().apply {
        fillType = Path.FillType.EVEN_ODD
        addRect(-blur * 2, -blur * 2, size.width + blur * 2, size.height + blur * 2, Path.Direction.CW)
        addRect(0f, offsetY, size.width, size.height + offsetY, Path.Direction.CW)
    }
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        color = ProgressInnerShadow.toArgb()
        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    }
    onDrawWithContent {
        drawContent()
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipRect(0f, 0f, size.width, size.height)
            canvas.nativeCanvas.drawPath(frame, paint)
            canvas.restore()
        }
    }
}
