package com.aria.rythme.feature.songlist.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.CommonOperateButton
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.MenuConfig
import com.aria.rythme.ui.component.OverlayMenu
import com.aria.rythme.ui.component.SongListItem
import com.aria.rythme.ui.component.buildSongContextMenuConfigs
import org.koin.compose.viewmodel.koinViewModel

/**
 * 歌曲列表页面
 */
@Composable
fun SongListScreen(
    viewModel: SongListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val songs = state.value.songs
    val overlayMenu = LocalOverlayMenu.current

    MainListPage(
        title = stringResource(R.string.title_music_list),
        routeKey = RythmeRoute.SongList,
        defaultTitleHidden = true,
        headerMode = HeaderMode.COLLAPSED
    ) {
        item {

            Box(modifier = Modifier.fillMaxWidth().padding(start = 21.dp, end = 21.dp, top = 8.dp, bottom = 24.dp)) {

                CommonOperateButton(
                    onPlayClick = { viewModel.sendIntent(SongListIntent.PlayAll) },
                    onRandomPlayClick = { viewModel.sendIntent(SongListIntent.ShufflePlay) }
                )

            }
        }

        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongListItem(
                song = song,
                showDivider = index != songs.size - 1,
                onClick = { viewModel.sendIntent(SongListIntent.PlaySong(song)) },
                onMoreClick = { bounds ->
                    overlayMenu.show(
                        OverlayMenu.SongContext(
                            song = song,
                            anchorBounds = bounds,
                            configs = buildSongContextMenuConfigs { overlayMenu.dismiss() }
                        )
                    )
                }
            )
        }
    }
}
