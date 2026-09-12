package com.aria.rythme.ui.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aria.rythme.core.music.data.model.Song

/**
 * 浮层菜单的内容类型
 */
sealed interface OverlayMenu {
    /** 右上角 Action 菜单 */
    data class ActionMenu(
        val sourceKey: Any,
        val anchorBounds: Rect,
        val configs: List<MenuConfig>,
        val sourceActions: List<Action> = emptyList(),
        val pressedActionKey: String? = null,
        val sourceScale: Float = 1f,
        val referenceScale: Float = 1f,
        val backdrop: com.kyant.backdrop.Backdrop? = null
    ) : OverlayMenu

    /** 歌曲上下文菜单（锚定到更多按钮位置） */
    data class SongContext(
        val song: Song,
        val anchorBounds: Rect,
        val configs: List<MenuConfig>,
        val sourceIconSize: Dp = 18.dp,
        val sourceIconTint: Color = Color.Unspecified
    ) : OverlayMenu

    /** 歌曲编辑表单 */
    data class SongEdit(val song: Song) : OverlayMenu
}

/**
 * 浮层菜单状态持有者
 *
 * 与 playerVisible 的设计一致：简单的可变状态，通过 CompositionLocal 传递。
 */
class OverlayMenuState {
    // 源按钮一直交给浮层绘制到收回完成，不能在 dismiss 的同一帧重新露出另一块玻璃。
    internal var presentedAction by mutableStateOf<OverlayMenu.ActionMenu?>(null)
        private set
    internal var presentedSongContext by mutableStateOf<OverlayMenu.SongContext?>(null)
        private set
    var currentMenu: OverlayMenu? by mutableStateOf(null)
        private set

    fun show(menu: OverlayMenu) {
        if (menu is OverlayMenu.ActionMenu) presentedAction = menu
        if (menu is OverlayMenu.SongContext) presentedSongContext = menu
        currentMenu = menu
    }

    fun dismiss() {
        currentMenu = null
    }

    internal fun finishActionExit(menu: OverlayMenu.ActionMenu) {
        if (currentMenu !== menu && presentedAction === menu) presentedAction = null
    }

    internal fun finishSongExit(menu: OverlayMenu.SongContext) {
        if (currentMenu !== menu && presentedSongContext === menu) presentedSongContext = null
    }

    val isVisible: Boolean
        get() = currentMenu != null || presentedAction != null || presentedSongContext != null
}

val LocalOverlayMenu = compositionLocalOf { OverlayMenuState() }
