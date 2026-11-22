package com.example.axs_player

import android.app.Application
import android.content.Intent
import androidx.room.InvalidationTracker
import com.example.axs_player.data.db.LibraryDatabase
import com.example.axs_player.data.library.AudioTagReader
import com.example.axs_player.data.library.ImageStore
import com.example.axs_player.data.library.LibraryFolder
import com.example.axs_player.data.library.LibraryImporter
import com.example.axs_player.data.library.PortableLibraryStore
import com.example.axs_player.data.search.LibrarySearch
import com.example.axs_player.data.search.SearchHistoryStore
import com.example.axs_player.data.stats.ListeningStats
import com.example.axs_player.playback.LibraryPlayback
import com.example.axs_player.playback.PlayHistoryRecorder
import com.example.axs_player.playback.PlaybackController
import com.example.axs_player.playback.PlaybackQueueStore
import com.example.axs_player.playback.PlaybackService
import com.example.axs_player.playback.PlaybackSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val QUEUE_SAVE_INTERVAL_MS = 10_000L

class AxsApplication : Application() {

    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var instance: AxsApplication? = null
            @Suppress("UNUSED_PARAMETER")
            set(value) { field = value }
        
        fun getInstance(): AxsApplication = instance!!
    }

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database: LibraryDatabase by lazy { LibraryDatabase.create(this) }
    val libraryFolder: LibraryFolder by lazy { LibraryFolder(this) }
    val imageStore: ImageStore by lazy { ImageStore(this) }
    val containerSync: com.example.axs_player.data.library.AxsContainerSync by lazy {
        com.example.axs_player.data.library.AxsContainerSync(this, libraryFolder, database.libraryDao(), imageStore)
    }
    val portableLibrary: PortableLibraryStore by lazy {
        PortableLibraryStore(this, libraryFolder, database, imageStore, applicationScope)
    }
    val libraryImporter: LibraryImporter by lazy {
        LibraryImporter(database, libraryFolder, imageStore, AudioTagReader(this), applicationScope, this)
    }
    val librarySearch: LibrarySearch by lazy { LibrarySearch(database.libraryDao(), applicationScope) }
    val listeningStats: ListeningStats by lazy { ListeningStats(database.statsDao()) }
    val searchHistory: SearchHistoryStore by lazy { SearchHistoryStore(this) }
    val playbackQueueStore: PlaybackQueueStore by lazy { PlaybackQueueStore(this) }
    val playbackSettings: PlaybackSettingsStore by lazy { PlaybackSettingsStore(this) }
    val playbackController: PlaybackController by lazy {
        PlaybackController(this).also { controller ->
            controller.applySettings(playbackSettings.settings.value)
            applicationScope.launch(Dispatchers.Main) {
                launch { playbackSettings.settings.collect(controller::applySettings) }
                playbackQueueStore.load()?.let { libraryPlayback.withActualArtwork(it) }?.let(controller::restore)
                launch { startServiceOnPlayback(controller) }
                launch { PlayHistoryRecorder(controller, database.statsDao()).run() }
                saveQueueContinuously(controller)
            }
        }
    }
    val libraryPlayback: LibraryPlayback by lazy {
        LibraryPlayback(libraryFolder, imageStore, database.libraryDao(), playbackController)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        imageStore.onChanged = { portableLibrary.scheduleExport() }
        database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(
            "tracks", "albums", "artists", "album_tracks", "track_artists", "album_artists",
            "playlists", "playlist_items", "play_events"
        ) {
            override fun onInvalidated(tables: Set<String>) {
                portableLibrary.scheduleExport()
            }
        })
        if (libraryFolder.treeUri.value != null) {
            applicationScope.launch(Dispatchers.IO) {
                runCatching { portableLibrary.export() }
                    .onFailure { android.util.Log.e("AxsApplication", "Не удалось подготовить переносимую библиотеку", it) }
            }
        }
    }

    fun savePortableLibrary() {
        portableLibrary.scheduleExport()
    }

    private suspend fun startServiceOnPlayback(controller: PlaybackController) {
        controller.state
            .map { it.isPlaying }
            .distinctUntilChanged()
            .filter { it }
            .collect { startService(Intent(this, PlaybackService::class.java)) }
    }

    private suspend fun saveQueueContinuously(controller: PlaybackController) {
        controller.state.collectLatest { state ->
            playbackQueueStore.save(controller.snapshot())
            while (state.isPlaying) {
                delay(QUEUE_SAVE_INTERVAL_MS)
                playbackQueueStore.save(controller.snapshot())
            }
        }
    }
}
