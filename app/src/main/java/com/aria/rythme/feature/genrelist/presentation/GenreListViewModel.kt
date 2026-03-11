package com.aria.rythme.feature.genrelist.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class GenreListViewModel(
    private val navigator: Navigator,
    private val musicRepository: MusicRepository
) : BaseViewModel<GenreListIntent, GenreListState, GenreListAction, GenreListEffect>() {

    init {
        observeGenres()
    }

    override fun createInitialState(): GenreListState = GenreListState()

    override fun handleIntent(intent: GenreListIntent) {
        when (intent) {
            is GenreListIntent.GoBack -> navigator.goBack()
            is GenreListIntent.ClickGenre -> navigator.navigate(
                RythmeRoute.GenreDetail(intent.genre)
            )
        }
    }

    override fun reduce(action: GenreListAction): GenreListState {
        return when (action) {
            is GenreListAction.GenresLoaded -> currentState.copy(
                genres = action.genres,
                isLoading = false
            )
        }
    }

    private fun observeGenres() {
        musicRepository.getAllGenres()
            .onEach { genres ->
                reduceAndUpdate(GenreListAction.GenresLoaded(genres))
            }
            .launchIn(viewModelScope)
    }
}
