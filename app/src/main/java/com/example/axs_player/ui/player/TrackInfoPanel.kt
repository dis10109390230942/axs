package com.example.axs_player.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.TextBlack

private val SWIPE_CLOSE_THRESHOLD = 48.dp

@Composable
fun TrackInfoPanel(
    info: TrackInfo,
    backgroundColor: Color,
    linkColor: Color,
    onClose: (PanelDirection) -> Unit,
    onArtistClick: (artistId: String) -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    onTrackClick: (trackId: String) -> Unit,
    onEditTrack: (trackId: String) -> Unit,
    onOpenSettings: () -> Unit,
    settingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnClose by rememberUpdatedState(onClose)
    val pullDownToClose = rememberPullDownToCollapse(SWIPE_CLOSE_THRESHOLD) { onClose(PanelDirection.DOWN) }
    val pullUpToSettings = rememberPullUpToExpand(SWIPE_CLOSE_THRESHOLD, onOpenSettings)

    BackHandler(onBack = { onClose(PanelDirection.DOWN) })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                onClick = {},
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(top = 100.dp)
    ) {
        Text(
            text = "track info",
            color = TextBlack,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(pullDownToClose)
                .nestedScroll(pullUpToSettings)
                .verticalScroll(rememberScrollState())
        ) {
            TrackInfoSection(
                info = info,
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                onTrackClick = onTrackClick,
                onEditTrack = onEditTrack,
                linkColor = linkColor,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 100.dp)
            )
        }
        settingsContent()
    }
}
