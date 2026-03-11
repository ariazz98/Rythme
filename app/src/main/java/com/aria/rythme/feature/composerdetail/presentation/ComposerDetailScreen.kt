package com.aria.rythme.feature.composerdetail.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.AlbumItem
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.MainGridPage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComposerDetailScreen(
    composerName: String,
    viewModel: ComposerDetailViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val albums = state.value.albums
    val routeKey = RythmeRoute.ComposerDetail(composerName)

    MainGridPage(
        title = composerName,
        routeKey = routeKey
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                CommonOperateButton(
                    onPlayClick = {  },
                    onRandomPlayClick = {  }
                )
            }
        }

        items(albums, key = {album -> album.id }) { album ->
            AlbumItem(album = album) {
                viewModel.sendIntent(ComposerDetailIntent.ClickAlbum(album))
            }
        }
    }
}
