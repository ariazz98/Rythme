package com.aria.rythme.ui.component

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * 播放列表面板状态（折叠头部 + 双列表方案）
 */
@Stable
class PlaylistPanelState {
    /** "当前播放"项的高度（像素），通过 onSizeChanged 更新 */
    var nowPlayingHeightPx by mutableFloatStateOf(0f)

    /** 容器总高度（像素），用于切换阈值计算 */
    var contentHeightPx by mutableFloatStateOf(0f)

    /** 历史标题 + 实际行高；未测量时沿用原来的整屏范围。 */
    var historyContentHeightPx by mutableFloatStateOf(Float.POSITIVE_INFINITY)

    val historyExtentPx: Float
        get() = minOf(contentHeightPx, historyContentHeightPx).coerceAtLeast(0f)

    /** 弹性切换的半程阈值不变；仅短历史按用户要求使用实际内容高度。 */
    val switchThresholdPx: Float get() = historyExtentPx * 0.5f

    /** 头部折叠偏移（0 = 完全展开, nowPlayingHeightPx = 完全折叠） */
    var headerCollapseOffset by mutableFloatStateOf(0f)
}
