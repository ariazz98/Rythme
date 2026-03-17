package com.aria.rythme.feature.player.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.domain.model.RepeatMode
import java.util.Locale

/**
 * 播放器页面契约
 *
 * 定义播放器页面的 Intent、State、Action 和 Effect。
 */

/**
 * 用户意图
 */
sealed interface PlayerIntent : UserIntent {
    /** 播放指定歌曲 */
    data class PlaySong(val song: Song) : PlayerIntent

    /** 播放/暂停切换 */
    data object TogglePlayPause : PlayerIntent

    /** 播放 */
    data object Play : PlayerIntent

    /** 暂停 */
    data object Pause : PlayerIntent

    /** 下一首 */
    data object Next : PlayerIntent

    /** 上一首 */
    data object Previous : PlayerIntent

    /** 跳转到指定位置 */
    data class SeekTo(val position: Long) : PlayerIntent

    /** 快进 */
    data object FastForward : PlayerIntent

    /** 快退 */
    data object Rewind : PlayerIntent

    /** 切换循环模式 */
    data object ToggleRepeatMode : PlayerIntent

    /** 切换随机播放 */
    data object ToggleShuffleMode : PlayerIntent

    /** 加载歌曲列表并随机播放一首 */
    data object LoadAndPlayRandom : PlayerIntent

    /** 选择播放列表中的歌曲 */
    data class SelectSongFromPlaylist(val index: Int) : PlayerIntent

    /** 设置音量百分比 */
    data class SetVolume(val percentage: Int) : PlayerIntent
}

/**
 * UI 状态
 */
data class PlayerState(
    /** 当前播放的歌曲 */
    val currentSong: Song? = null,

    /** 是否正在播放 */
    val isPlaying: Boolean = false,

    /** 当前播放位置（毫秒） */
    val currentPosition: Long = 0L,

    /** 歌曲总时长（毫秒） */
    val duration: Long = 0L,

    /** 播放列表 */
    val playlist: List<Song> = emptyList(),

    /** 当前歌曲在播放列表中的索引 */
    val currentIndex: Int = 0,

    /** 循环模式 */
    val repeatMode: RepeatMode = RepeatMode.OFF,

    /** 是否随机播放 */
    val isShuffleEnabled: Boolean = false,

    /** 封面主题色 */
    val themeColor: Int? = null,

    /** 音量百分比 (0-100) */
    val volume: Int = 0,

    /** 播放历史 */
    val playHistory: List<Song> = emptyList()
) : UiState {

    /**
     * 当前进度百分比（0-1）
     */
    val progress: Float
        get() = if (duration > 0) {
            currentPosition.toFloat() / duration.toFloat()
        } else {
            0f
        }

    /**
     * 格式化后的当前位置
     */
    val currentPositionText: String
        get() = formatDuration(currentPosition)

    /**
     * 格式化后的总时长
     */
    val durationText: String
        get() = formatDuration(duration)

    /**
     * 格式化时长
     */
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /**
     * 是否可以播放上一首
     */
    val canPlayPrevious: Boolean
        get() = playlist.isNotEmpty() && (currentIndex > 0 || repeatMode == RepeatMode.ALL)

    /**
     * 是否可以播放下一首
     */
    val canPlayNext: Boolean
        get() = playlist.isNotEmpty() && (currentIndex < playlist.size - 1 || repeatMode == RepeatMode.ALL)
}

/**
 * 内部动作
 */
sealed interface PlayerAction : InternalAction {
    /** 更新播放状态 */
    data class UpdatePlayState(val isPlaying: Boolean) : PlayerAction

    /** 更新当前歌曲 */
    data class UpdateCurrentSong(val song: Song?) : PlayerAction

    /** 更新进度 */
    data class UpdateProgress(val position: Long, val duration: Long) : PlayerAction

    /** 更新播放列表 */
    data class UpdatePlaylist(val playlist: List<Song>) : PlayerAction

    /** 更新当前索引 */
    data class UpdateCurrentIndex(val index: Int) : PlayerAction

    /** 更新循环模式 */
    data class UpdateRepeatMode(val mode: RepeatMode) : PlayerAction

    /** 更新随机播放状态 */
    data class UpdateShuffleMode(val enabled: Boolean) : PlayerAction

    /** 更新主题色 */
    data class UpdateThemeColor(val color: Int?) : PlayerAction

    /** 更新音量 */
    data class UpdateVolume(val volume: Int) : PlayerAction

    /** 更新播放历史 */
    data class UpdatePlayHistory(val history: List<Song>) : PlayerAction
}

/**
 * 副作用
 */
sealed interface PlayerEffect : SideEffect {
    /** 显示错误提示 */
    data class ShowError(val message: String) : PlayerEffect
    
    /** 显示消息提示 */
    data class ShowMessage(val message: String) : PlayerEffect
}
