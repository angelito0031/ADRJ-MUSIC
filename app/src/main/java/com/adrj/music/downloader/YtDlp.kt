package com.adrj.music.downloader

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class SearchResult(
    val id: String, val title: String, val artist: String,
    val duration: Long, val thumbnailUrl: String, val url: String
)

@Immutable
data class DownloadedSong(
    val id: String, val title: String, val artist: String,
    val duration: Long, val thumbnailUrl: String, val filePath: String
)

object YtDlpWrapper {
    const val USER_AGENT = "Mozilla/5.0"

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val arr = com.adrj.music.YtdlpHelper.search(query)
            val list = mutableListOf<SearchResult>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                if (id.isBlank()) continue
                list.add(SearchResult(
                    id = id,
                    title = o.optString("title", "Sin título"),
                    artist = o.optString("artist", "Desconocido"),
                    duration = 0L,
                    thumbnailUrl = o.optString("art"),
                    url = "https://www.youtube.com/watch?v=$id"
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    suspend fun searchMix(q: String, limit: Int = 12) = search(q)

    suspend fun getStreamUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        val id = Regex("[?&]v=([A-Za-z0-9_-]{11})").find(youtubeUrl)?.groupValues?.get(1) ?: youtubeUrl
        com.adrj.music.YtdlpHelper.resolveStreamUrl(id)
    }

    suspend fun download(
        context: Context,
        result: SearchResult,
        onProgress: (Float) -> Unit
    ): DownloadedSong = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "Music").apply { mkdirs() }
        val out = File(dir, "${result.id}.m4a")
        com.adrj.music.YtdlpHelper.downloadToFile(result.id, out)
        onProgress(1f)
        DownloadedSong(result.id, result.title, result.artist, result.duration, result.thumbnailUrl, out.absolutePath)
    }
}
