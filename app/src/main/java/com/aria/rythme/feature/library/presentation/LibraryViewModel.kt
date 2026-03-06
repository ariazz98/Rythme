package com.aria.rythme.feature.library.presentation

import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute

class LibraryViewModel(
    private val navigator: Navigator
): BaseViewModel<LibraryIntent, LibraryState, LibraryAction, LibraryEffect>() {
    override fun createInitialState(): LibraryState {
        return LibraryState("")
    }

    override fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.NavToSongList -> {
                navigator.navigate(RythmeRoute.SongList)
            }
            LibraryIntent.NavToAlbumList -> {
                navigator.navigate(RythmeRoute.AlbumList)
            }
            LibraryIntent.NavToArtistList -> {
                navigator.navigate(RythmeRoute.ArtistList)
            }
            LibraryIntent.NavToGenreList -> {
                navigator.navigate(RythmeRoute.GenreList)
            }
            LibraryIntent.NavToComposerList -> {
                navigator.navigate(RythmeRoute.ComposerList)
            }
        }
    }

    override fun reduce(action: LibraryAction): LibraryState {
        return LibraryState("")
    }
}