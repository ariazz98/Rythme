package com.aria.rythme.feature.albumdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.music.data.repository.ListeningOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumDetailState(
    val album: Album? = null,
    val songs: List<Song> = emptyList()
)

class AlbumDetailViewModel(
    private val albumId: Long,
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController,
    private val filterArtistId: Long? = null,
    private val filterComposer: String? = null,
    private val filterGenre: String? = null
) : ViewModel() {
    private val listeningOrigin = ListeningOrigin("album", albumId, filterArtistId, filterComposer, filterGenre)

    private val _state = MutableStateFlow(AlbumDetailState())
    val state = _state.asStateFlow()

    init {
        loadAlbum()
        observeSongs()
    }

    fun play(song: Song) {
        viewModelScope.launch {
            playbackController.play(song, _state.value.songs, listeningOrigin)
        }
    }

    fun playAll(shuffle: Boolean = false) {
        val songs = _state.value.songs
        if (songs.isEmpty()) return

        val queue = if (shuffle) songs.shuffled() else songs
        viewModelScope.launch {
            playbackController.play(queue.first(), queue, listeningOrigin)
        }
    }

    private fun loadAlbum() {
        viewModelScope.launch {
            musicRepository.getAlbumById(albumId)?.let { album ->
                _state.update { it.copy(album = album) }
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
            _state.update { it.copy(songs = songs) }
        }.launchIn(viewModelScope)
    }
}
