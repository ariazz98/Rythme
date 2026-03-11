package com.aria.rythme.feature.artistdetail.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    private val artistId: Long,
    private val navigator: Navigator,
    private val musicRepository: MusicRepository
) : BaseViewModel<ArtistDetailIntent, ArtistDetailState, ArtistDetailAction, ArtistDetailEffect>() {

    init {
        loadArtist()
        observeAlbums()
    }

    override fun createInitialState(): ArtistDetailState = ArtistDetailState()

    override fun handleIntent(intent: ArtistDetailIntent) {
        when (intent) {
            is ArtistDetailIntent.GoBack -> navigator.goBack()
            is ArtistDetailIntent.ClickAlbum -> { navigator.navigate(RythmeRoute.AlbumDetail(intent.album.id.toString(), filterArtistId = artistId)) }
        }
    }

    override fun reduce(action: ArtistDetailAction): ArtistDetailState {
        return when (action) {
            is ArtistDetailAction.ArtistLoaded -> currentState.copy(
                artist = action.artist
            )
            is ArtistDetailAction.AlbumsLoaded -> currentState.copy(
                albums = action.albums
            )
        }
    }

    private fun loadArtist() {
        viewModelScope.launch {
            musicRepository.getArtistById(artistId)?.let { artist ->
                reduceAndUpdate(ArtistDetailAction.ArtistLoaded(artist))
            }
        }
    }

    private fun observeAlbums() {
        musicRepository.getAlbumsContainingArtist(artistId)
            .onEach { albums ->
                reduceAndUpdate(ArtistDetailAction.AlbumsLoaded(albums))
            }
            .launchIn(viewModelScope)
    }
}
