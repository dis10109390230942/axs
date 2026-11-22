package com.example.axs_player.playback

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFERENCES_NAME = "playback_settings"
private const val KEY_GAPLESS = "gapless"
private const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
private const val KEY_MONO = "mono"
private const val KEY_NORMALIZED = "normalized"
const val MAX_CROSSFADE_SECONDS = 12

data class PlaybackSettings(
    val isGapless: Boolean = true,
    val crossfadeSeconds: Int = 3,
    val isMono: Boolean = false,
    val isNormalized: Boolean = false
)

class PlaybackSettingsStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<PlaybackSettings> = _settings.asStateFlow()

    fun update(transform: (PlaybackSettings) -> PlaybackSettings) {
        val current = _settings.value
        val updated = transform(current).let { it.copy(crossfadeSeconds = it.crossfadeSeconds.coerceIn(0, MAX_CROSSFADE_SECONDS)) }
        if (updated == current) return
        _settings.value = updated
        preferences.edit {
            putBoolean(KEY_GAPLESS, updated.isGapless)
            putInt(KEY_CROSSFADE_SECONDS, updated.crossfadeSeconds)
            putBoolean(KEY_MONO, updated.isMono)
            putBoolean(KEY_NORMALIZED, updated.isNormalized)
        }
    }

    private fun load(): PlaybackSettings {
        val defaults = PlaybackSettings()
        return PlaybackSettings(
            isGapless = preferences.getBoolean(KEY_GAPLESS, defaults.isGapless),
            crossfadeSeconds = preferences.getInt(KEY_CROSSFADE_SECONDS, defaults.crossfadeSeconds)
                .coerceIn(0, MAX_CROSSFADE_SECONDS),
            isMono = preferences.getBoolean(KEY_MONO, defaults.isMono),
            isNormalized = preferences.getBoolean(KEY_NORMALIZED, defaults.isNormalized)
        )
    }
}
