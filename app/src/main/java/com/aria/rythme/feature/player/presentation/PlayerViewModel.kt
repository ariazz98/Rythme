package com.aria.rythme.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.LyricLine
import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsStatus
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.LyricsRepository
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Adapts playback-domain state for Player UI and exposes only operations the UI actually uses.
 * PlaybackController remains the owner of playback and queue behavior.
 */
class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val musicRepository: MusicRepository,
    private val lyricsRepository: LyricsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val currentState: PlayerState
        get() = _state.value

    private var progressUpdateJob: Job? = null
    private var lyricsLoadJob: Job? = null
    private var favoriteObservationJob: Job? = null
    private var lastLyricsSongId: Long? = null
    private var favoriteSongId: Long? = null

    init {
        observePlaybackState()
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            val currentSong = currentState.currentSong
            if (
                currentSong != null &&
                !currentState.isPlaying &&
                playbackController.queue.value.currentEntry == null
            ) {
                playbackController.play(
                    currentSong,
                    currentState.queue.entries.map { it.song }
                )
            } else {
                playbackController.togglePlayPause()
            }
        }
    }

    fun next() {
        playbackController.next()
    }

    fun previous() {
        playbackController.previous()
    }

    fun seekTo(position: Long) {
        playbackController.seekTo(position)
        updateState { it.copy(currentPosition = position) }
    }

    fun toggleRepeatMode() {
        playbackController.toggleRepeatMode()
        updateState { it.copy(repeatMode = playbackController.repeatMode.value) }
    }

    fun toggleShuffleMode() {
        playbackController.toggleShuffleMode()
    }

    fun toggleCrossfade() {
        playbackController.toggleCrossfade()
    }

    fun toggleCurrentSongFavorite() {
        val songId = currentState.currentSong?.id ?: return
        viewModelScope.launch {
            musicRepository.toggleSongFavorite(songId)
        }
    }

    fun loadAndPlayRandom() {
        RythmeLogger.d(TAG, "随机播放")
        viewModelScope.launch {
            try {
                val songs = musicRepository.getAllSongsOnce()
                if (songs.isEmpty()) {
                    sendMessage("没有找到可播放的歌曲")
                    return@launch
                }

                val randomSong = songs.random()
                RythmeLogger.d(TAG, "随机播放: ${randomSong.title}")
                playbackController.play(randomSong, songs)
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "加载失败", e)
                sendMessage("加载失败: ${e.message}")
            }
        }
    }

    fun selectQueueEntry(entryId: String) {
        viewModelScope.launch {
            playbackController.playQueueEntry(entryId)
        }
    }

    fun setVolume(percentage: Int) {
        playbackController.setVolumePercentage(percentage)
    }

    fun reorderQueue(fromEntryId: String, toEntryId: String) {
        viewModelScope.launch {
            playbackController.moveQueueEntry(fromEntryId, toEntryId)
        }
    }

    fun toggleInfinitePlay() {
        viewModelScope.launch {
            try {
                val allSongs = musicRepository.getAllSongsOnce()
                playbackController.toggleInfinitePlay(allSongs)
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "切换无限播放失败", e)
                sendMessage("操作失败: ${e.message}")
            }
        }
    }

    fun clearHistory() {
        playbackController.clearHistory()
    }

    fun seekToLyricLine(index: Int) {
        val lines = currentState.lyricsData?.lines ?: return
        if (index !in lines.indices) return

        val timeMs = lines[index].startTimeMs
        playbackController.seekTo(timeMs)
        updateState {
            it.copy(currentPosition = timeMs, currentLyricIndex = index)
        }
    }

    private fun observePlaybackState() {
        playbackController.isPlaying
            .onEach { isPlaying ->
                updateState { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressUpdate() else stopProgressUpdate()
            }
            .launchIn(viewModelScope)

        playbackController.queue
            .onEach { queue ->
                updateState { it.copy(queue = queue) }
                val song = queue.currentEntry?.song
                if (song == null) {
                    updateLyrics(null, LyricsStatus.IDLE)
                    lastLyricsSongId = null
                } else if (song.id != lastLyricsSongId) {
                    loadLyrics(song)
                }
                observeFavorite(song?.id)
            }
            .launchIn(viewModelScope)

        playbackController.volume
            .onEach { volume -> updateState { it.copy(volume = volume) } }
            .launchIn(viewModelScope)

        playbackController.playHistory
            .onEach { history -> updateState { it.copy(playHistory = history) } }
            .launchIn(viewModelScope)

        playbackController.shuffleMode
            .onEach { enabled -> updateState { it.copy(isShuffleEnabled = enabled) } }
            .launchIn(viewModelScope)

        playbackController.repeatMode
            .onEach { repeatMode -> updateState { it.copy(repeatMode = repeatMode) } }
            .launchIn(viewModelScope)

        playbackController.isCrossfadeEnabled
            .onEach { enabled -> updateState { it.copy(isCrossfadeEnabled = enabled) } }
            .launchIn(viewModelScope)

        playbackController.isInfinitePlayEnabled
            .onEach { enabled -> updateState { it.copy(isInfinitePlayEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    private fun startProgressUpdate() {
        if (progressUpdateJob?.isActive == true) return

        progressUpdateJob = viewModelScope.launch {
            while (true) {
                val position = playbackController.getCurrentPosition()
                val duration = playbackController.getDuration()
                updateState { it.copy(currentPosition = position, duration = duration) }
                updateCurrentLyricIndex(position)
                delay(PROGRESS_UPDATE_INTERVAL)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun loadLyrics(song: Song) {
        lyricsLoadJob?.cancel()
        lastLyricsSongId = song.id
        updateLyrics(null, LyricsStatus.LOADING)

        lyricsLoadJob = viewModelScope.launch {
            try {
                val data = lyricsRepository.getLyrics(song)
                if (currentState.currentSong?.id == song.id) {
                    updateLyrics(
                        data = data,
                        status = if (data == null) LyricsStatus.NOT_FOUND else LyricsStatus.LOADED
                    )
                }
            } catch (e: Exception) {
                RythmeLogger.e(TAG, "歌词加载失败", e)
                if (currentState.currentSong?.id == song.id) {
                    updateLyrics(null, LyricsStatus.ERROR)
                }
            }
        }
    }

    private fun observeFavorite(songId: Long?) {
        if (songId == favoriteSongId) return

        favoriteSongId = songId
        favoriteObservationJob?.cancel()
        if (songId == null) {
            updateState { it.copy(isCurrentSongFavorite = false) }
            return
        }
        favoriteObservationJob = musicRepository.observeSongFavorite(songId)
            .onEach { isFavorite ->
                updateState { it.copy(isCurrentSongFavorite = isFavorite) }
            }
            .launchIn(viewModelScope)
    }

    private fun updateLyrics(data: LyricsData?, status: LyricsStatus) {
        updateState {
            it.copy(
                lyricsData = data,
                lyricsStatus = status,
                currentLyricIndex = -1
            )
        }
    }

    private fun updateCurrentLyricIndex(positionMs: Long) {
        val lines = currentState.lyricsData?.lines ?: return
        if (lines.isEmpty()) return

        val newIndex = findCurrentLineIndex(lines, positionMs)
        if (newIndex != currentState.currentLyricIndex) {
            updateState { it.copy(currentLyricIndex = newIndex) }
        }
    }

    private fun findCurrentLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var low = 0
        var high = lines.lastIndex
        var result = -1

        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].startTimeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    private inline fun updateState(transform: (PlayerState) -> PlayerState) {
        _state.update(transform)
    }

    private fun sendMessage(message: String) {
        _messages.trySend(message)
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
        lyricsLoadJob?.cancel()
        favoriteObservationJob?.cancel()
    }

    private companion object {
        const val TAG = "PlayerViewModel"
        const val PROGRESS_UPDATE_INTERVAL = 200L
    }
}
