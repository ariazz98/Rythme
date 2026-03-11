package com.aria.rythme.feature.artistdetail.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.AlbumItem
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.MainGridPage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArtistDetailScreen(
    artistId: String,
    viewModel: ArtistDetailViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val artist = state.value.artist
    val albums = state.value.albums
    val routeKey = RythmeRoute.ArtistDetail(artistId)

    MainGridPage(
        routeKey = routeKey
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .dropShadow(
                            shape = CircleShape,
                            shadow = Shadow(
                                radius = 12.dp,
                                color = Color.Black.copy(alpha = 0.5f),
                                alpha = 0.5f,
                                offset = DpOffset(x = 0.dp, 3.dp)
                            )
                        )
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9E9EA)),
                    contentAlignment = Alignment.Center
                ) {
                    // 图标
                    Icon(
                        painter = painterResource(R.drawable.ic_artist),
                        contentDescription = "",
                        tint = Color(0xFFB5B5B8),
                        modifier = Modifier.size(48.dp)
                    )

                    if (artist?.coverUri != null) {
                        AsyncImage(
                            model = artist.coverUri,
                            contentDescription = "cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = artist?.name ?: "未知艺术家",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(21.dp))

                CommonOperateButton(
                    onPlayClick = {
                        // TODO: 播放
                    },
                    onRandomPlayClick = {
                        // TODO: 随机播放
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        items(albums) { album ->
            AlbumItem(album = album) {
                viewModel.sendIntent(ArtistDetailIntent.ClickAlbum(album))
            }
        }
    }
}
