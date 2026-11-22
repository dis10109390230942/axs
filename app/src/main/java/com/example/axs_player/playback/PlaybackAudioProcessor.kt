package com.example.axs_player.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.exp
import kotlin.math.sqrt

private const val BYTES_PER_SAMPLE = 2
private const val SAMPLE_SCALE = 32768f
private const val SAMPLE_MAX = 32767f
private const val TARGET_MEAN_SQUARE = 0.0158f
private const val SILENCE_MEAN_SQUARE = 0.00001f
private const val MIN_GAIN = 0.25f
private const val MAX_GAIN = 2.5f
private const val LOUDNESS_WINDOW_S = 3f
private const val GAIN_SMOOTHING_S = 1f

@OptIn(UnstableApi::class)
class PlaybackAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isMono = false

    @Volatile
    var isNormalized = false

    @Volatile
    var trackVolume = 1f

    private var samples = FloatArray(0)
    private var meanSquareAlpha = 0f
    private var gainAlpha = 0f
    private var meanSquare = TARGET_MEAN_SQUARE
    private var gain = 1f
    private var sampleRate = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
        samples = FloatArray(inputAudioFormat.channelCount)
        sampleRate = inputAudioFormat.sampleRate
        val rate = sampleRate.toFloat()
        meanSquareAlpha = 1f - exp(-1f / (LOUDNESS_WINDOW_S * rate))
        gainAlpha = 1f - exp(-1f / (GAIN_SMOOTHING_S * rate))
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = samples.size
        val frameBytes = channels * BYTES_PER_SAMPLE
        val frames = inputBuffer.remaining() / frameBytes
        if (frames == 0) return
        val output = replaceOutputBuffer(frames * frameBytes)
        val isMixed = isMono && channels > 1
        val isLevelled = isNormalized
        repeat(frames) {
            var sum = 0f
            var energy = 0f
            for (channel in 0 until channels) {
                val sample = inputBuffer.short / SAMPLE_SCALE
                samples[channel] = sample
                sum += sample
                energy += sample * sample
            }
            if (isMixed) samples.fill(sum / channels)
            if (isLevelled) {
                meanSquare += (energy / channels - meanSquare) * meanSquareAlpha
                if (meanSquare > SILENCE_MEAN_SQUARE) {
                    val target = sqrt(TARGET_MEAN_SQUARE / meanSquare).coerceIn(MIN_GAIN, MAX_GAIN)
                    gain += (target - gain) * gainAlpha
                }
            } else {
                gain = 1f
            }
            for (channel in 0 until channels) {
                output.putShort(((samples[channel] * gain * trackVolume).coerceIn(-1f, 1f) * SAMPLE_MAX).toInt().toShort())
            }
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    override fun onReset() {
        samples = FloatArray(0)
        sampleRate = 0
        meanSquare = TARGET_MEAN_SQUARE
        gain = 1f
    }
}
