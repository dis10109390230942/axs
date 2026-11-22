package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

private val CARD_HEIGHT = 100.dp
private val IMAGE_SIZE = 80.dp
private val CARD_BORDER_WIDTH = 2.dp

enum class TopResultKind { ARTIST, ALBUM, TRACK }

data class TopResultData(
    val id: String,
    val kind: TopResultKind,
    val title: String,
    val subtitle: String?,
    val imageUri: Uri?
)

@Composable
fun TopResultItem(result: TopResultData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .coverShadow()
            .background(PageBackground)
            .border(CARD_BORDER_WIDTH, BackgroundGray)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(uri = result.imageUri, modifier = Modifier.size(IMAGE_SIZE))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val isTrack = result.kind == TopResultKind.TRACK
            Text(
                text = if (isTrack) "«${result.title}»" else result.title,
                color = TextBlack,
                fontSize = 16.sp,
                fontStyle = if (isTrack) FontStyle.Italic else FontStyle.Normal,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            result.subtitle?.let {
                Text(
                    text = it,
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
