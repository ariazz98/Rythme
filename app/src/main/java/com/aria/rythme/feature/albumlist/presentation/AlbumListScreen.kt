package com.aria.rythme.feature.albumlist.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.AlbumItem
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.MainGridPage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlbumListScreen(
    viewModel: AlbumListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val albums = state.value.albums

    MainGridPage(
        title = stringResource(R.string.title_album),
        routeKey = RythmeRoute.AlbumList,
        defaultTitleHidden = true,
        headerMode = HeaderMode.COLLAPSED
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                CommonOperateButton(
                    onPlayClick = {
                        // TODO: 播放全部专辑
                    },
                    onRandomPlayClick = {
                        // TODO: 更多操作
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
