package com.aria.rythme.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.player.data.model.Song
import org.koin.androidx.compose.koinViewModel

/**
 * 播放列表页面
 *
 * 显示当前播放列表，支持选择歌曲播放。
 *
 * @param viewModel 播放器 ViewModel
 * @param onSongClick 歌曲点击回调（可选，默认播放歌曲）
 */
@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
    onSongClick: ((Song) -> Unit)? = null
) {
    val state by viewModel.state.collectAsUiState()
    
    // 加载歌曲列表
    LaunchedEffect(Unit) {
        viewModel.sendIntent(PlayerIntent.LoadSongs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 标题栏
        PlaylistHeader(
            songCount = state.playlist.size,
            onPlayAllClick = {
                if (state.playlist.isNotEmpty()) {
                    viewModel.sendIntent(PlayerIntent.PlaySong(state.playlist.first()))
                }
            }
        )

        // 歌曲列表
        if (state.playlist.isEmpty()) {
            EmptyPlaylist()
        } else {
            SongList(
                songs = state.playlist,
                currentSong = state.currentSong,
                isPlaying = state.isPlaying,
                currentIndex = state.currentIndex,
                onSongClick = { song ->
                    onSongClick?.invoke(song)
                        ?: viewModel.sendIntent(PlayerIntent.PlaySong(song))
                },
                onPlayPauseClick = {
                    viewModel.sendIntent(PlayerIntent.TogglePlayPause)
                }
            )
        }
    }
}

/**
 * 播放列表标题栏
 *
 * @param songCount 歌曲数量
 * @param onPlayAllClick 播放全部点击
 */
@Composable
private fun PlaylistHeader(
    songCount: Int,
    onPlayAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "播放列表",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "($songCount 首)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 播放全部按钮
        if (songCount > 0) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onPlayAllClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "播放全部",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 空播放列表提示
 */
@Composable
private fun EmptyPlaylist() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "暂无歌曲",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "扫描本地音乐文件后显示",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 歌曲列表
 *
 * @param songs 歌曲列表
 * @param currentSong 当前播放歌曲
 * @param isPlaying 是否正在播放
 * @param currentIndex 当前索引
 * @param onSongClick 歌曲点击
 * @param onPlayPauseClick 播放/暂停点击
 */
@Composable
private fun SongList(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    currentIndex: Int,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id }
        ) { index, song ->
            val isCurrentSong = song.id == currentSong?.id

            SongItem(
                song = song,
                index = index,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying && isCurrentSong,
                onClick = { onSongClick(song) },
                onPlayPauseClick = {
                    if (isCurrentSong) {
                        onPlayPauseClick()
                    } else {
                        onSongClick(song)
                    }
                }
            )
        }
    }
}

/**
 * 歌曲项
 *
 * @param song 歌曲
 * @param index 索引
 * @param isCurrentSong 是否为当前播放歌曲
 * @param isPlaying 是否正在播放
 * @param onClick 点击回调
 * @param onPlayPauseClick 播放/暂停点击
 */
@Composable
private fun SongItem(
    song: Song,
    index: Int,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    val backgroundColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.background
    }

    val textColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号或播放状态
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrentSong && isPlaying) {
                // 正在播放动画（简化为图标）
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "正在播放",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 封面
        SongCover(
            coverUri = song.coverUri,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 歌曲信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${song.artist} · ${song.durationText}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 播放/暂停按钮
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isCurrentSong && isPlaying) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = if (isCurrentSong && isPlaying) "暂停" else "播放",
                tint = if (isCurrentSong) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * 歌曲封面
 *
 * @param coverUri 封面URI
 * @param modifier 修饰符
 */
@Composable
private fun SongCover(
    coverUri: android.net.Uri?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (coverUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "专辑封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
