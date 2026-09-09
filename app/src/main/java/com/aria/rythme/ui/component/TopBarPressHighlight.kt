package com.aria.rythme.ui.component

import android.graphics.RuntimeShader
import android.graphics.RenderEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.aria.rythme.ui.theme.rythmeColors
import kotlin.math.exp

/** 22-59-19 PQ 原片：局部底光约 +12%，触点光斑再增加约 80%，sigma 约为按压高度的 0.78。 */
internal fun topBarPressLightStrength(distanceInHeights: Float, progress: Float): Float {
    if (!distanceInHeights.isFinite() || !progress.isFinite()) return 0f
    val r = distanceInHeights / 0.78f
    return progress.coerceIn(0f, 1f) * (0.12f + 0.80f * exp(-0.5f * r * r))
}

/** 深色中性底面约从 1.4 升到 11/100 参考白；这是 sRGB 合成域增量，不是浅色 HDR 白光。 */
internal fun topBarDarkPressDelta(distanceInHeights: Float, progress: Float): Float =
    0.25f * topBarPressLightStrength(distanceInHeights, progress) / 0.92f

internal fun topBarDarkForegroundGain(progress: Float): Float =
    if (progress.isFinite()) 1f + 2f * progress.coerceIn(0f, 1f) else 1f

/** 头像的内盘与外层玻璃各有光照，两者共享窗口时取较高需求。 */
internal fun topBarPressHeadroom(progress: Float, avatar: Boolean, dark: Boolean): Float {
    val surface = if (dark) topBarDarkForegroundGain(progress) else 1f + topBarPressLightStrength(0f, progress)
    return if (avatar) maxOf(surface, topBarAvatarGain(progress, dark)) else surface
}

@Composable
internal fun TopBarPressHeadroom(press: TopBarPressState, avatar: Boolean) {
    val hdr = LocalGlassHdr.current
    val desired = if (hdr.pressAvailable) {
        topBarPressHeadroom(press.light.value, avatar, hdr.darkTheme)
    } else hdr.staticHeadroom
    SideEffect { hdr.requestPressHeadroom(press, desired) }
    DisposableEffect(hdr, press) { onDispose { hdr.requestPressHeadroom(press, 0f) } }
}

internal fun topBarAvatarGain(progress: Float, dark: Boolean = false): Float =
    if (progress.isFinite()) 1f + (if (dark) 5.5f else 0.9f) * progress.coerceIn(0f, 1f) else 1f

private fun hdrGainColor(color: Color, gain: Float): Color {
    val linear = color.convert(ColorSpaces.LinearExtendedSrgb)
    return Color(linear.red * gain, linear.green * gain, linear.blue * gain, linear.alpha,
        ColorSpaces.LinearExtendedSrgb).convert(ColorSpaces.ExtendedSrgb)
}

internal fun topBarAvatarColors(progress: Float, hdr: Boolean, dark: Boolean = false): List<Color> = if (dark) {
    // 23-49-19 深色 PQ 原片静态内盘拟合；浅色的蓝色基底不变。
    listOf(hdrGainColor(Color(0xFF595569), topBarAvatarGain(progress, true)),
        hdrGainColor(Color(0xFF2E2347), topBarAvatarGain(progress, true)))
} else if (hdr) {
    listOf(hdrGainColor(HeaderAvatarTop, topBarAvatarGain(progress)), hdrGainColor(HeaderAvatarBottom, topBarAvatarGain(progress)))
} else {
    listOf(lerp(HeaderAvatarTop, Color(0xFFCCFCFF), progress.coerceIn(0f, 1f)),
        lerp(HeaderAvatarBottom, Color(0xFF8CA4FF), progress.coerceIn(0f, 1f)))
}

/** 同时覆盖字母和用户照片，在线性光中增益，不改变色相，也不复制一份前景。 */
@Composable
internal fun Modifier.topBarAvatarLight(press: TopBarPressState): Modifier = topBarForegroundLight(press, avatar = true)

@Composable
internal fun Modifier.topBarForegroundLight(press: TopBarPressState, avatar: Boolean = false): Modifier {
    val state = LocalGlassHdr.current
    val hdr = state.pressAvailable && (avatar || state.darkTheme)
    val shader = remember { RuntimeShader("""
        uniform shader content;
        uniform float gain;
        half4 main(float2 p) {
            half4 c=content.eval(p);
            if (c.a<=0.0001) return c;
            return half4(fromLinearSrgb(toLinearSrgb(c.rgb/c.a)*gain)*c.a,c.a);
        }
    """.trimIndent()) }
    return if (!hdr) this else drawWithCache {
        // headroom 改变后重建离屏目标，不能复用按下前分配的 SDR RenderNode 缓存。
        @Suppress("UNUSED_VARIABLE") val generation = state.generation
        val layer = obtainGraphicsLayer()
        var appliedGain = Float.NaN
        onDrawWithContent {
            val gain = if (avatar) topBarAvatarGain(press.light.value, state.darkTheme) else topBarDarkForegroundGain(press.light.value)
            if (gain <= 1.001f) drawContent() else {
                if (gain != appliedGain) {
                    // RenderEffect 保存创建时的 uniform 快照，不能先创建再修改 shader。
                    // 增益变化才重建；保持按住时复用，HDR 缓存重建后首次绘制也先写入有效值。
                    shader.setFloatUniform("gain", gain)
                    layer.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
                    appliedGain = gain
                }
                layer.record { this@onDrawWithContent.drawContent() }
                drawLayer(layer)
            }
        }
    }
}

/** 光斑与几何/图标分层：只在已有玻璃表面内增亮，不覆盖图标，也不复制前景。 */
internal class TopBarPressHighlight(private val hdr: Boolean, private val dark: Boolean) {
    private val shader = RuntimeShader(PressHighlightShader)
    private val brush = ShaderBrush(shader)

    fun single(size: Size, press: TopBarPressState): ShaderBrush = pair(
        Rect(0f, 0f, size.width, size.height), press,
        Rect(0f, 0f, size.width, size.height), press, false
    )

    fun pair(first: Rect, a: TopBarPressState, second: Rect, b: TopBarPressState, dual: Boolean = true): ShaderBrush {
        shader.setFloatUniform("first", first.left, first.top, first.right, first.bottom)
        shader.setFloatUniform("second", second.left, second.top, second.right, second.bottom)
        shader.setFloatUniform("touches", first.left + a.touchPosition.x * first.width, first.top + a.touchPosition.y * first.height,
            second.left + b.touchPosition.x * second.width, second.top + b.touchPosition.y * second.height)
        shader.setFloatUniform("progress", a.light.value.coerceIn(0f, 1f), b.light.value.coerceIn(0f, 1f))
        shader.setFloatUniform("dual", if (dual) 1f else 0f)
        shader.setFloatUniform("hdr", if (hdr) 1f else 0f)
        shader.setFloatUniform("dark", if (dark) 1f else 0f)
        return brush
    }
}

@Composable
internal fun rememberTopBarPressHighlight(): TopBarPressHighlight {
    val hdr = LocalGlassHdr.current.pressAvailable
    val dark = MaterialTheme.rythmeColors.surface.luminance() < 0.5f
    return remember(hdr, dark) { TopBarPressHighlight(hdr, dark) }
}

private const val PressHighlightShader = """
uniform float4 first;
uniform float4 second;
uniform float4 touches;
uniform float2 progress;
uniform float dual;
uniform float hdr;
uniform float dark;
float capsuleDistance(float2 p, float4 r) {
    float2 halfSize=(r.zw-r.xy)*0.5;
    float2 q=p-(r.xy+r.zw)*0.5;
    return length(float2(max(abs(q.x)-(halfSize.x-halfSize.y),0.0),q.y))-halfSize.y;
}
half4 main(float2 p) {
    // 各自按完整胶囊裁剪，不能按最近表面把连接区竖直分成两半。
    float coverageA=clamp(0.5-capsuleDistance(p,first),0.0,1.0);
    float coverageB=dual*clamp(0.5-capsuleDistance(p,second),0.0,1.0);
    float2 qa=(p-touches.xy)/max((first.w-first.y)*0.78,1.0);
    float2 qb=(p-touches.zw)/max((second.w-second.y)*0.78,1.0);
    float wa=coverageA*progress.x*(0.12+0.80*exp(-0.5*dot(qa,qa)));
    float wb=coverageB*progress.y*(0.12+0.80*exp(-0.5*dot(qb,qb)));
    float weight=max(wa,wb);
    // 无高光时 Plus 层必须是透明零，不能把尚未完成首帧采样的玻璃强制变成不透明黑底。
    // 按压的既有亮度/合成参数保持不变；轮廓外也不应写入 alpha。
    if (weight <= 0.0) return half4(0.0);
    // 两个亮度端点必须转换到同一工作色彩空间，再求 Plus 增量。
    // HDR headroom 改变时，工作空间中的 SDR 白不保证是字面值 1。
    half delta=dark>0.5 ? half(0.25*weight/0.92) : (hdr>0.5 ?
        (fromLinearSrgb(half3(1.0+weight))-fromLinearSrgb(half3(1.0))).r : half(0.04*weight/0.92));
    return half4(delta,delta,delta,1.0);
}
"""
