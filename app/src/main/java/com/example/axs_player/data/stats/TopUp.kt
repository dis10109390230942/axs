package com.example.axs_player.data.stats

fun <T> topUp(ranked: List<T>, fallback: List<T>, limit: Int, id: (T) -> String): List<T> {
    val known = ranked.mapTo(mutableSetOf(), id)
    return (ranked + fallback.filterNot { id(it) in known }).take(limit)
}
