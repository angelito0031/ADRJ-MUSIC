package com.adrj.music.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY downloadedAt DESC")
    fun all(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE favorite = 1 ORDER BY downloadedAt DESC")
    fun favorites(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Query("UPDATE songs SET favorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50")
    fun history(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(item: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Delete
    suspend fun delete(song: Song)
}
