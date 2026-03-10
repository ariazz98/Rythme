package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 搜索框的初始显示模式
 */
enum class HeaderMode {
    /** 搜索框默认隐藏，初始 offset = 0 */
    COLLAPSED,
    /** 搜索框默认展开，初始 offset = searchHeight */
    EXPANDED
}

/**
 * 可折叠搜索框的嵌套滚动状态
 *
 * 通过 [NestedScrollConnection] 拦截上滑/下拉手势来折叠/展开搜索框区域。
 * 两个吸附点：0（折叠）、maxOffset（展开）。
 * 松手后自动吸附到最近的吸附点。
 */
@Stable
class CollapsibleHeaderState(
    private val searchHeightPx: Float,
    initialOffset: Float
) {
    val maxOffset: Float = searchHeightPx

    var currentOffset by mutableFloatStateOf(initialOffset)
        private set

    // 用于松手吸附动画
    private val animatable = Animatable(initialOffset)

    /** 搜索框可见比例 0..1 */
    val searchFraction: Float
        get() = if (searchHeightPx > 0f) (currentOffset / searchHeightPx).coerceIn(0f, 1f) else 0f

    val nestedScrollConnection = object : NestedScrollConnection {
        // 上滑时先折叠搜索框，再让列表滚动
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f) return Offset.Zero
            val current = currentOffset
            if (current <= 0f) return Offset.Zero

            val consumed = available.y.coerceAtLeast(-current)
            currentOffset = (current + consumed).coerceIn(0f, maxOffset)
            return Offset(0f, consumed)
        }

        // 下拉时列表先滚动，列表到顶后展开搜索框
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (available.y <= 0f) return Offset.Zero
            val current = currentOffset
            if (current >= maxOffset) return Offset.Zero

            val toConsume = available.y.coerceAtMost(maxOffset - current)
            currentOffset = (current + toConsume).coerceIn(0f, maxOffset)
            return Offset(0f, toConsume)
        }

        // fling 结束后吸附到最近边界
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val current = currentOffset
            val mid = maxOffset / 2f
            val target = if (current < mid) 0f else maxOffset
            if (abs(current - target) > 1f) {
                animatable.snapTo(current)
                animatable.animateTo(target) {
                    currentOffset = value
                }
            }
            return Velocity.Zero
        }
    }
}

@Composable
fun rememberCollapsibleHeaderState(
    mode: HeaderMode,
    searchHeight: Dp = 56.dp
): CollapsibleHeaderState {
    val density = LocalDensity.current
    val searchHeightPx = with(density) { searchHeight.toPx() }

    return remember(mode) {
        val initialOffset = when (mode) {
            HeaderMode.COLLAPSED -> 0f
            HeaderMode.EXPANDED -> searchHeightPx
        }
        CollapsibleHeaderState(searchHeightPx, initialOffset)
    }
}
