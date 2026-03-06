package com.aria.rythme.feature.songlist.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.navigation.Navigator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 歌曲列表 ViewModel
 *
 * 页面级 ViewModel，负责从 MusicRepository 读取歌曲列表。
 * 播放操作通过 PlaybackController 直接发起，PlayerViewModel 会自动观察到状态变化。
 */
class SongListViewModel(
    private val navigator: Navigator,
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : BaseViewModel<SongListIntent, SongListState, SongListAction, SongListEffect>() {

    init {
        observeSongs()
    }

    override fun createInitialState(): SongListState = SongListState()

    override fun handleIntent(intent: SongListIntent) {
        when (intent) {
            is SongListIntent.GoBack -> navigator.goBack()
            is SongListIntent.PlayAll -> playAll()
            is SongListIntent.ShufflePlay -> shufflePlay()
            is SongListIntent.PlaySong -> playSong(intent.song)
            is SongListIntent.ShowSongOptions -> { /* TODO */ }
        }
    }

    override fun reduce(action: SongListAction): SongListState {
        return when (action) {
            is SongListAction.SongsLoaded -> currentState.copy(
                songs = action.songs,
                isLoading = false
            )
        }
    }

    private fun observeSongs() {
        musicRepository.getAllSongs()
            .onEach { songs ->
                reduceAndUpdate(SongListAction.SongsLoaded(songs))
            }
            .launchIn(viewModelScope)
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

    private fun playSong(song: Song) {
        viewModelScope.launch {
            playbackController.play(song, currentState.songs)
        }
    }
}
