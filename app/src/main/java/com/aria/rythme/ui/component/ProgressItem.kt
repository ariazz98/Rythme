package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.core.utils.formatLeftTime
import com.aria.rythme.core.utils.formatPosition
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun ProgressItem(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    enabled: Boolean = false,
    emptyReferenceScale: Float = 1f,
    centerLabel: @Composable () -> Unit = {},
    onSeek: (Float) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekDisplayPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(currentPosition) { isSeeking = false }
    // 没有位置变更回调（例如原地松手）时，也不能永久停留在预览状态。
    LaunchedEffect(isSeeking) {
        if (isSeeking) { delay(750); isSeeking = false }
    }
    LaunchedEffect(progress, isDragging, isSeeking) {
        if (!isDragging && !isSeeking) sliderPosition = progress
    }

    val scale = emptyReferenceScale
    val barPadding by animateDpAsState(if (isDragging) 24.dp * scale else 32.dp * scale)
    val textTopPadding by animateDpAsState(if (isDragging) 14.dp * scale else 6.dp * scale)
    val textColor = Color.White.copy(alpha = if (isDragging) 1f else .4f)
    val displayPosition = when {
        isDragging -> (sliderPosition * duration).toLong()
        isSeeking -> seekDisplayPosition
        else -> currentPosition
    }
    Column(Modifier.fillMaxWidth().height(45.dp)) {
        PlayerSliderTrack(
            progress = sliderPosition,
            enabled = enabled,
            horizontalPadding = 32.dp * scale,
            pressedPadding = 24.dp * scale,
            onProgressChange = { sliderPosition = it },
            onProgressChangeFinished = {
                sliderPosition = it
                seekDisplayPosition = (it * duration).toLong()
                isSeeking = true
                onSeek(it)
            },
            onDragStateChange = { isDragging = it }
        )
        Box(Modifier.fillMaxWidth().padding(top = textTopPadding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = barPadding),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (remaining in listOf(false, true)) {
                Text(
                    text = if (!enabled) "––:––" else if (remaining)
                        formatLeftTime(displayPosition, duration) else formatPosition(displayPosition),
                    fontSize = ((if (enabled) 12f else 11f) * scale).sp,
                    letterSpacing = (if (enabled) 0f else 1.9f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
        Box(Modifier.align(Alignment.TopCenter)) { centerLabel() }
        }
    }
}

@Composable
fun VoiceItem(
    modifier: Modifier = Modifier,
    progress: Float,
    horizontalPadding: Dp = 36.dp,
    onSeek: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    val padding by animateDpAsState(if (isDragging) horizontalPadding - 8.dp else horizontalPadding)
    val iconColor = Color.White.copy(alpha = if (isDragging) 1f else .65f)
    Row(
        modifier.fillMaxWidth().height(20.dp).padding(horizontal = padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_voice_down), "低音量", tint = iconColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(12.dp))
        PlayerSliderTrack(
            modifier = Modifier.weight(1f),
            progress = progress,
            onProgressChange = onSeek,
            onProgressChangeFinished = onSeek,
            onDragStateChange = { isDragging = it }
        )
        Spacer(Modifier.width(12.dp))
        Icon(painterResource(R.drawable.ic_voice_up), "高音量", tint = iconColor, modifier = Modifier.size(20.dp))
    }
}

/** 相对拖动基于按下时的轨道宽度，不随轨道变宽或系统音量量化回调跳动。 */
internal fun sliderDragProgress(start: Float, delta: Float, width: Float): Float =
    if (width > 0f && width.isFinite() && delta.isFinite())
        (start + delta / width).coerceIn(0f, 1f) else start.coerceIn(0f, 1f)

@Composable
internal fun PlayerSliderTrack(
    progress: Float,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    horizontalPadding: Dp = 0.dp,
    pressedPadding: Dp = horizontalPadding,
    trackColor: Color = Color.White,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var preview by remember { mutableFloatStateOf(progress) }
    var trackOrigin by remember { mutableStateOf(Offset.Zero) }
    val latestProgress by rememberUpdatedState(progress)
    val changeProgress by rememberUpdatedState(onProgressChange)
    val finishProgress by rememberUpdatedState(onProgressChangeFinished)
    val changeDragging by rememberUpdatedState(onDragStateChange)
    val padding by animateDpAsState(if (dragging) pressedPadding else horizontalPadding)
    val height by animateDpAsState(if (dragging) 16.dp else 6.dp)
    val value = if (dragging) preview else progress
    Box(
        modifier.height(16.dp).onGloballyPositioned { trackOrigin = it.positionInRoot() }.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                val origin = down.position + trackOrigin
                val start = latestProgress
                val width = (size.width - 2 * padding.toPx()).coerceAtLeast(1f)
                var horizontal = false
                preview = start
                dragging = true
                changeDragging(true)
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed) break
                        if (!change.pressed) {
                            finishProgress(preview)
                            break
                        }
                        val delta = change.position + trackOrigin - origin
                        val dx = delta.x
                        val dy = delta.y
                        if (!horizontal) {
                            if (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx)) break
                            horizontal = abs(dx) > viewConfiguration.touchSlop
                        }
                        if (horizontal) {
                            change.consume()
                            preview = sliderDragProgress(start, dx, width)
                            changeProgress(preview)
                        }
                    }
                } finally {
                    dragging = false
                    changeDragging(false)
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxWidth().padding(horizontal = padding).height(height).clip(ContinuousCapsule)) {
            drawRect(trackColor.copy(alpha = if (dragging || !enabled) .3f else .25f))
            drawRect(
                trackColor.copy(alpha = if (dragging) 1f else .65f),
                topLeft = Offset.Zero,
                size = Size(size.width * value.coerceIn(0f, 1f), size.height)
            )
        }
    }
}
