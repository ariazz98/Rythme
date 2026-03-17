@file:OptIn(KoinExperimentalAPI::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)

package com.aria.rythme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.aria.rythme.core.navigation.NavigationState
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.core.navigation.rememberNavigationState
import com.aria.rythme.core.navigation.toEntries
import com.aria.rythme.feature.albumdetail.presentation.AlbumDetailScreen
import com.aria.rythme.feature.albumlist.presentation.AlbumListScreen
import com.aria.rythme.feature.artistdetail.presentation.ArtistDetailScreen
import com.aria.rythme.feature.artistlist.presentation.ArtistListScreen
import com.aria.rythme.feature.composerdetail.presentation.ComposerDetailScreen
import com.aria.rythme.feature.composerlist.presentation.ComposerListScreen
import com.aria.rythme.feature.genredetail.presentation.GenreDetailScreen
import com.aria.rythme.feature.genrelist.presentation.GenreListScreen
import com.aria.rythme.feature.home.presentation.HomeScreen
import com.aria.rythme.feature.library.presentation.LibraryScreen
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.feature.navigationbar.presentation.BottomNavigationBar
import com.aria.rythme.feature.player.presentation.PlayerScreen
import com.aria.rythme.feature.playlist.presentation.PlayListScreen
import com.aria.rythme.feature.search.presentation.SearchScreen
import com.aria.rythme.feature.songlist.presentation.SongListScreen
import com.aria.rythme.ui.component.LocalOverlayMenu
import com.aria.rythme.ui.component.LocalTopBarState
import com.aria.rythme.ui.component.OverlayMenuHost
import com.aria.rythme.ui.component.OverlayMenuState
import com.aria.rythme.ui.component.RythmeHeader
import com.aria.rythme.ui.component.TopBarState
import com.aria.rythme.ui.component.rememberTopBarState
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

val LocalInnerPadding = staticCompositionLocalOf { PaddingValues(0.dp) }
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { error("Backdrop must be provided") }
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> { error("No SharedTransitionScope") }
val LocalContentSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> { error("No SharedTransitionScope") }
val LocalPlayerVisible = compositionLocalOf { false }
val LocalSharedAlbumId = compositionLocalOf<String?> { null }

@Composable
fun RythmeApp() {
    val navigationState = rememberNavigationState(
        startRoute = RythmeRoute.Home,
        topLevelRoutes = ALL_TOP_LEVEL_ROUTES
    )
    val navigator = Navigator.getOrCreate(navigationState)
    // Player 以浮层方式叠加，Scaffold 始终存活不被销毁
    var playerVisible by remember { mutableStateOf(false) }
    val overlayMenuState = remember { OverlayMenuState() }
    val topBarState = rememberTopBarState()
    val backdrop = rememberLayerBackdrop()

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this@SharedTransitionLayout,
            LocalPlayerVisible provides playerVisible,
            LocalOverlayMenu provides overlayMenuState,
            LocalBackdrop provides backdrop,
            LocalTopBarState provides topBarState
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ScaffoldNavigation(
                    backdrop = backdrop,
                    topBarState = topBarState,
                    navigationState = navigationState,
                    navigator = navigator,
                    openPlayer = { playerVisible = true },
                    onBack = {
                        if (overlayMenuState.isVisible) {
                            overlayMenuState.dismiss()
                        } else if (playerVisible) {
                            playerVisible = false
                        } else if (topBarState.isSearchActive(navigationState.currentRoute)) {
                            topBarState.updateSearchActive(navigationState.currentRoute, false)
                        } else {
                            navigator.goBack()
                        }
                    }
                )
                BackHandler(enabled = playerVisible && !overlayMenuState.isVisible) {
                    playerVisible = false
                }
                BackHandler(enabled = overlayMenuState.isVisible) {
                    overlayMenuState.dismiss()
                }
                PlayerScreen(onBack = { playerVisible = false })
                OverlayMenuHost(state = overlayMenuState)
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.ScaffoldNavigation(
    backdrop: LayerBackdrop,
    topBarState: TopBarState,
    navigationState: NavigationState,
    navigator: Navigator,
    openPlayer: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            RythmeHeader(
                isShow = topBarState.isShow(navigationState.currentRoute),
                routeKey = navigationState.currentRoute,
                config = topBarState.getConfig(navigationState.currentRoute),
                isSearchActive = topBarState.isSearchActive(navigationState.currentRoute),
                searchTitle = topBarState.getSearchTitle(navigationState.currentRoute),
                onSearchClose = {
                    topBarState.updateSearchActive(navigationState.currentRoute, false)
                },
                skipAnimation = navigationState.isTabSwitch,
                onBackClick = { navigator.goBack() }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                isHeaderSearchActive = topBarState.isSearchActive(navigationState.currentRoute),
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
        val sharedAlbumId = (navigationState.currentRoute as? RythmeRoute.AlbumDetail)?.id

        // Album 专用 SharedTransitionLayout，overlay 在 topBar/bottomBar 之下
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalInnerPadding provides innerPadding,
                LocalSharedAlbumId provides sharedAlbumId,
                LocalContentSharedTransitionScope provides this@SharedTransitionLayout
            ) {
                // 页面内容区域：作为 backdrop 的背景录制源
                val snapSpec = EnterTransition.None togetherWith ExitTransition.None

                val focusManager = LocalFocusManager.current
                val imeVisible = WindowInsets.isImeVisible


                NavDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imeVisible) {
                            if (imeVisible) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.type == PointerEventType.Press) {
                                            focusManager.clearFocus()
                                        }
                                    }
                                }
                            }
                        }
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
                            entry<RythmeRoute.AlbumDetail>(
                                metadata = NavDisplay.transitionSpec {
                                    fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                                } + NavDisplay.popTransitionSpec {
                                    fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                                }
                            ) { key ->
                                AlbumDetailScreen(
                                    albumId = key.id,
                                    viewModel = koinViewModel(key = "${key.id}_${key.filterArtistId}_${key.filterComposer}_${key.filterGenre}") {
                                        parametersOf(key.id.toLong(), navigator, key.filterArtistId, key.filterComposer, key.filterGenre)
                                    }
                                )
                            }
                            entry<RythmeRoute.ArtistList>{
                                ArtistListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                            entry<RythmeRoute.ArtistDetail> { key ->
                                ArtistDetailScreen(
                                    artistId = key.id,
                                    viewModel = koinViewModel { parametersOf(key.id.toLong(), navigator) }
                                )
                            }
                            entry<RythmeRoute.GenreList> {
                                GenreListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                            entry<RythmeRoute.GenreDetail> { key ->
                                GenreDetailScreen(
                                    genreName = key.genre,
                                    viewModel = koinViewModel(key = key.genre) { parametersOf(key.genre, navigator) }
                                )
                            }
                            entry<RythmeRoute.ComposerList> {
                                ComposerListScreen(viewModel = koinViewModel { parametersOf(navigator) })
                            }
                            entry<RythmeRoute.ComposerDetail> { key ->
                                ComposerDetailScreen(
                                    composerName = key.composer,
                                    viewModel = koinViewModel(key = key.composer) { parametersOf(key.composer, navigator) }
                                )
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
