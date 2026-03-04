package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import org.koin.androidx.compose.koinViewModel

private val BarHeight = 64.dp
private val CollapsedHeight = 50.dp
private val GapWidth = 8.dp

@Composable
fun CustomBottomBar(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    onClickPlayer: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val navTabs = remember {
        TOP_LEVEL_DESTINATIONS.entries.toList().filter { it.key != RythmeRoute.Search }
    }
    val searchItem = remember {
        TOP_LEVEL_DESTINATIONS[RythmeRoute.Search]!!
    }

    val containerColor = MaterialTheme.rythmeColors.bottomBackground

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var mainTabExpanded by remember { mutableStateOf(true) }

    // 容器尺寸动画（较慢）
    val expandFraction by animateFloatAsState(
        targetValue = if (mainTabExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 600f),
        label = "expandFraction"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))))
            .navigationBarsPadding()
            .padding(start = 21.dp, end = 21.dp, bottom = 8.dp)
    ) {
        BoxWithConstraints(
            modifier = modifier.height(BarHeight),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            // 总宽度
            val totalWidthDp = with(density) { constraints.maxWidth.toDp() }

            // 当前高度
            val currentHeight = lerp(CollapsedHeight.value, BarHeight.value, expandFraction).dp

            //主tab宽度
            val mainTabWidth = lerp(
                currentHeight.value,
                (totalWidthDp - GapWidth - currentHeight).value,
                expandFraction
            ).dp
            //action宽度
            val actionWidth = lerp(
                (totalWidthDp - GapWidth - currentHeight).value,
                currentHeight.value,
                expandFraction
            ).dp

            // 展开态胶囊固定宽度（不随动画变化）
            val expandedCapsuleWidth = totalWidthDp - GapWidth - BarHeight

            // 像素值（用于 graphicsLayer 计算）
            val mainTabWidthPx = with(density) { mainTabWidth.toPx() }
            val expandedCapsuleWidthPx = with(density) { expandedCapsuleWidth.toPx() }
            val currentHeightPx = with(density) { currentHeight.toPx() }
            val iconSizePx = with(density) { 24.dp.toPx() }
            val paddingPx = with(density) { 4.dp.toPx() }

            // 选中icon位移动画
            val tabContentWidthPx = expandedCapsuleWidthPx - paddingPx * 2
            val collapsedCenterXPx = with(density) { CollapsedHeight.toPx() } / 2f
            // 当前选中tab到收起中心的距离
            val selectedNaturalXPx = paddingPx + tabContentWidthPx * (selectedTabIndex + 0.5f) / navTabs.size

            val iconFraction by animateFloatAsState(
                targetValue = if (mainTabExpanded) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 600f),
                label = "iconFraction"
            )

            // 仅当完全展开且动画结束时使用tab内icon，其余时刻用浮层
            val showTabIcon = mainTabExpanded && expandFraction > 0.99f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GapWidth),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 主Tab容器
                Box(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                vibrancy()
                                blur(radius = 4f.dp.toPx())
                                lens(
                                    refractionHeight = 24f.dp.toPx(),
                                    refractionAmount = 32f.dp.toPx()
                                )
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .width(mainTabWidth)
                        .height(currentHeight)
                        .clipToBounds()
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            if (!mainTabExpanded) mainTabExpanded = true
                        }
                ) {
                    // Layer 1: 等比缩放内容（选中icon在此层隐藏）
                    val contentScale = if (expandedCapsuleWidthPx > 0f) {
                        (mainTabWidthPx / expandedCapsuleWidthPx).coerceIn(0f, 1f)
                    } else 1f

                    Row(
                        modifier = Modifier
                            .width(expandedCapsuleWidth)
                            .height(BarHeight)
                            .graphicsLayer {
                                scaleX = contentScale
                                scaleY = contentScale
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = expandFraction
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        navTabs.forEachIndexed { index, entry ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = null,
                                        indication = null
                                    ) { selectedTabIndex = index },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(entry.value.icon),
                                    contentDescription = "",
                                    tint = MaterialTheme.rythmeColors.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            // 动画期间隐藏选中tab的icon，由浮层接管
                                            if (index == selectedTabIndex && !showTabIcon) alpha = 0f
                                        }
                                )
                                Text(
                                    text = stringResource(entry.value.title),
                                    color = MaterialTheme.rythmeColors.primary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Layer 2: 选中icon浮层（固定速度移动到最终收起位置）

                    // 展开态icon自然Y：Row padding(4dp) + Column居中偏移 + icon半高
                    // Column内容高度 = BarHeight - 8dp, 内含 Icon(24dp) + Text(~12dp) ≈ 36dp
                    val barHeightPx = with(density) { BarHeight.toPx() }
                    val textApproxPx = with(density) { 12.dp.toPx() }
                    val columnContentPx = barHeightPx - paddingPx * 2
                    val innerContentPx = iconSizePx + textApproxPx
                    val expandedIconCenterYPx =
                        paddingPx + (columnContentPx - innerContentPx) / 2f + iconSizePx / 2f
                    // 收起态icon自然Y：容器垂直中心
                    val collapsedIconCenterYPx = currentHeightPx / 2f

                    Icon(
                        painter = painterResource(navTabs[selectedTabIndex].value.icon),
                        contentDescription = "",
                        tint = MaterialTheme.rythmeColors.primary,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = if (showTabIcon) 0f else 1f
                                translationX = lerp(collapsedCenterXPx, selectedNaturalXPx, iconFraction) - iconSizePx / 2f
                                translationY = lerp(collapsedIconCenterYPx, expandedIconCenterYPx, iconFraction) - iconSizePx / 2f
                            }
                            .size(24.dp)
                    )
                }

                // Action容器
                Box(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                vibrancy()
                                blur(radius = 4f.dp.toPx())
                                lens(
                                    refractionHeight = 24f.dp.toPx(),
                                    refractionAmount = 32f.dp.toPx()
                                )
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .width(actionWidth)
                        .height(currentHeight)
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            if (mainTabExpanded) mainTabExpanded = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(searchItem.icon),
                        contentDescription = "",
                        tint = MaterialTheme.rythmeColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
