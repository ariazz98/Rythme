package com.aria.rythme.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFacet {
    GENRE,
    COMPOSER
}

class LibraryFacetListViewModel(
    facet: LibraryFacet,
    musicRepository: MusicRepository
) : ViewModel() {
    val items = facet.names(musicRepository)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class LibraryFacetDetailState(
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList()
)

class LibraryFacetDetailViewModel(
    facet: LibraryFacet,
    value: String,
    musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {
    val state = combine(
        facet.albums(value, musicRepository),
        facet.songs(value, musicRepository),
        ::LibraryFacetDetailState
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibraryFacetDetailState()
    )

    fun playAll(shuffle: Boolean = false) {
        val songs = state.value.songs
        if (songs.isEmpty()) return

        viewModelScope.launch {
            val queue = if (shuffle) songs.shuffled() else songs
            playbackController.play(queue.first(), queue)
        }
    }
}

private fun LibraryFacet.names(repository: MusicRepository): Flow<List<String>> = when (this) {
    LibraryFacet.GENRE -> repository.getAllGenres()
    LibraryFacet.COMPOSER -> repository.getAllComposers()
}

private fun LibraryFacet.albums(
    value: String,
    repository: MusicRepository
): Flow<List<Album>> = when (this) {
    LibraryFacet.GENRE -> repository.getAlbumsContainingGenre(value)
    LibraryFacet.COMPOSER -> repository.getAlbumsContainingComposer(value)
}

private fun LibraryFacet.songs(
    value: String,
    repository: MusicRepository
): Flow<List<Song>> = when (this) {
    LibraryFacet.GENRE -> repository.getSongsByGenre(value)
    LibraryFacet.COMPOSER -> repository.getSongsByComposer(value)
}
