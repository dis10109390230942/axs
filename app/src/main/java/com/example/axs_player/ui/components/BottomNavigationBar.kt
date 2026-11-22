package com.example.axs_player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.navigation.Screen
import com.example.axs_player.ui.theme.ActiveGray
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.TextBlack

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundGray)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            NavigationItem(
                text = "home",
                isActive = currentRoute.startsWith(Screen.Home.route),
                onClick = { onNavigate(Screen.Home) },
                modifier = Modifier.weight(1f)
            )
            NavigationItem(
                text = "search",
                isActive = currentRoute.startsWith(Screen.Search.route),
                onClick = { onNavigate(Screen.Search) },
                modifier = Modifier.weight(1f)
            )
            NavigationItem(
                text = "catalog",
                isActive = currentRoute.startsWith(Screen.Catalog.route),
                onClick = { onNavigate(Screen.Catalog) },
                modifier = Modifier.weight(1f)
            )
            NavigationItem(
                text = "more",
                isActive = currentRoute.startsWith(Screen.More.route),
                onClick = { onNavigate(Screen.More) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavigationItem(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (isActive) ActiveGray else BackgroundGray)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
