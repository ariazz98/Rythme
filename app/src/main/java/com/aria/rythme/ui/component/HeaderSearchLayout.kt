package com.aria.rythme.ui.component

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/** 保留已验收尺寸；其余位置从表面、触摸区和间距推导，不各自维护偏移。 */
internal object HeaderSearchLayout {
    val surfaceHeight = 44.dp
    val inlineVerticalPadding = 6.dp
    val inlineHeight get() = surfaceHeight + inlineVerticalPadding * 2
    val toolbarHeight get() = HeaderLayout.toolbar
    val toolbarSurfaceInset get() = (toolbarHeight - surfaceHeight) / 2
    val horizontalInset get() = HeaderLayout.horizontalPadding
    val closeGap = 12.dp
    val closeTouchSize get() = toolbarHeight
    val closeTouchEndInset get() = horizontalInset - (closeTouchSize - surfaceHeight) / 2

    fun bodyEndPadding(progress: Float) = lerp(
        horizontalInset, horizontalInset + surfaceHeight + closeGap, progress
    )
}

/** 现有视觉时序保持不变；这些是经过确认的动画参数，不是布局补偿值。 */
internal object HeaderSearchMotion {
    const val activationDuration = 300
    val closeEnterTravel = 52.dp
    private const val placeholderRevealStart = .9f
    private const val placeholderInteractiveAlpha = .9f
    private const val chromeFadeMultiplier = 3f

    fun placeholderAlpha(reveal: Float) =
        ((reveal - placeholderRevealStart) / (1f - placeholderRevealStart)).coerceIn(0f, 1f)
    fun placeholderInteractive(reveal: Float) = placeholderAlpha(reveal) > placeholderInteractiveAlpha
    fun chromeAlpha(progress: Float) = (1f - progress * chromeFadeMultiplier).coerceIn(0f, 1f)
}
