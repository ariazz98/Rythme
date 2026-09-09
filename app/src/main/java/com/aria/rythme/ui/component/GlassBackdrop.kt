package com.aria.rythme.ui.component

import android.graphics.Paint as AndroidPaint
import android.graphics.RuntimeShader
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp

// 录屏白底按钮的轮廓外只有少量灰阶衰减，使用轻微、近乎居中的环境投影。
internal val GlassSurfaceShadow = Shadow(
    radius = 12.dp,
    offset = DpOffset(0.dp, 0.75.dp),
    color = Color.Black.copy(alpha = 0.035f)
)

// 悬浮菜单在参考中有更宽、更明显的落影，与贴近页面的按钮区别处理。
internal val GlassMenuShadow = Shadow(
    radius = 24.dp,
    offset = DpOffset(0.dp, 4.dp),
    color = Color.Black.copy(alpha = 0.10f)
)

internal data class GlassLightingProfile(
    val rimWidth: Dp,
    val rimTopAlpha: Float,
    val rimSideAlpha: Float,
    val coreWidth: Dp,
    val coreAlpha: Float,
    val glowAlpha: Float,
    val directionPower: Float = 8f
)

private val LightGlassLighting = GlassLightingProfile(
    rimWidth = 0.45.dp,
    rimTopAlpha = 0.16f,
    rimSideAlpha = 0.34f,
    coreWidth = 0.55.dp,
    coreAlpha = 0.95f,
    glowAlpha = 1f
)
private val DarkGlassLighting = GlassLightingProfile(
    rimWidth = 0.40.dp,
    rimTopAlpha = 0.15f,
    rimSideAlpha = 0.30f,
    coreWidth = 0.50.dp,
    coreAlpha = 0.20f,
    glowAlpha = 0.22f
)

internal fun glassLightingProfile(darkTheme: Boolean): GlassLightingProfile =
    if (darkTheme) DarkGlassLighting else LightGlassLighting

/**
 * 折射范围不能越过胶囊中线：Backdrop 的 SDF 法线在中线退化为零向量。
 * 限制到半尺寸后，中线会由 shader 的距离判断直接返回原采样，避免彩色细线。
 * 按每帧真实尺寸计算，兼容共享元素形变；原本在安全范围内的折射参数不变。
 */
internal fun BackdropEffectScope.glassLens(
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean = false
) {
    lens(
        refractionHeight = glassRefractionHeight(refractionHeight, size.minDimension),
        refractionAmount = refractionAmount,
        chromaticAberration = chromaticAberration
    )
}

internal fun glassRefractionHeight(requested: Float, minDimension: Float): Float {
    if (!requested.isFinite() || !minDimension.isFinite() || requested <= 0f || minDimension <= 0f) {
        return 0f
    }
    return requested.coerceAtMost(minDimension / 2f)
}

/** 背景单独更新绘制缓存；兄弟节点中的图标、手势和动画状态不被重新创建。 */
@Composable
internal fun BoxScope.GlassBackdropSurface(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    hdr: Boolean = true,
    lightingAlpha: () -> Float = { 1f },
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    shadow: Shadow? = GlassSurfaceShadow,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    bodyReflectionEnabled: Boolean = true,
    pressProgress: () -> Float = { 0f },
    reflectionBrush: (() -> Brush)? = null,
    bodyReflectionBrush: (() -> Brush)? = null,
    rimHeadroom: () -> Float = { GlassHdrHeadroom },
    pressHdr: Boolean = false,
    rimHeadroomLimit: Float = GlassHdrHeadroom
) {
    val state = LocalGlassHdr.current
    val useHdr = hdr && (state.enabled || (pressHdr && state.pressAvailable))
    key(useHdr, if (useHdr) state.generation else 0) {
        Box(Modifier.matchParentSize().drawGlassBackdrop(
            backdrop = backdrop,
            shape = shape,
            effects = effects,
            lightingAlpha = lightingAlpha,
            shadow = shadow,
            layerBlock = layerBlock,
            onDrawSurface = onDrawSurface,
            bodyReflectionEnabled = bodyReflectionEnabled,
            pressProgress = pressProgress,
            reflectionBrush = reflectionBrush,
            bodyReflectionBrush = bodyReflectionBrush,
            highlightHeadroom = if (useHdr) rimHeadroom() else 1f,
            rimHeadroomLimit = rimHeadroomLimit
        ))
    }
}

/**
 * 只负责玻璃照明，不改变调用处的模糊、折射、布局或形变。
 * 柔光来自真实轮廓的一次连续模糊，不叠加描边，也不把亮色相加到过曝。
 */
@Composable
internal fun Modifier.drawGlassBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    lightingAlpha: () -> Float = { 1f },
    shadow: Shadow? = GlassSurfaceShadow,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    highlightHeadroom: Float = 1f,
    bodyReflectionEnabled: Boolean = true,
    pressProgress: () -> Float = { 0f },
    reflectionBrush: (() -> Brush)? = null,
    bodyReflectionBrush: (() -> Brush)? = null,
    rimHeadroomLimit: Float = GlassHdrHeadroom
): Modifier {
    // 跟随实际 RythmeTheme，而不是直接读系统开关，主题预览或覆盖也保持一致。
    val profile = glassLightingProfile(MaterialTheme.rythmeColors.surface.luminance() < 0.5f)
    val directionShader = remember { RuntimeShader(GlassDirectionShader) }
    return drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = effects,
        highlight = null,
        shadow = { shadow?.copy(alpha = shadow.alpha * lightingAlpha().coerceIn(0f, 1f)) },
        innerShadow = null,
        layerBlock = layerBlock,
        onDrawSurface = onDrawSurface
    ).drawWithCache {
        if (size.minDimension <= 0f) {
            return@drawWithCache onDrawWithContent { drawContent() }
        }
        val actualShape = shape()
        val outline = actualShape.createOutline(size, layoutDirection, this@drawWithCache)
        val path = Path().apply {
            when (outline) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
        }
        val rimWidth = profile.rimWidth.toPx().coerceAtMost(size.minDimension / 4f)
        val edgeWidth = profile.coreWidth.toPx().coerceAtMost(size.minDimension / 4f)
        // 从真实轮廓减掉暗边的覆盖区，得到白光内沿。不缩放形状，也不猜超椭圆的内圆角。
        val rimBand = Path()
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE
            strokeWidth = 2f * rimWidth
            strokeJoin = AndroidPaint.Join.ROUND
        }.getFillPath(path.asAndroidPath(), rimBand.asAndroidPath())
        val innerPath = Path.combine(PathOperation.Difference, path, rimBand)

        // 圆角几何只用于估算高光方向；绘制与裁剪始终使用上面的真实 Path。
        val maxRadius = size.minDimension / 2f
        val corners = actualShape as? CornerBasedShape
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val radii = if (corners != null) {
            floatArrayOf(
                (if (isLtr) corners.topStart else corners.topEnd).toPx(size, this),
                (if (isLtr) corners.topEnd else corners.topStart).toPx(size, this),
                (if (isLtr) corners.bottomEnd else corners.bottomStart).toPx(size, this),
                (if (isLtr) corners.bottomStart else corners.bottomEnd).toPx(size, this)
            )
        } else when (outline) {
            is Outline.Rounded -> floatArrayOf(
                outline.roundRect.topLeftCornerRadius.x, outline.roundRect.topRightCornerRadius.x,
                outline.roundRect.bottomRightCornerRadius.x, outline.roundRect.bottomLeftCornerRadius.x
            )
            is Outline.Rectangle -> FloatArray(4)
            is Outline.Generic -> FloatArray(4) { maxRadius }
        }
        radii.indices.forEach { radii[it] = radii[it].coerceIn(0f, maxRadius) }
        directionShader.setFloatUniform("extent", size.width, size.height)
        directionShader.setFloatUniform("radii", radii)
        directionShader.setFloatUniform("power", profile.directionPower)
        directionShader.setFloatUniform("headroom", highlightHeadroom.coerceIn(1f, rimHeadroomLimit.coerceAtLeast(1f)))
        // 复合轮廓可提供真实法线；普通玻璃仍完整沿用原来的光照与材质参数。
        val reflection = reflectionBrush?.invoke() ?: ShaderBrush(directionShader)
        // 光照不是跟着大菜单的整个高度拉长：平面内部不应出现一大片灰色腰带。
        val lightingHeight = size.height.coerceAtMost(64.dp.toPx())
        val bodyReflection = bodyReflectionBrush?.invoke() ?: Brush.verticalGradient(
            *Array(97) { index ->
                val depth = index / 96f
                depth to Color.Black.copy(alpha = glassBodyReflectionAlpha(depth))
            },
            endY = lightingHeight
        )
        // 圆形按钮的侧面暗边比上下重；等强度细线会丢掉玻璃转折处的厚度线索。
        val rim = Brush.verticalGradient(
            *Array(65) { index ->
                val depth = index / 64f
                val vertical = abs(2f * depth - 1f)
                depth to Color.Black.copy(
                    alpha = profile.rimTopAlpha +
                        (profile.rimSideAlpha - profile.rimTopAlpha) * (1f - vertical * vertical)
                )
            },
            endY = size.height
        )
        val blurRadius = 2.2.dp.toPx().coerceAtMost(size.minDimension / 8f)
        val padding = ceil(blurRadius * 3f)
        val glowSize = IntSize(
            ceil(size.width + padding * 2f).toInt(),
            ceil(size.height + padding * 2f).toInt()
        )
        val glow = obtainGraphicsLayer().apply {
            renderEffect = BlurEffect(
                radiusX = blurRadius,
                radiusY = blurRadius,
                edgeTreatment = TileMode.Decal
            )
            record(size = glowSize) {
                translate(padding, padding) {
                    drawPath(
                        innerPath,
                        reflection,
                        alpha = profile.glowAlpha,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }
        }
        onDrawWithContent {
            val alpha = lightingAlpha().coerceIn(0f, 1f)
            if (alpha > 0f && bodyReflectionEnabled) {
                clipPath(path) {
                    drawRect(bodyReflection, alpha = alpha)
                }
            }
            val pressAlpha = glassPressLightAlpha(pressProgress())
            if (pressAlpha > 0f) {
                // 只提亮材质，置于内容和轮廓高光下方；不用 Plus 叠白造成截白。
                clipPath(path) { drawRect(Color.White, alpha = pressAlpha) }
            }
            if (alpha > 0f) {
                clipPath(innerPath) {
                    // 普通透明合成保留灰阶余量，避免 Plus 在白底形成一条截白的内边界。
                    glow.alpha = alpha
                    translate(-padding, -padding) { drawLayer(glow) }
                    drawPath(
                        innerPath,
                        reflection,
                        alpha = alpha * profile.coreAlpha,
                        style = Stroke(2f * edgeWidth)
                    )
                }
            }
            // 只缓存光照，不缓存内容；文字、图标与它们原有的动画每帧正常绘制。
            drawContent()
            if (alpha > 0f) {
                clipPath(path) {
                    drawPath(
                        path,
                        rim,
                        alpha = alpha,
                        style = Stroke(2f * rimWidth)
                    )
                }
            }
        }
    }
}

/** 整体按压提亮的上限为 4%；独立于触点光斑和静态轮廓照明。 */
internal fun glassPressLightAlpha(progress: Float): Float =
    if (progress.isFinite()) 0.04f * progress.coerceIn(0f, 1f) else 0f

/** 连续的反射明暗曲线，没有分段线性渐变的折点或固定宽度描边的内边界。 */
internal fun glassBodyReflectionAlpha(depth: Float): Float {
    val offset = (depth - 0.27f) / 0.13f
    val square = offset * offset
    return 0.020f * exp(-square * square)
}

// 用圆角表面的方向收窄上下高光：45° 处只剩 1/16，垂直侧边归零。
// 相比仅看 y 坐标，这也能避免高菜单的左右直边被当成上/下边点亮。
private const val GlassDirectionShader = """
uniform float2 extent;
uniform float4 radii;
uniform float power;
uniform float headroom;
half4 main(float2 coordinate) {
    float2 halfExtent = extent * 0.5;
    float2 local = coordinate - halfExtent;
    float radius = local.y < 0.0
        ? (local.x < 0.0 ? radii.x : radii.y)
        : (local.x < 0.0 ? radii.w : radii.z);
    float2 corner = max(abs(local) - halfExtent + radius, 0.0);
    float magnitude = length(corner);
    float2 distanceToEdge = halfExtent - abs(local);
    float vertical = magnitude > 0.0001
        ? corner.y / magnitude
        : (distanceToEdge.y < distanceToEdge.x ? 1.0 : 0.0);
    half weight = half(pow(clamp(vertical, 0.0, 1.0), power));
    return half4(fromLinearSrgb(half3(headroom)) * weight, weight);
}
"""
