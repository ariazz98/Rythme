package com.aria.rythme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.R
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute

/**
 * 单个操作项（图标按钮或头像按钮）
 */
sealed class Action(val onClick: () -> Unit) {
    /** 图标按钮 */
    data class Icon(
        @DrawableRes val iconRes: Int,
        val iconSize: Dp = 22.dp,
        val contentDescription: String = "",
        val onIconClick: () -> Unit = {}
    ) : Action(onIconClick)

    /** 头像按钮 */
    data class Avatar(
        val url: String? = null,
        val name: String? = null,
        val onAvatarClick: () -> Unit = {}
    ) : Action(onAvatarClick)
}

/**
 * 某一路由的 TopBar 配置
 */
data class TopBarConfig(
    val showBackButton: Boolean = false,
    val showMoreButton: Boolean = false,
    val actions: List<Action> = emptyList()
)

/**
 * 顶部标题栏状态
 *
 * 按路由 key 分别存储各页面的可见性和按钮配置，
 * 切换 Tab 时可立即读取对应缓存值。
 */
class TopBarState {
    // 每个路由独立保存"是否显示 TopBar"的状态，默认 true
    private val isShowMap = mutableStateMapOf<NavKey, Boolean>()
    // 每个路由独立保存按钮配置
    private val configMap = mutableStateMapOf<NavKey, TopBarConfig>()
    // 每个路由独立保存搜索激活状态
    private val searchActiveMap = mutableStateMapOf<NavKey, Boolean>()
    // 每个路由独立保存搜索页面标题
    private val searchTitleMap = mutableStateMapOf<NavKey, String>()

    /** 读取指定路由的可见性，未记录时返回 true（默认展示） */
    fun isShow(routeKey: NavKey): Boolean = isShowMap[routeKey] ?: true

    /** 由各页面的滚动容器实时更新可见性 */
    fun updateIsShow(routeKey: NavKey, show: Boolean) {
        isShowMap[routeKey] = show
    }

    /** 读取指定路由的按钮配置 */
    fun getConfig(routeKey: NavKey): TopBarConfig = configMap[routeKey] ?: TopBarConfig()

    /** 更新指定路由的按钮配置 */
    fun updateConfig(routeKey: NavKey, config: TopBarConfig) {
        configMap[routeKey] = config
    }

    /** 读取指定路由的搜索激活状态 */
    fun isSearchActive(routeKey: NavKey): Boolean = searchActiveMap[routeKey] ?: false

    /** 读取指定路由搜索时的页面标题 */
    fun getSearchTitle(routeKey: NavKey): String = searchTitleMap[routeKey] ?: ""

    /** 更新指定路由的搜索激活状态，激活时可附带页面标题 */
    fun updateSearchActive(routeKey: NavKey, active: Boolean, title: String = "") {
        searchActiveMap[routeKey] = active
        if (title.isNotEmpty()) {
            searchTitleMap[routeKey] = title
        }
    }

    /** 页面出栈时重置搜索状态（保留 config 等初始配置） */
    fun resetSearchState(routeKey: NavKey) {
        searchActiveMap.remove(routeKey)
        searchTitleMap.remove(routeKey)
    }
}

val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun rememberTopBarState(): TopBarState = remember {
    TopBarState().apply {
        val avatarAction = Action.Avatar(
            name = "ARiA"
        ) {
            // TODO:
        }
        val defaultConfig = TopBarConfig(actions = listOf(avatarAction))
        updateConfig(RythmeRoute.Home, defaultConfig)
        updateConfig(RythmeRoute.Playlist, defaultConfig)
        updateConfig(RythmeRoute.Library, TopBarConfig(
            showMoreButton = true,
            actions = listOf(avatarAction)
        ))
        updateConfig(RythmeRoute.Search, defaultConfig)
        updateConfig(RythmeRoute.ArtistList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                Action.Icon(iconRes = R.drawable.ic_filter) { /* TODO */ }
            )
        ))
        updateConfig(RythmeRoute.AlbumList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                Action.Icon(iconRes = R.drawable.ic_filter) { /* TODO */ },
                Action.Icon(iconRes = R.drawable.ic_more) { /* TODO */ }
            )
        ))
        updateConfig(RythmeRoute.SongList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                Action.Icon(iconRes = R.drawable.ic_filter) { /* TODO */ },
                Action.Icon(iconRes = R.drawable.ic_more) { /* TODO */ }
            )
        ))
        updateConfig(RythmeRoute.GenreList, TopBarConfig(
            showBackButton = true
        ))
        updateConfig(RythmeRoute.ComposerList, TopBarConfig(
            showBackButton = true
        ))
    }
}
