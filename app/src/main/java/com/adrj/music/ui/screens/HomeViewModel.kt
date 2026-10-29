package com.adrj.music.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrj.music.data.MusicRepository
import com.adrj.music.data.Song
import com.adrj.music.downloader.SearchResult
import com.adrj.music.downloader.YtDlpWrapper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.create(app)
    val songs: StateFlow<List<Song>> = repo.all.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites: StateFlow<List<Song>> = repo.favorites.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var title by mutableStateOf("Para ti"); private set
    var recommended = mutableStateListOf<SearchResult>()
    var loading by mutableStateOf(false); private set
    val downloadingIds = mutableStateListOf<String>()

    fun toggleFavorite(s: Song) = viewModelScope.launch { repo.toggleFavorite(s.id, !s.favorite) }
    fun delete(s: Song) = viewModelScope.launch {
        try { java.io.File(s.filePath).delete() } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
        repo.delete(s)
    }
    fun loadRecommendations() {
        if (recommended.isNotEmpty() || loading) return
        viewModelScope.launch {
            loading = true
            try {
                // FIX BUG 5: first() espera el primer valor real de Room
                val favs = favorites.first()
                val allSongs = songs.first()
                val query = if (favs.isNotEmpty()) {
                    val artist = favs.random().artist
                    title = "Porque te gusta $artist"
                    "$artist mix canciones"
                } else { title = "Descubre"; GENERIC.random() }
                val res = YtDlpWrapper.searchMix(query, 12)
                val downloaded = allSongs.map { it.id }.toSet()
                recommended.clear()
                recommended.addAll(res.filter { it.id !in downloaded })
            } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
            loading = false
        }
    }
    fun download(r: SearchResult) {
        if (r.id in downloadingIds) return
        viewModelScope.launch {
            downloadingIds.add(r.id)
            try {
                val d = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { YtDlpWrapper.download(getApplication(), r) { } }
                withContext(Dispatchers.IO) { repo.save(d) }
                recommended.removeAll { it.id == r.id }
            } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
            downloadingIds.remove(r.id)
        }
    }
    companion object {
        private val GENERIC = listOf(
            "top hits 2024", "música trending latino", "reggaeton mix 2024",
            "pop en español hits", "indie alternativo mix", "clásicos rock",
            "trap latino mix", "baladas romanticas"
        )
    }

    fun searchByMood(query: String) {
        viewModelScope.launch {
            loading = true
            try {
                title = "Estado de ánimo"
                val res = YtDlpWrapper.searchMix(query, 20)
                val downloaded = songs.value.map { it.id }.toSet()
                recommended.clear()
                recommended.addAll(res.filter { it.id !in downloaded })
            } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
            loading = false
        }
    }
}
