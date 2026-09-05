package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.feature.navigationbar.data.model.BottomNavItem
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.ui.component.utils.DampedDragAnimation
import com.aria.rythme.ui.component.utils.BottomTabMorphGeometry
import com.aria.rythme.ui.component.utils.TAB_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.capsuleWidthDamping
import com.aria.rythme.ui.component.utils.InteractiveHighlight
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * 展开态 BottomBar：四个普通 Tab 共用一个外层胶囊。
 *
 * Search 与前三个 Tab 使用同一容器、同一选择器和同一套液态玻璃动效。
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
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
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier
            .height(androidx.compose.ui.unit.lerp(50.dp, 64.dp, expansion))
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val horizontalPadding = 4.dp
        val tabWidth = (maxWidth - horizontalPadding * 2) / tabsCount
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val totalWidthPx = constraints.maxWidth.toFloat()
        val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val compactDiameterPx = with(density) { 50.dp.toPx() }
        val morph = BottomTabMorphGeometry(
            width = totalWidthPx,
            height = with(density) { androidx.compose.ui.unit.lerp(50.dp, 64.dp, expansion).toPx() },
            diameter = compactDiameterPx,
            padding = horizontalPaddingPx,
            expansion = expansion,
            isLtr = isLtr
        )
        val expandedSearchWidth = maxWidth - horizontalPadding - tabWidth * 3
        val primaryTargetWidth = if (targetExpanded) maxWidth else 50.dp
        val searchTargetWidth = if (targetExpanded) expandedSearchWidth else 50.dp
        val primaryWidth by animateDpAsState(
            targetValue = primaryTargetWidth,
            animationSpec = spring(
                dampingRatio = capsuleWidthDamping(primaryTargetWidth.value, (maxWidth - 50.dp).value),
                stiffness = TAB_MOTION_STIFFNESS
            ),
            label = "primaryTabWidthRebound"
        )
        val searchWidth by animateDpAsState(
            targetValue = searchTargetWidth,
            animationSpec = spring(
                dampingRatio = capsuleWidthDamping(searchTargetWidth.value, (expandedSearchWidth - 50.dp).value),
                stiffness = TAB_MOTION_STIFFNESS
            ),
            label = "searchTabWidthRebound"
        )
        val fullWidthReboundScale = if (separateSearch) 1f else primaryWidth / maxWidth
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember {
            mutableIntStateOf(selectedTabIndex().coerceIn(0, tabsCount - 1))
        }
        val offsetAnimation = remember { Animatable(0f) }
        val dragSession = remember { FloatArray(2) }

        val panelOffset = run {
            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = panelOffset
                }
                .then(if (separateSearch) Modifier else Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur(2.dp.toPx())
                        lens(24.dp.toPx(), 32.dp.toPx())
                    },
                    layerBlock = {
                        val progress = dragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                        scaleX = scale * fullWidthReboundScale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                ).then(interactiveHighlight.modifier)),
            contentAlignment = Alignment.CenterStart
        ) {
            // 首个收起帧即切成两枚独立胶囊，不经过液桥或粘连轮廓。
            if (separateSearch) {
                SplitTabCapsule(
                    bounds = morph.primaryBounds(with(density) { primaryWidth.toPx() }),
                    backdrop = backdrop
                )
                SplitTabCapsule(
                    bounds = morph.searchBounds(with(density) { searchWidth.toPx() }),
                    backdrop = backdrop
                )
            }

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

        // 着色层仍只供选择器采样，但和可见层共用每帧的轮廓、图标位置及文字布局。
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
                    bounds = morph.primaryBounds(with(density) { primaryWidth.toPx() }),
                    backdrop = backdrop
                )
                SplitTabCapsule(
                    bounds = morph.searchBounds(with(density) { searchWidth.toPx() }),
                    backdrop = backdrop
                )
            } else {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                val progress = dragAnimation.pressProgress
                                vibrancy()
                                blur(2.dp.toPx())
                                lens(
                                    24.dp.toPx() * progress,
                                    32.dp.toPx() * progress
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = dragAnimation.pressProgress)
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(interactiveHighlight.modifier)
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
            BottomTabContents(
                tabs = tabs,
                morph = morph,
                primaryIndex = primaryIndex,
                currentIndex = currentIndex,
                emphasized = true,
                pressProgress = dragAnimation.pressProgress,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 液态选择器组合页面与强调色内容，按压和拖动时恢复原来的折射与形变。
        Box(
            modifier = Modifier
                .align(AbsoluteAlignment.CenterLeft)
                .graphicsLayer {
                    alpha = if (separateSearch && currentIndex == 3) 0f else selectorAlpha
                    translationX = morph.selectorCenterX(dragAnimation.value) - tabWidthPx / 2f + panelOffset
                }
                .then(
                    if (expandedInputEnabled) {
                        interactiveHighlight.gestureModifier.then(dragAnimation.modifier)
                    } else {
                        Modifier
                    }
                )
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dragAnimation.pressProgress
                        lens(
                            10.dp.toPx() * progress,
                            14.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = dragAnimation.pressProgress)
                    },
                    shadow = { Shadow(alpha = dragAnimation.pressProgress) },
                    innerShadow = {
                        InnerShadow(
                            radius = 8.dp * dragAnimation.pressProgress,
                            alpha = dragAnimation.pressProgress
                        )
                    },
                    layerBlock = {
                        scaleX = dragAnimation.scaleX
                        scaleY = dragAnimation.scaleY
                        val velocity = dragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dragAnimation.pressProgress
                        drawRect(selectedColor, alpha = 1f - progress)
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .width(tabWidth)
                .height(56.dp)
        )

        // 只给完全收起后的两个真实圆形分配点击区域，中间透明空隙不拦截 MiniPlayer。
        if (enabled && expansion < 0.001f) {
            listOf(primaryIndex, 3).forEach { index ->
                val center = morph.iconCenter(index)
                val label = stringResource(tabs[index].title)
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer { translationX = center.x - compactDiameterPx / 2f }
                        .size(50.dp)
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
    itemModifier: (Int) -> Modifier = { Modifier }
) {
    val density = LocalDensity.current
    val expansion = morph.expansion
    val separateSearch = expansion < 1f
    val contentAlpha = ((expansion - 0.5f) * 2f).coerceIn(0f, 1f)
    val colors = MaterialTheme.rythmeColors
    val selectorAlpha = ((expansion - 0.65f) / 0.35f).coerceIn(0f, 1f)
    val contentPress = pressProgress * if (separateSearch) selectorAlpha else 1f

    Box(modifier) {
        tabs.forEachIndexed { index, item ->
            key(index) {
                val sharedIcon = index == primaryIndex || index == 3
                // 合并时选择器正在淡入，底层仍可见；两种着色必须同步放大，避免形成双轮廓。
                val scaleWithSelection = emphasized ||
                    (separateSearch && currentIndex != 3 && index == currentIndex)
                val tabScale = if (scaleWithSelection) lerp(1f, 1.2f, contentPress) else 1f
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
                        selected = emphasized,
                        hideIcon = sharedIcon,
                        modifier = Modifier.fillMaxSize().clip(ContinuousCapsule).graphicsLayer {
                            alpha = if (separateSearch && index == 3) 0f else contentAlpha
                        }
                    )
                    // 主 Tab 与 Search 的图标贯穿展开/收起，两层连同淡入文字都使用相同坐标。
                    if (sharedIcon) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = null,
                            tint = if (emphasized) colors.primary else androidx.compose.ui.graphics.lerp(
                                colors.textColor,
                                colors.primary,
                                when {
                                    currentIndex != index -> 0f
                                    separateSearch && index == 3 -> 1f
                                    else -> 1f - expansion
                                }
                            ),
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
                                    translationY = 37.dp.toPx()
                                    alpha = contentAlpha
                                }
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
    backdrop: Backdrop
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
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(24.dp.toPx(), 32.dp.toPx())
                },
                // 独立胶囊只保留统一玻璃底色，选中态由图标颜色表达。
                onDrawSurface = { drawRect(background) }
            )
    )
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
