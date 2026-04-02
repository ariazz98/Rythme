package com.aria.rythme.core.music.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 歌词缓存实体
 *
 * 缓存原始歌词文本，避免重复解析和网络请求。
 */
@Entity(
    tableName = "lyrics",
    indices = [Index(value = ["song_id"], unique = true)]
)
data class LyricsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "raw_content")
    val rawContent: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)
