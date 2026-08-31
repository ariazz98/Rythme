package com.aria.rythme.feature.composerlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ComposerListState(
    val composers: List<String> = emptyList(),
    val isLoading: Boolean = true
) : UiState

class ComposerListViewModel(musicRepository: MusicRepository) : ViewModel() {
    val state = musicRepository.getAllComposers()
        .map { composers -> ComposerListState(composers = composers, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComposerListState())
}
