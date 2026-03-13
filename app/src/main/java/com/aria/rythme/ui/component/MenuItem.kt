package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.ui.theme.AvatarDefaultBgEnd
import com.aria.rythme.ui.theme.AvatarDefaultBgStart
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.ui.component.utils.InteractiveHighlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch

/** 按压缩放弹簧参数，与 LiquidBottomTabs 一致 */
private val PressAnimSpec = spring<Float>(1f, 1000f, 0.001f)

/**
 * 右侧操作按钮区域，动画由 [HeaderActionsAnimState] 统一编排：
 *
 * - 整体进出场（actions 空↔非空）：虚化渐显/渐隐
 * - 更多按钮显示/隐藏：水滴弹性滑入/滑出
 * - 内容变更（actions 非空→非空）：虚化交叉过渡，容器宽度平滑变化
 */
@Composable
fun AnimatedHeaderActions(
    showMoreButton: Boolean,
    actions: List<Action>,
    routeKey: NavKey,
    skipAnimation: Boolean = false,
    onMoreClick: () -> Unit = {},
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val coroutineScope = rememberCoroutineScope()
    val animState = remember { HeaderActionsAnimState(coroutineScope) }
    val distance = with(LocalDensity.current) { 60.dp.toPx() }

    // 初始化（仅首次）
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animState.initialize(actions, showMoreButton)
        initialized = true
    }

    // 输入变化时统一驱动动画
    LaunchedEffect(actions, showMoreButton) {
        if (!initialized) return@LaunchedEffect
        animState.update(actions, showMoreButton, skipAnimation, distance)
    }

    // 同步 lambda 引用（不触发动画）
    SideEffect {
        animState.syncActionRefs(actions)
    }

    // 未进入可见阶段，不渲染
    if (animState.phase == HeaderActionsAnimState.Phase.Hidden) return

    val containerColor = MaterialTheme.rythmeColors.bottomBackground

    Box(
        modifier = Modifier
            .height(68.dp)
            .graphicsLayer {
                alpha = animState.overallAlpha.value
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .thenBlur(animState.overallBlur.value),
        contentAlignment = Alignment.Center
    ) {
        // ---- 更多按钮（底层，独立定位，用 padding 控制间距） ----
        if (animState.showMore || showMoreButton) {
            val pressAnim = remember { Animatable(0f) }
            val moreHighlight = remember(coroutineScope) { InteractiveHighlight(coroutineScope) }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 60.dp)
                    .size(68.dp)
                    .thenBlur(animState.moreDroplet.blur),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            shadow = { Shadow.Default.copy(radius = 12.dp, offset = DpOffset(0.dp, 0.dp)) },
                            effects = {
                                vibrancy()
                                blur(2f.dp.toPx())
                                lens(24f.dp.toPx(), 32f.dp.toPx())
                            },
                            layerBlock = {
                                translationX = animState.moreDroplet.offsetX
                                val baseScaleX = animState.moreDroplet.scaleX
                                val baseScaleY = animState.moreDroplet.scaleY
                                val press = pressAnim.value
                                val pressScale = 1f + press * 8f.dp.toPx() / size.width
                                scaleX = baseScaleX * pressScale
                                scaleY = baseScaleY * pressScale
                            },
                            onDrawSurface = {
                                drawRect(color = containerColor)
                            }
                        )
                        .then(moreHighlight.modifier)
                        .size(44.dp)
                        .then(moreHighlight.gestureModifier)
                        .pointerInput(coroutineScope) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                coroutineScope.launch { pressAnim.animateTo(1f, PressAnimSpec) }
                                waitForUpOrCancellation()
                                coroutineScope.launch { pressAnim.animateTo(0f, PressAnimSpec) }
                            }
                        }
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) { onMoreClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "更多",
                        tint = MaterialTheme.rythmeColors.textColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ---- 自适应操作按钮容器（顶层，决定容器宽度） ----
        if (animState.displayActions.isNotEmpty()) {
            val sharedTransitionScope = LocalSharedTransitionScope.current
            val isActionMenuOverlay = LocalOverlayMenu.current.currentMenu is OverlayMenu.ActionMenu
            val isAvatarOnly = animState.displayActions.all { it is Action.Avatar }
            val pressAnim = remember { Animatable(0f) }
            val actionHighlight = remember(coroutineScope) { InteractiveHighlight(coroutineScope) }

            with(sharedTransitionScope) {
                AnimatedVisibility(
                    visible = !isActionMenuOverlay,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .height(68.dp)
                            .padding(horizontal = 12.dp)
                            .thenBlur(animState.contentBlur.value),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAvatarOnly) {
                            // 头像模式：无 backdrop，clip 圆形，无按压放大
                            Row(
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "actionMenuBounds"),
                                        animatedVisibilityScope = this@AnimatedVisibility
                                    )
                                    .graphicsLayer {
                                        val bulge = animState.heightBulge.value
                                        scaleY = 1f + bulge * 6f.dp.toPx() / size.height
                                    }
                                    .height(44.dp)
                                    .animateContentSize(tween(ANIM_DURATION / 2)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                animState.displayActions.forEach { action ->
                                    ActionItem(action, routeKey)
                                }
                            }
                        } else {
                            // 图标模式：backdrop 毛玻璃 + 容器整体按压放大
                            Row(
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "actionMenuBounds"),
                                        animatedVisibilityScope = this@AnimatedVisibility
                                    )
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { ContinuousCapsule },
                                        shadow = { Shadow.Default.copy(radius = 12.dp, offset = DpOffset(0.dp, 0.dp)) },
                                        effects = {
                                            vibrancy()
                                            blur(2f.dp.toPx())
                                            lens(24f.dp.toPx(), 32f.dp.toPx())
                                        },
                                        layerBlock = {
                                            val press = pressAnim.value
                                            val pressScale = 1f + press * 8f.dp.toPx() / size.width
                                            val bulge = animState.heightBulge.value
                                            val bulgeScaleY = 1f + bulge * 6f.dp.toPx() / size.height
                                            scaleX = pressScale
                                            scaleY = pressScale * bulgeScaleY
                                        },
                                        onDrawSurface = {
                                            drawRect(color = containerColor)
                                        }
                                    )
                                    .then(actionHighlight.modifier)
                                    .height(44.dp)
                                    .animateContentSize(tween(ANIM_DURATION / 2))
                                    .then(actionHighlight.gestureModifier)
                                    .pointerInput(coroutineScope) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            coroutineScope.launch { pressAnim.animateTo(1f, PressAnimSpec) }
                                            waitForUpOrCancellation()
                                            coroutineScope.launch { pressAnim.animateTo(0f, PressAnimSpec) }
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                animState.displayActions.forEach { action ->
                                    ActionItem(action, routeKey)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionItem(action: Action, routeKey: NavKey) {
    val topBarState = LocalTopBarState.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = null,
                indication = null
            ) { topBarState.getActionHandler(routeKey, action.key)?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        when (action) {
            is Action.Icon -> {
                Icon(
                    painter = painterResource(action.iconRes),
                    contentDescription = action.contentDescription,
                    tint = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.size(action.iconSize)
                )
            }
            is Action.Avatar -> {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            verticalGradient(
                                colors = listOf(AvatarDefaultBgStart, AvatarDefaultBgEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!action.url.isNullOrEmpty()) {
                        AsyncImage(
                            model = action.url,
                            contentDescription = "avatar",
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = if (action.name.isNullOrEmpty()) "R" else action.name.take(2),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---- 返回按钮（独立样式，不属于统一组件） ----

@Composable
fun BackButton(
    backdrop: Backdrop = LocalBackdrop.current,
    visible: Boolean,
    skipAnimation: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val coroutineScope = rememberCoroutineScope()
    val pressAnim = remember { Animatable(0f) }
    val backHighlight = remember(coroutineScope) { InteractiveHighlight(coroutineScope) }

    val blur by animateFloatAsState(
        targetValue = if (visible) 0f else 10f,
        animationSpec = if (skipAnimation) tween(0) else tween(ANIM_DURATION),
        label = "backBlur"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (skipAnimation) tween(0) else tween(ANIM_DURATION),
        label = "backAlpha"
    )

    if (alpha > 0.001f) {
        Box(modifier = Modifier
            .size(68.dp)
            .alpha(alpha)
            .thenBlur(blur),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        shadow = { Shadow.Default.copy(radius = 12.dp, offset = DpOffset(0.dp, 0.dp)) },
                        effects = {
                            vibrancy()
                            blur(2f.dp.toPx())
                            lens(24f.dp.toPx(), 32f.dp.toPx())
                        },
                        layerBlock = {
                            val press = pressAnim.value
                            val pressScale = 1f + press * 8f.dp.toPx() / size.width
                            scaleX = pressScale
                            scaleY = pressScale
                        },
                        onDrawSurface = {
                            drawRect(color = containerColor)
                        }
                    )
                    .then(backHighlight.modifier)
                    .size(44.dp)
                    .then(backHighlight.gestureModifier)
                    .pointerInput(coroutineScope) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            coroutineScope.launch { pressAnim.animateTo(1f, PressAnimSpec) }
                            waitForUpOrCancellation()
                            coroutineScope.launch { pressAnim.animateTo(0f, PressAnimSpec) }
                        }
                    }
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "返回",
                    tint = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.size(20.dp)
                )
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

    Box(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(24f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(color = containerColor)
                }
            )
            .size(44.dp)
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "返回",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SharedTransitionScope.MenuPanel(
    backdrop: Backdrop = LocalBackdrop.current,
    scope: AnimatedVisibilityScope,
    configs: List<MenuConfig>,
    interactive: Boolean = true
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
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }

    // 高光：跟随手指位置，半径限制为面板宽度的一半
    val highlight = remember(coroutineScope) {
        InteractiveHighlight(
            animationScope = coroutineScope,
            radius = { size -> size.width * 2 / 3 }
        )
    }

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

    Column(
        modifier = Modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(key = "actionMenuBounds"),
                animatedVisibilityScope = scope,
                boundsTransform = BoundsTransform { _, _ ->
                    spring(dampingRatio = 0.55f, stiffness = 250f)
                }
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(48.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                    lens(24f.dp.toPx(), 32f.dp.toPx())
                },
                layerBlock = {
                    val dx = dragOffsetX.value
                    val dy = dragOffsetY.value

                    // 阻尼位移（左和下有位移，右和上只形变无位移）
                    // maxShift = 最大位移量，refDist = 阻尼参考距离（越大起步越线性）
                    val maxShift = 6.dp.toPx()
                    val refDist = 100.dp.toPx()
                    fun damp(v: Float) = maxShift * v / (refDist + kotlin.math.abs(v))
                    translationX = if (dx < 0f) damp(dx) else 0f
                    translationY = if (dy > 0f) damp(dy) else 0f

                    // 方向性形变
                    val deform = 0.03f
                    val ny = (dy / size.height).coerceIn(-1f, 1f) * deform
                    // 左拖正常响应，右拖大阻力（轻微效果）
                    val dxDamped = if (dx > 0f) dx * 0.15f else dx
                    val nx = (dxDamped / size.width).coerceIn(-1f, 1f) * deform
                    // 下拉：窄+高，上拉：宽+矮，左拉：宽+矮
                    scaleX = 1f - ny - nx
                    scaleY = 1f + ny + nx
                },
                onDrawSurface = {
                    drawRect(color = containerColor)
                }
            )
            .width(256.dp)
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
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break

                        // 拖拽形变
                        coroutineScope.launch {
                            launch { dragOffsetX.snapTo(change.position.x - startPos.x) }
                            launch { dragOffsetY.snapTo(change.position.y - startPos.y) }
                        }

                        // 胶囊选择
                        val newIndex = itemInfos.indexOfFirst {
                            change.position.y >= it.yPx && change.position.y < it.yPx + it.heightPx
                        }
                        if (newIndex >= 0 && newIndex != activeIndex) {
                            activeIndex = newIndex
                            coroutineScope.launch {
                                capsuleY.animateTo(
                                    itemInfos[newIndex].yPx,
                                    spring(dampingRatio = 0.8f, stiffness = 600f)
                                )
                            }
                        }
                    }

                    // 松开：触发选中项的点击，形变回弹
                    if (activeIndex >= 0) {
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
                            .padding(vertical = 12.dp, horizontal = 24.dp)
                            .fillMaxWidth()
                    )
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
        if (config.isCheckMode) {
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
            Box(
                modifier = Modifier
                    .size(60.dp),
                contentAlignment = Alignment.Center
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
        val isCheckMode: Boolean = false,
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
