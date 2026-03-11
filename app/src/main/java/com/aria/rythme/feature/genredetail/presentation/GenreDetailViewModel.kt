package com.aria.rythme.feature.genredetail.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class GenreDetailViewModel(
    private val genreName: String,
    private val navigator: Navigator,
    private val musicRepository: MusicRepository
) : BaseViewModel<GenreDetailIntent, GenreDetailState, GenreDetailAction, GenreDetailEffect>() {

    init {
        observeAlbums()
    }

    override fun createInitialState(): GenreDetailState = GenreDetailState()

    override fun handleIntent(intent: GenreDetailIntent) {
        when (intent) {
            is GenreDetailIntent.GoBack -> navigator.goBack()
            is GenreDetailIntent.ClickAlbum -> {
                navigator.navigate(RythmeRoute.AlbumDetail(intent.album.id.toString(), filterGenre = genreName))
            }
        }
    }

    override fun reduce(action: GenreDetailAction): GenreDetailState {
        return when (action) {
            is GenreDetailAction.AlbumsLoaded -> currentState.copy(
                albums = action.albums
            )
        }
    }

    private fun observeAlbums() {
        musicRepository.getAlbumsContainingGenre(genreName)
            .onEach { albums ->
                reduceAndUpdate(GenreDetailAction.AlbumsLoaded(albums))
            }
            .launchIn(viewModelScope)
    }
}
