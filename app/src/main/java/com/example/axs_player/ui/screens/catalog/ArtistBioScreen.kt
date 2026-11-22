package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.components.MarkdownText
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextMuted

@Composable
fun ArtistBioScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistBioViewModel = viewModel(factory = ArtistBioViewModel.Factory)
) {
    val bio by viewModel.bio.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 20.dp)
    ) {
        SectionTitle(text = "artist bio")
        Spacer(modifier = Modifier.height(10.dp))
        MarkdownText(
            text = bio,
            modifier = Modifier.fillMaxWidth(),
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
