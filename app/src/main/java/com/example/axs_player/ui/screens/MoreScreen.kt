package com.example.axs_player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.axs_player.navigation.Screen
import com.example.axs_player.ui.components.SecondaryButton
import com.example.axs_player.ui.theme.PageBackground

@Composable
fun MoreScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(start = 10.dp, top = 60.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SecondaryButton(
            text = "playback",
            onClick = { navController.navigate(Screen.Playback.route) },
            showArrow = true
        )
        SecondaryButton(
            text = "data-saving",
            onClick = { navController.navigate(Screen.DataSaving.route) },
            showArrow = true
        )
    }
}
