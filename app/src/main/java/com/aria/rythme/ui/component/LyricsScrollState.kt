package com.aria.rythme.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs

sealed interface LyricsScrollMode {
    data object AutoFollow : LyricsScrollMode
    data object ManualScrolling : LyricsScrollMode
    data object WaitingToResume : LyricsScrollMode
    data object ReturningToFollow : LyricsScrollMode
}

/** 位置只由四个阶段管理；模糊由阶段推导，控制区和跟随共用最后操作时间。 */
@Stable
class LyricsScrollState {
    var mode by mutableStateOf<LyricsScrollMode>(LyricsScrollMode.AutoFollow)
        private set
    var isTouching by mutableStateOf(false)
        private set
    var isLyricsTouching by mutableStateOf(false)
        private set
    var browsingAtTouchStart = false
        private set
    private var returningFromSelection by mutableStateOf(false)
    private var touchClearsBlur by mutableStateOf(true)
    val isTouchClarityDeferred get() = isLyricsTouching && !touchClearsBlur
    var lastInteractionEndMs by mutableStateOf(0L)
        private set

    val isAutoFollow get() = mode == LyricsScrollMode.AutoFollow
    val isClearMode get() = (isLyricsTouching && touchClearsBlur) || (!isAutoFollow && !returningFromSelection)
    val canFollow get() = !isTouching && !isLyricsTouching &&
        (isAutoFollow || mode == LyricsScrollMode.ReturningToFollow)

    // 与全播放器的空闲计时分开：按播放按钮不应使歌词清晰。
    fun onLyricsTouchDown(clearOnPress: Boolean = true) {
        if (!isLyricsTouching) {
            browsingAtTouchStart = isClearMode
            touchClearsBlur = clearOnPress
        }
        isLyricsTouching = true
    }
    fun allowLyricsTouchClarity() { if (isLyricsTouching) touchClearsBlur = true }
    fun onLyricsTouchReleased() { isLyricsTouching = false }

    fun onTouchDown() { isTouching = true }
    fun onTouchReleased(nowMs: Long) {
        isTouching = false
        lastInteractionEndMs = nowMs
    }
    fun onUserScrollStart() {
        returningFromSelection = false
        mode = LyricsScrollMode.ManualScrolling
    }
    fun onUserScrollStop(nowMs: Long) {
        if (mode == LyricsScrollMode.ManualScrolling) {
            mode = LyricsScrollMode.WaitingToResume
            lastInteractionEndMs = nowMs
        }
    }
    fun onPlaybackChanged(isPlaying: Boolean, nowMs: Long) {
        lastInteractionEndMs = nowMs
        if (!isPlaying && mode == LyricsScrollMode.ReturningToFollow && !returningFromSelection) {
            mode = LyricsScrollMode.WaitingToResume
        }
    }
    fun onLyricLineClicked() {
        returningFromSelection = true
        mode = LyricsScrollMode.ReturningToFollow
    }
    fun onResumeTimerFired(nowMs: Long, isPlaying: Boolean) {
        if (mode == LyricsScrollMode.WaitingToResume && !isTouching && !isLyricsTouching && isPlaying &&
            nowMs - lastInteractionEndMs >= AUTO_FOLLOW_RESUME_DELAY) {
            returningFromSelection = false
            mode = LyricsScrollMode.ReturningToFollow
        }
    }
    fun onFollowAnimationFinished() {
        if (mode == LyricsScrollMode.ReturningToFollow && !isTouching && !isLyricsTouching) mode = LyricsScrollMode.AutoFollow
    }
    fun canAutoHideControls(nowMs: Long, isPlaying: Boolean): Boolean =
        isPlaying && isAutoFollow && !isTouching && !isLyricsTouching &&
            nowMs - lastInteractionEndMs >= CONTROLS_HIDE_DELAY

    fun reset(nowMs: Long) {
        mode = LyricsScrollMode.AutoFollow
        isTouching = false
        isLyricsTouching = false
        browsingAtTouchStart = false
        returningFromSelection = false
        lastInteractionEndMs = nowMs
        touchClearsBlur = true
    }

    companion object {
        const val AUTO_FOLLOW_RESUME_DELAY = 3000L
        const val CONTROLS_HIDE_DELAY = 4000L
    }
}

/** 距离按歌词句而非视觉换行计算；浏览态允许直接选择任意句。 */
internal fun canSelectLyric(relativeIndex: Int, browsingBeforePress: Boolean): Boolean =
    browsingBeforePress || relativeIndex in -3..3

/** NestedScroll 的屏幕坐标速度；只在真实用户手势的 fling 起点调用，不累计跟手位移。 */
internal fun lyricsFlingControlsVisibility(
    velocityY: Float,
    minimumVelocity: Float,
    canScrollEarlier: Boolean,
    canScrollLater: Boolean
): Boolean? = when {
    !velocityY.isFinite() || velocityY == 0f || abs(velocityY) < minimumVelocity -> null
    velocityY > 0f && canScrollEarlier -> true
    velocityY < 0f && canScrollLater -> false
    else -> null
}

@Composable
fun rememberLyricsScrollState(): LyricsScrollState = remember { LyricsScrollState() }
