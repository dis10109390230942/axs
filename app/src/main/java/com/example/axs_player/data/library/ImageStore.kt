package com.example.axs_player.data.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val COVERS_DIRECTORY = "covers"
private const val ARTISTS_DIRECTORY = "artists"
private const val ALBUM_MEDIA_DIRECTORY = "album_media"
const val ALBUM_TEXTURES_DIRECTORY = "album_textures"
private const val ARTIST_MEDIA_DIRECTORY = "artist_media"
private const val IMAGE_MAX_SIZE = 600
private const val IMAGE_QUALITY = 90
private val PHOTO_INDEX = Regex("""\s*\((\d+)\)$""")

class ImageStore(context: Context) {

    data class AlbumMedia(val uri: Uri, val fileName: String, val isVideo: Boolean)

    var onChanged: (() -> Unit)? = null

    private val _mediaVersion = MutableStateFlow(0L)
    val mediaVersion: StateFlow<Long> = _mediaVersion.asStateFlow()

    private val resolver = context.contentResolver
    private val coversDirectory = File(context.filesDir, COVERS_DIRECTORY)
    private val artistsDirectory = File(context.filesDir, ARTISTS_DIRECTORY)
    private val albumMediaDirectory = File(context.filesDir, ALBUM_MEDIA_DIRECTORY)
    private val albumTexturesDirectory = File(context.filesDir, ALBUM_TEXTURES_DIRECTORY)
    private val artistMediaDirectory = File(context.filesDir, ARTIST_MEDIA_DIRECTORY)

    private val _artistPhotos = MutableStateFlow(readArtistPhotos())
    val artistPhotos: StateFlow<Map<String, List<Uri>>> = _artistPhotos.asStateFlow()

    fun coverUri(fileName: String?): Uri? = fileName?.let { File(coversDirectory, it).toUri() }

    fun albumPhotos(albumId: String): List<Uri> =
        coversDirectory.listFiles().orEmpty()
            .filter { it.isFile && photoKey(it.nameWithoutExtension) == albumId.lowercase() }
            .sortedBy { photoIndex(it.nameWithoutExtension) }
            .map { it.toUri() }

    fun deleteAlbumAssets(albumId: String, textureFileName: String?) {
        albumPhotos(albumId).forEach { it.path?.let(::File)?.delete() }
        File(albumMediaDirectory, albumId).deleteRecursively()
        textureFileName?.let(::deleteAlbumTexture)
        File(albumTexturesDirectory, albumId).deleteRecursively()
        onChanged?.invoke()
        _mediaVersion.value++
    }

    fun saveAlbumPhoto(albumId: String, source: Uri): String? =
        saveAlbumPhoto(albumId, ImageDecoder.createSource(resolver, source))

    fun deleteAlbumPhoto(photo: Uri): Boolean {
        val file = photo.path?.let(::File)?.takeIf { it.parentFile == coversDirectory }
            ?: return false
        if (photoIndex(file.nameWithoutExtension) <= 1) return false
        return file.delete().also { if (it) onChanged?.invoke() }
    }

    fun albumMedia(albumId: String): List<AlbumMedia> =
        File(albumMediaDirectory, albumId).listFiles().orEmpty()
            .filter { it.isFile }
            .sortedBy { it.name.lowercase() }
            .map { file -> AlbumMedia(file.toUri(), file.name, file.extension.lowercase() in VIDEO_EXTENSIONS) }

    fun albumTextureUri(fileName: String?): Uri? = fileName?.let { File(albumTexturesDirectory, it).toUri() }

    fun saveAlbumTexture(albumId: String, source: Uri): String? {
        val originalName = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: source.lastPathSegment?.substringAfterLast('/') ?: return null
        val safeName = originalName.substringAfterLast('/').replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val extension = safeName.substringAfterLast('.', "").lowercase()
        if (extension !in IMAGE_EXTENSIONS) return null
        val directory = File(albumTexturesDirectory, albumId).apply { mkdirs() }
        val target = uniqueFile(directory, safeName)
        resolver.openInputStream(source)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } } ?: return null
        onChanged?.invoke()
        return "${albumId}/${target.name}"
    }

    fun deleteAlbumTexture(fileName: String?): Boolean {
        val file = fileName?.let { File(albumTexturesDirectory, it) } ?: return false
        if (file.parentFile?.parentFile != albumTexturesDirectory) return false
        return file.delete().also { if (it) onChanged?.invoke() }
    }

    fun saveAlbumMedia(albumId: String, source: Uri): AlbumMedia? {
        val originalName = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: source.lastPathSegment?.substringAfterLast('/') ?: return null
        val directory = File(albumMediaDirectory, albumId).apply { mkdirs() }
        val safeName = originalName.substringAfterLast('/').replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val extension = safeName.substringAfterLast('.', "").lowercase()
        if (extension !in MEDIA_EXTENSIONS) return null
        val target = uniqueFile(directory, safeName)
        resolver.openInputStream(source)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            ?: return null
        onChanged?.invoke()
        _mediaVersion.value++
        return AlbumMedia(target.toUri(), target.name, extension in VIDEO_EXTENSIONS)
    }

    private fun saveMedia(directory: File, source: Uri): AlbumMedia? {
        directory.mkdirs()
        val originalName = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: source.lastPathSegment?.substringAfterLast('/') ?: return null
        val safeName = originalName.substringAfterLast('/').replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val extension = safeName.substringAfterLast('.', "").lowercase()
        if (extension !in MEDIA_EXTENSIONS) return null
        val target = uniqueFile(directory, safeName)
        resolver.openInputStream(source)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } } ?: return null
        onChanged?.invoke()
        _mediaVersion.value++
        return AlbumMedia(target.toUri(), target.name, extension in VIDEO_EXTENSIONS)
    }

    fun deleteAlbumMedia(media: AlbumMedia): Boolean {
        val file = media.uri.path?.let(::File) ?: return false
        if (file.parentFile?.parentFile != albumMediaDirectory) return false
        return file.delete().also { if (it) { _mediaVersion.value++; onChanged?.invoke() } }
    }

    fun artistMedia(artistId: String): List<AlbumMedia> =
        File(artistMediaDirectory, artistId).listFiles().orEmpty()
            .filter { it.isFile }
            .sortedBy { it.name.lowercase() }
            .map { file -> AlbumMedia(file.toUri(), file.name, file.extension.lowercase() in VIDEO_EXTENSIONS) }

    fun saveArtistMedia(artistId: String, source: Uri): AlbumMedia? = saveMedia(File(artistMediaDirectory, artistId), source)

    fun deleteArtistMedia(media: AlbumMedia): Boolean {
        val file = media.uri.path?.let(::File) ?: return false
        if (file.parentFile?.parentFile != artistMediaDirectory) return false
        return file.delete().also { if (it) { _mediaVersion.value++; onChanged?.invoke() } }
    }

    private fun saveAlbumPhoto(albumId: String, source: ImageDecoder.Source): String? {
        val bitmap = decodeScaled(source) ?: return null
        val index = albumPhotos(albumId).size + 1
        val file = File(coversDirectory, if (index == 1) "$albumId.jpg" else "$albumId ($index).jpg")
        return file.name.takeIf { writeJpeg(file, bitmap) }?.also { onChanged?.invoke() }
    }

    fun saveCover(baseName: String, bytes: ByteArray): String? {
        val bitmap = decodeScaled(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) ?: return null
        val file = File(coversDirectory, "$baseName.jpg")
        return file.name.takeIf { writeJpeg(file, bitmap) }?.also { onChanged?.invoke() }
    }

    fun saveCoverRaw(baseName: String, bytes: ByteArray): String? {
        val file = File(coversDirectory, "$baseName.jpg")
        return try {
            file.outputStream().use { it.write(bytes) }
            onChanged?.invoke()
            file.name
        } catch (e: Exception) {
            file.delete()
            android.util.Log.e("ImageStore", "Failed to save raw cover for $baseName", e)
            null
        }
    }

    fun adoptCover(fileName: String, input: InputStream) {
        copy(input, File(coversDirectory, fileName))
        onChanged?.invoke()
    }

    fun saveArtistPhoto(nameKey: String, source: Uri): Boolean =
        saveArtistPhoto(nameKey, ImageDecoder.createSource(resolver, source))

    fun saveArtistPhoto(nameKey: String, bytes: ByteArray): Boolean =
        saveArtistPhoto(nameKey, ImageDecoder.createSource(ByteBuffer.wrap(bytes)))

    private fun saveArtistPhoto(nameKey: String, source: ImageDecoder.Source): Boolean {
        val bitmap = decodeScaled(source) ?: return false
        val index = artistPhotoFiles(nameKey).maxOfOrNull { photoIndex(it.nameWithoutExtension) }?.plus(1) ?: 1
        val file = File(artistsDirectory, if (index == 1) "$nameKey.jpg" else "$nameKey ($index).jpg")
        if (!writeJpeg(file, bitmap)) return false
        _artistPhotos.value = readArtistPhotos()
        onChanged?.invoke()
        return true
    }

    fun adoptArtistPhoto(fileName: String, input: InputStream) {
        copy(input, File(artistsDirectory, fileName))
        _artistPhotos.value = readArtistPhotos()
        onChanged?.invoke()
    }

    fun deleteArtistPhoto(photo: Uri): Boolean {
        val file = photo.path?.let(::File)?.takeIf { it.parentFile == artistsDirectory } ?: return false
        if (!file.delete()) return false
        onChanged?.invoke()
        _artistPhotos.value = readArtistPhotos()
        return true
    }

    fun getCoverFile(fileName: String): File = File(coversDirectory, fileName)

    fun getTextureFile(fileName: String): File = File(albumTexturesDirectory, fileName)

    fun getPhotoFile(albumId: String, photoFileName: String): File = File(coversDirectory, photoFileName)

    fun getMediaFile(albumId: String, mediaFileName: String): File = File(File(albumMediaDirectory, albumId), mediaFileName)

    fun totalSize(): Long =
        listOf(coversDirectory, artistsDirectory, albumMediaDirectory, artistMediaDirectory, albumTexturesDirectory).sumOf { directory -> directorySize(directory) }

    fun clear() {
        coversDirectory.deleteRecursively()
        artistsDirectory.deleteRecursively()
        albumMediaDirectory.deleteRecursively()
        artistMediaDirectory.deleteRecursively()
        albumTexturesDirectory.deleteRecursively()
        _mediaVersion.value++
        _artistPhotos.value = emptyMap()
        onChanged?.invoke()
    }

    fun reloadArtistPhotos() {
        _artistPhotos.value = readArtistPhotos()
    }

    private fun decodeScaled(source: ImageDecoder.Source): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val scale = IMAGE_MAX_SIZE.toFloat() / max(info.size.width, info.size.height)
            if (scale < 1f) {
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1)
                )
            }
        }
    }.getOrNull()

    private fun writeJpeg(file: File, bitmap: Bitmap): Boolean {
        file.parentFile?.mkdirs()
        val isWritten = runCatching {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, it) }
        }.getOrDefault(false)
        if (!isWritten) file.delete()
        return isWritten
    }

    private fun copy(input: InputStream, target: File) {
        target.parentFile?.mkdirs()
        runCatching { target.outputStream().use { input.copyTo(it) } }
            .onFailure { target.delete() }
            .getOrThrow()
    }

    private fun artistPhotoFiles(nameKey: String): List<File> =
        artistsDirectory.listFiles().orEmpty().filter { it.isFile && photoKey(it.nameWithoutExtension) == nameKey }

    private fun readArtistPhotos(): Map<String, List<Uri>> =
        artistsDirectory.listFiles().orEmpty()
            .filter { it.isFile }
            .sortedBy { photoIndex(it.nameWithoutExtension) }
            .groupBy { photoKey(it.nameWithoutExtension) }
            .mapValues { entry -> entry.value.map { it.toUri() } }

    private fun photoKey(baseName: String): String = baseName.replace(PHOTO_INDEX, "").trim().lowercase()

    private fun photoIndex(baseName: String): Int =
        PHOTO_INDEX.find(baseName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

    private fun uniqueFile(directory: File, name: String): File {
        val base = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "").takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
        return generateSequence(0) { it + 1 }
            .map { index -> File(directory, if (index == 0) name else "$base ($index)$extension") }
            .first { !it.exists() }
    }

    private fun directorySize(directory: File): Long =
        directory.listFiles().orEmpty().sumOf { if (it.isDirectory) directorySize(it) else it.length() }
}

private val MEDIA_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "mkv", "webm", "mov")
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov")
