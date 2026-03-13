package com.aria.rythme.feature.albumlist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Album

/**
 * 专辑列表页面的 MVI Contract
 */

enum class AlbumSortBy {
    TITLE, ARTIST, YEAR, SONG_COUNT, RECENTLY_ADDED
}

enum class AlbumLayoutMode {
    GRID, LIST
}

data class AlbumListState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val sortBy: AlbumSortBy = AlbumSortBy.TITLE,
    val layoutMode: AlbumLayoutMode = AlbumLayoutMode.GRID
) : UiState

sealed interface AlbumListIntent : UserIntent {
    data object GoBack : AlbumListIntent
    data class ClickAlbum(val album: Album) : AlbumListIntent
    data class SetSort(val sortBy: AlbumSortBy) : AlbumListIntent
    data class SetLayout(val layoutMode: AlbumLayoutMode) : AlbumListIntent
}

sealed interface AlbumListAction : InternalAction {
    data class AlbumsLoaded(val albums: List<Album>) : AlbumListAction
    data class SortChanged(val sortBy: AlbumSortBy, val albums: List<Album>) : AlbumListAction
    data class LayoutChanged(val layoutMode: AlbumLayoutMode) : AlbumListAction
}

sealed interface AlbumListEffect : SideEffect {
    data class ShowToast(val message: String) : AlbumListEffect
}
