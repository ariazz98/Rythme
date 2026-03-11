package com.aria.rythme.feature.albumdetail.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.LocalAlbumSharedTransitionScope
import com.aria.rythme.LocalSharedAlbumId
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.IndexedListItem
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.rememberTrackNumberWidth
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousRoundedRectangle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlbumDetailScreen(
    albumId: String,
    viewModel: AlbumDetailViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val album = state.value.album
    val songs = state.value.songs
    val routeKey = RythmeRoute.AlbumDetail(albumId)
    val context = LocalContext.current
    val sharedTransitionScope = LocalAlbumSharedTransitionScope.current
    val sharedAlbumId = LocalSharedAlbumId.current

    val trackNumberWidth = rememberTrackNumberWidth(songs)

    MainListPage(
        routeKey = routeKey,
        headerMode = HeaderMode.HIDDEN
    ) {

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                with(sharedTransitionScope) {
                    Box(
                        modifier = Modifier
                            .sharedElementWithCallerManagedVisibility(
                                sharedContentState = rememberSharedContentState(key = "albumCover_${albumId}"),
                                visible = sharedAlbumId == albumId
                            )
                            .dropShadow(
                                shape = ContinuousRoundedRectangle(14.dp),
                                shadow = Shadow(
                                    radius = 24.dp,
                                    color = Color.Black.copy(alpha = 0.5f),
                                    alpha = 0.5f,
                                    offset = DpOffset(x = 0.dp, 6.dp)
                                )
                            )
                            .size(264.dp)
                            .clip(ContinuousRoundedRectangle(14.dp))
                            .background(Color(0xFFE9E9EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_album),
                            contentDescription = "",
                            tint = Color(0xFFB5B5B8),
                            modifier = Modifier.fillMaxSize(0.5f)
                        )

                        if (album?.coverUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(album.coverUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = album?.title ?: "",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = album?.artist ?: "",
                    fontSize = 18.sp,
                    color = MaterialTheme.rythmeColors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                CommonOperateButton(
                    onPlayClick = { },
                    onRandomPlayClick = { }
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.rythmeColors.weakColor
                )
            }
        }

        items(songs, key = { song -> song.id }) { song ->
            IndexedListItem(
                song = song,
                album = album,
                trackNumberWidth = trackNumberWidth,
                onClick = { viewModel.sendIntent(AlbumDetailIntent.ClickSong(song)) },
                onMoreClick = {

                }
            )
        }
    }
}
