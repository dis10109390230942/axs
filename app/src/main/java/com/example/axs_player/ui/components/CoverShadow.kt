package com.example.axs_player.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.axs_player.ui.theme.CoverShadow

private val CoverShadowOffset = 2.dp

fun Modifier.coverShadow(): Modifier = drawBehind {
    val offset = CoverShadowOffset.toPx()
    drawRect(color = CoverShadow, topLeft = Offset(offset, offset), size = size)
}
