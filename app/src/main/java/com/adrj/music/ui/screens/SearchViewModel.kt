package com.adrj.music.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrj.music.data.MusicRepository
import com.adrj.music.downloader.DownloadedSong
import com.adrj.music.downloader.SearchResult
import com.adrj.music.downloader.YtDlpWrapper
import com.adrj.music.YtdlpHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.create(app)
    var query by mutableStateOf(""); private set
    var results by mutableStateOf<List<SearchResult>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    val downloadingIds = mutableStateListOf<String>()
    val downloadedIds = mutableStateListOf<String>()
    // FIX BUG 1: Map por canción en lugar de progreso global
    val progressMap = mutableStateMapOf<String, Float>()

    fun onQueryChange(v: String) { query = v }

    fun search() {
        val q = query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            loading = true; error = null; results = emptyList()
            try {
                results = YtDlpWrapper.search(q)
                if (results.isEmpty()) error = "Sin resultados"
            } catch (e: Exception) { error = e.message ?: "Error" }
            loading = false
        }
    }

    fun download(r: SearchResult) {
        if (r.id in downloadingIds) return
        viewModelScope.launch {
            downloadingIds.add(r.id)
            error = null
            progressMap[r.id] = 0f
            try {
                withContext(Dispatchers.IO) {
                    val dir = File(getApplication<Application>().getExternalFilesDir(null), "Music").apply { mkdirs() }
                    val dest = File(dir, "${r.id}.m4a")
                    // FIX BUG 2: callback de progreso real
                    YtdlpHelper.downloadToFile(r.id, dest) { p ->
                        progressMap[r.id] = p
                    }
                    val d = DownloadedSong(
                        r.id, r.title, r.artist, r.duration, r.thumbnailUrl, dest.absolutePath
                    )
                    // FIX BUG 3: repo.save en IO (no Main)
                    repo.save(d)
                }
                downloadedIds.add(r.id)
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            }
            downloadingIds.remove(r.id)
            progressMap.remove(r.id)
        }
    }

    fun progressFor(id: String): Float = progressMap[id] ?: 0f
}
