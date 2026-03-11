package com.aria.rythme.feature.genredetail.presentation

import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Song

data class GenreDetailState(
    val albums: List<Album> = emptyList()
) : UiState

sealed interface GenreDetailIntent : UserIntent {
    data object GoBack : GenreDetailIntent
    data class ClickAlbum(val album: Album) : GenreDetailIntent
}

sealed interface GenreDetailAction : InternalAction {
    data class AlbumsLoaded(val albums: List<Album>) : GenreDetailAction
}

sealed interface GenreDetailEffect : SideEffect {
    data class ShowToast(val message: String) : GenreDetailEffect
}
