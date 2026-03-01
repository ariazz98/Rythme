package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.domain.model.RepeatMode
import com.aria.rythme.core.utils.rememberScreenCornerRadiusDp
import com.aria.rythme.ui.component.CoverItem
import com.aria.rythme.ui.component.ProgressItem
import com.aria.rythme.ui.component.VoiceItem
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
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

    val width = LocalWindowInfo.current.containerDpSize.width

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

    Column(modifier = Modifier
        .fillMaxSize()
        .clip(ContinuousRoundedRectangle(rememberScreenCornerRadiusDp()))
        .background(Brush.verticalGradient(
            colors = listOf(Color(0xFF6B6B6E), Color(0xFF6A6A6D), Color(0xFF404042)),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.statusBarsPadding().height(16.dp))

        Box(
            modifier = Modifier
                .width(62.dp)
                .height(6.dp)
                .clip(ContinuousCapsule)
                .background(Color(0xFFB1B1B9))
                .clickable(interactionSource = null, indication = null) {
                    onBack()
                }
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

            CoverItem(
                size = animateCoverSize,
                corner = 9.dp,
                song = state.currentSong,
                defaultBgColor = Color(0xFF606063),
                defaultIconColor = Color(0xFF737376)
            )

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_previous),
                    contentDescription = "",
                    tint = if (state.canPlayPrevious) Color.White else Color(0x33FFFFFF),
                    modifier = Modifier.size(40.dp)
                        .clickable(interactionSource = null, indication = null) {
                            viewModel.sendIntent(PlayerIntent.Previous)
                        }
                )

                Icon(
                    painter = if (state.isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play),
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                        .clickable(interactionSource = null, indication = null) {
                            if (state.currentSong == null) {
                                // 如果当前没有歌曲播放，加载并随机播放一首
                                viewModel.sendIntent(PlayerIntent.LoadAndPlayRandom)
                            } else {
                                // 否则切换播放/暂停状态
                                viewModel.sendIntent(PlayerIntent.TogglePlayPause)
                            }
                        }
                )

                Icon(
                    painter = painterResource(R.drawable.ic_next),
                    contentDescription = "",
                    tint = if (state.canPlayNext) Color.White else Color(0x33FFFFFF),
                    modifier = Modifier.size(40.dp)
                        .clickable(interactionSource = null, indication = null) {
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

/**
 * 专辑封面组件
 *
 * @param song 当前歌曲
 * @param isPlaying 是否正在播放
 * @param onThemeColorExtracted 主题色提取回调
 */
@Composable
private fun AlbumCover(
    song: Song?,
    isPlaying: Boolean,
    onThemeColorExtracted: (Int) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        label = "album_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
            .scale(scale)
            .clip(ContinuousRoundedRectangle(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (song?.coverUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "专辑封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { result ->
                }
            )
        } else {
            // 默认封面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 进度条组件
 *
 * @param progress 当前进度（0-1）
 * @param currentPosition 当前位置文本
 * @param duration 总时长文本
 * @param onSeek 拖动回调
 */
@Composable
private fun ProgressSlider(
    progress: Float,
    currentPosition: String,
    duration: String,
    onSeek: (Float) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(progress) }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                onSeek(sliderPosition)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // 时间显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentPosition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 同步进度
    LaunchedEffect(progress) {
        sliderPosition = progress
    }
}

/**
 * 播放控制组件
 *
 * @param isPlaying 是否正在播放
 * @param repeatMode 循环模式
 * @param isShuffleEnabled 是否随机播放
 * @param onPlayPauseClick 播放/暂停点击
 * @param onNextClick 下一首点击
 * @param onPreviousClick 上一首点击
 * @param onRepeatClick 循环模式点击
 * @param onShuffleClick 随机播放点击
 */
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    isShuffleEnabled: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 随机播放按钮
        ControlButton(
            icon = Icons.Default.Shuffle,
            contentDescription = "随机播放",
            isActive = isShuffleEnabled,
            onClick = onShuffleClick
        )

        // 上一首
        ControlButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "上一首",
            size = 48.dp,
            onClick = onPreviousClick
        )

        // 播放/暂停
        PlayPauseButton(
            isPlaying = isPlaying,
            onClick = onPlayPauseClick
        )

        // 下一首
        ControlButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "下一首",
            size = 48.dp,
            onClick = onNextClick
        )

        // 循环模式
        val repeatIcon = when (repeatMode) {
            RepeatMode.OFF -> Icons.Default.Repeat
            RepeatMode.ONE -> Icons.Default.RepeatOne
            RepeatMode.ALL -> Icons.Default.Repeat
        }
        ControlButton(
            icon = repeatIcon,
            contentDescription = "循环模式",
            isActive = repeatMode != RepeatMode.OFF,
            onClick = onRepeatClick
        )
    }
}

/**
 * 播放/暂停按钮
 */
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * 控制按钮
 */
@Composable
private fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.6f),
            tint = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
