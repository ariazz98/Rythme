package com.aria.rythme.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val isSearching: Boolean = false
) : UiState

/** Search 页自己的查询状态与本地曲库搜索。 */
class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

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
