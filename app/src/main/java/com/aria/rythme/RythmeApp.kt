@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
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
import com.aria.rythme.feature.albumlist.presentation.AlbumListScreen
import com.aria.rythme.feature.artistlist.presentation.ArtistListScreen
import com.aria.rythme.feature.composerlist.presentation.ComposerListScreen
import com.aria.rythme.feature.genrelist.presentation.GenreListScreen
import com.aria.rythme.feature.songlist.presentation.SongListScreen
import com.aria.rythme.ui.component.LocalTopBarState
import com.aria.rythme.ui.component.RythmeHeader
import com.aria.rythme.ui.component.TopBarConfig
import com.aria.rythme.ui.component.rememberTopBarState
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

val LocalInnerPadding = staticCompositionLocalOf { PaddingValues(0.dp) }
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { error("Backdrop must be provided") }
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> { error("No SharedTransitionScope") }
val LocalPlayerVisible = compositionLocalOf { false }

@Composable
fun RythmeApp() {
    val navigationState = rememberNavigationState(
        startRoute = RythmeRoute.Home,
        topLevelRoutes = ALL_TOP_LEVEL_ROUTES
    )
    val navigator = remember { Navigator(navigationState) }
    // Player 以浮层方式叠加，Scaffold 始终存活不被销毁
    var playerVisible by remember { mutableStateOf(false) }

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this@SharedTransitionLayout,
            LocalPlayerVisible provides playerVisible
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ScaffoldNavigation(
                    navigationState = navigationState,
                    navigator = navigator,
                    openPlayer = { playerVisible = true },
                    onBack = {
                        if (playerVisible) {
                            playerVisible = false
                        } else {
                            navigator.goBack()
                        }
                    }
                )
                BackHandler(enabled = playerVisible) {
                    playerVisible = false
                }
                PlayerScreen(onBack = { playerVisible = false })
            }
        }
    }
}

@Composable
private fun ScaffoldNavigation(
    navigationState: NavigationState,
    navigator: Navigator,
    openPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color.White)
        drawContent()
    }

    val topBarState = rememberTopBarState()

    CompositionLocalProvider(
        LocalBackdrop provides backdrop
    ) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                RythmeHeader(
                    isShow = topBarState.isShow(navigationState.currentRoute),
                    config = topBarState.getConfig(navigationState.currentRoute),
                    skipAnimation = navigationState.isTabSwitch,
                    onBackClick = { navigator.goBack() }
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    selectedTabIndex = {
                        when (navigationState.topLevelRoute) {
                            RythmeRoute.Home -> 0
                            RythmeRoute.Playlist -> 1
                            RythmeRoute.Library -> 2
                            RythmeRoute.Search -> 3
                            else -> 0
                        }
                    },
                    onTabSelected = {
                        when (it) {
                            0 -> navigator.navigate(RythmeRoute.Home)
                            1 -> navigator.navigate(RythmeRoute.Playlist)
                            2 -> navigator.navigate(RythmeRoute.Library)
                            3 -> navigator.navigate(RythmeRoute.Search)
                        }
                    },
                    onClickPlayer = {
                        openPlayer()
                    }
                )
            }
        ) { innerPadding ->
            CompositionLocalProvider(
                LocalInnerPadding provides innerPadding,
                LocalTopBarState provides topBarState
            ) {
                // 页面内容区域：作为 backdrop 的背景录制源
                val snapSpec = EnterTransition.None togetherWith ExitTransition.None

                NavDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop),
                    onBack = onBack,
                    transitionSpec = {
                        if (navigationState.isTabSwitch) snapSpec
                        else slideInHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { it } togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { -it / 2 }
                    },
                    popTransitionSpec = {
                        if (navigationState.isTabSwitch) snapSpec
                        else slideInHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { -it / 2 } togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { it }
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
                            entry<RythmeRoute.SongList>{
                                SongListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                            entry<RythmeRoute.AlbumList>{
                                AlbumListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                            entry<RythmeRoute.ArtistList>{
                                ArtistListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                            entry<RythmeRoute.GenreList> {
                                GenreListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                            entry<RythmeRoute.ComposerList> {
                                ComposerListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                        }
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewRythmeApp() {
    RythmeApp()
}
