package com.aria.rythme.feature.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import com.aria.rythme.core.utils.RythmeLogger
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.aria.rythme.MainActivity
import androidx.core.app.NotificationCompat.Action as NotificationAction
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * 音乐播放服务
 *
 * 前台服务，负责在后台持续播放音乐。
 * 集成 Media3 ExoPlayer 和 MediaSession，支持系统媒体控制。
 *
 * ## 功能特性
 * - 后台播放保持
 * - 通知栏控制
 * - 锁屏显示
 * - 音频焦点管理
 * - 耳机控制响应
 *
 * ## 生命周期
 * - 启动：通过 startForegroundService() 启动
 * - 运行：播放音乐时保持前台状态
 * - 停止：无播放内容且用户清除通知时停止
 *
 * @see ExoPlayer 媒体播放器
 * @see MediaSession 媒体会话
 */
@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusGranted = false

    override fun onCreate() {
        super.onCreate()
        RythmeLogger.d(TAG, "服务创建")
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initializePlayer()
    }

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(): Boolean {
        if (audioFocusGranted) return true

        return run {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = focusRequest
            val result = audioManager?.requestAudioFocus(focusRequest)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }.also { audioFocusGranted = it == true }
    }

    /**
     * 放弃音频焦点
     */
    private fun abandonAudioFocus() {
        if (!audioFocusGranted) return

        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        audioFocusGranted = false
    }

    /**
     * 音频焦点变化监听
     */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 获得音频焦点，恢复播放
                if (exoPlayer?.playWhenReady == false) {
                    exoPlayer?.play()
                }
                exoPlayer?.volume = 1f
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久失去焦点，暂停播放
                exoPlayer?.pause()
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时失去焦点，暂停播放
                exoPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 暂时失去焦点，但可以降低音量继续播放
                exoPlayer?.volume = 0.3f
            }
        }
    }

    /**
     * 初始化播放器
     */
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false)  // false: 我们手动管理音频焦点
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(PlayerEventListener())
            }
        
        RythmeLogger.d(TAG, "ExoPlayer 初始化完成")

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setCallback(MediaSessionCallback())
            .build()
    }

    /**
     * 获取媒体会话
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * 更新通知
     */
    private fun updateNotification() {
        val player = exoPlayer ?: return
        val notification = buildNotification(player)
        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * 构建通知
     */
    private fun buildNotification(player: ExoPlayer): Notification {
        val mediaItem = player.currentMediaItem
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: "未知歌曲"
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: "未知艺术家"

        // 播放/暂停按钮
        val playPauseAction = NotificationAction(
            if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (player.isPlaying) "暂停" else "播放",
            createPendingIntent(ACTION_PLAY_PAUSE)
        )

        // 下一首按钮
        val nextAction = NotificationAction(
            android.R.drawable.ic_media_next,
            "下一首",
            createPendingIntent(ACTION_NEXT)
        )

        // 上一首按钮
        val previousAction = NotificationAction(
            android.R.drawable.ic_media_previous,
            "上一首",
            createPendingIntent(ACTION_PREVIOUS)
        )

        // 打开主界面
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(player.isPlaying)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    /**
     * 创建 PendingIntent
     */
    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "音乐播放控制"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 处理服务命令
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 切换播放/暂停
     */
    private fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                if (requestAudioFocus()) {
                    it.play()
                }
            }
        }
    }

    /**
     * 播放下一首
     */
    private fun playNext() {
        exoPlayer?.seekToNext()
    }

    /**
     * 播放上一首
     */
    private fun playPrevious() {
        exoPlayer?.seekToPrevious()
    }

    /**
     * 播放器事件监听
     */
    private inner class PlayerEventListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            RythmeLogger.d(TAG, "onIsPlayingChanged: isPlaying=$isPlaying")
            updateNotification()
            // 更新前台服务状态
            if (isPlaying) {
                if (requestAudioFocus()) {
                    startForeground(NOTIFICATION_ID, buildNotification(exoPlayer!!))
                }
            } else {
                // 暂停时停止前台服务（但保持服务运行）
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            RythmeLogger.d(TAG, "onMediaItemTransition: title=${mediaItem?.mediaMetadata?.title}")
            updateNotification()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateStr = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN($playbackState)"
            }
            RythmeLogger.d(TAG, "onPlaybackStateChanged: state=$stateStr")
            
            // 播放结束时检查是否需要停止服务
            if (playbackState == Player.STATE_ENDED) {
                if (exoPlayer?.repeatMode == Player.REPEAT_MODE_OFF) {
                    abandonAudioFocus()
                }
            }
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            RythmeLogger.e(TAG, "onPlayerError: ${error.message}", error)
        }
    }

    /**
     * 媒体会话回调
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        /**
         * 处理播放恢复请求
         * 当从通知栏或媒体按钮恢复播放时调用
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // 返回当前播放队列和位置
            val player = mediaSession.player
            val mediaItems = mutableListOf<MediaItem>()
            
            for (i in 0 until player.mediaItemCount) {
                player.getMediaItemAt(i)?.let { mediaItems.add(it) }
            }
            
            val startPosition = player.currentPosition
            val startIndex = player.currentMediaItemIndex
            
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPosition)
            )
        }
    }

    /**
     * 服务销毁
     */
    override fun onDestroy() {
        abandonAudioFocus()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MusicPlaybackService"
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_previous"
    }
}
