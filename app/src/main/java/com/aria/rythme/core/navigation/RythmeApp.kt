@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.aria.rythme.feature.home.presentation.HomeScreen
import com.aria.rythme.feature.library.presentation.LibraryScreen
import com.aria.rythme.feature.songlist.presentation.SongListScreen
import com.aria.rythme.feature.playlist.presentation.PlayListScreen
import com.aria.rythme.feature.search.presentation.SearchScreen
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

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
        modifier = Modifier.background(MaterialTheme.rythmeColors.surface),
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
                sceneStrategy = remember { DialogSceneStrategy() },
                entries = navigationState.toEntries(
                    entryProvider {
                        entry<RythmeRoute.Home> {
                            HomeScreen(viewModel = koinViewModel { parametersOf(navigator) })
                        }
                        entry<RythmeRoute.Playlist> {
                            PlayListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                        }
                        entry<RythmeRoute.Library> {
                            LibraryScreen(viewModel = koinViewModel { parametersOf(navigator) })
                        }
                        entry<RythmeRoute.Search> {
                            SearchScreen(viewModel = koinViewModel { parametersOf(navigator) })
                        }
                        entry<RythmeRoute.SongList>(
                            metadata = DialogSceneStrategy.dialog(
                                dialogProperties = DialogProperties(
                                    dismissOnBackPress = true,
                                    usePlatformDefaultWidth = false,
                                    decorFitsSystemWindows = false
                                )
                            )
                        ) {
                            SongListScreen(viewModel = koinViewModel { parametersOf(navigator) })
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
