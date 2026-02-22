package com.aria.rythme.feature.library.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.aria.rythme.R
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.RythmeListItem
import com.aria.rythme.ui.theme.rythmeColors

/**
 * 资料库页面
 * 包含可折叠的标题栏和列表内容
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel
) {

    MainListPage(
        titleRes = R.string.title_library,
        hasMoreMenu = true,
        hasAvatar = true,
        onMoreClick = {
            // TODO: 点击打开菜单
        },
        onAvatarClick = {
            // TODO: 头像点击打开半屏
        },
        mainContent = {
            // 播放列表
            item {
                RythmeListItem(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "播放列表",
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 艺人
            item {
                RythmeListItem(
                    icon = Icons.Default.Person,
                    title = "艺人",
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 专辑
            item {
                RythmeListItem(
                    icon = Icons.Default.Album,
                    title = "专辑",
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 歌曲
            item {
                RythmeListItem(
                    icon = Icons.Default.MusicNote,
                    title = "歌曲",
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = { viewModel.sendIntent(LibraryIntent.NavToSongList) }
                )
            }

            // 专属推荐
            item {
                RythmeListItem(
                    icon = Icons.Default.Recommend,
                    title = "专属推荐",
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 类型
            item {
                RythmeListItem(
                    icon = Icons.Default.Category,
                    title = "类型",
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 合辑
            item {
                RythmeListItem(
                    icon = Icons.Default.Group,
                    title = "合辑",
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }
        }
    )
}