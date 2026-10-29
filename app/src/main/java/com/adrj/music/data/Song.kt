package com.adrj.music.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val thumbnailUrl: String,
    val filePath: String,
    val favorite: Boolean = false,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val playedAt: Long = System.currentTimeMillis()
)
