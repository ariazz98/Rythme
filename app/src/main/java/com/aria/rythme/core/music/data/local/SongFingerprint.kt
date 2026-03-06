package com.aria.rythme.core.music.data.local

/**
 * 歌曲指纹 — Room 查询投影
 *
 * 仅包含增量同步对比所需的最少字段。
 */
data class SongFingerprint(
    val id: Long,
    val dateModified: Long,
    val size: Long,
    val generationModified: Long
)
