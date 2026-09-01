@file:OptIn(KoinExperimentalAPI::class)
@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.aria.rythme.di

import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.datasource.MediaStoreSource
import com.aria.rythme.core.music.data.indexer.MusicIndexer
import com.aria.rythme.core.music.data.local.MusicDatabase
import com.aria.rythme.core.music.data.observer.MediaStoreWatcher
import com.aria.rythme.core.music.data.lyrics.EmbeddedLyricsProvider
import com.aria.rythme.core.music.data.lyrics.LocalLrcProvider
import com.aria.rythme.core.music.data.lyrics.LrclibProvider
import com.aria.rythme.core.music.data.lyrics.LyricsProvider
import com.aria.rythme.core.music.data.repository.LyricsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.music.data.repository.PlaylistRepository
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.feature.playlist.presentation.PlayListViewModel
import com.aria.rythme.feature.playlistdetail.presentation.PlaylistDetailViewModel
import com.aria.rythme.feature.albumdetail.presentation.AlbumDetailViewModel
import com.aria.rythme.feature.albumlist.presentation.AlbumListViewModel
import com.aria.rythme.feature.artistdetail.presentation.ArtistDetailViewModel
import com.aria.rythme.feature.artistlist.presentation.ArtistListViewModel
import com.aria.rythme.feature.composerdetail.presentation.ComposerDetailViewModel
import com.aria.rythme.feature.composerlist.presentation.ComposerListViewModel
import com.aria.rythme.feature.genredetail.presentation.GenreDetailViewModel
import com.aria.rythme.feature.genrelist.presentation.GenreListViewModel
import com.aria.rythme.feature.songlist.presentation.SongListViewModel
import com.aria.rythme.feature.search.presentation.SearchViewModel
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
    single {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    single<List<LyricsProvider>> {
        listOf(
            EmbeddedLyricsProvider(androidContext()),
            LocalLrcProvider(),
            LrclibProvider(get())
        )
    }
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
            musicRepository = get(),
            playbackController = get(),
            filterArtistId = params[1],
            filterComposer = params[2],
            filterGenre = params[3]
        )
    }
}

val albumListModule = module {
    viewModel {
        AlbumListViewModel(
            musicRepository = get(),
            appSettings = get()
        )
    }
}

val artistDetailModule = module {
    viewModel { params ->
        ArtistDetailViewModel(
            artistId = params.get(),
            musicRepository = get()
        )
    }
}

val artistListModule = module {
    viewModel {
        ArtistListViewModel(
            musicRepository = get()
        )
    }
}

val genreDetailModule = module {
    viewModel { params ->
        GenreDetailViewModel(
            genreName = params[0],
            musicRepository = get()
        )
    }
}

val genreListModule = module {
    viewModel {
        GenreListViewModel(
            musicRepository = get()
        )
    }
}

val composerDetailModule = module {
    viewModel { params ->
        ComposerDetailViewModel(
            composerName = params[0],
            musicRepository = get()
        )
    }
}

val composerListModule = module {
    viewModel {
        ComposerListViewModel(
            musicRepository = get()
        )
    }
}

val songListModule = module {
    viewModel {
        SongListViewModel(
            musicRepository = get(),
            playbackController = get()
        )
    }
}

val searchModule = module {
    viewModel {
        SearchViewModel(
            musicRepository = get(),
            playbackController = get()
        )
    }
}

val playListModule = module {
    viewModel {
        PlayListViewModel(
            playlistRepository = get()
        )
    }
}

val playlistDetailModule = module {
    viewModel { params ->
        PlaylistDetailViewModel(
            playlistId = params[0],
            playlistRepository = get(),
            playbackController = get()
        )
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
    searchModule,
    playListModule,
    playlistDetailModule
)
