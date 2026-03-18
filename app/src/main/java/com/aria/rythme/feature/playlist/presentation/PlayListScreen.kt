package com.aria.rythme.feature.playlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.core.music.data.model.Playlist
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.LocalTopBarState
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun PlayListScreen(
    viewModel: PlayListViewModel
) {
    val state = viewModel.state.collectAsUiState()
    val playlists = state.value.playlists
    val showCreateDialog = state.value.showCreateDialog

    val topBarState = LocalTopBarState.current
    topBarState.registerActionHandler(RythmeRoute.Playlist, "more") {
        viewModel.sendIntent(PlayListIntent.ShowCreateDialog)
    }

    MainListPage(
        title = stringResource(R.string.title_play_list),
        routeKey = RythmeRoute.Playlist
    ) {
        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_playlist),
                        fontSize = 15.sp,
                        color = MaterialTheme.rythmeColors.subTitleColor
                    )
                }
            }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    showDivider = playlist != playlists.last(),
                    onClick = { viewModel.sendIntent(PlayListIntent.OpenDetail(playlist.id)) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.sendIntent(PlayListIntent.DismissCreateDialog) },
            onCreate = { name -> viewModel.sendIntent(PlayListIntent.CreatePlaylist(name)) }
        )
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp, horizontal = 21.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(ContinuousRoundedRectangle(6.dp))
                    .background(MaterialTheme.rythmeColors.coverBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_list),
                    contentDescription = null,
                    tint = MaterialTheme.rythmeColors.coverIcon,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.rythmeColors.textColor
                )
                Text(
                    text = playlist.displayDescription,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.rythmeColors.subTitleColor
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                tint = MaterialTheme.rythmeColors.weakColor,
                modifier = Modifier.size(12.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 81.dp, end = 18.dp),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.rythmeColors.weakColor
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_playlist)) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
