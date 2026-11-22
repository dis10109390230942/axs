package com.example.axs_player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

enum class ButtonType {
    PRIMARY,
    SECONDARY
}

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    type: ButtonType = ButtonType.PRIMARY,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = false
) {
    when (type) {
        ButtonType.PRIMARY -> PrimaryButtonContent(
            text = text,
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        )
        ButtonType.SECONDARY -> SecondaryButtonContent(
            text = text,
            onClick = onClick,
            enabled = enabled,
            showArrow = showArrow,
            modifier = modifier
        )
    }
}

@Composable
private fun PrimaryButtonContent(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (enabled) Color.White else Color(0xFFCCCCCC)
    val textColor = if (enabled) TextBlack else Color(0xFF666666)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SecondaryButtonContent(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    showArrow: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (enabled) PageBackground else Color(0xFFBBBBBB)
    val borderColor = if (enabled) BackgroundGray else Color(0xFF888888)
    val textColor = if (enabled) TextBlack else Color(0xFF666666)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .clickable(
                onClick = onClick,
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 21.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            if (showArrow) {
                Text(
                    text = "▶",
                    color = textColor,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        text = text,
        onClick = onClick,
        type = ButtonType.PRIMARY,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = true
) {
    Button(
        text = text,
        onClick = onClick,
        type = ButtonType.SECONDARY,
        modifier = modifier,
        enabled = enabled,
        showArrow = showArrow
    )
}
