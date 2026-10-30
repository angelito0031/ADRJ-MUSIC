package com.adrj.music

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object LocalServerHolder {
    @Volatile private var started = false
    var isReady by mutableStateOf(false)
        private set

    fun start(context: Context) {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true

            val t = Thread {
                try {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

                    YtdlpHelper.init(context)
                    LocalServer(context, 8080).start(1000, false)

                    try {
                        YtdlpHelper.search("test", 1)
                    } catch (_: Throwable) {}

                    isReady = true
                    android.util.Log.d("ADRJ", "Server listo")
                } catch (e: Throwable) {
                    android.util.Log.e("ADRJ", "Server error: ${e.message}")
                    isReady = true
                }
            }
            t.priority = Thread.MIN_PRIORITY
            t.name = "adrj-server-bootstrap"
            t.start()
        }
    }
}
