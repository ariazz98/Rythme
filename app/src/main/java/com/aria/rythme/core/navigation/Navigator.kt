package com.aria.rythme.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState){

    fun navigate(route: NavKey){
        if (route in state.backStacks.keys){
            state.isTabSwitch = true
            state.topLevelRoute = route
        } else {
            state.isTabSwitch = false
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack(){
        val currentStack = state.backStacks[state.topLevelRoute] ?:
        error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute){
            state.isTabSwitch = true
            state.topLevelRoute = state.startRoute
        } else {
            state.isTabSwitch = false
            currentStack.removeLastOrNull()
        }
    }
}