package com.aria.rythme.feature.player.presentation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aria.rythme.R
import com.aria.rythme.core.music.domain.model.RepeatMode
import com.aria.rythme.ui.component.HistoryListItem
import com.aria.rythme.ui.component.PlayListItem
import com.aria.rythme.ui.component.PlaylistPanelState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun SharedTransitionScope.QueueHistoryPanel(
    state: PlayerState,
    playerVisible: Boolean,
    scope: CoroutineScope,
    animatedContentScope: AnimatedContentScope,
    viewModel: PlayerViewModel,
    innerPadding: PaddingValues,
    stickyBackdrop: Backdrop,
    screenDragOffsetY: MutableFloatState,
    dismissThreshold: Float,
    velocityThreshold: Float,
    onDismiss: () -> Unit,
    onListScrolling: (Boolean) -> Unit,
    onCoverClick: () -> Unit,
    controlsSlide: Float = 0f
) {
    val panelState = remember { PlaylistPanelState() }

    // ── 列表切换偏移 ──
    var switchOffset by remember { mutableFloatStateOf(0f) }

    // ── 主列表状态 ──
    val mainListState = rememberLazyListState()

    // ── 历史列表状态 ──
    val historyListState = rememberLazyListState()

    val hasHistory = state.playHistory.isNotEmpty()

    // ── 拖拽状态 ──
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // 拖拽排序时隐藏 Controls，松手恢复
    LaunchedEffect(draggedIndex) {
        onListScrolling(draggedIndex != null)
    }

    // ── 主列表 NestedScrollConnection ──
    val mainNestedScrollConnection = remember(screenDragOffsetY, panelState, hasHistory) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                // switchOffset > 0 时，向上滑动优先恢复 switchOffset
                if (available.y < 0 && switchOffset > 0f) {
                    val oldOffset = switchOffset
                    switchOffset = (switchOffset + available.y).coerceAtLeast(0f)
                    return Offset(0f, switchOffset - oldOffset)
                }
                // screenDragOffsetY > 0 时，向上滑动优先恢复 dismiss offset
                if (available.y < 0 && screenDragOffsetY.floatValue > 0f) {
                    val oldValue = screenDragOffsetY.floatValue
                    val newOffset = (oldValue + available.y).coerceAtLeast(0f)
                    screenDragOffsetY.floatValue = newOffset
                    return Offset(0f, newOffset - oldValue)
                }
                // 向上滑动折叠 NowPlaying header
                if (available.y < 0 && panelState.nowPlayingHeightPx > 0f &&
                    panelState.headerCollapseOffset < panelState.nowPlayingHeightPx
                ) {
                    val old = panelState.headerCollapseOffset
                    panelState.headerCollapseOffset =
                        (old - available.y).coerceIn(0f, panelState.nowPlayingHeightPx)
                    val consumed = panelState.headerCollapseOffset - old
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y <= 0) return Offset.Zero

                var remaining = available.y

                // 1. 先展开 NowPlaying header
                if (panelState.headerCollapseOffset > 0f) {
                    val old = panelState.headerCollapseOffset
                    panelState.headerCollapseOffset = (old - remaining).coerceAtLeast(0f)
                    remaining -= (old - panelState.headerCollapseOffset)
                }

                if (remaining < 0.5f) return Offset(0f, available.y)

                // 2. 然后 history/dismiss
                if (hasHistory) {
                    val contentHeight = panelState.contentHeightPx
                    if (contentHeight > 0f) {
                        switchOffset = (switchOffset + remaining).coerceIn(0f, contentHeight)
                    }
                } else {
                    screenDragOffsetY.floatValue =
                        (screenDragOffsetY.floatValue + remaining).coerceAtLeast(0f)
                }

                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // 处理 dismiss offset
                if (screenDragOffsetY.floatValue > 0f) {
                    if (screenDragOffsetY.floatValue > dismissThreshold || available.y > velocityThreshold) {
                        onDismiss()
                    } else {
                        animate(screenDragOffsetY.floatValue, 0f) { value, _ ->
                            screenDragOffsetY.floatValue = value
                        }
                    }
                    return available
                }
                // 处理 switchOffset
                if (switchOffset > 0f) {
                    val contentHeight = panelState.contentHeightPx
                    val target = if (switchOffset > panelState.switchThresholdPx || available.y > velocityThreshold) {
                        contentHeight
                    } else {
                        0f
                    }
                    animate(switchOffset, target, initialVelocity = available.y) { value, _ ->
                        switchOffset = value
                    }
                    return available
                }
                // 处理 header snap
                if (panelState.nowPlayingHeightPx > 0f &&
                    panelState.headerCollapseOffset > 0f &&
                    panelState.headerCollapseOffset < panelState.nowPlayingHeightPx
                ) {
                    val target = if (panelState.headerCollapseOffset > panelState.nowPlayingHeightPx * 0.5f) {
                        panelState.nowPlayingHeightPx
                    } else {
                        0f
                    }
                    animate(panelState.headerCollapseOffset, target) { value, _ ->
                        panelState.headerCollapseOffset = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    // ── 历史列表 NestedScrollConnection ──
    val historyNestedScrollConnection = remember(screenDragOffsetY, panelState, hasHistory) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val contentHeight = panelState.contentHeightPx
                // switchOffset < contentHeight 时，向下滑动优先恢复 switchOffset
                if (available.y > 0 && switchOffset < contentHeight) {
                    val oldOffset = switchOffset
                    switchOffset = (switchOffset + available.y).coerceAtMost(contentHeight)
                    return Offset(0f, switchOffset - oldOffset)
                }
                // screenDragOffsetY > 0 时，向上滑动优先恢复 dismiss offset
                if (available.y < 0 && screenDragOffsetY.floatValue > 0f) {
                    val oldValue = screenDragOffsetY.floatValue
                    val newOffset = (oldValue + available.y).coerceAtLeast(0f)
                    screenDragOffsetY.floatValue = newOffset
                    return Offset(0f, newOffset - oldValue)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val contentHeight = panelState.contentHeightPx
                if (available.y < 0) {
                    // 列表在底部，剩余向上 delta → 减小 switchOffset（切换回主列表）
                    val oldOffset = switchOffset
                    switchOffset = (switchOffset + available.y).coerceAtLeast(0f)
                    return Offset(0f, switchOffset - oldOffset)
                }
                if (available.y > 0) {
                    // 列表在顶部，剩余向下 delta → 转发给 screenDragOffsetY（关闭播放器）
                    screenDragOffsetY.floatValue =
                        (screenDragOffsetY.floatValue + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // 处理 dismiss offset
                if (screenDragOffsetY.floatValue > 0f) {
                    if (screenDragOffsetY.floatValue > dismissThreshold || available.y > velocityThreshold) {
                        onDismiss()
                    } else {
                        animate(screenDragOffsetY.floatValue, 0f) { value, _ ->
                            screenDragOffsetY.floatValue = value
                        }
                    }
                    return available
                }
                // 处理 switchOffset
                val contentHeight = panelState.contentHeightPx
                if (switchOffset > 0f && switchOffset < contentHeight) {
                    val target = if (switchOffset > panelState.switchThresholdPx || available.y > velocityThreshold) {
                        contentHeight
                    } else if (available.y < -velocityThreshold) {
                        0f
                    } else if (switchOffset > panelState.switchThresholdPx) {
                        contentHeight
                    } else {
                        0f
                    }
                    animate(switchOffset, target, initialVelocity = available.y) { value, _ ->
                        switchOffset = value
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // ── 计算主列表内容 ──
    val upcomingOffset = state.queue.currentIndex + 1
    val orderedEnd = state.queue.orderedEntryCount.coerceAtMost(state.queue.entries.size)
    val upcomingOrdered = if (upcomingOffset in 0 until orderedEnd) {
        state.queue.entries.subList(upcomingOffset, orderedEnd)
    } else {
        emptyList()
    }
    val autoplayEntries = state.queue.autoplayEntries
    val showInfinite = state.isInfinitePlayEnabled && state.repeatMode == RepeatMode.OFF

    // 主列表中的固定索引偏移（NowPlaying 和 ActionButtons 已移出 LazyColumn）
    // index 0: upcoming_header
    // index 1 ~ 1+N-1: upcoming items
    val upcomingStartLazy = 1
    val extStartLazy = upcomingStartLazy + upcomingOrdered.size + (if (showInfinite) 1 else 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + 20.dp,
                bottom = innerPadding.calculateBottomPadding() * (1f - controlsSlide)
            )
            .clip(RectangleShape)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // 顶部渐隐
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black,
                        startY = 0f,
                        endY = 12.dp.toPx()
                    ),
                    blendMode = BlendMode.DstIn
                )

                // 底部渐隐
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        1f to Color.Transparent,
                        startY = size.height - 32.dp.toPx(),
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .onSizeChanged { size ->
                panelState.contentHeightPx = size.height.toFloat()
            }
    ) {
        // ── 拖拽自动滚动 ──
        LaunchedEffect(draggedIndex) {
            if (draggedIndex == null) return@LaunchedEffect
            while (draggedIndex != null) {
                val dragged = draggedIndex ?: break
                val lazyIndex = upcomingStartLazy + dragged
                val draggedItem = mainListState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == lazyIndex }
                if (draggedItem != null) {
                    val viewportStart = mainListState.layoutInfo.viewportStartOffset
                    val viewportEnd = mainListState.layoutInfo.viewportEndOffset
                    val viewportSize = viewportEnd - viewportStart
                    val draggedTop = draggedItem.offset + dragOffsetY.toInt()
                    val draggedBottom = draggedTop + draggedItem.size
                    val edgeZone = (viewportSize * 0.15f).coerceAtLeast(draggedItem.size.toFloat())

                    val scrollSpeed = when {
                        draggedBottom > viewportEnd - edgeZone -> {
                            val ratio = ((draggedBottom - (viewportEnd - edgeZone)) / edgeZone).coerceIn(0f, 1f)
                            ratio * ratio * 15f
                        }
                        draggedTop < viewportStart + edgeZone -> {
                            val ratio = (((viewportStart + edgeZone) - draggedTop) / edgeZone).coerceIn(0f, 1f)
                            -(ratio * ratio * 15f)
                        }
                        else -> 0f
                    }

                    if (scrollSpeed != 0f) {
                        val consumed = mainListState.dispatchRawDelta(scrollSpeed)
                        dragOffsetY += consumed
                    }
                }
                delay(16L)
            }
        }

        // ════════════════════════════════════════
        // 历史列表（translationY = switchOffset - contentHeight）
        // ════════════════════════════════════════
        if (hasHistory) {
            HistoryList(
                state = state,
                listState = historyListState,
                stickyBackdrop = stickyBackdrop,
                nestedScrollConnection = historyNestedScrollConnection,
                onClear = {
                    viewModel.sendIntent(PlayerIntent.ClearHistory)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = switchOffset - panelState.contentHeightPx
                    }
            )
        }

        // ════════════════════════════════════════
        // 主内容区（折叠头部 + 列表）
        // ════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = switchOffset
                }
        ) {
            // ── 折叠头部：NowPlaying（可折叠） + ActionButtons（始终可见） ──
            Column(
                modifier = Modifier.draggable(
                    state = rememberDraggableState { delta ->
                        screenDragOffsetY.floatValue =
                            (screenDragOffsetY.floatValue + delta).coerceAtLeast(0f)
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (screenDragOffsetY.floatValue > dismissThreshold || velocity > velocityThreshold) {
                            onDismiss()
                        } else {
                            scope.launch {
                                animate(screenDragOffsetY.floatValue, 0f) { value, _ ->
                                    screenDragOffsetY.floatValue = value
                                }
                            }
                        }
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            // 用自然高度更新 panelState，确保不受折叠影响
                            panelState.nowPlayingHeightPx = placeable.height.toFloat()
                            val collapseOffset = panelState.headerCollapseOffset.roundToInt()
                                .coerceAtMost(placeable.height)
                            val visibleHeight = (placeable.height - collapseOffset).coerceAtLeast(0)
                            layout(placeable.width, visibleHeight) {
                                placeable.placeRelative(0, -collapseOffset)
                            }
                        }
                ) {
                    CompactNowPlayingHeader(
                        state = state,
                        playerVisible = playerVisible,
                        animatedContentScope = animatedContentScope,
                        onCoverClick = onCoverClick
                    )
                }

                ActionButtonsRow(
                    state = state,
                    viewModel = viewModel
                )
            }

            // ── 列表区域 ──
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = mainListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(mainNestedScrollConnection),
                    overscrollEffect = null
                ) {
                    // upcoming_header（stickyHeader 原生吸顶）
                    stickyHeader(key = "upcoming_header") {
                        Column(
                            modifier = Modifier
                                .drawBackdrop(
                                    backdrop = stickyBackdrop,
                                    shape = { RectangleShape },
                                    effects = {},
                                    highlight = null,
                                    shadow = null,
                                    onDrawFront = {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Black,
                                                1f to Color.Transparent,
                                                startY = size.height - 12.dp.toPx(),
                                                endY = size.height
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                )
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.continue_play),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // ── 歌单待播列表 ──
                    itemsIndexed(upcomingOrdered, key = { _, entry -> entry.id }) { index, entry ->
                        val isDragged = draggedIndex == index
                        val currentIndex by rememberUpdatedState(index)
                        Box(
                            modifier = Modifier
                                .then(if (!isDragged) Modifier.animateItem() else Modifier)
                                .padding(horizontal = 32.dp)
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragged) dragOffsetY else 0f
                                }
                                .then(if (isDragged) {
                                    Modifier.drawBackdrop(
                                        backdrop = stickyBackdrop,
                                        shape = { RectangleShape },
                                        effects = {},
                                        highlight = null,
                                        shadow = null
                                    )
                                } else Modifier)
                        ) {
                            PlayListItem(
                                entry.song,
                                onClick = {
                                    viewModel.sendIntent(PlayerIntent.SelectQueueEntry(entry.id))
                                },
                                dragModifier = Modifier.pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = currentIndex
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y

                                            val dragged = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            val lazyIndex = upcomingStartLazy + dragged
                                            val draggedItem = mainListState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.index == lazyIndex } ?: return@detectDragGesturesAfterLongPress
                                            val draggedCenter = draggedItem.offset + draggedItem.size / 2 + dragOffsetY.toInt()

                                            mainListState.layoutInfo.visibleItemsInfo.forEach { item ->
                                                val itemLocalIndex = item.index - upcomingStartLazy
                                                if (itemLocalIndex < 0 || itemLocalIndex >= upcomingOrdered.size || itemLocalIndex == dragged) return@forEach
                                                val itemCenter = item.offset + item.size / 2
                                                if ((dragged < itemLocalIndex && draggedCenter > itemCenter) ||
                                                    (dragged > itemLocalIndex && draggedCenter < itemCenter)
                                                ) {
                                                    viewModel.sendIntent(
                                                        PlayerIntent.ReorderQueue(
                                                            fromEntryId = upcomingOrdered[dragged].id,
                                                            toEntryId = upcomingOrdered[itemLocalIndex].id
                                                        )
                                                    )
                                                    draggedIndex = itemLocalIndex
                                                    val sizeDiff = item.size - draggedItem.size
                                                    dragOffsetY += if (dragged < itemLocalIndex) -item.size.toFloat() + sizeDiff else item.size.toFloat() - sizeDiff
                                                    return@forEach
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // ── Infinite 扩展列表 ──
                    if (showInfinite) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        stickyHeader(key = "infinite_header") {
                            Column(
                                modifier = Modifier
                                    .drawBackdrop(
                                        backdrop = stickyBackdrop,
                                        shape = { RectangleShape },
                                        effects = {},
                                        highlight = null,
                                        shadow = null,
                                        onDrawFront = {
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    0f to Color.Black,
                                                    1f to Color.Transparent,
                                                    startY = size.height - 12.dp.toPx(),
                                                    endY = size.height
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                    )
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.infinite_extension),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (autoplayEntries.isEmpty()) R.string.infinite_exhausted
                                        else R.string.auto_play
                                    ),
                                    fontSize = 14.sp,
                                    color = Color(0x66FFFFFF),
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (autoplayEntries.isNotEmpty()) {
                            itemsIndexed(autoplayEntries, key = { _, entry -> entry.id }) { index, entry ->
                                val extDragTag = index + upcomingOrdered.size + 1
                                val currentIndex by rememberUpdatedState(index)
                                Box(
                                    modifier = Modifier
                                        .then(if (draggedIndex != extDragTag) Modifier.animateItem() else Modifier)
                                        .padding(horizontal = 32.dp)
                                        .zIndex(if (draggedIndex == extDragTag) 1f else 0f)
                                        .graphicsLayer {
                                            translationY = if (draggedIndex == extDragTag) dragOffsetY else 0f
                                        }
                                        .then(if (draggedIndex == extDragTag) {
                                            Modifier.drawBackdrop(
                                                backdrop = stickyBackdrop,
                                                shape = { RectangleShape },
                                                effects = {},
                                                highlight = null,
                                                shadow = null
                                            )
                                        } else Modifier)
                                ) {
                                    PlayListItem(
                                        entry.song,
                                        onClick = {
                                            viewModel.sendIntent(PlayerIntent.SelectQueueEntry(entry.id))
                                        },
                                        dragModifier = Modifier.pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedIndex = currentIndex + upcomingOrdered.size + 1
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y

                                                    val dragTag = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                                    val dragLocalIdx = dragTag - upcomingOrdered.size - 1
                                                    val lazyIndex = extStartLazy + dragLocalIdx
                                                    val draggedItem = mainListState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == lazyIndex } ?: return@detectDragGesturesAfterLongPress
                                                    val draggedCenter = draggedItem.offset + draggedItem.size / 2 + dragOffsetY.toInt()

                                                    mainListState.layoutInfo.visibleItemsInfo.forEach { item ->
                                                        val itemExtIdx = item.index - extStartLazy
                                                        if (itemExtIdx < 0 || itemExtIdx >= autoplayEntries.size || itemExtIdx == dragLocalIdx) return@forEach
                                                        val itemCenter = item.offset + item.size / 2
                                                        if ((dragLocalIdx < itemExtIdx && draggedCenter > itemCenter) ||
                                                            (dragLocalIdx > itemExtIdx && draggedCenter < itemCenter)
                                                        ) {
                                                            viewModel.sendIntent(
                                                                PlayerIntent.ReorderQueue(
                                                                    fromEntryId = autoplayEntries[dragLocalIdx].id,
                                                                    toEntryId = autoplayEntries[itemExtIdx].id
                                                                )
                                                            )
                                                            draggedIndex = itemExtIdx + upcomingOrdered.size + 1
                                                            val sizeDiff = item.size - draggedItem.size
                                                            dragOffsetY += if (dragLocalIdx < itemExtIdx) -item.size.toFloat() + sizeDiff else item.size.toFloat() - sizeDiff
                                                            return@forEach
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

            }
        }
    }
}

/**
 * 操作按钮行（Shuffle / Repeat / Autoplay）
 */
@Composable
private fun ActionButtonsRow(
    state: PlayerState,
    viewModel: PlayerViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Shuffle
        val shuffleEnabled = state.queue.entries.isNotEmpty() && !state.isPlayingInfiniteExtension
        ActionButton(
            icon = R.drawable.ic_shuffle_hard,
            iconSize = 20.dp,
            enabled = shuffleEnabled,
            active = state.isShuffleEnabled
        ) { viewModel.sendIntent(PlayerIntent.ToggleShuffleMode) }

        // Repeat
        val repeatEnabled = state.queue.entries.isNotEmpty() && !state.isPlayingInfiniteExtension
        val repeatIcon = if (state.repeatMode == RepeatMode.ONE) R.drawable.ic_repeat_1 else R.drawable.ic_repeat
        val repeatActive = state.repeatMode != RepeatMode.OFF
        ActionButton(
            icon = repeatIcon,
            iconSize = 18.dp,
            enabled = repeatEnabled,
            active = repeatActive
        ) { viewModel.sendIntent(PlayerIntent.ToggleRepeatMode) }

        // Infinite
        val infiniteEnabled = state.queue.entries.isNotEmpty()
        ActionButton(
            icon = R.drawable.ic_infinite,
            iconSize = 23.dp,
            enabled = infiniteEnabled,
            active = state.isInfinitePlayEnabled
        ) { viewModel.sendIntent(PlayerIntent.ToggleInfinitePlay) }

    }

    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * 播放历史列表
 */
@Composable
private fun HistoryList(
    state: PlayerState,
    listState: LazyListState,
    stickyBackdrop: Backdrop,
    nestedScrollConnection: NestedScrollConnection,
    onClear: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // 顶部渐隐
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black,
                        startY = 0f,
                        endY = 16.dp.toPx()
                    ),
                    blendMode = BlendMode.DstIn
                )
                // 底部渐隐
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        1f to Color.Transparent,
                        startY = size.height - 32.dp.toPx(),
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        overscrollEffect = null
    ) {
        stickyHeader(key = "history_header") {
            Column(
                modifier = Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawBackdrop(
                        backdrop = stickyBackdrop,
                        shape = { RectangleShape },
                        effects = {},
                        highlight = null,
                        shadow = null,
                        onDrawFront = {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Black,
                                    1f to Color.Transparent,
                                    startY = size.height - 16.dp.toPx(),
                                    endY = size.height
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.play_history),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    Text(
                        text = stringResource(R.string.play_history_clear),
                        fontSize = 14.sp,
                        color = Color(0x66FFFFFF),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = onClear
                            )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        items(state.playHistory, key = { "h_${it.id}" }) { song ->
            Box(modifier = Modifier.padding(horizontal = 32.dp)) {
                HistoryListItem(song)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 播放列表面板 Action 按钮
 *
 * 三态: disable / inactive / active（镂空效果）
 */
@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    iconSize: Dp,
    enabled: Boolean,
    active: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        !enabled -> Color(0x1AFFFFFF)
        active -> Color(0x80FFFFFF)
        else -> Color(0x33FFFFFF)
    }
    val tintColor = if (!enabled) Color(0x33FFFFFF) else Color.White

    Box(
        modifier = Modifier
            .width(70.dp)
            .height(36.dp)
            .then(
                if (active && enabled) {
                    Modifier
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            val outline = ContinuousCapsule.createOutline(size, layoutDirection, this)
                            when (outline) {
                                is Outline.Generic -> drawPath(outline.path, color = bgColor, blendMode = BlendMode.SrcOut)
                                is Outline.Rounded -> drawRoundRect(color = bgColor, cornerRadius = outline.roundRect.let { CornerRadius(it.topLeftCornerRadius.x, it.topLeftCornerRadius.y) }, blendMode = BlendMode.SrcOut)
                                is Outline.Rectangle -> drawRect(color = bgColor, blendMode = BlendMode.SrcOut)
                            }
                        }
                } else {
                    Modifier.background(bgColor, ContinuousCapsule)
                }
            )
            .then(
                if (enabled) {
                    Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "",
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
