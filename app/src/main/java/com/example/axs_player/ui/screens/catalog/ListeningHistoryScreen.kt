package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.TrackList
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun ListeningHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: ListeningHistoryViewModel = viewModel(factory = ListeningHistoryViewModel.Factory)
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    TrackList(
        tracks = items,
        onTrackClick = { viewModel.play(it.id) },
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp),
        header = {
            Text(
                text = "listening history",
                color = TextBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    )
}
