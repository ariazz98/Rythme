package com.aria.rythme.ui.component.utils

import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt

internal const val TAB_MOTION_STIFFNESS = 700f / (0.7f * 0.7f)
internal const val MINI_MOTION_STIFFNESS = 700f / (1.2f * 1.2f)

/** 宽度回弹峰值按目标宽度的约 2% 计算，两态宽度差不同也保持相近的视觉幅度。 */
internal fun capsuleWidthDamping(targetWidth: Float, travel: Float): Float {
    if (travel <= 0f) return 1f
    val peakRatio = (targetWidth * 0.02f / travel).coerceIn(0.001f, 0.25f)
    val logPeak = ln(peakRatio)
    return -logPeak / sqrt(PI.toFloat() * PI.toFloat() + logPeak * logPeak)
}

// 纵向位移比上一版缩短 20%，贴近宽度变化的节奏；宽度弹簧参数不变。
private val miniPlayerPositionDurationMillis = (
    FloatSpringSpec(dampingRatio = 1f, stiffness = MINI_MOTION_STIFFNESS, visibilityThreshold = 0.01f)
        .getDurationNanos(0f, 1f, 0f) / 1_000_000L
    ).toInt().let { (it * 0.8f).toInt().coerceAtLeast(1) }

/** 直接作用于时间：收起由慢到快，展开由快到慢，不再对弹簧输出做二次变换。 */
internal fun miniPlayerPositionAnimationSpec(expanding: Boolean) = tween<Float>(
    durationMillis = miniPlayerPositionDurationMillis,
    easing = if (expanding) EaseOutQuad else EaseInQuad
)

/** 独立胶囊、图标和点击区域共享坐标；展开态仍由完整的四 Tab 胶囊承载。 */
internal class BottomTabMorphGeometry(
    val width: Float,
    val height: Float,
    val diameter: Float,
    val padding: Float,
    val expansion: Float,
    val isLtr: Boolean
) {
    val tabWidth = (width - padding * 2f) / 4f
    private val split = padding + tabWidth * 3f
    // Search 独立后主胶囊仍从完整宽度开始收缩，初期允许两枚独立玻璃面重叠。
    val primary = physical(Rect(0f, 0f, lerp(diameter, width, expansion), height))
    val search = physical(Rect(width - lerp(diameter, width - split, expansion), 0f, width, height))

    // 位置跟随同一个实际宽度：正常形变时固定外侧边缘，只有超出端点的回弹才向两边扩散。
    // 不能用 expansion 的中心承载独立宽度弹簧，否则两条曲线的相位差会把外沿额外顶出去。
    fun primaryBounds(animatedWidth: Float): Rect {
        val baseWidth = animatedWidth.coerceIn(diameter, width)
        val left = (baseWidth - animatedWidth) / 2f
        return physical(Rect(left, 0f, left + animatedWidth, height))
    }

    fun searchBounds(animatedWidth: Float): Rect {
        val baseWidth = animatedWidth.coerceIn(diameter, width - split)
        val right = width + (animatedWidth - baseWidth) / 2f
        return physical(Rect(right - animatedWidth, 0f, right, height))
    }

    fun iconCenter(index: Int): Offset {
        val expandedX = padding + tabWidth * (index + 0.5f)
        val compactX = if (index == 3) width - diameter / 2f else diameter / 2f
        val x = lerp(compactX, expandedX, expansion)
        return Offset(if (isLtr) x else width - x, diameter / 2f)
    }

    /** 选择器沿同一条图标轨迹移动；小数索引保留展开态的连续拖动。 */
    fun selectorCenterX(index: Float): Float {
        val position = index.coerceIn(0f, 3f)
        val start = position.toInt()
        val end = (start + 1).coerceAtMost(3)
        return lerp(iconCenter(start).x, iconCenter(end).x, position - start)
    }

    private fun physical(rect: Rect): Rect = if (isLtr) rect else {
        Rect(width - rect.right, rect.top, width - rect.left, rect.bottom)
    }
}
