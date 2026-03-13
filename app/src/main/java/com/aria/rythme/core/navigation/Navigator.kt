package com.aria.rythme.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 *
 * 使用 [getOrCreate] 获取实例，确保跨配置变更（如深色模式切换）时
 * ViewModel 持有的 Navigator 引用不会失效。
 */
class Navigator(var state: NavigationState) {

    companion object {
        private var instance: Navigator? = null

        /**
         * 获取或创建 Navigator 单例，并同步最新的 NavigationState。
         * Activity 重建后 NavigationState 是新实例，但 ViewModel 中的 Navigator 引用不变。
         */
        fun getOrCreate(state: NavigationState): Navigator {
            return (instance ?: Navigator(state).also { instance = it }).also {
                it.state = state
            }
        }
    }

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.isTabSwitch = true
            state.topLevelRoute = route
        } else {
            state.isTabSwitch = false
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?:
        error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            state.isTabSwitch = true
            state.topLevelRoute = state.startRoute
        } else {
            state.isTabSwitch = false
            currentStack.removeLastOrNull()
        }
    }
}
