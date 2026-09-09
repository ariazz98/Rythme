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
import com.aria.rythme.core.music.domain.model.PlaybackQueue
import com.aria.rythme.core.music.domain.model.QueueEntry
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.aria.rythme.core.music.data.repository.ListeningHistoryRepository
import com.aria.rythme.core.music.data.repository.ListeningOrigin
import com.aria.rythme.core.music.data.repository.ResumePoint

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
class PlaybackController(private val context: Context, private val listeningHistory: ListeningHistoryRepository) {

    private var listeningOrigin = ListeningOrigin()
    private var listeningEntryId: String? = null
    private var listeningPositionJob: Job? = null

    private fun saveListeningPosition(player: Player, allowNewListen: Boolean) {
        val snapshot = _queue.value
        val entryId = player.currentMediaItem?.mediaId ?: return
        val index = snapshot.indexOf(entryId)
        val entry = snapshot.entries.getOrNull(index) ?: return
        val newListen = listeningEntryId != entryId
        if (newListen && !allowNewListen) return
        listeningEntryId = entryId
        val point = ResumePoint(entry.song.id, player.currentPosition.coerceAtLeast(0),
            snapshot.entries.map { it.song.id }, index, listeningOrigin)
        scope.launch {
            try { listeningHistory.record(point, newListen) }
            catch (error: java.io.IOException) { RythmeLogger.e(TAG, "保存收听位置失败", error) }
        }
    }

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    /** 初始化完成信号 */
    private val initializationDeferred = CompletableDeferred<Unit>()
    
    /** 播放器监听器实例（用于正确移除） */
    private val playerListener = PlayerListener()

    /** 上一次真正进入播放位置的队列条目，用于记录历史而不受预先发布的 UI 状态影响。 */
    private var lastTransitionedEntry: QueueEntry? = null
    
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

    // ── 播放队列数据源 ──

    // 原始队列（不受 shuffle 影响的源顺序）
    private var sourceQueue: List<QueueEntry> = emptyList()

    // 排序后的队列（shuffle 时物理重排，shuffle OFF 时与 sourceQueue 相同）
    private var orderedQueue: List<QueueEntry> = emptyList()

    // 自动播放扩展队列
    private var autoplayQueue: List<QueueEntry> = emptyList()

    // 队列、当前项和有序/自动播放边界必须作为同一个快照发布，避免 UI 观察到中间态。
    private val _queue = MutableStateFlow(PlaybackQueue())
    val queue: StateFlow<PlaybackQueue> = _queue.asStateFlow()

    // 播放历史（最近播放的歌曲，最新在前，上限50条）
    private val _playHistory = MutableStateFlow<List<Song>>(emptyList())
    val playHistory: StateFlow<List<Song>> = _playHistory.asStateFlow()

    // 播放错误
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 音量百分比 (0-100)
    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    // 交叉淡入淡出产品开关；实际双播放器音频混合将在播放内核阶段接入。
    private val _isCrossfadeEnabled = MutableStateFlow(false)
    val isCrossfadeEnabled: StateFlow<Boolean> = _isCrossfadeEnabled.asStateFlow()

    // 无限播放
    private val _isInfinitePlayEnabled = MutableStateFlow(false)
    val isInfinitePlayEnabled: StateFlow<Boolean> = _isInfinitePlayEnabled.asStateFlow()

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
     * 重建并原子发布最终播放队列。
     *
     * [preferredCurrentEntryId] 用队列条目身份保持当前位置；同一歌曲重复出现时不会跳到第一项。
     */
    private fun rebuildFinalQueue(
        preferredCurrentEntryId: String? = _queue.value.currentEntry?.id
    ) {
        val entries = if (_isInfinitePlayEnabled.value && _repeatMode.value == RepeatMode.OFF) {
            orderedQueue + autoplayQueue
        } else {
            orderedQueue
        }
        val preferredIndex = entries.indexOfFirst { it.id == preferredCurrentEntryId }
        val currentIndex = when {
            entries.isEmpty() -> -1
            preferredIndex >= 0 -> preferredIndex
            else -> _queue.value.currentIndex.coerceIn(entries.indices)
        }
        _queue.value = PlaybackQueue(
            entries = entries,
            currentIndex = currentIndex,
            orderedEntryCount = orderedQueue.size
        )
    }

    private fun updateCurrentIndex(index: Int) {
        val entries = _queue.value.entries
        _queue.value = _queue.value.copy(
            currentIndex = if (entries.isEmpty()) -1 else index.coerceIn(entries.indices)
        )
    }

    /**
     * 同步 ExoPlayer 媒体列表
     *
     * 用当前队列替换 ExoPlayer 中的所有 MediaItems，保持当前条目和播放位置。
     */
    private fun syncExoPlayer(positionMs: Long? = null) {
        val controller = mediaController ?: return
        val snapshot = _queue.value
        if (snapshot.entries.isEmpty()) return

        val idx = snapshot.currentIndex.coerceIn(snapshot.entries.indices)
        val mediaItems = snapshot.entries.map { createMediaItem(it) }
        controller.setMediaItems(mediaItems, idx, positionMs ?: controller.currentPosition)
        updateCurrentIndex(idx)
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
    suspend fun play(
        song: Song,
        playlist: List<Song> = emptyList(),
        origin: ListeningOrigin = ListeningOrigin(),
        startPositionMs: Long = 0L,
        startIndex: Int? = null
    ) {
        RythmeLogger.d(TAG, "准备播放: ${song.title}")
        
        // 等待初始化完成
        awaitInitialization()
        
        val controller = mediaController
        if (controller == null) {
            RythmeLogger.e(TAG, "mediaController 为 null，无法播放")
            _error.value = "播放器未初始化"
            return
        }

        listeningOrigin = origin
        listeningEntryId = null
        if (playlist.isNotEmpty()) {
            val sourceIndex = startIndex?.takeIf { playlist.getOrNull(it)?.id == song.id }
                ?: playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

            // 输入歌单变化时创建新的入队记录；相同歌单则复用条目身份。
            if (sourceQueue.map { it.song } != playlist) {
                RythmeLogger.d(TAG, "加载播放列表: ${playlist.size} 首歌曲")
                sourceQueue = playlist.map(QueueEntry::create)
                orderedQueue = sourceQueue
                autoplayQueue = emptyList()
                _shuffleMode.value = false
                _isInfinitePlayEnabled.value = false

                val targetEntryId = sourceQueue[sourceIndex].id
                rebuildFinalQueue(preferredCurrentEntryId = targetEntryId)
                val mediaItems = _queue.value.entries.map { createMediaItem(it) }
                controller.setMediaItems(mediaItems, _queue.value.currentIndex, startPositionMs.coerceAtLeast(0))
                controller.prepare()
                controller.play()
            } else {
                val targetEntryId = sourceQueue[sourceIndex].id
                val queueIndex = _queue.value.indexOf(targetEntryId).coerceAtLeast(0)
                RythmeLogger.d(TAG, "跳转到队列索引: $queueIndex")
                updateCurrentIndex(queueIndex)
                controller.seekTo(queueIndex, startPositionMs.coerceAtLeast(0))
                controller.play()
            }
        } else {
            RythmeLogger.d(TAG, "单曲播放: ${song.title}")
            val entry = QueueEntry.create(song)
            sourceQueue = listOf(entry)
            orderedQueue = sourceQueue
            autoplayQueue = emptyList()
            _shuffleMode.value = false
            _isInfinitePlayEnabled.value = false
            rebuildFinalQueue(preferredCurrentEntryId = entry.id)
            val mediaItem = createMediaItem(entry)
            controller.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0))
            controller.prepare()
            controller.play()
        }
        RythmeLogger.d(TAG, "已调用 controller.play()")
    }

    /**
     * 播放指定队列条目。
     */
    suspend fun playQueueEntry(entryId: String) {
        awaitInitialization()
        val controller = mediaController ?: return
        val snapshot = _queue.value
        val index = snapshot.indexOf(entryId)

        if (index in snapshot.entries.indices) {
            updateCurrentIndex(index)
            if (controller.mediaItemCount > 0) {
                controller.seekToDefaultPosition(index)
                controller.play()
            } else {
                val mediaItems = snapshot.entries.map { createMediaItem(it) }
                controller.setMediaItems(mediaItems, index, 0L)
                controller.prepare()
                controller.play()
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
            controller.seekToNextMediaItem()
        } else if (_queue.value.entries.isNotEmpty()) {
            val snapshot = _queue.value
            val nextIndex = (snapshot.currentIndex + 1) % snapshot.entries.size
            scope.launch { playQueueEntry(snapshot.entries[nextIndex].id) }
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
        } else if (_queue.value.entries.isNotEmpty()) {
            val snapshot = _queue.value
            val currentIndex = snapshot.currentIndex
            val previousIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else {
                snapshot.entries.size - 1
            }
            scope.launch { playQueueEntry(snapshot.entries[previousIndex].id) }
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
        val oldMode = _repeatMode.value
        _repeatMode.value = mode
        controller.repeatMode = mode.toExoPlayerMode()

        // repeat 状态变化影响是否追加 infinite extension
        if (_isInfinitePlayEnabled.value && ((oldMode == RepeatMode.OFF) != (mode == RepeatMode.OFF))) {
            rebuildFinalQueue()
            syncExoPlayer()
        }
    }

    /**
     * 切换播放模式
     */
    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    /**
     * 设置随机播放
     *
     * 开启时将歌单中当前位置之后的歌曲随机重排。
     * 关闭时恢复原始顺序。
     *
     * @param enabled 是否启用
     */
    suspend fun setShuffleMode(enabled: Boolean) {
        awaitInitialization()
        if (sourceQueue.isEmpty()) return

        if (enabled && !_shuffleMode.value) {
            // 当前位置之前（含当前）保持不变，之后的歌曲随机重排
            val currentEntryId = _queue.value.currentEntry?.id
            val sourceIndex = sourceQueue.indexOfFirst { it.id == currentEntryId }
                .coerceAtLeast(0)
            val past = sourceQueue.subList(0, sourceIndex + 1)
            val upcoming = sourceQueue.subList(sourceIndex + 1, sourceQueue.size).shuffled()
            orderedQueue = past + upcoming
        } else if (!enabled && _shuffleMode.value) {
            // 恢复原始顺序
            orderedQueue = sourceQueue
        }

        _shuffleMode.value = enabled
        rebuildFinalQueue()
        syncExoPlayer()
    }

    /**
     * 切换随机播放
     */
    fun toggleShuffleMode() {
        scope.launch { setShuffleMode(!_shuffleMode.value) }
    }

    /** 保留播放器中的交叉淡入淡出入口和选择状态。 */
    fun toggleCrossfade() {
        _isCrossfadeEnabled.value = !_isCrossfadeEnabled.value
        RythmeLogger.d(TAG, "Crossfade: ${_isCrossfadeEnabled.value}")
    }

    /**
     * 切换无限播放
     *
     * 激活时生成随机歌曲列表；取消时清空。
     * 最终播放列表是否追加由 repeat 模式决定（repeat 激活时不追加）。
     *
     * @param allSongs 全部可用歌曲
     */
    suspend fun toggleInfinitePlay(allSongs: List<Song>) {
        awaitInitialization()

        if (_isInfinitePlayEnabled.value) {
            val currentEntry = _queue.value.currentEntry
            val wasInExtension = currentEntry != null &&
                autoplayQueue.any { it.id == currentEntry.id }
            autoplayQueue = emptyList()
            _isInfinitePlayEnabled.value = false

            val targetEntryId = if (wasInExtension) orderedQueue.lastOrNull()?.id else currentEntry?.id
            rebuildFinalQueue(preferredCurrentEntryId = targetEntryId)
            syncExoPlayer(positionMs = if (wasInExtension) 0L else null)
            RythmeLogger.d(TAG, "无限播放已关闭")
        } else {
            // 激活：生成随机歌曲列表
            val currentIds = orderedQueue.map { it.song.id }.toSet()
            val candidates = allSongs.filter { it.id !in currentIds }

            autoplayQueue = candidates
                .shuffled()
                .take(INFINITE_EXTENSION_SIZE)
                .map(QueueEntry::create)
            RythmeLogger.d(TAG, "无限播放已开启，生成 ${autoplayQueue.size} 首随机歌曲")
            _isInfinitePlayEnabled.value = true
            rebuildFinalQueue()
            syncExoPlayer()
        }
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

        sourceQueue = songs.map(QueueEntry::create)
        orderedQueue = sourceQueue
        autoplayQueue = emptyList()
        _shuffleMode.value = false
        _isInfinitePlayEnabled.value = false

        val targetEntryId = sourceQueue[startIndex.coerceIn(sourceQueue.indices)].id
        rebuildFinalQueue(preferredCurrentEntryId = targetEntryId)
        val mediaItems = _queue.value.entries.map { createMediaItem(it) }
        controller.setMediaItems(mediaItems, _queue.value.currentIndex, 0L)
        controller.prepare()
        controller.play()

        RythmeLogger.d(TAG, "已设置播放队列: ${songs.size} 首歌曲，从索引 ${_queue.value.currentIndex} 开始")
    }

    /**
     * 追加一次新的入队记录；已有相同歌曲时不会复用或覆盖它。
     */
    fun addToQueue(song: Song) {
        val entry = QueueEntry.create(song)
        sourceQueue = sourceQueue + entry
        orderedQueue = orderedQueue + entry
        rebuildFinalQueue()
        mediaController?.addMediaItem(orderedQueue.size - 1, createMediaItem(entry))
    }

    /**
     * 按入队身份删除一项，不影响同一歌曲的其他入队记录。
     */
    fun removeQueueEntry(entryId: String) {
        val snapshot = _queue.value
        val index = snapshot.indexOf(entryId)
        if (index !in snapshot.entries.indices) return

        val currentEntryId = snapshot.currentEntry?.id
        val nextCurrentEntryId = if (entryId == currentEntryId) {
            snapshot.entries.getOrNull(index + 1)?.id
                ?: snapshot.entries.getOrNull(index - 1)?.id
        } else {
            currentEntryId
        }

        sourceQueue = sourceQueue.filterNot { it.id == entryId }
        orderedQueue = orderedQueue.filterNot { it.id == entryId }
        autoplayQueue = autoplayQueue.filterNot { it.id == entryId }
        rebuildFinalQueue(preferredCurrentEntryId = nextCurrentEntryId)
        mediaController?.removeMediaItem(index)
    }

    /**
     * 移动队列条目位置。
     *
     * 仅支持有序队列内部或自动播放队列内部重排，不能跨边界。
     */
    suspend fun moveQueueEntry(fromEntryId: String, toEntryId: String) {
        val snapshot = _queue.value
        val from = snapshot.indexOf(fromEntryId)
        val to = snapshot.indexOf(toEntryId)
        if (from !in snapshot.entries.indices || to !in snapshot.entries.indices || from == to) return

        val currentEntryId = snapshot.currentEntry?.id
        val orderedSize = orderedQueue.size
        val fromInOrdered = from < orderedSize
        val toInOrdered = to < orderedSize

        // 不允许跨列表重排
        if (fromInOrdered != toInOrdered) return

        if (fromInOrdered) {
            val ordered = orderedQueue.toMutableList()
            val item = ordered.removeAt(from)
            ordered.add(to, item)
            orderedQueue = ordered

            if (!_shuffleMode.value) {
                sourceQueue = ordered
            }
        } else {
            val extFrom = from - orderedSize
            val extTo = to - orderedSize
            val ext = autoplayQueue.toMutableList()
            val item = ext.removeAt(extFrom)
            ext.add(extTo, item)
            autoplayQueue = ext
        }

        rebuildFinalQueue(preferredCurrentEntryId = currentEntryId)

        awaitInitialization()
        mediaController?.moveMediaItem(from, to)
    }

    /**
     * 清空播放队列
     */
    fun clearQueue() {
        sourceQueue = emptyList()
        orderedQueue = emptyList()
        autoplayQueue = emptyList()
        _shuffleMode.value = false
        _isInfinitePlayEnabled.value = false
        rebuildFinalQueue(preferredCurrentEntryId = null)
        mediaController?.clearMediaItems()
    }

    /**
     * 清空播放历史
     */
    fun clearHistory() {
        _playHistory.value = emptyList()
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
    private fun createMediaItem(entry: QueueEntry): MediaItem {
        val song = entry.song
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.coverUri)
            .build()

        return MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(entry.id)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * 播放器监听
     */
    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            listeningPositionJob?.cancel()
            mediaController?.let { player ->
                saveListeningPosition(player, allowNewListen = isPlaying)
                if (isPlaying) listeningPositionJob = scope.launch {
                    while (isActive) {
                        delay(5_000)
                        if (player.isPlaying) saveListeningPosition(player, allowNewListen = true)
                    }
                }
            }
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
                saveListeningPosition(player, allowNewListen = player.isPlaying)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val newEntry = mediaItem?.let { item ->
                _queue.value.entries.find { it.id == item.mediaId }
            }
            val previousEntry = lastTransitionedEntry
            if (previousEntry != null && previousEntry.id != newEntry?.id) {
                val current = _playHistory.value
                val filtered = current.filter { it.id != previousEntry.song.id }
                _playHistory.value = (listOf(previousEntry.song) + filtered).take(MAX_HISTORY_SIZE)
            }

            if (newEntry == null) {
                lastTransitionedEntry = null
            } else {
                lastTransitionedEntry = newEntry
                val newIndex = _queue.value.indexOf(newEntry.id)
                if (newIndex >= 0) {
                    updateCurrentIndex(newIndex)
                }
            }
        }
    }

    companion object {
        private const val TAG = "PlaybackController"
        private const val MAX_HISTORY_SIZE = 50
        private const val INFINITE_EXTENSION_SIZE = 30
    }
}
