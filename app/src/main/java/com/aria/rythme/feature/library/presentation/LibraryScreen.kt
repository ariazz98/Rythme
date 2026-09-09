package com.aria.rythme.feature.library.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.ui.component.Action
import com.aria.rythme.ui.component.TopBarConfig
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.LibraryListItem
import com.aria.rythme.ui.theme.rythmeColors

/**
 * 资料库页面
 * 包含可折叠的标题栏和列表内容
 */
@Composable
fun LibraryScreen(
    onPlaylistsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
    onGenresClick: () -> Unit,
    onComposersClick: () -> Unit
) {

    MainListPage(
        title = stringResource(R.string.title_library),
        topBar = TopBarConfig(
            // 一级页最右侧留给头像；不在左组放 More，避免与子页右侧 More 跨组追踪。
            auxiliaryActions = listOf(
                Action.Icon(
                    actionKey = "add",
                    iconRes = R.drawable.ic_add_play_list,
                    // 复合列表图标与编辑图标同为 48×48 viewport，不沿用旧纯加号的 18dp 特例。
                    iconSize = 22.dp,
                    contentDescription = "${stringResource(R.string.create_playlist)}（待接入）"
                ),
                Action.Icon(
                    actionKey = "edit_library",
                    iconRes = R.drawable.ic_edit_list,
                    contentDescription = "${stringResource(R.string.edit_library)}（待接入）"
                )
            ),
            actions = listOf(Action.Avatar(actionKey = "avatar", name = "ARiA"))
        ),
        mainContent = {
            item {
                LibraryListItem(
                    icon = R.drawable.ic_music_list,
                    title = R.string.title_play_list,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = onPlaylistsClick
                )
            }

            // 艺人
            item {
                LibraryListItem(
                    icon = R.drawable.ic_artist,
                    title = R.string.music_artist,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = onArtistsClick
                )
            }

            // 专辑
            item {
                LibraryListItem(
                    icon = R.drawable.ic_album,
                    title = R.string.music_album,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = onAlbumsClick
                )
            }

            // 歌曲
            item {
                LibraryListItem(
                    icon = R.drawable.ic_music_library,
                    title = R.string.music_song,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = onSongsClick
                )
            }

            // 类型
            item {
                LibraryListItem(
                    icon = R.drawable.ic_type,
                    title = R.string.music_type,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = onGenresClick
                )
            }

            // 作曲者
            item {
                LibraryListItem(
                    icon = R.drawable.ic_composer,
                    title = R.string.music_composer,
                    iconColor = MaterialTheme.rythmeColors.primary,
                    onClick = onComposersClick
                )
            }
        }
    )
}
