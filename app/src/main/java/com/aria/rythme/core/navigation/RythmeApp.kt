package com.aria.rythme.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.aria.rythme.feature.home.presentation.HomeScreen
import com.aria.rythme.feature.library.presentation.LibraryScreen
import com.aria.rythme.feature.library.presentation.songlist.SongListScreen
import com.aria.rythme.feature.playlist.presentation.PlayListScreen
import com.aria.rythme.feature.search.presentation.SearchScreen
val LocalInnerPadding = staticCompositionLocalOf { PaddingValues(0.dp) }
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
        CompositionLocalProvider(
            LocalInnerPadding provides innerPadding
        ) {
            NavDisplay(
                modifier = Modifier
                    .fillMaxSize(),
                onBack = navigator::goBack,
                entries = navigationState.toEntries(
                    entryProvider {
                        entry<RythmeRoute.Home> {
                            HomeScreen(navigator)
                        }
                        entry<RythmeRoute.Playlist> {
                            PlayListScreen(navigator)
                        }
                        entry<RythmeRoute.Library> {
                            LibraryScreen(navigator)
                        }
                        entry<RythmeRoute.Search> {
                            SearchScreen(navigator)
                        }
                        entry<RythmeRoute.SongList> {
                            SongListScreen(navigator)
                        }
                    }
                )
            )
        }
    }
}

@Preview
@Composable
fun PreviewRythmeApp() {
    RythmeApp()
}
