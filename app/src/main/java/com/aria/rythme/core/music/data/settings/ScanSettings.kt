package com.aria.rythme.core.music.data.settings

/**
 * 扫描设置
 *
 * 用户可配置的音乐扫描过滤参数。
 *
 * @param minDurationMs 最小时长（毫秒），低于此时长的音频将被过滤
 * @param minSizeBytes 最小文件大小（字节），低于此大小的音频将被过滤
 * @param excludeSystemDirs 是否排除系统音效目录（Ringtones/Alarms/Notifications）
 */
data class ScanSettings(
    val minDurationMs: Long = DEFAULT_MIN_DURATION_MS,
    val minSizeBytes: Long = DEFAULT_MIN_SIZE_BYTES,
    val excludeSystemDirs: Boolean = true
) {
    companion object {
        /** 默认最小时长：30秒 */
        const val DEFAULT_MIN_DURATION_MS = 30_000L
        
        /** 默认最小大小：100KB */
        const val DEFAULT_MIN_SIZE_BYTES = 100 * 1024L
        
        /** 系统音效目录列表 */
        val SYSTEM_AUDIO_DIRS = listOf(
            "Ringtones",
            "Alarms", 
            "Notifications",
            "录音",
            "Recordings",
            "Voice Recorder"
        )
    }
    
    /**
     * 最小时长（秒），用于 UI 显示
     */
    val minDurationSeconds: Int
        get() = (minDurationMs / 1000).toInt()
    
    /**
     * 最小大小（KB），用于 UI 显示
     */
    val minSizeKB: Int
        get() = (minSizeBytes / 1024).toInt()
}
