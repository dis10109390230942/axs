package com.example.axs_player.data.library

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val failed: Int
)

sealed interface ImportState {
    data object Idle : ImportState
    data class Running(val processed: Int, val total: Int) : ImportState
    data class Finished(val result: ImportResult) : ImportState
    data object Failed : ImportState
}
