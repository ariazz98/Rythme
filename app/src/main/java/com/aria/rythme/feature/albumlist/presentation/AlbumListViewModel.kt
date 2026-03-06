package com.aria.rythme.feature.albumlist.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 专辑列表 ViewModel
 *
 * 从 MusicRepository 读取专辑列表并展示。
 */
class AlbumListViewModel(
    private val navigator: Navigator,
    private val musicRepository: MusicRepository
) : BaseViewModel<AlbumListIntent, AlbumListState, AlbumListAction, AlbumListEffect>() {

    init {
        observeAlbums()
    }

    override fun createInitialState(): AlbumListState = AlbumListState()

    override fun handleIntent(intent: AlbumListIntent) {
        when (intent) {
            is AlbumListIntent.GoBack -> navigator.goBack()
            is AlbumListIntent.ClickAlbum -> { /* TODO: 导航到专辑详情 */ }
        }
    }

    override fun reduce(action: AlbumListAction): AlbumListState {
        return when (action) {
            is AlbumListAction.AlbumsLoaded -> currentState.copy(
                albums = action.albums,
                isLoading = false
            )
        }
    }

    private fun observeAlbums() {
        musicRepository.getAllAlbums()
            .onEach { albums ->
                reduceAndUpdate(AlbumListAction.AlbumsLoaded(albums))
            }
            .launchIn(viewModelScope)
    }
}
