package com.aria.rythme.feature.playlistdetail.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Playlist
import com.aria.rythme.core.music.data.model.Song

sealed interface PlaylistDetailIntent : UserIntent {
    data object GoBack : PlaylistDetailIntent
    data class PlaySong(val song: Song) : PlaylistDetailIntent
    data object PlayAll : PlaylistDetailIntent
    data object ShufflePlay : PlaylistDetailIntent
    data class RemoveSong(val songId: Long) : PlaylistDetailIntent
}

data class PlaylistDetailState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
) : UiState

sealed interface PlaylistDetailAction : InternalAction {
    data class PlaylistLoaded(val playlist: Playlist) : PlaylistDetailAction
    data class SongsLoaded(val songs: List<Song>) : PlaylistDetailAction
}

sealed interface PlaylistDetailEffect : SideEffect
