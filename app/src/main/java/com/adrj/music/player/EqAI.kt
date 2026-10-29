package com.adrj.music.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class DeviceInfo(
    val type: String,
    val name: String,
    val preset: String
)

object EqAI {

    var currentDevice by mutableStateOf<DeviceInfo?>(null)
        private set
    var aiEnabled by mutableStateOf(false)
        private set

    /**
     * Detecta el dispositivo de audio conectado y aplica el preset óptimo.
     * Es un "IA" en el sentido de que decide el mejor preset según el hardware.
     */
    fun autoAdjust(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            var device: DeviceInfo? = null

            for (d in devices) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device = when (d.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceInfo(
                            "Bluetooth", d.productName.toString(), "Rock"
                        )
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_WIRED_HEADSET -> DeviceInfo(
                            "Auriculares", d.productName.toString(), "Bass"
                        )
                        AudioDeviceInfo.TYPE_USB_HEADSET -> DeviceInfo(
                            "USB Audio", d.productName.toString(), "Jazz"
                        )
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> DeviceInfo(
                            "Altavoz", "Altavoz del teléfono", "Vocal"
                        )
                        else -> null
                    }
                    if (device != null) break
                }
            }

            // Si no se detectó ninguno, usar altavoz por defecto
            if (device == null) {
                device = DeviceInfo("Altavoz", "Altavoz del teléfono", "Vocal")
            }

            currentDevice = device
            aiEnabled = true

            // Aplicar el preset
            val preset = EqManager.presets.firstOrNull { it.name == device.preset }
                ?: EqManager.presets.first()
            EqManager.applyPreset(preset)
            EqManager.enable(true)

        } catch (_: Exception) {
            currentDevice = null
            aiEnabled = false
        }
    }

    fun disable() {
        aiEnabled = false
        currentDevice = null
    }
}
