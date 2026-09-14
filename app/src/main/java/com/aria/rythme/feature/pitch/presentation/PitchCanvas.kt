package com.aria.rythme.feature.pitch.presentation

import com.aria.rythme.feature.pitch.data.PitchHistory
import android.graphics.Paint
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import kotlin.math.abs
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.translate
import com.aria.rythme.feature.pitch.data.DetectedPitch
import kotlinx.coroutines.isActive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.ui.component.GlassBackdropSurface
import com.aria.rythme.ui.component.glassLens
import com.aria.rythme.ui.component.TopBarPressState
import com.aria.rythme.ui.component.TopBarPressMotion
import com.aria.rythme.ui.component.TopBarPressHeadroom
import com.aria.rythme.ui.component.LocalGlassHdr
import com.aria.rythme.ui.component.rememberTopBarPressHighlight
import com.aria.rythme.ui.component.topBarPress
import com.aria.rythme.ui.component.topBarPressBodyReflection
import com.aria.rythme.ui.component.topBarForegroundLight
import com.aria.rythme.ui.component.glassHdrFadeAndBlur
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

internal data class ReferencePlayback(
    val positionMs: Long,
    val totalMs: Long,
    val playing: Boolean,
    val currentPosition: () -> Long,
    val frames: PitchHistory,
    val visible: Boolean = true
)

internal data class PracticeTimelineGestures(
    val begin: () -> Unit,
    val preview: (Long) -> Unit,
    val finish: (Boolean) -> Unit
)

private fun referenceWindowEnd(position: Long, total: Long): Double =
    position.coerceIn(0, total + 1_000).toDouble()

@Composable
internal fun PitchCanvas(
    state: PitchState,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    reference: ReferencePlayback? = null,
    showControls: Boolean = true,
    freeModeControls: Boolean = false,
    onSelectStart: (Long) -> Unit = {},
    olderTakes: List<PitchHistory> = emptyList(),
    timelineGestures: PracticeTimelineGestures? = null,
    bottomControls: (@Composable (Backdrop) -> Unit)? = null
) {
    val colors = MaterialTheme.rythmeColors
    val density = LocalDensity.current
    // 只采样画布本身，悬浮玻璃不能把自己递归录进 backdrop。
    val backdrop = rememberLayerBackdrop()
    val latestState by rememberUpdatedState(state)
    val latestReference by rememberUpdatedState(reference)
    val latestGestures by rememberUpdatedState(timelineGestures)
    var historyEndMs by rememberSaveable(state.sessionId) { mutableStateOf<Double?>(null) }
    val live = historyEndMs == null
    val viewEndMs = historyEndMs ?: reference?.let { referenceWindowEnd(it.positionMs, it.totalMs) } ?: state.durationMs.toDouble()
    val visibleFrames = state.frames.window((viewEndMs - PitchViewModel.HISTORY_MS - 256).toLong(), (viewEndMs + 256).toLong())
    val referenceFrames = if (reference?.visible == true) reference.frames.window(
        (pitchWindowStart(viewEndMs) - 256).toLong(), (pitchWindowStart(viewEndMs) + PitchViewModel.HISTORY_MS + 256).toLong()) else emptyList()
    val renderClock = remember(state.sessionId, state.listening) { PitchRenderClock() }
    val frameNanos = remember { mutableLongStateOf(0L) }
    SideEffect {
        state.frames.lastOrNull()?.takeIf { it.capturedUntilMs > state.segmentStartMs }?.let { renderClock.accept(it.timeMs, System.nanoTime()) }
    }
    LaunchedEffect(state.listening, reference?.playing, live, enabled) {
        if (enabled && ((state.listening && live) || reference?.playing == true)) while (isActive) withFrameNanos { frameNanos.longValue = it }
    }
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    var readoutHeight by remember { mutableIntStateOf(0) }
    var controlsHeight by remember { mutableIntStateOf(0) }
    var topMidi by rememberSaveable(state.sessionId) { mutableFloatStateOf(76f) }
    var dragging by remember { mutableStateOf(false) }
    val followScope = rememberCoroutineScope()
    var followJob by remember { mutableStateOf<Job?>(null) }
    var followTarget by remember { mutableStateOf<Float?>(null) }
    var followVelocity by remember { mutableFloatStateOf(0f) }
    fun interruptFollow() {
        followJob?.cancel()
        followJob = null
        followTarget = null
        followVelocity = 0f
    }
    val rowPx = with(density) { maxOf(19.dp.toPx(), 16.sp.toPx()) }
    val span = canvasHeight / rowPx
    val recentFrames = if (state.frames.isEmpty() && reference?.visible == true) reference.frames.window(reference.positionMs - 2_000, reference.positionMs + 2_000) else state.frames.window(state.durationMs - 2_000, state.durationMs)
    val latestVoiced = recentFrames.lastOrNull { it.pitch != null }
    val lastVoicedTime = latestVoiced?.timeMs
    val topInset = with(density) { 16.dp.toPx() }
    val bottomInset = with(density) { 32.dp.toPx() }
    val margin = with(density) { 16.dp.toPx() }
    // 由真实控件高度确定避让区域；默认字号与大字号用同一套坐标。
    val safeTop = readoutHeight + topInset + margin
    val safeBottom = canvasHeight - controlsHeight - bottomInset - margin
    LaunchedEffect(lastVoicedTime, canvasHeight, readoutHeight, controlsHeight, rowPx, live) {
        // 拖动松手本身不回弹，只响应后续输入或窗口尺寸改变。
        if (!dragging && live && latestVoiced != null && canvasHeight > 0) {
            val recent = recentFrames.mapNotNull { it.pitch?.midiNote }
            // 从已确定的目的地判断是否还需要移动，避免每个采样都把同一动画重启。
            val target = followPitchTop(followTarget ?: topMidi, recent, rowPx, safeTop, safeBottom, span)
            if (target != (followTarget ?: topMidi)) {
                followJob?.cancel()
                followTarget = target
                followJob = followScope.launch {
                    // 临界阻尼，换目标时承接当前位置和速度，不跳位、不反复从静止起步。
                    animate(
                        initialValue = topMidi,
                        targetValue = target,
                        initialVelocity = followVelocity,
                        animationSpec = spring(dampingRatio = 1f, stiffness = 400f, visibilityThreshold = 0.005f)
                    ) { value, velocity ->
                        topMidi = clampPitchTop(value, span)
                        followVelocity = velocity
                    }
                    followVelocity = 0f
                }
            }
        }
    }
    LaunchedEffect(span) {
        if (span > 0) {
            interruptFollow()
            topMidi = clampPitchTop(topMidi, span)
        }
    }
    LaunchedEffect(enabled, state.sessionId, live) { if (!enabled || !live || state.frames.isEmpty()) interruptFollow() }
    val pixelsPerMs = (canvasWidth * 0.82f - with(density) { 47.dp.toPx() }).coerceAtLeast(1f) / PitchViewModel.HISTORY_MS
    val dragModifier = Modifier.pointerInput(state.sessionId, rowPx, span, pixelsPerMs) {
        var horizontal: Boolean? = null
        var seekPosition = 0.0
        try {
            detectDragGestures(
                onDragStart = { interruptFollow(); dragging = true; horizontal = null; seekPosition = latestReference?.positionMs?.toDouble() ?: 0.0 },
                onDragEnd = { dragging = false; if (horizontal == true) latestGestures?.finish?.invoke(false) },
                onDragCancel = { dragging = false; if (horizontal == true) latestGestures?.finish?.invoke(true) }
            ) { change, amount ->
                change.consume()
                if (horizontal == null) {
                    horizontal = abs(amount.x) > abs(amount.y)
                    if (horizontal == true) latestGestures?.begin?.invoke()
                }
                if (horizontal == true && latestGestures != null) {
                    seekPosition = (seekPosition - amount.x / pixelsPerMs).coerceIn(0.0, latestReference?.totalMs?.toDouble() ?: 0.0)
                    latestGestures?.preview?.invoke(seekPosition.toLong())
                } else if (horizontal == true) {
                    val duration = maxOf(latestReference?.totalMs ?: 0, latestState.durationMs).toDouble()
                    if (duration > HISTORY_WINDOW) historyEndMs = clampHistoryEnd((historyEndMs ?: latestReference?.let { referenceWindowEnd(it.positionMs, it.totalMs) } ?: duration) - amount.x / pixelsPerMs, duration)
                } else topMidi = clampPitchTop(topMidi + amount.y / rowPx, span)
            }
        } finally { dragging = false }
    }
    Box(modifier.onSizeChanged { canvasHeight = it.height; canvasWidth = it.width }) {
        Box(Modifier.matchParentSize().clipToBounds().layerBackdrop(backdrop)) {
            Canvas(
                Modifier.fillMaxSize().clipToBounds().then(dragModifier)
                    .semantics {
                        contentDescription = "音高轨迹，左右拖动查看历史，上下拖动查看音阶"
                        stateDescription = "时间 ${(viewEndMs / 1000).toInt()} 秒，可见 MIDI ${floor(topMidi - span).toInt()} 至 ${ceil(topMidi).toInt()}"
                        customActions = listOf(
                            CustomAccessibilityAction("查看更早轨迹") { if (timelineGestures != null) {
                                timelineGestures.begin(); timelineGestures.preview((reference?.positionMs ?: 0) - 5_000); timelineGestures.finish(false)
                            } else { interruptFollow(); historyEndMs = clampHistoryEnd(viewEndMs - 5_000, maxOf(reference?.totalMs ?: 0, state.durationMs).toDouble()) }; true },
                            CustomAccessibilityAction("查看更晚轨迹") { if (timelineGestures != null) {
                                timelineGestures.begin(); timelineGestures.preview((reference?.positionMs ?: 0) + 5_000); timelineGestures.finish(false)
                            } else { interruptFollow(); historyEndMs = clampHistoryEnd(viewEndMs + 5_000, maxOf(reference?.totalMs ?: 0, state.durationMs).toDouble()) }; true },
                            CustomAccessibilityAction("查看更高音阶") { interruptFollow(); topMidi = clampPitchTop(topMidi + 12f, span); true },
                            CustomAccessibilityAction("查看更低音阶") { interruptFollow(); topMidi = clampPitchTop(topMidi - 12f, span); true }
                        )
                    }.graphicsLayer()
            ) {
                val keyboardWidth = 42.dp.toPx()
                val plotLeft = keyboardWidth + 5.dp.toPx()
                val liveX = size.width * 0.82f
                drawRect(colors.surface)
                labelPaint.color = colors.subTitleColor.toArgb()
                labelPaint.textSize = 11.sp.toPx()
                val names = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
                for (midi in ceil(topMidi + 1).toInt() downTo floor(topMidi - span - 1).toInt()) {
                    if (midi !in 0..127) continue
                    val y = (topMidi - midi) * rowPx
                    val blackKey = midi % 12 in listOf(1, 3, 6, 8, 10)
                    if (!blackKey) drawRect(colors.coverBg.copy(alpha = 0.30f), Offset(0f, y - rowPx / 2), Size(size.width, rowPx))
                    drawRect(colors.coverBg.copy(alpha = if (blackKey) 0.45f else 0.70f),
                        Offset(0f, y - rowPx / 2), Size(if (blackKey) keyboardWidth * 0.66f else keyboardWidth, rowPx - 1.dp.toPx()))
                    if (!blackKey) drawContext.canvas.nativeCanvas.drawText(
                        "${names[midi % 12]}${midi / 12 - 1}", 8.dp.toPx(), y - (labelPaint.ascent() + labelPaint.descent()) / 2, labelPaint)
                }
                for (i in 0..5) {
                    val x = plotLeft + (liveX - plotLeft) * i / 5
                    drawLine(colors.subTitleColor.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx())
                }
            }
            // 只有收到新检测结果时重建路径。显示帧只平移缓存路径，不重组整页或重算音高。
            Canvas(Modifier.matchParentSize().drawWithCache {
                val plotLeft = 47.dp.toPx()
                val liveX = size.width * 0.82f
                val pixelsPerMs = (liveX - plotLeft) / PitchViewModel.HISTORY_MS
                val frames = visibleFrames
                val originMs = frames.lastOrNull()?.timeMs ?: 0L
                val path = Path()
                val points = ArrayList<Pair<Long, Offset>?>(frames.size)
                var connected = false
                var previousTime = 0L
                frames.forEach { frame ->
                    val midi = frame.pitch?.midiNote
                    if (midi == null) {
                        connected = false
                        points.add(null)
                    } else {
                        val point = Offset((frame.timeMs - originMs) * pixelsPerMs, -midi * rowPx)
                        if (connected && frame.timeMs - previousTime <= 96) path.lineTo(point.x, point.y)
                        else path.moveTo(point.x, point.y)
                        connected = true
                        previousTime = frame.timeMs
                        points.add(frame.timeMs to point)
                    }
                }
                val isolatedPoints = isolatedPitchIndices(frames).mapNotNull { points[it]?.second }
                val olderPoints = ArrayList<Offset>()
                val olderPaths = olderTakes.map { take ->
                    val olderPath = Path(); var connected = false; var previous = 0L
                    val oldFrames = take.window((pitchWindowStart(viewEndMs) - 256).toLong(), (pitchWindowStart(viewEndMs) + 10_256).toLong())
                    isolatedPitchIndices(oldFrames).forEach { index ->
                        val frame = oldFrames[index]
                        olderPoints.add(Offset((frame.timeMs - originMs) * pixelsPerMs, -frame.pitch!!.midiNote * rowPx))
                    }
                    oldFrames.forEach { frame ->
                        val midi = frame.pitch?.midiNote
                        if (midi == null) connected = false else {
                            val px = (frame.timeMs - originMs) * pixelsPerMs; val py = -midi * rowPx
                            if (connected && frame.timeMs - previous <= 96) olderPath.lineTo(px, py) else olderPath.moveTo(px, py)
                            connected = true; previous = frame.timeMs
                        }
                    }
                    olderPath
                }
                val referencePath = Path()
                var referenceConnected = false
                var referenceTime = 0L
                referenceFrames.forEach { frame ->
                    val midi = frame.pitch?.midiNote
                    if (midi == null) referenceConnected = false else {
                        val px = (frame.timeMs - originMs) * pixelsPerMs
                        val py = -midi * rowPx
                        if (referenceConnected && frame.timeMs - referenceTime <= 96) referencePath.lineTo(px, py)
                        else referencePath.moveTo(px, py)
                        referenceConnected = true; referenceTime = frame.timeMs
                    }
                }
                val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    val playbackPosition = if (reference != null) {
                        frameNanos.longValue // 播放期间仅使绘制层按帧失效。
                        if (timelineGestures == null && state.listening && (state.frames.lastOrNull()?.capturedUntilMs ?: 0) > state.segmentStartMs)
                            renderClock.timeMs(frameNanos.longValue).toLong()
                        else reference.currentPosition().coerceIn(0, reference.totalMs + 1_000)
                    } else 0L
                    val end = historyEndMs ?: reference?.let { referenceWindowEnd(playbackPosition, it.totalMs) } ?: when {
                        !state.listening -> state.durationMs.toDouble()
                        (state.frames.lastOrNull()?.capturedUntilMs ?: 0) <= state.segmentStartMs -> state.segmentStartMs.toDouble()
                        else -> renderClock.timeMs(frameNanos.longValue)
                    }
                    val start = pitchWindowStart(end)
                    val cursorX = plotLeft + (((if (reference != null && !live) start + 5_000 else if (reference != null) playbackPosition.toDouble() else end) - start) * pixelsPerMs).toFloat()
                    val x = plotLeft + ((originMs - start) * pixelsPerMs).toFloat()
                    val y = topMidi * rowPx
                    if (cursorX in plotLeft..liveX) drawLine(colors.subTitleColor.copy(alpha = 0.32f), Offset(cursorX, 0f), Offset(cursorX, size.height), 1.dp.toPx())
                    clipRect(left = plotLeft, right = liveX + 3.dp.toPx()) {
                        translate(x, y) { drawPath(referencePath, colors.subTitleColor.copy(alpha = .45f), style = stroke) }
                    }
                    clipRect(left = plotLeft, right = liveX + 3.dp.toPx()) {
                        translate(x, y) {
                            olderPaths.forEach { drawPath(it, colors.primary.copy(alpha = .24f), style = stroke) }
                            drawPath(path, colors.primary, style = stroke)
                            olderPoints.forEach { drawCircle(colors.primary.copy(alpha = .24f), 1.5.dp.toPx(), it) }
                            isolatedPoints.forEach { drawCircle(colors.primary, 1.5.dp.toPx(), it) }
                            if (reference != null && state.listening && live) {
                                latestVisiblePitch(frames, playbackPosition)?.let { frame ->
                                    drawCircle(colors.primary, 3.dp.toPx(),
                                        Offset((frame.timeMs - originMs) * pixelsPerMs, -frame.pitch!!.midiNote * rowPx))
                                }
                            }
                        }
                        // 游标只在两次真实有声采样之间插值；静音与时间缺口保持断开。
                        val next = frames.indexOfFirst { it.timeMs >= end }
                        if (reference == null && state.listening && live && next > 0) {
                            val before = points[next - 1]
                            val after = points[next]
                            if (before != null && after != null && after.first - before.first <= 96) {
                                val fraction = ((end - before.first) / (after.first - before.first)).toFloat()
                                val pitchY = before.second.y + (after.second.y - before.second.y) * fraction
                                drawCircle(colors.primary, 3.dp.toPx(), Offset(cursorX, pitchY + y))
                            }
                        }
                    }
                }
            }) { }
            Canvas(Modifier.matchParentSize().graphicsLayer()) {
                // 淡出只画在画布层；玻璃和图标不随边缘一起消失。
                val fade = minOf(28.dp.toPx(), size.height / 6)
                drawRect(Brush.verticalGradient(listOf(colors.surface, Color.Transparent), endY = fade), size = Size(size.width, fade))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, colors.surface), startY = size.height - fade, endY = size.height),
                    topLeft = Offset(0f, size.height - fade), size = Size(size.width, fade))
            }
        }
        PitchReadout(
            if (live) state.reading else state.frames.atOrBefore((if (reference != null) pitchWindowStart(viewEndMs) + 5_000 else viewEndMs).toLong())?.pitch, backdrop,
            Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp)
                .onSizeChanged { readoutHeight = it.height }
        )
        if (!live) {
            PitchGlassButton(backdrop, if (state.listening) "回到实时" else if (reference?.playing == true) "跟随播放" else "回到录制位置", enabled,
                onClick = { frameNanos.longValue = System.nanoTime(); historyEndMs = null },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(48.dp)) {
                Icon(painterResource(R.drawable.ic_pitch_live), null, Modifier.size(23.dp), tint = colors.primary)
            }
        }
        if (bottomControls == null) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .padding(start = 47.dp, end = with(density) { (canvasWidth * 0.18f).toDp() }, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(pitchWindowStart(viewEndMs), pitchWindowStart(viewEndMs) + HISTORY_WINDOW / 2, pitchWindowStart(viewEndMs) + HISTORY_WINDOW).forEach {
                Text(if (it < 0) "" else pitchTime(it.toLong()), fontSize = 10.sp, color = colors.subTitleColor)
            }
        }
        if (showControls) Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp)
                .onSizeChanged { controlsHeight = it.height },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (bottomControls != null) bottomControls(backdrop)
            else if (freeModeControls) Box(Modifier.fillMaxWidth().height(60.dp)) {
                if (state.frames.isNotEmpty()) PitchOperation(backdrop, "清空轨迹", false, enabled, onReset,
                    Modifier.align(Alignment.Center).offset(x = (-76).dp)) {
                    Icon(painterResource(R.drawable.ic_pitch_clear), null, Modifier.size(22.dp), tint = colors.subTitleColor)
                }
                val label = if (state.listening) "暂停监听" else if (state.frames.isEmpty()) "开始监听" else "继续监听"
                PitchOperation(backdrop, label, true, enabled, onPrimary, Modifier.align(Alignment.Center)) {
                    Icon(painterResource(if (state.listening) R.drawable.ic_pitch_pause else R.drawable.ic_pitch_listen), null,
                        Modifier.size(24.dp), tint = colors.primary)
                }
            } else Box(Modifier.fillMaxWidth().height(60.dp)) {
                if (state.phase == PitchPhase.Paused || state.phase == PitchPhase.Finished) {
                    PitchOperation(backdrop, "复位", false, enabled, onReset,
                        Modifier.align(Alignment.Center).offset(x = (-76).dp)) {
                        Icon(painterResource(R.drawable.ic_pitch_reset), null, Modifier.size(21.dp), tint = colors.textColor)
                    }
                }
                val primary = when (state.phase) {
                    PitchPhase.Idle -> if (live || reference == null) "开始录制" else "从浏览位置录制"
                    PitchPhase.Recording -> "暂停录制"
                    PitchPhase.Paused -> "继续录制"
                    PitchPhase.Finished -> "录制已结束，请先复位"
                }
                PitchOperation(backdrop, primary, true, enabled && state.phase != PitchPhase.Finished, {
                    if (state.phase == PitchPhase.Idle && reference != null) {
                        onSelectStart(if (!live) (pitchWindowStart(viewEndMs) + 5_000).toLong() else reference.positionMs)
                        historyEndMs = null
                    }
                    onPrimary()
                }, Modifier.align(Alignment.Center)) {
                    if (state.phase == PitchPhase.Recording) Icon(painterResource(R.drawable.ic_pause), null,
                        Modifier.size(25.dp), tint = colors.primary)
                    else Box(Modifier.size(20.dp).clip(ContinuousCapsule).background(
                        if (state.phase == PitchPhase.Finished) colors.subTitleColor.copy(alpha = .3f) else colors.primary))
                }
                if (state.phase == PitchPhase.Recording || state.phase == PitchPhase.Paused) {
                    PitchOperation(backdrop, "停止录制", false, enabled, onStop,
                        Modifier.align(Alignment.Center).offset(x = 76.dp)) {
                        Box(Modifier.size(16.dp).clip(ContinuousRoundedRectangle(3.dp)).background(colors.textColor))
                    }
                }
            }

        }
    }
}

private const val HISTORY_WINDOW = 10_000.0
private fun pitchTime(ms: Long): String = String.format(Locale.ROOT, "%02d:%02d", ms / 60_000, ms / 1000 % 60)

@Composable
internal fun PitchOperation(backdrop: Backdrop, label: String, primary: Boolean, enabled: Boolean,
    onClick: () -> Unit, modifier: Modifier, glass: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    PitchGlassButton(backdrop, label, enabled, onClick, modifier.size(if (primary) 60.dp else 48.dp), glass = glass, content = content)
}

@Composable
private fun PitchReadout(reading: DetectedPitch?, backdrop: Backdrop, modifier: Modifier) {
    val colors = MaterialTheme.rythmeColors
    Box(modifier.width(160.dp)) {
        PitchGlassSurface(backdrop, ContinuousRoundedRectangle(20.dp))
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(with(LocalDensity.current) { 40.sp.toDp() }), contentAlignment = Alignment.CenterStart) {
                    Text(reading?.noteName ?: "—", modifier = Modifier.fillMaxWidth(), maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 30.sp),
                        fontWeight = FontWeight.Medium, color = colors.textColor)
                }
                Text(reading?.let { String.format(Locale.ROOT, "%.1f Hz", it.frequencyHz) } ?: "— Hz",
                    fontSize = 11.sp, maxLines = 1, color = colors.subTitleColor,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
            }
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawLine(colors.coverBg, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3.dp.toPx(), StrokeCap.Round)
                reading?.let {
                    val x = size.width * ((it.cents.coerceIn(-50, 50) + 50) / 100f)
                    drawLine(colors.primary, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx(), StrokeCap.Round)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("−50", fontSize = 10.sp, color = colors.subTitleColor)
                Text(reading?.let { "${if (it.cents > 0) "+" else ""}${it.cents} cent" } ?: "— cent",
                    fontSize = 10.sp, color = colors.subTitleColor)
                Text("+50", fontSize = 10.sp, color = colors.subTitleColor)
            }
        }
    }
}

@Composable
internal fun BoxScope.PitchGlassSurface(backdrop: Backdrop, shape: Shape) {
    val colors = MaterialTheme.rythmeColors
    GlassBackdropSurface(
        backdrop = backdrop, shape = { shape },
        effects = { vibrancy(); blur(4.dp.toPx()); glassLens(12.dp.toPx(), 16.dp.toPx()) },
        onDrawSurface = {
            drawRect(colors.bottomBackground)
        }
    )
}

@Composable
private fun PitchGlassButton(
    backdrop: Backdrop,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    glass: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val press = remember(scope) { TopBarPressState(scope) }
    val highlight = rememberTopBarPressHighlight()
    val hdr = LocalGlassHdr.current
    val colors = MaterialTheme.rythmeColors
    val density = LocalDensity.current
    TopBarPressHeadroom(press, avatar = false)
    val iconAlpha by animateFloatAsState(
        targetValue = TopBarPressMotion.iconTargetAlpha(press.pressedKey != null, dark = hdr.darkTheme),
        animationSpec = if (press.pressedKey != null) TopBarPressMotion.iconDown else TopBarPressMotion.iconUp,
        label = "pitchButtonPress"
    )
    val pressLayer: GraphicsLayerScope.() -> Unit = {
        scaleX = press.scale
        scaleY = press.scale
    }
    Box(modifier.onSizeChanged { press.nominalWidth = with(density) { it.width.toDp().value } }, contentAlignment = Alignment.Center) {
        if (glass) GlassBackdropSurface(
            backdrop = backdrop,
            shape = { ContinuousCapsule },
            pressHdr = true,
            rimHeadroom = { hdr.staticHeadroom },
            effects = { vibrancy(); blur(4.dp.toPx()); glassLens(12.dp.toPx(), 16.dp.toPx()) },
            layerBlock = pressLayer,
            bodyReflectionBrush = { topBarPressBodyReflection(press.light.value) },
            onDrawSurface = {
                drawRect(colors.bottomBackground)
                drawRect(highlight.single(size, press), blendMode = BlendMode.Plus)
            }
        )
        Box(Modifier.matchParentSize().graphicsLayer(pressLayer).clip(ContinuousCapsule)
            .topBarPress(press, listOf("pitch-button"), enabled)
            .clickable(enabled = enabled, interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().glassHdrFadeAndBlur(alpha = { iconAlpha }).topBarForegroundLight(press),
                contentAlignment = Alignment.Center, content = content)
        }
    }
}
