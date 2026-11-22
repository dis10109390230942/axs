package com.example.axs_player.data.library

import android.content.Context
import android.util.Log
import com.example.axs_player.data.db.LibraryDao
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val LOG_TAG = "AxsContainerSync"

/**
 * Handles reading from and atomic save-back to .axs album containers.
 * The .axs file in `axs_data/axs_containers/` is the single source of truth for the album.
 */
class AxsContainerSync(
    private val context: Context,
    private val folder: LibraryFolder,
    private val dao: LibraryDao,
    private val images: ImageStore
) {

    /**
     * Re-assembles and atomically rewrites the .axs container for [albumId] if it is container-owned.
     * Returns true if saved successfully, or false if not container-owned or failed.
     */
    suspend fun saveBackAlbum(albumId: String): Boolean = withContext(Dispatchers.IO) {
        val album = dao.albumEntity(albumId) ?: return@withContext false
        if (!album.isContainerOwned || album.containerFileName == null) {
            return@withContext false
        }

        val layout = folder.layout() ?: return@withContext false
        val containerName = album.containerFileName

        // Load existing container JSON to preserve any custom/unknown extra fields
        val cachedContainerFile = File(context.cacheDir, "read_$containerName")
        val preservedRawJson: JSONObject? = try {
            if (folder.copyContainerTo(layout, containerName, cachedContainerFile)) {
                AxsContainer.read(cachedContainerFile).rawJson
            } else null
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Не удалось прочитать исходный манифест контейнера для сохранения неизвестных полей", e)
            null
        } finally {
            if (cachedContainerFile.exists()) cachedContainerFile.delete()
        }

        val albumArtists = dao.artistNamesForAlbum(albumId)
        val albumTracks = dao.albumTracksForAlbum(albumId)
        val trackEntities = dao.tracksInAlbum(albumId).associateBy { it.id }

        // Gather cover
        val coverSource: AxsWriteSource? = album.coverFileName?.let { coverName ->
            val file = images.getCoverFile(coverName)
            if (file.exists()) {
                AxsWriteSource(
                    name = "cover.${file.extension.ifEmpty { "jpg" }}",
                    mime = if (file.extension.equals("png", ignoreCase = true)) "image/png" else "image/jpeg",
                    openStream = { file.inputStream() }
                )
            } else null
        }

        // Gather texture
        val textureSource: AxsWriteSource? = album.backgroundTextureFileName?.let { texName ->
            val file = images.getTextureFile(texName)
            if (file.exists()) {
                AxsWriteSource(
                    name = "background.${file.extension.ifEmpty { "png" }}",
                    mime = if (file.extension.equals("jpg", ignoreCase = true) || file.extension.equals("jpeg", ignoreCase = true)) "image/jpeg" else "image/png",
                    openStream = { file.inputStream() }
                )
            } else null
        }

        // Gather extra photos
        val photos = images.albumPhotos(albumId).mapIndexedNotNull { idx, uri ->
            val fileName = uri.lastPathSegment ?: return@mapIndexedNotNull null
            val file = images.getPhotoFile(albumId, fileName)
            if (file.exists()) {
                AxsWriteSource(
                    name = "photo_${idx + 1}.${file.extension.ifEmpty { "jpg" }}",
                    mime = "image/jpeg",
                    openStream = { file.inputStream() }
                )
            } else null
        }

        // Gather extra media
        val mediaFiles = images.albumMedia(albumId).mapIndexedNotNull { idx, media ->
            val file = images.getMediaFile(albumId, media.fileName)
            if (file.exists()) {
                AxsWriteSource(
                    name = media.fileName,
                    mime = if (media.isVideo) "video/mp4" else "image/jpeg",
                    openStream = { file.inputStream() }
                )
            } else null
        }

        // Build track write data
        val trackWriteDataList = mutableListOf<AxsTrackWriteData>()
        for (link in albumTracks) {
            val track = trackEntities[link.trackId] ?: continue
            val trackArtistDetails = dao.trackArtistDetails(track.id)
            val artists = trackArtistDetails.map { it.name }
            val artistCredits = trackArtistDetails.map { AxsArtistCredit(it.name, it.role.name) }

            // Open stream from SAF or cached track file
            val openMediaStream: () -> InputStream = {
                val uri = folder.trackUris.value[track.fileName]
                if (uri != null) {
                    context.contentResolver.openInputStream(uri)
                        ?: error("Не удалось открыть поток трека ${track.fileName}")
                } else {
                    error("Файл трека ${track.fileName} не найден")
                }
            }

            trackWriteDataList.add(
                AxsTrackWriteData(
                    id = track.id,
                    number = link.position,
                    discNumber = link.discNumber,
                    title = track.title,
                    subtitle = track.subtitle,
                    artists = artists,
                    artistCredits = artistCredits,
                    date = track.trackDate,
                    year = track.year,
                    description = track.description,
                    lyrics = track.lyrics,
                    duration = track.durationMs,
                    composer = track.composer,
                    hasVideo = track.hasVideo,
                    parentTrackId = track.parentTrackId,
                    playbackVolume = track.playbackVolume,
                    mediaSource = AxsWriteSource(
                        name = track.fileName,
                        mime = track.mimeType ?: "audio/mpeg",
                        openStream = openMediaStream
                    )
                )
            )
        }

        val albumWriteData = AxsAlbumWriteData(
            id = album.id,
            title = album.title,
            artists = albumArtists,
            artistCredits = albumArtists.map { AxsArtistCredit(it, "MAIN") },
            year = album.year,
            discNumber = 1,
            genre = null,
            description = album.description,
            coverSource = coverSource,
            photoSources = photos,
            mediaFileSources = mediaFiles,
            backgroundTextureSource = textureSource,
            backgroundTextureScale = album.backgroundTextureScale,
            backgroundTextureFit = album.backgroundTextureFit,
            backgroundTextureOffsetX = album.backgroundTextureOffsetX,
            backgroundTextureOffsetY = album.backgroundTextureOffsetY,
            backgroundTextureBlendMode = album.backgroundTextureBlendMode,
            backgroundTextureKeyColor = album.backgroundTextureKeyColor,
            backgroundTextureTolerance = album.backgroundTextureTolerance,
            backgroundTextureOpacity = album.backgroundTextureOpacity,
            tracks = trackWriteDataList,
            preservedRawJson = preservedRawJson
        )

        val tempTarget = File(context.cacheDir, "axs_write_${System.currentTimeMillis()}.axs")
        val isSaved = try {
            AxsContainer.writeContainer(tempTarget, albumWriteData)
            folder.importContainer(layout, tempTarget, containerName)
            dao.updateAlbumSyncState(albumId, "synced")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Ошибка сохранения контейнера $containerName", e)
            dao.updateAlbumSyncState(albumId, "error: ${e.message}")
            false
        } finally {
            if (tempTarget.exists()) tempTarget.delete()
        }

        isSaved
    }

    /**
     * Triggered when a track's metadata/volume/lyrics change: finds which container-owned album
     * it belongs to and updates the container.
     */
    suspend fun saveBackTrackAlbum(trackId: String): Boolean {
        val albumId = dao.firstAlbumIdForTrack(trackId) ?: return false
        return saveBackAlbum(albumId)
    }
}
