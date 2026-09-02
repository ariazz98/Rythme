package com.aria.rythme.feature.songlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 歌曲列表 ViewModel
 *
 * 页面级 ViewModel，负责从 MusicRepository 读取歌曲列表。
 * 播放操作通过 PlaybackController 直接发起，PlayerViewModel 会自动观察到状态变化。
 */
class SongListViewModel(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val songs: StateFlow<List<Song>> = musicRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun playAll() {
        playQueue(songs.value)
    }

    fun shufflePlay() {
        playQueue(songs.value.shuffled())
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            playbackController.play(song, songs.value)
        }
    }

    private fun playQueue(queue: List<Song>) {
        if (queue.isEmpty()) return

        viewModelScope.launch {
            playbackController.play(queue.first(), queue)
        }
    }
}
