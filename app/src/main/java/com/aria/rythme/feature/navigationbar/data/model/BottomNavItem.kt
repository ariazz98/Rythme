package com.aria.rythme.feature.navigationbar.data.model

import com.aria.rythme.R
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute

data class BottomNavItem(
    val icon: Int,
    val title: Int
)

val TOP_LEVEL_DESTINATIONS = mapOf(
    RythmeRoute.Home to BottomNavItem(
        icon = R.drawable.ic_home,
        title = R.string.title_home
    ),
    RythmeRoute.Pitch to BottomNavItem(
        icon = R.drawable.ic_pitch,
        title = R.string.title_pitch
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
