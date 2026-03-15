package com.aria.rythme.feature.albumlist.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.AlbumItem
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.LocalTopBarState
import com.aria.rythme.ui.component.MainGridPage
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.MenuConfig
import com.aria.rythme.ui.component.OverlayMenu
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlbumListScreen(
    viewModel: AlbumListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val albums = state.value.albums
    val sortBy = state.value.sortBy
    val layoutMode = state.value.layoutMode

    val overlayMenuState = LocalOverlayMenu.current
    val topBarState = LocalTopBarState.current

    // 注册 action handler：more 按钮打开菜单
    DisposableEffect(sortBy, layoutMode) {
        topBarState.registerActionHandler(RythmeRoute.AlbumList, "more") {
            overlayMenuState.show(
                OverlayMenu.ActionMenu(
                    buildAlbumListMenuConfigs(
                        currentSort = sortBy,
                        currentLayout = layoutMode,
                        onSortSelected = { viewModel.sendIntent(AlbumListIntent.SetSort(it)) },
                        onLayoutSelected = { viewModel.sendIntent(AlbumListIntent.SetLayout(it)) },
                        onDismiss = { overlayMenuState.dismiss() }
                    )
                )
            )
        }
        onDispose {}
    }

    when (layoutMode) {
        AlbumLayoutMode.GRID -> {
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
                            onPlayClick = { /* TODO */ },
                            onRandomPlayClick = { /* TODO */ }
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

        AlbumLayoutMode.LIST -> {
            MainListPage(
                title = stringResource(R.string.title_album),
                routeKey = RythmeRoute.AlbumList,
                defaultTitleHidden = true,
                headerMode = HeaderMode.COLLAPSED
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 21.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        CommonOperateButton(
                            onPlayClick = { /* TODO */ },
                            onRandomPlayClick = { /* TODO */ }
                        )
                    }
                }

                items(albums, key = { it.id }) { album ->
                    AlbumItem(
                        album = album,
                        onClick = {
                            viewModel.sendIntent(AlbumListIntent.ClickAlbum(album))
                        },
                        modifier = Modifier.padding(horizontal = 21.dp)
                    )
                }
            }
        }
    }
}

private fun buildAlbumListMenuConfigs(
    currentSort: AlbumSortBy,
    currentLayout: AlbumLayoutMode,
    onSortSelected: (AlbumSortBy) -> Unit,
    onLayoutSelected: (AlbumLayoutMode) -> Unit,
    onDismiss: () -> Unit
): List<MenuConfig> {
    val sortItems = listOf(
        AlbumSortBy.TITLE to R.string.sort_by_title,
        AlbumSortBy.ARTIST to R.string.sort_by_artist,
        AlbumSortBy.YEAR to R.string.sort_by_year,
        AlbumSortBy.RECENTLY_ADDED to R.string.sort_by_recently_added
    ).map { (sort, titleRes) ->
        MenuConfig.Item(
            isChecked = currentSort == sort,
            iconRes = null,
            titleRes = titleRes
        ) {
            onSortSelected(sort)
            onDismiss()
        }
    }

    val layoutItems = listOf(
        AlbumLayoutMode.GRID to R.string.layout_grid,
        AlbumLayoutMode.LIST to R.string.layout_list
    ).map { (layout, titleRes) ->
        MenuConfig.Item(
            isChecked = currentLayout == layout,
            iconRes = if (layout == AlbumLayoutMode.GRID) {
                R.drawable.ic_grid
            } else {
                R.drawable.ic_play_list
            },
            iconSize = if (layout == AlbumLayoutMode.GRID) {
                16.dp
            } else {
                18.dp
            },
            titleRes = titleRes
        ) {
            onLayoutSelected(layout)
            onDismiss()
        }
    }

    return layoutItems + MenuConfig.Separator + sortItems
}
