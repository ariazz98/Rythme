package com.aria.rythme.feature.playlistdetail.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.PlaylistRepository
import com.aria.rythme.core.navigation.Navigator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val navigator: Navigator,
    private val playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController
) : BaseViewModel<PlaylistDetailIntent, PlaylistDetailState, PlaylistDetailAction, PlaylistDetailEffect>() {

    init {
        loadPlaylist()
        observeSongs()
    }

    override fun createInitialState(): PlaylistDetailState = PlaylistDetailState()

    override fun handleIntent(intent: PlaylistDetailIntent) {
        when (intent) {
            is PlaylistDetailIntent.GoBack -> navigator.goBack()
            is PlaylistDetailIntent.PlaySong -> playSong(intent.song)
            is PlaylistDetailIntent.PlayAll -> playAll()
            is PlaylistDetailIntent.ShufflePlay -> shufflePlay()
            is PlaylistDetailIntent.RemoveSong -> {
                viewModelScope.launch {
                    playlistRepository.removeSongFromPlaylist(playlistId, intent.songId)
                    // 重新加载歌单信息以更新歌曲数量
                    loadPlaylist()
                }
            }
        }
    }

    override fun reduce(action: PlaylistDetailAction): PlaylistDetailState {
        return when (action) {
            is PlaylistDetailAction.PlaylistLoaded -> currentState.copy(
                playlist = action.playlist,
                isLoading = false
            )
            is PlaylistDetailAction.SongsLoaded -> currentState.copy(
                songs = action.songs
            )
        }
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            playlistRepository.getPlaylistById(playlistId)?.let { playlist ->
                reduceAndUpdate(PlaylistDetailAction.PlaylistLoaded(playlist))
            }
        }
    }

    private fun observeSongs() {
        playlistRepository.getPlaylistSongs(playlistId)
            .onEach { songs ->
                reduceAndUpdate(PlaylistDetailAction.SongsLoaded(songs))
            }
            .launchIn(viewModelScope)
    }

    private fun playSong(song: Song) {
        viewModelScope.launch {
            playbackController.play(song, currentState.songs)
        }
    }

    private fun playAll() {
        viewModelScope.launch {
            val songs = currentState.songs
            if (songs.isNotEmpty()) {
                playbackController.play(songs.first(), songs)
            }
        }
    }

    private fun shufflePlay() {
        viewModelScope.launch {
            val songs = currentState.songs
            if (songs.isNotEmpty()) {
                val shuffled = songs.shuffled()
                playbackController.play(shuffled.first(), shuffled)
            }
        }
    }
}
