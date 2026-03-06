package com.aria.rythme.feature.library.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.aria.rythme.R
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.LibraryListItem
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

            // 艺人
            item {
                LibraryListItem(
                    icon = R.drawable.ic_artist,
                    title = R.string.music_artist,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = { viewModel.sendIntent(LibraryIntent.NavToArtistList) }
                )
            }

            // 专辑
            item {
                LibraryListItem(
                    icon = R.drawable.ic_album,
                    title = R.string.music_album,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = { viewModel.sendIntent(LibraryIntent.NavToAlbumList) }
                )
            }

            // 歌曲
            item {
                LibraryListItem(
                    icon = R.drawable.ic_music_library,
                    title = R.string.music_song,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = { viewModel.sendIntent(LibraryIntent.NavToSongList) }
                )
            }

            // 类型
            item {
                LibraryListItem(
                    icon = R.drawable.ic_type,
                    title = R.string.music_type,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = { viewModel.sendIntent(LibraryIntent.NavToGenreList) }
                )
            }

            // 作曲者
            item {
                LibraryListItem(
                    icon = R.drawable.ic_composer,
                    title = R.string.music_composer,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = { viewModel.sendIntent(LibraryIntent.NavToComposerList) }
                )
            }
        }
    )
}