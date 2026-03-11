package com.aria.rythme.feature.composerdetail.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ComposerDetailViewModel(
    private val composerName: String,
    private val navigator: Navigator,
    private val musicRepository: MusicRepository
) : BaseViewModel<ComposerDetailIntent, ComposerDetailState, ComposerDetailAction, ComposerDetailEffect>() {

    init {
        observeAlbums()
    }

    override fun createInitialState(): ComposerDetailState = ComposerDetailState()

    override fun handleIntent(intent: ComposerDetailIntent) {
        when (intent) {
            is ComposerDetailIntent.GoBack -> navigator.goBack()
            is ComposerDetailIntent.ClickAlbum -> {
                navigator.navigate(RythmeRoute.AlbumDetail(id = intent.album.id.toString(), filterComposer = composerName))
            }
        }
    }

    override fun reduce(action: ComposerDetailAction): ComposerDetailState {
        return when (action) {
            is ComposerDetailAction.AlbumLoaded -> currentState.copy(
                albums = action.albums
            )
        }
    }

    private fun observeAlbums() {
        musicRepository.getAlbumsContainingComposer(composerName)
            .onEach { albums ->
                reduceAndUpdate(ComposerDetailAction.AlbumLoaded(albums))
            }
            .launchIn(viewModelScope)
    }
}
