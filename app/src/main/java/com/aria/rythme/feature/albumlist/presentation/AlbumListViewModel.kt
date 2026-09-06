package com.aria.rythme.feature.albumlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlbumSortBy {
    TITLE, ARTIST, YEAR, SONG_COUNT, RECENTLY_ADDED
}

enum class AlbumLayoutMode {
    GRID, LIST
}

data class AlbumListState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val sortBy: AlbumSortBy = AlbumSortBy.TITLE,
    val layoutMode: AlbumLayoutMode = AlbumLayoutMode.GRID
)

/** 普通数据页：状态、偏好设置和播放操作直接表达，不再包装空 MVI 层。 */
class AlbumListViewModel(
    private val musicRepository: MusicRepository,
    private val appSettings: AppSettingsRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumListState())
    val state = _state.asStateFlow()

    private var rawAlbums: List<Album> = emptyList()

    init {
        loadSavedPreferences()
        observeAlbums()
    }

    fun setSort(sortBy: AlbumSortBy) {
        _state.update { it.copy(sortBy = sortBy, albums = sortAlbums(rawAlbums, sortBy)) }
        viewModelScope.launch {
            appSettings.setPagePreference(PAGE_KEY, KEY_SORT, sortBy)
        }
    }

    fun setLayout(layoutMode: AlbumLayoutMode) {
        _state.update { it.copy(layoutMode = layoutMode) }
        viewModelScope.launch {
            appSettings.setPagePreference(PAGE_KEY, KEY_LAYOUT, layoutMode)
        }
    }

    fun playAll(shuffle: Boolean = false) {
        val visibleAlbums = _state.value.albums
        if (visibleAlbums.isEmpty()) return

        viewModelScope.launch {
            val songs = musicRepository.getAllSongsOnce()
            val orderedQueue = visibleAlbums.flatMap { album ->
                songs.asSequence()
                    .filter { it.albumId == album.id && it.album == album.title }
                    .sortedWith(trackOrder)
                    .toList()
            }
            val queue = if (shuffle) orderedQueue.shuffled() else orderedQueue
            if (queue.isNotEmpty()) {
                playbackController.play(queue.first(), queue)
            }
        }
    }

    private fun loadSavedPreferences() {
        viewModelScope.launch {
            val savedSort =
                appSettings.getPagePreferenceValue(PAGE_KEY, KEY_SORT, AlbumSortBy.TITLE)
            val savedLayout =
                appSettings.getPagePreferenceValue(PAGE_KEY, KEY_LAYOUT, AlbumLayoutMode.GRID)
            _state.update {
                it.copy(
                    sortBy = savedSort,
                    layoutMode = savedLayout,
                    albums = sortAlbums(rawAlbums, savedSort)
                )
            }
        }
    }

    private fun observeAlbums() {
        musicRepository.getAllAlbums()
            .onEach { albums ->
                rawAlbums = albums
                _state.update {
                    it.copy(
                        albums = sortAlbums(albums, it.sortBy),
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun sortAlbums(albums: List<Album>, sortBy: AlbumSortBy): List<Album> =
        when (sortBy) {
            AlbumSortBy.TITLE -> albums.sortedBy { it.title }
            AlbumSortBy.ARTIST -> albums.sortedBy { it.artist }
            AlbumSortBy.YEAR -> albums.sortedByDescending { it.year }
            AlbumSortBy.SONG_COUNT -> albums.sortedByDescending { it.songCount }
            AlbumSortBy.RECENTLY_ADDED -> albums.sortedByDescending { it.id }
        }

    private companion object {
        const val PAGE_KEY = "album_list"
        const val KEY_SORT = "sort_by"
        const val KEY_LAYOUT = "layout_mode"

        val trackOrder = compareBy<Song>(Song::discNumber, Song::trackNumber, Song::title)
    }
}
