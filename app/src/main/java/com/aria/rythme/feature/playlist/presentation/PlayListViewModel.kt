package com.aria.rythme.feature.playlist.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.PlaylistRepository
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlayListViewModel(
    private val navigator: Navigator,
    private val playlistRepository: PlaylistRepository
) : BaseViewModel<PlayListIntent, PlayListState, PlayListAction, PlayListEffect>() {

    init {
        observePlaylists()
    }

    override fun createInitialState(): PlayListState = PlayListState()

    override fun handleIntent(intent: PlayListIntent) {
        when (intent) {
            is PlayListIntent.ShowCreateDialog -> {
                reduceAndUpdate(PlayListAction.ToggleCreateDialog(true))
            }
            is PlayListIntent.DismissCreateDialog -> {
                reduceAndUpdate(PlayListAction.ToggleCreateDialog(false))
            }
            is PlayListIntent.CreatePlaylist -> {
                viewModelScope.launch {
                    playlistRepository.createPlaylist(intent.name)
                    reduceAndUpdate(PlayListAction.ToggleCreateDialog(false))
                }
            }
            is PlayListIntent.DeletePlaylist -> {
                viewModelScope.launch {
                    playlistRepository.deletePlaylist(intent.id)
                }
            }
            is PlayListIntent.OpenDetail -> {
                navigator.navigate(RythmeRoute.PlaylistDetail(intent.id.toString()))
            }
        }
    }

    override fun reduce(action: PlayListAction): PlayListState {
        return when (action) {
            is PlayListAction.PlaylistsLoaded -> currentState.copy(
                playlists = action.playlists
            )
            is PlayListAction.ToggleCreateDialog -> currentState.copy(
                showCreateDialog = action.show
            )
        }
    }

    private fun observePlaylists() {
        playlistRepository.getAllPlaylists()
            .onEach { playlists ->
                reduceAndUpdate(PlayListAction.PlaylistsLoaded(playlists))
            }
            .launchIn(viewModelScope)
    }
}
