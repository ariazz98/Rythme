package com.aria.rythme.feature.search.presentation

import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.navigation.Navigator

class SearchViewModel(
    private val navigator: Navigator
): BaseViewModel<SearchIntent, SearchState, SearchAction, SearchEffect>() {
    override fun createInitialState(): SearchState {
        return SearchState("")
    }

    override fun handleIntent(intent: SearchIntent) {
    }

    override fun reduce(action: SearchAction): SearchState {
        return SearchState("")
    }
}