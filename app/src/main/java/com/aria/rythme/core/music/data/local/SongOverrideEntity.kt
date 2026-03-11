package com.aria.rythme.core.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户手动编辑的歌曲覆盖层
 *
 * 只存储用户修改过的字段（非 null 表示已覆盖）。
 * 查询时与 SongEntity 合并，用户编辑优先于 MediaStore 原始值。
 * MediaStore 同步不会影响此表，确保用户修改不被覆盖。
 */
@Entity(tableName = "song_overrides")
data class SongOverrideEntity(
    @PrimaryKey val songId: Long,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val composer: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null
)
