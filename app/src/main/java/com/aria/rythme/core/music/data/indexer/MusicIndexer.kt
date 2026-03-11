package com.aria.rythme.core.music.data.indexer

import android.content.Context
import com.aria.rythme.core.music.data.datasource.MediaStoreFingerprint
import com.aria.rythme.core.music.data.datasource.MediaStoreSource
import com.aria.rythme.core.music.data.local.ScanMetadataDao
import com.aria.rythme.core.music.data.local.ScanMetadataEntity
import com.aria.rythme.core.music.data.local.SongDao
import com.aria.rythme.core.music.data.local.SongEntity
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.music.data.model.ScanProgress
import com.aria.rythme.core.music.data.model.ScanStats
import com.aria.rythme.core.music.data.model.SyncPhase
import com.aria.rythme.core.music.data.observer.MediaStoreWatcher
import com.aria.rythme.core.music.data.observer.WatchEvent
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 音乐索引器
 *
 * 协调整个扫描生命周期，三阶段管线：
 * 1. Discover — 轻量指纹查询
 * 2. Diff & Sync — 差异对比 + 精确同步
 * 3. Post-process — 重建聚合表
 *
 * 初始化策略：
 * - 有缓存 → 立即返回（UI 用缓存上屏）+ 后台增量同步
 * - 无缓存 → 全量扫描（带进度反馈）
 */
class MusicIndexer(
    private val context: Context,
    private val songDao: SongDao,
    private val scanMetadataDao: ScanMetadataDao,
    private val mediaStoreSource: MediaStoreSource,
    private val mediaStoreWatcher: MediaStoreWatcher,
    private val musicRepository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    /**
     * 初始化索引器
     *
     * 有缓存时立即返回 + 后台增量；无缓存时前台全量扫描。
     */
    suspend fun initialize() {
        val cachedCount = songDao.getSongCount()
        if (cachedCount > 0) {
            RythmeLogger.d(TAG, "有 $cachedCount 条缓存，后台增量同步")
            scope.launch { syncIfNeeded() }
        } else {
            RythmeLogger.d(TAG, "无缓存，执行全量扫描")
            fullScan()
        }
        startWatching()
    }

    /**
     * 手动全量重建
     */
    suspend fun forceFullRescan() {
        fullScan()
    }

    // ==================== 核心扫描逻辑 ====================

    /**
     * 检查是否需要同步并执行
     */
    private suspend fun syncIfNeeded() {
        when (mediaStoreWatcher.detectChangeType()) {
            WatchEvent.FullResyncNeeded -> fullScan()
            WatchEvent.IncrementalSyncNeeded -> incrementalSync()
            WatchEvent.NoChange -> {
                RythmeLogger.d(TAG, "无变化，跳过同步")
                _scanProgress.value = ScanProgress.Idle
            }
        }
    }

    /**
     * 全量扫描
     */
    private suspend fun fullScan() {
        val startTime = System.currentTimeMillis()
        try {
            _scanProgress.value = ScanProgress.Discovering(0)

            val songs = mediaStoreSource.scanAllSongs()
            _scanProgress.value = ScanProgress.Discovering(songs.size)

            if (songs.isEmpty()) {
                // 空结果保护
                val cachedCount = songDao.getSongCount()
                if (cachedCount > 0) {
                    RythmeLogger.d(TAG, "全量扫描结果为空但有缓存，跳过")
                    _scanProgress.value = ScanProgress.Idle
                    return
                }
            }

            // 同步到数据库
            _scanProgress.value = ScanProgress.Syncing(0, songs.size, SyncPhase.ADDING)

            val entities = songs.map { SongEntity.fromSong(it) }
            // 清空旧数据 + 批量插入（全量模式）
            songDao.deleteAll()
            entities.chunked(500).forEachIndexed { index, chunk ->
                songDao.upsertAll(chunk)
                _scanProgress.value = ScanProgress.Syncing(
                    current = ((index + 1) * 500).coerceAtMost(songs.size),
                    total = songs.size,
                    phase = SyncPhase.ADDING
                )
            }

            // 后处理：重建聚合表
            _scanProgress.value = ScanProgress.PostProcessing("重建专辑和艺术家索引")
            rebuildAggregations()

            // 保存扫描元数据
            saveScanMetadata(songs.size)

            val durationMs = System.currentTimeMillis() - startTime
            val stats = ScanStats(
                added = songs.size, deleted = 0, modified = 0, unchanged = 0,
                totalSongs = songs.size, durationMs = durationMs
            )
            _scanProgress.value = ScanProgress.Completed(stats)
            RythmeLogger.d(TAG, "全量扫描完成: ${songs.size} 首, 耗时 ${durationMs}ms")
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "全量扫描失败", e)
            _scanProgress.value = ScanProgress.Failed(e.message ?: "未知错误")
        }
    }

    /**
     * 增量同步
     */
    private suspend fun incrementalSync() {
        val startTime = System.currentTimeMillis()
        try {
            _scanProgress.value = ScanProgress.Discovering(0)

            // Step 1: 获取 MediaStore 指纹
            val msFingerprints = mediaStoreSource.queryFingerprints()
            _scanProgress.value = ScanProgress.Discovering(msFingerprints.size)

            // Step 2: 获取本地缓存指纹
            val localFingerprints = songDao.getAllFingerprints()

            // Step 3: 计算差异
            val diff = computeDiff(msFingerprints, localFingerprints)
            RythmeLogger.d(TAG, "增量对比: +${diff.added.size} ~${diff.modified.size} -${diff.deleted.size} =${diff.unchanged}")

            // 如果无变化，直接返回
            if (diff.added.isEmpty() && diff.modified.isEmpty() && diff.deleted.isEmpty()) {
                saveScanMetadata(localFingerprints.size)
                _scanProgress.value = ScanProgress.Idle
                return
            }

            // Step 4: 删除
            if (diff.deleted.isNotEmpty()) {
                _scanProgress.value = ScanProgress.Syncing(0, diff.deleted.size, SyncPhase.DELETING)
                diff.deleted.chunked(500).forEach { chunk ->
                    songDao.deleteByIds(chunk)
                }
            }

            // Step 5: 新增 — 仅对新增 ID 做完整查询
            if (diff.added.isNotEmpty()) {
                _scanProgress.value = ScanProgress.Syncing(0, diff.added.size, SyncPhase.ADDING)
                val newSongs = mediaStoreSource.querySongsByIds(diff.added)
                val newEntities = newSongs.map { SongEntity.fromSong(it) }
                newEntities.chunked(500).forEach { chunk ->
                    songDao.upsertAll(chunk)
                }
            }

            // Step 6: 更新 — 仅对修改 ID 做完整查询
            if (diff.modified.isNotEmpty()) {
                _scanProgress.value = ScanProgress.Syncing(0, diff.modified.size, SyncPhase.UPDATING)
                val updatedSongs = mediaStoreSource.querySongsByIds(diff.modified)
                val updatedEntities = updatedSongs.map { SongEntity.fromSong(it) }
                updatedEntities.chunked(500).forEach { chunk ->
                    songDao.upsertAll(chunk)
                }
            }

            // Step 7: 后处理
            _scanProgress.value = ScanProgress.PostProcessing("重建专辑和艺术家索引")
            rebuildAggregations()

            // 保存扫描元数据
            val totalSongs = localFingerprints.size + diff.added.size - diff.deleted.size
            saveScanMetadata(totalSongs)

            val durationMs = System.currentTimeMillis() - startTime
            val stats = ScanStats(
                added = diff.added.size,
                deleted = diff.deleted.size,
                modified = diff.modified.size,
                unchanged = diff.unchanged,
                totalSongs = totalSongs,
                durationMs = durationMs
            )
            _scanProgress.value = ScanProgress.Completed(stats)
            RythmeLogger.d(TAG, "增量同步完成: +${diff.added.size} ~${diff.modified.size} -${diff.deleted.size}, 耗时 ${durationMs}ms")
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "增量同步失败", e)
            _scanProgress.value = ScanProgress.Failed(e.message ?: "未知错误")
        }
    }

    // ==================== 差异计算 ====================

    private data class DiffResult(
        val added: List<Long>,
        val deleted: List<Long>,
        val modified: List<Long>,
        val unchanged: Int
    )

    private fun computeDiff(
        msFingerprints: List<MediaStoreFingerprint>,
        localFingerprints: List<com.aria.rythme.core.music.data.local.SongFingerprint>
    ): DiffResult {
        val msMap = msFingerprints.associateBy { it.id }
        val localMap = localFingerprints.associateBy { it.id }

        val added = mutableListOf<Long>()
        val modified = mutableListOf<Long>()
        var unchanged = 0

        // MediaStore 中有，本地没有 → 新增
        // MediaStore 中有，本地也有但指纹不同 → 修改
        for ((id, msFp) in msMap) {
            val localFp = localMap[id]
            if (localFp == null) {
                added.add(id)
            } else if (msFp.generationModified != localFp.generationModified ||
                msFp.dateModified != localFp.dateModified ||
                msFp.size != localFp.size
            ) {
                modified.add(id)
            } else {
                unchanged++
            }
        }

        // 本地有，MediaStore 没有 → 删除
        val deleted = localMap.keys.filter { it !in msMap }.toList()

        return DiffResult(added, deleted, modified, unchanged)
    }

    // ==================== 聚合表重建 ====================

    /**
     * 委托给 MusicRepository 重建聚合表，
     * 确保用户覆盖层（song_overrides）被正确合并。
     */
    private suspend fun rebuildAggregations() {
        musicRepository.rebuildAggregations()
        RythmeLogger.d(TAG, "聚合表重建完成")
    }

    // ==================== 元数据持久化 ====================

    private suspend fun saveScanMetadata(totalSongs: Int) {
        val metadata = ScanMetadataEntity(
            id = 0,
            lastMediaStoreVersion = mediaStoreSource.getMediaStoreVersion(),
            lastMediaStoreGeneration = mediaStoreSource.getMediaStoreGeneration(),
            lastFullScanTimestamp = System.currentTimeMillis(),
            lastIncrementalScanTimestamp = System.currentTimeMillis(),
            totalSongsScanned = totalSongs
        )
        scanMetadataDao.upsert(metadata)
    }

    // ==================== MediaStore 变化监听 ====================

    private fun startWatching() {
        scope.launch {
            mediaStoreWatcher.observeChanges().collect { event ->
                when (event) {
                    WatchEvent.FullResyncNeeded -> {
                        RythmeLogger.d(TAG, "收到全量重建事件")
                        fullScan()
                    }
                    WatchEvent.IncrementalSyncNeeded -> {
                        RythmeLogger.d(TAG, "收到增量同步事件")
                        incrementalSync()
                    }
                    WatchEvent.NoChange -> { /* 不应该走到这里 */ }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MusicIndexer"
    }
}
