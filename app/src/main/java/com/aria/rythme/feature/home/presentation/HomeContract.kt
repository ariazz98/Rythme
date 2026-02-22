package com.aria.rythme.feature.home.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent

sealed interface HomeIntent: UserIntent {

}

data class HomeState(
    val state: String
): UiState {

}

sealed interface HomeAction: InternalAction {

}

sealed interface HomeEffect: SideEffect {

}

