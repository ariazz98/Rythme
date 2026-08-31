package com.aria.rythme.feature.artistdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistDetailState(
    val artist: Artist? = null,
    val albums: List<Album> = emptyList()
) : UiState

class ArtistDetailViewModel(
    private val artistId: Long,
    private val musicRepository: MusicRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ArtistDetailState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(artist = musicRepository.getArtistById(artistId)) }
        }
        musicRepository.getAlbumsContainingArtist(artistId)
            .onEach { albums -> _state.update { it.copy(albums = albums) } }
            .launchIn(viewModelScope)
    }
}
