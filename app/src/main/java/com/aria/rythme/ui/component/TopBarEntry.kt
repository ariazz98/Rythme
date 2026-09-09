package com.aria.rythme.ui.component

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/** 与 Navigation3 的 contentKey 对齐，并隔离各 Tab；不使用页面重建的路由身份。 */
internal data class TopBarEntryKey(val tab: NavKey, val contentKey: Any)

/** 描述、输入状态和导航呈现各自独立；页面只更新自己的绑定。 */
@Stable
internal class TopBarEntry(
    val key: TopBarEntryKey,
    val route: NavKey,
    val navigationVisibility: State<Float>,
    val scroll: HeaderScrollState = HeaderScrollState()
) {
    var config by mutableStateOf<TopBarConfig?>(null)
        private set
    var search by mutableStateOf<PageSearchState?>(null)
        private set

    fun update(config: TopBarConfig, search: PageSearchState?) {
        this.config = config
        this.search = search
    }
}

internal val LocalTopBarEntry = staticCompositionLocalOf<TopBarEntry> {
    error("Page header must be hosted by a Navigation3 entry")
}

/** 入口统一接线，页面不再提供 routeKey 或启动自己的导航动画。 */
@Composable
internal fun rememberTopBarEntryDecorator(
    tab: NavKey,
    state: TopBarState,
    skipNavigationAnimation: Boolean
): NavEntryDecorator<NavKey> {
    // decorator 跨导航复用，但过渡策略必须读取本次操作，不能捕获创建时的值。
    val currentSkipNavigationAnimation by rememberUpdatedState(skipNavigationAnimation)
    return remember(tab, state) {
        NavEntryDecorator(
            // Navigation3 会等待该 entry 出栈且不再参与离场绘制；切 Tab 不触发清理。
            onPop = { contentKey -> state.remove(TopBarEntryKey(tab, contentKey)) }
        ) { entry ->
            NavEntry(navEntry = entry) { route ->
                val animationSpec = headerNavigationVisibilitySpec(currentSkipNavigationAnimation)
                val visibility = if (animationSpec == null) {
                    // Tab 切换不注册导航子动画，也不等待它下一帧报告离场完成。
                    rememberUpdatedState(1f)
                } else {
                    LocalNavAnimatedContentScope.current.transition.animateFloat(
                        transitionSpec = { animationSpec },
                        label = "headerNavigationVisibility"
                    ) { if (it == EnterExitState.Visible) 1f else 0f }
                }
                val scroll = remember(tab, entry.contentKey) { HeaderScrollState() }
                val binding = remember(tab, entry.contentKey, visibility) {
                    TopBarEntry(TopBarEntryKey(tab, entry.contentKey), route, visibility, scroll)
                }
                SideEffect { state.attach(binding) }
                CompositionLocalProvider(LocalTopBarEntry provides binding) {
                    entry.Content()
                }
            }.Content()
        }
    }
}

/** 子动画也参与 NavDisplay 的离场完成判定；切 Tab 时不能把旧页面再保留 400ms。 */
internal fun headerNavigationVisibilitySpec(skipAnimation: Boolean): FiniteAnimationSpec<Float>? =
    if (skipAnimation) null else tween(HeaderLayout.navigationDuration, easing = FastOutSlowInEasing)
