package com.adrj.music.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SleepTimer {
    var remainingSeconds by mutableStateOf(0)
        private set
    var active by mutableStateOf(false)
        private set

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(minutes: Int) {
        stop()
        if (minutes <= 0) return
        active = true
        remainingSeconds = minutes * 60
        job = scope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds -= 1
            }
            try { PlayerHolder.togglePlay() } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
            active = false
            remainingSeconds = 0
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        active = false
        remainingSeconds = 0
    }

    fun format(): String {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        return "%d:%02d".format(m, s)
    }
}
