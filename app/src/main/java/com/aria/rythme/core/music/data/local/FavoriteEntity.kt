package com.aria.rythme.core.music.data.local

import androidx.room.Entity

/** 同一张表保存不同媒体类型的收藏，避免为歌曲、艺人、专辑复制整套存储。 */
@Entity(
    tableName = "favorites",
    primaryKeys = ["kind", "itemId"]
)
data class FavoriteEntity(
    val kind: String,
    val itemId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
