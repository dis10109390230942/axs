package com.example.axs_player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.data.library.ImportState
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.ProgressBar
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun ScanScreen(
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory)
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    LaunchedEffect(importState) {
        if (importState !is ImportState.Running) onLeave()
    }

    val progress = (importState as? ImportState.Running)
        ?.let { if (it.total == 0) 0f else it.processed.toFloat() / it.total }
        ?: 1f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "auto rescan catalog", color = TextBlack, fontSize = 14.sp)
            ProgressBar(progress = progress)
        }
        PrimaryButton(
            text = "in the background",
            onClick = onLeave,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        )
    }
}
