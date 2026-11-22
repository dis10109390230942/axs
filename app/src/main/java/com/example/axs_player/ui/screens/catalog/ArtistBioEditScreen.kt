package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.MarkdownEditor
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

@Composable
fun ArtistBioEditScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistBioEditViewModel = viewModel(factory = ArtistBioEditViewModel.Factory)
) {
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    LaunchedEffect(isSaved) {
        if (isSaved) onSaved()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .imePadding()
            .padding(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
    ) {
        SectionTitle(text = "edit artist bio")
        Spacer(modifier = Modifier.height(10.dp))
        bio?.let { value ->
            MarkdownEditor(value = value, onValueChange = viewModel::onBioChange, modifier = Modifier.weight(1f), showBorder = false, fillAvailable = true)
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(text = "save", onClick = viewModel::save)
        }
    }
}
