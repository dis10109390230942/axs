package com.example.axs_player.data.library

import android.net.Uri
import android.provider.DocumentsContract.Document

data class LibraryFile(
    val name: String,
    val uri: Uri,
    val parentUri: Uri,
    val size: Long,
    val mimeType: String?
) {
    val isDirectory: Boolean get() = mimeType == Document.MIME_TYPE_DIR
    val extension: String get() = name.substringAfterLast('.', "").lowercase()
    val baseName: String get() = name.substringBeforeLast('.')
}
