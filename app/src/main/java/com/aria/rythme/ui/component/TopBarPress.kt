package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 21-39-14 原片：按下约 180ms 达峰；松开先回缩，再在约 550ms 内回正。 */
internal object TopBarPressMotion {
    val down = spring<Float>(0.62f, 570f, 0.001f)
    val up = spring<Float>(0.40f, 285f, 0.001f)
    // HDR 相邻帧 53→54：首个变化帧已约为 20%；之后变白主要是玻璃提亮，而非继续淡出。
    val iconDown = snap<Float>()
    val iconUp = spring<Float>(1f, 250f, 0.001f)
    val lightDown = tween<Float>(100, delayMillis = 16, easing = CubicBezierEasing(0.15f, 0.75f, 0.2f, 1f))
    val lightUp = spring<Float>(1f, 250f, 0.001f)

    // 09-08 22-59-19 的 PQ HDR 原片：按住仍有淡图标。
    // 0.20 是按局部线性亮度转换到当前合成方式后的近似值，不是 Apple 的私有实现参数。
    const val PressedIconAlpha = 0.20f
    fun iconTargetAlpha(pressed: Boolean, avatar: Boolean = false, dark: Boolean = false): Float =
        if (pressed && !avatar) { if (dark) 0.60f else PressedIconAlpha } else 1f

    // 参考的单/双按钮分别约 1.36 / 1.15，不是原先按宽度增加 8dp。
    fun scale(width: Float, avatar: Boolean, progress: Float): Float {
        if (!width.isFinite() || width <= 0f || !progress.isFinite()) return 1f
        val growth = if (avatar) TopBarComponentMetrics.AvatarPressedScale - 1f else 16f / width
        // 允许弹簧经过 0 和 1；只限制异常输入，不能剪掉录屏中的回缩/回弹。
        return 1f + growth * progress.coerceIn(-0.35f, 1.15f)
    }

    fun hitIndex(x: Float, width: Float, count: Int): Int? =
        if (!x.isFinite() || !width.isFinite() || width <= 0f || count <= 0 || x < 0f || x >= width) null
        else (x / width * count).toInt().coerceIn(0, count - 1)
}

/** 录屏按下后平面内部的灰色反射退去；不增强全局白色提亮或改变轮廓高光。 */
internal fun topBarPressBodyReflection(progress: Float): Brush = Brush.verticalGradient(
    *Array(97) { index ->
        val depth = index / 96f
        depth to Color.Black.copy(alpha = glassBodyReflectionAlpha(depth) * (1f - progress.coerceIn(0f, 1f)))
    }
)

/** 一个表面的反馈状态；不承担业务点击，也不添加长按后自动打开菜单的推断行为。 */
@Stable
internal class TopBarPressState(private val scope: CoroutineScope) {
    val amount = Animatable(0f)
    val light = Animatable(0f)
    var pressedKey: String? by mutableStateOf(null)
        private set
    private var motion: Job? = null
    var bounds by mutableStateOf(Rect.Zero)
    var canConnect by mutableStateOf(false)
    var avatar by mutableStateOf(false)
    var nominalWidth by mutableStateOf(45f)
    var touchPosition by mutableStateOf(Offset(0.5f, 0.5f))
        private set

    fun moveLight(position: Offset, size: IntSize) {
        if (size.width > 0 && size.height > 0) touchPosition = Offset(
            (position.x / size.width).coerceIn(0f, 1f),
            (position.y / size.height).coerceIn(0f, 1f)
        )
    }

    val scale: Float get() = TopBarPressMotion.scale(nominalWidth, avatar, amount.value)

    fun press(key: String) {
        pressedKey = key
        animate(true)
    }

    fun release() {
        pressedKey = null
        animate(false)
    }

    private fun animate(down: Boolean) {
        motion?.cancel()
        motion = scope.launch {
            launch { amount.animateTo(if (down) 1f else 0f, if (down) TopBarPressMotion.down else TopBarPressMotion.up) }
            launch { light.animateTo(if (down) 1f else 0f, if (down) TopBarPressMotion.lightDown else TopBarPressMotion.lightUp) }
        }
    }
}

internal fun Modifier.topBarPress(
    state: TopBarPressState,
    keys: List<String>,
    enabled: Boolean
): Modifier = pointerInput(state, keys, enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val index = TopBarPressMotion.hitIndex(down.position.x, size.width.toFloat(), keys.size)
        try {
            if (index != null) {
                state.moveLight(down.position, size)
                state.press(keys[index])
            }
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed || event.changes.any { it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding) }) break
                state.moveLight(change.position, size)
                if (awaitPointerEvent(PointerEventPass.Final).changes.any { it.isConsumed }) break
            }
        } finally {
            // 导航、菜单接管、移出或手势取消，都必须释放，不能留下卡住的放大态。
            state.release()
        }
    }
}
