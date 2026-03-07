package com.aria.rythme.feature.albumlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.AlbumItem
import com.aria.rythme.ui.component.CapsuleButton
import com.aria.rythme.ui.component.MainGridPage
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlbumListScreen(
    viewModel: AlbumListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val albums = state.value.albums

    MainGridPage(
        routeKey = RythmeRoute.AlbumList
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CapsuleButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    icon = R.drawable.ic_play,
                    iconSize = 14.dp,
                    text = R.string.music_play,
                    onClick = {
                        // TODO: 播放全部专辑
                    }
                )

                CapsuleButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    icon = R.drawable.ic_shuffle,
                    text = R.string.music_random_play,
                    onClick = {
                        // TODO: 随机播放
                    }
                )
            }
        }

        items(albums, key = { it.id }) { album ->
            AlbumItem(
                album = album,
                onClick = {
                    viewModel.sendIntent(AlbumListIntent.ClickAlbum(album))
                }
            )
        }
    }
}
