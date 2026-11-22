package com.example.axs_player.data.search

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFERENCES_NAME = "search_history"
private const val KEY_TRACK_IDS = "track_ids"
private const val SEPARATOR = "\n"
private const val MAX_RECENTS = 15

class SearchHistoryStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _trackIds = MutableStateFlow(load())
    val trackIds: StateFlow<List<String>> = _trackIds.asStateFlow()

    fun clear() {
        _trackIds.value = emptyList()
        preferences.edit { remove(KEY_TRACK_IDS) }
    }

    fun add(trackId: String) {
        val updated = (listOf(trackId) + _trackIds.value.filterNot { it == trackId }).take(MAX_RECENTS)
        _trackIds.value = updated
        preferences.edit { putString(KEY_TRACK_IDS, updated.joinToString(SEPARATOR)) }
    }

    private fun load(): List<String> =
        preferences.getString(KEY_TRACK_IDS, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotEmpty() }
            .orEmpty()
}
