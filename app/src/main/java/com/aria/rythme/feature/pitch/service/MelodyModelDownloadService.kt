package com.aria.rythme.feature.pitch.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aria.rythme.MainActivity
import com.aria.rythme.R
import com.aria.rythme.feature.pitch.data.MelodyModelManager
import com.aria.rythme.feature.pitch.data.ModelPhase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject

/** 只由用户点击启动；普通切页/退后台继续，暂停或系统超时保留分片，不自动重启下载。 */
class MelodyModelDownloadService : Service() {
    private val manager: MelodyModelManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var transfer: Job? = null
    private var latestStartId = 0
    private var notifications: Job? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "歌曲对照模型", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        if (intent?.action == PAUSE) {
            manager.interrupt()
            transfer?.cancel()
            if (transfer == null) { manager.interrupted(); stopSelf() }
            return START_NOT_STICKY
        }
        if (transfer != null) return START_NOT_STICKY
        try {
            startForeground(ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (error: Exception) {
            manager.failed(error.message ?: "暂时无法启动后台下载")
            stopSelf(); return START_NOT_STICKY
        }
        notifications = scope.launch {
            manager.state.collect {
                getSystemService(NotificationManager::class.java).notify(ID, notification())
                delay(500)
            }
        }
        transfer = scope.launch {
            try {
                val source = intent?.getStringExtra(IMPORT_URI)
                if (source == null) manager.download(intent?.getBooleanExtra(METERED, false) == true)
                else manager.importFile(source.toUri())
            }
            finally {
                notifications?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (manager.state.value.phase in listOf(ModelPhase.Ready, ModelPhase.Failed)) {
                    val ready = manager.state.value.phase == ModelPhase.Ready
                    val done = NotificationCompat.Builder(this@MelodyModelDownloadService, CHANNEL)
                        .setSmallIcon(R.drawable.ic_music)
                        .setContentTitle(if (ready) "歌曲对照模型已就绪" else "歌曲对照模型准备未完成")
                        .setContentText(if (ready) "模型文件已通过完整性校验" else manager.state.value.message)
                        .setContentIntent(PendingIntent.getActivity(this@MelodyModelDownloadService, ID,
                            Intent(this@MelodyModelDownloadService, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                        .setAutoCancel(true).build()
                    getSystemService(NotificationManager::class.java).notify(ID, done)
                }
                manager.interrupted()
                transfer = null
                stopSelf(latestStartId)
            }
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val state = manager.state.value
        val open = PendingIntent.getActivity(this, ID, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pause = PendingIntent.getService(this, ID, Intent(this, javaClass).setAction(PAUSE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(if (state.phase == ModelPhase.Verifying) "正在校验歌曲对照模型" else if (state.phase == ModelPhase.Importing) "正在导入歌曲对照模型" else "正在下载歌曲对照模型")
            .setContentText("${state.bytes / 1024 / 1024} / ${manager.totalBytes / 1024 / 1024} MiB · 可在后台继续")
            .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true)
            .setProgress(1000, (state.bytes * 1000 / manager.totalBytes).toInt(), state.phase == ModelPhase.Verifying)
            .addAction(0, if (state.importing) "取消导入" else "暂停", pause).build()
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        manager.interrupt(); transfer?.cancel(); manager.interrupted()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() {
        manager.interrupt(); scope.cancel(); manager.interrupted()
        super.onDestroy()
    }
    companion object {
        private const val CHANNEL = "melody-model"
        private const val ID = 3401
        private const val PAUSE = "com.aria.rythme.pitch.PAUSE_MODEL"
        private const val METERED = "metered"
        private const val IMPORT_URI = "import_uri"
        fun start(context: Context, manager: MelodyModelManager, allowMetered: Boolean) {
            if (manager.state.value.transferring) return
            manager.preparing()
            try { ContextCompat.startForegroundService(context, Intent(context, MelodyModelDownloadService::class.java).putExtra(METERED, allowMetered)) }
            catch (error: Exception) { manager.failed(error.message ?: "无法启动下载，请重试") }
        }
        fun importFile(context: Context, manager: MelodyModelManager, uri: android.net.Uri) {
            if (manager.state.value.transferring) return
            manager.preparing()
            try { ContextCompat.startForegroundService(context, Intent(context, MelodyModelDownloadService::class.java).putExtra(IMPORT_URI, uri.toString())) }
            catch (error: Exception) { manager.failed(error.message ?: "无法导入模型，请重试") }
        }
        fun pause(context: Context) { context.startService(Intent(context, MelodyModelDownloadService::class.java).setAction(PAUSE)) }
    }
}
