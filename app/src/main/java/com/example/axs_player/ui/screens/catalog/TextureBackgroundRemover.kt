package com.example.axs_player.ui.screens.catalog

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

/** Removes only background-colored pixels connected to the image edges. */
internal fun removeTextureBackground(source: Bitmap, requestedColor: Int, tolerance: Float): Bitmap? = runCatching {
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    val width = bitmap.width
    val height = bitmap.height
    val count = width * height
    val pixels = IntArray(count)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val keyColor = if (requestedColor != -1) requestedColor else {
        val corners = intArrayOf(pixels[0], pixels[width - 1], pixels[(height - 1) * width], pixels[count - 1])
        Color.rgb(corners.map(Color::red).average().toInt(), corners.map(Color::green).average().toInt(), corners.map(Color::blue).average().toInt())
    }
    val keyR = Color.red(keyColor)
    val keyG = Color.green(keyColor)
    val keyB = Color.blue(keyColor)
    val limit = tolerance.coerceAtLeast(1f)
    val seen = BooleanArray(count)
    val queue = IntArray(count)
    var head = 0
    var tail = 0

    fun isBackground(index: Int): Boolean {
        val color = pixels[index]
        if (Color.alpha(color) == 0) return true
        val dr = Color.red(color) - keyR
        val dg = Color.green(color) - keyG
        val db = Color.blue(color) - keyB
        return sqrt((dr * dr + dg * dg + db * db).toFloat()) <= limit
    }
    fun add(index: Int) {
        if (!seen[index] && isBackground(index)) {
            seen[index] = true
            queue[tail++] = index
        }
    }

    for (x in 0 until width) { add(x); add((height - 1) * width + x) }
    for (y in 0 until height) { add(y * width); add(y * width + width - 1) }
    while (head < tail) {
        val index = queue[head++]
        val x = index % width
        if (x > 0) add(index - 1)
        if (x + 1 < width) add(index + 1)
        if (index >= width) add(index - width)
        if (index + width < count) add(index + width)
    }

    // Feather the cut edge while retaining the original colors of the texture.
    for (index in 0 until count) {
        val color = pixels[index]
        if (seen[index]) {
            val dr = Color.red(color) - keyR
            val dg = Color.green(color) - keyG
            val db = Color.blue(color) - keyB
            val distance = sqrt((dr * dr + dg * dg + db * db).toFloat())
            val keep = ((distance - limit * 0.65f) / (limit * 0.35f)).coerceIn(0f, 1f)
            val alpha = (Color.alpha(color) * keep).toInt()
            pixels[index] = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    bitmap
}.getOrNull()
