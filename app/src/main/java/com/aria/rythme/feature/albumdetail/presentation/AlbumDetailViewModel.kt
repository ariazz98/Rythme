package com.aria.rythme.feature.albumdetail.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val albumId: Long,
    private val navigator: Navigator,
    private val musicRepository: MusicRepository,
    private val filterArtistId: Long? = null,
    private val filterComposer: String? = null,
    private val filterGenre: String? = null
) : BaseViewModel<AlbumDetailIntent, AlbumDetailState, AlbumDetailAction, AlbumDetailEffect>() {

    init {
        loadAlbum()
        observeSongs()
    }

    override fun createInitialState(): AlbumDetailState = AlbumDetailState()

    override fun handleIntent(intent: AlbumDetailIntent) {
        when (intent) {
            is AlbumDetailIntent.GoBack -> navigator.goBack()
            is AlbumDetailIntent.ClickSong -> { /* TODO: 播放歌曲 */ }
        }
    }

    override fun reduce(action: AlbumDetailAction): AlbumDetailState {
        return when (action) {
            is AlbumDetailAction.AlbumLoaded -> currentState.copy(
                album = action.album,
                isLoading = false
            )
            is AlbumDetailAction.SongsLoaded -> currentState.copy(
                songs = action.songs
            )
        }
    }

    private fun loadAlbum() {
        viewModelScope.launch {
            musicRepository.getAlbumById(albumId)?.let { album ->
                reduceAndUpdate(AlbumDetailAction.AlbumLoaded(album))
            }
        }
    }

    private fun observeSongs() {
        val flow = when {
            filterArtistId != null -> musicRepository.getSongsByAlbumAndArtist(albumId, filterArtistId)
            filterComposer != null -> musicRepository.getSongsByAlbumAndComposer(albumId, filterComposer)
            filterGenre != null -> musicRepository.getSongsByAlbumAndGenre(albumId, filterGenre)
            else -> musicRepository.getSongsByAlbum(albumId)
        }
        flow.onEach { songs ->
            reduceAndUpdate(AlbumDetailAction.SongsLoaded(songs))
        }.launchIn(viewModelScope)
    }
}
