package com.aria.rythme.feature.search.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.MainGridPage
import com.aria.rythme.ui.component.OverlayMenu
import com.aria.rythme.ui.component.PageSearchField
import com.aria.rythme.ui.component.SmallCategoryCard
import com.aria.rythme.ui.component.SongListItem
import com.aria.rythme.ui.component.buildSongContextMenuConfigs
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overlayMenu = LocalOverlayMenu.current
    val categories = remember { searchCategories() }

    MainGridPage(
        title = stringResource(R.string.title_search),
        routeKey = RythmeRoute.Search
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageSearchField(
                value = state.query,
                onValueChange = viewModel::updateQuery
            )
        }

        if (state.query.isBlank()) {
            items(categories, key = { it.title }) { category ->
                SmallCategoryCard(
                    title = category.title,
                    cover = Brush.linearGradient(category.colors)
                )
            }
        } else if (state.isSearching) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchMessage(stringResource(R.string.searching))
            }
        } else if (state.songs.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchMessage(stringResource(R.string.search_no_results))
            }
        } else {
            items(
                items = state.songs,
                key = { it.id },
                span = { GridItemSpan(maxLineSpan) }
            ) { song ->
                SongListItem(
                    song = song,
                    horizontalPadding = 0.dp,
                    showDivider = song != state.songs.last(),
                    onClick = { viewModel.play(song) },
                    onMoreClick = { bounds ->
                        overlayMenu.show(
                            OverlayMenu.SongContext(
                                song = song,
                                anchorBounds = bounds,
                                configs = buildSongContextMenuConfigs(
                                    onDismiss = overlayMenu::dismiss,
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

@Composable
private fun SearchMessage(message: String) {
    Text(
        text = message,
        color = MaterialTheme.rythmeColors.subTitleColor,
        fontSize = 15.sp,
        modifier = Modifier.padding(vertical = 32.dp)
    )
}

private data class SearchCategory(
    val title: String,
    val colors: List<Color>
)

private fun searchCategories(): List<SearchCategory> = listOf(
    SearchCategory("农历新年", listOf(Color(0xFF87CEEB), Color(0xFFFFB6C1))),
    SearchCategory("C-Pop", listOf(Color(0xFFE85D75), Color(0xFFFF8FA3))),
    SearchCategory("爱", listOf(Color(0xFFE8D5C4), Color(0xFFF5EBE0))),
    SearchCategory("空间音频", listOf(Color(0xFFE85D75), Color(0xFFFF6B6B))),
    SearchCategory("国语流行", listOf(Color(0xFFD4729B), Color(0xFFFF9EC5))),
    SearchCategory("DJ 混音精选", listOf(Color(0xFFB71C1C), Color(0xFFE53935))),
    SearchCategory("月度音乐回忆", listOf(Color(0xFFFFB347), Color(0xFF64B5F6))),
    SearchCategory("排行榜", listOf(Color(0xFF6B7C3D), Color(0xFF8FA456))),
    SearchCategory("爵士乐", listOf(Color(0xFF4A9FD8), Color(0xFF64B5F6))),
    SearchCategory("创作与制作", listOf(Color(0xFF7C7C3D), Color(0xFF9E9E5A))),
    SearchCategory("嘻哈 / 说唱", listOf(Color(0xFF5C6BC0), Color(0xFF7986CB))),
    SearchCategory("古典音乐", listOf(Color(0xFF7B1FA2), Color(0xFF9C27B0)))
)
