package com.aria.rythme.ui.component

import com.aria.rythme.R

/**
 * 构建歌曲上下文菜单的默认配置项
 */
fun buildSongContextMenuConfigs(
    onDismiss: () -> Unit
): List<MenuConfig> = listOf(
    MenuConfig.Item(
        iconRes = R.drawable.ic_edit,
        titleRes = R.string.song_edit,
        onClick = { onDismiss() }
    )
)
