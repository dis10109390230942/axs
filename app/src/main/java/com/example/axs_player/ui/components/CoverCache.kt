package com.example.axs_player.ui.components

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

private const val HEAP_FRACTION = 8
private const val BYTES_PER_PIXEL = 4
private const val DECODE_PARALLELISM = 3
private const val ORIGINAL_SIZE = 0
private const val COLOR_SAMPLE_SIZE = 128
private val SIZE_BUCKETS = intArrayOf(128, 256, 512)
private const val MAX_COLORS = 256
private const val MIN_LIGHTNESS = 0.5f
private const val MAX_LIGHTNESS = 0.65f
private const val MAX_SATURATION = 0.6f

private data class CoverKey(val uri: Uri, val size: Int)

object CoverCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(DECODE_PARALLELISM))
    private val loading = ConcurrentHashMap<CoverKey, Deferred<ImageBitmap?>>()
    private val bitmaps = object : LruCache<CoverKey, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / HEAP_FRACTION).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    ) {
        override fun sizeOf(key: CoverKey, value: ImageBitmap): Int = value.width * value.height * BYTES_PER_PIXEL
    }
    private val colors = LruCache<Uri, Color>(MAX_COLORS)

    fun get(uri: Uri, sizePx: Int): ImageBitmap? = bitmaps.get(CoverKey(uri, bucket(sizePx)))

    fun color(uri: Uri): Color? = colors.get(uri)

    fun clear() {
        bitmaps.evictAll()
        colors.evictAll()
    }

    suspend fun load(resolver: ContentResolver, uri: Uri, sizePx: Int): ImageBitmap? {
        val key = CoverKey(uri, bucket(sizePx))
        bitmaps.get(key)?.let { return it }
        return loading.computeIfAbsent(key) {
            scope.async(start = CoroutineStart.LAZY) {
                try {
                    decode(resolver, key.uri, key.size, isSoftware = false)?.also { bitmaps.put(key, it) }
                } finally {
                    loading.remove(key)
                }
            }
        }.also { it.start() }.await()
    }

    suspend fun loadColor(resolver: ContentResolver, uri: Uri): Color? {
        colors.get(uri)?.let { return it }
        val color = scope.async {
            decode(resolver, uri, COLOR_SAMPLE_SIZE, isSoftware = true)
                ?.let { Palette.from(it.asAndroidBitmap()).generate().dominantSwatch?.rgb }
                ?.let(::backgroundColor)
        }.await()
        color?.let { colors.put(uri, it) }
        return color
    }

    private fun bucket(sizePx: Int): Int = SIZE_BUCKETS.firstOrNull { it >= sizePx } ?: ORIGINAL_SIZE

    private fun decode(resolver: ContentResolver, uri: Uri, size: Int, isSoftware: Boolean): ImageBitmap? =
        runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                if (isSoftware) decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val shorterSide = min(info.size.width, info.size.height)
                if (size != ORIGINAL_SIZE && size < shorterSide) {
                    val scale = size.toFloat() / shorterSide
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }.asImageBitmap()
        }.getOrNull()

    private fun backgroundColor(rgb: Int): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(rgb, hsl)
        hsl[1] = hsl[1].coerceAtMost(MAX_SATURATION)
        hsl[2] = hsl[2].coerceIn(MIN_LIGHTNESS, MAX_LIGHTNESS)
        return Color(ColorUtils.HSLToColor(hsl))
    }
}
