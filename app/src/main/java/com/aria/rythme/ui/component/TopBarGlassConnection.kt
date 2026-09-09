package com.aria.rythme.ui.component

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** 共用一条距离场和轮廓，连接处不保留两个胶囊的内部描边。坐标均为父层像素。 */
internal data class TopBarJoinedGeometry(val left: Rect, val right: Rect, val smoothing: Float) {
    fun distance(x: Float, y: Float): Float {
        val a = capsuleDistance(left, x, y)
        val b = capsuleDistance(right, x, y)
        if (smoothing <= 0f) return min(a, b)
        val h = max(smoothing - abs(a - b), 0f) / smoothing
        return min(a, b) - h * h * smoothing * 0.25f
    }

    private fun capsuleDistance(rect: Rect, x: Float, y: Float): Float {
        val radius = rect.height / 2f
        return hypot(max(abs(x - rect.center.x) - (rect.width / 2f - radius), 0f), y - rect.center.y) - radius
    }

    fun outline(): Path {
        val path = Path()
        val centerY = (left.center.y + right.center.y) / 2f
        val extent = max(left.height, right.height) / 2f + smoothing
        fun heightAt(x: Float): Float {
            var low = 0f
            var high = extent
            repeat(12) {
                val mid = (low + high) / 2f
                if (distance(x, centerY - mid) <= 0f) low = mid else high = mid
            }
            return low
        }
        fun addContour(start: Float, end: Float) {
            val points = ArrayList<Offset>()
            points.add(Offset(start, 0f))
            fun segment(a: Offset, b: Offset, depth: Int) {
                val mid = Offset((a.x + b.x) / 2f, heightAt((a.x + b.x) / 2f))
                // 平坦区域少采样；圆弧和收腰加密到亚像素误差，降低描边 Path 运算的顶点数量。
                if (depth < 14 && (b.x - a.x > 16f || abs(mid.y - (a.y + b.y) / 2f) > 0.15f)) {
                    segment(a, mid, depth + 1)
                    segment(mid, b, depth + 1)
                } else points.add(b)
            }
            segment(points[0], Offset(end, 0f), 0)
            path.moveTo(start, centerY)
            points.drop(1).forEach { path.lineTo(it.x, centerY - it.y) }
            points.asReversed().forEach { path.lineTo(it.x, centerY + it.y) }
            path.close()
        }
        val first = if (left.center.x <= right.center.x) left else right
        val second = if (first == left) right else left
        fun boundary(inside: Float, outside: Float): Float {
            var a = inside
            var b = outside
            repeat(16) {
                val mid = (a + b) / 2f
                if (distance(mid, centerY) <= 0f) a = mid else b = mid
            }
            return a
        }
        // 导航会出现重叠、嵌套和交叉；平滑并集也可能略超出原始胶囊边界。
        val start = boundary(first.center.x, min(left.left, right.left) - smoothing - 1f)
        val end = boundary(second.center.x, max(left.right, right.right) + smoothing + 1f)
        val gapMid = (first.right + second.left) / 2f
        if (distance(gapMid, centerY) <= 0f) {
            addContour(start, end)
        } else {
            addContour(start, boundary(first.center.x, gapMid))
            addContour(boundary(second.center.x, gapMid), end)
        }
        return path
    }
}

internal fun TopBarPressState.scaledBounds(origin: Offset): Rect {
    val center = bounds.center - origin
    val half = Offset(bounds.width, bounds.height) * (scale / 2f)
    return Rect(center - half, center + half)
}

/** 仅按压相邻组时接管玻璃，静态、单独按钮、菜单与页面转场仍使用原表面。 */
@Composable
internal fun BoxScope.TopBarConnectedGlass(
    first: TopBarPressState,
    second: TopBarPressState,
    origin: Offset,
    backdrop: Backdrop,
    referenceScale: Float
) {
    val reflectionShader = remember { RuntimeShader(ConnectionSdf + ConnectionReflection) }
    val bodyShader = remember { RuntimeShader(ConnectionSdf + ConnectionBody) }
    val hdr = LocalGlassHdr.current
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val pressHighlight = rememberTopBarPressHighlight()
    val drawPadding = with(LocalDensity.current) { 24.dp.roundToPx() }
    val drawOrigin = origin - Offset(drawPadding.toFloat(), drawPadding.toFloat())
    val geometry = {
        TopBarJoinedGeometry(first.scaledBounds(drawOrigin), second.scaledBounds(drawOrigin),
            14f * first.bounds.height / TopBarComponentMetrics.SurfaceHeight)
    }
    // 背景采样、外阴影和光照都要读取轮廓；每次几何变化只求解一次，不能每个绘制通道重新算。
    val shape = remember(first, second, drawOrigin) {
        derivedStateOf {
            val outline = geometry().outline()
            object : Shape {
                override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(outline)
            }
        }
    }
    fun RuntimeShader.configure(g: TopBarJoinedGeometry) {
        setFloatUniform("left", g.left.left, g.left.top, g.left.right, g.left.bottom)
        setFloatUniform("right", g.right.left, g.right.top, g.right.right, g.right.bottom)
        setFloatUniform("smoothing", g.smoothing)
    }
    // 联合轮廓会越过原始 Row 的边缘，给背景采样/高光留出空间，但不改变静态布局或命中范围。
    Box(Modifier.matchParentSize().layout { measurable, constraints ->
        val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth + drawPadding * 2, constraints.maxHeight + drawPadding * 2))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-drawPadding, -drawPadding) }
    }) {
    GlassBackdropSurface(
        backdrop = backdrop,
        pressHdr = true,
        shape = { shape.value },
        effects = {
            vibrancy()
            blur(2.dp.toPx())
            val g = geometry()
            // Generic 复合轮廓不能交给仅支持圆角矩形的 lens；用同一圆映射及真实联合轮廓法线。
            padding = 0f
            runtimeShaderEffect("topBarConnectedLens", ConnectionSdf + ConnectionLens, "content") {
                setFloatUniform("left", g.left.left, g.left.top, g.left.right, g.left.bottom)
                setFloatUniform("right", g.right.left, g.right.top, g.right.right, g.right.bottom)
                setFloatUniform("smoothing", g.smoothing)
                setFloatUniform("amounts", 32.dp.toPx() * first.scale, 32.dp.toPx() * second.scale)
            }
        },
        reflectionBrush = {
            reflectionShader.configure(geometry())
            reflectionShader.setFloatUniform("headrooms",
                if (hdr.pressAvailable) maxOf(hdr.staticHeadroom, if (first.avatar) topBarAvatarGain(first.light.value, hdr.darkTheme) else 1f) else 1f,
                if (hdr.pressAvailable) maxOf(hdr.staticHeadroom, if (second.avatar) topBarAvatarGain(second.light.value, hdr.darkTheme) else 1f) else 1f)
            ShaderBrush(reflectionShader)
        },
        bodyReflectionBrush = {
            bodyShader.configure(geometry())
            bodyShader.setFloatUniform("lights", first.light.value.coerceIn(0f, 1f), second.light.value.coerceIn(0f, 1f))
            ShaderBrush(bodyShader)
        },
        onDrawSurface = {
            drawRect(containerColor)
            drawRect(pressHighlight.pair(first.scaledBounds(drawOrigin), first, second.scaledBounds(drawOrigin), second), blendMode = BlendMode.Plus)
            for (surface in listOf(first, second)) if (surface.avatar) {
                val rect = surface.scaledBounds(drawOrigin)
                val light = surface.light.value.coerceIn(0f, 1f)
                drawCircle(
                    Brush.verticalGradient(topBarAvatarColors(light, hdr.pressAvailable, hdr.darkTheme), rect.top, rect.bottom),
                    radius = rect.height / 2f - (TopBarComponentMetrics.AvatarInset * referenceScale).dp.toPx() * surface.scale,
                    center = rect.center
                )
            }
        }
    )
    }
}

internal const val ConnectionSdf = """
uniform float4 left;
uniform float4 right;
uniform float smoothing;
float capsule(float2 p, float4 r) {
    float2 halfSize = (r.zw-r.xy)*0.5;
    float2 q = p-(r.xy+r.zw)*0.5;
    return length(float2(max(abs(q.x)-(halfSize.x-halfSize.y),0.0),q.y))-halfSize.y;
}
float distanceField(float2 p) {
    float a=capsule(p,left), b=capsule(p,right);
    float h=max(smoothing-abs(a-b),0.0)/max(smoothing,0.001);
    return min(a,b)-h*h*smoothing*0.25;
}
float2 normalAt(float2 p) {
    float2 n=float2(distanceField(p+float2(0.5,0.0))-distanceField(p-float2(0.5,0.0)),
                    distanceField(p+float2(0.0,0.5))-distanceField(p-float2(0.0,0.5)));
    return n/max(length(n),0.001);
}
"""

internal const val ConnectionLens = """
uniform shader content;
uniform float2 amounts;
half4 main(float2 p) {
    bool first=capsule(p,left)<capsule(p,right);
    float radius=first ? (left.w-left.y)*0.5 : (right.w-right.y)*0.5;
    float depth=max(-distanceField(p),0.0);
    if (depth>=radius) return content.eval(p);
    float edge=clamp(1.0-depth/radius,0.0,1.0);
    float displacement=(1.0-sqrt(max(1.0-edge*edge,0.0)))*(first ? amounts.x : amounts.y);
    return content.eval(p-displacement*normalAt(p));
}
"""

internal const val ConnectionReflection = """
uniform float2 headrooms;
half4 main(float2 p) {
    half weight=half(pow(abs(normalAt(p).y),8.0));
    float headroom=capsule(p,left)<capsule(p,right) ? headrooms.x : headrooms.y;
    return half4(fromLinearSrgb(half3(headroom))*weight,weight);
}
"""

internal const val ConnectionBody = """
uniform float2 lights;
half4 main(float2 p) {
    bool first=capsule(p,left)<capsule(p,right);
    float4 r=first ? left : right;
    // 按压光照以各自的原始圆/胶囊为边界，收腰区只保留连接材质。
    float2 coverage=clamp(float2(0.5)-float2(capsule(p,left),capsule(p,right)),0.0,1.0);
    float2 localLights=lights*coverage;
    float light=max(localLights.x,localLights.y);
    float depth=(p.y-r.y)/(r.w-r.y);
    float x=(depth-0.27)/0.13;
    half dark=half(0.020*exp(-x*x*x*x)*(1.0-light));
    return half4(0.0,0.0,0.0,dark);
}
"""
