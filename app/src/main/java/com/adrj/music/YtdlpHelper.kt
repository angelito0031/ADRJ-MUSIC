package com.adrj.music

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object YtdlpHelper {
    private val downloadLock = java.util.concurrent.locks.ReentrantLock()
    @Volatile private var initialized = false
    @Volatile private var updating = false
    @Volatile var lastError: String? = null
    @Volatile var updateStatus: String = "Pendiente"
    @Volatile var initError: String = ""

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                YoutubeDL.getInstance().init(context)
                initialized = true
                initError = ""
            } catch (e: Throwable) {
                initError = "${e.javaClass.simpleName}: ${e.message}"
                lastError = initError
            }
        }
    }

    fun ensureInit(context: Context): Boolean {
        if (!initialized) init(context)
        return initialized
    }

    fun updateInBackground(context: Context) {
        if (updating || !initialized) return
        updating = true
        updateStatus = "Actualizando..."
        Thread {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
                updateStatus = "yt-dlp actualizado ✓"
                lastError = null
            } catch (e: Throwable) {
                updateStatus = "Error: ${e.message}"
            }
            updating = false
        }.start()
    }

    fun search(query: String, limit: Int = 20): JSONArray {
        if (!initialized) throw Exception("yt-dlp no inicializado ($initError)")
        val req = YoutubeDLRequest("ytsearch${limit}:$query").apply {
            addOption("--dump-single-json")
            addOption("--flat-playlist")
            addOption("--no-warnings")
            addOption("--no-check-certificates")
            addOption("--skip-download")
        }
        val resp = YoutubeDL.getInstance().execute(req) { _, _, _ -> }
        val json = JSONObject(resp.out)
        val entries = json.optJSONArray("entries") ?: JSONArray()
        val result = JSONArray()
        for (i in 0 until entries.length()) {
            val e = entries.getJSONObject(i)
            val id = e.optString("id")
            if (id.isBlank()) continue
            result.put(JSONObject().apply {
                put("id", id)
                put("title", e.optString("title", "Sin título"))
                put("artist", e.optString("uploader").ifBlank { e.optString("channel").ifBlank { "Desconocido" } })
                put("art", "https://i.ytimg.com/vi/$id/maxresdefault.jpg")
                put("src", ""); put("album", "")
            })
        }
        return result
    }

    private val urlCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val urlCacheTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val URL_CACHE_TTL = 3 * 60 * 60 * 1000L // 3 horas

    fun resolveStreamUrl(videoId: String): String? {
        if (!initialized) return null

        // BUG 9: Caché de URLs
        val cached = urlCache[videoId]
        val cachedTime = urlCacheTime[videoId] ?: 0L
        if (cached != null && System.currentTimeMillis() - cachedTime < URL_CACHE_TTL) {
            return cached
        }

        val req = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId").apply {
            addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
            addOption("-g")
            addOption("--no-warnings")
            addOption("--no-check-certificates")
        }
        return try {
            val out = YoutubeDL.getInstance().execute(req) { _, _, _ -> }.out
            val url = out.lineSequence().firstOrNull { it.trim().startsWith("http") }?.trim()
            // BUG 8: limpiar lastError en éxito
            if (!url.isNullOrBlank()) {
                urlCache[videoId] = url
                urlCacheTime[videoId] = System.currentTimeMillis()
                lastError = null
            }
            url
        } catch (e: Exception) {
            lastError = "resolveStreamUrl: ${e.message}"
            null
        }
    }

    fun downloadToFile(videoId: String, destFile: File, onProgress: ((Float) -> Unit)? = null): Boolean {
        // FIX BUG 2: serializar descargas (YoutubeDL no soporta paralelas)
        downloadLock.lock()
        try {
            return downloadToFileInternal(videoId, destFile, onProgress)
        } finally {
            downloadLock.unlock()
        }
    }

    private fun downloadToFileInternal(videoId: String, destFile: File, onProgress: ((Float) -> Unit)?): Boolean {
        if (!initialized) throw Exception("yt-dlp no inicializado")
        if (destFile.exists() && destFile.length() > 10000) {
            onProgress?.invoke(1f)
            return true
        }
        destFile.parentFile?.mkdirs()
        val req = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId").apply {
            addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
            addOption("-o", destFile.absolutePath)
            addOption("--no-warnings")
            addOption("--no-check-certificates")
        }
        YoutubeDL.getInstance().execute(req) { progress, _, _ ->
            // progress viene 0-100 de YoutubeDL, normalizar a 0-1
            onProgress?.invoke((progress / 100f).coerceIn(0f, 1f))
        }
        onProgress?.invoke(1f)
        return destFile.exists() && destFile.length() > 0
    }
}
