@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.aria.rythme.core.navigation.NavigationState
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.core.navigation.rememberNavigationState
import com.aria.rythme.core.navigation.toEntries
import com.aria.rythme.feature.home.presentation.HomeScreen
import com.aria.rythme.feature.library.presentation.LibraryScreen
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.feature.navigationbar.presentation.BottomNavigationBar
import com.aria.rythme.feature.player.presentation.PlayerScreen
import com.aria.rythme.feature.playlist.presentation.PlayListScreen
import com.aria.rythme.feature.search.presentation.SearchScreen
import com.aria.rythme.feature.songlist.presentation.SongListScreen
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

val LocalInnerPadding = staticCompositionLocalOf { PaddingValues(0.dp) }
@Composable
fun RythmeApp() {
    val rootBackStack = rememberNavBackStack(RythmeRoute.ScaffoldPage)
    val navigationState = rememberNavigationState(
        startRoute = RythmeRoute.Home,
        topLevelRoutes = ALL_TOP_LEVEL_ROUTES
    )
    // 创建导航控制器
    val navigator = remember { Navigator(navigationState) }

    NavDisplay(
        backStack = rootBackStack,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            slideInVertically(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) { it } togetherWith fadeOut(animationSpec = tween(durationMillis = 100))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith slideOutVertically(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) { it }
        },
        predictivePopTransitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith slideOutVertically(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) { it }
        },
        entryProvider = entryProvider {
            entry<RythmeRoute.ScaffoldPage> {
                ScaffoldNavigation(navigationState, navigator) {
                    rootBackStack.add(RythmeRoute.Player)
                }
            }
            entry<RythmeRoute.Player> {
                PlayerScreen(
                    onBack = {
                        rootBackStack.remove(RythmeRoute.Player)
                    }
                )
            }
        }
    )
}

@Composable
private fun ScaffoldNavigation(
    navigationState: NavigationState,
    navigator: Navigator,
    openPlayer: () -> Unit
) {

    val backdrop = rememberLayerBackdrop {
        drawRect(Color.White)
        drawContent()
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.rythmeColors.surface),
        bottomBar = {
            BottomNavigationBar(
                backdrop = backdrop,
                selectedKey = navigationState.topLevelRoute,
                onSelectKey = {
                    navigator.navigate(it)
                },
                onClickPlayer = {
                    openPlayer()
                }
            )
        }
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalInnerPadding provides innerPadding
        ) {
            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                onBack = navigator::goBack,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith fadeOut(animationSpec = tween(durationMillis = 100))
                },
                popTransitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith fadeOut(animationSpec = tween(durationMillis = 100))
                },
                predictivePopTransitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith fadeOut(animationSpec = tween(durationMillis = 100))
                },
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
                            metadata = NavDisplay.transitionSpec {
                                slideInHorizontally(
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                ) { it } togetherWith slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                ) { -it / 2 }
                            } + NavDisplay.popTransitionSpec {
                                slideInHorizontally(
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                ) { -it / 2 } togetherWith slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                ) { it }
                            } + NavDisplay.predictivePopTransitionSpec {
                                slideInHorizontally(
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                ) { -it / 2 } togetherWith slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                ) { it }
                            }
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
