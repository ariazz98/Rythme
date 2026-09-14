package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import kotlin.math.ceil

/** Header 专用材质；不改变按钮、菜单或 BottomBar 的玻璃参数。 */
internal object HeaderBackdropMaterial {
    const val topRadiusDp = 24f
    const val bottomRadiusDp = 6f

    fun strength(depth: Float): Float {
        val t = depth.coerceIn(0f, 1f)
        return 1f - t * t * (3f - 2f * t)
    }

    fun tintAlpha(depth: Float): Float = .40f + .45f * strength(depth)
}

/** 两个方向的可变半径采样，不用固定模糊叠透明渐变冒充渐进模糊。 */
internal fun BackdropEffectScope.headerProgressiveBlur() {
    val top = HeaderBackdropMaterial.topRadiusDp.dp.toPx()
    val bottom = HeaderBackdropMaterial.bottomRadiusDp.dp.toPx()
    // 预滤波避免大半径的离散采样留下重影；为上下文预留真实内容，不复制 Header 底沿。
    val prefilter = 3.dp.toPx()
    padding = ceil(top * 3f + prefilter)
    blur(prefilter)
    for (vertical in listOf(false, true)) {
        runtimeShaderEffect(
            if (vertical) "headerBlurVertical" else "headerBlurHorizontal",
            HeaderProgressiveBlurShader,
            "content"
        ) {
            setFloatUniform("extent", size.width, size.height)
            setFloatUniform("inset", padding)
            setFloatUniform("radii", top, bottom)
            setFloatUniform("axis", if (vertical) 0f else 1f, if (vertical) 1f else 0f)
        }
    }
}

internal fun DrawScope.drawHeaderBackdropSurface(surface: Color, showDivider: Boolean = true) {
    drawRect(Brush.verticalGradient(
        *Array(17) { index ->
            val depth = index / 16f
            depth to surface.copy(alpha = HeaderBackdropMaterial.tintAlpha(depth))
        },
        endY = size.height
    ))
    if (!showDivider) return
    // 一物理像素的分隔，不加投影；跟随外层 Header 的可见进度一起退出。
    val line = if (surface.luminance() < .5f) Color.White.copy(alpha = .16f)
        else Color.Black.copy(alpha = .12f)
    drawRect(line, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f))
}

private const val HeaderProgressiveBlurShader = """
uniform shader content;
uniform float2 extent;
uniform float inset;
uniform float2 radii;
uniform float2 axis;

half4 main(float2 p) {
    float depth = clamp((p.y - inset) / extent.y, 0.0, 1.0);
    float strength = 1.0 - depth * depth * (3.0 - 2.0 * depth);
    float radius = mix(radii.y, radii.x, strength);
    float4 result = float4(0.0);
    float total = 0.0;
    for (int i = -12; i <= 12; i++) {
        float distance = float(i) * 0.25;
        float weight = exp(-0.5 * distance * distance);
        float2 sampleAt = p + axis * (radius * distance);
        // Header 位于窗口顶端；只在屏幕边界夹取，底部仍采样正文。
        sampleAt = clamp(sampleAt, float2(inset + 0.5),
            float2(inset + extent.x - 0.5, extent.y + inset * 2.0 - 0.5));
        result += float4(content.eval(sampleAt)) * weight;
        total += weight;
    }
    return half4(result / total);
}
"""
