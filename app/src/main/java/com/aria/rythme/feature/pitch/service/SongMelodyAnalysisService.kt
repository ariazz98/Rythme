package com.aria.rythme.feature.pitch.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aria.rythme.MainActivity
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.feature.pitch.data.SongMelodyManager
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class SongMelodyAnalysisService : Service() {
    private val manager: SongMelodyManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var task: Job? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "歌曲参考旋律", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == CANCEL) { task?.cancel(); if (task == null) { manager.cancelled(); stopSelf() }; return START_NOT_STICKY }
        if (task != null) return START_NOT_STICKY
        try {
            startForeground(ID, notification(), if (Build.VERSION.SDK_INT >= 35) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (error: Exception) { manager.failed("无法启动后台分析，请重试"); stopSelf(); return START_NOT_STICKY }
        task = scope.launch {
            val updates = launch { manager.state.collect { getSystemService(NotificationManager::class.java).notify(ID, notification()); delay(500) } }
            try { manager.run() }
            finally {
                updates.cancel(); stopForeground(STOP_FOREGROUND_REMOVE)
                val state = manager.state.value
                if (state.curve != null || state.error != null) getSystemService(NotificationManager::class.java).notify(ID,
                    NotificationCompat.Builder(this@SongMelodyAnalysisService, CHANNEL).setSmallIcon(R.drawable.ic_music)
                        .setContentTitle(if (state.curve != null) "参考旋律已准备好" else "参考旋律分析未完成")
                        .setContentText(state.error ?: state.song?.title).setContentIntent(openIntent()).setAutoCancel(true).build())
                manager.cancelled(); stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    private fun openIntent() = PendingIntent.getActivity(this, ID, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun notification(): Notification {
        val state = manager.state.value
        val cancel = PendingIntent.getService(this, ID, Intent(this, javaClass).setAction(CANCEL), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_music)
            .setContentTitle(state.song?.title ?: "参考旋律")
            .setContentText(state.stage).setContentIntent(openIntent()).setOngoing(true).setOnlyAlertOnce(true)
            .setProgress(1000, (state.progress * 1000).toInt(), false).addAction(0, "取消分析", cancel).build()
    }
    override fun onTimeout(startId: Int, fgsType: Int) { task?.cancel(); manager.cancelled(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { scope.cancel(); manager.cancelled(); super.onDestroy() }
    companion object {
        private const val CHANNEL = "song-melody"
        private const val ID = 3402
        private const val CANCEL = "com.aria.rythme.pitch.CANCEL_ANALYSIS"
        fun start(context: Context, manager: SongMelodyManager, song: Song) {
            if (!manager.prepare(song)) return
            try { ContextCompat.startForegroundService(context, Intent(context, SongMelodyAnalysisService::class.java)) }
            catch (error: Exception) { manager.failed("无法启动分析，请重试") }
        }
        fun cancel(context: Context) { context.startService(Intent(context, SongMelodyAnalysisService::class.java).setAction(CANCEL)) }
    }
}
