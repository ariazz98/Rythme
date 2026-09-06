package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.ui.component.CoverItem
import com.aria.rythme.ui.component.LyricsView

@Composable
internal fun SharedTransitionScope.NowPlayingPanel(
    state: PlayerState,
    playerVisible: Boolean,
    animatedContentScope: AnimatedContentScope,
    innerPadding: PaddingValues,
    coverSize: Dp,
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
                        animatedVisibilityScope = animatedContentScope,
                        resizeMode = ResizeMode.RemeasureToBounds
                    ),
                size = coverSize,
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
                    animatedVisibilityScope = animatedContentScope,
                    resizeMode = ResizeMode.RemeasureToBounds
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0x80FFFFFF),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            NowPlayingActions(
                visible = state.currentSong != null,
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
    onControlsVisibleChange: (Boolean) -> Unit
) {
    val lyricsBottomPadding = innerPadding.calculateBottomPadding() * (1f - controlsSlide)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + 20.dp,
                bottom = lyricsBottomPadding
            )
    ) {
        CompactNowPlayingHeader(
            state = state,
            playerVisible = playerVisible,
            animatedContentScope = animatedContentScope,
            onCoverClick = onBackToNowPlaying,
            onFavoriteClick = onFavoriteClick,
            onMoreClick = onMoreClick,
            modifier = modifier
        )

        LyricsView(
            lyricsData = state.lyricsData,
            lyricsStatus = state.lyricsStatus,
            currentLyricIndex = state.currentLyricIndex,
            onSeekToLine = onSeekToLine,
            isFullScreen = !controlsVisible,
            onToggleControls = { onControlsVisibleChange(true) },
            onUserScrolling = { scrollingDown ->
                onControlsVisibleChange(!scrollingDown)
            },
            modifier = Modifier.fillMaxSize()
        )
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
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
                .clickable(interactionSource = null, indication = null, onClick = onCoverClick),
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

            Spacer(modifier = Modifier.width(8.dp))

            NowPlayingActions(
                visible = state.currentSong != null,
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
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onMoreClick: (Rect) -> Unit
) {
    if (!visible) return

    var moreBounds by remember { mutableStateOf(Rect.Zero) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x30FFFFFF))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onFavoriteClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = "收藏",
                tint = if (isFavorite) Color(0xFFFF375F) else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .onGloballyPositioned { moreBounds = it.boundsInWindow() }
                .clip(CircleShape)
                .background(Color(0x30FFFFFF))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { onMoreClick(moreBounds) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = "更多",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
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
