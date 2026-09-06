package com.aria.rythme.feature.artistlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ArtistListState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true
)

class ArtistListViewModel(musicRepository: MusicRepository) : ViewModel() {
    val state = musicRepository.getAllArtists()
        .map { artists -> ArtistListState(artists = artists, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtistListState())
}
