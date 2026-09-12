package com.aria.rythme.ui.component

import com.aria.rythme.R

/**
 * 列表、搜索、专辑、歌单与播放器共用的歌曲菜单。
 * 本轮按用户确认只补齐 UI：新增入口仅关闭菜单，不模拟业务成功或修改状态。
 * 编辑保留已有行为。后续接入功能时在这里接收明确回调，不在各页面复制菜单。
 */
fun buildSongContextMenuConfigs(
    onDismiss: () -> Unit,
    onEdit: () -> Unit
): List<MenuConfig> = listOf(
    MenuConfig.Group(listOf(
        MenuConfig.Item(iconRes = R.drawable.ic_star_filled, titleRes = R.string.song_menu_favorite,
            onClick = onDismiss),
        MenuConfig.Item(iconRes = R.drawable.ic_share, titleRes = R.string.song_menu_share,
            onClick = onDismiss)
    )),
    MenuConfig.Separator,
    MenuConfig.Item(iconRes = R.drawable.ic_add_play_list, titleRes = R.string.song_menu_add_to_playlist,
        onClick = onDismiss),
    MenuConfig.Separator,
    MenuConfig.Item(iconRes = R.drawable.ic_queue_next, titleRes = R.string.song_menu_play_next,
        onClick = onDismiss),
    MenuConfig.Item(iconRes = R.drawable.ic_queue_last, titleRes = R.string.song_menu_play_later,
        onClick = onDismiss),
    MenuConfig.Separator,
    MenuConfig.Item(iconRes = R.drawable.ic_album, titleRes = R.string.song_menu_go_to_album,
        onClick = onDismiss),
    MenuConfig.Item(iconRes = R.drawable.ic_song_info, titleRes = R.string.song_menu_info,
        onClick = onDismiss),
    MenuConfig.Separator,
    MenuConfig.Item(
        iconRes = R.drawable.ic_edit,
        titleRes = R.string.song_edit,
        onClick = {
            onDismiss()
            onEdit()
        }
    )
)
