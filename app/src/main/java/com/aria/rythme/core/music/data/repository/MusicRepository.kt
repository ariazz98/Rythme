package com.aria.rythme.core.music.data.repository

import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.core.music.data.datasource.MediaStoreSource
import com.aria.rythme.core.music.data.datasource.ScanResult
import com.aria.rythme.core.music.data.local.SongDao
import com.aria.rythme.core.music.data.local.SongEntity
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.observer.ChangeType
import com.aria.rythme.core.music.data.observer.MediaStoreObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 音乐仓库（合并数据访问和业务逻辑）
 * 
 * 在数据层完成所有数据操作的闭环，包括：
 * - Room 数据库的 CRUD 操作
 * - MediaStore 扫描和同步
 * - MediaStore 变化监听和自动刷新
 * - 业务状态管理（加载、错误）
 * 
 * ## 数据流
 * MediaStore → 扫描 → Room → Repository Flow → ViewModel → UI
 * 
 * @param songDao Room 数据访问对象
 * @param mediaStoreSource MediaStore 扫描器
 * @param mediaStoreObserver MediaStore 观察者
 */
class MusicRepository(
    private val songDao: SongDao,
    private val mediaStoreSource: MediaStoreSource,
    private val mediaStoreObserver: MediaStoreObserver
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /** 加载状态 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /** 错误信息 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // 自动启动 MediaStore 监听
        observeMediaStoreChanges()
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有歌曲（响应式）
     * 
     * 从 Room 读取，当数据变化时自动更新
     */
    fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }
    
    /**
     * 获取所有歌曲（一次性）
     */
    suspend fun getAllSongsOnce(): List<Song> {
        return songDao.getAllSongsOnce().map { it.toSong() }
    }
    
    /**
     * 根据 ID 获取歌曲
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
    
    // ==================== 业务操作 ====================

    
    /**
     * 初始化加载歌曲
     * 
     * 首次启动时调用：
     * 1. 触发 MediaStore 扫描
     * 2. 同步到 Room 数据库
     * 3. Room 数据变化会通过 Flow 自动通知
     * 
     * @return 扫描结果（成功/失败）
     */
    suspend fun loadSongs(): Result<ScanResult> {
        RythmeLogger.d(TAG, "初始化加载歌曲列表")
        _isLoading.value = true
        _error.value = null
        
        return try {
            val songs = mediaStoreSource.scanFromMediaStore()
            syncSongsToDb(songs)
            RythmeLogger.d(TAG, "加载完成: ${songs.size} 首歌曲")
            _isLoading.value = false
            Result.success(ScanResult(scannedCount = songs.size))
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "加载失败", e)
            _error.value = e.message
            _isLoading.value = false
            Result.failure(e)
        }
    }
    
    /**
     * 强制刷新歌曲列表
     * 
     * 用户手动刷新时调用，重新扫描 MediaStore
     * 
     * @return 扫描结果（成功/失败）
     */
    suspend fun refreshSongs(): Result<ScanResult> {
        RythmeLogger.d(TAG, "强制刷新歌曲列表")
        _isLoading.value = true
        _error.value = null
        
        return try {
            val songs = mediaStoreSource.scanFromMediaStore()
            syncSongsToDb(songs)
            RythmeLogger.d(TAG, "刷新完成: ${songs.size} 首歌曲")
            _isLoading.value = false
            Result.success(ScanResult(scannedCount = songs.size))
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "刷新失败", e)
            _error.value = e.message
            _isLoading.value = false
            Result.failure(e)
        }
    }
    
    // ==================== 内部辅助方法 ====================
    
    /**
     * 同步歌曲到数据库（添加新的，删除不存在的）
     */
    private suspend fun syncSongsToDb(currentSongs: List<Song>) {
        val currentIds = currentSongs.map { it.id }
        val cachedIds = songDao.getAllSongIds()
        
        // 删除已不存在的歌曲
        val toDelete = cachedIds.filter { it !in currentIds }
        if (toDelete.isNotEmpty()) {
            songDao.deleteByIds(toDelete)
            RythmeLogger.d(TAG, "删除 ${toDelete.size} 首已移除的歌曲")
        }
        
        // 更新/插入当前歌曲
        val entities = currentSongs.map { SongEntity.fromSong(it) }
        songDao.upsertAll(entities)
        RythmeLogger.d(TAG, "同步 ${currentSongs.size} 首歌曲到数据库")
    }
    
    /**
     * 监听 MediaStore 变化
     * 
     * 检测到音频文件变化时自动触发后台扫描
     */
    private fun observeMediaStoreChanges() {
        scope.launch {
            mediaStoreObserver.observeChanges()
                .collect { change ->
                    if (change.type == ChangeType.AUDIO_CHANGED) {
                        RythmeLogger.d(TAG, "检测到音频文件变化，触发后台扫描")
                        
                        // 后台静默扫描
                        try {
                            val songs = mediaStoreSource.scanFromMediaStore()
                            syncSongsToDb(songs)
                            RythmeLogger.d(TAG, "后台扫描完成: ${songs.size} 首歌曲")
                        } catch (e: Exception) {
                            RythmeLogger.e(TAG, "后台扫描失败", e)
                        }
                    }
                }
        }
    }
    
    companion object {
        private const val TAG = "MusicRepository"
    }
}
