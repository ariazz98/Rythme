package com.aria.rythme.feature.player.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.LyricsStatus
import com.aria.rythme.core.music.data.repository.LyricsRepository
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
    private val musicRepository: MusicRepository,
    private val lyricsRepository: LyricsRepository
) : BaseViewModel<PlayerIntent, PlayerState, PlayerAction, PlayerEffect>() {

    /** 进度更新任务 */
    private var progressUpdateJob: Job? = null

    /** 歌词加载任务 */
    private var lyricsLoadJob: Job? = null

    /** 上一首加载过歌词的歌曲 ID */
    private var lastLyricsSongId: Long? = null

    init {
        observePlaybackState()
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
            is PlayerIntent.LoadAndPlayRandom -> loadAndPlayRandom()
            is PlayerIntent.SelectSongFromPlaylist -> selectSongFromPlaylist(intent.index)
            is PlayerIntent.SetVolume -> setVolume(intent.percentage)
            is PlayerIntent.ReorderPlaylist -> reorderPlaylist(intent.from, intent.to)
            is PlayerIntent.ToggleCrossfade -> toggleCrossfade()
            is PlayerIntent.ToggleInfinitePlay -> toggleInfinitePlay()
            is PlayerIntent.SeekToLyricLine -> seekToLyricLine(intent.index)
            is PlayerIntent.RefreshLyrics -> refreshLyrics()
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
            is PlayerAction.UpdateVolume -> currentState.copy(volume = action.volume)
            is PlayerAction.UpdatePlayHistory -> currentState.copy(playHistory = action.history)
            is PlayerAction.UpdateCrossfadeMode -> currentState.copy(isCrossfadeEnabled = action.enabled)
            is PlayerAction.UpdateInfinitePlayMode -> currentState.copy(isInfinitePlayEnabled = action.enabled)
            is PlayerAction.UpdateIsPlayingInfiniteExtension -> currentState.copy(isPlayingInfiniteExtension = action.isInExtension)
            is PlayerAction.UpdateInfiniteExtension -> currentState.copy(infiniteExtension = action.extension)
            is PlayerAction.UpdateOrderedPlaylistSize -> currentState.copy(orderedPlaylistSize = action.size)
            is PlayerAction.UpdateLyrics -> currentState.copy(
                lyricsData = action.data,
                lyricsStatus = action.status,
                currentLyricIndex = -1
            )
            is PlayerAction.UpdateCurrentLyricIndex -> currentState.copy(currentLyricIndex = action.index)
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
        // 立即更新状态，避免暂停时进度不刷新
        reduceAndUpdate(PlayerAction.UpdateProgress(position, currentState.duration))
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
    }

    /**
     * 随机播放一首歌曲
     *
     * 从数据库读取全部歌曲并随机播放。MusicIndexer 已由 MainActivity 初始化。
     */
    private fun loadAndPlayRandom() {
        RythmeLogger.d(TAG, "随机播放")
        viewModelScope.launch {
            try {
                val songs = musicRepository.getAllSongsOnce()
                if (songs.isNotEmpty()) {
                    val randomSong = songs.random()
                    RythmeLogger.d(TAG, "随机播放: ${randomSong.title}")
                    playbackController.play(randomSong, songs)
                } else {
                    sendEffect(PlayerEffect.ShowMessage("没有找到可播放的歌曲"))
                }
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "加载失败", e)
                sendEffect(PlayerEffect.ShowError("加载失败: ${e.message}"))
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
     * 拖拽重排播放列表
     */
    private fun reorderPlaylist(from: Int, to: Int) {
        viewModelScope.launch {
            playbackController.movePlaylistItem(from, to)
        }
    }

    /**
     * 切换交叉淡入淡出
     */
    private fun toggleCrossfade() {
        playbackController.toggleCrossfade()
        reduceAndUpdate(PlayerAction.UpdateCrossfadeMode(playbackController.isCrossfadeEnabled.value))
    }

    /**
     * 切换无限播放
     */
    private fun toggleInfinitePlay() {
        viewModelScope.launch {
            try {
                val allSongs = musicRepository.getAllSongsOnce()
                playbackController.toggleInfinitePlay(allSongs)
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "切换无限播放失败", e)
                sendEffect(PlayerEffect.ShowError("操作失败: ${e.message}"))
            }
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
                    // 歌曲切换时加载歌词
                    if (song.id != lastLyricsSongId) {
                        loadLyrics(song)
                    }
                } ?: run {
                    reduceAndUpdate(PlayerAction.UpdateLyrics(null, LyricsStatus.IDLE))
                    lastLyricsSongId = null
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
        
        // 监听播放历史（独立启动）
        playbackController.playHistory
            .onEach { history ->
                reduceAndUpdate(PlayerAction.UpdatePlayHistory(history))
            }
            .launchIn(viewModelScope)

        // 监听随机播放状态（独立启动）
        playbackController.shuffleMode
            .onEach { enabled ->
                reduceAndUpdate(PlayerAction.UpdateShuffleMode(enabled))
            }
            .launchIn(viewModelScope)

        // 监听循环模式（独立启动）
        playbackController.repeatMode
            .onEach { repeatMode ->
                reduceAndUpdate(PlayerAction.UpdateRepeatMode(repeatMode))
            }
            .launchIn(viewModelScope)

        // 监听交叉淡入淡出状态（独立启动）
        playbackController.isCrossfadeEnabled
            .onEach { enabled ->
                reduceAndUpdate(PlayerAction.UpdateCrossfadeMode(enabled))
            }
            .launchIn(viewModelScope)

        // 监听无限播放状态（独立启动）
        playbackController.isInfinitePlayEnabled
            .onEach { enabled ->
                reduceAndUpdate(PlayerAction.UpdateInfinitePlayMode(enabled))
            }
            .launchIn(viewModelScope)

        // 监听是否在播放 infinite 扩展歌曲（独立启动）
        playbackController.isPlayingInfiniteExtension
            .onEach { isInExtension ->
                reduceAndUpdate(PlayerAction.UpdateIsPlayingInfiniteExtension(isInExtension))
            }
            .launchIn(viewModelScope)

        // 监听 infinite 扩展列表（独立启动）
        playbackController.infiniteExtension
            .onEach { extension ->
                reduceAndUpdate(PlayerAction.UpdateInfiniteExtension(extension))
                reduceAndUpdate(PlayerAction.UpdateOrderedPlaylistSize(playbackController.orderedPlaylistSize))
            }
            .launchIn(viewModelScope)

        // 监听播放列表变化时同步 orderedPlaylistSize
        playbackController.playlist
            .onEach {
                reduceAndUpdate(PlayerAction.UpdateOrderedPlaylistSize(playbackController.orderedPlaylistSize))
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
                val position = playbackController.getCurrentPosition()
                val duration = playbackController.getDuration()
                reduceAndUpdate(PlayerAction.UpdateProgress(position, duration))
                // 计算当前歌词行
                updateCurrentLyricIndex(position)
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
     * 加载歌词
     */
    private fun loadLyrics(song: com.aria.rythme.core.music.data.model.Song) {
        lyricsLoadJob?.cancel()
        lastLyricsSongId = song.id
        reduceAndUpdate(PlayerAction.UpdateLyrics(null, LyricsStatus.LOADING))

        lyricsLoadJob = viewModelScope.launch {
            try {
                val data = lyricsRepository.getLyrics(song)
                // 确保歌曲未切换
                if (currentState.currentSong?.id == song.id) {
                    if (data != null) {
                        reduceAndUpdate(PlayerAction.UpdateLyrics(data, LyricsStatus.LOADED))
                    } else {
                        reduceAndUpdate(PlayerAction.UpdateLyrics(null, LyricsStatus.NOT_FOUND))
                    }
                }
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "歌词加载失败", e)
                if (currentState.currentSong?.id == song.id) {
                    reduceAndUpdate(PlayerAction.UpdateLyrics(null, LyricsStatus.ERROR))
                }
            }
        }
    }

    /**
     * 刷新歌词（强制重新获取）
     */
    private fun refreshLyrics() {
        currentState.currentSong?.let { loadLyrics(it) }
    }

    /**
     * 点击歌词行跳转播放
     */
    private fun seekToLyricLine(index: Int) {
        val lines = currentState.lyricsData?.lines ?: return
        if (index in lines.indices) {
            val timeMs = lines[index].startTimeMs
            playbackController.seekTo(timeMs)
            reduceAndUpdate(PlayerAction.UpdateProgress(timeMs, currentState.duration))
            reduceAndUpdate(PlayerAction.UpdateCurrentLyricIndex(index))
        }
    }

    /**
     * 二分查找当前歌词行
     *
     * 仅当 index 变化时 dispatch Action，避免无谓的状态更新。
     */
    private fun updateCurrentLyricIndex(positionMs: Long) {
        val lines = currentState.lyricsData?.lines ?: return
        if (lines.isEmpty()) return

        val newIndex = findCurrentLineIndex(lines, positionMs)
        if (newIndex != currentState.currentLyricIndex) {
            reduceAndUpdate(PlayerAction.UpdateCurrentLyricIndex(newIndex))
        }
    }

    /**
     * 二分查找：找到最后一个 startTimeMs <= positionMs 的行
     */
    private fun findCurrentLineIndex(
        lines: List<com.aria.rythme.core.music.data.model.LyricLine>,
        positionMs: Long
    ): Int {
        var low = 0
        var high = lines.size - 1
        var result = -1

        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].startTimeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
        lyricsLoadJob?.cancel()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
        private const val PROGRESS_UPDATE_INTERVAL = 200L
    }
}
