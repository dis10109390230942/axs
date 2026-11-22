package com.example.axs_player.ui.screens.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.axs_player.AxsApplication
import com.example.axs_player.playback.PlaybackSettings
import com.example.axs_player.playback.PlaybackSettingsStore
import kotlinx.coroutines.flow.StateFlow

class PlaybackViewModel(private val store: PlaybackSettingsStore) : ViewModel() {

    val settings: StateFlow<PlaybackSettings> = store.settings

    fun setGapless(value: Boolean) = store.update { it.copy(isGapless = value) }

    fun setCrossfadeSeconds(value: Int) = store.update { it.copy(crossfadeSeconds = value) }

    fun setMono(value: Boolean) = store.update { it.copy(isMono = value) }

    fun setNormalized(value: Boolean) = store.update { it.copy(isNormalized = value) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AxsApplication
                PlaybackViewModel(application.playbackSettings)
            }
        }
    }
}
