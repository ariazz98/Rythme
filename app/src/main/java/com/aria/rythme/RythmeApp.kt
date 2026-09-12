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
import androidx.compose.animation.core.animateFloatAsState
import com.aria.rythme.feature.player.presentation.PlayerOverlayMotion
import com.aria.rythme.feature.player.presentation.LocalPlayerOverlayProgress
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.aria.rythme.ui.component.OverlayMenuHost
import com.aria.rythme.ui.component.OverlayMenuState
import com.aria.rythme.ui.component.RythmeHeader
import com.aria.rythme.ui.component.TopBarState
import com.aria.rythme.ui.component.rememberTopBarState
import com.aria.rythme.ui.component.TopBarEntry
import com.aria.rythme.ui.component.rememberTopBarEntryDecorator
import com.aria.rythme.ui.component.HeaderLayout
import com.aria.rythme.ui.component.utils.expandedBottomBarContentInset
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
    val playerDismissMotion = remember { mutableStateOf(com.aria.rythme.feature.player.presentation.PlayerDismissMotion()) }
    val playerOverlayProgress by animateFloatAsState(
        if (playerVisible) 1f else 0f,
        tween(if (playerVisible) PlayerOverlayMotion.ExpandMs else playerDismissMotion.value.durationMs,
            easing = if (playerVisible) PlayerOverlayMotion.ExpandEasing else playerDismissMotion.value.easing),
        label = "playerOverlayMaterial"
    )
    val overlayMenuState = remember { OverlayMenuState() }
    val topBarState = rememberTopBarState()
    LaunchedEffect(navigationState.topLevelRoute, navigationState.currentRoute) {
        if (overlayMenuState.currentMenu is com.aria.rythme.ui.component.OverlayMenu.ActionMenu) {
            overlayMenuState.dismiss()
        }
    }
    val bottomBarState = rememberBottomBarState(
        initialPrimaryTabIndex = when (navigationState.topLevelRoute) {
            RythmeRoute.Pitch -> 1
            RythmeRoute.Library -> 2
            else -> 0
        }
    )
    val backdrop = rememberLayerBackdrop()
    val playerSurfaceOpaque = remember { com.aria.rythme.feature.player.presentation.PlayerSurfaceOpacity() }

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this@SharedTransitionLayout,
            LocalPlayerVisible provides playerVisible,
            LocalPlayerOverlayProgress provides playerOverlayProgress,
            com.aria.rythme.feature.player.presentation.LocalPlayerDismissMotion provides playerDismissMotion,
            com.aria.rythme.feature.player.presentation.LocalPlayerSurfaceOpaque provides playerSurfaceOpaque,
            LocalOverlayMenu provides overlayMenuState,
            LocalBackdrop provides backdrop,
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
                        } else if (topBarState.find(navigationState.topLevelRoute, navigationState.currentRoute)?.search?.active == true) {
                            topBarState.find(navigationState.topLevelRoute, navigationState.currentRoute)?.search?.close()
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
    val settings = org.koin.compose.koinInject<com.aria.rythme.core.music.data.settings.AppSettingsRepository>()
    val profileName by settings.displayName.collectAsStateWithLifecycle("ARiA")
    val targetHeader = topBarState.find(navigationState.topLevelRoute, navigationState.currentRoute)
    var previousHeader by remember { mutableStateOf<TopBarEntry?>(null) }
    SideEffect { if (targetHeader != null) previousHeader = targetHeader }
    // 新 entry 首次提交前保留上一画面，禁止旧页面回调；不先绘制一套兜底按钮。
    val header = targetHeader ?: previousHeader
    Scaffold(
        modifier = Modifier,
        topBar = {
            if (header != null) RythmeHeader(
                entry = header,
                profileName = profileName,
                enabled = targetHeader != null,
                skipAnimation = navigationState.operation == NavigationOperation.TabSwitch,
                onBackClick = { navigator.goBack() }
            ) else Box(Modifier.statusBarsPadding().fillMaxWidth().height(HeaderLayout.toolbar))
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTabIndex = {
                    when (navigationState.topLevelRoute) {
                        RythmeRoute.Home -> 0
                        RythmeRoute.Pitch -> 1
                        RythmeRoute.Library -> 2
                        RythmeRoute.Search -> 3
                        else -> 0
                    }
                },
                onTabSelected = {
                    when (it) {
                        0 -> navigator.navigate(RythmeRoute.Home)
                        1 -> navigator.navigate(RythmeRoute.Pitch)
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
        val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val fixedBottom = expandedBottomBarContentInset(navigationInset.value).dp
        val contentPadding = remember(innerPadding, fixedBottom) {
            // 保留 Scaffold 的顶部和左右安全区，但不把底栏动画高度传给正文。
            object : PaddingValues by innerPadding {
                override fun calculateBottomPadding() = fixedBottom
            }
        }
        val sharedAlbumId = (navigationState.currentRoute as? RythmeRoute.AlbumDetail)?.id

        // Album 专用 SharedTransitionLayout，overlay 在 topBar/bottomBar 之下
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalInnerPadding provides contentPadding,
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
                            entry<RythmeRoute.Home> { HomeScreen(
                                onAlbumClick = { navigator.navigate(RythmeRoute.AlbumDetail(it.id.toString())) },
                                onOriginClick = { origin ->
                                    when (origin.kind) {
                                        "album" -> navigator.navigate(RythmeRoute.AlbumDetail(origin.id.toString(), origin.artistId, origin.composer, origin.genre))
                                        "playlist" -> navigator.navigate(RythmeRoute.PlaylistDetail(origin.id.toString()))
                                    }
                                },
                                onAlbumsClick = { navigator.navigate(RythmeRoute.AlbumList) },
                                onSettingsClick = { navigator.navigate(RythmeRoute.Settings) }
                            ) }
                            entry<RythmeRoute.Settings> { com.aria.rythme.feature.settings.SettingsScreen() }
                            entry<RythmeRoute.Pitch> { com.aria.rythme.feature.pitch.presentation.PitchScreen() }
                            entry<RythmeRoute.Playlist> {
                                PlayListScreen(
                                    onPlaylistClick = { id ->
                                        navigator.navigate(RythmeRoute.PlaylistDetail(id.toString()))
                                    }
                                )
                            }
                            entry<RythmeRoute.Library> {
                                LibraryScreen(
                                    onPlaylistsClick = { navigator.navigate(RythmeRoute.Playlist) },
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
                        },
                        extraDecorators = { tab ->
                            listOf(rememberTopBarEntryDecorator(
                                tab,
                                topBarState,
                                skipNavigationAnimation = navigationState.operation == NavigationOperation.TabSwitch
                            ))
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
