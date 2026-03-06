package com.aria.rythme.core.music.data.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.aria.rythme.core.music.data.datasource.MediaStoreSource
import com.aria.rythme.core.music.data.local.ScanMetadataDao
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * MediaStore 变化监听器（generation 感知 + 递进防抖）
 *
 * 替代旧的 MediaStoreObserver，核心改进：
 * 1. 利用 MediaStore.getVersion / getGeneration 做精确变化检测
 * 2. 递进防抖：首次 500ms -> 持续变化逐步延长到 5s
 * 3. 区分全量重建和增量同步事件
 */
class MediaStoreWatcher(
    private val context: Context,
    private val mediaStoreSource: MediaStoreSource,
    private val scanMetadataDao: ScanMetadataDao
) {
    /**
     * 快速检查是否有变化（不触发扫描）
     */
    suspend fun hasChanges(): Boolean {
        val metadata = scanMetadataDao.get() ?: return true

        val currentVersion = mediaStoreSource.getMediaStoreVersion()
        if (currentVersion != metadata.lastMediaStoreVersion) return true

        val currentGen = mediaStoreSource.getMediaStoreGeneration()
        return currentGen != metadata.lastMediaStoreGeneration
    }

    /**
     * 检测变化类型
     */
    suspend fun detectChangeType(): WatchEvent {
        val metadata = scanMetadataDao.get()
            ?: return WatchEvent.FullResyncNeeded

        val currentVersion = mediaStoreSource.getMediaStoreVersion()
        if (currentVersion != metadata.lastMediaStoreVersion) {
            RythmeLogger.d(TAG, "MediaStore version 变化: ${metadata.lastMediaStoreVersion} -> $currentVersion")
            return WatchEvent.FullResyncNeeded
        }

        val currentGen = mediaStoreSource.getMediaStoreGeneration()
        if (currentGen != metadata.lastMediaStoreGeneration) {
            RythmeLogger.d(TAG, "MediaStore generation 变化: ${metadata.lastMediaStoreGeneration} -> $currentGen")
            return WatchEvent.IncrementalSyncNeeded
        }

        return WatchEvent.NoChange
    }

    /**
     * 监听 MediaStore 变化（递进防抖）
     */
    fun observeChanges(): Flow<WatchEvent> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())
        var currentDelay = INITIAL_DEBOUNCE_MS
        var pendingRunnable: Runnable? = null

        val debounceRunnable = Runnable {
            trySend(WatchEvent.IncrementalSyncNeeded)
            // 重置防抖延迟
            currentDelay = INITIAL_DEBOUNCE_MS
        }

        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                // 取消之前的延迟任务
                pendingRunnable?.let { handler.removeCallbacks(it) }

                pendingRunnable = debounceRunnable
                handler.postDelayed(debounceRunnable, currentDelay)

                // 递进延长防抖时间（适应大量文件操作场景）
                currentDelay = (currentDelay * DEBOUNCE_FACTOR).toLong().coerceAtMost(MAX_DEBOUNCE_MS)
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        RythmeLogger.d(TAG, "MediaStoreWatcher 已启动")

        awaitClose {
            pendingRunnable?.let { handler.removeCallbacks(it) }
            context.contentResolver.unregisterContentObserver(observer)
            RythmeLogger.d(TAG, "MediaStoreWatcher 已停止")
        }
    }

    companion object {
        private const val TAG = "MediaStoreWatcher"
        private const val INITIAL_DEBOUNCE_MS = 500L
        private const val MAX_DEBOUNCE_MS = 5000L
        private const val DEBOUNCE_FACTOR = 1.5
    }
}

sealed interface WatchEvent {
    /** MediaStore version 变化，必须全量重建 */
    data object FullResyncNeeded : WatchEvent
    /** generation 变化，增量同步即可 */
    data object IncrementalSyncNeeded : WatchEvent
    /** 无变化 */
    data object NoChange : WatchEvent
}
