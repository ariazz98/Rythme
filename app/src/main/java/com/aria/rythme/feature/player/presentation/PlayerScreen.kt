package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.R
import androidx.compose.animation.animateColorAsState
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.domain.model.RepeatMode
import com.aria.rythme.core.utils.GradientColors
import com.aria.rythme.core.utils.ImageColorExtractor
import com.aria.rythme.core.utils.rememberScreenCornerRadiusDp
import com.aria.rythme.ui.component.CoverItem
import com.aria.rythme.ui.component.NextIcon
import com.aria.rythme.ui.component.PlayPauseIcon
import com.aria.rythme.ui.component.PreviousIcon
import com.aria.rythme.ui.component.ProgressItem
import com.aria.rythme.ui.component.VoiceItem
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 播放器页面
 *
 * 显示当前播放歌曲的信息和控制按钮。
 *
 * @param viewModel 播放 ViewModel
 */
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsUiState()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val playerVisible = LocalPlayerVisible.current

    var gradientColors by remember { mutableStateOf(GradientColors()) }
    val animatedTop by animateColorAsState(gradientColors.top, tween(600))
    val animatedCenter by animateColorAsState(gradientColors.center, tween(600))
    val animatedBottom by animateColorAsState(gradientColors.bottom, tween(600))
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(animatedTop, animatedCenter, animatedBottom),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    val width = LocalWindowInfo.current.containerDpSize.width
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalWindowInfo.current.containerDpSize.height.toPx()
    }
    val dismissThreshold = screenHeightPx * 0.35f
    val velocityThreshold = 2000f

    val scope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    // 当播放器打开时重置拖动偏移
    LaunchedEffect(playerVisible) {
        if (playerVisible) {
            dragOffsetY.snapTo(0f)
        }
    }

    val animateCoverSize by animateDpAsState(
        targetValue = if (state.isPlaying) min(width * 6 / 7, 350.dp) else min(width * 2 / 3, 256.dp),
        animationSpec = if (state.isPlaying) {
            spring(
                dampingRatio = 0.6f,
                stiffness = 100f
            )
        } else {
            tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        }
    )

    with(sharedTransitionScope) {
        AnimatedVisibility(
            visible = playerVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier
                .clickable(interactionSource = null, indication = null) {

                }
                .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "playerContainer"),
                    animatedVisibilityScope = this,
                    resizeMode = ResizeMode.RemeasureToBounds
                )
                .fillMaxSize()
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            dragOffsetY.snapTo((dragOffsetY.value + delta).coerceAtLeast(0f))
                        }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (dragOffsetY.value > dismissThreshold || velocity > velocityThreshold) {
                            onBack()
                        } else {
                            scope.launch {
                                dragOffsetY.animateTo(0f)
                            }
                        }
                    }
                )
                .clip(RoundedCornerShape(rememberScreenCornerRadiusDp()))
                .background(backgroundBrush),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.statusBarsPadding().height(16.dp))

                Box(
                    modifier = Modifier
                        .width(62.dp)
                        .height(6.dp)
                        .clip(ContinuousCapsule)
                        .background(Color(0x33FFFFFF))
                        .clickable(interactionSource = null, indication = null) {
                            onBack()
                        }
                )

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {

                    CoverItem(
                        modifier = Modifier.sharedElementWithCallerManagedVisibility(
                            sharedContentState = rememberSharedContentState(key = "cover"),
                            visible = playerVisible
                        ),
                        size = animateCoverSize,
                        corner = 9.dp,
                        song = state.currentSong,
                        defaultBgColor = Color(0xFF606063),
                        defaultIconColor = Color(0xFF737376),
                        onBitmapReady = { bitmap ->
                            if (bitmap != null) {
                                scope.launch {
                                    gradientColors = ImageColorExtractor.extractGradientColors(bitmap)
                                    bitmap.recycle()
                                }
                            } else {
                                gradientColors = GradientColors()
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .renderInSharedTransitionScopeOverlay(
                            zIndexInOverlay = 1f
                        )
                        .animateEnterExit(
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(durationMillis = 300)
                            )
                        )
                ) {

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                text = state.currentSong?.title ?: stringResource(R.string.not_play),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to Color.Transparent,
                                                1f to Color.Black,
                                                startX = 0f,
                                                endX = 8.dp.toPx()
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0.9f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                    .customMarquee()
                                    .padding(start = 32.dp)
                            )
                            if (!state.currentSong?.artist.isNullOrEmpty()) {
                                Text(
                                    text = state.currentSong!!.artist,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0x80FFFFFF),
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }

                        if (state.currentSong != null) {

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x30FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_star),
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x30FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more),
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    ProgressItem(
                        enabled = state.currentSong != null,
                        progress = state.progress,
                        currentPosition = state.currentPosition,
                        duration = state.duration,
                        onSeek = { viewModel.sendIntent(PlayerIntent.SeekTo((it * state.duration).toLong())) }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PreviousIcon(
                            enable = state.canPlayPrevious,
                            height = 21.dp,
                            tint = if (state.canPlayPrevious) Color.White else Color(0x33FFFFFF),
                            onClick = {
                                viewModel.sendIntent(PlayerIntent.Previous)
                            }
                        )

                        PlayPauseIcon(
                            isPlaying = state.isPlaying,
                            size = 40.dp,
                            tint = Color.White,
                            onClick = {
                                if (state.currentSong == null) {
                                    // 如果当前没有歌曲播放，加载并随机播放一首
                                    viewModel.sendIntent(PlayerIntent.LoadAndPlayRandom)
                                } else {
                                    // 否则切换播放/暂停状态
                                    viewModel.sendIntent(PlayerIntent.TogglePlayPause)
                                }
                            }
                        )

                        NextIcon(
                            enable = state.canPlayNext,
                            height = 21.dp,
                            tint = if (state.canPlayNext) Color.White else Color(0x33FFFFFF),
                            onClick = {
                                viewModel.sendIntent(PlayerIntent.Next)
                            }
                        )

                    }

                    Spacer(modifier = Modifier.height(56.dp))

                    VoiceItem(
                        progress = state.volume / 100f,
                        onSeek = { viewModel.sendIntent(PlayerIntent.SetVolume((it * 100).toInt())) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lrc),
                            contentDescription = "",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )

                        Icon(
                            painter = painterResource(R.drawable.ic_airplay),
                            contentDescription = "",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )

                        Icon(
                            painter = painterResource(R.drawable.ic_play_list),
                            contentDescription = "",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(56.dp))
                }

            }
        }
    }
}