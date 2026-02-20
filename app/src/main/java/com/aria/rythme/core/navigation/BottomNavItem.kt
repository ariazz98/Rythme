package com.aria.rythme.core.navigation

import com.aria.rythme.R

data class BottomNavItem(
    val icon: Int,
    val title: Int
)

val TOP_LEVEL_DESTINATIONS = mapOf(
    RythmeRoute.Home to BottomNavItem(
        icon = R.drawable.ic_home,
        title = R.string.title_home
    ),
    RythmeRoute.Discover to BottomNavItem(
        icon = R.drawable.ic_discover,
        title = R.string.title_discover
    ),
    RythmeRoute.Radio to BottomNavItem(
        icon = R.drawable.ic_radio,
        title = R.string.title_radio
    ),
    RythmeRoute.Library to BottomNavItem(
        icon = R.drawable.ic_library,
        title = R.string.title_library
    ),
    RythmeRoute.Search to BottomNavItem(
        icon = R.drawable.ic_search,
        title = R.string.title_search
    )
)