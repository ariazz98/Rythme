package com.aria.rythme.feature.playlist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent

sealed interface PlayListIntent: UserIntent {

}

data class PlayListState(
    val state: String
): UiState

sealed interface PlayListAction: InternalAction {

}

sealed interface PlayListEffect: SideEffect {

}
