package com.aria.rythme.feature.playlist.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.MainListPage

/**
 * 歌单页面
 * 包含精选电台和新近内容
 */
@Composable
fun PlayListScreen(
    viewModel: PlayListViewModel
) {

    MainListPage(routeKey = RythmeRoute.Playlist) {
        item {
            Text("施工中")
        }
    }
}