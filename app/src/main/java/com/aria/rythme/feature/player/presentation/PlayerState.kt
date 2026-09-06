package com.aria.rythme.feature.player.presentation

import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsStatus
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.domain.model.PlaybackQueue
import com.aria.rythme.core.music.domain.model.RepeatMode
import java.util.Locale

/** UI snapshot derived from playback, queue, history, volume, and lyrics sources. */
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val queue: PlaybackQueue = PlaybackQueue(),
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val volume: Int = 0,
    val playHistory: List<Song> = emptyList(),
    val isCrossfadeEnabled: Boolean = false,
    val isInfinitePlayEnabled: Boolean = false,
    val isCurrentSongFavorite: Boolean = false,
    val lyricsData: LyricsData? = null,
    val lyricsStatus: LyricsStatus = LyricsStatus.IDLE,
    val currentLyricIndex: Int = -1
) {
    val currentSong: Song?
        get() = queue.currentEntry?.song

    val isPlayingInfiniteExtension: Boolean
        get() = queue.currentIndex >= queue.orderedEntryCount && queue.currentEntry != null

    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    /** Each explicit occurrence of the same song has its own shared-element identity. */
    val currentQueueEntryIdentity: String
        get() = queue.currentEntry?.id ?: "empty"

    val currentPositionText: String
        get() = formatDuration(currentPosition)

    val durationText: String
        get() = formatDuration(duration)

    val canPlayPrevious: Boolean
        get() = queue.entries.isNotEmpty() &&
            (queue.currentIndex > 0 || repeatMode == RepeatMode.ALL)

    val canPlayNext: Boolean
        get() = queue.entries.isNotEmpty() &&
            (queue.currentIndex < queue.entries.size - 1 || repeatMode == RepeatMode.ALL)

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
