package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.player.presentation.PlayerIntent
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.ui.component.LiquidBottomTabs
import com.aria.rythme.ui.component.MiniPlayer
import org.koin.androidx.compose.koinViewModel

/**
 * 底部导航栏（液态玻璃动效版）。
 *
 * ## 双区布局
 *
 * ```
 * 正常态:
 * ┌─────────────────────────────────┐  8dp  ┌──────┐
 * │  [Home] [Playlist] [Library]    │  gap  │  🔍  │
 * │         3-tab 胶囊               │       │ 圆形  │
 * └─────────────────────────────────┘       └──────┘
 *
 * 搜索态:
 * ┌──────┐  8dp  ┌─────────────────────────────────┐
 * │ icon │  gap  │  🔍                              │
 * │ 圆形  │       │         搜索胶囊                  │
 * └──────┘       └─────────────────────────────────┘
 * ```
 *
 * ## 三层叠加架构（Tabs Section 内）
 *
 * ```
 * ┌──────────────────────────────────────────────┐
 * │  Layer 3：选择器胶囊（56dp，宽 1/3）           │  ← 拖拽手势 + Squash&Stretch
 * │  Layer 2：accent 染色录制层（56dp，alpha=0）   │  ← 供选择器采样 accent 着色内容
 * │  Layer 1：主 Bar（64dp，可见）                 │  ← vibrancy + blur + lens
 * └──────────────────────────────────────────────┘
 * ```
 *
 * @param selectedTabIndex   当前选中的路由，由父级导航状态驱动
 * @param onTabSelected   Tab 切换回调，通知父级更新导航
 * @param onClickPlayer 点击 MiniPlayer 时打开播放器页面的回调
 */
@Composable
fun BottomNavigationBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    onClickPlayer: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsUiState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))))
            .navigationBarsPadding()
            .padding(start = 21.dp, end = 21.dp, bottom = 8.dp)
    ) {
        MiniPlayer(
            song = state.currentSong,
            canPlayNext = state.canPlayNext,
            isPlaying = state.isPlaying,
            onClick = { onClickPlayer() },
            onPlayPauseClick = {
                if (state.currentSong == null) {
                    viewModel.sendIntent(PlayerIntent.LoadAndPlayRandom)
                } else {
                    viewModel.sendIntent(PlayerIntent.TogglePlayPause)
                }
            },
            onNextClick = { viewModel.sendIntent(PlayerIntent.Next) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LiquidBottomTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected
        )
    }
}
