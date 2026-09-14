package com.aria.rythme.feature.player.presentation

import android.content.Context
import android.media.MediaRouter2
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.core.view.WindowCompat
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
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
import com.aria.rythme.ui.component.LyricsScrollState
import com.aria.rythme.ui.component.LyricsPresentationMotion
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

/** 播放器尺寸按 393pt 屏宽归一；空状态与暂停态使用小封面，播放态使用大封面。 */
internal object PlayerLayoutMetrics {
    fun scale(width: Float): Float =
        if (width.isFinite() && width > 0f) (width / 393f).coerceAtMost(1f) else 1f
    const val CoverSize = 252f
    const val PlayingCoverSize = 350f
    const val QueueTopGap = 12f
    const val CompactCoverSize = 70f
    const val CompactVerticalPadding = 12f
    const val HandleWidth = 60f
    const val HandleHeight = 4f
    const val PlaySize = 37f
    const val SkipHeight = 20f
    const val ToolSize = 22.5f
    // 控件从底部锚定，封面区域仍消化不同屏幕高度的剩余空间。
    const val ProgressToTransport = 41f
    const val TransportToVolume = 68f
    const val VolumeToTools = 19f
    const val BottomSpace = 44f
}

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
    val referenceScale = PlayerLayoutMetrics.scale(width.value)
    val empty = state.currentSong == null
    val activity = LocalActivity.current
    // 播放器始终使用深底；关闭后归还原页面的系统栏样式。
    if (playerVisible) DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller?.isAppearanceLightStatusBars = previous }
    }
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalWindowInfo.current.containerDpSize.height.toPx()
    }
    val dismissThreshold = screenHeightPx * 0.35f
    val velocityThreshold = 2000f

    val scope = rememberCoroutineScope()
    val dragOffsetYState = remember { mutableFloatStateOf(0f) }
    val containerShape = rememberPlayerContainerShape()
    var dragOffsetY by dragOffsetYState
    val dismissMotionState = LocalPlayerDismissMotion.current
    val dismissMotion = dismissMotionState.value
    val dismissFromDrag: (Float) -> Unit = { velocity ->
        dismissMotionState.value = PlayerDismissMotion(velocity, screenHeightPx - dragOffsetY)
        onBack()
    }
    // 在共享元素首次测量展开目标之前重置，不能等 LaunchedEffect 的下一阶段。
    var previousPlayerVisible by remember { mutableStateOf(false) }
    if (previousPlayerVisible != playerVisible) {
        previousPlayerVisible = playerVisible
        if (playerVisible) {
            dragOffsetY = 0f
            dismissMotionState.value = PlayerDismissMotion()
        }
    }

    var activePanel by remember { mutableStateOf(PlayerPanel.NONE) }
    var expandedArtworkBottom by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    val lyricsScrollState = remember(state.currentQueueEntryIdentity) { LyricsScrollState() }
    val overlayMenu = LocalOverlayMenu.current
    val hasSyncedLyrics = !state.lyricsData?.lines.isNullOrEmpty()
    val onFavoriteClick = viewModel::toggleCurrentSongFavorite
    val onMoreClick: (Rect) -> Unit = { bounds ->
        state.currentSong?.let { song ->
            overlayMenu.show(
                OverlayMenu.SongContext(
                    song = song,
                    anchorBounds = bounds,
                    sourceIconSize = 22.dp * referenceScale,
                    sourceIconTint = Color.White,
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
            controlsVisible = true
            lyricsScrollState.reset(SystemClock.uptimeMillis())
        }
    }

    LaunchedEffect(hasSyncedLyrics) {
        if (!hasSyncedLyrics) {
            controlsVisible = true
        }
    }

    LaunchedEffect(state.canShowLyrics) {
        if (!state.canShowLyrics && activePanel == PlayerPanel.LYRICS) activePanel = PlayerPanel.NONE
    }

    LaunchedEffect(state.isPlaying, lyricsScrollState) {
        lyricsScrollState.onPlaybackChanged(state.isPlaying, SystemClock.uptimeMillis())
    }
    LaunchedEffect(overlayMenu.isVisible) {
        if (activePanel == PlayerPanel.LYRICS && !overlayMenu.isVisible && !lyricsScrollState.isTouching) {
            lyricsScrollState.onTouchReleased(SystemClock.uptimeMillis())
        }
    }

    // 模糊和回归阶段不再各开计时器；控制区仅在已跟随、无触摸、播放中的空闲时隐藏。
    LaunchedEffect(state.isPlaying, activePanel, controlsVisible, hasSyncedLyrics, playerVisible,
        lyricsScrollState.mode, lyricsScrollState.isTouching, lyricsScrollState.lastInteractionEndMs,
        overlayMenu.isVisible) {
        if (
            playerVisible && state.isPlaying &&
            activePanel == PlayerPanel.LYRICS &&
            controlsVisible &&
            hasSyncedLyrics && lyricsScrollState.isAutoFollow && !lyricsScrollState.isTouching &&
            !overlayMenu.isVisible
        ) {
            delay((lyricsScrollState.lastInteractionEndMs + LyricsScrollState.CONTROLS_HIDE_DELAY -
                SystemClock.uptimeMillis()).coerceAtLeast(0))
            if (lyricsScrollState.canAutoHideControls(SystemClock.uptimeMillis(), state.isPlaying)) {
                controlsVisible = false
            }
        }
    }

    // 切换面板时重置操作区可见
    LaunchedEffect(activePanel) {
        controlsVisible = true
        lyricsScrollState.reset(SystemClock.uptimeMillis())
    }

    val animateCoverSize by animateDpAsState(
        targetValue = when {
            empty -> (PlayerLayoutMetrics.CoverSize * referenceScale).dp
            state.isPlaying -> (PlayerLayoutMetrics.PlayingCoverSize * referenceScale).dp
            else -> (PlayerLayoutMetrics.CoverSize * referenceScale).dp
        },
        animationSpec = if (state.isPlaying) {
            spring(dampingRatio = 0.65f, stiffness = 180f)
        } else {
            tween(durationMillis = 500, easing = FastOutSlowInEasing)
        }
    )

    // 操作区（底部浮层：歌词模式自动隐藏 / 播放列表拖动时隐藏）
    val showControls = controlsVisible
    val controlsSlide by animateFloatAsState(
        targetValue = if (showControls) 0f else 1f,
        animationSpec = tween(if (activePanel == PlayerPanel.LYRICS && !showControls)
            LyricsPresentationMotion.ControlsHideMs else LyricsPresentationMotion.ControlsShowMs)
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
            val overlayProgress by transition.animateFloat(
                transitionSpec = { tween(
                    if (targetState == EnterExitState.Visible) PlayerOverlayMotion.ExpandMs else dismissMotion.durationMs,
                    easing = if (targetState == EnterExitState.Visible) PlayerOverlayMotion.ExpandEasing else dismissMotion.easing) },
                label = "playerOverlayProgress"
            ) { if (it == EnterExitState.Visible) 1f else 0f }
            val backgroundAlphaState = transition.animateFloat(
                transitionSpec = { tween(
                    if (targetState == EnterExitState.Visible) PlayerOverlayMotion.BackgroundExpandMs
                    else dismissMotion.durationMs,
                    easing = if (targetState == EnterExitState.Visible) PlayerOverlayMotion.ExpandEasing else dismissMotion.easing) },
                label = "playerBackgroundAlpha"
            ) { if (it == EnterExitState.Visible) 1f else 0f }
            val backgroundAlpha by backgroundAlphaState
            val surfaceOpacity = LocalPlayerSurfaceOpaque.current
            androidx.compose.runtime.SideEffect { surfaceOpacity.animation = backgroundAlphaState }
            DisposableEffect(surfaceOpacity) { onDispose { surfaceOpacity.animation = null } }
            val viewport = LocalWindowInfo.current.containerDpSize
            val surfaceOpaque = LocalPlayerSurfaceOpaque.current
            // 必须在组合阶段读取 derivedState 的结果，不能只在 SideEffect 中读动画值。
            val opaqueNow = remember(playerVisible) {
                androidx.compose.runtime.derivedStateOf { playerVisible && backgroundAlpha == 1f }
            }.value
            androidx.compose.runtime.SideEffect { surfaceOpaque.value = opaqueNow }
            DisposableEffect(surfaceOpaque) { onDispose { surfaceOpaque.value = false } }

            Box(
                modifier = Modifier.fillMaxSize()
                    .pointerInput(activePanel, lyricsScrollState) {
                        if (activePanel != PlayerPanel.LYRICS) return@pointerInput
                        try {
                            awaitPointerEventScope {
                                while (true) {
                                    // 只观察，不消费：歌词点击、列表拖动和播放器滑杆仍由原组件处理。
                                    val pressed = awaitPointerEvent(PointerEventPass.Final).changes.any { it.pressed }
                                    if (pressed && !lyricsScrollState.isTouching) lyricsScrollState.onTouchDown()
                                    else if (!pressed && lyricsScrollState.isTouching) {
                                        lyricsScrollState.onTouchReleased(SystemClock.uptimeMillis())
                                    }
                                }
                            }
                        } finally {
                            if (lyricsScrollState.isTouching) lyricsScrollState.onTouchReleased(SystemClock.uptimeMillis())
                        }
                    }
                    .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                    .sharedBounds(
                        sharedContentState = playerContainerState,
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = playerOverlayBounds(),
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                        resizeMode = ResizeMode.RemeasureToBounds
                    )
                    .clickable(interactionSource = null, indication = null) { }
                    .draggable(
                        state = rememberDraggableState { delta ->
                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                        },
                        orientation = Orientation.Vertical,
                        enabled = activePanel == PlayerPanel.NONE,
                        onDragStopped = { velocity ->
                            if (dragOffsetY > dismissThreshold || velocity > velocityThreshold) {
                                dismissFromDrag(velocity)
                            } else {
                                scope.launch {
                                    animate(dragOffsetY, 0f) { value, _ ->
                                        dragOffsetY = value
                                    }
                                }
                            }
                        }
                    )
                    .then(if (dragOffsetY > 0 || containerTransitionActive)
                        Modifier.clip(containerShape)
                    else
                       Modifier
                    )
            ) {
                // 内容保持全屏布局，容器负责裁切；不在窄胶囊里重新排版播放器。
                Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(viewport.width, viewport.height)) {

                // 背景：渐变色兜底 + 有封面时叠加模糊封面（Apple Music 风格）
                Box(
                    modifier = Modifier
                        .fillMaxSize()

                        .layerBackdrop(stickyBackdrop)
                        .graphicsLayer {
                            alpha = backgroundAlpha
                            // 无封面时只有一次渐变绘制，不需要全屏离屏透明合成。
                            // 有封面时两层重叠，仍按整组透明度合成，避免改变过渡颜色。
                            compositingStrategy = if (state.currentSong?.coverUri == null)
                                CompositingStrategy.ModulateAlpha else CompositingStrategy.Auto
                        }
                        .background(defaultGradientBrush)
                ) {
                    val coverUri = state.currentSong?.coverUri
                    if (coverUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(coverUri)
                                // 解码尺寸不随中间图层缩小，避免改变封面采样来源。
                                .size(with(density) { viewport.width.roundToPx() },
                                    with(density) { viewport.height.roundToPx() })
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                // 背景是大半径低频模糊：半尺寸处理，再以双倍倍率还原。
                                // 最终覆盖仍为 1.5 倍，最终模糊仍为原来的 60dp × 1.5。
                                .align(Alignment.Center)
                                .requiredSize(viewport.width / 2f, viewport.height / 2f)
                                .graphicsLayer { scaleX = 3f; scaleY = 3f }
                                .blur(30.dp)
                        )
                    }
                }

                Box(Modifier.graphicsLayer {
                    alpha = PlayerOverlayMotion.contentAlpha(overlayProgress, playerVisible)
                    translationY = (1f - overlayProgress) * screenHeightPx * .12f
                }) {
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
                                .clickable(interactionSource = null, indication = null, onClick = onBack)
                                .draggable(
                                    state = rememberDraggableState { delta ->
                                        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                                    },
                                    orientation = Orientation.Vertical,
                                    onDragStopped = { velocity ->
                                        if (dragOffsetY > dismissThreshold || velocity > velocityThreshold) {
                                            dismissFromDrag(velocity)
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
                                    .width((PlayerLayoutMetrics.HandleWidth * referenceScale).dp)
                                    .height((PlayerLayoutMetrics.HandleHeight * referenceScale).dp)
                                    .clip(ContinuousCapsule)
                                    .background(Color.White.copy(alpha = .45f))
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
                                            dismissFromDrag(velocity)
                                        } else {
                                            scope.launch {
                                                animate(dragOffsetY, 0f) { value, _ ->
                                                    dragOffsetY = value
                                                }
                                            }
                                        }
                                    }
                                )
                                // 与歌曲信息留在同一个内容层，统一继承展开位移和透明度。
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
                                emptyReferenceScale = referenceScale,
                                centerLabel = { state.currentSong?.let { PlayerAudioInfoBadge(it) } },
                                onSeek = { viewModel.seekTo((it * state.duration).toLong()) }
                            )

                            Spacer(modifier = Modifier.height((PlayerLayoutMetrics.ProgressToTransport * referenceScale).dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerPressFeedback(
                                    size = (PlayerLayoutMetrics.PlaySize * referenceScale).dp,
                                    haloSize = (64f * referenceScale).dp
                                ) { interactions -> PreviousIcon(
                                    enable = state.canPlayPrevious,
                                    height = (PlayerLayoutMetrics.SkipHeight * referenceScale).dp,
                                    interactionSource = interactions,
                                    tint = if (state.canPlayPrevious) Color.White else Color.White.copy(alpha = if (empty) .4f else .2f),
                                    onClick = {
                                        viewModel.previous()
                                    }
                                ) }

                                PlayerPressFeedback(
                                    size = (PlayerLayoutMetrics.PlaySize * referenceScale).dp,
                                    haloSize = (64f * referenceScale).dp
                                ) { interactions -> PlayPauseIcon(
                                    isPlaying = state.isPlaying,
                                    size = (PlayerLayoutMetrics.PlaySize * referenceScale).dp,
                                    playIconRes = R.drawable.ic_player_play,
                                    pauseIconRes = R.drawable.ic_player_pause,
                                    interactionSource = interactions,
                                    animationSpec = tween(180),
                                    tint = Color.White,
                                    onClick = {
                                        if (state.currentSong == null) {
                                            viewModel.loadAndPlayRandom()
                                        } else {
                                            viewModel.togglePlayPause()
                                        }
                                    }
                                ) }

                                PlayerPressFeedback(
                                    size = (PlayerLayoutMetrics.PlaySize * referenceScale).dp,
                                    haloSize = (64f * referenceScale).dp
                                ) { interactions -> NextIcon(
                                    enable = state.canPlayNext,
                                    height = (PlayerLayoutMetrics.SkipHeight * referenceScale).dp,
                                    interactionSource = interactions,
                                    tint = if (state.canPlayNext) Color.White else Color.White.copy(alpha = if (empty) .4f else .2f),
                                    onClick = {
                                        viewModel.next()
                                    }
                                ) }

                            }

                            Spacer(modifier = Modifier.height((PlayerLayoutMetrics.TransportToVolume * referenceScale).dp))

                            VoiceItem(
                                progress = state.volume / 100f,
                                horizontalPadding = (32f * referenceScale).dp,
                                onSeek = { viewModel.setVolume((it * 100).toInt()) }
                            )

                            Spacer(modifier = Modifier.height((PlayerLayoutMetrics.VolumeToTools * referenceScale).dp))

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
                                            },
                                            enabled = state.canShowLyrics
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(if (activePanel == PlayerPanel.LYRICS) R.drawable.ic_lrc_full else R.drawable.ic_lrc),
                                        contentDescription = "歌词",
                                        tint = when {
                                            activePanel == PlayerPanel.LYRICS -> Color.White
                                            empty -> Color.White.copy(alpha = .12f)
                                            else -> Color(0x80FFFFFF)
                                        },
                                        modifier = Modifier.size((PlayerLayoutMetrics.ToolSize * referenceScale).dp)
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
                                        tint = Color.White.copy(alpha = .85f),
                                        modifier = Modifier.size((PlayerLayoutMetrics.ToolSize * referenceScale).dp)
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
                                        tint = if (activePanel == PlayerPanel.QUEUE) Color.White else Color.White.copy(alpha = .65f),
                                        modifier = Modifier.size((PlayerLayoutMetrics.ToolSize * referenceScale).dp)
                                    )
                                    if (activePanel != PlayerPanel.QUEUE) state.queuePlaybackBadge?.let { badge ->
                                        val icon = when (badge) {
                                            QueuePlaybackBadge.SHUFFLE -> R.drawable.ic_shuffle_hard
                                            QueuePlaybackBadge.REPEAT -> R.drawable.ic_repeat
                                            QueuePlaybackBadge.REPEAT_ONE -> R.drawable.ic_repeat_1
                                            QueuePlaybackBadge.AUTOPLAY -> R.drawable.ic_infinite
                                        }
                                        Box(
                                            Modifier.align(Alignment.TopEnd)
                                                .offset(x = 3.dp * referenceScale, y = (-3).dp * referenceScale)
                                                .size(20.dp * referenceScale)
                                                .background(Color.White.copy(alpha = .14f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(painterResource(icon), contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp * referenceScale))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height((PlayerLayoutMetrics.BottomSpace * referenceScale).dp))
                        }
                    }
                    ) { innerPadding ->
                    // 内部面板过渡不触发 MiniPlayer 的全局共享层和容器过渡状态。
                    SharedTransitionLayout {
                    AnimatedContent(
                        targetState = activePanel,
                        contentKey = { it != PlayerPanel.NONE },
                        transitionSpec = {
                            val panels = run {
                                // 保留两端直到共享元素结束，但不把整页一起淡化。
                                fadeIn(tween(PlayerPanelMotion.Duration), initialAlpha = 1f) togetherWith
                                    fadeOut(tween(PlayerPanelMotion.Duration), targetAlpha = 1f)
                            }
                            panels.using(SizeTransform(clip = false))
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
                                    onArtworkBottom = { bottom ->
                                        if (!this@SharedTransitionLayout.isTransitionActive &&
                                            !sharedTransitionScope.isTransitionActive) expandedArtworkBottom = bottom
                                    },
                                    onFavoriteClick = onFavoriteClick,
                                    onMoreClick = onMoreClick
                                )
                            }
                            else -> {
                                val largePanelScope = this@AnimatedContent
                                val headerGeometry = remember { CompactPanelHeaderGeometry() }
                                val queueHeaderWeight by animateFloatAsState(
                                    if (targetPanel == PlayerPanel.QUEUE) 1f else 0f, tween(300),
                                    label = "compactHeaderPlacement")
                                val headerHeight = with(density) {
                                    if (headerGeometry.heightPx > 0f) headerGeometry.heightPx.toDp()
                                    else ((PlayerLayoutMetrics.CompactCoverSize +
                                        2 * PlayerLayoutMetrics.CompactVerticalPadding) * referenceScale).dp
                                }
                                Box(Modifier.fillMaxSize()) {
                                    AnimatedContent(
                                        targetState = targetPanel,
                                        transitionSpec = {
                                            (fadeIn(tween(CompactPanelMotion.LyricsEnterMs + CompactPanelMotion.EnterDelayMs), initialAlpha = 1f)
                                                togetherWith fadeOut(tween(CompactPanelMotion.ExitMs), targetAlpha = 1f))
                                                .using(SizeTransform(clip = false))
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        label = "lyricsQueueBody"
                                    ) { compactPanel ->
                                        val lyrics = compactPanel == PlayerPanel.LYRICS
                                        val compactBodyMotion = Modifier.animateEnterExit(
                                            enter = fadeIn(tween(if (lyrics) 250 else 160,
                                                delayMillis = CompactPanelMotion.EnterDelayMs)) +
                                                slideInVertically(tween(
                                                    if (lyrics) CompactPanelMotion.LyricsEnterMs else CompactPanelMotion.QueueEnterMs,
                                                    delayMillis = CompactPanelMotion.EnterDelayMs,
                                                    easing = PlayerPanelMotion.Easing)) {
                                                    with(density) { ((if (lyrics) CompactPanelMotion.LyricsTravelDp
                                                        else CompactPanelMotion.QueueTravelDp).dp * referenceScale).roundToPx() }
                                                },
                                            exit = fadeOut(tween(CompactPanelMotion.ExitMs))
                                        )
                                        when (compactPanel) {
                                            PlayerPanel.LYRICS -> {
                                                LyricsPanel(
                                                    state = state,
                                                    playerVisible = playerVisible,
                                                    animatedContentScope = largePanelScope,
                                                    innerPadding = innerPadding,
                                                    controlsSlide = controlsSlide,
                                                    controlsVisible = controlsVisible,
                                                    modifier = Modifier,
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
                                                    },
                                                    lyricsScrollState = lyricsScrollState,
                                                    headerPlaceholderHeight = headerHeight,
                                                    bodyTransitionModifier = compactBodyMotion
                                                )
                                            }
                                            PlayerPanel.QUEUE -> {
                                                // 以真实大封面底边与紧凑头部底边之差驱动列表位移。
                                                val compactBottom = with(density) {
                                                    (innerPadding.calculateTopPadding() +
                                                        ((PlayerLayoutMetrics.QueueTopGap + PlayerLayoutMetrics.CompactVerticalPadding +
                                                            PlayerLayoutMetrics.CompactCoverSize) * referenceScale).dp).toPx()
                                                }
                                                val bodyTravel = (expandedArtworkBottom - compactBottom).coerceAtLeast(0f).roundToInt()
                                                QueueHistoryPanel(
                                                    state = state,
                                                    playerVisible = playerVisible,
                                                    scope = scope,
                                                    animatedContentScope = largePanelScope,
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
                                                    onDismiss = dismissFromDrag,
                                                    onListScrolling = { scrolling ->
                                                        if (activePanel == PlayerPanel.QUEUE) controlsVisible = !scrolling
                                                    },
                                                    onCoverClick = {
                                                        activePanel = PlayerPanel.NONE
                                                    },
                                                    controlsSlide = controlsSlide,
                                                    externalHeader = headerGeometry,
                                                    bodyTransitionModifier = with(largePanelScope) { Modifier.animateEnterExit(
                                                        enter = fadeIn(tween(250, delayMillis = 100)) +
                                                            slideInVertically(tween(PlayerPanelMotion.Duration, easing = PlayerPanelMotion.Easing)) { bodyTravel },
                                                        exit = fadeOut(tween(150)) +
                                                            slideOutVertically(tween(PlayerPanelMotion.Duration, easing = PlayerPanelMotion.Easing)) { bodyTravel }
                                                    ) }.then(compactBodyMotion)
                                                )
                                            }
                                            PlayerPanel.NONE -> Unit
                                        }
                                    }
                                CompactNowPlayingHeader(
                                    state = state,
                                    playerVisible = playerVisible,
                                    animatedContentScope = largePanelScope,
                                    onCoverClick = { activePanel = PlayerPanel.NONE },
                                    onFavoriteClick = onFavoriteClick,
                                    onMoreClick = onMoreClick,
                                    referenceScale = referenceScale,
                                    modifier = Modifier
                                        .padding(top = innerPadding.calculateTopPadding() +
                                            (PlayerLayoutMetrics.QueueTopGap * referenceScale).dp)
                                        .graphicsLayer {
                                            translationY = headerGeometry.historyOffsetPx * queueHeaderWeight
                                            clip = true
                                        }
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            headerGeometry.heightPx = placeable.height.toFloat()
                                            val collapse = (headerGeometry.collapsePx * queueHeaderWeight)
                                                .roundToInt().coerceIn(0, placeable.height)
                                            layout(placeable.width, placeable.height - collapse) {
                                                placeable.placeRelative(0, -collapse)
                                            }
                                        }
                                        .draggable(
                                            state = rememberDraggableState { delta ->
                                                dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                                            },
                                            orientation = Orientation.Vertical,
                                            onDragStopped = { velocity ->
                                                if (dragOffsetY > dismissThreshold || velocity > velocityThreshold) dismissFromDrag(velocity)
                                                else scope.launch { animate(dragOffsetY, 0f) { value, _ -> dragOffsetY = value } }
                                            }
                                        )
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
