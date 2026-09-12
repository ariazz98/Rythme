package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal data class QueueScrollThumb(val top: Float, val height: Float)

internal fun queueScrollThumb(count: Int, rowHeight: Float, viewport: Float,
    firstIndex: Int, firstOffset: Int, reverse: Boolean, minimum: Float): QueueScrollThumb? {
    if (count <= 0 || rowHeight <= 0f || viewport <= 0f) return null
    val extent = count * rowHeight
    if (extent <= viewport) return null
    val height = (viewport * viewport / extent).coerceIn(minimum.coerceAtMost(viewport), viewport)
    val fraction = ((firstIndex * rowHeight + firstOffset) / (extent - viewport)).coerceIn(0f, 1f)
    return QueueScrollThumb((viewport - height) * if (reverse) 1f - fraction else fraction, height)
}

/** 仅作滚动位置提示，不改变列表手势或弹性切换。 */
@Composable
internal fun PlayerListScrollbar(state: LazyListState, modifier: Modifier, reverse: Boolean = false,
    activeOverride: Boolean = false, enabled: Boolean = true) {
    val active = enabled && (state.isScrollInProgress || activeOverride) && (state.canScrollBackward || state.canScrollForward)
    val alpha by animateFloatAsState(if (active) .45f else 0f,
        tween(durationMillis = if (active) 80 else 250, delayMillis = if (active) 0 else 350))
    Canvas(modifier) {
        if (alpha <= 0f) return@Canvas
        val info = state.layoutInfo
        val sizes = info.visibleItemsInfo.map { it.size }.sorted()
        val row = sizes.getOrNull(sizes.size / 2)?.toFloat() ?: return@Canvas
        val thumb = queueScrollThumb(info.totalItemsCount, row, size.height,
            state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, reverse, 18.dp.toPx()) ?: return@Canvas
        val width = 2.dp.toPx()
        drawRoundRect(Color.White.copy(alpha = alpha), Offset(size.width - width - 3.dp.toPx(), thumb.top),
            Size(width, thumb.height), CornerRadius(width / 2))
    }
}
