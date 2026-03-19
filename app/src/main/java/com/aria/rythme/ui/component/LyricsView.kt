package com.aria.rythme.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Apple Music 风格歌词视图
 *
 * @param isFullScreen controls 是否已隐藏（全屏模式）
 * @param onToggleControls 全屏模式下点击非相邻行时恢复 controls
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
    onUserScrolling: ((Boolean) -> Unit)? = null
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
            PlainLyricsView(plainText = lyricsData.plainText, modifier = modifier)
        }
        lyricsData != null && lyricsData.lines.isNotEmpty() -> {
            SyncedLyricsView(
                lyricsData = lyricsData,
                currentLyricIndex = currentLyricIndex,
                onSeekToLine = onSeekToLine,
                isFullScreen = isFullScreen,
                onToggleControls = onToggleControls,
                onUserScrolling = onUserScrolling,
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
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lines = remember(plainText) { plainText.lines().filter { it.isNotBlank() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.08f to Color.White
                    ),
                    blendMode = BlendMode.DstIn
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        0.92f to Color.White,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 48.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lines) { _, line ->
                Text(
                    text = line,
                    color = Color(0xB3FFFFFF),
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * 同步歌词视图（Apple Music 风格）
 */
@Composable
private fun SyncedLyricsView(
    lyricsData: LyricsData,
    currentLyricIndex: Int,
    onSeekToLine: (Int) -> Unit,
    isFullScreen: Boolean,
    onToggleControls: (() -> Unit)?,
    onUserScrolling: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lines = lyricsData.lines

    // 当前行固定在距容器顶部 56dp
    val topOffsetPx = with(density) { 32.dp.roundToPx() }

    // 手动滚动状态机
    var isManualScrolling by remember { mutableStateOf(false) }
    var autoFollowResumeJob by remember { mutableStateOf<Job?>(null) }

    // 拖动时取消模糊/alpha，松手后 5s 或歌词行切换时恢复
    var isClearMode by remember { mutableStateOf(false) }
    var clearModeResumeJob by remember { mutableStateOf<Job?>(null) }

    // 标记程序触发的滚动，用于区分用户手动滚动
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // 监听 LazyColumn 滚动状态，排除程序触发的滚动
    val isScrollInProgress = listState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress && !isProgrammaticScroll) {
            // 用户手动滚动开始
            isManualScrolling = true
            isClearMode = true
            autoFollowResumeJob?.cancel()
            clearModeResumeJob?.cancel()
        } else if (!isScrollInProgress && isManualScrolling) {
            // 滚动停止，启动恢复计时
            autoFollowResumeJob?.cancel()
            autoFollowResumeJob = scope.launch {
                delay(3000L)
                isManualScrolling = false
            }
            clearModeResumeJob?.cancel()
            clearModeResumeJob = scope.launch {
                delay(5000L)
                isClearMode = false
            }
        }
    }

    // 检测用户滚动方向：向前（上）显示 Controls，向后（下）隐藏 Controls
    LaunchedEffect(Unit) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            if (isManualScrolling) {
                val scrollingDown = index > prevIndex || (index == prevIndex && offset > prevOffset)
                val scrollingUp = index < prevIndex || (index == prevIndex && offset < prevOffset)
                if (scrollingUp) onUserScrolling?.invoke(false)
                else if (scrollingDown) onUserScrolling?.invoke(true)
            }
            prevIndex = index
            prevOffset = offset
        }
    }

    // 歌词行切换时，如果不在手动滚动中则立即恢复模糊
    LaunchedEffect(currentLyricIndex) {
        if (isClearMode && !isManualScrolling) {
            isClearMode = false
            clearModeResumeJob?.cancel()
        }
    }

    // 自动滚动：当前行固定在距顶部指定偏移
    LaunchedEffect(currentLyricIndex, isManualScrolling) {
        if (!isManualScrolling) {
            val targetItem = if (currentLyricIndex < 0) 0 else currentLyricIndex + 1
            isProgrammaticScroll = true

            // 先确保目标 item 在可见范围附近（无动画快速定位到大致位置）
            val layoutInfo = listState.layoutInfo
            val targetVisible = layoutInfo.visibleItemsInfo.any { it.index == targetItem }
            if (!targetVisible) {
                // 目标不在视口内（如快速切歌），直接跳转
                listState.scrollToItem(targetItem, scrollOffset = -topOffsetPx)
            } else {
                // 目标在视口内，计算精确偏移量用平滑动画滚动
                val currentItemInfo = layoutInfo.visibleItemsInfo.find { it.index == targetItem }
                if (currentItemInfo != null) {
                    val currentOffset = currentItemInfo.offset
                    val desiredOffset = topOffsetPx
                    val scrollDelta = (currentOffset - desiredOffset).toFloat()
                    listState.animateScrollBy(
                        value = scrollDelta,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                } else {
                    listState.animateScrollToItem(targetItem, scrollOffset = -topOffsetPx)
                }
            }

            isProgrammaticScroll = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.06f to Color.White
                    ),
                    blendMode = BlendMode.DstIn
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        0.92f to Color.White,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 32.dp),
            modifier = Modifier
                .fillMaxSize()
        ) {
            // index 0: 前奏等待 loading icon
            item {
                WaitingIndicator(
                    isActive = currentLyricIndex < 0,
                    distance = if (currentLyricIndex < 0) 0 else abs(currentLyricIndex + 1) // 距离当前行
                )
            }

            // index 1..n: 歌词行
            itemsIndexed(lines) { index, line ->
                val distance = if (currentLyricIndex >= 0) abs(index - currentLyricIndex) else index + 1

                LyricLineItem(
                    text = line.text,
                    isCurrent = index == currentLyricIndex,
                    distance = distance,
                    isClearMode = isClearMode,
                    onClick = {
                        if (isFullScreen && !isManualScrolling && distance > 1) {
                            // 全屏自动滚动模式：非相邻行点击恢复 controls
                            onToggleControls?.invoke()
                        } else {
                            onSeekToLine(index)
                            isManualScrolling = false
                            autoFollowResumeJob?.cancel()
                        }
                    }
                )
            }

            // 底部填充：确保最后几行也能滚动到顶部固定位置
            item {
                Spacer(modifier = Modifier.fillParentMaxHeight(0.85f))
            }
        }
    }
}

/**
 * 前奏等待指示器：ic_more 图标 + 呼吸缩放动画
 */
@Composable
private fun WaitingIndicator(
    isActive: Boolean,
    distance: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waitingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val targetAlpha = if (isActive) 0.8f else 0.15f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = 200f),
        label = "waitingAlpha"
    )

    val scale = if (isActive) pulseScale else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    alpha = animatedAlpha
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (!isActive && distance > 1) Modifier.blur(4.dp)
                    else Modifier
                )
        )
    }
}

/**
 * 单行歌词
 */
@Composable
private fun LyricLineItem(
    text: String,
    isCurrent: Boolean,
    distance: Int,
    isClearMode: Boolean = false,
    onClick: () -> Unit
) {
    val targetAlpha = if (isClearMode) 1f else when {
        isCurrent -> 1f
        distance == 1 -> 0.45f
        distance == 3 -> 0.35f
        else -> 0.25f
    }

    val targetBlur = if (isClearMode) 0.dp else when {
        isCurrent -> 0.dp
        distance == 1 -> 2.dp
        distance <= 3 -> 4.dp
        else -> 6.dp
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = 200f),
        label = "lyricAlpha"
    )
    val animatedBlur by animateDpAsState(
        targetValue = targetBlur,
        animationSpec = spring(stiffness = 200f),
        label = "lyricBlur"
    )

    Text(
        text = text,
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 44.sp,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .then(
                if (animatedBlur > 0.dp) Modifier.blur(animatedBlur)
                else Modifier
            )
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
    )
}
