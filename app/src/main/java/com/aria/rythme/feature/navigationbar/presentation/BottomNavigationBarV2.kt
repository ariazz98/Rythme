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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.feature.player.presentation.PlayerIntent
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.ui.component.MiniPlayer
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import org.koin.androidx.compose.koinViewModel

private val BarHeight = 64.dp
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

    // true = 主tab展开(胶囊)，action收起(圆形)
    // false = 主tab收起(圆形)，action展开(胶囊)
    var mainTabExpanded by remember { mutableStateOf(true) }

    // 动画进度：1f = 主tab展开，0f = action展开
    val expandFraction by animateFloatAsState(
        targetValue = if (mainTabExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
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
            modifier = modifier,
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            // 总可用宽度（去掉间距和圆形最小尺寸）
            val totalWidthDp = with(density) { constraints.maxWidth.toDp() }
            // 圆形尺寸 = 高度
            val circleSize = BarHeight
            // 胶囊最大宽度 = 总宽度 - 间距 - 圆形尺寸
            val capsuleMaxWidth = totalWidthDp - GapWidth - circleSize

            // 整体高度：主tab展开时64dp，action展开时56dp
            val currentHeight = lerp(BarHeight.value - 14f, BarHeight.value, expandFraction).dp

            // 主tab宽度：从圆形到胶囊
            val mainTabWidth = lerp(currentHeight.value, (totalWidthDp - GapWidth - currentHeight).value, expandFraction).dp
            // action宽度：从胶囊到圆形（与主tab互补）
            val actionWidth = lerp((totalWidthDp - GapWidth - currentHeight).value, currentHeight.value, expandFraction).dp

            Row(
                horizontalArrangement = Arrangement.spacedBy(GapWidth),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 主Tab区域
                Row(
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
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            if (!mainTabExpanded) mainTabExpanded = true
                        }
                        .padding(4f.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (expandFraction > 0.3f) {
                        // 展开时显示所有tab
                        navTabs.forEach {
                            BottomTab(
                                icon = it.value.icon,
                                title = it.value.title,
                                tint = MaterialTheme.rythmeColors.primary,
                                onClick = {}
                            )
                        }
                    } else {
                        // 收起成圆形时只显示第一个tab的图标
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(navTabs.first().value.icon),
                                contentDescription = "",
                                tint = MaterialTheme.rythmeColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Action区域
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

@Composable
fun RowScope.BottomTab(
    icon: Int,
    title: Int,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "",
            tint = tint,
            modifier = Modifier
                .size(24.dp)
        )
        Text(
            text = stringResource(title),
            color = tint,
            fontSize = 10.sp
        )
    }
}