package com.aria.rythme.feature.composerlist.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ComposerListViewModel(
    private val navigator: Navigator,
    private val musicRepository: MusicRepository
) : BaseViewModel<ComposerListIntent, ComposerListState, ComposerListAction, ComposerListEffect>() {

    init {
        observeComposers()
    }

    override fun createInitialState(): ComposerListState = ComposerListState()

    override fun handleIntent(intent: ComposerListIntent) {
        when (intent) {
            is ComposerListIntent.GoBack -> navigator.goBack()
            is ComposerListIntent.ClickComposer -> { /* TODO: 导航到作曲者详情 */ }
        }
    }

    override fun reduce(action: ComposerListAction): ComposerListState {
        return when (action) {
            is ComposerListAction.ComposersLoaded -> currentState.copy(
                composers = action.composers,
                isLoading = false
            )
        }
    }

    private fun observeComposers() {
        musicRepository.getAllComposers()
            .onEach { composers ->
                reduceAndUpdate(ComposerListAction.ComposersLoaded(composers))
            }
            .launchIn(viewModelScope)
    }
}
