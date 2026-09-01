package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.feature.navigationbar.data.model.BottomNavItem
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.ui.component.utils.DampedDragAnimation
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
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
 * Search 与前三个 Tab 使用同一容器和同一选择器，只通过一条轻量分隔线保留视觉分组。
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    enabled: Boolean = true,
    backdrop: Backdrop = LocalBackdrop.current
) {
    val tabs = remember { TOP_LEVEL_DESTINATIONS.values.toList() }
    val tabsCount = tabs.size
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val selectedColor = MaterialTheme.rythmeColors.bottomSelected
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    BoxWithConstraints(
        modifier = Modifier
            .height(64.dp)
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
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember {
            mutableIntStateOf(selectedTabIndex().coerceIn(0, tabsCount - 1))
        }
        val offsetAnimation = remember { Animatable(0f) }

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
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    currentOnTabSelected(targetIndex)
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }

        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex().coerceIn(0, tabsCount - 1) }
                .collectLatest { index ->
                    if (currentIndex != index || dragAnimation.targetValue != index.toFloat()) {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur(2.dp.toPx())
                        lens(24.dp.toPx(), 32.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
        )

        // Search 仍属于同一胶囊；分隔线只表达视觉分组，不建立第二个容器。
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = horizontalPaddingPx + tabWidthPx * 3f
                }
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.rythmeColors.textColor.copy(alpha = 0.12f))
        )

        // 可见选择器在内容下方移动，内容本身保持稳定，不做整体缩放或拉伸。
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = horizontalPadding)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dragAnimation.value * tabWidthPx + panelOffset
                        else totalWidthPx - horizontalPaddingPx * 2f - tabWidthPx -
                            dragAnimation.value * tabWidthPx + panelOffset
                    scaleX = dragAnimation.scaleX
                    scaleY = dragAnimation.scaleY
                    val velocity = dragAnimation.velocity / 10f
                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                }
                .drawBackdrop(
                    backdrop = backdrop,
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
                    onDrawSurface = {
                        drawRect(selectedColor)
                    }
                )
                .width(tabWidth)
                .height(56.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, item ->
                TabContent(
                    item = item,
                    selected = index == currentIndex,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ContinuousCapsule)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = enabled
                        ) { selectTab(index) }
                )
            }
        }

        // 透明手势层只覆盖当前选择器，使左右拖动仍保留原有的 Tab 切换亮点。
        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = horizontalPadding)
                    .graphicsLayer {
                        translationX =
                            if (isLtr) dragAnimation.value * tabWidthPx + panelOffset
                            else totalWidthPx - horizontalPaddingPx * 2f - tabWidthPx -
                                dragAnimation.value * tabWidthPx + panelOffset
                    }
                    .then(dragAnimation.modifier)
                    .width(tabWidth)
                    .height(56.dp)
            )
        }
    }
}

/** 收起态的主 Tab / Search 独立按钮。 */
@Composable
fun CompactBottomTab(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current
) {
    val background = if (selected) {
        MaterialTheme.rythmeColors.bottomSelected
    } else {
        MaterialTheme.rythmeColors.bottomBackground
    }

    Box(
        modifier = modifier
            .size(50.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(20.dp.toPx(), 28.dp.toPx())
                },
                onDrawSurface = { drawRect(background) }
            )
            .clip(ContinuousCapsule)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = stringResource(item.title),
            tint = if (selected) MaterialTheme.rythmeColors.primary else MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TabContent(
    item: BottomNavItem,
    selected: Boolean,
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
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(item.title),
            color = contentColor,
            fontSize = 10.sp
        )
    }
}
