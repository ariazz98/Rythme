@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme.di

import com.aria.rythme.feature.home.presentation.HomeViewModel
import com.aria.rythme.feature.library.presentation.LibraryViewModel
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.datasource.MediaStoreSource
import com.aria.rythme.core.music.data.indexer.MusicIndexer
import com.aria.rythme.core.music.data.local.MusicDatabase
import com.aria.rythme.core.music.data.observer.MediaStoreWatcher
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.feature.playlist.presentation.PlayListViewModel
import com.aria.rythme.feature.search.presentation.SearchViewModel
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
    single { MediaStoreSource(androidContext(), get()) }
    single { MediaStoreWatcher(androidContext(), get(), get()) }
    single { MusicRepository(get(), get(), get()) }
    single { MusicIndexer(androidContext(), get(), get(), get(), get(), get(), get()) }
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
        PlayListViewModel(params.get())
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
    songListModule,
    homeModule,
    playListModule,
    libraryModule,
    searchModule
)
