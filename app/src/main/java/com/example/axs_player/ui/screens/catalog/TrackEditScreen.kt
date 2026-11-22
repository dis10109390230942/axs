package com.example.axs_player.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.components.TextInput
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted

@Composable
fun TrackEditScreen(onSaved: () -> Unit, onEditDescription: () -> Unit, onEditLyrics: () -> Unit, modifier: Modifier = Modifier, viewModel: TrackEditViewModel = viewModel(factory = TrackEditViewModel.Factory)) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    var title by remember(form?.title) { mutableStateOf(form?.title.orEmpty()) }
    var subtitle by remember(form?.title, form?.subtitle) { mutableStateOf(form?.subtitle.orEmpty()) }
    var trackDate by remember(form?.title, form?.trackDate) { mutableStateOf(form?.trackDate.orEmpty()) }
    var description by remember(form?.title, form?.description) { mutableStateOf(form?.description.orEmpty()) }
    var year by remember(form?.title, form?.year) { mutableStateOf(form?.year.orEmpty()) }
    LaunchedEffect(isSaved) { if (isSaved) onSaved() }
    form?.let { current ->
        Column(modifier.fillMaxSize().background(PageBackground).verticalScroll(rememberScrollState()).padding(10.dp)) {
            SectionTitle(if (current.isLive) "live edit" else "track edit")
            Spacer(Modifier.height(30.dp))
            SectionTitle("title")
            Spacer(Modifier.height(10.dp))
            TextInput(title, { title = it }, placeholder = "-")
            Spacer(Modifier.height(20.dp))
            SectionTitle("sub title")
            Spacer(Modifier.height(10.dp))
            TextInput(subtitle, { subtitle = it }, placeholder = "-")
            Spacer(Modifier.height(20.dp))
            SectionTitle("track date")
            Spacer(Modifier.height(10.dp))
            TextInput(trackDate, { trackDate = it }, placeholder = "YYYY-MM-DD")
            Spacer(Modifier.height(20.dp))
            SectionTitle("year")
            Spacer(Modifier.height(10.dp))
            TextInput(year, { year = it }, placeholder = "-")
            Spacer(Modifier.height(20.dp))
            SectionTitle("description")
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .border(2.dp, BackgroundGray)
                    .clickable(onClick = onEditDescription, indication = null, interactionSource = remember { MutableInteractionSource() })
                    .padding(16.dp)
            ) {
                Text(description.ifBlank { "-" }, color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(20.dp))
            SectionTitle("track lyric")
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .border(2.dp, BackgroundGray)
                    .clickable(onClick = onEditLyrics, indication = null, interactionSource = remember { MutableInteractionSource() })
                    .padding(16.dp)
            ) {
                Text(
                    text = current.lyrics.ifBlank { "-" },
                    color = if (current.lyrics.isBlank()) TextMuted else TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(30.dp))
            PrimaryButton("save", onClick = { viewModel.save(title, subtitle, trackDate, description, year) })
        }
    }
}
