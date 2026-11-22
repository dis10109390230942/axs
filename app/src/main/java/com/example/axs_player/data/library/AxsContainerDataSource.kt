package com.example.axs_player.data.library

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.channels.FileChannel
import android.util.Log

/**
 * DataSource that reads a specific blob from an .axs container via SAF.
 * Uses ParcelFileDescriptor + FileChannel for fast seeking.
 * Allows ExoPlayer to play tracks directly from the container without extraction.
 * 
 * Return values for read():
 * -1 = RESULT_END_OF_INPUT (end of stream)
 * -2 = RESULT_ERROR_IO (I/O error)
 */
class AxsContainerDataSource(
    private val resolver: ContentResolver,
    private val containerUri: android.net.Uri,
    private val blobOffset: Long,
    private val blobLength: Long
) : androidx.media3.datasource.BaseDataSource(/* isNetwork= */ false) {

    private var fileChannel: FileChannel? = null
    private var pfd: ParcelFileDescriptor? = null
    private var fis: java.io.FileInputStream? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    companion object {
        const val RESULT_END_OF_INPUT = -1
        const val RESULT_ERROR_IO = -2
    }

    override fun open(dataSpec: DataSpec): Long {
        if (opened) {
            throw IllegalStateException("DataSource already opened")
        }

        Log.d("AxsContainerDataSource", "open: containerUri=$containerUri, blobOffset=$blobOffset, blobLength=$blobLength, dataSpec.position=${dataSpec.position}, dataSpec.length=${dataSpec.length}")

        // Open ParcelFileDescriptor for the container, then get FileChannel for fast seeking
        pfd = resolver.openFileDescriptor(containerUri, "r")
            ?: throw IOException("Failed to open container: $containerUri")
        val fis = java.io.FileInputStream(pfd!!.fileDescriptor)
        fileChannel = fis.channel

        val targetPosition = blobOffset + dataSpec.position
        fileChannel!!.position(targetPosition)

        // Handle LENGTH_UNSET (-1) correctly
        val requestedLength = if (dataSpec.length == androidx.media3.common.C.LENGTH_UNSET.toLong()) {
            blobLength - dataSpec.position
        } else {
            dataSpec.length
        }
        bytesRemaining = kotlin.math.min(blobLength - dataSpec.position, requestedLength)
        if (bytesRemaining < 0) bytesRemaining = 0
        opened = true

        Log.d("AxsContainerDataSource", "open: returning blobLength=$blobLength, bytesRemaining=$bytesRemaining, targetPosition=$targetPosition")

        // Return total blob length for proper duration/seek calculation
        return blobLength
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened || fileChannel == null) {
            Log.w("AxsContainerDataSource", "read: not opened or no channel")
            return RESULT_ERROR_IO
        }
        if (readLength <= 0) return 0
        if (bytesRemaining <= 0) {
            Log.d("AxsContainerDataSource", "read: bytesRemaining <= 0, returning END_OF_INPUT")
            return RESULT_END_OF_INPUT
        }

        val toRead = kotlin.math.min(readLength.toLong(), bytesRemaining).toInt()
        val byteBuffer = java.nio.ByteBuffer.wrap(buffer, offset, toRead)
        val read = fileChannel!!.read(byteBuffer)
        if (read <= 0) {
            if (bytesRemaining > 0) {
                Log.w("AxsContainerDataSource", "read: read <= 0 but bytesRemaining > 0, returning ERROR_IO")
                return RESULT_ERROR_IO
            }
            Log.d("AxsContainerDataSource", "read: read <= 0, bytesRemaining <= 0, returning END_OF_INPUT")
            return RESULT_END_OF_INPUT
        }
        bytesRemaining -= read
        return read
    }

    override fun close() {
        if (opened) {
            try {
                fileChannel?.close()
                fis?.close()
                pfd?.close()
            } catch (e: IOException) {
                // ignore
            }
            fileChannel = null
            fis = null
            pfd = null
            opened = false
        }
    }

    override fun getUri(): android.net.Uri? = containerUri
}

/**
 * Factory for creating AxsContainerDataSource instances.
 */
class AxsContainerDataSourceFactory(
    private val resolver: ContentResolver,
    private val containerUri: android.net.Uri,
    private val blobOffset: Long,
    private val blobLength: Long
) : androidx.media3.datasource.DataSource.Factory {

    override fun createDataSource(): androidx.media3.datasource.DataSource {
        return AxsContainerDataSource(resolver, containerUri, blobOffset, blobLength)
    }
}