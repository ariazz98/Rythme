package com.aria.rythme.feature.player.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 音乐数据库
 * 
 * Room 数据库，用于本地缓存音乐扫描结果。
 */
@Database(
    entities = [SongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    
    abstract fun songDao(): SongDao
    
    companion object {
        private const val DATABASE_NAME = "rythme_music.db"
        
        @Volatile
        private var INSTANCE: MusicDatabase? = null
        
        fun getInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): MusicDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MusicDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}
