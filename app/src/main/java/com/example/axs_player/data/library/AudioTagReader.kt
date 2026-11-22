package com.example.axs_player.data.library

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.inspector.MetadataRetriever

private enum class TagField { TITLE, ARTIST, ALBUM_ARTIST, ALBUM, TRACK, DISC, YEAR, COMPOSER }

private val ID3_FIELDS = mapOf(
    "TIT2" to TagField.TITLE, "TT2" to TagField.TITLE,
    "TPE1" to TagField.ARTIST, "TP1" to TagField.ARTIST,
    "TPE2" to TagField.ALBUM_ARTIST, "TP2" to TagField.ALBUM_ARTIST,
    "TALB" to TagField.ALBUM, "TAL" to TagField.ALBUM,
    "TRCK" to TagField.TRACK, "TRK" to TagField.TRACK,
    "TPOS" to TagField.DISC, "TPA" to TagField.DISC,
    "TDRC" to TagField.YEAR, "TYER" to TagField.YEAR, "TYE" to TagField.YEAR, "TDRL" to TagField.YEAR,
    "TCOM" to TagField.COMPOSER, "TCM" to TagField.COMPOSER
)

private val VORBIS_FIELDS = mapOf(
    "TITLE" to TagField.TITLE,
    "ARTIST" to TagField.ARTIST,
    "ALBUMARTIST" to TagField.ALBUM_ARTIST,
    "ALBUM ARTIST" to TagField.ALBUM_ARTIST,
    "ALBUM" to TagField.ALBUM,
    "TRACKNUMBER" to TagField.TRACK,
    "DISCNUMBER" to TagField.DISC,
    "DATE" to TagField.YEAR,
    "YEAR" to TagField.YEAR,
    "COMPOSER" to TagField.COMPOSER
)

private val LEADING_NUMBER = Regex("^\\s*(\\d+)")

@OptIn(UnstableApi::class)
class AudioTagReader(private val context: Context) {

    private val fragmentedDurationReader = FragmentedMp4DurationReader(context.contentResolver)

    fun read(uri: Uri): AudioTags =
        MetadataRetriever.Builder(context, MediaItem.fromUri(uri)).build().use { retriever ->
            val trackGroups: TrackGroupArray = retriever.retrieveTrackGroups().get()
            val durationUs: Long = retriever.retrieveDurationUs().get()
            var hasVideo = false
            val entries = buildList<Metadata.Entry> {
                for (groupIndex in 0 until trackGroups.length) {
                    val group = trackGroups[groupIndex]
                    for (formatIndex in 0 until group.length) {
                        val format = group.getFormat(formatIndex)
                        if (MimeTypes.isVideo(format.sampleMimeType)) hasVideo = true
                        val metadata: Metadata = format.metadata ?: continue
                        for (entryIndex in 0 until metadata.length()) add(metadata[entryIndex])
                    }
                }
            }
            val containerDurationMs = if (hasVideo) fragmentedDurationReader.durationMs(uri) else null
            val fallbackDurationMs = containerDurationMs
                ?: durationUs.takeIf { it != C.TIME_UNSET && it > 0L }?.div(1000)
            parse(entries.distinct(), durationUs, hasVideo, fallbackDurationMs)
        }

    private fun parse(
        entries: List<Metadata.Entry>,
        durationUs: Long,
        hasVideo: Boolean,
        fallbackDurationMs: Long?
    ): AudioTags {
        val fields = mutableMapOf<TagField, MutableList<String>>()
        entries.forEach { entry ->
            when (entry) {
                is TextInformationFrame -> ID3_FIELDS[entry.id]?.let { fields.getOrPut(it) { mutableListOf() } += entry.values }
                is VorbisComment -> VORBIS_FIELDS[entry.key]?.let { fields.getOrPut(it) { mutableListOf() } += entry.value }
                else -> Unit
            }
        }
        val fallback = MediaMetadata.Builder().apply { entries.forEach { it.populateMediaMetadata(this) } }.build()

        fun values(field: TagField): List<String> = fields[field].orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
        fun first(field: TagField): String? = values(field).firstOrNull()

        return AudioTags(
            title = first(TagField.TITLE) ?: fallback.title.asTag(),
            artists = values(TagField.ARTIST).ifEmpty { listOfNotNull(fallback.artist.asTag()) },
            albumArtists = values(TagField.ALBUM_ARTIST).ifEmpty { listOfNotNull(fallback.albumArtist.asTag()) },
            album = first(TagField.ALBUM) ?: fallback.albumTitle.asTag(),
            trackNumber = first(TagField.TRACK)?.leadingNumber() ?: fallback.trackNumber,
            discNumber = first(TagField.DISC)?.leadingNumber() ?: fallback.discNumber,
            year = first(TagField.YEAR)?.leadingNumber() ?: fallback.recordingYear ?: fallback.releaseYear,
            trackDate = first(TagField.YEAR),
            durationMs = durationUs.takeIf { it != C.TIME_UNSET && it > 0L }?.div(1000) ?: fallbackDurationMs,
            artwork = fallback.artworkData,
            hasVideo = hasVideo,
            composer = values(TagField.COMPOSER).joinToString(", ").ifEmpty { null } ?: fallback.composer.asTag()
        )
    }

    private fun CharSequence?.asTag(): String? = this?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun String.leadingNumber(): Int? = LEADING_NUMBER.find(this)?.groupValues?.get(1)?.toIntOrNull()
}
