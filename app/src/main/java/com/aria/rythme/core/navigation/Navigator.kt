package com.aria.rythme.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 *
 * Navigator 只在应用 UI 边界持有，不注入 ViewModel。
 */
class Navigator(private val state: NavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.operation = NavigationOperation.TabSwitch
            state.topLevelRoute = route
        } else {
            state.operation = NavigationOperation.Push
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    /** 返回 true 表示已由应用导航消费；false 表示应交给 Activity 退出。 */
    fun goBack(): Boolean {
        val currentStack = state.backStacks[state.topLevelRoute] ?:
        error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute == state.startRoute) {
                state.operation = NavigationOperation.Idle
                return false
            }
            state.operation = NavigationOperation.TabSwitch
            state.topLevelRoute = state.startRoute
        } else {
            state.operation = NavigationOperation.Pop
            currentStack.removeLastOrNull()
        }
        return true
    }
}
