package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.ui.component.CoverItem
import com.aria.rythme.ui.component.LyricsView
import com.aria.rythme.ui.component.LyricsScrollState
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.OverlayMenu
import kotlinx.coroutines.delay

/** MiniPlayer 只参与外部共享作用域；封面/队列/歌词使用播放器内部的作用域。 */
@Composable
private fun miniPlayerSharedElement(key: String, visible: Boolean): Modifier =
    with(LocalSharedTransitionScope.current) {
        Modifier.sharedElementWithCallerManagedVisibility(
            sharedContentState = rememberSharedContentState(key),
            visible = visible,
            boundsTransform = playerOverlayBounds()
        )
    }

@Composable
internal fun SharedTransitionScope.NowPlayingPanel(
    state: PlayerState,
    playerVisible: Boolean,
    animatedContentScope: AnimatedContentScope,
    innerPadding: PaddingValues,
    coverSize: Dp,
    onArtworkBottom: (Float) -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: (Rect) -> Unit
) {
    val sharedIdentity = state.currentQueueEntryIdentity

    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CoverItem(
                cachePlaceholderForTransition = true,
                modifier = Modifier
                    .onGloballyPositioned { onArtworkBottom(it.boundsInRoot().bottom) }
                    .then(miniPlayerSharedElement("playerArtworkOverlay_$sharedIdentity", playerVisible))
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = "playerArtworkInternal_$sharedIdentity"
                        ),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = PlayerPanelMotion.artworkBounds
                    ),
                size = coverSize,
                corner = 9.dp,
                song = state.currentSong,
                defaultBgColor = playerCoverBackground(),
                defaultIconColor = playerCoverIcon()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "playerInfoInternal_$sharedIdentity"
                    ),
                    animatedVisibilityScope = animatedContentScope,
                    resizeMode = ResizeMode.RemeasureToBounds,
                    boundsTransform = PlayerPanelMotion.titleBounds,
                    enter = fadeIn(tween(160, delayMillis = 200)),
                    exit = fadeOut(tween(100, delayMillis = 50))
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TextWithEdgeFade(
                    text = state.currentSong?.title ?: stringResource(R.string.not_play),
                    modifier = Modifier.padding(start = 32.dp)
                )
                if (!state.currentSong?.artist.isNullOrEmpty()) {
                    Text(
                        text = state.currentSong?.artist.orEmpty(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0x80FFFFFF),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            NowPlayingActions(
                visible = state.currentSong != null,
                songId = state.currentSong?.id,
                isFavorite = state.isCurrentSongFavorite,
                onFavoriteClick = onFavoriteClick,
                onMoreClick = onMoreClick
            )

            Spacer(modifier = Modifier.width(32.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
internal fun SharedTransitionScope.LyricsPanel(
    state: PlayerState,
    playerVisible: Boolean,
    animatedContentScope: AnimatedContentScope,
    innerPadding: PaddingValues,
    controlsSlide: Float,
    controlsVisible: Boolean,
    modifier: Modifier,
    onBackToNowPlaying: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: (Rect) -> Unit,
    onSeekToLine: (Int) -> Unit,
    onControlsVisibleChange: (Boolean) -> Unit,
    lyricsScrollState: LyricsScrollState,
    headerPlaceholderHeight: Dp? = null,
    bodyTransitionModifier: Modifier = Modifier
) {
    val referenceScale = PlayerLayoutMetrics.scale(LocalWindowInfo.current.containerDpSize.width.value)
    // 和队列共用紧凑头部；只让歌词正文淡入，不重复淡化共享封面和歌名。
    val bodyAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (animatedContentScope.transition.currentState == EnterExitState.PreEnter) {
            delay(PlayerPanelMotion.Duration.toLong())
            bodyAlpha.animateTo(1f, tween(300))
        } else bodyAlpha.snapTo(1f)
    }
    val lyricsBottomPadding = innerPadding.calculateBottomPadding() * (1f - controlsSlide)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + (PlayerLayoutMetrics.QueueTopGap * referenceScale).dp,
                bottom = lyricsBottomPadding
            )
    ) {
        if (headerPlaceholderHeight != null) Spacer(Modifier.height(headerPlaceholderHeight))
        else CompactNowPlayingHeader(
            state = state,
            playerVisible = playerVisible,
            animatedContentScope = animatedContentScope,
            onCoverClick = onBackToNowPlaying,
            onFavoriteClick = onFavoriteClick,
            onMoreClick = onMoreClick,
            referenceScale = referenceScale,
            modifier = modifier
        )

        // 换歌时丢弃上一首的浏览锚点和恢复计时器；不重建数据层。
        key(state.currentQueueEntryIdentity) {
        LyricsView(
            lyricsData = state.lyricsData,
            lyricsStatus = state.lyricsStatus,
            currentLyricIndex = state.currentLyricIndex,
            onSeekToLine = onSeekToLine,
            isFullScreen = !controlsVisible,
            onToggleControls = { onControlsVisibleChange(true) },
            scrollState = lyricsScrollState,
            isPlaying = state.isPlaying,
            currentPositionMs = state.currentPosition,
            positionDiscontinuity = state.positionDiscontinuity,
            onControlsVisibleChange = onControlsVisibleChange,
            referenceScale = referenceScale,
            modifier = Modifier.fillMaxSize().then(bodyTransitionModifier)
                .then(with(animatedContentScope) {
                    Modifier.animateEnterExit(enter = androidx.compose.animation.EnterTransition.None,
                        exit = fadeOut(tween(150)))
                })
                .graphicsLayer { alpha = bodyAlpha.value }
        )
        }
    }
}

/** 紧凑头部由 Lyrics 与 Queue 共享。 */
@Composable
internal fun SharedTransitionScope.CompactNowPlayingHeader(
    state: PlayerState,
    playerVisible: Boolean,
    animatedContentScope: AnimatedContentScope,
    onCoverClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    referenceScale: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(horizontal = 32.dp * referenceScale,
                vertical = PlayerLayoutMetrics.CompactVerticalPadding.dp * referenceScale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverItem(
            cachePlaceholderForTransition = true,
            modifier = Modifier
                .then(miniPlayerSharedElement("playerArtworkOverlay_${state.currentQueueEntryIdentity}", playerVisible))
                .sharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = "playerArtworkInternal_${state.currentQueueEntryIdentity}"
                    ),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = PlayerPanelMotion.artworkBounds
                )
                .clickable(interactionSource = null, indication = null, onClick = onCoverClick),
            size = PlayerLayoutMetrics.CompactCoverSize.dp * referenceScale,
            corner = 12.dp * referenceScale,
            song = state.currentSong,
            defaultBgColor = playerCoverBackground(),
            defaultIconColor = playerCoverIcon()
        )

        Spacer(modifier = Modifier.width(12.dp * referenceScale))

        Row(
            modifier = Modifier
                .weight(1f)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "playerInfoInternal_${state.currentQueueEntryIdentity}"
                    ),
                    animatedVisibilityScope = animatedContentScope,
                    resizeMode = ResizeMode.RemeasureToBounds,
                    boundsTransform = PlayerPanelMotion.titleBounds,
                    enter = fadeIn(tween(160, delayMillis = 200)),
                    exit = fadeOut(tween(100, delayMillis = 50))
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.currentSong?.title ?: stringResource(R.string.not_play),
                    fontSize = (16f * referenceScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (!state.currentSong?.artist.isNullOrEmpty()) {
                    Text(
                        text = state.currentSong?.artist.orEmpty(),
                        fontSize = (14f * referenceScale).sp,
                        color = Color(0x80FFFFFF),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            NowPlayingActions(
                visible = state.currentSong != null,
                songId = state.currentSong?.id,
                isFavorite = state.isCurrentSongFavorite,
                onFavoriteClick = onFavoriteClick,
                onMoreClick = onMoreClick
            )
        }
    }
}

/** 播放页和紧凑标题共用的收藏/更多入口，避免两套 UI 再次发生漂移。 */
@Composable
private fun NowPlayingActions(
    visible: Boolean,
    songId: Long?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onMoreClick: (Rect) -> Unit
) {
    if (!visible) return

    var moreBounds by remember { mutableStateOf(Rect.Zero) }
    val scale = PlayerLayoutMetrics.scale(LocalWindowInfo.current.containerDpSize.width.value)
    val menu = LocalOverlayMenu.current.presentedSongContext
    val menuVisible = menu?.song?.id == songId

    Row(verticalAlignment = Alignment.CenterVertically) {
        PlayerPressFeedback(size = 32.dp * scale) { interactions ->
        Box(
            modifier = Modifier
                .size(32.dp * scale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    onClick = onFavoriteClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star_normal),
                contentDescription = "收藏",
                tint = if (isFavorite) Color(0xFFFF375F) else Color.White,
                modifier = Modifier.size(18.dp * scale)
            )
        }
        }

        Spacer(modifier = Modifier.width(12.dp * scale))

        PlayerPressFeedback(
            size = 32.dp * scale,
            modifier = Modifier.graphicsLayer { alpha = if (menuVisible) 0f else 1f }
        ) { interactions ->
        Box(
            modifier = Modifier
                .size(32.dp * scale)
                .onGloballyPositioned { moreBounds = it.boundsInWindow() }
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    onClick = { onMoreClick(moreBounds) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = "更多",
                tint = Color.White,
                modifier = Modifier.size(22.dp * scale)
            )
        }
        }
    }
}

@Composable
private fun TextWithEdgeFade(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
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
            .then(modifier)
    )
}
