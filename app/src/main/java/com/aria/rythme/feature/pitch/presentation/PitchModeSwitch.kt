package com.aria.rythme.feature.pitch.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.ui.component.*
import com.aria.rythme.ui.component.utils.DampedDragAnimation
import com.aria.rythme.ui.component.utils.InteractiveHighlight
import com.aria.rythme.ui.component.utils.bottomTabPressScale
import com.aria.rythme.ui.component.utils.bottomTabEmphasisScale
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.backdrops.*
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** 使用展开态 BottomBar 的拖动器、整壳缩放、隐藏强调层和同一个液滴表面。 */
@Composable
internal fun PitchModeSwitch(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.rythmeColors
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val base = rememberLayerBackdrop()
    val hdr = LocalGlassHdr.current
    val emphasizedBackdrop = key(hdr.enabled, if (hdr.enabled) hdr.generation else 0) { rememberLayerBackdrop() }
    val currentSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val scope = rememberCoroutineScope()
    val height = maxOf(44.dp, with(density) { 14.sp.toDp() } + 20.dp)
    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val cell = (maxWidth - 8.dp) / 2
        val cellPx = with(density) { cell.toPx() }
        val currentCellPx by rememberUpdatedState(cellPx)
        val direction by rememberUpdatedState(if (isLtr) 1f else -1f)
        val widthPx = with(density) { maxWidth.toPx() }
        val paddingPx = with(density) { 4.dp.toPx() }
        val selectorHeight = height - 7.dp
        val selectorHeightPx = with(density) { selectorHeight.toPx() }
        val offset = remember { Animatable(0f) }
        val session = remember { FloatArray(2) }
        val drag = remember(scope) {
            DampedDragAnimation(
                scope, selectedIndex.toFloat(), 0f..1f, 0.001f, 1f, 74f / 56f,
                onDragStarted = { session[0] = targetValue; session[1] = 0f },
                onDragStopped = {
                    val candidate = targetValue.roundToInt().coerceIn(0, 1)
                    currentSelect(candidate)
                    // 未开放模式由页面拒绝，退回真实选中项；不伪造模式状态。
                    animateToValue(currentSelected.toFloat(), animatePress = false)
                    scope.launch { offset.animateTo(0f, spring(1f, 300f, 0.5f)) }
                },
                onDrag = { _, amount ->
                    session[1] += amount.x
                    updateValue((session[0] + session[1] / currentCellPx * direction).coerceIn(0f, 1f))
                    scope.launch { offset.snapTo(offset.value + amount.x) }
                }
            )
        }
        LaunchedEffect(selectedIndex) { drag.animateToValue(selectedIndex.toFloat(), animatePress = false) }
        val panelOffset = with(density) { 4.dp.toPx() } *
            (offset.value / widthPx).sign * EaseOut.transform(abs(offset.value / widthPx).coerceIn(0f, 1f))
        val wholeScale = bottomTabPressScale(widthPx, with(density) { 16.dp.toPx() }, drag.pressProgress)
        val wholeLayer: GraphicsLayerScope.() -> Unit = { scaleX = wholeScale; scaleY = wholeScale }
        val velocity = drag.velocity / 10f
        val dropletX = drag.scaleX / (1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f))
        val dropletY = drag.scaleY * (1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f))
        val verticalGap = (selectorHeightPx * dropletY - selectorHeightPx) / 2
        val alignedCenter = (widthPx - widthPx * wholeScale + cellPx * dropletX) / 2 - verticalGap
        val edgeOutset = (paddingPx + cellPx / 2 - alignedCenter) * drag.pressProgress
        val center = paddingPx + (drag.value + 0.5f) * cellPx + (drag.value * 2 - 1) * edgeOutset
        val highlight = remember(scope) { InteractiveHighlight(scope, surfaceAlpha = 0f, spotlightAlpha = 0f,
            position = { size, _ -> Offset(size.width / 2, size.height / 2) }) }
        // 放大后的液滴会超出控件边界，底稿留出采样余量并保持不透明，挡住下方真实外壳。
        Box(Modifier.align(Alignment.Center).requiredSize(maxWidth + 128.dp, height + 128.dp)
            .alpha(0f).layerBackdrop(base).background(colors.surface))
        Box(Modifier.matchParentSize().graphicsLayer { translationX = panelOffset }) {
            LiquidTabContainerSurface(base, colors.bottomBackground, { drag.pressProgress }, wholeLayer)
            Box(Modifier.matchParentSize().graphicsLayer(wholeLayer).clip(ContinuousCapsule).then(highlight.modifier)) {
                ModeLabels(false, drag.pressProgress, wholeScale, Modifier.fillMaxSize()) { index ->
                    Modifier.semantics {
                        role = Role.Tab; selected = selectedIndex == index
                        contentDescription = if (index == 0) "自由录制" else "歌曲对照"
                        onClick { currentSelect(index); drag.animateToValue(currentSelected.toFloat()); true }
                    }.pointerInput(index) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            try {
                                drag.press()
                                drag.animateToValue(index.toFloat(), animatePress = false)
                                if (waitForUpOrCancellation() != null) currentSelect(index)
                            } finally {
                                drag.animateToValue(currentSelected.toFloat(), animatePress = false)
                                drag.release()
                            }
                        }
                    }
                }
            }
        }
        // 与 BottomBar 相同：不可见的强调内容先录制，再由液滴采样。
        Box(Modifier.matchParentSize().clearAndSetSemantics { }.alpha(0f).layerBackdrop(emphasizedBackdrop)
            .graphicsLayer { translationX = panelOffset }) {
            Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(selectorHeight)) {
                GlassBackdropSurface(
                    backdrop = base, shape = { ContinuousCapsule },
                    effects = { vibrancy(); blur(2.dp.toPx()); glassLens(24.dp.toPx() * drag.pressProgress, 32.dp.toPx() * drag.pressProgress) },
                    lightingAlpha = { drag.pressProgress }, pressProgress = { drag.pressProgress },
                    layerBlock = { scaleX = wholeScale; scaleY = 1f },
                    onDrawSurface = { drawRect(colors.bottomBackground) }
                )
                Box(Modifier.matchParentSize().clip(ContinuousCapsule).then(highlight.modifier))
            }
            ModeLabels(true, drag.pressProgress, wholeScale, Modifier.fillMaxSize().graphicsLayer(wholeLayer))
        }
        Box(Modifier.align(Alignment.CenterStart).width(cell).height(selectorHeight)
            .graphicsLayer { translationX = (if (isLtr) center else widthPx - center) - cellPx / 2 + panelOffset }
            .then(highlight.gestureModifier).then(drag.modifier)) {
            LiquidTabSelectionSurface(rememberCombinedBackdrop(base, emphasizedBackdrop), colors.bottomSelected,
                { drag.pressProgress }, { dropletX }, { dropletY })
        }
    }
}

@Composable
private fun ModeLabels(emphasized: Boolean, press: Float, inheritedScale: Float, modifier: Modifier,
    itemModifier: (Int) -> Modifier = { Modifier }) {
    val colors = MaterialTheme.rythmeColors
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("自由录制", "歌曲对照").forEachIndexed { index, label ->
            Box(Modifier.weight(1f).fillMaxHeight().then(itemModifier(index)), contentAlignment = Alignment.Center) {
                Text(label, fontSize = 14.sp, color = if (emphasized) colors.primary else colors.textColor,
                    modifier = Modifier.clearAndSetSemantics { }.graphicsLayer {
                        val scale = if (emphasized) bottomTabEmphasisScale(press, inheritedScale) else 1f
                        scaleX = scale; scaleY = scale
                    })
            }
        }
    }
}
