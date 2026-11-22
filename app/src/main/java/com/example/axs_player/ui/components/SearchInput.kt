package com.example.axs_player.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.R
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextPlaceholder

private val INPUT_HEIGHT = 60.dp
private val INPUT_BORDER_WIDTH = 2.dp

@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "search",
    imeAction: ImeAction = ImeAction.Search,
    onImeAction: () -> Unit = {}
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val onAction: () -> Unit = {
        keyboard?.hide()
        onImeAction()
    }
    val textStyle = LocalTextStyle.current.copy(color = TextBlack, fontSize = 14.sp, fontWeight = FontWeight.Medium)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(TextBlack),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { onAction() },
            onSearch = { onAction() },
            onGo = { onAction() }
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(INPUT_HEIGHT)
                    .border(INPUT_BORDER_WIDTH, BackgroundGray)
                    .padding(start = 16.dp, end = if (value.isEmpty()) 16.dp else 25.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = textStyle.copy(color = TextPlaceholder), maxLines = 1)
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "clear",
                        tint = TextBlack,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                onClick = { onValueChange("") },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    )
                }
            }
        }
    )
}
