package com.example.axs_player.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.C
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener

/** Supplies SAF file sizes to Media3 so fragmented MP4 extractors can read the trailing mfra index. */
internal class LengthAwareDataSource(
    private val context: Context,
    private val fallbackFactory: DataSource.Factory
) : DataSource {

    private val resolver = context.contentResolver

    private var active: DataSource? = null
    private val listeners = mutableListOf<TransferListener>()

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        active?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val isContentUri = dataSpec.uri.scheme == ContentResolver.SCHEME_CONTENT
        val source = if (isContentUri) ContentDataSource(context) else fallbackFactory.createDataSource()
        active = source
        listeners.forEach(source::addTransferListener)
        val size = if (isContentUri) contentSize(dataSpec.uri) else null
        val sourceSpec = if (size != null && size > 0L && dataSpec.length == C.LENGTH_UNSET.toLong()) {
            dataSpec.buildUpon().setLength((size - dataSpec.position).coerceAtLeast(0L)).build()
        } else dataSpec
        return source.open(sourceSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(active) { "Data source is not open" }.read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders.orEmpty()

    override fun close() {
        val source = active ?: return
        active = null
        source.close()
    }

    private fun contentSize(uri: Uri): Long? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0).takeIf { it > 0L } else null
        }
    }.getOrNull()

    class Factory(context: Context) : DataSource.Factory {
        private val appContext = context.applicationContext
        private val fallbackFactory = DefaultDataSource.Factory(context)

        override fun createDataSource(): DataSource = LengthAwareDataSource(appContext, fallbackFactory)
    }
}
