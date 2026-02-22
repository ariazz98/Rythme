package com.aria.rythme.feature.playlist.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.aria.rythme.R
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.ui.component.MainListPage

/**
 * 歌单页面
 * 包含精选电台和新近内容
 */
@Composable
fun PlayListScreen(
    viewModel: PlayListViewModel
) {

    MainListPage(
        titleRes = R.string.title_play_list,
        hasMoreMenu = false,
        hasAvatar = true,
        onAvatarClick = {
            // TODO: 打开个人资料
        }
    ) {
        item {
            Text("施工中")
        }
    }
}