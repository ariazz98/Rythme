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
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute

/**
 * 单个操作项（图标按钮或头像按钮）
 *
 * Action 只定义外观，不包含点击事件。
 * 点击事件通过 [TopBarState.registerActionHandler] 按 key 注册。
 */
sealed class Action(val key: String) {
    /** 图标按钮 */
    data class Icon(
        val actionKey: String,
        @param:DrawableRes val iconRes: Int,
        val iconSize: Dp = 22.dp,
        val contentDescription: String = ""
    ) : Action(actionKey)

    /** 头像按钮 */
    data class Avatar(
        val actionKey: String,
        val url: String? = null,
        val name: String? = null
    ) : Action(actionKey)
}

/**
 * 某一路由的 TopBar 配置
 */
data class TopBarConfig(
    val showBackButton: Boolean = false,
    val moreAction: Action.Icon? = null,
    val actions: List<Action> = emptyList()
)

/**
 * 顶部标题栏状态
 *
 * 按路由 key 分别存储各页面的可见性和按钮配置，
 * 切换 Tab 时可立即读取对应缓存值。
 *
 * Action 的点击事件通过 [registerActionHandler] / [getActionHandler] 与 key 关联，
 * 各页面在 Compose 中动态注册，退出时由 [onPageDispose] 自动清除。
 */
class TopBarState(
    private val topLevelRoutes: Set<NavKey> = ALL_TOP_LEVEL_ROUTES
) {
    // 每个路由独立保存"是否显示 TopBar"的状态，默认 true
    private val isShowMap = mutableStateMapOf<NavKey, Boolean>()
    // 每个路由独立保存按钮配置（所有页面持久缓存，避免重复构造）
    private val configMap = mutableStateMapOf<NavKey, TopBarConfig>()
    // 每个路由独立保存搜索激活状态
    private val searchActiveMap = mutableStateMapOf<NavKey, Boolean>()
    // 每个路由独立保存搜索页面标题
    private val searchTitleMap = mutableStateMapOf<NavKey, String>()
    // Action 点击事件：(routeKey, actionKey) → handler
    private val actionHandlerMap = mutableMapOf<Pair<NavKey, String>, () -> Unit>()

    /** 读取指定路由的可见性，未记录时返回 true（默认展示） */
    fun isShow(routeKey: NavKey): Boolean = isShowMap[routeKey] ?: true

    /** 由各页面的滚动容器实时更新可见性 */
    fun updateIsShow(routeKey: NavKey, show: Boolean) {
        isShowMap[routeKey] = show
    }

    // 详情页默认配置（缓存固定实例，避免每次 getConfig 创建新对象导致 LaunchedEffect 反复触发）
    private val defaultBackOnly = TopBarConfig(showBackButton = true)

    private val defaultArtistDetail = TopBarConfig(
        showBackButton = true,
        actions = listOf(
            Action.Icon(actionKey = "star", iconRes = R.drawable.ic_star),
            Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)
        )
    )

    private val defaultAlbumDetail = TopBarConfig(
        showBackButton = true,
        actions = listOf(
            Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)
        )
    )

    /** 读取指定路由的按钮配置，未配置的路由按类型返回默认配置 */
    fun getConfig(routeKey: NavKey): TopBarConfig = configMap[routeKey] ?: defaultConfigFor(routeKey)

    private fun defaultConfigFor(routeKey: NavKey): TopBarConfig = when (routeKey) {
        is RythmeRoute.ArtistDetail -> defaultArtistDetail
        is RythmeRoute.AlbumDetail -> defaultAlbumDetail
        else -> defaultBackOnly
    }

    /** 更新指定路由的按钮配置 */
    fun updateConfig(routeKey: NavKey, config: TopBarConfig) {
        configMap[routeKey] = config
    }

    /** 注册指定路由下某个 actionKey 的点击事件 */
    fun registerActionHandler(routeKey: NavKey, actionKey: String, handler: () -> Unit) {
        actionHandlerMap[routeKey to actionKey] = handler
    }

    /** 获取指定路由下某个 actionKey 的点击事件 */
    fun getActionHandler(routeKey: NavKey, actionKey: String): (() -> Unit)? {
        return actionHandlerMap[routeKey to actionKey]
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

    /**
     * 页面出栈时调用：
     * - 一级页面：仅重置搜索状态，保留可见性等持久状态
     * - 非一级页面：清除所有临时状态（可见性、搜索、action handler），配置保留
     */
    fun onPageDispose(routeKey: NavKey) {
        searchActiveMap.remove(routeKey)
        searchTitleMap.remove(routeKey)
        if (routeKey !in topLevelRoutes) {
            isShowMap.remove(routeKey)
            // 清除该路由的所有 action handler
            actionHandlerMap.keys.removeAll { it.first == routeKey }
        }
    }
}

val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun rememberTopBarState(): TopBarState = remember {
    TopBarState().apply {
        val avatarAction = Action.Avatar(actionKey = "avatar", name = "ARiA")
        val defaultConfig = TopBarConfig(actions = listOf(avatarAction))
        updateConfig(RythmeRoute.Home, defaultConfig)
        updateConfig(RythmeRoute.Playlist, TopBarConfig(
            moreAction = Action.Icon(actionKey = "more", iconRes = R.drawable.ic_add, iconSize = 18.dp),
            actions = listOf(avatarAction)
        ))
        updateConfig(RythmeRoute.Library, TopBarConfig(
            moreAction = Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more),
            actions = listOf(avatarAction)
        ))
        updateConfig(RythmeRoute.Search, defaultConfig)
        updateConfig(RythmeRoute.ArtistList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                Action.Icon(actionKey = "filter", iconRes = R.drawable.ic_filter)
            )
        ))
        updateConfig(RythmeRoute.AlbumList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                Action.Icon(actionKey = "filter", iconRes = R.drawable.ic_filter),
                Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)
            )
        ))
        updateConfig(RythmeRoute.SongList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                Action.Icon(actionKey = "filter", iconRes = R.drawable.ic_filter),
                Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)
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
