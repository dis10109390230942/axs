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
import androidx.media3.extractor.TrackAwareSeekMap
import androidx.media3.extractor.mp4.FragmentedMp4Extractor

/** Lets the fragmented MP4 extractor retain its mfra seek map while supplying the measured duration. */
internal class DurationAwareExtractorsFactory(
    private val overrideDurationUs: Long?,
    private val seekOffsetCorrection: Long?
) : ExtractorsFactory {

    private val delegate = DefaultExtractorsFactory()
        .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_READ_MFRA_FOR_SEEK_MAP)
        .setConstantBitrateSeekingEnabled(true)
        .setConstantBitrateSeekingAlwaysEnabled(true)

    override fun createExtractors(): Array<Extractor> = wrap(delegate.createExtractors())

    override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
        wrap(delegate.createExtractors(uri, responseHeaders))

    private fun wrap(extractors: Array<Extractor>): Array<Extractor> = extractors.map { extractor ->
        if (extractor !is FragmentedMp4Extractor ||
            ((overrideDurationUs == null || overrideDurationUs <= 0L) && (seekOffsetCorrection == null || seekOffsetCorrection == 0L))
        ) {
            extractor
        } else {
            val measuredDurationUs = overrideDurationUs?.takeIf { it > 0L }
            val offsetCorrection = seekOffsetCorrection ?: 0L
            object : ForwardingExtractor(extractor) {
                override fun init(output: ExtractorOutput) {
                    super.init(object : ForwardingExtractorOutput(output) {
                        override fun seekMap(seekMap: SeekMap) {
                            super.seekMap(CorrectedMfraSeekMap(seekMap, measuredDurationUs, offsetCorrection))
                        }
                    })
                }
            }
        }
    }.toTypedArray()
}

private class CorrectedMfraSeekMap(
    private val delegate: SeekMap,
    private val durationUs: Long?,
    private val offsetCorrection: Long
) : TrackAwareSeekMap {

    override fun isSeekable(): Boolean = delegate.isSeekable

    override fun isSeekable(trackId: Int): Boolean =
        (delegate as? TrackAwareSeekMap)?.isSeekable(trackId) ?: delegate.isSeekable

    override fun getDurationUs(): Long = durationUs ?: delegate.durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints = correct(delegate.getSeekPoints(timeUs))

    override fun getSeekPoints(timeUs: Long, trackId: Int): SeekMap.SeekPoints =
        (delegate as? TrackAwareSeekMap)?.getSeekPoints(timeUs, trackId)?.let(::correct)
            ?: correct(delegate.getSeekPoints(timeUs))

    override fun isEstimated(): Boolean = delegate.isEstimated

    private fun correct(points: SeekMap.SeekPoints): SeekMap.SeekPoints = SeekMap.SeekPoints(
        correct(points.first),
        correct(points.second)
    )

    private fun correct(point: SeekPoint): SeekPoint =
        if (offsetCorrection == 0L) point else SeekPoint(point.timeUs, point.position + offsetCorrection)
}
