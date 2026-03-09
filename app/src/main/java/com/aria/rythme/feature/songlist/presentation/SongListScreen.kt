package com.aria.rythme.feature.songlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.CapsuleButton
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.SongListItem
import com.aria.rythme.ui.theme.rythmeColors
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

    MainListPage(
        title = stringResource(R.string.title_music_list),
        routeKey = RythmeRoute.SongList,
        autoHide = false
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 21.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                CapsuleButton(
                    modifier = Modifier.weight(1f).height(48.dp),
                    icon = R.drawable.ic_play,
                    iconSize = 14.dp,
                    text = R.string.music_play,
                    onClick = { viewModel.sendIntent(SongListIntent.PlayAll) }
                )

                CapsuleButton(
                    modifier = Modifier.weight(1f).height(48.dp),
                    icon = R.drawable.ic_shuffle,
                    text = R.string.music_random_play,
                    onClick = { viewModel.sendIntent(SongListIntent.ShufflePlay) }
                )
            }
        }

        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongListItem(
                song = song,
                showDivider = index != songs.size - 1,
                onClick = { viewModel.sendIntent(SongListIntent.PlaySong(song)) },
                onMoreClick = {}
            )
        }
    }
}
