package com.aria.rythme.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/** 正常换句的试调值集中在此，不能用于手动回归或远距离 seek。 */
internal object LyricsPresentationMotion {
    const val RowDurationMs = 520
    const val RowDelayMs = 40
    const val MaxDelayRows = 4
    const val TotalDurationMs = RowDurationMs + RowDelayMs * MaxDelayRows
    const val ControlsHideMs = 220
    const val ControlsShowMs = 300
    const val GlyphLiftDp = 2f
    const val UnsungAlpha = .5f
    // 16:52:26 同句当前/非当前字形对照约为 1 : 0.98；按住或控制区显隐不额外缩放。
    fun textScale(currentWeight: Float): Float = .98f + .02f * currentWeight.coerceIn(0f, 1f)
    val Easing = CubicBezierEasing(.2f, 0f, .2f, 1f)

    fun rowProgress(elapsedMs: Float, relativeIndex: Int): Float {
        val delay = (relativeIndex + 1).coerceIn(0, MaxDelayRows) * RowDelayMs
        return Easing.transform(((elapsedMs - delay) / RowDurationMs).coerceIn(0f, 1f))
    }

    // 非当前行始终低于当前句未唱字；淡化与散焦分开，避免用强模糊掩盖过亮的笔画。
    fun alpha(relativeIndex: Int, browsing: Boolean = false): Float = when {
        relativeIndex == 0 -> 1f
        relativeIndex < 0 -> .28f
        browsing -> (.30f - .03f * (relativeIndex - 1)).coerceAtLeast(.18f)
        relativeIndex == 1 -> .30f
        relativeIndex == 2 -> .24f
        relativeIndex == 3 -> .16f
        relativeIndex == 4 -> .09f
        relativeIndex == 5 -> .04f
        else -> .02f
    }
    fun blurDp(relativeIndex: Int, browsing: Boolean): Float = when {
        browsing || relativeIndex == 0 -> 0f
        // 用户实机对照：已唱行接近参考下两到三行；原下三行才接近参考下一行。
        relativeIndex < 0 -> 6.3f
        else -> (4.8f + (relativeIndex - 1) + if (relativeIndex >= 3) 1f else 0f).coerceAtMost(9f)
    }
}

/** 一个列表的基础滚动 + 每行补偿，文字始终只有一份，手动拖动可中断。 */
@Stable
internal class LyricsLineWave {
    var running by mutableStateOf(false)
    var targetItem by mutableIntStateOf(0)
    var distance by mutableFloatStateOf(0f)
    var consumed by mutableFloatStateOf(0f)
    var elapsed by mutableFloatStateOf(0f)

    fun offset(itemIndex: Int): Float = consumed - distance *
        LyricsPresentationMotion.rowProgress(elapsed, itemIndex - targetItem)
}

internal suspend fun LazyListState.animateLyricsLineChange(target: Int, wave: LyricsLineWave): Boolean {
    val targetInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == target } ?: return false
    val distance = targetInfo.offset.toFloat()
    if (abs(distance) <= .5f) return true
    wave.targetItem = target
    wave.distance = distance
    wave.consumed = 0f
    wave.elapsed = 0f
    wave.running = true
    try {
        scroll {
            animate(0f, LyricsPresentationMotion.TotalDurationMs.toFloat(), animationSpec =
                tween(LyricsPresentationMotion.TotalDurationMs, easing = LinearEasing)) { time, _ ->
                val base = distance * LyricsPresentationMotion.rowProgress(time, -1)
                wave.consumed += scrollBy(base - wave.consumed)
                wave.elapsed = time
            }
        }
    } finally {
        // 保留最后补偿值供手势中断时平滑退去，不立即清零导致字行跳动。
        wave.running = false
    }
    return true
}
