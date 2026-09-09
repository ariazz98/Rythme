package com.aria.rythme.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val isSearching: Boolean = false
)

/** Search 页自己的查询状态与本地曲库搜索。 */
class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()
    // 浏览卡片只读取现有曲库分类；本阶段不增加分类点击业务或搜索历史写入。
    val browseGenres = musicRepository.getAllGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        searchJob?.cancel()
        val normalizedQuery = query.trim()
        _state.update {
            it.copy(
                query = query,
                songs = emptyList(),
                isSearching = normalizedQuery.isNotEmpty()
            )
        }

        if (normalizedQuery.isEmpty()) return

        searchJob = viewModelScope.launch {
            delay(150)
            musicRepository.searchSongs(normalizedQuery).collectLatest { songs ->
                if (_state.value.query.trim() == normalizedQuery) {
                    _state.update { it.copy(songs = songs, isSearching = false) }
                }
            }
        }
    }

    fun play(song: Song) {
        viewModelScope.launch {
            playbackController.play(song, _state.value.songs)
        }
    }
}
