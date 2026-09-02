package com.aria.rythme.feature.library.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.AlbumItem
import com.aria.rythme.ui.component.CommonListItem
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.MainGridPage
import com.aria.rythme.ui.component.MainListPage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LibraryFacetListScreen(
    facet: LibraryFacet,
    onItemClick: (String) -> Unit
) {
    val viewModel = koinViewModel<LibraryFacetListViewModel>(
        key = "library-facet-list:${facet.name}",
        parameters = { parametersOf(facet) }
    )
    val items by viewModel.items.collectAsStateWithLifecycle()

    MainListPage(
        title = facet.title(),
        routeKey = facet.listRoute()
    ) {
        itemsIndexed(items, key = { _, item -> item }) { index, item ->
            CommonListItem(
                title = item,
                showDivider = index != items.lastIndex,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun LibraryFacetDetailScreen(
    facet: LibraryFacet,
    value: String,
    onAlbumClick: (Album) -> Unit
) {
    val viewModel = koinViewModel<LibraryFacetDetailViewModel>(
        key = "library-facet-detail:${facet.name}:$value",
        parameters = { parametersOf(facet, value) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    MainGridPage(
        title = value,
        routeKey = facet.detailRoute(value)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                CommonOperateButton(
                    onPlayClick = { viewModel.playAll() },
                    onRandomPlayClick = { viewModel.playAll(shuffle = true) }
                )
            }
        }

        items(state.albums, key = Album::id) { album ->
            AlbumItem(album = album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun LibraryFacet.title(): String = stringResource(
    when (this) {
        LibraryFacet.GENRE -> R.string.title_type
        LibraryFacet.COMPOSER -> R.string.title_composer
    }
)

private fun LibraryFacet.listRoute(): RythmeRoute = when (this) {
    LibraryFacet.GENRE -> RythmeRoute.GenreList
    LibraryFacet.COMPOSER -> RythmeRoute.ComposerList
}

private fun LibraryFacet.detailRoute(value: String): RythmeRoute = when (this) {
    LibraryFacet.GENRE -> RythmeRoute.GenreDetail(value)
    LibraryFacet.COMPOSER -> RythmeRoute.ComposerDetail(value)
}
