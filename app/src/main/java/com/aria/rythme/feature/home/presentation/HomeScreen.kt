package com.aria.rythme.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.repository.ListeningOrigin
import com.aria.rythme.ui.component.*
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousRoundedRectangle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onAlbumClick: (Album) -> Unit,
    onOriginClick: (ListeningOrigin) -> Unit,
    onAlbumsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playbackError by viewModel.operationError.collectAsStateWithLifecycle()
    val colors = MaterialTheme.rythmeColors
    val album = state.added.firstOrNull()
    val hasResume = state.resume != null
    val heroTitle = state.resumeTitle ?: album?.title
    val origin = state.resumeOrigin

    MainListPage(title = "主页", topBar = TopBarConfig(actions = listOf(
        Action.Avatar(actionKey = "avatar", name = "ARiA")
    ))) {
        if (state.loading) item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        state.error?.let { message -> item { HomeMessage(message) } }
        playbackError?.let { message -> item { HomeMessage(message) } }
        if (!state.loading && state.error == null && heroTitle == null) item {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("让音乐来到这里", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = colors.textColor)
                Spacer(Modifier.height(12.dp))
                Text("加入本地音乐后，你的专辑和收听记录会出现在首页。", color = colors.subTitleColor)
                TextButton(onClick = onSettingsClick) { Text("查看音乐扫描设置") }
            }
        }
        if (heroTitle != null) item {
            val open: (() -> Unit)? = if (hasResume) {
                origin?.takeIf { it.kind == "album" || it.kind == "playlist" }?.let { { onOriginClick(it) } }
            } else album?.let { { onAlbumClick(it) } }
            Column(Modifier.padding(horizontal = 21.dp, vertical = 12.dp)) {
                Text(if (hasResume) "继续聆听" else "最近加入", color = colors.subTitleColor, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(290.dp).clip(ContinuousRoundedRectangle(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF354960), Color(0xFF65527C))))
                    .then(if (open != null) Modifier.clickable(onClick = open) else Modifier)) {
                    Icon(painterResource(R.drawable.ic_album), null, Modifier.size(140.dp).align(Alignment.Center), tint = Color.White.copy(alpha = 0.25f))
                    AsyncImage(if (hasResume) state.resumeSong?.coverUri else album?.coverUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                        Text(heroTitle, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(state.resumeSong?.let { if (heroTitle == it.title) it.artist else "${it.title} · ${it.artist}" } ?: album?.artist.orEmpty(),
                            color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { if (hasResume) viewModel.resume() else album?.let(viewModel::playAlbum) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                            Icon(painterResource(R.drawable.ic_play), null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (hasResume) "继续播放" else "播放专辑")
                        }
                    }
                }
            }
        }
        if (!state.loading) item {
            SectionItem(title = "最近听过", withContentPadding = true) {
                if (state.recent.isEmpty()) HomeMessage("开始听一首歌，这里就会留下你的收听足迹。")
                else LazyRow(contentPadding = PaddingValues(horizontal = 21.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.recent.take(20), key = { it.id }) { song ->
                        Column(Modifier.width(148.dp).clickable { viewModel.play(song) }) {
                            CoverItem(size = 148.dp, corner = 8.dp, song = song, defaultBgColor = colors.coverBg, defaultIconColor = colors.coverIcon)
                            Spacer(Modifier.height(8.dp))
                            Text(song.title, color = colors.textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = colors.subTitleColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (!state.loading) item {
            SectionItem(title = "最近加入", withContentPadding = true, onClick = onAlbumsClick) {
                if (state.added.isEmpty()) HomeMessage("还没有本地专辑")
                else LazyRow(contentPadding = PaddingValues(horizontal = 21.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.added.take(20), key = { it.id }) { item ->
                        AlbumItem(item, modifier = Modifier.width(160.dp), onClick = { onAlbumClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMessage(text: String) {
    Text(text, modifier = Modifier.padding(horizontal = 21.dp, vertical = 16.dp), color = MaterialTheme.rythmeColors.subTitleColor)
}
