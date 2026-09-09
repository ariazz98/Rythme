package com.aria.rythme.ui.component

import android.graphics.RuntimeShader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule

/** 只在导航期间接管绘制。共同图标只有一份；稳定态仍使用原控件与菜单锚点。 */
@Composable
internal fun BoxScope.NavigationHeaderActions(frame: HeaderActionFrame, backdrop: Backdrop, referenceScale: Float) {
    if (frame.shells.isEmpty()) return
    val density = LocalDensity.current.density
    val unit = density * referenceScale
    val padding = 24f * density
    fun Rect.pixels() = Rect(
        padding + (frame.width + left) * unit,
        padding + 34f * density + (top - 34f) * unit,
        padding + (frame.width + right) * unit,
        padding + 34f * density + (bottom - 34f) * unit
    )
    val rectangles = frame.shells.map { it.bounds.pixels() }
    val geometry by rememberUpdatedState(TopBarJoinedGeometry(rectangles.first(), rectangles.last(),
        if (rectangles.size > 1) HeaderActionMotion.ConnectionSmoothing * unit * frame.connection else 0f))
    // 同宽布局不会触发尺寸失效；与按压连接相同，让采样和描边缓存直接观察轮廓状态。
    val shape = remember {
        derivedStateOf {
            val path = geometry.outline()
            object : Shape {
                override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(path)
            }
        }
    }
    val reflection = remember { RuntimeShader(ConnectionSdf + ConnectionReflection) }
    val body = remember { RuntimeShader(ConnectionSdf + ConnectionBody) }
    fun RuntimeShader.configure() {
        setFloatUniform("left", geometry.left.left, geometry.left.top, geometry.left.right, geometry.left.bottom)
        setFloatUniform("right", geometry.right.left, geometry.right.top, geometry.right.right, geometry.right.bottom)
        setFloatUniform("smoothing", geometry.smoothing)
    }
    val hdr = LocalGlassHdr.current
    val color = MaterialTheme.rythmeColors.bottomBackground
    // 使用真实并集采样一次背景，避免两个表面的染色、暗边和折射在连接处叠加。
    Box(Modifier.align(Alignment.Center)
        .requiredSize((frame.width * referenceScale + 48f).dp, 116.dp)
        .glassHdrFadeAndBlur(alpha = { frame.shellAlpha }, blurDp = { frame.shellBlur * referenceScale })) {
        GlassBackdropSurface(
            backdrop = backdrop,
            shape = { shape.value },
            pressHdr = true,
            effects = {
                vibrancy()
                blur(2.dp.toPx())
                this.padding = 0f
                runtimeShaderEffect("topBarNavigationLens", ConnectionSdf + ConnectionLens, "content") {
                    setFloatUniform("left", geometry.left.left, geometry.left.top, geometry.left.right, geometry.left.bottom)
                    setFloatUniform("right", geometry.right.left, geometry.right.top, geometry.right.right, geometry.right.bottom)
                    setFloatUniform("smoothing", geometry.smoothing)
                    setFloatUniform("amounts", 32.dp.toPx(), 32.dp.toPx())
                }
            },
            reflectionBrush = {
                reflection.configure()
                val headroom = if (hdr.pressAvailable) hdr.staticHeadroom else 1f
                reflection.setFloatUniform("headrooms", headroom, headroom)
                ShaderBrush(reflection)
            },
            bodyReflectionBrush = {
                body.configure()
                body.setFloatUniform("lights", 0f, 0f)
                ShaderBrush(body)
            },
            onDrawSurface = { drawRect(color) }
        )
    }
    val scope = rememberCoroutineScope()
    val neutralPress = remember(scope) { TopBarPressState(scope) }
    Box(Modifier.matchParentSize().drawWithContent {
        // 新侧部尚未长成时，散焦图标也属于玻璃内部，不在轮廓外悬空出现。
        val outline = shape.value.createOutline(size, layoutDirection, this)
        translate(-padding, -padding) {
            clipPath(outline.path) { translate(padding, padding) { this@drawWithContent.drawContent() } }
        }
    }) {
    frame.glyphs.forEach { glyph -> key(glyph.identity) {
        Box(Modifier.offset(x = ((frame.width + glyph.x - glyph.width / 2f) * referenceScale).dp,
            y = (34f - TopBarComponentMetrics.SurfaceHeight * referenceScale / 2f).dp)
            .requiredSize((glyph.width * referenceScale).dp, (TopBarComponentMetrics.SurfaceHeight * referenceScale).dp)
            .graphicsLayer { scaleX = glyph.scale; scaleY = glyph.scale }
            .glassHdrFadeAndBlur(alpha = { glyph.alpha }, blurDp = { glyph.blur * referenceScale }),
            contentAlignment = Alignment.Center) {
            if (glyph.action is Action.Avatar) {
                Box(Modifier.size((TopBarComponentMetrics.avatarSize * referenceScale).dp)
                    .background(Brush.verticalGradient(topBarAvatarColors(0f, hdr.pressAvailable, hdr.darkTheme)), ContinuousCapsule))
            }
            ActionItem(glyph.action, (glyph.width * referenceScale).dp, referenceScale, 0.dp, neutralPress, onClick = null)
        }
    } }
    }
}
