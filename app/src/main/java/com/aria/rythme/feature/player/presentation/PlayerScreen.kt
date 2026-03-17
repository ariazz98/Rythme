package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.ui.layout.onSizeChanged
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.core.utils.GradientColors
import com.aria.rythme.core.utils.ImageColorExtractor
import com.aria.rythme.core.utils.rememberScreenCornerRadiusDp
import com.aria.rythme.ui.component.PlaylistPanelState
import com.aria.rythme.ui.component.rememberPlaylistSnapFlingBehavior
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import com.aria.rythme.ui.component.CoverItem
import com.aria.rythme.ui.component.HistoryListItem
import com.aria.rythme.ui.component.NextIcon
import com.aria.rythme.ui.component.PlayListItem
import com.aria.rythme.ui.component.PlayPauseIcon
import com.aria.rythme.ui.component.PreviousIcon
import com.aria.rythme.ui.component.ProgressItem
import com.aria.rythme.ui.component.SongListItem
import com.aria.rythme.ui.component.VoiceItem
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 播放器底部面板状态
 */
private enum class PlayerPanel { NONE, LYRICS, PLAYLIST }

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

    var activePanel by remember { mutableStateOf(PlayerPanel.NONE) }
    var controlsVisible by remember { mutableStateOf(true) }

    // 当播放器打开时重置拖动偏移
    LaunchedEffect(playerVisible) {
        if (playerVisible) {
            dragOffsetY.snapTo(0f)
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

    // 操作区（底部浮层，歌词模式播放中可自动隐藏）
    val autoHideActive = state.isPlaying && activePanel == PlayerPanel.LYRICS
    val showControls = !autoHideActive || controlsVisible
    val controlsSlide by animateFloatAsState(
        targetValue = if (showControls) 0f else 1f,
        animationSpec = tween(400)
    )

    val stickyBackdrop = rememberLayerBackdrop()

    with(sharedTransitionScope) {
        AnimatedVisibility(
            visible = playerVisible,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                Box(modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(stickyBackdrop)
                    .background(backgroundBrush)
                )

                Scaffold(
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .clickable(interactionSource = null, indication = null) { }
                        .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "playerContainer"),
                            animatedVisibilityScope = this@AnimatedVisibility,
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
                        .then(
                            if (sharedTransitionScope.isTransitionActive || dragOffsetY.value > 0)
                                Modifier.clip(ContinuousRoundedRectangle(rememberScreenCornerRadiusDp()))
                            else
                                Modifier
                        ),
                    topBar = {
                        // Handle bar（顶部）
                        Box(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
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
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Icon(
                                    painter = painterResource(R.drawable.ic_airplay),
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )

                                Box(
                                    modifier = Modifier.size(40.dp)
                                        .then(if (activePanel == PlayerPanel.PLAYLIST) {
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
                                                activePanel = if (activePanel == PlayerPanel.PLAYLIST) PlayerPanel.NONE else PlayerPanel.PLAYLIST
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_play_list),
                                        contentDescription = "",
                                        tint = Color.White,
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
                                                    sharedContentState = rememberSharedContentState(key = "cover"),
                                                    visible = playerVisible
                                                )
                                                .sharedBounds(
                                                    sharedContentState = rememberSharedContentState(key = "playerCover"),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                    resizeMode = ResizeMode.RemeasureToBounds
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

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .sharedBounds(
                                                sharedContentState = rememberSharedContentState(key = "playerSongInfo"),
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
                                }
                            }
                            PlayerPanel.LYRICS -> {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .padding(top = innerPadding.calculateTopPadding() + 20.dp, bottom = innerPadding.calculateBottomPadding())
                                        .clickable(
                                            interactionSource = null,
                                            indication = null
                                        ) { controlsVisible = !controlsVisible }
                                ) {
                                    // 紧凑头部：封面 + 标题横排
                                    CompactNowPlayingHeader(
                                        state = state,
                                        playerVisible = playerVisible,
                                        onGradientColorsChange = { gradientColors = it },
                                        scope = scope,
                                        animatedContentScope = this@AnimatedContent
                                    )

                                    // 歌词占位
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            }
                            PlayerPanel.PLAYLIST -> {
                                PlaylistPanel(
                                    state = state,
                                    playerVisible = playerVisible,
                                    onGradientColorsChange = { gradientColors = it },
                                    scope = scope,
                                    animatedContentScope = this@AnimatedContent,
                                    viewModel = viewModel,
                                    innerPadding = innerPadding,
                                    stickyBackdrop = stickyBackdrop
                                )
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
private fun SharedTransitionScope.CompactNowPlayingHeader(
    state: PlayerState,
    playerVisible: Boolean,
    onGradientColorsChange: (GradientColors) -> Unit,
    scope: CoroutineScope,
    animatedContentScope: AnimatedContentScope
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverItem(
            modifier = Modifier
                .sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(key = "cover"),
                    visible = playerVisible
                )
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "playerCover"),
                    animatedVisibilityScope = animatedContentScope,
                    resizeMode = ResizeMode.RemeasureToBounds
                ),
            size = 72.dp,
            corner = 12.dp,
            song = state.currentSong,
            defaultBgColor = Color(0xFF606063),
            defaultIconColor = Color(0xFF737376),
            onBitmapReady = { bitmap ->
                if (bitmap != null) {
                    scope.launch {
                        onGradientColorsChange(ImageColorExtractor.extractGradientColors(bitmap))
                        bitmap.recycle()
                    }
                } else {
                    onGradientColorsChange(GradientColors())
                }
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "playerSongInfo"),
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
                        text = state.currentSong.artist,
                        fontSize = 14.sp,
                        color = Color(0x80FFFFFF),
                        maxLines = 1
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
            }
        }
    }
}

/**
 * 播放列表面板（单 LazyColumn + 吸附方案）
 */
@Composable
private fun SharedTransitionScope.PlaylistPanel(
    state: PlayerState,
    playerVisible: Boolean,
    onGradientColorsChange: (GradientColors) -> Unit,
    scope: CoroutineScope,
    animatedContentScope: AnimatedContentScope,
    viewModel: PlayerViewModel,
    innerPadding: PaddingValues,
    stickyBackdrop: Backdrop
) {
    val listState = rememberLazyListState()
    val panelState = remember { PlaylistPanelState() }
    val flingBehavior = rememberPlaylistSnapFlingBehavior(listState, panelState)

    // 更新动态数据
    panelState.historyCount = state.playHistory.size

    // 初始滚动到当前播放
    LaunchedEffect(Unit) {
        listState.scrollToItem(panelState.nowPlayingIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding() + 20.dp, bottom = innerPadding.calculateBottomPadding())
    ) {

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize()
        ) {
        // 播放历史（仅有历史时才渲染）
        if (state.playHistory.isNotEmpty()) {
            stickyHeader(key = "history_header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = stickyBackdrop,
                            shape = { RectangleShape },
                            effects = {},
                            highlight = null,
                            shadow = null
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.play_history),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    Text(
                        text = stringResource(R.string.play_history_clear),
                        fontSize = 14.sp,
                        color = Color(0x66FFFFFF),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            items(state.playHistory, key = { "h_${it.id}" }) { song ->
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    HistoryListItem(
                        song,
                        onClick = { viewModel.sendIntent(PlayerIntent.PlaySong(song)) }
                    )
                }
            }
        }

        // 当前播放（stickyHeader：到顶时顶走历史标题）
        stickyHeader(key = "now_playing") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = stickyBackdrop,
                        shape = { RectangleShape },
                        effects = {},
                        highlight = null,
                        shadow = null
                    )
                    .onSizeChanged { size ->
                        panelState.nowPlayingHeightPx = size.height.toFloat()
                    }
            ) {
                CompactNowPlayingHeader(
                    state = state,
                    playerVisible = playerVisible,
                    onGradientColorsChange = onGradientColorsChange,
                    scope = scope,
                    animatedContentScope = animatedContentScope
                )
            }
        }

        // 四个按钮（stickyHeader 吸顶）
        stickyHeader(key = "action_buttons") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = stickyBackdrop,
                        shape = { RectangleShape },
                        effects = {},
                        highlight = null,
                        shadow = null
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(39.dp)
                        .background(Color(0x33FFFFFF), ContinuousCapsule)
                        .clickable(interactionSource = null, indication = null) { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shuffle_hard),
                        contentDescription = "",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(39.dp)
                        .background(Color(0x33FFFFFF), ContinuousCapsule)
                        .clickable(interactionSource = null, indication = null) { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = "",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(39.dp)
                        .background(Color(0x33FFFFFF), ContinuousCapsule),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_infinite),
                        contentDescription = "",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(39.dp)
                        .background(Color(0x33FFFFFF), ContinuousCapsule),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cross_fade),
                        contentDescription = "",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 播放列表
        items(state.playlist, key = { it.id }) { song ->
            val index = state.playlist.indexOf(song)
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                PlayListItem(
                    song,
                    onClick = {
                        viewModel.sendIntent(PlayerIntent.SelectSongFromPlaylist(index))
                    }
                )
            }
        }
        }
    }
}