package com.adrj.music.data

import android.content.Context
import com.adrj.music.downloader.DownloadedSong
import kotlinx.coroutines.flow.Flow

class MusicRepository(private val dao: SongDao, private val historyDao: HistoryDao) {
    val all: Flow<List<Song>> = dao.all()
    val favorites: Flow<List<Song>> = dao.favorites()
    val history: Flow<List<HistoryEntity>> = historyDao.all()

    suspend fun save(d: DownloadedSong) {
        dao.insert(Song(
            id = d.id, title = d.title, artist = d.artist,
            duration = d.duration, thumbnailUrl = d.thumbnailUrl,
            filePath = d.filePath
        ))
    }

    suspend fun toggleFavorite(id: String, fav: Boolean) = dao.setFavorite(id, fav)
    suspend fun findSong(id: String): Song? = dao.byId(id)
    suspend fun delete(song: Song) = dao.delete(song)

    suspend fun recordPlay(id: String, title: String, artist: String, art: String) {
        try {
            historyDao.insert(HistoryEntity(id, title, artist, art))
        } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
    }

    suspend fun clearHistory() = historyDao.clear()

    companion object {
        fun create(context: Context): MusicRepository {
            val db = AdrjDatabase.get(context)
            return MusicRepository(db.songDao(), db.historyDao())
        }
    }
}
