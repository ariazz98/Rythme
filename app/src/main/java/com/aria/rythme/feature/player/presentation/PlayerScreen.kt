package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.player.data.model.Song
import org.koin.androidx.compose.koinViewModel

/**
 * 播放器页面
 *
 * 显示当前播放歌曲的信息和控制按钮。
 *
 * @param viewModel 播放器 ViewModel
 * @param onBack 返回回调
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = koinViewModel(),
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsUiState()

    // 加载歌曲列表
    LaunchedEffect(Unit) {
        viewModel.sendIntent(PlayerIntent.LoadSongs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = state.themeColor?.let { Color(it) }
                    ?: MaterialTheme.colorScheme.background
            )
    ) {
        // 返回按钮
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 专辑封面
            AlbumCover(
                song = state.currentSong,
                isPlaying = state.isPlaying,
                onThemeColorExtracted = { color ->
                    // 可以在这里更新主题色
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 歌曲信息
            SongInfo(
                title = state.currentSong?.title ?: "未知歌曲",
                artist = state.currentSong?.artist ?: "未知艺术家"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 进度条
            ProgressSlider(
                progress = state.progress,
                currentPosition = state.currentPositionText,
                duration = state.durationText,
                onSeek = { progress ->
                    val position = (progress * state.duration).toLong()
                    viewModel.sendIntent(PlayerIntent.SeekTo(position))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 播放控制
            PlaybackControls(
                isPlaying = state.isPlaying,
                repeatMode = state.repeatMode,
                isShuffleEnabled = state.isShuffleEnabled,
                onPlayPauseClick = {
                    viewModel.sendIntent(PlayerIntent.TogglePlayPause)
                },
                onNextClick = {
                    viewModel.sendIntent(PlayerIntent.Next)
                },
                onPreviousClick = {
                    viewModel.sendIntent(PlayerIntent.Previous)
                },
                onRepeatClick = {
                    viewModel.sendIntent(PlayerIntent.ToggleRepeatMode)
                },
                onShuffleClick = {
                    viewModel.sendIntent(PlayerIntent.ToggleShuffleMode)
                }
            )
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
            .clip(RoundedCornerShape(16.dp))
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
                    // 提取主题色（使用复制后的位图避免 HARDWARE 配置问题）
                    val drawable = result.result.drawable
                    val bitmap = drawable.toBitmap()
                    // 如果是 HARDWARE 位图，复制为 ARGB_8888
                    val safeBitmap = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
                        bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                    } else {
                        bitmap
                    }
                    safeBitmap?.let {
                        Palette.from(it).generate { palette ->
                            palette?.dominantSwatch?.rgb?.let { color ->
                                onThemeColorExtracted(color)
                            }
                        }
                    }
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
 * 歌曲信息组件
 *
 * @param title 歌曲标题
 * @param artist 艺术家
 */
@Composable
private fun SongInfo(
    title: String,
    artist: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
