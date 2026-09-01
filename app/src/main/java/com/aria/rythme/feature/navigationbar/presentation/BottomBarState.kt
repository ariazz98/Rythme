package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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
import kotlin.math.abs

/**
 * BottomBar 的纯展示状态。
 *
 * 它只处理 Tab 点击和用户滚动事件，不读取页面的绝对滚动位置。
 */
@Stable
class BottomBarState internal constructor(
    private val scrollThresholdPx: Float,
    initialPrimaryTabIndex: Int = 0
) {
    var isExpanded by mutableStateOf(true)
        private set

    var lastPrimaryTabIndex by mutableIntStateOf(initialPrimaryTabIndex.coerceIn(PRIMARY_TAB_INDICES))
        private set

    private var scrollDirection = 0
    private var accumulatedScrollPx = 0f

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (source == NestedScrollSource.UserInput) {
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
        val direction = when {
            dragDeltaY > 0f -> SCROLLING_UP
            dragDeltaY < 0f -> SCROLLING_DOWN
            else -> return
        }

        if (direction != scrollDirection) {
            scrollDirection = direction
            accumulatedScrollPx = 0f
        }

        accumulatedScrollPx += abs(dragDeltaY)
        if (accumulatedScrollPx < scrollThresholdPx) return

        if (direction == SCROLLING_DOWN) collapse() else expand()
        resetScrollAccumulator()
    }

    private fun expand() {
        isExpanded = true
    }

    private fun collapse() {
        isExpanded = false
    }

    private fun resetScrollAccumulator() {
        scrollDirection = 0
        accumulatedScrollPx = 0f
    }

    private companion object {
        val PRIMARY_TAB_INDICES = 0..2
        const val SCROLLING_UP = 1
        const val SCROLLING_DOWN = -1
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
