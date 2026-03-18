package com.aria.rythme.feature.playlistdetail.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.OverlayMenu
import com.aria.rythme.ui.component.SongListItem
import com.aria.rythme.ui.component.buildSongContextMenuConfigs
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel
) {
    val state = viewModel.state.collectAsUiState()
    val playlist = state.value.playlist
    val songs = state.value.songs
    val overlayMenu = LocalOverlayMenu.current

    MainListPage(
        title = playlist?.name,
        routeKey = RythmeRoute.PlaylistDetail(playlist?.id?.toString() ?: "0"),
        defaultTitleHidden = true,
        headerMode = HeaderMode.HIDDEN
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playlist?.name ?: "",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = MaterialTheme.rythmeColors.textColor,
                    overflow = TextOverflow.Ellipsis
                )

                if (!playlist?.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playlist.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.rythmeColors.subTitleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = playlist?.displayDescription ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.rythmeColors.subTitleColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                CommonOperateButton(
                    onPlayClick = { viewModel.sendIntent(PlaylistDetailIntent.PlayAll) },
                    onRandomPlayClick = { viewModel.sendIntent(PlaylistDetailIntent.ShufflePlay) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.rythmeColors.weakColor
                )
            }
        }

        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "歌单内暂无歌曲",
                        fontSize = 15.sp,
                        color = MaterialTheme.rythmeColors.subTitleColor
                    )
                }
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongListItem(
                    song = song,
                    showDivider = index != songs.size - 1,
                    onClick = { viewModel.sendIntent(PlaylistDetailIntent.PlaySong(song)) },
                    onMoreClick = { bounds ->
                        overlayMenu.show(
                            OverlayMenu.SongContext(
                                song = song,
                                anchorBounds = bounds,
                                configs = buildSongContextMenuConfigs(
                                    onDismiss = { overlayMenu.dismiss() },
                                    onEdit = { overlayMenu.show(OverlayMenu.SongEdit(song)) }
                                )
                            )
                        )
                    }
                )
            }
        }
    }
}
