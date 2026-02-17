package com.aria.rythme.feature.player.controller

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.feature.player.data.model.Song
import com.aria.rythme.feature.player.service.MusicPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    
    /** 协程作用域 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /** 是否已初始化完成 */
    @Volatile
    private var isInitialized = false

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
                            addListener(PlayerListener())
                            updateStateFromPlayer()
                        }
                        isInitialized = true
                        RythmeLogger.d(TAG, "PlaybackController 初始化成功")
                    } catch (e: Exception) {
                        RythmeLogger.e(TAG, "PlaybackController 初始化失败", e)
                        _error.value = "初始化失败: ${e.message}"
                    }
                }, MoreExecutors.directExecutor())
            }
    }
    
    /**
     * 等待初始化完成
     */
    private suspend fun awaitInitialization() {
        while (!isInitialized) {
            delay(50)
        }
    }

    /**
     * 播放指定歌曲
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

        // 更新播放列表
        if (playlist.isNotEmpty()) {
            _playlist.value = playlist
            _currentIndex.value = playlist.indexOfFirst { it.id == song.id }
        }

        // 创建 MediaItem
        val mediaItem = createMediaItem(song)
        RythmeLogger.d(TAG, "创建 MediaItem: uri=${song.uri}")

        // 设置并播放
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
        RythmeLogger.d(TAG, "已调用 controller.play()")

        _currentSong.value = song
    }

    /**
     * 播放播放列表中的指定位置
     *
     * @param index 歌曲索引
     */
    suspend fun playAtIndex(index: Int) {
        val songs = _playlist.value
        if (index in songs.indices) {
            _currentIndex.value = index
            play(songs[index], songs)
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
     */
    fun next() {
        val controller = mediaController ?: return
        val playlist = _playlist.value
        val currentIndex = _currentIndex.value

        if (playlist.isNotEmpty()) {
            val nextIndex = if (_shuffleMode.value) {
                // 随机播放
                playlist.indices.random()
            } else {
                // 顺序播放
                (currentIndex + 1) % playlist.size
            }
            scope.launch { playAtIndex(nextIndex) }
        } else {
            controller.seekToNext()
        }
    }

    /**
     * 上一首
     */
    fun previous() {
        val controller = mediaController ?: return
        val playlist = _playlist.value
        val currentIndex = _currentIndex.value

        if (playlist.isNotEmpty()) {
            val previousIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else {
                playlist.size - 1
            }
            scope.launch { playAtIndex(previousIndex) }
        } else {
            controller.seekToPrevious()
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
        controller.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
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
     * @param songs 歌曲列表
     * @param startIndex 开始播放的索引
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        _playlist.value = songs
        if (startIndex in songs.indices) {
            scope.launch { playAtIndex(startIndex) }
        }
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
        scope.cancel()
        mediaController?.removeListener(PlayerListener())
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        isInitialized = false
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

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                // 更新当前歌曲
                val songId = it.mediaId.toLongOrNull()
                val song = _playlist.value.find { s -> s.id == songId }
                _currentSong.value = song
            }
        }
    }

    /**
     * 播放模式
     */
    enum class RepeatMode {
        OFF,    // 不循环
        ONE,    // 单曲循环
        ALL     // 列表循环
    }
    
    companion object {
        private const val TAG = "PlaybackController"
    }
}
