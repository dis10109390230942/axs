package com.example.axs_player.playback

import androidx.media3.common.Player

enum class RepeatMode(val playerRepeatMode: Int) {
    OFF(Player.REPEAT_MODE_OFF),
    ALL(Player.REPEAT_MODE_ALL),
    ONE(Player.REPEAT_MODE_ONE);

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromPlayerRepeatMode(mode: Int): RepeatMode = entries.firstOrNull { it.playerRepeatMode == mode } ?: OFF
    }
}
