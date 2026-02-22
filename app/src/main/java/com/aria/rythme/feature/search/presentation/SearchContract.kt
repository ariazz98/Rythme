package com.aria.rythme.feature.search.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent

sealed interface SearchIntent: UserIntent {

}

data class SearchState(
    val state: String
): UiState {

}

sealed interface SearchAction: InternalAction {

}

sealed interface SearchEffect: SideEffect {

}