package com.adrj.music.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

/**
 * AudioProcessor que aplica un efecto 8D (rotación estéreo).
 * Modula la amplitud del canal izquierdo/derecho con una onda lenta
 * para simular movimiento alrededor del oyente.
 */
class Audio8DProcessor : BaseAudioProcessor() {

    private var phase = 0.0
    private var sampleRate = 44100
    private val rotationSpeedHz = 0.25  // vuelta cada 4 segundos

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!Audio8D.enabled) {
            // Sin efecto: pasar directo
            val remaining = inputBuffer.remaining()
            val buf = replaceOutputBuffer(remaining)
            buf.put(inputBuffer)
            buf.flip()
            return
        }

        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val size = inputBuffer.remaining()
        val output = replaceOutputBuffer(size)

        // Solo procesamos si es estéreo (2 canales, 16 bits)
        val bytesPerFrame = 4  // 2 canales * 2 bytes
        val frames = size / bytesPerFrame
        val phaseStep = 2.0 * Math.PI * rotationSpeedHz / sampleRate

        for (i in 0 until frames) {
            val l = input.short
            val r = input.short

            // Factor de rotación: izquierda = cos, derecha = sin
            val leftGain = cos(phase).toFloat()
            val rightGain = sin(phase).toFloat()

            val newL = (l * leftGain).toInt().coerceIn(-32768, 32767).toShort()
            val newR = (r * rightGain).toInt().coerceIn(-32768, 32767).toShort()

            output.putShort(newL)
            output.putShort(newR)

            phase += phaseStep
            if (phase > 2 * Math.PI) phase -= 2 * Math.PI
        }

        output.flip()
    }

    override fun onReset() {
        phase = 0.0
    }
}
