package com.aria.rythme.ui.component

import com.aria.rythme.R

/** 用户授权的动画样例：只关闭菜单，不修改曲库筛选；标题明确标注演示。 */
internal fun previewFilterMenu(onDismiss: () -> Unit): List<MenuConfig> = listOf(
    MenuConfig.Item(true, R.drawable.ic_album, titleRes = R.string.menu_preview_all, onClick = onDismiss),
    MenuConfig.Separator,
    MenuConfig.Item(false, R.drawable.ic_star, titleRes = R.string.menu_preview_favorites, onClick = onDismiss),
    MenuConfig.Item(false, null, titleRes = R.string.menu_preview_local, onClick = onDismiss)
)
