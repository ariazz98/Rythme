package com.aria.rythme.feature.playlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.data.model.Playlist
import com.aria.rythme.core.music.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayListState(
    val playlists: List<Playlist> = emptyList(),
    val showCreateDialog: Boolean = false
)

class PlayListViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayListState())
    val state = _state.asStateFlow()

    init {
        playlistRepository.getAllPlaylists()
            .onEach { playlists -> _state.update { it.copy(playlists = playlists) } }
            .launchIn(viewModelScope)
    }

    fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true) }
    }

    fun dismissCreateDialog() {
        _state.update { it.copy(showCreateDialog = false) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
            _state.update { it.copy(showCreateDialog = false) }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
        }
    }
}
