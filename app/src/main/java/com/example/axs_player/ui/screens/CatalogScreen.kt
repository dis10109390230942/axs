package com.example.axs_player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.axs_player.navigation.Screen
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.components.SecondaryButton
import com.example.axs_player.ui.screens.catalog.CreatePlaylistDialog
import com.example.axs_player.ui.theme.PageBackground

@Composable
fun CatalogScreen(navController: NavController, modifier: Modifier = Modifier) {
    var isCreatePlaylistVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(start = 10.dp, top = 60.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SecondaryButton(
            text = "favorite songs",
            onClick = { navController.navigate(Screen.FavoriteSongs.route) },
            showArrow = true
        )

        SecondaryButton(
            text = "lives",
            onClick = { navController.navigate(Screen.Lives.route) },
            showArrow = true
        )

        SecondaryButton(
            text = "albums",
            onClick = { navController.navigate(Screen.Albums.route) },
            showArrow = true
        )

        SecondaryButton(
            text = "playlists",
            onClick = { navController.navigate(Screen.Playlists.route) },
            showArrow = true
        )

        SecondaryButton(
            text = "artists",
            onClick = { navController.navigate(Screen.Artists.route) },
            showArrow = true
        )

        SecondaryButton(
            text = "listening history",
            onClick = { navController.navigate(Screen.ListeningHistory.route) },
            showArrow = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = "create",
            onClick = { isCreatePlaylistVisible = true }
        )
    }

    if (isCreatePlaylistVisible) {
        CreatePlaylistDialog(
            onCreated = {
                isCreatePlaylistVisible = false
                navController.navigate(Screen.Playlists.route)
            },
            onDismiss = { isCreatePlaylistVisible = false }
        )
    }
}
