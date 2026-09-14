package com.aria.rythme.feature.pitch.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.feature.pitch.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class SongPracticeState(
    val entry: ParsedMelodyEntry? = null,
    val loading: Boolean = false,
    val reference: PitchHistory = PitchHistory.Empty,
    val takes: PracticeTakes = PracticeTakes(),
    val reading: DetectedPitch? = null,
    val positionMs: Long = 0,
    val playing: Boolean = false,
    val scrubbing: Boolean = false,
    val showOlder: Boolean = true,
    val message: String? = null
)

@androidx.annotation.OptIn(UnstableApi::class)
class SongPracticeViewModel(
    private val context: Context,
    private val normalPlayback: PlaybackController,
    val library: ParsedMelodyStore,
    val analysis: SongMelodyManager,
    private val music: MusicRepository
) : ViewModel() {
    internal val state = MutableStateFlow(SongPracticeState())
    val initializing = MutableStateFlow(false)
    val songs = music.getAllSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val source = MicrophonePitchSource(context)
    private val captureMutex = Mutex()
    private var capture: Job? = null
    private var generation = 0L
    private var foreground = true
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var opening: Job? = null
    private var ticker: Job? = null
    private var clock = ReferencePlaybackClock()
    private var revision = 0L
    private var scrub: Pair<Long, Boolean>? = null

    init {
        viewModelScope.launch {
            normalPlayback.isPlaying.collect { if (it && player?.isPlaying == true) pause() }
        }
    }
    fun refreshLibrary() = viewModelScope.launch {
        initializing.value = true
        try { library.refresh(); analysis.importLegacy(music.getAllSongsOnce()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { message(error.message ?: "无法读取已解析歌曲") }
        finally { initializing.value = false }
    }
    fun open(entry: ParsedMelodyEntry) {
        closeSong()
        state.value = SongPracticeState(entry = entry, loading = true)
        opening = viewModelScope.launch {
            try {
                val song = music.getSongById(entry.songId)
                check(song != null && song.uri.toString() == entry.uri) { "原歌曲已不可用，请重新选择歌曲解析" }
                check(song.size == entry.sourceSize && song.dateModified == entry.modified) { "歌曲文件已变化，请重新解析" }
                val curve = checkNotNull(library.read(entry)) { "解析文件已不可用，请重新解析" }
                withContext(Dispatchers.IO) { context.contentResolver.openAssetFileDescriptor(entry.uri.toUri(), "r")?.use { }
                    ?: error("无法读取原歌曲，请检查媒体权限或文件是否存在") }
                val history = withContext(Dispatchers.Default) { PitchHistory.fromFrames(curve.hz.indices.map { i ->
                    PitchFrame(i * 10L, if (curve.confidence[i] >= MelodyCurve.DISPLAY_THRESHOLD && curve.hz[i] > 0)
                        DetectedPitch(curve.hz[i], curve.confidence[i]) else null)
                }) }
                normalPlayback.pause()
                val audio = ExoPlayer.Builder(context).setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true
                ).setHandleAudioBecomingNoisy(true).build()
                player = audio
                session = MediaSession.Builder(context, audio).setId("pitch-practice").build()
                audio.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && !foreground) { audio.pause(); return }
                        if (isPlaying && !microphoneAllowed()) { audio.pause(); message("需要麦克风权限才能实时跟唱"); return }
                        state.update { it.copy(playing = isPlaying, reading = null) }
                        syncCapture()
                    }
                    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                        revision += 1
                        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                            state.update { it.copy(takes = it.takes.beginSegment(), reading = null) }
                            if (com.aria.rythme.BuildConfig.DEBUG) android.util.Log.d("SongPractice",
                                "seek=${newPosition.positionMs} olderFrames=${state.value.takes.older.map { it.size }}")
                            syncCapture()
                        }
                        state.update { it.copy(positionMs = newPosition.positionMs.coerceAtLeast(0)) }
                    }
                    override fun onPlayerError(error: PlaybackException) { pause(); message(error.message ?: "无法播放歌曲") }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) { state.update { it.copy(positionMs = entry.durationMs) }; syncCapture() }
                    }
                })
                state.value = SongPracticeState(entry = entry, reference = history)
                audio.setMediaItem(MediaItem.Builder().setUri(entry.uri).setMediaId(entry.key)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(entry.title).setArtist(entry.artist)
                        .setArtworkUri(entry.coverUri.takeIf { it.isNotBlank() }?.toUri()).build()).build())
                audio.repeatMode = Player.REPEAT_MODE_OFF
                audio.prepare()
                ticker = viewModelScope.launch { while (isActive) {
                    if (!state.value.scrubbing) state.update { it.copy(positionMs = displayPosition()) }
                    delay(50)
                } }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { closeSong(); message(error.message ?: "无法打开歌曲轨迹") }
        }
    }
    private fun microphoneAllowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun syncCapture() {
        val token = ++generation
        capture?.cancel(); source.stop()
        val audio = player ?: return
        if (!audio.isPlaying || state.value.scrubbing) return
        capture = viewModelScope.launch {
            captureMutex.withLock {
                if (token != generation) return@withLock
                try {
                    val captureClock = PracticeCaptureClock()
                    var diagnosticUntilMs = 0L
                    source.frames().collect { frame ->
                        if (token != generation || !audio.isPlaying || state.value.scrubbing) return@collect
                        val position = displayPosition()
                        val mapped = captureClock.map(frame, position)
                        state.update { it.copy(reading = frame.pitch, takes = it.takes.append(mapped)) }
                        if (com.aria.rythme.BuildConfig.DEBUG && frame.capturedUntilMs >= diagnosticUntilMs) {
                            diagnosticUntilMs = frame.capturedUntilMs + 1000
                            val recent = state.value.takes.current.window(position - 2000, position + 256)
                            val voiced = recent.count { it.pitch != null }
                            val pairs = recent.zipWithNext().count { (a, b) -> a.pitch != null && b.pitch != null && b.timeMs - a.timeMs <= 96 }
                            android.util.Log.d("SongPractice", "capture position=$position mapped=${mapped.timeMs} stored=${state.value.takes.current.size} recent=${recent.size} voiced=$voiced pairs=$pairs")
                        }
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { pause(); message(error.message ?: "麦克风输入已中断") }
            }
        }
    }
    internal fun displayPosition(): Long {
        if (state.value.scrubbing) return state.value.positionMs
        val audio = player ?: return state.value.positionMs
        if (audio.playbackState == Player.STATE_ENDED) return state.value.entry?.durationMs ?: 0
        return clock.sample(audio.currentPosition, revision, audio.isPlaying).coerceIn(0, state.value.entry?.durationMs ?: 0)
    }
    fun setForeground(active: Boolean) { foreground = active; if (!active) pause() }
    fun toggle() {
        if (!foreground) return
        val audio = player ?: return
        if (audio.isPlaying) pause() else {
            if (!microphoneAllowed()) { message("需要麦克风权限才能实时跟唱"); return }
            normalPlayback.pause()
            if (audio.playbackState == Player.STATE_ENDED || audio.currentPosition >= (state.value.entry?.durationMs ?: 0)) audio.seekTo(0)
            audio.play()
        }
    }
    fun pause() { player?.pause(); generation++; capture?.cancel(); source.stop(); state.update { it.copy(playing = false, reading = null) } }
    fun beginScrub() {
        if (scrub != null || player == null) return
        scrub = displayPosition() to (player?.isPlaying == true)
        state.update { it.copy(scrubbing = true) }
        pause()
    }
    fun previewSeek(position: Long) { state.update { it.copy(positionMs = position.coerceIn(0, it.entry?.durationMs ?: 0)) } }
    fun finishScrub(cancelled: Boolean = false) {
        val before = scrub ?: return
        val target = if (cancelled) before.first else state.value.positionMs
        scrub = null
        state.update { it.copy(scrubbing = false, positionMs = target) }
        if (!cancelled) player?.seekTo(target)
        if (before.second && target < (state.value.entry?.durationMs ?: 0)) player?.play()
    }
    fun seek(position: Long) { beginScrub(); previewSeek(position); finishScrub() }
    fun showOlder(show: Boolean) { state.update { it.copy(showOlder = show) } }
    fun message(message: String?) { state.update { it.copy(message = message) } }
    fun remove(entry: ParsedMelodyEntry) = viewModelScope.launch {
        try { library.remove(entry) } catch (error: Exception) { message(error.message ?: "无法移除解析") }
    }
    fun closeSong() {
        opening?.cancel(); opening = null
        ticker?.cancel(); ticker = null
        pause(); session?.release(); session = null
        player?.release(); player = null
        clock = ReferencePlaybackClock(); scrub = null; revision = 0
        state.value = SongPracticeState()
    }
    override fun onCleared() { closeSong() }
}
