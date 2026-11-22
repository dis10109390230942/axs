package com.example.axs_player.data.library

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import com.example.axs_player.data.db.LibraryDatabase
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val LOG_TAG = "PortableLibraryStore"
private const val DATABASE_FILE = "library.db"
private const val COVERS_DIRECTORY = "covers"
private const val ARTISTS_DIRECTORY = "artists"
private const val ALBUM_MEDIA_DIRECTORY = "album_media"
private const val ARTIST_MEDIA_DIRECTORY = "artist_media"

class PortableLibraryStore(
    private val context: Context,
    private val folder: LibraryFolder,
    private val database: LibraryDatabase,
    private val images: ImageStore,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var exportJob: Job? = null

    fun scheduleExport() {
        exportJob?.cancel()
        exportJob = scope.launch(Dispatchers.IO) {
            delay(400)
            runCatching { export() }
                .onFailure { Log.e(LOG_TAG, "Не удалось синхронизировать библиотеку", it) }
        }
    }

    fun cancelPendingExport() {
        exportJob?.cancel()
        exportJob = null
    }

    suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val layout = folder.layout() ?: return@withLock false
            val source = File(context.cacheDir, "portable-library-restore.db")
            if (!folder.readMetadataFile(layout, DATABASE_FILE, source)) return@withLock false
            val restored = runCatching {
                val db = database.openHelper.writableDatabase
                db.execSQL("ATTACH DATABASE ? AS portable", arrayOf(source.absolutePath))
                try {
                    db.beginTransaction()
                    db.execSQL("PRAGMA foreign_keys = OFF")
                    listOf("play_events", "playlist_items", "album_tracks", "track_artists", "album_artists", "playlists", "tracks", "albums", "artists").forEach { db.execSQL("DELETE FROM $it") }
                    listOf("artists", "albums", "tracks", "playlists", "album_artists", "track_artists", "album_tracks", "playlist_items", "play_events").forEach { table ->
                        val targetColumns = db.query("PRAGMA main.table_info($table)").use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(1)) } }
                        val sourceColumns = db.query("PRAGMA portable.table_info($table)").use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) } }
                        val commonColumns = targetColumns.filter { it in sourceColumns }
                        val columns = commonColumns.joinToString(",") { "`$it`" }
                        db.execSQL("INSERT INTO main.$table ($columns) SELECT $columns FROM portable.$table")
                    }
                    db.setTransactionSuccessful()
                    db.endTransaction()
                } finally {
                    db.execSQL("PRAGMA foreign_keys = ON")
                    db.execSQL("DETACH DATABASE portable")
                }
                val covers = File(context.filesDir, COVERS_DIRECTORY)
                val artists = File(context.filesDir, ARTISTS_DIRECTORY)
                val albumMedia = File(context.filesDir, ALBUM_MEDIA_DIRECTORY)
                val artistMedia = File(context.filesDir, ARTIST_MEDIA_DIRECTORY)
                val albumTextures = File(context.filesDir, ALBUM_TEXTURES_DIRECTORY)
                covers.deleteRecursively()
                artists.deleteRecursively()
                albumMedia.deleteRecursively()
                artistMedia.deleteRecursively()
                albumTextures.deleteRecursively()
                folder.copyMetadataDirectoryTo(layout, COVERS_DIRECTORY, covers)
                folder.copyMetadataDirectoryTo(layout, ARTISTS_DIRECTORY, artists)
                folder.copyMetadataDirectoryTo(layout, ALBUM_MEDIA_DIRECTORY, albumMedia)
                folder.copyMetadataDirectoryTo(layout, ARTIST_MEDIA_DIRECTORY, artistMedia)
                folder.copyMetadataDirectoryTo(layout, ALBUM_TEXTURES_DIRECTORY, albumTextures)
                images.reloadArtistPhotos()
                true
            }.onFailure { Log.e(LOG_TAG, "Не удалось восстановить библиотеку", it) }.getOrDefault(false)
            source.delete()
            restored
        }
    }

    suspend fun export() = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val layout = folder.layout() ?: return@runCatching
                folder.ensureMetadataHiddenFromMediaScanners(layout)
                val snapshot = File(context.cacheDir, "portable-library-export.db")
                snapshot.delete()
                database.openHelper.writableDatabase.execSQL("VACUUM INTO ?", arrayOf(snapshot.absolutePath))
                folder.writeMetadataFile(layout, DATABASE_FILE, snapshot)
                folder.replaceMetadataDirectory(layout, COVERS_DIRECTORY, File(context.filesDir, COVERS_DIRECTORY))
                folder.replaceMetadataDirectory(layout, ARTISTS_DIRECTORY, File(context.filesDir, ARTISTS_DIRECTORY))
                folder.replaceMetadataDirectory(layout, ALBUM_MEDIA_DIRECTORY, File(context.filesDir, ALBUM_MEDIA_DIRECTORY))
                folder.replaceMetadataDirectory(layout, ARTIST_MEDIA_DIRECTORY, File(context.filesDir, ARTIST_MEDIA_DIRECTORY))
                folder.replaceMetadataDirectory(layout, ALBUM_TEXTURES_DIRECTORY, File(context.filesDir, ALBUM_TEXTURES_DIRECTORY))
                snapshot.delete()
            }.onFailure { Log.e(LOG_TAG, "Не удалось сохранить переносимую библиотеку", it) }
        }
    }
}
