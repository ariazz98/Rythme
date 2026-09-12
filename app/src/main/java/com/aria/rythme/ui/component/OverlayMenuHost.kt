package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
import androidx.compose.ui.unit.Dp
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
    state: OverlayMenuState = LocalOverlayMenu.current,
    onSaveSong: suspend (Song) -> Unit
) {
    val menu = state.currentMenu
    val sharedTransitionScope = LocalSharedTransitionScope.current

    // 同一份退场快照也供源按钮使用，收回完成前不重新绘制源玻璃。
    val actionMenu = menu as? OverlayMenu.ActionMenu
    state.presentedAction?.let { presented ->
        key(presented) {
            MorphingActionMenuOverlay(
                menu = presented,
                visible = actionMenu === presented,
                onDismiss = state::dismiss,
                onExitFinished = { state.finishActionExit(presented) }
            )
        }
    }

    // 来源图标与菜单共用同一份退场快照，尾滴结束后才交还真实图标。
    val songContextMenu = menu as? OverlayMenu.SongContext
    val songContextData = state.presentedSongContext

    if (songContextData != null) {
        // key 保证数据变化时重建 composable，避免 SubcomposeLayout 内部状态残留
        key(songContextData) {
            SongContextMenuOverlay(
                song = songContextData.song,
                anchorBounds = songContextData.anchorBounds,
                configs = songContextData.configs,
                sourceIconSize = songContextData.sourceIconSize,
                sourceIconTint = songContextData.sourceIconTint,
                visible = songContextMenu === songContextData,
                onDismiss = { state.dismiss() },
                onExitFinished = {
                    state.finishSongExit(songContextData)
                }
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

    if (songEditData != null) {
        key(songEditData.song.id) {
            SongEditOverlay(
                song = songEditData.song,
                visible = songEditVisible,
                onSave = onSaveSong,
                onDismiss = { state.dismiss() },
                onExitFinished = {
                    if (state.currentMenu !is OverlayMenu.SongEdit) {
                        cachedSongEdit.value = null
                    }
                }
            )
        }
    }
}


/**
 * 歌曲上下文菜单
 *
 * 以锚点（更多按钮）为基准定位，向空间更大的方向展开。
 * 与顶部共用轮廓渲染，但来源为裸图标，残余玻璃最终消失。
 */
@Composable
private fun SongContextMenuOverlay(
    song: Song,
    anchorBounds: Rect,
    configs: List<MenuConfig>,
    sourceIconSize: Dp,
    sourceIconTint: Color,
    visible: Boolean,
    onDismiss: () -> Unit,
    onExitFinished: () -> Unit
) {
    val presentation = remember(song.id, anchorBounds, configs) {
        OverlayMenu.ActionMenu(
            sourceKey = "songMore_${song.id}",
            anchorBounds = anchorBounds,
            configs = configs
        )
    }
    MorphingActionMenuOverlay(presentation, visible, onDismiss, onExitFinished,
        allowUpward = true, panelWidth = 256f, panelCorner = 48f, sourceSurface = MenuSourceSurface.Icon,
        sourceIconSize = sourceIconSize, sourceIconTint = sourceIconTint)
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
    onSave: suspend (Song) -> Unit,
    onDismiss: () -> Unit,
    onExitFinished: () -> Unit
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
            onExitFinished()
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
                onSave = onSave,
                onDismiss = onDismiss
            )
        }
    }
}
