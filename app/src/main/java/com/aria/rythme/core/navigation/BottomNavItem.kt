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
    RythmeRoute.Playlist to BottomNavItem(
        icon = R.drawable.ic_play_list,
        title = R.string.title_play_list
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