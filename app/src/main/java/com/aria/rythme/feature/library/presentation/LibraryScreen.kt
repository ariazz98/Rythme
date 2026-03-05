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
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
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
        routeKey = RythmeRoute.Library,
        mainContent = {
            // 播放列表
            item {
                RythmeListItem(
                    icon = R.drawable.ic_music_list,
                    title = R.string.music_play_list,
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 艺人
            item {
                RythmeListItem(
                    icon = R.drawable.ic_artist,
                    title = R.string.music_artist,
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 专辑
            item {
                RythmeListItem(
                    icon = R.drawable.ic_album,
                    title = R.string.music_album,
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 歌曲
            item {
                RythmeListItem(
                    icon = R.drawable.ic_music_library,
                    title = R.string.music_song,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = { viewModel.sendIntent(LibraryIntent.NavToSongList) }
                )
            }

            // 类型
            item {
                RythmeListItem(
                    icon = R.drawable.ic_type,
                    title = R.string.music_type,
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }

            // 作曲者
            item {
                RythmeListItem(
                    icon = R.drawable.ic_composer,
                    title = R.string.music_composer,
                    iconColor = MaterialTheme.rythmeColors.primary
                )
            }
        }
    )
}