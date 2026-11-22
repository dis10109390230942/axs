package com.example.axs_player

import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.axs_player.ui.theme.PageBackground
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navArgument
import com.example.axs_player.navigation.ALBUM_ID_ARGUMENT
import com.example.axs_player.navigation.ARTIST_CATEGORY_ARGUMENT
import com.example.axs_player.navigation.ARTIST_ID_ARGUMENT
import com.example.axs_player.navigation.PLAYLIST_ID_ARGUMENT
import com.example.axs_player.navigation.SEARCH_CATEGORY_ARGUMENT
import com.example.axs_player.navigation.SEARCH_QUERY_ARGUMENT
import com.example.axs_player.navigation.TRACK_ID_ARGUMENT
import com.example.axs_player.navigation.Screen
import com.example.axs_player.ui.components.BottomNavigationBar
import com.example.axs_player.ui.player.MiniPlayerBar
import com.example.axs_player.ui.player.PlayerScreen
import com.example.axs_player.ui.screens.CatalogScreen
import com.example.axs_player.ui.screens.HomeScreen
import com.example.axs_player.ui.screens.MoreScreen
import com.example.axs_player.ui.screens.ScanScreen
import com.example.axs_player.ui.screens.SearchScreen
import com.example.axs_player.ui.screens.catalog.AlbumScreen
import com.example.axs_player.ui.screens.catalog.AlbumEditScreen
import com.example.axs_player.ui.screens.catalog.AlbumTextureEditScreen
import com.example.axs_player.ui.screens.catalog.AlbumEditViewModel
import com.example.axs_player.ui.screens.catalog.AlbumDescriptionScreen
import com.example.axs_player.ui.screens.catalog.AlbumDescriptionEditScreen
import com.example.axs_player.ui.screens.catalog.TrackEditScreen
import com.example.axs_player.ui.screens.catalog.TrackDescriptionEditScreen
import com.example.axs_player.ui.screens.catalog.TrackLyricsEditScreen
import com.example.axs_player.ui.screens.catalog.AlbumsScreen
import com.example.axs_player.ui.screens.catalog.ArtistBioEditScreen
import com.example.axs_player.ui.screens.catalog.ArtistBioScreen
import com.example.axs_player.ui.screens.catalog.ArtistEditScreen
import com.example.axs_player.ui.screens.catalog.ArtistItemsScreen
import com.example.axs_player.ui.screens.catalog.ArtistScreen
import com.example.axs_player.ui.screens.catalog.ArtistsScreen
import com.example.axs_player.ui.screens.catalog.FavoriteSongsScreen
import com.example.axs_player.ui.screens.catalog.ListeningHistoryScreen
import com.example.axs_player.ui.screens.catalog.LivesScreen
import com.example.axs_player.ui.screens.catalog.LiveTrackScreen
import com.example.axs_player.ui.screens.catalog.PlaylistScreen
import com.example.axs_player.ui.screens.catalog.PlaylistsScreen
import com.example.axs_player.ui.screens.more.DataSavingScreen
import com.example.axs_player.ui.screens.more.PlaybackScreen
import com.example.axs_player.ui.screens.search.SearchResultsScreen
import com.example.axs_player.ui.theme.Axs_playerTheme

private fun NavController.isShowing(route: String, argument: String, value: String): Boolean =
    currentBackStackEntry?.let { it.destination.route == route && it.arguments?.getString(argument) == value } == true

class MainActivity : ComponentActivity() {
    override fun onStop() {
        (application as AxsApplication).savePortableLibrary()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val importer = (application as AxsApplication).libraryImporter
        val isScanStarted = savedInstanceState == null && importer.start()
        if (!isScanStarted) importer.ensureFiles()
        enableEdgeToEdge()
        setContent {
            Axs_playerTheme {
                val startRoute = rememberSaveable { if (isScanStarted) Screen.Scan.route else Screen.Home.route }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: startRoute
                var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
                val layoutDirection = LocalLayoutDirection.current

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (currentRoute != Screen.Scan.route) {
                                Column {
                                    MiniPlayerBar(onExpand = { isPlayerExpanded = true })
                                    BottomNavigationBar(
                                        currentRoute = currentRoute,
                                        onNavigate = { screen ->
                                            if (currentRoute == screen.route) {
                                                return@BottomNavigationBar
                                            }
                                            navController.navigate(screen.route) {
                                                popUpTo(screen.route) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startRoute,
                            modifier = Modifier.padding(
                                start = innerPadding.calculateStartPadding(layoutDirection),
                                top = if (currentRoute == Screen.Album.route || currentRoute == Screen.AlbumDescription.route) 0.dp else innerPadding.calculateTopPadding(),
                                end = innerPadding.calculateEndPadding(layoutDirection),
                                bottom = innerPadding.calculateBottomPadding()
                            ),
                            enterTransition = {
                                (slideInHorizontally(
                                    initialOffsetX = { it / 3 },
                                    animationSpec = tween(280)
                                ) + fadeIn(tween(280)) + scaleIn(
                                    initialScale = 0.97f,
                                    animationSpec = tween(280)
                                ))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 4 },
                                    animationSpec = tween(220)
                                ) + fadeOut(tween(180))
                            },
                            popEnterTransition = {
                                (slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = tween(280)
                                ) + fadeIn(tween(280)) + scaleIn(
                                    initialScale = 0.97f,
                                    animationSpec = tween(280)
                                ))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 4 },
                                    animationSpec = tween(220)
                                ) + fadeOut(tween(180))
                            }
                        ) {
                            composable(Screen.Scan.route) {
                                ScanScreen(
                                    onLeave = {
                                        if (navController.currentDestination?.route == Screen.Scan.route) {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Scan.route) {
                                                    inclusive = true
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onOpenPlaylist = { navController.navigate(Screen.Playlist.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) }
                                )
                            }
                            composable(Screen.Search.route) {
                                SearchScreen(
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) },
                                    onViewAll = { category, query ->
                                        navController.navigate(Screen.SearchResults.route(category, query))
                                    }
                                )
                            }
                            composable(
                                route = Screen.SearchResults.route,
                                arguments = listOf(
                                    navArgument(SEARCH_CATEGORY_ARGUMENT) { type = NavType.StringType },
                                    navArgument(SEARCH_QUERY_ARGUMENT) {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    }
                                )
                            ) {
                                SearchResultsScreen(
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) },
                                    onEditTrack = { navController.navigate(Screen.TrackEdit.route(it)) }
                                )
                            }
                            composable(Screen.Catalog.route) {
                                CatalogScreen(navController = navController)
                            }
                            composable(Screen.More.route) {
                                MoreScreen(navController = navController)
                            }
                            composable(Screen.Playback.route) {
                                PlaybackScreen()
                            }
                            composable(Screen.DataSaving.route) {
                                DataSavingScreen()
                            }
                            composable(Screen.FavoriteSongs.route) {
                                FavoriteSongsScreen(
                                    onEditTrack = { navController.navigate(Screen.TrackEdit.route(it)) },
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) }
                                )
                            }
                            composable(Screen.Lives.route) {
                                LivesScreen(onOpenTrack = { navController.navigate(Screen.LiveTrack.route(it)) })
                            }
                            composable(
                                route = Screen.LiveTrack.route,
                                arguments = listOf(navArgument(TRACK_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                LiveTrackScreen(
                                    onEditTrack = { navController.navigate(Screen.TrackEdit.route(it)) },
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) }
                                )
                            }
                            composable(Screen.Albums.route) {
                                AlbumsScreen(onAlbumClick = { navController.navigate(Screen.Album.route(it)) })
                            }
                            composable(
                                route = Screen.Album.route,
                                arguments = listOf(navArgument(ALBUM_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                val albumId = it.arguments?.getString(ALBUM_ID_ARGUMENT).orEmpty()
                                AlbumScreen(
                                    onEdit = { navController.navigate(Screen.AlbumEdit.route(albumId)) },
                                    onOpenDescription = { navController.navigate(Screen.AlbumDescription.route(albumId)) },
                                    onEditTrack = { navController.navigate(Screen.TrackEdit.route(it)) },
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) }
                                )
                            }
                            composable(
                                route = Screen.AlbumDescription.route,
                                arguments = listOf(navArgument(ALBUM_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                AlbumDescriptionScreen()
                            }
                            composable(
                                route = Screen.AlbumEdit.route,
                                arguments = listOf(navArgument(ALBUM_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                val albumId = it.arguments?.getString(ALBUM_ID_ARGUMENT).orEmpty()
                                AlbumEditScreen(
                                    onSaved = { navController.popBackStack() },
                                    onEditDescription = { navController.navigate(Screen.AlbumDescriptionEdit.route(albumId)) },
                                    onEditTexture = { navController.navigate(Screen.AlbumTextureEdit.route(albumId)) }
                                )
                            }
                            composable(
                                route = Screen.AlbumTextureEdit.route,
                                arguments = listOf(navArgument(ALBUM_ID_ARGUMENT) { type = NavType.StringType })
                            ) { entry ->
                                val albumId = entry.arguments?.getString(ALBUM_ID_ARGUMENT).orEmpty()
                                val editEntry = remember(entry) { navController.getBackStackEntry(Screen.AlbumEdit.route(albumId)) }
                                val editViewModel: AlbumEditViewModel = viewModel(editEntry, factory = AlbumEditViewModel.Factory)
                                AlbumTextureEditScreen(onBack = { navController.popBackStack() }, viewModel = editViewModel)
                            }
                            composable(
                                route = Screen.AlbumDescriptionEdit.route,
                                arguments = listOf(navArgument(ALBUM_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                AlbumDescriptionEditScreen(onSaved = { navController.popBackStack() })
                            }
                            composable(
                                route = Screen.TrackEdit.route,
                                arguments = listOf(navArgument(TRACK_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                val trackId = it.arguments?.getString(TRACK_ID_ARGUMENT).orEmpty()
                                TrackEditScreen(
                                    onSaved = { navController.popBackStack() },
                                    onEditDescription = { navController.navigate(Screen.TrackDescriptionEdit.route(trackId)) },
                                    onEditLyrics = { navController.navigate(Screen.TrackLyricsEdit.route(trackId)) }
                                )
                            }
                            composable(
                                route = Screen.TrackDescriptionEdit.route,
                                arguments = listOf(navArgument(TRACK_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                TrackDescriptionEditScreen(onSaved = { navController.popBackStack() })
                            }
                            composable(
                                route = Screen.TrackLyricsEdit.route,
                                arguments = listOf(navArgument(TRACK_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                TrackLyricsEditScreen(onSaved = { navController.popBackStack() })
                            }
                            composable(Screen.Playlists.route) {
                                PlaylistsScreen(onPlaylistClick = { navController.navigate(Screen.Playlist.route(it)) })
                            }
                            composable(
                                route = Screen.Playlist.route,
                                arguments = listOf(navArgument(PLAYLIST_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                PlaylistScreen(
                                    onEditTrack = { navController.navigate(Screen.TrackEdit.route(it)) },
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) }
                                )
                            }
                            composable(Screen.Artists.route) {
                                ArtistsScreen(onArtistClick = { navController.navigate(Screen.Artist.route(it)) })
                            }
                            composable(
                                route = Screen.Artist.route,
                                arguments = listOf(navArgument(ARTIST_ID_ARGUMENT) { type = NavType.StringType })
                            ) { entry ->
                                val artistId = entry.arguments?.getString(ARTIST_ID_ARGUMENT).orEmpty()
                                ArtistScreen(
                                    onEdit = { navController.navigate(Screen.ArtistEdit.route(artistId)) },
                                    onOpenBio = { navController.navigate(Screen.ArtistBio.route(artistId)) },
                                    onOpenAlbum = { navController.navigate(Screen.Album.route(it)) },
                                    onEditTrack = { navController.navigate(Screen.TrackEdit.route(it)) },
                                    onOpenArtist = { navController.navigate(Screen.Artist.route(it)) },
                                    onSeeAll = { category ->
                                        navController.navigate(Screen.ArtistItems.route(artistId, category))
                                    }
                                )
                            }
                            composable(
                                route = Screen.ArtistBio.route,
                                arguments = listOf(navArgument(ARTIST_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                ArtistBioScreen()
                            }
                            composable(
                                route = Screen.ArtistEdit.route,
                                arguments = listOf(navArgument(ARTIST_ID_ARGUMENT) { type = NavType.StringType })
                            ) { entry ->
                                val artistId = entry.arguments?.getString(ARTIST_ID_ARGUMENT).orEmpty()
                                ArtistEditScreen(
                                    onEditBio = { navController.navigate(Screen.ArtistBioEdit.route(artistId)) },
                                    onSaved = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = Screen.ArtistBioEdit.route,
                                arguments = listOf(navArgument(ARTIST_ID_ARGUMENT) { type = NavType.StringType })
                            ) {
                                ArtistBioEditScreen(onSaved = { navController.popBackStack() })
                            }
                            composable(
                                route = Screen.ArtistItems.route,
                                arguments = listOf(
                                    navArgument(ARTIST_ID_ARGUMENT) { type = NavType.StringType },
                                    navArgument(ARTIST_CATEGORY_ARGUMENT) { type = NavType.StringType }
                                )
                            ) {
                                ArtistItemsScreen(onOpenAlbum = { navController.navigate(Screen.Album.route(it)) })
                            }
                            composable(Screen.ListeningHistory.route) {
                                ListeningHistoryScreen()
                            }
                        }
                    }
                    if (isPlayerExpanded) {
                        PlayerScreen(
                            onCollapse = { isPlayerExpanded = false },
                            onEditTrack = { trackId ->
                                isPlayerExpanded = false
                                navController.navigate(Screen.TrackEdit.route(trackId))
                            },
                            onOpenArtist = { artistId ->
                                isPlayerExpanded = false
                                if (!navController.isShowing(Screen.Artist.route, ARTIST_ID_ARGUMENT, artistId)) {
                                    navController.navigate(Screen.Artist.route(artistId))
                                }
                            },
                            onOpenAlbum = { albumId ->
                                isPlayerExpanded = false
                                if (!navController.isShowing(Screen.Album.route, ALBUM_ID_ARGUMENT, albumId)) {
                                    navController.navigate(Screen.Album.route(albumId))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
