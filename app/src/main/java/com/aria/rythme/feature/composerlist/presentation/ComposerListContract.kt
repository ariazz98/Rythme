package com.aria.rythme.feature.composerlist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent

data class ComposerListState(
    val composers: List<String> = emptyList(),
    val isLoading: Boolean = true
) : UiState

sealed interface ComposerListIntent : UserIntent {
    data object GoBack : ComposerListIntent
    data class ClickComposer(val composer: String) : ComposerListIntent
}

sealed interface ComposerListAction : InternalAction {
    data class ComposersLoaded(val composers: List<String>) : ComposerListAction
}

sealed interface ComposerListEffect : SideEffect {
    data class ShowToast(val message: String) : ComposerListEffect
}
