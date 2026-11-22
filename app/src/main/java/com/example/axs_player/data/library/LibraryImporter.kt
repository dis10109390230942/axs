package com.example.axs_player.data.library

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.example.axs_player.data.db.AlbumArtistEntity
import com.example.axs_player.data.db.AlbumEntity
import com.example.axs_player.data.db.AlbumTrackEntity
import com.example.axs_player.data.db.ArtistEntity
import com.example.axs_player.data.db.ArtistRole
import com.example.axs_player.data.db.LibraryDatabase
import com.example.axs_player.data.db.TrackArtistEntity
import com.example.axs_player.data.db.TrackEntity
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.StringWriter
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_TAG = "LibraryImporter"
private const val LEGACY_COVERS_DIRECTORY = "images"
private const val LEGACY_ARTIST_PHOTOS_DIRECTORY = "artists"
private val WHITESPACE = Regex("\\s+")
private val FEATURING_SEPARATOR = Regex("\\s+(?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE)

private class Credits(val main: List<String>, val featured: List<String>)

class LibraryImporter(
    private val database: LibraryDatabase,
    private val folder: LibraryFolder,
    private val images: ImageStore,
    private val tagReader: AudioTagReader,
    private val scope: CoroutineScope,
    private val context: Context
) {

    private val dao = database.libraryDao()
    private var job: Job? = null

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun start(): Boolean {
        if (job?.isActive == true) return true
        if (folder.treeUri.value == null) return false
        _state.value = ImportState.Running(processed = 0, total = 0)
        job = scope.launch(Dispatchers.IO) {
            _state.value = runCatching { scan() }.fold(
                onSuccess = { ImportState.Finished(it) },
                onFailure = {
                    Log.e(LOG_TAG, "Сканирование папки библиотеки завершилось ошибкой", it)
                    ImportState.Failed
                }
            )
        }
        return true
    }

    fun ensureFiles() {
        if (job?.isActive == true || folder.trackUris.value.isNotEmpty()) return
        scope.launch(Dispatchers.IO) {
            runCatching { folder.layout()?.let { folder.refreshFiles(it) } }
                .onFailure { Log.e(LOG_TAG, "Не удалось прочитать папку библиотеки", it) }
        }
    }

    /**
     * Imports or restores an .axs container from a seekable file or URI.
     * Guarantees idempotency: updates existing albums/tracks if they were imported previously.
     */
    suspend fun importAxs(uri: Uri, targetContainerFileName: String? = null): Int = withContext(Dispatchers.IO) {
        val layout = folder.layout() ?: error("Сначала выберите папку библиотеки")
        val source = File.createTempFile("axs_import_", ".axs")
        try {
            folder.copyUriTo(uri, source)
            val container = AxsContainer.read(source)
            val now = System.currentTimeMillis()

            val safeContainerName = targetContainerFileName
                ?: "${container.title.trim().ifEmpty { "album" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")}.axs"

            // Idempotency: find existing album by container filename, stable container ID, or title
            val existingAlbum = dao.findAlbumByContainerFileName(safeContainerName)
                ?: (container.id?.let { dao.albumEntity(it) })

            val albumId = existingAlbum?.id ?: container.id ?: UUID.randomUUID().toString()

            val albumArtistIds = if (container.artistCredits.isNotEmpty()) {
                container.artistCredits.map { credit ->
                    artistId(credit.name, now) to (if (credit.role.equals("FEATURED", ignoreCase = true)) ArtistRole.FEATURED else ArtistRole.MAIN)
                }
            } else {
                container.artists.map { artistId(it, now) to ArtistRole.MAIN }
            }

            // Extract cover
            val coverName = container.cover?.let { blob ->
                val image = File.createTempFile("axs_cover_", ".img")
                try {
                    AxsContainer.extract(source, blob, image)
                    images.saveCover(albumId, image.readBytes())
                } finally {
                    image.delete()
                }
            } ?: existingAlbum?.coverFileName

            // Extract texture if present
            val textureName = container.backgroundTexture?.let { blob ->
                val ext = blob.name.substringAfterLast('.', "png")
                val tempImg = File.createTempFile("axs_tex_", ".$ext")
                try {
                    AxsContainer.extract(source, blob, tempImg)
                    images.saveAlbumTexture(albumId, android.net.Uri.fromFile(tempImg))
                } finally {
                    tempImg.delete()
                }
            } ?: existingAlbum?.backgroundTextureFileName

            // Extract additional photos
            container.photos.forEach { blob ->
                val ext = blob.name.substringAfterLast('.', "jpg")
                val tempImg = File.createTempFile("axs_photo_", ".$ext")
                try {
                    AxsContainer.extract(source, blob, tempImg)
                    images.saveAlbumPhoto(albumId, android.net.Uri.fromFile(tempImg))
                } finally {
                    tempImg.delete()
                }
            }

            // Extract additional media files
            container.mediaFiles.forEach { blob ->
                val ext = blob.name.substringAfterLast('.', "bin")
                val tempFile = File.createTempFile("axs_media_", ".$ext")
                try {
                    AxsContainer.extract(source, blob, tempFile)
                    images.saveAlbumMedia(albumId, android.net.Uri.fromFile(tempFile))
                } finally {
                    tempFile.delete()
                }
            }

            database.withTransaction {
                // Save manifest JSON for fast container track startup
                val manifestJson = try {
                    val writer = java.io.StringWriter()
                    val jsonWriter = com.google.gson.stream.JsonWriter(writer)
                    jsonWriter.beginObject()
                    jsonWriter.name("format").value("axs")
                    jsonWriter.name("version").value(1)
                    jsonWriter.name("album").beginObject()
                    jsonWriter.name("id").value(container.id ?: "")
                    jsonWriter.name("title").value(container.title)
                    jsonWriter.name("artists").beginArray()
                    container.artists.forEach { jsonWriter.value(it) }
                    jsonWriter.endArray()
                    jsonWriter.name("year").value(container.year ?: 0)
                    jsonWriter.name("description").value(container.description ?: "")
                    jsonWriter.name("text_color_mode").value(container.textColorMode)
                    jsonWriter.endObject()
                    jsonWriter.name("tracks").beginArray()
                    container.tracks.forEach { track ->
                        jsonWriter.beginObject()
                        jsonWriter.name("id").value(track.id ?: "")
                        jsonWriter.name("number").value(track.number ?: 0)
                        jsonWriter.name("title").value(track.title)
                        jsonWriter.name("artists").beginArray()
                        track.artists.forEach { jsonWriter.value(it) }
                        jsonWriter.endArray()
                        track.duration?.let { jsonWriter.name("duration_ms").value(it) }
                        jsonWriter.name("media").beginObject()
                        jsonWriter.name("file_name").value(track.media.name)
                        jsonWriter.name("mime_type").value(track.media.mime)
                        jsonWriter.name("offset").value(track.media.offset)
                        jsonWriter.name("length").value(track.media.length)
                        jsonWriter.endObject()
                        jsonWriter.endObject()
                    }
                    jsonWriter.endArray()
                    jsonWriter.endObject()
                    jsonWriter.close()
                    writer.toString()
                } catch (e: Exception) {
                    ""
                }
                
                val albumEntity = AlbumEntity(
                    id = albumId,
                    title = container.title,
                    year = container.year,
                    description = container.description,
                    coverFileName = coverName,
                    backgroundTextureFileName = textureName,
                    backgroundTextureScale = container.backgroundTextureScale,
                    backgroundTextureFit = container.backgroundTextureFit,
                    backgroundTextureOffsetX = container.backgroundTextureOffsetX,
                    backgroundTextureOffsetY = container.backgroundTextureOffsetY,
                    backgroundTextureBlendMode = container.backgroundTextureBlendMode,
                    backgroundTextureKeyColor = container.backgroundTextureKeyColor,
                    backgroundTextureTolerance = container.backgroundTextureTolerance,
                    backgroundTextureOpacity = container.backgroundTextureOpacity,
                    textColorMode = container.textColorMode,
                    createdAt = existingAlbum?.createdAt ?: now,
                    containerFileName = safeContainerName,
                    isContainerOwned = true,
                    isContainerOnly = true,
                    containerSyncState = "synced",
                    containerManifestJson = manifestJson
                )

                if (existingAlbum != null) {
                    dao.updateAlbum(albumEntity)
                    dao.deleteAlbumArtists(albumId)
                    dao.deleteAlbumTracks(albumId)
                } else {
                    dao.insertAlbum(albumEntity)
                }

                dao.insertAlbumArtists(
                    albumArtistIds.mapIndexed { i, (artistId, _) -> AlbumArtistEntity(albumId, artistId, i) }
                )

                container.tracks.forEachIndexed { index, track ->
                    val ext = track.media.name.substringAfterLast('.', "bin")
                    val temp = File.createTempFile("axs_track_", ".$ext")
                    try {
                        AxsContainer.extract(source, track.media, temp)
                        val hash = MessageDigest.getInstance("SHA-256").digest(temp.readBytes()).joinToString("") { "%02x".format(it) }

                        // Check if track already exists by hash or ID
                        val existingTrack = (track.id?.let { dao.findTrackById(it) })
                            ?: dao.findTrackByContentHash(hash)

                        val trackId = existingTrack?.id ?: track.id ?: UUID.randomUUID().toString()
                        // For container-only albums, track fileName is the blob name in the container
                        val target = existingTrack?.fileName ?: track.media.name

                        // For container-only albums, DO NOT import track to tracks folder - it stays in container
                        if (!albumEntity.isContainerOnly) {
                            if (existingTrack == null || folder.trackUris.value[existingTrack.fileName] == null) {
                                folder.importMedia(layout, temp, target)
                            }
                        }

                        val trackCredits = if (track.artistCredits.isNotEmpty()) {
                            track.artistCredits.map { credit ->
                                artistId(credit.name, now) to (if (credit.role.equals("FEATURED", ignoreCase = true)) ArtistRole.FEATURED else ArtistRole.MAIN)
                            }
                        } else {
                            track.artists.map { artistId(it, now) to ArtistRole.MAIN }
                        }

                        val trackEntity = TrackEntity(
                            id = trackId,
                            fileName = target,
                            fileSize = temp.length(),
                            contentHash = hash,
                            title = track.title,
                            subtitle = track.subtitle,
                            trackDate = track.date,
                            year = track.year,
                            coverFileName = null,
                            durationMs = track.duration,
                            mimeType = track.media.mime,
                            available = true,
                            addedAt = existingTrack?.addedAt ?: now,
                            hasVideo = track.hasVideo,
                            composer = track.composer,
                            parentTrackId = track.parentTrackId,
                            description = track.description,
                            lyrics = track.lyrics,
                            playbackVolume = track.playbackVolume
                        )

                        if (existingTrack != null) {
                            dao.updateTrack(trackEntity)
                            dao.deleteTrackArtists(trackId)
                        } else {
                            dao.insertTrack(trackEntity)
                        }

                        dao.insertTrackArtists(
                            trackCredits.mapIndexed { pos, (artId, role) ->
                                TrackArtistEntity(trackId, artId, role, pos)
                            }
                        )

                        val discNum = track.discNumber ?: 1
                        val pos = track.number ?: (index + 1)
                        dao.insertAlbumTrack(AlbumTrackEntity(albumId, trackId, discNum, pos))
                    } finally {
                        temp.delete()
                    }
                }
            }

            folder.importContainer(layout, source, safeContainerName)
            folder.refreshFiles(layout)
            container.tracks.size
        } finally {
            source.delete()
        }
    }

    fun deleteAll(): Job {
        val previous = job
        val current = scope.launch(Dispatchers.IO) {
            previous?.cancelAndJoin()
            runCatching {
                folder.layout()?.let(folder::clear)
                images.clear()
                database.clearAllTables()
            }.onFailure { Log.e(LOG_TAG, "Не удалось удалить каталог", it) }
            _state.value = ImportState.Idle
        }
        job = current
        return current
    }

    private suspend fun scan(): ImportResult {
        val layout = folder.layout() ?: return ImportResult(0, 0, 0)
        moveLegacyImages(layout)
        val trackFiles = folder.trackMediaFiles(layout)
        folder.refreshFiles(layout, trackFiles)
        val inboxFiles = folder.inboxMediaFiles(layout)
        val inboxContainers = folder.inboxAxsFiles(layout)
        val storedContainers = folder.libraryAxsFiles(layout)

        val knownTracks = dao.trackFileStates()
        val filesByName = trackFiles.associateBy { it.name }
        val knownNames = knownTracks.mapTo(mutableSetOf()) { it.fileName }
        val missingVideoDurations = dao.tracksMissingVideoDuration()
            .mapNotNull { track -> filesByName[track.fileName]?.let { track to it } }

        knownTracks
            .filter { it.available != (it.fileName in filesByName) }
            .groupBy({ it.fileName in filesByName }, { it.id })
            .forEach { (available, ids) -> dao.updateAvailability(ids, available) }

        val unhashed = knownTracks
            .filter { it.contentHash.isEmpty() }
            .mapNotNull { track -> filesByName[track.fileName]?.let { track.id to it } }
        val newTrackFiles = trackFiles.filterNot { it.name in knownNames }

        // If the database has no albums/tracks but stored .axs containers exist (e.g. after reinstall),
        // we must restore all albums from the stored .axs containers!
        val containersToRestore = if (knownTracks.isEmpty() && storedContainers.isNotEmpty()) {
            storedContainers
        } else {
            // Restore any stored container whose album is not in the database
            storedContainers.filter { containerFile ->
                dao.findAlbumByContainerFileName(containerFile.name) == null
            }
        }

        val total = missingVideoDurations.size + unhashed.size + newTrackFiles.size +
                inboxFiles.size + inboxContainers.size + containersToRestore.size

        val knownHashes = knownTracks.mapNotNullTo(mutableSetOf()) { it.contentHash.ifEmpty { null } }
        val missingByHash = knownTracks
            .filter { it.fileName !in filesByName && it.contentHash.isNotEmpty() }
            .associateTo(mutableMapOf()) { it.contentHash to it.id }
        val takenNames = trackFiles.mapTo(mutableSetOf()) { it.name.lowercase() }

        var processed = 0
        var imported = 0
        var skipped = 0
        var failed = 0
        _state.value = ImportState.Running(processed, total)

        suspend fun process(file: LibraryFile, block: suspend () -> Boolean?) {
            runCatching { block() }
                .onSuccess { isImported ->
                    when (isImported) {
                        true -> imported++
                        false -> skipped++
                        null -> Unit
                    }
                }
                .onFailure {
                    failed++
                    Log.e(LOG_TAG, "Не удалось обработать ${file.name}", it)
                }
            currentCoroutineContext().ensureActive()
            processed++
            _state.value = ImportState.Running(processed, total)
        }

        // 1. Restore any missing containers from axs_containers/
        containersToRestore.forEach { file ->
            process(file) {
                importAxs(file.uri, file.name)
                true
            }
        }

        // 2. Import containers from inbox, and delete only after successful import
        inboxContainers.forEach { file ->
            process(file) {
                importAxs(file.uri, file.name)
                folder.deleteInboxFile(file)
                true
            }
        }

        // 3. Process unhashed files
        unhashed.forEach { (trackId, file) ->
            process(file) {
                val hash = folder.fingerprint(file)
                dao.updateContentHash(trackId, hash)
                knownHashes += hash
                null
            }
        }

        // 4. Missing video durations
        missingVideoDurations.forEach { (track, file) ->
            process(file) {
                tagReader.read(file.uri).durationMs?.takeIf { it > 0L }?.let { dao.updateTrackDuration(track.id, it) }
                null
            }
        }

        // 5. New track files
        newTrackFiles.forEach { file ->
            process(file) {
                val hash = folder.fingerprint(file)
                val missingTrackId = missingByHash.remove(hash)
                when {
                    missingTrackId != null -> {
                        dao.relinkTrack(missingTrackId, file.name, file.size)
                        false
                    }
                    hash in knownHashes -> false
                    else -> {
                        insertTrack(file, tagReader.read(file.uri), file.name, hash)
                        knownHashes += hash
                        true
                    }
                }
            }
        }

        // 6. Inbox audio files
        inboxFiles.forEach { file ->
            process(file) {
                val hash = folder.fingerprint(file)
                if (hash in knownHashes) {
                    false
                } else {
                    val tags = tagReader.read(file.uri)
                    val fileName = folder.moveToTracks(layout, file, uniqueName(file.name, takenNames))
                    takenNames += fileName.lowercase()
                    insertTrack(file, tags, fileName, hash)
                    knownHashes += hash
                    true
                }
            }
        }

        // Live files are linked to parents
        dao.linkLiveTracksToParents()
        dao.removeLiveTracksFromAlbums()

        return ImportResult(imported, skipped, failed)
    }

    private fun moveLegacyImages(layout: LibraryLayout) {
        runCatching {
            folder.drainDirectory(layout, LEGACY_COVERS_DIRECTORY, images::adoptCover)
            folder.drainDirectory(layout, LEGACY_ARTIST_PHOTOS_DIRECTORY, images::adoptArtistPhoto)
        }.onFailure { Log.e(LOG_TAG, "Не удалось перенести изображения из папки библиотеки", it) }
    }

    private suspend fun insertTrack(
        file: LibraryFile,
        tags: AudioTags,
        fileName: String,
        contentHash: String
    ) {
        val trackId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val trackCredits = credits(tags.artists)
        val albumArtistNames = credits(tags.albumArtists).let { it.main + it.featured }

        val coverOwnerId = database.withTransaction {
            val mainIds = trackCredits.main.map { artistId(it, now) }
            val featuredIds = trackCredits.featured.map { artistId(it, now) }.filterNot { it in mainIds }
            val albumArtistIds = albumArtistNames.map { artistId(it, now) }.distinct()
            val album = tags.album?.let { findOrCreateAlbum(it, albumArtistIds, mainIds, tags.year, now) }

            dao.insertTrack(
                TrackEntity(
                    id = trackId,
                    fileName = fileName,
                    fileSize = file.size,
                    contentHash = contentHash,
                    title = tags.title ?: file.baseName,
                    subtitle = null,
                    trackDate = tags.trackDate,
                    year = tags.year,
                    coverFileName = null,
                    durationMs = tags.durationMs,
                    mimeType = file.mimeType,
                    available = true,
                    addedAt = now,
                    hasVideo = tags.hasVideo,
                    composer = tags.composer,
                    parentTrackId = null,
                    description = null,
                    lyrics = null
                )
            )
            dao.insertTrackArtists(
                (mainIds.map { it to ArtistRole.MAIN } + featuredIds.map { it to ArtistRole.FEATURED })
                    .mapIndexed { position, (artistId, role) -> TrackArtistEntity(trackId, artistId, role, position) }
            )

            if (album != null) {
                if (album.year == null && tags.year != null) dao.updateAlbum(album.copy(year = tags.year))
                val discNumber = tags.discNumber ?: 1
                val position = tags.trackNumber ?: (dao.maxAlbumPosition(album.id, discNumber) + 1)
                dao.insertAlbumTrack(AlbumTrackEntity(album.id, trackId, discNumber, position))
            }

            when {
                album == null -> trackId
                album.coverFileName == null -> album.id
                else -> null
            }
        }

        val artwork = tags.artwork ?: return
        val ownerId = coverOwnerId ?: return
        
        // Try to save cover with decoding, fallback to raw bytes if decoding fails
        val cover = images.saveCover(ownerId, artwork) 
            ?: images.saveCoverRaw(ownerId, artwork) 
            ?: return
            
        if (ownerId == trackId) dao.updateTrackCover(trackId, cover) else dao.updateAlbumCover(ownerId, cover)
    }

    private suspend fun artistId(name: String, now: Long): String {
        val nameKey = name.lowercase()
        return dao.findArtistByKey(nameKey)?.id
            ?: ArtistEntity(UUID.randomUUID().toString(), name, nameKey, null, now)
                .also { dao.insertArtist(it) }
                .id
    }

    private suspend fun findOrCreateAlbum(
        title: String,
        albumArtistIds: List<String>,
        trackArtistIds: List<String>,
        year: Int?,
        now: Long
    ): AlbumEntity {
        val titleKey = albumTitleKey(title)
        val existing = when {
            albumArtistIds.isNotEmpty() -> dao.albumsOfArtist(albumArtistIds.first()).firstOrNull { candidate ->
                albumTitleKey(candidate.title) == titleKey &&
                    dao.albumArtistIds(candidate.id).toSet() == albumArtistIds.toSet()
            }
            trackArtistIds.isNotEmpty() -> trackArtistIds.firstNotNullOfOrNull { artistId ->
                dao.albumsOfArtist(artistId).firstOrNull { albumTitleKey(it.title) == titleKey }
            }
            else -> dao.albumsWithoutArtists().firstOrNull { albumTitleKey(it.title) == titleKey }
        }
        if (existing != null) return existing

        val artistIds = albumArtistIds.ifEmpty { trackArtistIds }
        val album = AlbumEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            year = year,
            description = null,
            coverFileName = null,
            createdAt = now,
            containerFileName = null,
            isContainerOwned = false,
            containerSyncState = null
        )
        dao.insertAlbum(album)
        dao.insertAlbumArtists(artistIds.mapIndexed { position, artistId -> AlbumArtistEntity(album.id, artistId, position) })
        return album
    }

    private fun albumTitleKey(title: String): String = title.trim().replace(WHITESPACE, " ").lowercase()

    private fun credits(values: List<String>): Credits {
        val segments = values.map { it.split(FEATURING_SEPARATOR) }
        val main = segments.flatMap { artistNames(it.first()) }.distinctBy { it.lowercase() }
        val featured = segments.flatMap { parts -> parts.drop(1).flatMap(::artistNames) }
            .distinctBy { it.lowercase() }
            .filterNot { name -> main.any { it.equals(name, ignoreCase = true) } }
        return Credits(main, featured)
    }

    private fun artistNames(value: String): List<String> =
        value.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private fun uniqueName(name: String, takenNames: Set<String>): String {
        if (name.lowercase() !in takenNames) return name
        val baseName = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.', "")
        return generateSequence(2) { it + 1 }
            .map { index -> if (extension.isEmpty()) "$baseName ($index)" else "$baseName ($index).$extension" }
            .first { it.lowercase() !in takenNames }
    }

    /**
     * Packs an existing album (with files in folders) into an .axs container.
     * After successful packing, marks album as container-only and deletes extracted files.
     * Returns the container file name on success.
     */
    suspend fun packAlbumToContainer(albumId: String): String = withContext(Dispatchers.IO) {
        val layout = folder.layout() ?: error("Сначала выберите папку библиотеки")
        val album = dao.albumEntity(albumId) ?: error("Альбом не найден: $albumId")
        if (album.isContainerOwned) {
            throw IllegalStateException("Альбом уже является контейнером")
        }

        val tracks = dao.tracksInAlbum(albumId)
        if (tracks.isEmpty()) {
            throw IllegalStateException("В альбоме нет треков")
        }

        val now = System.currentTimeMillis()
        val containerName = "${album.title.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")}.axs"

        // Gather album artists
        val albumArtistIds = dao.albumArtistIds(albumId)
        val albumArtists = albumArtistIds.mapNotNull { dao.findArtistById(it)?.name }
        val albumArtistCredits = albumArtists.map { AxsArtistCredit(it, "MAIN") }

        // Gather cover
        val coverSource = album.coverFileName?.let { coverName ->
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
        val textureSource = album.backgroundTextureFileName?.let { texName ->
            val file = images.getTextureFile(texName)
            if (file.exists()) {
                AxsWriteSource(
                    name = "background.${file.extension.ifEmpty { "png" }}",
                    mime = if (file.extension.matches(Regex("(?i)jpg|jpeg"))) "image/jpeg" else "image/png",
                    openStream = { file.inputStream() }
                )
            } else null
        }

        // Gather photos
        val photoSources = images.albumPhotos(albumId).mapIndexedNotNull { idx, uri ->
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

        // Gather media files
        val mediaFileSources = images.albumMedia(albumId).mapIndexedNotNull { idx, media ->
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
        val trackUris = folder.trackUris.value
        for (link in dao.albumTracksForAlbum(albumId)) {
            val track = tracks.find { it.id == link.trackId } ?: continue
            val trackArtistDetails = dao.trackArtistDetails(track.id)
            val artists = trackArtistDetails.map { it.name }
            val artistCredits = trackArtistDetails.map { AxsArtistCredit(it.name, it.role.name) }

            val trackUri = trackUris[track.fileName]
            if (trackUri == null) continue

            val trackSource = AxsWriteSource(
                name = track.fileName,
                mime = track.mimeType ?: "audio/mpeg",
                openStream = { context.contentResolver.openInputStream(trackUri)!! }
            )

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
                    mediaSource = trackSource
                )
            )
        }

        val albumWriteData = AxsAlbumWriteData(
            id = album.id,
            title = album.title,
            artists = albumArtists,
            artistCredits = albumArtistCredits,
            year = album.year,
            discNumber = 1,
            genre = null,
            description = album.description,
            coverSource = coverSource,
            photoSources = photoSources,
            mediaFileSources = mediaFileSources,
            backgroundTextureSource = textureSource,
            backgroundTextureScale = album.backgroundTextureScale,
            backgroundTextureFit = album.backgroundTextureFit,
            backgroundTextureOffsetX = album.backgroundTextureOffsetX,
            backgroundTextureOffsetY = album.backgroundTextureOffsetY,
            backgroundTextureBlendMode = album.backgroundTextureBlendMode,
            backgroundTextureKeyColor = album.backgroundTextureKeyColor,
            backgroundTextureTolerance = album.backgroundTextureTolerance,
            backgroundTextureOpacity = album.backgroundTextureOpacity,
            textColorMode = album.textColorMode,
            tracks = trackWriteDataList,
            preservedRawJson = null
        )

        // Build manifest JSON for fast container track startup
        val manifestJson = try {
            val writer = StringWriter()
            val jsonWriter = JsonWriter(writer)
            jsonWriter.beginObject()
            jsonWriter.name("format").value("axs")
            jsonWriter.name("version").value(1)
            jsonWriter.name("album").beginObject()
            jsonWriter.name("id").value(album.id)
            jsonWriter.name("title").value(album.title)
            jsonWriter.name("artists").beginArray()
            albumArtists.forEach { jsonWriter.value(it) }
            jsonWriter.endArray()
            jsonWriter.name("year").value(album.year ?: 0)
            jsonWriter.name("description").value(album.description ?: "")
            jsonWriter.endObject()
            jsonWriter.name("tracks").beginArray()
            trackWriteDataList.forEach { track ->
                jsonWriter.beginObject()
                jsonWriter.name("id").value(track.id ?: "")
                jsonWriter.name("number").value(track.number ?: 0)
                jsonWriter.name("title").value(track.title)
                jsonWriter.name("artists").beginArray()
                track.artists.forEach { jsonWriter.value(it) }
                jsonWriter.endArray()
                track.duration?.let { jsonWriter.name("duration_ms").value(it) }
                jsonWriter.name("media").beginObject()
                jsonWriter.name("file_name").value(track.mediaSource.name)
                jsonWriter.name("mime_type").value(track.mediaSource.mime)
                // Note: offset/length will be filled by AxsContainer.writeContainer
                jsonWriter.name("offset").value(0L)
                jsonWriter.name("length").value(0L)
                jsonWriter.endObject()
                jsonWriter.endObject()
            }
            jsonWriter.endArray()
            jsonWriter.endObject()
            jsonWriter.close()
            writer.toString()
        } catch (e: Exception) {
            ""
        }

        val tempContainer = File(context.cacheDir, "axs_pack_${System.currentTimeMillis()}.axs")
        try {
            AxsContainer.writeContainer(tempContainer, albumWriteData)
            
            // Import container to library folder
            folder.importContainer(layout, tempContainer, containerName)
            
            // Update album to be container-owned and container-only
            val updatedAlbum = album.copy(
                containerFileName = containerName,
                isContainerOwned = true,
                isContainerOnly = true,
                containerSyncState = "synced",
                containerManifestJson = manifestJson
            )
            dao.updateAlbum(updatedAlbum)
            dao.updateAlbumManifestJson(albumId, manifestJson)
            
            // Delete extracted files (tracks, cover, texture, photos, media)
            deleteExtractedAlbumFiles(albumId, tracks)
            
            containerName
        } finally {
            tempContainer.delete()
        }
    }

    private fun deleteExtractedAlbumFiles(albumId: String, tracks: List<TrackEntity>) {
        // Delete track files
        for (track in tracks) {
            folder.deleteTrack(track.fileName)
        }
        // Delete cover, texture, photos, media
        images.deleteAlbumAssets(albumId, null)
    }
}
