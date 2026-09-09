package com.aria.rythme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey

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
    val title: String? = null
)

/** 二三级页按调用处给出的左到右顺序布局：1/2 项共用表面，3 项为左二右一。 */
internal fun secondaryTopBar(vararg actions: Action): TopBarConfig {
    require(actions.size <= 3) { "二三级页最多显示三个顶部操作，其余放入菜单" }
    val items = actions.toList()
    return TopBarConfig(
        showBackButton = true,
        auxiliaryActions = if (items.size == 3) items.take(2) else emptyList(),
        actions = if (items.size == 3) items.takeLast(1) else items
    )
}

/**
 * 只缓存实际导航 entry 的运行绑定，不再预填另一份页面按钮定义。
 * 清理由 Navigation3 的 onPop 驱动：真正出栈且离场组合释放后才移除。
 */
class TopBarState {
    private val entries = mutableStateMapOf<TopBarEntryKey, TopBarEntry>()

    internal fun attach(entry: TopBarEntry) {
        entries[entry.key] = entry
    }

    internal fun remove(key: TopBarEntryKey) {
        entries.remove(key)
    }

    internal fun find(tab: NavKey, route: NavKey): TopBarEntry? =
        entries.values.firstOrNull { it.key.tab == tab && it.route == route && it.config != null }
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

@Composable
fun rememberTopBarState(): TopBarState = remember { TopBarState() }
