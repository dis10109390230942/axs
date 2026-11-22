package com.example.axs_player.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.withTransaction
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.db.PlaylistItemEntity
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsBottomSheet(
    trackId: String,
    onEdit: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val application = LocalContext.current.applicationContext as AxsApplication
    val dao = application.database.libraryDao()
    val playlists by dao.observePlaylistChoices().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var isChoosingPlaylist by remember { mutableStateOf(false) }
    var isDeleteConfirmationVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = PageBackground,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = BackgroundGray,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                .padding(start = 10.dp, end = 10.dp, bottom = 30.dp)
        ) {
            if (isChoosingPlaylist) {
                SecondaryButton(text = "back", onClick = { isChoosingPlaylist = false })
                playlists.forEach { playlist ->
                    SecondaryButton(
                        text = playlist.name,
                        onClick = {
                            scope.launch {
                                application.database.withTransaction {
                                    dao.insertPlaylistItems(
                                        listOf(
                                            PlaylistItemEntity(
                                                id = UUID.randomUUID().toString(),
                                                playlistId = playlist.id,
                                                trackId = trackId,
                                                position = dao.nextPlaylistPosition(playlist.id)
                                            )
                                        )
                                    )
                                }
                                onDismiss()
                            }
                        }
                    )
                }
                if (playlists.isEmpty()) {
                    SecondaryButton(text = "no playlists", onClick = {}, enabled = false, showArrow = false)
                }
            } else {
                SecondaryButton(text = "play next", onClick = {
                    scope.launch { application.libraryPlayback.playNext(trackId); onDismiss() }
                })
                SecondaryButton(text = "add to playlist", onClick = { isChoosingPlaylist = true })
                SecondaryButton(text = "add to queue", onClick = {
                    scope.launch { application.libraryPlayback.enqueue(trackId); onDismiss() }
                })
                SecondaryButton(text = "open album", onClick = {
                    scope.launch { dao.firstAlbumIdForTrack(trackId)?.let(onOpenAlbum); onDismiss() }
                })
                SecondaryButton(text = "go to artist", onClick = {
                    scope.launch { dao.firstArtistIdForTrack(trackId)?.let(onOpenArtist); onDismiss() }
                })
                SecondaryButton(text = "edit", onClick = onEdit)
                SecondaryButton(text = "delete", onClick = { isDeleteConfirmationVisible = true })
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmationVisible = false },
            title = { Text("delete track?") },
            text = { Text("The track file will be deleted from the device and removed from the library.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val track = dao.trackEntity(trackId) ?: return@launch
                            val isFileDeleted = withContext(Dispatchers.IO) {
                                application.libraryFolder.deleteTrack(track.fileName)
                            }
                            if (isFileDeleted) {
                                application.database.withTransaction { dao.deleteTrack(trackId) }
                                application.playbackController.removeTrack(trackId)
                                isDeleteConfirmationVisible = false
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text("delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmationVisible = false }) {
                    Text("cancel")
                }
            }
        )
    }
}
