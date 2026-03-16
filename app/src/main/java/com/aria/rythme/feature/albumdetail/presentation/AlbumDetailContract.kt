package com.aria.rythme.feature.albumdetail.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Song

data class AlbumDetailState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val editingSong: Song? = null
) : UiState

sealed interface AlbumDetailIntent : UserIntent {
    data object GoBack : AlbumDetailIntent
    data class ClickSong(val song: Song) : AlbumDetailIntent
}

sealed interface AlbumDetailAction : InternalAction {
    data class AlbumLoaded(val album: Album) : AlbumDetailAction
    data class SongsLoaded(val songs: List<Song>) : AlbumDetailAction
}

sealed interface AlbumDetailEffect : SideEffect {
    data class ShowToast(val message: String) : AlbumDetailEffect
}
