package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.ui.component.LiquidBottomTabs
import com.aria.rythme.ui.component.MiniPlayer
import com.aria.rythme.ui.component.utils.BottomBarGeometry
import com.aria.rythme.ui.component.utils.BottomBarMetrics
import com.aria.rythme.ui.component.utils.bottomBarBottomSpacing
import com.aria.rythme.ui.component.utils.TAB_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.miniPlayerExpansionAnimationSpec
import com.aria.rythme.ui.component.utils.miniPlayerWidthAnimationSpec
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
        animationSpec = miniPlayerExpansionAnimationSpec,
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

    val tabHeight = lerp(BottomBarMetrics.CompactHeight.dp, BottomBarMetrics.ExpandedTabHeight.dp, tabExpandFraction)
    val miniHeight = lerp(BottomBarMetrics.CompactHeight.dp, BottomBarMetrics.ExpandedMiniHeight.dp, expandFraction)
    val geometry = BottomBarGeometry(tabHeight.value, miniHeight.value, miniPositionFraction)
    val preparationScale = 1f - (1f - BottomBarMetrics.CompactScale) * preparationProgress
    val density = LocalDensity.current
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 21.dp, end = 21.dp, bottom = bottomBarBottomSpacing(navigationInset.value).dp)
    ) {
        // 宽度约束保持稳定；高度动画留在普通布局中，避免逐帧重新子组合。
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(geometry.height.dp)
            ) {
                val compactMiniWidth = (availableWidth - (2f * (BottomBarMetrics.CompactHeight + BottomBarMetrics.CompactGap)).dp)
                    .coerceAtLeast(120.dp)
                val targetMiniWidth = if (bottomBarState.isExpanded) availableWidth else compactMiniWidth
                // 从收起端点展开时，宽度与内容比纵向位移晚约 50ms 起动；回弹不传给内容。
                val miniPlayerWidth by animateFloatAsState(
                    targetValue = targetMiniWidth.value,
                    animationSpec = miniPlayerWidthAnimationSpec(
                        compactWidth = compactMiniWidth.value,
                        expandedWidth = availableWidth.value,
                        targetWidth = targetMiniWidth.value
                    ),
                    label = "miniPlayerWidthRebound"
                )
                // 收起开始时 Search 独立成胶囊；两枚图标连续移动到左右圆形。
                LiquidBottomTabs(
                    availableWidth = availableWidth,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = selectTab,
                    expansionFraction = tabExpandFraction,
                    targetExpanded = bottomBarState.isExpanded,
                    lastPrimaryTabIndex = bottomBarState.lastPrimaryTabIndex,
                    modifier = Modifier
                        .offset(y = geometry.tabTop.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = preparationScale
                            scaleY = preparationScale
                        }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (geometry.tabCenterY +
                            (geometry.miniTop - geometry.tabCenterY) * preparationScale).dp)
                        .height(miniHeight * preparationScale),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        // 允许展开时短暂超出目标宽度，并始终以同一个中心向两侧形变。
                        // 共享边界使用真实显示尺寸，而不是外层 graphicsLayer 缩放前的尺寸。
                        modifier = Modifier.requiredWidth(miniPlayerWidth.dp * preparationScale)
                            .height(miniHeight * preparationScale),
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
                                        boundsTransform = com.aria.rythme.feature.player.presentation.playerOverlayBounds(),
                                        resizeMode = ResizeMode.RemeasureToBounds
                                    )
                            ) {
                                // 在测量阶段落实内容比例，不再靠父图层缩放。
                                // 封面自己的共享边界因此也是最终像素尺寸，退出 overlay 时无二次缩放。
                                CompositionLocalProvider(LocalDensity provides Density(
                                    density.density * preparationScale, density.fontScale
                                )) {
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
    }
}
