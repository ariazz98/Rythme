@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme.di

import com.aria.rythme.feature.home.presentation.HomeViewModel
import com.aria.rythme.feature.library.presentation.LibraryViewModel
import com.aria.rythme.feature.songlist.presentation.SongListViewModel
import com.aria.rythme.core.play.controller.PlaybackController
import com.aria.rythme.feature.player.data.datasource.MediaStoreSource
import com.aria.rythme.feature.player.data.local.MusicDatabase
import com.aria.rythme.feature.player.data.observer.MediaStoreObserver
import com.aria.rythme.feature.player.data.repository.MusicRepository
import com.aria.rythme.feature.player.data.settings.AppSettingsRepository
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import com.aria.rythme.feature.playlist.presentation.PlayListViewModel
import com.aria.rythme.feature.search.presentation.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin 依赖注入模块
 *
 * 定义应用级别的依赖注入配置。
 */

/**
 * 播放器模块
 *
 * 提供播放器相关的依赖：
 * - AppSettingsRepository: 扫描设置仓库
 * - MusicDatabase: 音乐数据库
 * - MediaStoreSource: MediaStore 扫描器（只扫描）
 * - MediaStoreObserver: MediaStore 观察者
 * - MusicRepository: 音乐数据仓库（数据层闭环，合并 CRUD + 业务逻辑）
 * - PlaybackController: 播放控制器
 * - PlayerViewModel: 播放器 ViewModel
 */

/**
 * 播放相关依赖项
 */
val playModule = module {

}
val playerModule = module {
    /**
     * 扫描设置仓库
     *
     * 单例模式，管理扫描配置的持久化
     */
    single { AppSettingsRepository(androidContext()) }
    
    /**
     * 音乐数据库
     *
     * 单例模式，整个应用共享一个数据库实例
     */
    single { MusicDatabase.getInstance(androidContext()) }
    
    /**
     * 歌曲 DAO
     *
     * 从数据库实例获取
     */
    single { get<MusicDatabase>().songDao() }
    
    /**
     * MediaStore 数据源
     *
     * 单例模式，只负责扫描，不负责数据存储
     */
    single { MediaStoreSource(androidContext(), get()) }
    
    /**
     * MediaStore 观察者
     *
     * 单例模式，监听音频文件变化
     */
    single { MediaStoreObserver(androidContext()) }
    
    /**
     * 音乐数据仓库
     *
     * 单例模式，统一管理歌曲数据加载和 MediaStore 监听
     * 数据层闭环，暴露响应式数据流给 ViewModel
     */
    single { MusicRepository(get(), get(), get()) }

    /**
     * 播放控制器
     *
     * 单例模式，确保整个应用使用同一个播放器实例
     * 在创建时自动初始化
     */
    single { 
        PlaybackController(androidContext()).apply {
            initialize()
        }
    }

    /**
     * 播放器 ViewModel
     *
     * ViewModel 作用域，与页面生命周期绑定
     * 只依赖 PlaybackController 和 MusicRepository
     */
    viewModel { PlayerViewModel(get(), get()) }
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

val songListModule = module {
    viewModel { params ->
        SongListViewModel(
            navigator = params.get(),
            musicRepository = get(),
            playbackController = get()
        )
    }
}

/**
 * 所有模块列表
 */
val appModules = listOf(
    playerModule,
    homeModule,
    playListModule,
    libraryModule,
    searchModule,
    songListModule
)
