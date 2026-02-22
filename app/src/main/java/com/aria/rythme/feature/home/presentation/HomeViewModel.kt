package com.aria.rythme.feature.home.presentation

import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.navigation.Navigator

class HomeViewModel(
    private val navigator: Navigator
): BaseViewModel<HomeIntent, HomeState, HomeAction, HomeEffect>()  {
    override fun createInitialState(): HomeState {
        return HomeState("")
    }

    override fun handleIntent(intent: HomeIntent) {
    }

    override fun reduce(action: HomeAction): HomeState {
        return HomeState("")
    }
}