package com.example.axs_player.data.library

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** Reads fragment sample durations when a fragmented MP4 leaves mvhd/mdhd duration at zero. */
internal class FragmentedMp4DurationReader(private val resolver: ContentResolver) {

    /** Returns the uniform shift applied to moof boxes while a tagger rewrote the MP4 metadata. */
    fun seekOffsetCorrection(uri: Uri): Long? = runCatching {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException(uri.toString())
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).channel.use { channel ->
            val fileSize = channel.size()
            val actualMoofOffsets = mutableSetOf<Long>()
            var indexedMoofOffsets: List<Long>? = null
            var position = 0L
            while (position + 8L <= fileSize) {
                val header = read(channel, position, 16) ?: break
                val size32 = header.int.toLong() and 0xffffffffL
                val type = header.int
                val headerSize = if (size32 == 1L) 16 else 8
                val boxSize = when (size32) {
                    0L -> fileSize - position
                    1L -> header.long
                    else -> size32
                }
                if (boxSize < headerSize || position + boxSize > fileSize) break
                when (type) {
                    TYPE_MOOF -> actualMoofOffsets += position
                    TYPE_MFRA -> {
                        if (boxSize <= MAX_BOX_SIZE) {
                            val mfra = read(channel, position, boxSize.toInt()) ?: return@use null
                            indexedMoofOffsets = parseMfraOffsets(mfra, headerSize, boxSize.toInt())
                        }
                    }
                }
                position += boxSize
            }
            val indexed = indexedMoofOffsets.orEmpty()
            if (actualMoofOffsets.isEmpty() || indexed.isEmpty()) return@use null
            val correction = actualMoofOffsets.minOrNull()!! - indexed.first()
            if (indexed.all { it + correction in actualMoofOffsets }) correction else null
        }
    }.getOrNull()

    fun durationMs(uri: Uri): Long? = runCatching {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException(uri.toString())
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).channel.use { channel -> readDurationMs(channel) }
    }.getOrNull()

    private fun readDurationMs(channel: FileChannel): Long? {
        val fileSize = channel.size()
        if (fileSize < 16L) return null
        val timescales = mutableMapOf<Long, Long>()
        val trexDurations = mutableMapOf<Long, Long>()
        val trackEnds = mutableMapOf<Long, Long>()
        var durationUs = 0L
        var position = 0L

        while (position + 8L <= fileSize) {
            val header = read(channel, position, 16) ?: break
            val size32 = header.int.toLong() and 0xffffffffL
            val type = header.int
            val headerSize = if (size32 == 1L) 16 else 8
            val boxSize = when (size32) {
                0L -> fileSize - position
                1L -> header.long
                else -> size32
            }
            if (boxSize < headerSize || position + boxSize > fileSize) break

            when (type) {
                TYPE_MOOV -> {
                    if (boxSize > MAX_BOX_SIZE) return null
                    val moov = read(channel, position, boxSize.toInt()) ?: return null
                    parseMoov(moov, headerSize, boxSize.toInt(), timescales, trexDurations)
                }
                TYPE_MOOF -> {
                    if (boxSize > MAX_BOX_SIZE) return null
                    val moof = read(channel, position, boxSize.toInt()) ?: return null
                    val fragmentEnds = parseMoof(moof, headerSize, boxSize.toInt(), trexDurations, trackEnds)
                    fragmentEnds.forEach { (trackId, end) ->
                        trackEnds[trackId] = end
                        val timescale = timescales[trackId] ?: return@forEach
                        if (timescale > 0L) durationUs = maxOf(durationUs, end * 1_000_000L / timescale)
                    }
                }
            }
            position += boxSize
        }
        return durationUs.takeIf { it > 0L }?.let { (it + 500L) / 1_000L }
    }

    private fun parseMoov(
        data: ByteBuffer,
        start: Int,
        end: Int,
        timescales: MutableMap<Long, Long>,
        trexDurations: MutableMap<Long, Long>
    ) {
        boxes(data, start, end).forEach { (type, boxStart, boxEnd, payload) ->
            when (type) {
                TYPE_TRAK -> {
                    var trackId: Long? = null
                    var timescale: Long? = null
                    boxes(data, payload, boxEnd).forEach { (childType, _, childEnd, childPayload) ->
                        when (childType) {
                            TYPE_TKHD -> trackId = parseTrackId(data, childPayload, childEnd)
                            TYPE_MDIA -> boxes(data, childPayload, childEnd).firstOrNull { it.type == TYPE_MDHD }
                                ?.let { timescale = parseTimescale(data, it.end, it.payload) }
                        }
                    }
                    if (trackId != null && timescale != null && timescale!! > 0L) timescales[trackId!!] = timescale!!
                }
                TYPE_MVEX -> boxes(data, payload, boxEnd).filter { it.type == TYPE_TREX }.forEach { (_, _, childEnd, childPayload) ->
                    if (childEnd - childPayload >= 20) {
                        val trackId = u32(data, childPayload + 4)
                        val defaultDuration = u32(data, childPayload + 12)
                        if (defaultDuration > 0L) trexDurations[trackId] = defaultDuration
                    }
                }
            }
        }
    }

    private fun parseMoof(
        data: ByteBuffer,
        start: Int,
        end: Int,
        trexDurations: Map<Long, Long>,
        previousEnds: Map<Long, Long>
    ): Map<Long, Long> = buildMap {
        boxes(data, start, end).filter { it.type == TYPE_TRAF }.forEach { (_, _, trafEnd, trafPayload) ->
            var trackId: Long? = null
            var defaultDuration: Long? = null
            var decodeTime: Long? = null
            val runs = mutableListOf<Pair<Int, Int>>()
            boxes(data, trafPayload, trafEnd).forEach { (type, _, childEnd, childPayload) ->
                when (type) {
                    TYPE_TFHD -> {
                        if (childEnd - childPayload < 8) return@forEach
                        val flags = flags(data, childPayload)
                        trackId = u32(data, childPayload + 4)
                        var cursor = childPayload + 8
                        if (flags and 0x000001 != 0) cursor += 8
                        if (flags and 0x000002 != 0) cursor += 4
                        if (flags and 0x000008 != 0 && cursor + 4 <= childEnd) {
                            defaultDuration = u32(data, cursor)
                        }
                    }
                    TYPE_TFDT -> {
                        if (childEnd - childPayload >= 8) {
                            decodeTime = if (data.get(childPayload).toInt() == 1) u64(data, childPayload + 4)
                            else u32(data, childPayload + 4)
                        }
                    }
                    TYPE_TRUN -> runs += childPayload to childEnd
                }
            }
            val id = trackId ?: return@forEach
            var cursorTime = decodeTime ?: previousEnds[id] ?: 0L
            val defaultSampleDuration = defaultDuration ?: trexDurations[id] ?: 0L
            runs.forEach { (runPayload, runEnd) ->
                if (runEnd - runPayload < 8) return@forEach
                val runFlags = flags(data, runPayload)
                val sampleCount = u32(data, runPayload + 4).coerceAtMost(MAX_SAMPLES_PER_RUN.toLong()).toInt()
                var cursor = runPayload + 8
                if (runFlags and 0x000001 != 0) cursor += 4
                if (runFlags and 0x000004 != 0) cursor += 4
                repeat(sampleCount) {
                    var sampleDuration = defaultSampleDuration
                    if (runFlags and 0x000100 != 0) {
                        if (cursor + 4 > runEnd) return@repeat
                        sampleDuration = u32(data, cursor)
                        cursor += 4
                    }
                    if (runFlags and 0x000200 != 0) cursor += 4
                    if (runFlags and 0x000400 != 0) cursor += 4
                    if (runFlags and 0x000800 != 0) cursor += 4
                    if (cursor > runEnd) return@repeat
                    cursorTime += sampleDuration
                }
            }
            if (cursorTime > 0L) put(id, cursorTime)
        }
    }

    private fun parseMfraOffsets(data: ByteBuffer, start: Int, end: Int): List<Long> =
        boxes(data, start, end).filter { it.type == TYPE_TFRA }.flatMap { box ->
            val payload = box.payload
            if (box.end - payload < 16) return@flatMap emptyList()
            val version = data.get(payload).toInt()
            val lengthFields = data.getInt(payload + 8)
            val entryCount = u32(data, payload + 12).coerceAtMost(MAX_SAMPLES_PER_RUN.toLong()).toInt()
            val entryWidth = (if (version == 1) 16 else 8) +
                (((lengthFields ushr 4) and 3) + 1) +
                (((lengthFields ushr 2) and 3) + 1) +
                ((lengthFields and 3) + 1)
            var cursor = payload + 16
            buildList {
                repeat(entryCount) {
                    if (cursor + entryWidth > box.end) return@repeat
                    val offset = if (version == 1) u64(data, cursor + 8) else u32(data, cursor + 4)
                    add(offset)
                    cursor += entryWidth
                }
            }
        }

    private fun parseTrackId(data: ByteBuffer, payload: Int, end: Int): Long? {
        if (end - payload < 20) return null
        val version = data.get(payload).toInt()
        val idOffset = payload + if (version == 1) 20 else 12
        return if (idOffset + 4 <= end) u32(data, idOffset) else null
    }

    private fun parseTimescale(data: ByteBuffer, end: Int, payload: Int): Long? {
        if (end - payload < 16) return null
        val version = data.get(payload).toInt()
        val scaleOffset = payload + if (version == 1) 20 else 12
        return if (scaleOffset + 4 <= end) u32(data, scaleOffset) else null
    }

    private fun boxes(data: ByteBuffer, start: Int, end: Int): List<Box> {
        val result = mutableListOf<Box>()
        var position = start
        while (position + 8 <= end) {
            val size32 = u32(data, position)
            val type = data.getInt(position + 4)
            val headerSize = if (size32 == 1L) 16 else 8
            val size = when (size32) {
                0L -> (end - position).toLong()
                1L -> if (position + 16 <= end) data.getLong(position + 8) else break
                else -> size32
            }
            if (size < headerSize || size > end - position) break
            val boxEnd = position + size.toInt()
            result += Box(type, position, boxEnd, position + headerSize)
            position = boxEnd
        }
        return result
    }

    private fun flags(data: ByteBuffer, payload: Int): Int =
        ((data.get(payload + 1).toInt() and 0xff) shl 16) or
            ((data.get(payload + 2).toInt() and 0xff) shl 8) or
            (data.get(payload + 3).toInt() and 0xff)

    private fun u32(data: ByteBuffer, offset: Int): Long = data.getInt(offset).toLong() and 0xffffffffL
    private fun u64(data: ByteBuffer, offset: Int): Long = data.getLong(offset).coerceAtLeast(0L)

    private fun read(channel: FileChannel, position: Long, size: Int): ByteBuffer? {
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        var offset = position
        while (buffer.hasRemaining()) {
            val count = channel.read(buffer, offset)
            if (count <= 0) return null
            offset += count
        }
        buffer.flip()
        return buffer
    }

    private data class Box(val type: Int, val start: Int, val end: Int, val payload: Int)

    private companion object {
        const val MAX_BOX_SIZE = 64 * 1024 * 1024
        const val MAX_SAMPLES_PER_RUN = 1_000_000
        const val TYPE_MOOV = 0x6d6f6f76
        const val TYPE_MOOF = 0x6d6f6f66
        const val TYPE_MFRA = 0x6d667261
        const val TYPE_TRAK = 0x7472616b
        const val TYPE_MDIA = 0x6d646961
        const val TYPE_MDHD = 0x6d646864
        const val TYPE_TKHD = 0x746b6864
        const val TYPE_MVEX = 0x6d766578
        const val TYPE_TREX = 0x74726578
        const val TYPE_TRAF = 0x74726166
        const val TYPE_TFHD = 0x74666864
        const val TYPE_TFDT = 0x74666474
        const val TYPE_TRUN = 0x7472756e
        const val TYPE_TFRA = 0x74667261
    }
}
