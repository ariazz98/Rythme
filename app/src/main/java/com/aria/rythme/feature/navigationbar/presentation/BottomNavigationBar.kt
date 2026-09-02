package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.ui.component.CompactBottomTab
import com.aria.rythme.ui.component.LiquidBottomTabs
import com.aria.rythme.ui.component.MiniPlayer
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
            stiffness = 700f
        ),
        label = "bottomBarExpansion"
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
        ) {
            val compactMiniWidth = (maxWidth - 116.dp).coerceAtLeast(120.dp)
            val miniPlayerWidth = lerp(compactMiniWidth, maxWidth, expandFraction)
            val miniPlayerOffsetX = lerp(58.dp, 0.dp, expandFraction)
            val compactAlpha = 1f - expandFraction

            CompactBottomTab(
                item = TOP_LEVEL_DESTINATIONS.values.elementAt(bottomBarState.lastPrimaryTabIndex),
                selected = selectedTabIndex() == bottomBarState.lastPrimaryTabIndex,
                onClick = { selectTab(bottomBarState.lastPrimaryTabIndex) },
                modifier = Modifier.graphicsLayer {
                    alpha = compactAlpha
                }
            )

            CompactBottomTab(
                item = TOP_LEVEL_DESTINATIONS[RythmeRoute.Search]!!,
                selected = selectedTabIndex() == SEARCH_TAB_INDEX,
                onClick = { selectTab(SEARCH_TAB_INDEX) },
                modifier = Modifier
                    .offset(x = maxWidth - 50.dp)
                    .graphicsLayer {
                        alpha = compactAlpha
                    }
            )

            Box(
                modifier = Modifier
                    .offset(x = miniPlayerOffsetX)
                    .width(miniPlayerWidth)
                    .height(50.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
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
                                onNextClick = viewModel::next
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset(y = 58.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .graphicsLayer {
                        alpha = expandFraction
                    }
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = selectTab,
                    enabled = expandFraction > 0.9f
                )
            }
        }
    }
}

private const val SEARCH_TAB_INDEX = 3
