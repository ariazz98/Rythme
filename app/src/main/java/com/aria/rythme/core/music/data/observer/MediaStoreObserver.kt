package com.aria.rythme.core.music.data.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * MediaStore 内容观察者
 * 
 * 监听设备音频文件的变化（新增、删除、修改），
 * 用于实现增量扫描机制。
 */
class MediaStoreObserver(
    private val context: Context
) {
    companion object {
        private const val TAG = "MediaStoreObserver"
        
        // 防抖延迟（毫秒）- 避免短时间内多次触发
        private const val DEBOUNCE_DELAY_MS = 1000L
    }
    
    /**
     * 观察 MediaStore 音频变化
     * 
     * 返回一个 Flow，当音频文件发生变化时发出事件。
     * 内置防抖机制，避免短时间内多次触发。
     */
    fun observeChanges(): Flow<MediaStoreChange> = callbackFlow {
        var pendingChange: MediaStoreChange? = null
        val handler = Handler(Looper.getMainLooper())
        
        val debounceRunnable = Runnable {
            pendingChange?.let { change ->
                trySend(change)
                RythmeLogger.d(TAG, "MediaStore 变化: $change")
            }
            pendingChange = null
        }
        
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                
                // 取消之前的延迟任务
                handler.removeCallbacks(debounceRunnable)
                
                // 记录变化类型
                val changeType = when {
                    uri == null -> ChangeType.UNKNOWN
                    uri.toString().contains("media/external/audio") -> ChangeType.AUDIO_CHANGED
                    else -> ChangeType.UNKNOWN
                }
                
                pendingChange = MediaStoreChange(
                    type = changeType,
                    uri = uri,
                    timestamp = System.currentTimeMillis()
                )
                
                // 延迟发送（防抖）
                handler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS)
            }
        }
        
        // 注册观察者
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        RythmeLogger.d(TAG, "MediaStore 观察者已注册")
        
        // 等待 Flow 关闭
        awaitClose {
            handler.removeCallbacks(debounceRunnable)
            context.contentResolver.unregisterContentObserver(observer)
            RythmeLogger.d(TAG, "MediaStore 观察者已注销")
        }
    }
}

/**
 * MediaStore 变化事件
 */
data class MediaStoreChange(
    val type: ChangeType,
    val uri: Uri?,
    val timestamp: Long
)

/**
 * 变化类型
 */
enum class ChangeType {
    /** 音频文件变化 */
    AUDIO_CHANGED,
    /** 未知变化 */
    UNKNOWN
}
