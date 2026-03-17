package com.aria.rythme.ui.component

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs

/**
 * 播放列表面板状态（单 LazyColumn 吸附方案）
 */
@Stable
class PlaylistPanelState {
    /** 历史记录数量，外部动态更新 */
    var historyCount by mutableIntStateOf(0)

    /** "当前播放"项的高度（像素），通过 onSizeChanged 更新 */
    var nowPlayingHeightPx by mutableFloatStateOf(0f)

    /** 是否有历史记录 */
    val hasHistory: Boolean get() = historyCount > 0

    /** 当前播放项的索引（有历史时 +1 for header item） */
    val nowPlayingIndex: Int get() = if (hasHistory) historyCount + 1 else 0

    /** 四个按钮 stickyHeader 的索引 */
    val buttonsIndex: Int get() = nowPlayingIndex + 1
}

/**
 * 自定义 FlingBehavior：fling 结束后根据吸附规则滚到目标位置
 */
@Composable
fun rememberPlaylistSnapFlingBehavior(
    listState: LazyListState,
    panelState: PlaylistPanelState
): FlingBehavior {
    val decaySpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return remember(listState, panelState, decaySpec) {
        PlaylistSnapFlingBehavior(listState, panelState, decaySpec)
    }
}

private class PlaylistSnapFlingBehavior(
    private val listState: LazyListState,
    private val panelState: PlaylistPanelState,
    private val decaySpec: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // 1. 执行默认 fling 动画
        var remainingVelocity = initialVelocity
        if (abs(initialVelocity) > 1f) {
            val state = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
            var lastValue = 0f
            state.animateDecay(decaySpec) {
                val delta = value - lastValue
                lastValue = value
                val consumed = scrollBy(delta)
                if (abs(delta - consumed) > 0.5f) {
                    cancelAnimation()
                }
                remainingVelocity = velocity
            }
        }

        // 2. fling 结束后执行吸附
        snapToTarget()

        return remainingVelocity
    }

    private suspend fun ScrollScope.snapToTarget() {
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        val nowPlayingIndex = panelState.nowPlayingIndex
        val buttonsIndex = panelState.buttonsIndex
        val nowPlayingHeight = panelState.nowPlayingHeightPx

        when {
            // 当前可见的第一项是"当前播放"，部分被滚出
            firstIndex == nowPlayingIndex && firstOffset > 0 -> {
                if (nowPlayingHeight > 0f) {
                    val scrolledFraction = firstOffset / nowPlayingHeight
                    if (scrolledFraction >= 0.5f) {
                        // 吸附到按钮行：需要继续滚 (nowPlayingHeight - firstOffset)
                        animateSnap(nowPlayingHeight - firstOffset)
                    } else {
                        // 回弹到当前播放：回滚 firstOffset
                        animateSnap(-firstOffset.toFloat())
                    }
                }
            }

            // 在历史区域，看是否接近当前播放
            firstIndex < nowPlayingIndex -> {
                val nowPlayingInfo = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == nowPlayingIndex }

                if (nowPlayingInfo != null) {
                    val viewportHeight = listState.layoutInfo.viewportEndOffset -
                            listState.layoutInfo.viewportStartOffset
                    val visiblePart = (listState.layoutInfo.viewportEndOffset - nowPlayingInfo.offset)
                        .coerceAtMost(nowPlayingInfo.size)
                        .coerceAtLeast(0)
                    if (visiblePart > viewportHeight * 0.3f) {
                        // 吸附到当前播放：滚动到 nowPlaying 顶部对齐 viewport 顶部
                        val distance = nowPlayingInfo.offset - listState.layoutInfo.viewportStartOffset
                        animateSnap(distance.toFloat())
                    }
                }
            }

            // 按钮行或之后 → 自由滚动
            else -> { /* no snap */ }
        }
    }

    /**
     * 用 spring 动画驱动吸附滚动
     */
    private suspend fun ScrollScope.animateSnap(distance: Float) {
        if (abs(distance) < 1f) return
        var previousValue = 0f
        AnimationState(initialValue = 0f).animateTo(
            targetValue = distance,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) {
            val delta = value - previousValue
            previousValue = value
            scrollBy(delta)
        }
    }
}
