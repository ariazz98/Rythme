package com.aria.rythme.feature.artistdetail.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.model.Song

data class ArtistDetailState(
    val artist: Artist? = null,
    val albums: List<Album> = emptyList()
) : UiState

sealed interface ArtistDetailIntent : UserIntent {
    data object GoBack : ArtistDetailIntent
    data class ClickAlbum(val album: Album) : ArtistDetailIntent
}

sealed interface ArtistDetailAction : InternalAction {
    data class ArtistLoaded(val artist: Artist) : ArtistDetailAction
    data class AlbumsLoaded(val albums: List<Album>) : ArtistDetailAction
}

sealed interface ArtistDetailEffect : SideEffect {
    data class ShowToast(val message: String) : ArtistDetailEffect
}
