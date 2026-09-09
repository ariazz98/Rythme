package com.aria.rythme.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 一个显隐过程协调外壳、两套前景及输入。页面仅提供源按钮和菜单项。 */
@Composable
internal fun MorphingActionMenuOverlay(
    menu: OverlayMenu.ActionMenu,
    visible: Boolean,
    onDismiss: () -> Unit,
    onExitFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val sourceAlpha = remember { Animatable(1f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentBlur = remember { Animatable(6f) }
    val sourceScale = remember { Animatable(menu.sourceScale) }
    val closeProgress = remember { Animatable(0f) }
    var closeStart by remember { mutableStateOf<MenuMorphGeometry?>(null) }
    var closeSurfaceStart by remember { mutableFloatStateOf(0f) }
    val surfaceProgress by remember {
        derivedStateOf { if (closeStart == null) progress.value else closeSurfaceStart * (1f - closeProgress.value) }
    }
    val density = LocalDensity.current
    val referenceDensity = Density(density.density * menu.referenceScale, density.fontScale)
    val unit = referenceDensity.density
    val backdrop = menu.backdrop ?: LocalBackdrop.current
    val color = MaterialTheme.rythmeColors.bottomBackground
    val hdr = LocalGlassHdr.current
    val reflection = remember { RuntimeShader(MenuMorphSdf + MenuMorphReflection) }
    val contentLens = remember { RuntimeShader(MenuMorphSdf + MenuMorphContentLens) }
    val neutralPress = rememberTopBarMenuSourcePress()
    val interaction = rememberMenuPanelInteraction()
    val scrollState = rememberScrollState()
    val naturalHeight = (24f + menu.configs.sumOf {
        when (it) { is MenuConfig.Item -> 44; is MenuConfig.Group -> 60; MenuConfig.Separator -> 25 }
    }) * unit
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val safeTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value * density.density
        val safeBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value * density.density
        val target = ActionMenuMotion.targetBounds(menu.anchorBounds, ActionMenuMotion.Width * unit, naturalHeight,
            constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), safeTop, safeBottom, 12f * unit)
        val source = menu.anchorBounds.let {
            val half = Offset(it.width, it.height) * (sourceScale.value / 2f)
            Rect(it.center - half, it.center + half)
        }
        val inset = 48f * unit
        val canvas = Rect(min(target.left, menu.anchorBounds.left) - inset, min(target.top, menu.anchorBounds.top) - inset,
            max(target.right, menu.anchorBounds.right) + inset, max(target.bottom, menu.anchorBounds.bottom) + inset)
        val fold = ActionMenuMotion.openingFoldFraction(menu.sourceActions.size,
            menu.sourceActions.indexOfFirst { it.key == menu.pressedActionKey })
        val renderedGeometry = closeStart?.let {
            ActionMenuMotion.closeFromSnapshot(it, source, target, closeProgress.value,
                ActionMenuMotion.ClosingFoldFraction)
        } ?: ActionMenuMotion.geometry(source, target, progress.value, ActionMenuMotion.Corner * unit, fold)
        val geometry = renderedGeometry.translated(-canvas.topLeft)
        val currentGeometry by rememberUpdatedState(geometry)
        LaunchedEffect(visible) {
            if (!visible) {
                // 先保存当前轮廓与材质，再启动独立关闭时钟；第一帧不会换成完整菜单。
                closeStart = renderedGeometry
                closeSurfaceStart = progress.value
                closeProgress.snapTo(0f)
            }
            coroutineScope {
                launch { sourceScale.animateTo(1f, tween(120)) }
                launch { sourceAlpha.animateTo(if (visible) 0f else 1f,
                    if (visible) tween(25) else tween(100, delayMillis = 105)) }
                launch { contentAlpha.animateTo(if (visible) 1f else 0f,
                    if (visible) tween(100, delayMillis = 20) else tween(60)) }
                launch { contentBlur.animateTo(if (visible) 0f else 6f,
                    if (visible) tween(140, delayMillis = 20) else tween(60)) }
                if (visible) progress.animateTo(1f, tween(ActionMenuMotion.OpenMillis, easing = LinearEasing))
                else closeProgress.animateTo(1f, tween(ActionMenuMotion.CloseMillis, easing = LinearEasing))
            }
            if (!visible) onExitFinished()
        }
        val shapeState = remember {
            derivedStateOf {
                val path = currentGeometry.outline()
                object : Shape {
                    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(path)
                }
            }
        }
        val shape = shapeState.value
        // 外部的新一次点击关闭；菜单内起始后移出松手仍由面板自己取消，不当成外部点击。
        Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { if (visible) onDismiss() })
        val settled = progress.value >= .9999f && visible
        val panelLayer: GraphicsLayerScope.() -> Unit = {
            val transform = menuPanelTransform(interaction.dragX.value, interaction.dragY.value,
                target.size, unit, PanelAnchor.TopEnd)
            val amount = ActionMenuMotion.smooth(surfaceProgress, .65f, 1f)
            transformOrigin = TransformOrigin(
                (target.center.x - canvas.left) / canvas.width,
                (target.center.y - canvas.top) / canvas.height)
            translationX = transform.x * amount
            translationY = transform.y * amount
            scaleX = 1f + (transform.scaleX - 1f) * amount
            scaleY = 1f + (transform.scaleY - 1f) * amount
        }
        Box(Modifier.offset { IntOffset(canvas.left.roundToInt(), canvas.top.roundToInt()) }
            .requiredSize((canvas.width / density.density).dp, (canvas.height / density.density).dp)) {
            // 外壳从展开到稳定及拖动始终是同一个节点，不在 settled 时替换材质。
                val shadowMix = ActionMenuMotion.smooth(surfaceProgress, 0f, .55f)
                GlassBackdropSurface(
                    backdrop = backdrop,
                    layerBlock = panelLayer,
                    pressProgress = { interaction.highlight.pressProgress },
                    // Backdrop 的裁剪层必须直接观察轮廓状态；捕获本帧普通 Shape 会冻结初始裁剪。
                    shape = { shapeState.value },
                    shadow = GlassMenuShadow.copy(
                        radius = androidx.compose.ui.unit.lerp(GlassSurfaceShadow.radius, GlassMenuShadow.radius * menu.referenceScale, shadowMix),
                        offset = DpOffset(0.dp, androidx.compose.ui.unit.lerp(GlassSurfaceShadow.offset.y, GlassMenuShadow.offset.y * menu.referenceScale, shadowMix)),
                        color = androidx.compose.ui.graphics.lerp(GlassSurfaceShadow.color, GlassMenuShadow.color, shadowMix)
                    ),
                    effects = {
                        vibrancy()
                        blur((2f + 10f * ActionMenuMotion.smooth(surfaceProgress, 0f, .45f)) * unit)
                        val g = currentGeometry
                        padding = 0f
                        runtimeShaderEffect("actionMenuLens", MenuMorphSdf + MenuMorphLens, "content") {
                            setFloatUniform("body", g.body.left, g.body.top, g.body.right, g.body.bottom)
                            setFloatUniform("corners", g.topCorner, g.bodyCorner)
                            setFloatUniform("warp", g.skew, g.neck, g.headScale)
                            setFloatUniform("headDepth", g.headDepth)
                            setFloatUniform("lens", min(24f * unit, min(g.body.width, g.body.height) / 2f), 32f * unit)
                        }
                        padding = 0f
                    },
                    reflectionBrush = {
                        reflection.configure(currentGeometry)
                        reflection.setFloatUniform("headroom", if (hdr.enabled) hdr.staticHeadroom else 1f)
                        ShaderBrush(reflection)
                    },
                    bodyReflectionBrush = {
                        val body = currentGeometry.body
                        Brush.verticalGradient(*Array(97) { index ->
                            val depth = index / 96f
                            depth to Color.Black.copy(alpha = glassBodyReflectionAlpha(depth))
                        }, startY = body.top, endY = body.top + min(body.height, 64f * unit))
                    },
                    onDrawSurface = { drawRect(color) }
                )
                Box(Modifier.matchParentSize().graphicsLayer(panelLayer).clip(shape)
                    // 稳定时不把触点光斑关进透明离屏层，保留原有叠亮反馈。
                    .then(if (settled) Modifier else Modifier
                    .glassHdrFadeAndBlur(alpha = { contentAlpha.value }, blurDp = { contentBlur.value * menu.referenceScale })
                    .graphicsLayer {
                        contentLens.configure(geometry)
                        contentLens.setFloatUniform("destination", target.left-canvas.left,target.top-canvas.top,
                            target.right-canvas.left,target.bottom-canvas.top)
                        contentLens.setFloatUniform("lens", 24f * unit, 24f * unit * (1f - ActionMenuMotion.smooth(surfaceProgress, .30f, .85f)))
                        renderEffect = RenderEffect.createRuntimeShaderEffect(contentLens, "content").asComposeRenderEffect()
                    })) {
                    CompositionLocalProvider(LocalDensity provides referenceDensity) {
                        MenuPanelContent(backdrop, menu.configs, interactive = settled,
                            columnModifier = Modifier.offset {
                                IntOffset((target.left - canvas.left).roundToInt(), (target.top - canvas.top).roundToInt())
                            }, panelWidth = (target.width / unit).dp,
                            panelShape = RoundedCornerShape(ActionMenuMotion.Corner.dp), drawSurface = false,
                            interaction = interaction, deformContent = false,
                            contentViewport = if (naturalHeight > target.height + .5f)
                                Modifier.heightIn(max = (target.height / unit).dp).verticalScroll(scrollState)
                            else Modifier)
                    }
                }
                if (sourceAlpha.value > .001f) Box(Modifier.matchParentSize().clip(shape)) { Row(
                    Modifier.offset { IntOffset((menu.anchorBounds.left - canvas.left).roundToInt(), (menu.anchorBounds.top - canvas.top).roundToInt()) }
                        .requiredSize((menu.anchorBounds.width / density.density).dp, (menu.anchorBounds.height / density.density).dp)
                        .graphicsLayer { scaleX = sourceScale.value; scaleY = sourceScale.value }
                        .glassHdrFadeAndBlur(alpha = { sourceAlpha.value }, blurDp = {
                            if (closeStart != null) 4f*menu.referenceScale*(1f-ActionMenuMotion.smooth(closeProgress.value,.28f,.55f)) else 0f
                        }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    menu.sourceActions.forEachIndexed { index, action ->
                        val pressedAlpha = TopBarPressMotion.iconTargetAlpha(true, dark = hdr.darkTheme)
                        Box(Modifier.graphicsLayer {
                            alpha = if (visible && action.key == menu.pressedActionKey) pressedAlpha else 1f
                        }) {
                            ActionItem(action, (TopBarComponentMetrics.touchWidth(menu.sourceActions.size) * menu.referenceScale).dp,
                                menu.referenceScale, (TopBarComponentMetrics.iconOffset(menu.sourceActions.size, index) * menu.referenceScale).dp,
                                neutralPress, onClick = null)
                        }
                    }
                }
                }
            // 隐形矩形拦截仅覆盖真实菜单目标；过渡期间不提交不可见的菜单项。
            if (!settled) Box(Modifier.offset { IntOffset((target.left - canvas.left).roundToInt(), (target.top - canvas.top).roundToInt()) }
                .requiredSize((target.width / density.density).dp, (target.height / density.density).dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                    }
                })
        }
    }
}

@Composable
private fun rememberTopBarMenuSourcePress(): TopBarPressState {
    val scope = rememberCoroutineScope()
    return remember(scope) { TopBarPressState(scope) }
}

private fun RuntimeShader.configure(geometry: MenuMorphGeometry) {
    setFloatUniform("body", geometry.body.left, geometry.body.top, geometry.body.right, geometry.body.bottom)
    setFloatUniform("corners", geometry.topCorner, geometry.bodyCorner)
    setFloatUniform("warp", geometry.skew, geometry.neck, geometry.headScale)
    setFloatUniform("headDepth", geometry.headDepth)
}
