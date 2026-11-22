package com.example.axs_player.data.library

import android.net.Uri

data class LibraryLayout(
    val tree: Uri,
    val inbox: Uri,
    val tracks: Uri,
    val metadata: Uri,
    val axsContainers: Uri
)
