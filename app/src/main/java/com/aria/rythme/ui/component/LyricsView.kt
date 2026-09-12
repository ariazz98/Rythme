package com.aria.rythme.ui.component

import android.os.SystemClock
import android.view.ViewConfiguration

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsStatus
import com.aria.rythme.core.music.data.model.LyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI

// 当前行固定在距容器顶部的偏移
private val TOP_OFFSET = 32.dp

// 歌词字体
private val LYRIC_FONT_SIZE = 32.sp
private val LYRIC_LINE_HEIGHT = 44.sp

/**
 * Apple Music 风格歌词视图
 *
 * @param isFullScreen controls 是否已隐藏（全屏模式）
 * @param onToggleControls 沉浸模式下第一次点击仅唤回 controls，不同时跳转歌词
 */
@Composable
fun LyricsView(
    lyricsData: LyricsData?,
    lyricsStatus: LyricsStatus,
    currentLyricIndex: Int,
    onSeekToLine: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    onToggleControls: (() -> Unit)? = null,
    scrollState: LyricsScrollState = rememberLyricsScrollState(),
    isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    positionDiscontinuity: Long = 0L,
    onControlsVisibleChange: (Boolean) -> Unit = {},
    referenceScale: Float = 1f
) {
    when {
        lyricsStatus == LyricsStatus.LOADING -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "歌词加载中…",
                    color = Color(0x80FFFFFF),
                    fontSize = 16.sp
                )
            }
        }
        lyricsStatus == LyricsStatus.NOT_FOUND || lyricsStatus == LyricsStatus.ERROR -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "暂无歌词",
                    color = Color(0x80FFFFFF),
                    fontSize = 16.sp
                )
            }
        }
        lyricsData != null && lyricsData.plainText != null && lyricsData.lines.isEmpty() -> {
            PlainLyricsView(plainText = lyricsData.plainText, referenceScale = referenceScale, modifier = modifier)
        }
        lyricsData != null && lyricsData.lines.isNotEmpty() -> {
            SyncedLyricsView(
                lyricsData = lyricsData,
                reportedLyricIndex = currentLyricIndex,
                onSeekToLine = onSeekToLine,
                isFullScreen = isFullScreen,
                onToggleControls = onToggleControls,
                scrollState = scrollState,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                positionDiscontinuity = positionDiscontinuity,
                onControlsVisibleChange = onControlsVisibleChange,
                referenceScale = referenceScale,
                modifier = modifier
            )
        }
        else -> {
            Box(modifier = modifier.fillMaxSize())
        }
    }
}

/**
 * 纯文本歌词（无时间同步）
 */
@Composable
private fun PlainLyricsView(
    plainText: String,
    referenceScale: Float,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lines = remember(plainText) { plainText.lines().filter { it.isNotBlank() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .lyricsFadingEdges(referenceScale)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 32.dp * referenceScale, vertical = 48.dp * referenceScale),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lines) { _, line ->
                Text(
                    text = line,
                    color = Color(0xB3FFFFFF),
                    fontSize = 18.sp * referenceScale,
                    lineHeight = 28.sp * referenceScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }
        }
        PlayerListScrollbar(listState, Modifier.fillMaxSize())
    }
}

/**
 * 同步歌词视图（Apple Music 风格）
 */
@Composable
private fun SyncedLyricsView(
    lyricsData: LyricsData,
    reportedLyricIndex: Int,
    onSeekToLine: (Int) -> Unit,
    isFullScreen: Boolean,
    onToggleControls: (() -> Unit)?,
    scrollState: LyricsScrollState,
    isPlaying: Boolean,
    currentPositionMs: Long,
    positionDiscontinuity: Long,
    onControlsVisibleChange: (Boolean) -> Unit,
    referenceScale: Float,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val lines = lyricsData.lines
    val displayPosition = remember { mutableLongStateOf(currentPositionMs) }
    val playbackClock = remember { LyricsPlaybackClock() }
    LaunchedEffect(currentPositionMs, isPlaying, positionDiscontinuity) {
        displayPosition.longValue = playbackClock.sample(currentPositionMs, isPlaying,
            positionDiscontinuity, System.nanoTime())
    }
    LaunchedEffect(playbackClock, isPlaying) {
        if (isPlaying) {
            while (true) withFrameNanos { frameNs ->
                displayPosition.longValue = playbackClock.frame(frameNs)
            }
        }
    }
    val currentLyricIndex by remember(lines, isPlaying, reportedLyricIndex) {
        derivedStateOf {
            // 换句也用同一展示时钟，避免扫亮已开始而当前行还落后一个 200ms 采样周期。
            if (isPlaying) lines.indexOfLast { it.startTimeMs <= displayPosition.longValue }
            else reportedLyricIndex
        }
    }
    val wave = remember { LyricsLineWave() }
    val waveStrength by animateFloatAsState(if (wave.running) 1f else 0f,
        animationSpec = if (wave.running) snap() else tween(100), label = "lyricWaveRelease")
    var lastFollowedItem by remember { mutableStateOf<Int?>(null) }

    var lastLineHeight by remember(density, referenceScale) { mutableStateOf(60.dp * referenceScale) }
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    val currentControlsCallback by rememberUpdatedState(onControlsVisibleChange)
    val controlsHidden by rememberUpdatedState(isFullScreen)
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis
    LaunchedEffect(scrollState.isLyricsTouching, longPressTimeout) {
        if (scrollState.isTouchClarityDeferred) {
            delay(longPressTimeout)
            scrollState.allowLyricsTouchClarity()
        }
    }
    val context = LocalContext.current
    val minimumFlingVelocity = remember(context) {
        ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()
    }
    val manualScroll = remember(scrollState, listState, minimumFlingVelocity) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && scrollState.isTouching) {
                    scrollState.onUserScrollStart()
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (scrollState.mode == LyricsScrollMode.ManualScrolling) {
                    lyricsFlingControlsVisibility(available.y, minimumFlingVelocity,
                        listState.canScrollBackward, listState.canScrollForward)
                        ?.let { currentControlsCallback(it) }
                }
                // 不消费速度，沿用 LazyColumn 原来的惯性与边界行为。
                return Velocity.Zero
            }
        }
    }

    // 手动阶段包含松手后的惯性；惯性停止才开始等待恢复跟随。
    LaunchedEffect(isDragged, listState.isScrollInProgress, scrollState.isTouching) {
        if (isDragged) {
            scrollState.onUserScrollStart()
        } else if (!listState.isScrollInProgress && !scrollState.isTouching) {
            scrollState.onUserScrollStop(SystemClock.uptimeMillis())
        }
    }

    val mode = scrollState.mode
    LaunchedEffect(mode, isPlaying, scrollState.isTouching, scrollState.lastInteractionEndMs) {
        if (mode == LyricsScrollMode.WaitingToResume && isPlaying && !scrollState.isTouching) {
            delay((scrollState.lastInteractionEndMs + LyricsScrollState.AUTO_FOLLOW_RESUME_DELAY -
                SystemClock.uptimeMillis()).coerceAtLeast(0))
            scrollState.onResumeTimerFired(SystemClock.uptimeMillis(), isPlaying)
        }
    }

    var initiallyPositioned by remember { mutableStateOf(false) }
    LaunchedEffect(currentLyricIndex, scrollState.canFollow, mode) {
        if (scrollState.canFollow) {
            val targetItem = if (currentLyricIndex < 0) 0 else currentLyricIndex + 1
            val naturalNext = initiallyPositioned && isPlaying && mode == LyricsScrollMode.AutoFollow &&
                lastFollowedItem?.plus(1) == targetItem
            lastFollowedItem = targetItem
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
            if (!initiallyPositioned) {
                // 唯一允许立即定位的路径：正文尚未显现之前。
                listState.scrollToItem(targetItem)
                initiallyPositioned = true
            } else {
                if (!naturalNext || !listState.animateLyricsLineChange(targetItem, wave)) {
                    listState.animateLyricsToItem(targetItem,
                        maximumStepPx = with(density) { (LYRIC_LINE_HEIGHT * referenceScale).toPx() / 2f })
                }
            }
            scrollState.onFollowAnimationFinished()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (initiallyPositioned) 1f else 0f }
            .pointerInput(scrollState) {
                try {
                    awaitPointerEventScope {
                        while (true) {
                            // 先记录手势开始前的浏览状态，再交给子项处理按压；不消费事件。
                            val pressed = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
                            if (pressed) scrollState.onLyricsTouchDown(
                                clearOnPress = !controlsHidden || scrollState.isClearMode)
                            else scrollState.onLyricsTouchReleased()
                        }
                    }
                } finally {
                    scrollState.onLyricsTouchReleased()
                }
            }
            .nestedScroll(manualScroll)
            .clickable(enabled = isFullScreen, interactionSource = null, indication = null) {
                onToggleControls?.invoke()
            }
            .lyricsFadingEdges(referenceScale)
    ) {
        // 尾部仅预留让最后一句到达跟随锚点的空间，随控制区显隐调整可视范围。
        val tailSpace = (maxHeight - TOP_OFFSET * referenceScale - lastLineHeight).coerceAtLeast(0.dp)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 32.dp * referenceScale, end = 32.dp * referenceScale,
                top = TOP_OFFSET * referenceScale, bottom = tailSpace),
            modifier = Modifier
                .fillMaxSize()
        ) {
            // index 0: 前奏等待 loading icon
            item(key = "intro") {
                WaitingIndicator(
                    isActive = currentLyricIndex < 0,
                    distance = if (currentLyricIndex < 0) 0 else abs(currentLyricIndex + 1),
                    referenceScale = referenceScale,
                    modifier = Modifier.graphicsLayer { translationY = wave.offset(0) * waveStrength }
                )
            }

            // index 1..n: 歌词行
            itemsIndexed(lines) { index, line ->
                val relativeIndex = if (currentLyricIndex >= 0) index - currentLyricIndex else index + 1

                LyricLineItem(
                    line = line,
                    isCurrent = index == currentLyricIndex,
                    relativeIndex = relativeIndex,
                    positionMs = { displayPosition.longValue },
                    isClearMode = scrollState.isClearMode,
                    referenceScale = referenceScale,
                    modifier = Modifier.graphicsLayer { translationY = wave.offset(index + 1) * waveStrength }
                        .then(if (index == lines.lastIndex) Modifier.onSizeChanged {
                        lastLineHeight = with(density) { it.height.toDp() }
                    } else Modifier),
                    resolveSelection = {
                        val select = canSelectLyric(index - currentLyricIndex,
                            if (scrollState.isLyricsTouching) scrollState.browsingAtTouchStart
                            else scrollState.isClearMode)
                        if (select) scrollState.allowLyricsTouchClarity()
                        select
                    },
                    onClick = { select ->
                        if (!select) {
                            onToggleControls?.invoke()
                        } else {
                            scrollState.onLyricLineClicked()
                            onSeekToLine(index)
                        }
                    }
                )
            }

        }
        PlayerListScrollbar(listState, Modifier.fillMaxSize(), enabled = scrollState.isClearMode)
    }
}

/** 同步／纯文本共用边缘遮罩，控制区显隐时只扩展窗口，不随高度改变淡出厚度。 */
private fun Modifier.lyricsFadingEdges(referenceScale: Float): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (size.height > 0f) {
                val top = (12.dp * referenceScale).toPx().coerceAtMost(size.height / 2f)
                val bottom = (48.dp * referenceScale).toPx().coerceAtMost(size.height / 2f)
                drawRect(Brush.verticalGradient(
                    0f to Color.Transparent, top / size.height to Color.White,
                    1f - bottom / size.height to Color.White, 1f to Color.Transparent
                ), blendMode = BlendMode.DstIn)
            }
        }

/**
 * 前奏等待指示器：三点各自轻微明暗轮替，不缩放整个“更多”图标。
 */
@Composable
private fun WaitingIndicator(
    isActive: Boolean,
    distance: Int,
    referenceScale: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waitingPulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    val targetAlpha = if (isActive) 0.8f else 0.15f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = 200f),
        label = "waitingAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp * referenceScale)
            .then(if (!isActive && distance > 1) Modifier.blur(4.dp * referenceScale) else Modifier)
    ) {
        val radius = (6.dp * referenceScale).toPx()
        val spacing = (22.dp * referenceScale).toPx()
        repeat(3) { dot ->
            val pulse = if (isActive) (.5f + .5f * cos((phase - dot / 3f) * 2f * PI).toFloat()) else 0f
            drawCircle(Color.White.copy(alpha = animatedAlpha * (.3f + .7f * pulse)),
                radius, Offset(radius + dot * spacing, size.height / 2f))
        }
    }
}

/**
 * 单行歌词
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    isCurrent: Boolean,
    relativeIndex: Int,
    positionMs: () -> Long,
    isClearMode: Boolean = false,
    referenceScale: Float,
    modifier: Modifier = Modifier,
    resolveSelection: () -> Boolean,
    onClick: (Boolean) -> Unit
) {
    val press = remember { Animatable(0f) }
    val highlight = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var feedbackJob by remember { mutableStateOf<Job?>(null) }
    val latestResolve by rememberUpdatedState(resolveSelection)
    val latestClick by rememberUpdatedState(onClick)
    val targetAlpha = LyricsPresentationMotion.alpha(relativeIndex, isClearMode)
    val targetBlur = LyricsPresentationMotion.blurDp(relativeIndex, isClearMode).dp
    val karaokeWeight by animateFloatAsState(if (isCurrent) 1f else 0f,
        tween(180), label = "lyricKaraokeWeight")

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(180),
        label = "lyricAlpha"
    )
    val animatedBlur by animateDpAsState(
        targetValue = targetBlur,
        animationSpec = tween(180),
        label = "lyricBlur"
    )

    LyricsKaraokeText(
        line = line,
        alpha = animatedAlpha + (1f - animatedAlpha) * highlight.value,
        karaokeWeight = karaokeWeight,
        positionMs = positionMs,
        referenceScale = referenceScale,
        style = TextStyle(fontSize = LYRIC_FONT_SIZE * referenceScale,
            fontWeight = FontWeight.ExtraBold, lineHeight = LYRIC_LINE_HEIGHT * referenceScale,
            textAlign = TextAlign.Start),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp * referenceScale)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val select = latestResolve()
                    feedbackJob?.cancel()
                    if (select) feedbackJob = scope.launch {
                        coroutineScope {
                            launch { press.animateTo(1f, tween(120)) }
                            launch { highlight.animateTo(1f, tween(80)) }
                        }
                    }
                    val released = tryAwaitRelease()
                    if (released) latestClick(select)
                    feedbackJob?.cancel()
                    feedbackJob = scope.launch {
                        // 快速点击也保留一次短按压，但选播不等待视觉反馈完成。
                        if (select && released) {
                            coroutineScope {
                                launch { press.animateTo(1f, tween(((1f - press.value) * 120).toInt())) }
                                launch { highlight.animateTo(1f, tween(40)) }
                            }
                        }
                        coroutineScope {
                            launch { press.animateTo(0f, tween(if (released) 300 else 180)) }
                            launch { highlight.animateTo(0f, tween(180)) }
                        }
                    }
                })
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick { latestClick(latestResolve()); true }
            }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp &&
                    it.key in listOf(Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter)) {
                    latestClick(latestResolve())
                    true
                } else false
            }
            .focusable()
            .graphicsLayer {
                val amount = 1f - .05f * press.value
                scaleX = amount
                scaleY = amount
            }
            .drawBehind {
                // 整句共用圆角底光；不是逐字背景，也不引入玻璃或阴影。
                drawRoundRect(Color.White.copy(alpha = .10f * highlight.value),
                    topLeft = Offset(-8.dp.toPx() * referenceScale, -4.dp.toPx() * referenceScale),
                    size = androidx.compose.ui.geometry.Size(size.width + 16.dp.toPx() * referenceScale,
                        size.height + 8.dp.toPx() * referenceScale),
                    cornerRadius = CornerRadius(12.dp.toPx() * referenceScale))
            }
            .then(
                if (animatedBlur > 0.dp) Modifier.blur(animatedBlur * referenceScale, BlurredEdgeTreatment.Unbounded)
                else Modifier
            )
            .graphicsLayer {
                // 复用当前句的过渡进度，只变换字形层；布局、换行、模糊半径不随之变化。
                transformOrigin = TransformOrigin(0f, 0.5f)
                val textScale = LyricsPresentationMotion.textScale(karaokeWeight)
                scaleX = textScale
                scaleY = textScale
            }
    )
}
