package com.aria.rythme.feature.artistdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistDetailState(
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
    val isFavorite: Boolean = false
)

class ArtistDetailViewModel(
    private val artistId: Long,
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
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
        musicRepository.getSongsByArtist(artistId)
            .onEach { songs -> _state.update { it.copy(songs = songs.sortedWith(trackOrder)) } }
            .launchIn(viewModelScope)
        musicRepository.observeArtistFavorite(artistId)
            .onEach { isFavorite -> _state.update { it.copy(isFavorite = isFavorite) } }
            .launchIn(viewModelScope)
    }

    fun playAll(shuffle: Boolean = false) {
        val songs = _state.value.songs
        if (songs.isEmpty()) return

        val queue = if (shuffle) songs.shuffled() else songs
        viewModelScope.launch {
            playbackController.play(queue.first(), queue)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            musicRepository.toggleArtistFavorite(artistId)
        }
    }

    private companion object {
        val trackOrder = compareBy<Song>(Song::album, Song::discNumber, Song::trackNumber, Song::title)
    }
}
