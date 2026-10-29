package com.adrj.music

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class LocalServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD("127.0.0.1", port) {

    private val audioCache = File(context.cacheDir, "adrj_audio").apply { mkdirs() }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        return try {
            when {
                uri.startsWith("/api/search")   -> apiSearch(session)
                uri.startsWith("/api/stream")   -> apiStream(session)
                uri.startsWith("/api/download") -> apiDownload(session)
                uri.startsWith("/api/debug")    -> apiDebug()
                uri.startsWith("/api/status")   -> apiStatus()
                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND, "text/plain", "not found"
                )
            }
        } catch (e: Throwable) {
            android.util.Log.e("ADRJ", "serve error $uri", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "text/plain", "Error: ${e.message}"
            )
        }
    }

    private fun apiStatus(): Response {
        val err = YtdlpHelper.lastError
        val json = """{"ok":${err == null},"error":"${(err ?: "").replace("\"", "'")}"}"""
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun apiDebug(): Response {
        val sb = StringBuilder()
        try {
            sb.append("lastError: ${YtdlpHelper.lastError}\n")
            sb.append("initError: ${YtdlpHelper.initError}\n")
            sb.append("updateStatus: ${YtdlpHelper.updateStatus}\n")
            try {
                val arr = YtdlpHelper.search("test", 3)
                sb.append("search test: ${arr.length()} resultados\n")
                if (arr.length() > 0) {
                    sb.append("ejemplo: ${arr.getJSONObject(0).optString("title")}\n")
                }
            } catch (e: Throwable) {
                sb.append("search err: ${e.message}\n")
            }
        } catch (e: Throwable) {
            sb.append("debug err: ${e.message}")
        }
        return newFixedLengthResponse(Response.Status.OK, "text/plain", sb.toString())
    }

    private fun apiSearch(session: IHTTPSession): Response {
        val q = session.parameters["q"]?.firstOrNull() ?: ""
        if (q.isBlank()) return newFixedLengthResponse(
            Response.Status.OK, "application/json", "[]"
        )
        val result = try {
            YtdlpHelper.search(q, 20).toString()
        } catch (e: Throwable) {
            android.util.Log.e("ADRJ", "search err", e)
            YtdlpHelper.lastError = e.message
            """{"error":"${e.message?.replace("\"", "'") ?: "unknown"}"}"""
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", result)
    }

    private fun apiStream(session: IHTTPSession): Response {
        val id = session.parameters["id"]?.firstOrNull() ?: return newFixedLengthResponse(
            Response.Status.BAD_REQUEST, "text/plain", "falta id"
        )
        try {
            val streamUrl = YtdlpHelper.resolveStreamUrl(id) ?: throw Exception("No stream url")
            // Redirección directa 302. ExoPlayer se encarga del seek y el buffering.
            return newFixedLengthResponse(Response.Status.REDIRECT, "text/plain", "").apply {
                addHeader("Location", streamUrl)
                addHeader("Access-Control-Allow-Origin", "*")
            }
        } catch (e: Throwable) {
            android.util.Log.e("ADRJ", "stream err", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: ${e.message}")
        }
    }

    private fun apiDownload(session: IHTTPSession): Response {
        val id = session.parameters["id"]?.firstOrNull()
        val q = session.parameters["q"]?.firstOrNull()
        val videoId = id ?: run {
            if (q.isNullOrBlank()) return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "text/plain", "falta id o q"
            )
            try {
                val arr = YtdlpHelper.search(q, 1)
                if (arr.length() == 0) return newFixedLengthResponse(
                    Response.Status.NOT_FOUND, "text/plain", "sin resultados"
                )
                arr.getJSONObject(0).getString("id")
            } catch (e: Throwable) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.message)
            }
        }
        
        try {
            val cached = findOrCreateCache(audioCache, videoId)
            if (!cached.exists() || cached.length() == 0L) {
                // Descarga síncrona al servidor, pero rápida
                YtdlpHelper.downloadToFile(videoId, File(audioCache, "$videoId.m4a"))
            }
            val finalFile = findOrCreateCache(audioCache, videoId)
            return serveAudio(session, finalFile, attachment = true)
        } catch (e: Throwable) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error dl: ${e.message}")
        }
    }

    private fun findOrCreateCache(dir: File, id: String): File {
        val m4a = File(dir, "$id.m4a")
        if (m4a.exists() && m4a.length() > 0) return m4a
        val webm = File(dir, "$id.webm")
        if (webm.exists() && webm.length() > 0) return webm
        return m4a
    }

    private fun serveAudio(
        session: IHTTPSession,
        file: File,
        attachment: Boolean = false
    ): Response {
        val total = file.length()
        val range = session.headers["range"]
        val mime = mimeFor(file)

        val response = if (range != null) {
            val parts = range.removePrefix("bytes=").split("-")
            val start = parts[0].toLongOrNull() ?: 0
            val end = parts.getOrNull(1)?.toLongOrNull() ?: (total - 1)
            val length = end - start + 1
            val fis = FileInputStream(file)
            fis.skip(start)
            val r = newFixedLengthResponse(
                Response.Status.PARTIAL_CONTENT, mime, fis, length
            )
            r.addHeader("Content-Range", "bytes $start-$end/$total")
            r.addHeader("Accept-Ranges", "bytes")
            r
        } else {
            val r = newFixedLengthResponse(
                Response.Status.OK, mime, FileInputStream(file), total
            )
            r.addHeader("Accept-Ranges", "bytes")
            r
        }
        if (attachment) {
            response.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        }
        return response
    }

    private fun mimeFor(f: File): String = when {
        f.name.endsWith(".webm") -> "audio/webm"
        f.name.endsWith(".m4a") -> "audio/mp4"
        else -> "audio/mpeg"
    }
}
