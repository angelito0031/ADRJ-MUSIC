package com.adrj.music.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Song::class, HistoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AdrjDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: AdrjDatabase? = null
        fun get(context: Context): AdrjDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, AdrjDatabase::class.java, "adrj.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
