package com.aria.rythme.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.aria.rythme.feature.discover.presentation.DiscoverScreen
import com.aria.rythme.feature.home.presentation.HomeScreen
import com.aria.rythme.feature.library.presentation.LibraryScreen
import com.aria.rythme.feature.radio.presentation.RadioScreen
import com.aria.rythme.feature.search.presentation.SearchScreen

@Composable
fun RythmeApp() {
    val navigationState = rememberNavigationState(
        startRoute = RythmeRoute.Home,
        topLevelRoutes = ALL_TOP_LEVEL_ROUTES
    )

    // 创建导航控制器
    val navigator = remember { Navigator(navigationState) }

    Scaffold(
        modifier = Modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedKey = navigationState.topLevelRoute,
                onSelectKey = {
                    navigator.navigate(it)
                }
            )
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onBack = navigator::goBack,
            entries = navigationState.toEntries(
                entryProvider {
                    entry<RythmeRoute.Home> {
                        HomeScreen()
                    }
                    entry<RythmeRoute.Discover> {
                        DiscoverScreen()
                    }
                    entry<RythmeRoute.Radio> {
                        RadioScreen()
                    }
                    entry<RythmeRoute.Library> {
                        LibraryScreen()
                    }
                    entry<RythmeRoute.Search> {
                        SearchScreen()
                    }
                }
            )
        )
    }
}

@Preview
@Composable
fun PreviewRythmeApp() {
    RythmeApp()
}
