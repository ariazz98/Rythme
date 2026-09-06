package com.aria.rythme.feature.playlistdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Playlist
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistDetailState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state = _state.asStateFlow()

    init {
        loadPlaylist()
        playlistRepository.getPlaylistSongs(playlistId)
            .onEach { songs -> _state.update { it.copy(songs = songs) } }
            .launchIn(viewModelScope)
    }

    fun playSong(song: Song) {
        playQueue(_state.value.songs, song)
    }

    fun playAll(shuffle: Boolean = false) {
        val songs = _state.value.songs
        val queue = if (shuffle) songs.shuffled() else songs
        playQueue(queue, queue.firstOrNull())
    }

    fun removeSong(songId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
            loadPlaylist()
        }
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            playlistRepository.getPlaylistById(playlistId)?.let { playlist ->
                _state.update { it.copy(playlist = playlist, isLoading = false) }
            }
        }
    }

    private fun playQueue(queue: List<Song>, first: Song?) {
        if (first == null) return
        viewModelScope.launch {
            playbackController.play(first, queue)
        }
    }
}
