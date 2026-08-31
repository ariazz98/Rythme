package com.aria.rythme.feature.genredetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GenreDetailState(val albums: List<Album> = emptyList()) : UiState

class GenreDetailViewModel(
    genreName: String,
    musicRepository: MusicRepository
) : ViewModel() {
    val state = musicRepository.getAlbumsContainingGenre(genreName)
        .map(::GenreDetailState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GenreDetailState())
}
