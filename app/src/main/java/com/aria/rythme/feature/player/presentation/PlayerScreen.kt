package com.aria.rythme.feature.player.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
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
import androidx.compose.foundation.gestures.DraggableState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.core.utils.defaultGradientBrush
import com.aria.rythme.core.utils.rememberScreenCornerRadiusDp
import com.aria.rythme.ui.component.CoverItem
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.NextIcon
import com.aria.rythme.ui.component.PlayPauseIcon
import com.aria.rythme.ui.component.PreviousIcon
import com.aria.rythme.ui.component.ProgressItem
import com.aria.rythme.ui.component.VoiceItem
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
    val state by viewModel.state.collectAsUiState()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val playerVisible = LocalPlayerVisible.current
    val sharedIdentity = state.currentQueueEntryIdentity

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

    BackHandler(
        enabled = playerVisible && activePanel != PlayerPanel.NONE && !overlayMenu.isVisible
    ) {
        activePanel = PlayerPanel.NONE
    }

    // 当播放器打开时重置拖动偏移
    LaunchedEffect(playerVisible) {
        if (playerVisible) {
            dragOffsetY = 0f
        }
    }

    // 歌词模式下播放中 5s 无操作自动隐藏操作区
    LaunchedEffect(state.isPlaying, activePanel, controlsVisible) {
        if (state.isPlaying && activePanel == PlayerPanel.LYRICS && controlsVisible) {
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
                        val context = LocalContext.current
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
                                            viewModel.sendIntent(PlayerIntent.LoadAndPlayRandom)
                                        } else {
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
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CoverItem(
                                            modifier = Modifier
                                                .sharedElementWithCallerManagedVisibility(
                                                    sharedContentState = rememberSharedContentState(
                                                        key = "playerArtworkOverlay_$sharedIdentity"
                                                    ),
                                                    visible = playerVisible
                                                )
                                                .sharedBounds(
                                                    sharedContentState = rememberSharedContentState(
                                                        key = "playerArtworkInternal_$sharedIdentity"
                                                    ),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                    resizeMode = ResizeMode.RemeasureToBounds
                                                ),
                                            size = animateCoverSize,
                                            corner = 9.dp,
                                            song = state.currentSong,
                                            defaultBgColor = Color(0xFF606063),
                                            defaultIconColor = Color(0xFF737376)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .sharedElementWithCallerManagedVisibility(
                                                sharedContentState = rememberSharedContentState(
                                                    key = "playerInfoOverlay_$sharedIdentity"
                                                ),
                                                visible = playerVisible
                                            )
                                            .sharedBounds(
                                                sharedContentState = rememberSharedContentState(
                                                    key = "playerInfoInternal_$sharedIdentity"
                                                ),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                resizeMode = ResizeMode.RemeasureToBounds
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
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

                                        Spacer(modifier = Modifier.width(32.dp))
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                            PlayerPanel.LYRICS -> {
                                // controls 收起时 bottom padding 随动画归零，歌词全屏
                                val lyricsBottomPadding = innerPadding.calculateBottomPadding() * (1f - controlsSlide)
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .padding(top = innerPadding.calculateTopPadding() + 20.dp, bottom = lyricsBottomPadding)
                                ) {
                                    // 紧凑头部：封面 + 标题横排，支持拖动关闭 Screen
                                    CompactNowPlayingHeader(
                                        state = state,
                                        playerVisible = playerVisible,
                                        animatedContentScope = this@AnimatedContent,
                                        onCoverClick = {
                                            activePanel = PlayerPanel.NONE
                                        },
                                        dragState = rememberDraggableState { delta ->
                                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                                        },
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

                                    // 歌词视图
                                    com.aria.rythme.ui.component.LyricsView(
                                        lyricsData = state.lyricsData,
                                        lyricsStatus = state.lyricsStatus,
                                        currentLyricIndex = state.currentLyricIndex,
                                        onSeekToLine = { index ->
                                            viewModel.sendIntent(PlayerIntent.SeekToLyricLine(index))
                                        },
                                        isFullScreen = !controlsVisible,
                                        onToggleControls = { controlsVisible = true },
                                        onUserScrolling = { scrollingDown ->
                                            controlsVisible = !scrollingDown
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            PlayerPanel.QUEUE -> {
                                QueueHistoryPanel(
                                    state = state,
                                    playerVisible = playerVisible,
                                    scope = scope,
                                    animatedContentScope = this@AnimatedContent,
                                    viewModel = viewModel,
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

/**
 * 紧凑头部：封面 + 标题横排（歌词面板用）
 */
@Composable
internal fun SharedTransitionScope.CompactNowPlayingHeader(
    state: PlayerState,
    playerVisible: Boolean,
    animatedContentScope: AnimatedContentScope,
    onCoverClick: () -> Unit,
    dragState: DraggableState? = null,
    onDragStopped: (suspend CoroutineScope.(Float) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (dragState != null) Modifier.draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = onDragStopped ?: {}
                ) else Modifier
            )
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverItem(
            modifier = Modifier
                .sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(
                        key = "playerArtworkOverlay_${state.currentQueueEntryIdentity}"
                    ),
                    visible = playerVisible
                )
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "playerArtworkInternal_${state.currentQueueEntryIdentity}"
                    ),
                    animatedVisibilityScope = animatedContentScope,
                    resizeMode = ResizeMode.RemeasureToBounds
                )
                .clickable(interactionSource = null, indication = null) {
                    onCoverClick()
                },
            size = 70.dp,
            corner = 12.dp,
            song = state.currentSong,
            defaultBgColor = Color(0xFF606063),
            defaultIconColor = Color(0xFF737376)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(
                        key = "playerInfoOverlay_${state.currentQueueEntryIdentity}"
                    ),
                    visible = playerVisible
                )
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "playerInfoInternal_${state.currentQueueEntryIdentity}"
                    ),
                    animatedVisibilityScope = animatedContentScope,
                    resizeMode = ResizeMode.RemeasureToBounds
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.currentSong?.title ?: stringResource(R.string.not_play),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (!state.currentSong?.artist.isNullOrEmpty()) {
                    Text(
                        text = state.currentSong?.artist.orEmpty(),
                        fontSize = 14.sp,
                        color = Color(0x80FFFFFF),
                        maxLines = 1
                    )
                }
            }

        }
    }
}

/**
 * 播放列表面板（折叠头部 + 双列表方案）
 */
