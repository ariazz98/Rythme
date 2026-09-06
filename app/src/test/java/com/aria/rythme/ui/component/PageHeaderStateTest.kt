package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Rect
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import org.junit.Assert.*
import org.junit.Test

class PageHeaderStateTest {
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

    @Test
    fun routeRetentionKeepsInactivePagesAndOnlyClearsPoppedPages() {
        val topBar = TopBarState()
        val albumRoute = RythmeRoute.AlbumDetail("1")
        val artistRoute = RythmeRoute.ArtistDetail("2")
        val search = PageSearchState("Eagles")
        val config = TopBarConfig(title = "Albums", search = search)
        topBar.updateConfig(albumRoute, config)
        topBar.updateConfig(artistRoute, TopBarConfig(title = "Artist"))
        topBar.retainRoutes(setOf(RythmeRoute.Home, albumRoute))
        assertSame(config, topBar.getConfig(albumRoute))
        assertNull(topBar.getConfig(artistRoute).title)
        topBar.updateConfig(albumRoute, config.copy(title = "Albums in list layout"))
        assertSame(search, topBar.getConfig(albumRoute).search)
    }
}
