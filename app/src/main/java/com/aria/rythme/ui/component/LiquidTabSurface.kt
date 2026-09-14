package com.aria.rythme.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.aria.rythme.ui.component.utils.BottomBarMetrics
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule

/** 展开态底栏和页内分段控件共用，禁止在调用方另配一套液滴材质。 */
@Composable
internal fun BoxScope.LiquidTabContainerSurface(
    backdrop: Backdrop,
    containerColor: Color,
    pressProgress: () -> Float,
    layerBlock: GraphicsLayerScope.() -> Unit
) {
    GlassBackdropSurface(
        backdrop = backdrop, shape = { ContinuousCapsule },
        effects = { vibrancy(); blur(2.dp.toPx()); glassLens(24.dp.toPx(), 32.dp.toPx()) },
        layerBlock = layerBlock, pressProgress = pressProgress,
        onDrawSurface = { drawRect(containerColor) }
    )
}

private val LiquidTabInnerShadow = Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.10f), offset = DpOffset.Zero, spread = 0.dp)

@Composable
internal fun BoxScope.LiquidTabSelectionSurface(
    backdrop: Backdrop,
    selectedColor: Color,
    pressProgress: () -> Float,
    scaleX: () -> Float,
    scaleY: () -> Float
) {
    GlassBackdropSurface(
        backdrop = backdrop, shape = { ContinuousCapsule },
        effects = {
            val progress = pressProgress()
            glassLens(size.height * BottomBarMetrics.SelectorRefractionHeightRatio * progress,
                size.height * BottomBarMetrics.SelectorRefractionAmountRatio * progress, chromaticAberration = true)
        },
        lightingAlpha = pressProgress, shadow = null, bodyReflectionEnabled = true,
        layerBlock = { this.scaleX = scaleX(); this.scaleY = scaleY() },
        onDrawSurface = { drawRect(selectedColor, alpha = 1f - pressProgress()) }
    )
    Box(Modifier.matchParentSize().graphicsLayer {
        this.scaleX = scaleX(); this.scaleY = scaleY(); alpha = pressProgress().coerceIn(0f, 1f)
    }.innerShadow(ContinuousCapsule, LiquidTabInnerShadow))
}
