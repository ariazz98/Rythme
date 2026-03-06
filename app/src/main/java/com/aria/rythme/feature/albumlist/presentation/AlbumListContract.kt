package com.aria.rythme.feature.albumlist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Album

/**
 * 专辑列表页面的 MVI Contract
 */

data class AlbumListState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true
) : UiState

sealed interface AlbumListIntent : UserIntent {
    data object GoBack : AlbumListIntent
    data class ClickAlbum(val album: Album) : AlbumListIntent
}

sealed interface AlbumListAction : InternalAction {
    data class AlbumsLoaded(val albums: List<Album>) : AlbumListAction
}

sealed interface AlbumListEffect : SideEffect {
    data class ShowToast(val message: String) : AlbumListEffect
}
