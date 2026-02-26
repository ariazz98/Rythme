package com.aria.rythme.core.music.controller

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.domain.model.RepeatMode
import com.aria.rythme.core.music.service.MusicPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 播放控制器
 *
 * 封装 MediaController，提供简化的播放控制接口。
 * 管理播放状态、播放列表和当前歌曲信息。
 *
 * ## 使用方式
 * ```kotlin
 * val controller = PlaybackController(context)
 * controller.initialize()
 *
 * // 播放歌曲
 * controller.play(song)
 *
 * // 控制播放
 * controller.togglePlayPause()
 * controller.next()
 * controller.previous()
 * controller.seekTo(position)
 *
 * // 观察状态
 * val isPlaying by controller.isPlaying.collectAsState()
 * val currentSong by controller.currentSong.collectAsState()
 * ```
 *
 * @param context 应用上下文
 */
class PlaybackController(private val context: Context) {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    /** 初始化完成信号 */
    private val initializationDeferred = CompletableDeferred<Unit>()
    
    /** 播放器监听器实例（用于正确移除） */
    private val playerListener = PlayerListener()
    
    /** 协程作用域 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /** 是否已初始化完成 */
    val isInitialized: Boolean
        get() = initializationDeferred.isCompleted
    
    /** 音频管理器 */
    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    /** 音量变化监听器（BroadcastReceiver）*/
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                RythmeLogger.d(TAG, "收到音量变化广播 - Action: ${intent.action}, StreamType: $streamType")

                if (streamType == AudioManager.STREAM_MUSIC) {
                    updateVolumeState()
                    RythmeLogger.d(TAG, "音量变化（Broadcast）: ${_volume.value}%")
                }
            }
        }
    }
    
    /** 音量监听器是否已注册 */
    private var isVolumeReceiverRegistered = false

    // 播放状态
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // 当前歌曲
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    // 当前位置
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    // 总时长
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // 播放模式
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // 是否随机播放
    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    // 播放列表
    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    // 当前索引
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // 播放错误
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 音量百分比 (0-100)
    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    /**
     * 初始化控制器
     *
     * 绑定到 MusicPlaybackService，创建 MediaController 实例。
     * 必须在调用其他方法之前调用。
     */
    fun initialize() {
        RythmeLogger.d(TAG, "开始初始化 PlaybackController")
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .apply {
                addListener({
                    try {
                        mediaController = get().apply {
                            addListener(playerListener)
                            updateStateFromPlayer()
                        }
                        initializationDeferred.complete(Unit)
                        RythmeLogger.d(TAG, "PlaybackController 初始化成功")
                    } catch (e: Exception) {
                        RythmeLogger.e(TAG, "PlaybackController 初始化失败", e)
                        _error.value = "初始化失败: ${e.message}"
                        initializationDeferred.completeExceptionally(e)
                    }
                }, MoreExecutors.directExecutor())
            }
        
        // 注册音量变化监听
        registerVolumeReceiver()
        // 初始化音量状态
        updateVolumeState()
    }
    
    /**
     * 等待初始化完成
     * 
     * 使用 CompletableDeferred 挂起等待，避免忙等待浪费 CPU
     */
    private suspend fun awaitInitialization() {
        initializationDeferred.await()
    }

    /**
     * 播放指定歌曲
     *
     * 如果提供了播放列表，会把整个列表加载到 ExoPlayer，并跳转到指定歌曲。
     * 这样 ExoPlayer 可以原生支持上一首/下一首切换。
     *
     * @param song 要播放的歌曲
     * @param playlist 可选的播放列表上下文
     */
    suspend fun play(song: Song, playlist: List<Song> = emptyList()) {
        RythmeLogger.d(TAG, "准备播放: ${song.title}")
        
        // 等待初始化完成
        awaitInitialization()
        
        val controller = mediaController
        if (controller == null) {
            RythmeLogger.e(TAG, "mediaController 为 null，无法播放")
            _error.value = "播放器未初始化"
            return
        }

        if (playlist.isNotEmpty()) {
            val songIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            
            // 检查播放列表是否已经加载
            if (_playlist.value != playlist) {
                // 播放列表变化，重新加载所有 MediaItems
                RythmeLogger.d(TAG, "加载播放列表: ${playlist.size} 首歌曲")
                _playlist.value = playlist
                
                val mediaItems = playlist.map { createMediaItem(it) }
                controller.setMediaItems(mediaItems, songIndex, 0L)
                controller.prepare()
                controller.play()
            } else {
                // 播放列表相同，只需跳转到指定位置
                RythmeLogger.d(TAG, "跳转到索引: $songIndex")
                controller.seekToDefaultPosition(songIndex)
                controller.play()
            }
            
            _currentIndex.value = songIndex
        } else {
            // 没有播放列表，单曲播放
            RythmeLogger.d(TAG, "单曲播放: ${song.title}")
            val mediaItem = createMediaItem(song)
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }

        _currentSong.value = song
        RythmeLogger.d(TAG, "已调用 controller.play()")
    }

    /**
     * 播放播放列表中的指定位置
     * 
     * 使用 ExoPlayer 原生的 seekToDefaultPosition 切换歌曲，更高效
     *
     * @param index 歌曲索引
     */
    suspend fun playAtIndex(index: Int) {
        awaitInitialization()
        val controller = mediaController ?: return
        val songs = _playlist.value
        
        if (index in songs.indices) {
            _currentIndex.value = index
            _currentSong.value = songs[index]
            
            // 使用 ExoPlayer 原生切换
            if (controller.mediaItemCount > 0) {
                controller.seekToDefaultPosition(index)
                controller.play()
            } else {
                // 播放列表未加载，先加载
                play(songs[index], songs)
            }
        }
    }

    /**
     * 切换播放/暂停
     */
    suspend fun togglePlayPause() {
        awaitInitialization()
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    /**
     * 播放
     */
    fun play() {
        mediaController?.play()
    }

    /**
     * 暂停
     */
    fun pause() {
        mediaController?.pause()
    }

    /**
     * 下一首
     * 
     * 优先使用 ExoPlayer 原生的 seekToNextMediaItem
     */
    fun next() {
        val controller = mediaController ?: return
        
        if (controller.mediaItemCount > 1) {
            // 使用 ExoPlayer 原生切换（已支持随机/顺序/循环模式）
            controller.seekToNextMediaItem()
        } else if (_playlist.value.isNotEmpty()) {
            // 转为手动切换
            val currentIndex = _currentIndex.value
            val nextIndex = if (_shuffleMode.value) {
                _playlist.value.indices.random()
            } else {
                (currentIndex + 1) % _playlist.value.size
            }
            scope.launch { playAtIndex(nextIndex) }
        }
    }

    /**
     * 上一首
     * 
     * 优先使用 ExoPlayer 原生的 seekToPreviousMediaItem
     */
    fun previous() {
        val controller = mediaController ?: return
        
        if (controller.mediaItemCount > 1) {
            // 使用 ExoPlayer 原生切换
            controller.seekToPreviousMediaItem()
        } else if (_playlist.value.isNotEmpty()) {
            // 转为手动切换
            val currentIndex = _currentIndex.value
            val previousIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else {
                _playlist.value.size - 1
            }
            scope.launch { playAtIndex(previousIndex) }
        }
    }

    /**
     * 跳转到指定位置
     *
     * @param position 位置（毫秒）
     */
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    /**
     * 快进
     *
     * @param milliseconds 快进毫秒数
     */
    fun fastForward(milliseconds: Long = 10000) {
        val controller = mediaController ?: return
        val newPosition = controller.currentPosition + milliseconds
        controller.seekTo(newPosition.coerceAtMost(controller.duration))
    }

    /**
     * 快退
     *
     * @param milliseconds 快退毫秒数
     */
    fun rewind(milliseconds: Long = 10000) {
        val controller = mediaController ?: return
        val newPosition = controller.currentPosition - milliseconds
        controller.seekTo(newPosition.coerceAtLeast(0))
    }

    /**
     * 设置播放模式
     *
     * @param mode 播放模式
     */
    fun setRepeatMode(mode: RepeatMode) {
        val controller = mediaController ?: return
        _repeatMode.value = mode
        controller.repeatMode = mode.toExoPlayerMode()
    }

    /**
     * 切换播放模式
     */
    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    /**
     * 设置随机播放
     *
     * @param enabled 是否启用
     */
    fun setShuffleMode(enabled: Boolean) {
        val controller = mediaController ?: return
        _shuffleMode.value = enabled
        controller.shuffleModeEnabled = enabled
    }

    /**
     * 切换随机播放
     */
    fun toggleShuffleMode() {
        setShuffleMode(!_shuffleMode.value)
    }

    /**
     * 设置播放列表
     * 
     * 批量加载所有 MediaItems 到 ExoPlayer
     *
     * @param songs 歌曲列表
     * @param startIndex 开始播放的索引
     */
    suspend fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        awaitInitialization()
        val controller = mediaController ?: return
        
        _playlist.value = songs
        _currentIndex.value = startIndex.coerceIn(songs.indices)
        _currentSong.value = songs[_currentIndex.value]
        
        // 批量加载所有 MediaItems
        val mediaItems = songs.map { createMediaItem(it) }
        controller.setMediaItems(mediaItems, _currentIndex.value, 0L)
        controller.prepare()
        controller.play()
        
        RythmeLogger.d(TAG, "已设置播放列表: ${songs.size} 首歌曲，从索引 ${_currentIndex.value} 开始")
    }

    /**
     * 添加歌曲到播放列表
     *
     * @param song 歌曲
     */
    fun addToPlaylist(song: Song) {
        _playlist.value = _playlist.value + song
    }

    /**
     * 从播放列表移除歌曲
     *
     * @param index 索引
     */
    fun removeFromPlaylist(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _playlist.value = currentList

            // 如果删除的是当前播放的歌曲，播放下一首
            if (index == _currentIndex.value) {
                next()
            }
        }
    }

    /**
     * 清空播放列表
     */
    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentIndex.value = 0
    }

    /**
     * 释放资源
     */
    fun release() {
        unregisterVolumeReceiver()
        scope.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
    }

    /**
     * 获取当前播放位置（实时）
     * 
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: _currentPosition.value
    }

    /**
     * 获取总时长（实时）
     * 
     * @return 总时长（毫秒）
     */
    fun getDuration(): Long {
        return mediaController?.duration?.coerceAtLeast(0) ?: _duration.value
    }

    /**
     * 从播放器更新状态
     */
    private fun updateStateFromPlayer() {
        val controller = mediaController ?: return
        _isPlaying.value = controller.isPlaying
        _currentPosition.value = controller.currentPosition
        _duration.value = controller.duration.coerceAtLeast(0)
    }

    /**
     * 获取当前音量百分比
     * 
     * @return 音量百分比 (0-100)
     */
    fun getVolumePercentage(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) {
            (currentVolume.toFloat() / maxVolume * 100).toInt()
        } else {
            0
        }
    }

    /**
     * 设置音量百分比（静默设置，不显示系统 UI）
     * 
     * @param percentage 音量百分比 (0-100)
     */
    fun setVolumePercentage(percentage: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (percentage / 100f * maxVolume).toInt().coerceIn(0, maxVolume)
        
        // 使用 FLAG = 0 静默设置，不显示系统音量 UI
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        
        RythmeLogger.d(TAG, "设置音量: $percentage% (实际: $volume/$maxVolume)")
    }

    /**
     * 增加音量
     * 
     * @param showUI 是否显示系统音量 UI
     */
    fun volumeUp(showUI: Boolean = false) {
        val flags = if (showUI) AudioManager.FLAG_SHOW_UI else 0
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            flags
        )
        RythmeLogger.d(TAG, "音量增加: ${_volume.value}%")
    }

    /**
     * 减少音量
     * 
     * @param showUI 是否显示系统音量 UI
     */
    fun volumeDown(showUI: Boolean = false) {
        val flags = if (showUI) AudioManager.FLAG_SHOW_UI else 0
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            flags
        )
        RythmeLogger.d(TAG, "音量减少: ${_volume.value}%")
    }

    /**
     * 静音/取消静音
     */
    fun toggleMute() {
        val currentVolume = getVolumePercentage()
        if (currentVolume > 0) {
            // 当前有音量，设置为静音
            setVolumePercentage(0)
            RythmeLogger.d(TAG, "已静音")
        } else {
            // 当前静音，恢复到 50%
            setVolumePercentage(50)
            RythmeLogger.d(TAG, "取消静音")
        }
    }

    /**
     * 更新音量状态
     */
    private fun updateVolumeState() {
        _volume.value = getVolumePercentage()
    }

    /**
     * 注册音量变化监听器（BroadcastReceiver）
     */
    private fun registerVolumeReceiver() {
        if (isVolumeReceiverRegistered) return
        
        try {
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            context.registerReceiver(volumeReceiver, filter)
            isVolumeReceiverRegistered = true
            RythmeLogger.d(TAG, "已注册音量变化监听器（Broadcast）")
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "注册音量监听器失败", e)
        }
    }

    /**
     * 注销音量变化监听器
     */
    private fun unregisterVolumeReceiver() {
        if (!isVolumeReceiverRegistered) return
        
        try {
            context.unregisterReceiver(volumeReceiver)
            isVolumeReceiverRegistered = false
            RythmeLogger.d(TAG, "已注销音量变化监听器")
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "注销音量监听器失败", e)
        }
    }

    /**
     * 创建 MediaItem
     */
    private fun createMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.coverUri)
            .build()

        return MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * 播放器监听
     */
    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentPosition.value = newPosition.positionMs
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _duration.value = mediaController?.duration?.coerceAtLeast(0) ?: 0
            }
        }
        
        override fun onEvents(player: Player, events: Player.Events) {
            // 持续更新当前位置和时长
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) || 
                events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION
                )) {
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration.coerceAtLeast(0)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                // 更新当前歌曲和索引
                val songId = it.mediaId.toLongOrNull()
                val song = _playlist.value.find { s -> s.id == songId }
                _currentSong.value = song
                
                // 同步索引
                val newIndex = _playlist.value.indexOfFirst { s -> s.id == songId }
                if (newIndex >= 0) {
                    _currentIndex.value = newIndex
                }
            }
        }
    }

    companion object {
        private const val TAG = "PlaybackController"
    }
}
