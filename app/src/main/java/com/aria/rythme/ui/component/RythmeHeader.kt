package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.ui.theme.rythmeColors
import kotlinx.coroutines.delay

internal const val ANIM_DURATION = 300

@Composable
fun RythmeHeader(
    isShow: Boolean,
    config: TopBarConfig,
    isSearchActive: Boolean = false,
    searchTitle: String = "",
    onSearchClose: () -> Unit = {},
    skipAnimation: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    // 搜索激活时强制显示 Header
    val headerAlpha by animateFloatAsState(
        targetValue = if (isShow || isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "headerAlpha"
    )

    // 折叠状态：搜索激活后延迟触发，先让用户看到标题+按钮+搜索栏完整布局
    var isCollapsing by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            isCollapsing = false
            delay(100)
            isCollapsing = true
        } else {
            isCollapsing = false
        }
    }

    // 折叠进度
    val collapseProgress by animateFloatAsState(
        targetValue = if (isCollapsing) 1f else 0f,
        animationSpec = tween(durationMillis = ANIM_DURATION),
        label = "collapseProgress"
    )

    // 搜索栏是否可见（激活时显示，关闭后等折叠动画结束再隐藏）
    val searchBarVisible = isSearchActive || collapseProgress > 0f

//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(120.dp)
//            .statusBarsPadding()
//    ) {
//
//        // 渐变背景，固定不变
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(64.dp)
//                .background(
//                    Brush.verticalGradient(
//                        listOf(
//                            MaterialTheme.rythmeColors.surface.copy(0.5f),
//                            Color.Transparent
//                        )
//                    )
//                )
//        )
//
//        Column {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(60.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                BackButton(
//                    visible = config.showBackButton,
//                    skipAnimation = skipAnimation,
//                    onClick = onBackClick
//                )
//
//                Spacer(modifier = Modifier.weight(1f))
//
//                AnimatedHeaderActions(
//                    showMoreButton = config.showMoreButton,
//                    actions = config.actions,
//                    skipAnimation = skipAnimation
//                )
//            }
//
//            // 搜索栏 — 仅搜索可见时挂载（顶层，接收触摸事件）
//            if (searchBarVisible) {
//                HeaderSearchBar(
//                    active = isCollapsing,
//                    onClose = onSearchClose,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                )
//            }
//
//        }
//    }

    // 内容区域 — Box 布局，按钮行与搜索栏叠放，高度恒定 = statusBar + 8dp + 44dp + 8dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 9.dp)
            .alpha(
                headerAlpha * if (searchBarVisible)
                    (1f - collapseProgress) else 1f
            )
    ) {
        // 按钮行 — 始终挂载，折叠时渐隐（底层，被搜索栏覆盖）
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(
                visible = config.showBackButton,
                skipAnimation = skipAnimation,
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.weight(1f))

            AnimatedHeaderActions(
                showMoreButton = config.showMoreButton,
                actions = config.actions,
                skipAnimation = skipAnimation
            )
        }

        Column {
            Spacer(modifier = Modifier.height(19.dp))
            Text(
                text = searchTitle,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.rythmeColors.textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 搜索栏 — 仅搜索可见时挂载（顶层，接收触摸事件）
        if (searchBarVisible) {
            HeaderSearchBar(
                active = isCollapsing,
                onClose = onSearchClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(
                        headerAlpha * collapseProgress.coerceAtLeast(
                            if (isSearchActive) 1f else 0f
                        )
                    )
            )
        }
    }

    // 搜索标题 — 用 layout 修改器报告 0 高度，溢出绘制在 Box 下方
    // 这样不影响 TopBar 测量高度，但视觉上显示在 44dp 行下方
    if (searchBarVisible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 21.dp)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    // 报告 0 高度，不影响 TopBar 测量
                    layout(placeable.width, 0) {
                        // 放置在 8dp(顶部padding) + 44dp(按钮行) 之后
                        val yOffset = (8.dp + 44.dp).roundToPx()
                        placeable.place(0, yOffset)
                    }
                }
        ) {
            AnimatedVisibility(
                visible = isSearchActive && !isCollapsing,
                enter = EnterTransition.None,
                exit = shrinkVertically(tween(250), shrinkTowards = Alignment.Top)
                        + fadeOut(tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(19.dp))
                    Text(
                        text = searchTitle,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.rythmeColors.textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
