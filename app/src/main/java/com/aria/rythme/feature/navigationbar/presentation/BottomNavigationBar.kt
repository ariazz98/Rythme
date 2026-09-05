package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.ui.component.LiquidBottomTabs
import com.aria.rythme.ui.component.MiniPlayer
import com.aria.rythme.ui.component.utils.MINI_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.TAB_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.capsuleWidthDamping
import com.aria.rythme.ui.component.utils.miniPlayerPositionAnimationSpec
import org.koin.androidx.compose.koinViewModel

/**
 * iOS 27 结构的 BottomBar。
 *
 * 展开态：MiniPlayer 在上，四个普通 Tab 共用一个胶囊。
 * 收起态：最近主 Tab、MiniPlayer、Search 在同一行。
 */
@Composable
fun BottomNavigationBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    onClickPlayer: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val playerState by viewModel.state.collectAsStateWithLifecycle()
    val bottomBarState = LocalBottomBarState.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val playerVisible = LocalPlayerVisible.current
    val expandFraction by animateFloatAsState(
        targetValue = if (bottomBarState.isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = MINI_MOTION_STIFFNESS
        ),
        label = "bottomBarExpansion"
    )
    val miniPositionFraction by animateFloatAsState(
        targetValue = if (bottomBarState.isExpanded) 1f else 0f,
        animationSpec = miniPlayerPositionAnimationSpec(bottomBarState.isExpanded),
        label = "miniPlayerPosition"
    )
    // Tab 基础过渡用时约为最初的 70%，宽度回弹独立处理。
    val tabExpandFraction by animateFloatAsState(
        targetValue = if (bottomBarState.isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = TAB_MOTION_STIFFNESS
        ),
        label = "bottomTabExpansion"
    )
    val preparationProgress by animateFloatAsState(
        targetValue = bottomBarState.collapsePreparationProgress,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1000f),
        label = "bottomBarCollapsePreparation"
    )

    val selectTab: (Int) -> Unit = { index ->
        bottomBarState.onTabSelected(index)
        onTabSelected(index)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))))
            .navigationBarsPadding()
            .padding(start = 21.dp, end = 21.dp, bottom = 8.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(lerp(50.dp, 122.dp, expandFraction))
                .graphicsLayer {
                    // 折叠完成仍保留缩放及两侧留白；只有重新展开才恢复正常比例。
                    val scale = 1f - 0.05f * preparationProgress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        ) {
            val compactMiniWidth = (maxWidth - 116.dp).coerceAtLeast(120.dp)
            val targetMiniWidth = if (bottomBarState.isExpanded) maxWidth else compactMiniWidth
            // 宽度独立回弹；不把过冲进度传给高度、位置或下一首按钮。
            val miniPlayerWidth by animateDpAsState(
                targetValue = targetMiniWidth,
                animationSpec = spring(
                    dampingRatio = capsuleWidthDamping(
                        targetMiniWidth.value,
                        (maxWidth - compactMiniWidth).value
                    ),
                    stiffness = MINI_MOTION_STIFFNESS
                ),
                label = "miniPlayerWidthRebound"
            )
            val tabHeight = lerp(50.dp, 64.dp, tabExpandFraction)
            val barHeight = lerp(50.dp, 122.dp, expandFraction)
            // 收起开始时 Search 独立成胶囊；两枚图标连续移动到左右圆形。
            LiquidBottomTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = selectTab,
                expansionFraction = tabExpandFraction,
                targetExpanded = bottomBarState.isExpanded,
                lastPrimaryTabIndex = bottomBarState.lastPrimaryTabIndex,
                modifier = Modifier
                    // 两套动画速度不同，仍让 Tab 底边保持在 BottomBar 底部。
                    .offset(y = barHeight - tabHeight)
                    .fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 抵消父容器位移，纵向只跟随独立时间曲线；宽度继续独立回弹。
                    .offset(y = 72.dp * (expandFraction - miniPositionFraction))
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    // 允许展开时短暂超出目标宽度，并始终以同一个中心向两侧形变。
                    modifier = Modifier.requiredWidth(miniPlayerWidth).height(50.dp),
                    visible = !playerVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val animatedVisibilityScope = this
                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "playerContainer"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    resizeMode = ResizeMode.RemeasureToBounds
                                )
                        ) {
                            MiniPlayer(
                                modifier = Modifier.fillMaxSize(),
                                song = playerState.currentSong,
                                sharedIdentity = playerState.currentQueueEntryIdentity,
                                canPlayNext = playerState.canPlayNext,
                                isPlaying = playerState.isPlaying,
                                onClick = onClickPlayer,
                                onPlayPauseClick = {
                                    if (playerState.currentSong == null) {
                                        viewModel.loadAndPlayRandom()
                                    } else {
                                        viewModel.togglePlayPause()
                                    }
                                },
                                onNextClick = viewModel::next,
                                expansionFraction = expandFraction
                            )
                        }
                    }
                }
            }

        }
    }
}
