@file:OptIn(KoinExperimentalAPI::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)

package com.aria.rythme

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.LaunchedEffect
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
import com.aria.rythme.core.navigation.NavigationOperation
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.core.navigation.rememberNavigationState
import com.aria.rythme.core.navigation.toEntries
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.feature.albumdetail.presentation.AlbumDetailScreen
import com.aria.rythme.feature.albumlist.presentation.AlbumListScreen
import com.aria.rythme.feature.artistdetail.presentation.ArtistDetailScreen
import com.aria.rythme.feature.artistlist.presentation.ArtistListScreen
import com.aria.rythme.feature.library.presentation.LibraryFacet
import com.aria.rythme.feature.library.presentation.LibraryFacetDetailScreen
import com.aria.rythme.feature.library.presentation.LibraryFacetListScreen
import com.aria.rythme.feature.home.presentation.HomeScreen
import com.aria.rythme.feature.library.presentation.LibraryScreen
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.feature.navigationbar.presentation.BottomNavigationBar
import com.aria.rythme.feature.navigationbar.presentation.LocalBottomBarState
import com.aria.rythme.feature.navigationbar.presentation.rememberBottomBarState
import com.aria.rythme.feature.player.presentation.PlayerScreen
import com.aria.rythme.feature.playlist.presentation.PlayListScreen
import com.aria.rythme.feature.playlistdetail.presentation.PlaylistDetailScreen
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
import org.koin.compose.koinInject
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
    val musicRepository = koinInject<MusicRepository>()
    val navigationState = rememberNavigationState(
        startRoute = RythmeRoute.Home,
        topLevelRoutes = ALL_TOP_LEVEL_ROUTES
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val activity = LocalActivity.current
    // Player 以浮层方式叠加，Scaffold 始终存活不被销毁
    var playerVisible by remember { mutableStateOf(false) }
    val overlayMenuState = remember { OverlayMenuState() }
    val topBarState = rememberTopBarState()
    val routesInBackStacks = navigationState.backStacks.values.flatMap { it }.toSet()
    LaunchedEffect(routesInBackStacks) { topBarState.retainRoutes(routesInBackStacks) }
    val bottomBarState = rememberBottomBarState(
        initialPrimaryTabIndex = when (navigationState.topLevelRoute) {
            RythmeRoute.Playlist -> 1
            RythmeRoute.Library -> 2
            else -> 0
        }
    )
    val backdrop = rememberLayerBackdrop()

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this@SharedTransitionLayout,
            LocalPlayerVisible provides playerVisible,
            LocalOverlayMenu provides overlayMenuState,
            LocalBackdrop provides backdrop,
            LocalTopBarState provides topBarState,
            LocalBottomBarState provides bottomBarState
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
                        } else if (topBarState.getConfig(navigationState.currentRoute).search?.active == true) {
                            topBarState.getConfig(navigationState.currentRoute).search?.close()
                        } else {
                            if (!navigator.goBack()) {
                                activity?.finish()
                            }
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
                OverlayMenuHost(
                    state = overlayMenuState,
                    onSaveSong = musicRepository::updateSong
                )
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
                routeKey = navigationState.currentRoute,
                config = topBarState.getConfig(navigationState.currentRoute),
                skipAnimation = navigationState.operation == NavigationOperation.TabSwitch,
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
                        if (navigationState.operation == NavigationOperation.TabSwitch) snapSpec
                        else slideInHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { it } togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { -it / 2 }
                    },
                    popTransitionSpec = {
                        if (navigationState.operation == NavigationOperation.TabSwitch) snapSpec
                        else slideInHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { -it / 2 } togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { it }
                    },
                    entries = navigationState.toEntries(
                        entryProvider {
                            entry<RythmeRoute.Home> { HomeScreen() }
                            entry<RythmeRoute.Playlist> {
                                PlayListScreen(
                                    onPlaylistClick = { id ->
                                        navigator.navigate(RythmeRoute.PlaylistDetail(id.toString()))
                                    }
                                )
                            }
                            entry<RythmeRoute.Library> {
                                LibraryScreen(
                                    onArtistsClick = { navigator.navigate(RythmeRoute.ArtistList) },
                                    onAlbumsClick = { navigator.navigate(RythmeRoute.AlbumList) },
                                    onSongsClick = { navigator.navigate(RythmeRoute.SongList) },
                                    onGenresClick = { navigator.navigate(RythmeRoute.GenreList) },
                                    onComposersClick = { navigator.navigate(RythmeRoute.ComposerList) }
                                )
                            }
                            entry<RythmeRoute.Search> { SearchScreen() }
                            entry<RythmeRoute.SongList>{
                                SongListScreen()
                            }
                            entry<RythmeRoute.AlbumList>{
                                AlbumListScreen(
                                    onAlbumClick = { album ->
                                        navigator.navigate(RythmeRoute.AlbumDetail(album.id.toString()))
                                    }
                                )
                            }
                            entry<RythmeRoute.PlaylistDetail> { key ->
                                PlaylistDetailScreen(
                                    viewModel = koinViewModel(key = key.id) {
                                        parametersOf(key.id.toLong())
                                    }
                                )
                            }
                            entry<RythmeRoute.AlbumDetail>(
                                metadata = NavDisplay.transitionSpec {
                                    if (navigationState.operation == NavigationOperation.TabSwitch) {
                                        snapSpec
                                    } else {
                                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                                    }
                                } + NavDisplay.popTransitionSpec {
                                    if (navigationState.operation == NavigationOperation.TabSwitch) {
                                        snapSpec
                                    } else {
                                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                                    }
                                }
                            ) { key ->
                                AlbumDetailScreen(
                                    albumId = key.id,
                                    viewModel = koinViewModel(key = "${key.id}_${key.filterArtistId}_${key.filterComposer}_${key.filterGenre}") {
                                        parametersOf(key.id.toLong(), key.filterArtistId, key.filterComposer, key.filterGenre)
                                    }
                                )
                            }
                            entry<RythmeRoute.ArtistList>{
                                ArtistListScreen(
                                    onArtistClick = { artist ->
                                        navigator.navigate(RythmeRoute.ArtistDetail(artist.id.toString()))
                                    }
                                )
                            }
                            entry<RythmeRoute.ArtistDetail> { key ->
                                ArtistDetailScreen(
                                    artistId = key.id,
                                    onAlbumClick = { album ->
                                        navigator.navigate(RythmeRoute.AlbumDetail(album.id.toString()))
                                    },
                                    viewModel = koinViewModel { parametersOf(key.id.toLong()) }
                                )
                            }
                            entry<RythmeRoute.GenreList> {
                                LibraryFacetListScreen(
                                    facet = LibraryFacet.GENRE,
                                    onItemClick = { genre ->
                                        navigator.navigate(RythmeRoute.GenreDetail(genre))
                                    }
                                )
                            }
                            entry<RythmeRoute.GenreDetail> { key ->
                                LibraryFacetDetailScreen(
                                    facet = LibraryFacet.GENRE,
                                    value = key.genre,
                                    onAlbumClick = { album ->
                                        navigator.navigate(
                                            RythmeRoute.AlbumDetail(album.id.toString(), filterGenre = key.genre)
                                        )
                                    }
                                )
                            }
                            entry<RythmeRoute.ComposerList> {
                                LibraryFacetListScreen(
                                    facet = LibraryFacet.COMPOSER,
                                    onItemClick = { composer ->
                                        navigator.navigate(RythmeRoute.ComposerDetail(composer))
                                    }
                                )
                            }
                            entry<RythmeRoute.ComposerDetail> { key ->
                                LibraryFacetDetailScreen(
                                    facet = LibraryFacet.COMPOSER,
                                    value = key.composer,
                                    onAlbumClick = { album ->
                                        navigator.navigate(
                                            RythmeRoute.AlbumDetail(album.id.toString(), filterComposer = key.composer)
                                        )
                                    }
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
