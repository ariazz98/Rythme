package com.aria.rythme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.R
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute

/** TopBar 操作的视觉定义；功能可以稍后接入，但入口不能因此消失。 */
sealed interface Action {
    val key: String
    val onClick: (() -> Unit)?

    data class Icon(
        val actionKey: String,
        @param:DrawableRes val iconRes: Int,
        val iconSize: Dp = 22.dp,
        val contentDescription: String = "",
        val isActive: Boolean = false,
        val menu: (() -> List<MenuConfig>)? = null,
        override val onClick: (() -> Unit)? = null
    ) : Action {
        override val key: String
            get() = actionKey
    }

    data class Avatar(
        val actionKey: String,
        val url: String? = null,
        val name: String? = null,
        val contentDescription: String = "",
        override val onClick: (() -> Unit)? = null
    ) : Action {
        override val key: String
            get() = actionKey
    }
}

data class TopBarConfig(
    val showBackButton: Boolean = false,
    val auxiliaryActions: List<Action> = emptyList(),
    val actions: List<Action> = emptyList(),
    val title: String? = null,
    val search: PageSearchState? = null,
    val navigationVisibility: State<Float>? = null
)

/**
 * 全局 TopBar 的页面级展示状态。
 *
 * 页面直接提交包含回调的 [TopBarConfig]；不再维护第二份 action-handler 注册表。
 * 动画层通过 [Action] 的内容身份识别页面变化；回调和收藏状态更新不触发页面过渡。
 */
class TopBarState(
    private val topLevelRoutes: Set<NavKey> = ALL_TOP_LEVEL_ROUTES
) {
    private val configMap = mutableStateMapOf<NavKey, TopBarConfig>()

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

    fun getConfig(routeKey: NavKey): TopBarConfig =
        configMap[routeKey] ?: defaultConfigFor(routeKey)

    private fun defaultConfigFor(routeKey: NavKey): TopBarConfig = when (routeKey) {
        is RythmeRoute.ArtistDetail -> defaultArtistDetail
        is RythmeRoute.AlbumDetail -> defaultAlbumDetail
        in topLevelRoutes -> TopBarConfig()
        else -> defaultBackOnly
    }

    fun updateConfig(routeKey: NavKey, config: TopBarConfig) {
        configMap[routeKey] = config
    }

    /** 只随真正出栈清理；List/Grid 重组或切 Tab 不代表页面被移除。 */
    fun retainRoutes(routes: Set<NavKey>) {
        configMap.keys.filter { it !in routes && it !in topLevelRoutes }.forEach(configMap::remove)
    }
}

internal fun Action.visualKey(): String = when (this) {
    is Action.Icon -> "icon:$key:$iconRes:${iconSize.value}:$contentDescription:$isActive"
    is Action.Avatar -> "avatar:$key:$url:$name:$contentDescription"
}

internal fun List<Action>.visualKey(): String = joinToString("|") { it.visualKey() }

/** 内容身份用于页面过渡；收藏等原地状态更新只重绘，不触发整组模糊。 */
internal fun List<Action>.contentKey(): List<Any> = map {
    when (it) {
        is Action.Icon -> listOf(it.key, it.iconRes, it.iconSize)
        is Action.Avatar -> listOf(it.key, it.url, it.name)
    }
}

val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun rememberTopBarState(): TopBarState = remember {
    TopBarState().apply {
        val backOnly = TopBarConfig(showBackButton = true)
        val avatarAction = Action.Avatar(actionKey = "avatar", name = "ARiA")
        val avatarConfig = TopBarConfig(actions = listOf(avatarAction))
        updateConfig(RythmeRoute.Home, avatarConfig)
        updateConfig(
            RythmeRoute.Playlist,
            TopBarConfig(
                auxiliaryActions = listOf(Action.Icon(
                    actionKey = "add",
                    iconRes = R.drawable.ic_add,
                    iconSize = 18.dp
                )),
                actions = listOf(avatarAction)
            )
        )
        updateConfig(
            RythmeRoute.Library,
            TopBarConfig(
                auxiliaryActions = listOf(Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)),
                actions = listOf(avatarAction)
            )
        )
        updateConfig(RythmeRoute.Search, avatarConfig)
        updateConfig(
            RythmeRoute.ArtistList,
            TopBarConfig(
                showBackButton = true,
                actions = listOf(
                    Action.Icon(actionKey = "filter", iconRes = R.drawable.ic_filter)
                )
            )
        )
        updateConfig(
            RythmeRoute.AlbumList,
            TopBarConfig(
                showBackButton = true,
                actions = listOf(
                    Action.Icon(actionKey = "filter", iconRes = R.drawable.ic_filter),
                    Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)
                )
            )
        )
        updateConfig(
            RythmeRoute.SongList,
            TopBarConfig(
                showBackButton = true,
                actions = listOf(
                    Action.Icon(actionKey = "filter", iconRes = R.drawable.ic_filter),
                    Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)
                )
            )
        )
        updateConfig(RythmeRoute.GenreList, backOnly)
        updateConfig(RythmeRoute.ComposerList, backOnly)
    }
}
