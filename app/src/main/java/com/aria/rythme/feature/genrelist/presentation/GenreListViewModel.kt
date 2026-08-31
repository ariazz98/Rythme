package com.aria.rythme.feature.genrelist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GenreListState(
    val genres: List<String> = emptyList(),
    val isLoading: Boolean = true
) : UiState

class GenreListViewModel(musicRepository: MusicRepository) : ViewModel() {
    val state = musicRepository.getAllGenres()
        .map { genres -> GenreListState(genres = genres, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GenreListState())
}
