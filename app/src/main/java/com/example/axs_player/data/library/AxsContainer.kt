package com.example.axs_player.data.library

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AxsBlob(
    val name: String,
    val mime: String,
    val offset: Long,
    val length: Long
)

data class AxsArtistCredit(
    val name: String,
    val role: String = "MAIN"
)

data class AxsTrack(
    val id: String? = null,
    val number: Int?,
    val discNumber: Int? = 1,
    val title: String,
    val subtitle: String?,
    val artists: List<String>,
    val artistCredits: List<AxsArtistCredit> = emptyList(),
    val date: String?,
    val year: Int?,
    val description: String?,
    val lyrics: String?,
    val duration: Long?,
    val composer: String?,
    val hasVideo: Boolean,
    val parentTrackId: String? = null,
    val playbackVolume: Float = 1f,
    val media: AxsBlob
)

data class AxsAlbum(
    val id: String? = null,
    val title: String,
    val artists: List<String>,
    val artistCredits: List<AxsArtistCredit> = emptyList(),
    val year: Int?,
    val discNumber: Int? = 1,
    val genre: String? = null,
    val description: String?,
    val cover: AxsBlob?,
    val photos: List<AxsBlob> = emptyList(),
    val mediaFiles: List<AxsBlob> = emptyList(),
    val backgroundTexture: AxsBlob? = null,
    val backgroundTextureScale: Float = 1f,
    val backgroundTextureFit: String = "cover",
    val backgroundTextureOffsetX: Float = 0f,
    val backgroundTextureOffsetY: Float = 0f,
    val backgroundTextureBlendMode: String = "multiply",
    val backgroundTextureKeyColor: Int = -1,
    val backgroundTextureTolerance: Float = 32f,
    val backgroundTextureOpacity: Float = 1f,
    val tracks: List<AxsTrack>,
    val rawJson: JSONObject? = null
)

/**
 * Payload descriptor for building or updating an .axs container.
 */
data class AxsWriteSource(
    val name: String,
    val mime: String,
    val openStream: () -> InputStream
)

data class AxsTrackWriteData(
    val id: String?,
    val number: Int?,
    val discNumber: Int?,
    val title: String,
    val subtitle: String?,
    val artists: List<String>,
    val artistCredits: List<AxsArtistCredit>,
    val date: String?,
    val year: Int?,
    val description: String?,
    val lyrics: String?,
    val duration: Long?,
    val composer: String?,
    val hasVideo: Boolean,
    val parentTrackId: String?,
    val playbackVolume: Float,
    val mediaSource: AxsWriteSource
)

data class AxsAlbumWriteData(
    val id: String?,
    val title: String,
    val artists: List<String>,
    val artistCredits: List<AxsArtistCredit>,
    val year: Int?,
    val discNumber: Int?,
    val genre: String?,
    val description: String?,
    val coverSource: AxsWriteSource?,
    val photoSources: List<AxsWriteSource> = emptyList(),
    val mediaFileSources: List<AxsWriteSource> = emptyList(),
    val backgroundTextureSource: AxsWriteSource? = null,
    val backgroundTextureScale: Float = 1f,
    val backgroundTextureFit: String = "cover",
    val backgroundTextureOffsetX: Float = 0f,
    val backgroundTextureOffsetY: Float = 0f,
    val backgroundTextureBlendMode: String = "multiply",
    val backgroundTextureKeyColor: Int = -1,
    val backgroundTextureTolerance: Float = 32f,
    val backgroundTextureOpacity: Float = 1f,
    val tracks: List<AxsTrackWriteData>,
    val preservedRawJson: JSONObject? = null
)

object AxsContainer {
    internal const val HEADER_SIZE = 32
    private const val MAGIC = "AXSC"
    private const val FORMAT_VERSION = 1
    private const val MAX_MANIFEST_SIZE = 64L * 1024 * 1024

    fun read(file: File): AxsAlbum = RandomAccessFile(file, "r").use { input ->
        require(input.length() >= HEADER_SIZE) { "AXS файл слишком короткий" }
        val header = ByteArray(HEADER_SIZE)
        input.readFully(header)
        require(String(header, 0, 4, Charsets.US_ASCII) == MAGIC) { "Неверная сигнатура AXS" }
        val version = (header[4].toInt() and 255) or ((header[5].toInt() and 255) shl 8)
        require(version in 1..FORMAT_VERSION) { "Версия AXS $version не поддерживается" }
        fun u64(offset: Int): Long = ByteBuffer.wrap(header, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
        val manifestOffset = u64(8)
        val manifestLength = u64(16)
        require(manifestOffset >= HEADER_SIZE && manifestLength in 1..MAX_MANIFEST_SIZE && manifestOffset + manifestLength <= input.length()) {
            "Повреждённый манифест AXS"
        }
        val bytes = ByteArray(manifestLength.toInt())
        input.seek(manifestOffset)
        input.readFully(bytes)
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        require(root.optString("format") == "axs")

        fun blob(obj: JSONObject?): AxsBlob? {
            if (obj == null) return null
            val name = cleanString(obj.optString("file_name")) ?: return null
            val mime = cleanString(obj.optString("mime_type")) ?: "application/octet-stream"
            val offset = obj.optLong("offset")
            val length = obj.optLong("length")
            return AxsBlob(name, mime, offset, length)
        }

        fun blobList(array: JSONArray?): List<AxsBlob> {
            if (array == null) return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                blob(array.optJSONObject(i))
            }
        }

        fun artistCredits(array: JSONArray?): List<AxsArtistCredit> {
            if (array == null) return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val name = cleanString(obj.optString("name")) ?: return@mapNotNull null
                val role = cleanString(obj.optString("role")) ?: "MAIN"
                AxsArtistCredit(name, role)
            }
        }

        val albumJson = root.getJSONObject("album")
        val tracksJson = root.optJSONArray("tracks") ?: JSONArray()
        val tracks = (0 until tracksJson.length()).map { i ->
            val item = tracksJson.getJSONObject(i)
            val mediaObj = item.getJSONObject("media")
            val media = blob(mediaObj) ?: error("Track media blob missing")
            val rawVolume = if (item.isNull("playback_volume")) 1.0 else item.optDouble("playback_volume", 1.0)
            AxsTrack(
                id = cleanString(item.optString("id")),
                number = item.optInt("number").takeUnless { item.isNull("number") },
                discNumber = item.optInt("disc_number", 1).takeUnless { item.isNull("disc_number") } ?: 1,
                title = cleanString(item.optString("title")) ?: media.name.substringBeforeLast('.'),
                subtitle = nullableString(item, "subtitle"),
                artists = strings(item.optJSONArray("artists")),
                artistCredits = artistCredits(item.optJSONArray("artist_credits")),
                date = nullableString(item, "date"),
                year = item.optInt("year").takeUnless { item.isNull("year") },
                description = nullableString(item, "description"),
                lyrics = nullableString(item, "lyrics"),
                duration = item.optLong("duration_ms").takeIf { !item.isNull("duration_ms") },
                composer = nullableString(item, "composer"),
                hasVideo = item.optBoolean("has_video") || media.mime.startsWith("video/"),
                parentTrackId = nullableString(item, "parent_track_id"),
                playbackVolume = rawVolume.toFloat(),
                media = media
            )
        }

        tracks.forEach {
            require(it.media.offset >= HEADER_SIZE && it.media.length >= 0 && it.media.offset + it.media.length <= input.length()) {
                "Медиаблок трека выходит за пределы AXS"
            }
        }

        val cover = blob(albumJson.optJSONObject("cover"))
        val photos = blobList(albumJson.optJSONArray("photos"))
        val mediaFiles = blobList(albumJson.optJSONArray("media_files"))
        val texture = blob(albumJson.optJSONObject("background_texture"))

        (listOfNotNull(cover, texture) + photos + mediaFiles).forEach {
            require(it.offset >= HEADER_SIZE && it.length >= 0 && it.offset + it.length <= input.length()) {
                "Блок медиа/обложки выходит за пределы AXS"
            }
        }

        val albumArtists = strings(albumJson.optJSONArray("artists"))
        val albumArtistCredits = artistCredits(albumJson.optJSONArray("artist_credits"))
        val textureScale = if (albumJson.isNull("background_texture_scale")) 1f else albumJson.optDouble("background_texture_scale", 1.0).toFloat()
        val textureFit = cleanString(albumJson.optString("background_texture_fit")) ?: "cover"
        val textureOffsetX = albumJson.optDouble("background_texture_offset_x", 0.0).toFloat()
        val textureOffsetY = albumJson.optDouble("background_texture_offset_y", 0.0).toFloat()
        val textureBlend = cleanString(albumJson.optString("background_texture_blend_mode")) ?: "multiply"
        val textureKeyColor = albumJson.optInt("background_texture_key_color", -1)
        val textureTolerance = albumJson.optDouble("background_texture_tolerance", 32.0).toFloat()
        val textureOpacity = if (albumJson.isNull("background_texture_opacity")) 1f else albumJson.optDouble("background_texture_opacity", 1.0).toFloat()

        AxsAlbum(
            id = cleanString(albumJson.optString("id")),
            title = cleanString(albumJson.optString("title")) ?: file.nameWithoutExtension,
            artists = albumArtists,
            artistCredits = albumArtistCredits,
            year = albumJson.optInt("year").takeUnless { albumJson.isNull("year") },
            discNumber = albumJson.optInt("disc_number", 1).takeUnless { albumJson.isNull("disc_number") },
            genre = nullableString(albumJson, "genre"),
            description = nullableString(albumJson, "description"),
            cover = cover,
            photos = photos,
            mediaFiles = mediaFiles,
            backgroundTexture = texture,
            backgroundTextureScale = textureScale,
            backgroundTextureFit = textureFit,
            backgroundTextureOffsetX = textureOffsetX,
            backgroundTextureOffsetY = textureOffsetY,
            backgroundTextureBlendMode = textureBlend,
            backgroundTextureKeyColor = textureKeyColor,
            backgroundTextureTolerance = textureTolerance,
            backgroundTextureOpacity = textureOpacity,
            tracks = tracks,
            rawJson = root
        )
    }

    fun extract(source: File, blob: AxsBlob, target: File) = RandomAccessFile(source, "r").use { input ->
        require(blob.offset >= HEADER_SIZE && blob.length >= 0 && blob.offset + blob.length <= input.length())
        input.seek(blob.offset)
        target.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            var left = blob.length
            while (left > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                check(count > 0) { "Обрезанный блок AXS" }
                output.write(buffer, 0, count)
                left -= count
            }
        }
    }

    /**
     * Atomically writes an .axs container to [targetFile] by assembling a temporary file and renaming it.
     */
    fun writeContainer(targetFile: File, albumData: AxsAlbumWriteData) {
        val partFile = File(targetFile.parentFile ?: File("."), "${targetFile.name}.part_${System.currentTimeMillis()}")
        try {
            writePart(partFile, albumData)
            if (targetFile.exists()) {
                val backupFile = File(targetFile.parentFile ?: File("."), "${targetFile.name}.bak")
                if (targetFile.renameTo(backupFile)) {
                    if (partFile.renameTo(targetFile)) {
                        backupFile.delete()
                    } else {
                        backupFile.renameTo(targetFile)
                        error("Не удалось заменить целевой контейнер ${targetFile.name}")
                    }
                } else {
                    targetFile.delete()
                    if (!partFile.renameTo(targetFile)) {
                        error("Не удалось перезаписать ${targetFile.name}")
                    }
                }
            } else {
                if (!partFile.renameTo(targetFile)) {
                    error("Не удалось переименовать временный файл в ${targetFile.name}")
                }
            }
        } finally {
            if (partFile.exists()) partFile.delete()
        }
    }

    private fun writePart(partFile: File, albumData: AxsAlbumWriteData) {
        val out = partFile.outputStream().buffered(128 * 1024)
        out.use { stream ->
            // Write placeholder header (32 bytes)
            stream.write(ByteArray(HEADER_SIZE))
            var currentOffset = HEADER_SIZE.toLong()

            fun writeBlob(source: AxsWriteSource?): AxsBlob? {
                if (source == null) return null
                val start = currentOffset
                var length = 0L
                source.openStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        stream.write(buffer, 0, read)
                        length += read
                    }
                }
                currentOffset += length
                return AxsBlob(source.name, source.mime, start, length)
            }

            val coverBlob = writeBlob(albumData.coverSource)
            val textureBlob = writeBlob(albumData.backgroundTextureSource)
            val photoBlobs = albumData.photoSources.mapNotNull { writeBlob(it) }
            val mediaBlobs = albumData.mediaFileSources.mapNotNull { writeBlob(it) }

            val trackManifests = mutableListOf<Pair<AxsTrackWriteData, AxsBlob>>()
            for (track in albumData.tracks) {
                val mediaBlob = writeBlob(track.mediaSource) ?: error("Failed to write track media for ${track.title}")
                trackManifests.add(track to mediaBlob)
            }

            val manifestOffset = currentOffset
            val manifestJson = buildManifestJson(albumData, coverBlob, textureBlob, photoBlobs, mediaBlobs, trackManifests)
            val manifestBytes = manifestJson.toString(2).toByteArray(Charsets.UTF_8)
            stream.write(manifestBytes)
            val manifestLength = manifestBytes.size.toLong()

            stream.flush()

            // Update header
            RandomAccessFile(partFile, "rw").use { raf ->
                raf.seek(0)
                raf.write(MAGIC.toByteArray(Charsets.US_ASCII))
                raf.write((FORMAT_VERSION and 255))
                raf.write((FORMAT_VERSION shr 8) and 255)
                raf.write((HEADER_SIZE and 255))
                raf.write((HEADER_SIZE shr 8) and 255)

                val buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                buf.putLong(manifestOffset)
                buf.putLong(manifestLength)
                raf.write(buf.array())
                // Reserved (8 bytes of 0)
                raf.write(ByteArray(8))
            }
        }
    }

    private fun buildManifestJson(
        albumData: AxsAlbumWriteData,
        coverBlob: AxsBlob?,
        textureBlob: AxsBlob?,
        photoBlobs: List<AxsBlob>,
        mediaBlobs: List<AxsBlob>,
        trackManifests: List<Pair<AxsTrackWriteData, AxsBlob>>
    ): JSONObject {
        val root = if (albumData.preservedRawJson != null) {
            JSONObject(albumData.preservedRawJson.toString())
        } else {
            JSONObject()
        }

        root.put("format", "axs")
        root.put("version", FORMAT_VERSION)
        root.put("generator", "axs_player 1.0.0")
        root.put("container_name", albumData.title)
        val nowSec = System.currentTimeMillis() / 1000L
        if (!root.has("created_at") || root.optLong("created_at") <= 0) {
            root.put("created_at", nowSec)
        }
        root.put("updated_at", nowSec)

        fun blobJson(blob: AxsBlob?): JSONObject? = blob?.let {
            JSONObject().apply {
                put("file_name", it.name)
                put("mime_type", it.mime)
                put("offset", it.offset)
                put("length", it.length)
            }
        }

        val albumObj = root.optJSONObject("album") ?: JSONObject()
        albumData.id?.let { albumObj.put("id", it) }
        albumObj.put("title", albumData.title)
        albumObj.put("artists", JSONArray(albumData.artists))
        if (albumData.artistCredits.isNotEmpty()) {
            val creditsArr = JSONArray()
            albumData.artistCredits.forEach { credit ->
                creditsArr.put(JSONObject().apply {
                    put("name", credit.name)
                    put("role", credit.role)
                })
            }
            albumObj.put("artist_credits", creditsArr)
        }
        albumData.year?.let { albumObj.put("year", it) } ?: albumObj.remove("year")
        albumData.discNumber?.let { albumObj.put("disc_number", it) } ?: albumObj.remove("disc_number")
        albumData.genre?.let { albumObj.put("genre", it) } ?: albumObj.remove("genre")
        albumData.description?.let { albumObj.put("description", it) } ?: albumObj.remove("description")

        coverBlob?.let { albumObj.put("cover", blobJson(it)) } ?: albumObj.remove("cover")
        textureBlob?.let { albumObj.put("background_texture", blobJson(it)) } ?: albumObj.remove("background_texture")

        if (photoBlobs.isNotEmpty()) {
            val arr = JSONArray()
            photoBlobs.forEach { arr.put(blobJson(it)) }
            albumObj.put("photos", arr)
        } else {
            albumObj.remove("photos")
        }

        if (mediaBlobs.isNotEmpty()) {
            val arr = JSONArray()
            mediaBlobs.forEach { arr.put(blobJson(it)) }
            albumObj.put("media_files", arr)
        } else {
            albumObj.remove("media_files")
        }

        albumObj.put("background_texture_scale", albumData.backgroundTextureScale)
        albumObj.put("background_texture_fit", albumData.backgroundTextureFit)
        albumObj.put("background_texture_offset_x", albumData.backgroundTextureOffsetX)
        albumObj.put("background_texture_offset_y", albumData.backgroundTextureOffsetY)
        albumObj.put("background_texture_blend_mode", albumData.backgroundTextureBlendMode)
        albumObj.put("background_texture_key_color", albumData.backgroundTextureKeyColor)
        albumObj.put("background_texture_tolerance", albumData.backgroundTextureTolerance)
        albumObj.put("background_texture_opacity", albumData.backgroundTextureOpacity)

        root.put("album", albumObj)

        val tracksArray = JSONArray()
        for ((track, mediaBlob) in trackManifests) {
            val trackObj = JSONObject()
            track.id?.let { trackObj.put("id", it) }
            track.number?.let { trackObj.put("number", it) }
            track.discNumber?.let { trackObj.put("disc_number", it) }
            trackObj.put("title", track.title)
            track.subtitle?.let { trackObj.put("subtitle", it) }
            trackObj.put("artists", JSONArray(track.artists))
            if (track.artistCredits.isNotEmpty()) {
                val creditsArr = JSONArray()
                track.artistCredits.forEach { credit ->
                    creditsArr.put(JSONObject().apply {
                        put("name", credit.name)
                        put("role", credit.role)
                    })
                }
                trackObj.put("artist_credits", creditsArr)
            }
            track.date?.let { trackObj.put("date", it) }
            track.year?.let { trackObj.put("year", it) }
            track.description?.let { trackObj.put("description", it) }
            track.lyrics?.let { trackObj.put("lyrics", it) }
            track.duration?.let { trackObj.put("duration_ms", it) }
            track.composer?.let { trackObj.put("composer", it) }
            trackObj.put("has_video", track.hasVideo)
            track.parentTrackId?.let { trackObj.put("parent_track_id", it) }
            trackObj.put("playback_volume", track.playbackVolume)
            trackObj.put("media", blobJson(mediaBlob))

            tracksArray.put(trackObj)
        }
        root.put("tracks", tracksArray)

        return root
    }

    private fun strings(array: JSONArray?): List<String> =
        array?.let { (0 until it.length()).mapNotNull { n -> cleanString(it.optString(n)) } }.orEmpty()

    private fun nullableString(obj: JSONObject, key: String): String? =
        if (obj.isNull(key)) null else cleanString(obj.optString(key))

    fun cleanString(str: String?): String? {
        if (str == null) return null
        val trimmed = str.trim()
        if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return null
        return trimmed
    }
}
