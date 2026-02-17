package com.aria.rythme.feature.player.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.feature.player.controller.PlaybackController
import com.aria.rythme.feature.player.data.datasource.MediaStoreSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 播放器 ViewModel
 *
 * 管理播放器页面的业务逻辑，处理用户交互和播放状态同步。
 *
 * @param playbackController 播放控制器
 * @param mediaStoreSource MediaStore 数据源
 */
class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val mediaStoreSource: MediaStoreSource
) : BaseViewModel<PlayerIntent, PlayerState, PlayerAction, PlayerEffect>() {

    /** 进度更新任务 */
    private var progressUpdateJob: Job? = null

    init {
        // 监听播放控制器状态
        observePlaybackState()

        // 开始进度更新
        startProgressUpdate()
    }

    /**
     * 创建初始状态
     */
    override fun createInitialState(): PlayerState = PlayerState()

    /**
     * 处理用户意图
     */
    override fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.PlaySong -> playSong(intent.song)
            is PlayerIntent.TogglePlayPause -> togglePlayPause()
            is PlayerIntent.Play -> play()
            is PlayerIntent.Pause -> pause()
            is PlayerIntent.Next -> next()
            is PlayerIntent.Previous -> previous()
            is PlayerIntent.SeekTo -> seekTo(intent.position)
            is PlayerIntent.FastForward -> fastForward()
            is PlayerIntent.Rewind -> rewind()
            is PlayerIntent.ToggleRepeatMode -> toggleRepeatMode()
            is PlayerIntent.ToggleShuffleMode -> toggleShuffleMode()
            is PlayerIntent.LoadSongs -> loadSongs()
            is PlayerIntent.SelectSongFromPlaylist -> selectSongFromPlaylist(intent.index)
        }
    }

    /**
     * 状态归约
     */
    override fun reduce(action: PlayerAction): PlayerState {
        return when (action) {
            is PlayerAction.UpdatePlayState -> currentState.copy(isPlaying = action.isPlaying)
            is PlayerAction.UpdateCurrentSong -> currentState.copy(currentSong = action.song)
            is PlayerAction.UpdateProgress -> currentState.copy(
                currentPosition = action.position,
                duration = action.duration
            )
            is PlayerAction.UpdatePlaylist -> currentState.copy(playlist = action.playlist)
            is PlayerAction.UpdateCurrentIndex -> currentState.copy(currentIndex = action.index)
            is PlayerAction.UpdateRepeatMode -> currentState.copy(repeatMode = action.mode)
            is PlayerAction.UpdateShuffleMode -> currentState.copy(isShuffleEnabled = action.enabled)
            is PlayerAction.UpdateThemeColor -> currentState.copy(themeColor = action.color)
            is PlayerAction.SetLoading -> currentState.copy(isLoading = action.isLoading)
            is PlayerAction.SetError -> currentState.copy(errorMessage = action.message)
        }
    }

    /**
     * 播放歌曲
     */
    private fun playSong(song: com.aria.rythme.feature.player.data.model.Song) {
        viewModelScope.launch {
            playbackController.play(song, currentState.playlist)
        }
    }

    /**
     * 切换播放/暂停
     * 
     * 如果播放器还没有加载内容，先加载当前歌曲再播放
     */
    private fun togglePlayPause() {
        viewModelScope.launch {
            // 如果有当前歌曲且不在播放状态，先加载再播放
            val currentSong = currentState.currentSong
            if (currentSong != null && !currentState.isPlaying) {
                // 检查是否需要加载歌曲到播放器
                if (playbackController.currentSong.value == null) {
                    playbackController.play(currentSong, currentState.playlist)
                } else {
                    playbackController.togglePlayPause()
                }
            } else {
                playbackController.togglePlayPause()
            }
        }
    }

    /**
     * 播放
     */
    private fun play() {
        playbackController.play()
    }

    /**
     * 暂停
     */
    private fun pause() {
        playbackController.pause()
    }

    /**
     * 下一首
     */
    private fun next() {
        playbackController.next()
    }

    /**
     * 上一首
     */
    private fun previous() {
        playbackController.previous()
    }

    /**
     * 跳转到指定位置
     */
    private fun seekTo(position: Long) {
        playbackController.seekTo(position)
    }

    /**
     * 快进
     */
    private fun fastForward() {
        playbackController.fastForward()
    }

    /**
     * 快退
     */
    private fun rewind() {
        playbackController.rewind()
    }

    /**
     * 切换循环模式
     */
    private fun toggleRepeatMode() {
        playbackController.toggleRepeatMode()
        val controllerMode = playbackController.repeatMode.value
        val mode = when (controllerMode) {
            PlaybackController.RepeatMode.OFF -> RepeatMode.OFF
            PlaybackController.RepeatMode.ONE -> RepeatMode.ONE
            PlaybackController.RepeatMode.ALL -> RepeatMode.ALL
        }
        reduceAndUpdate(PlayerAction.UpdateRepeatMode(mode))
    }

    /**
     * 切换随机播放
     */
    private fun toggleShuffleMode() {
        playbackController.toggleShuffleMode()
        reduceAndUpdate(PlayerAction.UpdateShuffleMode(playbackController.shuffleMode.value))
    }

    /**
     * 加载歌曲列表
     */
    private fun loadSongs() {
        RythmeLogger.d(TAG, "开始加载歌曲列表")
        viewModelScope.launch {
            reduceAndUpdate(PlayerAction.SetLoading(true))
            try {
                mediaStoreSource.getAllSongs()
                    .onEach { songs ->
                        RythmeLogger.d(TAG, "加载到 ${songs.size} 首歌曲")
                        reduceAndUpdate(PlayerAction.UpdatePlaylist(songs))
                        if (songs.isNotEmpty() && currentState.currentSong == null) {
                            RythmeLogger.d(TAG, "设置默认歌曲: ${songs.first().title}")
                            reduceAndUpdate(PlayerAction.UpdateCurrentSong(songs.first()))
                            reduceAndUpdate(PlayerAction.UpdateCurrentIndex(0))
                        }
                        reduceAndUpdate(PlayerAction.SetLoading(false))
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "加载歌曲失败", e)
                reduceAndUpdate(PlayerAction.SetError(e.message))
                reduceAndUpdate(PlayerAction.SetLoading(false))
                sendEffect(PlayerEffect.ShowError("加载歌曲失败: ${e.message}"))
            }
        }
    }

    /**
     * 从播放列表选择歌曲
     */
    private fun selectSongFromPlaylist(index: Int) {
        viewModelScope.launch {
            playbackController.playAtIndex(index)
        }
    }

    /**
     * 监听播放状态
     */
    private fun observePlaybackState() {
        viewModelScope.launch {
            // 监听播放状态
            playbackController.isPlaying
                .onEach { isPlaying ->
                    reduceAndUpdate(PlayerAction.UpdatePlayState(isPlaying))
                }
                .launchIn(viewModelScope)

            // 监听当前歌曲
            playbackController.currentSong
                .onEach { song ->
                    reduceAndUpdate(PlayerAction.UpdateCurrentSong(song))
                    song?.let {
                        val index = currentState.playlist.indexOfFirst { it.id == song.id }
                        if (index >= 0) {
                            reduceAndUpdate(PlayerAction.UpdateCurrentIndex(index))
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * 开始进度更新
     */
    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                val position = playbackController.currentPosition.value
                val duration = playbackController.duration.value
                reduceAndUpdate(PlayerAction.UpdateProgress(position, duration))
                delay(1000) // 每秒更新一次
            }
        }
    }

    /**
     * 清理资源
     * 
     * 注意：PlaybackController 是单例，不应在此处释放
     */
    override fun onCleared() {
        super.onCleared()
        progressUpdateJob?.cancel()
        // 不要释放 PlaybackController，因为它是单例
    }
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }
}
