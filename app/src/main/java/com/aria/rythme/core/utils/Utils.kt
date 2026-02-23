package com.aria.rythme.core.utils

import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp

@Composable
fun rememberScreenCornerRadius(): Int {
    val view = LocalView.current
    return remember(view) {
        val insets = view.rootWindowInsets
        // 获取四个角中最大的圆角值
        maxOf(
            insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0,
            insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0,
            insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0,
            insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
        )
    }
}

@Composable
fun rememberScreenCornerRadiusDp(): Dp {
    val density = LocalDensity.current
    val radiusPx = rememberScreenCornerRadius()
    return with(density) { radiusPx.toDp() }
}