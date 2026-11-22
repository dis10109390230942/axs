package com.example.axs_player.ui.screens.catalog

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.navigation.ArtistCategory
import com.example.axs_player.ui.components.AlbumItem
import com.example.axs_player.ui.components.AlbumItemData
import com.example.axs_player.ui.components.AlbumMediaRow
import com.example.axs_player.ui.components.CoverImage
import com.example.axs_player.ui.components.CoverScale
import com.example.axs_player.ui.components.TrackActionsBottomSheet
import com.example.axs_player.ui.components.InfoRow
import com.example.axs_player.ui.components.MarkdownText
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SectionTitle
import com.example.axs_player.ui.components.TrackItem
import com.example.axs_player.ui.theme.OldStandardTT
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.theme.TextWhite

private val PAGE_PADDING = 10.dp
private val NAME_BAND_HEIGHT = 100.dp
private val PHOTO_HEIGHT = 350.dp
private val PHOTO_PEEK = 40.dp
private val ALBUM_WIDTH = 160.dp
private const val BIO_MAX_LINES = 4

@Composable
fun ArtistScreen(
    onEdit: () -> Unit,
    onOpenBio: () -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onSeeAll: (category: ArtistCategory) -> Unit,
    onEditTrack: (trackId: String) -> Unit,
    onOpenArtist: (artistId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = viewModel(factory = ArtistViewModel.Factory)
) {
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    var selectedTrackId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(top = 60.dp)
    ) {
        artist?.let { current ->
            NameBand(name = current.name, onClick = onEdit)
            Spacer(modifier = Modifier.height(10.dp))
            if (photos.isNotEmpty()) {
                PhotoRow(photos = photos)
                Spacer(modifier = Modifier.height(30.dp))
            }
            if (media.isNotEmpty()) {
                AlbumMediaRow(media = media, modifier = Modifier.padding(horizontal = PAGE_PADDING), viewerBackground = PageBackground)
                Spacer(modifier = Modifier.height(30.dp))
            }
            current.bio?.takeIf { it.isNotBlank() }?.let {
                Bio(text = it, onClick = onOpenBio)
                Spacer(modifier = Modifier.height(30.dp))
            }
            Column(
                modifier = Modifier.padding(horizontal = PAGE_PADDING),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRow(label = "full name", value = current.fullName)
                InfoRow(label = "date of birth", value = current.birthDate)
                InfoRow(label = "date of death", value = current.deathDate)
                InfoRow(label = "place of birth", value = current.birthPlace)
                InfoRow(label = "countries", value = current.countries)
            }
            Spacer(modifier = Modifier.height(30.dp))
            if (tracks.isNotEmpty()) {
                SectionTitle(text = "top tracks", modifier = Modifier.padding(horizontal = PAGE_PADDING))
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(horizontal = PAGE_PADDING),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tracks.forEach { track ->
                        TrackItem(track = track, onClick = { viewModel.play(track.id) }, onMore = { selectedTrackId = track.id })
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(
                    text = "see all",
                    onClick = { onSeeAll(ArtistCategory.TRACKS) },
                    modifier = Modifier.padding(horizontal = PAGE_PADDING)
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
            if (albums.isNotEmpty()) {
                SectionTitle(text = "albums", modifier = Modifier.padding(horizontal = PAGE_PADDING))
                Spacer(modifier = Modifier.height(10.dp))
                AlbumRow(albums = albums, onAlbumClick = onOpenAlbum)
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(
                    text = "see all",
                    onClick = { onSeeAll(ArtistCategory.ALBUMS) },
                    modifier = Modifier.padding(horizontal = PAGE_PADDING)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    if (selectedTrackId != null) {
        TrackActionsBottomSheet(selectedTrackId!!, onEdit = { onEditTrack(selectedTrackId!!); selectedTrackId = null }, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onDismiss = { selectedTrackId = null })
    }
}

@Composable
private fun NameBand(name: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_PADDING)
            .height(NAME_BAND_HEIGHT)
            .background(TextBlack)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.uppercase(),
            color = TextWhite,
            fontSize = 24.sp,
            fontFamily = OldStandardTT,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PhotoRow(photos: List<Uri>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val photoWidth = maxWidth - PAGE_PADDING - PHOTO_PEEK - PAGE_PADDING
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_PADDING),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(photos, key = { it.toString() }) { photo ->
                CoverImage(
                    uri = photo,
                    modifier = Modifier
                        .width(photoWidth)
                        .height(PHOTO_HEIGHT),
                    scale = CoverScale.STRETCH
                )
            }
        }
    }
}

@Composable
private fun Bio(text: String, onClick: () -> Unit) {
    MarkdownText(
        text = text,
        color = TextMuted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        onTextClick = onClick,
        maxLines = BIO_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = PAGE_PADDING)
    )
}

@Composable
private fun AlbumRow(albums: List<AlbumItemData>, onAlbumClick: (albumId: String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PAGE_PADDING),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumItem(
                album = album,
                onClick = { onAlbumClick(album.id) },
                modifier = Modifier.width(ALBUM_WIDTH)
            )
        }
    }
}
