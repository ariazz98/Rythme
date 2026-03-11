package com.aria.rythme.feature.composerdetail.presentation

import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent

data class ComposerDetailState(
    val albums: List<Album> = emptyList()
) : UiState

sealed interface ComposerDetailIntent : UserIntent {
    data object GoBack : ComposerDetailIntent
    data class ClickAlbum(val album: Album) : ComposerDetailIntent
}

sealed interface ComposerDetailAction : InternalAction {
    data class AlbumLoaded(val albums: List<Album>) : ComposerDetailAction
}

sealed interface ComposerDetailEffect : SideEffect {
    data class ShowToast(val message: String) : ComposerDetailEffect
}
