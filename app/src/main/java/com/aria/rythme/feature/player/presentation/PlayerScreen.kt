package com.aria.rythme.feature.player.presentation

import android.content.Context
import android.media.MediaRouter2
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.R
import com.aria.rythme.core.utils.defaultGradientBrush
import com.aria.rythme.core.utils.rememberScreenCornerRadiusDp
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.NextIcon
import com.aria.rythme.ui.component.PlayPauseIcon
import com.aria.rythme.ui.component.PreviousIcon
import com.aria.rythme.ui.component.ProgressItem
import com.aria.rythme.ui.component.OverlayMenu
import com.aria.rythme.ui.component.VoiceItem
import com.aria.rythme.ui.component.buildSongContextMenuConfigs
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * 播放器底部面板状态
 */
private enum class PlayerPanel { NONE, LYRICS, QUEUE }

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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val playerVisible = LocalPlayerVisible.current
    val width = LocalWindowInfo.current.containerDpSize.width
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalWindowInfo.current.containerDpSize.height.toPx()
    }
    val dismissThreshold = screenHeightPx * 0.35f
    val velocityThreshold = 2000f

    val scope = rememberCoroutineScope()
    val dragOffsetYState = remember { mutableFloatStateOf(0f) }
    var dragOffsetY by dragOffsetYState

    var activePanel by remember { mutableStateOf(PlayerPanel.NONE) }
    var controlsVisible by remember { mutableStateOf(true) }
    val overlayMenu = LocalOverlayMenu.current
    val hasSyncedLyrics = !state.lyricsData?.lines.isNullOrEmpty()
    val onFavoriteClick = viewModel::toggleCurrentSongFavorite
    val onMoreClick: (Rect) -> Unit = { bounds ->
        state.currentSong?.let { song ->
            overlayMenu.show(
                OverlayMenu.SongContext(
                    song = song,
                    anchorBounds = bounds,
                    configs = buildSongContextMenuConfigs(
                        onDismiss = overlayMenu::dismiss,
                        onEdit = { overlayMenu.show(OverlayMenu.SongEdit(song)) }
                    )
                )
            )
        }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(
        enabled = playerVisible && activePanel != PlayerPanel.NONE && !overlayMenu.isVisible
    ) {
        activePanel = PlayerPanel.NONE
    }

    // 当播放器打开时重置拖动偏移
    LaunchedEffect(playerVisible) {
        if (playerVisible) {
            dragOffsetY = 0f
            controlsVisible = true
        }
    }

    LaunchedEffect(hasSyncedLyrics) {
        if (!hasSyncedLyrics) {
            controlsVisible = true
        }
    }

    // 歌词模式下播放中 5s 无操作自动隐藏操作区
    LaunchedEffect(state.isPlaying, activePanel, controlsVisible, hasSyncedLyrics) {
        if (
            state.isPlaying &&
            activePanel == PlayerPanel.LYRICS &&
            controlsVisible &&
            hasSyncedLyrics
        ) {
            delay(5000L)
            controlsVisible = false
        }
    }

    // 切换面板时重置操作区可见
    LaunchedEffect(activePanel) {
        controlsVisible = true
    }

    val animateCoverSize by animateDpAsState(
        targetValue = if (state.isPlaying) min(width * 6 / 7, 350.dp) else min(width * 2 / 3, 256.dp),
        animationSpec = if (state.isPlaying) {
            spring(dampingRatio = 0.6f, stiffness = 100f)
        } else {
            tween(durationMillis = 500, easing = FastOutSlowInEasing)
        }
    )

    // 操作区（底部浮层：歌词模式自动隐藏 / 播放列表拖动时隐藏）
    val showControls = controlsVisible
    val controlsSlide by animateFloatAsState(
        targetValue = if (showControls) 0f else 1f,
        animationSpec = tween(300)
    )

    val stickyBackdrop = rememberLayerBackdrop()

    with(sharedTransitionScope) {
        val playerContainerState = rememberSharedContentState(key = "playerContainer")
        val containerTransitionActive =
            playerContainerState.isMatchFound && sharedTransitionScope.isTransitionActive

        AnimatedVisibility(
            visible = playerVisible,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {

            Box(
                modifier = Modifier.fillMaxSize()
                    .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                    .clickable(interactionSource = null, indication = null) { }
                    .draggable(
                        state = rememberDraggableState { delta ->
                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                        },
                        orientation = Orientation.Vertical,
                        enabled = activePanel == PlayerPanel.NONE,
                        onDragStopped = { velocity ->
                            if (dragOffsetY > dismissThreshold || velocity > velocityThreshold) {
                                onBack()
                            } else {
                                scope.launch {
                                    animate(dragOffsetY, 0f) { value, _ ->
                                        dragOffsetY = value
                                    }
                                }
                            }
                        }
                    )
                    .then(if (dragOffsetY > 0)
                        Modifier.clip(ContinuousRoundedRectangle(rememberScreenCornerRadiusDp()))
                    else
                       Modifier
                    )
            ) {

                // 背景：渐变色兜底 + 有封面时叠加模糊封面（Apple Music 风格）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            sharedContentState = playerContainerState,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            resizeMode = ResizeMode.RemeasureToBounds
                        )
                        .then(
                            if (containerTransitionActive || dragOffsetY > 0) {
                                Modifier.clip(
                                    ContinuousRoundedRectangle(rememberScreenCornerRadiusDp())
                                )
                            } else {
                                Modifier
                            }
                        )
                        .layerBackdrop(stickyBackdrop)
                        .background(defaultGradientBrush)
                ) {
                    val coverUri = state.currentSong?.coverUri
                    if (coverUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(coverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }
                                .blur(60.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !containerTransitionActive,
                    enter = fadeIn(tween(180)),
                    exit = ExitTransition.None
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        modifier = Modifier
                            .fillMaxSize(),
                        topBar = {
                        // Handle bar（顶部）
                        Box(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .draggable(
                                    state = rememberDraggableState { delta ->
                                        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                                    },
                                    orientation = Orientation.Vertical,
                                    onDragStopped = { velocity ->
                                        if (dragOffsetY > dismissThreshold || velocity > velocityThreshold) {
                                            onBack()
                                        } else {
                                            scope.launch {
                                                animate(dragOffsetY, 0f) { value, _ ->
                                                    dragOffsetY = value
                                                }
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
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
                        }
                    },
                    bottomBar = {
                        if (showControls || controlsSlide < 1f) Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .draggable(
                                    state = rememberDraggableState { delta ->
                                        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                                    },
                                    orientation = Orientation.Vertical,
                                    onDragStopped = { velocity ->
                                        if (dragOffsetY > dismissThreshold || velocity > velocityThreshold) {
                                            onBack()
                                        } else {
                                            scope.launch {
                                                animate(dragOffsetY, 0f) { value, _ ->
                                                    dragOffsetY = value
                                                }
                                            }
                                        }
                                    }
                                )
                                .then(
                                    // 仅在共享元素过渡期间使用 overlay，拖拽时禁用以避免与 offset 脱节
                                    if (sharedTransitionScope.isTransitionActive)
                                        Modifier.renderInSharedTransitionScopeOverlay(
                                            zIndexInOverlay = 1f
                                        )
                                    else
                                        Modifier
                                )
                                .animateEnterExit(
                                    enter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                )
                                .graphicsLayer {
                                    translationY = size.height * controlsSlide
                                    alpha = 1f - controlsSlide
                                }
                        ) {
                            ProgressItem(
                                enabled = state.currentSong != null,
                                progress = state.progress,
                                currentPosition = state.currentPosition,
                                duration = state.duration,
                                onSeek = { viewModel.seekTo((it * state.duration).toLong()) }
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
                                        viewModel.previous()
                                    }
                                )

                                PlayPauseIcon(
                                    isPlaying = state.isPlaying,
                                    size = 40.dp,
                                    tint = Color.White,
                                    onClick = {
                                        if (state.currentSong == null) {
                                            viewModel.loadAndPlayRandom()
                                        } else {
                                            viewModel.togglePlayPause()
                                        }
                                    }
                                )

                                NextIcon(
                                    enable = state.canPlayNext,
                                    height = 21.dp,
                                    tint = if (state.canPlayNext) Color.White else Color(0x33FFFFFF),
                                    onClick = {
                                        viewModel.next()
                                    }
                                )

                            }

                            Spacer(modifier = Modifier.height(56.dp))

                            VoiceItem(
                                progress = state.volume / 100f,
                                onSeek = { viewModel.setVolume((it * 100).toInt()) }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp)
                                        .then(if (activePanel == PlayerPanel.LYRICS) {
                                            Modifier
                                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                                .drawWithContent {
                                                    drawContent()
                                                    drawCircle(
                                                        color = Color(0x66FFFFFF),
                                                        blendMode = BlendMode.SrcOut
                                                    )
                                                }
                                        } else {
                                            Modifier
                                        })
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                            onClick = {
                                                activePanel = if (activePanel == PlayerPanel.LYRICS) PlayerPanel.NONE else PlayerPanel.LYRICS
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(if (activePanel == PlayerPanel.LYRICS) R.drawable.ic_lrc_full else R.drawable.ic_lrc),
                                        contentDescription = "",
                                        tint = if (activePanel == PlayerPanel.LYRICS) Color.White else Color(0x80FFFFFF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                            onClick = { showSystemOutputSwitcher(context) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_airplay),
                                        contentDescription = "输出设备",
                                        tint = Color(0x80FFFFFF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier.size(40.dp)
                                        .then(if (activePanel == PlayerPanel.QUEUE) {
                                            Modifier
                                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                                .drawWithContent {
                                                    drawContent()
                                                    drawCircle(
                                                        color = Color(0x66FFFFFF),
                                                        blendMode = BlendMode.SrcOut
                                                    )
                                                }
                                        } else {
                                            Modifier
                                        })
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                            onClick = {
                                                activePanel = if (activePanel == PlayerPanel.QUEUE) PlayerPanel.NONE else PlayerPanel.QUEUE
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_play_list),
                                        contentDescription = "",
                                        tint = if (activePanel == PlayerPanel.QUEUE) Color.White else Color(0x80FFFFFF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(56.dp))
                        }
                    }
                    ) { innerPadding ->
                    AnimatedContent(
                        targetState = activePanel,
                        contentKey = { it != PlayerPanel.NONE },
                        transitionSpec = {
                            (fadeIn(tween(300)) togetherWith fadeOut(tween(300)))
                                .using(SizeTransform(clip = false))
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { targetPanel ->
                        when (targetPanel) {
                            PlayerPanel.NONE -> {
                                NowPlayingPanel(
                                    state = state,
                                    playerVisible = playerVisible,
                                    animatedContentScope = this@AnimatedContent,
                                    innerPadding = innerPadding,
                                    coverSize = animateCoverSize,
                                    onFavoriteClick = onFavoriteClick,
                                    onMoreClick = onMoreClick
                                )
                            }
                            PlayerPanel.LYRICS -> {
                                LyricsPanel(
                                    state = state,
                                    playerVisible = playerVisible,
                                    animatedContentScope = this@AnimatedContent,
                                    innerPadding = innerPadding,
                                    controlsSlide = controlsSlide,
                                    controlsVisible = controlsVisible,
                                    modifier = Modifier.draggable(
                                        state = rememberDraggableState { delta ->
                                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                                        },
                                        orientation = Orientation.Vertical,
                                        onDragStopped = { velocity ->
                                            if (dragOffsetY > dismissThreshold || velocity > velocityThreshold) {
                                                onBack()
                                            } else {
                                                scope.launch {
                                                    animate(dragOffsetY, 0f) { value, _ ->
                                                        dragOffsetY = value
                                                    }
                                                }
                                            }
                                        }
                                    ),
                                    onBackToNowPlaying = {
                                        activePanel = PlayerPanel.NONE
                                    },
                                    onFavoriteClick = onFavoriteClick,
                                    onMoreClick = onMoreClick,
                                    onSeekToLine = { index ->
                                        viewModel.seekToLyricLine(index)
                                    },
                                    onControlsVisibleChange = { visible ->
                                        controlsVisible = visible
                                    }
                                )
                            }
                            PlayerPanel.QUEUE -> {
                                QueueHistoryPanel(
                                    state = state,
                                    playerVisible = playerVisible,
                                    scope = scope,
                                    animatedContentScope = this@AnimatedContent,
                                    onClearHistory = viewModel::clearHistory,
                                    onSelectQueueEntry = viewModel::selectQueueEntry,
                                    onReorderQueue = viewModel::reorderQueue,
                                    onToggleShuffle = viewModel::toggleShuffleMode,
                                    onToggleRepeat = viewModel::toggleRepeatMode,
                                    onToggleInfinitePlay = viewModel::toggleInfinitePlay,
                                    onToggleCrossfade = viewModel::toggleCrossfade,
                                    onFavoriteClick = onFavoriteClick,
                                    onMoreClick = onMoreClick,
                                    innerPadding = innerPadding,
                                    stickyBackdrop = stickyBackdrop,
                                    screenDragOffsetY = dragOffsetYState,
                                    dismissThreshold = dismissThreshold,
                                    velocityThreshold = velocityThreshold,
                                    onDismiss = onBack,
                                    onListScrolling = { scrolling ->
                                        controlsVisible = !scrolling
                                    },
                                    onCoverClick = {
                                        activePanel = PlayerPanel.NONE
                                    },
                                    controlsSlide = controlsSlide
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

private fun showSystemOutputSwitcher(context: Context) {
    val shown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        MediaRouter2.getInstance(context).showSystemOutputSwitcher()
    } else {
        false
    }
    if (!shown) {
        Toast.makeText(context, R.string.media_output_unavailable, Toast.LENGTH_SHORT).show()
    }
}
