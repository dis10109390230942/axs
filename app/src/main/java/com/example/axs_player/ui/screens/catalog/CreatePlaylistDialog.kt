package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.R
import com.example.axs_player.ui.components.AlbumTrackItemData
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SearchInput
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

@Composable
fun CreatePlaylistDialog(
    onCreated: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: CreatePlaylistViewModel = viewModel(factory = CreatePlaylistViewModel.Factory)
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val dismiss = {
        viewModel.reset()
        onDismiss()
    }

    when (step) {
        CreatePlaylistStep.NAME -> PlaylistNameDialog(viewModel = viewModel, onDismiss = dismiss)
        CreatePlaylistStep.TRACKS -> TrackPickerDialog(viewModel = viewModel, onCreated = onCreated)
    }
}

@Composable
private fun PlaylistNameDialog(viewModel: CreatePlaylistViewModel, onDismiss: () -> Unit) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PageBackground)
                .border(2.dp, BackgroundGray)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "new playlist", color = TextBlack, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            SearchInput(
                value = name,
                onValueChange = viewModel::onNameChange,
                placeholder = "playlist name",
                imeAction = ImeAction.Next,
                onImeAction = viewModel::openTracks,
                modifier = Modifier.focusRequester(focusRequester)
            )
            PrimaryButton(text = "next", onClick = viewModel::openTracks, enabled = name.isNotBlank())
            PrimaryButton(text = "cancel", onClick = onDismiss)
        }
    }
}

@Composable
private fun TrackPickerDialog(viewModel: CreatePlaylistViewModel, onCreated: () -> Unit) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = viewModel::backToName,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBackground)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "add tracks to «${name.trim()}»",
                color = TextBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SearchInput(value = query, onValueChange = viewModel::onQueryChange, placeholder = "search tracks")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tracks, key = { it.id }) { track ->
                    val order = selected.indexOf(track.id)
                    TrackItem(
                        track = AlbumTrackItemData(
                            id = track.id,
                            number = if (order >= 0) order + 1 else null,
                            title = track.title,
                            hasVideo = track.hasVideo
                        ),
                        onClick = { viewModel.toggle(track.id) },
                        trailingIcon = if (order >= 0) R.drawable.ic_minus else R.drawable.ic_plus
                    )
                }
                if (tracks.isEmpty()) {
                    item {
                        Text(
                            text = if (query.isBlank()) "library is empty" else "nothing found",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
            PrimaryButton(
                text = if (selected.isEmpty()) "create" else "create • ${selected.size}",
                onClick = { viewModel.create(onCreated) }
            )
            PrimaryButton(text = "back", onClick = viewModel::backToName)
        }
    }
}
