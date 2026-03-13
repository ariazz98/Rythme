package com.aria.rythme.ui.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aria.rythme.core.music.data.model.Song

/**
 * 浮层菜单的内容类型
 */
sealed interface OverlayMenu {
    /** 右上角 Action 菜单 */
    data class ActionMenu(val configs: List<MenuConfig>) : OverlayMenu

    /** 歌曲上下文菜单 */
    data class SongContext(val song: Song) : OverlayMenu

    /** 歌曲编辑表单 */
    data class SongEdit(val song: Song) : OverlayMenu
}

/**
 * 浮层菜单状态持有者
 *
 * 与 playerVisible 的设计一致：简单的可变状态，通过 CompositionLocal 传递。
 */
class OverlayMenuState {
    var currentMenu: OverlayMenu? by mutableStateOf(null)
        private set

    fun show(menu: OverlayMenu) {
        currentMenu = menu
    }

    fun dismiss() {
        currentMenu = null
    }

    val isVisible: Boolean
        get() = currentMenu != null
}

val LocalOverlayMenu = compositionLocalOf { OverlayMenuState() }
