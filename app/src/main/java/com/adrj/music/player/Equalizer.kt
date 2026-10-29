package com.adrj.music.player

import android.media.audiofx.Equalizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class EqPreset(val name: String, val bands: List<Float>)

object EqManager {

    private var eq: Equalizer? = null

    var enabled by mutableStateOf(false)
        private set
    var currentPreset by mutableStateOf("Flat")
        private set

    var bands by mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f))
        private set

    val presets = listOf(
        EqPreset("Flat",       listOf(0f,   0f,   0f,   0f,   0f)),
        EqPreset("Bass",       listOf(8f,   5f,   2f,  -1f,  -2f)),
        EqPreset("Rock",       listOf(5f,   2f,  -1f,   3f,   5f)),
        EqPreset("Pop",        listOf(-1f,  2f,   4f,   3f,  -1f)),
        EqPreset("Jazz",       listOf(3f,   2f,  -2f,   2f,   4f)),
        EqPreset("Clásica",    listOf(4f,   2f,  -1f,   3f,   4f)),
        EqPreset("Vocal",      listOf(-2f, -1f,   4f,   5f,   2f)),
        EqPreset("Electrónica",listOf(6f,   3f,   1f,   2f,   5f))
    )

    fun init(audioSessionId: Int) {
        try {
            release()
            eq = Equalizer(0, audioSessionId).apply { setEnabled(false) }
            android.util.Log.d("ADRJ-EQ", "EQ inicializado con sessionId=$audioSessionId")
        } catch (e: Exception) {
            android.util.Log.e("ADRJ-EQ", "init error", e)
            eq = null
        }
    }

    fun enable(on: Boolean) {
        enabled = on
        try {
            eq?.setEnabled(on)
        } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
    }

    fun applyPreset(preset: EqPreset) {
        currentPreset = preset.name
        bands = preset.bands
        try {
            val e = eq ?: return
            val min = e.bandLevelRange[0]
            val max = e.bandLevelRange[1]
            preset.bands.forEachIndexed { i, db ->
                if (i < e.numberOfBands) {
                    val level = (db * 100).toInt().coerceIn(min.toInt(), max.toInt()).toShort()
                    e.setBandLevel(i.toShort(), level)
                }
            }
            android.util.Log.d("ADRJ-EQ", "preset aplicado: ${preset.name}")
        } catch (e: Exception) {
            android.util.Log.e("ADRJ-EQ", "preset error", e)
        }
    }

    fun setBand(index: Int, db: Float) {
        val newBands = bands.toMutableList()
        newBands[index] = db
        bands = newBands
        currentPreset = "Custom"
        try {
            val e = eq ?: return
            val min = e.bandLevelRange[0]
            val max = e.bandLevelRange[1]
            val level = (db * 100).toInt().coerceIn(min.toInt(), max.toInt()).toShort()
            e.setBandLevel(index.toShort(), level)
        } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
    }

    fun release() {
        try { eq?.release() } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
        eq = null
    }

    fun hasSession(): Boolean = eq != null
}
