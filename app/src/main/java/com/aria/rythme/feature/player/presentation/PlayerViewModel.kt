package com.aria.rythme.feature.player.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.feature.player.controller.PlaybackController
import com.aria.rythme.feature.player.data.datasource.MediaStoreSource
import com.aria.rythme.feature.player.data.observer.ChangeType
import com.aria.rythme.feature.player.data.observer.MediaStoreObserver
import com.aria.rythme.feature.player.data.repository.SongCacheRepository
import com.aria.rythme.feature.player.domain.model.RepeatMode
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
 * ## 数据流设计（单一可信数据源）
 * - 歌曲数据统一从 SongCacheRepository 读取（Room Flow）
 * - MediaStoreSource 只负责扫描和写入 Room
 * - Room 数据变化会自动通知 UI
 *
 * @param playbackController 播放控制器
 * @param songCacheRepository 歌曲缓存仓库（单一数据源）
 * @param mediaStoreSource MediaStore 扫描器（只写入）
 * @param mediaStoreObserver MediaStore 观察者
 */
class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val songCacheRepository: SongCacheRepository,
    private val mediaStoreSource: MediaStoreSource,
    private val mediaStoreObserver: MediaStoreObserver
) : BaseViewModel<PlayerIntent, PlayerState, PlayerAction, PlayerEffect>() {

    /** 进度更新任务 */
    private var progressUpdateJob: Job? = null
    
    /** MediaStore 观察任务 */
    private var observerJob: Job? = null
    
    /** 歌曲列表观察任务 */
    private var songsObserverJob: Job? = null

    init {
        // 监听播放控制器状态（包括播放状态驱动的进度更新）
        observePlaybackState()
        
        // 监听 MediaStore 变化
        observeMediaStoreChanges()
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
     * 1. 订阅 Room Flow（单一数据源）
     * 2. 触发 MediaStore 扫描同步到 Room
     * 3. Room 数据变化会自动通过 Flow 更新 UI
     */
    private fun loadSongs() {
        RythmeLogger.d(TAG, "开始加载歌曲列表")
        
        // 取消之前的订阅
        songsObserverJob?.cancel()
        
        // 订阅 Room Flow（单一数据源）
        songsObserverJob = songCacheRepository.getAllSongs()
            .onEach { songs ->
                RythmeLogger.d(TAG, "Room 数据更新: ${songs.size} 首歌曲")
                reduceAndUpdate(PlayerAction.UpdatePlaylist(songs))
                
                // 设置默认歌曲
                if (songs.isNotEmpty() && currentState.currentSong == null) {
                    RythmeLogger.d(TAG, "设置默认歌曲: ${songs.first().title}")
                    reduceAndUpdate(PlayerAction.UpdateCurrentSong(songs.first()))
                    reduceAndUpdate(PlayerAction.UpdateCurrentIndex(0))
                }
                
                reduceAndUpdate(PlayerAction.SetLoading(false))
            }
            .launchIn(viewModelScope)
        
        // 触发扫描（写入 Room，Room Flow 会自动通知 UI）
        viewModelScope.launch {
            reduceAndUpdate(PlayerAction.SetLoading(true))
            try {
                val result = mediaStoreSource.scanAndSync()
                RythmeLogger.d(TAG, "扫描完成: ${result.scannedCount} 首歌曲")
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "扫描失败", e)
                reduceAndUpdate(PlayerAction.SetError(e.message))
                reduceAndUpdate(PlayerAction.SetLoading(false))
                sendEffect(PlayerEffect.ShowError("扫描失败: ${e.message}"))
            }
        }
    }

    /**
     * 强制刷新歌曲列表
     * 
     * 只触发扫描，Room Flow 会自动更新 UI
     */
    private fun refreshSongs() {
        RythmeLogger.d(TAG, "强制刷新歌曲列表")
        viewModelScope.launch {
            reduceAndUpdate(PlayerAction.SetLoading(true))
            try {
                val result = mediaStoreSource.scanAndSync()
                RythmeLogger.d(TAG, "刷新完成: ${result.scannedCount} 首歌曲")
                reduceAndUpdate(PlayerAction.SetLoading(false))
                sendEffect(PlayerEffect.ShowMessage("已刷新 ${result.scannedCount} 首歌曲"))
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "刷新失败", e)
                reduceAndUpdate(PlayerAction.SetError(e.message))
                reduceAndUpdate(PlayerAction.SetLoading(false))
                sendEffect(PlayerEffect.ShowError("刷新失败: ${e.message}"))
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
     * 清理资源
     * 
     * 注意：PlaybackController 是单例，不应在此处释放
     */
    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
        observerJob?.cancel()
        songsObserverJob?.cancel()
        // 不要释放 PlaybackController，因为它是单例
    }
    
    /**
     * 监听 MediaStore 变化
     * 
     * 检测到变化时只触发扫描，Room Flow 会自动更新 UI
     */
    private fun observeMediaStoreChanges() {
        observerJob = viewModelScope.launch {
            mediaStoreObserver.observeChanges()
                .collect { change ->
                    if (change.type == ChangeType.AUDIO_CHANGED) {
                        RythmeLogger.d(TAG, "检测到音频文件变化，触发后台扫描")
                        // 只触发扫描，Room Flow 会自动更新 UI
                        try {
                            val result = mediaStoreSource.scanAndSync()
                            RythmeLogger.d(TAG, "后台扫描完成: ${result.scannedCount} 首歌曲")
                        } catch (e: Exception) {
                            RythmeLogger.e(TAG, "后台扫描失败", e)
                        }
                    }
                }
        }
    }
    
    companion object {
        private const val TAG = "PlayerViewModel"
        private const val PROGRESS_UPDATE_INTERVAL = 500L  // 500ms 更新一次，更平滑
    }
}
