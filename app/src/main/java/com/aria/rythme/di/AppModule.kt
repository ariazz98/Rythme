@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme.di

import com.aria.rythme.feature.home.presentation.HomeViewModel
import com.aria.rythme.feature.library.presentation.LibraryViewModel
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.datasource.MediaStoreSource
import com.aria.rythme.core.music.data.indexer.MusicIndexer
import com.aria.rythme.core.music.data.local.MusicDatabase
import com.aria.rythme.core.music.data.observer.MediaStoreWatcher
import com.aria.rythme.core.music.data.lyrics.EmbeddedLyricsReader
import com.aria.rythme.core.music.data.repository.LyricsRepository
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.music.data.repository.PlaylistRepository
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.feature.playlist.presentation.PlayListViewModel
import com.aria.rythme.feature.playlistdetail.presentation.PlaylistDetailViewModel
import com.aria.rythme.feature.search.presentation.SearchViewModel
import com.aria.rythme.feature.albumdetail.presentation.AlbumDetailViewModel
import com.aria.rythme.feature.albumlist.presentation.AlbumListViewModel
import com.aria.rythme.feature.artistdetail.presentation.ArtistDetailViewModel
import com.aria.rythme.feature.artistlist.presentation.ArtistListViewModel
import com.aria.rythme.feature.composerdetail.presentation.ComposerDetailViewModel
import com.aria.rythme.feature.composerlist.presentation.ComposerListViewModel
import com.aria.rythme.feature.genredetail.presentation.GenreDetailViewModel
import com.aria.rythme.feature.genrelist.presentation.GenreListViewModel
import com.aria.rythme.feature.songlist.presentation.SongListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val playModule = module {
    single { AppSettingsRepository(androidContext()) }
    single { MusicDatabase.getInstance(androidContext()) }
    single { get<MusicDatabase>().songDao() }
    single { get<MusicDatabase>().albumDao() }
    single { get<MusicDatabase>().artistDao() }
    single { get<MusicDatabase>().scanMetadataDao() }
    single { get<MusicDatabase>().songOverrideDao() }
    single { get<MusicDatabase>().playlistDao() }
    single { get<MusicDatabase>().lyricsDao() }
    single { EmbeddedLyricsReader(androidContext()) }
    single { LyricsRepository(get(), get()) }
    single { PlaylistRepository(get(), get(), get()) }
    single { MediaStoreSource(androidContext(), get()) }
    single { MediaStoreWatcher(androidContext(), get(), get()) }
    single { MusicRepository(get(), get(), get(), get()) }
    single { MusicIndexer(androidContext(), get(), get(), get(), get(), get()) }
    single {
        PlaybackController(androidContext()).apply {
            initialize()
        }
    }
}

val playerModule = module {
    viewModel {
        PlayerViewModel(
            playbackController = get(),
            musicRepository = get(),
            lyricsRepository = get()
        )
    }
}

val albumDetailModule = module {
    viewModel { params ->
        AlbumDetailViewModel(
            albumId = params[0],
            navigator = params[1],
            musicRepository = get(),
            playbackController = get(),
            filterArtistId = params[2],
            filterComposer = params[3],
            filterGenre = params[4]
        )
    }
}

val albumListModule = module {
    viewModel { params ->
        AlbumListViewModel(
            navigator = params.get(),
            musicRepository = get(),
            appSettings = get()
        )
    }
}

val artistDetailModule = module {
    viewModel { params ->
        ArtistDetailViewModel(
            artistId = params.get(),
            navigator = params.get(),
            musicRepository = get()
        )
    }
}

val artistListModule = module {
    viewModel { params ->
        ArtistListViewModel(
            navigator = params.get(),
            musicRepository = get()
        )
    }
}

val genreDetailModule = module {
    viewModel { params ->
        GenreDetailViewModel(
            genreName = params[0],
            navigator = params[1],
            musicRepository = get()
        )
    }
}

val genreListModule = module {
    viewModel { params ->
        GenreListViewModel(
            navigator = params.get(),
            musicRepository = get()
        )
    }
}

val composerDetailModule = module {
    viewModel { params ->
        ComposerDetailViewModel(
            composerName = params[0],
            navigator = params[1],
            musicRepository = get()
        )
    }
}

val composerListModule = module {
    viewModel { params ->
        ComposerListViewModel(
            navigator = params.get(),
            musicRepository = get()
        )
    }
}

val songListModule = module {
    viewModel { params ->
        SongListViewModel(
            navigator = params.get(),
            musicRepository = get(),
            playbackController = get()
        )
    }
}

val homeModule = module {
    viewModel { params ->
        HomeViewModel(params.get())
    }
}

val playListModule = module {
    viewModel { params ->
        PlayListViewModel(
            navigator = params.get(),
            playlistRepository = get()
        )
    }
}

val playlistDetailModule = module {
    viewModel { params ->
        PlaylistDetailViewModel(
            playlistId = params[0],
            navigator = params[1],
            playlistRepository = get(),
            playbackController = get()
        )
    }
}

val libraryModule = module {
    viewModel { params ->
        LibraryViewModel(params.get())
    }
}

val searchModule = module {
    viewModel { params ->
        SearchViewModel(params.get())
    }
}

val appModules = listOf(
    playModule,
    playerModule,
    albumDetailModule,
    albumListModule,
    artistDetailModule,
    artistListModule,
    genreDetailModule,
    genreListModule,
    composerDetailModule,
    composerListModule,
    songListModule,
    homeModule,
    playListModule,
    playlistDetailModule,
    libraryModule,
    searchModule
)
