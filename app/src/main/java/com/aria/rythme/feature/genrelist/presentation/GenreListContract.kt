package com.aria.rythme.feature.genrelist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent

data class GenreListState(
    val genres: List<String> = emptyList(),
    val isLoading: Boolean = true
) : UiState

sealed interface GenreListIntent : UserIntent {
    data object GoBack : GenreListIntent
    data class ClickGenre(val genre: String) : GenreListIntent
}

sealed interface GenreListAction : InternalAction {
    data class GenresLoaded(val genres: List<String>) : GenreListAction
}

sealed interface GenreListEffect : SideEffect {
    data class ShowToast(val message: String) : GenreListEffect
}
