package com.example.axs_player.playback

data class PlaybackState(
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val currentItem: QueueItem? get() = queue.getOrNull(currentIndex)
    val hasNext: Boolean
        get() = currentIndex >= 0 && (repeatMode == RepeatMode.ALL || currentIndex < queue.lastIndex)
    val hasPrevious: Boolean
        get() = currentIndex >= 0 && (repeatMode == RepeatMode.ALL || currentIndex > 0)
}
