package com.aria.rythme.feature.songlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.core.navigation.LocalInnerPadding
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.ui.theme.rythmeColors

/**
 * 歌曲列表页面
 */
@Composable
fun SongListScreen(
    viewModel: SongListViewModel
) {
    val state = viewModel.state.collectAsUiState()
    val innerPadding = LocalInnerPadding.current
    val topPadding = innerPadding.calculateTopPadding()
    val bottomPadding = innerPadding.calculateBottomPadding()

    // 加载歌曲列表
    LaunchedEffect(Unit) {
        viewModel.sendIntent(SongListIntent.LoadSongs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        SongListTopBar(
            onBack = { viewModel.sendIntent(SongListIntent.GoBack) },
            modifier = Modifier.padding(top = topPadding)
        )

        // 播放按钮区域
        PlaybackButtonsRow(
            isShuffleEnabled = state.value.isShuffleEnabled,
            onPlayClick = {
                viewModel.sendIntent(SongListIntent.PlayAll)
            },
            onShuffleClick = {
                viewModel.sendIntent(SongListIntent.ToggleShuffle)
            },
            modifier = Modifier.padding(horizontal = 21.dp, vertical = 12.dp)
        )

        // 歌曲列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 21.dp,
                end = 21.dp,
                bottom = bottomPadding + 16.dp
            )
        ) {
            items(
                items = state.value.songs,
                key = { it.id }
            ) { song ->
                SongListItem(
                    song = song,
                    onClick = {
                        viewModel.sendIntent(SongListIntent.PlaySong(song))
                    },
                    onMoreClick = {
                        viewModel.sendIntent(SongListIntent.ShowSongOptions(song))
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * 顶部导航栏
 */
@Composable
fun SongListTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 21.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.rythmeColors.textColor
            )
        }

        // 右侧按钮组
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 排序按钮
            IconButton(
                onClick = { /* TODO: 排序功能 */ },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "排序",
                    tint = MaterialTheme.rythmeColors.textColor
                )
            }

            // 更多按钮
            IconButton(
                onClick = { /* TODO: 更多选项 */ },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "更多",
                    tint = MaterialTheme.rythmeColors.textColor
                )
            }
        }
    }
}

/**
 * 播放按钮行
 */
@Composable
fun PlaybackButtonsRow(
    isShuffleEnabled: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 播放按钮
        PlaybackButton(
            text = "播放",
            icon = Icons.Default.PlayArrow,
            onClick = onPlayClick,
            modifier = Modifier.weight(1f)
        )

        // 随机播放按钮
        PlaybackButton(
            text = "随机播放",
            icon = Icons.Default.Shuffle,
            onClick = onShuffleClick,
            isActive = isShuffleEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 播放按钮组件
 */
@Composable
fun PlaybackButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isActive) {
                    MaterialTheme.rythmeColors.primary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) {
                    MaterialTheme.rythmeColors.primary
                } else {
                    MaterialTheme.rythmeColors.textColor
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) {
                    MaterialTheme.rythmeColors.primary
                } else {
                    MaterialTheme.rythmeColors.textColor
                }
            )
        }
    }
}

/**
 * 歌曲列表项
 */
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 专辑封面
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.coverUri)
                .crossfade(true)
                .build(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 歌曲信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.rythmeColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                fontSize = 14.sp,
                color = MaterialTheme.rythmeColors.weakColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 更多按钮
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "更多",
                tint = MaterialTheme.rythmeColors.weakColor
            )
        }
    }
}
