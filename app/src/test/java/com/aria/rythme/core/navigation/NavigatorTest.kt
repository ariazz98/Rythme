package com.aria.rythme.core.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    @Test
    fun switchingTabsDoesNotPlaceOldSceneButPreservesItsStack() {
        val state = createState()
        val navigator = Navigator(state)
        navigator.navigate(RythmeRoute.Library)
        navigator.navigate(RythmeRoute.AlbumList)
        val stack = state.backStacks.getValue(RythmeRoute.Library).toList()
        navigator.navigate(RythmeRoute.Home)
        assertFalse(state.shouldPlaceTab(RythmeRoute.Library))
        assertTrue(state.shouldPlaceTab(RythmeRoute.Home))
        assertEquals(stack, state.backStacks.getValue(RythmeRoute.Library).toList())
        navigator.navigate(RythmeRoute.Library)
        assertTrue(state.shouldPlaceTab(RythmeRoute.Library))
        assertEquals(RythmeRoute.AlbumList, state.currentRoute)
        navigator.goBack()
        assertTrue(state.shouldPlaceTab(RythmeRoute.Library))
        // 普通导航仍由 NavDisplay 控制前后两个场景，placement 策略不介入。
        assertTrue(state.shouldPlaceTab(RythmeRoute.Home))
    }

    @Test
    fun primaryTabsAreHomePitchLibraryAndSearchInDisplayOrder() {
        assertEquals(listOf(RythmeRoute.Home, RythmeRoute.Pitch, RythmeRoute.Library, RythmeRoute.Search), ALL_TOP_LEVEL_ROUTES.toList())
        assertEquals(ALL_TOP_LEVEL_ROUTES.toList(), com.aria.rythme.feature.navigationbar.data.model.TOP_LEVEL_DESTINATIONS.keys.toList())
    }

    @Test
    fun playlistsRemainInsideTheLibraryStackWhenSwitchingToPitch() {
        val state = createState()
        val navigator = Navigator(state)
        navigator.navigate(RythmeRoute.Library)
        navigator.navigate(RythmeRoute.Playlist)
        assertEquals(NavigationOperation.Push, state.operation)
        assertEquals(RythmeRoute.Library, state.topLevelRoute)
        navigator.navigate(RythmeRoute.PlaylistDetail("12"))
        navigator.navigate(RythmeRoute.Pitch)
        assertEquals(RythmeRoute.Pitch, state.currentRoute)
        navigator.navigate(RythmeRoute.Library)
        assertEquals(RythmeRoute.PlaylistDetail("12"), state.currentRoute)
        assertTrue(navigator.goBack())
        assertEquals(RythmeRoute.Playlist, state.currentRoute)
        assertTrue(navigator.goBack())
        assertEquals(RythmeRoute.Library, state.currentRoute)
    }

    @Test
    fun pushAndPopExposeExplicitOperations() {
        val state = createState()
        val navigator = Navigator(state)

        navigator.navigate(RythmeRoute.ArtistList)
        assertEquals(NavigationOperation.Push, state.operation)
        assertEquals(RythmeRoute.ArtistList, state.currentRoute)

        assertTrue(navigator.goBack())
        assertEquals(NavigationOperation.Pop, state.operation)
        assertEquals(RythmeRoute.Home, state.currentRoute)
    }

    @Test
    fun tabSwitchPreservesEachTabStack() {
        val state = createState()
        val navigator = Navigator(state)

        navigator.navigate(RythmeRoute.Library)
        navigator.navigate(RythmeRoute.AlbumList)
        navigator.navigate(RythmeRoute.Home)
        navigator.navigate(RythmeRoute.ArtistList)
        navigator.navigate(RythmeRoute.Library)

        assertEquals(NavigationOperation.TabSwitch, state.operation)
        assertEquals(RythmeRoute.AlbumList, state.currentRoute)

        navigator.navigate(RythmeRoute.Home)
        assertEquals(RythmeRoute.ArtistList, state.currentRoute)
    }

    @Test
    fun rootBackReturnsToStartTabThenAllowsActivityExit() {
        val state = createState()
        val navigator = Navigator(state)

        navigator.navigate(RythmeRoute.Library)
        assertTrue(navigator.goBack())
        assertEquals(NavigationOperation.TabSwitch, state.operation)
        assertEquals(RythmeRoute.Home, state.currentRoute)

        assertFalse(navigator.goBack())
        assertEquals(NavigationOperation.Idle, state.operation)
    }

    private fun createState(): NavigationState {
        val routes: Set<NavKey> = ALL_TOP_LEVEL_ROUTES
        val stacks: Map<NavKey, NavBackStack<NavKey>> = routes.associateWith { route ->
            NavBackStack(route)
        }
        return NavigationState(
            startRoute = RythmeRoute.Home,
            topLevelRoute = mutableStateOf<NavKey>(RythmeRoute.Home),
            backStacks = stacks
        )
    }
}
