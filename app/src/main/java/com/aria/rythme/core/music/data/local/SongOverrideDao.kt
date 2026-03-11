package com.aria.rythme.core.music.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongOverrideDao {

    /**
     * 获取所有覆盖（响应式），用于与歌曲查询 combine
     */
    @Query("SELECT * FROM song_overrides")
    fun getAllOverrides(): Flow<List<SongOverrideEntity>>

    /**
     * 获取所有覆盖（一次性），用于 suspend 方法中的合并
     */
    @Query("SELECT * FROM song_overrides")
    suspend fun getAllOverridesOnce(): List<SongOverrideEntity>

    /**
     * 获取单首歌曲的覆盖
     */
    @Query("SELECT * FROM song_overrides WHERE songId = :songId")
    suspend fun getOverride(songId: Long): SongOverrideEntity?

    /**
     * 插入或更新覆盖
     */
    @Upsert
    suspend fun upsert(override: SongOverrideEntity)

    /**
     * 删除指定歌曲的覆盖
     */
    @Query("DELETE FROM song_overrides WHERE songId = :songId")
    suspend fun delete(songId: Long)

    /**
     * 清理孤立的覆盖（歌曲已不存在）
     */
    @Query("DELETE FROM song_overrides WHERE songId NOT IN (SELECT id FROM songs)")
    suspend fun cleanupOrphans()
}
