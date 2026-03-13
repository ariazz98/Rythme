package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.ui.component.MenuPanel

/**
 * 浮层菜单宿主
 *
 * 放置在 RythmeApp 的 Box 中，层级在 PlayerScreen 之上。
 * 使用 AnimatedVisibility 控制显隐动画。
 *
 * 触摸事件处理：
 * - overlay 可见时，全屏拦截所有触摸（阻止穿透到下层）
 * - 点击菜单外部区域关闭 overlay
 * - 点击菜单内容区域不关闭
 */
@Composable
fun OverlayMenuHost(
    state: OverlayMenuState = LocalOverlayMenu.current
) {
    val menu = state.currentMenu
    val sharedTransitionScope = LocalSharedTransitionScope.current

    // 全屏触摸拦截层：overlay 可见时阻止所有触摸穿透到下层，点击即关闭
    if (state.isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            // 消费所有事件，阻止穿透
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        )
    }

    // ActionMenu 始终保持在同一组合位置，避免 when 分支切换导致 AnimatedVisibility 重建
    val actionMenu = menu as? OverlayMenu.ActionMenu
    val cachedConfigs = remember { mutableStateOf(emptyList<MenuConfig>()) }
    if (actionMenu != null) {
        cachedConfigs.value = actionMenu.configs
    }

    // ActionMenuOverlay 始终保持组合（sharedElement 过渡需要两端同时存在）。
    // interactive 控制触摸事件：仅在菜单可见或过渡进行中时处理，
    // 过渡完成后的残留内容不再拦截触摸。
    val visible = actionMenu != null
    val interactive = visible || sharedTransitionScope.isTransitionActive
    ActionMenuOverlay(
        configs = cachedConfigs.value,
        visible = visible,
        interactive = interactive,
        onDismiss = { state.dismiss() }
    )

    // 其他 overlay 类型
    when (menu) {
        is OverlayMenu.SongContext -> { /* TODO */ }
        is OverlayMenu.SongEdit -> { /* TODO */ }
        else -> {}
    }
}

/**
 * 右上角 Action 菜单
 *
 * 从右上角缩放弹出，点击外部关闭。
 */
@Composable
private fun ActionMenuOverlay(
    configs: List<MenuConfig>,
    visible: Boolean,
    interactive: Boolean,
    onDismiss: () -> Unit
) {
    // MenuPanel：右上角弹出，通过 sharedBounds 与 action 容器共享过渡
    val sharedTransitionScope = LocalSharedTransitionScope.current
    with(sharedTransitionScope) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // scrim：仅在 interactive 状态下拦截触摸
                if (interactive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { change ->
                                            if (!change.isConsumed) {
                                                change.consume()
                                                onDismiss()
                                            }
                                        }
                                    }
                                }
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(
                            end = 12.dp
                        )
                        .then(
                            if (interactive) Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        // 消费事件，阻止穿透到 scrim
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } else Modifier
                        )
                ) {
                    MenuPanel(
                        scope = this@AnimatedVisibility,
                        configs = configs,
                        interactive = interactive
                    )
                }
            }
        }
    }
}
