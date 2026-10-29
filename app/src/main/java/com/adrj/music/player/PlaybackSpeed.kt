package com.adrj.music.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PlaybackSpeed {
    var current by mutableStateOf(1.0f)
        private set

    fun set(speed: Float) {
        val s = speed.coerceIn(0.5f, 2.0f)
        current = s
        try {
            PlayerHolder.controller?.setPlaybackSpeed(s)
        } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
    }

    fun cycle() {
        val options = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val idx = options.indexOfFirst { it >= current }
        val next = if (idx == -1) 1.0f else options[(idx + 1) % options.size]
        set(next)
    }

    fun label(): String = when (current) {
        0.5f -> "0.5x"
        0.75f -> "0.75x"
        1.0f -> "1x"
        1.25f -> "1.25x"
        1.5f -> "1.5x"
        2.0f -> "2x"
        else -> "${current}x"
    }
}
