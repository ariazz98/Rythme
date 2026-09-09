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
import com.aria.rythme.ui.component.AlbumItem
import com.aria.rythme.ui.component.Action
import com.aria.rythme.ui.component.secondaryTopBar
import com.aria.rythme.ui.component.CommonListItem
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.MainGridPage
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.rememberPageSearchState
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
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val search = rememberPageSearchState()
    val items = allItems.filter { search.matches(it) }

    MainListPage(
        title = facet.title(),
        search = search
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
    val search = rememberPageSearchState()
    val albums = state.albums.filter { search.matches(it.title, it.artist) }

    MainGridPage(
        title = value,
        topBar = secondaryTopBar(
            Action.Icon("filter", R.drawable.ic_filter, contentDescription = "筛选（待接入）"),
            Action.Icon("more", R.drawable.ic_more, contentDescription = "更多（待接入）")
        ),
        search = search
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                CommonOperateButton(
                    onPlayClick = { viewModel.playAll() },
                    onRandomPlayClick = { viewModel.playAll(shuffle = true) }
                )
            }
        }

        items(albums, key = Album::id) { album ->
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
