package com.aria.rythme.core.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Size
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import com.aria.rythme.MainActivity
import com.aria.rythme.R
import com.aria.rythme.core.utils.RythmeLogger
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * 音乐播放服务
 *
 * 前台服务，负责在后台持续播放音乐。
 * 使用自定义 MediaNotification.Provider 确保通知栏显示专辑封面。
 */
@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var defaultArtwork: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        RythmeLogger.d(TAG, "服务创建")
        defaultArtwork = BitmapFactory.decodeResource(resources, R.drawable.ic_notification)
        createNotificationChannel()
        initializePlayer()
        setMediaNotificationProvider(RythmeNotificationProvider())
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "音乐播放控制"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    /**
     * 加载专辑封面 bitmap
     */
    private fun loadArtwork(player: Player): Bitmap? {
        val artworkUri = player.currentMediaItem?.mediaMetadata?.artworkUri ?: return null
        return try {
            contentResolver.loadThumbnail(artworkUri, Size(512, 512), null)
        } catch (e: Exception) {
            RythmeLogger.d(TAG, "加载封面失败: $artworkUri")
            null
        }
    }

    /**
     * 自定义通知提供者
     */
    private inner class RythmeNotificationProvider : MediaNotification.Provider {

        override fun createNotification(
            session: MediaSession,
            customLayout: ImmutableList<CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            onNotificationChangedCallback: MediaNotification.Provider.Callback
        ): MediaNotification {
            val player = session.player
            val metadata = player.currentMediaItem?.mediaMetadata
            val title = metadata?.title?.toString() ?: "未知歌曲"
            val artist = metadata?.artist?.toString() ?: "未知艺术家"

            val artwork = loadArtwork(player) ?: defaultArtwork

            val contentIntent = PendingIntent.getActivity(
                this@MusicPlaybackService,
                0,
                Intent(this@MusicPlaybackService, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this@MusicPlaybackService, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(artwork)
                .setContentIntent(contentIntent)
                .setOngoing(player.isPlaying)
                .setStyle(
                    androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
                )
                .build()

            return MediaNotification(NOTIFICATION_ID, notification)
        }

        override fun handleCustomCommand(
            session: MediaSession,
            action: String,
            extras: Bundle
        ): Boolean = false
    }

    private class PlayerEventListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            RythmeLogger.d(TAG, "onIsPlayingChanged: isPlaying=$isPlaying")
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            RythmeLogger.d(TAG, "onMediaItemTransition: title=${mediaItem?.mediaMetadata?.title}")
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
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            RythmeLogger.e(TAG, "onPlayerError: ${error.message}", error)
        }
    }

    private class MediaSessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val player = mediaSession.player
            val mediaItems = mutableListOf<MediaItem>()
            for (i in 0 until player.mediaItemCount) {
                player.getMediaItemAt(i).let { mediaItems.add(it) }
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, player.currentMediaItemIndex, player.currentPosition)
            )
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        defaultArtwork = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MusicPlaybackService"
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1
    }
}
