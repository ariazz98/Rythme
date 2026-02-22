package com.aria.rythme.feature.library.presentation

import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute

class LibraryViewModel(
    private val navigator: Navigator
): BaseViewModel<LibraryIntent, LibraryState, LibraryAction, LibraryEffect>() {
    override fun createInitialState(): LibraryState {
        return LibraryState("")
    }

    override fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.NavToSongList -> {
                navigator.navigate(RythmeRoute.SongList)
            }
        }
    }

    override fun reduce(action: LibraryAction): LibraryState {
        return LibraryState("")
    }
}