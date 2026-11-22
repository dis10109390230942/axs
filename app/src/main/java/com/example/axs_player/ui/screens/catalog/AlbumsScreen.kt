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
import com.example.axs_player.ui.components.AlbumGrid
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun AlbumsScreen(
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumsViewModel = viewModel(factory = AlbumsViewModel.Factory)
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    AlbumGrid(
        albums = albums,
        onAlbumClick = { onAlbumClick(it.id) },
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp),
        header = {
            Text(
                text = "albums",
                color = TextBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    )
}
