package com.aria.rythme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.R
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.ActionGroup

/**
 * 单个操作项（图标按钮或头像按钮）
 */
sealed interface HeaderActionItem {
    /** 图标按钮 */
    data class Icon(
        @DrawableRes val iconRes: Int,
        val contentDescription: String = "",
        val onClick: () -> Unit = {}
    ) : HeaderActionItem

    /** 头像按钮 */
    data class Avatar(
        val url: String? = null,
        val name: String? = null,
        val onClick: () -> Unit = {}
    ) : HeaderActionItem
}

/**
 * 操作组，渲染为一个视觉单元
 *
 * - [Single] 独立按钮
 * - [Segmented] 组合按钮（多个图标共享一个容器）
 */
sealed interface ActionGroup {
    /** 单个独立按钮 */
    data class Single(val item: HeaderActionItem) : ActionGroup

    /** 组合按钮：多个图标合并在一个胶囊容器中 */
    data class Segmented(val items: List<HeaderActionItem.Icon>) : ActionGroup
}

/**
 * 某一路由的 TopBar 配置
 */
data class TopBarConfig(
    val showBackButton: Boolean = false,
    val actions: List<ActionGroup> = emptyList()
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
}

val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun rememberTopBarState(): TopBarState = remember {
    TopBarState().apply {
        val avatarAction = ActionGroup.Single(
            HeaderActionItem.Avatar(name = "ARiA") { /* TODO */ }
        )
        val defaultConfig = TopBarConfig(actions = listOf(avatarAction))
        updateConfig(RythmeRoute.Home, defaultConfig)
        updateConfig(RythmeRoute.Playlist, defaultConfig)
        updateConfig(RythmeRoute.Library, TopBarConfig(
            actions = listOf(
                ActionGroup.Single(HeaderActionItem.Icon(R.drawable.ic_more) { /* TODO */ }),
                avatarAction
            )
        ))
        updateConfig(RythmeRoute.Search, defaultConfig)
        updateConfig(RythmeRoute.ArtistList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                ActionGroup.Single(HeaderActionItem.Icon(R.drawable.ic_filter) { /* TODO */ })
            )
        ))
        updateConfig(RythmeRoute.AlbumList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                ActionGroup.Segmented(listOf(
                    HeaderActionItem.Icon(R.drawable.ic_filter) { /* TODO */ },
                    HeaderActionItem.Icon(R.drawable.ic_more) { /* TODO */ }
                ))
            )
        ))
        updateConfig(RythmeRoute.SongList, TopBarConfig(
            showBackButton = true,
            actions = listOf(
                ActionGroup.Segmented(listOf(
                    HeaderActionItem.Icon(R.drawable.ic_filter) { /* TODO */ },
                    HeaderActionItem.Icon(R.drawable.ic_more) { /* TODO */ }
                ))
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
