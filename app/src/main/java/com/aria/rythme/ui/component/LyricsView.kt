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
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import kotlin.math.abs

// 滚动动画时长
private const val SCROLL_ANIMATION_DURATION_MS = 400

// 当前行固定在距容器顶部的偏移
private val TOP_OFFSET = 32.dp

// 歌词字体
private val LYRIC_FONT_SIZE = 32.sp
private val LYRIC_LINE_HEIGHT = 44.sp

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
    val scrollState = rememberLyricsScrollState()
    val density = LocalDensity.current
    val lines = lyricsData.lines

    val topOffsetPx = with(density) { TOP_OFFSET.roundToPx() }

    // LE#1: 滚动检测 + 方向 + 定时恢复
    LaunchedEffect(Unit) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }.collect { (isScrolling, index, offset) ->
            if (isScrolling && !scrollState.isProgrammaticScroll) {
                // 用户手动滚动中
                if (scrollState.isAutoFollow || scrollState.mode is LyricsScrollMode.WaitingToResume) {
                    scrollState.onUserScrollStart()
                }
                // 检测滚动方向
                val scrollingDown = index > prevIndex || (index == prevIndex && offset > prevOffset)
                val scrollingUp = index < prevIndex || (index == prevIndex && offset < prevOffset)
                if (scrollingUp) onUserScrolling?.invoke(false)
                else if (scrollingDown) onUserScrolling?.invoke(true)
            } else if (!isScrolling && scrollState.mode is LyricsScrollMode.ManualScrolling) {
                // 滚动停止，进入等待恢复
                scrollState.onUserScrollStop(System.currentTimeMillis())
            }
            prevIndex = index
            prevOffset = offset
        }
    }

    // WaitingToResume 定时恢复
    val mode = scrollState.mode
    if (mode is LyricsScrollMode.WaitingToResume) {
        LaunchedEffect(mode) {
            val now = System.currentTimeMillis()
            val waitMs = (mode.followResumeTimeMs - now).coerceAtLeast(0)
            delay(waitMs)
            scrollState.onResumeTimerFired()
        }
    }

    // LE#2: 自动滚动 + 歌词行变更时恢复模糊
    LaunchedEffect(currentLyricIndex, scrollState.isAutoFollow) {
        // 歌词行变更时，如果在等待状态则立即恢复
        scrollState.onLyricIndexChanged()

        if (scrollState.isAutoFollow) {
            val targetItem = if (currentLyricIndex < 0) 0 else currentLyricIndex + 1
            scrollState.isProgrammaticScroll = true

            val layoutInfo = listState.layoutInfo
            val targetVisible = layoutInfo.visibleItemsInfo.any { it.index == targetItem }
            if (!targetVisible) {
                listState.scrollToItem(targetItem, scrollOffset = -topOffsetPx)
            } else {
                val currentItemInfo = layoutInfo.visibleItemsInfo.find { it.index == targetItem }
                if (currentItemInfo != null) {
                    val scrollDelta = (currentItemInfo.offset - topOffsetPx).toFloat()
                    listState.animateScrollBy(
                        value = scrollDelta,
                        animationSpec = tween(
                            durationMillis = SCROLL_ANIMATION_DURATION_MS,
                            easing = FastOutSlowInEasing
                        )
                    )
                } else {
                    listState.animateScrollToItem(targetItem, scrollOffset = -topOffsetPx)
                }
            }

            scrollState.isProgrammaticScroll = false
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
                    distance = if (currentLyricIndex < 0) 0 else abs(currentLyricIndex + 1)
                )
            }

            // index 1..n: 歌词行
            itemsIndexed(lines) { index, line ->
                val distance = if (currentLyricIndex >= 0) abs(index - currentLyricIndex) else index + 1

                LyricLineItem(
                    text = line.text,
                    isCurrent = index == currentLyricIndex,
                    distance = distance,
                    isClearMode = scrollState.isClearMode,
                    onClick = {
                        if (isFullScreen && scrollState.isAutoFollow && distance > 1) {
                            onToggleControls?.invoke()
                        } else {
                            onSeekToLine(index)
                            scrollState.onLyricLineClicked()
                        }
                    }
                )
            }

            // 底部填充
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
    val targetAlpha = when {
        isCurrent -> 1f
        distance == 1 -> 0.55f
        distance == 3 -> 0.45f
        else -> 0.35f
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
        color = if (isCurrent) Color.White else Color.White.copy(alpha = animatedAlpha),
        fontSize = LYRIC_FONT_SIZE,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = LYRIC_LINE_HEIGHT,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .graphicsLayer {
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
