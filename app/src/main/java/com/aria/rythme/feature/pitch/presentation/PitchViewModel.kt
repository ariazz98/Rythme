package com.aria.rythme.feature.pitch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.feature.pitch.data.DetectedPitch
import com.aria.rythme.feature.pitch.data.MicrophonePitchSource
import com.aria.rythme.feature.pitch.data.PitchFrame
import com.aria.rythme.feature.pitch.data.PitchHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.aria.rythme.feature.pitch.data.SongMelodyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PitchPhase { Idle, Recording, Paused, Finished }

data class PitchState(
    val phase: PitchPhase = PitchPhase.Idle,
    val stopping: Boolean = false,
    val preparing: Boolean = false,
    val reading: DetectedPitch? = null,
    val frames: PitchHistory = PitchHistory.Empty,
    val durationMs: Long = 0,
    val segmentStartMs: Long = 0,
    val sessionId: Long = 0,
    val message: String? = null
) {
    val listening: Boolean get() = phase == PitchPhase.Recording
}

class PitchViewModel(private val source: MicrophonePitchSource, private val playback: PlaybackController) : ViewModel() {
    private val _state = MutableStateFlow(PitchState())
    val state = _state.asStateFlow()
    private var capture: Job? = null
    private var resetting: Job? = null

    private val _reference = MutableStateFlow<SongMelodyState?>(null)
    val reference = _reference.asStateFlow()
    private var monitor: Job? = null
    private var tail = false
    private var tailSourceMs: Long? = null

    init {
        viewModelScope.launch {
            playback.isPlaying.collect { playing ->
                if (playing && _reference.value == null && _state.value.listening)
                    pause("音乐已开始播放，录制已暂停")

            }
        }
    }

    fun attachReference(reference: SongMelodyState?) {
        if (_state.value.phase != PitchPhase.Idle || _state.value.preparing) return
        _reference.value = reference
        _state.update { it.copy(durationMs = 0, sessionId = it.sessionId + 1) }
    }

    fun selectStart(positionMs: Long) {
        if (_state.value.phase == PitchPhase.Idle) _state.update {
            it.copy(durationMs = positionMs.coerceIn(0, (_reference.value?.curve?.endMs ?: 1).minus(1).coerceAtLeast(0)))
        }
    }

    /** 仅开始空记录或继续暂停记录，结束态绝不隐式清空。 */
    fun start() {
        val old = _state.value
        if (capture != null || resetting?.isActive == true || old.preparing || old.stopping) return
        if (_reference.value != null && old.phase !in listOf(PitchPhase.Idle, PitchPhase.Paused)) return
        val reference = _reference.value
        val offset = old.durationMs
        val history = if (old.frames.isNotEmpty() && old.frames.last().timeMs < offset) old.frames.append(PitchFrame(offset, null)) else old.frames
        _state.value = old.copy(preparing = true, message = null)
        tail = false; tailSourceMs = null
        capture = viewModelScope.launch {
            try {
                if (reference != null) playback.preparePractice(reference.song!!.id, offset) else playback.pause()
                // MediaController 会先估计、再确认同一次 seek；开始采集后才建立监测基线。
                var revision: Long? = null
                val endRevision = playback.practiceEnds.value
                _state.update { it.copy(preparing = false, phase = PitchPhase.Recording, frames = history, segmentStartMs = offset) }
                if (reference != null) {
                    monitor = viewModelScope.launch {
                        while (_state.value.listening) {
                            delay(50)
                            if (revision == null && playback.practiceEnds.value == endRevision) continue
                            when (practicePlaybackBoundary(
                                sameSong = playback.queue.value.currentEntry?.song?.id == reference.song!!.id,
                                naturallyEnded = playback.practiceEnds.value != endRevision,
                                samePositionRevision = playback.practiceSeeks.value == revision,
                                playing = playback.isPlaying.value
                            )) {
                                PracticePlaybackBoundary.Tail -> { tail = true; delay(1_000); stop(); break }
                                PracticePlaybackBoundary.SongChanged -> pause("已切换歌曲，本次录制已暂停；继续时将恢复原歌曲和录制位置")
                                PracticePlaybackBoundary.PositionChanged -> pause("播放位置已改变，本次录制已暂停；继续时将恢复录制位置")
                                PracticePlaybackBoundary.Paused -> pause()
                                PracticePlaybackBoundary.Continue -> Unit
                            }
                        }
                    }
                }
                source.frames(onReady = {
                    withContext(Dispatchers.Main.immediate) {
                        if (reference != null && _state.value.listening) playback.play()
                    }
                }).collect { frame ->
                    if (reference != null && !tail && !playback.isPlaying.value) {
                        check(frame.capturedUntilMs < 10_000 || revision != null) { "歌曲未能开始播放，请重试" }
                        return@collect
                    }
                    if (reference != null && revision == null) revision = playback.practiceSeeks.value
                    val capturedUntil = if (reference == null) offset + frame.capturedUntilMs else if (tail) {
                        val origin = tailSourceMs ?: frame.capturedUntilMs.also { tailSourceMs = it }
                        reference.curve!!.endMs + frame.capturedUntilMs - origin
                    } else playback.getCurrentPosition()
                    _state.update { current ->
                        if (!current.listening) current else {
                            val until = maxOf(current.durationMs, capturedUntil)
                            val center = (until - (frame.capturedUntilMs - frame.timeMs))
                                .coerceAtLeast(current.frames.lastOrNull()?.timeMs?.plus(1) ?: offset)
                            current.copy(reading = frame.pitch,
                                frames = current.frames.append(frame.copy(timeMs = center, capturedUntilMs = until)), durationMs = until)
                        }
                    }

                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (reference != null) { playback.pause(); playback.releasePractice() }
                _state.update { it.copy(phase = if (it.frames.isEmpty()) PitchPhase.Idle else PitchPhase.Paused,
                    message = error.message ?: "暂时无法读取麦克风") }
            } finally {
                monitor?.cancel(); monitor = null
                capture = null
                _state.update { it.copy(preparing = false, stopping = false, reading = null) }
            }
        }
    }

    fun pause(message: String? = null) {
        if (!_state.value.listening) return
        endCapture(PitchPhase.Paused, message)
    }
    fun stop() {
        if (_state.value.phase == PitchPhase.Idle && !_state.value.preparing) return
        endCapture(PitchPhase.Finished)
    }
    private fun endCapture(phase: PitchPhase, message: String? = null) {
        monitor?.cancel(); monitor = null
        if (com.aria.rythme.BuildConfig.DEBUG) android.util.Log.d("PitchPractice", "phase=$phase position=${_state.value.durationMs} tail=$tail")
        _state.update { it.copy(phase = phase, stopping = capture != null, reading = null, message = message ?: it.message) }
        if (_reference.value != null) {
            if (playback.queue.value.currentEntry?.song?.id == _reference.value?.song?.id) playback.pause()
            playback.releasePractice()
        }
        capture?.cancel()
        source.stop()
    }
    // 离开页面只暂停，返回后可继续观察同一段自由轨迹。
    fun onInactive() {
        if (_state.value.preparing || _state.value.listening) endCapture(PitchPhase.Paused)
    }
    fun dismissMessage() { _state.update { it.copy(message = null) } }
    fun permissionDenied() { _state.update { it.copy(message = "需要麦克风权限才能显示实时音高。自由模式不保存录音。") } }
    fun clearHistory() {
        if (resetting?.isActive == true || _state.value.stopping || _state.value.preparing) return
        if (_reference.value != null && (_state.value.listening || capture != null)) return
        resetting = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            val previousCapture = capture
            if (previousCapture != null) endCapture(PitchPhase.Paused)
            // 等待读取彻底退出，避免旧采样写入清空后的画布；期间 start() 不会开启新采集。
            previousCapture?.join()
            _state.value = PitchState(sessionId = _state.value.sessionId + 1)
        }
    }
    override fun onCleared() {
        source.stop()
        if (_reference.value != null) {
            if (_state.value.listening && playback.queue.value.currentEntry?.song?.id == _reference.value?.song?.id) playback.pause()
            playback.releasePractice()
        }
    }
    companion object { const val HISTORY_MS = 10_000L }
}
