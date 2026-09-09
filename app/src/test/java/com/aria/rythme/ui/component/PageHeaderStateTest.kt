package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class PageHeaderStateTest {
    @Test fun controlledSearchWritesToItsOwnerAndClosesWithAnEmptyQuery() {
        var ownerQuery = "Hotel"
        val search = PageSearchState.controlled(false, { ownerQuery }, { ownerQuery = it })
        assertEquals("Hotel", search.query)
        ownerQuery = "Eagles"
        assertEquals("Eagles", search.query)
        search.query = "Moon"
        assertEquals("Moon", ownerQuery)
        search.open()
        search.close()
        assertFalse(search.active)
        assertEquals("", ownerQuery)
    }

    @Test fun repeatedOpenDoesNotReplaceTheCapturedTransitionOrigin() {
        val search = PageSearchState()
        val origin = Rect(21f, 140f, 320f, 184f)
        search.bounds = origin
        search.open()
        search.bounds = Rect.Zero
        search.open()
        assertEquals(origin, search.origin)
    }

    @Test fun rootUsesActualTopStateWithoutWaitingForATitleToMove() {
        val scroll = HeaderScrollState()
        assertFalse(scroll.shouldShowChrome(isRoot = true, hasStandardTitle = false))
        scroll.atTop = false
        assertTrue(scroll.shouldShowChrome(isRoot = true, hasStandardTitle = false))
        scroll.atTop = true
        assertFalse(scroll.shouldShowChrome(isRoot = true, hasStandardTitle = false))
    }

    @Test fun standardTitleSwitchesAtFixedScrollDistanceAndReversesBelowIt() {
        val scroll = HeaderScrollState().apply {
            atTop = false
            firstVisibleItemScrollOffsetDp = 47f
        }
        assertFalse(scroll.shouldShowChrome(false, true))
        scroll.firstVisibleItemScrollOffsetDp = 48f
        assertTrue(scroll.shouldShowChrome(false, true))
        scroll.firstVisibleItemScrollOffsetDp = 49f
        assertTrue(scroll.shouldShowChrome(false, true))
        scroll.firstVisibleItemScrollOffsetDp = 47f
        assertFalse(scroll.shouldShowChrome(false, true))
    }

    @Test fun recycledTitleStaysCompactButReturningToTopAlwaysClearsChrome() {
        val scroll = HeaderScrollState().apply {
            atTop = false
            firstVisibleItemIndex = 4
        }
        assertTrue(scroll.shouldShowChrome(false, true))
        scroll.atTop = true
        assertFalse(scroll.shouldShowChrome(false, true))
    }

    @Test fun pagesWithoutStandardTitleNeverShowScrollChrome() {
        val scroll = HeaderScrollState().apply {
            atTop = false
            firstVisibleItemScrollOffsetDp = 200f
        }
        assertFalse(scroll.shouldShowChrome(false, false))
        scroll.firstVisibleItemIndex = 2
        assertFalse(scroll.shouldShowChrome(false, false))
    }

    @Test fun scrollStateIsIsolatedPerEntry() {
        val first = HeaderScrollState().apply { atTop = false }
        val second = HeaderScrollState()
        assertFalse(first.shouldShowChrome(false, true))
        assertTrue(first.shouldShowChrome(true, true))
        assertFalse(second.shouldShowChrome(true, true))
    }

    @Test
    fun searchUsesCapturedOriginAndQueryIsOwnedByItsPage() {
        val albums = PageSearchState()
        val artists = PageSearchState()
        val origin = Rect(21f, 140f, 320f, 184f)
        albums.bounds = origin
        albums.open()
        albums.bounds = Rect.Zero
        albums.query = " hotel "
        assertEquals(origin, albums.origin)
        assertTrue(albums.matches("Hotel California", "Eagles"))
        assertFalse(albums.matches("Other album"))
        assertEquals("", artists.query)
        assertFalse(artists.active)
        albums.close()
        assertFalse(albums.active)
        assertEquals("", albums.query)
        assertTrue(albums.matches("Other album"))
    }

}
