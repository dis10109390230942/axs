package com.example.axs_player.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextPlaceholder

private val INPUT_HEIGHT = 32.dp
private val INPUT_BORDER_WIDTH = 2.dp
private val INPUT_PADDING = 16.dp

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "-"
) {
    val textStyle = LocalTextStyle.current.copy(color = TextBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(TextBlack),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(INPUT_HEIGHT)
                    .border(INPUT_BORDER_WIDTH, BackgroundGray)
                    .padding(horizontal = INPUT_PADDING),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = textStyle.copy(color = TextPlaceholder), maxLines = 1)
                }
                innerTextField()
            }
        }
    )
}
