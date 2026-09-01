package com.aria.rythme.ui.component

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.ui.theme.rythmeColors

internal const val ANIM_DURATION = 300

@Composable
fun RythmeHeader(
    isShow: Boolean,
    routeKey: NavKey,
    config: TopBarConfig,
    isSearchActive: Boolean = false,
    searchTitle: String = "",
    onSearchClose: () -> Unit = {},
    skipAnimation: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    // 根据 searchTitle 是否为空判断页面有无标题，调整搜索框静止位置
    // 有标题：按钮行(68) + 标题行(36) = 104dp
    // 无标题：按钮行(68)，搜索框紧接其后
    val searchBarRestY = if (searchTitle.isNotEmpty()) 104.dp else 68.dp

    // isShow && !isSearchActive控制顶部按钮区域的显隐
    val headerAlpha by animateFloatAsState(
        targetValue = if (isShow && !isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "headerAlpha"
    )

    val searchTransitionState = remember {
        MutableTransitionState(isSearchActive)
    }.apply {
        targetState = isSearchActive
    }
    val searchTransition = rememberTransition(
        transitionState = searchTransitionState,
        label = "headerSearch"
    )

    val titleTranslationY by searchTransition.animateDp(
        transitionSpec = { tween(ANIM_DURATION) },
        label = "titleTranslationY"
    ) { active -> if (active) 0.dp else 68.dp }

    val titleAlpha by searchTransition.animateFloat(
        transitionSpec = { tween(ANIM_DURATION) },
        label = "titleAlpha"
    ) { active -> if (active) 0f else 1f }

    val searchBarTranslationY by searchTransition.animateDp(
        transitionSpec = { tween(ANIM_DURATION) },
        label = "searchBarTranslationY"
    ) { active -> if (active) 0.dp else searchBarRestY }

    val topBarHeight by searchTransition.animateDp(
        transitionSpec = { tween(ANIM_DURATION) },
        label = "topBarHeight"
    ) { active -> if (active) 68.dp else 172.dp }

    Box {
        // 渐变背景，固定不变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.rythmeColors.surface.copy(0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Header高度固定
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(topBarHeight)
        ) {

            // 顶部按钮区域，高度固定，在折叠时快速渐隐
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 9.dp)
                    .alpha(headerAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(
                    visible = config.showBackButton,
                    skipAnimation = skipAnimation,
                    onClick = onBackClick
                )

                Spacer(modifier = Modifier.weight(1f))

                AnimatedHeaderActions(
                    actions = config.actions,
                    skipAnimation = skipAnimation
                )
            }

            // 搜索框区域：激活时立即显示，退出时等动画完成再移除
            val showSearchContent =
                searchTransitionState.currentState || searchTransitionState.targetState
            if (showSearchContent) {
                if (searchTitle.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 21.dp, end = 21.dp, top = titleTranslationY)
                            .fillMaxWidth()
                            .height(36.dp)
                            .alpha(titleAlpha)
                    ) {
                        Text(
                            text = searchTitle,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.rythmeColors.textColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(start = 21.dp, top = searchBarTranslationY)
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    HeaderSearchBar(
                        active = isSearchActive,
                        onClose = onSearchClose,
                    )
                }
            }
        }
    }
}
