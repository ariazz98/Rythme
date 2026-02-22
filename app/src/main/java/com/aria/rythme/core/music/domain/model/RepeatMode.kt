package com.aria.rythme.core.music.domain.model

import androidx.media3.common.Player

/**
 * 播放循环模式
 * 
 * 统一的循环模式枚举，供 PlaybackController 和 UI 层共同使用。
 */
enum class RepeatMode {
    /** 不循环 */
    OFF,
    
    /** 单曲循环 */
    ONE,
    
    /** 列表循环 */
    ALL;
    
    /**
     * 转换为 ExoPlayer 的循环模式
     */
    fun toExoPlayerMode(): Int = when (this) {
        OFF -> Player.REPEAT_MODE_OFF
        ONE -> Player.REPEAT_MODE_ONE
        ALL -> Player.REPEAT_MODE_ALL
    }
    
    companion object {
        /**
         * 从 ExoPlayer 的循环模式转换
         */
        fun fromExoPlayerMode(mode: Int): RepeatMode = when (mode) {
            Player.REPEAT_MODE_ONE -> ONE
            Player.REPEAT_MODE_ALL -> ALL
            else -> OFF
        }
    }
}
