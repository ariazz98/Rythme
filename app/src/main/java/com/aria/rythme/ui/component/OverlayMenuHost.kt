package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.feature.songeditor.presentation.SongEditorContent
import com.aria.rythme.ui.theme.rythmeColors
import kotlin.math.roundToInt

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

    // SongEdit 底部面板
    val songEditMenu = menu as? OverlayMenu.SongEdit
    val cachedSongEdit = remember { mutableStateOf<OverlayMenu.SongEdit?>(null) }
    if (songEditMenu != null) {
        cachedSongEdit.value = songEditMenu
    }
    val songEditVisible = songEditMenu != null
    val songEditData = cachedSongEdit.value

    LaunchedEffect(songEditVisible) {
        if (!songEditVisible && cachedSongEdit.value != null) {
            delay(300)
            cachedSongEdit.value = null
        }
    }

    if (songEditData != null) {
        key(songEditData.song.id) {
            SongEditOverlay(
                song = songEditData.song,
                visible = songEditVisible,
                onDismiss = { state.dismiss() }
            )
        }
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

/**
 * 歌曲编辑底部面板
 *
 * 从底部滑入，顶部到状态栏下方，40dp 圆角，支持下滑关闭。
 */
@Composable
private fun SongEditOverlay(
    song: Song,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalWindowInfo.current.containerDpSize.height.toPx()
    }
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val sheetHeightPx = screenHeightPx - statusBarTopPx

    val dismissThreshold = sheetHeightPx * 0.35f
    val velocityThreshold = 2000f

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    // 初始在屏幕外，visible 时滑入
    val dragOffsetY = remember { Animatable(sheetHeightPx) }

    LaunchedEffect(visible) {
        if (visible) {
            dragOffsetY.animateTo(0f, tween(durationMillis = 300))
        } else {
            dragOffsetY.animateTo(sheetHeightPx, tween(durationMillis = 250))
        }
    }

    // NestedScroll：在 onPreScroll 中拦截，避免 overscroll 效果抢占 delta
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y

                // 上滑且 sheet 有偏移：优先归位 sheet
                if (dy < 0f && dragOffsetY.value > 0f) {
                    val consumed = dy.coerceAtLeast(-dragOffsetY.value)
                    scope.launch { dragOffsetY.snapTo((dragOffsetY.value + consumed).coerceAtLeast(0f)) }
                    return Offset(0f, consumed)
                }

                // 下拉且内容已到顶（或 sheet 已有偏移）：移动 sheet
                if (dy > 0f && (scrollState.value == 0 || dragOffsetY.value > 0f)) {
                    scope.launch { dragOffsetY.snapTo(dragOffsetY.value + dy) }
                    return Offset(0f, dy)
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetY.value > dismissThreshold || available.y > velocityThreshold) {
                    dragOffsetY.animateTo(sheetHeightPx, tween(durationMillis = 250))
                    onDismiss()
                    return available
                } else if (dragOffsetY.value > 0f) {
                    dragOffsetY.animateTo(0f)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val progress = (1f - dragOffsetY.value / sheetHeightPx).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * progress))
                .clickable(interactionSource = null, indication = null) {
                    scope.launch {
                        dragOffsetY.animateTo(sheetHeightPx, tween(durationMillis = 250))
                        onDismiss()
                    }
                }
        )

        // 面板内容
        Box(
            modifier = Modifier
                .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                .fillMaxSize()
                .padding(top = with(density) { statusBarTopPx.toDp() })
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(MaterialTheme.rythmeColors.surface)
                .nestedScroll(nestedScrollConnection)
        ) {
            SongEditorContent(
                song = song,
                scrollState = scrollState,
                onDismiss = onDismiss
            )
        }
    }
}
