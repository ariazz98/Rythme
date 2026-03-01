package com.aria.rythme.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * 顶部标题栏的滚动可见性状态
 *
 * 按路由 key 分别存储各 Tab 的滚动位置，切换 Tab 时可立即读取对应缓存值，
 * 避免依赖 snapshotFlow 异步发射导致的"慢一帧"问题。
 */
class TopBarState {
    // 每个 Tab 路由独立保存"是否在顶部"的状态，默认 true（首次显示时展示 TopBar）
    private val scrollAtTopMap = mutableStateMapOf<NavKey, Boolean>()

    /** 读取指定路由的滚动状态，未记录时返回 true（默认展示 TopBar） */
    fun isScrollAtTop(routeKey: NavKey): Boolean = scrollAtTopMap[routeKey] ?: true

    /** 由各页面的滚动容器实时更新 */
    fun updateScrollAtTop(routeKey: NavKey, isAtTop: Boolean) {
        scrollAtTopMap[routeKey] = isAtTop
    }
}

/**
 * 提供全局 TopBarState 的 CompositionLocal
 */
val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun rememberTopBarState(): TopBarState = remember { TopBarState() }
