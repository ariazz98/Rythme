package com.aria.rythme.feature.library.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent

sealed interface LibraryIntent: UserIntent {
    data object NavToSongList: LibraryIntent
}

data class LibraryState(
    val state: String
): UiState {

}

sealed interface LibraryAction: InternalAction {
}

sealed interface LibraryEffect: SideEffect {

}