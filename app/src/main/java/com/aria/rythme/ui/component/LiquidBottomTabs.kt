package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.feature.navigationbar.data.model.BottomNavItem
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.ui.component.utils.DampedDragAnimation
import com.aria.rythme.ui.component.utils.BottomTabMorphGeometry
import com.aria.rythme.ui.component.utils.BottomBarMetrics
import com.aria.rythme.ui.component.utils.bottomTabPressScale
import com.aria.rythme.ui.component.utils.bottomTabEmphasisScale
import com.aria.rythme.ui.component.utils.searchSurfaceMergeProgress
import com.aria.rythme.ui.component.utils.TAB_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.capsuleWidthDamping
import com.aria.rythme.ui.component.utils.InteractiveHighlight
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

// 当前已恢复暗边/高光、折射、色散和内部明暗；整体按压提亮由玻璃表面统一处理。
// 整片黑色着色、触点补光仍关闭，原参数留在调用处。
private const val TAB_DROPLET_TINT_AND_SPOTLIGHT_ENABLED = false

/**
 * 展开态 BottomBar：四个普通 Tab 共用一个外层胶囊。
 *
 * Search 与前三个 Tab 使用同一容器、同一选择器和同一套液态玻璃动效。
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    availableWidth: Dp,
    enabled: Boolean = true,
    backdrop: Backdrop = LocalBackdrop.current,
    modifier: Modifier = Modifier,
    expansionFraction: Float = 1f,
    targetExpanded: Boolean = true,
    lastPrimaryTabIndex: Int = 0
) {
    val expansion = expansionFraction.coerceIn(0f, 1f)
    val separateSearch = expansion < 1f
    val expandedInputEnabled = enabled && !separateSearch
    val primaryIndex = lastPrimaryTabIndex.coerceIn(0, 2)
    val selectorAlpha = ((expansion - 0.65f) / 0.35f).coerceIn(0f, 1f)
    val tabs = remember { TOP_LEVEL_DESTINATIONS.values.toList() }
    val tabsCount = tabs.size
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val selectedColor = MaterialTheme.rythmeColors.bottomSelected
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val hdr = LocalGlassHdr.current
    val glassGeneration = if (hdr.enabled) hdr.generation else 0
    // 只重建强调色的采样缓存，拖动、选择和回弹状态仍由下面的稳定节点持有。
    val tabsBackdrop = key(hdr.enabled, glassGeneration) { rememberLayerBackdrop() }

    // 宽度由外层提供，动画不再触发第二层 SubcomposeLayout；可变节点挂在稳定的 Box 下。
    Box(
        modifier = modifier
            .height(androidx.compose.ui.unit.lerp(BottomBarMetrics.CompactHeight.dp, BottomBarMetrics.ExpandedTabHeight.dp, expansion))
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val horizontalPadding = 4.dp
        val tabWidth = (availableWidth - horizontalPadding * 2) / tabsCount
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val totalWidthPx = with(density) { availableWidth.roundToPx().toFloat() }
        val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val compactDiameter = BottomBarMetrics.CompactHeight.dp
        val compactDiameterPx = with(density) { compactDiameter.toPx() }
        val morph = BottomTabMorphGeometry(
            width = totalWidthPx,
            height = with(density) {
                androidx.compose.ui.unit.lerp(BottomBarMetrics.CompactHeight.dp, BottomBarMetrics.ExpandedTabHeight.dp, expansion).toPx()
            },
            diameter = compactDiameterPx,
            padding = horizontalPaddingPx,
            expansion = expansion,
            isLtr = isLtr,
            labelHeight = with(density) { 14.dp.toPx() }
        )
        val expandedSearchWidth = availableWidth - horizontalPadding - tabWidth * 3
        val primaryTargetWidth = if (targetExpanded) availableWidth else compactDiameter
        val searchTargetWidth = if (targetExpanded) expandedSearchWidth else compactDiameter
        val primaryWidth by animateDpAsState(
            targetValue = primaryTargetWidth,
            animationSpec = spring(
                dampingRatio = capsuleWidthDamping(primaryTargetWidth.value, (availableWidth - compactDiameter).value),
                stiffness = TAB_MOTION_STIFFNESS
            ),
            label = "primaryTabWidthRebound"
        )
        val searchWidth by animateDpAsState(
            targetValue = searchTargetWidth,
            animationSpec = spring(
                dampingRatio = capsuleWidthDamping(searchTargetWidth.value, (expandedSearchWidth - compactDiameter).value),
                stiffness = TAB_MOTION_STIFFNESS
            ),
            label = "searchTabWidthRebound"
        )
        val fullWidthReboundScale = if (separateSearch) 1f else primaryWidth / availableWidth
        val primaryBounds = morph.primaryBounds(with(density) { primaryWidth.toPx() })
        val searchBounds = morph.searchBounds(with(density) { searchWidth.toPx() })
        // 收起起点仍立即分离；展开由实际覆盖范围消去内沿。短交接只用于方向反转，
        // 不给轮廓退出再附加一条延迟到终点的动画，也不重建图标/手势节点。
        val joiningSearch = remember { Animatable(0f) }
        LaunchedEffect(separateSearch, targetExpanded) {
            if (!separateSearch) joiningSearch.snapTo(0f)
            else joiningSearch.animateTo(if (targetExpanded) 1f else 0f, tween(50))
        }
        val searchSurfaceAlpha = 1f - joiningSearch.value * searchSurfaceMergeProgress(primaryBounds, searchBounds)
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember {
            mutableIntStateOf(selectedTabIndex().coerceIn(0, tabsCount - 1))
        }
        val offsetAnimation = remember { Animatable(0f) }
        val dragSession = remember { FloatArray(2) }

        val panelOffset = run {
            val widthPx = totalWidthPx.coerceAtLeast(1f)
            val fraction = (offsetAnimation.value / widthPx).fastCoerceIn(-1f, 1f)
            with(density) {
                4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }

        val dragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 74f / 56f,
                onDragStarted = {
                    dragSession[DRAG_START_VALUE] = targetValue
                    dragSession[DRAG_DISTANCE_PX] = 0f
                },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat(), animatePress = false)
                    currentOnTabSelected(targetIndex)
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    dragSession[DRAG_DISTANCE_PX] += dragAmount.x
                    updateValue(
                        (dragSession[DRAG_START_VALUE] +
                            dragSession[DRAG_DISTANCE_PX] / tabWidthPx *
                            if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                // 整体提亮放在可见/隐藏玻璃底面各一次，这里不再重复补光。
                surfaceAlpha = 0f,
                spotlightAlpha = if (TAB_DROPLET_TINT_AND_SPOTLIGHT_ENABLED) 0.04f else 0f,
                position = { size, _ ->
                    Offset(
                        if (isLtr) {
                            (dragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        } else {
                            size.width - (dragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        },
                        size.height / 2f
                    )
                }
            )
        }

        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex().coerceIn(0, tabsCount - 1) }
                .collectLatest { index ->
                    if (currentIndex != index) {
                        currentIndex = index
                        dragAnimation.animateToValue(index.toFloat())
                    }
                }
        }

        val selectTab: (Int) -> Unit = { index ->
            currentIndex = index
            dragAnimation.animateToValue(index.toFloat())
            currentOnTabSelected(index)
        }

        val selectTabOnDown: (Int) -> Unit = { index ->
            currentIndex = index
            dragAnimation.press()
            dragAnimation.animateToValue(index.toFloat(), animatePress = false)
            currentOnTabSelected(index)
        }

        val expandedPressScale = bottomTabPressScale(
            width = totalWidthPx,
            widthGrowth = with(density) { 16.dp.toPx() },
            progress = dragAnimation.pressProgress
        )
        val expandedLayer: GraphicsLayerScope.() -> Unit = {
            scaleX = expandedPressScale * fullWidthReboundScale
            scaleY = expandedPressScale
        }
        val dropletVelocity = dragAnimation.velocity / 10f
        val dropletScaleX = dragAnimation.scaleX /
            (1f - (dropletVelocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
        val dropletScaleY = dragAnimation.scaleY *
            (1f - (dropletVelocity * 0.25f).fastCoerceIn(-0.2f, 0.2f))
        val hiddenCapsuleHeightPx = with(density) { BottomBarMetrics.ExpandedSelectorHeight.dp.toPx() }
        // 以隐藏胶囊为参照，让液滴两端的外扩距离等于上下间距；计入实际非等比形变。
        val dropletEdgeOutset = if (separateSearch) 0f else morph.selectorEdgeOutset(
            capsuleWidth = totalWidthPx * expandedPressScale * fullWidthReboundScale,
            capsuleHeight = hiddenCapsuleHeightPx,
            selectorWidth = tabWidthPx * dropletScaleX,
            selectorHeight = hiddenCapsuleHeightPx * dropletScaleY
        ) * dragAnimation.pressProgress

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = panelOffset
                },
            contentAlignment = Alignment.CenterStart
        ) {
            if (!separateSearch) {
                LiquidTabContainerSurface(backdrop, containerColor,
                    pressProgress = { dragAnimation.pressProgress }, layerBlock = expandedLayer)
            }
            // 首个收起帧即切成两枚独立胶囊，不经过液桥或粘连轮廓。
            if (separateSearch) {
                SplitTabCapsule(
                    bounds = primaryBounds,
                    backdrop = backdrop,
                    pressProgress = { if (currentIndex == primaryIndex) dragAnimation.pressProgress else 0f }
                )
                SplitTabCapsule(
                    bounds = searchBounds,
                    backdrop = backdrop,
                    alpha = searchSurfaceAlpha,
                    pressProgress = { if (currentIndex == 3) dragAnimation.pressProgress else 0f }
                )
            }

            Box(Modifier.fillMaxSize().then(
                if (separateSearch) Modifier else Modifier.graphicsLayer(expandedLayer)
                    .clip(ContinuousCapsule).then(interactiveHighlight.modifier)
            )) {
                // 抵消背景的宽度回弹，不拉伸文字和图标；原来的整体按压反馈仍保留。
                BottomTabContents(
                    tabs = tabs,
                    morph = morph,
                    primaryIndex = primaryIndex,
                    currentIndex = currentIndex,
                    pressProgress = dragAnimation.pressProgress,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1f / fullWidthReboundScale },
                    itemModifier = { index ->
                        Modifier
                            .then(if (expandedInputEnabled) Modifier else Modifier.clearAndSetSemantics { })
                            .semantics {
                                role = Role.Tab
                                selected = index == currentIndex
                                onClick {
                                    if (expandedInputEnabled) selectTab(index)
                                    expandedInputEnabled
                                }
                            }
                            .pointerInput(index, expandedInputEnabled) {
                                if (!expandedInputEnabled) return@pointerInput
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    try {
                                        selectTabOnDown(index)
                                        waitForUpOrCancellation()
                                    } finally {
                                        dragAnimation.release()
                                    }
                                }
                            }
                    }
                )
            }
        }

        // 着色层仍只供选择器采样，但和可见层共用每帧的轮廓、图标位置及文字布局。
        key(hdr.enabled, glassGeneration) {
            Box(
                modifier = Modifier
                    .clearAndSetSemantics { }
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .fillMaxSize()
            ) {
                if (separateSearch) {
                    SplitTabCapsule(
                        bounds = primaryBounds,
                        backdrop = backdrop,
                        pressProgress = { if (currentIndex == primaryIndex) dragAnimation.pressProgress else 0f }
                    )
                    SplitTabCapsule(
                        bounds = searchBounds,
                        backdrop = backdrop,
                        alpha = searchSurfaceAlpha,
                        pressProgress = { if (currentIndex == 3) dragAnimation.pressProgress else 0f }
                    )
                } else {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth()
                            .height(BottomBarMetrics.ExpandedSelectorHeight.dp)
                    ) {
                        GlassBackdropSurface(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                val progress = dragAnimation.pressProgress
                                vibrancy()
                                blur(2.dp.toPx())
                                glassLens(
                                    24.dp.toPx() * progress,
                                    32.dp.toPx() * progress
                                )
                            },
                            lightingAlpha = { dragAnimation.pressProgress },
                            pressProgress = { dragAnimation.pressProgress },
                            // 隐藏壳体保持原高度，仅宽度跟随可见壳体的按压与回弹。
                            layerBlock = {
                                scaleX = expandedPressScale * fullWidthReboundScale
                                scaleY = 1f
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        Box(Modifier.matchParentSize().clip(ContinuousCapsule)
                            .then(interactiveHighlight.modifier))
                    }
                }
                BottomTabContents(
                    tabs = tabs,
                    morph = morph,
                    primaryIndex = primaryIndex,
                    currentIndex = currentIndex,
                    emphasized = true,
                    pressProgress = dragAnimation.pressProgress,
                    inheritedPressScale = if (separateSearch) 1f else expandedPressScale,
                    // 和可见内容共用最终位置变换，宽度回弹仍只改变壳体。
                    modifier = Modifier.fillMaxSize().then(
                        if (separateSearch) Modifier else Modifier
                            .graphicsLayer(expandedLayer)
                            .graphicsLayer { scaleX = 1f / fullWidthReboundScale }
                    )
                )
            }
        }

        // 在已校准的折射基线上恢复色散；按压放大、拖动形变和坐标映射不变。
        Box(
            modifier = Modifier
                .align(AbsoluteAlignment.CenterLeft)
                .graphicsLayer {
                    translationX = morph.selectorCenterX(dragAnimation.value, dropletEdgeOutset) -
                        tabWidthPx / 2f + panelOffset
                }
                .glassHdrFadeAndBlur(alpha = {
                    if (separateSearch && currentIndex == 3) 0f else selectorAlpha
                })
                .then(
                    if (expandedInputEnabled) {
                        interactiveHighlight.gestureModifier.then(dragAnimation.modifier)
                    } else {
                        Modifier
                    }
                )
                .width(tabWidth)
                .height(BottomBarMetrics.ExpandedSelectorHeight.dp)
        ) {
            LiquidTabSelectionSurface(
                backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop), selectedColor = selectedColor,
                pressProgress = { dragAnimation.pressProgress },
                scaleX = { dropletScaleX }, scaleY = { dropletScaleY }
            )
        }

        // 只给完全收起后的两个真实圆形分配点击区域，中间透明空隙不拦截 MiniPlayer。
        if (enabled && expansion < 0.001f) {
            listOf(primaryIndex, 3).forEach { index ->
                val center = morph.iconCenter(index)
                val label = stringResource(tabs[index].title)
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer { translationX = center.x - compactDiameterPx / 2f }
                        .size(compactDiameter)
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            contentDescription = label
                            selected = selectedTabIndex() == index
                            onClick { selectTab(index); true }
                        }
                        .pointerInput(index) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                try {
                                    selectTabOnDown(index)
                                    waitForUpOrCancellation()
                                } finally {
                                    dragAnimation.release()
                                }
                            }
                        }
                )
            }
        }
    }
}

private const val DRAG_START_VALUE = 0
private const val DRAG_DISTANCE_PX = 1

/** 两种着色复用同一布局，避免采样到停在终点的另一套图标。交互只挂在可见层。 */
@Composable
private fun BottomTabContents(
    tabs: List<BottomNavItem>,
    morph: BottomTabMorphGeometry,
    primaryIndex: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    pressProgress: Float = 0f,
    inheritedPressScale: Float = 1f,
    itemModifier: (Int) -> Modifier = { Modifier }
) {
    val density = LocalDensity.current
    val expansion = morph.expansion
    val separateSearch = expansion < 1f
    val contentAlpha = ((expansion - 0.5f) * 2f).coerceIn(0f, 1f)
    val contentBlur = 4f * (1f - contentAlpha)
    val colors = MaterialTheme.rythmeColors
    val selectorAlpha = ((expansion - 0.65f) / 0.35f).coerceIn(0f, 1f)
    val contentPress = pressProgress * if (separateSearch) selectorAlpha else 1f

    Box(modifier) {
        tabs.forEachIndexed { index, item ->
            key(index) {
                val sharedIcon = index == primaryIndex || index == 3
                // 过渡时保留图标直接持有选中色，不依赖正在淡出的选择器补足颜色。
                val selectedTint = emphasized || (separateSearch && currentIndex == index)
                // 合并时选择器正在淡入，底层仍可见；两种着色必须同步放大，避免形成双轮廓。
                val scaleWithSelection = emphasized ||
                    (separateSearch && currentIndex != 3 && index == currentIndex)
                // 图标和标题作为整体围绕共同中心放大；位置先跟随可见层，尺寸不重复叠乘。
                // 分离合并期间保留原倍率和两种着色的共同变换。
                val tabScale = if (scaleWithSelection) {
                    bottomTabEmphasisScale(
                        contentPress,
                        if (emphasized && !separateSearch) inheritedPressScale else 1f
                    )
                } else 1f
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .width(with(density) { morph.tabWidth.toDp() })
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = morph.iconCenter(index).x - size.width / 2f
                            // 保留原强调色内容在按压时的放大，中心跟随当前 Tab。
                            scaleX = tabScale
                            scaleY = tabScale
                        }
                        .then(itemModifier(index))
                ) {
                    TabContent(
                        item = item,
                        selected = selectedTint,
                        hideIcon = sharedIcon,
                        modifier = Modifier.fillMaxSize().clip(ContinuousCapsule).graphicsLayer {
                            alpha = if (separateSearch && index == 3) 0f else contentAlpha
                        }.thenBlur(contentBlur)
                    )
                    // 主 Tab 与 Search 的图标贯穿展开/收起，两层连同淡入文字都使用相同坐标。
                    if (sharedIcon) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = null,
                            tint = if (selectedTint) colors.primary else colors.textColor,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(24.dp)
                                .graphicsLayer { translationY = morph.iconCenter(index).y - size.height / 2f }
                        )
                    }
                    if (separateSearch && index == 3) {
                        Text(
                            text = stringResource(item.title),
                            color = if (emphasized || currentIndex == 3) colors.primary else colors.textColor,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .clearAndSetSemantics { }
                                .graphicsLayer {
                                    translationY = morph.height / 2f + 5.dp.toPx()
                                    alpha = contentAlpha
                                }
                                .thenBlur(contentBlur)
                        )
                    }
                }
            }
        }
    }
}

/** 各自拥有完整玻璃轮廓；图标由上方共享节点绘制，不在此复制。 */
@Composable
private fun SplitTabCapsule(
    bounds: Rect,
    backdrop: Backdrop,
    alpha: Float = 1f,
    pressProgress: () -> Float = { 0f }
) {
    val density = LocalDensity.current
    val background = MaterialTheme.rythmeColors.bottomBackground
    Box(
        Modifier
            .graphicsLayer {
                translationX = bounds.left
                translationY = bounds.top
            }
            .wrapContentSize(align = AbsoluteAlignment.TopLeft, unbounded = true)
            .width(with(density) { bounds.width.toDp() })
            .height(with(density) { bounds.height.toDp() })
            .glassHdrFadeAndBlur(alpha = { alpha })
    ) {
        GlassBackdropSurface(
            backdrop = backdrop,
            shape = { ContinuousCapsule },
            effects = {
                vibrancy()
                blur(2.dp.toPx())
                glassLens(24.dp.toPx(), 32.dp.toPx())
            },
            pressProgress = pressProgress,
            // 独立胶囊只保留统一玻璃底色，选中态由图标颜色表达。
            onDrawSurface = { drawRect(background) }
        )
    }
}

@Composable
private fun TabContent(
    item: BottomNavItem,
    selected: Boolean,
    hideIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) {
        MaterialTheme.rythmeColors.primary
    } else {
        MaterialTheme.rythmeColors.textColor
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = stringResource(item.title),
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    alpha = if (hideIcon) 0f else 1f
                }
        )
        Text(
            text = stringResource(item.title),
            color = contentColor,
            fontSize = 10.sp
        )
    }
}
