package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * BottomBar 的纯展示状态。
 *
 * 向下用户滚动累计到阈值时收起；向上回到页面顶部或点击 Tab 时展开。
 * 页面通过回调提供实时的到顶状态，不在这里持有列表或网格状态。
 */
@Stable
class BottomBarState internal constructor(
    private val scrollThresholdPx: Float,
    initialPrimaryTabIndex: Int = 0
) {
    var isExpanded by mutableStateOf(true)
        private set

    /** 仅表示本次向下滚动距离，不绑定页面绝对位置；到阈值时保持 1，避免收起首帧跳变。 */
    var collapsePreparationProgress by mutableFloatStateOf(0f)
        private set

    var lastPrimaryTabIndex by mutableIntStateOf(initialPrimaryTabIndex.coerceIn(PRIMARY_TAB_INDICES))
        private set

    private var accumulatedScrollPx = 0f

    fun nestedScrollConnection(isAtTop: () -> Boolean) = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // 使用滚动消费后的实际位置；惯性滚动到顶也展开，不依赖剩余拖动距离。
            // 仅响应向上滚动/顶端下拉，横向轮播或页面初次布局不会误触发展开。
            if ((consumed.y > 0f || available.y > 0f) && isAtTop()) {
                expand()
                resetScrollAccumulator()
            } else if (source == NestedScrollSource.UserInput) {
                onUserScroll(consumed.y)
            }
            return Offset.Zero
        }
    }

    /** 任意 Tab 点击都强制展开；Search 不覆盖最近的主 Tab。 */
    fun onTabSelected(index: Int) {
        if (index in PRIMARY_TAB_INDICES) {
            lastPrimaryTabIndex = index
        }
        expand()
        resetScrollAccumulator()
    }

    private fun onUserScroll(dragDeltaY: Float) {
        if (dragDeltaY == 0f) return
        if (dragDeltaY > 0f) {
            // 反向只取消未完成的预收起；已收起时保持缩放，直到实际回顶。
            resetScrollAccumulator()
            if (isExpanded) collapsePreparationProgress = 0f
            return
        }
        if (!isExpanded) return

        accumulatedScrollPx -= dragDeltaY
        collapsePreparationProgress = (accumulatedScrollPx / scrollThresholdPx).coerceIn(0f, 1f)
        if (accumulatedScrollPx < scrollThresholdPx) return

        collapse()
        resetScrollAccumulator()
    }

    private fun expand() {
        isExpanded = true
        collapsePreparationProgress = 0f
    }

    private fun collapse() {
        isExpanded = false
    }

    private fun resetScrollAccumulator() {
        accumulatedScrollPx = 0f
    }

    private companion object {
        val PRIMARY_TAB_INDICES = 0..2
    }
}

@Composable
fun rememberBottomBarState(
    initialPrimaryTabIndex: Int = 0,
    scrollThreshold: Dp = 48.dp
): BottomBarState {
    val density = LocalDensity.current
    val thresholdPx = with(density) { scrollThreshold.toPx() }
    return remember(thresholdPx) { BottomBarState(thresholdPx, initialPrimaryTabIndex) }
}

val LocalBottomBarState = staticCompositionLocalOf<BottomBarState> {
    error("BottomBarState must be provided")
}
