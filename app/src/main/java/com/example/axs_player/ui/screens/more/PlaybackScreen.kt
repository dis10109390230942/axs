package com.example.axs_player.ui.screens.more

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.playback.MAX_CROSSFADE_SECONDS
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.theme.TextWhite
import com.example.axs_player.ui.theme.ToggleOff
import com.example.axs_player.ui.theme.ToggleOn
import kotlin.math.roundToInt

private val ROW_GAP = 10.dp
private val ROW_BOTTOM_PADDING = 20.dp
private val DIVIDER_WIDTH = 1.dp
private val TOGGLE_HEIGHT = 30.dp
private val TOGGLE_PADDING = 28.dp
private val SLIDER_HEIGHT = 24.dp
private val SLIDER_INSET = 5.dp
private val TICK_HEIGHT = 4.dp
private val THUMB_WIDTH = 10.dp
private val THUMB_HEIGHT = 11.dp
private val THUMB_GAP = 6.dp
private const val TICK_COUNT = 4
private val LABEL_LETTER_SPACING = (-1).sp

@Composable
fun PlaybackScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaybackViewModel = viewModel(factory = PlaybackViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP)
    ) {
        ToggleRow(
            title = "Gapless playback",
            description = "Removes any gaps or pauses that may occur in between tracks.",
            isOn = settings.isGapless,
            onToggle = viewModel::setGapless
        )
        SettingRow {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    SettingText(
                        title = "Crossfade",
                        description = "Adjust the length of fading and overlap in between tracks.",
                        modifier = Modifier.weight(1f)
                    )
                    Label(
                        text = if (settings.crossfadeSeconds == 0) "off" else "${settings.crossfadeSeconds} s",
                        color = TextMuted
                    )
                }
                StepSlider(
                    value = settings.crossfadeSeconds,
                    maxValue = MAX_CROSSFADE_SECONDS,
                    onValueChange = viewModel::setCrossfadeSeconds
                )
            }
        }
        ToggleRow(
            title = "Mono audio",
            description = "Left and right speakers play the same audio.",
            isOn = settings.isMono,
            onToggle = viewModel::setMono
        )
        ToggleRow(
            title = "Volume normalisation",
            description = "Sets the same loudness level for all tracks.",
            isOn = settings.isNormalized,
            onToggle = viewModel::setNormalized
        )
    }
}

@Composable
private fun SettingRow(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val width = DIVIDER_WIDTH.toPx()
                drawRect(
                    color = BackgroundGray,
                    topLeft = Offset(0f, size.height - width),
                    size = Size(size.width, width)
                )
            }
            .padding(bottom = ROW_BOTTOM_PADDING)
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(title: String, description: String, isOn: Boolean, onToggle: (Boolean) -> Unit) {
    SettingRow {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingText(title = title, description = description, modifier = Modifier.weight(1f))
            Toggle(isOn = isOn, onClick = { onToggle(!isOn) })
        }
    }
}

@Composable
private fun SettingText(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Label(text = title, color = TextBlack)
        Label(text = description, color = TextMuted)
    }
}

@Composable
private fun Label(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LABEL_LETTER_SPACING
    )
}

@Composable
private fun Toggle(isOn: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(TOGGLE_HEIGHT)
            .background(if (isOn) ToggleOn else ToggleOff)
            .border(DIVIDER_WIDTH, BackgroundGray)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = TOGGLE_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isOn) "on" else "off",
            color = if (isOn) TextBlack else TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StepSlider(value: Int, maxValue: Int, onValueChange: (Int) -> Unit) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(SLIDER_HEIGHT)
            .pointerInput(maxValue) {
                val inset = SLIDER_INSET.toPx()
                fun valueAt(x: Float): Int =
                    ((x - inset) / (size.width - inset * 2) * maxValue).roundToInt().coerceIn(0, maxValue)
                detectTapGestures { currentOnValueChange(valueAt(it.x)) }
            }
            .pointerInput(maxValue) {
                val inset = SLIDER_INSET.toPx()
                fun valueAt(x: Float): Int =
                    ((x - inset) / (size.width - inset * 2) * maxValue).roundToInt().coerceIn(0, maxValue)
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnValueChange(valueAt(change.position.x))
                }
            }
    ) {
        val inset = SLIDER_INSET.toPx()
        val trackWidth = size.width - inset * 2
        val lineWidth = DIVIDER_WIDTH.toPx()
        drawRect(color = TextBlack, topLeft = Offset(inset, 0f), size = Size(trackWidth, lineWidth))
        (0..TICK_COUNT).forEach { index ->
            val x = inset + trackWidth * index / TICK_COUNT
            drawRect(color = TextBlack, topLeft = Offset(x - lineWidth / 2, 0f), size = Size(lineWidth, TICK_HEIGHT.toPx()))
        }
        val thumbX = inset + trackWidth * value / maxValue
        val thumbTop = THUMB_GAP.toPx()
        val halfWidth = THUMB_WIDTH.toPx() / 2
        drawPath(
            path = Path().apply {
                moveTo(thumbX, thumbTop)
                lineTo(thumbX + halfWidth, thumbTop + THUMB_HEIGHT.toPx())
                lineTo(thumbX - halfWidth, thumbTop + THUMB_HEIGHT.toPx())
                close()
            },
            color = TextBlack
        )
    }
}
