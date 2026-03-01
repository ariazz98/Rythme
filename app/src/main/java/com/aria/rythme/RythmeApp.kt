@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.aria.rythme.feature.songlist.presentation.SongListScreen
import com.aria.rythme.ui.component.LocalTopBarState
import com.aria.rythme.ui.component.RythmeHeader
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

@Composable
fun RythmeApp() {
    val rootNavigationState = rememberNavigationState(
        startRoute = RythmeRoute.ScaffoldPage,
        topLevelRoutes = setOf(RythmeRoute.ScaffoldPage, RythmeRoute.Player)
    )
    val scaffoldNavigationState = rememberNavigationState(
        startRoute = RythmeRoute.Home,
        topLevelRoutes = ALL_TOP_LEVEL_ROUTES
    )
    val rootNavigator = remember { Navigator(rootNavigationState) }
    // 创建导航控制器
    val navigator = remember { Navigator(scaffoldNavigationState) }

    NavDisplay(
        modifier = Modifier.fillMaxSize(),
        onBack = rootNavigator::goBack,
        transitionSpec = {
            slideInVertically(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) { it } togetherWith fadeOut(animationSpec = tween(durationMillis = 100))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith slideOutVertically(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) { it }
        },
        predictivePopTransitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith slideOutVertically(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) { it }
        },
        entries = rootNavigationState.toEntries(
            entryProvider = entryProvider {
                entry<RythmeRoute.ScaffoldPage> {
                    ScaffoldNavigation(scaffoldNavigationState, navigator) {
                        rootNavigator.navigate(RythmeRoute.Player)
                    }
                }
                entry<RythmeRoute.Player> {
                    PlayerScreen(
                        onBack = {
                            rootNavigator.goBack()
                        }
                    )
                }
            }
        )
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

    val topBarState = rememberTopBarState()

    // 根据当前 tab 确定标题和按钮配置
    val topBarTitleRes = when (navigationState.topLevelRoute) {
        RythmeRoute.Home -> R.string.title_home
        RythmeRoute.Library -> R.string.title_library
        RythmeRoute.Search -> R.string.title_search
        RythmeRoute.Playlist -> R.string.title_play_list
        else -> R.string.title_home
    }
    val topBarHasMoreMenu = navigationState.topLevelRoute == RythmeRoute.Library

    CompositionLocalProvider(
        LocalBackdrop provides backdrop
    ) {
        Scaffold(
            topBar = {
                RythmeHeader(
                    title = stringResource(topBarTitleRes),
                    hasMoreMenu = topBarHasMoreMenu,
                    hasAvatar = true,
                    isShow = topBarState.isScrollAtTop,
                    onMoreClick = { /* TODO */ },
                    onAvatarClick = { /* TODO */ }
                )
            },
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
                LocalInnerPadding provides innerPadding,
                LocalTopBarState provides topBarState
            ) {
                // 页面内容区域：作为 backdrop 的背景录制源
                NavDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .background(MaterialTheme.rythmeColors.surface),
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
}

@Preview
@Composable
fun PreviewRythmeApp() {
    RythmeApp()
}
