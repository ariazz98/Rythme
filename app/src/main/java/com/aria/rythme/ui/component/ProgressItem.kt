@file:OptIn(ExperimentalMaterial3Api::class)

package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.core.utils.formatLeftTime
import com.aria.rythme.core.utils.formatPosition
import com.kyant.capsule.ContinuousCapsule
@Composable
fun ProgressItem(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    enabled: Boolean = false,
    onSeek: (Float) -> Unit
) {

    var sliderPosition by remember { mutableFloatStateOf(progress) }

    var isDragging by remember { mutableStateOf(false) }

    // 松手后等待状态更新期间，记住估算位置
    var isSeeking by remember { mutableStateOf(false) }
    var seekDisplayPosition by remember { mutableLongStateOf(0L) }

    // currentPosition 更新后说明 seek 已生效
    LaunchedEffect(currentPosition) {
        isSeeking = false
    }

    val barPadding by animateDpAsState(
        targetValue = if (isDragging) 24.dp else 32.dp,
        label = "barPadding"
    )

    val textTopPadding by animateDpAsState(
        targetValue = if (isDragging) 14.dp else 9.dp,
        label = "textPadding"
    )

    val textSize by animateIntAsState(
        targetValue = if (isDragging) 11 else 10,
        label = "textSize"
    )

    val textColor = if (isDragging) Color(0xFFFFFFFF) else Color(0x33FFFFFF)

    val displayPosition = when {
        isDragging -> (sliderPosition * duration).toLong()
        isSeeking -> seekDisplayPosition
        else -> currentPosition
    }

    Column(Modifier.fillMaxWidth().height(45.dp)) {

        RythmeDraggableProgressBar(
            enabled = enabled,
            progress = sliderPosition,
            onProgressChange = { newValue ->
                sliderPosition = newValue
            },
            onProgressChangeFinished = {
                seekDisplayPosition = (sliderPosition * duration).toLong()
                isSeeking = true
                onSeek(sliderPosition)
            },
            onDragStateChange = {
                isDragging = it
            },
        )

        // 时间显示
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = textTopPadding),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (enabled) formatPosition(displayPosition) else "--:--",
                fontSize = textSize.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(start = barPadding)
            )

            Text(
                text = if (enabled) formatLeftTime(displayPosition, duration) else "--:--",
                fontSize = textSize.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(end = barPadding)
            )
        }
    }

    // 同步进度
    LaunchedEffect(progress) {
        sliderPosition = progress
    }
}

@Composable
fun VoiceItem(
    modifier: Modifier = Modifier,
    progress: Float,
    onSeek: (Float) -> Unit
) {

    var sliderPosition by remember { mutableFloatStateOf(progress) }

    var isDragging by remember { mutableStateOf(false) }

    val barPadding by animateDpAsState(
        targetValue = if (isDragging) 28.dp else 36.dp,
        label = "barPadding"
    )

    val iconColor = if (isDragging) Color(0xFFFFFFFF) else Color(0x66FFFFFF)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(horizontal = barPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(R.drawable.ic_voice_down),
            contentDescription = "",
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        RythmeDraggableProgressBarNoPadding(
            modifier = Modifier.weight(1f),
            progress = sliderPosition,
            onProgressChange = { newValue ->
                onSeek(newValue)
            },
            onProgressChangeFinished = { newValue ->
                onSeek(newValue)
            },
            onDragStateChange = { newValue ->
                isDragging = newValue
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            painter = painterResource(R.drawable.ic_voice_up),
            contentDescription = "",
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )

    }

    // 同步进度
    LaunchedEffect(progress) {
        sliderPosition = progress
    }
}

/**
 * Rythme 可拖动进度条
 *
 * 样式特点：
 * - 高度：6dp
 * - 圆角：3dp
 * - 进度条背景色：Color(0x33FFFFFF)
 * - 进度颜色：Color(0x80FFFFFF)
 * - 进度为矩形，但限制在圆角轨道内
 * - 支持拖动，实时回调进度
 *
 * @param progress 当前进度（0.0-1.0）
 * @param onProgressChange 拖动时实时回调当前进度
 * @param onProgressChangeFinished 拖动结束时回调最终进度
 * @param enabled 是否启用拖动，默认 true
 */
@Composable
fun RythmeDraggableProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    // 内部拖动进度状态
    var dragProgress by remember { mutableFloatStateOf(progress) }
    // 是否正在拖动
    var isDragging by remember { mutableStateOf(false) }
    // 拖动开始时的初始进度
    var dragStartProgress by remember { mutableFloatStateOf(0f) }
    // 拖动开始时的X位置
    var dragStartX by remember { mutableFloatStateOf(0f) }

    // 同步外部 progress 到 dragProgress（仅在非拖动状态）
    LaunchedEffect(progress, isDragging) {
        if (!isDragging) {
            dragProgress = progress
        }
    }

    // 根据拖动状态决定高度和颜色
    val barHeight by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 6.dp,
        label = "barHeight"
    )
    val barPadding by animateDpAsState(
        targetValue = if (isDragging) 24.dp else 32.dp,
        label = "barPadding"
    )
    val trackColor = if (isDragging) Color(0x4DFFFFFF) else Color(0x33FFFFFF)
    val progressColor = if (isDragging) Color(0xFFFFFFFF) else Color(0x80FFFFFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .padding(horizontal = barPadding),
        contentAlignment = Alignment.Center // 中心对齐
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(ContinuousCapsule)
                .pointerInput(enabled) {
                    if (enabled) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                when {
                                    // 按下事件
                                    event.changes.any { it.pressed && it.previousPressed.not() } -> {
                                        val change = event.changes.first { it.pressed && it.previousPressed.not() }
                                        isDragging = true
                                        dragStartProgress = dragProgress
                                        dragStartX = change.position.x
                                        onDragStateChange(true)
                                    }
                                    // 抬起事件
                                    event.changes.any { it.pressed.not() && it.previousPressed } -> {
                                        if (isDragging) {
                                            isDragging = false
                                            onProgressChangeFinished(dragProgress)
                                            onDragStateChange(false)
                                        }
                                    }
                                    // 拖动事件
                                    isDragging && event.changes.any { it.pressed } -> {
                                        val change = event.changes.first { it.pressed }
                                        change.consume()
                                        // 根据拖动距离计算进度增量
                                        val dragDistance = change.position.x - dragStartX
                                        val progressDelta = dragDistance / size.width
                                        val newProgress = (dragStartProgress + progressDelta).coerceIn(0f, 1f)
                                        dragProgress = newProgress
                                        onProgressChange(newProgress)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // 绘制进度条背景（带圆角）
            drawRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(width, height)
            )

            // 计算进度宽度
            val progressWidth = width * dragProgress.coerceIn(0f, 1f)

            if (progressWidth > 0f) {
                // 绘制进度矩形
                drawRect(
                    color = progressColor,
                    topLeft = Offset.Zero,
                    size = Size(progressWidth, height)
                )
            }
        }
    }
}


@Composable
fun RythmeDraggableProgressBarNoPadding(
    modifier: Modifier = Modifier,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    // 内部拖动进度状态
    var dragProgress by remember { mutableFloatStateOf(progress) }
    // 是否正在拖动
    var isDragging by remember { mutableStateOf(false) }
    // 拖动开始时的初始进度
    var dragStartProgress by remember { mutableFloatStateOf(0f) }
    // 拖动开始时的X位置
    var dragStartX by remember { mutableFloatStateOf(0f) }

    // 同步外部 progress 到 dragProgress（仅在非拖动状态）
    LaunchedEffect(progress, isDragging) {
        if (!isDragging) {
            dragProgress = progress
        }
    }

    // 根据拖动状态决定高度和颜色
    val barHeight by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 6.dp,
        label = "barHeight"
    )
    val trackColor = if (isDragging) Color(0x4DFFFFFF) else Color(0x33FFFFFF)
    val progressColor = if (isDragging) Color(0xFFFFFFFF) else Color(0x80FFFFFF)

    Box(
        modifier = modifier
            .height(16.dp),
        contentAlignment = Alignment.Center // 中心对齐
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(ContinuousCapsule)
                .pointerInput(enabled) {
                    if (enabled) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                when {
                                    // 按下事件
                                    event.changes.any { it.pressed && it.previousPressed.not() } -> {
                                        val change = event.changes.first { it.pressed && it.previousPressed.not() }
                                        isDragging = true
                                        dragStartProgress = dragProgress
                                        dragStartX = change.position.x
                                        onDragStateChange(true)
                                    }
                                    // 抬起事件
                                    event.changes.any { it.pressed.not() && it.previousPressed } -> {
                                        if (isDragging) {
                                            isDragging = false
                                            onProgressChangeFinished(dragProgress)
                                            onDragStateChange(false)
                                        }
                                    }
                                    // 拖动事件
                                    isDragging && event.changes.any { it.pressed } -> {
                                        val change = event.changes.first { it.pressed }
                                        change.consume()
                                        // 根据拖动距离计算进度增量
                                        val dragDistance = change.position.x - dragStartX
                                        val progressDelta = dragDistance / size.width
                                        val newProgress = (dragStartProgress + progressDelta).coerceIn(0f, 1f)
                                        dragProgress = newProgress
                                        onProgressChange(newProgress)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // 绘制进度条背景（带圆角）
            drawRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(width, height)
            )

            // 计算进度宽度
            val progressWidth = width * dragProgress.coerceIn(0f, 1f)

            if (progressWidth > 0f) {
                // 绘制进度矩形
                drawRect(
                    color = progressColor,
                    topLeft = Offset.Zero,
                    size = Size(progressWidth, height)
                )
            }
        }
    }
}