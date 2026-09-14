package com.aria.rythme.feature.pitch.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class PitchFrame(val timeMs: Long, val pitch: DetectedPitch?, val capturedUntilMs: Long = timeMs)

/** 原始音频仅在内存中用于检测，不保存录音。每个收集者拥有自己的 AudioRecord。 */
class MicrophonePitchSource(private val context: Context) {
    private val recorderLock = Any()
    private var active: AudioRecord? = null

    fun stop() = synchronized(recorderLock) {
        active?.let { recorder ->
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) runCatching { recorder.stop() }
        }
    }

    fun frames(onReady: suspend () -> Unit = {}) = flow {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { "尚未获得麦克风权限" }
        val minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "当前设备不支持此音频采集配置" }
        val audioManager = context.getSystemService(AudioManager::class.java)
        val source = if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true")
            MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        val recorder = AudioRecord.Builder().setAudioSource(source)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(minimum, 8192)).build()
        try {
            check(recorder.state == AudioRecord.STATE_INITIALIZED) { "麦克风初始化失败" }
            synchronized(recorderLock) {
                check(active == null) { "麦克风正在使用中" }
                active = recorder
                recorder.startRecording()
            }
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "无法开始监听，请检查麦克风是否被其他应用占用" }
            onReady()
            val detector = PitchDetector()
            val hop = ShortArray(512)
            val frame = FloatArray(2048)
            var totalSamples = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = recorder.read(hop, 0, hop.size, AudioRecord.READ_BLOCKING)
                currentCoroutineContext().ensureActive()
                check(count > 0) { "麦克风输入已中断，请重试" }
                frame.copyInto(frame, 0, count, frame.size)
                for (i in 0 until count) frame[frame.size - count + i] = hop[i] / 32768f
                totalSamples += count
                if (totalSamples >= frame.size) {
                    val centerTime = (totalSamples - frame.size / 2) * 1000 / SAMPLE_RATE
                    emit(PitchFrame(centerTime, detector.detect(frame), totalSamples * 1000 / SAMPLE_RATE))
                }
            }
        } finally {
            synchronized(recorderLock) {
                if (active === recorder) active = null
                runCatching { recorder.stop() }
                recorder.release()
            }
        }
    }.flowOn(Dispatchers.IO)

    companion object { const val SAMPLE_RATE = 16_000 }
}
