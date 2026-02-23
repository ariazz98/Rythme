package com.aria.rythme.feature.player.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 播放器 ViewModel
 *
 * 管理播放器页面的 UI 状态和用户交互。
 * 所有数据操作已下沉到 MusicRepository，ViewModel 只负责 UI 状态管理。
 *
 * ## 架构分层
 * - ViewModel：UI 状态管理、用户交互处理
 * - Repository：数据加载、刷新、MediaStore 监听（数据层闭环）
 * - PlaybackController：播放控制
 *
 * @param playbackController 播放控制器
 * @param musicRepository 音乐数据仓库
 */
class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val musicRepository: MusicRepository
) : BaseViewModel<PlayerIntent, PlayerState, PlayerAction, PlayerEffect>() {

    /** 进度更新任务 */
    private var progressUpdateJob: Job? = null
    
    /** 歌曲列表观察任务 */
    private var songsObserverJob: Job? = null

    init {
        // 监听播放控制器状态
        observePlaybackState()
        
        // 监听数据仓库加载状态
        observeRepositoryState()
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
            is PlayerIntent.RefreshSongs -> refreshSongs()
            is PlayerIntent.LoadAndPlayRandom -> loadAndPlayRandom()
            is PlayerIntent.SelectSongFromPlaylist -> selectSongFromPlaylist(intent.index)
            is PlayerIntent.SetVolume -> setVolume(intent.percentage)
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
            is PlayerAction.UpdateVolume -> currentState.copy(volume = action.volume)
        }
    }

    /**
     * 播放歌曲
     */
    private fun playSong(song: com.aria.rythme.core.music.data.model.Song) {
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
        reduceAndUpdate(PlayerAction.UpdateRepeatMode(playbackController.repeatMode.value))
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
     * 
     * 委托给 MusicRepository 处理：
     * 1. 订阅 Repository 的歌曲流
     * 2. Repository 自动触发扫描和同步
     */
    private fun loadSongs() {
        RythmeLogger.d(TAG, "开始加载歌曲列表")
        
        // 取消之前的订阅
        songsObserverJob?.cancel()
        
        // 订阅 Repository 的歌曲流
        songsObserverJob = musicRepository.getAllSongs()
            .onEach { songs ->
                RythmeLogger.d(TAG, "歌曲列表更新: ${songs.size} 首")
                reduceAndUpdate(PlayerAction.UpdatePlaylist(songs))
                
                // 设置默认歌曲
                if (songs.isNotEmpty() && currentState.currentSong == null) {
                    RythmeLogger.d(TAG, "设置默认歌曲: ${songs.first().title}")
                    reduceAndUpdate(PlayerAction.UpdateCurrentSong(songs.first()))
                    reduceAndUpdate(PlayerAction.UpdateCurrentIndex(0))
                }
            }
            .launchIn(viewModelScope)
        
        // 触发数据加载（Repository 内部处理）
        viewModelScope.launch {
            val result = musicRepository.loadSongs()
            result.onFailure { error ->
                RythmeLogger.e(TAG, "加载失败", error)
                sendEffect(PlayerEffect.ShowError("加载失败: ${error.message}"))
            }
        }
    }

    /**
     * 强制刷新歌曲列表
     * 
     * 委托给 MusicRepository 处理刷新逻辑
     */
    private fun refreshSongs() {
        RythmeLogger.d(TAG, "强制刷新歌曲列表")
        viewModelScope.launch {
            val result = musicRepository.refreshSongs()
            result.onSuccess { scanResult ->
                sendEffect(PlayerEffect.ShowMessage("已刷新 ${scanResult.scannedCount} 首歌曲"))
            }.onFailure { error ->
                RythmeLogger.e(TAG, "刷新失败", error)
                sendEffect(PlayerEffect.ShowError("刷新失败: ${error.message}"))
            }
        }
    }

    /**
     * 加载歌曲列表并随机播放一首
     * 
     * 当当前没有歌曲播放时，加载所有歌曲并随机播放一首
     */
    private fun loadAndPlayRandom() {
        RythmeLogger.d(TAG, "加载歌曲列表并随机播放")
        viewModelScope.launch {
            // 先加载歌曲列表
            val result = musicRepository.loadSongs()
            result.onSuccess {
                // 获取所有歌曲
                val songs = musicRepository.getAllSongsOnce()
                if (songs.isNotEmpty()) {
                    // 随机选择一首歌曲
                    val randomSong = songs.random()
                    RythmeLogger.d(TAG, "随机播放: ${randomSong.title}")
                    // 播放选中的歌曲
                    playbackController.play(randomSong, songs)
                } else {
                    sendEffect(PlayerEffect.ShowMessage("没有找到可播放的歌曲"))
                }
            }.onFailure { error ->
                RythmeLogger.e(TAG, "加载失败", error)
                sendEffect(PlayerEffect.ShowError("加载失败: ${error.message}"))
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
     * 设置音量
     */
    private fun setVolume(percentage: Int) {
        playbackController.setVolumePercentage(percentage)
    }

    /**
     * 监听播放状态
     * 
     * 注意：每个 Flow 需要独立启动，不能包裹在同一个 launch 块中
     */
    private fun observePlaybackState() {
        // 监听播放状态（独立启动）
        playbackController.isPlaying
            .onEach { isPlaying ->
                reduceAndUpdate(PlayerAction.UpdatePlayState(isPlaying))
                // 根据播放状态启停进度更新
                if (isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }
            .launchIn(viewModelScope)

        // 监听当前歌曲（独立启动）
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
        
        // 监听播放列表（独立启动）
        playbackController.playlist
            .onEach { playlist ->
                if (playlist.isNotEmpty()) {
                    RythmeLogger.d(TAG, "播放列表更新: ${playlist.size} 首")
                    reduceAndUpdate(PlayerAction.UpdatePlaylist(playlist))
                }
            }
            .launchIn(viewModelScope)
        
        // 监听音量变化（独立启动）
        playbackController.volume
            .onEach { volume ->
                reduceAndUpdate(PlayerAction.UpdateVolume(volume))
            }
            .launchIn(viewModelScope)
        
        // 监听循环模式（独立启动）
        playbackController.repeatMode
            .onEach { repeatMode ->
                reduceAndUpdate(PlayerAction.UpdateRepeatMode(repeatMode))
            }
            .launchIn(viewModelScope)
    }

    /**
     * 开始进度更新
     * 
     * 仅在播放时启动，避免无谓的 CPU 消耗
     */
    private fun startProgressUpdate() {
        if (progressUpdateJob?.isActive == true) return
        
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                // 直接从 PlaybackController 获取实时位置
                val position = playbackController.getCurrentPosition()
                val duration = playbackController.getDuration()
                reduceAndUpdate(PlayerAction.UpdateProgress(position, duration))
                delay(PROGRESS_UPDATE_INTERVAL)
            }
        }
    }
    
    /**
     * 停止进度更新
     */
    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

   /**
     * 监听数据仓库状态
     * 
     * 同步 Repository 的加载状态和错误信息到 UI 状态
     */
    private fun observeRepositoryState() {
        // 监听加载状态
        musicRepository.isLoading
            .onEach { isLoading ->
                reduceAndUpdate(PlayerAction.SetLoading(isLoading))
            }
            .launchIn(viewModelScope)
        
        // 监听错误信息
        musicRepository.error
            .onEach { error ->
                error?.let {
                    reduceAndUpdate(PlayerAction.SetError(it))
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * 清理资源
     * 
     * 注意：PlaybackController 和 MusicRepository 都是单例，不需要在此处释放
     */
    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
        songsObserverJob?.cancel()
    }
    
    companion object {
        private const val TAG = "PlayerViewModel"
        private const val PROGRESS_UPDATE_INTERVAL = 500L  // 500ms 更新一次，更平滑
    }
}
