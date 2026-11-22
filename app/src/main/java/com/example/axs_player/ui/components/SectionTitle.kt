package com.example.axs_player.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextBlack,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-1).sp,
        modifier = modifier
    )
}
