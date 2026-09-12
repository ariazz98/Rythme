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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.aria.rythme.ui.component.PlayerListScrollbar
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
    onClearHistory: () -> Unit,
    onSelectQueueEntry: (String) -> Unit,
    onReorderQueue: (fromEntryId: String, toEntryId: String) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    onToggleCrossfade: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: (Rect) -> Unit,
    innerPadding: PaddingValues,
    stickyBackdrop: Backdrop,
    screenDragOffsetY: MutableFloatState,
    dismissThreshold: Float,
    velocityThreshold: Float,
    onDismiss: (Float) -> Unit,
    onListScrolling: (Boolean) -> Unit,
    onCoverClick: () -> Unit,
    controlsSlide: Float = 0f,
    bodyTransitionModifier: Modifier = Modifier,
    externalHeader: CompactPanelHeaderGeometry? = null
) {
    val panelState = remember { PlaylistPanelState() }
    val density = LocalDensity.current
    val referenceScale = PlayerLayoutMetrics.scale(LocalWindowInfo.current.containerDpSize.width.value)

    // ── 列表切换偏移 ──
    var switchOffset by remember { mutableFloatStateOf(0f) }
    val headerCollapse = panelState.headerCollapseOffset
    SideEffect {
        externalHeader?.collapsePx = headerCollapse
        externalHeader?.historyOffsetPx = switchOffset
    }

    // ── 主列表状态 ──
    val mainListState = rememberLazyListState()

    // ── 历史列表状态 ──
    val historyListState = rememberLazyListState()

    val hasHistory = state.playHistory.isNotEmpty()
    var historyRowHeight by remember(density, referenceScale) {
        mutableFloatStateOf(with(density) { (54.dp * referenceScale).toPx() })
    }
    val historyHeadingHeight = with(density) { (44.dp * referenceScale).toPx() }
    SideEffect {
        panelState.historyContentHeightPx = if (hasHistory)
            historyHeadingHeight + historyRowHeight * state.playHistory.size else 0f
        // 数据变空时归还当前歌曲页，不调用清除历史或修改队列。
        if (!hasHistory) switchOffset = 0f
    }
    LaunchedEffect(switchOffset <= 0f, state.playHistory.firstOrNull()?.id) {
        if (hasHistory && switchOffset <= 0f) historyListState.scrollToItem(0)
    }

    // ── 拖拽状态 ──
    var draggedEntryId by remember { mutableStateOf<String?>(null) }
    var draggedTop by remember { mutableFloatStateOf(0f) }
    var pendingMoveIndex by remember { mutableStateOf<Int?>(null) }

    // 拖拽排序时隐藏 Controls，松手恢复
    LaunchedEffect(draggedEntryId) {
        onListScrolling(draggedEntryId != null)
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
                    val contentHeight = panelState.historyExtentPx
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
                        onDismiss(available.y)
                    } else {
                        animate(screenDragOffsetY.floatValue, 0f) { value, _ ->
                            screenDragOffsetY.floatValue = value
                        }
                    }
                    return available
                }
                // 处理 switchOffset
                if (switchOffset > 0f) {
                    val contentHeight = panelState.historyExtentPx
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
                val contentHeight = panelState.historyExtentPx
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
                val contentHeight = panelState.historyExtentPx
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
                        onDismiss(available.y)
                    } else {
                        animate(screenDragOffsetY.floatValue, 0f) { value, _ ->
                            screenDragOffsetY.floatValue = value
                        }
                    }
                    return available
                }
                // 处理 switchOffset
                val contentHeight = panelState.historyExtentPx
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
    val autoplayEntries = state.queue.entries.drop(maxOf(upcomingOffset, orderedEnd))
    val showInfinite = state.isInfinitePlayEnabled &&
        (state.repeatMode == RepeatMode.OFF || state.isPlayingInfiniteExtension)
    val separateAutoplaySection = showInfinite && upcomingOrdered.isNotEmpty()
    val onlyAutoplayUpcoming = showInfinite && upcomingOrdered.isEmpty()
    val showAutoplayHint = state.queue.currentEntry != null && upcomingOrdered.isEmpty() &&
        !showInfinite && (state.isPlayingInfiniteExtension || state.repeatMode == RepeatMode.OFF)

    // 两个分区共享同一套排序手势，条目身份和可移动范围不依赖标题数量。
    val dragSection = if (upcomingOrdered.any { it.id == draggedEntryId })
        upcomingOrdered else autoplayEntries
    val dragSectionIds = dragSection.map { it.id }
    val currentDragSectionIds by rememberUpdatedState(dragSectionIds)
    LaunchedEffect(upcomingOrdered.size, autoplayEntries.size, state.queue.currentEntry?.id) {
        draggedEntryId = null
        pendingMoveIndex = null
    }
    val moveDragged by rememberUpdatedState<(Float) -> Unit>({ delta ->
        val id = draggedEntryId
        val info = mainListState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == id }
        if (id != null && item != null) {
            // 视觉位置始终跟手；仅下方的候选落位限制在原分区。
            draggedTop += delta
            if (pendingMoveIndex == item.index) pendingMoveIndex = null
            if (pendingMoveIndex == null) {
                val center = draggedTop + item.size / 2f
                val target = info.visibleItemsInfo.firstOrNull { candidate ->
                    candidate.key in dragSectionIds && candidate.key != id &&
                        ((candidate.index > item.index && center > candidate.offset + candidate.size / 2f) ||
                         (candidate.index < item.index && center < candidate.offset + candidate.size / 2f))
                }
                if (target != null) {
                    pendingMoveIndex = target.index
                    onReorderQueue(id, target.key as String)
                }
            }
        }
    })
    fun startDrag(id: String) {
        val item = mainListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return
        draggedTop = item.offset.toFloat()
        pendingMoveIndex = null
        draggedEntryId = id
    }
    fun endDrag() {
        draggedEntryId = null
        pendingMoveIndex = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + (PlayerLayoutMetrics.QueueTopGap * referenceScale).dp,
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
        // 自动滚动只在所属分区还有未露出的条目时继续，不能把拖动项带到另一分区。
        LaunchedEffect(draggedEntryId) {
            while (draggedEntryId != null) {
                val info = mainListState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.key == draggedEntryId }
                if (item != null) {
                    val first = info.visibleItemsInfo.firstOrNull { it.key == currentDragSectionIds.firstOrNull() }
                    val last = info.visibleItemsInfo.firstOrNull { it.key == currentDragSectionIds.lastOrNull() }
                    val edge = maxOf(item.size.toFloat(), (info.viewportEndOffset - info.viewportStartOffset) * .15f)
                    val speed = when {
                        draggedTop + item.size > info.viewportEndOffset - edge &&
                            (last == null || last.offset + last.size > info.viewportEndOffset) -> {
                                val ratio = ((draggedTop + item.size - (info.viewportEndOffset - edge)) / edge).coerceIn(0f, 1f)
                                ratio * ratio * 15f
                            }
                        draggedTop < info.viewportStartOffset + edge &&
                            (first == null || first.offset < info.viewportStartOffset) -> {
                                val ratio = ((info.viewportStartOffset + edge - draggedTop) / edge).coerceIn(0f, 1f)
                                -ratio * ratio * 15f
                            }
                        else -> 0f
                    }
                    if (speed != 0f) mainListState.dispatchRawDelta(speed)
                    moveDragged(0f)
                }
                delay(16L)
            }
        }

        // ════════════════════════════════════════
        // 弹性位移仍由原 switchOffset 驱动；历史从最近条目向上揭露，标题不随整页飞入。
        // ════════════════════════════════════════
        if (hasHistory) {
            HistoryList(
                state = state,
                listState = historyListState,
                stickyBackdrop = stickyBackdrop,
                nestedScrollConnection = historyNestedScrollConnection,
                onClear = onClearHistory,
                revealInProgress = mainListState.isScrollInProgress,
                onRowHeight = { historyRowHeight = maxOf(historyRowHeight, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { switchOffset.coerceIn(0f, panelState.contentHeightPx).toDp() })
                    .then(bodyTransitionModifier)
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
                            onDismiss(velocity)
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
                    if (externalHeader != null) {
                        androidx.compose.foundation.layout.Spacer(Modifier.height(with(density) {
                            if (externalHeader.heightPx > 0f) externalHeader.heightPx.toDp()
                            else ((PlayerLayoutMetrics.CompactCoverSize +
                                2 * PlayerLayoutMetrics.CompactVerticalPadding) * referenceScale).dp
                        }))
                    } else CompactNowPlayingHeader(
                        state = state,
                        playerVisible = playerVisible,
                        animatedContentScope = animatedContentScope,
                        onCoverClick = onCoverClick,
                        onFavoriteClick = onFavoriteClick,
                        onMoreClick = onMoreClick,
                        referenceScale = referenceScale
                    )
                }

                Box(bodyTransitionModifier) {
                    ActionButtonsRow(
                        state = state,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onToggleInfinitePlay = onToggleInfinitePlay,
                        onToggleCrossfade = onToggleCrossfade
                    )
                }
            }

            // ── 列表区域 ──
            Box(modifier = Modifier.weight(1f).then(bodyTransitionModifier)) {
                LazyColumn(
                    state = mainListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(mainNestedScrollConnection),
                    overscrollEffect = null
                ) {
                    // upcoming_header（stickyHeader 原生吸顶）
                    stickyHeader(key = "upcoming_header") {
                        if (state.queue.entries.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.player_queue_empty),
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = .4f)
                                )
                            }
                        } else Column(
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
                            if (!showAutoplayHint) Text(
                                text = stringResource(R.string.continue_play),
                                fontSize = (17f * referenceScale).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp * referenceScale)
                            )
                            val subtitle = when {
                                onlyAutoplayUpcoming -> stringResource(R.string.player_autoplay_current)
                                !showAutoplayHint && state.queueSourceTitle != null ->
                                    stringResource(R.string.player_queue_from, state.queueSourceTitle)
                                else -> null
                            }
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    fontSize = (13f * referenceScale).sp,
                                    color = Color.White.copy(alpha = .65f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 32.dp * referenceScale)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp * referenceScale))
                        }
                    }

                    // ── 歌单待播列表 ──
                    itemsIndexed(upcomingOrdered, key = { _, entry -> entry.id }) { index, entry ->
                        val isDragged = draggedEntryId == entry.id

                        Box(
                            modifier = Modifier
                                .then(if (!isDragged) Modifier.animateItem() else Modifier)
                                .padding(horizontal = 32.dp * referenceScale)
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragged) draggedTop -
                                        (mainListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entry.id }?.offset ?: draggedTop.toInt()) else 0f
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
                                referenceScale = referenceScale,
                                onClick = { onSelectQueueEntry(entry.id) },
                                dragModifier = if (upcomingOrdered.size < 2) null else Modifier.pointerInput(entry.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { startDrag(entry.id) },
                                        onDrag = { change, amount -> change.consume(); moveDragged(amount.y) },
                                        onDragEnd = { endDrag() },
                                        onDragCancel = { endDrag() }
                                    )
                                }
                            )
                        }
                    }

                    // ── Infinite 扩展列表 ──
                    if (showInfinite) {
                        if (separateAutoplaySection) item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (separateAutoplaySection) stickyHeader(key = "infinite_header") {
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
                                    text = stringResource(R.string.player_autoplay_heading),
                                    fontSize = (17f * referenceScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 32.dp * referenceScale)
                                )
                                Text(
                                    text = stringResource(
                                        if (autoplayEntries.isEmpty()) R.string.infinite_exhausted
                                        else R.string.player_autoplay_future
                                    ),
                                    fontSize = (13f * referenceScale).sp,
                                    color = Color.White.copy(alpha = .65f),
                                    modifier = Modifier.padding(horizontal = 32.dp * referenceScale)
                                )
                                Spacer(modifier = Modifier.height(8.dp * referenceScale))
                            }
                        }

                        if (autoplayEntries.isNotEmpty()) {
                            itemsIndexed(autoplayEntries, key = { _, entry -> entry.id }) { index, entry ->
                                val isDragged = draggedEntryId == entry.id

                                Box(
                                    modifier = Modifier
                                        .then(if (!isDragged) Modifier.animateItem() else Modifier)
                                        .padding(horizontal = 32.dp * referenceScale)
                                        .zIndex(if (isDragged) 1f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragged) draggedTop -
                                                (mainListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entry.id }?.offset ?: draggedTop.toInt()) else 0f
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
                                        referenceScale = referenceScale,
                                        onClick = { onSelectQueueEntry(entry.id) },
                                        dragModifier = if (autoplayEntries.size < 2) null else Modifier.pointerInput(entry.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { startDrag(entry.id) },
                                                onDrag = { change, amount -> change.consume(); moveDragged(amount.y) },
                                                onDragEnd = { endDrag() },
                                                onDragCancel = { endDrag() }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (!state.isPlayingInfiniteExtension && state.repeatMode == RepeatMode.ALL) {
                        item(key = "repeat_footer") {
                            Text(stringResource(R.string.player_repeat_count, state.queue.orderedEntryCount),
                                color = Color.White.copy(alpha = .65f),
                                fontSize = (13f * referenceScale).sp,
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    if (showAutoplayHint) {
                        item(key = "autoplay_disabled") {
                            Text(stringResource(R.string.player_autoplay_disabled),
                                color = Color.White.copy(alpha = .65f),
                                fontSize = (15f * referenceScale).sp,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 32.dp * referenceScale)
                                    .padding(top = 80.dp * referenceScale),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                PlayerListScrollbar(mainListState, modifier = Modifier.fillMaxSize())
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
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    onToggleCrossfade: () -> Unit
) {
    val scale = PlayerLayoutMetrics.scale(LocalWindowInfo.current.containerDpSize.width.value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp * scale, vertical = 8.dp * scale),
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
        ) { onToggleShuffle() }

        // Repeat
        val repeatEnabled = state.queue.entries.isNotEmpty() && !state.isPlayingInfiniteExtension
        val repeatIcon = if (state.repeatMode == RepeatMode.ONE) R.drawable.ic_repeat_1 else R.drawable.ic_repeat
        val repeatActive = state.repeatMode != RepeatMode.OFF
        ActionButton(
            icon = repeatIcon,
            iconSize = 18.dp,
            enabled = repeatEnabled,
            active = repeatActive
        ) { onToggleRepeat() }

        // Infinite
        val infiniteEnabled = state.queue.entries.isNotEmpty()
        ActionButton(
            icon = R.drawable.ic_infinite,
            iconSize = 23.dp,
            enabled = infiniteEnabled,
            active = state.isInfinitePlayEnabled
        ) { onToggleInfinitePlay() }

        val crossfadeEnabled = state.queue.entries.isNotEmpty()
        ActionButton(
            icon = R.drawable.ic_cross_fade,
            iconSize = 24.dp,
            enabled = crossfadeEnabled,
            active = state.isCrossfadeEnabled,
            onClick = onToggleCrossfade
        )

    }

    Spacer(modifier = Modifier.height(8.dp * scale))
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
    revealInProgress: Boolean,
    onRowHeight: (Float) -> Unit,
    modifier: Modifier
) {
    val scale = PlayerLayoutMetrics.scale(LocalWindowInfo.current.containerDpSize.width.value)
    val headingHeight = 44.dp * scale
    Box(modifier.clip(RectangleShape)) {
        // playHistory 本身按最近优先保存；倒序布局使最新一条紧邻下方当前歌曲。
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().padding(top = headingHeight)
                .nestedScroll(nestedScrollConnection),
            overscrollEffect = null
        ) {
            items(state.playHistory, key = { "h_${it.id}" }) { entry ->
                Box(Modifier.padding(horizontal = 32.dp * scale)
                    .onSizeChanged { onRowHeight(it.height.toFloat()) }) {
                    HistoryListItem(entry.song, referenceScale = scale)
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(headingHeight)
                .drawBackdrop(
                    backdrop = stickyBackdrop,
                    shape = { RectangleShape },
                    effects = {},
                    highlight = null,
                    shadow = null
                )
                .padding(horizontal = 32.dp * scale),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(stringResource(R.string.play_history), fontSize = (14f * scale).sp,
                fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                stringResource(R.string.play_history_clear),
                fontSize = (14f * scale).sp, color = Color(0x66FFFFFF),
                modifier = Modifier.align(Alignment.CenterEnd)
                    .clickable(interactionSource = null, indication = null, onClick = onClear)
            )
        }
        PlayerListScrollbar(listState, reverse = true, activeOverride = revealInProgress,
            modifier = Modifier.fillMaxSize().padding(top = headingHeight))
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
    val scale = PlayerLayoutMetrics.scale(LocalWindowInfo.current.containerDpSize.width.value)
    val bgColor = when {
        !enabled -> Color(0x1AFFFFFF)
        active -> Color(0x80FFFFFF)
        else -> Color(0x33FFFFFF)
    }
    val tintColor = if (!enabled) Color(0x33FFFFFF) else Color.White

    Box(
        modifier = Modifier
            .width(70.dp * scale)
            .height(36.dp * scale)
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
            modifier = Modifier.size(iconSize * scale)
        )
    }
}
