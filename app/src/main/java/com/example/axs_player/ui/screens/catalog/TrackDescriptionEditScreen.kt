package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.MarkdownEditor
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.theme.PageBackground

@Composable
fun TrackDescriptionEditScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackEditViewModel = viewModel(factory = TrackEditViewModel.Factory)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    var description by remember(form?.title) { mutableStateOf(form?.description.orEmpty()) }
    LaunchedEffect(isSaved) { if (isSaved) onSaved() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .imePadding()
            .padding(10.dp)
    ) {
        SectionTitle("edit track description")
        Spacer(Modifier.height(20.dp))
        MarkdownEditor(description, { description = it }, showBorder = false, modifier = Modifier.weight(1f), fillAvailable = true)
        Spacer(Modifier.height(20.dp))
        PrimaryButton("save", onClick = { viewModel.save(form?.title.orEmpty(), form?.subtitle.orEmpty(), form?.trackDate.orEmpty(), description, form?.year.orEmpty()) })
    }
}
