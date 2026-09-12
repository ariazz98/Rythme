package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf

/** 公共头部读取队列既有折叠/历史位移；正文仍由队列自身管理。 */
@Stable
internal class CompactPanelHeaderGeometry {
    var heightPx by mutableFloatStateOf(0f)
    var collapsePx by mutableFloatStateOf(0f)
    var historyOffsetPx by mutableFloatStateOf(0f)
}

internal object CompactPanelMotion {
    const val ExitMs = 150
    const val QueueEnterMs = 300
    const val LyricsEnterMs = 550
    const val EnterDelayMs = 90
    const val QueueTravelDp = 12
    const val LyricsTravelDp = 128
}

/** 09-09 22:42 HDR 原片：约 500ms 收尾，无过冲；文字换位发生在淡化阶段。 */
internal object PlayerPanelMotion {
    const val Duration = 500
    val Easing = CubicBezierEasing(.2f, 0f, .15f, 1f)
    val artworkBounds = BoundsTransform { _, _ -> tween(Duration, easing = Easing) }

    fun titleBoundsAt(start: Rect, end: Rect, millis: Int): Rect {
        val fraction = (millis.toFloat() / Duration).coerceIn(0f, 1f)
        val vertical = lerp(start, end, Easing.transform(fraction))
        // 大标题沿左边上移，不在仍清晰时斜穿到小封面的右侧。
        val horizontal = Easing.transform(((millis - 150f) / 50f).coerceIn(0f, 1f))
        val left = start.left + (end.left - start.left) * horizontal
        return Rect(left, vertical.top, left + vertical.width, vertical.bottom)
    }

    val titleBounds = BoundsTransform { start, end ->
        keyframes {
            durationMillis = Duration
            for (ms in 0..Duration step 10) titleBoundsAt(start, end, ms) at ms
        }
    }
}
