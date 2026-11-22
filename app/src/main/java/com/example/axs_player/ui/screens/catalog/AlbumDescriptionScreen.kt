package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.components.MarkdownText
import com.example.axs_player.ui.components.rememberCoverColors
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun AlbumDescriptionScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.Factory)
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val coverUri by viewModel.coverUri.collectAsStateWithLifecycle()
    val colors = rememberCoverColors(coverUri)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, top = 60.dp, end = 10.dp, bottom = 30.dp)
    ) {
        SectionTitle(text = "album description")
        MarkdownText(
            text = album?.description?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "–",
            color = TextBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            linkColor = colors.highlight,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
