@file:OptIn(KoinExperimentalAPI::class)

package com.aria.rythme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val navigationState = rememberNavigationState(
        startRoute = RythmeRoute.Home,
        topLevelRoutes = ALL_TOP_LEVEL_ROUTES
    )
    val navigator = remember { Navigator(navigationState) }
    // Player 以浮层方式叠加，Scaffold 始终存活不被销毁
    var playerVisible by remember { mutableStateOf(false) }

    // 当 Player 可见时拦截系统返回键，关闭 Player 而非退出应用
    BackHandler(enabled = playerVisible) {
        playerVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScaffoldNavigation(
            navigationState = navigationState,
            navigator = navigator,
            openPlayer = { playerVisible = true }
        )

        // Player 全屏浮层：从底部滑入/滑出，Scaffold 保持存活
        AnimatedVisibility(
            visible = playerVisible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) { it },
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) { it }
        ) {
            PlayerScreen(onBack = { playerVisible = false })
        }
    }
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

    CompositionLocalProvider(
        LocalBackdrop provides backdrop
    ) {
        Scaffold(
            topBar = {
                // Crossfade 使标题、按钮与页面内容同步切换，避免标题先于内容更新
                Crossfade(
                    targetState = navigationState.topLevelRoute,
                    animationSpec = tween(durationMillis = 100)
                ) { route ->
                    val titleRes = when (route) {
                        RythmeRoute.Home -> R.string.title_home
                        RythmeRoute.Library -> R.string.title_library
                        RythmeRoute.Search -> R.string.title_search
                        RythmeRoute.Playlist -> R.string.title_play_list
                        else -> R.string.title_home
                    }
                    RythmeHeader(
                        title = stringResource(titleRes),
                        hasMoreMenu = route == RythmeRoute.Library,
                        hasAvatar = true,
                        // 使用该路由自己的滚动缓存，而非全局当前值
                        isShow = topBarState.isScrollAtTop(route),
                        onMoreClick = { /* TODO */ },
                        onAvatarClick = { /* TODO */ }
                    )
                }
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
