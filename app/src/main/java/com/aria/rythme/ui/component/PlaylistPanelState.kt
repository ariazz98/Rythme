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

    /** 切换阈值：50% 内容高度 */
    val switchThresholdPx: Float get() = contentHeightPx * 0.5f

    /** 头部折叠偏移（0 = 完全展开, nowPlayingHeightPx = 完全折叠） */
    var headerCollapseOffset by mutableFloatStateOf(0f)
}
