package com.aria.rythme.feature.artistlist.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ArtistListViewModel(
    private val navigator: Navigator,
    private val musicRepository: MusicRepository
) : BaseViewModel<ArtistListIntent, ArtistListState, ArtistListAction, ArtistListEffect>() {

    init {
        observeArtists()
    }

    override fun createInitialState(): ArtistListState = ArtistListState()

    override fun handleIntent(intent: ArtistListIntent) {
        when (intent) {
            is ArtistListIntent.GoBack -> navigator.goBack()
            is ArtistListIntent.ClickArtist -> { /* TODO: 导航到艺术家详情 */ }
        }
    }

    override fun reduce(action: ArtistListAction): ArtistListState {
        return when (action) {
            is ArtistListAction.ArtistsLoaded -> currentState.copy(
                artists = action.artists,
                isLoading = false
            )
        }
    }

    private fun observeArtists() {
        musicRepository.getAllArtists()
            .onEach { artists ->
                reduceAndUpdate(ArtistListAction.ArtistsLoaded(artists))
            }
            .launchIn(viewModelScope)
    }
}
