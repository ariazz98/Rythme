package com.aria.rythme.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 歌词滚动模式
 */
sealed interface LyricsScrollMode {
    /** 自动跟踪当前行 */
    data object AutoFollow : LyricsScrollMode

    /** 用户手动滚动中 */
    data object ManualScrolling : LyricsScrollMode

    /** 手动滚动结束，等待恢复自动跟踪 */
    data class WaitingToResume(
        val followResumeTimeMs: Long,  // 恢复自动跟踪的时间（3s）
        val clearModeEndTimeMs: Long   // 恢复模糊效果的时间（5s）
    ) : LyricsScrollMode
}

/**
 * 歌词滚动状态机
 *
 * 三态：AutoFollow → ManualScrolling → WaitingToResume → AutoFollow
 *
 * 替代原来的 isManualScrolling、isClearMode、isProgrammaticScroll、
 * autoFollowResumeJob、clearModeResumeJob 五个独立状态。
 */
@Stable
class LyricsScrollState {
    var mode by mutableStateOf<LyricsScrollMode>(LyricsScrollMode.AutoFollow)
        private set

    /** 自动跟踪中 */
    val isAutoFollow: Boolean get() = mode is LyricsScrollMode.AutoFollow

    /** 清晰模式（非自动跟踪时关闭模糊和 alpha） */
    val isClearMode: Boolean get() = mode !is LyricsScrollMode.AutoFollow

    /** 标记程序触发的滚动，用于区分用户手动滚动 */
    var isProgrammaticScroll by mutableStateOf(false)
        internal set

    /** 用户开始手动滚动 */
    fun onUserScrollStart() {
        mode = LyricsScrollMode.ManualScrolling
    }

    /** 用户停止滚动，进入等待恢复状态 */
    fun onUserScrollStop(nowMs: Long) {
        mode = LyricsScrollMode.WaitingToResume(
            followResumeTimeMs = nowMs + AUTO_FOLLOW_RESUME_DELAY,
            clearModeEndTimeMs = nowMs + CLEAR_MODE_RESUME_DELAY
        )
    }

    /** 用户点击歌词行，立即恢复自动跟踪 */
    fun onLyricLineClicked() {
        mode = LyricsScrollMode.AutoFollow
    }

    /** 歌词行变更时，如果处于等待状态则恢复（模糊恢复） */
    fun onLyricIndexChanged() {
        if (mode is LyricsScrollMode.WaitingToResume) {
            mode = LyricsScrollMode.AutoFollow
        }
    }

    /** 恢复计时器到期 */
    fun onResumeTimerFired() {
        mode = LyricsScrollMode.AutoFollow
    }

    companion object {
        const val AUTO_FOLLOW_RESUME_DELAY = 3000L
        const val CLEAR_MODE_RESUME_DELAY = 5000L
    }
}

@Composable
fun rememberLyricsScrollState(): LyricsScrollState {
    return remember { LyricsScrollState() }
}
