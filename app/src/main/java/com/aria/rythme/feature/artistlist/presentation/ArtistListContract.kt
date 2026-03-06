package com.aria.rythme.feature.artistlist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Artist

data class ArtistListState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true
) : UiState

sealed interface ArtistListIntent : UserIntent {
    data object GoBack : ArtistListIntent
    data class ClickArtist(val artist: Artist) : ArtistListIntent
}

sealed interface ArtistListAction : InternalAction {
    data class ArtistsLoaded(val artists: List<Artist>) : ArtistListAction
}

sealed interface ArtistListEffect : SideEffect {
    data class ShowToast(val message: String) : ArtistListEffect
}
