package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.utils.DampedDragAnimation
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop = LocalBackdrop.current
) {
    var searchText by remember { mutableStateOf("") }
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val selectColor = MaterialTheme.rythmeColors.bottomSelected

    // 3-tab 路由列表（不含 Search）
    val navTabs = remember {
        TOP_LEVEL_DESTINATIONS.entries.toList().filter { it.key != RythmeRoute.Search }
    }
    val searchItem = remember {
        TOP_LEVEL_DESTINATIONS[RythmeRoute.Search]!!
    }

    val tabsCount = navTabs.size

    val tabsBackdrop = rememberLayerBackdrop()

    var mainTabExpanded by remember { mutableStateOf(true) }
    // 容器尺寸动画（较慢）
    val expandFraction by animateFloatAsState(
        targetValue = if (mainTabExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 600f),
        label = "expandFraction"
    )

    /** 按压进度 [0, 1]，驱动 layerBlock 中的缩放系数 */
    val actionPressAnimation = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current

        // 总宽度
        val totalWidthDp = with(density) { constraints.maxWidth.toDp() }
        // 当前高度
        val currentHeight = lerp(50.dp, 64.dp, expandFraction)
        //主tab宽度
        val mainTabWidth = lerp(
            50.dp,
            totalWidthDp - 72.dp,
            expandFraction
        )

        //action宽度
        val actionWidth = lerp(
            (totalWidthDp - 58.dp),
            64.dp,
            expandFraction
        )

        val actionIconSize = lerp(
            18.dp,
            24.dp,
            expandFraction
        )

        val actionIconPadding = lerp(
            16.dp,
            20.dp,
            expandFraction
        )

        // 展开态胶囊固定宽度（不随动画变化）
        val expandedCapsuleWidth = totalWidthDp - 72.dp

        val tabWidth = (expandedCapsuleWidth - 8.dp) / tabsCount

        // 像素值（用于 graphicsLayer 计算）
        // 主tab宽度动态值
        val mainTabWidthPx = with(density) { mainTabWidth.toPx() }
        // 主tab宽度展开值
        val expandedCapsuleWidthPx = with(density) { expandedCapsuleWidth.toPx() }
        // 当前高度动态值
        val currentHeightPx = with(density) { currentHeight.toPx() }
        // icon大小
        val iconSizePx = with(density) { 24.dp.toPx() }
        // padding
        val paddingPx = with(density) { 4.dp.toPx() }

        // 选中icon位移动画
        val tabContentWidthPx = expandedCapsuleWidthPx - paddingPx * 2

        val tabWidthPx = tabContentWidthPx / tabsCount

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex().coerceIn(0, tabsCount - 1))
        }

        val contentScale = if (expandedCapsuleWidthPx > 0f) {
            (mainTabWidthPx / expandedCapsuleWidthPx).coerceIn(0f, 1f)
        } else 1f

        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex =
                        targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
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
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    // index=3(search)时不更新主tab选中状态
                    if (index in 0 until tabsCount) {
                        currentIndex = index
                    }
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        enabled = !mainTabExpanded
                    ) {
                        // 收起态点击主tab胶囊 → 展开并恢复之前的主tab
                        mainTabExpanded = true
                        onTabSelected(currentIndex)
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(24f.dp.toPx(), 24f.dp.toPx())
                            },
                            layerBlock = {
                                val progress = dampedDragAnimation.pressProgress
                                val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(currentHeight)
                        .width(mainTabWidth),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Row 内容：非选中icon+文字 缩放渐隐到左上角
                    Row(
                        Modifier
                            .width(expandedCapsuleWidth)
                            .height(64.dp)
                            .graphicsLayer {
                                scaleX = contentScale
                                scaleY = contentScale
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = expandFraction
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navTabs.forEachIndexed { index, entry ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = null,
                                        indication = null
                                    ) { currentIndex = index },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(entry.value.icon),
                                    contentDescription = "",
                                    tint = MaterialTheme.rythmeColors.textColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            // 选中icon始终隐藏，由浮层接管
                                            if (index == currentIndex) alpha = 0f
                                        }
                                )
                                Text(
                                    text = stringResource(entry.value.title),
                                    color = MaterialTheme.rythmeColors.textColor,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Layer 2: 选中icon浮层 — 位移动画（共享元素效果）
                    val collapsedIconLeftPx = with(density) { 13.dp.toPx() }
                    val expandedIconLeftPx = paddingPx + tabWidthPx * (currentIndex + 0.5f) - iconSizePx / 2f
                    // icon中心目标Y约25dp（展开态Column居中偏上，收起态capsule居中）
                    val targetIconCenterYPx = with(density) { 25.dp.toPx() }

                    Icon(
                        painter = painterResource(navTabs[currentIndex].value.icon),
                        contentDescription = "",
                        tint = MaterialTheme.rythmeColors.textColor,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                translationX = lerp(
                                    collapsedIconLeftPx,
                                    expandedIconLeftPx,
                                    expandFraction
                                )
                                translationY = targetIconCenterYPx - currentHeightPx / 2f
                            }
                    )
                }

                if (mainTabExpanded && expandFraction > 0.99f) {
                    val tabScale = lerp(1f, 1.2f, dampedDragAnimation.pressProgress)

                    Row(
                        Modifier
                            .clearAndSetSemantics {}
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .graphicsLayer {
                                translationX = panelOffset
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { ContinuousCapsule },
                                effects = {
                                    val progress = dampedDragAnimation.pressProgress
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                    lens(
                                        24f.dp.toPx() * progress,
                                        24f.dp.toPx() * progress
                                    )
                                },
                                highlight = {
                                    val progress = dampedDragAnimation.pressProgress
                                    Highlight.Default.copy(alpha = progress)
                                },
                                onDrawSurface = { drawRect(containerColor) }
                            )
                            .then(interactiveHighlight.modifier)
                            .height(56f.dp)
                            .width(mainTabWidth)
                            .padding(horizontal = 4f.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navTabs.forEachIndexed { index, entry ->
                            Column(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .clickable(
                                        interactionSource = null,
                                        indication = null
                                    ) { currentIndex = index }
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = tabScale
                                        scaleY = tabScale
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(entry.value.icon),
                                    contentDescription = "",
                                    tint = MaterialTheme.rythmeColors.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                                Text(
                                    text = stringResource(entry.value.title),
                                    color = MaterialTheme.rythmeColors.primary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx(), 24f.dp.toPx())
                        },
                        layerBlock = {
                            val progress = actionPressAnimation.value
                            val scale = lerp(1f, 1f + 8f.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .pointerInput(animationScope) {
                        awaitEachGesture {
                            // requireUnconsumed = false：接受已被 clickable 消费的 DOWN 事件，
                            // 确保动画在手指按下时立即触发，而非被 clickable 拦截后丢失
                            awaitFirstDown(requireUnconsumed = false)
                            animationScope.launch { actionPressAnimation.animateTo(1f, dampedDragAnimation.pressProgressAnimationSpec) }

                            // 等待手指抬起或手势取消，无论哪种情况都恢复到 0
                            waitForUpOrCancellation()
                            animationScope.launch { actionPressAnimation.animateTo(0f, dampedDragAnimation.pressProgressAnimationSpec) }
                        }
                    }
                    .width(actionWidth)
                    .height(currentHeight)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            if (mainTabExpanded) {
                                // 展开态点击action → 收起主tab，选中search(index=3)
                                mainTabExpanded = false
                                onTabSelected(tabsCount)
                            }
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(actionIconPadding))

                Icon(
                    painter = painterResource(searchItem.icon),
                    contentDescription = "",
                    tint = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier
                        .size(actionIconSize)
                )

                if (expandFraction < 0.8f) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // 输入框
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.rythmeColors.primary),
                        textStyle = TextStyle(
                            color = MaterialTheme.rythmeColors.subTitleColor,
                            fontSize = 16.sp
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchText.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_hint),
                                        color = MaterialTheme.rythmeColors.subTitleColor,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.requiredWidth(totalWidthDp - 144.dp).alpha(1f - expandFraction)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "",
                        tint = MaterialTheme.rythmeColors.textColor,
                        modifier = Modifier
                            .size(18.dp)
                            .alpha(1f - expandFraction)
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }

        if (mainTabExpanded  && expandFraction > 0.99f) {
            Box(
                Modifier
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer {
                        translationX =
                            if (isLtr) dampedDragAnimation.value * tabWidthPx + panelOffset
                            else size.width - (dampedDragAnimation.value + 1f) * tabWidthPx + panelOffset
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { ContinuousCapsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            lens(
                                10f.dp.toPx() * progress,
                                14f.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        shadow = {
                            val progress = dampedDragAnimation.pressProgress
                            Shadow(alpha = progress)
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = 8f.dp * progress,
                                alpha = progress
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(
                                selectColor,
                                alpha = 1f - progress
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        }
                    )
                    .height(56f.dp)
                    .width(tabWidth)
            )
        }
    }
}
