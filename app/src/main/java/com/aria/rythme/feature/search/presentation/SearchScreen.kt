package com.aria.rythme.feature.search.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.aria.rythme.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.ui.component.Action
import com.aria.rythme.ui.component.TopBarConfig
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.MainGridPage
import com.aria.rythme.ui.component.OverlayMenu
import com.aria.rythme.ui.component.rememberPageSearchState
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
    val genres by viewModel.browseGenres.collectAsStateWithLifecycle()
    val search = rememberPageSearchState(state.query, viewModel::updateQuery)

    MainGridPage(
        title = stringResource(R.string.title_search),
        search = search,
        topBar = TopBarConfig(actions = listOf(Action.Avatar(actionKey = "avatar", name = "ARiA")))
    ) {
        if (state.query.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("最近搜索", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.rythmeColors.textColor)
                    Text("搜索记录待接入", fontSize = 13.sp, color = MaterialTheme.rythmeColors.subTitleColor)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("浏览本地音乐", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.rythmeColors.textColor)
                    Text("来自曲库的类型 · 分类入口待接入", fontSize = 12.sp, color = MaterialTheme.rythmeColors.subTitleColor)
                }
            }
            if (genres.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                SearchMessage("曲库暂未提供类型信息，你仍然可以在上方搜索歌曲。")
            }
            items(genres, key = { "genre:$it" }) { genre ->
                SmallCategoryCard(
                    title = genre,
                    cover = Brush.linearGradient(genreColors(genre))
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("歌曲", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
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

private fun genreColors(name: String): List<Color> {
    val hue = ((name.hashCode() ushr 1) % 360).toFloat()
    return listOf(Color.hsv(hue, 0.48f, 0.66f), Color.hsv((hue + 24f) % 360f, 0.58f, 0.86f))
}
