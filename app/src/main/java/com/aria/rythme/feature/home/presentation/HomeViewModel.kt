package com.aria.rythme.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Playlist
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val recent: List<Song> = emptyList(),
    val added: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val resume: ResumePoint? = null,
    val resumeSong: Song? = null,
    val resumeTitle: String? = null,
    val resumeOrigin: ListeningOrigin? = null,
    val error: String? = null
)

/** 保留重复入队的位置；曲库删除条目后，当前项仍映射回同一次出现。 */
internal fun restoredQueueIndex(ids: List<Long>, originalIndex: Int, available: Set<Long>): Int? {
    if (ids.getOrNull(originalIndex) !in available) return null
    return ids.take(originalIndex).count { it in available }
}

class HomeViewModel(
    private val music: MusicRepository,
    private val playlists: PlaylistRepository,
    private val history: ListeningHistoryRepository,
    private val playback: PlaybackController
) : ViewModel() {
    private val _operationError = MutableStateFlow<String?>(null)
    val operationError = _operationError.asStateFlow()

    val state = combine(music.getAllSongs(), music.getAllAlbums(), playlists.getAllPlaylists(), history.history) { songs, albums, lists, listening ->
        val byId = songs.associateBy { it.id }
        val addedAt = songs.groupBy { it.albumId }.mapValues { (_, tracks) -> tracks.maxOf { it.dateAdded } }
        val resume = listening.resume?.takeIf { it.songId in byId }
        val current = resume?.let { byId[it.songId] }
        val validOrigin = resume?.origin?.takeIf { origin ->
            when (origin.kind) {
                "album" -> albums.any { it.id == origin.id }
                "playlist" -> lists.any { it.id == origin.id }
                else -> false
            }
        }
        HomeState(
            loading = false,
            recent = listening.recent.mapNotNull { byId[it.songId] },
            added = albums.filter { it.id in addedAt }.sortedWith(compareByDescending<Album> { addedAt[it.id] }.thenBy { it.title }),
            playlists = lists,
            resume = resume,
            resumeSong = current,
            resumeOrigin = validOrigin,
            resumeTitle = when (resume?.origin?.kind) {
                "album" -> albums.firstOrNull { it.id == resume.origin.id }?.title
                "playlist" -> lists.firstOrNull { it.id == resume.origin.id }?.name
                else -> null
            } ?: current?.title
        )
    }.catch { emit(HomeState(loading = false, error = "暂时无法读取本地音乐，请稍后重试")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun play(song: Song) = runPlayback { playback.play(song) }

    fun playAlbum(album: Album) = runPlayback {
        val songs = music.getSongsByAlbum(album.id).first()
        val first = songs.firstOrNull() ?: error("这张专辑已没有可播放的歌曲")
        playback.play(first, songs, ListeningOrigin("album", album.id))
    }

    fun resume() = runPlayback {
        // 点击时重新读取，不能用页面首次出现时捕获的播放位置覆盖后台新位置。
        val point = history.history.first().resume ?: error("暂时没有可继续的收听记录")
        val available = music.getAllSongsOnce().associateBy { it.id }
        val index = restoredQueueIndex(point.queueIds, point.queueIndex, available.keys)
            ?: error("上次播放的歌曲已不在曲库中")
        val queue = point.queueIds.mapNotNull(available::get)
        val song = queue[index]
        playback.play(song, queue, point.origin, point.positionMs.coerceAtMost(song.duration.coerceAtLeast(0)), index)
    }

    private fun runPlayback(action: suspend () -> Unit) {
        viewModelScope.launch {
            _operationError.value = null
            try { action() }
            catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (error: Exception) { _operationError.value = error.message ?: "暂时无法播放" }
        }
    }
}
