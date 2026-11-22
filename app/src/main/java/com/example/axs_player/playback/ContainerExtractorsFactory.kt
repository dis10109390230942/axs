package com.example.axs_player.playback

import android.net.Uri
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ForwardingExtractor
import androidx.media3.extractor.ForwardingExtractorOutput
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import android.util.Log

/** Extractors factory for .axs container tracks that enables seeking for all formats
 * by providing a constant-bitrate seek map based on known track byte boundaries. */
internal class ContainerExtractorsFactory(
    private val trackDurationUs: Long?,
    private val trackLengthBytes: Long?,
    private val mimeType: String? = null  // for duration estimation when manifest duration unknown
) : ExtractorsFactory {

    private val delegate = DefaultExtractorsFactory()
        .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_READ_MFRA_FOR_SEEK_MAP)
        .setConstantBitrateSeekingEnabled(true)
        .setConstantBitrateSeekingAlwaysEnabled(true)

    override fun createExtractors(): Array<Extractor> = wrap(delegate.createExtractors())

    override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
        wrap(delegate.createExtractors(uri, responseHeaders))

    private fun wrap(extractors: Array<Extractor>): Array<Extractor> = extractors.map { extractor ->
        Log.d("ContainerExtractorsFactory", "wrap: extractor=${extractor::class.simpleName}, trackDurationUs=$trackDurationUs, trackLengthBytes=$trackLengthBytes, mimeType=$mimeType")
        // Always wrap container tracks with ContainerSeekMap when we know byte length.
        // If manifest duration unknown, estimate from bitrate assumption.
        if (trackLengthBytes != null && trackLengthBytes > 0) {
            val effectiveDurationUs = trackDurationUs ?: estimateDurationUs(mimeType, trackLengthBytes)
            Log.d("ContainerExtractorsFactory", "wrap: using ContainerSeekMap, effectiveDurationUs=$effectiveDurationUs")
            object : ForwardingExtractor(extractor) {
                override fun init(output: ExtractorOutput) {
                    super.init(object : ForwardingExtractorOutput(output) {
                        override fun seekMap(seekMap: SeekMap) {
                            Log.d("ContainerExtractorsFactory", "seekMap override: original=${seekMap::class.simpleName}, isSeekable=${seekMap.isSeekable}, durationUs=${seekMap.durationUs}")
                            super.seekMap(ContainerSeekMap(seekMap, effectiveDurationUs, trackLengthBytes))
                        }
                    })
                }
            }
        } else {
            extractor
        }
    }.toTypedArray()

    /** Estimate duration (microseconds) from file size and mime type using constant bitrate assumption. */
    private fun estimateDurationUs(mimeType: String?, lengthBytes: Long): Long {
        val bitrateBps = when (mimeType?.lowercase()) {
            "mp3" -> 192_000L   // ~192 kbps typical
            "flac" -> 700_000L  // ~700 kbps typical lossless
            "m4a", "aac", "mp4" -> 128_000L  // ~128 kbps typical
            "ogg", "opus" -> 96_000L  // ~96 kbps typical
            "wav" -> 1_411_200L  // 44.1kHz 16-bit stereo
            else -> 128_000L  // default assumption
        }
        // durationUs = (bytes * 8) / bitrateBps * 1_000_000
        return (lengthBytes * 8 * 1_000_000L / bitrateBps).coerceAtLeast(1_000_000L)
    }
}

private class ContainerSeekMap(
    private val delegate: SeekMap,
    private val trackDurationUs: Long,
    private val trackLengthBytes: Long
) : SeekMap {

    private companion object {
        // Leave room for reading after seek point so playback doesn't end immediately
        private const val SAFE_READ_SIZE = 65536L // 64KB
    }

    override fun isSeekable(): Boolean = true

    override fun getDurationUs(): Long = trackDurationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        val safeTimeUs = timeUs.coerceAtLeast(0L)
        val position = if (trackDurationUs > 0 && trackLengthBytes > 0) {
            // Linear mapping: time -> byte position (constant bitrate assumption)
            // Don't clamp timeUs to trackDurationUs - allow seeking beyond estimated duration
            // Clamp position to leave SAFE_READ_SIZE bytes for reading after seek
            (safeTimeUs * trackLengthBytes / trackDurationUs)
                .coerceAtMost(trackLengthBytes - SAFE_READ_SIZE)
                .coerceAtLeast(0L)
        } else 0L
        val seekPoint = SeekPoint(safeTimeUs, position)
        return SeekMap.SeekPoints(seekPoint, seekPoint)
    }

    override fun isEstimated(): Boolean = true
}