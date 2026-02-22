package com.aria.rythme.feature.playlist.presentation

import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.navigation.Navigator

class PlayListViewModel(
    private val navigator: Navigator
): BaseViewModel<PlayListIntent, PlayListState, PlayListAction, PlayListEffect>() {
    override fun createInitialState(): PlayListState {
        return PlayListState("")
    }

    override fun handleIntent(intent: PlayListIntent) {

    }

    override fun reduce(action: PlayListAction): PlayListState {
        return PlayListState("")
    }
}