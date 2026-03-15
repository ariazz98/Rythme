package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.core.music.data.model.Song

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

    // SongContext 菜单：缓存数据以支持退出动画
    val songContextMenu = menu as? OverlayMenu.SongContext
    val cachedSongContext = remember { mutableStateOf<OverlayMenu.SongContext?>(null) }
    if (songContextMenu != null) {
        cachedSongContext.value = songContextMenu
    }
    val songContextVisible = songContextMenu != null
    val songContextData = cachedSongContext.value

    // dismiss 后延迟清除缓存，给退出动画留时间，
    // 同时避免页面切换后面板残留
    LaunchedEffect(songContextVisible) {
        if (!songContextVisible && cachedSongContext.value != null) {
            delay(300)
            cachedSongContext.value = null
        }
    }

    if (songContextData != null) {
        // key 保证数据变化时重建 composable，避免 SubcomposeLayout 内部状态残留
        key(songContextData.song.id, songContextData.anchorBounds) {
            SongContextMenuOverlay(
                song = songContextData.song,
                anchorBounds = songContextData.anchorBounds,
                configs = songContextData.configs,
                visible = songContextVisible,
                onDismiss = { state.dismiss() }
            )
        }
    }

    // 其他 overlay 类型
    when (menu) {
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

/**
 * 歌曲上下文菜单
 *
 * 以锚点（更多按钮）为基准定位，向空间更大的方向展开。
 * 通过 sharedElementWithCallerManagedVisibility 与列表中的更多按钮联动过渡。
 */
@Composable
private fun SongContextMenuOverlay(
    song: Song,
    anchorBounds: Rect,
    configs: List<MenuConfig>,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val panelHeightPx = rememberMenuPanelHeightPx(configs)
    val panelWidthPx = with(density) { 256.dp.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }

    // 安全区域（状态栏 + 导航栏）
    val statusBarPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    val navBarPx = with(density) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // 计算展开方向和位置
        val safeTop = statusBarPx + marginPx
        val safeBottom = containerHeight - navBarPx - marginPx
        val expandDown = (containerHeight - anchorBounds.center.y) >= anchorBounds.center.y

        // 垂直定位：向下展开以 icon 顶部为锚，向上展开以 icon 底部为锚，盖住 icon
        val panelTop = run {
            val idealTop = if (expandDown) anchorBounds.top else (anchorBounds.bottom - panelHeightPx)
            idealTop.coerceIn(safeTop, (safeBottom - panelHeightPx).coerceAtLeast(safeTop))
        }

        // 水平定位：面板右边缘对齐 icon 右边缘
        val panelLeft = run {
            (anchorBounds.right - panelWidthPx)
                .coerceIn(marginPx, (containerWidth - marginPx - panelWidthPx).coerceAtLeast(marginPx))
        }

        // scrim：淡入淡出，点击关闭
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
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

        // 定位的面板：通过 sharedElement 与列表 icon 联动
        Box(
            modifier = Modifier
                .offset { IntOffset(panelLeft.toInt(), panelTop.toInt()) }
                .then(
                    if (visible) Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    } else Modifier
                )
        ) {
            with(sharedTransitionScope) {
                AnchoredMenuPanel(
                    configs = configs,
                    interactive = visible,
                    anchor = if (expandDown) PanelAnchor.TopEnd else PanelAnchor.BottomEnd,
                    columnModifier = Modifier.sharedElementWithCallerManagedVisibility(
                        sharedContentState = rememberSharedContentState(
                            key = "songMore_${song.id}"
                        ),
                        visible = visible,
                        boundsTransform = BoundsTransform { _, _ ->
                            spring(dampingRatio = 0.55f, stiffness = 250f)
                        }
                    )
                )
            }
        }
    }
}
