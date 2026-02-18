package com.aria.rythme.di

import com.aria.rythme.feature.player.controller.PlaybackController
import com.aria.rythme.feature.player.data.datasource.MediaStoreSource
import com.aria.rythme.feature.player.data.local.MusicDatabase
import com.aria.rythme.feature.player.data.observer.MediaStoreObserver
import com.aria.rythme.feature.player.data.repository.SongCacheRepository
import com.aria.rythme.feature.player.data.settings.ScanSettingsRepository
import com.aria.rythme.feature.player.presentation.PlayerViewModel
import org.koin.android.ext.koin.androidContext
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
 * - ScanSettingsRepository: 扫描设置仓库
 * - MediaStoreSource: 本地音乐数据源
 * - PlaybackController: 播放控制器
 * - PlayerViewModel: 播放器 ViewModel
 */
val playerModule = module {
    /**
     * 扫描设置仓库
     *
     * 单例模式，管理扫描配置的持久化
     */
    single { ScanSettingsRepository(androidContext()) }
    
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
     * 歌曲缓存仓库
     *
     * 单例模式，管理本地歌曲缓存
     */
    single { SongCacheRepository(get()) }
    
    /**
     * MediaStore 数据源
     *
     * 单例模式，整个应用共享一个实例
     */
    single { MediaStoreSource(androidContext(), get(), get()) }
    
    /**
     * MediaStore 观察者
     *
     * 单例模式，监听音频文件变化
     */
    single { MediaStoreObserver(androidContext()) }

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
     */
    viewModel { PlayerViewModel(get(), get(), get()) }
}

/**
 * 所有模块列表
 */
val appModules = listOf(
    playerModule
)
