package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors
import kotlinx.coroutines.launch

@Composable
fun PlayPauseIcon(
    isPlaying: Boolean,
    size: Dp,
    tint: Color = MaterialTheme.rythmeColors.textColor,
    onClick: () -> Unit
) {
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val playAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 1f,
        animationSpec = springSpec
    )
    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 0.5f else 1f,
        animationSpec = springSpec
    )
    val pauseAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = springSpec
    )
    val pauseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.5f,
        animationSpec = springSpec
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(interactionSource = null, indication = null) {
                onClick()
            }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(size)
                .scale(playScale)
                .alpha(playAlpha)
        )
        Icon(
            painter = painterResource(R.drawable.ic_pause),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(size)
                .scale(pauseScale)
                .alpha(pauseAlpha)
        )
    }
}

@Composable
fun NextIcon(
    enable: Boolean = true,
    height: Dp,
    tint: Color = Color.Black,
    onClick: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var iconWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val p = animProgress.value

    Row(
        modifier = if (enable) {
            Modifier.clickable(interactionSource = null, indication = null) {
                onClick()
                scope.launch {
                    animProgress.snapTo(0f)
                    animProgress.animateTo(1f, tween(300))
                    animProgress.snapTo(0f)
                }
            }
        } else {
            Modifier
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (p > 0f) {
            val iconWidthDp = with(density) { iconWidth.toDp() }
            // 新 icon 从左侧放大渐显
            Box(
                modifier = Modifier
                    .width(iconWidthDp * p)
                    .height(height)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_next_single),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .height(height)
                        .graphicsLayer {
                            scaleX = p
                            scaleY = p
                            alpha = p
                        }
                )
            }
        }

        // 静止的左侧 icon
        Icon(
            painter = painterResource(R.drawable.ic_next_single),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .height(height)
                .onSizeChanged { iconWidth = it.width }
        )

        if (p > 0f) {
            val iconWidthDp = with(density) { iconWidth.toDp() }
            // 右侧 icon 缩小渐隐
            Box(
                modifier = Modifier
                    .width(iconWidthDp * (1f - p))
                    .height(height)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_next_single),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .height(height)
                        .graphicsLayer {
                            scaleX = 1f - p
                            scaleY = 1f - p
                            alpha = 1f - p
                        }
                )
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_next_single),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.height(height)
            )
        }
    }
}

@Composable
fun PreviousIcon(
    enable: Boolean = true,
    height: Dp,
    tint: Color = Color.Black,
    onClick: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var iconWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val p = animProgress.value

    Row(
        modifier = if (enable) {
            Modifier.clickable(interactionSource = null, indication = null) {
                onClick()
                scope.launch {
                    animProgress.snapTo(0f)
                    animProgress.animateTo(1f, tween(300))
                    animProgress.snapTo(0f)
                }
            }
        } else {
            Modifier
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (p > 0f) {
            val iconWidthDp = with(density) { iconWidth.toDp() }
            // 左侧 icon 缩小渐隐
            Box(
                modifier = Modifier
                    .width(iconWidthDp * (1f - p))
                    .height(height)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_previous_single),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .height(height)
                        .graphicsLayer {
                            scaleX = 1f - p
                            scaleY = 1f - p
                            alpha = 1f - p
                        }
                )
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_previous_single),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.height(height)
            )
        }

        // 静止的右侧 icon
        Icon(
            painter = painterResource(R.drawable.ic_previous_single),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .height(height)
                .onSizeChanged { iconWidth = it.width }
        )

        if (p > 0f) {
            val iconWidthDp = with(density) { iconWidth.toDp() }
            // 新 icon 从右侧放大渐显
            Box(
                modifier = Modifier
                    .width(iconWidthDp * p)
                    .height(height)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_previous_single),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .height(height)
                        .graphicsLayer {
                            scaleX = p
                            scaleY = p
                            alpha = p
                        }
                )
            }
        }
    }
}