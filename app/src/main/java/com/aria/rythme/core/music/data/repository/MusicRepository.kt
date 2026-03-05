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
    
    /** 上次成功扫描的时间戳，用于防抖 */
    @Volatile
    private var lastScanTimeMs = 0L

    init {
        // 只启动监听，不立即扫描（扫描由 MainActivity 在权限确认后触发）
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
     * 加载歌曲（权限确认后调用）
     *
     * 内置防抖：短时间内重复调用会跳过扫描。
     * 空结果保护：扫描结果为空但有缓存时不清空（可能是权限问题）。
     */
    suspend fun loadSongs(): Result<ScanResult> {
        _isLoading.value = true
        _error.value = null
        val result = performScan(allowEmptySync = false)
        result.onFailure { _error.value = it.message }
        _isLoading.value = false
        return result
    }

    /**
     * 强制刷新歌曲列表
     *
     * 用户手动刷新时调用，重置防抖并允许空结果同步。
     */
    suspend fun refreshSongs(): Result<ScanResult> {
        lastScanTimeMs = 0L
        _isLoading.value = true
        _error.value = null
        val result = performScan(allowEmptySync = true)
        result.onFailure { _error.value = it.message }
        _isLoading.value = false
        return result
    }

    /**
     * 执行扫描并同步到数据库
     *
     * @param allowEmptySync 是否允许空结果清空缓存（强制刷新时为 true）
     */
    private suspend fun performScan(allowEmptySync: Boolean = false): Result<ScanResult> {
        val now = System.currentTimeMillis()
        if (now - lastScanTimeMs < SCAN_DEBOUNCE_MS) {
            RythmeLogger.d(TAG, "距上次扫描不足 ${SCAN_DEBOUNCE_MS / 1000}s，跳过")
            return Result.success(ScanResult(scannedCount = 0))
        }

        return try {
            val songs = mediaStoreSource.scanFromMediaStore()

            // 空结果保护：扫描为空但有缓存时跳过同步（可能无权限或 MediaStore 未就绪）
            if (songs.isEmpty() && !allowEmptySync) {
                val cachedCount = songDao.getSongCount()
                if (cachedCount > 0) {
                    RythmeLogger.d(TAG, "扫描结果为空但有 ${cachedCount} 条缓存，跳过同步")
                    lastScanTimeMs = now
                    return Result.success(ScanResult(scannedCount = 0))
                }
            }

            syncSongsToDb(songs)
            lastScanTimeMs = System.currentTimeMillis()
            RythmeLogger.d(TAG, "扫描完成: ${songs.size} 首歌曲")
            Result.success(ScanResult(scannedCount = songs.size))
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "扫描失败", e)
            Result.failure(e)
        }
    }
    
    // ==================== 内部辅助方法 ====================
    
    /**
     * 同步歌曲到数据库（添加新的，删除不存在的）
     */
    private suspend fun syncSongsToDb(currentSongs: List<Song>) {
        val currentIds = currentSongs.map { it.id }.toSet()
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
     * 检测到音频文件变化时自动触发后台扫描（复用 performScan 的防抖和空结果保护）
     */
    private fun observeMediaStoreChanges() {
        scope.launch {
            mediaStoreObserver.observeChanges()
                .collect { change ->
                    if (change.type == ChangeType.AUDIO_CHANGED) {
                        RythmeLogger.d(TAG, "检测到音频文件变化，触发后台扫描")
                        performScan()
                    }
                }
        }
    }

    companion object {
        private const val TAG = "MusicRepository"
        private const val SCAN_DEBOUNCE_MS = 5_000L
    }
}
