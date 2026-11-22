package com.example.axs_player.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.TextBlack

const val ARTIST_PHOTO_RATIO = 3f / 4f

data class ArtistItemData(
    val id: String,
    val name: String,
    val photoUri: Uri?
)

@Composable
fun ArtistItem(artist: ArtistItemData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        CoverImage(
            uri = artist.photoUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ARTIST_PHOTO_RATIO)
                .coverShadow()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = artist.name,
            color = TextBlack,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
