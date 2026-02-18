package com.aria.rythme.feature.player.data.repository

import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.feature.player.data.local.SongDao
import com.aria.rythme.feature.player.data.local.SongEntity
import com.aria.rythme.feature.player.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 歌曲缓存仓库
 * 
 * 管理本地歌曲缓存，提供 CRUD 操作。
 */
class SongCacheRepository(
    private val songDao: SongDao
) {
    companion object {
        private const val TAG = "SongCache"
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有缓存的歌曲（响应式）
     */
    fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }
    
    /**
     * 获取所有缓存的歌曲（一次性）
     */
    suspend fun getAllSongsOnce(): List<Song> {
        return songDao.getAllSongsOnce().map { it.toSong() }
    }
    
    /**
     * 根据ID获取歌曲
     */
    suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.toSong()
    }
    
    /**
     * 获取缓存歌曲数量
     */
    suspend fun getCachedCount(): Int {
        return songDao.getSongCount()
    }
    
    /**
     * 检查是否有缓存
     */
    suspend fun hasCache(): Boolean {
        return songDao.getSongCount() > 0
    }
    
    /**
     * 搜索歌曲
     */
    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { entities ->
            entities.map { it.toSong() }
        }
    }
    
    /**
     * 获取最近添加的歌曲
     */
    fun getRecentlyAdded(limit: Int = 50): Flow<List<Song>> {
        return songDao.getRecentlyAdded(limit).map { entities ->
            entities.map { it.toSong() }
        }
    }
    
    // ==================== 写入操作 ====================
    
    /**
     * 保存歌曲列表到缓存
     */
    suspend fun saveSongs(songs: List<Song>) {
        val entities = songs.map { SongEntity.fromSong(it) }
        songDao.upsertAll(entities)
        RythmeLogger.d(TAG, "Cached ${songs.size} songs")
    }
    
    /**
     * 同步歌曲列表（添加新的，删除不存在的）
     */
    suspend fun syncSongs(currentSongs: List<Song>) {
        val currentIds = currentSongs.map { it.id }
        val cachedIds = songDao.getAllSongIds()
        
        // 找出需要删除的歌曲（在缓存中但不在当前扫描结果中）
        val toDelete = cachedIds.filter { it !in currentIds }
        if (toDelete.isNotEmpty()) {
            songDao.deleteByIds(toDelete)
            RythmeLogger.d(TAG, "Removed ${toDelete.size} deleted songs from cache")
        }
        
        // 更新/插入当前歌曲
        saveSongs(currentSongs)
    }
    
    /**
     * 增量更新歌曲
     */
    suspend fun updateSongs(songs: List<Song>) {
        val entities = songs.map { SongEntity.fromSong(it) }
        songDao.upsertAll(entities)
        RythmeLogger.d(TAG, "Updated ${songs.size} songs")
    }
    
    /**
     * 删除指定歌曲
     */
    suspend fun deleteSongs(ids: List<Long>) {
        songDao.deleteByIds(ids)
        RythmeLogger.d(TAG, "Deleted ${ids.size} songs from cache")
    }
    
    /**
     * 清空缓存
     */
    suspend fun clearCache() {
        songDao.deleteAll()
        RythmeLogger.d(TAG, "Cache cleared")
    }
}
