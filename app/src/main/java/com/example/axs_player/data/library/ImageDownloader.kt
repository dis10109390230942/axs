package com.example.axs_player.data.library

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

private const val HTTPS_SCHEME = "https"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000
private const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
private const val BUFFER_SIZE = 8 * 1024

object ImageDownloader {

    fun download(link: String): ByteArray? {
        val url = runCatching { URI(link.trim()).toURL() }.getOrNull()
            ?.takeIf { it.protocol == HTTPS_SCHEME }
            ?: return null
        val connection = runCatching { url.openConnection() as HttpURLConnection }.getOrNull() ?: return null
        return try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            if (connection.contentLengthLong > MAX_IMAGE_BYTES) return null
            connection.inputStream.use(::readLimited)
        } catch (e: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(input: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_IMAGE_BYTES) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
