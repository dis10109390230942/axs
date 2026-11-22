package com.example.axs_player.data.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.FileNotFoundException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val PREFERENCES_NAME = "library_folder"
private const val KEY_TREE_URI = "tree_uri"
private const val INBOX_DIRECTORY = "inbox"
private const val TRACKS_DIRECTORY = "tracks"
private const val METADATA_DIRECTORY = "axs_data"
private const val AXS_CONTAINERS_DIRECTORY = "axs_containers"
private const val LEGACY_METADATA_DIRECTORY = ".axs"
private const val NO_MEDIA_FILE = ".nomedia"
private const val FINGERPRINT_CHUNK = 64L * 1024
private const val PERMISSION_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
private val MEDIA_EXTENSIONS = setOf("mp3", "flac", "wav", "mp4", "m4a", "ogg", "opus", "webm", "mkv", "mov")
private val CHILD_PROJECTION = arrayOf(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_SIZE
)

class LibraryFolder(private val appContext: Context) {

    val context: Context
        get() = appContext

    private val resolver = appContext.contentResolver
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _treeUri = MutableStateFlow(storedTreeUri())
    val treeUri: StateFlow<Uri?> = _treeUri.asStateFlow()

    private val _trackUris = MutableStateFlow<Map<String, Uri>>(emptyMap())
    val trackUris: StateFlow<Map<String, Uri>> = _trackUris.asStateFlow()

    fun select(tree: Uri) {
        resolver.takePersistableUriPermission(tree, PERMISSION_FLAGS)
        _treeUri.value?.takeIf { it != tree }?.let { previous ->
            runCatching { resolver.releasePersistableUriPermission(previous, PERMISSION_FLAGS) }
        }
        preferences.edit { putString(KEY_TREE_URI, tree.toString()) }
        _treeUri.value = tree
        _trackUris.value = emptyMap()
    }

    fun layout(): LibraryLayout? {
        val tree = _treeUri.value ?: return null
        val root = rootUri(tree)
        val directories = directoriesIn(tree, root)
        fun directory(name: String): Uri = directories[name]?.uri
            ?: DocumentsContract.createDocument(resolver, root, Document.MIME_TYPE_DIR, name)
            ?: throw FileNotFoundException("Не удалось создать папку $name")
        val metadata = directories[METADATA_DIRECTORY]?.uri
            ?: directories[LEGACY_METADATA_DIRECTORY]?.uri
            ?: directory(METADATA_DIRECTORY)
        val axsContainers = directoriesIn(tree, metadata)[AXS_CONTAINERS_DIRECTORY]?.uri
            ?: DocumentsContract.createDocument(resolver, metadata, Document.MIME_TYPE_DIR, AXS_CONTAINERS_DIRECTORY)
            ?: throw FileNotFoundException("Не удалось создать папку контейнеров")
        return LibraryLayout(
            tree = tree,
            inbox = directory(INBOX_DIRECTORY),
            tracks = directory(TRACKS_DIRECTORY),
            metadata = metadata,
            axsContainers = axsContainers
        )
    }

    fun readMetadataFile(layout: LibraryLayout, name: String, target: File): Boolean {
        val file = children(layout.tree, layout.metadata).firstOrNull { !it.isDirectory && it.name == name } ?: return false
        resolver.openInputStream(file.uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: return false
        return true
    }

    fun writeMetadataFile(layout: LibraryLayout, name: String, source: File) {
        val existing = children(layout.tree, layout.metadata).firstOrNull { !it.isDirectory && it.name == name }
        val uri = existing?.uri ?: DocumentsContract.createDocument(resolver, layout.metadata, "application/octet-stream", name)
            ?: throw FileNotFoundException("Не удалось создать $name")
            resolver.openOutputStream(uri, "wt")?.use { output -> source.inputStream().use { it.copyTo(output) } }
            ?: throw FileNotFoundException("Не удалось записать $name")
    }

    /** Keeps exported artwork and other media out of Android media scans. */
    fun ensureMetadataHiddenFromMediaScanners(layout: LibraryLayout) {
        val exists = children(layout.tree, layout.metadata).any { !it.isDirectory && it.name == NO_MEDIA_FILE }
        if (exists) return
        val uri = DocumentsContract.createDocument(
            resolver,
            layout.metadata,
            "application/octet-stream",
            NO_MEDIA_FILE
        ) ?: throw FileNotFoundException("Не удалось создать $NO_MEDIA_FILE")
        resolver.openOutputStream(uri, "wt")?.use { } 
            ?: throw FileNotFoundException("Не удалось записать $NO_MEDIA_FILE")
    }

    fun copyMetadataDirectoryTo(layout: LibraryLayout, name: String, target: File) {
        val directory = children(layout.tree, layout.metadata).firstOrNull { it.isDirectory && it.name == name } ?: return
        fun copyDirectory(source: LibraryFile, destination: File) {
            destination.mkdirs()
            children(layout.tree, source.uri).forEach { child ->
                val childTarget = File(destination, child.name)
                if (child.isDirectory) {
                    copyDirectory(child, childTarget)
                } else {
                    resolver.openInputStream(child.uri)?.use { input ->
                        childTarget.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw FileNotFoundException("Не удалось открыть ${child.name}")
                }
            }
        }
        copyDirectory(directory, target)
    }

    fun replaceMetadataDirectory(layout: LibraryLayout, name: String, source: File) {
        val existing = children(layout.tree, layout.metadata).firstOrNull { it.isDirectory && it.name == name }
        existing?.let { children(layout.tree, it.uri).forEach { file -> DocumentsContract.deleteDocument(resolver, file.uri) } }
        val directory = existing?.uri ?: DocumentsContract.createDocument(resolver, layout.metadata, Document.MIME_TYPE_DIR, name)
            ?: throw FileNotFoundException("Не удалось создать $name")
        source.walkTopDown().filter { it.isFile }.forEach { file ->
            val relativeName = file.relativeTo(source).invariantSeparatorsPath
            val parent = relativeName.substringBeforeLast('/', "")
            val targetDirectory = parent.split('/').filter { it.isNotEmpty() }.fold(directory) { current, name ->
                children(layout.tree, current).firstOrNull { it.isDirectory && it.name == name }?.uri
                    ?: DocumentsContract.createDocument(resolver, current, Document.MIME_TYPE_DIR, name)
                    ?: return@forEach
            }
            val uri = DocumentsContract.createDocument(resolver, targetDirectory, mimeType(file.name), file.name) ?: return@forEach
            resolver.openOutputStream(uri, "wt")?.use { output -> file.inputStream().use { it.copyTo(output) } }
        }
    }

    private fun mimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        else -> "application/octet-stream"
    }

    fun drainDirectory(layout: LibraryLayout, name: String, consume: (fileName: String, input: InputStream) -> Unit) {
        val directory = directoriesIn(layout.tree, rootUri(layout.tree))[name] ?: return
        children(layout.tree, directory.uri).filterNot { it.isDirectory }.forEach { file ->
            resolver.openInputStream(file.uri)?.use { consume(file.name, it) }
                ?: throw FileNotFoundException("Не удалось открыть ${file.name}")
            DocumentsContract.deleteDocument(resolver, file.uri)
        }
        if (children(layout.tree, directory.uri).isEmpty()) DocumentsContract.deleteDocument(resolver, directory.uri)
    }

    fun inboxMediaFiles(layout: LibraryLayout): List<LibraryFile> = mediaFilesIn(layout.tree, layout.inbox)

    fun inboxAxsFiles(layout: LibraryLayout): List<LibraryFile> = children(layout.tree, layout.inbox).filter { !it.isDirectory && it.extension == "axs" }

    fun libraryAxsFiles(layout: LibraryLayout): List<LibraryFile> = children(layout.tree, layout.axsContainers).filter { !it.isDirectory && it.extension == "axs" }

    fun containerFileUri(layout: LibraryLayout, fileName: String): Uri? =
        children(layout.tree, layout.axsContainers).firstOrNull { !it.isDirectory && it.name == fileName }?.uri

    fun copyContainerTo(layout: LibraryLayout, fileName: String, target: File): Boolean {
        val uri = containerFileUri(layout, fileName) ?: return false
        copyUriTo(uri, target)
        return true
    }

    fun deleteInboxFile(file: LibraryFile): Boolean = DocumentsContract.deleteDocument(resolver, file.uri)

    fun clear(layout: LibraryLayout) {
        children(layout.tree, layout.tracks).forEach { DocumentsContract.deleteDocument(resolver, it.uri) }
        children(layout.tree, layout.metadata).forEach { DocumentsContract.deleteDocument(resolver, it.uri) }
        _trackUris.value = emptyMap()
    }

    fun trackMediaFiles(layout: LibraryLayout): List<LibraryFile> =
        children(layout.tree, layout.tracks).filter { !it.isDirectory && it.extension in MEDIA_EXTENSIONS }

    fun moveToTracks(layout: LibraryLayout, file: LibraryFile, targetName: String): String {
        val source = if (file.name == targetName) {
            file.uri
        } else {
            DocumentsContract.renameDocument(resolver, file.uri, targetName)
                ?: throw FileNotFoundException("Не удалось переименовать ${file.name}")
        }
        val moved = DocumentsContract.moveDocument(resolver, source, file.parentUri, layout.tracks)
            ?: throw FileNotFoundException("Не удалось перенести ${file.name}")
        val name = displayName(moved) ?: targetName
        _trackUris.update { it + (name to moved) }
        return name
    }

    fun importMedia(layout: LibraryLayout, source: java.io.File, targetName: String): Uri {
        val uri = DocumentsContract.createDocument(resolver, layout.tracks, mimeType(targetName), targetName)
            ?: throw FileNotFoundException("Не удалось создать $targetName")
        resolver.openOutputStream(uri, "wt")?.use { output -> source.inputStream().use { it.copyTo(output) } }
            ?: throw FileNotFoundException("Не удалось записать $targetName")
        _trackUris.update { it + (targetName to uri) }
        return uri
    }

    fun importContainer(layout: LibraryLayout, source: java.io.File, targetName: String): String {
        val existing = children(layout.tree, layout.axsContainers).firstOrNull { !it.isDirectory && it.name == targetName }
        val uri = existing?.uri
            ?: DocumentsContract.createDocument(resolver, layout.axsContainers, "application/vnd.axs", targetName)
            ?: throw FileNotFoundException("Не удалось создать $targetName")
        resolver.openOutputStream(uri, "wt")?.use { output -> source.inputStream().use { it.copyTo(output) } }
            ?: throw FileNotFoundException("Не удалось записать $targetName")
        return targetName
    }

    /**
     * Reads a blob from an .axs container by blob name.
     * Returns InputStream positioned at blob data, or null if not found.
     */
    fun openContainerBlobInputStream(layout: LibraryLayout, containerFileName: String, blobName: String): InputStream? {
        val containerUri = containerFileUri(layout, containerFileName) ?: return null
        val containerFile = File(context.cacheDir, "axs_read_${System.currentTimeMillis()}.axs")
        try {
            copyUriTo(containerUri, containerFile)
            val album = AxsContainer.read(containerFile)
            val blob = findBlob(album, blobName) ?: return null
            val input = FileInputStream(containerFile)
            input.channel.position(blob.offset)
            return input
        } catch (e: Exception) {
            return null
        }
    }

/**
     * Replaces a blob in an .axs container by rebuilding the container.
     * Extracts all existing blobs, replaces the target one, and rewrites the container.
     */
    suspend fun replaceContainerBlob(
        layout: LibraryLayout,
        containerFileName: String,
        blobName: String,
        mimeType: String,
        source: java.io.File
    ): Boolean {
        val containerUri = containerFileUri(layout, containerFileName) ?: return false
        val tempContainer = File(context.cacheDir, "axs_read_${System.currentTimeMillis()}.axs")
        val tempWriteContainer = File(context.cacheDir, "axs_write_${System.currentTimeMillis()}.axs")
        return try {
            // Copy current container to temp for reading
            copyUriTo(containerUri, tempContainer)
            val album = AxsContainer.read(tempContainer)
            
            // Find the blob to replace
            val blob = findBlob(album, blobName) ?: return false
            
            // Extract all blobs to temporary files
            val extractedBlobs = extractAllBlobs(tempContainer, album)
            
            // Replace the target blob with new data
            source.copyTo(extractedBlobs[blobName]!!)
            
            // Build updated album write data
            val albumWriteData = buildAlbumWriteData(album, extractedBlobs, blobName, mimeType)
            
            // Write new container
            AxsContainer.writeContainer(tempWriteContainer, albumWriteData)
            
            // Import new container to replace old one
            importContainer(layout, tempWriteContainer, containerFileName)
            
            true
        } catch (e: Exception) {
            android.util.Log.e("LibraryFolder", "Failed to replace blob $blobName", e)
            false
        } finally {
            tempContainer.delete()
            tempWriteContainer.delete()
            // Clean up extracted files
        }
    }

    private fun extractAllBlobs(containerFile: File, album: AxsAlbum): MutableMap<String, File> {
        val blobs = mutableMapOf<String, File>()
        
        fun extractBlob(blob: AxsBlob?): File? {
            blob?.let {
                val tempFile = File(context.cacheDir, "axs_extract_${System.currentTimeMillis()}_${it.name}")
                AxsContainer.extract(containerFile, it, tempFile)
                blobs[it.name] = tempFile
                return tempFile
            }
            return null
        }
        
        extractBlob(album.cover)
        extractBlob(album.backgroundTexture)
        album.photos.forEach { extractBlob(it) }
        album.mediaFiles.forEach { extractBlob(it) }
        album.tracks.forEach { extractBlob(it.media) }
        
        return blobs
    }

    private fun buildAlbumWriteData(
        album: AxsAlbum,
        extractedBlobs: Map<String, File>,
        replacedBlobName: String,
        replacedMimeType: String
    ): AxsAlbumWriteData {
        val writeBlobs = mutableMapOf<String, AxsWriteSource>()
        
        fun makeSource(file: File, mime: String): AxsWriteSource {
            return AxsWriteSource(
                name = file.name,
                mime = mime,
                openStream = { file.inputStream() }
            )
        }
        
        // Cover
        album.cover?.let { writeBlobs[it.name] = makeSource(extractedBlobs[it.name]!!, it.mime) }
        // Texture
        album.backgroundTexture?.let { writeBlobs[it.name] = makeSource(extractedBlobs[it.name]!!, it.mime) }
        // Photos
        album.photos.forEach { writeBlobs[it.name] = makeSource(extractedBlobs[it.name]!!, it.mime) }
        // Media files
        album.mediaFiles.forEach { writeBlobs[it.name] = makeSource(extractedBlobs[it.name]!!, it.mime) }
        // Tracks
        album.tracks.forEach { writeBlobs[it.media.name] = makeSource(extractedBlobs[it.media.name]!!, it.media.mime) }
        
        // Replace the target blob
        writeBlobs[replacedBlobName] = AxsWriteSource(
            name = extractedBlobs[replacedBlobName]!!.name,
            mime = replacedMimeType,
            openStream = { extractedBlobs[replacedBlobName]!!.inputStream() }
        )
        
        // Build track write data
        val trackWriteDataList = mutableListOf<AxsTrackWriteData>()
        for (track in album.tracks) {
            trackWriteDataList.add(
                AxsTrackWriteData(
                    id = track.id,
                    number = track.number,
                    discNumber = track.discNumber,
                    title = track.title,
                    subtitle = track.subtitle,
                    artists = track.artists,
                    artistCredits = track.artistCredits,
                    date = track.date,
                    year = track.year,
                    description = track.description,
                    lyrics = track.lyrics,
                    duration = track.duration,
                    composer = track.composer,
                    hasVideo = track.hasVideo,
                    parentTrackId = track.parentTrackId,
                    playbackVolume = track.playbackVolume,
                    mediaSource = writeBlobs[track.media.name]!!
                )
            )
        }
        
        return AxsAlbumWriteData(
            id = album.id,
            title = album.title,
            artists = album.artists,
            artistCredits = album.artistCredits,
            year = album.year,
            discNumber = album.discNumber,
            genre = album.genre,
            description = album.description,
            coverSource = writeBlobs[album.cover?.name ?: ""],
            photoSources = album.photos.map { writeBlobs[it.name]!! },
            mediaFileSources = album.mediaFiles.map { writeBlobs[it.name]!! },
            backgroundTextureSource = album.backgroundTexture?.let { writeBlobs[it.name] },
            backgroundTextureScale = album.backgroundTextureScale,
            backgroundTextureFit = album.backgroundTextureFit,
            backgroundTextureOffsetX = album.backgroundTextureOffsetX,
            backgroundTextureOffsetY = album.backgroundTextureOffsetY,
            backgroundTextureBlendMode = album.backgroundTextureBlendMode,
            backgroundTextureKeyColor = album.backgroundTextureKeyColor,
            backgroundTextureTolerance = album.backgroundTextureTolerance,
            backgroundTextureOpacity = album.backgroundTextureOpacity,
            tracks = trackWriteDataList,
            preservedRawJson = album.rawJson
        )
    }

    private fun findBlob(album: AxsAlbum, name: String): AxsBlob? {
        if (album.cover?.name == name) return album.cover
        if (album.backgroundTexture?.name == name) return album.backgroundTexture
        return album.photos.firstOrNull { it.name == name }
            ?: album.mediaFiles.firstOrNull { it.name == name }
            ?: album.tracks.firstOrNull { it.media.name == name }?.media
    }

    fun copyUriTo(uri: Uri, target: java.io.File) {
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: throw FileNotFoundException("Не удалось открыть $uri")
    }

    fun refreshFiles(layout: LibraryLayout, trackFiles: List<LibraryFile> = trackMediaFiles(layout)) {
        _trackUris.value = trackFiles.associate { it.name to it.uri }
    }

    /**
     * Deletes a library track through the Storage Access Framework. If the file
     * has already disappeared from the device, there is nothing left to delete.
     */
    fun deleteTrack(fileName: String): Boolean {
        val uri = _trackUris.value[fileName]
            ?: layout()?.let { layout -> trackMediaFiles(layout).firstOrNull { it.name == fileName }?.uri }
            ?: return true
        val deleted = DocumentsContract.deleteDocument(resolver, uri)
        if (deleted) _trackUris.update { it - fileName }
        return deleted
    }

    fun deleteContainer(layout: LibraryLayout, fileName: String): Boolean {
        val uri = containerFileUri(layout, fileName) ?: return true
        return DocumentsContract.deleteDocument(resolver, uri)
    }

    fun totalSize(): Long {
        val tree = _treeUri.value ?: return 0L
        return directorySize(tree, rootUri(tree))
    }

    fun fingerprint(file: LibraryFile): String {
        val descriptor = resolver.openFileDescriptor(file.uri, "r")
            ?: throw FileNotFoundException("Не удалось открыть ${file.name}")
        val digest = MessageDigest.getInstance("SHA-256")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).channel.use { channel ->
            val size = channel.size()
            digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(size).array())
            listOf(0L, max(0L, size - FINGERPRINT_CHUNK)).distinct().forEach { start ->
                val buffer = ByteBuffer.allocate(min(FINGERPRINT_CHUNK, size - start).toInt())
                while (buffer.hasRemaining() && channel.read(buffer, start + buffer.position()) > 0) Unit
                buffer.flip()
                digest.update(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun rootUri(tree: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    private fun directoriesIn(tree: Uri, directory: Uri): Map<String, LibraryFile> =
        children(tree, directory).filter { it.isDirectory }.associateBy { it.name }

    private fun directorySize(tree: Uri, directory: Uri): Long =
        children(tree, directory).sumOf { if (it.isDirectory) directorySize(tree, it.uri) else it.size }

    private fun mediaFilesIn(tree: Uri, directory: Uri): List<LibraryFile> =
        children(tree, directory).flatMap { child ->
            when {
                child.isDirectory -> mediaFilesIn(tree, child.uri)
                child.extension in MEDIA_EXTENSIONS -> listOf(child)
                else -> emptyList()
            }
        }

    private fun children(tree: Uri, directory: Uri): List<LibraryFile> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getDocumentId(directory)
        )
        return resolver.query(childrenUri, CHILD_PROJECTION, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        LibraryFile(
                            name = cursor.getString(1),
                            uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)),
                            parentUri = directory,
                            size = cursor.getLong(3),
                            mimeType = cursor.getString(2)
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun storedTreeUri(): Uri? =
        preferences.getString(KEY_TREE_URI, null)?.toUri()?.takeIf { uri ->
            resolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        }
}
