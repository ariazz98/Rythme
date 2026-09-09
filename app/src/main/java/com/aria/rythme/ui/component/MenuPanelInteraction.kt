package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Size
import com.aria.rythme.ui.component.utils.InteractiveHighlight
import kotlinx.coroutines.CoroutineScope

/** 外壳与内容共享按压/拖动状态，显隐切换不重新创建菜单交互。 */
internal class MenuPanelInteraction(scope: CoroutineScope) {
    val dragX = Animatable(0f)
    val dragY = Animatable(0f)
    val highlight = InteractiveHighlight(scope, surfaceAlpha = 0f, radius = { it.width * 2 / 3 })
}

@Composable
internal fun rememberMenuPanelInteraction(): MenuPanelInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { MenuPanelInteraction(scope) }
}

internal data class MenuPanelTransform(val x: Float, val y: Float, val scaleX: Float, val scaleY: Float)

/** 原菜单的阻尼参数不变；使用面板尺寸而非带投影留白的外层画布计算。 */
internal fun menuPanelTransform(dx: Float, dy: Float, size: Size, unit: Float, anchor: PanelAnchor): MenuPanelTransform {
    fun damp(v: Float) = 6f * unit * v / (100f * unit + kotlin.math.abs(v))
    val nyRaw = (dy / size.height.coerceAtLeast(1f)).coerceIn(-1f, 1f) * .03f
    val ny = if (anchor == PanelAnchor.TopEnd) nyRaw else -nyRaw
    val nx = ((if (dx > 0f) dx * .15f else dx) / size.width.coerceAtLeast(1f)).coerceIn(-1f, 1f) * .03f
    return MenuPanelTransform(
        if (dx < 0f) damp(dx) else 0f,
        if ((anchor == PanelAnchor.TopEnd && dy > 0f) || (anchor == PanelAnchor.BottomEnd && dy < 0f)) damp(dy) else 0f,
        1f - ny - nx, 1f + ny + nx
    )
}
