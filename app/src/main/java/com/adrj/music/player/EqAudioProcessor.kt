package com.adrj.music.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ecualizador real con filtros biquad.
 * 5 bandas: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
 * Aplicado directamente sobre el stream de ExoPlayer.
 */
class EqAudioProcessor : BaseAudioProcessor() {

    private var sampleRate = 44100

    // Coeficientes biquad por banda (calculados al configurar)
    private class Biquad {
        var a0 = 1.0; var a1 = 0.0; var a2 = 0.0
        var b0 = 1.0; var b1 = 0.0; var b2 = 0.0
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0

        fun process(input: Double): Double {
            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = input
            y2 = y1; y1 = output
            return output
        }

        fun reset() {
            x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
        }

        fun configurePeaking(freq: Double, q: Double, gainDb: Double, sr: Double) {
            val A = Math.pow(10.0, gainDb / 40.0)
            val w0 = 2 * PI * freq / sr
            val alpha = sin(w0) / (2 * q)
            val cosW0 = cos(w0)
            val b0n = 1 + alpha * A
            val b1n = -2 * cosW0
            val b2n = 1 - alpha * A
            val a0n = 1 + alpha / A
            val a1n = -2 * cosW0
            val a2n = 1 - alpha / A
            b0 = b0n / a0n; b1 = b1n / a0n; b2 = b2n / a0n
            a0 = 1.0; a1 = a1n / a0n; a2 = a2n / a0n
        }
    }

    private val bands = Array(5) { Biquad() }
    private val bandFreqs = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)
    private val lastGains = FloatArray(5) { 0f }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        // Aplicar los coeficientes actuales
        for (i in 0 until 5) {
            bands[i].configurePeaking(bandFreqs[i], 1.0, lastGains[i].toDouble(), sampleRate.toDouble())
            bands[i].reset()
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val enabled = EqManager.enabled
        val size = inputBuffer.remaining()

        // Si no está activo, pasar directo
        if (!enabled) {
            val buf = replaceOutputBuffer(size)
            buf.put(inputBuffer)
            buf.flip()
            return
        }

        // Actualizar coeficientes si las bandas cambiaron
        val currentBands = EqManager.bands
        for (i in 0 until 5) {
            if (i < currentBands.size && lastGains[i] != currentBands[i]) {
                lastGains[i] = currentBands[i]
                bands[i].configurePeaking(
                    bandFreqs[i], 1.0, currentBands[i].toDouble(), sampleRate.toDouble()
                )
            }
        }

        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(size)

        // Asumimos 16-bit PCM (C.ENCODING_PCM_16BIT)
        val samples = size / 2
        for (i in 0 until samples) {
            var s = input.short.toDouble()
            for (band in bands) {
                s = band.process(s)
            }
            val out = s.toInt().coerceIn(-32768, 32767).toShort()
            output.putShort(out)
        }

        output.flip()
    }

    override fun onReset() {
        for (band in bands) band.reset()
    }
}
