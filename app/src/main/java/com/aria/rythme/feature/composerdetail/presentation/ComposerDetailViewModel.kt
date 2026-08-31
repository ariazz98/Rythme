package com.aria.rythme.feature.composerdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ComposerDetailState(val albums: List<Album> = emptyList()) : UiState

class ComposerDetailViewModel(
    composerName: String,
    musicRepository: MusicRepository
) : ViewModel() {
    val state = musicRepository.getAlbumsContainingComposer(composerName)
        .map(::ComposerDetailState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComposerDetailState())
}
