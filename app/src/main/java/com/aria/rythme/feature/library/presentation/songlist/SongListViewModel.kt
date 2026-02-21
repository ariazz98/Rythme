package com.aria.rythme.feature.library.presentation.songlist

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.feature.player.controller.PlaybackController
import com.aria.rythme.feature.player.data.model.Song
import com.aria.rythme.feature.player.data.repository.SongCacheRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 歌曲列表 ViewModel
 */
class SongListViewModel(
    private val songRepository: SongCacheRepository,
    private val playbackController: PlaybackController
) : BaseViewModel<SongListIntent, SongListState, SongListAction, SongListEffect>() {

    companion object {
        private const val TAG = "SongListViewModel"
    }

    init {
        loadSongs()
    }

    override fun createInitialState(): SongListState {
        return SongListState()
    }

    override fun handleIntent(intent: SongListIntent) {
        when (intent) {
            is SongListIntent.LoadSongs -> loadSongs()
            is SongListIntent.PlayAll -> playAllSongs()
            is SongListIntent.ToggleShuffle -> toggleShuffle()
            is SongListIntent.PlaySong -> playSong(intent.song)
            is SongListIntent.ShowSongOptions -> showSongOptions(intent.song)
        }
    }

    override fun reduce(action: SongListAction): SongListState {
        return when (action) {
            is SongListAction.SongsLoaded -> currentState.copy(
                songs = action.songs,
                isLoading = false
            )
            is SongListAction.ShuffleToggled -> currentState.copy(
                isShuffleEnabled = action.isEnabled
            )
            is SongListAction.LoadingChanged -> currentState.copy(
                isLoading = action.isLoading
            )
        }
    }

    /**
     * 加载歌曲列表
     */
    private fun loadSongs() {
        viewModelScope.launch {
            try {
                songRepository.getAllSongs().collectLatest { songs ->
                    RythmeLogger.d(TAG, "Loaded ${songs.size} songs")
                    reduceAndUpdate(SongListAction.SongsLoaded(songs))
                }
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "Failed to load songs", e)
                sendEffect(SongListEffect.ShowToast("加载歌曲失败"))
            }
        }
    }

    /**
     * 播放所有歌曲
     */
    private fun playAllSongs() {
        val songs = currentState.songs
        if (songs.isEmpty()) {
            sendEffect(SongListEffect.ShowToast("没有可播放的歌曲"))
            return
        }

        viewModelScope.launch {
            try {
                val songsToPlay = if (currentState.isShuffleEnabled) {
                    songs.shuffled()
                } else {
                    songs
                }
                // 播放第一首歌，并传入完整的播放列表
                playbackController.play(songsToPlay.first(), songsToPlay)
                RythmeLogger.d(TAG, "Playing all ${songsToPlay.size} songs")
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "Failed to play all songs", e)
                sendEffect(SongListEffect.ShowToast("播放失败"))
            }
        }
    }

    /**
     * 切换随机播放
     */
    private fun toggleShuffle() {
        val newState = !currentState.isShuffleEnabled
        reduceAndUpdate(SongListAction.ShuffleToggled(newState))
        RythmeLogger.d(TAG, "Shuffle toggled: $newState")
    }

    /**
     * 播放指定歌曲
     */
    private fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                val songs = currentState.songs
                // 播放指定歌曲，并传入完整的播放列表
                playbackController.play(song, songs)
                RythmeLogger.d(TAG, "Playing song: ${song.title}")
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "Failed to play song", e)
                sendEffect(SongListEffect.ShowToast("播放失败"))
            }
        }
    }

    /**
     * 显示歌曲更多选项
     */
    private fun showSongOptions(song: Song) {
        // TODO: 实现显示更多选项的底部弹窗
        RythmeLogger.d(TAG, "Show options for: ${song.title}")
    }
}

/**
 * 内部动作
 */
sealed interface SongListAction : InternalAction {
    data class SongsLoaded(val songs: List<Song>) : SongListAction
    data class ShuffleToggled(val isEnabled: Boolean) : SongListAction
    data class LoadingChanged(val isLoading: Boolean) : SongListAction
}
