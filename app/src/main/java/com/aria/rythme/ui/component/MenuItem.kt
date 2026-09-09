package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.ui.component.utils.InteractiveHighlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch

/** 按压缩放弹簧参数，与 LiquidBottomTabs 一致 */
private val PressAnimSpec = spring(1f, 1000f, 0.001f)

// 头像单独按参考截图的 sRGB 转换结果校准，不更改全局玻璃颜色。
internal val HeaderAvatarTop = Color(0xFFADC8E8)
internal val HeaderAvatarBottom = Color(0xFF737CB8)

/**
 * 面板锚点位置，决定拖拽形变的方向响应。
 *
 * 锚点所在方向只产生形变（弹性），远离锚点方向产生位移+形变。
 */
enum class PanelAnchor {
    /** 锚点在右上角（ActionMenu 默认，向左下拖拽有位移） */
    TopEnd,
    /** 锚点在右下角（向左上拖拽有位移） */
    BottomEnd,
}

/** 每组有自己的实际边界和共享元素身份；独立操作与头像按实际宽度排列。 */
@Composable
fun AnimatedHeaderActions(
    sourceKey: Any,
    auxiliaryActions: List<Action> = emptyList(),
    actions: List<Action>,
    referenceScale: Float = 1f,
    skipAnimation: Boolean = false,
    navigationProgress: () -> Float = { 1f },
    enabled: Boolean = true,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val scope = rememberCoroutineScope()
    val auxiliaryPress = remember(scope) { TopBarPressState(scope) }
    val actionPress = remember(scope) { TopBarPressState(scope) }
    val target = headerActionLayout(auxiliaryActions, actions)
    val signature = auxiliaryActions.contentKey() to actions.contentKey()
    val transition = remember { HeaderActionTransitionState(target, sourceKey, signature) }
    val pending = transition.source != sourceKey || transition.signature != signature
    val navigationVisibility = navigationProgress()
    val navigationEnded = navigationVisibility >= 0.999f
    val navProgress = HeaderActionMotion.navigationPhase(navigationVisibility)
    val tail = remember { Animatable(0f) }
    // 主段跟随真实 entry；只在页面完成后接上本地尾巴，不向 NavDisplay 注册额外退出动画。
    LaunchedEffect(sourceKey, signature, skipAnimation, navigationEnded, transition.morph) {
        tail.snapTo(0f)
        if (!skipAnimation && navigationEnded && !pending && transition.morph != null) {
            tail.animateTo(1f, tween(HeaderActionMotion.TailMillis, easing = LinearEasing))
        }
    }
    // 同样的视觉布局只更新回调；切 Tab 不继承上一段尚未完成的形变。
    val snap = skipAnimation || (pending && transition.signature == signature)
    val progress = transition.progress(navProgress + if (navigationEnded) (1f - navProgress) * tail.value else 0f)
    val animating = !snap && if (pending) !navigationEnded else transition.morph != null && progress < 0.999f
    val frame = when {
        !animating -> target
        pending -> transition.lastFrame
        else -> transition.morph!!.frame(progress)
    }
    SideEffect {
        transition.lastFrame = frame
        if (pending || (skipAnimation && transition.morph != null)) {
            transition.retarget(target, sourceKey, signature, navProgress, snap || navigationEnded)
        } else if (!animating && transition.morph != null) {
            // 旧 entry 之后开始离场时，不能倒放已经结束的上一段导航。
            transition.finish(target)
        }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val menuSource = LocalOverlayMenu.current.presentedAction?.sourceKey
    val sourceOwnedByMenu = menuSource == (sourceKey to "auxiliary") || menuSource == (sourceKey to "actions")
    val connected = !animating && !sourceOwnedByMenu && auxiliaryActions.isNotEmpty() && actions.isNotEmpty() &&
        auxiliaryPress.canConnect && actionPress.canConnect &&
        !auxiliaryPress.bounds.isEmpty && !actionPress.bounds.isEmpty &&
        (kotlin.math.abs(auxiliaryPress.amount.value) > 0.001f || kotlin.math.abs(actionPress.amount.value) > 0.001f ||
            auxiliaryPress.light.value > 0.001f || actionPress.light.value > 0.001f) &&
        kotlin.math.abs(auxiliaryPress.bounds.center.y - actionPress.bounds.center.y) < 1f
    Box(Modifier.width((frame.width * referenceScale).dp).height(68.dp)
        .onGloballyPositioned { origin = it.boundsInRoot().topLeft }) {
        if (connected) TopBarConnectedGlass(auxiliaryPress, actionPress, origin, backdrop, referenceScale)
        Row(Modifier.align(Alignment.CenterEnd).requiredWidth((target.width * referenceScale).dp)
            .graphicsLayer { alpha = if (animating) 0f else 1f }, verticalAlignment = Alignment.CenterVertically) {
            HeaderActionGroup(sourceKey to "auxiliary", auxiliaryActions, referenceScale, enabled && !animating, backdrop, auxiliaryPress, connected)
            HeaderActionGroup(sourceKey to "actions", actions, referenceScale, enabled && !animating, backdrop, actionPress, connected)
        }
        if (animating) NavigationHeaderActions(frame, backdrop, referenceScale)
    }
}

@Composable
private fun HeaderActionGroup(
    sourceKey: Any,
    actions: List<Action>,
    referenceScale: Float,
    enabled: Boolean,
    backdrop: Backdrop,
    press: TopBarPressState,
    connected: Boolean
) {
    if (actions.isEmpty()) {
        SideEffect { press.canConnect = false }
        return
    }
    val overlay = LocalOverlayMenu.current
    val menu = overlay.presentedAction
    val sourceHidden = menu?.sourceKey == sourceKey
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    var anchorBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val avatar = actions.singleOrNull() as? Action.Avatar
    val pressHighlight = rememberTopBarPressHighlight()
    val hdr = LocalGlassHdr.current
    TopBarPressHeadroom(press, avatar != null)
    val canConnect = !sourceHidden && enabled
    SideEffect {
        press.avatar = avatar != null
        press.nominalWidth = TopBarComponentMetrics.surfaceWidth(actions.size)
        press.canConnect = canConnect
    }
    LaunchedEffect(sourceKey, enabled) { press.release() }
    val itemWidth = (TopBarComponentMetrics.touchWidth(actions.size) * referenceScale).dp
    val pressLayer: GraphicsLayerScope.() -> Unit = {
        val scale = TopBarPressMotion.scale(TopBarComponentMetrics.surfaceWidth(actions.size), avatar != null, press.amount.value)
        scaleX = scale
        scaleY = scale
    }

    // 菜单打开时仍保留组的占位，避免旁边的独立按钮/头像移位。
    Box(
        Modifier
            .height(68.dp)
            .width(((TopBarComponentMetrics.surfaceWidth(actions.size) + TopBarComponentMetrics.GroupGap) * referenceScale).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.graphicsLayer { alpha = if (sourceHidden) 0f else 1f }) {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned {
                            anchorBounds = it.boundsInRoot()
                            press.bounds = anchorBounds
                        }
,
                    contentAlignment = Alignment.Center
                ) {
                    if (!connected) GlassBackdropSurface(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        hdr = true,
                        pressHdr = true,
                        rimHeadroomLimit = if (hdr.darkTheme) 7f else GlassHdrHeadroom,
                        rimHeadroom = { if (avatar != null) maxOf(hdr.staticHeadroom, topBarAvatarGain(press.light.value, hdr.darkTheme)) else hdr.staticHeadroom },
                        effects = {
                            vibrancy()
                            blur(2.dp.toPx())
                            glassLens(24.dp.toPx(), 32.dp.toPx())
                        },
                        layerBlock = pressLayer,
                        bodyReflectionBrush = { topBarPressBodyReflection(press.light.value) },
                        onDrawSurface = {
                            drawRect(containerColor)
                            // 先照亮完整玻璃容器，再绘制不透明内盘，外圈与内盘各自增亮，不叠白。
                            drawRect(pressHighlight.single(size, press), blendMode = BlendMode.Plus)
                            if (avatar != null) {
                                // 内盘四周仍能采样页面背景，不是纯色圆盘描边。
                                // 图片成功加载后覆盖内盘；加载中/失败时仍保留原有渐变底。
                                drawCircle(
                                    brush = verticalGradient(topBarAvatarColors(press.light.value, hdr.pressAvailable, hdr.darkTheme)),
                                    radius = (size.minDimension / 2f - (TopBarComponentMetrics.AvatarInset * referenceScale).dp.toPx()).coerceAtLeast(0f)
                                )
                            }
                        }
                    )
                    Row(
                        modifier = Modifier
                            .graphicsLayer(pressLayer)
                            .clip(ContinuousCapsule)
                            .height((TopBarComponentMetrics.SurfaceHeight * referenceScale).dp)
                            .topBarPress(press, actions.map { it.key }, enabled && !sourceHidden),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions.forEachIndexed { index, action ->
                            val menuProvider = (action as? Action.Icon)?.menu
                            ActionItem(
                                action,
                                width = itemWidth,
                                referenceScale = referenceScale,
                                opticalOffset = (TopBarComponentMetrics.iconOffset(actions.size, index) * referenceScale).dp,
                                press = press,
                                onClick = if (!enabled || sourceHidden) null else if (menuProvider != null) {
                                    {
                                        if (!anchorBounds.isEmpty) overlay.show(
                                            OverlayMenu.ActionMenu(sourceKey, anchorBounds, menuProvider(),
                                                actions.toList(), action.key, press.scale, referenceScale, backdrop)
                                        )
                                    }
                                } else action.onClick
                            )
                        }
                    }
                    }
        }
    }
}

@Composable
internal fun ActionItem(action: Action, width: Dp, referenceScale: Float, opticalOffset: Dp, press: TopBarPressState, onClick: (() -> Unit)? = action.onClick) {
    val pressed = press.pressedKey == action.key
    val dark = LocalGlassHdr.current.darkTheme
    val dimIcon = pressed && action is Action.Icon
    val iconAlpha by animateFloatAsState(
        targetValue = TopBarPressMotion.iconTargetAlpha(pressed, avatar = action is Action.Avatar, dark = dark),
        animationSpec = if (dimIcon) TopBarPressMotion.iconDown else TopBarPressMotion.iconUp,
        label = "topBarPressedIcon"
    )
    val clickModifier = onClick?.let { onClick ->
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        )
    } ?: Modifier

    Box(
        modifier = Modifier
            .width(width).height((TopBarComponentMetrics.SurfaceHeight * referenceScale).dp)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        // 透明合成层覆盖完整点击槽，给图标的光学偏移留余量；否则 More 右侧圆点会在淡化时被裁切。
        Box(Modifier.fillMaxSize().glassHdrFadeAndBlur(alpha = { iconAlpha })
            .then(if (action is Action.Icon) Modifier.topBarForegroundLight(press) else Modifier), contentAlignment = Alignment.Center) {
        when (action) {
            is Action.Icon -> if (action.iconRes == R.drawable.ic_more) {
                val color = if (action.isActive) MaterialTheme.rythmeColors.primary else MaterialTheme.rythmeColors.textColor
                // 只校准顶栏的三点：参考圆点直径 4pt、中心距 8pt；不改其他位置的 More 图标。
                Canvas(Modifier.offset(x = opticalOffset, y = (0.3f * referenceScale).dp).size((action.iconSize.value * referenceScale).dp)
                    .semantics { contentDescription = action.contentDescription }) {
                    val unit = size.minDimension / 22f
                    for (index in -1..1) drawCircle(color, 2.04f * unit, Offset(center.x + index * 7.875f * unit, center.y))
                }
            } else Icon(
                painter = painterResource(action.iconRes),
                contentDescription = action.contentDescription,
                tint = if (action.isActive) {
                    MaterialTheme.rythmeColors.primary
                } else {
                    MaterialTheme.rythmeColors.textColor
                },
                modifier = Modifier.offset(x = opticalOffset, y = (0.3f * referenceScale).dp).size(action.iconSize * referenceScale)
            )

            is Action.Avatar -> HeaderAvatarContent(action, referenceScale, Modifier.topBarAvatarLight(press))
        }
        }
    }
}

// ---- 返回按钮（独立样式，不属于统一组件） ----

@Composable
fun BackButton(
    backdrop: Backdrop = LocalBackdrop.current,
    visible: Boolean,
    referenceScale: Float = 1f,
    skipAnimation: Boolean = false,
    visibilityFraction: Float? = null,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val coroutineScope = rememberCoroutineScope()
    val press = remember(coroutineScope) { TopBarPressState(coroutineScope) }
    val pressHighlight = rememberTopBarPressHighlight()
    val hdr = LocalGlassHdr.current
    TopBarPressHeadroom(press, avatar = false)
    val iconAlpha by animateFloatAsState(
        targetValue = TopBarPressMotion.iconTargetAlpha(press.pressedKey != null, dark = hdr.darkTheme),
        animationSpec = if (press.pressedKey != null) TopBarPressMotion.iconDown else TopBarPressMotion.iconUp,
        label = "backPressedIcon"
    )

    val animatedBlur by animateFloatAsState(
        targetValue = if (visible) 0f else 10f,
        animationSpec = if (skipAnimation) tween(0) else tween(ANIM_DURATION),
        label = "backBlur"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (skipAnimation) tween(0) else tween(ANIM_DURATION),
        label = "backAlpha"
    )

    val alpha = visibilityFraction ?: animatedAlpha
    val blur = visibilityFraction?.let { 10f * (1f - it) } ?: animatedBlur
    val pressLayer: GraphicsLayerScope.() -> Unit = {
        val pressScale = TopBarPressMotion.scale(TopBarComponentMetrics.SurfaceHeight, false, press.amount.value)
        scaleX = pressScale
        scaleY = pressScale
    }
    if (alpha > 0.001f) {
        Box(modifier = Modifier
            .size(68.dp)
            .glassHdrFadeAndBlur(alpha = { alpha }, blurDp = { blur }),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size((TopBarComponentMetrics.SurfaceHeight * referenceScale).dp)) {
                GlassBackdropSurface(
                    backdrop = backdrop,
                    pressHdr = true,
                    rimHeadroom = { hdr.staticHeadroom },
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        glassLens(24f.dp.toPx(), 32f.dp.toPx())
                    },
                    layerBlock = pressLayer,
                    bodyReflectionBrush = { topBarPressBodyReflection(press.light.value) },
                    onDrawSurface = {
                        drawRect(containerColor)
                        drawRect(pressHighlight.single(size, press), blendMode = BlendMode.Plus)
                    }
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer(pressLayer)
                        .clip(ContinuousCapsule)
                        .size((TopBarComponentMetrics.SurfaceHeight * referenceScale).dp)
                        .topBarPress(press, listOf("back"), visible)
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxSize().glassHdrFadeAndBlur(alpha = { iconAlpha }).topBarForegroundLight(press), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "返回",
                        tint = MaterialTheme.rythmeColors.textColor,
                        modifier = Modifier.offset(x = (-1.5f * referenceScale).dp).size((18.8f * referenceScale).dp)
                    )
                    }
            }
            }
        }
    }
}

@Composable
fun CloseButton(
    backdrop: Backdrop = LocalBackdrop.current,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = PressAnimSpec,
        label = "closeGlassPress"
    )

    Box(
        modifier = Modifier
            .size(HeaderSearchLayout.surfaceHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        GlassBackdropSurface(
            backdrop = backdrop,
            shape = { ContinuousCapsule },
            effects = {
                vibrancy()
                blur(2f.dp.toPx())
                glassLens(24f.dp.toPx(), 32f.dp.toPx())
            },
            pressProgress = { pressProgress },
            onDrawSurface = { drawRect(containerColor) }
        )
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "返回",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(18.dp)
        )
    }
}


/**
 * 独立锚定菜单面板，支持通过 columnModifier 注入共享元素等 Modifier。
 */
@Composable
fun AnchoredMenuPanel(
    backdrop: Backdrop = LocalBackdrop.current,
    configs: List<MenuConfig>,
    interactive: Boolean = true,
    anchor: PanelAnchor = PanelAnchor.TopEnd,
    columnModifier: Modifier = Modifier
) {
    MenuPanelContent(
        backdrop = backdrop,
        configs = configs,
        interactive = interactive,
        anchor = anchor,
        columnModifier = columnModifier
    )
}

@Composable
fun rememberMenuPanelHeightPx(configs: List<MenuConfig>): Float {
    val density = LocalDensity.current
    return remember(configs) {
        with(density) {
            val padding = 12.dp.toPx() * 2
            padding + configs.sumOf { config ->
                when (config) {
                    is MenuConfig.Item -> 44.dp.toPx().toDouble()
                    is MenuConfig.Group -> 60.dp.toPx().toDouble()
                    is MenuConfig.Separator -> 25.dp.toPx().toDouble()
                }
            }.toFloat()
        }
    }
}

@Composable
internal fun MenuPanelContent(
    backdrop: Backdrop = LocalBackdrop.current,
    configs: List<MenuConfig>,
    interactive: Boolean = true,
    columnModifier: Modifier = Modifier,
    anchor: PanelAnchor = PanelAnchor.TopEnd,
    hdr: Boolean = true,
    panelWidth: Dp = 256.dp,
    panelShape: androidx.compose.ui.graphics.Shape = ContinuousRoundedRectangle(48.dp),
    drawSurface: Boolean = true,
    interaction: MenuPanelInteraction = rememberMenuPanelInteraction(),
    deformContent: Boolean = true,
    contentViewport: Modifier = Modifier
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 计算可选 Item 在 Column 内的 Y 偏移
    val itemHeightPx = with(density) { 44.dp.toPx() }
    val separatorHeightPx = with(density) { 25.dp.toPx() }
    val groupHeightPx = with(density) { 60.dp.toPx() }

    // 胶囊选择器状态
    val capsuleY = remember { Animatable(0f) }
    val capsuleAlpha = remember { Animatable(0f) }
    var activeIndex by remember { mutableStateOf(-1) }

    // 拖拽形变状态（原始像素偏移，layerBlock 中做阻尼映射）
    val dragOffsetX = interaction.dragX
    val dragOffsetY = interaction.dragY

    // 高光：跟随手指位置，半径限制为面板宽度的一半
    val highlight = interaction.highlight

    data class ItemInfo(val yPx: Float, val heightPx: Float, val onClick: () -> Unit)

    val itemInfos = remember(configs) {
        buildList {
            var y = 0f
            configs.forEach { config ->
                when (config) {
                    is MenuConfig.Item -> {
                        add(ItemInfo(y, itemHeightPx, config.onClick))
                        y += itemHeightPx
                    }
                    is MenuConfig.Group -> y += groupHeightPx
                    is MenuConfig.Separator -> y += separatorHeightPx
                }
            }
        }
    }

    val panelLayer: GraphicsLayerScope.() -> Unit = {
        val transform = menuPanelTransform(dragOffsetX.value, dragOffsetY.value, size, density.density, anchor)
        translationX = transform.x
        translationY = transform.y
        scaleX = transform.scaleX
        scaleY = transform.scaleY
    }
    Box(modifier = columnModifier.width(panelWidth)) {
        if (drawSurface) GlassBackdropSurface(
            backdrop = backdrop,
            shape = { panelShape },
            shadow = GlassMenuShadow,
            hdr = hdr,
            effects = {
                vibrancy()
                blur(12f.dp.toPx())
                glassLens(24f.dp.toPx(), 32f.dp.toPx())
            },
            layerBlock = panelLayer,
            pressProgress = { highlight.pressProgress },
            onDrawSurface = {
                drawRect(color = containerColor)
            }
        )
        Column(
            modifier = Modifier
                .then(if (deformContent) Modifier.graphicsLayer(panelLayer) else Modifier)
                .clip(panelShape)
                .width(panelWidth)
                // 只裁剪可滚动内容；外壳和投影不进入滚动视口。
                .then(contentViewport)
                // 径向高光（在 padding 之前，覆盖整个面板）
                .then(highlight.modifier)
                .then(if (interactive) highlight.gestureModifier else Modifier)
                .padding(12.dp)
                // 胶囊绘制
                .drawWithContent {
                    val alpha = capsuleAlpha.value
                    if (alpha > 0f) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.1f * alpha),
                            topLeft = Offset(0f, capsuleY.value),
                            size = Size(size.width, itemHeightPx),
                            cornerRadius = CornerRadius(itemHeightPx / 2)
                        )
                    }
                    drawContent()
                }
                // 胶囊手势：按下显示 → 拖拽跟随 → 松开触发点击
                .then(if (interactive) Modifier.pointerInput(itemInfos) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        fun hit(position: Offset): Int = if (position.x < 0f || position.x > size.width) -1 else
                            itemInfos.indexOfFirst { position.y >= it.yPx && position.y < it.yPx + it.heightPx }
                        val hitIndex = itemInfos.indexOfFirst {
                            down.position.y >= it.yPx && down.position.y < it.yPx + it.heightPx
                        }
                        if (hitIndex >= 0) {
                            activeIndex = hitIndex
                            coroutineScope.launch {
                                capsuleY.snapTo(itemInfos[hitIndex].yPx)
                                capsuleAlpha.snapTo(1f)
                            }
                        }

                        // 跟踪手指移动（胶囊选择 + 拖拽形变）
                        var released = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                released = true
                                activeIndex = hit(change.position)
                                break
                            }

                            // 拖拽形变
                            coroutineScope.launch {
                                launch { dragOffsetX.snapTo(change.position.x - startPos.x) }
                                launch { dragOffsetY.snapTo(change.position.y - startPos.y) }
                            }

                            // 胶囊选择
                            val newIndex = hit(change.position)
                            if (newIndex != activeIndex) {
                                activeIndex = newIndex
                                coroutineScope.launch {
                                    capsuleAlpha.snapTo(if (newIndex >= 0) 1f else 0f)
                                    if (newIndex >= 0) capsuleY.animateTo(
                                        itemInfos[newIndex].yPx,
                                        spring(dampingRatio = 0.8f, stiffness = 600f)
                                    )
                                }
                            }
                        }

                        // 松开：触发选中项的点击，形变回弹
                        if (released && activeIndex >= 0) {
                            itemInfos[activeIndex].onClick()
                        }
                        activeIndex = -1
                        coroutineScope.launch {
                            launch { capsuleAlpha.animateTo(0f, tween(150)) }
                            launch { dragOffsetX.animateTo(0f, spring(0.65f, 400f)) }
                            launch { dragOffsetY.animateTo(0f, spring(0.65f, 400f)) }
                        }
                    }
                } else Modifier)
        ) {
            configs.forEach { config ->
                when (config) {
                    is MenuConfig.Item -> MenuPanelItem(config)
                    is MenuConfig.Group -> MenuPanelRow(config)
                    is MenuConfig.Separator -> {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuPanelItem(
    config: MenuConfig.Item
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
        ) {
            if (config.isChecked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checked),
                    contentDescription = "checked",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.rythmeColors.textColor
                )

                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        if (config.iconRes != null) {
            Box(
                modifier = Modifier
                    .width(30.dp)
            ) {
                Icon(
                    painter = painterResource(config.iconRes),
                    contentDescription = "menu",
                    modifier = Modifier.size(config.iconSize),
                    tint = MaterialTheme.rythmeColors.textColor
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        Text(
            text = stringResource(config.titleRes),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            color = MaterialTheme.rythmeColors.textColor,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MenuPanelRow(
    config: MenuConfig.Group
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        config.items.forEach { item ->
            Column(
                modifier = Modifier
                    .size(60.dp)
                    .clip(ContinuousRoundedRectangle(12.dp))
                    .background(if (item.isChecked) MaterialTheme.rythmeColors.bottomSelected else Color.Transparent)
                    .clickable { item.onClick() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                if (item.iconRes != null) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = "menu",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.rythmeColors.textColor
                    )
                }

                Text(
                    text = stringResource(item.titleRes),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.rythmeColors.textColor
                )

            }
        }
    }
}

sealed class MenuConfig {
    data class Item(
        val isChecked: Boolean = false,
        val iconRes: Int?,
        val iconSize: Dp = 18.dp,
        val titleRes: Int,
        val onClick: () -> Unit
    ) : MenuConfig()
    data class Group(val items: List<Item>) : MenuConfig()

    data object Separator : MenuConfig()
}

// ---- 工具函数 ----

/** 当模糊半径足够大时才应用 blur，使用 Unbounded 避免矩形裁剪阴影 */
internal fun Modifier.thenBlur(radius: Float): Modifier =
    if (radius > 0.5f) this.blur(radius.dp, BlurredEdgeTreatment.Unbounded) else this
