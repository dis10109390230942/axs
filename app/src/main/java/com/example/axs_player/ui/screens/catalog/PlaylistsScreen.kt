package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.PlaylistGrid
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun PlaylistsScreen(
    onPlaylistClick: (playlistId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.Factory)
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var playlistToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val playlistToDelete = playlists.firstOrNull { it.id == playlistToDeleteId }

    PlaylistGrid(
        playlists = playlists,
        onPlaylistClick = { onPlaylistClick(it.id) },
        onPlaylistLongClick = { playlistToDeleteId = it.id },
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp),
        header = {
            Text(
                text = "playlists",
                color = TextBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    )

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDeleteId = null },
            title = { Text("delete playlist?") },
            text = { Text("«${playlist.name}» will be deleted. Tracks stay in the library.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistToDeleteId = null
                        viewModel.delete(playlist.id)
                    }
                ) {
                    Text("delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDeleteId = null }) {
                    Text("cancel")
                }
            }
        )
    }
}
