package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

@Composable
fun TrackLyricsEditScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackLyricsEditViewModel = viewModel(factory = TrackLyricsEditViewModel.Factory)
) {
    val title by viewModel.title.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    LaunchedEffect(isSaved) { if (isSaved) onSaved() }
    lyrics?.let { value ->
        Column(
            modifier = modifier.fillMaxSize().background(PageBackground).verticalScroll(rememberScrollState()).padding(10.dp)
        ) {
            SectionTitle("edit track lyric")
            Spacer(Modifier.height(10.dp))
            SectionTitle(title)
            Spacer(Modifier.height(20.dp))
            BasicTextField(
                value = value,
                onValueChange = viewModel::onLyricsChange,
                modifier = Modifier.fillMaxWidth().height(420.dp).border(2.dp, BackgroundGray).padding(16.dp),
                textStyle = LocalTextStyle.current.copy(color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(TextBlack)
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton("save", onClick = viewModel::save)
        }
    }
}
