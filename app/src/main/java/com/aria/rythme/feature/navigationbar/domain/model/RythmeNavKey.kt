package com.aria.rythme.feature.navigationbar.domain.model

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Rythme 应用导航键定义
 *
 * Navigation 3 使用类型安全的路由，所有路由都必须实现 NavKey 接口。
 * 使用 Kotlin Serialization 进行序列化。
 *
 * ## 路由结构
 * - Home: 主页（包含四个标签页）
 * - Player: 全屏播放器
 * - PlaylistDetail: 歌单详情
 * - AlbumDetail: 专辑详情
 * - ArtistDetail: 艺术家详情
 */

sealed interface RythmeRoute: NavKey {

    @Serializable
    data object ScaffoldPage : RythmeRoute

    /**
     * 主页
     */
    @Serializable
    data object Home : RythmeRoute

    /**
     * 广播
     */
    @Serializable
    data object Playlist : RythmeRoute

    /**
     * 资料库
     */
    @Serializable
    data object Library : RythmeRoute

    /**
     * 搜索
     */
    @Serializable
    data object Search : RythmeRoute

    /**
     * 全屏播放器
     */
    @Serializable
    data object Player : RythmeRoute

    /**
     * 歌单详情
     *
     * @param id 歌单ID
     */
    @Serializable
    data class PlaylistDetail(val id: String) : RythmeRoute

    /**
     * 专辑详情
     *
     * @param id 专辑ID
     */
    @Serializable
    data class AlbumDetail(val id: String) : RythmeRoute

    /**
     * 艺术家详情
     *
     * @param id 艺术家ID
     */
    @Serializable
    data class ArtistDetail(val id: String) : RythmeRoute

    /**
     * 歌曲列表
     */
    @Serializable
    data object SongList : RythmeRoute

    @Serializable
    data object AlbumList: RythmeRoute

    @Serializable
    data object ArtistList: RythmeRoute

    @Serializable
    data object GenreList : RythmeRoute

    @Serializable
    data object ComposerList : RythmeRoute
}

val ALL_TOP_LEVEL_ROUTES = setOf(
    RythmeRoute.Home,
    RythmeRoute.Playlist,
    RythmeRoute.Library,
    RythmeRoute.Search
)
