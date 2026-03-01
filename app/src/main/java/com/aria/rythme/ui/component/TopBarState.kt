package com.aria.rythme.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 顶部标题栏的滚动可见性状态
 *
 * 由各页面内的滚动容器通过 SideEffect 实时更新 [isScrollAtTop]，
 * 外层 Scaffold 根据此值决定是否显示全局 TopBar。
 */
class TopBarState {
    var isScrollAtTop by mutableStateOf(true)
}

/**
 * 提供全局 TopBarState 的 CompositionLocal
 */
val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun rememberTopBarState(): TopBarState = remember { TopBarState() }
