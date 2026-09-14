package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/** 原片按下出现圆形底光，图标收小；松手恢复。点击仍由原控件执行。 */
@Composable
internal fun PlayerPressFeedback(
    size: Dp,
    haloSize: Dp = size,
    modifier: Modifier = Modifier,
    haloColor: Color = Color.White,
    content: @Composable BoxScope.(MutableInteractionSource) -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val amount by animateFloatAsState(if (pressed) 1f else 0f, tween(if (pressed) 80 else 220))
    Box(modifier.size(size).drawBehind {
        drawCircle(haloColor.copy(alpha = .1f * amount), radius = haloSize.toPx() / 2f)
    }, contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer {
            scaleX = 1f - .55f * amount
            scaleY = scaleX
        }, contentAlignment = Alignment.Center) { content(interactions) }
    }
}
