package com.aria.rythme.feature.playlist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Playlist

sealed interface PlayListIntent : UserIntent {
    data object ShowCreateDialog : PlayListIntent
    data object DismissCreateDialog : PlayListIntent
    data class CreatePlaylist(val name: String) : PlayListIntent
    data class DeletePlaylist(val id: Long) : PlayListIntent
    data class OpenDetail(val id: Long) : PlayListIntent
}

data class PlayListState(
    val playlists: List<Playlist> = emptyList(),
    val showCreateDialog: Boolean = false
) : UiState

sealed interface PlayListAction : InternalAction {
    data class PlaylistsLoaded(val playlists: List<Playlist>) : PlayListAction
    data class ToggleCreateDialog(val show: Boolean) : PlayListAction
}

sealed interface PlayListEffect : SideEffect
