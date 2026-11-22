package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.components.TrackActionsBottomSheet
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun LiveTrackScreen(
    onEditTrack: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveTrackViewModel = viewModel(factory = LiveTrackViewModel.Factory)
) {
    val title by viewModel.parentTitle.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(PageBackground),
        contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
    ) {
        item {
            Text(title, color = TextBlack, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp))
        }
        items(items, key = { it.id }) { live ->
            TrackItem(track = live, onClick = { viewModel.play(live.id) }, onMore = { selectedTrackId = live.id })
        }
    }
    if (selectedTrackId != null) {
        TrackActionsBottomSheet(selectedTrackId!!, onEdit = { onEditTrack(selectedTrackId!!); selectedTrackId = null }, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onDismiss = { selectedTrackId = null })
    }
}
