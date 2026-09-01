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

/** 一个真实可用的 TopBar 操作；onClick 不参与视觉身份。 */
data class Action(
    val actionKey: String,
    @param:DrawableRes val iconRes: Int,
    val iconSize: Dp = 22.dp,
    val contentDescription: String = "",
    val onClick: (() -> Unit)? = null
) {
    val key: String
        get() = actionKey
}

data class TopBarConfig(
    val showBackButton: Boolean = false,
    val actions: List<Action> = emptyList()
)

/**
 * 全局 TopBar 的页面级展示状态。
 *
 * 页面直接提交包含回调的 [TopBarConfig]；不再维护第二份 action-handler 注册表。
 * 动画层只读取 [Action.visualKey]，因此 lambda 更新不会触发视觉过渡。
 */
class TopBarState(
    private val topLevelRoutes: Set<NavKey> = ALL_TOP_LEVEL_ROUTES
) {
    private val isShowMap = mutableStateMapOf<NavKey, Boolean>()
    private val configMap = mutableStateMapOf<NavKey, TopBarConfig>()
    private val searchActiveMap = mutableStateMapOf<NavKey, Boolean>()
    private val searchTitleMap = mutableStateMapOf<NavKey, String>()

    private val defaultBackOnly = TopBarConfig(showBackButton = true)

    fun isShow(routeKey: NavKey): Boolean = isShowMap[routeKey] ?: true

    fun updateIsShow(routeKey: NavKey, show: Boolean) {
        isShowMap[routeKey] = show
    }

    fun getConfig(routeKey: NavKey): TopBarConfig =
        configMap[routeKey] ?: defaultConfigFor(routeKey)

    private fun defaultConfigFor(routeKey: NavKey): TopBarConfig = when (routeKey) {
        in topLevelRoutes -> TopBarConfig()
        else -> defaultBackOnly
    }

    fun updateConfig(routeKey: NavKey, config: TopBarConfig) {
        configMap[routeKey] = config
    }

    fun isSearchActive(routeKey: NavKey): Boolean = searchActiveMap[routeKey] ?: false

    fun getSearchTitle(routeKey: NavKey): String = searchTitleMap[routeKey] ?: ""

    fun updateSearchActive(routeKey: NavKey, active: Boolean, title: String = "") {
        searchActiveMap[routeKey] = active
        if (title.isNotEmpty()) {
            searchTitleMap[routeKey] = title
        }
    }

    fun onPageDispose(routeKey: NavKey) {
        searchActiveMap.remove(routeKey)
        searchTitleMap.remove(routeKey)
        if (routeKey !in topLevelRoutes) {
            isShowMap.remove(routeKey)
            configMap.remove(routeKey)
        }
    }
}

internal fun Action.visualKey(): String =
    "$key:$iconRes:${iconSize.value}:$contentDescription"

internal fun List<Action>.visualKey(): String = joinToString("|") { it.visualKey() }

val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun rememberTopBarState(): TopBarState = remember {
    TopBarState().apply {
        val backOnly = TopBarConfig(showBackButton = true)
        updateConfig(RythmeRoute.Home, TopBarConfig())
        updateConfig(
            RythmeRoute.Playlist,
            TopBarConfig(
                actions = listOf(
                    Action(actionKey = "add", iconRes = R.drawable.ic_add, iconSize = 18.dp)
                )
            )
        )
        updateConfig(RythmeRoute.Library, TopBarConfig())
        updateConfig(RythmeRoute.Search, TopBarConfig())
        updateConfig(RythmeRoute.ArtistList, backOnly)
        updateConfig(
            RythmeRoute.AlbumList,
            TopBarConfig(
                showBackButton = true,
                actions = listOf(Action(actionKey = "more", iconRes = R.drawable.ic_more))
            )
        )
        updateConfig(RythmeRoute.SongList, backOnly)
        updateConfig(RythmeRoute.GenreList, backOnly)
        updateConfig(RythmeRoute.ComposerList, backOnly)
    }
}
