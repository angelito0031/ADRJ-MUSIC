package com.adrj.music.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrj.music.data.MusicRepository
import com.adrj.music.data.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.create(app)
    val songs: StateFlow<List<Song>> = repo.all.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites: StateFlow<List<Song>> = repo.favorites.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun toggleFavorite(s: Song) = viewModelScope.launch { repo.toggleFavorite(s.id, !s.favorite) }
    fun delete(s: Song) = viewModelScope.launch {
        try { java.io.File(s.filePath).delete() } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
        repo.delete(s)
    }
}
