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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    .graphicsLayer {
                        // 折叠完成仍保留缩放及两侧留白；只有重新展开才恢复正常比例。
                        val scale = 1f - (1f - BottomBarMetrics.CompactScale) * preparationProgress
                        scaleX = scale
                        scaleY = scale
                        // 围绕固定的 Tab 中线缩放，两种高度不再把圆心额外向下推。
                        transformOrigin = TransformOrigin(0.5f, geometry.scalePivotY)
                    }
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
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = geometry.miniTop.dp)
                        .height(miniHeight),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        // 允许展开时短暂超出目标宽度，并始终以同一个中心向两侧形变。
                        modifier = Modifier.requiredWidth(miniPlayerWidth.dp).height(miniHeight),
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
}
