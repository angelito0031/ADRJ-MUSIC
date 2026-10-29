package com.adrj.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Audio 8D usando efectos nativos de Android:
 * - Virtualizer: sonido envolvente (izq/der girando)
 * - PresetReverb: espacio grande
 * - BassBoost: un poco de graves para sensación de inmersión
 */
object Audio8D {
    var enabled by mutableStateOf(false)
        private set

    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var bassBoost: BassBoost? = null

    fun init(audioSessionId: Int) {
        try {
            release()
            virtualizer = Virtualizer(0, audioSessionId).apply { setEnabled(false) }
            reverb = PresetReverb(0, audioSessionId).apply {
                preset = PresetReverb.PRESET_LARGEROOM
                setEnabled(false)
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                setStrength(500.toShort())  // 50% de fuerza
                setEnabled(false)
            }
            android.util.Log.d("ADRJ-8D", "Efectos inicializados con sessionId=$audioSessionId")
        } catch (e: Exception) {
            android.util.Log.e("ADRJ-8D", "init error", e)
            release()
        }
    }

    fun set(on: Boolean) {
        enabled = on
        try {
            virtualizer?.setEnabled(on)
            reverb?.setEnabled(on)
            bassBoost?.setEnabled(on)
            android.util.Log.d("ADRJ-8D", "set enabled=$on")
        } catch (e: Exception) {
            android.util.Log.e("ADRJ-8D", "set error", e)
        }
    }

    fun toggle() = set(!enabled)

    fun release() {
        try { virtualizer?.release() } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
        try { reverb?.release() } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
        try { bassBoost?.release() } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
        virtualizer = null
        reverb = null
        bassBoost = null
    }

    fun hasSession(): Boolean = virtualizer != null
}
