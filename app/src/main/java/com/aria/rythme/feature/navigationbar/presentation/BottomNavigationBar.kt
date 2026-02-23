package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS
import com.aria.rythme.feature.player.presentation.PlayerIntent
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.ui.component.MiniPlayer
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.androidx.compose.koinViewModel

/**
 * 底部导航栏（Apple Music 风格）
 *
 * 四个主要标签页：
 * 1. 主页 - 专属推荐、最近播放
 * 2. 新发现 - 新专辑、排行榜
 * 3. 广播 - 电台、直播
 * 4. 资料库 - 本地音乐、歌单
 *
 * @param selectedKey 当前选中的标签索引
 * @param onSelectKey 标签选中回调
 */
@Composable
fun BottomNavigationBar(
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    onClickPlayer: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsUiState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .padding(start = 21.dp, end = 21.dp, bottom = 8.dp)
    ) {
        MiniPlayer(
            song = state.currentSong,
            canPlayNext = state.canPlayNext,
            isPlaying = state.isPlaying,
            onClick = {
                onClickPlayer()
            },
            onPlayPauseClick = {
                if (state.currentSong == null) {
                    viewModel.sendIntent(PlayerIntent.LoadAndPlayRandom)
                } else {
                    viewModel.sendIntent(PlayerIntent.TogglePlayPause)
                }
            },
            onNextClick = {
                viewModel.sendIntent(PlayerIntent.Next)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.rythmeColors.bottomBackground, RoundedCornerShape(30.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                TOP_LEVEL_DESTINATIONS.forEach { (route, item) ->
                    BottomNavigationItem(
                        modifier = Modifier
                            .weight(1f),
                        iconRes = item.icon,
                        titleRes = item.title,
                        isSelected = selectedKey == route,
                        onClick = { onSelectKey(route) }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationItem(
    modifier: Modifier,
    iconRes: Int,
    titleRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isSelected) MaterialTheme.rythmeColors.bottomSelected else MaterialTheme.rythmeColors.bottomUnselected,
                RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = if (isSelected) MaterialTheme.rythmeColors.primary else MaterialTheme.rythmeColors.textColor,
            modifier = Modifier
                .size(24.dp)
        )
        Text(
            text = stringResource(titleRes),
            color = if (isSelected) MaterialTheme.rythmeColors.primary else MaterialTheme.rythmeColors.textColor,
            fontSize = 10.sp
        )
    }
}
