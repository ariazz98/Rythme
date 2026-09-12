package com.aria.rythme.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

/** 在当前可见窗口连续推进，不使用 animateScrollToItem 的远距离跳项优化。 */
internal suspend fun LazyListState.animateLyricsToItem(target: Int, maximumStepPx: Float) {
    scroll {
        fun remaining(): Float {
            val visible = layoutInfo.visibleItemsInfo
            visible.firstOrNull { it.index == target }?.let { return it.offset.toFloat() }
            val first = visible.firstOrNull() ?: return 0f
            val average = visible.map { it.size }.average().toFloat()
            return (target - first.index) * average + first.offset
        }
        while (abs(remaining()) > .5f) {
            var previousProgress = 0f
            var travelled = 0f
            animate(0f, 1f, animationSpec = tween(400, easing = FastOutSlowInEasing)) { progress, _ ->
                // 每帧依据新布局修正剩余距离，长短句不依赖固定行高，不在末帧补瞬移。
                val fraction = ((progress - previousProgress) / (1f - previousProgress).coerceAtLeast(.0001f))
                    .coerceIn(0f, 1f)
                // 不在一帧中跨过整行，未知高度的目标至少先进入可见窗口，再精确对齐。
                val limit = maximumStepPx.coerceAtLeast(1f)
                travelled += abs(scrollBy((remaining() * fraction).coerceIn(-limit, limit)))
                previousProgress = progress
            }
            if (travelled < .5f) break // 内容边界，不与布局限制反复争抢。
        }
    }
}
