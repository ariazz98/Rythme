package com.aria.rythme.ui.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.R
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import org.junit.Assert.*
import org.junit.Test

class TopBarEntryTest {
    private val album = RythmeRoute.AlbumDetail("1")

    @Test fun switchingNavigationPresentationDoesNotResetSameEntrysScrollBinding() {
        val key = TopBarEntryKey(RythmeRoute.Library, album)
        val scroll = HeaderScrollState().apply { atTop = false; firstVisibleItemIndex = 3 }
        val snapped = TopBarEntry(key, album, mutableStateOf(1f), scroll)
        val animated = TopBarEntry(key, album, mutableStateOf(0f), scroll)
        assertSame(snapped.scroll, animated.scroll)
        assertTrue(animated.scroll.shouldShowChrome(false, true))
    }

    @Test
    fun tabSwitchDoesNotKeepEitherSceneAliveForHeaderAnimation() {
        // 入场和离场都不注册子动画，避免旧场景挡住已经切换的主页。
        assertNull(headerNavigationVisibilitySpec(skipAnimation = true))
    }

    @Test
    fun pushAndPopKeepExistingHeaderDuration() {
        val spec = requireNotNull(headerNavigationVisibilitySpec(skipAnimation = false)).vectorize(Float.VectorConverter)
        for ((from, to) in listOf(0f to 1f, 1f to 0f)) {
            assertEquals(
                400_000_000L,
                spec.getDurationNanos(AnimationVector1D(from), AnimationVector1D(to), AnimationVector1D(0f))
            )
        }
    }

    @Test fun actionMainSegmentUsesActualNavigationTimeAndOnlyTailIsLocal() {
        val visibilityAt100ms = androidx.compose.animation.core.FastOutSlowInEasing.transform(.25f)
        assertEquals(100f / 520f, HeaderActionMotion.navigationPhase(visibilityAt100ms), .0001f)
        assertEquals(400f / 520f, HeaderActionMotion.navigationPhase(1f), .0001f)
        assertEquals(120, HeaderActionMotion.TailMillis)
    }

    private fun entry(tab: NavKey, route: NavKey = album, contentKey: Any = route): TopBarEntry =
        TopBarEntry(TopBarEntryKey(tab, contentKey), route, mutableStateOf(1f)).apply {
            update(TopBarConfig(title = "Albums"), PageSearchState())
        }

    @Test
    fun sameRouteInDifferentTabsDoesNotShareSearchOrConfiguration() {
        val state = TopBarState()
        val library = entry(RythmeRoute.Library)
        val search = entry(RythmeRoute.Search)
        state.attach(library)
        state.attach(search)
        library.search!!.query = "Eagles"
        assertSame(library, state.find(RythmeRoute.Library, album))
        assertSame(search, state.find(RythmeRoute.Search, album))
        assertEquals("", search.search!!.query)
        assertNotEquals(library.key, search.key)
    }

    @Test
    fun filteredAlbumUsesFullNavigationRoute() {
        val state = TopBarState()
        val filtered = album.copy(filterComposer = "Composer", filterGenre = "Rock")
        val plainEntry = entry(RythmeRoute.Library)
        val filteredEntry = entry(RythmeRoute.Library, filtered)
        state.attach(plainEntry)
        state.attach(filteredEntry)
        assertSame(plainEntry, state.find(RythmeRoute.Library, album))
        assertSame(filteredEntry, state.find(RythmeRoute.Library, filtered))
    }

    @Test
    fun inactiveAndExitingEntriesRemainUntilNavigationReleasesTheirContentKey() {
        val state = TopBarState()
        val first = entry(RythmeRoute.Library)
        val second = entry(RythmeRoute.Search)
        state.attach(first)
        state.attach(second)
        repeat(3) {
            assertSame(first, state.find(RythmeRoute.Library, album))
            assertSame(second, state.find(RythmeRoute.Search, album))
        }
        // 对应 Navigation3 的 onPop，而不是切 Tab 或列表/网格离开组合。
        state.remove(first.key)
        assertNull(state.find(RythmeRoute.Library, album))
        assertSame(second, state.find(RythmeRoute.Search, album))
    }

    @Test
    fun navigationContentKeyIsPreservedInsteadOfReconstructedFromRoute() {
        val state = TopBarState()
        val binding = entry(RythmeRoute.Library, contentKey = "nav-entry-content-key")
        state.attach(binding)
        state.remove(TopBarEntryKey(RythmeRoute.Library, album))
        assertSame(binding, state.find(RythmeRoute.Library, album))
        state.remove(binding.key)
        assertNull(state.find(RythmeRoute.Library, album))
    }

    @Test
    fun layoutRebindKeepsSearchAndNavigationProgressSeparateFromDescription() {
        val binding = entry(RythmeRoute.Library)
        val query = binding.search!!
        query.open()
        query.query = "Hotel"
        val progress = binding.navigationVisibility
        binding.update(TopBarConfig(title = "Grid"), query)
        assertSame(query, binding.search)
        assertSame(progress, binding.navigationVisibility)
        assertEquals("Hotel", binding.search!!.query)
        assertTrue(binding.search!!.active)
    }

    @Test
    fun sameVisualActionReceivesLatestCallback() {
        val binding = entry(RythmeRoute.Library)
        var selected = ""
        val initial = Action.Icon("more", R.drawable.ic_more, onClick = { selected = "old" })
        val updated = initial.copy(onClick = { selected = "new" })
        binding.update(TopBarConfig(actions = listOf(initial)), null)
        binding.update(TopBarConfig(actions = listOf(updated)), null)
        binding.config!!.actions.single().onClick!!.invoke()
        assertEquals("new", selected)
        assertEquals(listOf(initial).contentKey(), binding.config!!.actions.contentKey())
    }

    @Test
    fun entryIsNotSelectableBeforeItsPageDeclaresHeader() {
        val state = TopBarState()
        val binding = TopBarEntry(TopBarEntryKey(RythmeRoute.Library, album), album, mutableStateOf(0f))
        state.attach(binding)
        assertNull(state.find(RythmeRoute.Library, album))
        binding.update(TopBarConfig(showBackButton = true), null)
        assertSame(binding, state.find(RythmeRoute.Library, album))
    }
}
