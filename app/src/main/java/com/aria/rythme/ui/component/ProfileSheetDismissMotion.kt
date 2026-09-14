package com.aria.rythme.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import kotlin.math.roundToInt

/** 松手速度转换为曲线起始斜率；只走剩余距离，离开屏幕时不再减速停顿。 */
internal class ProfileSheetDismissMotion(velocity: Float, remaining: Float, height: Float) {
    private val distance = remaining.coerceAtLeast(1f)
    private val speed = velocity.coerceAtLeast(0f)
    private val distanceDuration = (220f * distance / height.coerceAtLeast(1f)).roundToInt().coerceIn(80, 220)
    // 极快的甩动缩短时间，保持曲线控制点单调，同时仍保留实际松手速度。
    val durationMs = if (remaining <= 1f) 1 else minOf(distanceDuration,
        if (speed > 0f) (3000f * distance / speed).toInt().coerceAtLeast(1) else distanceDuration)
    private val slope = (speed * durationMs / (1000f * distance)).coerceIn(0f, 3f)
    val easing = CubicBezierEasing(.2f, .2f * slope, .65f, .7f)
}
